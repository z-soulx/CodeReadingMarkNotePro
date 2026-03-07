package jp.kitabatakep.intellij.plugins.codereadingnote.sync.github;

import com.intellij.openapi.project.Project;
import jp.kitabatakep.intellij.plugins.codereadingnote.CodeReadingNoteBundle;
import jp.kitabatakep.intellij.plugins.codereadingnote.sync.AbstractSyncProvider;
import jp.kitabatakep.intellij.plugins.codereadingnote.sync.FilePushReport;
import jp.kitabatakep.intellij.plugins.codereadingnote.sync.SyncConfig;
import jp.kitabatakep.intellij.plugins.codereadingnote.sync.SyncProviderType;
import jp.kitabatakep.intellij.plugins.codereadingnote.sync.SyncResult;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * GitHub同步提供者实现
 * 使用GitHub REST API v3进行文件操作
 */
public class GitHubSyncProvider extends AbstractSyncProvider {
    
    private static final String GITHUB_API_BASE = "https://api.github.com";
    private static final int TIMEOUT = 30000; // 30秒超时
    
    @Override
    @NotNull
    public SyncProviderType getType() {
        return SyncProviderType.GITHUB;
    }
    
    @Override
    @NotNull
    protected SyncResult doValidateConfig(@NotNull SyncConfig config) {
        if (!(config instanceof GitHubSyncConfig)) {
            return SyncResult.failure("Invalid config type");
        }
        
        GitHubSyncConfig ghConfig = (GitHubSyncConfig) config;
        
        try {
            // 测试连接GitHub API
            String apiUrl = String.format("%s/repos/%s", GITHUB_API_BASE, ghConfig.getRepository());
            HttpURLConnection conn = createConnection(apiUrl, "GET", ghConfig.getToken());
            
            int responseCode = conn.getResponseCode();
            conn.disconnect();
            
            if (responseCode == 200) {
                return SyncResult.success("Configuration validated successfully");
            } else if (responseCode == 401) {
                return SyncResult.failure("Token authentication failed, please check access permissions");
            } else if (responseCode == 404) {
                return SyncResult.failure("Repository not found or no access permission");
            } else {
                return SyncResult.failure("Validation failed: HTTP " + responseCode);
            }
        } catch (Exception e) {
            LOG.warn("Config validation failed", e);
            return SyncResult.failure("Validation failed", e);
        }
    }
    
    @Override
    @NotNull
    public SyncResult push(@NotNull Project project, @NotNull SyncConfig config, 
                          @NotNull String data, @NotNull String projectIdentifier) {
        if (!(config instanceof GitHubSyncConfig)) {
            return SyncResult.failure("Invalid config type");
        }
        
        GitHubSyncConfig ghConfig = (GitHubSyncConfig) config;
        
        try {
            String filePath = buildFilePath(ghConfig, projectIdentifier);
            String md5FilePath = filePath + ".md5";
            
            // 计算本地数据的MD5
            String localMd5 = calculateMD5(data);
            
            // 获取远端MD5
            String remoteMd5 = getRemoteMD5(ghConfig, md5FilePath);
            
            // 比较MD5，如果一致则跳过推送
            if (localMd5.equals(remoteMd5)) {
                LOG.info("MD5 check: No changes detected, skipping push");
                return SyncResult.success(CodeReadingNoteBundle.message("message.push.no.changes"));
            }
            
            LOG.info("MD5 check: Changes detected, pushing to remote");
            
            // 先尝试获取文件SHA（如果文件存在）
            String sha = getFileSha(ghConfig, filePath);
            String md5Sha = getFileSha(ghConfig, md5FilePath);
            
            // 创建或更新文件
            SyncResult result = pushFile(ghConfig, filePath, data, sha);
            
            // 如果推送成功，同时推送MD5文件
            if (result.isSuccess()) {
                pushMD5File(ghConfig, md5FilePath, localMd5, md5Sha);
            }
            
            return result;
            
        } catch (Exception e) {
            LOG.error("Push to GitHub failed", e);
            return SyncResult.failure(formatError("Push", e), e);
        }
    }
    
    @Override
    @NotNull
    public SyncResult pull(@NotNull Project project, @NotNull SyncConfig config, 
                          @NotNull String projectIdentifier) {
        if (!(config instanceof GitHubSyncConfig)) {
            return SyncResult.failure("Invalid config type");
        }
        
        GitHubSyncConfig ghConfig = (GitHubSyncConfig) config;
        
        try {
            String filePath = buildFilePath(ghConfig, projectIdentifier);
            String content = getFileContent(ghConfig, filePath);
            
            if (content == null) {
                return SyncResult.failure("Remote file not found");
            }
            
            return SyncResult.success("Pulled successfully", content);
            
        } catch (Exception e) {
            LOG.error("Pull from GitHub failed", e);
            return SyncResult.failure(formatError("Pull", e), e);
        }
    }
    
    @Override
    public long getRemoteTimestamp(@NotNull Project project, @NotNull SyncConfig config, 
                                   @NotNull String projectIdentifier) {
        if (!(config instanceof GitHubSyncConfig)) {
            return 0;
        }
        
        GitHubSyncConfig ghConfig = (GitHubSyncConfig) config;
        
        try {
            String filePath = buildFilePath(ghConfig, projectIdentifier);
            return getFileTimestamp(ghConfig, filePath);
        } catch (Exception e) {
            LOG.warn("Failed to get remote timestamp", e);
            return 0;
        }
    }
    
    /**
     * 构建远程文件路径
     */
    @NotNull
    private String buildFilePath(@NotNull GitHubSyncConfig config, @NotNull String projectIdentifier) {
        String basePath = config.getBasePath();
        if (basePath.endsWith("/")) {
            basePath = basePath.substring(0, basePath.length() - 1);
        }
        return String.format("%s/%s/CodeReadingNote.xml", basePath, projectIdentifier);
    }
    
    /**
     * 获取文件的SHA值（用于更新文件）
     */
    private String getFileSha(@NotNull GitHubSyncConfig config, @NotNull String filePath) {
        try {
            String apiUrl = buildApiUrl(config, filePath);
            HttpURLConnection conn = createConnection(apiUrl, "GET", config.getToken());
            
            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                String response = readResponse(conn);
                conn.disconnect();
                
                // 从JSON响应中提取SHA
                Pattern pattern = Pattern.compile("\"sha\"\\s*:\\s*\"([^\"]+)\"");
                Matcher matcher = pattern.matcher(response);
                if (matcher.find()) {
                    return matcher.group(1);
                }
            } else if (responseCode == 404) {
                // 文件不存在，返回null
                conn.disconnect();
                return null;
            }
            conn.disconnect();
        } catch (Exception e) {
            LOG.debug("Failed to get file SHA", e);
        }
        return null;
    }
    
    /**
     * 获取文件内容
     */
    private String getFileContent(@NotNull GitHubSyncConfig config, @NotNull String filePath) throws IOException {
        String apiUrl = buildApiUrl(config, filePath);
        HttpURLConnection conn = createConnection(apiUrl, "GET", config.getToken());
        
        int responseCode = conn.getResponseCode();
        if (responseCode == 200) {
            String response = readResponse(conn);
            conn.disconnect();
            
            // 从JSON响应中提取content（base64编码）
            Pattern pattern = Pattern.compile("\"content\"\\s*:\\s*\"([^\"]+)\"");
            Matcher matcher = pattern.matcher(response);
            if (matcher.find()) {
                String base64Content = matcher.group(1).replace("\\n", "");
                byte[] decoded = Base64.getDecoder().decode(base64Content);
                return new String(decoded, StandardCharsets.UTF_8);
            }
        } else if (responseCode == 404) {
            conn.disconnect();
            return null;
        }
        
        String error = readError(conn);
        conn.disconnect();
        throw new IOException("Failed to get file: " + error);
    }
    
    /**
     * 获取文件最后修改时间戳
     */
    private long getFileTimestamp(@NotNull GitHubSyncConfig config, @NotNull String filePath) throws IOException {
        String commitsUrl = String.format("%s/repos/%s/commits?path=%s&page=1&per_page=1", 
            GITHUB_API_BASE, config.getRepository(), URLEncoder.encode(filePath, StandardCharsets.UTF_8));
        
        HttpURLConnection conn = createConnection(commitsUrl, "GET", config.getToken());
        
        int responseCode = conn.getResponseCode();
        if (responseCode == 200) {
            String response = readResponse(conn);
            conn.disconnect();
            
            // 提取commit日期
            Pattern pattern = Pattern.compile("\"date\"\\s*:\\s*\"([^\"]+)\"");
            Matcher matcher = pattern.matcher(response);
            if (matcher.find()) {
                String dateStr = matcher.group(1);
                // 解析ISO 8601日期格式
                return parseIso8601(dateStr);
            }
        }
        conn.disconnect();
        return 0;
    }
    
    /**
     * 推送文件到GitHub
     */
    @NotNull
    private SyncResult pushFile(@NotNull GitHubSyncConfig config, @NotNull String filePath, 
                                @NotNull String content, String sha) {
        try {
            String apiUrl = buildApiUrl(config, filePath);
            HttpURLConnection conn = createConnection(apiUrl, "PUT", config.getToken());
            
            // 构建请求体
            String base64Content = Base64.getEncoder().encodeToString(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder json = new StringBuilder();
            json.append("{");
            json.append("\"message\":\"Update CodeReadingNote\",");
            json.append("\"content\":\"").append(base64Content).append("\",");
            json.append("\"branch\":\"").append(escapeJson(config.getBranch())).append("\"");
            if (sha != null) {
                json.append(",\"sha\":\"").append(escapeJson(sha)).append("\"");
            }
            json.append("}");
            
            // 发送请求
            try (OutputStream os = conn.getOutputStream()) {
                os.write(json.toString().getBytes(StandardCharsets.UTF_8));
            }
            
            int responseCode = conn.getResponseCode();
            if (responseCode == 200 || responseCode == 201) {
                conn.disconnect();
                return SyncResult.success(sha == null ? "File created successfully" : "File updated successfully");
            } else {
                String error = readError(conn);
                conn.disconnect();
                return SyncResult.failure(error);
            }
            
        } catch (Exception e) {
            LOG.error("Push file failed", e);
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            return SyncResult.failure("[Local] " + msg, e);
        }
    }
    
    /**
     * Build GitHub API URL with proper URL-encoding of each path segment.
     * Non-ASCII characters (e.g. Chinese) must be percent-encoded.
     */
    @NotNull
    private String buildApiUrl(@NotNull GitHubSyncConfig config, @NotNull String filePath) {
        String[] segments = filePath.split("/");
        StringBuilder encodedPath = new StringBuilder();
        for (int i = 0; i < segments.length; i++) {
            if (i > 0) encodedPath.append("/");
            encodedPath.append(URLEncoder.encode(segments[i], StandardCharsets.UTF_8)
                    .replace("+", "%20"));
        }
        return String.format("%s/repos/%s/contents/%s",
            GITHUB_API_BASE, config.getRepository(), encodedPath);
    }
    
    /**
     * 创建HTTP连接
     */
    @NotNull
    private HttpURLConnection createConnection(@NotNull String urlStr, @NotNull String method, 
                                               @NotNull String token) throws IOException {
        URL url = URI.create(urlStr).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setConnectTimeout(TIMEOUT);
        conn.setReadTimeout(TIMEOUT);
        conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
        
        // 自动识别 Token 类型并使用正确的认证方式
        // Fine-grained tokens (github_pat_) 使用 Bearer，Classic tokens (ghp_) 使用 token
        if (token.startsWith("github_pat_")) {
            conn.setRequestProperty("Authorization", "Bearer " + token);
        } else {
            conn.setRequestProperty("Authorization", "token " + token);
        }
        
        conn.setRequestProperty("User-Agent", "CodeReadingNotePro");
        
        if ("PUT".equals(method) || "POST".equals(method) || "DELETE".equals(method)) {
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        }
        
        return conn;
    }
    
    /**
     * 读取响应内容
     */
    @NotNull
    private String readResponse(@NotNull HttpURLConnection conn) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        }
    }
    
    /**
     * Reads the error response and produces a clean, user-friendly message.
     * GitHub API errors may be JSON ({"message":"..."}) or HTML error pages.
     */
    @NotNull
    private String readError(@NotNull HttpURLConnection conn) {
        int httpCode;
        try {
            httpCode = conn.getResponseCode();
        } catch (Exception e) {
            return "Connection error";
        }

        String rawBody = "";
        try {
            InputStream errorStream = conn.getErrorStream();
            if (errorStream != null) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(errorStream, StandardCharsets.UTF_8))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    rawBody = sb.toString().trim();
                }
            }
        } catch (Exception e) {
            LOG.debug("Failed to read error stream", e);
        }

        return formatApiError(httpCode, rawBody);
    }

    /**
     * Converts a raw HTTP error into a clean, actionable message.
     * Tries JSON "message" field first, strips HTML if needed, and
     * provides guidance based on the HTTP status code.
     */
    @NotNull
    private String formatApiError(int httpCode, @NotNull String rawBody) {
        // Try to extract JSON "message" field (GitHub API standard error format)
        if (rawBody.startsWith("{")) {
            Pattern msgPattern = Pattern.compile("\"message\"\\s*:\\s*\"([^\"]+)\"");
            Matcher matcher = msgPattern.matcher(rawBody);
            if (matcher.find()) {
                return "[GitHub API " + httpCode + "] " + matcher.group(1);
            }
        }

        // If body is HTML, extract just the <title> text and decode HTML entities
        if (rawBody.contains("<html") || rawBody.contains("<HTML") || rawBody.contains("<!DOCTYPE")) {
            Pattern titlePattern = Pattern.compile("<title>([^<]+)</title>", Pattern.CASE_INSENSITIVE);
            Matcher matcher = titlePattern.matcher(rawBody);
            String summary = matcher.find() ? decodeHtmlEntities(matcher.group(1).trim()) : "GitHub returned an HTML error page";
            return "[GitHub API " + httpCode + "] " + summary + describeHttpAction(httpCode);
        }

        // Plain text or empty
        if (!rawBody.isEmpty() && rawBody.length() < 200) {
            return "[GitHub API " + httpCode + "] " + rawBody;
        }

        return "[GitHub API " + httpCode + "]" + describeHttpAction(httpCode);
    }

    @NotNull
    private static String describeHttpAction(int httpCode) {
        switch (httpCode) {
            case 400: return " — Bad request. File path may contain invalid characters or be too long.";
            case 401: return " — Authentication failed. Check your access token.";
            case 403: return " — Permission denied. Token may lack write access to this repository.";
            case 404: return " — Resource not found. Check repository name and branch.";
            case 409: return " — Conflict. The file may have been modified concurrently.";
            case 422: return " — Validation failed. The file content or commit message may be invalid.";
            case 429: return " — Rate limited. Please wait a moment and retry.";
            default:
                if (httpCode >= 500) return " — GitHub server error. Please retry later.";
                return "";
        }
    }

    @NotNull
    private static String decodeHtmlEntities(@NotNull String html) {
        return html
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&middot;", "\u00B7")
            .replace("&mdash;", "\u2014")
            .replace("&ndash;", "\u2013")
            .replace("&#39;", "'");
    }
    
    /**
     * 转义JSON字符串
     */
    @NotNull
    private String escapeJson(@NotNull String str) {
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
    
    /**
     * 解析ISO 8601日期格式
     */
    private long parseIso8601(@NotNull String dateStr) {
        try {
            // 简单的ISO 8601解析 (格式: 2024-11-01T12:34:56Z)
            return java.time.Instant.parse(dateStr).toEpochMilli();
        } catch (Exception e) {
            LOG.debug("Failed to parse date: " + dateStr, e);
            return 0;
        }
    }
    
    /**
     * 计算字符串的MD5哈希值
     */
    @NotNull
    private String calculateMD5(@NotNull String data) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(data.getBytes(StandardCharsets.UTF_8));
            
            // 转换为16进制字符串
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            LOG.error("MD5 algorithm not available", e);
            return "";
        }
    }
    
    /**
     * 获取远端MD5文件内容
     */
    private String getRemoteMD5(@NotNull GitHubSyncConfig config, @NotNull String md5FilePath) {
        try {
            String content = getFileContent(config, md5FilePath);
            if (content != null) {
                // MD5文件内容就是MD5哈希值，去除可能的空白字符
                return content.trim();
            }
        } catch (Exception e) {
            LOG.debug("Failed to get remote MD5", e);
        }
        return null;
    }
    
    // ========== File-based sync for AI configs ==========

    @Override
    @NotNull
    public SyncResult pushFiles(@NotNull Project project, @NotNull SyncConfig config,
                                @NotNull java.util.Map<String, byte[]> files, @NotNull String projectIdentifier,
                                @NotNull java.util.Set<String> emptyDirs,
                                @NotNull java.util.Map<String, String> lastPushedFileHashes,
                                boolean forceAll) {
        if (!(config instanceof GitHubSyncConfig)) {
            return SyncResult.failure("Invalid config type");
        }

        GitHubSyncConfig ghConfig = (GitHubSyncConfig) config;
        FilePushReport report = new FilePushReport();

        // Read old manifest to find files/dirs that should be deleted from remote
        java.util.Set<String> oldManifestPaths = readRemoteManifest(ghConfig, projectIdentifier);
        java.util.Set<String> newPaths = files.keySet();
        java.util.Set<String> newEmptyDirMarkers = new java.util.LinkedHashSet<>();
        for (String dir : emptyDirs) {
            newEmptyDirMarkers.add(dir.endsWith("/") ? dir : dir + "/");
        }

            // Delete remote files no longer tracked, and delete stale .gitkeep for removed empty dirs
        for (String oldPath : oldManifestPaths) {
            if (oldPath.endsWith("/")) {
                if (!newEmptyDirMarkers.contains(oldPath)) {
                    // Delete the .gitkeep file for the stale empty dir
                    String gitkeepPath = buildAIConfigFilePath(ghConfig, projectIdentifier, oldPath + ".gitkeep");
                    if (deleteRemoteFile(ghConfig, gitkeepPath, "Remove empty dir placeholder: " + oldPath)) {
                        report.addDeleted(oldPath);
                    }
                }
                continue;
            }
            if (!newPaths.contains(oldPath)) {
                String remotePath = buildAIConfigFilePath(ghConfig, projectIdentifier, oldPath);
                if (deleteRemoteFile(ghConfig, remotePath, "Remove untracked AI config: " + oldPath)) {
                    report.addDeleted(oldPath);
                } else {
                    report.addFailed(oldPath, "Failed to delete from remote");
                }
            }
        }

        // Push each tracked file, with optional per-file MD5 skip
        for (java.util.Map.Entry<String, byte[]> entry : files.entrySet()) {
            String relativePath = entry.getKey();
            byte[] fileBytes = entry.getValue();
            String content = new String(fileBytes, StandardCharsets.UTF_8);

            // Per-file MD5 comparison
            if (!forceAll && !lastPushedFileHashes.isEmpty()) {
                String localMd5 = calculateMD5(content);
                String previousMd5 = lastPushedFileHashes.get(relativePath);
                if (previousMd5 != null && localMd5.equals(previousMd5)) {
                    report.addSkipped(relativePath);
                    continue;
                }
            }

            String remotePath = buildAIConfigFilePath(ghConfig, projectIdentifier, relativePath);
            try {
                String sha = getFileSha(ghConfig, remotePath);
                SyncResult result = pushFileWithMessage(ghConfig, remotePath, content, sha,
                        "Update AI config: " + relativePath);
                if (result.isSuccess()) {
                    report.addPushed(relativePath);
                } else {
                    String reason = result.getMessage() != null ? result.getMessage() : "Unknown error";
                    report.addFailed(relativePath, reason);
                    LOG.warn("Failed to push AI config file " + relativePath + ": " + reason);
                }
            } catch (Exception e) {
                String reason = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                report.addFailed(relativePath, reason);
                LOG.warn("Failed to push AI config file " + relativePath, e);
            }
        }

        // Push .gitkeep placeholder for each empty directory so the folder appears on GitHub
        for (String dir : newEmptyDirMarkers) {
            String gitkeepPath = buildAIConfigFilePath(ghConfig, projectIdentifier, dir + ".gitkeep");
            try {
                String sha = getFileSha(ghConfig, gitkeepPath);
                SyncResult gkResult = pushFileWithMessage(ghConfig, gitkeepPath, "",
                        sha, "Create empty dir placeholder: " + dir);
                if (gkResult.isSuccess()) {
                    report.addEmptyDir(dir);
                } else {
                    report.addFailed(dir, "Failed to create .gitkeep: " + gkResult.getMessage());
                }
            } catch (Exception e) {
                report.addFailed(dir, "Failed to create .gitkeep: " + e.getMessage());
            }
        }

        // Build and push manifest (file paths + empty dir markers)
        java.util.Set<String> manifestPaths = new java.util.LinkedHashSet<>(newPaths);
        manifestPaths.addAll(newEmptyDirMarkers);
        pushAIConfigManifest(ghConfig, projectIdentifier, manifestPaths);

        // Determine overall success/failure
        boolean hasFailures = report.hasFailures();
        String reportJson = report.toJson();

        if (files.isEmpty() && report.getDeletedFiles().isEmpty() && emptyDirs.isEmpty()) {
            return SyncResult.success("Remote AI configs cleared (0 tracked files)", reportJson);
        }

        if (hasFailures) {
            return SyncResult.failureWithData("ai.config.push.partial.failure", reportJson);
        }

        return SyncResult.success("ai.config.push.completed", reportJson);
    }

    @Override
    @NotNull
    public SyncResult pullFiles(@NotNull Project project, @NotNull SyncConfig config,
                                @NotNull String projectIdentifier) {
        if (!(config instanceof GitHubSyncConfig)) {
            return SyncResult.failure("Invalid config type");
        }

        GitHubSyncConfig ghConfig = (GitHubSyncConfig) config;

        try {
            // Read the manifest to know which files exist
            String manifestPath = buildAIConfigManifestPath(ghConfig, projectIdentifier);
            String manifestContent = getFileContent(ghConfig, manifestPath);

            if (manifestContent == null || manifestContent.trim().isEmpty()) {
                return SyncResult.success("No AI config files on remote", "{}");
            }

            // Parse manifest (one relative path per line; dirs end with '/')
            String[] manifestLines = manifestContent.trim().split("\n");
            StringBuilder jsonBuilder = new StringBuilder("{");
            boolean first = true;

            for (String line : manifestLines) {
                line = line.trim();
                if (line.isEmpty()) continue;

                // Directory markers end with '/' — include as-is for the adapter to create
                if (line.endsWith("/")) {
                    if (!first) jsonBuilder.append(",");
                    jsonBuilder.append("\"").append(escapeJson(line)).append("\":\"\"");
                    first = false;
                    continue;
                }

                String remotePath = buildAIConfigFilePath(ghConfig, projectIdentifier, line);
                try {
                    String content = getFileContent(ghConfig, remotePath);
                    if (content != null) {
                        if (!first) jsonBuilder.append(",");
                        jsonBuilder.append("\"").append(escapeJson(line)).append("\":\"")
                                   .append(Base64.getEncoder().encodeToString(content.getBytes(StandardCharsets.UTF_8)))
                                   .append("\"");
                        first = false;
                    }
                } catch (Exception e) {
                    LOG.warn("Failed to pull AI config file: " + line, e);
                }
            }

            jsonBuilder.append("}");
            return SyncResult.success("Pulled AI config files", jsonBuilder.toString());
        } catch (Exception e) {
            LOG.error("Pull AI config files failed", e);
            return SyncResult.failure(formatError("Pull AI configs", e), e);
        }
    }

    @NotNull
    private String buildAIConfigFilePath(@NotNull GitHubSyncConfig config, @NotNull String projectIdentifier,
                                         @NotNull String relativePath) {
        String basePath = config.getBasePath();
        if (basePath.endsWith("/")) basePath = basePath.substring(0, basePath.length() - 1);
        return String.format("%s/%s/ai-configs/%s", basePath, projectIdentifier, relativePath);
    }

    @NotNull
    private String buildAIConfigManifestPath(@NotNull GitHubSyncConfig config, @NotNull String projectIdentifier) {
        String basePath = config.getBasePath();
        if (basePath.endsWith("/")) basePath = basePath.substring(0, basePath.length() - 1);
        return String.format("%s/%s/ai-config-manifest.txt", basePath, projectIdentifier);
    }

    @NotNull
    private SyncResult pushFileWithMessage(@NotNull GitHubSyncConfig config, @NotNull String filePath,
                                           @NotNull String content, String sha, @NotNull String commitMessage) {
        try {
            String apiUrl = buildApiUrl(config, filePath);
            HttpURLConnection conn = createConnection(apiUrl, "PUT", config.getToken());

            String base64Content = Base64.getEncoder().encodeToString(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder json = new StringBuilder();
            json.append("{");
            json.append("\"message\":\"").append(escapeJson(commitMessage)).append("\",");
            json.append("\"content\":\"").append(base64Content).append("\",");
            json.append("\"branch\":\"").append(escapeJson(config.getBranch())).append("\"");
            if (sha != null) {
                json.append(",\"sha\":\"").append(escapeJson(sha)).append("\"");
            }
            json.append("}");

            try (OutputStream os = conn.getOutputStream()) {
                os.write(json.toString().getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == 200 || responseCode == 201) {
                conn.disconnect();
                return SyncResult.success("File pushed successfully");
            } else {
                String error = readError(conn);
                conn.disconnect();
                return SyncResult.failure(error);
            }
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            return SyncResult.failure("[Local] " + msg, e);
        }
    }

    @NotNull
    private java.util.Set<String> readRemoteManifest(@NotNull GitHubSyncConfig config,
                                                      @NotNull String projectIdentifier) {
        java.util.Set<String> paths = new java.util.LinkedHashSet<>();
        try {
            String manifestPath = buildAIConfigManifestPath(config, projectIdentifier);
            String content = getFileContent(config, manifestPath);
            if (content != null && !content.trim().isEmpty()) {
                for (String line : content.trim().split("\n")) {
                    String trimmed = line.trim();
                    if (!trimmed.isEmpty()) paths.add(trimmed);
                }
            }
        } catch (Exception e) {
            LOG.debug("Failed to read remote manifest", e);
        }
        return paths;
    }

    private boolean deleteRemoteFile(@NotNull GitHubSyncConfig config, @NotNull String filePath,
                                     @NotNull String commitMessage) {
        try {
            String sha = getFileSha(config, filePath);
            if (sha == null) return false;

            String apiUrl = buildApiUrl(config, filePath);
            HttpURLConnection conn = createConnection(apiUrl, "DELETE", config.getToken());

            StringBuilder json = new StringBuilder();
            json.append("{");
            json.append("\"message\":\"").append(escapeJson(commitMessage)).append("\",");
            json.append("\"sha\":\"").append(escapeJson(sha)).append("\",");
            json.append("\"branch\":\"").append(escapeJson(config.getBranch())).append("\"");
            json.append("}");

            try (OutputStream os = conn.getOutputStream()) {
                os.write(json.toString().getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = conn.getResponseCode();
            conn.disconnect();
            if (responseCode == 200) {
                LOG.info("Deleted remote file: " + filePath);
                return true;
            }
            LOG.warn("Failed to delete remote file " + filePath + ": HTTP " + responseCode);
            return false;
        } catch (Exception e) {
            LOG.warn("Failed to delete remote file: " + filePath, e);
            return false;
        }
    }

    @NotNull
    private String buildAIConfigRegistryPath(@NotNull GitHubSyncConfig config, @NotNull String projectIdentifier) {
        String basePath = config.getBasePath();
        if (basePath.endsWith("/")) basePath = basePath.substring(0, basePath.length() - 1);
        return String.format("%s/%s/ai-config-registry.json", basePath, projectIdentifier);
    }

    @Override
    @NotNull
    public SyncResult pushMetadata(@NotNull SyncConfig config, @NotNull String projectIdentifier,
                                   @NotNull String metadataJson) {
        if (!(config instanceof GitHubSyncConfig)) {
            return SyncResult.failure("Invalid config type");
        }
        GitHubSyncConfig ghConfig = (GitHubSyncConfig) config;
        try {
            String registryPath = buildAIConfigRegistryPath(ghConfig, projectIdentifier);
            String sha = getFileSha(ghConfig, registryPath);
            return pushFileWithMessage(ghConfig, registryPath, metadataJson, sha, "Update AI config registry");
        } catch (Exception e) {
            LOG.warn("Failed to push AI config metadata", e);
            return SyncResult.failure("Failed to push metadata", e);
        }
    }

    @Override
    @NotNull
    public SyncResult pullMetadata(@NotNull SyncConfig config, @NotNull String projectIdentifier) {
        if (!(config instanceof GitHubSyncConfig)) {
            return SyncResult.failure("Invalid config type");
        }
        GitHubSyncConfig ghConfig = (GitHubSyncConfig) config;
        try {
            String registryPath = buildAIConfigRegistryPath(ghConfig, projectIdentifier);
            String content = getFileContent(ghConfig, registryPath);
            if (content == null) {
                return SyncResult.success("No metadata on remote");
            }
            return SyncResult.success("Metadata pulled", content);
        } catch (Exception e) {
            LOG.warn("Failed to pull AI config metadata", e);
            return SyncResult.failure("Failed to pull metadata", e);
        }
    }

    private void pushAIConfigManifest(@NotNull GitHubSyncConfig config, @NotNull String projectIdentifier,
                                      @NotNull java.util.Set<String> filePaths) {
        try {
            String manifestPath = buildAIConfigManifestPath(config, projectIdentifier);
            String manifestContent = String.join("\n", filePaths);
            String sha = getFileSha(config, manifestPath);
            pushFileWithMessage(config, manifestPath, manifestContent, sha, "Update AI config manifest");
        } catch (Exception e) {
            LOG.warn("Failed to push AI config manifest", e);
        }
    }

    /**
     * 推送MD5文件到GitHub
     */
    private void pushMD5File(@NotNull GitHubSyncConfig config, @NotNull String md5FilePath, 
                            @NotNull String md5Value, String sha) {
        try {
            String apiUrl = buildApiUrl(config, md5FilePath);
            HttpURLConnection conn = createConnection(apiUrl, "PUT", config.getToken());
            
            // 构建请求体
            String base64Content = Base64.getEncoder().encodeToString(md5Value.getBytes(StandardCharsets.UTF_8));
            StringBuilder json = new StringBuilder();
            json.append("{");
            json.append("\"message\":\"Update MD5 checksum\",");
            json.append("\"content\":\"").append(base64Content).append("\",");
            json.append("\"branch\":\"").append(escapeJson(config.getBranch())).append("\"");
            if (sha != null) {
                json.append(",\"sha\":\"").append(escapeJson(sha)).append("\"");
            }
            json.append("}");
            
            // 发送请求
            try (OutputStream os = conn.getOutputStream()) {
                os.write(json.toString().getBytes(StandardCharsets.UTF_8));
            }
            
            int responseCode = conn.getResponseCode();
            conn.disconnect();
            
            if (responseCode == 200 || responseCode == 201) {
                LOG.info("MD5 file pushed successfully");
            } else {
                LOG.warn("Failed to push MD5 file: HTTP " + responseCode);
            }
            
        } catch (Exception e) {
            LOG.warn("Failed to push MD5 file", e);
        }
    }
}


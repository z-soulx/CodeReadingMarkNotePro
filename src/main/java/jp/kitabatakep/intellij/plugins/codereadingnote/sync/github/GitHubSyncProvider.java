package jp.kitabatakep.intellij.plugins.codereadingnote.sync.github;

import com.intellij.openapi.project.Project;
import jp.kitabatakep.intellij.plugins.codereadingnote.CodeReadingNoteBundle;
import jp.kitabatakep.intellij.plugins.codereadingnote.sync.AbstractSyncProvider;
import jp.kitabatakep.intellij.plugins.codereadingnote.sync.SyncConfig;
import jp.kitabatakep.intellij.plugins.codereadingnote.sync.SyncProviderType;
import jp.kitabatakep.intellij.plugins.codereadingnote.sync.SyncResult;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.net.HttpURLConnection;
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
                return SyncResult.failure("Push failed: " + error);
            }
            
        } catch (Exception e) {
            LOG.error("Push file failed", e);
            return SyncResult.failure(formatError("Push file", e), e);
        }
    }
    
    /**
     * 构建GitHub API URL
     */
    @NotNull
    private String buildApiUrl(@NotNull GitHubSyncConfig config, @NotNull String filePath) {
        return String.format("%s/repos/%s/contents/%s", 
            GITHUB_API_BASE, config.getRepository(), filePath);
    }
    
    /**
     * 创建HTTP连接
     */
    @NotNull
    @SuppressWarnings("deprecation")
    private HttpURLConnection createConnection(@NotNull String urlStr, @NotNull String method, 
                                               @NotNull String token) throws IOException {
        URL url = new URL(urlStr);
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
        
        if ("PUT".equals(method) || "POST".equals(method)) {
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
     * 读取错误信息
     */
    @NotNull
    private String readError(@NotNull HttpURLConnection conn) {
        try {
            InputStream errorStream = conn.getErrorStream();
            if (errorStream != null) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(errorStream, StandardCharsets.UTF_8))) {
                    StringBuilder error = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        error.append(line);
                    }
                    return error.toString();
                }
            }
            return "HTTP " + conn.getResponseCode();
        } catch (Exception e) {
            LOG.debug("Failed to read error stream", e);
            return "Unknown error";
        }
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


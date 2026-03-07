package jp.kitabatakep.intellij.plugins.codereadingnote.aiconfig;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import jp.kitabatakep.intellij.plugins.codereadingnote.sync.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Bridges AI config file sync with the existing SyncService infrastructure.
 * Provides independent manual push/pull for AI config files via the AI Workspace panel.
 */
@Service(Service.Level.PROJECT)
public final class AIConfigSyncAdapter {

    private static final Logger LOG = Logger.getInstance(AIConfigSyncAdapter.class);

    private final Project project;

    public AIConfigSyncAdapter(@NotNull Project project) {
        this.project = project;
    }

    @NotNull
    public static AIConfigSyncAdapter getInstance(@NotNull Project project) {
        return project.getService(AIConfigSyncAdapter.class);
    }

    /**
     * Push tracked AI config files to remote.
     * Includes per-file MD5 change detection: only pushes files whose content actually changed.
     * When no files are tracked, pushes an empty manifest to clear the remote.
     *
     * @param forceAll if true, bypass per-file MD5 comparison and push all files
     */
    @NotNull
    public SyncResult pushAIConfigs(@NotNull SyncConfig config, @NotNull String projectIdentifier, boolean forceAll) {
        AIConfigService aiService = AIConfigService.getInstance(project);
        AIConfigRegistry registry = aiService.getRegistry();

        Map<String, byte[]> trackedFiles = registry.collectTrackedFilesContent();

        // Combined hash check (skip entire push if nothing changed at all, unless forcing)
        // Include tracked empty dirs in hash so checking/unchecking them triggers a push
        String baseHash = registry.computeTrackedContentHash();
        Set<String> emptyDirSet = aiService.getTrackedEmptyDirs();
        String currentHash = baseHash + "|EDIRS:" + emptyDirSet.stream().sorted()
                .reduce("", (a, b) -> a + "," + b);
        String lastPushedHash = aiService.getLastPushedHash();
        if (!forceAll && !currentHash.isEmpty() && currentHash.equals(lastPushedHash)) {
            LOG.info("AI config hash unchanged, skipping push");
            return SyncResult.success("ai.config.push.no.changes");
        }

        SyncProvider provider = SyncProviderFactory.getProvider(config);
        if (provider == null) {
            return SyncResult.failure("Unsupported sync provider: " + config.getProviderType());
        }

        Set<String> emptyDirs = findEmptyTrackedDirs(registry);
        Map<String, String> lastFileHashes = forceAll ? Collections.emptyMap() : aiService.getLastPushedFileHashes();

        LOG.info("Pushing " + trackedFiles.size() + " AI config file(s), " + emptyDirs.size() + " empty dir(s)"
                 + (forceAll ? " [FORCE]" : ""));
        SyncResult result = provider.pushFiles(project, config, trackedFiles, projectIdentifier,
                                               emptyDirs, lastFileHashes, forceAll);

        // Parse the detailed report from result data
        FilePushReport report = null;
        String reportData = result.getData();
        if (reportData != null && reportData.startsWith("{")) {
            try {
                report = FilePushReport.fromJson(reportData);
            } catch (Exception e) {
                LOG.warn("Failed to parse push report", e);
            }
        }

        // Selective hash recording: only record hashes for pushed + skipped files, not failed
        Map<String, String> newFileHashes = new HashMap<>(aiService.getLastPushedFileHashes());

        if (report != null) {
            // Pushed files: update their hashes to current content hash
            for (String pushed : report.getPushedFiles()) {
                AIConfigEntry entry = registry.findByPath(pushed);
                if (entry != null && !entry.getContentHash().isEmpty()) {
                    newFileHashes.put(pushed, entry.getContentHash());
                }
            }
            // Skipped files: hashes already correct in the map, no change needed

            // Failed files: remove from hash map so they'll be retried next push
            for (String failedPath : report.getFailedFiles().keySet()) {
                newFileHashes.remove(failedPath);
            }

            // Deleted files: remove from hash map
            for (String deleted : report.getDeletedFiles()) {
                newFileHashes.remove(deleted);
            }

            aiService.setLastPushedFileHashes(newFileHashes);

            // Only update combined hash if there are no failures
            if (!report.hasFailures()) {
                aiService.setLastPushedHash(currentHash);
            }
        } else if (result.isSuccess()) {
            // Fallback: no report available, use legacy behavior
            aiService.setLastPushedHash(currentHash);
            Map<String, String> fileHashes = new HashMap<>();
            for (AIConfigEntry entry : registry.getTrackedEntries()) {
                if (!entry.getContentHash().isEmpty()) {
                    fileHashes.put(entry.getRelativePath(), entry.getContentHash());
                }
            }
            aiService.setLastPushedFileHashes(fileHashes);
        }

        // Best-effort: push workspace metadata for cross-machine state sharing
        if (result.isSuccess() || (report != null && !report.getPushedFiles().isEmpty())) {
            pushWorkspaceMetadata(provider, config, projectIdentifier, aiService);
        }

        return result;
    }

    /** Convenience overload for non-force push. */
    @NotNull
    public SyncResult pushAIConfigs(@NotNull SyncConfig config, @NotNull String projectIdentifier) {
        return pushAIConfigs(config, projectIdentifier, false);
    }

    // ========== Merge-aware pull (two-phase) ==========

    /**
     * Phase A: fetch remote files and parse them into a map without writing to disk.
     * Returns null on failure (caller should check the SyncResult stored in {@link FetchResult}).
     */
    @NotNull
    public FetchResult fetchRemoteFiles(@NotNull SyncConfig config, @NotNull String projectIdentifier) {
        SyncProvider provider = SyncProviderFactory.getProvider(config);
        if (provider == null) {
            return new FetchResult(SyncResult.failure("Unsupported sync provider: " + config.getProviderType()));
        }

        SyncResult result = provider.pullFiles(project, config, projectIdentifier);
        if (!result.isSuccess()) {
            return new FetchResult(result);
        }

        String data = result.getData();
        if (data == null || data.equals("{}")) {
            return new FetchResult(new LinkedHashMap<>(), provider);
        }

        Map<String, byte[]> remoteFiles = new LinkedHashMap<>();
        try {
            Pattern entryPattern = Pattern.compile("\"([^\"]+)\"\\s*:\\s*\"([^\"]*)\"");
            Matcher matcher = entryPattern.matcher(data);
            String basePath = project.getBasePath();

            while (matcher.find()) {
                String relativePath = unescapeJson(matcher.group(1));
                String base64Content = matcher.group(2);

                if (relativePath.endsWith("/") && base64Content.isEmpty()) {
                    if (basePath != null) {
                        File dir = new File(basePath, relativePath);
                        if (!dir.exists()) {
                            dir.mkdirs();
                        }
                    }
                    continue;
                }

                byte[] content = Base64.getDecoder().decode(base64Content);
                remoteFiles.put(relativePath, content);
            }
        } catch (Exception e) {
            LOG.error("Failed to parse pulled AI config data", e);
            return new FetchResult(SyncResult.failure("Failed to parse AI config data: " + e.getMessage()));
        }

        return new FetchResult(remoteFiles, provider);
    }

    /**
     * Phase B: apply user-selected merge decisions to disk, then reconcile metadata.
     */
    @NotNull
    public SyncResult applyMergeDecisions(
            @NotNull List<AIConfigMergeItem> mergeItems,
            @NotNull SyncConfig config,
            @NotNull String projectIdentifier,
            @NotNull SyncProvider provider) {

        String basePath = project.getBasePath();
        if (basePath == null) {
            return SyncResult.failure("Project base path is null");
        }

        int written = 0;
        int deleted = 0;
        int skipped = 0;
        int failed = 0;

        for (AIConfigMergeItem item : mergeItems) {
            AIConfigMergeItem.Action action = item.getUserAction();
            String relativePath = item.getRelativePath();

            switch (action) {
                case TAKE_REMOTE:
                case ADD: {
                    byte[] content = item.getRemoteContent();
                    if (content == null) { skipped++; break; }
                    try {
                        File targetFile = new File(basePath, relativePath);
                        File parent = targetFile.getParentFile();
                        if (parent != null && !parent.exists()) {
                            parent.mkdirs();
                        }
                        Files.write(targetFile.toPath(), content);
                        written++;
                    } catch (Exception e) {
                        failed++;
                        LOG.warn("Failed to write AI config file: " + relativePath, e);
                    }
                    break;
                }
                case DELETE: {
                    try {
                        File targetFile = new File(basePath, relativePath);
                        if (targetFile.exists()) {
                            java.nio.file.Files.deleteIfExists(targetFile.toPath());
                            deleted++;
                        }
                    } catch (Exception e) {
                        failed++;
                        LOG.warn("Failed to delete AI config file: " + relativePath, e);
                    }
                    break;
                }
                case KEEP_LOCAL:
                case SKIP:
                default:
                    skipped++;
                    break;
            }
        }

        AIConfigService aiService = AIConfigService.getInstance(project);

        pullAndApplyMetadataPreRescan(provider, config, projectIdentifier, aiService);
        aiService.rescan();
        pullAndApplyMetadataPostRescan(provider, config, projectIdentifier, aiService);
        updateHashesAfterPull(aiService);

        String msg = "Pulled: " + written + " written, " + deleted + " deleted, "
                + skipped + " skipped" + (failed > 0 ? ", " + failed + " failed" : "");
        return failed == 0 ? SyncResult.success(msg) : SyncResult.success(msg);
    }

    /**
     * Legacy pull that overwrites everything (kept for backward compatibility).
     */
    @NotNull
    public SyncResult pullAIConfigs(@NotNull SyncConfig config, @NotNull String projectIdentifier) {
        SyncProvider provider = SyncProviderFactory.getProvider(config);
        if (provider == null) {
            return SyncResult.failure("Unsupported sync provider: " + config.getProviderType());
        }

        SyncResult result = provider.pullFiles(project, config, projectIdentifier);
        if (!result.isSuccess()) {
            return result;
        }

        String data = result.getData();
        if (data == null || data.equals("{}")) {
            return SyncResult.success("No AI config files on remote");
        }

        String basePath = project.getBasePath();
        if (basePath == null) {
            return SyncResult.failure("Project base path is null");
        }

        int written = 0;
        int failed = 0;

        try {
            Pattern entryPattern = Pattern.compile("\"([^\"]+)\"\\s*:\\s*\"([^\"]*)\"");
            Matcher matcher = entryPattern.matcher(data);

            while (matcher.find()) {
                String relativePath = unescapeJson(matcher.group(1));
                String base64Content = matcher.group(2);

                if (relativePath.endsWith("/") && base64Content.isEmpty()) {
                    File dir = new File(basePath, relativePath);
                    if (!dir.exists()) {
                        dir.mkdirs();
                    }
                    continue;
                }

                try {
                    byte[] content = Base64.getDecoder().decode(base64Content);
                    File targetFile = new File(basePath, relativePath);

                    File parent = targetFile.getParentFile();
                    if (parent != null && !parent.exists()) {
                        parent.mkdirs();
                    }

                    Files.write(targetFile.toPath(), content);
                    written++;
                } catch (Exception e) {
                    failed++;
                    LOG.warn("Failed to write AI config file: " + relativePath, e);
                }
            }
        } catch (Exception e) {
            LOG.error("Failed to parse pulled AI config data", e);
            return SyncResult.failure("Failed to parse AI config data: " + e.getMessage());
        }

        AIConfigService aiService = AIConfigService.getInstance(project);

        pullAndApplyMetadataPreRescan(provider, config, projectIdentifier, aiService);
        aiService.rescan();
        pullAndApplyMetadataPostRescan(provider, config, projectIdentifier, aiService);
        updateHashesAfterPull(aiService);

        if (failed == 0) {
            return SyncResult.success("Pulled " + written + " AI config file(s)");
        } else {
            return SyncResult.success("Pulled " + written + " file(s), " + failed + " failed");
        }
    }

    private void updateHashesAfterPull(@NotNull AIConfigService aiService) {
        AIConfigRegistry updatedRegistry = aiService.getRegistry();
        String baseHash = updatedRegistry.computeTrackedContentHash();
        Set<String> emptyDirSet = aiService.getTrackedEmptyDirs();
        String newHash = baseHash + "|EDIRS:" + emptyDirSet.stream().sorted()
                .reduce("", (a, b) -> a + "," + b);
        aiService.setLastPushedHash(newHash);
        Map<String, String> fileHashes = new HashMap<>();
        for (AIConfigEntry entry : updatedRegistry.getTrackedEntries()) {
            if (!entry.getContentHash().isEmpty()) {
                fileHashes.put(entry.getRelativePath(), entry.getContentHash());
            }
        }
        aiService.setLastPushedFileHashes(fileHashes);
    }

    /**
     * Result of fetching remote files without writing them.
     */
    public static class FetchResult {
        private final SyncResult errorResult;
        private final Map<String, byte[]> remoteFiles;
        private final SyncProvider provider;

        FetchResult(@NotNull SyncResult errorResult) {
            this.errorResult = errorResult;
            this.remoteFiles = null;
            this.provider = null;
        }

        FetchResult(@NotNull Map<String, byte[]> remoteFiles, @NotNull SyncProvider provider) {
            this.errorResult = null;
            this.remoteFiles = remoteFiles;
            this.provider = provider;
        }

        public boolean isSuccess() {
            return remoteFiles != null;
        }

        @Nullable
        public SyncResult getErrorResult() {
            return errorResult;
        }

        @NotNull
        public Map<String, byte[]> getRemoteFiles() {
            return remoteFiles != null ? remoteFiles : Collections.emptyMap();
        }

        @Nullable
        public SyncProvider getProvider() {
            return provider;
        }
    }

    // ========== Workspace metadata sync ==========

    private void pushWorkspaceMetadata(@NotNull SyncProvider provider, @NotNull SyncConfig config,
                                       @NotNull String projectIdentifier, @NotNull AIConfigService aiService) {
        try {
            String metadataJson = serializeMetadata(aiService);
            SyncResult metaResult = provider.pushMetadata(config, projectIdentifier, metadataJson);
            if (!metaResult.isSuccess()) {
                LOG.warn("Failed to push workspace metadata: " + metaResult.getMessage());
            }
        } catch (Exception e) {
            LOG.warn("Failed to push workspace metadata", e);
        }
    }

    /** Cached metadata JSON from remote, used across the two-phase apply. */
    private String cachedRemoteMetadataJson;

    private void pullAndApplyMetadataPreRescan(@NotNull SyncProvider provider, @NotNull SyncConfig config,
                                                @NotNull String projectIdentifier, @NotNull AIConfigService aiService) {
        cachedRemoteMetadataJson = null;
        try {
            SyncResult metaResult = provider.pullMetadata(config, projectIdentifier);
            if (metaResult.isSuccess() && metaResult.getData() != null) {
                cachedRemoteMetadataJson = metaResult.getData();
                applyMetadataPreRescan(cachedRemoteMetadataJson, aiService);
            }
        } catch (Exception e) {
            LOG.warn("Failed to pull workspace metadata (pre-rescan)", e);
        }
    }

    private void pullAndApplyMetadataPostRescan(@NotNull SyncProvider provider, @NotNull SyncConfig config,
                                                 @NotNull String projectIdentifier, @NotNull AIConfigService aiService) {
        if (cachedRemoteMetadataJson == null) return;
        try {
            applyMetadataPostRescan(cachedRemoteMetadataJson, aiService);
        } catch (Exception e) {
            LOG.warn("Failed to apply workspace metadata (post-rescan)", e);
        } finally {
            cachedRemoteMetadataJson = null;
        }
    }

    @NotNull
    private String serializeMetadata(@NotNull AIConfigService aiService) {
        AIConfigRegistry registry = aiService.getRegistry();
        StringBuilder sb = new StringBuilder("{");

        // customPaths
        sb.append("\"customPaths\":[");
        boolean first = true;
        for (String p : registry.getCustomPaths()) {
            if (!first) sb.append(",");
            sb.append("\"").append(escapeJson(p)).append("\"");
            first = false;
        }
        sb.append("],");

        // ignorePatterns
        sb.append("\"ignorePatterns\":[");
        first = true;
        for (String p : registry.getUserIgnorePatterns()) {
            if (!first) sb.append(",");
            sb.append("\"").append(escapeJson(p)).append("\"");
            first = false;
        }
        sb.append("],");

        // trackedEntries
        sb.append("\"trackedEntries\":[");
        first = true;
        for (AIConfigEntry entry : registry.getEntries()) {
            if (!first) sb.append(",");
            sb.append("{\"relativePath\":\"").append(escapeJson(entry.getRelativePath())).append("\"")
              .append(",\"tracked\":").append(entry.isTracked())
              .append(",\"typeName\":\"").append(escapeJson(entry.getType().name())).append("\"}");
            first = false;
        }
        sb.append("],");

        // lastPushedFileHashes
        sb.append("\"lastPushedFileHashes\":[");
        first = true;
        for (Map.Entry<String, String> e : aiService.getLastPushedFileHashes().entrySet()) {
            if (!first) sb.append(",");
            sb.append("{\"relativePath\":\"").append(escapeJson(e.getKey())).append("\"")
              .append(",\"contentHash\":\"").append(escapeJson(e.getValue())).append("\"}");
            first = false;
        }
        sb.append("],");

        // trackedEmptyDirs
        sb.append("\"trackedEmptyDirs\":[");
        first = true;
        for (String dir : aiService.getTrackedEmptyDirs()) {
            if (!first) sb.append(",");
            sb.append("\"").append(escapeJson(dir)).append("\"");
            first = false;
        }
        sb.append("]}");

        return sb.toString();
    }

    /**
     * Phase 1 (before rescan): apply customPaths and ignorePatterns so rescan
     * discovers the correct set of files.
     */
    private void applyMetadataPreRescan(@NotNull String metadataJson, @NotNull AIConfigService aiService) {
        AIConfigRegistry registry = aiService.getRegistry();

        List<String> remoteCustomPaths = parseJsonStringList(metadataJson, "customPaths");
        if (!remoteCustomPaths.isEmpty()) {
            Set<String> merged = new LinkedHashSet<>(registry.getCustomPaths());
            merged.addAll(remoteCustomPaths);
            registry.setCustomPaths(merged);
        }

        List<String> remoteIgnorePatterns = parseJsonStringList(metadataJson, "ignorePatterns");
        if (!remoteIgnorePatterns.isEmpty()) {
            Set<String> merged = new LinkedHashSet<>(registry.getUserIgnorePatterns());
            merged.addAll(remoteIgnorePatterns);
            registry.setUserIgnorePatterns(new ArrayList<>(merged));
        }

        LOG.info("Applied remote metadata (pre-rescan): " + remoteCustomPaths.size() + " custom paths, "
                 + remoteIgnorePatterns.size() + " ignore patterns");
    }

    /**
     * Phase 2 (after rescan): apply trackedEntries and file hashes to the
     * newly created entries so they reflect the remote state.
     */
    private void applyMetadataPostRescan(@NotNull String metadataJson, @NotNull AIConfigService aiService) {
        List<TrackedEntryMeta> remoteTracked = parseTrackedEntries(metadataJson);
        aiService.applyRemoteTrackedState(remoteTracked);

        Map<String, String> remoteHashes = parseFileHashes(metadataJson);
        if (!remoteHashes.isEmpty()) {
            aiService.setLastPushedFileHashes(remoteHashes);
        }

        List<String> remoteEmptyDirs = parseJsonStringList(metadataJson, "trackedEmptyDirs");
        if (!remoteEmptyDirs.isEmpty()) {
            aiService.setTrackedEmptyDirs(new LinkedHashSet<>(remoteEmptyDirs));
        }

        LOG.info("Applied remote metadata (post-rescan): " + remoteTracked.size() + " tracked entries, "
                 + remoteHashes.size() + " file hashes, " + remoteEmptyDirs.size() + " tracked empty dirs");
    }

    // ========== Metadata JSON parsing helpers ==========

    @NotNull
    private List<String> parseJsonStringList(@NotNull String json, @NotNull String key) {
        List<String> result = new ArrayList<>();
        String prefix = "\"" + key + "\":[";
        int start = json.indexOf(prefix);
        if (start < 0) return result;
        start += prefix.length();
        int end = json.indexOf("]", start);
        if (end <= start) return result;
        String content = json.substring(start, end);
        if (content.isEmpty()) return result;

        int i = 0;
        while (i < content.length()) {
            int qStart = content.indexOf('"', i);
            if (qStart < 0) break;
            int qEnd = findClosingQuote(content, qStart + 1);
            if (qEnd < 0) break;
            result.add(unescapeJson(content.substring(qStart + 1, qEnd)));
            i = qEnd + 1;
        }
        return result;
    }

    @NotNull
    private List<TrackedEntryMeta> parseTrackedEntries(@NotNull String json) {
        List<TrackedEntryMeta> result = new ArrayList<>();
        String prefix = "\"trackedEntries\":[";
        int start = json.indexOf(prefix);
        if (start < 0) return result;
        start += prefix.length();

        // Find matching ]
        int depth = 1;
        int end = start;
        while (end < json.length() && depth > 0) {
            char c = json.charAt(end);
            if (c == '[') depth++;
            else if (c == ']') depth--;
            if (depth > 0) end++;
        }
        String content = json.substring(start, end);

        // Parse each {...} object
        int i = 0;
        while (i < content.length()) {
            int objStart = content.indexOf('{', i);
            if (objStart < 0) break;
            int objEnd = content.indexOf('}', objStart);
            if (objEnd < 0) break;
            String obj = content.substring(objStart, objEnd + 1);
            String path = extractJsonStringValue(obj, "relativePath");
            boolean tracked = obj.contains("\"tracked\":true");
            String typeName = extractJsonStringValue(obj, "typeName");
            if (path != null) {
                result.add(new TrackedEntryMeta(path, tracked, typeName != null ? typeName : "CUSTOM"));
            }
            i = objEnd + 1;
        }
        return result;
    }

    @NotNull
    private Map<String, String> parseFileHashes(@NotNull String json) {
        Map<String, String> result = new LinkedHashMap<>();
        String prefix = "\"lastPushedFileHashes\":[";
        int start = json.indexOf(prefix);
        if (start < 0) return result;
        start += prefix.length();

        int depth = 1;
        int end = start;
        while (end < json.length() && depth > 0) {
            char c = json.charAt(end);
            if (c == '[') depth++;
            else if (c == ']') depth--;
            if (depth > 0) end++;
        }
        String content = json.substring(start, end);

        int i = 0;
        while (i < content.length()) {
            int objStart = content.indexOf('{', i);
            if (objStart < 0) break;
            int objEnd = content.indexOf('}', objStart);
            if (objEnd < 0) break;
            String obj = content.substring(objStart, objEnd + 1);
            String path = extractJsonStringValue(obj, "relativePath");
            String hash = extractJsonStringValue(obj, "contentHash");
            if (path != null && hash != null) {
                result.put(path, hash);
            }
            i = objEnd + 1;
        }
        return result;
    }

    private String extractJsonStringValue(@NotNull String json, @NotNull String key) {
        String prefix = "\"" + key + "\":\"";
        int start = json.indexOf(prefix);
        if (start < 0) return null;
        start += prefix.length();
        int end = findClosingQuote(json, start);
        if (end < 0) return null;
        return unescapeJson(json.substring(start, end));
    }

    private int findClosingQuote(@NotNull String s, int from) {
        for (int i = from; i < s.length(); i++) {
            if (s.charAt(i) == '\\') { i++; continue; }
            if (s.charAt(i) == '"') return i;
        }
        return -1;
    }

    // ========== Utility ==========

    /**
     * Returns the set of empty directories the user has explicitly checked for sync.
     * These are stored in AIConfigService persistent state.
     */
    @NotNull
    private Set<String> findEmptyTrackedDirs(@NotNull AIConfigRegistry registry) {
        AIConfigService aiService = AIConfigService.getInstance(project);
        Set<String> persisted = aiService.getTrackedEmptyDirs();
        // Only include dirs that still exist in the discovered dirs (not stale)
        Set<String> discoveredDirs = registry.getDiscoveredDirs();
        Set<String> result = new LinkedHashSet<>();
        for (String dir : persisted) {
            if (discoveredDirs.contains(dir)) {
                result.add(dir);
            }
        }
        return result;
    }

    @NotNull
    private String escapeJson(@NotNull String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    @NotNull
    private String unescapeJson(@NotNull String str) {
        return str.replace("\\\\", "\\")
                  .replace("\\\"", "\"")
                  .replace("\\n", "\n")
                  .replace("\\r", "\r")
                  .replace("\\t", "\t");
    }

    /**
     * Lightweight data holder for tracked entry metadata from remote.
     */
    public static class TrackedEntryMeta {
        public final String relativePath;
        public final boolean tracked;
        public final String typeName;

        public TrackedEntryMeta(@NotNull String relativePath, boolean tracked, @NotNull String typeName) {
            this.relativePath = relativePath;
            this.tracked = tracked;
            this.typeName = typeName;
        }
    }
}

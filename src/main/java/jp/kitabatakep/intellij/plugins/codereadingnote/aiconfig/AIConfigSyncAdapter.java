package jp.kitabatakep.intellij.plugins.codereadingnote.aiconfig;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import jp.kitabatakep.intellij.plugins.codereadingnote.sync.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.*;

/**
 * Bridges AI config file sync with the existing SyncService infrastructure.
 * Provides independent manual push/pull for AI config files via the AI Workspace panel.
 */
@Service(Service.Level.PROJECT)
public final class AIConfigSyncAdapter {

    private static final Logger LOG = Logger.getInstance(AIConfigSyncAdapter.class);
    private static final Gson GSON = new Gson();

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

        FilePushReport report = null;
        String reportData = result.getData();
        if (reportData != null && reportData.startsWith("{")) {
            try {
                report = FilePushReport.fromJson(reportData);
            } catch (Exception e) {
                LOG.warn("Failed to parse push report", e);
            }
        }

        Map<String, String> newFileHashes = new HashMap<>(aiService.getLastPushedFileHashes());

        if (report != null) {
            for (String pushed : report.getPushedFiles()) {
                AIConfigEntry entry = registry.findByPath(pushed);
                if (entry != null && !entry.getContentHash().isEmpty()) {
                    newFileHashes.put(pushed, entry.getContentHash());
                }
            }
            for (String failedPath : report.getFailedFiles().keySet()) {
                newFileHashes.remove(failedPath);
            }
            for (String deleted : report.getDeletedFiles()) {
                newFileHashes.remove(deleted);
            }
            aiService.setLastPushedFileHashes(newFileHashes);
            if (!report.hasFailures()) {
                aiService.setLastPushedHash(currentHash);
            }
        } else if (result.isSuccess()) {
            aiService.setLastPushedHash(currentHash);
            Map<String, String> fileHashes = new HashMap<>();
            for (AIConfigEntry entry : registry.getTrackedEntries()) {
                if (!entry.getContentHash().isEmpty()) {
                    fileHashes.put(entry.getRelativePath(), entry.getContentHash());
                }
            }
            aiService.setLastPushedFileHashes(fileHashes);
        }

        if (result.isSuccess() || (report != null && !report.getPushedFiles().isEmpty())) {
            pushWorkspaceMetadata(provider, config, projectIdentifier, aiService);
        }

        return result;
    }

    @NotNull
    public SyncResult pushAIConfigs(@NotNull SyncConfig config, @NotNull String projectIdentifier) {
        return pushAIConfigs(config, projectIdentifier, false);
    }

    // ========== Merge-aware pull (two-phase) ==========

    /**
     * Phase A: fetch remote files and parse them into a map without writing to disk.
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
            JsonObject jsonObj = JsonParser.parseString(data).getAsJsonObject();
            String basePath = project.getBasePath();

            for (Map.Entry<String, JsonElement> entry : jsonObj.entrySet()) {
                String relativePath = entry.getKey();
                String base64Content = entry.getValue().getAsString();

                // TODO: directory creation here is a side effect during fetch (before user confirms merge).
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

        int written = 0, deleted = 0, skipped = 0, failed = 0;

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
                            Files.deleteIfExists(targetFile.toPath());
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

        refreshProjectVFS(basePath);
        pullAndApplyMetadata(provider, config, projectIdentifier, aiService);
        updateHashesAfterPull(aiService);

        String msg = "Pulled: " + written + " written, " + deleted + " deleted, "
                + skipped + " skipped" + (failed > 0 ? ", " + failed + " failed" : "");
        return failed > 0 ? SyncResult.failure(msg) : SyncResult.success(msg);
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

        int written = 0, failed = 0;

        try {
            JsonObject jsonObj = JsonParser.parseString(data).getAsJsonObject();

            for (Map.Entry<String, JsonElement> entry : jsonObj.entrySet()) {
                String relativePath = entry.getKey();
                String base64Content = entry.getValue().getAsString();

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

        refreshProjectVFS(basePath);
        pullAndApplyMetadata(provider, config, projectIdentifier, aiService);
        updateHashesAfterPull(aiService);

        if (failed == 0) {
            return SyncResult.success("Pulled " + written + " AI config file(s)");
        } else {
            return SyncResult.failure("Pulled " + written + " file(s), " + failed + " failed");
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

    // ========== FetchResult ==========

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

        public boolean isSuccess() { return remoteFiles != null; }

        @Nullable
        public SyncResult getErrorResult() { return errorResult; }

        @NotNull
        public Map<String, byte[]> getRemoteFiles() {
            return remoteFiles != null ? remoteFiles : Collections.emptyMap();
        }

        @Nullable
        public SyncProvider getProvider() { return provider; }
    }

    // ========== Workspace metadata sync ==========

    private void pushWorkspaceMetadata(@NotNull SyncProvider provider, @NotNull SyncConfig config,
                                       @NotNull String projectIdentifier, @NotNull AIConfigService aiService) {
        try {
            String metadataJson = serializeMetadata(aiService);
            SyncResult metaResult = provider.pushMetadata(config, projectIdentifier, metadataJson);
            if (metaResult.isSuccess()) {
                aiService.setLastSyncedRemoteMetadataHash(computeMD5(metadataJson));
            } else {
                LOG.warn("Failed to push workspace metadata: " + metaResult.getMessage());
            }
        } catch (Exception e) {
            LOG.warn("Failed to push workspace metadata", e);
        }
    }

    /**
     * Unified metadata pull and apply: pre-rescan (customPaths, ignorePatterns),
     * then rescan, then post-rescan (tracked state, empty dirs).
     */
    private void pullAndApplyMetadata(@NotNull SyncProvider provider, @NotNull SyncConfig config,
                                      @NotNull String projectIdentifier, @NotNull AIConfigService aiService) {
        AIConfigMetadata metadata = null;
        String rawJson = null;
        try {
            SyncResult metaResult = provider.pullMetadata(config, projectIdentifier);
            if (metaResult.isSuccess() && metaResult.getData() != null) {
                rawJson = metaResult.getData();
                metadata = GSON.fromJson(rawJson, AIConfigMetadata.class);
            }
        } catch (Exception e) {
            LOG.warn("Failed to pull workspace metadata", e);
        }

        if (metadata == null) return;

        if (rawJson != null) {
            aiService.setLastSyncedRemoteMetadataHash(computeMD5(rawJson));
        }

        applyMetadataPreRescan(metadata, aiService);
        aiService.rescan();
        applyMetadataPostRescan(metadata, aiService);
    }

    @NotNull
    private String serializeMetadata(@NotNull AIConfigService aiService) {
        AIConfigRegistry registry = aiService.getRegistry();

        AIConfigMetadata metadata = new AIConfigMetadata();
        metadata.customPaths = new ArrayList<>(registry.getCustomPaths());
        metadata.ignorePatterns = new ArrayList<>(registry.getUserIgnorePatterns());

        for (AIConfigEntry entry : registry.getEntries()) {
            metadata.trackedEntries.add(new AIConfigMetadata.TrackedEntry(
                    entry.getRelativePath(), entry.isTracked(), entry.getType().name()));
        }

        for (Map.Entry<String, String> e : aiService.getLastPushedFileHashes().entrySet()) {
            metadata.lastPushedFileHashes.add(new AIConfigMetadata.FileHash(e.getKey(), e.getValue()));
        }

        metadata.trackedEmptyDirs = new ArrayList<>(aiService.getTrackedEmptyDirs());

        return GSON.toJson(metadata);
    }

    private void applyMetadataPreRescan(@NotNull AIConfigMetadata metadata, @NotNull AIConfigService aiService) {
        AIConfigRegistry registry = aiService.getRegistry();

        if (metadata.customPaths != null && !metadata.customPaths.isEmpty()) {
            Set<String> merged = new LinkedHashSet<>(registry.getCustomPaths());
            merged.addAll(metadata.customPaths);
            registry.setCustomPaths(merged);
        }

        if (metadata.ignorePatterns != null && !metadata.ignorePatterns.isEmpty()) {
            Set<String> merged = new LinkedHashSet<>(registry.getUserIgnorePatterns());
            merged.addAll(metadata.ignorePatterns);
            registry.setUserIgnorePatterns(new ArrayList<>(merged));
        }

        LOG.info("Applied remote metadata (pre-rescan): "
                + (metadata.customPaths != null ? metadata.customPaths.size() : 0) + " custom paths, "
                + (metadata.ignorePatterns != null ? metadata.ignorePatterns.size() : 0) + " ignore patterns");
    }

    private void applyMetadataPostRescan(@NotNull AIConfigMetadata metadata, @NotNull AIConfigService aiService) {
        List<TrackedEntryMeta> remoteTracked = metadata.toTrackedEntryMetas();
        aiService.applyRemoteTrackedState(remoteTracked);

        if (metadata.trackedEmptyDirs != null && !metadata.trackedEmptyDirs.isEmpty()) {
            aiService.setTrackedEmptyDirs(new LinkedHashSet<>(metadata.trackedEmptyDirs));
        }

        LOG.info("Applied remote metadata (post-rescan): " + remoteTracked.size() + " tracked entries, "
                + (metadata.trackedEmptyDirs != null ? metadata.trackedEmptyDirs.size() : 0) + " tracked empty dirs");
    }

    // ========== Utility ==========

    @NotNull
    private Set<String> findEmptyTrackedDirs(@NotNull AIConfigRegistry registry) {
        AIConfigService aiService = AIConfigService.getInstance(project);
        Set<String> persisted = aiService.getTrackedEmptyDirs();
        Set<String> discoveredDirs = registry.getDiscoveredDirs();
        Set<String> result = new LinkedHashSet<>();
        for (String dir : persisted) {
            if (discoveredDirs.contains(dir)) {
                result.add(dir);
            }
        }
        return result;
    }

    private void refreshProjectVFS(@NotNull String basePath) {
        VirtualFile projectRoot = LocalFileSystem.getInstance().refreshAndFindFileByPath(
                basePath.replace('\\', '/'));
        if (projectRoot != null) {
            projectRoot.refresh(false, true);
        }
    }

    /**
     * Checks if remote metadata has been modified since our last sync.
     * Used by auto-sync to avoid overwriting newer remote content.
     *
     * @return true if remote has changes we haven't synced, false if safe to push
     */
    public boolean checkRemoteConflict(@NotNull SyncConfig config, @NotNull String projectIdentifier) {
        AIConfigService aiService = AIConfigService.getInstance(project);
        String lastKnownHash = aiService.getLastSyncedRemoteMetadataHash();
        if (lastKnownHash.isEmpty()) return false;

        try {
            SyncProvider provider = SyncProviderFactory.getProvider(config);
            SyncResult metaResult = provider.pullMetadata(config, projectIdentifier);
            if (!metaResult.isSuccess() || metaResult.getData() == null) {
                return false;
            }
            String currentRemoteHash = computeMD5(metaResult.getData());
            return !lastKnownHash.equals(currentRemoteHash);
        } catch (Exception e) {
            LOG.warn("Failed to check remote conflict", e);
            return false;
        }
    }

    @NotNull
    private static String computeMD5(@NotNull String data) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : digest) {
                String h = Integer.toHexString(0xff & b);
                if (h.length() == 1) hex.append('0');
                hex.append(h);
            }
            return hex.toString();
        } catch (Exception e) {
            return "";
        }
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

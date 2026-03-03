package jp.kitabatakep.intellij.plugins.codereadingnote.aiconfig;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import jp.kitabatakep.intellij.plugins.codereadingnote.sync.*;
import org.jetbrains.annotations.NotNull;

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
     * Includes change detection: compares local content hash with last-pushed hash to skip no-op pushes.
     * When no files are tracked, pushes an empty manifest to clear the remote.
     */
    @NotNull
    public SyncResult pushAIConfigs(@NotNull SyncConfig config, @NotNull String projectIdentifier) {
        AIConfigService aiService = AIConfigService.getInstance(project);
        AIConfigRegistry registry = aiService.getRegistry();

        Map<String, byte[]> trackedFiles = registry.collectTrackedFilesContent();

        String currentHash = registry.computeTrackedContentHash();
        String lastPushedHash = aiService.getLastPushedHash();
        if (!currentHash.isEmpty() && currentHash.equals(lastPushedHash)) {
            LOG.info("AI config hash unchanged, skipping push");
            return SyncResult.success("ai.config.push.no.changes");
        }

        SyncProvider provider = SyncProviderFactory.getProvider(config);
        if (provider == null) {
            return SyncResult.failure("Unsupported sync provider: " + config.getProviderType());
        }

        // Collect empty dirs within tracked scope for sync
        Set<String> emptyDirs = findEmptyTrackedDirs(registry);

        LOG.info("Pushing " + trackedFiles.size() + " AI config file(s), " + emptyDirs.size() + " empty dir(s)");
        SyncResult result = provider.pushFiles(project, config, trackedFiles, projectIdentifier, emptyDirs);

        if (result.isSuccess()) {
            aiService.setLastPushedHash(currentHash);
            // Store per-file hashes for sync status indicators
            Map<String, String> fileHashes = new HashMap<>();
            for (AIConfigEntry entry : registry.getTrackedEntries()) {
                if (!entry.getContentHash().isEmpty()) {
                    fileHashes.put(entry.getRelativePath(), entry.getContentHash());
                }
            }
            aiService.setLastPushedFileHashes(fileHashes);
        }

        return result;
    }

    /**
     * Pull AI config files from remote and write them to the project.
     * Triggers a rescan after writing files so the UI reflects the new state.
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

                // Directory marker: path ends with '/', content is empty
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
        aiService.rescan();

        // Update hashes to match pulled state, avoiding unnecessary re-push
        AIConfigRegistry updatedRegistry = aiService.getRegistry();
        String newHash = updatedRegistry.computeTrackedContentHash();
        aiService.setLastPushedHash(newHash);
        Map<String, String> fileHashes = new HashMap<>();
        for (AIConfigEntry entry : updatedRegistry.getTrackedEntries()) {
            if (!entry.getContentHash().isEmpty()) {
                fileHashes.put(entry.getRelativePath(), entry.getContentHash());
            }
        }
        aiService.setLastPushedFileHashes(fileHashes);

        if (failed == 0) {
            return SyncResult.success("Pulled " + written + " AI config file(s)");
        } else {
            return SyncResult.success("Pulled " + written + " file(s), " + failed + " failed");
        }
    }

    /**
     * Find directories that exist in scan scope but contain no tracked files.
     */
    @NotNull
    private Set<String> findEmptyTrackedDirs(@NotNull AIConfigRegistry registry) {
        Set<String> allDirs = registry.getDiscoveredDirs();
        Set<String> trackedFileDirs = new HashSet<>();
        for (AIConfigEntry entry : registry.getTrackedEntries()) {
            String parentDir = entry.getParentDir();
            while (!parentDir.isEmpty()) {
                trackedFileDirs.add(parentDir);
                int lastSlash = parentDir.lastIndexOf('/');
                parentDir = lastSlash > 0 ? parentDir.substring(0, lastSlash) : "";
            }
        }
        Set<String> emptyDirs = new LinkedHashSet<>();
        for (String dir : allDirs) {
            if (!trackedFileDirs.contains(dir)) {
                emptyDirs.add(dir);
            }
        }
        return emptyDirs;
    }

    @NotNull
    private String unescapeJson(@NotNull String str) {
        return str.replace("\\\\", "\\")
                  .replace("\\\"", "\"")
                  .replace("\\n", "\n")
                  .replace("\\r", "\r")
                  .replace("\\t", "\t");
    }
}

package jp.kitabatakep.intellij.plugins.codereadingnote.aiconfig;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.security.MessageDigest;
import java.util.*;

/**
 * Analyzes differences between local, remote, and base (last-synced) AI config files
 * to produce per-file merge decisions.
 *
 * Pure logic — no UI or IDE dependencies beyond file I/O.
 */
public final class AIConfigMergeAnalyzer {

    private AIConfigMergeAnalyzer() {
    }

    /**
     * Performs three-way comparison and produces merge items for every affected file.
     *
     * @param remoteFiles  map of relativePath -> content bytes fetched from remote
     * @param registry     local AI config registry (provides local entries and hashes)
     * @param baseHashes   per-file MD5 hashes from the last successful sync (the "base")
     * @param basePath     project base path for reading local file content
     * @return list of merge items sorted by path; UNCHANGED items are included for completeness
     */
    @NotNull
    public static List<AIConfigMergeItem> analyze(
            @NotNull Map<String, byte[]> remoteFiles,
            @NotNull AIConfigRegistry registry,
            @NotNull Map<String, String> baseHashes,
            @NotNull String basePath) {

        List<AIConfigMergeItem> items = new ArrayList<>();
        Set<String> allPaths = new LinkedHashSet<>();

        // Collect all known file paths from remote, local tracked entries, and base
        allPaths.addAll(remoteFiles.keySet());
        for (AIConfigEntry entry : registry.getTrackedEntries()) {
            allPaths.add(entry.getRelativePath());
        }
        allPaths.addAll(baseHashes.keySet());

        for (String path : allPaths) {
            // Skip directory entries (trailing slash) — handled separately
            if (path.endsWith("/")) continue;

            byte[] remoteContent = remoteFiles.get(path);
            String remoteHash = remoteContent != null ? calculateMD5(remoteContent) : null;

            AIConfigEntry localEntry = registry.findByPath(path);
            String localHash = null;
            if (localEntry != null) {
                String hash = localEntry.getContentHash();
                if (hash != null && !hash.isEmpty()) {
                    localHash = hash;
                } else {
                    localHash = computeLocalHash(basePath, path);
                }
            } else {
                // Entry not in registry but file may exist on disk
                String diskHash = computeLocalHash(basePath, path);
                if (diskHash != null) {
                    localHash = diskHash;
                }
            }

            String baseHash = baseHashes.get(path);

            AIConfigMergeItem item = categorize(path, localHash, remoteHash, baseHash, remoteContent);
            items.add(item);
        }

        items.sort(Comparator.comparing(AIConfigMergeItem::getRelativePath));
        return items;
    }

    @NotNull
    private static AIConfigMergeItem categorize(
            @NotNull String path,
            String localHash,
            String remoteHash,
            String baseHash,
            byte[] remoteContent) {

        boolean hasLocal = localHash != null;
        boolean hasRemote = remoteHash != null;
        boolean hasBase = baseHash != null && !baseHash.isEmpty();

        // NEW_REMOTE: remote has file, local doesn't
        if (hasRemote && !hasLocal) {
            return new AIConfigMergeItem(path, AIConfigMergeCategory.NEW_REMOTE,
                    AIConfigMergeItem.Action.ADD,
                    null, remoteHash, baseHash, remoteContent);
        }

        // DELETED_REMOTE: local has file (and it was in base), but remote doesn't
        if (!hasRemote && hasLocal && hasBase) {
            return new AIConfigMergeItem(path, AIConfigMergeCategory.DELETED_REMOTE,
                    AIConfigMergeItem.Action.KEEP_LOCAL,
                    localHash, null, baseHash, null);
        }

        // File only exists locally and never synced — not a merge concern, skip
        if (!hasRemote && hasLocal && !hasBase) {
            return new AIConfigMergeItem(path, AIConfigMergeCategory.LOCAL_CHANGED,
                    AIConfigMergeItem.Action.KEEP_LOCAL,
                    localHash, null, baseHash, null);
        }

        // Both exist — compare hashes
        if (hasRemote && hasLocal) {
            if (remoteHash.equals(localHash)) {
                return new AIConfigMergeItem(path, AIConfigMergeCategory.UNCHANGED,
                        AIConfigMergeItem.Action.SKIP,
                        localHash, remoteHash, baseHash, remoteContent);
            }

            boolean localChanged = hasBase ? !localHash.equals(baseHash) : true;
            boolean remoteChanged = hasBase ? !remoteHash.equals(baseHash) : true;

            if (localChanged && remoteChanged) {
                return new AIConfigMergeItem(path, AIConfigMergeCategory.BOTH_CHANGED,
                        AIConfigMergeItem.Action.KEEP_LOCAL,
                        localHash, remoteHash, baseHash, remoteContent);
            }
            if (remoteChanged) {
                return new AIConfigMergeItem(path, AIConfigMergeCategory.REMOTE_CHANGED,
                        AIConfigMergeItem.Action.TAKE_REMOTE,
                        localHash, remoteHash, baseHash, remoteContent);
            }
            if (localChanged) {
                return new AIConfigMergeItem(path, AIConfigMergeCategory.LOCAL_CHANGED,
                        AIConfigMergeItem.Action.KEEP_LOCAL,
                        localHash, remoteHash, baseHash, remoteContent);
            }

            // Hashes differ but neither matches base — treat as both changed
            return new AIConfigMergeItem(path, AIConfigMergeCategory.BOTH_CHANGED,
                    AIConfigMergeItem.Action.KEEP_LOCAL,
                    localHash, remoteHash, baseHash, remoteContent);
        }

        // Fallback: remote only, no base
        return new AIConfigMergeItem(path, AIConfigMergeCategory.NEW_REMOTE,
                AIConfigMergeItem.Action.ADD,
                null, remoteHash, baseHash, remoteContent);
    }

    private static String computeLocalHash(@NotNull String basePath, @NotNull String relativePath) {
        try {
            File file = new File(basePath, relativePath);
            if (file.exists() && file.isFile()) {
                byte[] content = java.nio.file.Files.readAllBytes(file.toPath());
                return calculateMD5(content);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    @NotNull
    private static String calculateMD5(@NotNull byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(data);
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
}

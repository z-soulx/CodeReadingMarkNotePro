package jp.kitabatakep.intellij.plugins.codereadingnote.aiconfig;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Strongly-typed representation of ai-config-registry.json exchanged with remote.
 * Serialized/deserialized via Gson — replaces hand-built JSON string construction.
 */
public class AIConfigMetadata {

    public List<String> customPaths = new ArrayList<>();
    public List<String> ignorePatterns = new ArrayList<>();
    public List<TrackedEntry> trackedEntries = new ArrayList<>();
    public List<FileHash> lastPushedFileHashes = new ArrayList<>();
    public List<String> trackedEmptyDirs = new ArrayList<>();

    public static class TrackedEntry {
        public String relativePath;
        public boolean tracked;
        public String typeName;

        public TrackedEntry() {}

        public TrackedEntry(String relativePath, boolean tracked, String typeName) {
            this.relativePath = relativePath;
            this.tracked = tracked;
            this.typeName = typeName;
        }
    }

    public static class FileHash {
        public String relativePath;
        public String contentHash;

        public FileHash() {}

        public FileHash(String relativePath, String contentHash) {
            this.relativePath = relativePath;
            this.contentHash = contentHash;
        }
    }

    /**
     * Converts lastPushedFileHashes list to a map for easy lookup.
     */
    public Map<String, String> toFileHashMap() {
        Map<String, String> map = new LinkedHashMap<>();
        if (lastPushedFileHashes != null) {
            for (FileHash fh : lastPushedFileHashes) {
                if (fh.relativePath != null && fh.contentHash != null) {
                    map.put(fh.relativePath, fh.contentHash);
                }
            }
        }
        return map;
    }

    /**
     * Converts trackedEntries to TrackedEntryMeta list for AIConfigService.
     */
    public List<AIConfigSyncAdapter.TrackedEntryMeta> toTrackedEntryMetas() {
        List<AIConfigSyncAdapter.TrackedEntryMeta> result = new ArrayList<>();
        if (trackedEntries != null) {
            for (TrackedEntry te : trackedEntries) {
                if (te.relativePath != null) {
                    result.add(new AIConfigSyncAdapter.TrackedEntryMeta(
                            te.relativePath, te.tracked,
                            te.typeName != null ? te.typeName : "CUSTOM"));
                }
            }
        }
        return result;
    }
}

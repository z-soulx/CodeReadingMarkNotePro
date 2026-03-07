package jp.kitabatakep.intellij.plugins.codereadingnote.sync;

import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Carries detailed per-file results from a pushFiles operation.
 * Serialized to a simple JSON string so it can travel through SyncResult.data.
 */
public class FilePushReport {

    private final List<String> skippedFiles = new ArrayList<>();
    private final List<String> pushedFiles = new ArrayList<>();
    private final Map<String, String> failedFiles = new LinkedHashMap<>();
    private final List<String> deletedFiles = new ArrayList<>();
    private final List<String> emptyDirsSynced = new ArrayList<>();

    public void addSkipped(@NotNull String path) { skippedFiles.add(path); }
    public void addPushed(@NotNull String path) { pushedFiles.add(path); }
    public void addFailed(@NotNull String path, @NotNull String reason) { failedFiles.put(path, reason); }
    public void addDeleted(@NotNull String path) { deletedFiles.add(path); }
    public void addEmptyDir(@NotNull String path) { emptyDirsSynced.add(path); }

    @NotNull public List<String> getSkippedFiles() { return skippedFiles; }
    @NotNull public List<String> getPushedFiles() { return pushedFiles; }
    @NotNull public Map<String, String> getFailedFiles() { return failedFiles; }
    @NotNull public List<String> getDeletedFiles() { return deletedFiles; }
    @NotNull public List<String> getEmptyDirsSynced() { return emptyDirsSynced; }

    public boolean hasFailures() { return !failedFiles.isEmpty(); }
    public boolean allSkipped() { return pushedFiles.isEmpty() && deletedFiles.isEmpty() && failedFiles.isEmpty(); }

    /**
     * Serialize to a minimal JSON string for transport through SyncResult.data.
     */
    @NotNull
    public String toJson() {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"skipped\":").append(listToJson(skippedFiles));
        sb.append(",\"pushed\":").append(listToJson(pushedFiles));
        sb.append(",\"failed\":").append(mapToJson(failedFiles));
        sb.append(",\"deleted\":").append(listToJson(deletedFiles));
        sb.append(",\"emptyDirs\":").append(listToJson(emptyDirsSynced));
        sb.append("}");
        return sb.toString();
    }

    /**
     * Deserialize from the JSON string produced by {@link #toJson()}.
     */
    @NotNull
    public static FilePushReport fromJson(@NotNull String json) {
        FilePushReport report = new FilePushReport();
        parseJsonList(json, "skipped", report.skippedFiles);
        parseJsonList(json, "pushed", report.pushedFiles);
        parseJsonMap(json, "failed", report.failedFiles);
        parseJsonList(json, "deleted", report.deletedFiles);
        parseJsonList(json, "emptyDirs", report.emptyDirsSynced);
        return report;
    }

    // --- simple JSON helpers (no external lib dependency) ---

    @NotNull
    private static String listToJson(@NotNull List<String> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(escapeJson(list.get(i))).append("\"");
        }
        return sb.append("]").toString();
    }

    @NotNull
    private static String mapToJson(@NotNull Map<String, String> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (!first) sb.append(",");
            sb.append("\"").append(escapeJson(entry.getKey())).append("\":\"")
              .append(escapeJson(entry.getValue())).append("\"");
            first = false;
        }
        return sb.append("}").toString();
    }

    @NotNull
    private static String escapeJson(@NotNull String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    private static void parseJsonList(@NotNull String json, @NotNull String key, @NotNull List<String> out) {
        String prefix = "\"" + key + "\":[";
        int start = json.indexOf(prefix);
        if (start < 0) return;
        start += prefix.length();
        int end = json.indexOf("]", start);
        if (end <= start) return;
        String content = json.substring(start, end);
        if (content.isEmpty()) return;
        for (String token : splitJsonStrings(content)) {
            out.add(unescapeJson(token));
        }
    }

    private static void parseJsonMap(@NotNull String json, @NotNull String key, @NotNull Map<String, String> out) {
        String prefix = "\"" + key + "\":{";
        int start = json.indexOf(prefix);
        if (start < 0) return;
        start += prefix.length();
        int depth = 1;
        int end = start;
        while (end < json.length() && depth > 0) {
            char c = json.charAt(end);
            if (c == '{') depth++;
            else if (c == '}') depth--;
            if (depth > 0) end++;
        }
        String content = json.substring(start, end);
        if (content.isEmpty()) return;
        List<String> tokens = splitJsonStrings(content);
        for (int i = 0; i + 1 < tokens.size(); i += 2) {
            out.put(unescapeJson(tokens.get(i)), unescapeJson(tokens.get(i + 1)));
        }
    }

    @NotNull
    private static List<String> splitJsonStrings(@NotNull String content) {
        List<String> result = new ArrayList<>();
        int i = 0;
        while (i < content.length()) {
            int qStart = content.indexOf('"', i);
            if (qStart < 0) break;
            int qEnd = findClosingQuote(content, qStart + 1);
            if (qEnd < 0) break;
            result.add(content.substring(qStart + 1, qEnd));
            i = qEnd + 1;
        }
        return result;
    }

    private static int findClosingQuote(@NotNull String s, int from) {
        for (int i = from; i < s.length(); i++) {
            if (s.charAt(i) == '\\') { i++; continue; }
            if (s.charAt(i) == '"') return i;
        }
        return -1;
    }

    @NotNull
    private static String unescapeJson(@NotNull String s) {
        return s.replace("\\\\", "\\").replace("\\\"", "\"")
                .replace("\\n", "\n").replace("\\r", "\r").replace("\\t", "\t");
    }
}

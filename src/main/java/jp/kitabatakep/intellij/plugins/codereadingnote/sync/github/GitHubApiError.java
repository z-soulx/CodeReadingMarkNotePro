package jp.kitabatakep.intellij.plugins.codereadingnote.sync.github;

import com.google.gson.*;
import jp.kitabatakep.intellij.plugins.codereadingnote.CodeReadingNoteBundle;
import java.util.Locale;

/** Parse escaped JSON as JSON; never infer overwrite permission from an HTTP error. */
public final class GitHubApiError {
    private GitHubApiError() {}
    public static String summary(String body) {
        try {
            JsonElement parsed = JsonParser.parseString(body);
            if (parsed.isJsonObject()) {
                JsonElement message = parsed.getAsJsonObject().get("message");
                if (message != null && message.isJsonPrimitive() && message.getAsJsonPrimitive().isString()) return message.getAsString();
            }
        } catch (JsonParseException | IllegalStateException malformed) { /* Fall back to the localized status advice. */ }
        java.util.regex.Matcher title = java.util.regex.Pattern.compile("<title[^>]*>([^<]+)</title>", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(body);
        if (title.find()) return title.group(1).trim().replace("&middot;", "·").replace("&mdash;", "—")
                .replace("&ndash;", "–").replace("&quot;", "\"").replace("&#39;", "'").replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&");
        String plain = body.trim();
        return plain.length() < 200 && !plain.startsWith("{") && !plain.startsWith("[") && !plain.contains("<") ? plain : "";
    }
    public static String validationKind(String body) {
        String detail = summary(body).toLowerCase(Locale.ROOT);
        // Structured field errors are used when GitHub supplies no useful message.
        try {
            JsonElement parsed = JsonParser.parseString(body);
            if (parsed.isJsonObject()) {
                JsonElement errors = parsed.getAsJsonObject().get("errors");
                if (errors != null && errors.isJsonArray()) for (JsonElement e : errors.getAsJsonArray()) {
                    if (e.isJsonObject() && e.getAsJsonObject().has("field")
                            && "sha".equals(e.getAsJsonObject().get("field").getAsString())) detail += " sha " + e;
                }
            }
        } catch (JsonParseException | IllegalStateException | UnsupportedOperationException malformed) { /* Generic validation advice remains available. */ }
        if (detail.contains("sha") && (detail.contains("match") || detail.contains("outdated"))) return "conflict";
        if (detail.contains("sha")) return "sha";
        return "validation";
    }
    public static String adviceKey(int status, String body) {
        return switch (status) {
            case 401 -> "auth"; case 403 -> "access"; case 404 -> "repository";
            case 409, 412 -> "conflict"; case 422 -> validationKind(body); case 429 -> "rate";
            default -> "network";
        };
    }
    public static String format(int status, String body, String credential) {
        String summary = summary(body);
        if (credential != null && !credential.isEmpty()) summary = summary.replace(credential, "[redacted]");
        summary = summary.replaceAll("(?i)\\b(?:gh[pousr]_[A-Za-z0-9_]+|github_pat_[A-Za-z0-9_]+)\\b", "[redacted]");
        if (summary.length() > 1000) summary = summary.substring(0, 1000);
        String advice = CodeReadingNoteBundle.message("notes.sync.status." + adviceKey(status, body));
        return CodeReadingNoteBundle.message("sync.github.api.error", status, summary.isBlank() ? advice : summary + "\n" + advice);
    }
}

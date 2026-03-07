package jp.kitabatakep.intellij.plugins.codereadingnote.aiconfig;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

/**
 * Known AI config directory/file patterns with auto-discovery support.
 */
public enum AIConfigType {
    CURSOR_RULES("Cursor Rules", ".cursor/rules/", true),
    CLAUDE_RULES("Claude Rules", ".claude/", true),
    AI_DOCS("AI Docs", ".ai/", true),
    WINDSURF("Windsurf", ".windsurf/", true),
    CODEX("Codex", ".codex/", true),
    COPILOT("GitHub Copilot", ".github/copilot-instructions.md", false),
    CUSTOM("Custom", null, true);

    private final String displayName;
    private final String defaultPath;
    private final boolean isDirectory;

    AIConfigType(@NotNull String displayName, @Nullable String defaultPath, boolean isDirectory) {
        this.displayName = displayName;
        this.defaultPath = defaultPath;
        this.isDirectory = isDirectory;
    }

    @NotNull
    public String getDisplayName() {
        return displayName;
    }

    @Nullable
    public String getDefaultPath() {
        return defaultPath;
    }

    public boolean isDirectory() {
        return isDirectory;
    }

    @NotNull
    public static List<AIConfigType> getKnownTypes() {
        return Arrays.asList(CURSOR_RULES, CLAUDE_RULES, AI_DOCS, WINDSURF, CODEX, COPILOT);
    }

    @NotNull
    public static AIConfigType detectType(@NotNull String relativePath) {
        String normalized = relativePath.replace('\\', '/');
        if (normalized.startsWith(".cursor/rules/") || normalized.equals(".cursor/rules")) {
            return CURSOR_RULES;
        }
        if (normalized.startsWith(".claude/") || normalized.equals(".claude")) {
            return CLAUDE_RULES;
        }
        if (normalized.startsWith(".ai/") || normalized.equals(".ai")) {
            return AI_DOCS;
        }
        if (normalized.startsWith(".windsurf/") || normalized.equals(".windsurf")) {
            return WINDSURF;
        }
        if (normalized.startsWith(".codex/") || normalized.equals(".codex")) {
            return CODEX;
        }
        if (normalized.equals(".github/copilot-instructions.md")) {
            return COPILOT;
        }
        return CUSTOM;
    }
}

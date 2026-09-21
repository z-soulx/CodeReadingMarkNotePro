package jp.kitabatakep.intellij.plugins.codereadingnote.aiconfig;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Registry for user-defined AI tool types.
 * Allows runtime registration of new AI config path patterns
 * (e.g., ".amazonq/", ".jetbrains-ai/") without code changes.
 *
 * Built-in types remain in {@link AIConfigType} enum;
 * this registry handles only custom definitions.
 */
public final class AIConfigTypeRegistry {

    private static final List<CustomAIToolDef> definitions = new CopyOnWriteArrayList<>();

    private AIConfigTypeRegistry() {}

    public static void register(@NotNull CustomAIToolDef def) {
        for (CustomAIToolDef existing : definitions) {
            if (existing.id.equals(def.id)) {
                definitions.remove(existing);
                break;
            }
        }
        definitions.add(def);
    }

    public static void unregister(@NotNull String id) {
        definitions.removeIf(d -> d.id.equals(id));
    }

    @NotNull
    public static List<CustomAIToolDef> getDefinitions() {
        return Collections.unmodifiableList(new ArrayList<>(definitions));
    }

    public static void setDefinitions(@NotNull List<CustomAIToolDef> defs) {
        definitions.clear();
        definitions.addAll(defs);
    }

    /**
     * Checks if a normalized path matches a custom type definition.
     * Returns the display name if matched, null otherwise.
     */
    @Nullable
    public static String detectCustomTypeName(@NotNull String normalizedPath) {
        for (CustomAIToolDef def : definitions) {
            String prefix = def.pathPrefix;
            if (prefix != null && !prefix.isEmpty()) {
                if (normalizedPath.startsWith(prefix) || normalizedPath.equals(prefix.endsWith("/")
                        ? prefix.substring(0, prefix.length() - 1) : prefix)) {
                    return def.displayName;
                }
            }
        }
        return null;
    }

    /**
     * Returns effective display name for a path: custom definition name or built-in type name.
     */
    @NotNull
    public static String getEffectiveDisplayName(@NotNull String relativePath) {
        String normalized = relativePath.replace('\\', '/');
        String customName = detectCustomTypeName(normalized);
        if (customName != null) return customName;
        return AIConfigType.detectType(relativePath).getDisplayName();
    }

    /**
     * Checks if a path falls under any custom type definition prefix.
     */
    public static boolean isCustomTypePath(@NotNull String relativePath) {
        String normalized = relativePath.replace('\\', '/');
        return detectCustomTypeName(normalized) != null;
    }

    public static class CustomAIToolDef {
        public String id = "";
        public String displayName = "";
        public String pathPrefix = "";
        public boolean isDirectory = true;

        public CustomAIToolDef() {}

        public CustomAIToolDef(@NotNull String id, @NotNull String displayName,
                               @NotNull String pathPrefix, boolean isDirectory) {
            this.id = id;
            this.displayName = displayName;
            this.pathPrefix = pathPrefix.replace('\\', '/');
            this.isDirectory = isDirectory;
        }
    }
}

package jp.kitabatakep.intellij.plugins.codereadingnote.aiconfig;

import jp.kitabatakep.intellij.plugins.codereadingnote.CodeReadingNoteBundle;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Defines the .ai/ directory skeleton structure: available directories,
 * their descriptions, required/optional status, and preset groupings.
 */
public final class AISkeletonConfig {

    public enum Preset {
        NONE, MINIMAL, STANDARD, FULL
    }

    private AISkeletonConfig() {
    }

    /**
     * One directory entry in the skeleton.
     */
    public static class DirEntry {
        private final String name;
        private final String nameKey;
        private final String descKey;
        private final String referenceKey;
        private final boolean required;

        DirEntry(@NotNull String name, @NotNull String nameKey, @NotNull String descKey,
                 @NotNull String referenceKey, boolean required) {
            this.name = name;
            this.nameKey = nameKey;
            this.descKey = descKey;
            this.referenceKey = referenceKey;
            this.required = required;
        }

        @NotNull public String getName() { return name; }
        @NotNull public String getDisplayName() { return CodeReadingNoteBundle.message(nameKey); }
        @NotNull public String getDescription() { return CodeReadingNoteBundle.message(descKey); }
        @NotNull public String getReferenceText() { return CodeReadingNoteBundle.message(referenceKey); }
        public boolean isRequired() { return required; }

        /** Relative path under project root, e.g. ".ai/context" */
        @NotNull
        public String getRelativePath() {
            return ".ai/" + name;
        }
    }

    private static final List<DirEntry> ALL_DIRS = Collections.unmodifiableList(Arrays.asList(
        new DirEntry("context", "aiconfig.skeleton.dir.context", "aiconfig.skeleton.dir.context.desc",
                "aiconfig.skeleton.reference.context", true),
        new DirEntry("prd", "aiconfig.skeleton.dir.prd", "aiconfig.skeleton.dir.prd.desc",
                "aiconfig.skeleton.reference.prd", true),
        new DirEntry("spec", "aiconfig.skeleton.dir.spec", "aiconfig.skeleton.dir.spec.desc",
                "aiconfig.skeleton.reference.spec", true),
        new DirEntry("adr", "aiconfig.skeleton.dir.adr", "aiconfig.skeleton.dir.adr.desc",
                "aiconfig.skeleton.reference.adr", true),
        new DirEntry("runs", "aiconfig.skeleton.dir.runs", "aiconfig.skeleton.dir.runs.desc",
                "aiconfig.skeleton.reference.runs", true),
        new DirEntry("contracts", "aiconfig.skeleton.dir.contracts", "aiconfig.skeleton.dir.contracts.desc",
                "aiconfig.skeleton.reference.contracts", false),
        new DirEntry("tasks", "aiconfig.skeleton.dir.tasks", "aiconfig.skeleton.dir.tasks.desc",
                "aiconfig.skeleton.reference.tasks", false),
        new DirEntry("patterns", "aiconfig.skeleton.dir.patterns", "aiconfig.skeleton.dir.patterns.desc",
                "aiconfig.skeleton.reference.patterns", false),
        new DirEntry("docs", "aiconfig.skeleton.dir.docs", "aiconfig.skeleton.dir.docs.desc",
                "aiconfig.skeleton.reference.docs", false)
    ));

    @NotNull
    public static List<DirEntry> getAllDirs() {
        return ALL_DIRS;
    }

    /**
     * Returns the set of directory names included in the given preset.
     */
    @NotNull
    public static Set<String> getDirsForPreset(@NotNull Preset preset) {
        Set<String> names = new LinkedHashSet<>();
        switch (preset) {
            case NONE:
                break;
            case MINIMAL:
                for (DirEntry d : ALL_DIRS) {
                    if (d.isRequired()) names.add(d.getName());
                }
                break;
            case STANDARD:
                for (DirEntry d : ALL_DIRS) {
                    if (d.isRequired()) names.add(d.getName());
                }
                names.add("contracts");
                names.add("tasks");
                break;
            case FULL:
                for (DirEntry d : ALL_DIRS) {
                    names.add(d.getName());
                }
                break;
        }
        return names;
    }

    @NotNull
    public static String getPresetDisplayName(@NotNull Preset preset) {
        switch (preset) {
            case NONE: return CodeReadingNoteBundle.message("aiconfig.skeleton.preset.none");
            case MINIMAL: return CodeReadingNoteBundle.message("aiconfig.skeleton.preset.minimal");
            case STANDARD: return CodeReadingNoteBundle.message("aiconfig.skeleton.preset.standard");
            case FULL: return CodeReadingNoteBundle.message("aiconfig.skeleton.preset.full");
            default: return preset.name();
        }
    }
}

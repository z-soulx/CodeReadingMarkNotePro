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
        WORKSPACE, NOTES, ALL
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
        new DirEntry("adr", "aiconfig.skeleton.dir.adr", "aiconfig.skeleton.dir.adr.desc",
                "aiconfig.skeleton.reference.adr", false),
        new DirEntry("specs", "aiconfig.skeleton.dir.specs", "aiconfig.skeleton.dir.specs.desc",
                "aiconfig.skeleton.reference.spec", true),
        new DirEntry("runs", "aiconfig.skeleton.dir.runs", "aiconfig.skeleton.dir.runs.desc",
                "aiconfig.skeleton.reference.runs", true),
        new DirEntry("docs", "aiconfig.skeleton.dir.docs", "aiconfig.skeleton.dir.docs.desc",
                "aiconfig.skeleton.reference.docs", false),
        new DirEntry("docs/architecture", "aiconfig.skeleton.dir.docs.architecture", "aiconfig.skeleton.dir.docs.architecture.desc", "aiconfig.skeleton.reference.docs.architecture", false),
        new DirEntry("docs/domain", "aiconfig.skeleton.dir.docs.domain", "aiconfig.skeleton.dir.docs.domain.desc", "aiconfig.skeleton.reference.docs.domain", false),
        new DirEntry("docs/scenario", "aiconfig.skeleton.dir.docs.scenario", "aiconfig.skeleton.dir.docs.scenario.desc", "aiconfig.skeleton.reference.docs.scenario", false),
        new DirEntry("docs/integration", "aiconfig.skeleton.dir.docs.integration", "aiconfig.skeleton.dir.docs.integration.desc", "aiconfig.skeleton.reference.docs.integration", false),
        new DirEntry("docs/suppliers", "aiconfig.skeleton.dir.docs.suppliers", "aiconfig.skeleton.dir.docs.suppliers.desc", "aiconfig.skeleton.reference.docs.suppliers", false),
        new DirEntry("docs/shared", "aiconfig.skeleton.dir.docs.shared", "aiconfig.skeleton.dir.docs.shared.desc", "aiconfig.skeleton.reference.docs.shared", false),
        new DirEntry("docs/runbooks", "aiconfig.skeleton.dir.docs.runbooks", "aiconfig.skeleton.dir.docs.runbooks.desc", "aiconfig.skeleton.reference.docs.runbooks", false)
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
            case WORKSPACE:
                names.add("context"); names.add("adr"); names.add("specs"); names.add("runs");
                break;
            case NOTES:
                for (DirEntry d : ALL_DIRS) if (d.getName().startsWith("docs")) names.add(d.getName());
                break;
            case ALL:
                for (DirEntry d : ALL_DIRS) names.add(d.getName());
                break;
        }
        return names;
    }

    @NotNull
    public static String getPresetDisplayName(@NotNull Preset preset) {
        switch (preset) {
            case WORKSPACE: return CodeReadingNoteBundle.message("aiconfig.skeleton.preset.workspace");
            case NOTES: return CodeReadingNoteBundle.message("aiconfig.skeleton.preset.notes");
            case ALL: return CodeReadingNoteBundle.message("aiconfig.skeleton.preset.all");
            default: return preset.name();
        }
    }
}

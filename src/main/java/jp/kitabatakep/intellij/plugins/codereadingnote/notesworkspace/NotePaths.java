package jp.kitabatakep.intellij.plugins.codereadingnote.notesworkspace;

import java.nio.file.Path;

/** XML uses project-relative paths and forward slashes, independent of the writing OS. */
public final class NotePaths {
    private NotePaths() {}
    public static Path resolve(Path root, String relative) {
        Path base = root.toAbsolutePath().normalize();
        Path path = base.resolve(relative.replace('\\', '/')).normalize();
        if (!path.startsWith(base)) throw new IllegalArgumentException("Relative note path escapes its project");
        return path;
    }
}

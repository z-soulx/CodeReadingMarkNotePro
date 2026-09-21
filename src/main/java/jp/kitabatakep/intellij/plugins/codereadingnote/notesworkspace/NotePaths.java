package jp.kitabatakep.intellij.plugins.codereadingnote.notesworkspace;

import java.nio.file.Path;

/** XML uses project-relative paths and forward slashes, independent of the writing OS. */
public final class NotePaths {
    private NotePaths() {}
    public static String relative(Path root, Path file) {
        Path base = WorkspaceDiscovery.identity(root);
        Path target = WorkspaceDiscovery.identity(file);
        if (!target.startsWith(base)) return null;
        return base.relativize(target).toString().replace('\\', '/');
    }
    public static Path resolve(Path root, String relative) {
        Path base = root.toAbsolutePath().normalize();
        Path path = base.resolve(relative.replace('\\', '/')).normalize();
        if (!path.startsWith(base)) throw new IllegalArgumentException("Relative note path escapes its project");
        return path;
    }
}

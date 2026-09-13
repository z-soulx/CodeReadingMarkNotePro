package jp.kitabatakep.intellij.plugins.codereadingnote.notesworkspace;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.function.BiConsumer;

/** Uses NOFOLLOW_LINKS, including Windows reparse points (junctions). */
public final class WorkspaceDiscovery {
    private static final Set<String> EXCLUDED = Set.of(".git", ".idea", "node_modules", "build", "target", "out", ".gradle");
    private WorkspaceDiscovery() {}

    public static List<Path> discover(Path workspace, BiConsumer<Path, IOException> errors) throws IOException {
        Path root = workspace.toAbsolutePath().normalize();
        Set<Path> roots = new TreeSet<>();
        roots.add(root);
        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                if (Thread.currentThread().isInterrupted()) return FileVisitResult.TERMINATE;
                if (!dir.equals(root) && (EXCLUDED.contains(dir.getFileName().toString()) || isLink(dir))) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                Path idea = dir.resolve(".idea");
                if (Files.isDirectory(idea, LinkOption.NOFOLLOW_LINKS) && !isLink(idea)) roots.add(dir);
                return FileVisitResult.CONTINUE;
            }
            @Override public FileVisitResult visitFileFailed(Path file, IOException error) {
                errors.accept(file, error);
                return FileVisitResult.CONTINUE;
            }
        });
        return List.copyOf(roots);
    }

    public static boolean isLink(Path path) throws IOException {
        BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        // The Windows provider reports junctions as other/reparse points; real path also catches aliases.
        return attrs.isSymbolicLink() || attrs.isOther()
                || !path.toAbsolutePath().normalize().equals(path.toRealPath());
    }

    public static Path owner(Path file, Collection<Path> roots, Path fallback) {
        Path normalized = file.toAbsolutePath().normalize();
        return roots.stream().filter(normalized::startsWith)
                .max(Comparator.comparingInt(Path::getNameCount)).orElse(fallback);
    }
}

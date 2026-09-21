package jp.kitabatakep.intellij.plugins.codereadingnote.notesworkspace;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.function.BiConsumer;

/** Discovers real directories recursively and linked project roots without traversing arbitrary directory links. */
public final class WorkspaceDiscovery {
    private static final Set<String> EXCLUDED = Set.of(".git", ".idea", "node_modules", "build", "target", "out", ".gradle");
    private WorkspaceDiscovery() {}

    public static List<Path> discover(Path workspace, BiConsumer<Path, IOException> errors) throws IOException {
        Path root = workspace.toAbsolutePath().normalize();
        Set<Path> candidates = new TreeSet<>();
        candidates.add(root);
        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                if (Thread.currentThread().isInterrupted()) return FileVisitResult.TERMINATE;
                if (!dir.equals(root) && EXCLUDED.contains(dir.getFileName().toString())) return FileVisitResult.SKIP_SUBTREE;
                if (!dir.equals(root) && isLink(dir)) {
                    if (isProjectRoot(dir)) candidates.add(dir.toAbsolutePath().normalize());
                    return FileVisitResult.SKIP_SUBTREE;
                }
                if (isProjectRoot(dir)) candidates.add(dir.toAbsolutePath().normalize());
                return FileVisitResult.CONTINUE;
            }
            @Override public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                // Unix symbolic links are files when the tree is walked without FOLLOW_LINKS.
                if (Files.isSymbolicLink(file) && !EXCLUDED.contains(file.getFileName().toString())
                        && Files.isDirectory(file) && isProjectRoot(file)) {
                    candidates.add(file.toAbsolutePath().normalize());
                }
                return FileVisitResult.CONTINUE;
            }
            @Override public FileVisitResult visitFileFailed(Path file, IOException error) {
                errors.accept(file, error);
                return FileVisitResult.CONTINUE;
            }
        });
        Map<Path, Path> unique = new LinkedHashMap<>();
        for (Path candidate : candidates) unique.putIfAbsent(identity(candidate), candidate);
        return List.copyOf(unique.values());
    }

    private static boolean isProjectRoot(Path path) throws IOException {
        Path idea = path.resolve(".idea");
        return Files.isDirectory(idea) && !isDirectLink(idea)
                && Files.isRegularFile(idea.resolve("CodeReadingNote.xml"), LinkOption.NOFOLLOW_LINKS);
    }

    private static boolean isDirectLink(Path path) throws IOException {
        BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        return attrs.isSymbolicLink() || attrs.isOther();
    }

    public static boolean isLink(Path path) throws IOException {
        BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        // The Windows provider reports junctions as other/reparse points; real path also catches aliases.
        return attrs.isSymbolicLink() || attrs.isOther()
                || !path.toAbsolutePath().normalize().equals(path.toRealPath());
    }

    public static Path identity(Path path) {
        Path normalized = path.toAbsolutePath().normalize();
        try { return normalized.toRealPath(); }
        catch (IOException unavailable) {
            // Deleted/unavailable projects retain a lexical key for lifecycle cleanup and error reporting.
            return normalized;
        }
    }

    public static Path owner(Path file, Collection<Path> roots, Path fallback) {
        Path lexical = file.toAbsolutePath().normalize();
        Path lexicalOwner = roots.stream().filter(lexical::startsWith)
                .max(Comparator.comparingInt(Path::getNameCount)).orElse(null);
        if (lexicalOwner != null && !lexicalOwner.equals(fallback)) return lexicalOwner;
        Path normalized = identity(lexical);
        return roots.stream().filter(root -> normalized.startsWith(identity(root)))
                .max(Comparator.comparingInt(root -> identity(root).getNameCount()))
                .orElse(lexicalOwner == null ? fallback : lexicalOwner);
    }
}

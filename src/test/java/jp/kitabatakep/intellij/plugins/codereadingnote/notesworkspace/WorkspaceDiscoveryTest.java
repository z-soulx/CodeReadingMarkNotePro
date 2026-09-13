package jp.kitabatakep.intellij.plugins.codereadingnote.notesworkspace;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import java.nio.file.*;
import java.util.*;
import static org.junit.Assert.*;

public class WorkspaceDiscoveryTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();
    @Test public void recursiveProjectsIncludeEmptyAndNestedButExcludeBuildTrees() throws Exception {
        Path root = temporary.getRoot().toPath();
        for (String path : List.of("a", "a/nested", "other/a", "empty")) Files.createDirectories(root.resolve(path + "/.idea"));
        for (String excluded : List.of(".git", ".idea", "node_modules", "build", "target", "out", ".gradle")) {
            Files.createDirectories(root.resolve(excluded + "/hidden/.idea"));
        }
        List<Path> found = WorkspaceDiscovery.discover(root, (path, error) -> fail(path + ": " + error));
        assertEquals(5, found.size());
        assertTrue(found.contains(root.resolve("a/nested")));
        assertTrue(found.contains(root.resolve("other/a")));
        assertFalse(Files.exists(root.resolve("empty/.idea/CodeReadingNote.xml")));
    }
    @Test public void closestRootOwnsFilesWithIdenticalRelativeNames() {
        Path root = temporary.getRoot().toPath();
        List<Path> roots = List.of(root, root.resolve("a"), root.resolve("b"), root.resolve("a/nested"));
        assertEquals(root.resolve("a"), WorkspaceDiscovery.owner(root.resolve("a/src/Main.java"), roots, root));
        assertEquals(root.resolve("b"), WorkspaceDiscovery.owner(root.resolve("b/src/Main.java"), roots, root));
        assertEquals(root.resolve("a/nested"), WorkspaceDiscovery.owner(root.resolve("a/nested/Main.java"), roots, root));
        assertEquals(root, WorkspaceDiscovery.owner(root.resolve("abc/Main.java"), roots, root));
        assertEquals(root, WorkspaceDiscovery.owner(root.resolve("README.md"), roots, root));
    }
    @Test public void relativePathsFromBothOperatingSystemsStayInTheirOwner() {
        Path root = temporary.getRoot().toPath();
        Path a = root.resolve("a"), b = root.resolve("b");
        assertEquals(a.resolve("src/Main.java"), NotePaths.resolve(a, "src\\Main.java"));
        assertEquals(b.resolve("src/Main.java"), NotePaths.resolve(b, "src/Main.java"));
        assertNotEquals(NotePaths.resolve(a, "src/Main.java"), NotePaths.resolve(b, "src/Main.java"));
        try { NotePaths.resolve(a, "../b/secret"); fail("Escaping path accepted"); }
        catch (IllegalArgumentException expected) { /* project boundary */ }
    }
    @Test public void normalizedIdentityAndRelativeDisplayDistinguishSameDirectoryNames() {
        Path root = temporary.getRoot().toPath();
        NoteProjectContext left = new NoteProjectContext(root.resolve("left/app"));
        NoteProjectContext right = new NoteProjectContext(root.resolve("right/app"));
        assertNotEquals(left, right);
        assertEquals(left, new NoteProjectContext(root.resolve("left/x/../app")));
        assertEquals("left/app", left.displayName(root));
        assertEquals("right/app", right.displayName(root));
    }
    @Test public void loadingDoesNotEmitWrites() {
        NoteProjectContext context = new NoteProjectContext(temporary.getRoot().toPath());
        int[] changes = {0}; context.onChange(() -> changes[0]++);
        context.loading(() -> { context.changed(); context.loading(context::changed); });
        assertEquals(0, changes[0]);
        context.changed(); assertEquals(1, changes[0]);
    }
    @Test public void linksAreNotFollowed() throws Exception {
        Path root = temporary.newFolder("workspace").toPath();
        Path external = temporary.newFolder("external").toPath();
        Files.createDirectories(external.resolve(".idea"));
        Path link = root.resolve("linked");
        if (System.getProperty("os.name").startsWith("Windows")) {
            Process process = new ProcessBuilder("cmd", "/c", "mklink", "/J", link.toString(), external.toString()).redirectErrorStream(true).start();
            String output = new String(process.getInputStream().readAllBytes());
            assertEquals(output, 0, process.waitFor());
        } else Files.createSymbolicLink(link, external);
        try {
            assertTrue(WorkspaceDiscovery.isLink(link));
            assertEquals(List.of(root), WorkspaceDiscovery.discover(root, (path, error) -> fail(error.toString())));
        } finally { Files.delete(link); }
    }
}

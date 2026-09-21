package jp.kitabatakep.intellij.plugins.codereadingnote.notesworkspace;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import java.nio.file.*;
import java.util.*;
import static org.junit.Assert.*;

public class WorkspaceDiscoveryTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();
    @Test public void recursiveProjectsRequireNotesDataAndExcludeBuildTrees() throws Exception {
        Path root = temporary.getRoot().toPath();
        for (String path : List.of("a", "a/nested", "other/a")) createNotesProject(root.resolve(path));
        Files.createDirectories(root.resolve("empty/.idea"));
        for (String excluded : List.of(".git", ".idea", "node_modules", "build", "target", "out", ".gradle")) {
            createNotesProject(root.resolve(excluded + "/hidden"));
        }
        List<Path> found = WorkspaceDiscovery.discover(root, (path, error) -> fail(path + ": " + error));
        assertEquals(4, found.size());
        assertTrue(found.contains(root.resolve("a/nested")));
        assertTrue(found.contains(root.resolve("other/a")));
        assertFalse(found.contains(root.resolve("empty")));
        assertFalse(Files.exists(root.resolve("empty/.idea/CodeReadingNote.xml")));
        Path notes = root.resolve("empty/.idea/CodeReadingNote.xml");
        Files.writeString(notes, "<topics/>");
        assertTrue(WorkspaceDiscovery.discover(root, (path, error) -> fail(error.toString())).contains(root.resolve("empty")));
        Files.delete(notes);
        assertFalse(WorkspaceDiscovery.discover(root, (path, error) -> fail(error.toString())).contains(root.resolve("empty")));
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
    @Test public void linkedProjectRootsAreDiscoveredButLinkedContainersAreNotTraversed() throws Exception {
        Path root = temporary.newFolder("workspace").toPath();
        Path project = temporary.newFolder("project").toPath();
        Path container = temporary.newFolder("container").toPath();
        Path emptyProject = temporary.newFolder("empty-project").toPath();
        createNotesProject(project);
        createNotesProject(container.resolve("nested"));
        Files.createDirectories(emptyProject.resolve(".idea"));
        Path projectLink = createDirectoryLink(root.resolve("linked-project"), project);
        Path containerLink = createDirectoryLink(root.resolve("linked-container"), container);
        Path emptyLink = createDirectoryLink(root.resolve("linked-empty"), emptyProject);
        try {
            assertTrue(WorkspaceDiscovery.isLink(projectLink));
            assertEquals(List.of(root, projectLink), WorkspaceDiscovery.discover(root, (path, error) -> fail(error.toString())));
        } finally { Files.delete(projectLink); Files.delete(containerLink); Files.delete(emptyLink); }
    }
    @Test public void duplicateAliasesShareOneRealProjectIdentityAndOwnCanonicalFiles() throws Exception {
        Path root = temporary.newFolder("aliases").toPath();
        Path project = temporary.newFolder("shared-project").toPath();
        createNotesProject(project);
        Path source = Files.createDirectories(project.resolve("src")).resolve("Main.java");
        Files.createFile(source);
        Path first = createDirectoryLink(root.resolve("aa1"), project);
        Path second = createDirectoryLink(root.resolve("aa2"), project);
        try {
            assertEquals(List.of(root, first), WorkspaceDiscovery.discover(root, (path, error) -> fail(error.toString())));
            assertEquals(project.toRealPath(), new NoteProjectContext(first).root());
            assertEquals(new NoteProjectContext(first), new NoteProjectContext(second));
            assertEquals(project.toRealPath(), WorkspaceDiscovery.owner(first.resolve("src/Main.java"), List.of(root, project.toRealPath()), root));
            assertEquals(project.toRealPath(), WorkspaceDiscovery.owner(source, List.of(root, project.toRealPath()), root));
            assertEquals("src/Main.java", NotePaths.relative(project.toRealPath(), first.resolve("src/Main.java")));
        } finally { Files.delete(first); Files.delete(second); }
    }
    private Path createDirectoryLink(Path link, Path target) throws Exception {
        if (System.getProperty("os.name").startsWith("Windows")) {
            Process process = new ProcessBuilder("cmd", "/c", "mklink", "/J", link.toString(), target.toString()).redirectErrorStream(true).start();
            String output = new String(process.getInputStream().readAllBytes());
            assertEquals(output, 0, process.waitFor());
        } else Files.createSymbolicLink(link, target);
        return link;
    }
    private Path createNotesProject(Path root) throws Exception {
        Path notes = Files.createDirectories(root.resolve(".idea")).resolve("CodeReadingNote.xml");
        Files.writeString(notes, "<topics/>");
        return root;
    }
}

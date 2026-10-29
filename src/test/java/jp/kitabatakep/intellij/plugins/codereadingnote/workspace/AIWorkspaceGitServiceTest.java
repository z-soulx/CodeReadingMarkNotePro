package jp.kitabatakep.intellij.plugins.codereadingnote.workspace;

import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import com.intellij.openapi.project.Project;

import java.nio.file.Files;
import java.nio.file.Path;
import java.lang.reflect.Proxy;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AIWorkspaceGitServiceTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    @Test public void initializingNestedGitLeavesParentIgnoreAndTrackedFilesUntouched() throws Exception {
        Path root = temporary.getRoot().toPath();
        git(root, "init");
        Path ai = Files.createDirectories(root.resolve(".ai"));
        Files.writeString(ai.resolve("workspace-commands.json"), "{\"schemaVersion\":1,\"commands\":[]}");
        Files.writeString(ai.resolve("note.md"), "tracked by parent");
        Path ignore = root.resolve(".gitignore"); Files.writeString(ignore, "build/\n");
        git(root, "add", ".");
        byte[] index = Files.readAllBytes(root.resolve(".git/index"));
        Map<Class<?>, Object> services = new java.util.HashMap<>();
        Project project = (Project) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{Project.class}, (p, method, args) -> {
            if (method.getName().equals("getBasePath")) return root.toString();
            if (method.getName().equals("getService")) return services.get(args[0]);
            return null;
        });
        services.put(AIWorkspaceService.class, new AIWorkspaceService(project));
        services.put(AIWorkspaceCommandService.class, new AIWorkspaceCommandService(project));
        AIWorkspaceGitService service = new AIWorkspaceGitService(project);
        assertTrue(service.initialize().success);
        assertTrue(service.isInitialized());
        assertEquals("build/\n", Files.readString(ignore));
        assertArrayEquals(index, Files.readAllBytes(root.resolve(".git/index")));
        assertTrue(Files.readString(ai.resolve(".gitignore")).contains("token.txt"));
        Files.delete(ignore);
        assertTrue(service.initialize().success);
        assertFalse(Files.exists(ignore));
        assertArrayEquals(index, Files.readAllBytes(root.resolve(".git/index")));
    }

    private void git(Path root, String... args) throws Exception {
        java.util.List<String> command = new java.util.ArrayList<>();
        command.add("git"); command.addAll(java.util.List.of(args));
        Process process = new ProcessBuilder(command).directory(root.toFile()).redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes());
        assertEquals(output, 0, process.waitFor());
    }
    @Test
    public void detectsPathsUnderAiRoot() {
        Path root = Path.of("workspace", ".ai").toAbsolutePath().normalize();
        assertTrue(AIWorkspaceGitService.isPathUnder(root, root.resolve("test.md")));
        assertTrue(AIWorkspaceGitService.isPathUnder(root, root.resolve("docs").resolve("a.md")));
        assertFalse(AIWorkspaceGitService.isPathUnder(root, root.getParent().resolve("src").resolve("A.java")));
    }
}

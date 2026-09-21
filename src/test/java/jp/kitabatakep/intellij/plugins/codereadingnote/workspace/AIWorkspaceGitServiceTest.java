package jp.kitabatakep.intellij.plugins.codereadingnote.workspace;

import org.junit.Test;

import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AIWorkspaceGitServiceTest {
    @Test
    public void appendsParentIgnoreWhenMissing() {
        String result = AIWorkspaceGitService.withParentIgnoreRule("");
        assertTrue(AIWorkspaceGitService.hasParentIgnoreRule(result));
        assertTrue(result.contains(AIWorkspaceGitService.PARENT_IGNORE_RULE));
        assertTrue(result.contains(AIWorkspaceGitService.PARENT_IGNORE_COMMENT));
    }

    @Test
    public void doesNotTreatNestedIgnoreAsFullIsolation() {
        String existing = "/.ai/runs/\n/.ai/token.txt\n/.ai/.git/\n";
        assertFalse(AIWorkspaceGitService.hasParentIgnoreRule(existing));
        String result = AIWorkspaceGitService.withParentIgnoreRule(existing);
        assertTrue(AIWorkspaceGitService.hasParentIgnoreRule(result));
        assertTrue(result.contains("/.ai/runs/"));
        assertTrue(result.contains(AIWorkspaceGitService.PARENT_IGNORE_RULE + "\n")
                || result.endsWith(AIWorkspaceGitService.PARENT_IGNORE_RULE));
    }

    @Test
    public void skipsWhenRootAiAlreadyIgnored() {
        String existing = "build\n/.ai/\n";
        assertEquals(existing, AIWorkspaceGitService.withParentIgnoreRule(existing));
    }

    @Test
    public void detectsPathsUnderAiRoot() {
        Path root = Path.of("workspace", ".ai").toAbsolutePath().normalize();
        assertTrue(AIWorkspaceGitService.isPathUnder(root, root.resolve("test.md")));
        assertTrue(AIWorkspaceGitService.isPathUnder(root, root.resolve("docs").resolve("a.md")));
        assertFalse(AIWorkspaceGitService.isPathUnder(root, root.getParent().resolve("src").resolve("A.java")));
    }
}

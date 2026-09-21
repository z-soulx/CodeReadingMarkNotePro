package jp.kitabatakep.intellij.plugins.codereadingnote.workspace;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AIWorkspaceVersionServiceTest {
    @Test public void validatesStrictSemver() {
        assertTrue(AIWorkspaceVersionService.isValidSemver("1.0.0"));
        assertTrue(AIWorkspaceVersionService.isValidSemver("1.2.3-beta.1+build"));
        assertFalse(AIWorkspaceVersionService.isValidSemver(""));
        assertFalse(AIWorkspaceVersionService.isValidSemver("1.0"));
        assertFalse(AIWorkspaceVersionService.isValidSemver("01.0.0"));
    }

    @Test public void parsesCoreVersion() {
        AIWorkspaceVersionService.Semver version = AIWorkspaceVersionService.parseSemver("2.4.9-rc.1");
        assertEquals(2, version.major());
        assertEquals(4, version.minor());
        assertEquals(9, version.patch());
    }
}

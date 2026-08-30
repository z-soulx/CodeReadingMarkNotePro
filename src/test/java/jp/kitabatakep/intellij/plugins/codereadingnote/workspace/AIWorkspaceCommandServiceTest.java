package jp.kitabatakep.intellij.plugins.codereadingnote.workspace;

import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AIWorkspaceCommandServiceTest {
    @Test
    public void windowsTerminalGetsExplicitWorkingDirectory() {
        AIWorkspaceCommand command = command("wt", List.of("new-tab", "--profile", "PowerShell"));

        String result = AIWorkspaceCommandService.buildTerminalCommand(
                command, Path.of("C:\\work\\Project Space"), true);

        assertEquals("wt -d \"C:\\work\\Project Space\" new-tab --profile PowerShell", result);
    }

    @Test
    public void windowsTerminalExePathIsRecognized() {
        AIWorkspaceCommand command = command("C:\\Windows\\System32\\wt.exe", List.of());

        String result = AIWorkspaceCommandService.buildTerminalCommand(
                command, Path.of("C:\\work\\project"), true);

        assertEquals("C:\\Windows\\System32\\wt.exe -d C:\\work\\project", result);
    }

    @Test
    public void nonWindowsCommandsRemainUnchanged() {
        AIWorkspaceCommand command = command("echo", List.of("hello"));

        String result = AIWorkspaceCommandService.buildTerminalCommand(
                command, Path.of("C:\\work\\project"), true);

        assertEquals("echo hello", result);
    }

    @Test
    public void wtIsNotSpecialOnNonWindows() {
        AIWorkspaceCommand command = command("wt", List.of("--version"));

        String result = AIWorkspaceCommandService.buildTerminalCommand(
                command, Path.of("/tmp/project"), false);

        assertEquals("wt --version", result);
    }

    @Test
    public void windowsPrefersCmdSiblingOverUnixShim() throws Exception {
        Path dir = Files.createTempDirectory("cursor-bin");
        Path shim = dir.resolve("cursor");
        Path cmd = dir.resolve("cursor.cmd");
        Files.writeString(shim, "#!/bin/sh\n");
        Files.writeString(cmd, "@echo off\n");

        List<String> result = AIWorkspaceSilentLauncher.buildSilentCommand(
                shim.toString(), List.of("."), true, false, List.of(), List.of(), "cmd.exe");

        assertEquals(List.of("cmd.exe", "/c", cmd.toString(), "."), result);
    }

    @Test
    public void windowsPathSearchSkipsExtensionlessShim() throws Exception {
        Path dir = Files.createTempDirectory("path-cursor");
        Files.writeString(dir.resolve("cursor"), "#!/bin/sh\n");
        Path cmd = dir.resolve("cursor.cmd");
        Files.writeString(cmd, "@echo off\n");

        List<String> result = AIWorkspaceSilentLauncher.buildSilentCommand(
                "cursor", List.of("."), true, false, List.of(dir.toString()), List.of(), "cmd.exe");

        assertEquals(List.of("cmd.exe", "/c", cmd.toString(), "."), result);
    }

    @Test
    public void windowsFindsTyporaInProgramFilesWhenNotOnPath() throws Exception {
        Path programFiles = Files.createTempDirectory("pf");
        Path exe = programFiles.resolve("Typora").resolve("Typora.exe");
        Files.createDirectories(exe.getParent());
        Files.writeString(exe, "mz");

        List<String> result = AIWorkspaceSilentLauncher.buildSilentCommand(
                "typora.exe", List.of("note.md"), true, false, List.of(), List.of(programFiles), "cmd.exe");

        assertEquals(2, result.size());
        assertTrue(Files.isSameFile(Path.of(result.get(0)), exe));
        assertEquals("note.md", result.get(1));
    }

    @Test
    public void macFallsBackToOpenWhenAppNotOnPath() {
        List<String> result = AIWorkspaceSilentLauncher.buildSilentCommand(
                "typora", List.of("/tmp/note.md"), false, true, List.of(), List.of(), "cmd.exe");

        assertEquals(List.of("open", "-a", "Typora", "/tmp/note.md"), result);
    }

    @Test
    public void macUsesPathBinaryWhenPresent() throws Exception {
        Path dir = Files.createTempDirectory("mac-path");
        Path binary = dir.resolve("cursor");
        Files.writeString(binary, "#!/bin/sh\n");

        List<String> result = AIWorkspaceSilentLauncher.buildSilentCommand(
                "cursor", List.of("."), false, true, List.of(dir.toString()), List.of(), "cmd.exe");

        assertEquals(List.of(binary.toString(), "."), result);
    }

    @Test
    public void windowsFindsCursorCmdUnderProgramFilesBin() throws Exception {
        Path programFiles = Files.createTempDirectory("pf-cursor");
        Path bin = programFiles.resolve("cursor").resolve("resources").resolve("app").resolve("bin");
        Files.createDirectories(bin);
        Files.writeString(bin.resolve("cursor"), "#!/bin/sh\n");
        Path cmd = bin.resolve("cursor.cmd");
        Files.writeString(cmd, "@echo off\n");

        List<String> result = AIWorkspaceSilentLauncher.buildSilentCommand(
                "cursor", List.of("."), true, false, List.of(), List.of(programFiles), "cmd.exe");

        assertEquals(List.of("cmd.exe", "/c", cmd.toString(), "."), result);
    }

    @Test
    public void windowsQuotedExeGetsPowerShellCallOperator() {
        assertEquals("& \"C:\\Program Files\\Typora\\Typora.exe\" note.md",
                AIWorkspaceCommandService.joinQuotedForTerminal(
                        List.of("C:\\Program Files\\Typora\\Typora.exe", "note.md"), true));
    }

    @Test
    public void missingFileSourceDefaultsToProject() {
        assertEquals("project", AIWorkspaceCommandService.resolveFileSource(null));
        assertEquals("project", AIWorkspaceCommandService.resolveFileSource(""));
        assertEquals("project", AIWorkspaceCommandService.resolveFileSource("project"));
        assertEquals("project", AIWorkspaceCommandService.resolveFileSource("tree"));
        assertEquals("editor", AIWorkspaceCommandService.resolveFileSource("editor"));
        assertEquals("editor", AIWorkspaceCommandService.resolveFileSource("EDITOR"));
    }

    @Test
    public void unquotedProgramKeepsPlainInvocation() {
        assertEquals("cursor .",
                AIWorkspaceCommandService.joinQuotedForTerminal(List.of("cursor", "."), true));
    }

    @Test
    public void seedsOnlyWhenWorkspaceExistsAndCommandFileIsMissing() {
        assertTrue(AIWorkspaceCommandService.shouldSeedBuiltIns(true, false));
        assertFalse(AIWorkspaceCommandService.shouldSeedBuiltIns(true, true));
        assertFalse(AIWorkspaceCommandService.shouldSeedBuiltIns(false, false));
        assertFalse(AIWorkspaceCommandService.shouldSeedBuiltIns(false, true));
    }

    @Test
    public void builtInCommandsAreSilentCursorAndTypora() throws Exception {
        List<AIWorkspaceCommand> commands = AIWorkspaceCommandService.builtInCommands(
                "Launch Cursor for this project", "Open selected Markdown in Typora");
        assertEquals(2, commands.size());

        AIWorkspaceCommand cursor = commands.get(0);
        assertEquals(AIWorkspaceCommandService.ID_LAUNCH_CURSOR, cursor.id);
        assertEquals("Launch Cursor for this project", cursor.displayName);
        assertEquals("Launch Cursor for this project", cursor.name);
        assertEquals("cursor", cursor.executable);
        assertEquals(List.of("."), cursor.args);
        assertEquals("silent", cursor.executionMode);
        assertFalse(cursor.openTerminal);
        assertFalse(cursor.confirmEachTime);
        assertTrue(cursor.enabled);
        assertEquals("project", cursor.fileSource);

        AIWorkspaceCommand typora = commands.get(1);
        assertEquals(AIWorkspaceCommandService.ID_OPEN_TYPORA, typora.id);
        assertEquals("Open selected Markdown in Typora", typora.displayName);
        assertEquals("typora", typora.executable);
        assertEquals(List.of("$FilePath$"), typora.args);
        assertEquals("silent", typora.executionMode);
        assertFalse(typora.openTerminal);
        assertFalse(typora.confirmEachTime);
        assertEquals("project", typora.fileSource);

        Path root = Files.createTempDirectory("builtin-commands");
        for (AIWorkspaceCommand command : commands) {
            AIWorkspaceCommandService.validate(command, root);
        }
    }

    @Test
    public void guiCommandsDetachAndBatCommandsWait() {
        assertTrue(AIWorkspaceSilentLauncher.shouldDetach(
                List.of("cmd.exe", "/c", "C:\\cursor.cmd", "."), true, false));
        assertTrue(AIWorkspaceSilentLauncher.shouldDetach(
                List.of("C:\\Program Files\\Typora\\Typora.exe", "note.md"), true, false));
        assertTrue(AIWorkspaceSilentLauncher.shouldDetach(List.of("open", "-a", "Cursor", "."), false, true));
        assertFalse(AIWorkspaceSilentLauncher.shouldDetach(
                List.of("cmd.exe", "/c", "gradlew.bat", "buildPlugin"), true, false));
    }

    private static AIWorkspaceCommand command(String executable, List<String> args) {
        AIWorkspaceCommand command = new AIWorkspaceCommand();
        command.executable = executable;
        command.args = args;
        return command;
    }
}

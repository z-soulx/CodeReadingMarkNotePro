package jp.kitabatakep.intellij.plugins.codereadingnote.workspace;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.intellij.ide.DataManager;
import com.intellij.ide.projectView.ProjectView;
import com.intellij.ide.projectView.impl.AbstractProjectViewPane;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import jp.kitabatakep.intellij.plugins.codereadingnote.CodeReadingNoteBundle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Service(Service.Level.PROJECT)
public final class AIWorkspaceCommandService {
    static final String ID_LAUNCH_CURSOR = "launch-cursor-project";
    static final String ID_OPEN_TYPORA = "open-selected-md-in-typora";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Pattern SHELL = Pattern.compile("[;&|<>`$()\\r\\n]");
    private final Project project;
    public AIWorkspaceCommandService(@NotNull Project project) { this.project = project; }
    @NotNull public static AIWorkspaceCommandService getInstance(@NotNull Project project) { return project.getService(AIWorkspaceCommandService.class); }
    @NotNull private Path file() { return AIWorkspaceService.getInstance(project).getWorkspaceRoot().resolve("workspace-commands.json"); }

    /**
     * Writes the two first-run commands only when {@code .ai/} exists and
     * {@code workspace-commands.json} does not. An existing file (including
     * {@code commands: []}) is never merged or rewritten.
     */
    public void ensureSeeded() throws IOException {
        Path root = AIWorkspaceService.getInstance(project).getWorkspaceRoot();
        if (!shouldSeedBuiltIns(Files.isDirectory(root), Files.exists(file()))) return;
        save(builtInCommands());
    }

    @NotNull public List<AIWorkspaceCommand> load() throws IOException {
        ensureSeeded();
        if (!Files.exists(file())) return new ArrayList<>();
        WorkspaceCommands data = GSON.fromJson(Files.readString(file(), StandardCharsets.UTF_8), WorkspaceCommands.class);
        if (data == null || data.commands == null) return new ArrayList<>();
        if (data.schemaVersion != 0 && data.schemaVersion != 1) throw new IOException("Unsupported workspace command schema version: " + data.schemaVersion);
        return data.commands;
    }
    public void save(@NotNull List<AIWorkspaceCommand> commands) throws IOException {
        Files.createDirectories(file().getParent());
        WorkspaceCommands data = new WorkspaceCommands();
        data.schemaVersion = 1;
        data.commands = commands;
        Files.writeString(file(), GSON.toJson(data), StandardCharsets.UTF_8);
        refreshWrittenFile();
    }

    private void refreshWrittenFile() {
        VirtualFile vf = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file().toFile());
        if (vf == null) return;
        ApplicationManager.getApplication().invokeLater(() -> {
            var doc = FileDocumentManager.getInstance().getCachedDocument(vf);
            if (doc != null) FileDocumentManager.getInstance().reloadFromDisk(doc);
            else vf.refresh(false, false);
        }, ModalityState.any());
    }
    public void upsert(@NotNull AIWorkspaceCommand command) throws IOException {
        validate(command, AIWorkspaceService.getInstance(project).getProjectRoot());
        List<AIWorkspaceCommand> commands = load();
        commands.removeIf(existing -> command.id.equals(existing.id));
        commands.add(command);
        save(commands);
    }
    public boolean delete(@NotNull String id) throws IOException {
        List<AIWorkspaceCommand> commands = load();
        boolean removed = commands.removeIf(command -> id.equals(command.id));
        if (removed) save(commands);
        return removed;
    }
    public AIWorkspaceCommand findById(@NotNull String id) throws IOException {
        return load().stream().filter(command -> id.equals(command.id)).findFirst().orElse(null);
    }
    public static void validate(@NotNull AIWorkspaceCommand command, @NotNull Path projectRoot) {
        String displayName = command.displayName == null || command.displayName.isBlank() ? command.name : command.displayName;
        if (command.id == null || command.id.isBlank() || displayName == null || displayName.isBlank() || command.executable == null || command.executable.isBlank()) throw new IllegalArgumentException("Command id, display name, and executable are required");
        if (SHELL.matcher(command.executable).find()) throw new IllegalArgumentException("Executable contains shell control characters");
        if (command.args != null) for (String arg : command.args) if (arg != null && !isFileMacro(arg) && SHELL.matcher(arg).find()) throw new IllegalArgumentException("Arguments contain shell control characters");
        if (command.workingDirectory != null && !command.workingDirectory.isBlank()) { Path wd = projectRoot.resolve(command.workingDirectory).normalize(); if (!wd.startsWith(projectRoot) || !Files.isDirectory(wd)) throw new IllegalArgumentException("Working directory must be inside the project"); }
        Path executablePath = Path.of(command.executable);
        if (executablePath.isAbsolute() && !isExistingLaunchPath(executablePath)) throw new IllegalArgumentException("Executable path is not a file");
    }
    @NotNull public ExecutionResult execute(@NotNull AIWorkspaceCommand command) {
        try {
            validate(command, AIWorkspaceService.getInstance(project).getProjectRoot());
            String executable = command.executable.trim();
            if (executable.matches(".*\\s+.*")) {
                Path executablePath = Path.of(executable);
                if (!executablePath.isAbsolute() || !isExistingLaunchPath(executablePath)) {
                    return new ExecutionResult(-1, CodeReadingNoteBundle.message("aiworkspace.commands.silent.shell.only"));
                }
            }
            Path wd = command.workingDirectory == null || command.workingDirectory.isBlank() ? AIWorkspaceService.getInstance(project).getProjectRoot() : AIWorkspaceService.getInstance(project).getProjectRoot().resolve(command.workingDirectory).normalize();
            List<String> args = new ArrayList<>();
            if (command.args != null) for (String arg : command.args) args.add(resolveMacro(arg, command));
            List<String> cmd = AIWorkspaceSilentLauncher.buildSilentCommand(
                    executable, args, SystemInfo.isWindows, SystemInfo.isMac, pathEntries(), platformSearchRoots(),
                    System.getenv().getOrDefault("ComSpec", "cmd.exe"));
            if (cmd == null || cmd.isEmpty()) {
                return new ExecutionResult(-1, CodeReadingNoteBundle.message("aiworkspace.commands.silent.not.found", executable));
            }
            boolean detach = AIWorkspaceSilentLauncher.shouldDetach(cmd, SystemInfo.isWindows, SystemInfo.isMac);
            ProcessBuilder builder = new ProcessBuilder(cmd).directory(wd.toFile());
            if (detach) {
                builder.redirectOutput(ProcessBuilder.Redirect.DISCARD);
                builder.redirectError(ProcessBuilder.Redirect.DISCARD);
                Process process = builder.start();
                boolean finished = process.waitFor(3, TimeUnit.SECONDS);
                if (finished && process.exitValue() != 0) {
                    return new ExecutionResult(process.exitValue(), CodeReadingNoteBundle.message("aiworkspace.commands.silent.exited", String.valueOf(process.exitValue())));
                }
                return new ExecutionResult(0, "");
            }
            Process process = builder.redirectErrorStream(true).start();
            byte[] bytes = process.getInputStream().readNBytes(12000);
            int code = process.waitFor();
            return new ExecutionResult(code, new String(bytes, StandardCharsets.UTF_8));
        } catch (Exception e) {
            String detail = e.getMessage() == null ? CodeReadingNoteBundle.message("aiworkspace.commands.silent.failed") : e.getMessage();
            return new ExecutionResult(-1, detail);
        }
    }

    @NotNull public ExecutionResult runConfigured(@NotNull AIWorkspaceCommand command) {
        boolean silent = "silent".equalsIgnoreCase(command.executionMode);
        return sendToTerminal(command, !silent);
    }

    private static boolean isExistingLaunchPath(@NotNull Path path) {
        if (AIWorkspaceSilentLauncher.isMacAppBundle(path)) return true;
        if (Files.isRegularFile(path)) return true;
        return AIWorkspaceSilentLauncher.preferWindowsImage(path) != null;
    }

    @NotNull private List<String> pathEntries() {
        String path = System.getenv("PATH");
        if (path == null || path.isBlank()) return List.of();
        return List.of(path.split(java.util.regex.Pattern.quote(java.io.File.pathSeparator)));
    }

    @NotNull private List<Path> platformSearchRoots() {
        List<Path> roots = new ArrayList<>();
        if (SystemInfo.isWindows) {
            addEnvDir(roots, "ProgramFiles");
            addEnvDir(roots, "ProgramFiles(x86)");
            String local = System.getenv("LOCALAPPDATA");
            if (local != null && !local.isBlank()) roots.add(Path.of(local, "Programs"));
        } else if (SystemInfo.isMac) {
            roots.add(Path.of("/Applications"));
            roots.add(Path.of(System.getProperty("user.home"), "Applications"));
        }
        return roots;
    }

    private static void addEnvDir(@NotNull List<Path> roots, @NotNull String key) {
        String value = System.getenv(key);
        if (value != null && !value.isBlank()) roots.add(Path.of(value));
    }
    /** Sends the command to an IDEA Terminal tab instead of spawning a detached process. */
    @NotNull public ExecutionResult sendToTerminal(@NotNull AIWorkspaceCommand command) {
        return sendToTerminal(command, command.openTerminal);
    }

    @NotNull public ExecutionResult sendToTerminal(@NotNull AIWorkspaceCommand command, boolean activateTerminal) {
        try {
            validate(command, AIWorkspaceService.getInstance(project).getProjectRoot());
            Path root = AIWorkspaceService.getInstance(project).getProjectRoot();
            Path wd = command.workingDirectory == null || command.workingDirectory.isBlank() ? root : root.resolve(command.workingDirectory).normalize();
            String title = command.displayName == null || command.displayName.isBlank() ? command.name : command.displayName;
            String shellCommand = buildTerminalCommandForProject(command, wd, SystemInfo.isWindows);

            Class<?> managerClass = Class.forName("org.jetbrains.plugins.terminal.TerminalToolWindowManager");
            Object manager = managerClass.getMethod("getInstance", Project.class).invoke(null, project);
            Object widget;
            try {
                // Always start the session immediately. Silent used to pass
                // deferSessionStartUntilShown=true, so the shell never ran until the
                // Terminal tool window was opened.
                widget = managerClass.getMethod("createLocalShellWidget", String.class, String.class, boolean.class, boolean.class)
                    .invoke(manager, wd.toString(), title, activateTerminal, false);
            } catch (NoSuchMethodException ignored) {
                widget = managerClass.getMethod("createLocalShellWidget", String.class, String.class).invoke(manager, wd.toString(), title);
            }
            java.lang.reflect.Method execute = widget.getClass().getMethod("executeCommand", String.class);
            try {
                execute.invoke(widget, shellCommand);
            } catch (Exception first) {
                Object terminalWidget = widget;
                ApplicationManager.getApplication().invokeLater(() -> {
                    try {
                        execute.invoke(terminalWidget, shellCommand);
                    } catch (Exception e) {
                        String detail = e.getCause() == null ? e.getMessage() : e.getCause().getMessage();
                        Messages.showErrorDialog(project,
                                detail == null ? CodeReadingNoteBundle.message("aiworkspace.commands.silent.failed") : detail,
                                CodeReadingNoteBundle.message("aiworkspace.error.title"));
                    }
                }, ModalityState.any());
            }
            return new ExecutionResult(0, "");
        } catch (ClassNotFoundException e) {
            return new ExecutionResult(-1, CodeReadingNoteBundle.message("aiworkspace.commands.terminal.missing"));
        } catch (Exception e) {
            return new ExecutionResult(-1, e.getCause() == null ? e.getMessage() : e.getCause().getMessage());
        }
    }

    /**
     * Builds the command line sent to the terminal shell. Windows Terminal's
     * {@code wt.exe} does not reliably inherit the caller's directory on
     * Windows 10 (especially when reusing an existing window), so pass the
     * directory explicitly with {@code -d}.
     */
    static String buildTerminalCommand(@NotNull AIWorkspaceCommand command,
                                       @NotNull Path workingDirectory,
                                       boolean windows) {
        List<String> tokens = new ArrayList<>();
        tokens.add(quoteForShell(command.executable));
        if (windows && isWindowsTerminalExecutable(command.executable)) {
            tokens.add("-d");
            tokens.add(quoteForShell(workingDirectory.toString()));
        }
        if (command.args != null) {
            for (String arg : command.args) tokens.add(quoteForShell(arg));
        }
        return String.join(" ", tokens);
    }

    private String buildTerminalCommandForProject(@NotNull AIWorkspaceCommand command, @NotNull Path workingDirectory, boolean windows) {
        List<String> args = new ArrayList<>();
        if (command.args != null) for (String arg : command.args) args.add(resolveMacro(arg, command));
        String executable = command.executable.trim();
        boolean shellBuiltin = executable.matches(".*\\s+.*") && !Path.of(executable).isAbsolute();
        List<String> resolved = shellBuiltin ? null : AIWorkspaceSilentLauncher.buildSilentCommand(
                executable, args, windows, SystemInfo.isMac, pathEntries(), platformSearchRoots(),
                System.getenv().getOrDefault("ComSpec", "cmd.exe"));
        if (resolved != null && !resolved.isEmpty()) {
            if (windows && isWindowsTerminalExecutable(executable)) {
                resolved = new ArrayList<>(resolved);
                resolved.add(1, "-d");
                resolved.add(2, workingDirectory.toString());
            }
            return joinQuotedForTerminal(resolved, windows);
        }
        List<String> tokens = new ArrayList<>();
        tokens.add(executable);
        if (windows && isWindowsTerminalExecutable(executable)) {
            tokens.add("-d");
            tokens.add(workingDirectory.toString());
        }
        tokens.addAll(args);
        return joinQuotedForTerminal(tokens, windows);
    }

    static String joinQuotedForTerminal(@NotNull List<String> tokens, boolean windows) {
        if (tokens.isEmpty()) return "";
        List<String> quoted = new ArrayList<>();
        for (String token : tokens) quoted.add(quoteForShell(token));
        if (windows && quoted.get(0).startsWith("\"")) quoted.set(0, "& " + quoted.get(0));
        return String.join(" ", quoted);
    }

    private static boolean isWindowsTerminalExecutable(@NotNull String executable) {
        String normalized = executable.trim().replace('\\', '/');
        int slash = normalized.lastIndexOf('/');
        String name = slash >= 0 ? normalized.substring(slash + 1) : normalized;
        return "wt".equalsIgnoreCase(name) || "wt.exe".equalsIgnoreCase(name);
    }

    private static boolean isFileMacro(String value) {
        return "$FilePath$".equals(value) || "$FileDir$".equals(value) || "$FileName$".equals(value);
    }

    private String resolveMacro(String value, @NotNull AIWorkspaceCommand command) {
        if (!isFileMacro(value)) return value;
        VirtualFile file = resolveTargetFile(command);
        if ("$FilePath$".equals(value)) return localPath(file);
        if ("$FileDir$".equals(value)) return file.getParent() == null ? localPath(file) : localPath(file.getParent());
        return file.getName();
    }

    @NotNull private static String localPath(@NotNull VirtualFile file) {
        return FileUtil.toSystemDependentName(file.getPath());
    }

    @NotNull private VirtualFile resolveTargetFile(@NotNull AIWorkspaceCommand command) {
        if ("editor".equalsIgnoreCase(resolveFileSource(command.fileSource))) {
            VirtualFile editor = selectedEditorFile();
            if (editor == null) throw new IllegalArgumentException(CodeReadingNoteBundle.message("aiworkspace.commands.file.none.editor"));
            return editor;
        }
        VirtualFile selected = selectedProjectFile();
        if (selected != null) return selected;
        VirtualFile editor = selectedEditorFile();
        if (editor != null) return editor;
        throw new IllegalArgumentException(CodeReadingNoteBundle.message("aiworkspace.commands.file.none.project"));
    }

    public static String resolveFileSource(String fileSource) {
        if (fileSource == null || fileSource.isBlank()) return "project";
        return "editor".equalsIgnoreCase(fileSource.trim()) ? "editor" : "project";
    }

    static boolean shouldSeedBuiltIns(boolean workspaceDirExists, boolean commandsFileExists) {
        return workspaceDirExists && !commandsFileExists;
    }

    @NotNull static List<AIWorkspaceCommand> builtInCommands() {
        return builtInCommands(
                CodeReadingNoteBundle.message("aiworkspace.commands.builtin.cursor"),
                CodeReadingNoteBundle.message("aiworkspace.commands.builtin.typora"));
    }

    @NotNull static List<AIWorkspaceCommand> builtInCommands(@NotNull String cursorName, @NotNull String typoraName) {
        List<AIWorkspaceCommand> commands = new ArrayList<>();
        commands.add(silentGuiCommand(ID_LAUNCH_CURSOR, cursorName, "cursor", List.of("."), "project"));
        commands.add(silentGuiCommand(ID_OPEN_TYPORA, typoraName, "typora", List.of("$FilePath$"), "project"));
        return commands;
    }

    @NotNull private static AIWorkspaceCommand silentGuiCommand(
            @NotNull String id, @NotNull String name, @NotNull String executable,
            @NotNull List<String> args, @NotNull String fileSource) {
        AIWorkspaceCommand command = new AIWorkspaceCommand();
        command.id = id;
        command.name = name;
        command.displayName = name;
        command.executable = executable;
        command.args = new ArrayList<>(args);
        command.workingDirectory = "";
        command.enabled = true;
        command.openTerminal = false;
        command.executionMode = "silent";
        command.fileSource = fileSource;
        command.confirmEachTime = false;
        return command;
    }

    @Nullable private VirtualFile selectedProjectFile() {
        try {
            AbstractProjectViewPane pane = ProjectView.getInstance(project).getCurrentProjectViewPane();
            if (pane == null || pane.getTree() == null) return null;
            DataContext context = DataManager.getInstance().getDataContext(pane.getTree());
            VirtualFile[] files = CommonDataKeys.VIRTUAL_FILE_ARRAY.getData(context);
            if (files != null && files.length > 0) return files[0];
            return CommonDataKeys.VIRTUAL_FILE.getData(context);
        } catch (Exception ignored) {
            return null;
        }
    }

    @Nullable private VirtualFile selectedEditorFile() {
        VirtualFile[] files = FileEditorManager.getInstance(project).getSelectedFiles();
        return files.length == 0 ? null : files[0];
    }

    @NotNull private static String quoteForShell(String token) {
        if (token != null && token.matches("[A-Za-z0-9_./\\\\:=@%+,-]+")) return token;
        // Allow users to enter a shell built-in/pipeline command (for example `cd ai3`)
        // in the executable field. Paths containing separators remain quoted safely.
        if (token != null && token.matches("[A-Za-z_][A-Za-z0-9_-]*(\\s+[A-Za-z0-9_./:=@%+,-]+)+")) return token;
        String value = token == null ? "" : token.replace("\"", "\\\"");
        return "\"" + value + "\"";
    }
    public record ExecutionResult(int exitCode, String output) { }
    private static class WorkspaceCommands { int schemaVersion = 1; List<AIWorkspaceCommand> commands = new ArrayList<>(); }
}

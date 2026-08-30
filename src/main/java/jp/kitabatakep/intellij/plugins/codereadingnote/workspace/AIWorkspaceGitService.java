package jp.kitabatakep.intellij.plugins.codereadingnote.workspace;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** Git operations restricted to the .ai work tree. Call from a background thread. */
@Service(Service.Level.PROJECT)
public final class AIWorkspaceGitService {
    private static final String GITIGNORE = "token.txt\nruns/\n*.tmp\n*.log\n.git/\n";
    private final Project project;

    public AIWorkspaceGitService(@NotNull Project project) { this.project = project; }
    @NotNull public static AIWorkspaceGitService getInstance(@NotNull Project project) { return project.getService(AIWorkspaceGitService.class); }
    @NotNull private Path root() { return AIWorkspaceService.getInstance(project).getWorkspaceRoot(); }
    public boolean isInitialized() { return Files.isDirectory(root().resolve(".git")); }

    public GitResult initialize() {
        try {
            Files.createDirectories(root());
            Path ignore = root().resolve(".gitignore");
            if (!Files.exists(ignore)) Files.writeString(ignore, GITIGNORE, StandardCharsets.UTF_8);
            return run("init");
        } catch (Exception e) { return GitResult.failure(e.getMessage()); }
    }

    @NotNull public GitStatus status() {
        if (!isInitialized()) return new GitStatus(false, false, "");
        GitResult result = run("status", "--porcelain");
        return new GitStatus(true, result.success && !result.output.isBlank(), result.output);
    }

    public GitResult stageAll() { return isInitialized() ? run("add", "-A") : GitResult.failure("Git repository is not initialized"); }

    public GitResult commit(@NotNull String message) {
        if (!isInitialized()) return GitResult.failure("Git repository is not initialized");
        GitStatus status = status();
        if (!status.hasChanges()) return GitResult.noChanges();
        GitResult staged = stageAll();
        return staged.success ? run("commit", "-m", message) : staged;
    }

    @NotNull private GitResult run(String... args) {
        try {
            List<String> command = new ArrayList<>(); command.add("git"); command.addAll(List.of(args));
            Process process = new ProcessBuilder(command).directory(root().toFile()).redirectErrorStream(true).start();
            StringBuilder out = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line; while ((line = reader.readLine()) != null) { if (out.length() < 8000) out.append(line).append('\n'); }
            }
            boolean finished = process.waitFor(30, TimeUnit.SECONDS);
            if (!finished) { process.destroyForcibly(); return GitResult.failure("Git command timed out"); }
            int code = process.exitValue();
            return code == 0 ? GitResult.success(out.toString().trim()) : GitResult.failure(out.toString().trim());
        } catch (Exception e) { return GitResult.failure(e.getMessage()); }
    }

    public record GitStatus(boolean initialized, boolean hasChanges, String details) { }
    public static final class GitResult {
        public final boolean success; public final boolean noChanges; public final String output;
        private GitResult(boolean success, boolean noChanges, String output) { this.success = success; this.noChanges = noChanges; this.output = output == null ? "" : output; }
        public static GitResult success(String output) { return new GitResult(true, false, output); }
        public static GitResult failure(String output) { return new GitResult(false, false, output); }
        public static GitResult noChanges() { return new GitResult(true, true, ""); }
    }
}

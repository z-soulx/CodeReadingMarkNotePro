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
    static final String PARENT_IGNORE_RULE = "/.ai/";
    static final String PARENT_IGNORE_COMMENT = "# Code Reading Mark Note Pro: .ai is an independent Git workspace";
    private final Project project;

    public AIWorkspaceGitService(@NotNull Project project) { this.project = project; }
    @NotNull public static AIWorkspaceGitService getInstance(@NotNull Project project) { return project.getService(AIWorkspaceGitService.class); }
    @NotNull private Path root() { return AIWorkspaceService.getInstance(project).getWorkspaceRoot(); }
    public boolean isInitialized() { return Files.isDirectory(root().resolve(".git")); }

    public GitResult initialize() {
        try {
            Files.createDirectories(root());
            AIWorkspaceCommandService.getInstance(project).ensureSeeded();
            Path ignore = root().resolve(".gitignore");
            if (!Files.exists(ignore)) Files.writeString(ignore, GITIGNORE, StandardCharsets.UTF_8);
            GitResult isolation = ensureParentIsolation();
            if (!isolation.success) return isolation;
            GitResult init = run("init");
            if (!init.success) return init;
            return isolation.untrackedFromParent ? isolation : init;
        } catch (Exception e) { return GitResult.failure(e.getMessage()); }
    }

    public GitResult ensureParentIsolation() {
        try {
            ensureParentGitignore();
            return untrackFromParent();
        } catch (Exception e) { return GitResult.failure(e.getMessage()); }
    }

    static boolean isPathUnder(@NotNull Path root, @NotNull Path candidate) {
        Path normalizedRoot = root.toAbsolutePath().normalize();
        Path normalizedCandidate = candidate.toAbsolutePath().normalize();
        return normalizedCandidate.startsWith(normalizedRoot);
    }

    static boolean hasParentIgnoreRule(@NotNull String content) {
        for (String line : content.split("\\R", -1)) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
            if (PARENT_IGNORE_RULE.equals(trimmed) || "/.ai".equals(trimmed)
                    || ".ai/".equals(trimmed) || ".ai".equals(trimmed)) {
                return true;
            }
        }
        return false;
    }

    @NotNull
    static String withParentIgnoreRule(@NotNull String content) {
        if (hasParentIgnoreRule(content)) return content;
        StringBuilder sb = new StringBuilder(content);
        if (!content.isEmpty() && !content.endsWith("\n")) sb.append('\n');
        if (!content.isEmpty()) sb.append('\n');
        sb.append(PARENT_IGNORE_COMMENT).append('\n');
        sb.append(PARENT_IGNORE_RULE).append('\n');
        return sb.toString();
    }

    private void ensureParentGitignore() throws Exception {
        Path gitignore = AIWorkspaceService.getInstance(project).getProjectRoot().resolve(".gitignore");
        String existing = Files.exists(gitignore) ? Files.readString(gitignore, StandardCharsets.UTF_8) : "";
        String updated = withParentIgnoreRule(existing);
        if (!updated.equals(existing)) Files.writeString(gitignore, updated, StandardCharsets.UTF_8);
    }

    @NotNull
    GitResult untrackFromParent() {
        Path projectRoot = AIWorkspaceService.getInstance(project).getProjectRoot();
        GitResult inside = runIn(projectRoot, "rev-parse", "--is-inside-work-tree");
        if (!inside.success) {
            String detail = inside.output == null ? "" : inside.output.toLowerCase();
            if (detail.contains("not a git repository")) return GitResult.success("");
            return inside;
        }
        GitResult listed = runIn(projectRoot, "ls-files", "--", ".ai");
        if (!listed.success) return listed;
        if (listed.output.isBlank()) return GitResult.success("");
        GitResult removed = runIn(projectRoot, "rm", "-r", "--cached", "--ignore-unmatch", "--", ".ai");
        return removed.success ? GitResult.untrackedFromParent(listed.output) : removed;
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

    @NotNull private GitResult run(String... args) { return runIn(root(), args); }

    @NotNull private GitResult runIn(@NotNull Path cwd, String... args) {
        try {
            List<String> command = new ArrayList<>(); command.add("git"); command.addAll(List.of(args));
            Process process = new ProcessBuilder(command).directory(cwd.toFile()).redirectErrorStream(true).start();
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
        public final boolean success; public final boolean noChanges; public final boolean untrackedFromParent; public final String output;
        private GitResult(boolean success, boolean noChanges, boolean untrackedFromParent, String output) {
            this.success = success; this.noChanges = noChanges; this.untrackedFromParent = untrackedFromParent; this.output = output == null ? "" : output;
        }
        public static GitResult success(String output) { return new GitResult(true, false, false, output); }
        public static GitResult failure(String output) { return new GitResult(false, false, false, output); }
        public static GitResult noChanges() { return new GitResult(true, true, false, ""); }
        public static GitResult untrackedFromParent(String output) { return new GitResult(true, false, true, output == null ? "" : output); }
    }
}

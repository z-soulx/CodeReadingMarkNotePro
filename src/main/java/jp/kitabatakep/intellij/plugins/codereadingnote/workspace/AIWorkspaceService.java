package jp.kitabatakep.intellij.plugins.codereadingnote.workspace;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.util.messages.Topic;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;

/** Project-level AI workspace settings and path resolution. */
@Service(Service.Level.PROJECT)
@State(name = "AIWorkspaceService", storages = @Storage("aiWorkspace.xml"))
public final class AIWorkspaceService implements PersistentStateComponent<AIWorkspaceService.PersistentState> {
    public static final String DEFAULT_DOCS_PATH = ".ai/docs";
    public static final Topic<AIWorkspaceNotifier> TOPIC =
            Topic.create("AI workspace changed", AIWorkspaceNotifier.class);

    private final Project project;
    private PersistentState state = new PersistentState();

    public AIWorkspaceService(@NotNull Project project) { this.project = project; }

    @NotNull
    public static AIWorkspaceService getInstance(@NotNull Project project) {
        return project.getService(AIWorkspaceService.class);
    }

    @NotNull public Path getProjectRoot() {
        String base = project.getBasePath();
        return base == null ? Path.of(".").toAbsolutePath().normalize() : Path.of(base).toAbsolutePath().normalize();
    }

    @NotNull public Path getWorkspaceRoot() { return getProjectRoot().resolve(".ai"); }

    @NotNull public String getDocsRelativePath() {
        return state.docsRelativePath == null || state.docsRelativePath.isBlank() ? DEFAULT_DOCS_PATH : state.docsRelativePath;
    }

    @NotNull public Path getDocsRoot() { return getProjectRoot().resolve(getDocsRelativePath()).normalize(); }

    public void setDocsRelativePath(@NotNull String relativePath) {
        String normalized = relativePath.replace('\\', '/').trim();
        if (normalized.isBlank()) normalized = DEFAULT_DOCS_PATH;
        Path candidate = getProjectRoot().resolve(normalized).normalize();
        if (!candidate.startsWith(getProjectRoot())) throw new IllegalArgumentException("Docs path must be inside project");
        state.docsRelativePath = normalized;
        project.getMessageBus().syncPublisher(TOPIC).workspaceChanged();
    }

    public void ensureDocsDirectory() throws java.io.IOException { Files.createDirectories(getDocsRoot()); }

    @Override public PersistentState getState() { return state; }
    @Override public void loadState(@NotNull PersistentState state) { XmlSerializerUtil.copyBean(state, this.state); }

    public static class PersistentState { public String docsRelativePath = DEFAULT_DOCS_PATH; }
}

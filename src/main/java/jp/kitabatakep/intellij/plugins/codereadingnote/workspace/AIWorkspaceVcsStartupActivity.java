package jp.kitabatakep.intellij.plugins.codereadingnote.workspace;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;

/** After project open, restore an explicitly initialized .ai Git mapping. */
public final class AIWorkspaceVcsStartupActivity implements ProjectActivity {
    private static final Logger LOG = Logger.getInstance(AIWorkspaceVcsStartupActivity.class);

    @Override
    public @Nullable Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                if (project.isDisposed()) return;
                if (!Files.isDirectory(AIWorkspaceService.getInstance(project).getWorkspaceRoot())) return;
                try {
                    AIWorkspaceCommandService.getInstance(project).ensureSeeded();
                } catch (Exception e) {
                    LOG.warn("Failed to seed built-in workspace commands", e);
                }
                AIWorkspaceGitService git = AIWorkspaceGitService.getInstance(project);
                boolean nested = git.isInitialized();
                ApplicationManager.getApplication().invokeLater(() -> {
                    if (project.isDisposed()) return;
                    if (nested) {
                        AIWorkspaceVcsSupport.ensureDirectoryMapping(project);
                        AIWorkspaceChangeListService.getInstance(project).sync();
                    }
                });
            } catch (Exception e) {
                LOG.warn("Failed to restore .ai Git mapping on startup", e);
            }
        });
        return Unit.INSTANCE;
    }
}

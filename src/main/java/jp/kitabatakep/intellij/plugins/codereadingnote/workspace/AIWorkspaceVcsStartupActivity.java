package jp.kitabatakep.intellij.plugins.codereadingnote.workspace;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import com.intellij.openapi.vcs.changes.VcsDirtyScopeManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import jp.kitabatakep.intellij.plugins.codereadingnote.CodeReadingNoteBundle;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;

/** After project open, isolate .ai from the parent Git root and restore the nested mapping. */
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
                AIWorkspaceGitService.GitResult isolation = git.ensureParentIsolation();
                if (!isolation.success) {
                    LOG.warn("Failed to isolate .ai from the parent Git repository: " + isolation.output);
                    return;
                }
                boolean untracked = isolation.untrackedFromParent;
                boolean nested = git.isInitialized();
                ApplicationManager.getApplication().invokeLater(() -> {
                    if (project.isDisposed()) return;
                    AIWorkspaceVcsSupport.refreshParentVcs(project);
                    if (untracked) AIWorkspaceVcsSupport.notifyParentUntracked(project);
                    if (nested) {
                        AIWorkspaceVcsSupport.ensureDirectoryMapping(project);
                        AIWorkspaceChangeListService.getInstance(project).sync();
                    }
                });
            } catch (Exception e) {
                LOG.warn("Failed to isolate .ai Git on startup", e);
            }
        });
        return Unit.INSTANCE;
    }
}

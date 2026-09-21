package jp.kitabatakep.intellij.plugins.codereadingnote.workspace;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.ActionUiKind;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vcs.VcsDirectoryMapping;
import com.intellij.openapi.vcs.changes.VcsDirtyScopeManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;
import jp.kitabatakep.intellij.plugins.codereadingnote.CodeReadingNoteBundle;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Registers .ai as an IDEA Git root and opens the native Commit UI. */
public final class AIWorkspaceVcsSupport {
    private static final Logger LOG = Logger.getInstance(AIWorkspaceVcsSupport.class);
    private static final String GIT_VCS_NAME = "Git";
    private static final String CHECKIN_FILES_ACTION = "CheckinFiles";
    private static final String COMMIT_TOOL_WINDOW = "Commit";

    private AIWorkspaceVcsSupport() { }

    public static void ensureDirectoryMapping(@NotNull Project project) {
        if (project.isDisposed()) return;
        Path root = AIWorkspaceService.getInstance(project).getWorkspaceRoot();
        if (!AIWorkspaceGitService.getInstance(project).isInitialized()) return;

        VirtualFile vf = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(root);
        String path = vf != null ? vf.getPath() : FileUtil.toSystemIndependentName(root.toAbsolutePath().normalize().toString());

        ProjectLevelVcsManager vcsManager = ProjectLevelVcsManager.getInstance(project);
        if (vcsManager.findVcsByName(GIT_VCS_NAME) == null) {
            LOG.warn("Git VCS is not available; skipping .ai directory mapping");
            return;
        }
        for (VcsDirectoryMapping mapping : vcsManager.getDirectoryMappings()) {
            if (FileUtil.pathsEqual(mapping.getDirectory(), path)) return;
        }
        List<VcsDirectoryMapping> mappings = new ArrayList<>(vcsManager.getDirectoryMappings());
        mappings.add(new VcsDirectoryMapping(path, GIT_VCS_NAME));
        vcsManager.setDirectoryMappings(mappings);
    }

    public static void refreshParentVcs(@NotNull Project project) {
        if (project.isDisposed()) return;
        Path projectRoot = AIWorkspaceService.getInstance(project).getProjectRoot();
        VirtualFile vf = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(projectRoot);
        if (vf != null) VcsDirtyScopeManager.getInstance(project).dirDirtyRecursively(vf);
    }

    public static void notifyParentUntracked(@NotNull Project project) {
        Notification notification = new Notification(
                "CodeReadingNote.AIConfig",
                CodeReadingNoteBundle.message("aiworkspace.git.parent.untracked.title"),
                CodeReadingNoteBundle.message("aiworkspace.git.parent.untracked"),
                NotificationType.INFORMATION);
        Notifications.Bus.notify(notification, project);
    }

    public static boolean openCommitUi(@NotNull Project project) {
        if (project.isDisposed()) return false;
        Path root = AIWorkspaceService.getInstance(project).getWorkspaceRoot();
        VirtualFile vf = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(root);
        if (vf == null) return false;
        AIWorkspaceChangeListService.getInstance(project).sync();

        AnAction action = ActionManager.getInstance().getAction(CHECKIN_FILES_ACTION);
        if (action != null) {
            try {
                DataContext ctx = SimpleDataContext.builder()
                        .add(CommonDataKeys.PROJECT, project)
                        .add(CommonDataKeys.VIRTUAL_FILE, vf)
                        .add(CommonDataKeys.VIRTUAL_FILE_ARRAY, new VirtualFile[]{vf})
                        .build();
                AnActionEvent event = AnActionEvent.createEvent(ctx, null, ActionPlaces.UNKNOWN, ActionUiKind.NONE, null);
                action.actionPerformed(event);
                return true;
            } catch (Exception e) {
                LOG.warn("CheckinFiles failed for .ai, falling back to Commit tool window", e);
            }
        }

        ToolWindowManager twm = ToolWindowManager.getInstance(project);
        ToolWindow commit = twm.getToolWindow(COMMIT_TOOL_WINDOW);
        if (commit == null) commit = twm.getToolWindow(ToolWindowId.VCS);
        if (commit != null) {
            commit.activate(null);
            return true;
        }
        return false;
    }
}

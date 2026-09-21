package jp.kitabatakep.intellij.plugins.codereadingnote.workspace;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangeListAdapter;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.changes.ChangesUtil;
import com.intellij.openapi.vcs.changes.LocalChangeList;
import jp.kitabatakep.intellij.plugins.codereadingnote.CodeReadingNoteBundle;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/** Keeps .ai VCS changes on a dedicated changelist so project Default commits do not include them. */
@Service(Service.Level.PROJECT)
public final class AIWorkspaceChangeListService implements Disposable {
    private final Project project;
    private final AtomicBoolean syncing = new AtomicBoolean(false);

    public AIWorkspaceChangeListService(@NotNull Project project) {
        this.project = project;
        ChangeListManager.getInstance(project).addChangeListListener(new ChangeListAdapter() {
            @Override
            public void changeListUpdateDone() {
                sync();
            }
        }, this);
    }

    @NotNull
    public static AIWorkspaceChangeListService getInstance(@NotNull Project project) {
        return project.getService(AIWorkspaceChangeListService.class);
    }

    public void sync() {
        if (project.isDisposed()) return;
        if (!AIWorkspaceGitService.getInstance(project).isInitialized()) return;
        if (!syncing.compareAndSet(false, true)) return;
        try {
            Path aiRoot = AIWorkspaceService.getInstance(project).getWorkspaceRoot();
            ChangeListManager manager = ChangeListManager.getInstance(project);
            LocalChangeList aiList = ensureAiChangeList(manager);
            List<Change> toMove = new ArrayList<>();
            for (Change change : manager.getAllChanges()) {
                if (!isAiChange(change, aiRoot)) continue;
                LocalChangeList owner = manager.getChangeList(change);
                if (owner != null && aiList.getName().equals(owner.getName())) continue;
                toMove.add(change);
            }
            if (!toMove.isEmpty()) {
                manager.moveChangesTo(aiList, toMove.toArray(Change[]::new));
            }
            if (aiList.getName().equals(manager.getDefaultChangeList().getName())) {
                for (LocalChangeList other : manager.getChangeLists()) {
                    if (!aiList.getName().equals(other.getName())) {
                        manager.setDefaultChangeList(other);
                        break;
                    }
                }
            }
        } finally {
            syncing.set(false);
        }
    }

    @NotNull
    private static LocalChangeList ensureAiChangeList(@NotNull ChangeListManager manager) {
        String name = CodeReadingNoteBundle.message("aiworkspace.git.changelist");
        String comment = CodeReadingNoteBundle.message("aiworkspace.git.changelist.comment");
        LocalChangeList existing = manager.findChangeList(name);
        if (existing != null) return existing;
        return manager.addChangeList(name, comment);
    }

    private static boolean isAiChange(@NotNull Change change, @NotNull Path aiRoot) {
        FilePath path = ChangesUtil.getFilePath(change);
        if (path == null || path.getIOFile() == null) return false;
        return AIWorkspaceGitService.isPathUnder(aiRoot, path.getIOFile().toPath());
    }

    @Override
    public void dispose() { }
}

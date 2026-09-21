package jp.kitabatakep.intellij.plugins.codereadingnote.sync;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import jp.kitabatakep.intellij.plugins.codereadingnote.notesworkspace.WorkspaceNotesService;
import jp.kitabatakep.intellij.plugins.codereadingnote.sync.workspace.WorkspaceNotesSyncCoordinator;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SyncStartupActivity implements ProjectActivity {
    @Override public @Nullable Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        if (project.getBasePath() != null) {
            WorkspaceNotesService.getInstance(project);
            WorkspaceNotesSyncCoordinator.getInstance().attach(project);
        }
        return Unit.INSTANCE;
    }
}
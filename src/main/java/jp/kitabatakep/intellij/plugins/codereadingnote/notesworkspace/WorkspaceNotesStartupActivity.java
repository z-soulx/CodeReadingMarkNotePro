package jp.kitabatakep.intellij.plugins.codereadingnote.notesworkspace;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class WorkspaceNotesStartupActivity implements ProjectActivity {
    @Override public @Nullable Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        WorkspaceNotesService.getInstance(project).refresh();
        return Unit.INSTANCE;
    }
}

package jp.kitabatakep.intellij.plugins.codereadingnote.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import jp.kitabatakep.intellij.plugins.codereadingnote.*;
import jp.kitabatakep.intellij.plugins.codereadingnote.notesworkspace.WorkspaceNotesService;
import jp.kitabatakep.intellij.plugins.codereadingnote.sync.workspace.*;
import java.util.function.Supplier;

public class SyncPullAction extends CommonAnAction {
    private final Supplier<TopicList> selection;
    public SyncPullAction() { this(null); }
    public SyncPullAction(Supplier<TopicList> selection) {
        super(CodeReadingNoteBundle.message("action.sync.pull"),
                CodeReadingNoteBundle.message("action.sync.pull.description"), AllIcons.Actions.Download);
        this.selection = selection;
    }
    private TopicList target(AnActionEvent e) {
        if (selection != null) return selection.get();
        var ws = WorkspaceNotesService.getInstance(e.getProject());
        return ws.isWorkspace() ? null : ws.projects().get(0);
    }
    @Override public void actionPerformed(AnActionEvent e) {
        if (e.getProject() != null) NotesSyncUi.run(e.getProject(), target(e), WorkspaceNotesSyncCoordinator.Operation.PULL);
    }
    @Override public void update(AnActionEvent e) {
        e.getPresentation().setEnabled(e.getProject() != null);
        if (e.getProject() == null) return;
        TopicList list = target(e);
        String label = CodeReadingNoteBundle.message("action.sync.pull");
        e.getPresentation().setText(list == null ? label : label + ": " + WorkspaceNotesService.getInstance(e.getProject()).displayName(list.context()));
        e.getPresentation().setDescription(e.getPresentation().getText());
    }
}
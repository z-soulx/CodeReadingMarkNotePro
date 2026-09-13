package jp.kitabatakep.intellij.plugins.codereadingnote.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import jp.kitabatakep.intellij.plugins.codereadingnote.*;
import jp.kitabatakep.intellij.plugins.codereadingnote.notesworkspace.WorkspaceNotesService;
import jp.kitabatakep.intellij.plugins.codereadingnote.sync.workspace.*;
import java.util.function.Supplier;

public class SyncPushAction extends CommonAnAction {
    private final Supplier<TopicList> selection;
    public SyncPushAction() { this(null); }
    public SyncPushAction(Supplier<TopicList> selection) {
        super(CodeReadingNoteBundle.message("action.sync.push"),
                CodeReadingNoteBundle.message("action.sync.push.description"), AllIcons.Actions.Upload);
        this.selection = selection;
    }
    private TopicList target(AnActionEvent e) {
        if (selection != null) return selection.get();
        var ws = WorkspaceNotesService.getInstance(e.getProject());
        return ws.isWorkspace() ? null : ws.projects().get(0);
    }
    @Override public void actionPerformed(AnActionEvent e) {
        if (e.getProject() != null) NotesSyncUi.run(e.getProject(), target(e), WorkspaceNotesSyncCoordinator.Operation.PUSH);
    }
    @Override public void update(AnActionEvent e) {
        e.getPresentation().setEnabled(e.getProject() != null);
        if (e.getProject() == null) return;
        TopicList list = target(e);
        String label = CodeReadingNoteBundle.message("action.sync.push");
        e.getPresentation().setText(list == null ? label : label + ": " + WorkspaceNotesService.getInstance(e.getProject()).displayName(list.context()));
        e.getPresentation().setDescription(e.getPresentation().getText());
    }
}
package jp.kitabatakep.intellij.plugins.codereadingnote.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import jp.kitabatakep.intellij.plugins.codereadingnote.CodeReadingNoteBundle;
import jp.kitabatakep.intellij.plugins.codereadingnote.sync.workspace.NotesSyncUi;

public class WorkspaceSyncAction extends CommonAnAction {
    public WorkspaceSyncAction() { super(CodeReadingNoteBundle.message("notes.sync.title"), CodeReadingNoteBundle.message("notes.sync.title"), AllIcons.Actions.Refresh); }
    @Override public void actionPerformed(AnActionEvent e) { if (e.getProject() != null) NotesSyncUi.open(e.getProject()); }
    @Override public void update(AnActionEvent e) {
        e.getPresentation().setText(CodeReadingNoteBundle.message("notes.sync.title"));
        e.getPresentation().setDescription(CodeReadingNoteBundle.message("notes.sync.title"));
        e.getPresentation().setEnabled(e.getProject() != null);
    }
}

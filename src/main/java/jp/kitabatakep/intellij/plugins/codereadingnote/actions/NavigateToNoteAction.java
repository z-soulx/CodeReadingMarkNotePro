package jp.kitabatakep.intellij.plugins.codereadingnote.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import jp.kitabatakep.intellij.plugins.codereadingnote.CodeReadingNoteService;
import jp.kitabatakep.intellij.plugins.codereadingnote.TopicLine;
import jp.kitabatakep.intellij.plugins.codereadingnote.remark.NotePopupHelper;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Opens the note edit popup for the current editor line.
 * Default shortcut: Alt+G
 */
public class NavigateToNoteAction extends CommonAnAction {

    @Override
    public void update(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        Editor editor = event.getData(CommonDataKeys.EDITOR);
        event.getPresentation().setEnabled(project != null && editor != null);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        Editor editor = event.getData(CommonDataKeys.EDITOR);
        if (project == null || editor == null) return;

        VirtualFile file = event.getData(CommonDataKeys.VIRTUAL_FILE);
        if (file == null) return;

        int line = editor.getCaretModel().getLogicalPosition().line;

        CodeReadingNoteService service = CodeReadingNoteService.getInstance(project);
        List<TopicLine> topicLines = service.listSource(project, file);

        for (TopicLine tl : topicLines) {
            if (tl.line() == line) {
                NotePopupHelper.show(editor, project, tl);
                return;
            }
        }
    }
}

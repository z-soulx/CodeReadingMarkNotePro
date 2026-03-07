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
 * Reverse-locate: select the TreeView node corresponding to the note at the current cursor line.
 * Default shortcut: Alt+Shift+G
 */
public class ReverseLocateAction extends CommonAnAction {

    @Override
    public void update(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        Editor editor = event.getData(CommonDataKeys.EDITOR);
        event.getPresentation().setEnabledAndVisible(project != null && editor != null);
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
                NotePopupHelper.reverseLocate(project, tl);
                return;
            }
        }
    }
}

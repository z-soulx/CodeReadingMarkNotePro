package jp.kitabatakep.intellij.plugins.codereadingnote.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import jp.kitabatakep.intellij.plugins.codereadingnote.CodeReadingNoteService;
import jp.kitabatakep.intellij.plugins.codereadingnote.TopicLine;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Remove the note attached to the current editor line.
 * Default shortcut: Alt+Delete
 */
public class RemoveNoteAtLineAction extends CommonAnAction {

    @Override
    public void update(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        Editor editor = event.getData(CommonDataKeys.EDITOR);
        boolean enabled = false;

        if (project != null && editor != null) {
            VirtualFile file = event.getData(CommonDataKeys.VIRTUAL_FILE);
            if (file != null) {
                int line = editor.getCaretModel().getLogicalPosition().line;
                CodeReadingNoteService service = CodeReadingNoteService.getInstance(project);
                List<TopicLine> topicLines = service.listSource(project, file);
                for (TopicLine tl : topicLines) {
                    if (tl.line() == line) {
                        enabled = true;
                        break;
                    }
                }
            }
        }
        event.getPresentation().setEnabled(enabled);
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
                tl.topic().removeLine(tl);
                break;
            }
        }
    }
}

package jp.kitabatakep.intellij.plugins.codereadingnote.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import jp.kitabatakep.intellij.plugins.codereadingnote.CodeReadingNoteBundle;
import jp.kitabatakep.intellij.plugins.codereadingnote.TopicLine;
import jp.kitabatakep.intellij.plugins.codereadingnote.operations.LineNumberUpdateService;
import jp.kitabatakep.intellij.plugins.codereadingnote.ui.dialogs.EditLineNumberDialog;
import org.jetbrains.annotations.NotNull;

/**
 * Action to edit a single TopicLine's line number
 */
public class EditLineNumberAction extends CommonAnAction {
    
    private static final Logger LOG = Logger.getInstance(EditLineNumberAction.class);
    
    private final TopicLine topicLine;
    
    public EditLineNumberAction(@NotNull TopicLine topicLine) {
        super(
            CodeReadingNoteBundle.message("action.edit.line.number"),
            null,
            AllIcons.Actions.Edit
        );
        this.topicLine = topicLine;
    }
    
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }
        
        EditLineNumberDialog dialog = new EditLineNumberDialog(project, topicLine);
        if (dialog.showAndGet()) {
            int newLineNumber = dialog.getNewLineNumber();
            boolean updateBookmark = dialog.shouldUpdateBookmark();
            boolean validate = dialog.shouldValidate();
            
            LOG.info(String.format("Updating line number from %d to %d for %s",
                topicLine.line(), newLineNumber, topicLine.url()));
            
            LineNumberUpdateService service = LineNumberUpdateService.getInstance(project);
            LineNumberUpdateService.LineNumberUpdateResult result = 
                service.updateLineNumber(topicLine, newLineNumber, updateBookmark, validate);
            
            if (result.isSuccess()) {
                Messages.showInfoMessage(
                    project,
                    CodeReadingNoteBundle.message("message.line.number.updated"),
                    "Success"
                );
            } else {
                Messages.showErrorDialog(
                    project,
                    result.getErrorMessage(),
                    "Update Failed"
                );
            }
        }
    }
}


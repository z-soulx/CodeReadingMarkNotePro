package jp.kitabatakep.intellij.plugins.codereadingnote.actions;

import com.intellij.icons.AllIcons;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import jp.kitabatakep.intellij.plugins.codereadingnote.CodeReadingNoteBundle;
import jp.kitabatakep.intellij.plugins.codereadingnote.TopicLine;
import jp.kitabatakep.intellij.plugins.codereadingnote.operations.LineNumberUpdateService;
import jp.kitabatakep.intellij.plugins.codereadingnote.ui.dialogs.BatchLineNumberAdjustDialog;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Action to batch adjust line numbers of multiple TopicLines
 */
public class BatchAdjustLineNumbersAction extends CommonAnAction {
    
    private static final Logger LOG = Logger.getInstance(BatchAdjustLineNumbersAction.class);
    
    private final List<TopicLine> topicLines;
    
    public BatchAdjustLineNumbersAction(@NotNull List<TopicLine> topicLines) {
        super(
            CodeReadingNoteBundle.message("action.batch.adjust.line.numbers"),
            null,
            AllIcons.Actions.ListChanges
        );
        this.topicLines = topicLines;
    }
    
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }
        
        if (topicLines.isEmpty()) {
            return;
        }
        
        BatchLineNumberAdjustDialog dialog = new BatchLineNumberAdjustDialog(project, topicLines);
        if (dialog.showAndGet()) {
            LineNumberUpdateService.LineNumberAdjustment adjustment = dialog.getAdjustment();
            boolean updateBookmarks = dialog.shouldUpdateBookmarks();
            boolean validate = dialog.shouldValidate();

            
            LineNumberUpdateService service = LineNumberUpdateService.getInstance(project);
            LineNumberUpdateService.BatchLineNumberUpdateResult result = 
                service.batchUpdateLineNumbers(topicLines, adjustment, updateBookmarks, validate);
            
            if (result.hasFailures()) {
                String message = CodeReadingNoteBundle.message(
                    "message.batch.update.summary",
                    result.getSuccessCount(),
                    result.getFailureCount()
                );
                
                Messages.showWarningDialog(
                    project,
                    message + "\n\nCheck logs for details.",
                    "Batch Update Completed with Errors"
                );
            } else {
                String message = CodeReadingNoteBundle.message(
                    "message.batch.update.summary",
                    result.getSuccessCount(),
                    0
                );
                
                Notifications.Bus.notify(
                    new Notification(
                        "Code Reading Note",
                        "Batch Update Successful",
                        message,
                        NotificationType.INFORMATION
                    ),
                    project
                );
            }
        }
    }
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(!topicLines.isEmpty());
    }
}


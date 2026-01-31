package jp.kitabatakep.intellij.plugins.codereadingnote.actions;

import com.intellij.icons.AllIcons;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import jp.kitabatakep.intellij.plugins.codereadingnote.CodeReadingNoteBundle;
import jp.kitabatakep.intellij.plugins.codereadingnote.operations.BookmarkRepairService;
import org.jetbrains.annotations.NotNull;

/**
 * Action to repair missing or orphaned bookmarks
 */
public class RepairBookmarksAction extends CommonAnAction {
    
    private static final Logger LOG = Logger.getInstance(RepairBookmarksAction.class);
    
    public RepairBookmarksAction() {
        super(
            CodeReadingNoteBundle.message("action.repair.bookmarks"),
            CodeReadingNoteBundle.message("action.repair.bookmarks.description"),
            AllIcons.Actions.ForceRefresh
        );
    }
    
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }
        
        LOG.info("Starting bookmark repair process");
        
        ProgressManager.getInstance().run(new Task.Backgroundable(
            project,
            CodeReadingNoteBundle.message("progress.repair.bookmarks"),
            true
        ) {
            private BookmarkRepairService.BookmarkRepairResult result;
            
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(false);
                indicator.setFraction(0.0);
                
                BookmarkRepairService service = BookmarkRepairService.getInstance(project);
                
                indicator.setText("Scanning topics...");
                indicator.setFraction(0.3);
                
                result = service.scanAndRepair(true);
                
                indicator.setText("Repair completed");
                indicator.setFraction(1.0);
            }
            
            @Override
            public void onSuccess() {
                showRepairResults(project, result);
            }
            
            @Override
            public void onThrowable(@NotNull Throwable error) {
                LOG.error("Failed to repair bookmarks", error);
                ApplicationManager.getApplication().invokeLater(() -> {
                    Messages.showErrorDialog(
                        project,
                        CodeReadingNoteBundle.message("message.repair.failed") + ": " + error.getMessage(),
                        CodeReadingNoteBundle.message("message.repair.failed.title")
                    );
                });
            }
        });
    }
    
    private void showRepairResults(@NotNull Project project, 
                                   @NotNull BookmarkRepairService.BookmarkRepairResult result) {
        if (!result.hasIssues()) {
            Messages.showInfoMessage(
                project,
                CodeReadingNoteBundle.message("message.repair.no.issues"),
                CodeReadingNoteBundle.message("message.repair.no.issues.title")
            );
            return;
        }
        
        if (result.getRepairedCount() > 0) {
            String message = CodeReadingNoteBundle.message(
                "notification.repair.summary",
                result.getTotalCount(),
                result.getMissingCount() + result.getOrphanedCount(),
                result.getRepairedCount(),
                result.getFailedCount()
            );
            
            Notifications.Bus.notify(
                new Notification(
                    "Code Reading Note",
                    CodeReadingNoteBundle.message("notification.repair.success.title"),
                    message,
                    NotificationType.INFORMATION
                ),
                project
            );
            
            LOG.info(String.format("Bookmark repair completed: %s", result.getSummary()));
        }
        
        if (result.hasFailures()) {
            Messages.showWarningDialog(
                project,
                String.format("Repaired %d bookmarks, but %d failed. Check logs for details.",
                    result.getRepairedCount(), result.getFailedCount()),
                "Repair Completed with Warnings"
            );
        }
    }
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        e.getPresentation().setEnabled(project != null);
    }
}


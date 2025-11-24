package jp.kitabatakep.intellij.plugins.codereadingnote.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import jp.kitabatakep.intellij.plugins.codereadingnote.CodeReadingNoteBundle;
import jp.kitabatakep.intellij.plugins.codereadingnote.Topic;
import jp.kitabatakep.intellij.plugins.codereadingnote.operations.BookmarkRepairService;
import org.jetbrains.annotations.NotNull;

/**
 * Action to repair bookmarks for a specific topic
 */
public class RepairTopicBookmarksAction extends CommonAnAction {
    private static final Logger LOG = Logger.getInstance(RepairTopicBookmarksAction.class);
    private final Topic topic;

    public RepairTopicBookmarksAction(@NotNull Topic topic) {
        super(
            CodeReadingNoteBundle.message("action.repair.bookmarks.topic"),
            CodeReadingNoteBundle.message("action.repair.bookmarks.description"),
            AllIcons.Actions.ForceRefresh
        );
        this.topic = topic;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }

        ProgressManager.getInstance().run(new Task.Backgroundable(
                project,
                CodeReadingNoteBundle.message("progress.repair.bookmarks"),
                false
        ) {
            @Override
            public void run(com.intellij.openapi.progress.@NotNull ProgressIndicator indicator) {
                BookmarkRepairService service = BookmarkRepairService.getInstance(project);
                BookmarkRepairService.BookmarkRepairResult result = service.scanAndRepairTopic(topic, true);
                
                // Show results on UI thread
                com.intellij.openapi.application.ApplicationManager.getApplication()
                        .invokeLater(() -> showRepairResults(project, result));
            }
        });
    }

    private void showRepairResults(@NotNull Project project, 
                                   @NotNull BookmarkRepairService.BookmarkRepairResult result) {
        if (result.getMissingCount() == 0) {
            Messages.showInfoMessage(
                    project,
                    CodeReadingNoteBundle.message("message.repair.no.issues"),
                    CodeReadingNoteBundle.message("message.repair.no.issues.title")
            );
        } else {
            String message = String.format(
                    CodeReadingNoteBundle.message("notification.repair.summary"),
                    result.getTotalCount(),
                    result.getMissingCount(),
                    result.getRepairedCount(),
                    result.getFailedCount()
            );
            
            if (result.getFailedCount() > 0) {
                Messages.showWarningDialog(project, message, 
                        CodeReadingNoteBundle.message("notification.repair.success.title"));
            } else {
                Messages.showInfoMessage(project, message,
                        CodeReadingNoteBundle.message("notification.repair.success.title"));
            }
        }
    }
}


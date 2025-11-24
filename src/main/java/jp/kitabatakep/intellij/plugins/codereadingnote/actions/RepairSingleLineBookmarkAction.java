package jp.kitabatakep.intellij.plugins.codereadingnote.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import jp.kitabatakep.intellij.plugins.codereadingnote.CodeReadingNoteBundle;
import jp.kitabatakep.intellij.plugins.codereadingnote.Topic;
import jp.kitabatakep.intellij.plugins.codereadingnote.TopicLine;
import jp.kitabatakep.intellij.plugins.codereadingnote.operations.BookmarkRepairService;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;

/**
 * Action to repair bookmark for a single TopicLine
 */
public class RepairSingleLineBookmarkAction extends CommonAnAction {
    private static final Logger LOG = Logger.getInstance(RepairSingleLineBookmarkAction.class);
    private final TopicLine topicLine;
    private final Topic topic;

    public RepairSingleLineBookmarkAction(@NotNull TopicLine topicLine, @NotNull Topic topic) {
        super(
            CodeReadingNoteBundle.message("action.repair.bookmarks.single"),
            CodeReadingNoteBundle.message("action.repair.bookmarks.single.description"),
            AllIcons.Actions.Refresh
        );
        this.topicLine = topicLine;
        this.topic = topic;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }

        BookmarkRepairService service = BookmarkRepairService.getInstance(project);
        
        // Create a result for single line
        BookmarkRepairService.BookmarkRepairResult result = 
            new BookmarkRepairService.BookmarkRepairResult();
        
        // Check and repair this single line
        boolean repaired = service.repairSingleLine(topicLine, topic, result);
        
        // Show result
        showRepairResult(project, result, repaired);
    }

    private void showRepairResult(@NotNull Project project, 
                                  @NotNull BookmarkRepairService.BookmarkRepairResult result,
                                  boolean repaired) {
        if (result.getMissingCount() == 0 && result.getOrphanedCount() == 0) {
            Messages.showInfoMessage(
                project,
                CodeReadingNoteBundle.message("message.repair.bookmark.healthy"),
                CodeReadingNoteBundle.message("message.repair.bookmark.status")
            );
        } else if (repaired) {
            String status = result.getMissingCount() > 0 
                ? CodeReadingNoteBundle.message("message.repair.bookmark.missing") 
                : CodeReadingNoteBundle.message("message.repair.bookmark.orphaned");
            String message = CodeReadingNoteBundle.message("message.repair.bookmark.success", status);
            Messages.showInfoMessage(
                project, 
                message, 
                CodeReadingNoteBundle.message("notification.repair.success.title")
            );
        } else {
            Messages.showErrorDialog(
                project,
                CodeReadingNoteBundle.message("message.repair.failed"),
                CodeReadingNoteBundle.message("message.repair.failed.title")
            );
        }
    }
}



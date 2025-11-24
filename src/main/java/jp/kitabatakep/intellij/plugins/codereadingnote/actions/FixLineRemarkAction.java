package jp.kitabatakep.intellij.plugins.codereadingnote.actions;

import com.intellij.icons.AllIcons;
import com.intellij.ide.bookmark.Bookmark;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import jp.kitabatakep.intellij.plugins.codereadingnote.Topic;
import jp.kitabatakep.intellij.plugins.codereadingnote.TopicLine;
import jp.kitabatakep.intellij.plugins.codereadingnote.remark.BookmarkUtils;
import jp.kitabatakep.intellij.plugins.codereadingnote.remark.EditorUtils;
import jp.kitabatakep.intellij.plugins.codereadingnote.ui.fix.LineFixResult;
import jp.kitabatakep.intellij.plugins.codereadingnote.ui.fix.SingleLineFixDialog;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.function.Function;

/**
 * 修复单个 TopicLine 的位置
 */
public class FixLineRemarkAction extends CommonAnAction {
    
    private final Project project;
    private final Function<Void, Pair<Topic, TopicLine>> fetcher;
    
    public FixLineRemarkAction(Project project, Function<Void, Pair<Topic, TopicLine>> fetcher) {
        super("Sync Bookmark Position", "Sync TopicLine position to current Bookmark position", AllIcons.Actions.Diff);
        this.project = project;
        this.fetcher = fetcher;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(e.getProject() != null);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Pair<Topic, TopicLine> payload = fetcher.apply(null);
        if (payload == null || payload.getSecond() == null) {
            return;
        }
        
        TopicLine topicLine = payload.getSecond();
        
        // 收集修复信息
        LineFixResult result = collectFixInfo(topicLine);
        
        // 显示预览对话框
        SingleLineFixDialog dialog = new SingleLineFixDialog(project, result);
        
        // 用户确认后执行修复
        if (dialog.showAndGet()) {
            performFix(result);
        }
    }
    
    /**
     * 收集修复信息
     */
    private LineFixResult collectFixInfo(TopicLine topicLine) {
        Map<String, Bookmark> bookmarkMap = BookmarkUtils.getStringBookmarkMap(project);
        Bookmark bookmark = bookmarkMap.get(topicLine.getBookmarkUid());
        
        Integer bookmarkLine = null;
        if (bookmark != null) {
            Object lineObj = bookmark.getAttributes().get("line");
            if (lineObj != null) {
                bookmarkLine = Integer.valueOf(lineObj.toString());
            }
        }
        
        return new LineFixResult(topicLine, bookmarkLine);
    }
    
    /**
     * 执行修复
     */
    private void performFix(LineFixResult result) {
        if (!result.needsFix()) {
            return;
        }
        
        TopicLine topicLine = result.getTopicLine();
        int oldLine = result.getOldLine();
        int newLine = result.getNewLine();
        
        // 移除旧的 remark
        EditorUtils.removeLineCodeRemark(project, topicLine);
        
        // 执行修复
        topicLine.modifyLine(newLine);
        
        // 添加新的 remark
        EditorUtils.addLineCodeRemark(project, topicLine);
        
        // Show success notification
        String message = String.format("✅ %s:%d → %d", 
                result.getFileName(), oldLine, newLine);
        showNotification("Position Fixed", message, NotificationType.INFORMATION);
    }
    
    /**
     * 显示通知
     */
    private void showNotification(String title, String content, NotificationType type) {
        Notification notification = new Notification(
                "CodeReadingNote",
                title,
                content,
                type
        );
        Notifications.Bus.notify(notification, project);
    }
}

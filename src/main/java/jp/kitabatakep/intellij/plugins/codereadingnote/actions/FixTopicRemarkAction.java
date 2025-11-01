package jp.kitabatakep.intellij.plugins.codereadingnote.actions;

import com.intellij.icons.AllIcons;
import com.intellij.ide.bookmark.Bookmark;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import jp.kitabatakep.intellij.plugins.codereadingnote.Topic;
import jp.kitabatakep.intellij.plugins.codereadingnote.TopicLine;
import jp.kitabatakep.intellij.plugins.codereadingnote.remark.BookmarkUtils;
import jp.kitabatakep.intellij.plugins.codereadingnote.ui.fix.BatchFixDialog;
import jp.kitabatakep.intellij.plugins.codereadingnote.ui.fix.FixPreviewData;
import jp.kitabatakep.intellij.plugins.codereadingnote.ui.fix.LineFixResult;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 修复整个 Topic 的所有 TopicLine 位置
 */
public class FixTopicRemarkAction extends CommonAnAction {
    
    private final Project project;
    private final Function<Void, Topic> topicFetcher;
    
    public FixTopicRemarkAction(Project project, Function<Void, Topic> topicFetcher) {
        super("Sync Topic Position", "Sync all TopicLine positions in Topic to Bookmarks", AllIcons.Actions.Refresh);
        this.project = project;
        this.topicFetcher = topicFetcher;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(e.getProject() != null);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Topic topic = topicFetcher.apply(null);
        if (topic == null) {
            return;
        }
        
        // 收集修复信息
        FixPreviewData previewData = collectFixInfo(topic);
        
        if (previewData.isEmpty()) {
            showNotification("No Items to Fix", "No TopicLine in this Topic", NotificationType.INFORMATION);
            return;
        }
        
        // Show preview dialog
        String title = String.format("Fix Topic: \"%s\"", topic.name());
        BatchFixDialog dialog = new BatchFixDialog(
                project, 
                new FixPreviewData(previewData.getResults(), topic, title)
        );
        
        // 用户确认后执行修复
        if (dialog.showAndGet()) {
            performFix(previewData, dialog.getSelectedMode());
        }
    }
    
    /**
     * 收集修复信息
     */
    private FixPreviewData collectFixInfo(Topic topic) {
        Map<String, Bookmark> bookmarkMap = BookmarkUtils.getStringBookmarkMap(project);
        List<LineFixResult> results = new ArrayList<>();
        
        for (TopicLine topicLine : topic.getLines()) {
            Bookmark bookmark = bookmarkMap.get(topicLine.getBookmarkUid());
            
            Integer bookmarkLine = null;
            if (bookmark != null) {
                Object lineObj = bookmark.getAttributes().get("line");
                if (lineObj != null) {
                    bookmarkLine = Integer.valueOf(lineObj.toString());
                }
            }
            
            results.add(new LineFixResult(topicLine, bookmarkLine));
        }
        
        String title = String.format("Fix Topic: \"%s\"", topic.name());
        return new FixPreviewData(results, topic, title);
    }
    
    /**
     * 执行修复
     */
    private void performFix(FixPreviewData previewData, BatchFixDialog.FixMode mode) {
        List<LineFixResult> toFix;
        
        switch (mode) {
            case FIX_ONLY_NEEDS:
                toFix = previewData.getNeedsFixResults();
                break;
            case FIX_ALL:
                toFix = previewData.getResults().stream()
                        .filter(r -> r.getStatus() != LineFixResult.FixStatus.BOOKMARK_MISSING 
                                  && r.getStatus() != LineFixResult.FixStatus.FILE_NOT_FOUND)
                        .collect(java.util.stream.Collectors.toList());
                break;
            default:
                toFix = new ArrayList<>();
        }
        
        int successCount = 0;
        int failCount = 0;
        
        for (LineFixResult result : toFix) {
            if (result.getBookmarkLine() != null) {
                result.getTopicLine().modifyLine(result.getBookmarkLine());
                successCount++;
            } else {
                failCount++;
            }
        }
        
        // Show result notification
        String message = String.format("✅ Successfully fixed %d item(s)", successCount);
        if (failCount > 0) {
            message += String.format(", ❌ Failed %d item(s)", failCount);
        }
        
        showNotification("Topic Position Fixed", message, NotificationType.INFORMATION);
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

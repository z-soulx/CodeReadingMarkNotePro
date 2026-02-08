package jp.kitabatakep.intellij.plugins.codereadingnote.actions;

import com.intellij.icons.AllIcons;
import com.intellij.ide.bookmark.Bookmark;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import jp.kitabatakep.intellij.plugins.codereadingnote.CodeReadingNoteBundle;
import jp.kitabatakep.intellij.plugins.codereadingnote.Topic;
import jp.kitabatakep.intellij.plugins.codereadingnote.TopicLine;
import jp.kitabatakep.intellij.plugins.codereadingnote.remark.BookmarkUtils;
import jp.kitabatakep.intellij.plugins.codereadingnote.remark.EditorUtils;
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
        super(
            CodeReadingNoteBundle.message("action.fix.topic"),
            CodeReadingNoteBundle.message("action.fix.topic.description"),
            AllIcons.Actions.Refresh
        );
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
            showNotification(
                CodeReadingNoteBundle.message("message.fix.no.items"),
                CodeReadingNoteBundle.message("message.fix.no.topicline.topic"),
                NotificationType.INFORMATION
            );
            return;
        }
        
        // Show preview dialog
        String title = CodeReadingNoteBundle.message("message.fix.title.topic", topic.name());
        BatchFixDialog dialog = new BatchFixDialog(
                project, 
                new FixPreviewData(previewData.getResults(), topic, title)
        );
        
        // 用户确认后执行修复或清理
        if (dialog.showAndGet()) {
            if (dialog.getSelectedMode() == BatchFixDialog.FixMode.CLEAN_UP) {
                performCleanUp(dialog.getCleanUpSelection());
            } else {
                performFix(previewData, dialog.getSelectedMode());
            }
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
        
        String title = CodeReadingNoteBundle.message("message.fix.title.topic", topic.name());
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
                TopicLine topicLine = result.getTopicLine();
                // 移除旧的 remark
                EditorUtils.removeLineCodeRemark(project, topicLine);
                // 更新行号
                topicLine.modifyLine(result.getBookmarkLine());
                // 添加新的 remark
                EditorUtils.addLineCodeRemark(project, topicLine);
                successCount++;
            } else {
                failCount++;
            }
        }
        
        // Show result notification
        String message = CodeReadingNoteBundle.message("message.fix.success.items", successCount);
        if (failCount > 0) {
            message += ", " + CodeReadingNoteBundle.message("message.fix.failed", failCount);
        }
        
        showNotification(
            CodeReadingNoteBundle.message("message.fix.result.topic"),
            message,
            NotificationType.INFORMATION
        );
    }
    
    /**
     * 执行清理：删除选中的错误 TopicLine
     */
    private void performCleanUp(List<LineFixResult> toCleanUp) {
        if (toCleanUp == null || toCleanUp.isEmpty()) {
            return;
        }
        
        int successCount = 0;
        
        for (LineFixResult result : toCleanUp) {
            TopicLine topicLine = result.getTopicLine();
            try {
                // Remove editor remark if file is still valid
                if (topicLine.file() != null && topicLine.file().isValid()) {
                    EditorUtils.removeLineCodeRemark(project, topicLine);
                }
                
                // Remove associated bookmark if it still exists
                BookmarkUtils.removeMachBookmark(topicLine, project);
                
                // Remove TopicLine from its Topic
                if (topicLine.topic() != null) {
                    topicLine.topic().removeLine(topicLine);
                }
                
                successCount++;
            } catch (Exception ex) {
                // Log but continue with other items
            }
        }
        
        showNotification(
            CodeReadingNoteBundle.message("message.cleanup.result.title"),
            CodeReadingNoteBundle.message("message.cleanup.result", successCount, toCleanUp.size()),
            NotificationType.INFORMATION
        );
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

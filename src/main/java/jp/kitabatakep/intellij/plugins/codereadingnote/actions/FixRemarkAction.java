package jp.kitabatakep.intellij.plugins.codereadingnote.actions;

import com.intellij.icons.AllIcons;
import com.intellij.ide.bookmark.Bookmark;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import jp.kitabatakep.intellij.plugins.codereadingnote.CodeReadingNoteBundle;
import jp.kitabatakep.intellij.plugins.codereadingnote.CodeReadingNoteService;
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

/**
 * 修复所有 TopicLine 的位置
 */
public class FixRemarkAction extends CommonAnAction {
    
    private final Project project;
    
    public FixRemarkAction(Project project) {
        super(
            CodeReadingNoteBundle.message("action.fix.all"),
            CodeReadingNoteBundle.message("action.fix.all.description"),
            AllIcons.Actions.ForceRefresh
        );
        this.project = project;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(e.getProject() != null);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        CodeReadingNoteService service = CodeReadingNoteService.getInstance(e.getProject());
        
        // 收集所有修复信息
        FixPreviewData previewData = collectFixInfo(service);
        
        if (previewData.isEmpty()) {
            showNotification(
                CodeReadingNoteBundle.message("message.fix.no.items"),
                CodeReadingNoteBundle.message("message.fix.no.topicline"),
                NotificationType.INFORMATION
            );
            return;
        }
        
        // Show preview dialog
        String title = CodeReadingNoteBundle.message("message.fix.title.all");
        BatchFixDialog dialog = new BatchFixDialog(
                project, 
                new FixPreviewData(previewData.getResults(), title)
        );
        
        // 用户确认后执行修复
        if (dialog.showAndGet()) {
            performFix(previewData, dialog.getSelectedMode());
        }
    }
    
    /**
     * 收集所有修复信息
     */
    private FixPreviewData collectFixInfo(CodeReadingNoteService service) {
        Map<String, Bookmark> bookmarkMap = BookmarkUtils.getStringBookmarkMap(project);
        List<LineFixResult> results = new ArrayList<>();
        
        // 遍历所有 Topic 的所有 TopicLine
        service.getTopicList().getTopics().stream()
                .flatMap(topic -> topic.getLines().stream())
                .forEach(topicLine -> {
                    Bookmark bookmark = bookmarkMap.get(topicLine.getBookmarkUid());
                    
                    Integer bookmarkLine = null;
                    if (bookmark != null) {
                        Object lineObj = bookmark.getAttributes().get("line");
                        if (lineObj != null) {
                            bookmarkLine = Integer.valueOf(lineObj.toString());
                        }
                    }
                    
                    results.add(new LineFixResult(topicLine, bookmarkLine));
                });
        
        return new FixPreviewData(results, CodeReadingNoteBundle.message("message.fix.title.all"));
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
        StringBuilder message = new StringBuilder();
        message.append(CodeReadingNoteBundle.message("message.fix.success", successCount));
        
        if (failCount > 0) {
            message.append("\n").append(CodeReadingNoteBundle.message("message.fix.failed", failCount));
        }
        
        // Add statistics
        if (previewData.getSyncedCount() > 0) {
            message.append("\n").append(CodeReadingNoteBundle.message("message.fix.synced", previewData.getSyncedCount()));
        }
        
        showNotification(
            CodeReadingNoteBundle.message("message.fix.result.global"),
            message.toString(),
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

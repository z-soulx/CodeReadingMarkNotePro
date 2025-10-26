package jp.kitabatakep.intellij.plugins.codereadingnote.autofix;

import com.intellij.ide.bookmark.Bookmark;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import jp.kitabatakep.intellij.plugins.codereadingnote.CodeReadingNoteService;
import jp.kitabatakep.intellij.plugins.codereadingnote.Topic;
import jp.kitabatakep.intellij.plugins.codereadingnote.TopicLine;
import jp.kitabatakep.intellij.plugins.codereadingnote.remark.BookmarkUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 自动修复服务 - 核心业务逻辑
 */
public class AutoFixService {
    private static final Logger LOG = Logger.getInstance(AutoFixService.class);
    private static final AutoFixService INSTANCE = new AutoFixService();
    private static final String NOTIFICATION_GROUP = "Code Reading Mark Note Pro";
    
    private final LineOffsetDetector detector = LineOffsetDetector.getInstance();
    
    public static AutoFixService getInstance() {
        return INSTANCE;
    }
    
    private AutoFixService() {}
    
    /**
     * 手动修复单个 TopicLine
     */
    public FixResult fixLine(@NotNull Project project, @NotNull TopicLine line) {
        long startTime = System.currentTimeMillis();
        
        try {
            OffsetInfo info = detector.detectOffset(project, line);

            if (info.getStatus() == OffsetStatus.OFFSET) {
                line.modifyLine(info.getNewLine());

                // 刷新 CodeRemark 显示
                refreshCodeRemark(project, line);

                long duration = System.currentTimeMillis() - startTime;
                return FixResult.success(1, 1, duration);
            } else if (info.getStatus() == OffsetStatus.SYNCED) {
                return FixResult.noActionNeeded();
            } else if (info.getStatus() == OffsetStatus.BOOKMARK_MISSING) {
                if (restoreBookmark(project, line, info)) {
                    refreshCodeRemark(project, line);
                    long duration = System.currentTimeMillis() - startTime;
                    return FixResult.success(1, 1, duration);
                }
                return FixResult.failed("无法修复: ❌ Bookmark 丢失。");
            } else {
                return FixResult.failed("无法修复: " + info.getShortDescription());
            }
        } catch (Exception e) {
            LOG.error("Failed to fix line: " + line, e);
            return FixResult.failed("修复失败: " + e.getMessage());
        }
    }
    
    /**
     * 手动修复 Topic 下的所有 TopicLine
     */
    public FixResult fixTopic(@NotNull Project project, @NotNull Topic topic) {
        List<TopicLine> lines = topic.getLines();
        return fixLines(project, lines, FixTrigger.MANUAL);
    }
    
    /**
     * 手动修复所有 TopicLine
     */
    public FixResult fixAll(@NotNull Project project) {
        CodeReadingNoteService service = CodeReadingNoteService.getInstance(project);
        List<TopicLine> allLines = service.getTopicList().getTopics().stream()
                .flatMap(t -> t.getLines().stream())
                .collect(Collectors.toList());
        
        return fixLines(project, allLines, FixTrigger.MANUAL);
    }
    
    /**
     * 批量修复多个 TopicLine
     */
    public FixResult fixLines(@NotNull Project project, @NotNull List<TopicLine> lines, @NotNull FixTrigger trigger) {
        long startTime = System.currentTimeMillis();
 
         try {
             // 1. 检测错位状态
             Map<TopicLine, OffsetInfo> offsetMap = detector.detectLines(project, lines);
 
             // 2. 过滤出需要修复的行
             List<TopicLine> offsetLines = offsetMap.entrySet().stream()
                     .filter(e -> e.getValue().getStatus().needsFix())
                     .map(Map.Entry::getKey)
                     .collect(Collectors.toList());
 
             if (offsetLines.isEmpty()) {
                 return FixResult.noActionNeeded();
             }
 
             // 3. 可选：显示确认对话框
             AutoFixSettings settings = AutoFixSettings.getInstance();
             if (settings.isShowFixConfirmDialog() && trigger != FixTrigger.MANUAL) {
                 // TODO: 实现确认对话框
                 // boolean confirmed = showFixConfirmDialog(offsetLines, offsetMap);
                 // if (!confirmed) {
                 //     return FixResult.cancelled();
                 // }
             }
 
             // 4. 执行修复
             int fixed = 0;
             int failed = 0;
             List<String> errors = new ArrayList<>();
 
             for (TopicLine line : offsetLines) {
                 try {
                     OffsetInfo info = offsetMap.get(line);
                     if (info == null) {
                         continue;
                     }
 
                     if (info.getStatus() == OffsetStatus.OFFSET) {
                         line.modifyLine(info.getNewLine());
                         fixed++;
                         refreshCodeRemark(project, line);
                     } else if (info.getStatus() == OffsetStatus.BOOKMARK_MISSING) {
                         if (restoreBookmark(project, line, info)) {
                             fixed++;
                             refreshCodeRemark(project, line);
                         } else {
                             failedBookmark(errors, line);
                             failed++;
                         }
                     }
                 } catch (Exception e) {
                     failed++;
                     errors.add(String.format("%s:%d - %s", 
                             line.file() != null ? line.file().getName() : "Unknown",
                             line.line(), 
                             e.getMessage()));
                     LOG.warn("Failed to fix line: " + line, e);
                 }
             }
 
             // 5. 刷新 UI
             // 注意：行号修改不需要特别通知UI，因为 TopicLine 是引用类型
             // UI 会在下次刷新时自动显示新的行号
 
             // 清除检测缓存，因为数据已更改
             detector.clearCache(project);
 
             // 6. 显示结果通知
             long duration = System.currentTimeMillis() - startTime;
             FixResult result;
 
             if (failed > 0) {
                 result = FixResult.partialSuccess(fixed, offsetLines.size(), failed, errors);
             } else {
                 result = FixResult.success(fixed, offsetLines.size(), duration);
             }
 
             if (settings.isShowFixResultNotification() && trigger != FixTrigger.MANUAL) {
                 showFixResultNotification(project, result);
             }
 
             return result;
 
         } catch (Exception e) {
             LOG.error("Failed to fix lines", e);
             return FixResult.failed("批量修复失败: " + e.getMessage());
         }
     }

    private void failedBookmark(List<String> errors, TopicLine line) {
        errors.add(String.format("%s:%d - 无法重新创建 Bookmark",
                line.file() != null ? line.file().getName() : "Unknown",
                line.line()));
    }

    private boolean restoreBookmark(Project project, TopicLine line, OffsetInfo info) {
        if (line.file() == null || !line.file().isValid()) {
            return false;
        }

        String uid = line.getBookmarkUid();
        if (uid == null || uid.isEmpty()) {
            uid = java.util.UUID.randomUUID().toString();
            line.setBookmarkUid(uid);
        }

        int bookmarkLine = info.getBookmarkLine() >= 0 ? info.getBookmarkLine() : line.line();

        Bookmark bookmark = BookmarkUtils.addBookmark(project, line.file(), bookmarkLine, line.note(), uid);
        return bookmark != null;
    }
 
    /**
     * 自动修复（异步）
     */
    public CompletableFuture<FixResult> autoFixAsync(@NotNull Project project, @NotNull FixTrigger trigger) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                AutoFixSettings settings = AutoFixSettings.getInstance();
                
                // 检查是否启用自动修复
                if (!settings.isAutoFixEnabled()) {
                    return FixResult.disabled();
                }
                
                // 根据策略判断是否应该修复
                if (!settings.shouldFixInSmartMode(trigger)) {
                    LOG.debug("Skipping auto fix for trigger: " + trigger);
                    return FixResult.skipped();
                }
                
                // 执行修复
                return fixAll(project);
                
            } catch (Exception e) {
                LOG.error("Auto fix failed", e);
                return FixResult.failed("自动修复失败: " + e.getMessage());
            }
        }, ApplicationManager.getApplication()::executeOnPooledThread);
    }
    
    /**
     * 检测并通知错位情况
     */
    public void detectAndNotify(@NotNull Project project) {
        AutoFixSettings settings = AutoFixSettings.getInstance();
        
        if (!settings.isShowOffsetNotification()) {
            return;
        }
        
        try {
            LineOffsetDetector.OffsetStatistics stats = detector.getStatistics(project);
            
            if (stats.hasIssues()) {
                showOffsetDetectionNotification(project, stats);
            }
            
        } catch (Exception e) {
            LOG.error("Failed to detect and notify", e);
        }
    }
    
    /**
     * 刷新 CodeRemark 显示（修复后需要更新编辑器中的标记位置）
     */
    private void refreshCodeRemark(@NotNull Project project, @NotNull TopicLine line) {
        try {
            // 先移除旧的 remark
            jp.kitabatakep.intellij.plugins.codereadingnote.remark.EditorUtils.removeLineCodeRemark(project, line);
            
            // 添加新的 remark（使用新行号）
            jp.kitabatakep.intellij.plugins.codereadingnote.remark.EditorUtils.addLineCodeRemark(project, line);
        } catch (Exception e) {
            LOG.warn("Failed to refresh CodeRemark", e);
        }
    }
    
    /**
     * 显示错位检测通知
     */
    private void showOffsetDetectionNotification(@NotNull Project project, 
                                                 @NotNull LineOffsetDetector.OffsetStatistics stats) {
        String title = "检测到 TopicLine 错位";
        String content = String.format(
                "发现 %d 个错位项\n" +
                "⚠️ 错位: %d\n" +
                "❌ Bookmark 丢失: %d\n" +
                "🚫 文件不存在: %d",
                stats.getOffset() + stats.getBookmarkMissing() + stats.getFileMissing(),
                stats.getOffset(),
                stats.getBookmarkMissing(),
                stats.getFileMissing()
        );
        
        Notification notification = new Notification(
                NOTIFICATION_GROUP,
                title,
                content,
                NotificationType.WARNING
        );
        
        // TODO: 添加 action 按钮 "自动修复" 和 "查看详情"
        
        Notifications.Bus.notify(notification, project);
    }
    
    /**
     * 显示修复结果通知
     */
    private void showFixResultNotification(@NotNull Project project, @NotNull FixResult result) {
        if (!result.hasFixed()) {
            return;
        }
        
        String title = "TopicLine 修复完成";
        String content = result.getSummary();
        
        NotificationType type = result.getFailedCount() > 0 ? 
                NotificationType.WARNING : NotificationType.INFORMATION;
        
        Notification notification = new Notification(
                NOTIFICATION_GROUP,
                title,
                content,
                type
        );
        
        Notifications.Bus.notify(notification, project);
    }
}


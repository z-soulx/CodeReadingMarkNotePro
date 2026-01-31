package jp.kitabatakep.intellij.plugins.codereadingnote.sync.ui;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.ui.AnimatedIcon;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.util.ui.JBUI;
import jp.kitabatakep.intellij.plugins.codereadingnote.CodeReadingNoteBundle;
import jp.kitabatakep.intellij.plugins.codereadingnote.sync.*;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * UI component to display sync status in the toolbar
 * Shows icon + text based on current sync status with tooltip
 */
public class SyncStatusLabel extends JPanel {
    
    private final Project project;
    private final JBLabel iconLabel;
    private final JBLabel textLabel;
    private SyncStatus currentStatus = SyncStatus.DISABLED;
    
    public SyncStatusLabel(@NotNull Project project) {
        super(new FlowLayout(FlowLayout.LEFT, 5, 0));
        this.project = project;
        
        setBorder(JBUI.Borders.empty(0, 10, 0, 5));
        setOpaque(false);
        
        // Icon label
        iconLabel = new JBLabel();
        iconLabel.setBorder(JBUI.Borders.empty());
        add(iconLabel);
        
        // Text label
        textLabel = new JBLabel();
        textLabel.setBorder(JBUI.Borders.empty());
        add(textLabel);
        
        // Make clickable
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                handleClick();
            }
        });
        
        // Subscribe to status changes
        subscribeToStatusChanges();
        
        // Initial update
        updateDisplay();
    }
    
    private void subscribeToStatusChanges() {
        MessageBusConnection connection = project.getMessageBus().connect();
        connection.subscribe(SyncStatusNotifier.SYNC_STATUS_TOPIC, new SyncStatusNotifier() {
            @Override
            public void statusChanged(@NotNull SyncStatus status) {
                currentStatus = status;
                SwingUtilities.invokeLater(() -> updateDisplay());
            }
            
            @Override
            public void lastSyncTimeUpdated(long timestamp) {
                SwingUtilities.invokeLater(() -> updateDisplay());
            }
        });
    }
    
    private void updateDisplay() {
        SyncStatusService statusService = SyncStatusService.getInstance(project);
        currentStatus = statusService.getCurrentStatus();
        
        Icon icon;
        String text;
        Color textColor;
        String tooltip;
        
        switch (currentStatus) {
            case SYNCED:
                icon = AllIcons.Actions.Commit;
                text = CodeReadingNoteBundle.message("sync.status.synced");
                textColor = new JBColor(new Color(0, 128, 0), new Color(98, 150, 85));
                tooltip = buildSyncedTooltip(statusService);
                break;
                
            case DIRTY:
                icon = AllIcons.General.Modified;
                text = CodeReadingNoteBundle.message("sync.status.dirty");
                textColor = new JBColor(new Color(255, 165, 0), new Color(204, 140, 50));
                tooltip = CodeReadingNoteBundle.message("sync.status.tooltip.dirty");
                break;
                
            case PENDING:
                icon = AllIcons.General.Warning;
                text = CodeReadingNoteBundle.message("sync.status.pending");
                textColor = new JBColor(new Color(255, 140, 0), new Color(204, 120, 50));
                tooltip = CodeReadingNoteBundle.message("sync.status.tooltip.pending");
                break;
                
            case SYNCING:
                icon = new AnimatedIcon.Default();
                text = CodeReadingNoteBundle.message("sync.status.syncing");
                textColor = new JBColor(new Color(0, 120, 215), new Color(88, 157, 246));
                tooltip = CodeReadingNoteBundle.message("sync.status.tooltip.syncing");
                break;
                
            case ERROR:
                icon = AllIcons.General.Error;
                text = CodeReadingNoteBundle.message("sync.status.error");
                textColor = JBColor.RED;
                String errorMsg = statusService.getLastErrorMessage();
                tooltip = CodeReadingNoteBundle.message("sync.status.tooltip.error", 
                                                       errorMsg != null ? errorMsg : "Unknown error");
                break;
                
            case CONFLICT_PAUSED:
                icon = AllIcons.General.BalloonWarning;
                text = CodeReadingNoteBundle.message("sync.status.conflict.paused");
                textColor = new JBColor(new Color(255, 140, 0), new Color(204, 120, 50));
                tooltip = CodeReadingNoteBundle.message("sync.status.tooltip.conflict");
                break;
                
            case DISABLED:
            default:
                icon = AllIcons.Actions.Suspend;
                text = CodeReadingNoteBundle.message("sync.status.disabled");
                textColor = JBColor.GRAY;
                tooltip = CodeReadingNoteBundle.message("sync.status.tooltip.disabled");
                break;
        }
        
        iconLabel.setIcon(icon);
        textLabel.setText(text);
        textLabel.setForeground(textColor);
        setToolTipText(tooltip);
    }
    
    private String buildSyncedTooltip(SyncStatusService statusService) {
        long lastSyncTime = statusService.getLastSyncTime();
        
        if (lastSyncTime == 0) {
            return CodeReadingNoteBundle.message("sync.status.never.synced");
        }
        
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String formattedTime = dateFormat.format(new Date(lastSyncTime));
        
        return CodeReadingNoteBundle.message("sync.status.tooltip.synced", formattedTime) +
               " - " + CodeReadingNoteBundle.message("sync.status.tooltip.click");
    }
    
    private void handleClick() {
        SyncConfig config = SyncSettings.getInstance().getSyncConfig();
        
        switch (currentStatus) {
            case DIRTY:
                // Data modified but not yet persisted - notify user
                com.intellij.notification.Notifications.Bus.notify(
                    new com.intellij.notification.Notification(
                        CodeReadingNoteBundle.message("notification.group.name"),
                        CodeReadingNoteBundle.message("notification.data.modified.title"),
                        CodeReadingNoteBundle.message("notification.data.modified.message"),
                        com.intellij.notification.NotificationType.INFORMATION
                    ),
                    project
                );
                break;
                
            case CONFLICT_PAUSED:
                // Re-check and show conflict dialog
                recheckRemoteAndShowDialog();
                break;
                
            case PENDING:
            case ERROR:
                // Manually trigger sync
                if (config.isEnabled()) {
                    manuallyTriggerSync();
                } else {
                    openSyncSettings();
                }
                break;
                
            case DISABLED:
                // Open sync settings
                openSyncSettings();
                break;
                
            case SYNCED:
                // Check for remote updates
                if (config.isEnabled()) {
                    checkForRemoteUpdates();
                } else {
                    openSyncSettings();
                }
                break;
                
            case SYNCING:
                // Do nothing while syncing
                break;
        }
    }
    
    /**
     * Check for remote updates and show notification
     */
    private void checkForRemoteUpdates() {
        SyncStatusService statusService = SyncStatusService.getInstance(project);
        statusService.markSyncing();
        
        com.intellij.openapi.application.ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                SyncConflictDetector detector = SyncConflictDetector.getInstance(project);
                ConflictDetectionResult result = detector.checkRemoteUpdate();
                
                com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater(() -> {
                    if (result.hasConflict()) {
                        // Remote has updates, show conflict dialog
                        statusService.pauseAutoPush();
                        SyncConflictDialog.show(project, result);
                    } else {
                        // No updates
                        statusService.markSynced();
                        com.intellij.notification.Notifications.Bus.notify(
                            new com.intellij.notification.Notification(
                                CodeReadingNoteBundle.message("notification.group.name"),
                                CodeReadingNoteBundle.message("sync.check.title"),
                                CodeReadingNoteBundle.message("sync.check.up.to.date"),
                                com.intellij.notification.NotificationType.INFORMATION
                            ),
                            project
                        );
                    }
                });
            } catch (Exception e) {
                com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater(() -> {
                    statusService.setError("Check failed: " + e.getMessage());
                });
            }
        });
    }
    
    /**
     * Re-check remote and show conflict dialog if needed
     */
    private void recheckRemoteAndShowDialog() {
        com.intellij.openapi.application.ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                SyncConflictDetector detector = SyncConflictDetector.getInstance(project);
                ConflictDetectionResult result = detector.checkRemoteUpdate();
                
                com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater(() -> {
                    if (result.hasConflict()) {
                        SyncConflictDialog.show(project, result);
                    } else {
                        // No longer in conflict, resume auto-sync
                        SyncStatusService statusService = SyncStatusService.getInstance(project);
                        statusService.resumeAutoPush();
                        com.intellij.notification.Notifications.Bus.notify(
                            new com.intellij.notification.Notification(
                                CodeReadingNoteBundle.message("notification.group.name"),
                                CodeReadingNoteBundle.message("sync.conflict.resolved.title"),
                                CodeReadingNoteBundle.message("sync.conflict.no.longer.exists"),
                                com.intellij.notification.NotificationType.INFORMATION
                            ),
                            project
                        );
                    }
                });
            } catch (Exception e) {
                // Ignore errors
            }
        });
    }
    
    private void manuallyTriggerSync() {
        SyncStatusService statusService = SyncStatusService.getInstance(project);
        statusService.markSyncing();
        
        SyncConfig config = SyncSettings.getInstance().getSyncConfig();
        SyncService syncService = SyncService.getInstance(project);
        
        com.intellij.openapi.progress.ProgressManager.getInstance().run(
            new com.intellij.openapi.progress.Task.Backgroundable(
                project,
                CodeReadingNoteBundle.message("progress.pushing.text"),
                false) {
                
                private SyncResult result;
                
                @Override
                public void run(@NotNull com.intellij.openapi.progress.ProgressIndicator indicator) {
                    // 1. 强制刷盘
                    indicator.setText(CodeReadingNoteBundle.message("progress.saving.state"));
                    forceSaveProjectState();
                    
                    // 2. 执行 Push
                    indicator.setText(CodeReadingNoteBundle.message("progress.pushing.to.remote"));
                    result = syncService.push(config);
                }
                
                @Override
                public void onSuccess() {
                    // 3. Push 成功后标记为已同步（已在 SyncService.push() 中处理）
                    if (result.isSuccess()) {
                        statusService.markSynced();
                    } else {
                        statusService.setError(result.getMessage());
                    }
                }
            }
        );
    }
    
    /**
     * 强制触发状态序列化
     * 
     * 说明：调用 getState() 会触发 IDE 的 StateStorageManager 自动序列化并保存到 XML。
     * 虽然无法确保立即写入磁盘（IDE 可能异步处理），但可以确保最新的内存数据被序列化。
     */
    private void forceSaveProjectState() {
        try {
            // 调用 getState() 触发 StateStorageManager 序列化
            jp.kitabatakep.intellij.plugins.codereadingnote.CodeReadingNoteService service = 
                jp.kitabatakep.intellij.plugins.codereadingnote.CodeReadingNoteService.getInstance(project);
            service.getState();
            
        } catch (Exception ex) {
            com.intellij.openapi.diagnostic.Logger.getInstance(SyncStatusLabel.class)
                .warn("Failed to trigger state serialization", ex);
        }
    }
    
    private void openSyncSettings() {
        com.intellij.openapi.options.ShowSettingsUtil.getInstance().showSettingsDialog(
            project,
            "Code Reading Note Sync"
        );
    }
}


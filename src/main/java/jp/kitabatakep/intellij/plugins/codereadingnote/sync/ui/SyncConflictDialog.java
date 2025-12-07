package jp.kitabatakep.intellij.plugins.codereadingnote.sync.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.JBUI;
import jp.kitabatakep.intellij.plugins.codereadingnote.CodeReadingNoteBundle;
import jp.kitabatakep.intellij.plugins.codereadingnote.sync.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Dialog for resolving sync conflicts
 * Shows conflict information and lets user choose: Pull, Push, or Cancel
 */
public class SyncConflictDialog extends DialogWrapper {
    
    private final ConflictDetectionResult conflictResult;
    private ConflictResolutionAction selectedAction = ConflictResolutionAction.CANCEL;
    
    public enum ConflictResolutionAction {
        PULL,
        PUSH,
        CANCEL
    }
    
    private SyncConflictDialog(@NotNull Project project, @NotNull ConflictDetectionResult result) {
        super(project);
        this.conflictResult = result;
        
        // Set title and message based on conflict type
        switch (result.getConflictType()) {
            case BOTH_MODIFIED:
                setTitle(CodeReadingNoteBundle.message("sync.conflict.both.modified.title"));
                break;
            case LOCAL_MODIFIED:
                setTitle(CodeReadingNoteBundle.message("sync.conflict.local.modified.title"));
                break;
            case REMOTE_UPDATED:
                setTitle(CodeReadingNoteBundle.message("sync.conflict.remote.updated.title"));
                break;
            default:
                setTitle(CodeReadingNoteBundle.message("sync.conflict.title"));
        }
        
        setOKButtonText(CodeReadingNoteBundle.message("sync.conflict.action.pull"));
        setCancelButtonText(CodeReadingNoteBundle.message("sync.conflict.action.cancel"));
        
        init();
    }
    
    /**
     * Show conflict dialog and handle user's choice
     */
    public static void show(@NotNull Project project, @NotNull ConflictDetectionResult result) {
        SyncConflictDialog dialog = new SyncConflictDialog(project, result);
        
        if (dialog.showAndGet()) {
            // User clicked Pull or Push
            ConflictResolutionAction action = dialog.getSelectedAction();
            handleResolution(project, action, result);
        } else {
            // User cancelled or closed dialog
            handleResolution(project, ConflictResolutionAction.CANCEL, result);
        }
    }
    
    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBorder(JBUI.Borders.empty(10));
        
        // Message based on conflict type
        String messageKey;
        switch (conflictResult.getConflictType()) {
            case BOTH_MODIFIED:
                messageKey = "sync.conflict.both.modified.message";
                break;
            case LOCAL_MODIFIED:
                messageKey = "sync.conflict.local.modified.message";
                break;
            case REMOTE_UPDATED:
                messageKey = "sync.conflict.remote.updated.message";
                break;
            default:
                messageKey = "sync.conflict.message";
        }
        
        JBLabel messageLabel = new JBLabel(CodeReadingNoteBundle.message(messageKey));
        messageLabel.setFont(messageLabel.getFont().deriveFont(Font.BOLD));
        panel.add(messageLabel, BorderLayout.NORTH);
        
        // Conflict details
        JPanel detailsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = JBUI.insets(5);
        
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        
        // Remote info
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        detailsPanel.add(new JBLabel("📊"), gbc);
        
        gbc.gridx = 1;
        gbc.weightx = 1;
        String remoteInfo = CodeReadingNoteBundle.message("sync.conflict.remote.info",
            dateFormat.format(new Date(conflictResult.getRemoteTimestamp())),
            conflictResult.getRemoteTopicCount() >= 0 ? 
                String.valueOf(conflictResult.getRemoteTopicCount()) : "?",
            conflictResult.getRemoteTopicLineCount() >= 0 ?
                String.valueOf(conflictResult.getRemoteTopicLineCount()) : "?"
        );
        detailsPanel.add(new JBLabel(remoteInfo), gbc);
        
        // Local info
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        detailsPanel.add(new JBLabel("💻"), gbc);
        
        gbc.gridx = 1;
        gbc.weightx = 1;
        String localInfo = CodeReadingNoteBundle.message("sync.conflict.local.info",
            dateFormat.format(new Date(conflictResult.getLocalTimestamp())),
            String.valueOf(conflictResult.getLocalTopicCount()),
            String.valueOf(conflictResult.getLocalTopicLineCount())
        );
        detailsPanel.add(new JBLabel(localInfo), gbc);
        
        panel.add(detailsPanel, BorderLayout.CENTER);
        
        // Warning message
        JPanel warningPanel = new JPanel(new BorderLayout(0, 5));
        warningPanel.setBorder(JBUI.Borders.empty(10, 0, 0, 0));
        
        JBLabel warningLabel = new JBLabel("⚠️ " + CodeReadingNoteBundle.message("sync.conflict.warning"));
        warningLabel.setForeground(new Color(255, 140, 0));
        warningPanel.add(warningLabel, BorderLayout.NORTH);
        
        JBLabel cancelHintLabel = new JBLabel(CodeReadingNoteBundle.message("sync.conflict.cancel.hint"));
        cancelHintLabel.setFont(cancelHintLabel.getFont().deriveFont(cancelHintLabel.getFont().getSize() - 1.0f));
        cancelHintLabel.setForeground(JBColor.GRAY);
        warningPanel.add(cancelHintLabel, BorderLayout.CENTER);
        
        panel.add(warningPanel, BorderLayout.SOUTH);
        
        panel.setPreferredSize(new Dimension(500, 200));
        
        return panel;
    }
    
    @NotNull
    @Override
    protected Action[] createActions() {
        // Create three actions: Pull, Push, Cancel
        Action pullAction = new DialogWrapperAction(
            CodeReadingNoteBundle.message("sync.conflict.action.pull")) {
            @Override
            protected void doAction(ActionEvent e) {
                selectedAction = ConflictResolutionAction.PULL;
                close(OK_EXIT_CODE);
            }
        };
        
        Action pushAction = new DialogWrapperAction(
            CodeReadingNoteBundle.message("sync.conflict.action.push")) {
            @Override
            protected void doAction(ActionEvent e) {
                // Confirm push action since it will overwrite remote
                int result = JOptionPane.showConfirmDialog(
                    getContentPane(),
                    CodeReadingNoteBundle.message("sync.conflict.push.confirm"),
                    CodeReadingNoteBundle.message("sync.conflict.push.confirm.title"),
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
                );
                
                if (result == JOptionPane.YES_OPTION) {
                    selectedAction = ConflictResolutionAction.PUSH;
                    close(OK_EXIT_CODE);
                }
            }
        };
        
        return new Action[]{pullAction, pushAction, getCancelAction()};
    }
    
    public ConflictResolutionAction getSelectedAction() {
        return selectedAction;
    }
    
    /**
     * Handle the user's resolution choice
     */
    private static void handleResolution(@NotNull Project project, 
                                        @NotNull ConflictResolutionAction action,
                                        @NotNull ConflictDetectionResult result) {
        SyncStatusService statusService = SyncStatusService.getInstance(project);
        SyncConfig config = SyncSettings.getInstance().getSyncConfig();
        SyncService syncService = SyncService.getInstance(project);
        
        switch (action) {
            case PULL:
                // Execute pull with merge mode
                statusService.markSyncing();
                com.intellij.openapi.progress.ProgressManager.getInstance().run(
                    new com.intellij.openapi.progress.Task.Backgroundable(
                        project, 
                        CodeReadingNoteBundle.message("progress.pulling.text"), 
                        false) {
                        
                        private SyncResult pullResult;
                        
                        @Override
                        public void run(@NotNull com.intellij.openapi.progress.ProgressIndicator indicator) {
                            pullResult = syncService.pull(config, true);
                        }
                        
                        @Override
                        public void onSuccess() {
                            if (pullResult.isSuccess()) {
                                statusService.resumeAutoPush();
                                statusService.markSynced();
                                com.intellij.openapi.ui.Messages.showInfoMessage(
                                    project,
                                    CodeReadingNoteBundle.message("sync.conflict.resolved.pull"),
                                    CodeReadingNoteBundle.message("sync.conflict.resolved.title")
                                );
                            } else {
                                statusService.setError(pullResult.getMessage());
                                com.intellij.openapi.ui.Messages.showErrorDialog(
                                    project,
                                    pullResult.getMessage(),
                                    CodeReadingNoteBundle.message("message.pull.failed.title")
                                );
                            }
                        }
                    }
                );
                break;
                
            case PUSH:
                // Force push local to remote
                statusService.markSyncing();
                com.intellij.openapi.progress.ProgressManager.getInstance().run(
                    new com.intellij.openapi.progress.Task.Backgroundable(
                        project,
                        CodeReadingNoteBundle.message("progress.pushing.text"),
                        false) {
                        
                        private SyncResult pushResult;
                        
                        @Override
                        public void run(@NotNull com.intellij.openapi.progress.ProgressIndicator indicator) {
                            // 1. 强制刷盘
                            indicator.setText("Saving current state...");
                            forceSaveProjectState(project);
                            
                            // 2. Temporarily resume to allow push
                            statusService.resumeAutoPush();
                            
                            // 3. 执行 Push
                            indicator.setText(CodeReadingNoteBundle.message("progress.pushing.text"));
                            pushResult = syncService.push(config);
                        }
                        
                        @Override
                        public void onSuccess() {
                            if (pushResult.isSuccess()) {
                                statusService.markSynced();
                                com.intellij.openapi.ui.Messages.showInfoMessage(
                                    project,
                                    CodeReadingNoteBundle.message("sync.conflict.resolved.push"),
                                    CodeReadingNoteBundle.message("sync.conflict.resolved.title")
                                );
                            } else {
                                statusService.setError(pushResult.getMessage());
                                statusService.pauseAutoPush(); // Re-pause on failure
                                com.intellij.openapi.ui.Messages.showErrorDialog(
                                    project,
                                    pushResult.getMessage(),
                                    CodeReadingNoteBundle.message("message.push.failed.title")
                                );
                            }
                        }
                    }
                );
                break;
                
            case CANCEL:
                // Keep auto-sync paused
                // Status remains CONFLICT_PAUSED
                break;
        }
    }
    
    /**
     * 强制触发状态序列化
     * 
     * 说明：调用 getState() 会触发 IDE 的 StateStorageManager 自动序列化并保存到 XML。
     * 虽然无法确保立即写入磁盘（IDE 可能异步处理），但可以确保最新的内存数据被序列化。
     */
    private static void forceSaveProjectState(@NotNull Project project) {
        try {
            // 调用 getState() 触发 StateStorageManager 序列化
            jp.kitabatakep.intellij.plugins.codereadingnote.CodeReadingNoteService service = 
                jp.kitabatakep.intellij.plugins.codereadingnote.CodeReadingNoteService.getInstance(project);
            service.getState();
            
        } catch (Exception ex) {
            com.intellij.openapi.diagnostic.Logger.getInstance(SyncConflictDialog.class)
                .warn("Failed to trigger state serialization", ex);
        }
    }
}


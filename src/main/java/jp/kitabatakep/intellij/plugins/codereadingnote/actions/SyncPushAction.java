package jp.kitabatakep.intellij.plugins.codereadingnote.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import jp.kitabatakep.intellij.plugins.codereadingnote.CodeReadingNoteBundle;
import jp.kitabatakep.intellij.plugins.codereadingnote.CodeReadingNoteService;
import jp.kitabatakep.intellij.plugins.codereadingnote.sync.SyncConfig;
import jp.kitabatakep.intellij.plugins.codereadingnote.sync.SyncResult;
import jp.kitabatakep.intellij.plugins.codereadingnote.sync.SyncService;
import jp.kitabatakep.intellij.plugins.codereadingnote.sync.SyncSettings;
import org.jetbrains.annotations.NotNull;

/**
 * 推送同步操作
 */
public class SyncPushAction extends CommonAnAction {
    
    public SyncPushAction() {
        super(
            CodeReadingNoteBundle.message("action.sync.push"),
            CodeReadingNoteBundle.message("action.sync.push.description"),
            AllIcons.Actions.Upload
        );
    }
    
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }
        
        // 获取同步配置
        SyncConfig config = SyncSettings.getInstance().getSyncConfig();
        
        if (!config.isEnabled()) {
            Messages.showWarningDialog(project, 
                CodeReadingNoteBundle.message("message.sync.not.enabled"), 
                CodeReadingNoteBundle.message("message.sync.not.enabled.title"));
            return;
        }
        
        // 验证配置
        String validationError = config.validate();
        if (validationError != null) {
            Messages.showErrorDialog(project, 
                CodeReadingNoteBundle.message("message.sync.config.error", validationError), 
                CodeReadingNoteBundle.message("message.sync.config.error.title"));
            return;
        }
        
        // 在后台执行同步
        ProgressManager.getInstance().run(new Task.Backgroundable(project, CodeReadingNoteBundle.message("progress.pushing"), true) {
            private SyncResult result;
            
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                indicator.setText(CodeReadingNoteBundle.message("progress.pushing.text"));
                
                // 1. 强制刷盘 - 确保当前数据已持久化
                indicator.setText("Saving current state...");
                forceSaveProjectState(project);
                
                // 2. 执行 Push
                indicator.setText(CodeReadingNoteBundle.message("progress.pushing.text"));
                SyncService syncService = SyncService.getInstance(project);
                result = syncService.push(config);
            }
            
            @Override
            public void onSuccess() {
                if (result.isSuccess()) {
                    // 3. Push 成功后标记为已同步
                    // 注意：不需要再次刷盘 CodeReadingNote.xml，因为数据没有变化
                    // SyncStatusService 的 syncStatus.xml 会由 IDE 自动持久化（PersistentStateComponent）
                    jp.kitabatakep.intellij.plugins.codereadingnote.sync.SyncStatusService statusService = 
                        jp.kitabatakep.intellij.plugins.codereadingnote.sync.SyncStatusService.getInstance(project);
                    statusService.markSynced();
                    
                    // 检查是否是跳过推送的情况（通过比较消息内容）
                    String noChangesMsg = CodeReadingNoteBundle.message("message.push.no.changes");
                    String title = result.getUserMessage().equals(noChangesMsg) ?
                        CodeReadingNoteBundle.message("message.push.no.changes.title") :
                        CodeReadingNoteBundle.message("message.push.successful.title");
                    
                    Messages.showInfoMessage(project, 
                        result.getUserMessage(), 
                        title);
                } else {
                    // Push 失败，标记为错误
                    jp.kitabatakep.intellij.plugins.codereadingnote.sync.SyncStatusService statusService = 
                        jp.kitabatakep.intellij.plugins.codereadingnote.sync.SyncStatusService.getInstance(project);
                    statusService.setError(result.getMessage());
                    
                    Messages.showErrorDialog(project, 
                        result.getUserMessage(), 
                        CodeReadingNoteBundle.message("message.push.failed.title"));
                }
            }
            
            @Override
            public void onThrowable(@NotNull Throwable error) {
                Messages.showErrorDialog(project, 
                    CodeReadingNoteBundle.message("message.push.error", error.getMessage()), 
                    CodeReadingNoteBundle.message("message.push.failed.title"));
            }
        });
    }
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            e.getPresentation().setEnabled(false);
            return;
        }
        
        SyncConfig config = SyncSettings.getInstance().getSyncConfig();
        e.getPresentation().setEnabled(config.isEnabled());
    }
    
    /**
     * 强制触发状态序列化
     * 
     * 说明：调用 getState() 会触发 IDE 的 StateStorageManager 自动序列化并保存到 XML。
     * 虽然无法确保立即写入磁盘（IDE 可能异步处理），但可以确保最新的内存数据被序列化。
     * 
     * 注意：saveAllDocuments() 只保存编辑器文档，对 PersistentStateComponent 的 XML 无效，
     * 因此已移除。
     */
    private void forceSaveProjectState(@NotNull Project project) {
        try {
            // 调用 getState() 触发 StateStorageManager 序列化
            // IDE 会自动将返回的 Element 保存到 CodeReadingNote.xml
            CodeReadingNoteService service = CodeReadingNoteService.getInstance(project);
            service.getState();
            
            // 可选：添加短暂延迟让 IDE 完成异步保存
            // 但通常不需要，因为 Push 操作本身会读取文件，此时 IDE 应该已经完成保存
            
        } catch (Exception ex) {
            // 不影响后续 Push，只记录日志
            com.intellij.openapi.diagnostic.Logger.getInstance(SyncPushAction.class)
                .warn("Failed to trigger state serialization", ex);
        }
    }
}


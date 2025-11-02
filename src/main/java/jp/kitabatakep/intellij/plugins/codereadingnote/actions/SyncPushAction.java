package jp.kitabatakep.intellij.plugins.codereadingnote.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
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
        super("Push to Remote", "Push notes to remote repository", null);
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
                "Sync is not enabled. Please configure it in Settings first.", 
                "Sync Not Enabled");
            return;
        }
        
        // 验证配置
        String validationError = config.validate();
        if (validationError != null) {
            Messages.showErrorDialog(project, 
                "Sync configuration incomplete: " + validationError, 
                "Configuration Error");
            return;
        }
        
        // 在后台执行同步
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Pushing Notes", true) {
            private SyncResult result;
            
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                indicator.setText("Pushing to remote...");
                
                SyncService syncService = SyncService.getInstance(project);
                result = syncService.push(config);
            }
            
            @Override
            public void onSuccess() {
                if (result.isSuccess()) {
                    Messages.showInfoMessage(project, 
                        result.getUserMessage(), 
                        "Push Successful");
                } else {
                    Messages.showErrorDialog(project, 
                        result.getUserMessage(), 
                        "Push Failed");
                }
            }
            
            @Override
            public void onThrowable(@NotNull Throwable error) {
                Messages.showErrorDialog(project, 
                    "Error occurred during push: " + error.getMessage(), 
                    "Push Failed");
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
}


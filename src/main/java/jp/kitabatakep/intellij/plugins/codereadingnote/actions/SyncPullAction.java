package jp.kitabatakep.intellij.plugins.codereadingnote.actions;

import com.intellij.icons.AllIcons;
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
 * 拉取同步操作
 */
public class SyncPullAction extends CommonAnAction {
    
    public SyncPullAction() {
        super("Pull from Remote", "Pull notes from remote repository", AllIcons.Actions.Download);
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
        
        // 询问合并策略
        int choice = Messages.showYesNoCancelDialog(
            project,
            "Choose pull mode:\n" +
            "- Yes: Merge remote data (keep local data)\n" +
            "- No: Overwrite local data (local data will be lost)\n" +
            "- Cancel: Cancel operation",
            "Pull Mode",
            "Merge",
            "Overwrite",
            "Cancel",
            Messages.getQuestionIcon()
        );
        
        if (choice == Messages.CANCEL) {
            return;
        }
        
        boolean merge = (choice == Messages.YES);
        
        // 在后台执行同步
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Pulling Notes", true) {
            private SyncResult result;
            
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                indicator.setText("Pulling from remote...");
                
                SyncService syncService = SyncService.getInstance(project);
                result = syncService.pull(config, merge);
            }
            
            @Override
            public void onSuccess() {
                if (result.isSuccess()) {
                    Messages.showInfoMessage(project, 
                        result.getUserMessage(), 
                        "Pull Successful");
                } else {
                    Messages.showErrorDialog(project, 
                        result.getUserMessage(), 
                        "Pull Failed");
                }
            }
            
            @Override
            public void onThrowable(@NotNull Throwable error) {
                Messages.showErrorDialog(project, 
                    "Error occurred during pull: " + error.getMessage(), 
                    "Pull Failed");
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


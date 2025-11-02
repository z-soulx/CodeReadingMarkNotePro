package jp.kitabatakep.intellij.plugins.codereadingnote.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import jp.kitabatakep.intellij.plugins.codereadingnote.CodeReadingNoteBundle;
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
                
                SyncService syncService = SyncService.getInstance(project);
                result = syncService.push(config);
            }
            
            @Override
            public void onSuccess() {
                if (result.isSuccess()) {
                    Messages.showInfoMessage(project, 
                        result.getUserMessage(), 
                        CodeReadingNoteBundle.message("message.push.successful.title"));
                } else {
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
}


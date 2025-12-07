package jp.kitabatakep.intellij.plugins.codereadingnote.sync;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import jp.kitabatakep.intellij.plugins.codereadingnote.sync.ui.SyncConflictDialog;
import org.jetbrains.annotations.NotNull;

/**
 * Startup activity to check for sync conflicts on project open
 */
public class SyncStartupActivity implements StartupActivity {
    
    private static final Logger LOG = Logger.getInstance(SyncStartupActivity.class);
    
    @Override
    public void runActivity(@NotNull Project project) {
        // Check if sync is enabled
        SyncConfig config = SyncSettings.getInstance().getSyncConfig();
        
        if (!config.isEnabled() || !config.isAutoSync()) {
            return;
        }
        
        // Check remote for updates in background thread
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                SyncConflictDetector detector = SyncConflictDetector.getInstance(project);
                ConflictDetectionResult result = detector.checkRemoteUpdate();
                
                SyncStatusService statusService = SyncStatusService.getInstance(project);
                
                switch (result.getConflictType()) {
                    case BOTH_MODIFIED:
                        // Both sides modified - need user decision
                        LOG.warn("Sync conflict detected on startup, showing dialog");
                        statusService.pauseAutoPush();
                        
                        ApplicationManager.getApplication().invokeLater(() -> {
                            if (!project.isDisposed()) {
                                SyncConflictDialog.show(project, result);
                            }
                        });
                        break;
                        
                    case LOCAL_MODIFIED:
                        // Local has changes - mark as pending (will auto-push later)
                        statusService.markPending();
                        break;
                        
                    case REMOTE_UPDATED:
                        // Remote has updates - notify user
                        statusService.markPending();
                        break;
                        
                    case NO_CONFLICT:
                        // All synced
                        statusService.markSynced();
                        break;
                }
            } catch (Exception e) {
                LOG.error("Failed to check for sync conflicts on startup", e);
            }
        });
    }
}


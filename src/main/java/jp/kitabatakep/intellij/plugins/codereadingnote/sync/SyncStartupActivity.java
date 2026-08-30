package jp.kitabatakep.intellij.plugins.codereadingnote.sync;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import jp.kitabatakep.intellij.plugins.codereadingnote.sync.ui.SyncConflictDialog;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * After project open, check remote sync state when auto-sync is enabled.
 */
public class SyncStartupActivity implements ProjectActivity {

    private static final Logger LOG = Logger.getInstance(SyncStartupActivity.class);

    @Override
    public @Nullable Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        checkSyncOnOpen(project);
        return Unit.INSTANCE;
    }

    private static void checkSyncOnOpen(@NotNull Project project) {
        SyncConfig config = SyncSettings.getInstance().getSyncConfig();
        if (!config.isEnabled() || !config.isAutoSync()) {
            return;
        }

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                SyncConflictDetector detector = SyncConflictDetector.getInstance(project);
                ConflictDetectionResult result = detector.checkRemoteUpdate();
                SyncStatusService statusService = SyncStatusService.getInstance(project);

                switch (result.getConflictType()) {
                    case BOTH_MODIFIED:
                        LOG.warn("Sync conflict detected on startup, showing dialog");
                        statusService.pauseAutoPush();
                        ApplicationManager.getApplication().invokeLater(() -> {
                            if (!project.isDisposed()) {
                                SyncConflictDialog.show(project, result);
                            }
                        });
                        break;
                    case LOCAL_MODIFIED:
                    case REMOTE_UPDATED:
                        statusService.markPending();
                        break;
                    case NO_CONFLICT:
                        statusService.markSynced();
                        break;
                }
            } catch (Exception e) {
                LOG.error("Failed to check for sync conflicts on startup", e);
            }
        });
    }
}

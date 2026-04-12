package jp.kitabatakep.intellij.plugins.codereadingnote.aiconfig;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import jp.kitabatakep.intellij.plugins.codereadingnote.sync.SyncConfig;
import jp.kitabatakep.intellij.plugins.codereadingnote.sync.SyncResult;
import jp.kitabatakep.intellij.plugins.codereadingnote.sync.SyncSettings;
import jp.kitabatakep.intellij.plugins.codereadingnote.ui.AIConfigSyncConflictDialog;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Auto-sync scheduler for AI config files.
 * Debounces VFS change events and pushes tracked AI configs to remote after a delay.
 * When a remote conflict is detected, auto-push is paused and a modal conflict dialog is shown.
 */
@Service(Service.Level.PROJECT)
public final class AIConfigAutoSyncScheduler {

    private static final Logger LOG = Logger.getInstance(AIConfigAutoSyncScheduler.class);
    private static final int DEBOUNCE_DELAY_SECONDS = 5;

    private final Project project;
    private final ScheduledExecutorService scheduler;
    private ScheduledFuture<?> pendingSync;
    private volatile boolean autoPushPaused = false;

    public AIConfigAutoSyncScheduler(@NotNull Project project) {
        this.project = project;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "AIConfigAutoSync-" + project.getName());
            thread.setDaemon(true);
            return thread;
        });
    }

    @NotNull
    public static AIConfigAutoSyncScheduler getInstance(@NotNull Project project) {
        return project.getService(AIConfigAutoSyncScheduler.class);
    }

    public void scheduleAutoSync() {
        SyncConfig config = SyncSettings.getInstance().getSyncConfig();
        if (!config.isEnabled() || !config.isAiConfigAutoSync()) return;

        if (autoPushPaused) {
            LOG.info("AI config auto-sync is paused due to conflict, skipping schedule");
            return;
        }

        if (pendingSync != null && !pendingSync.isDone()) {
            pendingSync.cancel(false);
        }

        pendingSync = scheduler.schedule(() ->
                ApplicationManager.getApplication().executeOnPooledThread(this::executePush),
                DEBOUNCE_DELAY_SECONDS, TimeUnit.SECONDS);
    }

    public boolean isAutoPushPaused() {
        return autoPushPaused;
    }

    public void pauseAutoSync() {
        autoPushPaused = true;
        LOG.info("AI config auto-sync paused");
    }

    public void resumeAutoSync() {
        autoPushPaused = false;
        LOG.info("AI config auto-sync resumed");
    }

    private void executePush() {
        try {
            if (autoPushPaused) return;

            SyncConfig config = SyncSettings.getInstance().getSyncConfig();
            if (!config.isEnabled() || !config.isAiConfigAutoSync()) return;

            String validationError = config.validate();
            if (validationError != null) {
                LOG.warn("AI config auto-sync skipped: " + validationError);
                return;
            }

            AIConfigService aiService = AIConfigService.getInstance(project);
            if (aiService.getLastSyncedRemoteMetadataHash().isEmpty()) {
                LOG.info("AI config auto-sync skipped: no prior sync history, manual push/pull required first");
                return;
            }

            AIConfigSyncAdapter adapter = AIConfigSyncAdapter.getInstance(project);

            if (adapter.checkRemoteConflict(config, project.getName())) {
                LOG.warn("AI config auto-sync paused: remote has newer changes");
                pauseAutoSync();
                showConflictDialog();
                return;
            }

            SyncResult result = adapter.pushAIConfigs(config, project.getName(), false);

            if (result.isSuccess()) {
                LOG.info("AI config auto-sync push completed: " + result.getMessage());
            } else {
                LOG.warn("AI config auto-sync push failed: " + result.getMessage());
            }
        } catch (Exception e) {
            LOG.error("AI config auto-sync error", e);
        }
    }

    private void showConflictDialog() {
        ApplicationManager.getApplication().invokeLater(() -> {
            if (project.isDisposed()) return;
            AIConfigSyncConflictDialog.show(project);
        });
    }

    public void shutdown() {
        if (pendingSync != null && !pendingSync.isDone()) {
            pendingSync.cancel(false);
        }
        scheduler.shutdown();
    }
}

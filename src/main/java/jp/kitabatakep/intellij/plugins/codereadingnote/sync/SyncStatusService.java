package jp.kitabatakep.intellij.plugins.codereadingnote.sync;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Service for tracking and managing sync status
 * Handles pause/resume of auto-sync and status updates
 * 
 * Note: This stores CLIENT-SIDE state only (lastSyncTime, MD5 cache, etc.)
 * These states should NOT be synced to remote, as each device has its own state.
 * Business data (Topics, TopicLines) are stored in CodeReadingNoteService.
 */
@Service(Service.Level.PROJECT)
@State(
    name = "SyncStatusService",
    storages = @Storage("syncStatus.xml")
)
public final class SyncStatusService implements PersistentStateComponent<SyncStatusService.State> {
    
    private final Project project;
    private State state = new State();
    private SyncStatus currentStatus = SyncStatus.DISABLED;
    private String lastErrorMessage = null;
    private boolean autoPushPaused = false;
    
    /**
     * Persistent state for client-side sync information
     * These are NOT synced to remote
     */
    public static class State {
        public long lastSyncTime = 0;        // 最后一次成功同步的时间
        public String lastLocalMd5 = null;   // 最后一次本地数据的MD5（可选，用于缓存）
    }
    
    public SyncStatusService(@NotNull Project project) {
        this.project = project;
    }
    
    @Nullable
    @Override
    public State getState() {
        return state;
    }
    
    @Override
    public void loadState(@NotNull State state) {
        XmlSerializerUtil.copyBean(state, this.state);
    }
    
    @NotNull
    public static SyncStatusService getInstance(@NotNull Project project) {
        return project.getService(SyncStatusService.class);
    }
    
    /**
     * Get current sync status
     */
    @NotNull
    public SyncStatus getCurrentStatus() {
        // Check if sync is enabled first
        SyncConfig config = SyncSettings.getInstance().getSyncConfig();
        if (!config.isEnabled()) {
            return SyncStatus.DISABLED;
        }
        
        return currentStatus;
    }
    
    /**
     * Update sync status and notify listeners
     */
    public void updateStatus(@NotNull SyncStatus newStatus) {
        if (currentStatus != newStatus) {
            currentStatus = newStatus;
            
            // Notify listeners
            MessageBus messageBus = project.getMessageBus();
            SyncStatusNotifier publisher = messageBus.syncPublisher(SyncStatusNotifier.SYNC_STATUS_TOPIC);
            publisher.statusChanged(newStatus);
        }
    }
    
    /**
     * Get last sync time
     */
    public long getLastSyncTime() {
        return state.lastSyncTime;
    }
    
    /**
     * Update last sync time and persist it
     */
    public void updateLastSyncTime(long timestamp) {
        state.lastSyncTime = timestamp;
        
        // Notify listeners
        MessageBus messageBus = project.getMessageBus();
        SyncStatusNotifier publisher = messageBus.syncPublisher(SyncStatusNotifier.SYNC_STATUS_TOPIC);
        publisher.lastSyncTimeUpdated(timestamp);
    }
    
    /**
     * Get last error message (if status is ERROR)
     */
    @Nullable
    public String getLastErrorMessage() {
        return lastErrorMessage;
    }
    
    /**
     * Set error message and update status to ERROR
     */
    public void setError(@NotNull String errorMessage) {
        this.lastErrorMessage = errorMessage;
        updateStatus(SyncStatus.ERROR);
    }
    
    /**
     * Clear error and return to appropriate status
     */
    public void clearError() {
        this.lastErrorMessage = null;
        if (currentStatus == SyncStatus.ERROR) {
            updateStatus(SyncStatus.SYNCED);
        }
    }
    
    /**
     * Check if auto-push is currently paused
     */
    public boolean isAutoSyncPaused() {
        return autoPushPaused;
    }
    
    /**
     * Pause auto-push (typically due to conflict detection)
     */
    public void pauseAutoPush() {
        if (!autoPushPaused) {
            autoPushPaused = true;
            updateStatus(SyncStatus.CONFLICT_PAUSED);
        }
    }
    
    /**
     * Resume auto-push after conflict resolution
     */
    public void resumeAutoPush() {
        if (autoPushPaused) {
            autoPushPaused = false;
            updateStatus(SyncStatus.SYNCED);
        }
    }
    
    /**
     * Get last local data MD5 hash (cached)
     */
    @NotNull
    public String getLastLocalDataMd5() {
        return state.lastLocalMd5 != null ? state.lastLocalMd5 : "";
    }
    
    /**
     * Update last local data MD5 hash (cached)
     */
    public void updateLastLocalDataMd5(@NotNull String md5) {
        state.lastLocalMd5 = md5;
    }
    
    /**
     * Mark as dirty (data modified but not yet persisted by IDE)
     */
    public void markDirty() {
        if (currentStatus != SyncStatus.SYNCING && currentStatus != SyncStatus.CONFLICT_PAUSED) {
            updateStatus(SyncStatus.DIRTY);
        }
    }
    
    /**
     * Mark sync as in progress
     */
    public void markSyncing() {
        updateStatus(SyncStatus.SYNCING);
    }
    
    /**
     * Mark sync as completed successfully
     */
    public void markSynced() {
        clearError();
        updateStatus(SyncStatus.SYNCED);
    }
    
    /**
     * Mark that there are pending changes to sync
     */
    public void markPending() {
        if (currentStatus != SyncStatus.SYNCING && currentStatus != SyncStatus.CONFLICT_PAUSED) {
            updateStatus(SyncStatus.PENDING);
        }
    }
}


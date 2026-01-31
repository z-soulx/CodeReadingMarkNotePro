package jp.kitabatakep.intellij.plugins.codereadingnote.sync;

import org.jetbrains.annotations.NotNull;

/**
 * Sync status enumeration
 */
public enum SyncStatus {
    /**
     * Local and remote are in sync
     */
    SYNCED("synced"),
    
    /**
     * Data modified but not yet persisted by IDE
     * Waiting for IDE to save state
     */
    DIRTY("dirty"),
    
    /**
     * Local has changes that need to be synced
     * (After IDE persisted the changes)
     */
    PENDING("pending"),
    
    /**
     * Currently syncing
     */
    SYNCING("syncing"),
    
    /**
     * Sync failed with error
     */
    ERROR("error"),
    
    /**
     * Auto-sync paused due to conflict detection
     */
    CONFLICT_PAUSED("conflict.paused"),
    
    /**
     * Sync is not enabled
     */
    DISABLED("disabled");
    
    private final String messageKey;
    
    SyncStatus(String messageKey) {
        this.messageKey = messageKey;
    }
    
    /**
     * Get the message bundle key for this status
     */
    @NotNull
    public String getMessageKey() {
        return "sync.status." + messageKey;
    }
}


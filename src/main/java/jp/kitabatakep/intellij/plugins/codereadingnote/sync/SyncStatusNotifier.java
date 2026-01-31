package jp.kitabatakep.intellij.plugins.codereadingnote.sync;

import com.intellij.util.messages.Topic;
import org.jetbrains.annotations.NotNull;

/**
 * Notifier interface for sync status changes
 * Used with MessageBus for pub-sub pattern
 */
public interface SyncStatusNotifier {
    
    Topic<SyncStatusNotifier> SYNC_STATUS_TOPIC =
        Topic.create("SyncStatusNotifier", SyncStatusNotifier.class);
    
    /**
     * Called when sync status changes
     * @param status New sync status
     */
    void statusChanged(@NotNull SyncStatus status);
    
    /**
     * Called when last sync time updates
     * @param timestamp Last sync timestamp in milliseconds
     */
    default void lastSyncTimeUpdated(long timestamp) {}
}



package jp.kitabatakep.intellij.plugins.codereadingnote.sync;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Result of conflict detection check
 */
public class ConflictDetectionResult {
    
    public enum ConflictType {
        NO_CONFLICT,           // 已同步，无冲突
        LOCAL_MODIFIED,        // 本地有修改待推送
        REMOTE_UPDATED,        // 远端有更新可拉取
        BOTH_MODIFIED          // 双方都有修改，需要用户决策
    }
    
    private final ConflictType conflictType;
    private final boolean hasConflict;
    private final long remoteTimestamp;
    private final long localTimestamp;
    private final int remoteTopicCount;
    private final int localTopicCount;
    private final int remoteTopicLineCount;
    private final int localTopicLineCount;
    private final boolean localModified;  // 本地数据是否被修改（MD5不同）
    private final String message;
    
    private ConflictDetectionResult(ConflictType conflictType,
                                   boolean hasConflict, 
                                   long remoteTimestamp, 
                                   long localTimestamp,
                                   int remoteTopicCount,
                                   int localTopicCount,
                                   int remoteTopicLineCount,
                                   int localTopicLineCount,
                                   boolean localModified,
                                   String message) {
        this.conflictType = conflictType;
        this.hasConflict = hasConflict;
        this.remoteTimestamp = remoteTimestamp;
        this.localTimestamp = localTimestamp;
        this.remoteTopicCount = remoteTopicCount;
        this.localTopicCount = localTopicCount;
        this.remoteTopicLineCount = remoteTopicLineCount;
        this.localTopicLineCount = localTopicLineCount;
        this.localModified = localModified;
        this.message = message;
    }
    
    /**
     * Create a conflict result (both sides modified)
     */
    @NotNull
    public static ConflictDetectionResult bothModified(long remoteTimestamp, 
                                                       long localTimestamp,
                                                       int remoteTopicCount,
                                                       int localTopicCount,
                                                       int remoteTopicLineCount,
                                                       int localTopicLineCount) {
        return new ConflictDetectionResult(ConflictType.BOTH_MODIFIED, true, 
                                          remoteTimestamp, localTimestamp, 
                                          remoteTopicCount, localTopicCount,
                                          remoteTopicLineCount, localTopicLineCount,
                                          true,
                                          "Both remote and local have been modified");
    }
    
    /**
     * Create a local modified result (can push)
     */
    @NotNull
    public static ConflictDetectionResult localModified(long localTimestamp,
                                                        int localTopicCount,
                                                        int localTopicLineCount) {
        return new ConflictDetectionResult(ConflictType.LOCAL_MODIFIED, false,
                                          0, localTimestamp,
                                          -1, localTopicCount,
                                          -1, localTopicLineCount,
                                          true,
                                          "Local has unpushed changes");
    }
    
    /**
     * Create a remote updated result (can pull)
     */
    @NotNull
    public static ConflictDetectionResult remoteUpdated(long remoteTimestamp,
                                                        long localTimestamp,
                                                        int remoteTopicCount,
                                                        int localTopicCount,
                                                        int remoteTopicLineCount,
                                                        int localTopicLineCount) {
        return new ConflictDetectionResult(ConflictType.REMOTE_UPDATED, false,
                                          remoteTimestamp, localTimestamp,
                                          remoteTopicCount, localTopicCount,
                                          remoteTopicLineCount, localTopicLineCount,
                                          false,
                                          "Remote has updates available");
    }
    
    /**
     * Create a no-conflict result (synced)
     */
    @NotNull
    public static ConflictDetectionResult noConflict(String message) {
        return new ConflictDetectionResult(ConflictType.NO_CONFLICT, false, 
                                          0, 0, 0, 0, 0, 0, false, message);
    }
    
    /**
     * Create an error result
     */
    @NotNull
    public static ConflictDetectionResult error(String message) {
        return new ConflictDetectionResult(ConflictType.NO_CONFLICT, false, 
                                          0, 0, 0, 0, 0, 0, false, message);
    }
    
    public ConflictType getConflictType() {
        return conflictType;
    }
    
    public boolean hasConflict() {
        return hasConflict;
    }
    
    public boolean isLocalModified() {
        return localModified;
    }
    
    public boolean needsUserDecision() {
        return conflictType == ConflictType.BOTH_MODIFIED;
    }
    
    public boolean canAutoPush() {
        return conflictType == ConflictType.LOCAL_MODIFIED;
    }
    
    public boolean canAutoPull() {
        return conflictType == ConflictType.REMOTE_UPDATED;
    }
    
    public long getRemoteTimestamp() {
        return remoteTimestamp;
    }
    
    public long getLocalTimestamp() {
        return localTimestamp;
    }
    
    public int getRemoteTopicCount() {
        return remoteTopicCount;
    }
    
    public int getLocalTopicCount() {
        return localTopicCount;
    }
    
    public int getRemoteTopicLineCount() {
        return remoteTopicLineCount;
    }
    
    public int getLocalTopicLineCount() {
        return localTopicLineCount;
    }
    
    @Nullable
    public String getMessage() {
        return message;
    }
}


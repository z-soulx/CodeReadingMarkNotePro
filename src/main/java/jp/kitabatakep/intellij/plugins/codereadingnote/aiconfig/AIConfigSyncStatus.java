package jp.kitabatakep.intellij.plugins.codereadingnote.aiconfig;

/**
 * Sync status of an individual AI config file relative to the last push.
 */
public enum AIConfigSyncStatus {
    /** File has been pushed and content matches the last push */
    SYNCED,
    /** File exists locally but has never been pushed */
    NEW,
    /** File has been modified since the last push */
    MODIFIED
}

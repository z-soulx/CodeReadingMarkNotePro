package jp.kitabatakep.intellij.plugins.codereadingnote.aiconfig;

/**
 * Categories for per-file merge analysis during AI config pull.
 * Determined by three-way comparison: local hash, remote hash, base hash (last synced).
 */
public enum AIConfigMergeCategory {
    /** File exists on remote but not locally. */
    NEW_REMOTE,
    /** File exists locally and in base, but was deleted on remote. */
    DELETED_REMOTE,
    /** Only remote changed vs base (local unchanged). */
    REMOTE_CHANGED,
    /** Only local changed vs base (remote unchanged). */
    LOCAL_CHANGED,
    /** Both local and remote changed vs base. */
    BOTH_CHANGED,
    /** No changes — local and remote are identical. */
    UNCHANGED
}

package jp.kitabatakep.intellij.plugins.codereadingnote.sync.workspace;

import java.util.Objects;

public enum NotesSyncDecision {
    SAME, LOCAL_CHANGED, REMOTE_CHANGED, CONFLICT, FIRST_SYNC, REMOTE_MISSING;
    public static NotesSyncDecision compare(String baseline, String local, String remote) {
        if (remote == null) return REMOTE_MISSING;
        if (Objects.equals(local, remote)) return SAME;
        if (baseline == null || baseline.isEmpty()) return FIRST_SYNC;
        if (baseline.equals(remote)) return LOCAL_CHANGED;
        if (baseline.equals(local)) return REMOTE_CHANGED;
        return CONFLICT;
    }
    public enum Direction { NONE, PUSH, PULL }
    public Direction automatic(NotesSyncBinding.Policy policy) {
        if (policy == NotesSyncBinding.Policy.MANUAL) return Direction.NONE;
        if (this == LOCAL_CHANGED) return Direction.PUSH;
        if (this == REMOTE_CHANGED && policy == NotesSyncBinding.Policy.BIDIRECTIONAL) return Direction.PULL;
        return Direction.NONE;
    }
}

package jp.kitabatakep.intellij.plugins.codereadingnote.sync.workspace;

/** Clock supplied by caller; one instance per application-coordinated project. */
public final class NotesSyncSchedule {
    private long revision = -1, editDue, pollDue;
    public void observe(long currentRevision, long now) {
        if (revision != currentRevision) { revision = currentRevision; editDue = now + 3000; }
    }
    public boolean due(long now, long checked, long activated, boolean paused, long retryAt) {
        if (paused || now < retryAt) return false;
        return now >= pollDue || editDue > 0 && now >= editDue || activated > checked && now - checked >= 60000;
    }
    public void attempted(long now, int intervalMinutes) { editDue = 0; pollDue = now + intervalMinutes * 60000L; }
    public void acknowledged(long currentRevision, boolean pending, long now) {
        revision = currentRevision; editDue = pending ? now + 3000 : 0;
    }
}

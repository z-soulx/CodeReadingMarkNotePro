package jp.kitabatakep.intellij.plugins.codereadingnote.sync.workspace;

import org.junit.Test;
import static org.junit.Assert.*;

public class NotesSyncScheduleTest {
    @Test public void startupThenPeriodicCheckDoesNotRequireAnyEdit() {
        NotesSyncSchedule s = new NotesSyncSchedule();
        assertTrue(s.due(10000, 0, 0, false, 0));
        s.attempted(10000, 5);
        assertFalse(s.due(309999, 10000, 0, false, 0));
        assertTrue(s.due(310000, 10000, 0, false, 0));
    }
    @Test public void editsDebounceAndSuccessfulRemoteApplyDoesNotEcho() {
        NotesSyncSchedule s = new NotesSyncSchedule(); s.attempted(10000, 5);
        s.observe(1, 11000); s.observe(2, 13000);
        assertFalse(s.due(15999, 10000, 0, false, 0)); assertTrue(s.due(16000, 10000, 0, false, 0));
        s.acknowledged(3, false, 16000); s.observe(3, 18000);
        assertFalse(s.due(20000, 16000, 0, false, 0));
        s.acknowledged(4, true, 21000);
        assertTrue(s.due(24000, 16000, 0, false, 0));
    }
    @Test public void activationThrottlesAndConflictOrBackoffOverridesAllTriggers() {
        NotesSyncSchedule s = new NotesSyncSchedule(); s.attempted(10000, 5);
        assertFalse(s.due(69999, 10000, 60000, false, 0));
        assertTrue(s.due(70000, 10000, 60000, false, 0));
        assertFalse(s.due(70000, 10000, 60000, true, 0));
        assertFalse(s.due(70000, 10000, 60000, false, 90000));
        assertTrue(s.due(90000, 10000, 60000, false, 90000));
    }
    @Test public void conflictPauseIsPerProject() {
        NotesSyncSchedule first = new NotesSyncSchedule(), second = new NotesSyncSchedule();
        assertFalse(first.due(10000, 0, 0, true, 0));
        assertTrue(second.due(10000, 0, 0, false, 0));
    }
}

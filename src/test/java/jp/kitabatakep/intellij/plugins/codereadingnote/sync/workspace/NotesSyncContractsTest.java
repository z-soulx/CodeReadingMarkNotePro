package jp.kitabatakep.intellij.plugins.codereadingnote.sync.workspace;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.jdom.Element;
import java.nio.file.*;
import static org.junit.Assert.*;

public class NotesSyncContractsTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();
    @Test public void baselineDistinguishesEveryDirectionIncludingConvergedEdits() {
        assertEquals(NotesSyncDecision.SAME, NotesSyncDecision.compare("old", "new", "new"));
        assertEquals(NotesSyncDecision.LOCAL_CHANGED, NotesSyncDecision.compare("old", "new", "old"));
        assertEquals(NotesSyncDecision.REMOTE_CHANGED, NotesSyncDecision.compare("old", "old", "new"));
        assertEquals(NotesSyncDecision.CONFLICT, NotesSyncDecision.compare("old", "left", "right"));
        assertEquals(NotesSyncDecision.FIRST_SYNC, NotesSyncDecision.compare("", "left", "right"));
        assertEquals(NotesSyncDecision.REMOTE_MISSING, NotesSyncDecision.compare("old", "old", null));
        assertEquals(NotesSyncDecision.SAME, NotesSyncDecision.compare(null, "empty", "empty"));
    }
    @Test public void autoPoliciesNeverInitializeOrResolveConflictsWithoutTheUser() {
        for (var policy : NotesSyncBinding.Policy.values()) {
            for (var unsafe : new NotesSyncDecision[]{NotesSyncDecision.FIRST_SYNC, NotesSyncDecision.CONFLICT, NotesSyncDecision.REMOTE_MISSING, NotesSyncDecision.SAME})
                assertEquals(NotesSyncDecision.Direction.NONE, unsafe.automatic(policy));
        }
        assertEquals(NotesSyncDecision.Direction.NONE, NotesSyncDecision.LOCAL_CHANGED.automatic(NotesSyncBinding.Policy.MANUAL));
        assertEquals(NotesSyncDecision.Direction.PUSH, NotesSyncDecision.LOCAL_CHANGED.automatic(NotesSyncBinding.Policy.PUSH));
        assertEquals(NotesSyncDecision.Direction.NONE, NotesSyncDecision.REMOTE_CHANGED.automatic(NotesSyncBinding.Policy.PUSH));
        assertEquals(NotesSyncDecision.Direction.PULL, NotesSyncDecision.REMOTE_CHANGED.automatic(NotesSyncBinding.Policy.BIDIRECTIONAL));
    }
    @Test public void canonicalXmlIgnoresFormattingButPreservesOrderNotesWhitespaceAndExtensions() throws Exception {
        Element a = NotesSyncXml.topics("<topics a='1' b='2'><topic><note> a </note></topic><future x='yes'/><trash/></topics>");
        Element b = NotesSyncXml.topics("<topics b='2' a='1'>\n <topic><note> a </note></topic>\n <future x='yes'/>\n <trash/>\n</topics>");
        assertEquals(NotesSyncXml.digest(a), NotesSyncXml.digest(b));
        b.getChild("topic").getChild("note").setText("a"); assertNotEquals(NotesSyncXml.digest(a), NotesSyncXml.digest(b));
        b = a.clone(); b.getChild("future").setAttribute("x", "no"); assertNotEquals(NotesSyncXml.digest(a), NotesSyncXml.digest(b));
        b = a.clone(); Element first = b.getChild("topic").detach(); b.addContent(first); assertNotEquals(NotesSyncXml.digest(a), NotesSyncXml.digest(b));
        assertNotEquals(NotesSyncXml.digest(NotesSyncXml.topics("<topics/>")), NotesSyncXml.digest(NotesSyncXml.topics("<topics><trash/></topics>")));
    }
    @Test public void maliciousXmlAndWrongRootsAreRejected() throws Exception {
        for (String xml : new String[]{"<!DOCTYPE topics [<!ENTITY e SYSTEM 'file:///sensitive'>]><topics>&e;</topics>", "<unrelated/>", "<topics>"}) {
            try { NotesSyncXml.topics(xml); fail("Invalid XML accepted"); } catch (java.io.IOException expected) { }
        }
    }
    @Test public void bindingsArePortablePartitionedAndRejectTraversal() throws Exception {
        Path a = temporary.newFolder("a").toPath(), b = temporary.newFolder("b").toPath();
        Files.createDirectory(a.resolve(".idea")); Files.createDirectory(b.resolve(".idea"));
        NotesSyncBinding first = new NotesSyncBinding("owner/repo", "feature/notes", "notes", "same", NotesSyncBinding.Policy.BIDIRECTIONAL, 5);
        NotesSyncBinding second = new NotesSyncBinding("owner/repo", "feature/notes", "notes", "other-same", NotesSyncBinding.Policy.MANUAL, 15);
        first.save(a); second.save(b);
        assertEquals(first, NotesSyncBinding.read(a)); assertEquals(second, NotesSyncBinding.read(b));
        assertNotEquals(first.identity(), second.identity());
        assertFalse(Files.readString(NotesSyncBinding.path(a)).contains(a.toString()));
        assertFalse(Files.readString(NotesSyncBinding.path(a)).contains("token"));
        for (String id : new String[]{"", "..", ".", "a/b", "a\\b", "a:b", "bad\nname"}) assertFalse(NotesSyncBinding.validId(id));
        Files.writeString(NotesSyncBinding.path(b), "<broken>");
        try { NotesSyncBinding.read(b); fail(); } catch (java.io.IOException expected) { }
        assertEquals("<broken>", Files.readString(NotesSyncBinding.path(b)));
    }
    @Test public void baselineAndPauseSurviveRestartAndSeparateProjects() throws Exception {
        Path first = temporary.getRoot().toPath().resolve("a.xml"), second = first.resolveSibling("b.xml");
        NotesSyncState s = new NotesSyncState(); s.baseline = "committed-snapshot"; s.remoteSha = "blob";
        s.paused = true; s.status = "conflict"; s.pendingHash = "in-flight"; s.save(first);
        NotesSyncState restored = NotesSyncState.read(first);
        assertTrue(restored.paused); assertEquals("committed-snapshot", restored.baseline); assertEquals("in-flight", restored.pendingHash);
        assertEquals("", NotesSyncState.read(second).baseline);
        Files.writeString(second, "broken");
        try { NotesSyncState.read(second); fail(); } catch (java.io.IOException expected) { }
        assertEquals("broken", Files.readString(second));
    }
}

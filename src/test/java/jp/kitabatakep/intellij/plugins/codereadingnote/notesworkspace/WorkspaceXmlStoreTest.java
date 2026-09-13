package jp.kitabatakep.intellij.plugins.codereadingnote.notesworkspace;

import org.jdom.Element;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;
import static org.junit.Assert.*;

public class WorkspaceXmlStoreTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();
    private Path file(String project) throws IOException {
        Path path = temporary.getRoot().toPath().resolve(project + "/.idea/CodeReadingNote.xml");
        Files.createDirectories(path.getParent());
        return path;
    }
    private static String legacy(String note) {
        return "<project version=\"4\"><component name=\"Other\"><option value=\"keep\"/></component>"
                + "<component name=\"CodeReadingNote\" extension=\"keep\"><topics custom=\"keep\"><extension/>"
                + "<topic><name>Same topic</name><note>" + note + "</note><updatedAt>2026-09-01 00:00:00</updatedAt>"
                + "<topicLines><topicLine><line>4</line><inProject>true</inProject><url>file:///old/src/Main.java</url>"
                + "<relativePath>src/Main.java</relativePath><note>备注</note><bookmarkUid>same-uid</bookmarkUid>"
                + "</topicLine></topicLines></topic></topics><state lastImportDir=\"keep\"/></component></project>";
    }
    @Test public void oldXmlRoundTripAndIndependentWritesRetainUnrelatedState() throws Exception {
        Path a = file("a"), b = file("b"), root = file("root");
        Files.writeString(a, legacy("A")); Files.writeString(b, legacy("B")); Files.writeString(root, legacy("Root"));
        byte[] untouchedB = Files.readAllBytes(b), untouchedRoot = Files.readAllBytes(root);
        WorkspaceXmlStore store = new WorkspaceXmlStore(a);
        Element topics = store.load();
        assertEquals("src/Main.java", topics.getChild("topic").getChild("topicLines").getChild("topicLine").getChildText("relativePath"));
        topics.getChild("topic").getChild("note").setText("修改");
        // The model exporter preserves entity extensions; the store preserves component and sibling data.
        topics.removeChild("extension");
        store.save(topics, false);
        String saved = Files.readString(a);
        assertTrue(saved.contains("name=\"Other\""));
        assertTrue(saved.contains("lastImportDir=\"keep\""));
        assertTrue(saved.contains("<extension"));
        assertTrue(saved.contains("same-uid"));
        assertEquals("修改", new WorkspaceXmlStore(a).load().getChild("topic").getChildText("note"));
        assertArrayEquals(untouchedB, Files.readAllBytes(b));
        assertArrayEquals(untouchedRoot, Files.readAllBytes(root));
    }
    @Test public void emptyProjectCreatesStorageOnlyOnFirstSave() throws Exception {
        Path path = file("empty");
        WorkspaceXmlStore store = new WorkspaceXmlStore(path);
        Element topics = store.load();
        assertFalse(Files.exists(path));
        store.save(topics, false);
        assertTrue(Files.readString(path).contains("name=\"CodeReadingNote\""));
        assertTrue(new WorkspaceXmlStore(path).load().getChildren().isEmpty());
    }
    @Test public void corruptAndUnsafeXmlCannotBecomeEmptyWritableState() throws Exception {
        for (String xml : List.of("<broken", "<unrelated/>", "<project><component name=\"CodeReadingNote\"/></project>",
                "<!DOCTYPE x [<!ENTITY x SYSTEM 'file:///secret'>]><project>&x;</project>")) {
            Path path = file("bad"); Files.writeString(path, xml);
            WorkspaceXmlStore store = new WorkspaceXmlStore(path);
            try { store.load(); fail("Invalid file accepted"); } catch (IOException expected) { }
            try { store.save(new Element("topics"), false); fail("Read failure overwritten"); } catch (IOException expected) { }
            assertEquals(xml, Files.readString(path));
        }
    }
    @Test public void externalChangeBlocksOverwriteAndCanBeReloaded() throws Exception {
        Path path = file("conflict"); Files.writeString(path, legacy("base"));
        WorkspaceXmlStore store = new WorkspaceXmlStore(path); Element local = store.load();
        Files.writeString(path, legacy("disk"));
        assertTrue(store.externallyChanged());
        try { store.save(local, false); fail("Conflict overwritten"); } catch (WorkspaceXmlStore.ConflictException expected) { }
        assertEquals("disk", store.load().getChild("topic").getChildText("note"));
        assertFalse(store.externallyChanged());
    }
    @Test public void backupThenSavePreservesExactExternalBytesAndLocalRecovery() throws Exception {
        Path path = file("conflict"); Files.writeString(path, legacy("base"));
        WorkspaceXmlStore store = new WorkspaceXmlStore(path); Element local = store.load();
        local.getChild("topic").getChild("note").setText("local");
        byte[] external = legacy("disk").getBytes(StandardCharsets.UTF_8);
        Files.write(path, external);
        store.preservePending(local);
        assertEquals("local", new WorkspaceXmlStore(path).readPending().getChild("topic").getChildText("note"));
        store.save(local, true);
        assertFalse(Files.exists(store.pendingPath()));
        try (var files = Files.list(path.getParent())) {
            Path backup = files.filter(p -> p.toString().endsWith(".bak")).findFirst().orElseThrow();
            assertArrayEquals(external, Files.readAllBytes(backup));
        }
        assertEquals("local", new WorkspaceXmlStore(path).load().getChild("topic").getChildText("note"));
    }
    @Test public void competingInstancesAndExternalDeletionAreConflicts() throws Exception {
        Path path = file("shared"); Files.writeString(path, legacy("base"));
        WorkspaceXmlStore first = new WorkspaceXmlStore(path), second = new WorkspaceXmlStore(path);
        Element a = first.load(), b = second.load(); a.getChild("topic").getChild("note").setText("first");
        first.save(a, false);
        try { second.save(b, false); fail("Second instance overwrote first"); } catch (WorkspaceXmlStore.ConflictException expected) { }
        Files.delete(path);
        try { first.save(a, false); fail("Deletion overwritten"); } catch (WorkspaceXmlStore.ConflictException expected) { }
        assertFalse(Files.exists(path));
    }
    @Test public void failedReplacementLeavesOriginalBytesAndPendingAvailable() throws Exception {
        Path path = file("failure"); Files.writeString(path, legacy("base"));
        WorkspaceXmlStore store = new WorkspaceXmlStore(path); Element local = store.load();
        store.preservePending(local);
        Files.delete(path); Files.createDirectory(path);
        try { store.save(local, false); fail("Directory replacement accepted"); } catch (IOException expected) { }
        assertTrue(Files.isDirectory(path));
        assertTrue(Files.exists(store.pendingPath()));
    }
}

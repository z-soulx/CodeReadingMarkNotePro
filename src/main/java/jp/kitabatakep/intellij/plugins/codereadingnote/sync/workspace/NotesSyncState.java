package jp.kitabatakep.intellij.plugins.codereadingnote.sync.workspace;

import org.jdom.Element;
import java.io.IOException;
import java.nio.file.*;

/** Local-only, never contains credentials or absolute source paths in the XML. */
public final class NotesSyncState {
    public String baseline = "", remoteSha = "", status = "first", pendingHash = "", pendingSha = "";
    public long checked, synced, retryAt;
    public int failures;
    public boolean paused;
    public static NotesSyncState read(Path file) throws IOException {
        NotesSyncState state = new NotesSyncState();
        if (!Files.exists(file)) return state;
        Element e = NotesSyncXml.parse(Files.readString(file));
        try {
            if (!"notesSyncState".equals(e.getName())) throw new IllegalArgumentException();
            state.baseline = e.getAttributeValue("baseline", ""); state.remoteSha = e.getAttributeValue("remoteSha", "");
            state.status = e.getAttributeValue("status", "first"); state.paused = Boolean.parseBoolean(e.getAttributeValue("paused"));
            state.checked = Long.parseLong(e.getAttributeValue("checked", "0")); state.synced = Long.parseLong(e.getAttributeValue("synced", "0"));
            state.pendingHash = e.getAttributeValue("pendingHash", ""); state.pendingSha = e.getAttributeValue("pendingSha", "");
        } catch (RuntimeException error) { throw new IOException("Invalid sync state", error); }
        return state;
    }
    public void save(Path file) throws IOException {
        NotesSyncXml.atomicWrite(file, NotesSyncXml.write(new Element("notesSyncState").setAttribute("version", "1")
                .setAttribute("baseline", baseline).setAttribute("remoteSha", remoteSha).setAttribute("status", status)
                .setAttribute("checked", "" + checked).setAttribute("synced", "" + synced).setAttribute("paused", "" + paused)
                .setAttribute("pendingHash", pendingHash).setAttribute("pendingSha", pendingSha)));
    }
}

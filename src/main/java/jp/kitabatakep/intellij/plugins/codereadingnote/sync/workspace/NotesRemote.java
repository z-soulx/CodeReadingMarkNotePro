package jp.kitabatakep.intellij.plugins.codereadingnote.sync.workspace;

import java.io.IOException;
import java.util.List;

public interface NotesRemote {
    record Snapshot(String xml, String sha, String etag) { public boolean exists() { return xml != null; } }
    Snapshot read(String id, Snapshot cached) throws IOException;
    Snapshot push(String id, String xml, String expectedSha) throws IOException;
    List<String> projects() throws IOException;
    final class Failure extends IOException {
        public final String key;
        public final long retryAt;
        public Failure(String key, long retryAt) { super(key); this.key = key; this.retryAt = retryAt; }
    }
}

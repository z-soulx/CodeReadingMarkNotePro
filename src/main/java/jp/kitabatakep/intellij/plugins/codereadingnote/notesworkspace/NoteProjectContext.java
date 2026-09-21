package jp.kitabatakep.intellij.plugins.codereadingnote.notesworkspace;

import java.nio.file.Path;
import java.util.Objects;

/** Runtime identity only: never included in the legacy XML or sync payload. */
public final class NoteProjectContext {
    private final Path root;
    private Runnable onChange = () -> {};
    private final ThreadLocal<Integer> loading = ThreadLocal.withInitial(() -> 0);

    public NoteProjectContext(Path root) { this.root = WorkspaceDiscovery.identity(root); }
    public Path root() { return root; }
    public Path storage() { return root.resolve(".idea/CodeReadingNote.xml"); }
    public String id() { return root.toString(); }
    public String displayName(Path workspace) {
        Path base = workspace.toAbsolutePath().normalize();
        return root.equals(base) ? (root.getFileName() == null ? root.toString() : root.getFileName().toString())
                : base.relativize(root).toString().replace('\\', '/');
    }
    public void onChange(Runnable callback) { onChange = callback; }
    public void changed() { if (loading.get() == 0) onChange.run(); }
    public boolean isLoading() { return loading.get() != 0; }
    public void loading(Runnable action) {
        loading.set(loading.get() + 1);
        try { action.run(); } finally {
            int remaining = loading.get() - 1;
            if (remaining == 0) loading.remove(); else loading.set(remaining);
        }
    }
    @Override public boolean equals(Object other) {
        return other instanceof NoteProjectContext context && root.equals(context.root);
    }
    @Override public int hashCode() { return Objects.hash(root); }
    @Override public String toString() { return root.toString(); }
}

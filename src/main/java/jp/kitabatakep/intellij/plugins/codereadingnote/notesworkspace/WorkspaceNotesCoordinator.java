package jp.kitabatakep.intellij.plugins.codereadingnote.notesworkspace;

import com.intellij.openapi.Disposable;
import com.intellij.notification.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import jp.kitabatakep.intellij.plugins.codereadingnote.*;
import org.jdom.Element;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/** Application-wide ownership: a subproject opened separately uses the very same TopicList. */
@Service(Service.Level.APP)
public final class WorkspaceNotesCoordinator implements Disposable {
    private static final Logger LOG = Logger.getInstance(WorkspaceNotesCoordinator.class);
    private final Map<Path, Entry> entries = new ConcurrentHashMap<>();
    private final ScheduledExecutorService io = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "workspace-notes-io"); thread.setDaemon(true); return thread;
    });

    static final class Entry {
        final NoteProjectContext context;
        final TopicList list;
        final WorkspaceXmlStore store;
        final Set<Project> viewers = ConcurrentHashMap.newKeySet();
        volatile Project standalone;
        volatile Project modelProject;
        volatile boolean loaded, failed, conflict, reloading;
        boolean recoveryScheduled;
        final java.util.concurrent.atomic.AtomicLong revision = new java.util.concurrent.atomic.AtomicLong();
        volatile boolean dirty;
        volatile Element pending;
        volatile ScheduledFuture<?> save;
        final Object scheduling = new Object();
        final Set<String> rootSerialized = ConcurrentHashMap.newKeySet();
        volatile boolean rootHadDisk;
        Entry(Project project, Path root) {
            context = new NoteProjectContext(root);
            list = new TopicList(project, context);
            Path recovery = Path.of(com.intellij.openapi.application.PathManager.getConfigPath(),
                    "CodeReadingNote", "workspace-recovery", UUID.nameUUIDFromBytes(context.id().getBytes(java.nio.charset.StandardCharsets.UTF_8)) + ".xml");
            store = new WorkspaceXmlStore(context.storage(), recovery);
            modelProject = project;
        }
    }

    public static WorkspaceNotesCoordinator getInstance() {
        return ApplicationManager.getApplication().getService(WorkspaceNotesCoordinator.class);
    }
    public record SyncSnapshot(Element topics, long revision, byte[] disk) {}
    public void rootSerialized(TopicList list, Element xml) {
        Entry entry = entries.get(list.context().root());
        if (entry != null) entry.rootSerialized.add(jp.kitabatakep.intellij.plugins.codereadingnote.sync.workspace.NotesSyncXml.digest(xml));
    }
    public long syncRevision(TopicList list) {
        Entry entry = entries.get(list.context().root());
        return entry == null ? -1 : entry.revision.get();
    }
    /** Called only from background sync workers. Live model access is confined to EDT. */
    public SyncSnapshot syncSnapshot(TopicList list) throws Exception {
        Entry entry = entries.get(list.context().root());
        if (entry == null) throw new IOException("Unavailable project");
        Element[] xml = new Element[1]; long[] revision = new long[1];
        onEdt(() -> {
            if (entry.failed || entry.conflict || entry.reloading || entry.modelProject.isDisposed()
                    || !entry.loaded && entry.standalone == null) throw new IOException("Unavailable notes");
            xml[0] = TopicListExporter.export(list); revision[0] = entry.revision.get();
        });
        byte[] bytes = disk(entry);
        if (entry.standalone != null) {
            Element persisted = new WorkspaceXmlStore(entry.context.storage()).load();
            String digest = jp.kitabatakep.intellij.plugins.codereadingnote.sync.workspace.NotesSyncXml.digest(persisted);
            if (bytes == null && entry.rootHadDisk || bytes != null && !entry.rootSerialized.contains(digest)) {
                preserve(entry, xml[0]); conflict(entry); throw new IOException("External root notes change");
            }
        }
        return new SyncSnapshot(xml[0], revision[0], bytes);
    }
    private byte[] disk(Entry entry) throws IOException {
        try { return Files.readAllBytes(entry.context.storage()); }
        catch (NoSuchFileException missing) { return null; }
    }
    public boolean syncCurrent(TopicList list, SyncSnapshot snapshot) throws IOException {
        Entry entry = entries.get(list.context().root());
        return entry != null && !entry.failed && !entry.conflict && !entry.reloading
                && !entry.modelProject.isDisposed() && Files.isDirectory(entry.context.root().resolve(".idea"))
                && entry.revision.get() == snapshot.revision() && Arrays.equals(snapshot.disk(), disk(entry));
    }
    public SyncSnapshot flushForSync(TopicList list) throws Exception {
        SyncSnapshot snapshot = syncSnapshot(list);
        Entry entry = entries.get(list.context().root());
        Project standalone = entry.standalone;
        if (standalone != null) {
            // A durable recovery snapshot precedes platform persistence; verify disk after the blocking save.
            entry.store.preservePending(snapshot.topics());
            if (!syncCurrent(list, snapshot)) throw new IOException("Notes changed during save");
            com.intellij.configurationStore.StoreUtil.saveSettings(standalone, true);
        } else {
            io.submit(() -> {
                synchronized (entry) {
                    if (!syncCurrent(list, snapshot)) throw new IOException("Notes changed during save");
                    entry.store.save(snapshot.topics(), false);
                }
                return null;
            }).get();
        }
        return verifySyncSave(entry, snapshot.topics(), snapshot.revision());
    }
    private SyncSnapshot verifySyncSave(Entry entry, Element xml, long revision) throws Exception {
        synchronized (entry) {
            WorkspaceXmlStore check = new WorkspaceXmlStore(entry.context.storage());
            Element saved = check.load();
            if (!jp.kitabatakep.intellij.plugins.codereadingnote.sync.workspace.NotesSyncXml.digest(saved).equals(
                    jp.kitabatakep.intellij.plugins.codereadingnote.sync.workspace.NotesSyncXml.digest(xml))) throw new IOException("Notes not persisted");
            entry.store.load();
            if (entry.revision.get() == revision) entry.dirty = false;
            if (entry.standalone != null) entry.rootHadDisk = true;
            entry.store.discardPending();
            return new SyncSnapshot(xml, revision, disk(entry));
        }
    }
    public SyncSnapshot applySync(TopicList list, Element remote, SyncSnapshot expected) throws Exception {
        Entry entry = entries.get(list.context().root());
        if (entry == null || !syncCurrent(list, expected)) throw new IOException("Notes changed during pull");
        // Parse everything before touching the current model. Preserve legacy missing trash.
        Element applied = remote.clone();
        for (org.jdom.Attribute attribute : expected.topics().getAttributes())
            if (applied.getAttribute(attribute.getName(), attribute.getNamespace()) == null) applied.setAttribute(attribute.clone());
        for (Element extension : expected.topics().getChildren())
            if (!Set.of("topic", "trash").contains(extension.getName()) && applied.getChild(extension.getName(), extension.getNamespace()) == null)
                applied.addContent(extension.clone());
        if (applied.getChild("trash") == null && expected.topics().getChild("trash") != null)
            applied.addContent(expected.topics().getChild("trash").clone());
        onEdt(() -> {
            if (!syncCurrent(list, expected)) throw new IOException("Notes changed during pull");
            ArrayList<Topic> topics = TopicListImporter.importElement(entry.modelProject, entry.context, applied);
            ArrayList<TrashedLine> trash = TopicListImporter.importTrashedLines(entry.modelProject, entry.context, applied);
            entry.context.loading(() -> {
                list.setTopics(topics); list.setTrashedLines(trash); list.setXmlTemplate(applied);
            });
            entry.revision.incrementAndGet(); entry.dirty = true; entry.pending = applied.clone();
            publish(entry);
        });
        return flushForSync(list);
    }
    private interface EdtWork { void run() throws Exception; }
    private static void onEdt(EdtWork work) throws Exception {
        Exception[] failure = new Exception[1];
        Runnable task = () -> { try { work.run(); } catch (Exception e) { failure[0] = e; } };
        if (ApplicationManager.getApplication().isDispatchThread()) task.run();
        else ApplicationManager.getApplication().invokeAndWait(task, com.intellij.openapi.application.ModalityState.any());
        if (failure[0] != null) throw failure[0];
    }
    private Entry entry(Project project, Path root) {
        return entries.computeIfAbsent(root.toAbsolutePath().normalize(), key -> {
            Entry entry = new Entry(project, key);
            entry.context.onChange(() -> modified(entry));
            return entry;
        });
    }
    public TopicList attachRoot(Project project) {
        Entry entry = entry(project, Path.of(project.getBasePath()));
        synchronized (entry) {
            if (entry.save != null) entry.save.cancel(false);
            entry.standalone = project;
            entry.modelProject = project;
            entry.list.rebind(project);
            entry.viewers.add(project);
            if (entry.loaded) {
                rootSerialized(entry.list, entry.list.xmlTemplate());
                rootSerialized(entry.list, TopicListExporter.export(entry.list));
                entry.rootHadDisk = Files.exists(entry.context.storage());
            }
        }
        Disposer.register(project, () -> release(project));
        return entry.list;
    }
    public boolean wasLoaded(TopicList list) { return entries.get(list.context().root()).loaded; }
    public boolean persistencePaused(TopicList list) {
        Entry entry = entries.get(list.context().root());
        return entry != null && (entry.conflict || entry.failed);
    }
    public void rootFailed(TopicList list) { entries.get(list.context().root()).failed = true; }
    public boolean canEdit(TopicList list) {
        Entry entry = entries.get(list.context().root());
        if (entry != null && !entry.failed && (entry.loaded || entry.standalone != null)) return true;
        if (entry != null) report(entry, "workspace.project.not.ready", new IllegalStateException("Notes not loaded"));
        return false;
    }
    public void rootLoaded(TopicList list) {
        Entry entry = entries.get(list.context().root());
        entry.loaded = true;
        entry.failed = false;
        entry.dirty = false;
        entry.rootHadDisk = true;
        rootSerialized(list, list.xmlTemplate());
        publish(entry);
        recoverRoot(list);
    }
    public void recoverRoot(TopicList list) {
        Entry entry = entries.get(list.context().root());
        synchronized (entry.scheduling) {
            if (entry.recoveryScheduled) return;
            entry.recoveryScheduled = true;
        }
        io.execute(() -> {
            try {
                if (!Files.exists(entry.store.pendingPath())) return;
                entry.store.load();
                Element pending = entry.store.readPending();
                ApplicationManager.getApplication().invokeLater(() -> restorePending(entry, pending));
            } catch (IOException error) { report(entry, "workspace.load.failed", error); }
        });
    }
    public TopicList listFor(Topic topic) {
        Entry entry = entries.get(topic.context().root());
        if (entry == null) throw new IllegalStateException(CodeReadingNoteBundle.message("workspace.project.unavailable", topic.context().root()));
        return entry.list;
    }
    TopicList acquire(Project project, Path root) {
        Entry entry = entry(project, root);
        entry.viewers.add(project);
        return entry.list;
    }
    /** Runs on the serial IO queue. Applying models and reading live model snapshots always uses EDT. */
    void refresh(Project project, Path root) {
        Entry entry = entry(project, root);
        synchronized (entry) {
            if (entry.standalone != null || project.isDisposed()) return;
            try {
                if (entry.dirty) {
                    if (entry.store.externallyChanged()) conflict(entry);
                    else save(entry, false);
                    return;
                }
                if (entry.loaded && !entry.failed && !entry.store.externallyChanged()) return;
                entry.reloading = true;
                long revision = entry.revision.get();
                Element topics = entry.store.load();
                applyLater(entry, topics, false, revision);
            } catch (Exception error) {
                entry.reloading = false;
                entry.failed = true;
                report(entry, "workspace.load.failed", error);
            }
        }
    }
    private void applyLater(Entry entry, Element topics, boolean reload, long revision) {
        ApplicationManager.getApplication().invokeLater(() -> {
            entry.reloading = false;
            if (entry.modelProject.isDisposed() || entry.standalone != null && !reload) return;
            if (entry.revision.get() != revision || entry.dirty && !reload) {
                entry.conflict = false;
                conflict(entry);
                return;
            }
            try {
                ArrayList<Topic> imported = TopicListImporter.importElement(entry.modelProject, entry.context, topics);
                ArrayList<TrashedLine> trash = TopicListImporter.importTrashedLines(entry.modelProject, entry.context, topics);
                entry.list.setTopics(imported);
                entry.list.setTrashedLines(trash);
                entry.list.setXmlTemplate(topics);
                if (entry.standalone != null) { entry.rootSerialized.clear(); rootSerialized(entry.list, topics); }
                entry.loaded = true;
                entry.failed = false;
                entry.dirty = false;
                entry.pending = null;
                entry.conflict = false;
                publish(entry);
                if (reload) io.execute(() -> {
                    try { entry.store.discardPending(); }
                    catch (IOException error) { report(entry, "workspace.save.failed", error); }
                });
                if (!reload) {
                    io.execute(() -> {
                        try {
                            if (!Files.exists(entry.store.pendingPath())) return;
                            Element pending = entry.store.readPending();
                            ApplicationManager.getApplication().invokeLater(() -> restorePending(entry, pending));
                        } catch (Exception error) { report(entry, "workspace.load.failed", error); }
                    });
                }
            } catch (Exception error) {
                entry.failed = true;
                report(entry, "workspace.load.failed", error);
            }
        });
    }
    private void restorePending(Entry entry, Element pending) {
        if (entry.modelProject.isDisposed() || entry.dirty) return;
        try {
            ArrayList<Topic> topics = TopicListImporter.importElement(entry.modelProject, entry.context, pending);
            ArrayList<TrashedLine> trash = TopicListImporter.importTrashedLines(entry.modelProject, entry.context, pending);
            entry.list.setTopics(topics);
            entry.list.setTrashedLines(trash);
            entry.list.setXmlTemplate(pending);
            entry.loaded = true;
            entry.pending = pending;
            entry.dirty = true;
            conflict(entry);
            publish(entry);
        } catch (Exception error) { report(entry, "workspace.load.failed", error); }
    }
    private void modified(Entry entry) {
        entry.revision.incrementAndGet();
        entry.dirty = true;
        if (entry.standalone != null && !entry.standalone.isDisposed()) {
            CodeReadingNoteService.getInstance(entry.standalone).workspaceDataModified();
        }
        ApplicationManager.getApplication().invokeLater(() -> {
            if (entry.modelProject.isDisposed()) return;
            try {
                entry.pending = TopicListExporter.export(entry.list);
                synchronized (entry.scheduling) {
                    if (entry.save != null) entry.save.cancel(false);
                    if (entry.standalone == null) entry.save = io.schedule(() -> save(entry, false), 500, TimeUnit.MILLISECONDS);
                }
                publish(entry);
            } catch (Exception error) { report(entry, "workspace.save.failed", error); }
        });
    }
    private void save(Entry entry, boolean force) {
        synchronized (entry) {
            if (entry.standalone != null && !force || !entry.dirty || entry.pending == null) return;
            if (entry.reloading) {
                entry.save = io.schedule(() -> save(entry, force), 500, TimeUnit.MILLISECONDS);
                return;
            }
            Element snapshot = entry.pending;
            long revision = entry.revision.get();
            try {
                if (entry.failed || entry.conflict && !force) {
                    entry.store.preservePending(snapshot);
                    return;
                }
                entry.store.save(snapshot, force);
                if (entry.pending == snapshot && entry.revision.get() == revision) entry.dirty = false;
                entry.conflict = false;
            } catch (WorkspaceXmlStore.ConflictException error) {
                preserve(entry, snapshot);
                conflict(entry);
            } catch (Exception error) {
                preserve(entry, snapshot);
                report(entry, "workspace.save.failed", error);
                if (force) { entry.conflict = false; conflict(entry); }
            }
        }
    }
    private void preserve(Entry entry, Element snapshot) {
        try { entry.store.preservePending(snapshot); }
        catch (IOException error) { report(entry, "workspace.save.failed", error); }
    }
    private void conflict(Entry entry) {
        if (entry.conflict) return;
        entry.conflict = true;
        Notification notification = new Notification("CodeReadingNote.Workspace", CodeReadingNoteBundle.message("workspace.title"),
                CodeReadingNoteBundle.message("workspace.conflict", entry.context.root()), NotificationType.WARNING);
        notification.addAction(NotificationAction.createSimpleExpiring(CodeReadingNoteBundle.message("workspace.reload.disk"), () ->
                io.execute(() -> {
                    synchronized (entry) {
                        try {
                            long revision = entry.revision.get();
                            entry.reloading = true;
                            Element topics = entry.store.load();
                            applyLater(entry, topics, true, revision);
                        } catch (Exception error) {
                            entry.reloading = false;
                            report(entry, "workspace.load.failed", error);
                            entry.conflict = false;
                            conflict(entry);
                        }
                    }
                })));
        notification.addAction(NotificationAction.createSimpleExpiring(CodeReadingNoteBundle.message("workspace.backup.save"),
                () -> io.execute(() -> save(entry, true))));
        notification.notify(entry.modelProject);
    }
    private void publish(Entry entry) {
        ApplicationManager.getApplication().invokeLater(() -> {
            for (Project viewer : entry.viewers) if (!viewer.isDisposed()) {
                viewer.getMessageBus().syncPublisher(WorkspaceNotesNotifier.TOPIC).changed(entry.context);
            }
        });
    }
    public static void report(Project project, Path path, String key, Throwable error) {
        LOG.warn(key + ": " + path, error);
        new Notification("CodeReadingNote.Workspace", CodeReadingNoteBundle.message("workspace.title"),
                CodeReadingNoteBundle.message(key, path), NotificationType.ERROR).notify(project);
    }
    private void report(Entry entry, String key, Throwable error) { report(entry.modelProject, entry.context.root(), key, error); }


    void release(Project project) {
        List<Entry> released = new ArrayList<>();
        for (Entry entry : entries.values()) {
            if (!entry.viewers.remove(project)) continue;
            // Called during disposal on EDT: capture the last edit even if its debounce has not fired.
            if (entry.dirty) entry.pending = TopicListExporter.export(entry.list);
            if (entry.save != null) entry.save.cancel(false);
            synchronized (entry) {
                if (entry.standalone == project) {
                    entry.standalone = null;
                    // The platform just saved its root PersistentStateComponent. Refresh the disk baseline.
                    try { entry.store.load(); } catch (IOException error) { entry.failed = true; report(entry, "workspace.load.failed", error); }
                    if (!entry.conflict && !entry.failed) entry.dirty = false;
                }
                Project remaining = entry.viewers.stream().filter(p -> !p.isDisposed()).findFirst().orElse(null);
                if (remaining != null) {
                    entry.modelProject = remaining;
                    entry.list.rebind(remaining);
                }
            }
            released.add(entry);
        }
        try {
            io.submit(() -> {
                for (Entry entry : released) {
                    entry.reloading = false;
                    save(entry, false);
                    if (entry.viewers.isEmpty()) entries.remove(entry.context.root(), entry);
                }
            }).get();
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            report(project, Path.of(project.getBasePath()), "workspace.save.failed", error);
        } catch (ExecutionException error) { report(project, Path.of(project.getBasePath()), "workspace.save.failed", error); }
    }
    @Override public void dispose() {
        for (Entry entry : entries.values()) save(entry, false);
        io.shutdown();
        try {
            if (!io.awaitTermination(30, TimeUnit.SECONDS)) io.shutdownNow();
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            LOG.warn("Interrupted while closing workspace notes storage", error);
        }
    }
}

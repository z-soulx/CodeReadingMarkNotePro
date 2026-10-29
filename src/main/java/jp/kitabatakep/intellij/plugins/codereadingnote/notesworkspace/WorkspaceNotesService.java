package jp.kitabatakep.intellij.plugins.codereadingnote.notesworkspace;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.vfs.*;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import jp.kitabatakep.intellij.plugins.codereadingnote.*;
import jp.kitabatakep.intellij.plugins.codereadingnote.remark.EditorUtils;
import org.jetbrains.annotations.NotNull;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ScheduledFuture;

@Service(Service.Level.PROJECT)
public final class WorkspaceNotesService implements Disposable {
    private final Project project;
    private final Path workspacePath;
    private final Path root;
    private final WorkspaceNotesCoordinator coordinator;
    private volatile List<TopicList> projects;
    private volatile boolean disposed;
    private volatile boolean ready;
    private ScheduledFuture<?> refresh;
    private boolean reloadRequested;
    private final java.util.Set<LocalFileSystem.WatchRequest> watches = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private final java.util.Set<Path> watchedPaths = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private volatile Map<Path, Path> displayPaths = Map.of();
    private volatile List<Path> eventRoots = List.of();
    private final java.util.concurrent.ScheduledExecutorService scanner = java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "workspace-notes-discovery"); thread.setDaemon(true); return thread;
    });

    public WorkspaceNotesService(Project project) {
        this.project = project;
        workspacePath = Path.of(project.getBasePath()).toAbsolutePath().normalize();
        root = WorkspaceDiscovery.identity(workspacePath);
        coordinator = WorkspaceNotesCoordinator.getInstance();
        watch(workspacePath);
        if (!root.equals(workspacePath)) watch(root);
        displayPaths = Map.of(root, workspacePath);
        eventRoots = root.equals(workspacePath) ? List.of(root) : List.of(workspacePath, root);
        projects = List.of(CodeReadingNoteService.getInstance(project).getTopicList());
        coordinator.recoverRoot(projects.get(0));
        ApplicationManager.getApplication().getMessageBus().connect(this).subscribe(VirtualFileManager.VFS_CHANGES, new BulkFileListener() {
            @Override public void after(@NotNull List<? extends VFileEvent> events) {
                if (events.stream().anyMatch(WorkspaceNotesService.this::isWorkspaceEvent)) refresh();
            }
        });
        project.getMessageBus().connect(this).subscribe(WorkspaceNotesNotifier.TOPIC, context -> {
            if (disposed) return;
            for (TopicList list : projects) list.refreshAllTopicLines();
            for (TopicList list : projects) if (list.context().equals(context)) {
                try { jp.kitabatakep.intellij.plugins.codereadingnote.remark.BookmarkUtils.pruneWorkspaceBookmarks(project, list); }
                catch (Exception error) { WorkspaceNotesCoordinator.report(project, context.root(), "workspace.bookmark.failed", error); }
            }
            // Rebuild marks in already-open editors as well as editors opened after discovery.
            for (var file : com.intellij.openapi.fileEditor.FileEditorManager.getInstance(project).getOpenFiles()) {
                var editor = EditorUtils.getEditor(com.intellij.openapi.fileEditor.FileEditorManager.getInstance(project), file);
                if (editor != null) {
                    EditorUtils.clearNoteMarks(editor);
                    for (TopicLine line : linesFor(file)) EditorUtils.addLineCodeRemark(project, line);
                }
            }
        });
        refresh();
    }
    public static WorkspaceNotesService getInstance(Project project) { return project.getService(WorkspaceNotesService.class); }
    public List<TopicList> projects() { return projects; }
    public boolean isWorkspace() { return projects.size() > 1; }
    public boolean isReady() { return ready; }
    public String displayName(NoteProjectContext context) {
        Path logical = displayPaths.getOrDefault(context.root(), context.root());
        if (context.root().equals(root)) return workspacePath.getFileName() == null ? workspacePath.toString() : workspacePath.getFileName().toString();
        try { return workspacePath.relativize(logical).toString().replace('\\', '/'); }
        catch (IllegalArgumentException ignored) { return logical.toString(); }
    }
    public List<Topic> allTopics() { return projects.stream().flatMap(list -> list.getTopics().stream()).toList(); }
    public List<TopicLine> linesFor(VirtualFile file) {
        return allTopics().stream().flatMap(t -> t.getLines().stream()).filter(line -> file.equals(line.file())).toList();
    }
    public TopicList forFile(VirtualFile file) {
        if (file == null || !file.isInLocalFileSystem()) return projects.get(0);
        Path owner = WorkspaceDiscovery.owner(Path.of(file.getPath()), projects.stream().map(p -> p.context().root()).toList(), root);
        return projects.stream().filter(p -> p.context().root().equals(owner)).findFirst().orElse(projects.get(0));
    }
    public TopicList forContext(NoteProjectContext context) {
        return projects.stream().filter(p -> p.context().equals(context)).findFirst().orElse(null);
    }
    public synchronized void refresh() {
        if (disposed) return;
        if (refresh != null) refresh.cancel(false);
        refresh = scanner.schedule(this::discover, 400, java.util.concurrent.TimeUnit.MILLISECONDS);
    }
    /** Refresh file references recursively, then rediscover projects and safely reload their notes. */
    public void refreshAndReload() {
        if (disposed || project.isDisposed()) return;
        synchronized (this) { reloadRequested = true; }
        Set<Path> roots = new LinkedHashSet<>(eventRoots);
        roots.add(workspacePath);
        List<VirtualFile> files = roots.stream()
                .map(path -> LocalFileSystem.getInstance().findFileByNioFile(path))
                .filter(Objects::nonNull).toList();
        com.intellij.openapi.vfs.newvfs.RefreshQueue.getInstance().refresh(true, true, this::refresh, files);
    }
    private void discover() {
        if (disposed) return;
        boolean reload;
        synchronized (this) { reload = reloadRequested; reloadRequested = false; }
        try {
            List<Path> logicalPaths = WorkspaceDiscovery.discover(workspacePath, (path, error) ->
                    WorkspaceNotesCoordinator.report(project, path, "workspace.scan.failed", error));
            Map<Path, Path> resolved = new LinkedHashMap<>();
            for (Path path : logicalPaths) resolved.putIfAbsent(WorkspaceDiscovery.identity(path), path.toAbsolutePath().normalize());
            // Never hide unsaved edits or an unresolved conflict when the disk file disappears or becomes empty.
            for (TopicList list : projects) if (coordinator.retainForRefresh(list)) {
                Path path = list.context().root();
                resolved.putIfAbsent(path, displayPaths.getOrDefault(path, path));
            }
            List<Path> paths = List.copyOf(resolved.keySet());
            List<Path> observed = java.util.stream.Stream.concat(resolved.keySet().stream(), resolved.values().stream()).distinct().toList();
            for (Map.Entry<Path, Path> entry : resolved.entrySet()) if (!entry.getKey().equals(entry.getValue())) watch(entry.getKey());
            // Instantiate existing independent project services before acquiring their data (never create a Project).
            for (Project open : ProjectManager.getInstance().getOpenProjects()) {
                if (!open.isDisposed() && open.getBasePath() != null && paths.contains(WorkspaceDiscovery.identity(Path.of(open.getBasePath())))) {
                    CodeReadingNoteService.getInstance(open);
                }
            }
            List<TopicList> found = new ArrayList<>();
            found.add(CodeReadingNoteService.getInstance(project).getTopicList());
            for (Path path : paths) if (!path.equals(root)) {
                if (disposed || Thread.currentThread().isInterrupted()) return;
                found.add(coordinator.acquire(project, path));
                LocalFileSystem.getInstance().refreshAndFindFileByNioFile(path.resolve(".idea"));
                LocalFileSystem.getInstance().refreshAndFindFileByNioFile(path.resolve(".idea/CodeReadingNote.xml"));
            }
            coordinator.refreshAll(project, paths, reload, () -> {
                if (disposed || project.isDisposed()) return;
                displayPaths = Map.copyOf(resolved);
                eventRoots = observed;
                projects = found.stream().filter(list -> list.context().root().equals(root)
                        || !list.getTopics().isEmpty() || coordinator.retainForRefresh(list)).toList();
                ready = true;
                project.getMessageBus().syncPublisher(WorkspaceNotesNotifier.TOPIC).changed(projects.get(0).context());
            });
        } catch (Exception error) { WorkspaceNotesCoordinator.report(project, root, "workspace.scan.failed", error); }
    }
    private void watch(Path path) {
        Path normalized = path.toAbsolutePath().normalize();
        if (watchedPaths.add(normalized)) watches.addAll(LocalFileSystem.getInstance().addRootsToWatch(List.of(normalized.toString()), true));
    }
    private boolean isWorkspaceEvent(VFileEvent event) {
        String path = event.getPath();
        if (path.endsWith(".tmp") || path.endsWith(".bak") || path.endsWith("workspace-pending.xml")) return false;
        Path changed;
        try { changed = Path.of(path).toAbsolutePath().normalize(); }
        catch (InvalidPathException ignored) { return false; }
        return eventRoots.stream().anyMatch(changed::startsWith);
    }
    @Override public synchronized void dispose() {
        disposed = true;
        if (refresh != null) refresh.cancel(false);
        scanner.shutdownNow();
        LocalFileSystem.getInstance().removeWatchedRoots(watches);
        coordinator.release(project);
    }
}

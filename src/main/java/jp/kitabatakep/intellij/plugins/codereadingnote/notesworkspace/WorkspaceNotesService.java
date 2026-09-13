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

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ScheduledFuture;

@Service(Service.Level.PROJECT)
public final class WorkspaceNotesService implements Disposable {
    private final Project project;
    private final Path root;
    private final WorkspaceNotesCoordinator coordinator;
    private volatile List<TopicList> projects;
    private volatile boolean disposed;
    private volatile boolean ready;
    private ScheduledFuture<?> refresh;
    private final java.util.Set<LocalFileSystem.WatchRequest> watches;
    private final java.util.concurrent.ScheduledExecutorService scanner = java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "workspace-notes-discovery"); thread.setDaemon(true); return thread;
    });

    public WorkspaceNotesService(Project project) {
        this.project = project;
        root = Path.of(project.getBasePath()).toAbsolutePath().normalize();
        coordinator = WorkspaceNotesCoordinator.getInstance();
        watches = LocalFileSystem.getInstance().addRootsToWatch(java.util.List.of(root.toString()), true);
        projects = List.of(CodeReadingNoteService.getInstance(project).getTopicList());
        coordinator.recoverRoot(projects.get(0));
        ApplicationManager.getApplication().getMessageBus().connect(this).subscribe(VirtualFileManager.VFS_CHANGES, new BulkFileListener() {
            @Override public void after(@NotNull List<? extends VFileEvent> events) {
                if (events.stream().anyMatch(e -> e.getPath().replace('\\', '/').startsWith(root.toString().replace('\\', '/') + "/")
                        && !e.getPath().endsWith(".tmp") && !e.getPath().endsWith(".bak")
                        && !e.getPath().endsWith("workspace-pending.xml"))) refresh();
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
    public String displayName(NoteProjectContext context) { return context.displayName(root); }
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
    private void discover() {
        if (disposed) return;
        try {
            List<Path> paths = WorkspaceDiscovery.discover(root, (path, error) ->
                    WorkspaceNotesCoordinator.report(project, path, "workspace.scan.failed", error));
            // Instantiate existing independent project services before acquiring their data (never create a Project).
            for (Project open : ProjectManager.getInstance().getOpenProjects()) {
                if (!open.isDisposed() && open.getBasePath() != null && paths.contains(Path.of(open.getBasePath()).toAbsolutePath().normalize())) {
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
                coordinator.refresh(project, path);
            }
            ApplicationManager.getApplication().invokeLater(() -> {
                if (disposed || project.isDisposed()) return;
                projects = List.copyOf(found);
                ready = true;
                project.getMessageBus().syncPublisher(WorkspaceNotesNotifier.TOPIC).changed(projects.get(0).context());
            });
        } catch (Exception error) { WorkspaceNotesCoordinator.report(project, root, "workspace.scan.failed", error); }
    }
    @Override public synchronized void dispose() {
        disposed = true;
        if (refresh != null) refresh.cancel(false);
        scanner.shutdownNow();
        LocalFileSystem.getInstance().removeWatchedRoots(watches);
        coordinator.release(project);
    }
}

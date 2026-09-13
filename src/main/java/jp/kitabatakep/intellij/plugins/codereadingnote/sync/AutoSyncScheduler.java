package jp.kitabatakep.intellij.plugins.codereadingnote.sync;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import jp.kitabatakep.intellij.plugins.codereadingnote.sync.workspace.WorkspaceNotesSyncCoordinator;

/** Compatibility entry point; the application coordinator owns debounce and lifecycle. */
@Service(Service.Level.PROJECT)
public final class AutoSyncScheduler {
    private final Project project;
    public AutoSyncScheduler(Project project) { this.project = project; }
    public static AutoSyncScheduler getInstance(Project project) { return project.getService(AutoSyncScheduler.class); }
    public void scheduleAutoSync() { WorkspaceNotesSyncCoordinator.getInstance().attach(project); }
    public void shutdown() { /* No project-local executor. */ }
}
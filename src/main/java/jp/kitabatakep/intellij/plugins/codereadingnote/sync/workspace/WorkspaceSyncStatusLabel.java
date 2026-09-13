package jp.kitabatakep.intellij.plugins.codereadingnote.sync.workspace;

import com.intellij.openapi.project.Project;
import jp.kitabatakep.intellij.plugins.codereadingnote.notesworkspace.*;
import javax.swing.*;
import java.awt.event.*;

public final class WorkspaceSyncStatusLabel extends JLabel {
    public WorkspaceSyncStatusLabel(Project project) {
        Runnable refresh = () -> {
            var lists = WorkspaceNotesService.getInstance(project).projects();
            var sync = WorkspaceNotesSyncCoordinator.getInstance();
            long complete = lists.stream().filter(l -> "synced".equals(sync.view(l).status())).count();
            setText(NotesSyncUi.m("summary", complete, lists.size())); setToolTipText(NotesSyncUi.m("title"));
        };
        project.getMessageBus().connect(project).subscribe(NotesSyncNotifier.TOPIC, context -> refresh.run());
        project.getMessageBus().connect(project).subscribe(WorkspaceNotesNotifier.TOPIC, context -> refresh.run());
        com.intellij.openapi.application.ApplicationManager.getApplication().getMessageBus().connect(project).subscribe(
                jp.kitabatakep.intellij.plugins.codereadingnote.settings.LanguageSettings.LANGUAGE_CHANGED, refresh::run);
        addMouseListener(new MouseAdapter() { @Override public void mouseClicked(MouseEvent event) { NotesSyncUi.open(project); } });
        refresh.run();
    }
}

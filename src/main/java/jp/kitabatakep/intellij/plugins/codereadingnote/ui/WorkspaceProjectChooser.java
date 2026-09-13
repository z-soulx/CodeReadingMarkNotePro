package jp.kitabatakep.intellij.plugins.codereadingnote.ui;

import com.intellij.openapi.project.Project;
import jp.kitabatakep.intellij.plugins.codereadingnote.CodeReadingNoteBundle;
import jp.kitabatakep.intellij.plugins.codereadingnote.TopicList;
import jp.kitabatakep.intellij.plugins.codereadingnote.notesworkspace.WorkspaceNotesService;
import javax.swing.JOptionPane;
import java.util.function.Supplier;

public final class WorkspaceProjectChooser {
    private WorkspaceProjectChooser() {}
    public static TopicList choose(Project project, Supplier<TopicList> selection) {
        if (project == null) return null;
        WorkspaceNotesService workspace = WorkspaceNotesService.getInstance(project);
        if (!workspace.isReady()) {
            com.intellij.openapi.ui.Messages.showInfoMessage(project, CodeReadingNoteBundle.message("workspace.discovering"), CodeReadingNoteBundle.message("workspace.title"));
            return null;
        }
        TopicList selected = selection == null ? null : selection.get();
        if (selected != null && workspace.projects().contains(selected)) return selected;
        if (!workspace.isWorkspace()) return workspace.projects().get(0);
        Item[] items = workspace.projects().stream().map(list -> new Item(list, workspace.displayName(list.context()))).toArray(Item[]::new);
        Object chosen = JOptionPane.showInputDialog(null, CodeReadingNoteBundle.message("workspace.choose.project"),
                CodeReadingNoteBundle.message("workspace.title"), JOptionPane.QUESTION_MESSAGE, null, items, items[0]);
        return chosen instanceof Item item ? item.list() : null;
    }
    private record Item(TopicList list, String label) { @Override public String toString() { return label; } }
}

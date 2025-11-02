package jp.kitabatakep.intellij.plugins.codereadingnote.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import jp.kitabatakep.intellij.plugins.codereadingnote.CodeReadingNoteBundle;
import jp.kitabatakep.intellij.plugins.codereadingnote.TopicGroup;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

/**
 * Action to rename a group
 */
public class GroupRenameAction extends AnAction {
    
    private Supplier<TopicGroup> groupSupplier;
    
    public GroupRenameAction(Supplier<TopicGroup> groupSupplier) {
        super(
            CodeReadingNoteBundle.message("action.rename.group"),
            CodeReadingNoteBundle.message("action.rename.group.description"),
            null
        );
        this.groupSupplier = groupSupplier;
    }
    
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;
        
        TopicGroup group = groupSupplier.get();
        if (group == null) return;
        
        String currentName = group.name();
        String newName = Messages.showInputDialog(
            project,
            CodeReadingNoteBundle.message("dialog.rename.group.message"),
            CodeReadingNoteBundle.message("dialog.rename.group.title"),
            Messages.getQuestionIcon(),
            currentName,
            null
        );
        
        if (newName != null && !newName.trim().isEmpty() && !newName.trim().equals(currentName)) {
            // Check if new name already exists in the same topic
            if (group.getParentTopic().findGroupByName(newName.trim()) != null) {
                Messages.showErrorDialog(
                    project,
                    CodeReadingNoteBundle.message("message.group.duplicate.name.in.topic"),
                    CodeReadingNoteBundle.message("message.duplicate.name.title")
                );
                return;
            }
            
            // Rename the group
            group.setName(newName.trim());
        }
    }
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        TopicGroup group = groupSupplier.get();
        e.getPresentation().setEnabled(group != null);
    }
}

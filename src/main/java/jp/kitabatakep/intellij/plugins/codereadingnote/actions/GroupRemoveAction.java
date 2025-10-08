package jp.kitabatakep.intellij.plugins.codereadingnote.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import jp.kitabatakep.intellij.plugins.codereadingnote.TopicGroup;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

/**
 * Action to remove a group
 */
public class GroupRemoveAction extends AnAction {
    
    private Supplier<TopicGroup> groupSupplier;
    
    public GroupRemoveAction(Supplier<TopicGroup> groupSupplier) {
        super("Remove Group", "Remove the selected group (lines will be moved to ungrouped)", null);
        this.groupSupplier = groupSupplier;
    }
    
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;
        
        TopicGroup group = groupSupplier.get();
        if (group == null) return;
        
        String message = "Are you sure you want to remove the group '" + group.name() + "'?";
        if (group.getLineCount() > 0) {
            message += "\n\nAll " + group.getLineCount() + " lines in this group will be moved to ungrouped lines.";
        }
        
        int result = Messages.showYesNoDialog(
            project,
            message,
            "Remove Group",
            Messages.getQuestionIcon()
        );
        
        if (result == Messages.YES) {
            // Remove the group (lines will be automatically moved to ungrouped)
            group.getParentTopic().removeGroup(group);
        }
    }
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        TopicGroup group = groupSupplier.get();
        e.getPresentation().setEnabled(group != null);
    }
}
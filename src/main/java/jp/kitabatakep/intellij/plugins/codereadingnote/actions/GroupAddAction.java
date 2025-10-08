package jp.kitabatakep.intellij.plugins.codereadingnote.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import jp.kitabatakep.intellij.plugins.codereadingnote.Topic;
import jp.kitabatakep.intellij.plugins.codereadingnote.CodeReadingNoteService;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

/**
 * Action to add a new group to a topic
 */
public class GroupAddAction extends AnAction {
    
    private Supplier<Topic> topicSupplier;
    
    public GroupAddAction(Supplier<Topic> topicSupplier) {
        super("Add Group", "Add a new group to organize topic lines", null);
        this.topicSupplier = topicSupplier;
    }
    
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;
        
        Topic topic = topicSupplier.get();
        if (topic == null) return;
        
        // Ask for group name
        String groupName = Messages.showInputDialog(
            project,
            "Enter group name:",
            "Add Group",
            Messages.getQuestionIcon(),
            "",
            null
        );
        
        if (groupName != null && !groupName.trim().isEmpty()) {
            // Check if group name already exists
            if (topic.findGroupByName(groupName.trim()) != null) {
                Messages.showErrorDialog(
                    project,
                    "A group with this name already exists.",
                    "Duplicate Name"
                );
                return;
            }
            
            // Create new group
            topic.addGroup(groupName.trim());
            
            // Save changes
            CodeReadingNoteService service = CodeReadingNoteService.getInstance(project);
            // The service should automatically save due to the topic change notification
        }
    }
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        Topic topic = topicSupplier.get();
        e.getPresentation().setEnabled(topic != null);
    }
}

package jp.kitabatakep.intellij.plugins.codereadingnote.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import jp.kitabatakep.intellij.plugins.codereadingnote.CodeReadingNoteBundle;
import jp.kitabatakep.intellij.plugins.codereadingnote.Topic;
import jp.kitabatakep.intellij.plugins.codereadingnote.TopicLine;
import jp.kitabatakep.intellij.plugins.codereadingnote.TopicGroup;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

/**
 * Action to move a TopicLine to a group
 */
public class LineToGroupMoveAction extends AnAction {
    
    private Supplier<TopicLine> topicLineSupplier;
    
    public LineToGroupMoveAction(Supplier<TopicLine> topicLineSupplier) {
        super(
            CodeReadingNoteBundle.message("action.move.to.group"),
            CodeReadingNoteBundle.message("action.move.to.group.description"),
            null
        );
        this.topicLineSupplier = topicLineSupplier;
    }
    
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;
        
        TopicLine topicLine = topicLineSupplier.get();
        if (topicLine == null) return;
        
        Topic topic = topicLine.topic();
        
        // Enable groups if not already have any groups
        if (topic.getGroups().isEmpty()) {
            int result = Messages.showYesNoDialog(
                project,
                CodeReadingNoteBundle.message("dialog.move.to.group.no.groups"),
                CodeReadingNoteBundle.message("dialog.move.to.group.create.title"),
                Messages.getQuestionIcon()
            );
            
            if (result == Messages.YES) {
                String groupName = Messages.showInputDialog(
                    project,
                    CodeReadingNoteBundle.message("dialog.move.to.group.create.message"),
                    CodeReadingNoteBundle.message("dialog.move.to.group.create.title"),
                    Messages.getQuestionIcon(),
                    "Default Group",
                    null
                );
                if (groupName != null && !groupName.trim().isEmpty()) {
                    topic.addGroup(groupName.trim());
                } else {
                    return;
                }
            } else {
                return;
            }
        }
        
        // Get available groups
        if (topic.getGroups().isEmpty()) {
            Messages.showInfoMessage(
                project,
                CodeReadingNoteBundle.message("dialog.move.to.group.no.groups.available"),
                CodeReadingNoteBundle.message("dialog.move.to.group.no.groups.title")
            );
            return;
        }
        
        // Show group selection dialog
        String[] groupNames = topic.getGroups().stream()
                .map(TopicGroup::name)
                .toArray(String[]::new);
        
        String selectedGroupName = Messages.showEditableChooseDialog(
            CodeReadingNoteBundle.message("dialog.move.to.group.message"),
            CodeReadingNoteBundle.message("dialog.move.to.group.title"),
            Messages.getQuestionIcon(),
            groupNames,
            groupNames[0],
            null
        );
        
        if (selectedGroupName != null) {
            TopicGroup targetGroup = topic.findGroupByName(selectedGroupName);
            if (targetGroup != null) {
                topic.moveLineToGroup(topicLine, targetGroup);
            }
        }
    }
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        TopicLine topicLine = topicLineSupplier.get();
        e.getPresentation().setEnabled(topicLine != null);
    }
}

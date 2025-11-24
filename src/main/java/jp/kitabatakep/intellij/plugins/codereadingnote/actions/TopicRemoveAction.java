package jp.kitabatakep.intellij.plugins.codereadingnote.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import jp.kitabatakep.intellij.plugins.codereadingnote.CodeReadingNoteBundle;
import jp.kitabatakep.intellij.plugins.codereadingnote.CodeReadingNoteService;
import jp.kitabatakep.intellij.plugins.codereadingnote.Topic;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public class TopicRemoveAction extends CommonAnAction
{
    Function<Void, Topic> topicFetcher;
    Project project;

    public TopicRemoveAction(Project project, Function<Void, Topic> topicFetcher)
    {
        super(
            CodeReadingNoteBundle.message("action.remove.topic"),
            CodeReadingNoteBundle.message("action.remove.topic.description"),
            AllIcons.General.Remove
        );
        this.project = project;
        this.topicFetcher = topicFetcher;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(
            e.getProject() != null &&
                topicFetcher.apply(null) != null
        );
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e)
    {
        CodeReadingNoteService service = CodeReadingNoteService.getInstance(project);
        Topic topic = topicFetcher.apply(null);
        if (topic == null) { return; }

        int confirmationResult = Messages.showYesNoCancelDialog(
            CodeReadingNoteBundle.message("dialog.confirm.remove.topic.message"),
            CodeReadingNoteBundle.message("dialog.confirm.remove.topic.title"),
            Messages.getQuestionIcon()
        );

        if (confirmationResult == Messages.YES) {
            service.getTopicList().removeTopic(topic);
        }
    }
}

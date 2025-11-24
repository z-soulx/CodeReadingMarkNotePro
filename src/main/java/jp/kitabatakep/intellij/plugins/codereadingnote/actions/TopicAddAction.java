package jp.kitabatakep.intellij.plugins.codereadingnote.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.InputValidator;
import com.intellij.openapi.ui.Messages;
import jp.kitabatakep.intellij.plugins.codereadingnote.CodeReadingNoteBundle;
import jp.kitabatakep.intellij.plugins.codereadingnote.CodeReadingNoteService;
import org.jetbrains.annotations.NotNull;

public class TopicAddAction extends CommonAnAction
{
    public TopicAddAction() {
        super(
            CodeReadingNoteBundle.message("action.new.topic"),
            CodeReadingNoteBundle.message("action.new.topic.description"),
            AllIcons.General.Add
        );
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(e.getProject() != null);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e)
    {
        String newTopicName =  Messages.showInputDialog(
            CodeReadingNoteBundle.message("dialog.create.topic.message"),
            CodeReadingNoteBundle.message("dialog.create.topic.title"),
            Messages.getQuestionIcon(),
            "",
            new InputValidator()
            {
                @Override
                public boolean checkInput(String inputString) { return !inputString.trim().equals(""); }

                @Override
                public boolean canClose(String inputString) { return true; }
            }
        );

        if (newTopicName != null) {
            CodeReadingNoteService service = CodeReadingNoteService.getInstance(e.getProject());
            service.getTopicList().addTopic(newTopicName);
        }
    }
}

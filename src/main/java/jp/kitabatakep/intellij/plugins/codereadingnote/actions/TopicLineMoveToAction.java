package jp.kitabatakep.intellij.plugins.codereadingnote.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import jp.kitabatakep.intellij.plugins.codereadingnote.Topic;
import jp.kitabatakep.intellij.plugins.codereadingnote.TopicLine;
import org.jetbrains.annotations.NotNull;

public class TopicLineMoveToAction extends CommonAnAction
{
    private final Topic moveTo;
    private final TopicLine topicLine;

    public TopicLineMoveToAction(TopicLine topicLine, Topic moveTo)
    {
        super(moveTo.name(), moveTo.name(), null);
        this.moveTo = moveTo;
        this.topicLine = topicLine;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(e.getProject() != null && moveTo.context().equals(topicLine.topic().context()));
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e)
    {
        moveTo.moveLineHere(topicLine, null);
    }
}

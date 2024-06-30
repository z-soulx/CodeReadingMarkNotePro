package jp.kitabatakep.intellij.plugins.codereadingnote.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.InputValidator;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Pair;
import jp.kitabatakep.intellij.plugins.codereadingnote.CodeReadingNoteService;
import jp.kitabatakep.intellij.plugins.codereadingnote.Topic;
import jp.kitabatakep.intellij.plugins.codereadingnote.TopicLine;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public class ShowBookmarkUidAction extends AnAction
{
    Project project;
    Function<Void, Pair<Topic, TopicLine>> fetcher;
    public ShowBookmarkUidAction(Project project, Function<Void, Pair<Topic, TopicLine>> fetcher) {
        super("Show BookmarkUid", "ShowBookmarkUidAction", AllIcons.General.Information);
        this.project = project;
        this.fetcher = fetcher;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(e.getProject() != null);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e)
    {
        Pair<Topic, TopicLine> payload = fetcher.apply(null);
        if (payload.getSecond() == null) { return; }
        Messages.showInfoMessage(project,payload.getSecond().getBookmarkUid(),"Show BookmarkUid");
    }
}

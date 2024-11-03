package jp.kitabatakep.intellij.plugins.codereadingnote.actions;

import com.intellij.icons.AllIcons;
import com.intellij.ide.bookmark.Bookmark;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Pair;
import jp.kitabatakep.intellij.plugins.codereadingnote.Topic;
import jp.kitabatakep.intellij.plugins.codereadingnote.TopicLine;
import jp.kitabatakep.intellij.plugins.codereadingnote.remark.BookmarkUtils;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

public class FixLineRemarkAction extends CommonAnAction
{
    Project project;
    Function<Void, Pair<Topic, TopicLine>> fetcher;
    public FixLineRemarkAction(Project project, Function<Void, Pair<Topic, TopicLine>> fetcher) {
        super("Fix line RemarkAction Offset", "FixLineRemarkAction", AllIcons.General.Information);
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
        Map<String, Bookmark> stringBookmarkMap = BookmarkUtils.getStringBookmarkMap(project);
        Stream.of(payload.getSecond()).forEach(BookmarkUtils.consumerLine(stringBookmarkMap));
    }
}

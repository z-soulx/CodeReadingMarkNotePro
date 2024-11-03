package jp.kitabatakep.intellij.plugins.codereadingnote.actions;

import com.intellij.icons.AllIcons;
import com.intellij.ide.bookmark.Bookmark;
import com.intellij.ide.bookmark.BookmarkGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import jp.kitabatakep.intellij.plugins.codereadingnote.CodeReadingNoteService;
import jp.kitabatakep.intellij.plugins.codereadingnote.Topic;
import jp.kitabatakep.intellij.plugins.codereadingnote.TopicLine;
import jp.kitabatakep.intellij.plugins.codereadingnote.remark.BookmarkUtils;
import jp.kitabatakep.intellij.plugins.codereadingnote.remark.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class FixTopicRemarkAction extends CommonAnAction
{
    private Project project;
    private Function<Void, Topic> topicFetcher;
    public FixTopicRemarkAction(Project project, Function<Void, Topic> topicFetcher) {
        super("Fix Topic RemarkAction Offset", "FixTopicRemarkAction", AllIcons.General.Warning);
        this.project = project;
        this.topicFetcher = topicFetcher;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(e.getProject() != null);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e)
    {
        Topic topic = topicFetcher.apply(null);
        if (topic == null) { return; }
        Map<String, Bookmark> collect = BookmarkUtils.getStringBookmarkMap(project);
        topic.getLines().forEach(BookmarkUtils.consumerLine(collect));
    }




}

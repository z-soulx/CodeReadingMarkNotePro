package jp.kitabatakep.intellij.plugins.codereadingnote.actions;

import com.intellij.icons.AllIcons;
import com.intellij.ide.bookmark.Bookmark;
import com.intellij.ide.bookmark.BookmarkGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import jp.kitabatakep.intellij.plugins.codereadingnote.CodeReadingNoteService;
import jp.kitabatakep.intellij.plugins.codereadingnote.remark.BookmarkUtils;
import jp.kitabatakep.intellij.plugins.codereadingnote.remark.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FixRemarkAction extends AnAction
{
    private Project project;
    public FixRemarkAction(Project project) {
        super("Fix All RemarkAction Offset", "FixRemarkAction", AllIcons.General.WarningDialog);
        this.project = project;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(e.getProject() != null);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e)
    {
        CodeReadingNoteService service = CodeReadingNoteService.getInstance(e.getProject());
        List<Bookmark> allBookmark = BookmarkUtils.getAllBookmark(project);
        BookmarkGroup group = BookmarkUtils.getGroup(project);
        Map<String, Bookmark> collect = allBookmark.stream().collect(Collectors.toMap(r -> StringUtils.extractUUID(group.getDescription(r)), value -> value));
        service.getTopicList().getTopics().stream().flatMap(r -> r.getLines().stream()).forEach(  topicLine -> {
            Bookmark bookmark = collect.get(topicLine.getBookmarkUid());
            if ( bookmark != null ){
                String newline = bookmark.getAttributes().get("line").toString();
                if (StringUtils.isNotEmpty(newline) && !newline.equals(String.valueOf(topicLine.line()))) {
                topicLine.modifyLine(Integer.valueOf(newline));
            }
        }
        });
    }

}

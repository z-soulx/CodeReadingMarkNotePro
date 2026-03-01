package jp.kitabatakep.intellij.plugins.codereadingnote.remark;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import icons.MyIcons;
import jp.kitabatakep.intellij.plugins.codereadingnote.CodeReadingNoteBundle;
import jp.kitabatakep.intellij.plugins.codereadingnote.CodeReadingNoteService;
import jp.kitabatakep.intellij.plugins.codereadingnote.TopicLine;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Objects;

/**
 * Custom gutter icon (bookmark ribbon) for lines that have a code reading note.
 * Click opens an edit popup; hover shows note + action hints.
 */
public class NoteGutterIconRenderer extends GutterIconRenderer {

    private final String topicLineUid;
    private final String notePreview;
    private final Project project;

    public NoteGutterIconRenderer(@NotNull Project project, @NotNull String topicLineUid, @NotNull String notePreview) {
        this.project = project;
        this.topicLineUid = topicLineUid;
        this.notePreview = notePreview;
    }

    @NotNull
    @Override
    public Icon getIcon() {
        return MyIcons.GUTTER_NOTE;
    }

    @Nullable
    @Override
    public String getTooltipText() {
        StringBuilder sb = new StringBuilder();
        if (!notePreview.isEmpty()) {
            sb.append(notePreview).append("\n\n");
        }
        sb.append("[").append(CodeReadingNoteBundle.message("gutter.tooltip.click")).append("] ")
          .append(CodeReadingNoteBundle.message("gutter.tooltip.edit")).append("  ");
        sb.append("[Alt+Enter] ").append(CodeReadingNoteBundle.message("gutter.tooltip.edit.or.locate"));
        return sb.toString();
    }

    @NotNull
    public String getTopicLineUid() {
        return topicLineUid;
    }

    @Nullable
    @Override
    public AnAction getClickAction() {
        return new AnAction() {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                TopicLine topicLine = findTopicLine();
                if (topicLine == null) return;

                Editor editor = e.getData(com.intellij.openapi.actionSystem.CommonDataKeys.EDITOR);
                if (editor == null) {
                    editor = EditorUtils.getEditor(FileEditorManager.getInstance(project), topicLine.file());
                }
                if (editor == null) return;

                NotePopupHelper.show(editor, project, topicLine);
            }
        };
    }

    @NotNull
    @Override
    public Alignment getAlignment() {
        return Alignment.LEFT;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NoteGutterIconRenderer that = (NoteGutterIconRenderer) o;
        return Objects.equals(topicLineUid, that.topicLineUid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(topicLineUid);
    }

    @Nullable
    private TopicLine findTopicLine() {
        CodeReadingNoteService service = CodeReadingNoteService.getInstance(project);
        for (jp.kitabatakep.intellij.plugins.codereadingnote.Topic topic : service.getTopicList().getTopics()) {
            for (TopicLine tl : topic.getLines()) {
                if (topicLineUid.equals(tl.getBookmarkUid())) {
                    return tl;
                }
            }
        }
        return null;
    }
}

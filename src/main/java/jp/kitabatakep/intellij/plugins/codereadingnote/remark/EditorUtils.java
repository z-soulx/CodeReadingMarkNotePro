/*
 * MIT License
 *
 * Copyright (c) 2021 吴汶泽 <wenzewoo@gmail.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package jp.kitabatakep.intellij.plugins.codereadingnote.remark;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorCustomElementRenderer;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.HighlighterTargetArea;
import com.intellij.openapi.editor.markup.MarkupModel;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import jp.kitabatakep.intellij.plugins.codereadingnote.TopicLine;
import org.jetbrains.annotations.NotNull;

public class EditorUtils {

    public static VirtualFile getVirtualFile(@NotNull final Editor editor) {
        if (editor instanceof EditorEx)
            return ((EditorEx) editor).getVirtualFile();
        return null;
    }

    public static void addLineCodeRemark(Project project, TopicLine _topicLine) {
        if (_topicLine.file() == null || !_topicLine.file().isValid()) return;
        FileEditorManager instance = FileEditorManager.getInstance(project);
        Editor editor = getEditor(instance, _topicLine.file());
        if (editor != null) {
            String uid = _topicLine.getBookmarkUid();
            String noteText = StringUtils.spNote(_topicLine.note());
            EditorUtils.addAfterLineCodeRemark(editor, _topicLine.line(), noteText, uid);
            EditorUtils.addGutterIcon(editor, project, _topicLine.line(), uid, noteText);
        }
    }

    public static void removeLineCodeRemark(Project project, TopicLine _topicLine) {
        if (_topicLine.file() == null || !_topicLine.file().isValid()) return;
        FileEditorManager instance = FileEditorManager.getInstance(project);
        Editor editor = getEditor(instance, _topicLine.file());
        if (editor != null) {
            EditorUtils.clearAfterLineEndCodeRemark(editor, _topicLine.line());
            EditorUtils.removeGutterIcon(editor, _topicLine.getBookmarkUid());
        }
    }
    public static Editor getEditor(@NotNull final FileEditorManager source, @NotNull final VirtualFile file) {
        final FileEditor fileEditor = source.getSelectedEditor(file);
        if (!(fileEditor instanceof TextEditor)) return null;

        return ((TextEditor) fileEditor).getEditor();
    }
    public static int getLineNumber(@NotNull final Editor editor) {
        return editor.getDocument().getLineNumber(
                editor.getCaretModel().getOffset());
    }

    public static void addAfterLineCodeRemark(@NotNull final Editor editor, final int lineNumber, @NotNull final String text) {
        addAfterLineEndElement(editor, lineNumber, new CodeRemarkEditorInlineInlayRenderer(text));
    }

    public static void addAfterLineCodeRemark(@NotNull final Editor editor, final int lineNumber,
                                               @NotNull final String text, String topicLineUid) {
        addAfterLineEndElement(editor, lineNumber, new CodeRemarkEditorInlineInlayRenderer(text, topicLineUid));
    }

    public static void addAfterLineEndElement(
            @NotNull final Editor editor, final int lineNumber, @NotNull final EditorCustomElementRenderer renderer) {
        try {
            // if exists, clear it.
            clearAfterLineEndElement(editor, lineNumber, renderer.getClass());
            if (lineNumber > editor.getDocument().getLineCount()) return;
            final int endOffset = editor.getDocument().getLineEndOffset(lineNumber);
            editor.getInlayModel().addAfterLineEndElement(endOffset, true, renderer);
        } catch (final Throwable e) {
            e.printStackTrace();
        }
    }

    public static void clearAfterLineEndCodeRemark(@NotNull final Editor editor, final int lineNumber) {
        clearAfterLineEndElement(editor, lineNumber, CodeRemarkEditorInlineInlayRenderer.class);
    }

    public static void clearAfterLineEndElement(
            @NotNull final Editor editor, final int lineNumber, @NotNull final Class<? extends EditorCustomElementRenderer> rendererClass) {
        try {
            editor.getInlayModel().getAfterLineEndElementsForLogicalLine(lineNumber).forEach(inlay -> {
                if (inlay.getRenderer().getClass().getName().equals(rendererClass.getName())) {
                    Disposer.dispose(inlay);
                }
            });
        } catch (final Throwable e) {
            e.printStackTrace();
        }
    }

    // ========== Gutter Icon Methods ==========

    public static void addGutterIcon(@NotNull final Editor editor, @NotNull final Project project,
                                      final int lineNumber, String topicLineUid, @NotNull final String notePreview) {
        try {
            if (lineNumber >= editor.getDocument().getLineCount()) return;
            if (topicLineUid == null || topicLineUid.isEmpty()) return;

            removeGutterIcon(editor, topicLineUid);

            int startOffset = editor.getDocument().getLineStartOffset(lineNumber);
            MarkupModel markupModel = editor.getMarkupModel();
            RangeHighlighter highlighter = markupModel.addRangeHighlighter(
                    startOffset, startOffset, HighlighterLayer.LAST,
                    null, HighlighterTargetArea.LINES_IN_RANGE);
            highlighter.setGutterIconRenderer(new NoteGutterIconRenderer(project, topicLineUid, notePreview));
        } catch (final Throwable e) {
            e.printStackTrace();
        }
    }

    public static void removeGutterIcon(@NotNull final Editor editor, String topicLineUid) {
        try {
            if (topicLineUid == null || topicLineUid.isEmpty()) return;
            MarkupModel markupModel = editor.getMarkupModel();
            for (RangeHighlighter highlighter : markupModel.getAllHighlighters()) {
                if (highlighter.getGutterIconRenderer() instanceof NoteGutterIconRenderer) {
                    NoteGutterIconRenderer renderer = (NoteGutterIconRenderer) highlighter.getGutterIconRenderer();
                    if (topicLineUid.equals(renderer.getTopicLineUid())) {
                        markupModel.removeHighlighter(highlighter);
                        return;
                    }
                }
            }
        } catch (final Throwable e) {
            e.printStackTrace();
        }
    }

    public static void addGutterIconForTopicLine(@NotNull final Project project, @NotNull final TopicLine topicLine) {
        if (topicLine.file() == null || !topicLine.file().isValid()) return;
        FileEditorManager instance = FileEditorManager.getInstance(project);
        Editor editor = getEditor(instance, topicLine.file());
        if (editor != null) {
            addGutterIcon(editor, project, topicLine.line(),
                    topicLine.getBookmarkUid(), StringUtils.spNote(topicLine.note()));
        }
    }

    public static void removeGutterIconForTopicLine(@NotNull final Project project, @NotNull final TopicLine topicLine) {
        if (topicLine.file() == null || !topicLine.file().isValid()) return;
        FileEditorManager instance = FileEditorManager.getInstance(project);
        Editor editor = getEditor(instance, topicLine.file());
        if (editor != null) {
            removeGutterIcon(editor, topicLine.getBookmarkUid());
        }
    }
}

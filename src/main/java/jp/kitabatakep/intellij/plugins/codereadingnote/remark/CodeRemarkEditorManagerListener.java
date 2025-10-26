/*
 * MIT License
 *
 * Copyright (c) 2023 吴汶泽<wenzewoo@gmail.com>
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

import com.intellij.ide.bookmark.Bookmark;
import com.intellij.ide.bookmark.BookmarkGroup;
import com.intellij.ide.bookmark.BookmarksManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import jp.kitabatakep.intellij.plugins.codereadingnote.AppConstants;
import jp.kitabatakep.intellij.plugins.codereadingnote.CodeReadingNoteService;
import jp.kitabatakep.intellij.plugins.codereadingnote.TopicLine;
import jp.kitabatakep.intellij.plugins.codereadingnote.autofix.AutoFixService;
import jp.kitabatakep.intellij.plugins.codereadingnote.autofix.AutoFixSettings;
import jp.kitabatakep.intellij.plugins.codereadingnote.autofix.FixTrigger;
import jp.kitabatakep.intellij.plugins.codereadingnote.autofix.LineOffsetDetector;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

public class CodeRemarkEditorManagerListener implements FileEditorManagerListener {
    private static final Logger LOG = Logger.getInstance(CodeRemarkEditorManagerListener.class);

    @Override
    public void fileOpened(@NotNull final FileEditorManager source, @NotNull final VirtualFile file) {
        final Project project = source.getProject();

        final Editor editor = getEditor(source, file);
        if (null == editor) return; // Skipped.
        
        // 🔧 修复：使用实际的 TopicLine 数据而不是 CodeRemark
        // 这样可以确保使用最新的行号（已同步 Bookmark）
        List<TopicLine> topicLines = getTopicLinesForFile(project, file);
        
        // 🔧 修复：在显示 remark 前，先同步行号
        fixOffsetBeforeDisplay(project, topicLines);
        
        // 显示 CodeRemark（使用同步后的行号）
        topicLines.forEach(topicLine -> {
            try {
                if (topicLine.file() != null && topicLine.file().equals(file)) {
                    EditorUtils.addAfterLineCodeRemark(editor, topicLine.line(), StringUtils.spNote(topicLine.note()));
                }
            } catch (Exception e) {
                LOG.warn("Failed to add CodeRemark for line: " + topicLine.line(), e);
            }
        });
        
        // 🆕 检测错位并可选通知
        detectOffsetIfEnabled(project);
    }
    
    /**
     * 获取该文件的所有 TopicLine
     */
    private List<TopicLine> getTopicLinesForFile(@NotNull Project project, @NotNull VirtualFile file) {
        CodeReadingNoteService service = CodeReadingNoteService.getInstance(project);
        return service.getTopicList().getTopics().stream()
                .flatMap(topic -> topic.getLines().stream())
                .filter(line -> line.file() != null && line.file().equals(file))
                .collect(Collectors.toList());
    }
    
    /**
     * 🔧 修复：在显示前同步行号
     * 这解决了"关闭文件再重新打开才能显示"的问题
     */
    private void fixOffsetBeforeDisplay(@NotNull Project project, @NotNull List<TopicLine> lines) {
        try {
            AutoFixSettings settings = AutoFixSettings.getInstance();
            
            // 如果启用了文件打开时自动修复
            if (settings.isAutoFixEnabled() && settings.isFixOnFileOpen()) {
                AutoFixService.getInstance().fixLines(project, lines, FixTrigger.FILE_OPENED);
            } else {
                // 否则至少检测一下，不修复但更新缓存
                LineOffsetDetector.getInstance().detectLines(project, lines);
            }
        } catch (Exception e) {
            LOG.warn("Failed to fix offset before display", e);
        }
    }
    
    /**
     * 🆕 检测错位并通知
     */
    private void detectOffsetIfEnabled(@NotNull Project project) {
        try {
            AutoFixSettings settings = AutoFixSettings.getInstance();
            
            if (settings.isAutoFixEnabled() && settings.isDetectOnFileOpen()) {
                // 异步检测，避免阻塞文件打开
                AutoFixService.getInstance().detectAndNotify(project);
            }
        } catch (Exception e) {
            LOG.warn("Failed to detect offset", e);
        }
    }

    private void fixOffset(List<CodeRemark> machRemarklist, Project project) {
        //wait dev
       BookmarkUtils.getAllBookmark(project);
    }

    private Editor getEditor(@NotNull final FileEditorManager source, @NotNull final VirtualFile file) {
        final FileEditor fileEditor = source.getSelectedEditor(file);
        if (!(fileEditor instanceof TextEditor)) return null;

        return ((TextEditor) fileEditor).getEditor();
    }
}
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

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

public class CodeRemarkEditorManagerListener implements FileEditorManagerListener {

    @Override
    public void fileOpened(@NotNull final FileEditorManager source, @NotNull final VirtualFile file) {
        final Project project = source.getProject();
//        CodeRemarkEditorInlineInlayListener.getInstance(project).startListening();

        final Editor editor = getEditor(source, file);
        if (null == editor) return; // Skipped.

        CodeRemarkRepositoryFactory.getInstance(project).list(project, file).forEach(codeRemark -> {
            EditorUtils.addAfterLineCodeRemark(editor, codeRemark.getLineNumber(), codeRemark.getText());
        });
    }

    private Editor getEditor(@NotNull final FileEditorManager source, @NotNull final VirtualFile file) {
        final FileEditor fileEditor = source.getSelectedEditor(file);
        if (!(fileEditor instanceof TextEditor)) return null;

        return ((TextEditor) fileEditor).getEditor();
    }
}
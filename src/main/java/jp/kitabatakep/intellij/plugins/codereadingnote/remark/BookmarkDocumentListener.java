package jp.kitabatakep.intellij.plugins.codereadingnote.remark;

/**
 * @program: CodeReadingMarkNotePro
 * @description:
 * @author:
 * @create: 2024-06-25 19:36
 **/

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.Inlay;
import com.intellij.openapi.editor.RangeMarker;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.editor.impl.EditorFactoryImpl;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import java.util.List;
import org.apache.commons.collections.CollectionUtils;
import org.jetbrains.annotations.NotNull;

/**
 * @author: codeleep  copy bookmark-x
 * @createTime: 2024/03/20 19:44
 * @description: 文档变化监听
 */
public class BookmarkDocumentListener implements DocumentListener {

	private static final Logger LOG = Logger.getInstance(BookmarkDocumentListener.class);

	@Override
	public void documentChanged(@NotNull DocumentEvent event) {
		try {
			Document document = event.getDocument();

			VirtualFile virtualFile = FileDocumentManager.getInstance().getFile(document);
			Editor editor = getEditor(document);

			if (virtualFile == null || editor == null) {
				return;
			}
			Project project = editor.getProject();
			if (project == null) {
				return;
			}
			// 获取当前 Inlay 模型
			List<Inlay<? extends CodeRemarkEditorInlineInlayRenderer>> inlays = editor.getInlayModel()
					.getAfterLineEndElementsInRange(0, document.getTextLength(),
							CodeRemarkEditorInlineInlayRenderer.class);
			if (CollectionUtils.isEmpty(inlays)) return;
			CodeRemarkRepositoryFactory.getInstance(project).listSource(project, virtualFile).forEach(
					r -> {
						for (Inlay<? extends CodeRemarkEditorInlineInlayRenderer> inlay : inlays) {
							if (StringUtils.spNote(r.note()).equals(inlay.getRenderer().getText())) {
								int offset = inlay.getOffset();
								int line = document.getLineNumber(offset);
								r.modifyLine(line);
							}
						}
					}
			);


		} catch (Exception e) {
			LOG.info("perceivedLineChange error", e);
		}
	}


	private void perceivedLineChange(Project project, List<CodeRemark> indexList) {

	}

	private Editor getEditor(Document document) {
		Editor[] editors = EditorFactoryImpl.getInstance().getEditors(document);
		if (editors.length >= 1) {
			return editors[0];
		}
		return null;
	}

}

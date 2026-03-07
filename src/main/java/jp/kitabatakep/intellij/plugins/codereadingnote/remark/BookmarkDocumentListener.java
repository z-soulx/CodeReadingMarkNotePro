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
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.editor.impl.EditorFactoryImpl;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import jp.kitabatakep.intellij.plugins.codereadingnote.TopicLine;
import java.util.List;
import org.apache.commons.collections.CollectionUtils;
import org.jetbrains.annotations.NotNull;

/**
 * Keeps TopicLine line numbers in sync when the document changes.
 * Matches inlays to TopicLines by UID for reliable tracking, with
 * text-based fallback for legacy data without UIDs.
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

			List<Inlay<? extends CodeRemarkEditorInlineInlayRenderer>> inlays = editor.getInlayModel()
					.getAfterLineEndElementsInRange(0, document.getTextLength(),
							CodeRemarkEditorInlineInlayRenderer.class);
			if (CollectionUtils.isEmpty(inlays)) return;

			List<TopicLine> topicLines = CodeRemarkRepositoryFactory.getInstance(project).listSource(project, virtualFile);
			for (TopicLine tl : topicLines) {
				for (Inlay<? extends CodeRemarkEditorInlineInlayRenderer> inlay : inlays) {
					CodeRemarkEditorInlineInlayRenderer renderer = inlay.getRenderer();
					boolean matched = false;

					String tlUid = tl.getBookmarkUid();
					String rendererUid = renderer.getTopicLineUid();
					if (tlUid != null && !tlUid.isEmpty() && rendererUid != null && !rendererUid.isEmpty()) {
						matched = tlUid.equals(rendererUid);
					} else {
						matched = StringUtils.spNote(tl.note()).equals(renderer.getText());
					}

					if (matched) {
						int offset = inlay.getOffset();
						int line = document.getLineNumber(offset);
						tl.modifyLine(line);
						break;
					}
				}
			}

		} catch (Exception e) {
			LOG.info("perceivedLineChange error", e);
		}
	}

	private Editor getEditor(Document document) {
		Editor[] editors = EditorFactoryImpl.getInstance().getEditors(document);
		if (editors.length >= 1) {
			return editors[0];
		}
		return null;
	}

}

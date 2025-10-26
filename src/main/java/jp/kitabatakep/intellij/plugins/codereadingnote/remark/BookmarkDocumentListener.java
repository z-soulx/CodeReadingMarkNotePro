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
			
			// 🔧 修复：使用 Inlay 的实际位置更新 TopicLine
			// Inlay 会自动跟随文档变化移动，我们只需要读取它的新位置
			List<Inlay<? extends CodeRemarkEditorInlineInlayRenderer>> inlays = editor.getInlayModel()
					.getAfterLineEndElementsInRange(0, document.getTextLength(),
							CodeRemarkEditorInlineInlayRenderer.class);
			
			if (CollectionUtils.isEmpty(inlays)) {
				return;
			}
			
			// 获取该文件的所有 TopicLine
			List<jp.kitabatakep.intellij.plugins.codereadingnote.TopicLine> topicLines = 
					CodeRemarkRepositoryFactory.getInstance(project).listSource(project, virtualFile);
			
			if (CollectionUtils.isEmpty(topicLines)) {
				return;
			}
			
			// 🔧 修复：通过 note 内容匹配 TopicLine 和 Inlay
			// 更新 TopicLine 的行号为 Inlay 的实际行号
			for (jp.kitabatakep.intellij.plugins.codereadingnote.TopicLine topicLine : topicLines) {
				String notePrefix = StringUtils.spNote(topicLine.note());
				
				for (Inlay<? extends CodeRemarkEditorInlineInlayRenderer> inlay : inlays) {
					if (notePrefix.equals(inlay.getRenderer().getText())) {
						try {
							int offset = inlay.getOffset();
							int newLine = document.getLineNumber(offset);
							
							// 只在行号真的变化时才更新
							if (newLine != topicLine.line()) {
								LOG.debug(String.format("Updating TopicLine from line %d to %d", 
										topicLine.line(), newLine));
								topicLine.modifyLine(newLine);
							}
						} catch (Exception e) {
							LOG.warn("Failed to update TopicLine position", e);
						}
						break; // 找到匹配的就跳出
					}
				}
			}

		} catch (Exception e) {
			LOG.warn("documentChanged error", e);
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

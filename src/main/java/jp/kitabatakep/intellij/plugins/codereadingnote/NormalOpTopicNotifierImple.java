package jp.kitabatakep.intellij.plugins.codereadingnote;

import com.intellij.ide.bookmark.Bookmark;
import com.intellij.openapi.project.Project;
import java.util.UUID;
import jp.kitabatakep.intellij.plugins.codereadingnote.remark.BookmarkUtils;
import jp.kitabatakep.intellij.plugins.codereadingnote.remark.EditorUtils;

/**
 * @program: CodeReadingMarkNotePro
 * @description:
 * @author:
 * @create: 2024-07-01 10:33
 **/
public class NormalOpTopicNotifierImple implements TopicNotifier {

	private Project project;

	public NormalOpTopicNotifierImple(Project project) {
		this.project = project;
	}

	@Override
	public void lineRemoved(Topic topic, TopicLine _topicLine) {
			BookmarkUtils.removeMachBookmark(_topicLine,project);
			EditorUtils.removeLineCodeRemark(project,_topicLine);

	}

	@Override
	public void lineAdded(Topic topic, TopicLine _topicLine) {
			String uid = UUID.randomUUID().toString();
			Bookmark bookmark = BookmarkUtils.addBookmark(project, _topicLine.file(), _topicLine.line(), _topicLine.note(), uid);
			if (bookmark != null) {
				_topicLine.setBookmarkUid(uid);
			}
			EditorUtils.addLineCodeRemark(project, _topicLine);

	}
}

package jp.kitabatakep.intellij.plugins.codereadingnote.ui;

import com.intellij.ide.DataManager;
import com.intellij.ide.bookmarks.Bookmark;
import com.intellij.ide.bookmarks.BookmarkManager;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.RowsDnDSupport;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.ui.EditableModel;
import com.intellij.util.ui.UIUtil;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.Iterator;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import jp.kitabatakep.intellij.plugins.codereadingnote.AppConstants;
import jp.kitabatakep.intellij.plugins.codereadingnote.Topic;
import jp.kitabatakep.intellij.plugins.codereadingnote.TopicLine;
import jp.kitabatakep.intellij.plugins.codereadingnote.TopicNotifier;
import jp.kitabatakep.intellij.plugins.codereadingnote.actions.TopicLineMoveToGroupAction;
import jp.kitabatakep.intellij.plugins.codereadingnote.actions.TopicLineRemoveAction;
import jp.kitabatakep.intellij.plugins.codereadingnote.remark.EditorUtils;
import jp.kitabatakep.intellij.plugins.codereadingnote.remark.StringUtils;
import org.jetbrains.annotations.NotNull;

class TopicDetailPanel extends JPanel {

	private Project project;
	private MyEditorTextField noteArea;
	private TopicLineDetailPanel topicLineDetailPanel;

	private JBList<TopicLine> topicLineList;
	private TopicLineListModel<TopicLine> topicLineListModel = new TopicLineListModel<>();

	private Topic topic;
	private TopicLine selectedTopicLine;

	public TopicDetailPanel(Project project) {
		super(new BorderLayout());

		this.project = project;

		JBSplitter contentPane = new JBSplitter(true, 0.2f);
		contentPane.setSplitterProportionKey(
				AppConstants.appName + "TopicDetailPanelContentPane.splitter");

		noteArea = new MyEditorTextField(project,
				FileTypeManager.getInstance().getStdFileType("Markdown"));
		noteArea.setOneLineMode(false);
		noteArea.setEnabled(false);

		contentPane.setFirstComponent(noteArea);

		initTopicLineList();
		topicLineDetailPanel = new TopicLineDetailPanel(project);

		JBSplitter topicLinePane = new JBSplitter(0.2f);
		topicLinePane.setSplitterProportionKey(
				AppConstants.appName + "TopicDetailPanelTopicLinePane.splitter");
		topicLinePane.setFirstComponent(new JBScrollPane(topicLineList));
		topicLinePane.setSecondComponent(topicLineDetailPanel);
		topicLinePane.setHonorComponentsMinimumSize(false);

		contentPane.setSecondComponent(topicLinePane);
		add(contentPane);

		MessageBus messageBus = project.getMessageBus();
		messageBus.connect().subscribe(TopicNotifier.TOPIC_NOTIFIER_TOPIC, new TopicNotifier() {
			@Override
			public void lineRemoved(Topic _topic, TopicLine _topicLine) {
				if (_topic == topic) {
					topicLineListModel.removeElement(_topicLine);
					BookmarkManager.getInstance(project).getFileBookmarks(_topicLine.file()).stream().forEach(r -> {
              if (r.hashCode() == _topicLine.bookmarkHash() || r.getDescription().equals(StringUtils.spNote(_topicLine.note()))) {
	              BookmarkManager instance = BookmarkManager.getInstance(project);
	              instance
		                  .removeBookmark(r);

              }
					});
				}
			}

			@Override
			public void lineAdded(Topic _topic, TopicLine _topicLine) {
				if (_topic == topic) {
					Bookmark bookmark = addBookmark(_topicLine.file(), _topicLine.line(), _topicLine.note());
            if (bookmark != null) {
                _topicLine.setBookmarkHash(bookmark.hashCode());
            }
					topicLineListModel.addElement(_topicLine);
					EditorUtils.addLineCodeRemark(project, _topicLine);
				}
			}
		});
	}

	public Bookmark addBookmark(@NotNull VirtualFile file, int line, String note) {
		BookmarkManager bookmarkManager = BookmarkManager.getInstance(project);
		Document document = FileDocumentManager.getInstance().getDocument(file);

		if (document != null && line < document.getLineCount()) {
//            int offset = document.getLineStartOffset(line);
//            OpenFileDescriptor descriptor = new OpenFileDescriptor(project, file, line);
			Bookmark bookmark = bookmarkManager.addTextBookmark(file, line,
					note.substring(0, Math.min(note.length(), 20)));
			Collection<Bookmark> fileBookmarks = bookmarkManager.getFileBookmarks(file);

			return bookmark;
			// 执行自定义逻辑
//            executeCustomLogic(bookmark, descriptor);
		}
		return null;

	}

	@Override
	public void removeNotify() {
		super.removeNotify();
		if (noteArea.getEditor() != null) {
			EditorFactory.getInstance().releaseEditor(noteArea.getEditor());
		}
	}

	private static class NoteAreaListener implements DocumentListener {

		TopicDetailPanel topicDetailPanel;

		private NoteAreaListener(TopicDetailPanel topicDetailPanel) {
			this.topicDetailPanel = topicDetailPanel;
		}

		public void documentChanged(com.intellij.openapi.editor.event.DocumentEvent e) {
			Document doc = e.getDocument();
			topicDetailPanel.topic.setNote(doc.getText());
		}
	}

	private void initTopicLineList() {
		topicLineList = new JBList<>();
		topicLineList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		topicLineList.setCellRenderer(new TopicLineListCellRenderer<>(project));
		topicLineList.addListSelectionListener(e -> {
			TopicLine topicLine = topicLineList.getSelectedValue();
			if (topicLine == null) {
				topicLineDetailPanel.clear();
			} else if (selectedTopicLine == null || topicLine != selectedTopicLine) {
				selectedTopicLine = topicLine;
				topicLineDetailPanel.setTopicLine(topicLine);
			}
		});

		topicLineList.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				int index = topicLineList.locationToIndex(e.getPoint());
				TopicLine topicLine = topicLineListModel.get(index);

				if (e.getClickCount() >= 2) {
					topicLine.navigate(true);
				}

				if (SwingUtilities.isRightMouseButton(e)) {
					DefaultActionGroup actions = new DefaultActionGroup();
					actions.add(new TopicLineRemoveAction(project, (v) -> new Pair<>(topic, topicLine)));
					actions.add(new TopicLineMoveToGroupAction(topicLine));
					JBPopupFactory.getInstance().createActionGroupPopup(
							null,
							actions,
							DataManager.getInstance().getDataContext(topicLineList),
							false,
							null,
							10
					).show(new RelativePoint(e));
				}
			}
		});

		topicLineList.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_DELETE || e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
					TopicLine topicLine = topicLineList.getSelectedValue();
					ActionUtil.performActionDumbAwareWithCallbacks(
							new TopicLineRemoveAction(project, (v) -> {
								return new Pair<>(topic, topicLine);
							}),
							ActionUtil.createEmptyEvent()
					);
				}
			}
		});

		topicLineList.setDragEnabled(true);
		RowsDnDSupport.install(topicLineList, topicLineListModel);
	}

	void clear() {
		noteArea.setDocument(EditorFactory.getInstance().createDocument(""));
		noteArea.setEnabled(false);
		topicLineListModel.clear();
		topicLineDetailPanel.clear();
		selectedTopicLine = null;
	}

	void setTopic(Topic topic) {
		this.topic = topic;
		selectedTopicLine = null;

		noteArea.setEnabled(true);
		if (topic.note().equals("")) {
			noteArea.setPlaceholder(" Topic note input area (Markdown)");
		}
		noteArea.setDocument(EditorFactory.getInstance().createDocument(topic.note()));
		noteArea.getDocument().addDocumentListener(new NoteAreaListener(this));

		topicLineListModel.clear();
		Iterator<TopicLine> iterator = topic.linesIterator();
		while (iterator.hasNext()) {
			topicLineListModel.addElement(iterator.next());
		}

		topicLineList.setModel(topicLineListModel);
	}

	private static class TopicLineListCellRenderer<T> extends SimpleColoredComponent implements
			ListCellRenderer<T> {

		private Project project;

		private TopicLineListCellRenderer(Project project) {
			this.project = project;
			setOpaque(true);
		}

		public Component getListCellRendererComponent(
				JList list,
				Object value,
				int index,
				boolean isSelected,
				boolean cellHasFocus) {
			clear();
			TopicLine topicLine = (TopicLine) value;
			VirtualFile file = topicLine.file();

//            PsiElement fileOrDir = PsiUtilCore.findFileSystemItem(project, file);
//            if (fileOrDir != null) {
//                setIcon(fileOrDir.getIcon(0));
//            }
			ApplicationManager.getApplication().runReadAction(() -> {
				PsiElement fileOrDir = PsiUtilCore.findFileSystemItem(project, file);
				if (fileOrDir != null) {
					setIcon(fileOrDir.getIcon(0));
				}
			});

			if (topicLine.isValid()) {
//                append(file.getName() + ":" + (topicLine.line()+1));
				append(topicLine.note().substring(0, Math.min(topicLine.note().length(), 20)));
				append(" (" + topicLine.pathForDisplay() + ")", SimpleTextAttributes.GRAY_ATTRIBUTES);
			} else {
				append(topicLine.pathForDisplay() + ":" + (topicLine.line() + 1),
						SimpleTextAttributes.ERROR_ATTRIBUTES);
			}

			setForeground(UIUtil.getListSelectionForeground(isSelected));
			setBackground(UIUtil.getListSelectionBackground(isSelected));
			return this;
		}
	}

	private class TopicLineListModel<T> extends DefaultListModel<T> implements EditableModel {

		public void addRow() {
		}

		public void removeRow(int i) {
		}

		public boolean canExchangeRows(int oldIndex, int newIndex) {
			return true;
		}

		public void exchangeRows(int oldIndex, int newIndex) {
			TopicLine target = (TopicLine) get(oldIndex);
			remove(oldIndex);
			add(newIndex, (T) target);
			topic.changeLineOrder(target, newIndex);
		}
	}
}

package jp.kitabatakep.intellij.plugins.codereadingnote.ui;

import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.ActionPopupMenu;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.RowsDnDSupport;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.ui.EditableModel;
import com.intellij.util.ui.UIUtil;
import jp.kitabatakep.intellij.plugins.codereadingnote.*;
import jp.kitabatakep.intellij.plugins.codereadingnote.actions.FixLineRemarkAction;
import jp.kitabatakep.intellij.plugins.codereadingnote.actions.ShowBookmarkUidAction;
import jp.kitabatakep.intellij.plugins.codereadingnote.actions.TopicLineMoveToGroupAction;
import jp.kitabatakep.intellij.plugins.codereadingnote.actions.TopicLineRemoveAction;
import jp.kitabatakep.intellij.plugins.codereadingnote.remark.BookmarkUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Iterator;

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
					BookmarkUtils.removeMachBookmark(_topicLine,project);
					// Mobile monitoring that does not rely on Panel
					//移动不依赖Panel的监听
//					EditorUtils.removeLineCodeRemark(project,_topicLine);
				}
			}

		@Override
		public void lineAdded(Topic _topic, TopicLine _topicLine) {
			if (_topic == topic) {
				// 单一来源：书签与 UUID 绑定交由 CodeReadingNoteService 处理
				// 这里只维护列表，避免重复添加
				boolean exists = false;
				for (int i = 0; i < topicLineListModel.size(); i++) {
					if (topicLineListModel.get(i) == _topicLine) {
						exists = true;
						break;
					}
				}
				if (!exists) {
					topicLineListModel.addElement(_topicLine);
				}
			}
		}
		});
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
					try {
						DefaultActionGroup actions = new DefaultActionGroup();
						
						System.out.println("[DEBUG DetailPanel] Creating context menu for TopicLine: " + topicLine.line());
						
						TopicLineRemoveAction removeAction = new TopicLineRemoveAction(project, (v) -> new Pair<>(topic, topicLine));
						actions.add(removeAction);
						System.out.println("[DEBUG DetailPanel] Added Remove action");
						
						ShowBookmarkUidAction bookmarkAction = new ShowBookmarkUidAction(project, (v) -> new Pair<>(topic, topicLine));
						actions.add(bookmarkAction);
						System.out.println("[DEBUG DetailPanel] Added ShowBookmarkUid action");
						
						FixLineRemarkAction fixAction = new FixLineRemarkAction(project, (v) -> new Pair<>(topic, topicLine));
						actions.add(fixAction);
						System.out.println("[DEBUG DetailPanel] Added FixLineRemark action");
						
						TopicLineMoveToGroupAction moveAction = new TopicLineMoveToGroupAction(topicLine);
						actions.add(moveAction);
						System.out.println("[DEBUG DetailPanel] Added MoveTo action");
						
						System.out.println("[DEBUG DetailPanel] Total actions: " + actions.getChildrenCount());
						
						ActionPopupMenu popupMenu = ActionManager.getInstance()
								.createActionPopupMenu(ActionPlaces.POPUP, actions);
						popupMenu.getComponent().show(topicLineList, e.getX(), e.getY());
						
						System.out.println("[DEBUG DetailPanel] Popup shown");
					} catch (Exception ex) {
						System.err.println("[ERROR DetailPanel] Failed to show context menu: " + ex.getMessage());
						ex.printStackTrace();
					}
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
//			ApplicationManager.getApplication().runReadAction(() -> {
//				PsiElement fileOrDir = PsiUtilCore.findFileSystemItem(project, file);
//				if (fileOrDir != null) {
//					setIcon(fileOrDir.getIcon(0));
//				}
//			});
            // Use lightweight icon retrieval that does not force heavy indexing on EDT.
            // We avoid PSI and fall back to file type icons to stay fast.
            if (file != null) {
                Icon icon = file.getFileType().getIcon();
                if (icon != null) {
                    setIcon(icon);
                }
            }


			if (topicLine.isValid()) {
				// 修改显示格式：主显示中文注释，附带类名和行号
				// 原格式：类名 (中文注释)
				// 新格式：中文注释 (类名:行号)
				String note = topicLine.note();
				String pathInfo = topicLine.pathForDisplay() + ":" + (topicLine.line() + 1);
				
				if (note != null && !note.trim().isEmpty()) {
					// 有注释：显示注释作为主要内容，类名作为附加信息
					append(note.substring(0, Math.min(note.length(), 50))); // 主显示：中文注释（限制长度）
					append(" (" + pathInfo + ")", SimpleTextAttributes.GRAY_ATTRIBUTES); // 附带：类名和行号
				} else {
					// 无注释：直接显示类名和行号
					append(pathInfo);
				}
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
	
	// ========== New methods for subgroup support ==========
	
	/**
	 * Set the detail panel to display a subgroup
	 */
    public void setGroup(TopicGroup group) {
		this.topic = group.getParentTopic();
		this.selectedTopicLine = null;
		
		// Update note area with group note
		noteArea.setEnabled(true);
		if (group.note().equals("")) {
			noteArea.setPlaceholder(" Group note input area (Markdown)");
		}
		noteArea.setDocument(EditorFactory.getInstance().createDocument(group.note()));
		noteArea.getDocument().addDocumentListener(new GroupNoteAreaListener(group));
		
		// Update topic line list with group lines
		topicLineListModel.clear();
		for (TopicLine line : group.getLines()) {
			topicLineListModel.addElement(line);
		}
		
		topicLineList.setModel(topicLineListModel);
		topicLineDetailPanel.clear();
	}
	
	/**
	 * Set the detail panel to display ungrouped lines of a topic
	 */
	public void setUngroupedLines(Topic topic) {
		this.topic = topic;
		this.selectedTopicLine = null;
		
		// Update note area to show topic note (since ungrouped lines belong to topic)
		noteArea.setEnabled(true);
		if (topic.note().equals("")) {
			noteArea.setPlaceholder(" Topic note input area (Markdown) - Ungrouped Lines View");
		}
		noteArea.setDocument(EditorFactory.getInstance().createDocument(topic.note()));
		noteArea.getDocument().addDocumentListener(new NoteAreaListener(this));
		
		// Update topic line list with ONLY ungrouped lines
		topicLineListModel.clear();
		for (TopicLine line : topic.getUngroupedLines()) {
			topicLineListModel.addElement(line);
		}
		
		topicLineList.setModel(topicLineListModel);
		topicLineDetailPanel.clear();
	}
	public void setTopicLine(TopicLine topicLine) {
		this.topic = topicLine.topic();
		this.selectedTopicLine = topicLine;
		
		// Update note area with topic line note
		noteArea.setEnabled(true);
		if (topicLine.note().equals("")) {
			noteArea.setPlaceholder(" Topic line note input area (Markdown)");
		}
		noteArea.setDocument(EditorFactory.getInstance().createDocument(topicLine.note()));
		noteArea.getDocument().addDocumentListener(new TopicLineNoteAreaListener(topicLine));
		
		// Show only this line in the list
		topicLineListModel.clear();
		topicLineListModel.addElement(topicLine);
		topicLineList.setModel(topicLineListModel);
		topicLineList.setSelectedIndex(0);
		
		// Show line details
		topicLineDetailPanel.setTopicLine(topicLine);
	}
	
	/**
	 * Document listener for group notes
	 */
	private static class GroupNoteAreaListener implements DocumentListener {
		private TopicGroup group;
		
		private GroupNoteAreaListener(TopicGroup group) {
			this.group = group;
		}
		
		public void documentChanged(com.intellij.openapi.editor.event.DocumentEvent e) {
			Document doc = e.getDocument();
			group.setNote(doc.getText());
		}
	}
	
	/**
	 * Document listener for topic line notes
	 */
	private static class TopicLineNoteAreaListener implements DocumentListener {
		private TopicLine topicLine;
		
		private TopicLineNoteAreaListener(TopicLine topicLine) {
			this.topicLine = topicLine;
		}
		
		public void documentChanged(com.intellij.openapi.editor.event.DocumentEvent e) {
			Document doc = e.getDocument();
			topicLine.setNote(doc.getText());
		}
	}
}

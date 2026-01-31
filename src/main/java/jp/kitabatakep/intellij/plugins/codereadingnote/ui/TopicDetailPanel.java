package jp.kitabatakep.intellij.plugins.codereadingnote.ui;

import com.intellij.icons.AllIcons;
import com.intellij.ide.DataManager;
import org.jetbrains.annotations.NotNull;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.messages.MessageBus;
import com.intellij.ui.RowsDnDSupport;
import com.intellij.util.ui.EditableModel;
import com.intellij.util.ui.UIUtil;
import jp.kitabatakep.intellij.plugins.codereadingnote.*;
import jp.kitabatakep.intellij.plugins.codereadingnote.CodeReadingNoteBundle;
import jp.kitabatakep.intellij.plugins.codereadingnote.actions.FixLineRemarkAction;
import jp.kitabatakep.intellij.plugins.codereadingnote.actions.RepairBookmarksAction;
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
	private TopicGroup currentGroup; // 当前显示的 Group（null 表示 Topic 或 Ungrouped 视图）
	private boolean isUngroupedView = false; // 是否是 Ungrouped 视图
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
		
		// Create panel with toolbar for topicLineList
		JPanel leftPanel = new JPanel(new BorderLayout());
		leftPanel.add(createTopicLineToolbar(), BorderLayout.NORTH);
		leftPanel.add(new JBScrollPane(topicLineList), BorderLayout.CENTER);
		
		topicLinePane.setFirstComponent(leftPanel);
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
				// Bookmark creation is now handled by CodeReadingNoteService.lineAdded()
				// Don't create duplicate bookmarks here!
				// Just update the UI
				topicLineListModel.addElement(_topicLine);
					// Mobile monitoring that does not rely on Panel
					//移动不依赖Panel的监听
//					EditorUtils.addLineCodeRemark(project, _topicLine);
				}
			}
			
		@Override
		public void lineUpdated(Topic _topic, TopicLine _topicLine, int oldLineNum, int newLineNum) {
			if (_topic == topic) {
				// Refresh list to show updated line number
				int index = topicLineListModel.indexOf(_topicLine);
				if (index >= 0) {
					topicLineListModel.set(index, _topicLine);
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
		topicLineList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION); // Changed to support multi-selection
		topicLineList.setCellRenderer(new TopicLineListCellRenderer<>(project));
		
		// 启用拖拽排序
		RowsDnDSupport.install(topicLineList, topicLineListModel);
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
					// Get selected lines for batch operations
					java.util.List<TopicLine> selectedLines = topicLineList.getSelectedValuesList();
					boolean multipleSelected = selectedLines.size() > 1;
					
					DefaultActionGroup actions = new DefaultActionGroup();
					
					if (multipleSelected) {
						// 批量操作
						actions.add(new BatchRemoveAction(selectedLines));
						actions.add(new TopicLineMoveToGroupAction(selectedLines));
					} else {
						// 单个操作
						actions.add(new TopicLineRemoveAction(project, (v) -> new Pair<>(topic, topicLine)));
						actions.add(new TopicLineMoveToGroupAction(topicLine));
					}
					
					actions.addSeparator();
					actions.add(new ShowBookmarkUidAction(project, (v) -> new Pair<>(topic, topicLine)));
					actions.add(new FixLineRemarkAction(project, (v) -> new Pair<>(topic, topicLine)));
					
					// Add new actions for single line
					if (!multipleSelected) {
						actions.addSeparator();
						actions.add(new jp.kitabatakep.intellij.plugins.codereadingnote.actions.EditLineNumberAction(topicLine));
						actions.add(new jp.kitabatakep.intellij.plugins.codereadingnote.actions.RepairSingleLineBookmarkAction(topicLine, topic));
					}
					
					// Add batch operations for multiple selection
					if (multipleSelected) {
						actions.addSeparator();
						actions.add(new jp.kitabatakep.intellij.plugins.codereadingnote.actions.BatchAdjustLineNumbersAction(selectedLines));
					}
					
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
	}

	private JComponent createTopicLineToolbar() {
		JPanel toolbarPanel = new JPanel(new BorderLayout());
		toolbarPanel.setPreferredSize(new Dimension(0, 30)); // 设置最小高度确保可见
		
		// Create action group for toolbar
		DefaultActionGroup actionGroup = new DefaultActionGroup();
		
		// Add "Repair All Bookmarks" action (always enabled)
		actionGroup.add(new RepairBookmarksAction());
		
		// Add "Repair Bookmarks for This Topic" action (only enabled when topic is selected)
		// Use a wrapper action that can dynamically create RepairTopicBookmarksAction based on current topic
		com.intellij.openapi.actionSystem.AnAction repairTopicAction = new com.intellij.openapi.actionSystem.AnAction(
				jp.kitabatakep.intellij.plugins.codereadingnote.CodeReadingNoteBundle.message("action.repair.bookmarks.topic"),
				jp.kitabatakep.intellij.plugins.codereadingnote.CodeReadingNoteBundle.message("action.repair.bookmarks.description"),
				com.intellij.icons.AllIcons.Actions.Refresh
		) {
			@Override
			public void actionPerformed(@org.jetbrains.annotations.NotNull com.intellij.openapi.actionSystem.AnActionEvent e) {
				// Trigger repair for current topic only
				if (topic != null) {
					com.intellij.openapi.actionSystem.AnAction action = 
							new jp.kitabatakep.intellij.plugins.codereadingnote.actions.RepairTopicBookmarksAction(topic);
					ActionUtil.performActionDumbAwareWithCallbacks(action, e);
				}
			}
			
			@Override
			public void update(@org.jetbrains.annotations.NotNull com.intellij.openapi.actionSystem.AnActionEvent e) {
				e.getPresentation().setEnabled(topic != null);
				e.getPresentation().setVisible(true); // 确保按钮始终可见
			}
		};
		actionGroup.add(repairTopicAction);
		
		// Create toolbar
		com.intellij.openapi.actionSystem.ActionToolbar toolbar = 
				com.intellij.openapi.actionSystem.ActionManager.getInstance()
						.createActionToolbar("CodeReadingNoteTopicLineToolbar", actionGroup, true);
		toolbar.setTargetComponent(topicLineList);
		
		toolbarPanel.add(toolbar.getComponent(), BorderLayout.CENTER);
		return toolbarPanel;
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
		this.currentGroup = null; // Topic 视图，不属于任何 Group
		this.isUngroupedView = false;
		selectedTopicLine = null;

		noteArea.setEnabled(true);
		if (topic.note().equals("")) {
			noteArea.setPlaceholder(" Topic note input area (Markdown)");
		}
		noteArea.setDocument(EditorFactory.getInstance().createDocument(topic.note()));
		noteArea.getDocument().addDocumentListener(new NoteAreaListener(this));

		// 按 Group 顺序显示 TopicLine：先按 Group 顺序，再按 Group 内部顺序
		topicLineListModel.clear();
		
		// 1. 先添加各 Group 中的 TopicLine（按 Group 顺序）
		for (TopicGroup group : topic.getGroups()) {
			for (TopicLine line : group.getLines()) {
				topicLineListModel.addElement(line);
			}
		}
		
		// 2. 最后添加 ungrouped 的 TopicLine
		for (TopicLine line : topic.getUngroupedLines()) {
			topicLineListModel.addElement(line);
		}

		topicLineList.setModel(topicLineListModel);
		
		// Topic 视图禁用拖拽排序（因为包含多个 Group 的混合数据）
		topicLineListModel.setDragEnabled(false);
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
			
			// Use simple validity check to avoid SlowOperations on EDT
			boolean isValid = topicLine.isValid();

			// Use static icons to avoid SlowOperations from PSI/file index lookups
			if (isValid) {
				setIcon(com.intellij.icons.AllIcons.FileTypes.Text);
			} else {
				setIcon(com.intellij.icons.AllIcons.General.Warning);
			}

			if (isValid) {
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
		
		private boolean dragEnabled = true;

		public void addRow() {
		}

		public void removeRow(int i) {
		}
		
		public void setDragEnabled(boolean enabled) {
			this.dragEnabled = enabled;
		}

		public boolean canExchangeRows(int oldIndex, int newIndex) {
			return dragEnabled;
		}

		public void exchangeRows(int oldIndex, int newIndex) {
			if (!dragEnabled) return;
			
			TopicLine target = (TopicLine) get(oldIndex);
			remove(oldIndex);
			add(newIndex, (T) target);
			
			// 根据当前视图类型调用正确的排序方法
			if (currentGroup != null) {
				// Group 视图：在 Group 内排序
				currentGroup.changeLineOrder(target, newIndex);
			} else if (isUngroupedView && topic != null) {
				// Ungrouped 视图：在 ungroupedLines 中排序
				// Topic.changeLineOrder 会自动处理 ungrouped lines
				topic.changeLineOrder(target, newIndex);
			}
			// Topic 视图不应该进入这里（dragEnabled = false）
		}
	}
	
	// ========== New methods for subgroup support ==========
	
	/**
	 * Set the detail panel to display a subgroup
	 */
    public void setGroup(TopicGroup group) {
		this.topic = group.getParentTopic();
		this.currentGroup = group; // 记录当前 Group
		this.isUngroupedView = false;
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
		
		// Group 视图启用拖拽排序
		topicLineListModel.setDragEnabled(true);
	}
	
	/**
	 * Set the detail panel to display ungrouped lines of a topic
	 */
	public void setUngroupedLines(Topic topic) {
		this.topic = topic;
		this.currentGroup = null; // Ungrouped 视图，标记为 null 表示未分组
		this.isUngroupedView = true; // 标记为 Ungrouped 视图
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
		
		// Ungrouped 视图启用拖拽排序
		topicLineListModel.setDragEnabled(true);
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
	
	/**
	 * 批量删除 TopicLine 的 Action
	 */
	private static class BatchRemoveAction extends AnAction {
		private final java.util.List<TopicLine> lines;
		
		public BatchRemoveAction(java.util.List<TopicLine> lines) {
			super(CodeReadingNoteBundle.message("action.batch.remove", lines.size()), 
				  CodeReadingNoteBundle.message("action.batch.remove.description"), 
				  AllIcons.General.Remove);
			this.lines = lines;
		}
		
		@Override
		public void actionPerformed(@NotNull AnActionEvent e) {
			for (TopicLine line : lines) {
				line.topic().removeLine(line);
			}
		}
	}
}

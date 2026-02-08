package jp.kitabatakep.intellij.plugins.codereadingnote.actions;

import com.google.common.collect.Lists;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.ex.FocusChangeListener;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.ui.popup.JBPopupListener;
import com.intellij.openapi.ui.popup.PopupChooserBuilder;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.components.JBList;
import com.intellij.util.ui.JBDimension;
import com.intellij.util.ui.UIUtil;
import jp.kitabatakep.intellij.plugins.codereadingnote.CodeReadingNoteService;
import jp.kitabatakep.intellij.plugins.codereadingnote.Topic;
import jp.kitabatakep.intellij.plugins.codereadingnote.TopicGroup;
import jp.kitabatakep.intellij.plugins.codereadingnote.TopicLine;
import jp.kitabatakep.intellij.plugins.codereadingnote.TopicList;
import jp.kitabatakep.intellij.plugins.codereadingnote.remark.BookmarkUtils;
import jp.kitabatakep.intellij.plugins.codereadingnote.ui.MyEditorTextField;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

public class TopicLineAddAction extends CommonAnAction {
    @Override
    public void update(AnActionEvent event) {
        Project project = event.getProject();
        DataContext dataContext = event.getDataContext();

        if (project == null) {
            event.getPresentation().setEnabled(false);
        } else {
            CodeReadingNoteService service = CodeReadingNoteService.getInstance(project);
            event.getPresentation().setEnabled(
                    service.getTopicList().iterator().hasNext() &&
                            (CommonDataKeys.EDITOR.getData(dataContext) != null ||
                                    CommonDataKeys.VIRTUAL_FILE.getData(dataContext) != null));
        }

        event.getPresentation().setText(jp.kitabatakep.intellij.plugins.codereadingnote.CodeReadingNoteBundle.message("action.add.to.topic"));
    }

    /**
     * PopupChooserBuilder
     * @param event
     */
    //    @Override
    public void actionPerformedV1(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        if (project == null) return;

        CodeReadingNoteService service = CodeReadingNoteService.getInstance(project);

        VirtualFile file = event.getData(PlatformDataKeys.VIRTUAL_FILE);
        Editor editor = event.getData(PlatformDataKeys.EDITOR);
        int line = editor.getCaretModel().getLogicalPosition().line;

        TopicList topicList = service.getTopicList();
        Iterator<Topic> iterator = topicList.iterator();
        ArrayList<Topic> topics = new ArrayList<>();
        while (iterator.hasNext()) {
            Topic topic = iterator.next();
            topics.add(topic);
        }

        MyEditorTextField noteInputField = new MyEditorTextField(project, FileTypeManager.getInstance().getStdFileType("Markdown"));

        PopupChooserBuilder<Topic> builder = new PopupChooserBuilder<Topic>(new JList<>(topics.toArray(new Topic[0])));
        builder
                .setTitle(jp.kitabatakep.intellij.plugins.codereadingnote.CodeReadingNoteBundle.message("dialog.select.topic.with.note"))
                .setRenderer(new MyCellRenderer<Topic>())
                .setResizable(true)
                .setItemChosenCallback((topic) -> {
                    if (topic != null) {
                        topic.addLine(TopicLine.createByAction(project, topic, file, line, noteInputField.getText()));
                    }
                })
                .setMovable(true);
//            .createPopup();


        noteInputField.setOneLineMode(false);
        noteInputField.setPlaceholder(jp.kitabatakep.intellij.plugins.codereadingnote.CodeReadingNoteBundle.message("panel.note.input.placeholder"));
        noteInputField.setPreferredSize(
                new JBDimension(
                        240,
                        (int) builder.getChooserComponent().getPreferredSize().getHeight()
                )
        );
        builder.setEastComponent(noteInputField);
//        MyEditorTextField noteInputField2 = new MyEditorTextField(project, FileTypeManager.getInstance().getStdFileType("Markdown"));
//        noteInputField2.setOneLineMode(false);
//        noteInputField2.setPlaceholder("[Optional] note input area");
//
//        JOptionPane.showConfirmDialog(null, noteInputField2, "Test EditorTextField", JOptionPane.OK_CANCEL_OPTION);
//问题可能与 PopupChooserBuilder 内部的焦点管理、弹出窗口的渲染顺序或输入法（IME）兼容性有关。
        builder.createPopup().showInBestPositionFor(editor);

    }

    //todo 弹框需优化但是 PopupChooserBuilder 内部的焦点管理、弹出窗口的渲染顺序或输入法（IME）兼容性有关。 中文输入接收不到字

    /**
     *  JPopupMenu
     * @param event
     */
    public void actionPerformedV2(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        if (project == null) return;

        CodeReadingNoteService service = CodeReadingNoteService.getInstance(project);

        VirtualFile file = event.getData(PlatformDataKeys.VIRTUAL_FILE);
        Editor editor = event.getData(PlatformDataKeys.EDITOR);
        CaretModel caretModel = editor.getCaretModel();
        int line = caretModel.getLogicalPosition().line;
        TopicList topicList = service.getTopicList();
        Iterator<Topic> iterator = topicList.iterator();
        ArrayList<Topic> topics = new ArrayList<>();
        while (iterator.hasNext()) {
            Topic topic = iterator.next();
            topics.add(topic);
        }

        // 创建一个 JPanel 用于包装弹出菜单
        JPanel popupPanel = new JPanel(new BorderLayout());  // 使用 BorderLayout 布局
        JLabel titleLabel = new JLabel(jp.kitabatakep.intellij.plugins.codereadingnote.CodeReadingNoteBundle.message("dialog.select.topic.with.note"));
        titleLabel.setFont(new Font("Arial", Font.BOLD, 14));  // 设置标题的字体
        titleLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));  // 设置边距
        popupPanel.add(titleLabel, BorderLayout.NORTH);
        // 创建 JList 显示 Topic 列表
        JList<Topic> topicListComponent = new JList<>(topics.toArray(new Topic[0]));
        topicListComponent.setCellRenderer(new MyCellRenderer<Topic>());
        JScrollPane scrollPane = new JScrollPane(topicListComponent);
        popupPanel.add(scrollPane, BorderLayout.CENTER);  // 将列表添加到中间部分

        // 创建输入框
        MyEditorTextField noteInputField = new MyEditorTextField(project, FileTypeManager.getInstance().getStdFileType("Markdown"));
        noteInputField.setOneLineMode(false);
        noteInputField.setPreferredSize(new Dimension(240, 50));
        noteInputField.setPlaceholder(jp.kitabatakep.intellij.plugins.codereadingnote.CodeReadingNoteBundle.message("panel.note.input.placeholder"));

        // 将输入框添加到右侧（East）
        popupPanel.add(noteInputField, BorderLayout.EAST);

        // 创建 JPopupMenu 来显示这个 JPanel
        JPopupMenu popupMenu = new JPopupMenu();
        popupMenu.add(popupPanel);  // 将包含列表和输入框的 JPanel 添加到 JPopupMenu 中

        // 监听鼠标位置，显示菜单在鼠标光标附近
//        editor.getContentComponent().addMouseListener(new MouseAdapter() {
//            @Override
//            public void mousePressed(MouseEvent e) {
//                // 获取鼠标位置
//                Point mouseLocation = e.getPoint();
//                // 将 JPopupMenu 显示在鼠标位置附近
//                popupMenu.show(editor.getContentComponent(), mouseLocation.x, mouseLocation.y);
//            }
//        });

        // 处理选择事件
        topicListComponent.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1) {
                    Topic selectedTopic = topicListComponent.getSelectedValue();
                    if (selectedTopic != null) {
                        selectedTopic.addLine(TopicLine.createByAction(project, selectedTopic, file, line, noteInputField.getText()));
                        popupMenu.setVisible(false);  // 关闭菜单
                    }
                }
            }
        });
        // 获取光标位置：行号和列号
        int column = caretModel.getLogicalPosition().column;  // 获取列号（从0开始）
        FoldingModel foldingModel = editor.getFoldingModel();
        foldingModel.runBatchFoldingOperation(() -> {
            // 展开所有折叠的区域
            FoldRegion[] allFoldRegions = foldingModel.getAllFoldRegions();
            Arrays.asList(allFoldRegions)
                    .forEach(foldRegion -> {

                        if (!foldRegion.isExpanded()) {
                            foldRegion.setExpanded(true);
//                    LogicalPosition startPos = editor.offsetToLogicalPosition(foldRegion.getStartOffset());
//                    LogicalPosition endPos = editor.offsetToLogicalPosition(foldRegion.getEndOffset());
//                    if (endPos.line <= line) {
//                        addline[0] += endPos.line - startPos.line;
//                    }

                        }
                    });
        });
        Point point = editor.visualPositionToXY(new VisualPosition(line, column));
        int x = point.x;
        int y = point.y;
//        MouseEvent inputEvent = (MouseEvent) event.getInputEvent();
//        int x2 = inputEvent.getLocationOnScreen().x;
//        int y2 = inputEvent.getLocationOnScreen().y;

        popupMenu.show(editor.getContentComponent(), x, y);

//        popupMenu.show(editor.getContentComponent(),location.x, location.y);

    }

    /**
     * JDialog and JWindow(待定 undetermined)
     * @param event
     */
    public void actionPerformed(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        if (project == null) return;

        CodeReadingNoteService service = CodeReadingNoteService.getInstance(project);

        VirtualFile file = event.getData(PlatformDataKeys.VIRTUAL_FILE);
        Editor editor = event.getData(PlatformDataKeys.EDITOR);
        int line = editor.getCaretModel().getLogicalPosition().line;

        TopicList topicList = service.getTopicList();
        Iterator<Topic> iterator = topicList.iterator();
        ArrayList<Topic> topics = new ArrayList<>();
        while (iterator.hasNext()) {
            Topic topic = iterator.next();
            topics.add(topic);
        }

        // Create the JDialog with enhanced layout for group selection
        JDialog dialog = new JDialog((Frame) null, jp.kitabatakep.intellij.plugins.codereadingnote.CodeReadingNoteBundle.message("dialog.add.to.topic.title"), true);
        dialog.setLayout(new BorderLayout());

        // Create main panel with three columns: Topic, Group, Note
        JPanel mainPanel = new JPanel(new BorderLayout());
        
        // Left panel: Topic selection
        JPanel topicPanel = new JPanel(new BorderLayout());
        topicPanel.setBorder(BorderFactory.createTitledBorder(jp.kitabatakep.intellij.plugins.codereadingnote.CodeReadingNoteBundle.message("panel.select.topic")));
        
        JBList<Topic> topicJList = new JBList<>(topics.toArray(new Topic[0]));
        topicJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        topicJList.setCellRenderer(new MyCellRenderer<>());
        JScrollPane topicScrollPane = new JScrollPane(topicJList);
        topicScrollPane.setPreferredSize(new JBDimension(200, 200));
        topicPanel.add(topicScrollPane, BorderLayout.CENTER);
        
        // Middle panel: Group selection
        JPanel groupPanel = new JPanel(new BorderLayout());
        groupPanel.setBorder(BorderFactory.createTitledBorder(jp.kitabatakep.intellij.plugins.codereadingnote.CodeReadingNoteBundle.message("panel.select.group")));
        
        DefaultListModel<Object> groupListModel = new DefaultListModel<>();
        JBList<Object> groupJList = new JBList<>(groupListModel);
        groupJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        groupJList.setCellRenderer(new GroupCellRenderer());
        JScrollPane groupScrollPane = new JScrollPane(groupJList);
        groupScrollPane.setPreferredSize(new JBDimension(200, 200));
        groupPanel.add(groupScrollPane, BorderLayout.CENTER);
        
        // Right panel: Note input
        JPanel notePanel = new JPanel(new BorderLayout());
        notePanel.setBorder(BorderFactory.createTitledBorder(jp.kitabatakep.intellij.plugins.codereadingnote.CodeReadingNoteBundle.message("panel.note.optional")));
        
        MyEditorTextField noteInputField = new MyEditorTextField(project, FileTypeManager.getInstance().getStdFileType("Markdown"));
        noteInputField.setOneLineMode(false);
        noteInputField.setPlaceholder(jp.kitabatakep.intellij.plugins.codereadingnote.CodeReadingNoteBundle.message("panel.note.input.placeholder"));
        noteInputField.setPreferredSize(new JBDimension(250, 200));
        notePanel.add(noteInputField, BorderLayout.CENTER);
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton addButton = new JButton(jp.kitabatakep.intellij.plugins.codereadingnote.CodeReadingNoteBundle.message("button.add.to.topic"));
        JButton cancelButton = new JButton(jp.kitabatakep.intellij.plugins.codereadingnote.CodeReadingNoteBundle.message("button.cancel"));
        buttonPanel.add(addButton);
        buttonPanel.add(cancelButton);
        
        // Layout main panel
        JPanel topGroupPanel = new JPanel(new BorderLayout());
        topGroupPanel.add(topicPanel, BorderLayout.WEST);
        topGroupPanel.add(groupPanel, BorderLayout.CENTER);
        
        mainPanel.add(topGroupPanel, BorderLayout.WEST);
        mainPanel.add(notePanel, BorderLayout.CENTER);
        
        dialog.add(mainPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        // Topic selection listener - updates group list
        topicJList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                Topic selectedTopic = topicJList.getSelectedValue();
                updateGroupList(selectedTopic, groupListModel);
            }
        });
        
        // Add button listener
        addButton.addActionListener(e -> {
            try {
                Topic selectedTopic = topicJList.getSelectedValue();
                if (selectedTopic != null) {
                    // Check if a bookmark or TopicLine already exists at this line
                    // Two-level check: native bookmark + TopicLine data
                    String existingUuid = BookmarkUtils.findExistingBookmarkUuidAtLine(project, file, line);
                    boolean hasDuplicateTopicLine = BookmarkUtils.hasTopicLineAtSameFileLine(project, file, line);
                    if (existingUuid != null || hasDuplicateTopicLine) {
                        // Show warning dialog
                        int result = JOptionPane.showConfirmDialog(dialog,
                            jp.kitabatakep.intellij.plugins.codereadingnote.CodeReadingNoteBundle.message("message.bookmark.exists.at.line"),
                            jp.kitabatakep.intellij.plugins.codereadingnote.CodeReadingNoteBundle.message("message.warning.title"),
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.WARNING_MESSAGE);
                        if (result != JOptionPane.YES_OPTION) {
                            return; // User cancelled
                        }
                    }
                    
                    Object selectedGroup = groupJList.getSelectedValue();
                    String noteText = noteInputField.getText();
                    
                    TopicLine newLine = TopicLine.createByAction(project, selectedTopic, file, line, noteText);
                    
                    if (selectedGroup instanceof TopicGroup) {
                        // Add to selected group
                        selectedTopic.addLineToGroup(newLine, ((TopicGroup) selectedGroup).name());
                    } else if (selectedGroup instanceof String) {
                        String groupOption = (String) selectedGroup;
                        if (groupOption.startsWith("+")) {
                            // Create new group
                            String newGroupName = JOptionPane.showInputDialog(dialog, 
                                jp.kitabatakep.intellij.plugins.codereadingnote.CodeReadingNoteBundle.message("dialog.create.new.group.message"), 
                                jp.kitabatakep.intellij.plugins.codereadingnote.CodeReadingNoteBundle.message("dialog.create.new.group.title"), 
                                JOptionPane.QUESTION_MESSAGE);
                            if (newGroupName != null && !newGroupName.trim().isEmpty()) {
                                selectedTopic.addLineToGroup(newLine, newGroupName.trim());
                            } else {
                                return; // Cancel if no group name provided
                            }
                        } else {
                            // "No Group" option - add directly to topic
                            selectedTopic.addLine(newLine);
                        }
                    } else {
                        // Default: add to topic directly (ungrouped)
                        selectedTopic.addLine(newLine);
                    }
                    
                    // 确保在EDT线程中关闭对话框
                    SwingUtilities.invokeLater(() -> {
                        dialog.setVisible(false);
                        dialog.dispose();
                    });
                }
            } catch (Exception ex) {
                // 记录异常但不中断用户操作
                Logger.getInstance(TopicLineAddAction.class).warn("Error adding topic line", ex);
                JOptionPane.showMessageDialog(dialog, 
                    jp.kitabatakep.intellij.plugins.codereadingnote.CodeReadingNoteBundle.message("message.error.adding.topic.line", ex.getMessage()), 
                    jp.kitabatakep.intellij.plugins.codereadingnote.CodeReadingNoteBundle.message("message.error.title"), 
                    JOptionPane.ERROR_MESSAGE);
            }
        });
        
        // Cancel button listener
        cancelButton.addActionListener(e -> {
            dialog.setVisible(false);
            dialog.dispose();
        });

        // Show dialog
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
                SwingUtilities.invokeLater(() -> {
                    dialog.pack();
                    dialog.setLocationRelativeTo(editor.getComponent());
                    dialog.setVisible(true);
                });
        });
    }
    
    private void updateGroupList(Topic topic, DefaultListModel<Object> groupListModel) {
        groupListModel.clear();
        
        if (topic != null) {
            // Add "No Group" option
            groupListModel.addElement(jp.kitabatakep.intellij.plugins.codereadingnote.CodeReadingNoteBundle.message("panel.no.group"));
            
            // Add existing groups
            for (TopicGroup group : topic.getGroups()) {
                groupListModel.addElement(group);
            }
            
            // Add "Create New Group" option
            groupListModel.addElement(jp.kitabatakep.intellij.plugins.codereadingnote.CodeReadingNoteBundle.message("panel.create.new.group"));
        }
    }
    
    private static class GroupCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            
            if (value instanceof TopicGroup) {
                TopicGroup group = (TopicGroup) value;
                setText(group.name() + " (" + jp.kitabatakep.intellij.plugins.codereadingnote.CodeReadingNoteBundle.message("renderer.lines.count", group.getLineCount()) + ")");
                setIcon(AllIcons.Nodes.Folder);
            } else if (value instanceof String) {
                String text = (String) value;
                setText(text);
                if (text.startsWith("+") || text.contains(jp.kitabatakep.intellij.plugins.codereadingnote.CodeReadingNoteBundle.message("panel.create.new.group").substring(2))) {
                    setIcon(AllIcons.General.Add);
                } else {
                    setIcon(AllIcons.Actions.Unselectall);
                }
            }
            
            return this;
        }
    }



    private static class MyCellRenderer<T> extends SimpleColoredComponent implements ListCellRenderer<T> {
        private MyCellRenderer() {
            setOpaque(true);
        }

        public Component getListCellRendererComponent(
                JList list,
                Object value,
                int index,
                boolean isSelected,
                boolean cellHasFocus) {
            clear();
            Topic topic = (Topic) value;
            append(topic.name());

            append(
                    " (" + new SimpleDateFormat("yyyy/MM/dd HH:mm").format(topic.updatedAt()) + ")",
                    SimpleTextAttributes.GRAY_ATTRIBUTES
            );

            setForeground(UIUtil.getListSelectionForeground(isSelected));
            setBackground(UIUtil.getListSelectionBackground(isSelected));
            return this;
        }
    }
}



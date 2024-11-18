package jp.kitabatakep.intellij.plugins.codereadingnote.actions;

import com.google.common.collect.Lists;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
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
import jp.kitabatakep.intellij.plugins.codereadingnote.TopicLine;
import jp.kitabatakep.intellij.plugins.codereadingnote.TopicList;
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

        event.getPresentation().setText("Add to Topic");
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
                .setTitle("Select Topic With Note")
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
        noteInputField.setPlaceholder("[Optional] note input area");
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
        JLabel titleLabel = new JLabel("Select Topic With Note");
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
        noteInputField.setPlaceholder("[Optional] note input area");

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

        // Create the JDialog
        JDialog dialog = new JDialog((Frame) null, "Select Topic With Note", true);
        dialog.setLayout(new BorderLayout());

        // Create a JList for topics
        JBList<Topic> topicJList = new JBList<>(topics.toArray(new Topic[0]));
        topicJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // 设置自定义渲染器
        topicJList.setCellRenderer(new MyCellRenderer<>());

        JScrollPane listScrollPane = new JScrollPane(topicJList);


        // Create the note input field (MyEditorTextField)
        MyEditorTextField noteInputField = new MyEditorTextField(project, FileTypeManager.getInstance().getStdFileType("Markdown"));
        noteInputField.setOneLineMode(false);
        noteInputField.setPlaceholder("[Optional] note input area");

        // Set preferred size for note input field
        int listWidth = topicJList.getPreferredSize().width;
        noteInputField.setPreferredSize(new JBDimension(listWidth, 80));
//        noteInputField.setPreferredSize(new JBDimension(240, 80));

        // Add components to the dialog
        dialog.add(listScrollPane, BorderLayout.WEST);
        dialog.add(noteInputField, BorderLayout.EAST);

        // Add a listener for item selection from JList
        topicJList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                Topic selectedTopic = topicJList.getSelectedValue();
                if (selectedTopic != null) {
                    dialog.setVisible(false); // Close the dialog on selection
                    selectedTopic.addLine(TopicLine.createByAction(project, selectedTopic, file, line, noteInputField.getText()));
                }
            }
        });

        // Set the dialog size and location
//        dialog.setSize(new Dimension(300, 300));
//        dialog.pack();
//        dialog.setLocationRelativeTo(editor.getComponent()); // Show dialog relative to the editor
//        dialog.setVisible(true);

        ApplicationManager.getApplication().executeOnPooledThread(() -> {

                // Any suspend function or async work can be done here safely
                // For example:
                // someAsyncWork()
                SwingUtilities.invokeLater(() -> {
                    // Updates UI in EDT thread
                    dialog.pack();
                    dialog.setLocationRelativeTo(editor.getComponent());
                    dialog.setVisible(true);
                });
        });
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

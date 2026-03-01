package jp.kitabatakep.intellij.plugins.codereadingnote.remark;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import jp.kitabatakep.intellij.plugins.codereadingnote.AppConstants;
import jp.kitabatakep.intellij.plugins.codereadingnote.CodeReadingNoteBundle;
import jp.kitabatakep.intellij.plugins.codereadingnote.TopicLine;
import jp.kitabatakep.intellij.plugins.codereadingnote.ui.ManagementPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/**
 * Reusable popup for editing/viewing a TopicLine note.
 * Used by both the gutter icon click and Alt+G keyboard shortcut.
 */
public final class NotePopupHelper {

    private NotePopupHelper() {}

    public static void show(Editor editor, Project project, TopicLine topicLine) {
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 12, 8, 12));

        JLabel titleLabel = new JLabel(CodeReadingNoteBundle.message("gutter.popup.title"));
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
        panel.add(titleLabel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new BorderLayout(0, 4));

        JLabel noteLabel = new JLabel(CodeReadingNoteBundle.message("gutter.popup.note.label"));
        centerPanel.add(noteLabel, BorderLayout.NORTH);

        JTextArea noteField = new JTextArea(topicLine.note(), 3, 30);
        noteField.setLineWrap(true);
        noteField.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(noteField);
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        JLabel linkLabel = new JLabel(
                CodeReadingNoteBundle.message("gutter.popup.link.label") + " "
                        + topicLine.pathForDisplay() + ":" + (topicLine.line() + 1));
        linkLabel.setFont(linkLabel.getFont().deriveFont(Font.PLAIN, 11f));
        linkLabel.setForeground(Color.GRAY);
        centerPanel.add(linkLabel, BorderLayout.SOUTH);

        panel.add(centerPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));

        JButton locateBtn = new JButton(CodeReadingNoteBundle.message("gutter.popup.locate") + " (Alt+G)");
        locateBtn.setIcon(AllIcons.General.Locate);

        JButton reverseLocateBtn = new JButton(CodeReadingNoteBundle.message("gutter.popup.reverse.locate"));
        reverseLocateBtn.setIcon(AllIcons.Nodes.PpLib);

        JButton saveBtn = new JButton(CodeReadingNoteBundle.message("gutter.popup.save") + " Enter");

        JButton deleteBtn = new JButton(CodeReadingNoteBundle.message("gutter.popup.delete") + " Alt+Delete");
        deleteBtn.setIcon(AllIcons.General.Remove);

        buttonPanel.add(locateBtn);
        buttonPanel.add(reverseLocateBtn);
        buttonPanel.add(saveBtn);
        buttonPanel.add(deleteBtn);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        JBPopup popup = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(panel, noteField)
                .setTitle(CodeReadingNoteBundle.message("gutter.popup.title"))
                .setFocusable(true)
                .setRequestFocus(true)
                .setMovable(true)
                .setResizable(true)
                .createPopup();

        locateBtn.addActionListener(ev -> {
            popup.cancel();
            topicLine.navigate(true);
        });

        reverseLocateBtn.addActionListener(ev -> {
            popup.cancel();
            reverseLocate(project, topicLine);
        });

        saveBtn.addActionListener(ev -> {
            topicLine.setNote(noteField.getText());
            popup.cancel();
        });

        deleteBtn.addActionListener(ev -> {
            popup.cancel();
            topicLine.topic().removeLine(topicLine);
        });

        InputMap inputMap = panel.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        ActionMap actionMap = panel.getActionMap();

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_G, KeyEvent.ALT_DOWN_MASK), "locate");
        actionMap.put("locate", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                locateBtn.doClick();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "save");
        actionMap.put("save", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveBtn.doClick();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, KeyEvent.ALT_DOWN_MASK), "delete");
        actionMap.put("delete", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                deleteBtn.doClick();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancel");
        actionMap.put("cancel", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                popup.cancel();
            }
        });

        popup.showInBestPositionFor(editor);
    }

    /**
     * Open the tool window and select the given TopicLine in the tree.
     */
    public static void reverseLocate(Project project, TopicLine topicLine) {
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(AppConstants.TOOL_WINDOW_ID);
        if (toolWindow == null) return;

        toolWindow.show(() -> {
            javax.swing.SwingUtilities.invokeLater(() -> {
                Content content = toolWindow.getContentManager().getContent(0);
                if (content != null && content.getComponent() instanceof ManagementPanel) {
                    ((ManagementPanel) content.getComponent()).reverseLocate(topicLine);
                }
            });
        });
    }
}

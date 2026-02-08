package jp.kitabatakep.intellij.plugins.codereadingnote.ui.fix;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.CheckBoxList;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import jp.kitabatakep.intellij.plugins.codereadingnote.CodeReadingNoteBundle;
import jp.kitabatakep.intellij.plugins.codereadingnote.TopicLine;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Dialog for cleaning up error TopicLines (Bookmark Missing / File Not Found).
 * Supports checkbox selection and navigate button.
 */
public class CleanUpDialog extends DialogWrapper {

    private final Project project;
    private final List<LineFixResult> errorResults;
    private final CheckBoxList<LineFixResult> checkBoxList;
    private final JBLabel selectionLabel;
    private final JPanel detailPanel;

    public CleanUpDialog(Project project, List<LineFixResult> errorResults) {
        super(project);
        this.project = project;
        this.errorResults = new ArrayList<>(errorResults);
        this.checkBoxList = new CheckBoxList<>();
        this.selectionLabel = new JBLabel();
        this.detailPanel = new JPanel(new GridBagLayout());

        setTitle(CodeReadingNoteBundle.message("dialog.cleanup.title"));
        init();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(JBUI.Borders.empty(10));
        panel.setPreferredSize(new Dimension(750, 450));

        // Description
        JPanel descPanel = new JPanel(new BorderLayout());
        JBLabel descLabel = new JBLabel(CodeReadingNoteBundle.message("dialog.cleanup.description"));
        descLabel.setBorder(JBUI.Borders.emptyBottom(5));
        descPanel.add(descLabel, BorderLayout.NORTH);

        // Statistics summary
        int bookmarkMissing = 0;
        int fileNotFound = 0;
        for (LineFixResult r : errorResults) {
            if (r.getStatus() == LineFixResult.FixStatus.BOOKMARK_MISSING) bookmarkMissing++;
            else if (r.getStatus() == LineFixResult.FixStatus.FILE_NOT_FOUND) fileNotFound++;
        }
        StringBuilder statsText = new StringBuilder();
        if (bookmarkMissing > 0) {
            statsText.append("❌ Bookmark Missing: ").append(bookmarkMissing);
        }
        if (fileNotFound > 0) {
            if (statsText.length() > 0) statsText.append("    ");
            statsText.append("🚫 File Not Found: ").append(fileNotFound);
        }
        JBLabel statsLabel = new JBLabel(statsText.toString());
        statsLabel.setForeground(Color.GRAY);
        statsLabel.setBorder(JBUI.Borders.emptyBottom(5));
        descPanel.add(statsLabel, BorderLayout.SOUTH);

        panel.add(descPanel, BorderLayout.NORTH);

        // Main content: left list + right detail
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0.6);

        // Left: Checkbox list
        JPanel listPanel = new JPanel(new BorderLayout());
        listPanel.setBorder(BorderFactory.createTitledBorder(
                CodeReadingNoteBundle.message("dialog.cleanup.select.items")));

        // Populate checkbox list with rich display text
        for (LineFixResult result : errorResults) {
            checkBoxList.addItem(result, buildDisplayText(result), true);
        }

        // Selection listener - update detail panel
        checkBoxList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int index = checkBoxList.getSelectedIndex();
                if (index >= 0) {
                    LineFixResult result = checkBoxList.getItemAt(index);
                    if (result != null) {
                        updateDetailPanel(result);
                    }
                }
            }
        });

        JBScrollPane scrollPane = new JBScrollPane(checkBoxList);
        listPanel.add(scrollPane, BorderLayout.CENTER);

        // Selection count and Select All / Deselect All buttons
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBorder(JBUI.Borders.emptyTop(5));

        JPanel buttonBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        JButton selectAllBtn = new JButton(CodeReadingNoteBundle.message("dialog.cleanup.select.all"));
        JButton deselectAllBtn = new JButton(CodeReadingNoteBundle.message("dialog.cleanup.deselect.all"));

        selectAllBtn.addActionListener(e -> {
            for (LineFixResult result : errorResults) {
                checkBoxList.setItemSelected(result, true);
            }
            updateSelectionLabel();
        });
        deselectAllBtn.addActionListener(e -> {
            for (LineFixResult result : errorResults) {
                checkBoxList.setItemSelected(result, false);
            }
            updateSelectionLabel();
        });

        buttonBar.add(selectAllBtn);
        buttonBar.add(deselectAllBtn);
        bottomPanel.add(buttonBar, BorderLayout.WEST);

        updateSelectionLabel();
        selectionLabel.setForeground(Color.GRAY);
        bottomPanel.add(selectionLabel, BorderLayout.EAST);

        listPanel.add(bottomPanel, BorderLayout.SOUTH);

        // Right: Detail panel
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBorder(BorderFactory.createTitledBorder(
                CodeReadingNoteBundle.message("dialog.cleanup.detail")));

        detailPanel.setBorder(JBUI.Borders.empty(10));
        rightPanel.add(new JBScrollPane(detailPanel), BorderLayout.CENTER);

        splitPane.setLeftComponent(listPanel);
        splitPane.setRightComponent(rightPanel);

        panel.add(splitPane, BorderLayout.CENTER);

        // Select first item to show detail
        if (!errorResults.isEmpty()) {
            checkBoxList.setSelectedIndex(0);
            updateDetailPanel(errorResults.get(0));
        }

        return panel;
    }

    /**
     * Build rich display text showing Topic > Group > File:Line (Status)
     */
    private String buildDisplayText(LineFixResult result) {
        TopicLine topicLine = result.getTopicLine();
        StringBuilder sb = new StringBuilder();

        // Topic name
        if (topicLine.topic() != null) {
            sb.append("[").append(topicLine.topic().name()).append("]");
        }

        // Group name
        if (topicLine.hasGroup()) {
            sb.append(" > ").append(topicLine.getGroupName());
        }

        sb.append("  ");

        // File:Line
        sb.append(result.getFileName()).append(":").append(result.getOldLine());

        // Status
        if (result.getStatus() == LineFixResult.FixStatus.BOOKMARK_MISSING) {
            sb.append("  (Bookmark Missing)");
        } else if (result.getStatus() == LineFixResult.FixStatus.FILE_NOT_FOUND) {
            sb.append("  (File Not Found)");
        }

        return sb.toString();
    }

    /**
     * Update detail panel with selected item's info
     */
    private void updateDetailPanel(LineFixResult result) {
        detailPanel.removeAll();
        TopicLine topicLine = result.getTopicLine();

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = JBUI.insets(3, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        int row = 0;

        // Status
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        detailPanel.add(new JBLabel("Status:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        JBLabel statusLabel = new JBLabel(result.getStatus() == LineFixResult.FixStatus.BOOKMARK_MISSING
                ? "❌ Bookmark Missing" : "🚫 File Not Found");
        statusLabel.setForeground(result.getStatus() == LineFixResult.FixStatus.BOOKMARK_MISSING
                ? Color.RED : Color.GRAY);
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.BOLD));
        detailPanel.add(statusLabel, gbc);
        row++;

        // Topic
        if (topicLine.topic() != null) {
            gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
            detailPanel.add(new JBLabel("Topic:"), gbc);
            gbc.gridx = 1; gbc.weightx = 1;
            JBLabel topicLabel = new JBLabel(topicLine.topic().name());
            topicLabel.setFont(topicLabel.getFont().deriveFont(Font.BOLD));
            detailPanel.add(topicLabel, gbc);
            row++;
        }

        // Group
        if (topicLine.hasGroup()) {
            gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
            detailPanel.add(new JBLabel("Group:"), gbc);
            gbc.gridx = 1; gbc.weightx = 1;
            detailPanel.add(new JBLabel(topicLine.getGroupName()), gbc);
            row++;
        }

        // File
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        detailPanel.add(new JBLabel("File:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        detailPanel.add(new JBLabel(result.getFileName()), gbc);
        row++;

        // Path
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        detailPanel.add(new JBLabel("Path:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        JBLabel pathLabel = new JBLabel(result.getFilePath());
        pathLabel.setForeground(Color.GRAY);
        detailPanel.add(pathLabel, gbc);
        row++;

        // Line
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        detailPanel.add(new JBLabel("Line:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        detailPanel.add(new JBLabel(String.valueOf(result.getOldLine())), gbc);
        row++;

        // Note
        String note = result.getNote();
        if (note != null && !note.isEmpty()) {
            gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
            gbc.anchor = GridBagConstraints.NORTHWEST;
            detailPanel.add(new JBLabel("Note:"), gbc);
            gbc.gridx = 1; gbc.weightx = 1;
            JTextArea noteArea = new JTextArea(note.length() > 200 ? note.substring(0, 200) + "..." : note);
            noteArea.setEditable(false);
            noteArea.setLineWrap(true);
            noteArea.setWrapStyleWord(true);
            noteArea.setBackground(detailPanel.getBackground());
            noteArea.setBorder(null);
            noteArea.setRows(3);
            detailPanel.add(noteArea, gbc);
            row++;
        }

        // Navigate button - only for Bookmark Missing (file still exists)
        if (result.getStatus() == LineFixResult.FixStatus.BOOKMARK_MISSING
                && topicLine.isValid() && topicLine.canNavigate()) {
            gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
            gbc.anchor = GridBagConstraints.CENTER;
            gbc.fill = GridBagConstraints.NONE;
            gbc.insets = JBUI.insets(10, 0, 0, 0);
            JButton navigateBtn = new JButton(CodeReadingNoteBundle.message("dialog.cleanup.navigate"));
            navigateBtn.addActionListener(e -> topicLine.navigate(true));
            detailPanel.add(navigateBtn, gbc);
            row++;
        }

        // Filler to push everything up
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        gbc.weighty = 1; gbc.fill = GridBagConstraints.BOTH;
        detailPanel.add(new JPanel(), gbc);

        detailPanel.revalidate();
        detailPanel.repaint();
    }

    /**
     * Update the selection count label
     */
    private void updateSelectionLabel() {
        int selected = getSelectedResults().size();
        int total = errorResults.size();
        selectionLabel.setText(CodeReadingNoteBundle.message("dialog.cleanup.selected.count", selected, total));
    }

    /**
     * Get the list of selected (checked) results to clean up
     */
    public List<LineFixResult> getSelectedResults() {
        List<LineFixResult> selected = new ArrayList<>();
        for (LineFixResult result : errorResults) {
            if (checkBoxList.isItemSelected(result)) {
                selected.add(result);
            }
        }
        return selected;
    }

    @Override
    protected Action @Nullable [] createActions() {
        return new Action[]{
                getCancelAction(),
                new DialogWrapperAction(CodeReadingNoteBundle.message("dialog.cleanup.confirm")) {
                    @Override
                    protected void doAction(java.awt.event.ActionEvent e) {
                        doOKAction();
                    }
                }
        };
    }
}

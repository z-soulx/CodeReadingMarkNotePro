package jp.kitabatakep.intellij.plugins.codereadingnote.ui;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.util.ui.JBUI;
import jp.kitabatakep.intellij.plugins.codereadingnote.CodeReadingNoteBundle;
import jp.kitabatakep.intellij.plugins.codereadingnote.sync.FilePushReport;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Map;

/**
 * Dialog showing a push report with summary header, collapsible detail sections,
 * and an optional "Force Push All" button for the user to trigger a full re-push.
 */
public class PushReportDialog extends DialogWrapper {

    public static final int FORCE_PUSH_EXIT_CODE = NEXT_USER_EXIT_CODE;

    private final FilePushReport report;
    private final boolean success;
    private final boolean noChanges;

    /**
     * @param noChanges true when the push was skipped because the hash was unchanged
     */
    public PushReportDialog(@Nullable Project project, @NotNull FilePushReport report,
                            boolean success, boolean noChanges) {
        super(project, false);
        this.report = report;
        this.success = success;
        this.noChanges = noChanges;
        setTitle(success
                ? CodeReadingNoteBundle.message("aiconfig.sync.push.success.title")
                : CodeReadingNoteBundle.message("aiconfig.sync.push.failed.title"));
        setOKButtonText(CodeReadingNoteBundle.message("button.ok"));
        init();
    }

    @Override
    protected Action @NotNull [] createActions() {
        return new Action[]{
                buildForcePushAction(),
                getOKAction()
        };
    }

    @NotNull
    private Action buildForcePushAction() {
        return new DialogWrapperAction(CodeReadingNoteBundle.message("aiconfig.sync.push.force.yes")) {
            @Override
            protected void doAction(java.awt.event.ActionEvent e) {
                close(FORCE_PUSH_EXIT_CODE);
            }
        };
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        JPanel root = new JPanel(new BorderLayout());
        root.setPreferredSize(new Dimension(520, 400));

        JPanel summaryPanel = createSummaryPanel();
        root.add(summaryPanel, BorderLayout.NORTH);

        JPanel detailsPanel = new JPanel();
        detailsPanel.setLayout(new BoxLayout(detailsPanel, BoxLayout.Y_AXIS));
        detailsPanel.setBorder(JBUI.Borders.empty(4, 0));

        if (noChanges) {
            JPanel noChangePanel = new JPanel(new BorderLayout());
            noChangePanel.setBorder(JBUI.Borders.empty(16, 24));
            JLabel label = new JLabel(CodeReadingNoteBundle.message("aiconfig.sync.push.no.changes"));
            label.setFont(label.getFont().deriveFont(Font.PLAIN, 12f));
            label.setForeground(Color.GRAY);
            label.setHorizontalAlignment(SwingConstants.CENTER);
            noChangePanel.add(label, BorderLayout.CENTER);
            detailsPanel.add(noChangePanel);
        } else {
            addSection(detailsPanel,
                    CodeReadingNoteBundle.message("aiconfig.sync.push.report.section.pushed", report.getPushedFiles().size()),
                    report.getPushedFiles(), null,
                    AllIcons.Actions.Commit, new Color(92, 184, 92));

            addSection(detailsPanel,
                    CodeReadingNoteBundle.message("aiconfig.sync.push.report.section.skipped", report.getSkippedFiles().size()),
                    report.getSkippedFiles(), null,
                    AllIcons.Actions.Forward, Color.GRAY);

            addSection(detailsPanel,
                    CodeReadingNoteBundle.message("aiconfig.sync.push.report.section.failed", report.getFailedFiles().size()),
                    null, report.getFailedFiles(),
                    AllIcons.General.Error, new Color(217, 83, 79));

            addSection(detailsPanel,
                    CodeReadingNoteBundle.message("aiconfig.sync.push.report.section.deleted", report.getDeletedFiles().size()),
                    report.getDeletedFiles(), null,
                    AllIcons.Actions.GC, new Color(240, 173, 78));

            addSection(detailsPanel,
                    CodeReadingNoteBundle.message("aiconfig.sync.push.report.section.empty.dirs", report.getEmptyDirsSynced().size()),
                    report.getEmptyDirsSynced(), null,
                    AllIcons.Nodes.Folder, Color.GRAY);
        }

        detailsPanel.add(Box.createVerticalGlue());

        JScrollPane scrollPane = new JScrollPane(detailsPanel);
        scrollPane.setBorder(JBUI.Borders.empty());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        root.add(scrollPane, BorderLayout.CENTER);

        return root;
    }

    @NotNull
    private JPanel createSummaryPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(JBUI.Borders.empty(8, 12, 12, 12));

        Icon statusIcon = noChanges ? AllIcons.General.Information
                : success ? AllIcons.General.InspectionsOK : AllIcons.General.Error;
        JLabel iconLabel = new JLabel(statusIcon);
        panel.add(iconLabel, BorderLayout.WEST);

        JPanel textPanel = new JPanel(new GridLayout(2, 1, 0, 4));
        textPanel.setBorder(JBUI.Borders.emptyLeft(12));

        String titleText;
        if (noChanges) {
            titleText = CodeReadingNoteBundle.message("aiconfig.sync.push.report.no.changes.title");
        } else {
            titleText = success
                    ? CodeReadingNoteBundle.message("aiconfig.sync.push.success.title")
                    : CodeReadingNoteBundle.message("aiconfig.sync.push.failed.title");
        }
        JLabel titleLabel = new JLabel(titleText);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 14f));
        textPanel.add(titleLabel);

        if (!noChanges) {
            String summary = CodeReadingNoteBundle.message("aiconfig.sync.push.report.summary",
                    report.getPushedFiles().size(),
                    report.getSkippedFiles().size(),
                    report.getFailedFiles().size(),
                    report.getDeletedFiles().size(),
                    report.getEmptyDirsSynced().size());
            JLabel summaryLabel = new JLabel(summary);
            summaryLabel.setFont(summaryLabel.getFont().deriveFont(Font.PLAIN, 12f));
            summaryLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
            textPanel.add(summaryLabel);
        } else {
            JLabel hintLabel = new JLabel(CodeReadingNoteBundle.message("aiconfig.sync.push.report.no.changes.hint"));
            hintLabel.setFont(hintLabel.getFont().deriveFont(Font.PLAIN, 12f));
            hintLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
            textPanel.add(hintLabel);
        }

        panel.add(textPanel, BorderLayout.CENTER);

        JSeparator separator = new JSeparator();
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(panel, BorderLayout.CENTER);
        wrapper.add(separator, BorderLayout.SOUTH);
        return wrapper;
    }

    private void addSection(@NotNull JPanel parent,
                            @NotNull String title,
                            @Nullable List<String> items,
                            @Nullable Map<String, String> mapItems,
                            @NotNull Icon icon,
                            @NotNull Color accentColor) {
        int count = items != null ? items.size() : (mapItems != null ? mapItems.size() : 0);

        JPanel section = new JPanel(new BorderLayout());
        section.setAlignmentX(Component.LEFT_ALIGNMENT);
        section.setBorder(JBUI.Borders.empty(2, 8));

        JPanel header = new JPanel(new BorderLayout());
        header.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JPanel headerLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        headerLeft.setOpaque(false);

        JLabel toggleLabel = new JLabel(AllIcons.General.ArrowRight);
        headerLeft.add(toggleLabel);

        JLabel iconLabel = new JLabel(icon);
        headerLeft.add(iconLabel);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 12f));
        titleLabel.setForeground(count > 0 ? accentColor : Color.GRAY);
        headerLeft.add(titleLabel);

        header.add(headerLeft, BorderLayout.WEST);

        JPanel detailPanel = new JPanel();
        detailPanel.setLayout(new BoxLayout(detailPanel, BoxLayout.Y_AXIS));
        detailPanel.setBorder(JBUI.Borders.empty(2, 32, 4, 4));
        detailPanel.setVisible(false);

        if (count == 0) {
            JLabel noItems = new JLabel(CodeReadingNoteBundle.message("aiconfig.sync.push.report.no.items"));
            noItems.setForeground(Color.GRAY);
            noItems.setFont(noItems.getFont().deriveFont(Font.ITALIC, 11f));
            detailPanel.add(noItems);
        } else if (items != null) {
            for (String item : items) {
                JLabel label = new JLabel(item);
                label.setFont(label.getFont().deriveFont(Font.PLAIN, 11f));
                label.setBorder(JBUI.Borders.emptyBottom(1));
                detailPanel.add(label);
            }
        } else {
            for (Map.Entry<String, String> entry : mapItems.entrySet()) {
                JPanel row = new JPanel(new BorderLayout());
                row.setAlignmentX(Component.LEFT_ALIGNMENT);
                JLabel pathLabel = new JLabel(entry.getKey());
                pathLabel.setFont(pathLabel.getFont().deriveFont(Font.PLAIN, 11f));
                row.add(pathLabel, BorderLayout.WEST);

                JLabel reasonLabel = new JLabel("  \u2014 " + entry.getValue());
                reasonLabel.setFont(reasonLabel.getFont().deriveFont(Font.ITALIC, 11f));
                reasonLabel.setForeground(new Color(217, 83, 79));
                row.add(reasonLabel, BorderLayout.CENTER);
                row.setBorder(JBUI.Borders.emptyBottom(2));
                detailPanel.add(row);
            }
        }

        header.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                boolean visible = !detailPanel.isVisible();
                detailPanel.setVisible(visible);
                toggleLabel.setIcon(visible ? AllIcons.General.ArrowDown : AllIcons.General.ArrowRight);
                section.revalidate();
                section.repaint();
            }
        });

        section.add(header, BorderLayout.NORTH);
        section.add(detailPanel, BorderLayout.CENTER);

        if (mapItems != null && !mapItems.isEmpty()) {
            detailPanel.setVisible(true);
            toggleLabel.setIcon(AllIcons.General.ArrowDown);
        }

        parent.add(section);
    }
}

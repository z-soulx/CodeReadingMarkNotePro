package jp.kitabatakep.intellij.plugins.codereadingnote.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.util.ui.JBUI;
import jp.kitabatakep.intellij.plugins.codereadingnote.CodeReadingNoteBundle;
import jp.kitabatakep.intellij.plugins.codereadingnote.aiconfig.AISkeletonConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.*;
import java.util.List;

/**
 * Dialog for creating the .ai/ directory skeleton.
 * Replaces the old AIConfigCreateDialog — creates folder structures only (no files).
 */
public class AISkeletonCreateDialog extends DialogWrapper {

    private final Project project;
    private JComboBox<AISkeletonConfig.Preset> presetCombo;
    private final Map<String, JCheckBox> dirCheckboxes = new LinkedHashMap<>();
    private JLabel statusLabel;
    private JPanel referencePanel;
    private boolean referenceVisible = false;

    public AISkeletonCreateDialog(@NotNull Project project) {
        super(project);
        this.project = project;
        setTitle(CodeReadingNoteBundle.message("aiconfig.skeleton.title"));
        init();
    }

    @Override
    @Nullable
    protected JComponent createCenterPanel() {
        JPanel main = new JPanel(new BorderLayout(0, 8));
        main.setPreferredSize(new Dimension(500, 460));

        // Top: preset selector
        JPanel presetPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        presetPanel.add(new JLabel(CodeReadingNoteBundle.message("aiconfig.skeleton.preset.label")));
        presetCombo = new JComboBox<>(AISkeletonConfig.Preset.values());
        presetCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                                                          int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof AISkeletonConfig.Preset) {
                    setText(AISkeletonConfig.getPresetDisplayName((AISkeletonConfig.Preset) value));
                }
                return this;
            }
        });
        presetCombo.addActionListener(e -> applyPreset());
        presetPanel.add(presetCombo);
        main.add(presetPanel, BorderLayout.NORTH);

        // Center: directory checkboxes with descriptions
        JPanel centerWrapper = new JPanel(new BorderLayout(0, 8));

        JPanel dirsPanel = new JPanel();
        dirsPanel.setLayout(new BoxLayout(dirsPanel, BoxLayout.Y_AXIS));
        dirsPanel.setBorder(JBUI.Borders.empty(4, 8));

        JLabel dirsLabel = new JLabel(CodeReadingNoteBundle.message("aiconfig.skeleton.dirs.label"));
        dirsLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        dirsLabel.setFont(dirsLabel.getFont().deriveFont(Font.BOLD));
        dirsPanel.add(dirsLabel);
        dirsPanel.add(Box.createVerticalStrut(6));

        for (AISkeletonConfig.DirEntry dir : AISkeletonConfig.getAllDirs()) {
            JPanel rowPanel = new JPanel(new BorderLayout(8, 0));
            rowPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
            rowPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));

            JCheckBox cb = new JCheckBox(".ai/" + dir.getName() + "/");
            cb.setSelected(false);
            cb.setFont(cb.getFont().deriveFont(Font.PLAIN));
            dirCheckboxes.put(dir.getName(), cb);
            rowPanel.add(cb, BorderLayout.WEST);

            String tag = dir.isRequired() ? " [required]" : " [optional]";
            JLabel descLabel = new JLabel(dir.getDescription() + tag);
            descLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
            descLabel.setFont(descLabel.getFont().deriveFont(Font.ITALIC, 11f));
            rowPanel.add(descLabel, BorderLayout.CENTER);

            dirsPanel.add(rowPanel);
        }

        JScrollPane dirsScroll = new JScrollPane(dirsPanel);
        dirsScroll.setBorder(JBUI.Borders.customLine(UIManager.getColor("Separator.foreground")));
        dirsScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        centerWrapper.add(dirsScroll, BorderLayout.CENTER);

        // Quick Reference toggle button and panel
        JPanel refTogglePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        JButton refToggle = new JButton(CodeReadingNoteBundle.message("aiconfig.skeleton.reference.title") + " \u25BC");
        refToggle.setFont(refToggle.getFont().deriveFont(Font.PLAIN, 11f));
        refToggle.setBorderPainted(false);
        refToggle.setContentAreaFilled(false);
        refToggle.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        refToggle.addActionListener(e -> {
            referenceVisible = !referenceVisible;
            referencePanel.setVisible(referenceVisible);
            refToggle.setText(CodeReadingNoteBundle.message("aiconfig.skeleton.reference.title")
                + (referenceVisible ? " \u25B2" : " \u25BC"));
            main.revalidate();
        });
        refTogglePanel.add(refToggle);
        centerWrapper.add(refTogglePanel, BorderLayout.SOUTH);

        main.add(centerWrapper, BorderLayout.CENTER);

        // Quick Reference content (collapsible)
        referencePanel = new JPanel();
        referencePanel.setLayout(new BoxLayout(referencePanel, BoxLayout.Y_AXIS));
        referencePanel.setBorder(JBUI.Borders.merge(
            JBUI.Borders.customLine(UIManager.getColor("Separator.foreground")),
            JBUI.Borders.empty(8, 12), true));
        referencePanel.setVisible(false);

        for (AISkeletonConfig.DirEntry dir : AISkeletonConfig.getAllDirs()) {
            JLabel refLine = new JLabel(dir.getReferenceText());
            refLine.setFont(refLine.getFont().deriveFont(Font.PLAIN, 11f));
            refLine.setAlignmentX(Component.LEFT_ALIGNMENT);
            referencePanel.add(refLine);
            referencePanel.add(Box.createVerticalStrut(4));
        }

        JScrollPane refScroll = new JScrollPane(referencePanel);
        refScroll.setPreferredSize(new Dimension(0, 160));
        refScroll.setBorder(JBUI.Borders.empty());
        refScroll.setVisible(false);

        // Wrap reference scroll so it toggles
        JPanel refWrapper = new JPanel(new BorderLayout()) {
            @Override
            public boolean isVisible() {
                return referenceVisible;
            }
        };
        refWrapper.add(refScroll, BorderLayout.CENTER);

        // Use a wrapper that includes referencePanel visibility
        referencePanel.addPropertyChangeListener("visible", evt -> {
            refScroll.setVisible(referenceVisible);
            refWrapper.revalidate();
        });

        // Status label at bottom
        statusLabel = new JLabel();
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.ITALIC, 11f));
        statusLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        statusLabel.setBorder(JBUI.Borders.empty(4, 8));

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(referencePanel, BorderLayout.CENTER);
        bottomPanel.add(statusLabel, BorderLayout.SOUTH);
        main.add(bottomPanel, BorderLayout.SOUTH);

        applyPreset();
        updateStatus();
        return main;
    }

    private void applyPreset() {
        AISkeletonConfig.Preset preset = (AISkeletonConfig.Preset) presetCombo.getSelectedItem();
        if (preset == null) preset = AISkeletonConfig.Preset.NONE;
        Set<String> selected = AISkeletonConfig.getDirsForPreset(preset);
        for (Map.Entry<String, JCheckBox> entry : dirCheckboxes.entrySet()) {
            entry.getValue().setSelected(selected.contains(entry.getKey()));
        }
        updateStatus();
    }

    private void updateStatus() {
        String basePath = project.getBasePath();
        if (basePath == null) return;

        File aiDir = new File(basePath, ".ai");
        if (!aiDir.exists()) {
            statusLabel.setText(CodeReadingNoteBundle.message("aiconfig.skeleton.status.not.exist"));
        } else {
            boolean allExist = true;
            for (Map.Entry<String, JCheckBox> entry : dirCheckboxes.entrySet()) {
                if (entry.getValue().isSelected()) {
                    File sub = new File(aiDir, entry.getKey());
                    if (!sub.exists()) {
                        allExist = false;
                        break;
                    }
                }
            }
            statusLabel.setText(allExist
                ? CodeReadingNoteBundle.message("aiconfig.skeleton.status.complete")
                : CodeReadingNoteBundle.message("aiconfig.skeleton.status.partial"));
        }
    }

    @Override
    @Nullable
    protected ValidationInfo doValidate() {
        boolean anySelected = dirCheckboxes.values().stream().anyMatch(JCheckBox::isSelected);
        if (!anySelected) {
            return new ValidationInfo(
                CodeReadingNoteBundle.message("aiconfig.skeleton.validation.none.selected"));
        }
        return null;
    }

    /**
     * Returns the list of directory names selected by the user.
     */
    @NotNull
    public List<String> getSelectedDirNames() {
        List<String> selected = new ArrayList<>();
        for (Map.Entry<String, JCheckBox> entry : dirCheckboxes.entrySet()) {
            if (entry.getValue().isSelected()) {
                selected.add(entry.getKey());
            }
        }
        return selected;
    }
}

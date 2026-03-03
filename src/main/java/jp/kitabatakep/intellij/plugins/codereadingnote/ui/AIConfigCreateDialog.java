package jp.kitabatakep.intellij.plugins.codereadingnote.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.util.ui.JBUI;
import jp.kitabatakep.intellij.plugins.codereadingnote.CodeReadingNoteBundle;
import jp.kitabatakep.intellij.plugins.codereadingnote.aiconfig.AIConfigType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class AIConfigCreateDialog extends DialogWrapper {

    private final Project project;
    private JComboBox<AIConfigType> typeCombo;
    private JTextField fileNameField;
    private JLabel pathPreviewLabel;

    public AIConfigCreateDialog(@NotNull Project project) {
        super(project);
        this.project = project;
        setTitle(CodeReadingNoteBundle.message("aiconfig.create.title"));
        init();
    }

    @Override
    @Nullable
    protected JComponent createCenterPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setPreferredSize(new Dimension(420, 130));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = JBUI.insets(4, 4);
        gbc.anchor = GridBagConstraints.WEST;

        // Row 0: Type
        gbc.gridx = 0; gbc.gridy = 0; gbc.fill = GridBagConstraints.NONE;
        panel.add(new JLabel(CodeReadingNoteBundle.message("aiconfig.create.type")), gbc);

        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        typeCombo = new JComboBox<>(new AIConfigType[]{
            AIConfigType.CURSOR_RULES, AIConfigType.CLAUDE_RULES, AIConfigType.AI_DOCS,
            AIConfigType.WINDSURF, AIConfigType.COPILOT, AIConfigType.CUSTOM
        });
        typeCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                                                          int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof AIConfigType) {
                    setText(((AIConfigType) value).getDisplayName());
                }
                return this;
            }
        });
        typeCombo.addActionListener(e -> onTypeChanged());
        panel.add(typeCombo, gbc);

        // Row 1: File name
        gbc.gridx = 0; gbc.gridy = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        panel.add(new JLabel(CodeReadingNoteBundle.message("aiconfig.create.filename")), gbc);

        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        fileNameField = new JTextField(getDefaultFileName(AIConfigType.CURSOR_RULES));
        fileNameField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { updatePreview(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { updatePreview(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { updatePreview(); }
        });
        panel.add(fileNameField, gbc);

        // Row 2: Path preview
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.HORIZONTAL;
        pathPreviewLabel = new JLabel();
        pathPreviewLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        pathPreviewLabel.setFont(pathPreviewLabel.getFont().deriveFont(Font.ITALIC, 11f));
        panel.add(pathPreviewLabel, gbc);

        updatePreview();
        return panel;
    }

    private void onTypeChanged() {
        AIConfigType type = getSelectedType();
        fileNameField.setText(getDefaultFileName(type));
        updatePreview();
    }

    private void updatePreview() {
        AIConfigType type = getSelectedType();
        String basePath = type.getDefaultPath();
        if (basePath == null) basePath = "";
        String fileName = fileNameField.getText().trim();
        String fullPath = basePath + fileName;
        pathPreviewLabel.setText(CodeReadingNoteBundle.message("aiconfig.create.preview", fullPath));
    }

    @NotNull
    private String getDefaultFileName(@NotNull AIConfigType type) {
        switch (type) {
            case CURSOR_RULES: return "my-rules.md";
            case CLAUDE_RULES: return "CLAUDE.md";
            case AI_DOCS: return "ARCHITECTURE.md";
            case WINDSURF: return "rules.md";
            case COPILOT: return "copilot-instructions.md";
            default: return "config.md";
        }
    }

    @NotNull
    private String getBoilerplate(@NotNull AIConfigType type, @NotNull String fileName) {
        String title = fileName.replace(".md", "").replace("-", " ").replace("_", " ");
        switch (type) {
            case CURSOR_RULES:
                return "# " + title + "\n\n## Rules\n\n- \n";
            case CLAUDE_RULES:
                return "# Project Name\n\n## Hard Rules\n\n- \n\n## Key Paths\n\n| What | Where |\n|------|-------|\n| | |\n";
            case AI_DOCS:
                return "# " + title + "\n\n## Overview\n\n\n## Details\n\n\n";
            case WINDSURF:
                return "# " + title + "\n\n## Rules\n\n- \n";
            case COPILOT:
                return "## Project Context\n\n\n## Instructions\n\n- \n";
            default:
                return "# " + title + "\n\n";
        }
    }

    @Override
    @Nullable
    protected ValidationInfo doValidate() {
        String fileName = fileNameField.getText().trim();
        if (fileName.isEmpty()) {
            return new ValidationInfo(
                CodeReadingNoteBundle.message("aiconfig.create.validation.empty"), fileNameField);
        }
        if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
            return new ValidationInfo(
                CodeReadingNoteBundle.message("aiconfig.create.validation.invalid"), fileNameField);
        }
        AIConfigType type = getSelectedType();
        String basePath = type.getDefaultPath();
        if (basePath == null) basePath = "";
        String fullRelative = basePath + fileName;
        String projectBase = project.getBasePath();
        if (projectBase != null) {
            File target = new File(projectBase, fullRelative);
            if (target.exists()) {
                return new ValidationInfo(
                    CodeReadingNoteBundle.message("aiconfig.create.validation.exists"), fileNameField);
            }
        }
        return null;
    }

    @NotNull
    public AIConfigType getSelectedType() {
        return (AIConfigType) typeCombo.getSelectedItem();
    }

    @NotNull
    public String getFileName() {
        return fileNameField.getText().trim();
    }

    @NotNull
    public String getRelativePath() {
        AIConfigType type = getSelectedType();
        String basePath = type.getDefaultPath();
        if (basePath == null) basePath = "";
        return basePath + getFileName();
    }

    @NotNull
    public String getBoilerplateContent() {
        return getBoilerplate(getSelectedType(), getFileName());
    }
}

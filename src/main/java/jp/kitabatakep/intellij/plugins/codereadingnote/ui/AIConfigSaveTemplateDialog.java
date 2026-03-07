package jp.kitabatakep.intellij.plugins.codereadingnote.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.util.ui.JBUI;
import jp.kitabatakep.intellij.plugins.codereadingnote.CodeReadingNoteBundle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Dialog for saving AI config files as a reusable template.
 */
public class AIConfigSaveTemplateDialog extends DialogWrapper {

    private final JTextField nameField;
    private final JTextArea descriptionArea;
    private final JTextField tagsField;

    public AIConfigSaveTemplateDialog(@NotNull Project project) {
        super(project, true);
        nameField = new JTextField();
        descriptionArea = new JTextArea(3, 30);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        tagsField = new JTextField();

        setTitle(CodeReadingNoteBundle.message("aiconfig.template.save.title"));
        init();
    }

    @Override
    @Nullable
    protected JComponent createCenterPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(JBUI.Borders.empty(8));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = JBUI.insets(4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Name
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        panel.add(new JLabel(CodeReadingNoteBundle.message("aiconfig.template.detail.name") + ":"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        panel.add(nameField, gbc);

        // Description
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        panel.add(new JLabel(CodeReadingNoteBundle.message("aiconfig.template.detail.description") + ":"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        gbc.fill = GridBagConstraints.BOTH; gbc.weighty = 1;
        panel.add(new JScrollPane(descriptionArea), gbc);

        // Tags
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weighty = 0;
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(new JLabel(CodeReadingNoteBundle.message("aiconfig.template.detail.tags") + ":"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        panel.add(tagsField, gbc);

        // Tags hint
        gbc.gridx = 1; gbc.gridy = 3;
        JLabel hint = new JLabel(CodeReadingNoteBundle.message("aiconfig.template.tags.hint"));
        hint.setFont(hint.getFont().deriveFont(Font.ITALIC, 10f));
        hint.setForeground(UIManager.getColor("Label.disabledForeground"));
        panel.add(hint, gbc);

        panel.setPreferredSize(new Dimension(400, 250));
        return panel;
    }

    @Override
    @Nullable
    protected ValidationInfo doValidate() {
        if (nameField.getText().trim().isEmpty()) {
            return new ValidationInfo(
                CodeReadingNoteBundle.message("aiconfig.template.validation.name.required"), nameField);
        }
        return null;
    }

    @NotNull
    public String getTemplateName() {
        return nameField.getText().trim();
    }

    @NotNull
    public String getTemplateDescription() {
        return descriptionArea.getText().trim();
    }

    @NotNull
    public List<String> getTemplateTags() {
        String text = tagsField.getText().trim();
        if (text.isEmpty()) return List.of();
        return Arrays.stream(text.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}

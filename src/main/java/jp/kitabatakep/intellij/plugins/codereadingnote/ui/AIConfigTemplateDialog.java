package jp.kitabatakep.intellij.plugins.codereadingnote.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import jp.kitabatakep.intellij.plugins.codereadingnote.CodeReadingNoteBundle;
import jp.kitabatakep.intellij.plugins.codereadingnote.aiconfig.AIConfigTemplate;
import jp.kitabatakep.intellij.plugins.codereadingnote.aiconfig.AIConfigTemplateService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Dialog for browsing and applying AI config templates.
 */
public class AIConfigTemplateDialog extends DialogWrapper {

    private final Project project;
    private final JBList<AIConfigTemplate> templateList;
    private final JTextArea detailArea;
    private AIConfigTemplate selectedTemplate;

    public AIConfigTemplateDialog(@NotNull Project project) {
        super(project, true);
        this.project = project;

        templateList = new JBList<>();
        detailArea = new JTextArea();
        detailArea.setEditable(false);
        detailArea.setLineWrap(true);
        detailArea.setWrapStyleWord(true);

        setTitle(CodeReadingNoteBundle.message("aiconfig.template.dialog.title"));
        setOKButtonText(CodeReadingNoteBundle.message("aiconfig.template.apply"));
        init();
        loadTemplates();
    }

    @Override
    @Nullable
    protected JComponent createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setPreferredSize(new Dimension(600, 400));

        // Template list on the left
        templateList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof AIConfigTemplate) {
                    AIConfigTemplate t = (AIConfigTemplate) value;
                    setText(t.getName());
                    String tags = t.getTags().isEmpty() ? "" : " [" + String.join(", ", t.getTags()) + "]";
                    setText(t.getName() + tags);
                }
                return this;
            }
        });

        templateList.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            selectedTemplate = templateList.getSelectedValue();
            updateDetail();
        });

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setLeftComponent(new JBScrollPane(templateList));
        splitPane.setRightComponent(new JBScrollPane(detailArea));
        splitPane.setDividerLocation(250);
        panel.add(splitPane, BorderLayout.CENTER);

        // Buttons at the bottom
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton deleteBtn = new JButton(CodeReadingNoteBundle.message("aiconfig.template.delete"));
        deleteBtn.addActionListener(e -> deleteSelected());
        buttonPanel.add(deleteBtn);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void loadTemplates() {
        List<AIConfigTemplate> templates = AIConfigTemplateService.getInstance().getTemplates();
        templateList.setListData(templates.toArray(new AIConfigTemplate[0]));
        if (!templates.isEmpty()) {
            templateList.setSelectedIndex(0);
        }
    }

    private void updateDetail() {
        if (selectedTemplate == null) {
            detailArea.setText("");
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        StringBuilder sb = new StringBuilder();
        sb.append(CodeReadingNoteBundle.message("aiconfig.template.detail.name")).append(": ").append(selectedTemplate.getName()).append("\n");
        sb.append(CodeReadingNoteBundle.message("aiconfig.template.detail.description")).append(": ").append(selectedTemplate.getDescription()).append("\n");
        sb.append(CodeReadingNoteBundle.message("aiconfig.template.detail.tags")).append(": ").append(String.join(", ", selectedTemplate.getTags())).append("\n");
        sb.append(CodeReadingNoteBundle.message("aiconfig.template.detail.created")).append(": ").append(sdf.format(new Date(selectedTemplate.getCreatedAt()))).append("\n");
        sb.append("\n--- ").append(CodeReadingNoteBundle.message("aiconfig.info.files")).append(" ---\n");
        for (AIConfigTemplate.TemplateFileEntry entry : selectedTemplate.getEntries()) {
            sb.append("  ").append(entry.getRelativePath());
            sb.append(" [").append(entry.getType().getDisplayName()).append("]");
            sb.append(" (").append(entry.getContent().length()).append(" chars)\n");
        }

        detailArea.setText(sb.toString());
        detailArea.setCaretPosition(0);
    }

    private void deleteSelected() {
        if (selectedTemplate == null) return;
        int confirm = Messages.showYesNoDialog(
            project,
            CodeReadingNoteBundle.message("aiconfig.template.delete.confirm", selectedTemplate.getName()),
            CodeReadingNoteBundle.message("aiconfig.template.delete"),
            Messages.getWarningIcon()
        );
        if (confirm == Messages.YES) {
            AIConfigTemplateService.getInstance().removeTemplate(selectedTemplate.getId());
            loadTemplates();
        }
    }

    @Nullable
    public AIConfigTemplate getSelectedTemplate() {
        return selectedTemplate;
    }
}

package jp.kitabatakep.intellij.plugins.codereadingnote.ui.dialogs;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBRadioButton;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import jp.kitabatakep.intellij.plugins.codereadingnote.CodeReadingNoteBundle;
import jp.kitabatakep.intellij.plugins.codereadingnote.TopicLine;
import jp.kitabatakep.intellij.plugins.codereadingnote.operations.LineNumberUpdateService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.List;

/**
 * Dialog for batch adjusting line numbers of multiple TopicLines
 */
public class BatchLineNumberAdjustDialog extends DialogWrapper {
    
    public enum AdjustMode {
        ADD_OFFSET,
        SUBTRACT_OFFSET,
        SET_SPECIFIC
    }
    
    private final Project project;
    private final List<TopicLine> topicLines;
    
    private JBRadioButton addOffsetButton;
    private JBRadioButton subtractOffsetButton;
    private JBRadioButton setSpecificButton;
    private ButtonGroup modeButtonGroup;
    
    private JBTextField valueField;
    private JBCheckBox updateBookmarksCheckBox;
    private JBCheckBox validateCheckBox;
    
    private JTextArea previewArea;
    
    public BatchLineNumberAdjustDialog(@NotNull Project project, 
                                       @NotNull List<TopicLine> topicLines) {
        super(project);
        this.project = project;
        this.topicLines = topicLines;
        
        setTitle(CodeReadingNoteBundle.message("dialog.batch.adjust.title"));
        init();
    }
    
    @Override
    @Nullable
    protected JComponent createCenterPanel() {
        // Mode selection
        addOffsetButton = new JBRadioButton(
            CodeReadingNoteBundle.message("dialog.batch.adjust.mode.add"),
            true
        );
        subtractOffsetButton = new JBRadioButton(
            CodeReadingNoteBundle.message("dialog.batch.adjust.mode.subtract")
        );
        setSpecificButton = new JBRadioButton(
            CodeReadingNoteBundle.message("dialog.batch.adjust.mode.set")
        );
        
        modeButtonGroup = new ButtonGroup();
        modeButtonGroup.add(addOffsetButton);
        modeButtonGroup.add(subtractOffsetButton);
        modeButtonGroup.add(setSpecificButton);
        
        // Value field
        valueField = new JBTextField("0", 10);
        
        // Options
        updateBookmarksCheckBox = new JBCheckBox(
            CodeReadingNoteBundle.message("dialog.batch.adjust.update.bookmarks"),
            true
        );
        validateCheckBox = new JBCheckBox(
            CodeReadingNoteBundle.message("dialog.batch.adjust.validate"),
            true
        );
        
        // Preview area
        previewArea = new JTextArea(8, 40);
        previewArea.setEditable(false);
        previewArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane previewScrollPane = new JScrollPane(previewArea);
        previewScrollPane.setBorder(BorderFactory.createTitledBorder("Preview"));
        
        // Add listeners for live preview
        addOffsetButton.addActionListener(e -> updatePreview());
        subtractOffsetButton.addActionListener(e -> updatePreview());
        setSpecificButton.addActionListener(e -> updatePreview());
        valueField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updatePreview();
            }
            
            @Override
            public void removeUpdate(DocumentEvent e) {
                updatePreview();
            }
            
            @Override
            public void changedUpdate(DocumentEvent e) {
                updatePreview();
            }
        });
        
        // Build form
        JPanel modesPanel = new JPanel();
        modesPanel.setLayout(new BoxLayout(modesPanel, BoxLayout.Y_AXIS));
        modesPanel.add(addOffsetButton);
        modesPanel.add(subtractOffsetButton);
        modesPanel.add(setSpecificButton);
        
        FormBuilder builder = FormBuilder.createFormBuilder()
            .addComponent(new JBLabel(
                CodeReadingNoteBundle.message("dialog.batch.adjust.count", topicLines.size())
            ))
            .addSeparator()
            .addLabeledComponent(new JBLabel("Mode:"), modesPanel)
            .addLabeledComponent(new JBLabel("Value:"), valueField)
            .addSeparator()
            .addComponent(updateBookmarksCheckBox)
            .addComponent(validateCheckBox)
            .addSeparator()
            .addComponent(previewScrollPane);
        
        JPanel panel = builder.getPanel();
        panel.setPreferredSize(new Dimension(600, 450));
        
        // Initial preview
        updatePreview();
        
        return panel;
    }
    
    @Override
    @Nullable
    protected ValidationInfo doValidate() {
        String text = valueField.getText().trim();
        
        if (text.isEmpty()) {
            return new ValidationInfo("Please enter a value", valueField);
        }
        
        try {
            int value = Integer.parseInt(text);
            
            if (setSpecificButton.isSelected() && value < 0) {
                return new ValidationInfo("Line number cannot be negative", valueField);
            }
            
            return null;
            
        } catch (NumberFormatException e) {
            return new ValidationInfo("Invalid number format", valueField);
        }
    }
    
    /**
     * Update preview area with calculated results
     */
    private void updatePreview() {
        StringBuilder preview = new StringBuilder();
        preview.append("Old Line → New Line | File\n");
        preview.append("─".repeat(60)).append("\n");
        
        try {
            int value = Integer.parseInt(valueField.getText().trim());
            LineNumberUpdateService.LineNumberAdjustment adjustment = getAdjustment();
            
            int validCount = 0;
            int invalidCount = 0;
            
            for (TopicLine line : topicLines) {
                int oldLine = line.line();
                int newLine = adjustment.calculate(oldLine);
                
                String status = "✓";
                if (validateCheckBox.isSelected()) {
                    LineNumberUpdateService service = LineNumberUpdateService.getInstance(project);
                    LineNumberUpdateService.LineNumberValidation validation = 
                        service.validateLineNumber(line, newLine);
                    
                    if (!validation.isValid()) {
                        status = "❌";
                        invalidCount++;
                    } else {
                        validCount++;
                    }
                } else {
                    validCount++;
                }
                
                String fileName = line.url();
                int lastSlash = Math.max(fileName.lastIndexOf('/'), fileName.lastIndexOf('\\'));
                if (lastSlash >= 0) {
                    fileName = fileName.substring(lastSlash + 1);
                }
                
                preview.append(String.format("%s %4d → %4d | %s\n", 
                    status, oldLine, newLine, fileName));
            }
            
            preview.append("─".repeat(60)).append("\n");
            preview.append(String.format("Valid: %d | Invalid: %d | Total: %d", 
                validCount, invalidCount, topicLines.size()));
            
        } catch (NumberFormatException e) {
            preview.append("Invalid value. Please enter a number.");
        }
        
        previewArea.setText(preview.toString());
        previewArea.setCaretPosition(0);
    }
    
    @NotNull
    public LineNumberUpdateService.LineNumberAdjustment getAdjustment() {
        try {
            int value = Integer.parseInt(valueField.getText().trim());
            
            if (addOffsetButton.isSelected()) {
                return LineNumberUpdateService.LineNumberAdjustment.addOffset(value);
            } else if (subtractOffsetButton.isSelected()) {
                return LineNumberUpdateService.LineNumberAdjustment.subtractOffset(value);
            } else {
                return LineNumberUpdateService.LineNumberAdjustment.setSpecific(value);
            }
        } catch (NumberFormatException e) {
            return oldNum -> oldNum;
        }
    }
    
    public AdjustMode getAdjustMode() {
        if (addOffsetButton.isSelected()) {
            return AdjustMode.ADD_OFFSET;
        } else if (subtractOffsetButton.isSelected()) {
            return AdjustMode.SUBTRACT_OFFSET;
        } else {
            return AdjustMode.SET_SPECIFIC;
        }
    }
    
    public int getValue() {
        try {
            return Integer.parseInt(valueField.getText().trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    
    public boolean shouldUpdateBookmarks() {
        return updateBookmarksCheckBox.isSelected();
    }
    
    public boolean shouldValidate() {
        return validateCheckBox.isSelected();
    }
}


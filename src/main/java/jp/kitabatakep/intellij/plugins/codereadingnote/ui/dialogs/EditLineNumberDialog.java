package jp.kitabatakep.intellij.plugins.codereadingnote.ui.dialogs;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
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

/**
 * Dialog for editing a single TopicLine's line number
 */
public class EditLineNumberDialog extends DialogWrapper {
    
    private final Project project;
    private final TopicLine topicLine;
    
    private JBTextField lineNumberField;
    private JBCheckBox updateBookmarkCheckBox;
    private JBCheckBox validateCheckBox;
    private JBLabel validationResultLabel;
    
    private int newLineNumber;
    
    public EditLineNumberDialog(@NotNull Project project, @NotNull TopicLine topicLine) {
        super(project);
        this.project = project;
        this.topicLine = topicLine;
        this.newLineNumber = topicLine.line();
        
        setTitle(CodeReadingNoteBundle.message("dialog.line.number.title"));
        init();
    }
    
    @Override
    @Nullable
    protected JComponent createCenterPanel() {
        lineNumberField = new JBTextField(String.valueOf(topicLine.line()), 10);
        updateBookmarkCheckBox = new JBCheckBox(
            CodeReadingNoteBundle.message("dialog.batch.adjust.update.bookmarks"),
            true
        );
        validateCheckBox = new JBCheckBox(
            CodeReadingNoteBundle.message("dialog.batch.adjust.validate"),
            true
        );
        validationResultLabel = new JBLabel();
        
        // Add document listener for real-time validation
        lineNumberField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                validateInput();
            }
            
            @Override
            public void removeUpdate(DocumentEvent e) {
                validateInput();
            }
            
            @Override
            public void changedUpdate(DocumentEvent e) {
                validateInput();
            }
        });
        
        FormBuilder builder = FormBuilder.createFormBuilder()
            .addLabeledComponent(
                new JBLabel(CodeReadingNoteBundle.message("dialog.line.number.file")),
                new JBLabel(topicLine.pathForDisplay())
            )
            .addLabeledComponent(
                new JBLabel(CodeReadingNoteBundle.message("dialog.line.number.current")),
                new JBLabel(String.valueOf(topicLine.line()))
            )
            .addSeparator()
            .addLabeledComponent(
                new JBLabel(CodeReadingNoteBundle.message("dialog.line.number.new")),
                lineNumberField
            )
            .addComponent(validationResultLabel)
            .addSeparator()
            .addComponent(updateBookmarkCheckBox)
            .addComponent(validateCheckBox);
        
        JPanel panel = builder.getPanel();
        panel.setPreferredSize(new Dimension(500, 250));
        
        return panel;
    }
    
    @Override
    protected void doOKAction() {
        if (getOKAction().isEnabled()) {
            try {
                newLineNumber = Integer.parseInt(lineNumberField.getText().trim());
                super.doOKAction();
            } catch (NumberFormatException e) {
                setErrorText(CodeReadingNoteBundle.message("message.line.number.invalid", 
                    lineNumberField.getText()));
            }
        }
    }
    
    @Override
    @Nullable
    protected ValidationInfo doValidate() {
        String text = lineNumberField.getText().trim();
        
        if (text.isEmpty()) {
            return new ValidationInfo(
                CodeReadingNoteBundle.message("message.line.number.invalid", "empty"),
                lineNumberField
            );
        }
        
        try {
            int lineNum = Integer.parseInt(text);
            
            if (lineNum < 0) {
                return new ValidationInfo(
                    CodeReadingNoteBundle.message("message.line.number.invalid", 
                        "negative number"),
                    lineNumberField
                );
            }
            
            if (validateCheckBox.isSelected()) {
                LineNumberUpdateService service = LineNumberUpdateService.getInstance(project);
                LineNumberUpdateService.LineNumberValidation validation = 
                    service.validateLineNumber(topicLine, lineNum);
                
                if (!validation.isValid()) {
                    return new ValidationInfo(
                        validation.getErrorMessage(),
                        lineNumberField
                    );
                }
            }
            
            return null;
            
        } catch (NumberFormatException e) {
            return new ValidationInfo(
                CodeReadingNoteBundle.message("message.line.number.invalid", text),
                lineNumberField
            );
        }
    }
    
    /**
     * Real-time validation feedback
     */
    private void validateInput() {
        String text = lineNumberField.getText().trim();
        
        if (text.isEmpty()) {
            validationResultLabel.setText("");
            validationResultLabel.setIcon(null);
            return;
        }
        
        try {
            int lineNum = Integer.parseInt(text);
            
            if (lineNum < 0) {
                validationResultLabel.setText("❌ Line number cannot be negative");
                validationResultLabel.setForeground(Color.RED);
                return;
            }
            
            if (validateCheckBox.isSelected()) {
                LineNumberUpdateService service = LineNumberUpdateService.getInstance(project);
                LineNumberUpdateService.LineNumberValidation validation = 
                    service.validateLineNumber(topicLine, lineNum);
                
                if (!validation.isValid()) {
                    validationResultLabel.setText("❌ " + validation.getErrorMessage());
                    validationResultLabel.setForeground(Color.RED);
                } else if (validation.isWarning()) {
                    validationResultLabel.setText("⚠️ " + validation.getErrorMessage());
                    validationResultLabel.setForeground(Color.ORANGE);
                } else {
                    validationResultLabel.setText("✓ Valid line number");
                    validationResultLabel.setForeground(new Color(0, 128, 0));
                }
            } else {
                validationResultLabel.setText("✓ Validation disabled");
                validationResultLabel.setForeground(Color.GRAY);
            }
            
        } catch (NumberFormatException e) {
            validationResultLabel.setText("❌ Invalid number format");
            validationResultLabel.setForeground(Color.RED);
        }
    }
    
    public int getNewLineNumber() {
        return newLineNumber;
    }
    
    public boolean shouldUpdateBookmark() {
        return updateBookmarkCheckBox.isSelected();
    }
    
    public boolean shouldValidate() {
        return validateCheckBox.isSelected();
    }
}


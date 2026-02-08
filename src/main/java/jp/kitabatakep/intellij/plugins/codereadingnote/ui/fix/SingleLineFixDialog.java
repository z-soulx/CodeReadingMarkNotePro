package jp.kitabatakep.intellij.plugins.codereadingnote.ui.fix;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * Single line fix preview dialog
 */
public class SingleLineFixDialog extends DialogWrapper {
    
    private final LineFixResult result;
    
    public SingleLineFixDialog(Project project, LineFixResult result) {
        super(project);
        this.result = result;
        
        setTitle("Fix TopicLine Position");
        init();
    }
    
    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(JBUI.Borders.empty(10));
        
        // Info panel
        JPanel infoPanel = createInfoPanel();
        panel.add(infoPanel, BorderLayout.NORTH);
        
        // Status panel
        JPanel statusPanel = createStatusPanel();
        panel.add(statusPanel, BorderLayout.CENTER);
        
        // Warning panel
        if (result.getStatus() == LineFixResult.FixStatus.NEEDS_FIX) {
            JPanel warningPanel = createWarningPanel();
            panel.add(warningPanel, BorderLayout.SOUTH);
        }
        
        return panel;
    }
    
    /**
     * Create info panel
     */
    private JPanel createInfoPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = JBUI.insets(2);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // File name
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        panel.add(new JBLabel("File:"), gbc);
        
        gbc.gridx = 1;
        gbc.weightx = 1;
        JBLabel fileLabel = new JBLabel(result.getFileName());
        fileLabel.setFont(fileLabel.getFont().deriveFont(Font.BOLD));
        panel.add(fileLabel, gbc);
        
        // Path
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        panel.add(new JBLabel("Path:"), gbc);
        
        gbc.gridx = 1;
        gbc.weightx = 1;
        JBLabel pathLabel = new JBLabel(result.getFilePath());
        pathLabel.setForeground(Color.GRAY);
        panel.add(pathLabel, gbc);
        
        // Topic
        if (result.getTopicLine().topic() != null) {
            gbc.gridx = 0;
            gbc.gridy = 2;
            gbc.weightx = 0;
            panel.add(new JBLabel("Topic:"), gbc);
            
            gbc.gridx = 1;
            gbc.weightx = 1;
            panel.add(new JBLabel(result.getTopicLine().topic().name()), gbc);
        }
        
        // Note
        if (!result.getNote().isEmpty()) {
            gbc.gridx = 0;
            gbc.gridy = 3;
            gbc.weightx = 0;
            gbc.anchor = GridBagConstraints.NORTHWEST;
            panel.add(new JBLabel("Note:"), gbc);
            
            gbc.gridx = 1;
            gbc.weightx = 1;
            String note = result.getNote();
            if (note.length() > 100) {
                note = note.substring(0, 100) + "...";
            }
            JTextArea noteArea = new JTextArea(note);
            noteArea.setEditable(false);
            noteArea.setLineWrap(true);
            noteArea.setWrapStyleWord(true);
            noteArea.setBackground(panel.getBackground());
            noteArea.setBorder(null);
            panel.add(noteArea, gbc);
        }
        
        return panel;
    }
    
    /**
     * Create status panel
     */
    private JPanel createStatusPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(JBUI.Borders.empty(10));
        
        // Show different content based on status
        switch (result.getStatus()) {
            case NEEDS_FIX:
                panel.add(createNeedsFixPanel(), BorderLayout.CENTER);
                break;
            case SYNCED:
                panel.add(createSyncedPanel(), BorderLayout.CENTER);
                break;
            case BOOKMARK_MISSING:
                panel.add(createBookmarkMissingPanel(), BorderLayout.CENTER);
                break;
            case FILE_NOT_FOUND:
                panel.add(createFileNotFoundPanel(), BorderLayout.CENTER);
                break;
        }
        
        return panel;
    }
    
    /**
     * Create needs fix panel
     */
    private JPanel createNeedsFixPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Position Change"));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = JBUI.insets(5);
        gbc.anchor = GridBagConstraints.CENTER;
        
        // Current line
        gbc.gridx = 0;
        gbc.gridy = 0;
        JBLabel currentLabel = new JBLabel("Current Line:");
        panel.add(currentLabel, gbc);
        
        gbc.gridy = 1;
        JBLabel oldLineLabel = new JBLabel(String.valueOf(result.getOldLine()));
        oldLineLabel.setFont(oldLineLabel.getFont().deriveFont(Font.BOLD, 24f));
        oldLineLabel.setForeground(new Color(255, 140, 0)); // Orange
        panel.add(oldLineLabel, gbc);
        
        // Arrow
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridheight = 2;
        JBLabel arrowLabel = new JBLabel("→");
        arrowLabel.setFont(arrowLabel.getFont().deriveFont(Font.BOLD, 32f));
        arrowLabel.setForeground(Color.GRAY);
        panel.add(arrowLabel, gbc);
        
        // Bookmark position
        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.gridheight = 1;
        JBLabel bookmarkLabel = new JBLabel("Bookmark Position:");
        panel.add(bookmarkLabel, gbc);
        
        gbc.gridy = 1;
        JBLabel newLineLabel = new JBLabel(String.valueOf(result.getNewLine()));
        newLineLabel.setFont(newLineLabel.getFont().deriveFont(Font.BOLD, 24f));
        newLineLabel.setForeground(new Color(0, 128, 0)); // Green
        panel.add(newLineLabel, gbc);
        
        // Offset
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 3;
        int offset = result.getNewLine() - result.getOldLine();
        String offsetText = String.format("Offset: %+d lines", offset);
        JBLabel offsetLabel = new JBLabel(offsetText);
        offsetLabel.setForeground(Color.GRAY);
        panel.add(offsetLabel, gbc);
        
        return panel;
    }
    
    /**
     * Create synced panel
     */
    private JPanel createSyncedPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Status"));
        
        JBLabel label = new JBLabel("✅ This TopicLine is already synced with Bookmark, no fix needed");
        label.setIcon(result.getIcon());
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setBorder(JBUI.Borders.empty(20));
        
        panel.add(label, BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * Create bookmark missing panel
     */
    private JPanel createBookmarkMissingPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createTitledBorder("Status"));
        
        // Main error message
        JBLabel errorLabel = new JBLabel("❌ Cannot find the corresponding Bookmark");
        errorLabel.setIcon(result.getIcon());
        errorLabel.setHorizontalAlignment(SwingConstants.CENTER);
        errorLabel.setForeground(Color.RED);
        errorLabel.setBorder(JBUI.Borders.empty(10));
        panel.add(errorLabel, BorderLayout.NORTH);
        
        // Explanation panel
        JPanel explanationPanel = new JPanel();
        explanationPanel.setLayout(new BoxLayout(explanationPanel, BoxLayout.Y_AXIS));
        explanationPanel.setBorder(JBUI.Borders.empty(10));
        
        JBLabel causeTitle = new JBLabel("Possible causes:");
        causeTitle.setFont(causeTitle.getFont().deriveFont(Font.BOLD));
        causeTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        explanationPanel.add(causeTitle);
        explanationPanel.add(Box.createVerticalStrut(5));
        
        String[] causes = {
            "1. The bookmark was manually deleted",
            "2. Another bookmark was added on the same line (IntelliJ only supports one bookmark per line)",
            "3. The bookmark was lost during branch switching or code merging"
        };
        
        for (String cause : causes) {
            JBLabel causeLabel = new JBLabel(cause);
            causeLabel.setForeground(Color.GRAY);
            causeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            explanationPanel.add(causeLabel);
            explanationPanel.add(Box.createVerticalStrut(3));
        }
        
        explanationPanel.add(Box.createVerticalStrut(10));
        
        JBLabel tipTitle = new JBLabel("💡 Tip:");
        tipTitle.setFont(tipTitle.getFont().deriveFont(Font.BOLD));
        tipTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        explanationPanel.add(tipTitle);
        
        JBLabel tipLabel = new JBLabel("Use \"Repair Bookmarks\" action to recreate missing bookmarks");
        tipLabel.setForeground(new Color(0, 100, 180));
        tipLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        explanationPanel.add(tipLabel);
        
        panel.add(explanationPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * Create file not found panel
     */
    private JPanel createFileNotFoundPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Status"));
        
        JBLabel label = new JBLabel("🚫 File does not exist, may have been deleted in current branch");
        label.setIcon(result.getIcon());
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setForeground(Color.GRAY);
        label.setBorder(JBUI.Borders.empty(20));
        
        panel.add(label, BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * Create warning panel
     */
    private JPanel createWarningPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(JBUI.Borders.empty(10, 0, 0, 0));
        
        JBLabel warningLabel = new JBLabel("⚠️ This code line may have moved due to branch switch or code modification");
        warningLabel.setForeground(new Color(255, 140, 0));
        panel.add(warningLabel, BorderLayout.CENTER);
        
        return panel;
    }
    
    @Override
    protected Action @Nullable [] createActions() {
        if (result.getStatus() == LineFixResult.FixStatus.NEEDS_FIX) {
            return new Action[]{
                getCancelAction(),
                new DialogWrapperAction("Fix to Line " + result.getNewLine()) {
                    @Override
                    protected void doAction(java.awt.event.ActionEvent e) {
                        doOKAction();
                    }
                }
            };
        } else {
            // For synced, bookmark missing, file not found, only show close button
            return new Action[]{
                new DialogWrapperAction("Close") {
                    @Override
                    protected void doAction(java.awt.event.ActionEvent e) {
                        doCancelAction();
                    }
                }
            };
        }
    }
}

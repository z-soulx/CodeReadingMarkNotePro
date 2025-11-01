package jp.kitabatakep.intellij.plugins.codereadingnote.ui.fix;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * Batch fix preview dialog (for Topic and Fix All)
 */
public class BatchFixDialog extends DialogWrapper {
    
    private final FixPreviewData previewData;
    private final boolean showOnlyNeedsFix;
    
    private JBList<LineFixResult> resultList;
    
    public enum FixMode {
        FIX_ONLY_NEEDS,    // Fix only needs fix
        FIX_ALL            // Resync all
    }
    
    private FixMode selectedMode = FixMode.FIX_ONLY_NEEDS;
    
    public BatchFixDialog(Project project, FixPreviewData previewData) {
        this(project, previewData, false);
    }
    
    public BatchFixDialog(Project project, FixPreviewData previewData, boolean showOnlyNeedsFix) {
        super(project);
        this.previewData = previewData;
        this.showOnlyNeedsFix = showOnlyNeedsFix;
        
        setTitle(previewData.getTitle());
        init();
    }
    
    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(JBUI.Borders.empty(10));
        panel.setPreferredSize(new Dimension(600, 400));
        
        // Statistics panel
        JPanel statisticsPanel = createStatisticsPanel();
        panel.add(statisticsPanel, BorderLayout.NORTH);
        
        // Result list
        JPanel listPanel = createListPanel();
        panel.add(listPanel, BorderLayout.CENTER);
        
        // Hint
        if (previewData.hasNeedsFix()) {
            JPanel hintPanel = createHintPanel();
            panel.add(hintPanel, BorderLayout.SOUTH);
        }
        
        return panel;
    }
    
    /**
     * Create statistics panel
     */
    private JPanel createStatisticsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Statistics"));
        
        JPanel statsPanel = new JPanel(new GridLayout(0, 2, 10, 5));
        statsPanel.setBorder(JBUI.Borders.empty(5));
        
        // Total
        addStatRow(statsPanel, "📊 Total:", previewData.getTotalCount() + " TopicLine(s)");
        
        // Needs fix
        int needsFixCount = previewData.getNeedsFixCount();
        if (needsFixCount > 0) {
            addStatRow(statsPanel, "⚠️ Needs Fix:", 
                    needsFixCount + " item(s)", new Color(255, 140, 0));
        }
        
        // Synced
        int syncedCount = previewData.getSyncedCount();
        if (syncedCount > 0) {
            addStatRow(statsPanel, "✅ Synced:", 
                    syncedCount + " item(s)", new Color(0, 128, 0));
        }
        
        // Bookmark missing
        int bookmarkMissingCount = previewData.getBookmarkMissingCount();
        if (bookmarkMissingCount > 0) {
            addStatRow(statsPanel, "❌ Bookmark Missing:", 
                    bookmarkMissingCount + " item(s)", Color.RED);
        }
        
        // File not found
        int fileNotFoundCount = previewData.getFileNotFoundCount();
        if (fileNotFoundCount > 0) {
            addStatRow(statsPanel, "🚫 File Not Found:", 
                    fileNotFoundCount + " item(s)", Color.GRAY);
        }
        
        panel.add(statsPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * Add stat row
     */
    private void addStatRow(JPanel panel, String label, String value) {
        addStatRow(panel, label, value, null);
    }
    
    private void addStatRow(JPanel panel, String label, String value, Color valueColor) {
        JBLabel labelComponent = new JBLabel(label);
        panel.add(labelComponent);
        
        JBLabel valueComponent = new JBLabel(value);
        valueComponent.setFont(valueComponent.getFont().deriveFont(Font.BOLD));
        if (valueColor != null) {
            valueComponent.setForeground(valueColor);
        }
        panel.add(valueComponent);
    }
    
    /**
     * Create list panel
     */
    private JPanel createListPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Details"));
        
        // Create list
        DefaultListModel<LineFixResult> listModel = new DefaultListModel<>();
        
        // Decide which results to show
        if (showOnlyNeedsFix) {
            previewData.getNeedsFixResults().forEach(listModel::addElement);
        } else {
            previewData.getResults().forEach(listModel::addElement);
        }
        
        resultList = new JBList<>(listModel);
        resultList.setCellRenderer(new FixResultRenderer());
        resultList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        JBScrollPane scrollPane = new JBScrollPane(resultList);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // Add empty label if needed
        if (listModel.isEmpty()) {
            JBLabel emptyLabel = new JBLabel("No items need to be fixed");
            emptyLabel.setHorizontalAlignment(SwingConstants.CENTER);
            emptyLabel.setForeground(Color.GRAY);
            panel.add(emptyLabel, BorderLayout.SOUTH);
        }
        
        return panel;
    }
    
    /**
     * Create hint panel
     */
    private JPanel createHintPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(JBUI.Borders.empty(10, 0, 0, 0));
        
        JBLabel hintLabel = new JBLabel("💡 Tip: Code positions may change due to branch switch, Git operations or code editing");
        hintLabel.setForeground(Color.GRAY);
        panel.add(hintLabel, BorderLayout.CENTER);
        
        return panel;
    }
    
    @Override
    protected Action @Nullable [] createActions() {
        if (!previewData.hasNeedsFix()) {
            // No items need fix, only show close button
            return new Action[]{
                new DialogWrapperAction("Close") {
                    @Override
                    protected void doAction(java.awt.event.ActionEvent e) {
                        doCancelAction();
                    }
                }
            };
        }
        
        // Has items need fix, show fix options
        return new Action[]{
            getCancelAction(),
            new DialogWrapperAction("Fix Only Out of Sync (" + previewData.getNeedsFixCount() + ")") {
                @Override
                protected void doAction(java.awt.event.ActionEvent e) {
                    selectedMode = FixMode.FIX_ONLY_NEEDS;
                    doOKAction();
                }
            },
            new DialogWrapperAction("Resync All (" + previewData.getTotalCount() + ")") {
                @Override
                protected void doAction(java.awt.event.ActionEvent e) {
                    selectedMode = FixMode.FIX_ALL;
                    doOKAction();
                }
            }
        };
    }
    
    /**
     * Get selected fix mode
     */
    public FixMode getSelectedMode() {
        return selectedMode;
    }
}

package jp.kitabatakep.intellij.plugins.codereadingnote.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.util.ui.JBUI;
import jp.kitabatakep.intellij.plugins.codereadingnote.CodeReadingNoteBundle;
import jp.kitabatakep.intellij.plugins.codereadingnote.aiconfig.AIConfigMergeCategory;
import jp.kitabatakep.intellij.plugins.codereadingnote.aiconfig.AIConfigMergeItem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.List;

/**
 * Dialog that shows a merge preview for AI config pull operations.
 * Displays files grouped by category with per-file action selection.
 */
public class AIConfigMergePreviewDialog extends DialogWrapper {

    private final List<AIConfigMergeItem> items;
    private final JLabel summaryLabel;
    private MergeTableModel tableModel;

    public AIConfigMergePreviewDialog(@NotNull Project project, @NotNull List<AIConfigMergeItem> items) {
        super(project);
        this.items = items;
        this.summaryLabel = new JLabel();
        setTitle(CodeReadingNoteBundle.message("aiconfig.merge.preview.title"));
        setOKButtonText(CodeReadingNoteBundle.message("button.ok"));
        setCancelButtonText(CodeReadingNoteBundle.message("button.cancel"));
        init();
    }

    @Override
    @Nullable
    protected JComponent createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setPreferredSize(new Dimension(620, 400));

        // Bulk action buttons
        JPanel buttonBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        JButton acceptAllRemote = new JButton(CodeReadingNoteBundle.message("aiconfig.merge.bulk.accept.remote"));
        JButton keepAllLocal = new JButton(CodeReadingNoteBundle.message("aiconfig.merge.bulk.keep.local"));
        acceptAllRemote.addActionListener(e -> bulkSetAction(true));
        keepAllLocal.addActionListener(e -> bulkSetAction(false));
        buttonBar.add(acceptAllRemote);
        buttonBar.add(keepAllLocal);
        panel.add(buttonBar, BorderLayout.NORTH);

        // Table
        tableModel = new MergeTableModel(items);
        JTable table = new JTable(tableModel);
        table.setRowHeight(24);
        table.getColumnModel().getColumn(0).setPreferredWidth(300);
        table.getColumnModel().getColumn(1).setPreferredWidth(130);
        table.getColumnModel().getColumn(2).setPreferredWidth(130);

        // Category column renderer with color coding
        table.getColumnModel().getColumn(1).setCellRenderer(new CategoryRenderer());

        // Action column uses combo box
        JComboBox<String> actionCombo = new JComboBox<>(new String[]{
            CodeReadingNoteBundle.message("aiconfig.merge.action.take.remote"),
            CodeReadingNoteBundle.message("aiconfig.merge.action.keep.local"),
            CodeReadingNoteBundle.message("aiconfig.merge.action.add"),
            CodeReadingNoteBundle.message("aiconfig.merge.action.delete"),
            CodeReadingNoteBundle.message("aiconfig.merge.action.skip")
        });
        table.getColumnModel().getColumn(2).setCellEditor(new DefaultCellEditor(actionCombo));

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(JBUI.Borders.empty());
        panel.add(scrollPane, BorderLayout.CENTER);

        // Summary
        updateSummary();
        summaryLabel.setBorder(JBUI.Borders.empty(4, 4));
        summaryLabel.setFont(summaryLabel.getFont().deriveFont(Font.PLAIN, 11f));
        panel.add(summaryLabel, BorderLayout.SOUTH);

        return panel;
    }

    private void bulkSetAction(boolean acceptRemote) {
        for (AIConfigMergeItem item : items) {
            if (item.getCategory() == AIConfigMergeCategory.UNCHANGED) continue;
            if (acceptRemote) {
                switch (item.getCategory()) {
                    case NEW_REMOTE:
                        item.setUserAction(AIConfigMergeItem.Action.ADD);
                        break;
                    case DELETED_REMOTE:
                        item.setUserAction(AIConfigMergeItem.Action.DELETE);
                        break;
                    case REMOTE_CHANGED:
                    case BOTH_CHANGED:
                        item.setUserAction(AIConfigMergeItem.Action.TAKE_REMOTE);
                        break;
                    case LOCAL_CHANGED:
                        item.setUserAction(AIConfigMergeItem.Action.SKIP);
                        break;
                    default:
                        break;
                }
            } else {
                switch (item.getCategory()) {
                    case NEW_REMOTE:
                        item.setUserAction(AIConfigMergeItem.Action.SKIP);
                        break;
                    case DELETED_REMOTE:
                    case LOCAL_CHANGED:
                    case BOTH_CHANGED:
                        item.setUserAction(AIConfigMergeItem.Action.KEEP_LOCAL);
                        break;
                    case REMOTE_CHANGED:
                        item.setUserAction(AIConfigMergeItem.Action.KEEP_LOCAL);
                        break;
                    default:
                        break;
                }
            }
        }
        tableModel.fireTableDataChanged();
        updateSummary();
    }

    private void updateSummary() {
        int toUpdate = 0, toAdd = 0, conflicts = 0, unchanged = 0;
        for (AIConfigMergeItem item : items) {
            switch (item.getCategory()) {
                case NEW_REMOTE: toAdd++; break;
                case BOTH_CHANGED: conflicts++; break;
                case UNCHANGED: unchanged++; break;
                default: toUpdate++; break;
            }
        }
        summaryLabel.setText(CodeReadingNoteBundle.message("aiconfig.merge.summary",
            toUpdate, toAdd, conflicts, unchanged));
    }

    @NotNull
    public List<AIConfigMergeItem> getMergeItems() {
        return items;
    }

    // ========== Table Model ==========

    private class MergeTableModel extends AbstractTableModel {
        private final List<AIConfigMergeItem> data;
        private final String[] columnNames = {
            CodeReadingNoteBundle.message("aiconfig.merge.column.path"),
            CodeReadingNoteBundle.message("aiconfig.merge.column.category"),
            CodeReadingNoteBundle.message("aiconfig.merge.column.action")
        };

        MergeTableModel(List<AIConfigMergeItem> data) {
            this.data = data;
        }

        @Override
        public int getRowCount() {
            return data.size();
        }

        @Override
        public int getColumnCount() {
            return 3;
        }

        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }

        @Override
        public Object getValueAt(int row, int col) {
            AIConfigMergeItem item = data.get(row);
            switch (col) {
                case 0: return item.getRelativePath();
                case 1: return categoryDisplayName(item.getCategory());
                case 2: return actionDisplayName(item.getUserAction());
                default: return "";
            }
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            if (col != 2) return false;
            return data.get(row).getCategory() != AIConfigMergeCategory.UNCHANGED;
        }

        @Override
        public void setValueAt(Object value, int row, int col) {
            if (col == 2 && value instanceof String) {
                AIConfigMergeItem item = data.get(row);
                item.setUserAction(parseAction((String) value));
                fireTableCellUpdated(row, col);
                updateSummary();
            }
        }
    }

    // ========== Category renderer ==========

    private static class CategoryRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (!isSelected && value instanceof String) {
                String text = (String) value;
                if (text.equals(CodeReadingNoteBundle.message("aiconfig.merge.category.both.changed"))) {
                    c.setForeground(new Color(204, 120, 50));
                } else if (text.equals(CodeReadingNoteBundle.message("aiconfig.merge.category.new.remote"))) {
                    c.setForeground(new Color(98, 150, 85));
                } else if (text.equals(CodeReadingNoteBundle.message("aiconfig.merge.category.deleted.remote"))) {
                    c.setForeground(new Color(170, 80, 80));
                } else if (text.equals(CodeReadingNoteBundle.message("aiconfig.merge.category.unchanged"))) {
                    c.setForeground(UIManager.getColor("Label.disabledForeground"));
                } else {
                    c.setForeground(UIManager.getColor("Label.foreground"));
                }
            }
            return c;
        }
    }

    // ========== Display helpers ==========

    @NotNull
    private static String categoryDisplayName(@NotNull AIConfigMergeCategory cat) {
        switch (cat) {
            case NEW_REMOTE: return CodeReadingNoteBundle.message("aiconfig.merge.category.new.remote");
            case DELETED_REMOTE: return CodeReadingNoteBundle.message("aiconfig.merge.category.deleted.remote");
            case REMOTE_CHANGED: return CodeReadingNoteBundle.message("aiconfig.merge.category.remote.changed");
            case LOCAL_CHANGED: return CodeReadingNoteBundle.message("aiconfig.merge.category.local.changed");
            case BOTH_CHANGED: return CodeReadingNoteBundle.message("aiconfig.merge.category.both.changed");
            case UNCHANGED: return CodeReadingNoteBundle.message("aiconfig.merge.category.unchanged");
            default: return cat.name();
        }
    }

    @NotNull
    private static String actionDisplayName(@NotNull AIConfigMergeItem.Action action) {
        switch (action) {
            case TAKE_REMOTE: return CodeReadingNoteBundle.message("aiconfig.merge.action.take.remote");
            case KEEP_LOCAL: return CodeReadingNoteBundle.message("aiconfig.merge.action.keep.local");
            case ADD: return CodeReadingNoteBundle.message("aiconfig.merge.action.add");
            case DELETE: return CodeReadingNoteBundle.message("aiconfig.merge.action.delete");
            case SKIP: return CodeReadingNoteBundle.message("aiconfig.merge.action.skip");
            default: return action.name();
        }
    }

    @NotNull
    private static AIConfigMergeItem.Action parseAction(@NotNull String displayName) {
        if (displayName.equals(CodeReadingNoteBundle.message("aiconfig.merge.action.take.remote")))
            return AIConfigMergeItem.Action.TAKE_REMOTE;
        if (displayName.equals(CodeReadingNoteBundle.message("aiconfig.merge.action.keep.local")))
            return AIConfigMergeItem.Action.KEEP_LOCAL;
        if (displayName.equals(CodeReadingNoteBundle.message("aiconfig.merge.action.add")))
            return AIConfigMergeItem.Action.ADD;
        if (displayName.equals(CodeReadingNoteBundle.message("aiconfig.merge.action.delete")))
            return AIConfigMergeItem.Action.DELETE;
        if (displayName.equals(CodeReadingNoteBundle.message("aiconfig.merge.action.skip")))
            return AIConfigMergeItem.Action.SKIP;
        return AIConfigMergeItem.Action.SKIP;
    }
}

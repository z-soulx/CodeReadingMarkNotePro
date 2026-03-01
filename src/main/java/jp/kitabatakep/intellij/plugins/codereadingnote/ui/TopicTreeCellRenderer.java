package jp.kitabatakep.intellij.plugins.codereadingnote.ui;

import com.intellij.icons.AllIcons;
import com.intellij.ui.ColoredTreeCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import jp.kitabatakep.intellij.plugins.codereadingnote.CodeReadingNoteBundle;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.text.SimpleDateFormat;

/**
 * Custom tree cell renderer for TopicTree nodes
 */
public class TopicTreeCellRenderer extends ColoredTreeCellRenderer {
    
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy/MM/dd HH:mm");
    
    @Override
    public void customizeCellRenderer(@NotNull JTree tree, Object value, boolean selected,
                                    boolean expanded, boolean leaf, int row, boolean hasFocus) {
        
        if (!(value instanceof TopicTreeNode)) {
            return;
        }
        
        TopicTreeNode node = (TopicTreeNode) value;
        
        switch (node.getNodeType()) {
            case TOPIC:
                renderTopicNode(node, selected);
                break;
                
            case GROUP:
                renderGroupNode(node, selected);
                break;
                
            case TOPIC_LINE:
                renderTopicLineNode(node, selected);
                break;
                
            case UNGROUPED_LINES_FOLDER:
                renderUngroupedFolderNode(node, selected);
                break;
                
            case TRASH_BIN:
                renderTrashBinNode(node);
                break;
                
            case TRASHED_LINE:
                renderTrashedLineNode(node);
                break;
        }
    }
    
    private void renderTopicNode(TopicTreeNode node, boolean selected) {
        var topic = node.getTopic();
        if (topic == null) return;
        
        setIcon(AllIcons.Nodes.Folder);
        append(topic.name(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
        
        // Add timestamp and line count
        String info = " (" + DATE_FORMAT.format(topic.updatedAt()) + 
                     ", " + CodeReadingNoteBundle.message("renderer.lines.count", topic.getTotalLineCount()) + ")";
        append(info, SimpleTextAttributes.GRAY_ATTRIBUTES);
    }
    
    private void renderGroupNode(TopicTreeNode node, boolean selected) {
        var group = node.getGroup();
        if (group == null) return;
        
        // Use folder icon for groups
        setIcon(AllIcons.Nodes.Folder);
        
        append(group.name(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
        
        // Add line count
        String info = " (" + CodeReadingNoteBundle.message("renderer.lines.count", group.getLineCount()) + ")";
        append(info, SimpleTextAttributes.GRAY_ATTRIBUTES);
        
        // Add note if present
        if (!group.note().isEmpty()) {
            append(" - " + group.note(), SimpleTextAttributes.GRAY_ITALIC_ATTRIBUTES);
        }
    }
    
    private void renderTopicLineNode(TopicTreeNode node, boolean selected) {
        var topicLine = node.getTopicLine();
        if (topicLine == null) return;
        
        // Use simple validity check to avoid SlowOperations on EDT
        // Note: isValidWithRefresh() can trigger file index checks which are prohibited on EDT
        boolean isValid = topicLine.isValid();
        
        // Use different icons based on file validity
        if (isValid) {
            setIcon(AllIcons.FileTypes.Text);
        } else {
            setIcon(AllIcons.General.Warning);
        }
        
        // Show file name and line number
        String fileName = topicLine.pathForDisplay();
        if (fileName.contains("/") || fileName.contains("\\")) {
            fileName = fileName.substring(Math.max(fileName.lastIndexOf("/"), fileName.lastIndexOf("\\")) + 1);
        }
        
        append(fileName, SimpleTextAttributes.REGULAR_ATTRIBUTES);
        append(":" + (topicLine.line() + 1), SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
        
        // Add note if present
        if (!topicLine.note().isEmpty()) {
            append(" - " + topicLine.note(), SimpleTextAttributes.GRAY_ATTRIBUTES);
        }
        
        // Show warning for invalid files
        if (!isValid) {
            append(" " + CodeReadingNoteBundle.message("tree.file.not.found"), SimpleTextAttributes.ERROR_ATTRIBUTES);
        }
    }
    
    private void renderUngroupedFolderNode(TopicTreeNode node, boolean selected) {
        setIcon(AllIcons.Nodes.Folder);
        
        append(CodeReadingNoteBundle.message("tree.ungrouped.lines"), SimpleTextAttributes.GRAY_ATTRIBUTES);
        
        int childCount = node.getChildCount();
        if (childCount > 0) {
            append(" (" + childCount + ")", SimpleTextAttributes.GRAY_ATTRIBUTES);
        }
    }
    
    private void renderTrashBinNode(TopicTreeNode node) {
        setIcon(AllIcons.Actions.GC);
        append(CodeReadingNoteBundle.message("trash.bin.name"), SimpleTextAttributes.GRAY_ATTRIBUTES);
        int childCount = node.getChildCount();
        append("  " + childCount, SimpleTextAttributes.GRAY_ATTRIBUTES);
    }
    
    private void renderTrashedLineNode(TopicTreeNode node) {
        var trashedLine = node.getTrashedLine();
        if (trashedLine == null) return;
        
        setIcon(AllIcons.General.Warning);
        var topicLine = trashedLine.getLine();
        String fileName = topicLine.pathForDisplay();
        if (fileName.contains("/") || fileName.contains("\\")) {
            fileName = fileName.substring(Math.max(fileName.lastIndexOf("/"), fileName.lastIndexOf("\\")) + 1);
        }
        
        append(fileName, SimpleTextAttributes.GRAYED_ATTRIBUTES);
        append(":" + (topicLine.line() + 1), SimpleTextAttributes.GRAYED_BOLD_ATTRIBUTES);
        
        if (!topicLine.note().isEmpty()) {
            append(" - " + topicLine.note(), SimpleTextAttributes.GRAY_ITALIC_ATTRIBUTES);
        }
        
        append(" [" + trashedLine.getOriginalTopicName() + "]", SimpleTextAttributes.GRAY_SMALL_ATTRIBUTES);
    }
}

package jp.kitabatakep.intellij.plugins.codereadingnote.ui.fix;

import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.ui.JBUI;
import jp.kitabatakep.intellij.plugins.codereadingnote.TopicLine;

import javax.swing.*;
import java.awt.*;

/**
 * LineFixResult 的列表渲染器
 * Displays: [Icon] [Topic] > Group  FileName:Line (Status)
 */
public class FixResultRenderer extends JPanel implements ListCellRenderer<LineFixResult> {
    
    private final SimpleColoredComponent textComponent;
    private final JLabel iconLabel;
    
    public FixResultRenderer() {
        setLayout(new BorderLayout(5, 0));
        setBorder(JBUI.Borders.empty(2, 5));
        
        iconLabel = new JLabel();
        iconLabel.setBorder(JBUI.Borders.emptyRight(5));
        add(iconLabel, BorderLayout.WEST);
        
        textComponent = new SimpleColoredComponent();
        add(textComponent, BorderLayout.CENTER);
    }
    
    @Override
    public Component getListCellRendererComponent(
            JList<? extends LineFixResult> list,
            LineFixResult value,
            int index,
            boolean isSelected,
            boolean cellHasFocus) {
        
        // 设置背景色
        if (isSelected) {
            setBackground(list.getSelectionBackground());
            textComponent.setBackground(list.getSelectionBackground());
        } else {
            setBackground(list.getBackground());
            textComponent.setBackground(list.getBackground());
        }
        
        // 设置图标
        iconLabel.setIcon(value.getIcon());
        
        // 清空之前的文本
        textComponent.clear();
        
        // Render Topic/Group prefix
        renderTopicGroupPrefix(value);
        
        // 根据状态设置不同的文本样式
        switch (value.getStatus()) {
            case SYNCED:
                renderSyncedResult(value);
                break;
            case NEEDS_FIX:
                renderNeedsFixResult(value);
                break;
            case BOOKMARK_MISSING:
                renderBookmarkMissingResult(value);
                break;
            case FILE_NOT_FOUND:
                renderFileNotFoundResult(value);
                break;
        }
        
        // Render note preview
        renderNotePreview(value);
        
        // 设置工具提示
        setToolTipText(value.getHtmlText());
        
        return this;
    }
    
    /**
     * Render [Topic] > Group prefix
     */
    private void renderTopicGroupPrefix(LineFixResult result) {
        TopicLine topicLine = result.getTopicLine();
        
        // Topic name
        if (topicLine.topic() != null) {
            textComponent.append("[", SimpleTextAttributes.GRAYED_ATTRIBUTES);
            textComponent.append(topicLine.topic().name(), 
                    new SimpleTextAttributes(SimpleTextAttributes.STYLE_BOLD, new Color(70, 130, 180)));
            textComponent.append("]", SimpleTextAttributes.GRAYED_ATTRIBUTES);
        }
        
        // Group name
        if (topicLine.hasGroup()) {
            textComponent.append(" > ", SimpleTextAttributes.GRAYED_ATTRIBUTES);
            textComponent.append(topicLine.getGroupName(), 
                    new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, new Color(100, 149, 237)));
        }
        
        textComponent.append("  ", SimpleTextAttributes.REGULAR_ATTRIBUTES);
    }
    
    /**
     * Render note preview (short)
     */
    private void renderNotePreview(LineFixResult result) {
        String note = result.getNote();
        if (note != null && !note.isEmpty()) {
            String preview = note.length() > 25 ? note.substring(0, 25) + "..." : note;
            textComponent.append("  - ", SimpleTextAttributes.GRAYED_ATTRIBUTES);
            textComponent.append(preview, SimpleTextAttributes.GRAYED_SMALL_ATTRIBUTES);
        }
    }
    
    /**
     * Render synced result
     */
    private void renderSyncedResult(LineFixResult result) {
        textComponent.append(result.getFileName(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
        textComponent.append(":", SimpleTextAttributes.GRAY_ATTRIBUTES);
        textComponent.append(String.valueOf(result.getOldLine()), SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
        textComponent.append(" (Synced)", SimpleTextAttributes.GRAYED_ATTRIBUTES);
    }
    
    /**
     * Render needs fix result
     */
    private void renderNeedsFixResult(LineFixResult result) {
        textComponent.append(result.getFileName(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
        textComponent.append(":", SimpleTextAttributes.GRAY_ATTRIBUTES);
        
        // Old line number (current line)
        textComponent.append(String.valueOf(result.getOldLine()), 
                SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
        
        // Arrow
        textComponent.append(" → ", new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, 
                new Color(255, 165, 0))); // Orange arrow
        
        // New line number (Bookmark position)
        textComponent.append(String.valueOf(result.getNewLine()), 
                new SimpleTextAttributes(SimpleTextAttributes.STYLE_BOLD, 
                        new Color(0, 128, 0))); // Green new line
        
        // Show offset
        int offset = result.getNewLine() - result.getOldLine();
        String offsetText = String.format(" (%+d)", offset);
        textComponent.append(offsetText, SimpleTextAttributes.GRAYED_SMALL_ATTRIBUTES);
    }
    
    /**
     * Render bookmark missing result
     */
    private void renderBookmarkMissingResult(LineFixResult result) {
        textComponent.append(result.getFileName(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
        textComponent.append(":", SimpleTextAttributes.GRAY_ATTRIBUTES);
        textComponent.append(String.valueOf(result.getOldLine()), SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
        textComponent.append(" (Bookmark Missing)", 
                new SimpleTextAttributes(SimpleTextAttributes.STYLE_ITALIC, Color.RED));
    }
    
    /**
     * Render file not found result
     */
    private void renderFileNotFoundResult(LineFixResult result) {
        textComponent.append(result.getFileName(), 
                new SimpleTextAttributes(SimpleTextAttributes.STYLE_ITALIC, Color.GRAY));
        textComponent.append(":", SimpleTextAttributes.GRAY_ATTRIBUTES);
        textComponent.append(String.valueOf(result.getOldLine()), SimpleTextAttributes.GRAYED_ATTRIBUTES);
        textComponent.append(" (File Not Found)", 
                new SimpleTextAttributes(SimpleTextAttributes.STYLE_ITALIC, Color.GRAY));
    }
}

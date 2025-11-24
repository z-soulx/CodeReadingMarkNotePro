package jp.kitabatakep.intellij.plugins.codereadingnote.ui.fix;

import com.intellij.icons.AllIcons;
import jp.kitabatakep.intellij.plugins.codereadingnote.TopicLine;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * 表示单个 TopicLine 的修复结果信息
 */
public class LineFixResult {
    private final TopicLine topicLine;
    private final Integer bookmarkLine;  // Bookmark 当前所在行号，null 表示 Bookmark 丢失
    private final boolean needsFix;
    private final FixStatus status;
    
    public enum FixStatus {
        SYNCED,           // 已同步，无需修复
        NEEDS_FIX,        // 需要修复
        BOOKMARK_MISSING, // Bookmark 丢失
        FILE_NOT_FOUND    // 文件不存在
    }
    
    public LineFixResult(TopicLine topicLine, @Nullable Integer bookmarkLine) {
        this.topicLine = topicLine;
        this.bookmarkLine = bookmarkLine;
        
        // 判断状态
        if (topicLine.file() == null || !topicLine.isValid()) {
            this.status = FixStatus.FILE_NOT_FOUND;
            this.needsFix = false;
        } else if (bookmarkLine == null) {
            this.status = FixStatus.BOOKMARK_MISSING;
            this.needsFix = false;
        } else if (!bookmarkLine.equals(topicLine.line())) {
            this.status = FixStatus.NEEDS_FIX;
            this.needsFix = true;
        } else {
            this.status = FixStatus.SYNCED;
            this.needsFix = false;
        }
    }
    
    public TopicLine getTopicLine() {
        return topicLine;
    }
    
    public Integer getBookmarkLine() {
        return bookmarkLine;
    }
    
    public int getOldLine() {
        return topicLine.line();
    }
    
    public int getNewLine() {
        return bookmarkLine != null ? bookmarkLine : topicLine.line();
    }
    
    public boolean needsFix() {
        return needsFix;
    }
    
    public FixStatus getStatus() {
        return status;
    }
    
    public String getFileName() {
        if (topicLine.file() != null) {
            return topicLine.file().getName();
        }
        return "<File Not Found>";
    }
    
    public String getFilePath() {
        return topicLine.pathForDisplay();
    }
    
    public String getNote() {
        return topicLine.note();
    }
    
    /**
     * 获取状态图标
     */
    public Icon getIcon() {
        switch (status) {
            case SYNCED:
                return AllIcons.General.InspectionsOK;  // ✅ 绿色对勾
            case NEEDS_FIX:
                return AllIcons.General.Warning;  // ⚠️ 黄色警告
            case BOOKMARK_MISSING:
                return AllIcons.General.Error;  // ❌ 红色错误
            case FILE_NOT_FOUND:
                return AllIcons.General.BalloonError;  // 文件不存在
            default:
                return AllIcons.General.Information;
        }
    }
    
    /**
     * Get display text (short version)
     */
    public String getDisplayText() {
        String fileName = getFileName();
        
        switch (status) {
            case SYNCED:
                return String.format("✅ %s:%d (Synced)", fileName, getOldLine());
            case NEEDS_FIX:
                return String.format("⚠️ %s:%d → %d", fileName, getOldLine(), getNewLine());
            case BOOKMARK_MISSING:
                return String.format("❌ %s:%d (Bookmark Missing)", fileName, getOldLine());
            case FILE_NOT_FOUND:
                return String.format("🚫 %s:%d (File Not Found)", fileName, getOldLine());
            default:
                return fileName + ":" + getOldLine();
        }
    }
    
    /**
     * Get detailed display text (with path and note)
     */
    public String getDetailedText() {
        StringBuilder sb = new StringBuilder();
        sb.append(getDisplayText()).append("\n");
        sb.append("Path: ").append(getFilePath()).append("\n");
        if (!getNote().isEmpty()) {
            sb.append("Note: ").append(getNote().substring(0, Math.min(50, getNote().length())));
            if (getNote().length() > 50) {
                sb.append("...");
            }
        }
        return sb.toString();
    }
    
    /**
     * Get HTML formatted display text (for tooltip)
     */
    public String getHtmlText() {
        StringBuilder html = new StringBuilder("<html>");
        
        html.append("<b>File:</b> ").append(getFileName()).append("<br>");
        html.append("<b>Path:</b> ").append(getFilePath()).append("<br>");
        
        switch (status) {
            case SYNCED:
                html.append("<b>Status:</b> <font color='green'>✓ Synced</font><br>");
                html.append("<b>Line:</b> ").append(getOldLine());
                break;
            case NEEDS_FIX:
                html.append("<b>Status:</b> <font color='orange'>⚠ Needs Fix</font><br>");
                html.append("<b>Current Line:</b> ").append(getOldLine()).append("<br>");
                html.append("<b>Bookmark Position:</b> ").append(getNewLine()).append("<br>");
                html.append("<b>Offset:</b> ").append(getNewLine() - getOldLine()).append(" lines");
                break;
            case BOOKMARK_MISSING:
                html.append("<b>Status:</b> <font color='red'>✗ Bookmark Missing</font><br>");
                html.append("<b>Line:</b> ").append(getOldLine());
                break;
            case FILE_NOT_FOUND:
                html.append("<b>Status:</b> <font color='red'>✗ File Not Found</font><br>");
                html.append("<b>Line:</b> ").append(getOldLine());
                break;
        }
        
        if (!getNote().isEmpty()) {
            html.append("<br><b>Note:</b> ");
            String note = getNote();
            if (note.length() > 100) {
                html.append(note.substring(0, 100)).append("...");
            } else {
                html.append(note);
            }
        }
        
        html.append("</html>");
        return html.toString();
    }
    
    @Override
    public String toString() {
        return getDisplayText();
    }
}


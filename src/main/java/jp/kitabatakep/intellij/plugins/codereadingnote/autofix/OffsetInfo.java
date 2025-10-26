package jp.kitabatakep.intellij.plugins.codereadingnote.autofix;

import jp.kitabatakep.intellij.plugins.codereadingnote.TopicLine;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * TopicLine 错位信息
 */
public class OffsetInfo {
    private final TopicLine topicLine;
    private final OffsetStatus status;
    private final int originalLine;
    private final int newLine;
    private final int offset;
    private final String reason;
    private final int bookmarkLine;
    
    OffsetInfo(TopicLine topicLine, OffsetStatus status, int originalLine, int newLine, int offset, String reason, int bookmarkLine) {
        this.topicLine = topicLine;
        this.status = status;
        this.originalLine = originalLine;
        this.newLine = newLine;
        this.offset = offset;
        this.reason = reason;
        this.bookmarkLine = bookmarkLine;
    }
    
    public static OffsetInfo synced(@NotNull TopicLine line) {
        return new OffsetInfo(line, OffsetStatus.SYNCED, line.line(), line.line(), 0, null, line.line());
    }
    
    public static OffsetInfo offset(@NotNull TopicLine line, int originalLine, int newLine) {
        return new OffsetInfo(line, OffsetStatus.OFFSET, originalLine, newLine, newLine - originalLine, null, newLine);
    }
    
    public static OffsetInfo bookmarkMissing(@NotNull TopicLine line, @Nullable String reason, int bookmarkLine) {
        return new OffsetInfo(line, OffsetStatus.BOOKMARK_MISSING, line.line(), -1, 0, reason, bookmarkLine);
    }
 
    public static OffsetInfo fileMissing(@NotNull TopicLine line) {
        return new OffsetInfo(line, OffsetStatus.FILE_MISSING, line.line(), -1, 0, "文件不存在", -1);
    }
 
    public static OffsetInfo unknown(@NotNull TopicLine line, @Nullable String reason) {
        return new OffsetInfo(line, OffsetStatus.UNKNOWN, line.line(), -1, 0, reason, -1);
    }
 
    public static OffsetInfo missingBookmarkUid(@NotNull TopicLine line) {
        return new OffsetInfo(line, OffsetStatus.BOOKMARK_MISSING, line.line(), -1, 0, "缺少 Bookmark UID", -1);
    }
 
    public static OffsetInfo rebuiltBookmark(@NotNull TopicLine line, int bookmarkLine) {
        return new OffsetInfo(line, OffsetStatus.OFFSET, line.line(), bookmarkLine, bookmarkLine - line.line(), "已重建 Bookmark", bookmarkLine);
    }
    
    public TopicLine getTopicLine() {
        return topicLine;
    }
    
    public OffsetStatus getStatus() {
        return status;
    }
    
    public int getOriginalLine() {
        return originalLine;
    }
    
    public int getNewLine() {
        return newLine;
    }
    
    public int getOffset() {
        return offset;
    }
    
    @Nullable
    public String getReason() {
        return reason;
    }
    
    public int getBookmarkLine() {
        return bookmarkLine;
    }
    
    /**
     * 获取错位描述文本
     * 例如: "128 → 134 (+6)"
     */
    public String getOffsetDescription() {
        if (status == OffsetStatus.OFFSET) {
            String arrow = offset > 0 ? "→" : "←";
            String sign = offset > 0 ? "+" : "";
            return String.format("%d %s %d (%s%d)", originalLine, arrow, newLine, sign, offset);
        } else if (status == OffsetStatus.BOOKMARK_MISSING) {
            if (reason != null && !reason.isEmpty()) {
                return reason;
            }
            if (bookmarkLine >= 0) {
                return String.format("Bookmark 丢失 (原行 %d)", bookmarkLine + 1);
            }
            return "Bookmark 丢失";
        } else if (status == OffsetStatus.FILE_MISSING) {
            return "文件不存在";
        }
        return reason != null ? reason : "";
    }
    
    /**
     * 获取简短描述
     */
    public String getShortDescription() {
        switch (status) {
            case SYNCED:
                return "已同步";
            case OFFSET:
                return getOffsetDescription();
            case BOOKMARK_MISSING:
                return reason != null ? reason : "Bookmark 丢失";
            case FILE_MISSING:
                return "文件不存在";
            default:
                return reason != null ? reason : "未知状态";
        }
    }
    
    @Override
    public String toString() {
        return String.format("OffsetInfo{status=%s, line=%d->%d, offset=%d, reason=%s}", 
                            status, originalLine, newLine, offset, reason);
    }
}


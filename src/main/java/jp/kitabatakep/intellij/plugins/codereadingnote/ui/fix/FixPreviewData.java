package jp.kitabatakep.intellij.plugins.codereadingnote.ui.fix;

import jp.kitabatakep.intellij.plugins.codereadingnote.Topic;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 修复预览数据容器，包含修复结果列表和统计信息
 */
public class FixPreviewData {
    private final List<LineFixResult> results;
    private final Topic topic;  // 可选，用于单个 Topic 的修复
    private final String title;
    
    public FixPreviewData(List<LineFixResult> results, Topic topic, String title) {
        this.results = new ArrayList<>(results);
        this.topic = topic;
        this.title = title;
    }
    
    public FixPreviewData(List<LineFixResult> results, String title) {
        this(results, null, title);
    }
    
    public List<LineFixResult> getResults() {
        return results;
    }
    
    public Topic getTopic() {
        return topic;
    }
    
    public String getTitle() {
        return title;
    }
    
    /**
     * 获取所有需要修复的结果
     */
    public List<LineFixResult> getNeedsFixResults() {
        return results.stream()
                .filter(LineFixResult::needsFix)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取已同步的结果
     */
    public List<LineFixResult> getSyncedResults() {
        return results.stream()
                .filter(r -> r.getStatus() == LineFixResult.FixStatus.SYNCED)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取 Bookmark 丢失的结果
     */
    public List<LineFixResult> getBookmarkMissingResults() {
        return results.stream()
                .filter(r -> r.getStatus() == LineFixResult.FixStatus.BOOKMARK_MISSING)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取文件不存在的结果
     */
    public List<LineFixResult> getFileNotFoundResults() {
        return results.stream()
                .filter(r -> r.getStatus() == LineFixResult.FixStatus.FILE_NOT_FOUND)
                .collect(Collectors.toList());
    }
    
    /**
     * 总数
     */
    public int getTotalCount() {
        return results.size();
    }
    
    /**
     * 需要修复的数量
     */
    public int getNeedsFixCount() {
        return (int) results.stream()
                .filter(LineFixResult::needsFix)
                .count();
    }
    
    /**
     * 已同步的数量
     */
    public int getSyncedCount() {
        return (int) results.stream()
                .filter(r -> r.getStatus() == LineFixResult.FixStatus.SYNCED)
                .count();
    }
    
    /**
     * Bookmark 丢失的数量
     */
    public int getBookmarkMissingCount() {
        return (int) results.stream()
                .filter(r -> r.getStatus() == LineFixResult.FixStatus.BOOKMARK_MISSING)
                .count();
    }
    
    /**
     * 文件不存在的数量
     */
    public int getFileNotFoundCount() {
        return (int) results.stream()
                .filter(r -> r.getStatus() == LineFixResult.FixStatus.FILE_NOT_FOUND)
                .count();
    }
    
    /**
     * 是否有需要修复的项
     */
    public boolean hasNeedsFix() {
        return getNeedsFixCount() > 0;
    }
    
    /**
     * 是否为空
     */
    public boolean isEmpty() {
        return results.isEmpty();
    }
    
    /**
     * Get summary statistics
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("Total: ").append(getTotalCount()).append(" TopicLine(s)\n");
        
        int needsFix = getNeedsFixCount();
        int synced = getSyncedCount();
        int bookmarkMissing = getBookmarkMissingCount();
        int fileNotFound = getFileNotFoundCount();
        
        if (needsFix > 0) {
            sb.append("⚠️ ").append(needsFix).append(" need(s) fix\n");
        }
        if (synced > 0) {
            sb.append("✅ ").append(synced).append(" synced\n");
        }
        if (bookmarkMissing > 0) {
            sb.append("❌ ").append(bookmarkMissing).append(" bookmark(s) missing\n");
        }
        if (fileNotFound > 0) {
            sb.append("🚫 ").append(fileNotFound).append(" file(s) not found\n");
        }
        
        return sb.toString().trim();
    }
    
    /**
     * Get short summary
     */
    public String getShortSummary() {
        int needsFix = getNeedsFixCount();
        int total = getTotalCount();
        
        if (needsFix == 0) {
            return String.format("All %d synced", total);
        } else {
            return String.format("%d/%d needs fix", needsFix, total);
        }
    }
    
    /**
     * Get HTML formatted summary
     */
    public String getHtmlSummary() {
        StringBuilder html = new StringBuilder("<html><body>");
        
        html.append("<b>Total:</b> ").append(getTotalCount()).append(" TopicLine(s)<br><br>");
        
        int needsFix = getNeedsFixCount();
        int synced = getSyncedCount();
        int bookmarkMissing = getBookmarkMissingCount();
        int fileNotFound = getFileNotFoundCount();
        
        if (needsFix > 0) {
            html.append("<font color='orange'>⚠️ ")
                .append(needsFix).append(" need(s) fix</font><br>");
        }
        if (synced > 0) {
            html.append("<font color='green'>✓ ")
                .append(synced).append(" synced</font><br>");
        }
        if (bookmarkMissing > 0) {
            html.append("<font color='red'>✗ ")
                .append(bookmarkMissing).append(" bookmark(s) missing</font><br>");
        }
        if (fileNotFound > 0) {
            html.append("<font color='gray'>🚫 ")
                .append(fileNotFound).append(" file(s) not found</font><br>");
        }
        
        html.append("</body></html>");
        return html.toString();
    }
}


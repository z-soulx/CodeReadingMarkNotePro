package jp.kitabatakep.intellij.plugins.codereadingnote.autofix;

import com.intellij.ide.bookmark.Bookmark;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import jp.kitabatakep.intellij.plugins.codereadingnote.CodeReadingNoteService;
import jp.kitabatakep.intellij.plugins.codereadingnote.Topic;
import jp.kitabatakep.intellij.plugins.codereadingnote.TopicLine;
import jp.kitabatakep.intellij.plugins.codereadingnote.remark.BookmarkUtils;
import jp.kitabatakep.intellij.plugins.codereadingnote.remark.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * TopicLine 行号错位检测器
 */
public class LineOffsetDetector {
    private static final Logger LOG = Logger.getInstance(LineOffsetDetector.class);
    private static final LineOffsetDetector INSTANCE = new LineOffsetDetector();
    
    // 缓存：避免频繁检测
    private final Map<Project, Map<TopicLine, OffsetInfo>> cacheMap = new HashMap<>();
    private final Map<Project, Long> lastDetectionTime = new HashMap<>();
    private static final long CACHE_DURATION_MS = 10000; // 10秒缓存
    
    public static LineOffsetDetector getInstance() {
        return INSTANCE;
    }
    
    private LineOffsetDetector() {}
    
    /**
     * 检测单个 TopicLine 的错位状态
     */
    public OffsetInfo detectOffset(@NotNull Project project, @NotNull TopicLine topicLine) {
        try {
            // 1. 检查文件是否存在
            if (topicLine.file() == null || !topicLine.file().isValid()) {
                return OffsetInfo.fileMissing(topicLine);
            }
            
            // 2. 检查是否有 bookmarkUid
            String uid = topicLine.getBookmarkUid();
            if (StringUtils.isEmpty(uid)) {
                return OffsetInfo.missingBookmarkUid(topicLine);
            }
            
            // 3. 查找对应的 Bookmark
            Bookmark bookmark = BookmarkUtils.machBookmark(topicLine, project);
            if (bookmark == null) {
                return OffsetInfo.bookmarkMissing(topicLine, "Bookmark 已被删除或不存在", topicLine.line());
            }
            
            // 4. 获取 Bookmark 的当前行号
            Object lineAttr = bookmark.getAttributes().get("line");
            if (lineAttr == null) {
                return OffsetInfo.bookmarkMissing(topicLine, "Bookmark 行号属性缺失", topicLine.line());
            }
            
            int bookmarkLine;
            try {
                bookmarkLine = Integer.parseInt(lineAttr.toString());
            } catch (NumberFormatException e) {
                LOG.warn("Invalid bookmark line number: " + lineAttr, e);
                return OffsetInfo.unknown(topicLine, "Bookmark 行号格式错误");
            }
            
            // 5. 比对行号
            int topicLineNumber = topicLine.line();
            if (topicLineNumber == bookmarkLine) {
                return OffsetInfo.synced(topicLine);
            } else {
                return OffsetInfo.offset(topicLine, topicLineNumber, bookmarkLine);
            }
            
        } catch (Exception e) {
            LOG.error("Error detecting offset for TopicLine: " + topicLine, e);
            return OffsetInfo.unknown(topicLine, "检测出错: " + e.getMessage());
        }
    }
    
    /**
     * 检测 Topic 下所有 TopicLine 的错位状态
     */
    public Map<TopicLine, OffsetInfo> detectTopic(@NotNull Project project, @NotNull Topic topic) {
        List<TopicLine> lines = topic.getLines();
        return detectLines(project, lines);
    }
    
    /**
     * 检测所有 TopicLine 的错位状态
     */
    public Map<TopicLine, OffsetInfo> detectAll(@NotNull Project project) {
        return detectAll(project, false);
    }
    
    /**
     * 检测所有 TopicLine 的错位状态
     * @param forceRefresh 是否强制刷新缓存
     */
    public Map<TopicLine, OffsetInfo> detectAll(@NotNull Project project, boolean forceRefresh) {
        // 检查缓存
        if (!forceRefresh && shouldUseCache(project)) {
            Map<TopicLine, OffsetInfo> cached = cacheMap.get(project);
            if (cached != null) {
                LOG.debug("Using cached offset detection result");
                return new HashMap<>(cached);
            }
        }
        
        // 获取所有 TopicLine
        CodeReadingNoteService service = CodeReadingNoteService.getInstance(project);
        List<TopicLine> allLines = service.getTopicList().getTopics().stream()
                .flatMap(topic -> topic.getLines().stream())
                .collect(Collectors.toList());
        
        Map<TopicLine, OffsetInfo> result = detectLines(project, allLines);
        
        // 更新缓存
        cacheMap.put(project, result);
        lastDetectionTime.put(project, System.currentTimeMillis());
        
        return result;
    }
    
    /**
     * 批量检测多个 TopicLine
     */
    public Map<TopicLine, OffsetInfo> detectLines(@NotNull Project project, @NotNull List<TopicLine> lines) {
        Map<TopicLine, OffsetInfo> result = new HashMap<>();
        
        for (TopicLine line : lines) {
            OffsetInfo info = detectOffset(project, line);
            result.put(line, info);
        }
        
        return result;
    }
    
    /**
     * 获取错位统计信息
     */
    public OffsetStatistics getStatistics(@NotNull Project project) {
        Map<TopicLine, OffsetInfo> allOffsets = detectAll(project);
        return new OffsetStatistics(allOffsets);
    }
    
    /**
     * 清除缓存
     */
    public void clearCache(@NotNull Project project) {
        cacheMap.remove(project);
        lastDetectionTime.remove(project);
    }
    
    /**
     * 清除所有缓存
     */
    public void clearAllCache() {
        cacheMap.clear();
        lastDetectionTime.clear();
    }
    
    private boolean shouldUseCache(Project project) {
        Long lastTime = lastDetectionTime.get(project);
        if (lastTime == null) {
            return false;
        }
        
        long now = System.currentTimeMillis();
        return (now - lastTime) < CACHE_DURATION_MS;
    }
    
    /**
     * 错位统计信息
     */
    public static class OffsetStatistics {
        private final int total;
        private final int synced;
        private final int offset;
        private final int bookmarkMissing;
        private final int fileMissing;
        private final int unknown;
        
        public OffsetStatistics(Map<TopicLine, OffsetInfo> offsetMap) {
            this.total = offsetMap.size();
            
            Map<OffsetStatus, Long> counts = offsetMap.values().stream()
                    .collect(Collectors.groupingBy(OffsetInfo::getStatus, Collectors.counting()));
            
            this.synced = counts.getOrDefault(OffsetStatus.SYNCED, 0L).intValue();
            this.offset = counts.getOrDefault(OffsetStatus.OFFSET, 0L).intValue();
            this.bookmarkMissing = counts.getOrDefault(OffsetStatus.BOOKMARK_MISSING, 0L).intValue();
            this.fileMissing = counts.getOrDefault(OffsetStatus.FILE_MISSING, 0L).intValue();
            this.unknown = counts.getOrDefault(OffsetStatus.UNKNOWN, 0L).intValue();
        }
        
        public int getTotal() {
            return total;
        }
        
        public int getSynced() {
            return synced;
        }
        
        public int getOffset() {
            return offset;
        }
        
        public int getBookmarkMissing() {
            return bookmarkMissing;
        }
        
        public int getFileMissing() {
            return fileMissing;
        }
        
        public int getUnknown() {
            return unknown;
        }
        
        public int getSyncedPercentage() {
            return total == 0 ? 0 : (synced * 100 / total);
        }
        
        public boolean hasIssues() {
            return offset > 0 || bookmarkMissing > 0 || fileMissing > 0;
        }
        
        @Override
        public String toString() {
            return String.format("Total: %d | ✅ %d | ⚠️ %d | ❌ %d | 🚫 %d | ❓ %d",
                    total, synced, offset, bookmarkMissing, fileMissing, unknown);
        }
        
        public String toShortString() {
            return String.format("%d (✅%d ⚠️%d ❌%d)", total, synced, offset, bookmarkMissing + fileMissing);
        }
    }
}


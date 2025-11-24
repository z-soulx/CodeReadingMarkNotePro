package jp.kitabatakep.intellij.plugins.codereadingnote.operations;

import com.intellij.ide.bookmark.Bookmark;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import jp.kitabatakep.intellij.plugins.codereadingnote.CodeReadingNoteService;
import jp.kitabatakep.intellij.plugins.codereadingnote.Topic;
import jp.kitabatakep.intellij.plugins.codereadingnote.TopicLine;
import jp.kitabatakep.intellij.plugins.codereadingnote.TopicList;
import jp.kitabatakep.intellij.plugins.codereadingnote.remark.BookmarkUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for repairing missing bookmarks
 * Scans all TopicLines and recreates missing or orphaned bookmarks
 */
@Service(Service.Level.PROJECT)
public final class BookmarkRepairService {
    
    private static final Logger LOG = Logger.getInstance(BookmarkRepairService.class);
    
    private final Project project;
    
    public BookmarkRepairService(@NotNull Project project) {
        this.project = project;
    }
    
    /**
     * Scan and repair all bookmarks
     * 
     * @param autoFix Whether to automatically fix issues
     * @return Repair result
     */
    @NotNull
    public BookmarkRepairResult scanAndRepair(boolean autoFix) {
        LOG.info("Starting bookmark repair scan, autoFix=" + autoFix);
        
        BookmarkRepairResult result = new BookmarkRepairResult();
        CodeReadingNoteService noteService = CodeReadingNoteService.getInstance(project);
        TopicList topicList = noteService.getTopicList();
        
        if (topicList == null) {
            LOG.warn("TopicList is null");
            return result;
        }
        
        for (Topic topic : topicList.getTopics()) {
            for (TopicLine line : topic.getLines()) {
                BookmarkStatus status = checkBookmarkStatus(line);
                
                switch (status) {
                    case MISSING:
                        result.addMissing(line);
                        if (autoFix) {
                            if (repairBookmark(line, topic)) {
                                result.addRepaired(line);
                            } else {
                                result.addFailed(line);
                            }
                        }
                        break;
                        
                    case ORPHANED_UUID:
                        result.addOrphaned(line);
                        if (autoFix) {
                            if (recreateBookmark(line, topic)) {
                                result.addRepaired(line);
                            } else {
                                result.addFailed(line);
                            }
                        }
                        break;
                        
                    case OK:
                        result.addHealthy(line);
                        break;
                }
            }
        }
        
        LOG.info(String.format("Bookmark repair scan completed: %s", result.getSummary()));
        return result;
    }
    
    /**
     * Repair bookmark for a single TopicLine
     */
    public boolean repairSingleLine(@NotNull TopicLine line, @NotNull Topic topic, 
                                   @NotNull BookmarkRepairResult result) {
        LOG.info("Repairing bookmark for single line: " + line.pathForDisplay());
        
        BookmarkStatus status = checkBookmarkStatus(line);
        
        switch (status) {
            case MISSING:
                result.addMissing(line);
                if (repairBookmark(line, topic)) {
                    result.addRepaired(line);
                    return true;
                } else {
                    result.addFailed(line);
                    return false;
                }
                
            case ORPHANED_UUID:
                result.addOrphaned(line);
                if (recreateBookmark(line, topic)) {
                    result.addRepaired(line);
                    return true;
                } else {
                    result.addFailed(line);
                    return false;
                }
                
            case OK:
                result.addHealthy(line);
                return true;
        }
        
        return false;
    }
    
    /**
     * Repair bookmarks for a specific topic
     */
    @NotNull
    public BookmarkRepairResult scanAndRepairTopic(@NotNull Topic topic, boolean autoFix) {
        LOG.info("Starting bookmark repair for topic: " + topic.name());
        
        BookmarkRepairResult result = new BookmarkRepairResult();
        
        for (TopicLine line : topic.getLines()) {
            BookmarkStatus status = checkBookmarkStatus(line);
            
            switch (status) {
                case MISSING:
                    result.addMissing(line);
                    if (autoFix) {
                        if (repairBookmark(line, topic)) {
                            result.addRepaired(line);
                        } else {
                            result.addFailed(line);
                        }
                    }
                    break;
                    
                case ORPHANED_UUID:
                    result.addOrphaned(line);
                    if (autoFix) {
                        if (recreateBookmark(line, topic)) {
                            result.addRepaired(line);
                        } else {
                            result.addFailed(line);
                        }
                    }
                    break;
                    
                case OK:
                    result.addHealthy(line);
                    break;
            }
        }
        
        LOG.info(String.format("Topic bookmark repair completed: %s", result.getSummary()));
        return result;
    }
    
    /**
     * Check bookmark status for a TopicLine
     */
    @NotNull
    private BookmarkStatus checkBookmarkStatus(@NotNull TopicLine line) {
        String uuid = line.getBookmarkUid();
        
        if (StringUtil.isEmpty(uuid)) {
            return BookmarkStatus.MISSING;
        }
        
        // Use existing machBookmark method to find bookmark by UUID
        Bookmark bookmark = BookmarkUtils.machBookmark(line, project);
        if (bookmark == null) {
            return BookmarkStatus.ORPHANED_UUID;
        }
        
        return BookmarkStatus.OK;
    }
    
    /**
     * Repair missing bookmark (TopicLine has no UUID)
     */
    private boolean repairBookmark(@NotNull TopicLine line, @NotNull Topic topic) {
        try {
            // Create bookmark using existing addBookmark method
            if (line.file() == null || !line.file().isValid()) {
                LOG.warn("Cannot repair bookmark: file not found or invalid");
                return false;
            }
            
            String uuid = java.util.UUID.randomUUID().toString();
            Bookmark bookmark = BookmarkUtils.addBookmark(
                project, 
                line.file(), 
                line.line(), 
                line.note(), 
                uuid
            );
            
            if (bookmark != null) {
                line.setBookmarkUid(uuid);
                topic.touch();
                LOG.info(String.format("Repaired bookmark for line: %s:%d with UUID: %s", 
                    line.pathForDisplay(), line.line(), uuid));
                return true;
            }
            
            return false;
        } catch (Exception e) {
            LOG.error("Failed to repair bookmark", e);
            return false;
        }
    }
    
    /**
     * Recreate bookmark (TopicLine has UUID but bookmark doesn't exist)
     */
    private boolean recreateBookmark(@NotNull TopicLine line, @NotNull Topic topic) {
        try {
            // Remove old bookmark if exists
            BookmarkUtils.removeMachBookmark(line, project);
            
            // Clear old UUID and create new bookmark
            line.setBookmarkUid(null);
            return repairBookmark(line, topic);
        } catch (Exception e) {
            LOG.error("Failed to recreate bookmark", e);
            return false;
        }
    }
    
    public static BookmarkRepairService getInstance(@NotNull Project project) {
        return project.getService(BookmarkRepairService.class);
    }
    
    /**
     * Bookmark status enum
     */
    private enum BookmarkStatus {
        OK,              // Bookmark is healthy
        MISSING,         // No UUID, needs creation
        ORPHANED_UUID    // Has UUID but bookmark doesn't exist
    }
    
    /**
     * Bookmark repair result
     */
    public static class BookmarkRepairResult {
        private final List<TopicLine> missing = new ArrayList<>();
        private final List<TopicLine> orphaned = new ArrayList<>();
        private final List<TopicLine> repaired = new ArrayList<>();
        private final List<TopicLine> failed = new ArrayList<>();
        private final List<TopicLine> healthy = new ArrayList<>();
        
        public void addMissing(TopicLine line) { missing.add(line); }
        public void addOrphaned(TopicLine line) { orphaned.add(line); }
        public void addRepaired(TopicLine line) { repaired.add(line); }
        public void addFailed(TopicLine line) { failed.add(line); }
        public void addHealthy(TopicLine line) { healthy.add(line); }
        
        public int getTotalCount() { return missing.size() + orphaned.size() + healthy.size(); }
        public int getMissingCount() { return missing.size(); }
        public int getOrphanedCount() { return orphaned.size(); }
        public int getRepairedCount() { return repaired.size(); }
        public int getFailedCount() { return failed.size(); }
        public int getHealthyCount() { return healthy.size(); }
        
        public boolean hasIssues() { return !missing.isEmpty() || !orphaned.isEmpty(); }
        public boolean hasFailures() { return !failed.isEmpty(); }
        
        @NotNull
        public String getSummary() {
            return String.format(
                "Total: %d | Missing: %d | Orphaned: %d | Repaired: %d | Failed: %d | Healthy: %d",
                getTotalCount(), missing.size(), orphaned.size(), 
                repaired.size(), failed.size(), healthy.size()
            );
        }
        
        @NotNull
        public List<TopicLine> getMissing() { return new ArrayList<>(missing); }
        @NotNull
        public List<TopicLine> getOrphaned() { return new ArrayList<>(orphaned); }
        @NotNull
        public List<TopicLine> getRepaired() { return new ArrayList<>(repaired); }
        @NotNull
        public List<TopicLine> getFailed() { return new ArrayList<>(failed); }
    }
}


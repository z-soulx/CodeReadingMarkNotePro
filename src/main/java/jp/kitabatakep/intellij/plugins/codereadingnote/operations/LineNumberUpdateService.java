package jp.kitabatakep.intellij.plugins.codereadingnote.operations;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.messages.MessageBus;
import jp.kitabatakep.intellij.plugins.codereadingnote.Topic;
import jp.kitabatakep.intellij.plugins.codereadingnote.TopicLine;
import jp.kitabatakep.intellij.plugins.codereadingnote.TopicNotifier;
import jp.kitabatakep.intellij.plugins.codereadingnote.remark.BookmarkUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for updating TopicLine line numbers
 * Includes validation, bookmark synchronization, etc.
 */
@Service(Service.Level.PROJECT)
public final class LineNumberUpdateService {
    
    private static final Logger LOG = Logger.getInstance(LineNumberUpdateService.class);
    
    private final Project project;
    
    public LineNumberUpdateService(@NotNull Project project) {
        this.project = project;
    }
    
    /**
     * Update a single TopicLine's line number
     */
    @NotNull
    public LineNumberUpdateResult updateLineNumber(@NotNull TopicLine line, 
                                                   int newLineNum, 
                                                   boolean updateBookmark) {
        return updateLineNumber(line, newLineNum, updateBookmark, true);
    }
    
    /**
     * Update a single TopicLine's line number with validation option
     */
    @NotNull
    public LineNumberUpdateResult updateLineNumber(@NotNull TopicLine line, 
                                                   int newLineNum, 
                                                   boolean updateBookmark,
                                                   boolean validate) {
        int oldLineNum = line.line();
        
        // Validate line number
        if (validate) {
            LineNumberValidation validation = validateLineNumber(line, newLineNum);
            if (!validation.isValid()) {
                LOG.warn(String.format("Invalid line number %d for file %s: %s", 
                    newLineNum, line.pathForDisplay(), validation.getErrorMessage()));
                return LineNumberUpdateResult.failure(validation.getErrorMessage());
            }
        }
        
        try {
            // 1. Remove old bookmark BEFORE changing line number (while line still has old line number)
            String uuid = line.getBookmarkUid();
            boolean shouldUpdateBookmark = updateBookmark && 
                                          line.file() != null && 
                                          line.file().isValid() && 
                                          !StringUtil.isEmpty(uuid);
            
            if (shouldUpdateBookmark) {
                BookmarkUtils.removeMachBookmark(line, project);
            }
            
            // 2. Remove remark at OLD line BEFORE changing line number
            if (line.file() != null && line.file().isValid()) {
                jp.kitabatakep.intellij.plugins.codereadingnote.remark.EditorUtils.removeLineCodeRemark(project, line);
            }
            
            // 3. Update TopicLine to new line number
            line.modifyLine(newLineNum);
            Topic topic = line.topic();
            if (topic != null) {
                topic.touch();
            }
            
            // 4. Create new bookmark at new line number
            if (shouldUpdateBookmark) {
                com.intellij.ide.bookmark.Bookmark newBookmark = BookmarkUtils.addBookmark(
                    project, 
                    line.file(), 
                    newLineNum, 
                    line.note(), 
                    uuid
                );
                
                if (newBookmark == null) {
                    LOG.warn("Failed to recreate bookmark at new line: " + newLineNum);
                }
            }
            
            // 5. Add remark at NEW line AFTER changing line number
            if (line.file() != null && line.file().isValid()) {
                jp.kitabatakep.intellij.plugins.codereadingnote.remark.EditorUtils.addLineCodeRemark(project, line);
            }
            
            // 6. Notify changes
            if (topic != null) {
                notifyLineNumberChanged(topic, line, oldLineNum, newLineNum);
            }
            
            LOG.info(String.format("Updated line number: %s:%d -> %d", 
                line.url(), oldLineNum, newLineNum));
            
            return LineNumberUpdateResult.success();
            
        } catch (Exception e) {
            LOG.error("Failed to update line number", e);
            return LineNumberUpdateResult.failure("Failed to update line number: " + e.getMessage());
        }
    }
    
    /**
     * Batch update TopicLine line numbers
     */
    @NotNull
    public BatchLineNumberUpdateResult batchUpdateLineNumbers(@NotNull List<TopicLine> lines,
                                                              @NotNull LineNumberAdjustment adjustment,
                                                              boolean updateBookmark,
                                                              boolean validate) {
        if (lines.isEmpty()) {
            return new BatchLineNumberUpdateResult();
        }
        
        LOG.info(String.format("Batch updating line numbers for %d lines", lines.size()));
        
        BatchLineNumberUpdateResult batchResult = new BatchLineNumberUpdateResult();
        
        for (TopicLine line : lines) {
            int newLineNum = adjustment.calculate(line.line());
            LineNumberUpdateResult result = updateLineNumber(line, newLineNum, updateBookmark, validate);
            
            if (result.isSuccess()) {
                batchResult.addSuccess(line);
            } else {
                batchResult.addFailure(line, result.getErrorMessage());
            }
        }
        
        LOG.info(String.format("Batch update completed: %d success, %d failed", 
            batchResult.getSuccessCount(), batchResult.getFailureCount()));
        
        return batchResult;
    }
    
    /**
     * Validate line number for a TopicLine
     */
    @NotNull
    public LineNumberValidation validateLineNumber(@NotNull TopicLine line, int lineNum) {
        VirtualFile file = line.file();
        if (file == null) {
            return LineNumberValidation.invalid("File not found");
        }
        return validateLineNumber(file, lineNum);
    }
    
    /**
     * Validate line number for a file
     */
    @NotNull
    public LineNumberValidation validateLineNumber(@NotNull VirtualFile file, int lineNum) {
        // Basic range check
        if (lineNum < 0) {
            return LineNumberValidation.invalid("Line number cannot be negative");
        }
        
        // Check if file is valid
        if (!file.isValid()) {
            return LineNumberValidation.invalid("File is not valid");
        }
        
        // Get document and check line range
        Document document = FileDocumentManager.getInstance().getDocument(file);
        if (document == null) {
            return LineNumberValidation.warning("Cannot validate line number for this file type");
        }
        
        int lineCount = document.getLineCount();
        if (lineNum >= lineCount) {
            return LineNumberValidation.invalid(
                String.format("Line %d is out of range (file has %d lines)", lineNum, lineCount)
            );
        }
        
        return LineNumberValidation.valid();
    }
    
    /**
     * Notify line number change
     */
    private void notifyLineNumberChanged(@NotNull Topic topic,
                                        @NotNull TopicLine line,
                                        int oldLineNum,
                                        int newLineNum) {
        MessageBus messageBus = project.getMessageBus();
        TopicNotifier publisher = messageBus.syncPublisher(TopicNotifier.TOPIC_NOTIFIER_TOPIC);
        // Use lineUpdated event instead of lineAdded, since we're updating an existing line, not adding a new one
        publisher.lineUpdated(topic, line, oldLineNum, newLineNum);
    }
    
    public static LineNumberUpdateService getInstance(@NotNull Project project) {
        return project.getService(LineNumberUpdateService.class);
    }
    
    /**
     * Line number adjustment functional interface
     */
    @FunctionalInterface
    public interface LineNumberAdjustment {
        int calculate(int oldLineNum);
        
        static LineNumberAdjustment addOffset(int offset) {
            return oldLineNum -> oldLineNum + offset;
        }
        
        static LineNumberAdjustment subtractOffset(int offset) {
            return oldLineNum -> Math.max(0, oldLineNum - offset);
        }
        
        static LineNumberAdjustment setSpecific(int value) {
            return oldLineNum -> value;
        }
    }
    
    /**
     * Line number validation result
     */
    public static class LineNumberValidation {
        private final boolean valid;
        private final String errorMessage;
        private final boolean warning;
        
        private LineNumberValidation(boolean valid, String errorMessage, boolean warning) {
            this.valid = valid;
            this.errorMessage = errorMessage;
            this.warning = warning;
        }
        
        public static LineNumberValidation valid() {
            return new LineNumberValidation(true, null, false);
        }
        
        public static LineNumberValidation invalid(String errorMessage) {
            return new LineNumberValidation(false, errorMessage, false);
        }
        
        public static LineNumberValidation warning(String message) {
            return new LineNumberValidation(true, message, true);
        }
        
        public boolean isValid() { return valid; }
        public boolean isWarning() { return warning; }
        @Nullable
        public String getErrorMessage() { return errorMessage; }
    }
    
    /**
     * Single line number update result
     */
    public static class LineNumberUpdateResult {
        private final boolean success;
        private final String errorMessage;
        
        private LineNumberUpdateResult(boolean success, String errorMessage) {
            this.success = success;
            this.errorMessage = errorMessage;
        }
        
        public static LineNumberUpdateResult success() {
            return new LineNumberUpdateResult(true, null);
        }
        
        public static LineNumberUpdateResult failure(String errorMessage) {
            return new LineNumberUpdateResult(false, errorMessage);
        }
        
        public boolean isSuccess() { return success; }
        @Nullable
        public String getErrorMessage() { return errorMessage; }
    }
    
    /**
     * Batch update result
     */
    public static class BatchLineNumberUpdateResult {
        private final List<TopicLine> successLines = new ArrayList<>();
        private final List<TopicLine> failedLines = new ArrayList<>();
        private final List<String> errorMessages = new ArrayList<>();
        
        public void addSuccess(TopicLine line) { successLines.add(line); }
        public void addFailure(TopicLine line, String errorMessage) {
            failedLines.add(line);
            errorMessages.add(errorMessage);
        }
        
        public int getSuccessCount() { return successLines.size(); }
        public int getFailureCount() { return failedLines.size(); }
        public boolean hasFailures() { return !failedLines.isEmpty(); }
        
        @NotNull
        public List<TopicLine> getSuccessLines() { return new ArrayList<>(successLines); }
        @NotNull
        public List<TopicLine> getFailedLines() { return new ArrayList<>(failedLines); }
        @NotNull
        public List<String> getErrorMessages() { return new ArrayList<>(errorMessages); }
        
        @NotNull
        public String getSummary() {
            return String.format("Success: %d | Failed: %d", 
                getSuccessCount(), getFailureCount());
        }
    }
}


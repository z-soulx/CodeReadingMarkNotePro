package jp.kitabatakep.intellij.plugins.codereadingnote.autofix;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 修复操作结果
 */
public class FixResult {
    private final boolean success;
    private final int fixedCount;
    private final int totalCount;
    private final int failedCount;
    private final long durationMs;
    private final String message;
    private final List<String> errors;
    
    private FixResult(boolean success, int fixedCount, int totalCount, int failedCount, 
                     long durationMs, String message, List<String> errors) {
        this.success = success;
        this.fixedCount = fixedCount;
        this.totalCount = totalCount;
        this.failedCount = failedCount;
        this.durationMs = durationMs;
        this.message = message;
        this.errors = errors != null ? errors : new ArrayList<>();
    }
    
    public static FixResult success(int fixedCount, int totalCount) {
        return new FixResult(true, fixedCount, totalCount, 0, 0, null, null);
    }
    
    public static FixResult success(int fixedCount, int totalCount, long durationMs) {
        return new FixResult(true, fixedCount, totalCount, 0, durationMs, null, null);
    }
    
    public static FixResult partialSuccess(int fixedCount, int totalCount, int failedCount, List<String> errors) {
        return new FixResult(true, fixedCount, totalCount, failedCount, 0, null, errors);
    }
    
    public static FixResult failed(@NotNull String message) {
        return new FixResult(false, 0, 0, 0, 0, message, null);
    }
    
    public static FixResult noActionNeeded() {
        return new FixResult(true, 0, 0, 0, 0, "没有需要修复的 TopicLine", null);
    }
    
    public static FixResult cancelled() {
        return new FixResult(false, 0, 0, 0, 0, "用户取消操作", null);
    }
    
    public static FixResult disabled() {
        return new FixResult(false, 0, 0, 0, 0, "自动修复已禁用", null);
    }
    
    public static FixResult skipped() {
        return new FixResult(true, 0, 0, 0, 0, "跳过修复", null);
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public int getFixedCount() {
        return fixedCount;
    }
    
    public int getTotalCount() {
        return totalCount;
    }
    
    public int getFailedCount() {
        return failedCount;
    }
    
    public long getDurationMs() {
        return durationMs;
    }
    
    @Nullable
    public String getMessage() {
        return message;
    }
    
    public List<String> getErrors() {
        return new ArrayList<>(errors);
    }
    
    public boolean hasErrors() {
        return !errors.isEmpty();
    }
    
    public boolean hasFixed() {
        return fixedCount > 0;
    }
    
    /**
     * 获取成功率百分比
     */
    public int getSuccessRate() {
        if (totalCount == 0) return 100;
        return (fixedCount * 100) / totalCount;
    }
    
    /**
     * 获取摘要信息
     */
    public String getSummary() {
        if (message != null) {
            return message;
        }
        
        if (totalCount == 0) {
            return "没有需要修复的项";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("修复完成: %d/%d", fixedCount, totalCount));
        
        if (failedCount > 0) {
            sb.append(String.format(" (失败: %d)", failedCount));
        }
        
        if (durationMs > 0) {
            sb.append(String.format(" [耗时: %.2f秒]", durationMs / 1000.0));
        }
        
        return sb.toString();
    }
    
    /**
     * 获取详细信息
     */
    public String getDetailedMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append(getSummary());
        
        if (hasErrors()) {
            sb.append("\n\n错误信息:\n");
            for (int i = 0; i < errors.size(); i++) {
                sb.append(String.format("%d. %s\n", i + 1, errors.get(i)));
            }
        }
        
        return sb.toString();
    }
    
    @Override
    public String toString() {
        return String.format("FixResult{success=%s, fixed=%d/%d, failed=%d, duration=%dms, message='%s'}", 
                           success, fixedCount, totalCount, failedCount, durationMs, message);
    }
}


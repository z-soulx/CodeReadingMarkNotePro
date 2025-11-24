package jp.kitabatakep.intellij.plugins.codereadingnote.sync;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 同步操作结果
 */
public class SyncResult {
    
    private final boolean success;
    private final String message;
    private final String data;
    private final Throwable exception;
    private final long timestamp;
    
    private SyncResult(boolean success, @Nullable String message, @Nullable String data, @Nullable Throwable exception) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.exception = exception;
        this.timestamp = System.currentTimeMillis();
    }
    
    /**
     * 创建成功结果
     */
    @NotNull
    public static SyncResult success(@Nullable String message) {
        return new SyncResult(true, message, null, null);
    }
    
    /**
     * 创建成功结果（带数据）
     */
    @NotNull
    public static SyncResult success(@Nullable String message, @Nullable String data) {
        return new SyncResult(true, message, data, null);
    }
    
    /**
     * 创建失败结果
     */
    @NotNull
    public static SyncResult failure(@NotNull String message) {
        return new SyncResult(false, message, null, null);
    }
    
    /**
     * 创建失败结果（带异常）
     */
    @NotNull
    public static SyncResult failure(@NotNull String message, @NotNull Throwable exception) {
        return new SyncResult(false, message, null, exception);
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    @Nullable
    public String getMessage() {
        return message;
    }
    
    @Nullable
    public String getData() {
        return data;
    }
    
    @Nullable
    public Throwable getException() {
        return exception;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    /**
     * 获取用户友好的错误消息
     */
    @NotNull
    public String getUserMessage() {
        if (success) {
            return message != null ? message : "操作成功";
        } else {
            if (exception != null) {
                String exMsg = exception.getMessage();
                return message + (exMsg != null ? ": " + exMsg : "");
            }
            return message != null ? message : "操作失败";
        }
    }
}


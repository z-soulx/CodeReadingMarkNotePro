package jp.kitabatakep.intellij.plugins.codereadingnote.sync;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

/**
 * 同步提供者抽象基类 - 提供通用实现和工具方法
 */
public abstract class AbstractSyncProvider implements SyncProvider {
    
    protected static final Logger LOG = Logger.getInstance(AbstractSyncProvider.class);
    
    @Override
    @NotNull
    public SyncResult validateConfig(@NotNull SyncConfig config) {
        // 基础验证
        if (config.getProviderType() != getType()) {
            return SyncResult.failure("Config type mismatch");
        }
        
        String validationError = config.validate();
        if (validationError != null) {
            return SyncResult.failure(validationError);
        }
        
        // 子类特定验证
        return doValidateConfig(config);
    }
    
    /**
     * 子类实现特定的配置验证逻辑
     */
    @NotNull
    protected abstract SyncResult doValidateConfig(@NotNull SyncConfig config);
    
    @Override
    public boolean hasRemoteUpdate(@NotNull Project project, @NotNull SyncConfig config, 
                                    @NotNull String projectIdentifier, long localTimestamp) {
        try {
            long remoteTimestamp = getRemoteTimestamp(project, config, projectIdentifier);
            return remoteTimestamp > localTimestamp;
        } catch (Exception e) {
            LOG.warn("Failed to check remote update", e);
            return false;
        }
    }
    
    @Override
    public long getRemoteLastModifiedTime(@NotNull Project project, @NotNull SyncConfig config, 
                                          @NotNull String projectIdentifier) {
        try {
            return getRemoteTimestamp(project, config, projectIdentifier);
        } catch (Exception e) {
            LOG.warn("Failed to get remote last modified time", e);
            return 0;
        }
    }
    
    /**
     * 生成错误消息的辅助方法
     */
    @NotNull
    protected String formatError(@NotNull String operation, @NotNull Throwable e) {
        String message = e.getMessage();
        if (message == null || message.isEmpty()) {
            message = e.getClass().getSimpleName();
        }
        return String.format("%s失败: %s", operation, message);
    }
}


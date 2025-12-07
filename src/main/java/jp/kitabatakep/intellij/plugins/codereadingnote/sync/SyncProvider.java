package jp.kitabatakep.intellij.plugins.codereadingnote.sync;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

/**
 * 同步提供者接口 - 定义第三方同步的核心能力
 * 
 * 实现此接口以支持新的同步方式（如GitHub、Gitee、WebDAV等）
 */
public interface SyncProvider {
    
    /**
     * 获取提供者类型
     * @return 提供者类型枚举
     */
    @NotNull
    SyncProviderType getType();
    
    /**
     * 验证配置是否有效
     * @param config 同步配置
     * @return 配置验证结果
     */
    @NotNull
    SyncResult validateConfig(@NotNull SyncConfig config);
    
    /**
     * 推送本地数据到远程
     * @param project 当前项目
     * @param config 同步配置
     * @param data 要同步的数据（XML内容）
     * @param projectIdentifier 项目唯一标识符
     * @return 同步结果
     */
    @NotNull
    SyncResult push(@NotNull Project project, @NotNull SyncConfig config, @NotNull String data, @NotNull String projectIdentifier);
    
    /**
     * 从远程拉取数据到本地
     * @param project 当前项目
     * @param config 同步配置
     * @param projectIdentifier 项目唯一标识符
     * @return 同步结果，包含拉取的数据
     */
    @NotNull
    SyncResult pull(@NotNull Project project, @NotNull SyncConfig config, @NotNull String projectIdentifier);
    
    /**
     * 检查远程是否有更新
     * @param project 当前项目
     * @param config 同步配置
     * @param projectIdentifier 项目唯一标识符
     * @param localTimestamp 本地数据时间戳
     * @return 是否有远程更新
     */
    boolean hasRemoteUpdate(@NotNull Project project, @NotNull SyncConfig config, @NotNull String projectIdentifier, long localTimestamp);
    
    /**
     * 获取远程数据的最后修改时间
     * @param project 当前项目
     * @param config 同步配置
     * @param projectIdentifier 项目唯一标识符
     * @return 时间戳（毫秒），如果远程文件不存在返回0
     */
    long getRemoteLastModifiedTime(@NotNull Project project, @NotNull SyncConfig config, @NotNull String projectIdentifier);
    long getRemoteTimestamp(@NotNull Project project, @NotNull SyncConfig config, @NotNull String projectIdentifier);
}


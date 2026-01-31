package jp.kitabatakep.intellij.plugins.codereadingnote.sync;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 自动同步调度器
 * 负责在笔记数据变化时自动推送到远程仓库
 * 使用防抖机制，延迟3秒执行，避免频繁操作
 */
@Service(Service.Level.PROJECT)
public final class AutoSyncScheduler {
    
    private static final Logger LOG = Logger.getInstance(AutoSyncScheduler.class);
    private static final int DEBOUNCE_DELAY_SECONDS = 3;
    
    private final Project project;
    private final ScheduledExecutorService scheduler;
    private ScheduledFuture<?> pendingSync;
    
    public AutoSyncScheduler(@NotNull Project project) {
        this.project = project;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "AutoSyncScheduler-" + project.getName());
            thread.setDaemon(true);
            return thread;
        });
    }
    
    @NotNull
    public static AutoSyncScheduler getInstance(@NotNull Project project) {
        return project.getService(AutoSyncScheduler.class);
    }
    
    /**
     * 调度自动同步任务
     * 使用防抖机制：如果在延迟期间再次调用，会取消之前的任务并重新计时
     */
    public void scheduleAutoSync() {
        // 取消之前待执行的任务
        if (pendingSync != null && !pendingSync.isDone()) {
            pendingSync.cancel(false);
        }
        
        // 标记为待同步状态（PENDING）
        SyncStatusService statusService = SyncStatusService.getInstance(project);
        if (!statusService.isAutoSyncPaused()) {
            statusService.markPending();
        }
        
        // 延迟3秒后执行同步
        pendingSync = scheduler.schedule(() -> {
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                executePush();
            });
        }, DEBOUNCE_DELAY_SECONDS, TimeUnit.SECONDS);
    }
    
    /**
     * 执行推送操作
     * 静默执行，失败时仅记录日志，不弹窗
     */
    private void executePush() {
        try {
            // Check if auto-sync is paused (e.g., due to conflict)
            SyncStatusService statusService = SyncStatusService.getInstance(project);
            if (statusService.isAutoSyncPaused()) {
                LOG.info("Auto-sync is paused due to conflict, skipping push");
                return;
            }
            
            // 获取同步配置
            SyncConfig config = SyncSettings.getInstance().getSyncConfig();
            
            // 检查是否启用同步
            if (!config.isEnabled()) {
                statusService.updateStatus(SyncStatus.DISABLED);
                return;
            }
            
            // 检查是否启用自动同步
            if (!config.isAutoSync()) {
                return;
            }
            
            // 验证配置
            String validationError = config.validate();
            if (validationError != null) {
                LOG.warn("Auto-sync skipped: Invalid configuration - " + validationError);
                return;
            }
            
            // Mark as syncing (同步进行中，此时不更新lastSyncTime)
            statusService.markSyncing();
            
            // 执行推送
            SyncService syncService = SyncService.getInstance(project);
            SyncResult result = syncService.push(config);
            
            if (result.isSuccess()) {
                // Push 成功后才更新状态和时间
                
                // 1. 更新同步时间（表示成功同步到远端的时间）
                long syncTime = System.currentTimeMillis();
                statusService.updateLastSyncTime(syncTime);
                
                // 2. 更新 MD5（表示当前已同步的数据状态）
                String currentMd5 = syncService.calculateLocalDataMd5();
                statusService.updateLastLocalDataMd5(currentMd5);
                
                // 3. 标记为已同步
                statusService.markSynced();
                
            } else {
                // Push 失败，不更新时间和 MD5
                LOG.warn("Auto-sync push failed: " + result.getMessage());
                statusService.setError(result.getMessage());
            }
            
        } catch (Exception e) {
            LOG.error("Auto-sync push error", e);
            SyncStatusService statusService = SyncStatusService.getInstance(project);
            statusService.setError("Auto-sync error: " + e.getMessage());
        }
    }
    
    /**
     * 取消所有待执行的任务并关闭调度器
     */
    public void shutdown() {
        if (pendingSync != null && !pendingSync.isDone()) {
            pendingSync.cancel(false);
        }
        scheduler.shutdown();
    }
}


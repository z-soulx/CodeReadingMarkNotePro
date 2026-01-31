package jp.kitabatakep.intellij.plugins.codereadingnote.sync;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import jp.kitabatakep.intellij.plugins.codereadingnote.*;
import com.intellij.openapi.util.JDOMUtil;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.io.StringReader;
import java.security.MessageDigest;
import java.util.ArrayList;

/**
 * 同步服务 - 管理数据同步的核心服务
 */
@Service(Service.Level.PROJECT)
public final class SyncService {
    
    private static final Logger LOG = Logger.getInstance(SyncService.class);
    
    private final Project project;
    private SyncStatus lastSyncStatus = SyncStatus.IDLE;
    private String lastSyncMessage = "";
    // Note: lastSyncTime is now stored in CodeReadingNoteService
    
    public SyncService(@NotNull Project project) {
        this.project = project;
    }
    
    /**
     * Get effective local timestamp for conflict detection
     * Uses lastSyncTime if available, otherwise uses the latest Topic updatedAt
     */
    public long getEffectiveLocalTimestamp() {
        // Get lastSyncTime from SyncStatusService
        SyncStatusService statusService = SyncStatusService.getInstance(project);
        long storedSyncTime = statusService.getLastSyncTime();
        
        if (storedSyncTime > 0) {
            return storedSyncTime;
        }
        
        // Fallback: use the latest Topic updatedAt as local timestamp
        CodeReadingNoteService noteService = CodeReadingNoteService.getInstance(project);
        TopicList topicList = noteService.getTopicList();
        
        if (topicList == null || topicList.getTopics().isEmpty()) {
            return 0;
        }
        
        long latestTime = 0;
        for (Topic topic : topicList.getTopics()) {
            if (topic.updatedAt() != null) {
                long topicTime = topic.updatedAt().getTime();
                if (topicTime > latestTime) {
                    latestTime = topicTime;
                }
            }
        }
        
        return latestTime;
    }
    
    @NotNull
    public static SyncService getInstance(@NotNull Project project) {
        return project.getService(SyncService.class);
    }
    
    /**
     * Calculate MD5 hash of current local data
     * Uses getStateWithoutTrigger() to avoid triggering auto-sync during calculation
     */
    @NotNull
    public String calculateLocalDataMd5() {
        try {
            // Get current local data as XML (without triggering auto-sync)
            CodeReadingNoteService noteService = CodeReadingNoteService.getInstance(project);
            Element dataElement = noteService.getStateWithoutTrigger();
            
            if (dataElement == null) {
                return "";
            }
            
            // Convert to string
            String xmlString = JDOMUtil.write(dataElement);
            
            // Calculate MD5
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(xmlString.getBytes("UTF-8"));
            
            // Convert to hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : digest) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
            
        } catch (Exception e) {
            LOG.error("Failed to calculate MD5", e);
            return "";
        }
    }
    
    /**
     * 推送当前项目的笔记数据到远程
     */
    @NotNull
    public SyncResult push(@NotNull SyncConfig config) {
        if (lastSyncStatus == SyncStatus.SYNCING) {
            return SyncResult.failure("Sync in progress, please try again later");
        }
        
        try {
            // 标记为同步中（此时不更新 lastSyncTime）
            lastSyncStatus = SyncStatus.SYNCING;
            
            // 获取同步提供者
            SyncProvider provider = SyncProviderFactory.getProvider(config);
            if (provider == null) {
                return SyncResult.failure("Unsupported sync type: " + config.getProviderType());
            }
            
            // 验证配置
            SyncResult validation = provider.validateConfig(config);
            if (!validation.isSuccess()) {
                return validation;
            }
            
            // 获取当前项目的数据
            CodeReadingNoteService service = CodeReadingNoteService.getInstance(project);
            TopicList topicList = service.getTopicList();
            
            if (topicList == null || topicList.getTopics().isEmpty()) {
                return SyncResult.failure("No data to sync");
            }
            
            // 导出为XML
            Element topicsElement = TopicListExporter.export(topicList.getTopics().iterator());
            String xmlData = JDOMUtil.writeElement(topicsElement);
            
            if (xmlData == null || xmlData.isEmpty()) {
                return SyncResult.failure("Failed to export data");
            }
            
            // 生成项目标识符
            String projectIdentifier = generateProjectIdentifier(project);
            
            // 执行推送到远端
            SyncResult result = provider.push(project, config, xmlData, projectIdentifier);
            
            if (result.isSuccess()) {
                // ✅ Push 成功后才更新状态和时间
                SyncStatusService statusService = SyncStatusService.getInstance(project);
                
                // 1. 更新同步时间（表示成功推送到远端的时间）
                long syncTime = System.currentTimeMillis();
                statusService.updateLastSyncTime(syncTime);
                
                // 2. 更新 MD5（表示当前已同步的数据状态）
                String currentMd5 = calculateLocalDataMd5();
                statusService.updateLastLocalDataMd5(currentMd5);
                
                // 3. 更新内部状态
                lastSyncStatus = SyncStatus.SUCCESS;
                lastSyncMessage = "Pushed successfully";
            } else {
                // ❌ Push 失败，不更新时间和 MD5
                LOG.warn("Push failed: " + result.getMessage());
                lastSyncStatus = SyncStatus.FAILED;
                lastSyncMessage = result.getMessage();
            }
            
            return result;
            
        } catch (Exception e) {
            LOG.error("Push failed", e);
            lastSyncStatus = SyncStatus.FAILED;
            lastSyncMessage = "Push failed: " + e.getMessage();
            return SyncResult.failure("Push failed", e);
        } finally {
            if (lastSyncStatus == SyncStatus.SYNCING) {
                lastSyncStatus = SyncStatus.IDLE;
            }
        }
    }
    
    /**
     * 从远程拉取笔记数据到当前项目
     */
    @NotNull
    public SyncResult pull(@NotNull SyncConfig config, boolean merge) {
        if (lastSyncStatus == SyncStatus.SYNCING) {
            return SyncResult.failure("Sync in progress, please try again later");
        }
        
        try {
            lastSyncStatus = SyncStatus.SYNCING;
            
            // 获取同步提供者
            SyncProvider provider = SyncProviderFactory.getProvider(config);
            if (provider == null) {
                return SyncResult.failure("Unsupported sync type: " + config.getProviderType());
            }
            
            // 验证配置
            SyncResult validation = provider.validateConfig(config);
            if (!validation.isSuccess()) {
                return validation;
            }
            
            // 生成项目标识符
            String projectIdentifier = generateProjectIdentifier(project);
            
            // 执行拉取
            SyncResult result = provider.pull(project, config, projectIdentifier);
            
            if (!result.isSuccess()) {
                lastSyncStatus = SyncStatus.FAILED;
                lastSyncMessage = result.getMessage();
                return result;
            }
            
            // 获取远程数据
            String xmlData = result.getData();
            if (xmlData == null || xmlData.isEmpty()) {
                lastSyncStatus = SyncStatus.FAILED;
                lastSyncMessage = "No remote data";
                return SyncResult.failure("No remote data");
            }
            
            // 解析XML数据
            Element topicsElement = JDOMUtil.load(new StringReader(xmlData));
            
            ArrayList<Topic> remoteTopics = TopicListImporter.importElement(project, topicsElement);
            if (remoteTopics == null || remoteTopics.isEmpty()) {
                lastSyncStatus = SyncStatus.FAILED;
                lastSyncMessage = "Failed to parse remote data or no remote data";
                return SyncResult.failure("Failed to parse remote data or no remote data");
            }
            
            // 应用到本地
            CodeReadingNoteService service = CodeReadingNoteService.getInstance(project);
            TopicList localTopicList = service.getTopicList();
            
            if (merge) {
                // 合并模式：保留本地数据，添加远程数据
                mergeTopics(localTopicList, remoteTopics);
            } else {
                // 覆盖模式：清除本地数据，添加远程数据
                localTopicList.setTopics(remoteTopics);
                // 触发加载通知
                project.getMessageBus().syncPublisher(TopicListNotifier.TOPIC_LIST_NOTIFIER_TOPIC).topicsLoaded();
            }
            
            // ✅ Pull 成功后才更新状态和时间
            SyncStatusService statusService = SyncStatusService.getInstance(project);
            
            // 1. 更新同步时间（表示成功从远端拉取的时间）
            long syncTime = System.currentTimeMillis();
            statusService.updateLastSyncTime(syncTime);
            
            // 2. 更新 MD5（表示当前已同步的数据状态）
            String currentMd5 = calculateLocalDataMd5();
            statusService.updateLastLocalDataMd5(currentMd5);
            
            // 3. 更新内部状态
            lastSyncStatus = SyncStatus.SUCCESS;
            lastSyncMessage = merge ? "Pulled and merged successfully" : "Pulled successfully";
            
            return SyncResult.success(lastSyncMessage);
            
        } catch (Exception e) {
            LOG.error("Pull failed", e);
            lastSyncStatus = SyncStatus.FAILED;
            lastSyncMessage = "Pull failed: " + e.getMessage();
            return SyncResult.failure("Pull failed", e);
        } finally {
            if (lastSyncStatus == SyncStatus.SYNCING) {
                lastSyncStatus = SyncStatus.IDLE;
            }
        }
    }
    
    /**
     * 检查远程是否有更新
     */
    public boolean hasRemoteUpdate(@NotNull SyncConfig config) {
        try {
            SyncProvider provider = SyncProviderFactory.getProvider(config);
            if (provider == null) {
                return false;
            }
            
            String projectIdentifier = generateProjectIdentifier(project);
            long localLastSyncTime = getLastSyncTime();
            return provider.hasRemoteUpdate(project, config, projectIdentifier, localLastSyncTime);
        } catch (Exception e) {
            LOG.warn("Failed to check remote update", e);
            return false;
        }
    }
    
    /**
     * 合并主题列表
     */
    private void mergeTopics(@NotNull TopicList local, @NotNull ArrayList<Topic> remoteTopics) {
        // 简单的合并策略：以主题名称为key，远程数据覆盖本地同名主题
        for (Topic remoteTopic : remoteTopics) {
            // 查找本地是否存在同名主题
            Topic localTopic = null;
            for (Topic topic : local.getTopics()) {
                if (topic.name().equals(remoteTopic.name())) {
                    localTopic = topic;
                    break;
                }
            }
            
            if (localTopic != null) {
                // 如果远程主题更新时间更新，则替换
                if (remoteTopic.updatedAt().after(localTopic.updatedAt())) {
                    local.removeTopic(localTopic);
                    local.getTopics().add(remoteTopic);
                }
            } else {
                // 本地不存在，直接添加
                local.getTopics().add(remoteTopic);
            }
        }
        
        // 触发加载通知
        project.getMessageBus().syncPublisher(TopicListNotifier.TOPIC_LIST_NOTIFIER_TOPIC).topicsLoaded();
    }
    
    /**
     * 生成项目唯一标识符
     * 直接使用项目名称，确保可读性和跨设备同步
     */
    @NotNull
    private String generateProjectIdentifier(@NotNull Project project) {
        String projectName = project.getName();
        
        // 直接使用项目名称，只替换文件系统不允许的特殊字符
        // 保留大小写和常见符号，保证可读性
        String identifier = projectName
            .replace('\\', '_')  // 反斜杠
            .replace('/', '_')   // 斜杠
            .replace(':', '_')   // 冒号
            .replace('*', '_')   // 星号
            .replace('?', '_')   // 问号
            .replace('"', '_')   // 双引号
            .replace('<', '_')   // 小于号
            .replace('>', '_')   // 大于号
            .replace('|', '_');  // 竖线
        
        // 如果为空或只有空白字符，使用默认名称
        if (identifier.trim().isEmpty()) {
            identifier = "unnamed-project";
        }
        
        return identifier;
    }
    
    // Getters for status
    
    @NotNull
    public SyncStatus getLastSyncStatus() {
        return lastSyncStatus;
    }
    
    /**
     * Get last sync time (from SyncStatusService)
     */
    public long getLastSyncTime() {
        SyncStatusService statusService = SyncStatusService.getInstance(project);
        return statusService.getLastSyncTime();
    }
    
    @NotNull
    public String getLastSyncMessage() {
        return lastSyncMessage;
    }
    
    /**
     * 同步状态枚举
     */
    public enum SyncStatus {
        IDLE("空闲"),
        SYNCING("同步中"),
        SUCCESS("成功"),
        FAILED("失败");
        
        private final String displayName;
        
        SyncStatus(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
}


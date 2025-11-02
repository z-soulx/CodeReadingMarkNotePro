package jp.kitabatakep.intellij.plugins.codereadingnote.sync;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import jp.kitabatakep.intellij.plugins.codereadingnote.*;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jetbrains.annotations.NotNull;

import java.io.StringReader;
import java.util.ArrayList;

/**
 * 同步服务 - 管理数据同步的核心服务
 */
@Service(Service.Level.PROJECT)
public final class SyncService {
    
    private static final Logger LOG = Logger.getInstance(SyncService.class);
    
    private final Project project;
    private SyncStatus lastSyncStatus = SyncStatus.IDLE;
    private long lastSyncTime = 0;
    private String lastSyncMessage = "";
    
    public SyncService(@NotNull Project project) {
        this.project = project;
    }
    
    @NotNull
    public static SyncService getInstance(@NotNull Project project) {
        return project.getService(SyncService.class);
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
            Document document = new Document(topicsElement);
            @SuppressWarnings("deprecation")
            XMLOutputter xmlOutputter = new XMLOutputter(Format.getPrettyFormat());
            @SuppressWarnings("deprecation")
            String xmlData = xmlOutputter.outputString(document);
            
            if (xmlData == null || xmlData.isEmpty()) {
                return SyncResult.failure("Failed to export data");
            }
            
            // 生成项目标识符
            String projectIdentifier = generateProjectIdentifier(project);
            
            // 执行推送
            SyncResult result = provider.push(project, config, xmlData, projectIdentifier);
            
            if (result.isSuccess()) {
                lastSyncTime = System.currentTimeMillis();
                lastSyncStatus = SyncStatus.SUCCESS;
                lastSyncMessage = "Pushed successfully";
            } else {
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
            @SuppressWarnings("deprecation")
            SAXBuilder saxBuilder = new SAXBuilder();
            @SuppressWarnings("deprecation")
            Document document = saxBuilder.build(new StringReader(xmlData));
            Element topicsElement = document.getRootElement();
            
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
            
            lastSyncTime = System.currentTimeMillis();
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
            return provider.hasRemoteUpdate(project, config, projectIdentifier, lastSyncTime);
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
        
        LOG.info("Generated project identifier: " + identifier + " for project: " + projectName);
        return identifier;
    }
    
    // Getters for status
    
    @NotNull
    public SyncStatus getLastSyncStatus() {
        return lastSyncStatus;
    }
    
    public long getLastSyncTime() {
        return lastSyncTime;
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


package jp.kitabatakep.intellij.plugins.codereadingnote.sync;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import jp.kitabatakep.intellij.plugins.codereadingnote.sync.github.GitHubSyncConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * 同步配置持久化服务
 * 存储在应用级别（跨项目共享）
 */
@State(
    name = "CodeReadingNoteSyncSettings",
    storages = @Storage("codeReadingNoteSync.xml")
)
public class SyncSettings implements PersistentStateComponent<SyncSettings.State> {
    
    private State state = new State();
    
    @NotNull
    public static SyncSettings getInstance() {
        return ApplicationManager.getApplication().getService(SyncSettings.class);
    }
    
    @Override
    @Nullable
    public State getState() {
        return state;
    }
    
    @Override
    public void loadState(@NotNull State state) {
        XmlSerializerUtil.copyBean(state, this.state);
    }
    
    /**
     * 获取当前的同步配置
     */
    @NotNull
    public SyncConfig getSyncConfig() {
        // 根据类型创建对应的配置对象
        SyncConfig config = createConfigByType(state.providerType);
        
        // 加载配置
        config.setEnabled(state.enabled);
        config.setAutoSync(state.autoSync);
        config.setProviderType(state.providerType);
        
        // 逐个设置properties，使用setProperty确保正确保存
        for (Map.Entry<String, String> entry : state.properties.entrySet()) {
            config.setProperty(entry.getKey(), entry.getValue());
        }
        
        return config;
    }
    
    /**
     * 保存同步配置
     */
    public void setSyncConfig(@NotNull SyncConfig config) {
        state.enabled = config.isEnabled();
        state.autoSync = config.isAutoSync();
        state.providerType = config.getProviderType();
        state.properties.clear();
        
        // 获取properties的副本并保存
        Map<String, String> configProps = config.getProperties();
        state.properties.putAll(configProps);
    }
    
    /**
     * 根据类型创建配置对象
     */
    @NotNull
    private SyncConfig createConfigByType(@NotNull SyncProviderType type) {
        switch (type) {
            case GITHUB:
                return new GitHubSyncConfig();
            case GITEE:
                // 未来实现
                // return new GiteeSyncConfig();
            case WEBDAV:
                // 未来实现
                // return new WebDAVSyncConfig();
            case LOCAL_FILE:
                // 未来实现
                // return new LocalFileSyncConfig();
            default:
                return new GitHubSyncConfig(); // 默认使用GitHub
        }
    }
    
    /**
     * 持久化状态类
     */
    public static class State {
        public boolean enabled = false;
        public boolean autoSync = false;
        public SyncProviderType providerType = SyncProviderType.GITHUB;
        public Map<String, String> properties = new HashMap<>();
    }
}


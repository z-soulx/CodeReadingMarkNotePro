package jp.kitabatakep.intellij.plugins.codereadingnote.sync;

import jp.kitabatakep.intellij.plugins.codereadingnote.sync.github.GitHubSyncProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * 同步提供者工厂 - 根据类型创建对应的同步提供者实例
 */
public class SyncProviderFactory {
    
    private static final Map<SyncProviderType, SyncProvider> providers = new HashMap<>();
    
    static {
        // 注册所有支持的同步提供者
        registerProvider(new GitHubSyncProvider());
        // 未来在这里添加新的提供者
        // registerProvider(new GiteeSyncProvider());
        // registerProvider(new WebDAVSyncProvider());
    }
    
    /**
     * 注册同步提供者
     */
    private static void registerProvider(@NotNull SyncProvider provider) {
        providers.put(provider.getType(), provider);
    }
    
    /**
     * 获取指定类型的同步提供者
     * 
     * @param type 提供者类型
     * @return 同步提供者实例，如果不支持则返回null
     */
    @Nullable
    public static SyncProvider getProvider(@NotNull SyncProviderType type) {
        return providers.get(type);
    }
    
    /**
     * 获取指定配置对应的同步提供者
     * 
     * @param config 同步配置
     * @return 同步提供者实例，如果不支持则返回null
     */
    @Nullable
    public static SyncProvider getProvider(@NotNull SyncConfig config) {
        return getProvider(config.getProviderType());
    }
    
    /**
     * 检查是否支持指定类型的同步提供者
     */
    public static boolean isSupported(@NotNull SyncProviderType type) {
        return providers.containsKey(type);
    }
    
    /**
     * 获取所有已注册的提供者类型
     */
    @NotNull
    public static SyncProviderType[] getSupportedTypes() {
        return providers.keySet().toArray(new SyncProviderType[0]);
    }
}


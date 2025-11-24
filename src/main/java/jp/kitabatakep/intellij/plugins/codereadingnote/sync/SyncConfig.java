package jp.kitabatakep.intellij.plugins.codereadingnote.sync;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 同步配置抽象类 - 所有同步提供者的配置基类
 */
public abstract class SyncConfig {
    
    private boolean enabled = false;
    private boolean autoSync = false;
    private SyncProviderType providerType;
    
    /**
     * 扩展属性，用于存储提供者特定的配置
     */
    protected Map<String, String> properties = new HashMap<>();
    
    public SyncConfig(@NotNull SyncProviderType providerType) {
        this.providerType = providerType;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public boolean isAutoSync() {
        return autoSync;
    }
    
    public void setAutoSync(boolean autoSync) {
        this.autoSync = autoSync;
    }
    
    @NotNull
    public SyncProviderType getProviderType() {
        return providerType;
    }
    
    public void setProviderType(@NotNull SyncProviderType providerType) {
        this.providerType = providerType;
    }
    
    @Nullable
    public String getProperty(@NotNull String key) {
        return properties.get(key);
    }
    
    public void setProperty(@NotNull String key, @Nullable String value) {
        if (value == null) {
            properties.remove(key);
        } else {
            properties.put(key, value);
        }
    }
    
    @NotNull
    public Map<String, String> getProperties() {
        return new HashMap<>(properties);
    }
    
    /**
     * 验证配置是否完整有效
     * @return 验证结果消息，null表示验证通过
     */
    @Nullable
    public abstract String validate();
    
    /**
     * 克隆配置
     */
    public abstract SyncConfig clone();
    
    /**
     * 从另一个配置复制数据
     */
    public void copyFrom(@NotNull SyncConfig other) {
        this.enabled = other.enabled;
        this.autoSync = other.autoSync;
        this.providerType = other.providerType;
        this.properties = new HashMap<>(other.properties);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SyncConfig that = (SyncConfig) o;
        return enabled == that.enabled &&
                autoSync == that.autoSync &&
                providerType == that.providerType &&
                Objects.equals(properties, that.properties);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(enabled, autoSync, providerType, properties);
    }
}


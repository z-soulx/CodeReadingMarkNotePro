package jp.kitabatakep.intellij.plugins.codereadingnote.sync.ui;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.util.NlsContexts;
import jp.kitabatakep.intellij.plugins.codereadingnote.sync.SyncConfig;
import jp.kitabatakep.intellij.plugins.codereadingnote.sync.SyncSettings;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * 同步配置界面
 * 集成到Settings中
 */
public class SyncConfigurable implements Configurable {
    
    private SyncSettingsPanel settingsPanel;
    private SyncConfig workingConfig;
    
    @Override
    @NlsContexts.ConfigurableName
    public String getDisplayName() {
        return "Code Reading Note Sync";
    }
    
    @Override
    @Nullable
    public JComponent createComponent() {
        if (settingsPanel == null) {
            settingsPanel = new SyncSettingsPanel();
            // 创建面板后立即加载配置
            workingConfig = SyncSettings.getInstance().getSyncConfig();
            settingsPanel.loadFrom(workingConfig);
        }
        return settingsPanel.getPanel();
    }
    
    @Override
    public boolean isModified() {
        if (settingsPanel == null) {
            return false;
        }
        
        if (workingConfig == null) {
            workingConfig = SyncSettings.getInstance().getSyncConfig();
        }
        
        return settingsPanel.isModified(workingConfig);
    }
    
    @Override
    public void apply() throws ConfigurationException {
        if (settingsPanel == null) {
            return;
        }
        
        if (workingConfig == null) {
            workingConfig = SyncSettings.getInstance().getSyncConfig();
        }
        
        // 从UI保存到工作副本
        settingsPanel.saveTo(workingConfig);
        
        // 验证配置
        String error = workingConfig.validate();
        if (error != null && workingConfig.isEnabled()) {
            throw new ConfigurationException(error);
        }
        
        // 保存到持久化存储
        SyncSettings.getInstance().setSyncConfig(workingConfig);
        
        // 重新加载workingConfig，确保下次打开时显示正确
        workingConfig = SyncSettings.getInstance().getSyncConfig();
    }
    
    @Override
    public void reset() {
        if (settingsPanel == null) {
            return;
        }
        
        // 重新加载配置
        workingConfig = SyncSettings.getInstance().getSyncConfig();
        settingsPanel.reset(workingConfig);
    }
    
    @Override
    public void disposeUIResources() {
        settingsPanel = null;
        workingConfig = null;
    }
}


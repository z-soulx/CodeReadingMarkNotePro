package jp.kitabatakep.intellij.plugins.codereadingnote.sync.ui;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import jp.kitabatakep.intellij.plugins.codereadingnote.sync.SyncConfig;
import jp.kitabatakep.intellij.plugins.codereadingnote.sync.SyncProviderType;
import jp.kitabatakep.intellij.plugins.codereadingnote.sync.github.GitHubSyncConfig;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

/**
 * 同步设置面板
 */
public class SyncSettingsPanel {
    
    private final JPanel mainPanel;
    
    // 通用配置
    private final JBCheckBox enabledCheckBox;
    private final JBCheckBox autoSyncCheckBox;
    private final ComboBox<SyncProviderType> providerTypeComboBox;
    
    // GitHub配置
    private final JBTextField repositoryField;
    private final JBPasswordField tokenField;
    private final JBTextField branchField;
    private final JBTextField basePathField;
    
    private final JPanel providerConfigPanel;
    private final CardLayout providerConfigLayout;
    
    public SyncSettingsPanel() {
        // 初始化组件
        enabledCheckBox = new JBCheckBox("Enable Sync");
        autoSyncCheckBox = new JBCheckBox("Auto Sync (automatically push on save)");
        
        providerTypeComboBox = new ComboBox<>(new DefaultComboBoxModel<>(SyncProviderType.values()));
        providerTypeComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, 
                                                         int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof SyncProviderType) {
                    setText(((SyncProviderType) value).getDisplayName());
                }
                return this;
            }
        });
        
        // GitHub配置字段
        repositoryField = new JBTextField();
        repositoryField.getEmptyText().setText("e.g., username/repo-name");
        
        tokenField = new JBPasswordField();
        tokenField.getEmptyText().setText("GitHub Personal Access Token");
        
        branchField = new JBTextField();
        branchField.setText("main");
        
        basePathField = new JBTextField();
        basePathField.setText("code-reading-notes");
        
        // 创建GitHub配置面板
        JPanel githubPanel = createGitHubConfigPanel();
        
        // 创建提供者配置容器（使用CardLayout支持多个提供者）
        providerConfigLayout = new CardLayout();
        providerConfigPanel = new JPanel(providerConfigLayout);
        providerConfigPanel.add(githubPanel, SyncProviderType.GITHUB.name());
        providerConfigPanel.add(new JPanel(), "EMPTY"); // 占位面板
        
        // 监听提供者类型变化
        providerTypeComboBox.addActionListener(e -> {
            SyncProviderType selected = (SyncProviderType) providerTypeComboBox.getSelectedItem();
            if (selected != null) {
                providerConfigLayout.show(providerConfigPanel, selected.name());
            }
        });
        
        // 监听启用状态
        enabledCheckBox.addActionListener(e -> updateEnabledState());
        
        // 构建主面板
        mainPanel = FormBuilder.createFormBuilder()
            .addComponent(enabledCheckBox, 1)
            .addVerticalGap(10)
            .addLabeledComponent(new JBLabel("Sync Provider:"), providerTypeComboBox, 1, false)
            .addComponent(autoSyncCheckBox, 1)
            .addVerticalGap(10)
            .addSeparator()
            .addLabeledComponent(new JBLabel(""), new JBLabel("Sync Configuration"), 1, false)
            .addComponent(providerConfigPanel, 1)
            .addComponentFillVertically(new JPanel(), 0)
            .getPanel();
        
        mainPanel.setBorder(JBUI.Borders.empty(10));
        
        // 初始状态
        updateEnabledState();
    }
    
    /**
     * 创建GitHub配置面板
     */
    @NotNull
    private JPanel createGitHubConfigPanel() {
        JPanel panel = FormBuilder.createFormBuilder()
            .addLabeledComponent(new JBLabel("Repository:"), repositoryField, 1, false)
            .addTooltip("Format: owner/repo, e.g., username/code-notes")
            .addLabeledComponent(new JBLabel("Access Token:"), tokenField, 1, false)
            .addTooltip("Personal Access Token with Contents: Read and write permission")
            .addLabeledComponent(new JBLabel("Branch:"), branchField, 1, false)
            .addTooltip("Branch name for storing notes")
            .addLabeledComponent(new JBLabel("Base Path:"), basePathField, 1, false)
            .addTooltip("Storage path in repository")
            .getPanel();
        
        panel.setBorder(JBUI.Borders.emptyLeft(20));
        return panel;
    }
    
    /**
     * 更新启用状态
     */
    private void updateEnabledState() {
        boolean enabled = enabledCheckBox.isSelected();
        providerTypeComboBox.setEnabled(enabled);
        autoSyncCheckBox.setEnabled(enabled);
        repositoryField.setEnabled(enabled);
        tokenField.setEnabled(enabled);
        branchField.setEnabled(enabled);
        basePathField.setEnabled(enabled);
    }
    
    /**
     * 获取主面板
     */
    @NotNull
    public JPanel getPanel() {
        return mainPanel;
    }
    
    /**
     * 从配置加载到UI
     */
    public void loadFrom(@NotNull SyncConfig config) {
        enabledCheckBox.setSelected(config.isEnabled());
        autoSyncCheckBox.setSelected(config.isAutoSync());
        providerTypeComboBox.setSelectedItem(config.getProviderType());
        
        if (config instanceof GitHubSyncConfig) {
            GitHubSyncConfig ghConfig = (GitHubSyncConfig) config;
            repositoryField.setText(ghConfig.getRepository() != null ? ghConfig.getRepository() : "");
            tokenField.setText(ghConfig.getToken() != null ? ghConfig.getToken() : "");
            branchField.setText(ghConfig.getBranch());
            basePathField.setText(ghConfig.getBasePath());
        }
        
        updateEnabledState();
    }
    
    /**
     * 从UI保存到配置
     */
    public void saveTo(@NotNull SyncConfig config) {
        config.setEnabled(enabledCheckBox.isSelected());
        config.setAutoSync(autoSyncCheckBox.isSelected());
        
        SyncProviderType selectedType = (SyncProviderType) providerTypeComboBox.getSelectedItem();
        if (selectedType != null) {
            config.setProviderType(selectedType);
        }
        
        if (config instanceof GitHubSyncConfig) {
            GitHubSyncConfig ghConfig = (GitHubSyncConfig) config;
            ghConfig.setRepository(repositoryField.getText().trim());
            ghConfig.setToken(new String(tokenField.getPassword()));
            ghConfig.setBranch(branchField.getText().trim());
            ghConfig.setBasePath(basePathField.getText().trim());
        }
    }
    
    /**
     * 检查是否已修改
     */
    public boolean isModified(@NotNull SyncConfig config) {
        if (enabledCheckBox.isSelected() != config.isEnabled()) return true;
        if (autoSyncCheckBox.isSelected() != config.isAutoSync()) return true;
        
        SyncProviderType selectedType = (SyncProviderType) providerTypeComboBox.getSelectedItem();
        if (selectedType != config.getProviderType()) return true;
        
        if (config instanceof GitHubSyncConfig) {
            GitHubSyncConfig ghConfig = (GitHubSyncConfig) config;
            
            String repo = repositoryField.getText().trim();
            if (!repo.equals(ghConfig.getRepository() != null ? ghConfig.getRepository() : "")) return true;
            
            String token = new String(tokenField.getPassword());
            if (!token.equals(ghConfig.getToken() != null ? ghConfig.getToken() : "")) return true;
            
            if (!branchField.getText().trim().equals(ghConfig.getBranch())) return true;
            if (!basePathField.getText().trim().equals(ghConfig.getBasePath())) return true;
        }
        
        return false;
    }
    
    /**
     * 重置UI
     */
    public void reset(@NotNull SyncConfig config) {
        loadFrom(config);
    }
}


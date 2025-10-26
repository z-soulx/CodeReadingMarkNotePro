package jp.kitabatakep.intellij.plugins.codereadingnote.autofix;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 自动修复功能配置
 */
@State(
    name = "CodeReadingNoteAutoFixSettings",
    storages = @Storage("codeReadingNoteAutoFix.xml")
)
public class AutoFixSettings implements PersistentStateComponent<AutoFixSettings> {
    
    // 是否启用自动修复
    private boolean autoFixEnabled = false;  // 默认关闭，用户手动开启
    
    // 自动修复策略
    private AutoFixStrategy strategy = AutoFixStrategy.SMART;
    
    // 文件打开时自动检测
    private boolean detectOnFileOpen = true;
    
    // 文件打开时自动修复（谨慎使用）
    private boolean fixOnFileOpen = false;
    
    // 分支切换后自动修复
    private boolean fixOnBranchSwitch = true;
    
    // VCS 更新后自动修复
    private boolean fixOnVcsUpdate = true;
    
    // 通知设置
    private boolean showOffsetNotification = true;
    private boolean showFixConfirmDialog = false;  // 默认不显示确认对话框
    private boolean showFixResultNotification = true;
    
    // 高级选项
    private boolean autoRemoveInvalidLines = false;
    private boolean trySmartRecovery = false;
    private int detectionThrottleSeconds = 5;
    private int batchFixThreshold = 50;
    
    public static AutoFixSettings getInstance() {
        return ApplicationManager.getApplication().getService(AutoFixSettings.class);
    }
    
    @Nullable
    @Override
    public AutoFixSettings getState() {
        return this;
    }
    
    @Override
    public void loadState(@NotNull AutoFixSettings state) {
        XmlSerializerUtil.copyBean(state, this);
    }
    
    // Getters and Setters
    
    public boolean isAutoFixEnabled() {
        return autoFixEnabled;
    }
    
    public void setAutoFixEnabled(boolean autoFixEnabled) {
        this.autoFixEnabled = autoFixEnabled;
    }
    
    public AutoFixStrategy getStrategy() {
        return strategy;
    }
    
    public void setStrategy(AutoFixStrategy strategy) {
        this.strategy = strategy;
    }
    
    public boolean isDetectOnFileOpen() {
        return detectOnFileOpen;
    }
    
    public void setDetectOnFileOpen(boolean detectOnFileOpen) {
        this.detectOnFileOpen = detectOnFileOpen;
    }
    
    public boolean isFixOnFileOpen() {
        return fixOnFileOpen;
    }
    
    public void setFixOnFileOpen(boolean fixOnFileOpen) {
        this.fixOnFileOpen = fixOnFileOpen;
    }
    
    public boolean isFixOnBranchSwitch() {
        return fixOnBranchSwitch;
    }
    
    public void setFixOnBranchSwitch(boolean fixOnBranchSwitch) {
        this.fixOnBranchSwitch = fixOnBranchSwitch;
    }
    
    public boolean isFixOnVcsUpdate() {
        return fixOnVcsUpdate;
    }
    
    public void setFixOnVcsUpdate(boolean fixOnVcsUpdate) {
        this.fixOnVcsUpdate = fixOnVcsUpdate;
    }
    
    public boolean isShowOffsetNotification() {
        return showOffsetNotification;
    }
    
    public void setShowOffsetNotification(boolean showOffsetNotification) {
        this.showOffsetNotification = showOffsetNotification;
    }
    
    public boolean isShowFixConfirmDialog() {
        return showFixConfirmDialog;
    }
    
    public void setShowFixConfirmDialog(boolean showFixConfirmDialog) {
        this.showFixConfirmDialog = showFixConfirmDialog;
    }
    
    public boolean isShowFixResultNotification() {
        return showFixResultNotification;
    }
    
    public void setShowFixResultNotification(boolean showFixResultNotification) {
        this.showFixResultNotification = showFixResultNotification;
    }
    
    public boolean isAutoRemoveInvalidLines() {
        return autoRemoveInvalidLines;
    }
    
    public void setAutoRemoveInvalidLines(boolean autoRemoveInvalidLines) {
        this.autoRemoveInvalidLines = autoRemoveInvalidLines;
    }
    
    public boolean isTrySmartRecovery() {
        return trySmartRecovery;
    }
    
    public void setTrySmartRecovery(boolean trySmartRecovery) {
        this.trySmartRecovery = trySmartRecovery;
    }
    
    public int getDetectionThrottleSeconds() {
        return detectionThrottleSeconds;
    }
    
    public void setDetectionThrottleSeconds(int detectionThrottleSeconds) {
        this.detectionThrottleSeconds = detectionThrottleSeconds;
    }
    
    public int getDetectionThrottleMs() {
        return detectionThrottleSeconds * 1000;
    }
    
    public int getBatchFixThreshold() {
        return batchFixThreshold;
    }
    
    public void setBatchFixThreshold(int batchFixThreshold) {
        this.batchFixThreshold = batchFixThreshold;
    }
    
    /**
     * 根据触发器判断是否应该自动修复
     */
    public boolean shouldFix(FixTrigger trigger) {
        if (!autoFixEnabled) {
            return false;
        }
        
        return switch (trigger) {
            case FILE_OPENED -> fixOnFileOpen;
            case BRANCH_SWITCHED -> fixOnBranchSwitch;
            case VCS_UPDATED -> fixOnVcsUpdate;
            case MANUAL -> true;
            case PERIODIC -> false;  // 定时触发默认不自动修复
        };
    }
    
    /**
     * 智能模式下的策略判断
     */
    public boolean shouldFixInSmartMode(FixTrigger trigger) {
        if (strategy != AutoFixStrategy.SMART) {
            return shouldFix(trigger);
        }
        
        // 智能模式：文件打开只检测，分支切换和 VCS 更新自动修复
        return switch (trigger) {
            case FILE_OPENED -> false;  // 只检测不修复
            case BRANCH_SWITCHED, VCS_UPDATED -> true;
            case MANUAL -> true;
            case PERIODIC -> false;
        };
    }
    
    /**
     * 恢复默认设置
     */
    public void restoreDefaults() {
        autoFixEnabled = false;
        strategy = AutoFixStrategy.SMART;
        detectOnFileOpen = true;
        fixOnFileOpen = false;
        fixOnBranchSwitch = true;
        fixOnVcsUpdate = true;
        showOffsetNotification = true;
        showFixConfirmDialog = false;
        showFixResultNotification = true;
        autoRemoveInvalidLines = false;
        trySmartRecovery = false;
        detectionThrottleSeconds = 5;
        batchFixThreshold = 50;
    }
    
    /**
     * 自动修复策略枚举
     */
    public enum AutoFixStrategy {
        /**
         * 智能模式 (推荐)
         * - 文件打开时: 自动检测
         * - 分支切换后: 自动修复全部
         * - VCS 更新后: 自动修复全部
         */
        SMART("智能模式", "Smart Mode (Recommended)"),
        
        /**
         * 仅文件打开时修复
         */
        FILE_OPEN_ONLY("仅文件打开时", "File Open Only"),
        
        /**
         * 仅分支切换时修复
         */
        BRANCH_SWITCH_ONLY("仅分支切换时", "Branch Switch Only"),
        
        /**
         * 仅VCS更新时修复
         */
        VCS_UPDATE_ONLY("仅代码更新时", "VCS Update Only"),
        
        /**
         * 自定义
         */
        CUSTOM("自定义", "Custom"),
        
        /**
         * 关闭自动修复
         */
        DISABLED("关闭", "Disabled");
        
        private final String chineseName;
        private final String englishName;
        
        AutoFixStrategy(String chineseName, String englishName) {
            this.chineseName = chineseName;
            this.englishName = englishName;
        }
        
        public String getChineseName() {
            return chineseName;
        }
        
        public String getEnglishName() {
            return englishName;
        }
        
        @Override
        public String toString() {
            return chineseName + " (" + englishName + ")";
        }
    }
}


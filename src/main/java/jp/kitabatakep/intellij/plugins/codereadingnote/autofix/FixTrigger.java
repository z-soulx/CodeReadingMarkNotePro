package jp.kitabatakep.intellij.plugins.codereadingnote.autofix;

/**
 * 修复触发器 - 标识修复操作的触发来源
 */
public enum FixTrigger {
    /**
     * 手动触发 - 用户主动点击修复按钮
     */
    MANUAL("手动修复", "Manual"),
    
    /**
     * 文件打开触发 - 打开包含 TopicLine 的文件时
     */
    FILE_OPENED("文件打开", "File Opened"),
    
    /**
     * 分支切换触发 - Git 分支切换后
     */
    BRANCH_SWITCHED("分支切换", "Branch Switched"),
    
    /**
     * VCS 更新触发 - Git Pull/Update 后
     */
    VCS_UPDATED("代码更新", "VCS Updated"),
    
    /**
     * 定时触发 - 后台定时检测
     */
    PERIODIC("定时检测", "Periodic");
    
    private final String chineseName;
    private final String englishName;
    
    FixTrigger(String chineseName, String englishName) {
        this.chineseName = chineseName;
        this.englishName = englishName;
    }
    
    public String getChineseName() {
        return chineseName;
    }
    
    public String getEnglishName() {
        return englishName;
    }
    
    public boolean isAutomatic() {
        return this != MANUAL;
    }
}


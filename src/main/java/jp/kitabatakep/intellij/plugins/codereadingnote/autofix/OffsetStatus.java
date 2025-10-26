package jp.kitabatakep.intellij.plugins.codereadingnote.autofix;

/**
 * TopicLine 错位状态枚举
 */
public enum OffsetStatus {
    /**
     * 已同步 - TopicLine 行号与 Bookmark 一致
     */
    SYNCED("已同步", "Synced"),
    
    /**
     * 已错位 - TopicLine 行号与 Bookmark 不一致
     */
    OFFSET("已错位", "Offset"),
    
    /**
     * Bookmark 丢失 - TopicLine 的 Bookmark 不存在
     */
    BOOKMARK_MISSING("Bookmark丢失", "Bookmark Missing"),
    
    /**
     * 文件不存在 - TopicLine 指向的文件已删除
     */
    FILE_MISSING("文件不存在", "File Missing"),
    
    /**
     * 未知状态 - 无法判断（例如老数据没有 bookmarkUid）
     */
    UNKNOWN("未知", "Unknown");
    
    private final String chineseName;
    private final String englishName;
    
    OffsetStatus(String chineseName, String englishName) {
        this.chineseName = chineseName;
        this.englishName = englishName;
    }
    
    public String getChineseName() {
        return chineseName;
    }
    
    public String getEnglishName() {
        return englishName;
    }
    
    public boolean needsFix() {
        // 需要修复的情况：出现错位或 Bookmark 丢失
        // （FILE_MISSING/UNKNOWN 视为无法自动修复，不计入需要修复的阈值）
        return this == OFFSET || this == BOOKMARK_MISSING;
    }
    
    public boolean isValid() {
        return this == SYNCED || this == OFFSET;
    }
}


package jp.kitabatakep.intellij.plugins.codereadingnote.search;

/**
 * 搜索范围枚举
 * 定义搜索的目标范围：Topics、Bookmarks 或全部
 */
public enum SearchScope {
    TOPICS_ONLY("Topics Only", ""),
    BOOKMARKS_ONLY("Bookmarks Only", ""),
    ALL("All", "");
    
    private final String displayName;
    private final String chineseDisplay;
    
    SearchScope(String displayName, String chineseDisplay) {
        this.displayName = displayName;
        this.chineseDisplay = chineseDisplay;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getChineseDisplay() {
        return chineseDisplay;
    }
    
    @Override
    public String toString() {
        // return displayName + " (" + chineseDisplay + ")";
        return displayName;
    }
}


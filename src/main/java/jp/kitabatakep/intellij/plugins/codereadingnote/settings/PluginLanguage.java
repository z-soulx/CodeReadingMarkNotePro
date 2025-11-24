package jp.kitabatakep.intellij.plugins.codereadingnote.settings;

import java.util.Locale;

/**
 * 插件支持的语言选项
 * Plugin supported language options
 */
public enum PluginLanguage {
    /**
     * 英语
     * 使用 Locale.ROOT 因为默认的 .properties 文件(没有语言后缀)是英文
     */
    ENGLISH("English", "English", Locale.ROOT),
    
    /**
     * 简体中文
     */
    SIMPLIFIED_CHINESE("简体中文", "Simplified Chinese", Locale.SIMPLIFIED_CHINESE);
    
    private final String displayNameEn;
    private final String displayNameZh;
    private final Locale locale;
    
    PluginLanguage(String displayNameEn, String displayNameZh, Locale locale) {
        this.displayNameEn = displayNameEn;
        this.displayNameZh = displayNameZh;
        this.locale = locale;
    }
    
    /**
     * 获取显示名称（根据当前语言）
     */
    public String getDisplayName() {
        // 使用当前设置的语言来显示选项名称
        return displayNameEn + " / " + displayNameZh;
    }
    
    /**
     * 获取英文显示名称
     */
    public String getDisplayNameEn() {
        return displayNameEn;
    }
    
    /**
     * 获取中文显示名称
     */
    public String getDisplayNameZh() {
        return displayNameZh;
    }
    
    /**
     * 获取对应的 Locale
     */
    public Locale getLocale() {
        return locale;
    }
}


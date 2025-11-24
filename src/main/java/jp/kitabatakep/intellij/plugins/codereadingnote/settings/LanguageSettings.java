package jp.kitabatakep.intellij.plugins.codereadingnote.settings;

import com.intellij.DynamicBundle;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * 插件语言设置
 * Plugin language settings - persisted at application level
 */
@State(
    name = "CodeReadingNoteLanguageSettings",
    storages = @Storage("codeReadingNoteLanguage.xml")
)
public class LanguageSettings implements PersistentStateComponent<LanguageSettings.State> {
    
    /**
     * 持久化状态类
     */
    public static class State {
        public String selectedLanguage = null;  // 存储枚举名称，不是枚举对象
    }
    
    private State myState = new State();
    
    /**
     * 获取单例实例
     */
    public static LanguageSettings getInstance() {
        return ApplicationManager.getApplication().getService(LanguageSettings.class);
    }
    
    @Nullable
    @Override
    public State getState() {
        return myState;
    }
    
    @Override
    public void loadState(@NotNull State state) {
        XmlSerializerUtil.copyBean(state, myState);
    }
    
    /**
     * 获取选中的语言
     * 如果从未设置过，返回基于 IDE 语言的智能默认值
     */
    public PluginLanguage getSelectedLanguage() {
        if (myState.selectedLanguage == null) {
            // 首次使用，根据 IDE 语言自动选择
            return detectDefaultLanguage();
        }
        
        try {
            return PluginLanguage.valueOf(myState.selectedLanguage);
        } catch (IllegalArgumentException e) {
            // 如果枚举值无效，返回默认值
            return detectDefaultLanguage();
        }
    }
    
    /**
     * 设置选中的语言
     */
    public void setSelectedLanguage(PluginLanguage language) {
        if (language != null) {
            myState.selectedLanguage = language.name();
        }
    }
    
    /**
     * 获取实际使用的 Locale
     */
    @NotNull
    public Locale getEffectiveLocale() {
        return getSelectedLanguage().getLocale();
    }
    
    /**
     * 检测默认语言：如果 IDE 是中文则默认中文，否则默认英文
     */
    private PluginLanguage detectDefaultLanguage() {
        try {
            // 获取 IDE 的语言设置
            Locale ideLocale = DynamicBundle.getLocale();
            String language = ideLocale.getLanguage();
            
            // 如果是中文，返回简体中文，否则返回英文
            if ("zh".equals(language)) {
                return PluginLanguage.SIMPLIFIED_CHINESE;
            }
        } catch (Exception e) {
            // 如果获取失败，使用系统语言作为后备
            String sysLanguage = Locale.getDefault().getLanguage();
            if ("zh".equals(sysLanguage)) {
                return PluginLanguage.SIMPLIFIED_CHINESE;
            }
        }
        
        return PluginLanguage.ENGLISH;
    }
}


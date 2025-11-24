package jp.kitabatakep.intellij.plugins.codereadingnote.sync;

import jp.kitabatakep.intellij.plugins.codereadingnote.CodeReadingNoteBundle;

/**
 * 同步提供者类型枚举
 */
public enum SyncProviderType {
    
    /**
     * GitHub同步
     */
    GITHUB("sync.provider.github", "sync.provider.github.description"),
    
    /**
     * Gitee同步（未来支持）
     */
    GITEE("sync.provider.gitee", "sync.provider.gitee.description"),
    
    /**
     * WebDAV同步（未来支持）
     */
    WEBDAV("sync.provider.webdav", "sync.provider.webdav.description"),
    
    /**
     * 本地文件系统同步（未来支持）
     */
    LOCAL_FILE("sync.provider.local", "sync.provider.local.description");
    
    private final String displayNameKey;
    private final String descriptionKey;
    
    SyncProviderType(String displayNameKey, String descriptionKey) {
        this.displayNameKey = displayNameKey;
        this.descriptionKey = descriptionKey;
    }
    
    public String getDisplayName() {
        return CodeReadingNoteBundle.message(displayNameKey);
    }
    
    public String getDescription() {
        return CodeReadingNoteBundle.message(descriptionKey);
    }
}


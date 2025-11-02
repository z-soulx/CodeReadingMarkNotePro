package jp.kitabatakep.intellij.plugins.codereadingnote.sync;

/**
 * 同步提供者类型枚举
 */
public enum SyncProviderType {
    
    /**
     * GitHub同步
     */
    GITHUB("GitHub", "Sync notes using GitHub repository"),
    
    /**
     * Gitee同步（未来支持）
     */
    GITEE("Gitee (future support)", "Sync notes using Gitee repository"),
    
    /**
     * WebDAV同步（未来支持）
     */
    WEBDAV("WebDAV (future support)", "Sync notes using WebDAV protocol"),
    
    /**
     * 本地文件系统同步（未来支持）
     */
    LOCAL_FILE("Local File (future support)", "Sync to local file system directory");
    
    private final String displayName;
    private final String description;
    
    SyncProviderType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
}


package jp.kitabatakep.intellij.plugins.codereadingnote.sync.github;

import com.intellij.openapi.util.text.StringUtil;
import jp.kitabatakep.intellij.plugins.codereadingnote.sync.SyncConfig;
import jp.kitabatakep.intellij.plugins.codereadingnote.sync.SyncProviderType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * GitHub同步配置
 */
public class GitHubSyncConfig extends SyncConfig {
    
    // 配置键常量
    private static final String KEY_REPOSITORY = "repository";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_BRANCH = "branch";
    private static final String KEY_BASE_PATH = "basePath";
    
    // 默认值
    private static final String DEFAULT_BRANCH = "main";
    private static final String DEFAULT_BASE_PATH = "code-reading-notes";
    
    public GitHubSyncConfig() {
        super(SyncProviderType.GITHUB);
        // 设置默认值
        setBranch(DEFAULT_BRANCH);
        setBasePath(DEFAULT_BASE_PATH);
    }
    
    /**
     * GitHub仓库地址 (格式: owner/repo)
     */
    @Nullable
    public String getRepository() {
        return getProperty(KEY_REPOSITORY);
    }
    
    public void setRepository(@Nullable String repository) {
        setProperty(KEY_REPOSITORY, repository);
    }
    
    /**
     * GitHub Personal Access Token
     */
    @Nullable
    public String getToken() {
        return getProperty(KEY_TOKEN);
    }
    
    public void setToken(@Nullable String token) {
        setProperty(KEY_TOKEN, token);
    }
    
    /**
     * 分支名称
     */
    @NotNull
    public String getBranch() {
        String branch = getProperty(KEY_BRANCH);
        return branch != null ? branch : DEFAULT_BRANCH;
    }
    
    public void setBranch(@NotNull String branch) {
        setProperty(KEY_BRANCH, branch);
    }
    
    /**
     * 远程仓库中的基础路径
     */
    @NotNull
    public String getBasePath() {
        String path = getProperty(KEY_BASE_PATH);
        return path != null ? path : DEFAULT_BASE_PATH;
    }
    
    public void setBasePath(@NotNull String basePath) {
        setProperty(KEY_BASE_PATH, basePath);
    }
    
    @Override
    @Nullable
    public String validate() {
        if (StringUtil.isEmpty(getRepository())) {
            return "Please enter GitHub repository address (format: owner/repo)";
        }
        
        String repo = getRepository();
        if (repo != null && !repo.matches("^[\\w-]+/[\\w.-]+$")) {
            return "Repository address format incorrect, should be: owner/repo";
        }
        
        if (StringUtil.isEmpty(getToken())) {
            return "Please enter GitHub Personal Access Token";
        }
        
        if (StringUtil.isEmpty(getBranch())) {
            return "Please enter branch name";
        }
        
        return null;
    }
    
    @Override
    @NotNull
    public GitHubSyncConfig clone() {
        GitHubSyncConfig cloned = new GitHubSyncConfig();
        cloned.copyFrom(this);
        return cloned;
    }
}


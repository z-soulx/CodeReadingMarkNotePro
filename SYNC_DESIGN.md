# Code Reading Note Pro - 同步功能架构设计

## 设计目标

创建一个**可扩展的第三方同步架构**，支持多种同步服务提供商（GitHub、GitLab、Gitee、云存储等）。

## 核心设计原则

1. **开闭原则** - 对扩展开放，对修改关闭
2. **依赖倒置** - 依赖抽象而非具体实现
3. **单一职责** - 每个类只负责一个功能
4. **插件化设计** - 新的同步服务可以通过实现接口轻松添加

## 架构设计

### 1. 核心接口层

```java
// 同步服务提供者接口
public interface SyncProvider {
    String getProviderId();           // 唯一标识: "github", "gitlab", etc.
    String getProviderName();         // 显示名称: "GitHub", "GitLab", etc.
    Icon getProviderIcon();           // 提供者图标
    
    // 认证相关
    boolean isAuthenticated();
    void authenticate(SyncConfig config) throws SyncException;
    void logout();
    
    // 同步操作
    SyncResult push(Project project, String content) throws SyncException;
    SyncResult pull(Project project) throws SyncException;
    SyncStatus getStatus(Project project) throws SyncException;
    
    // 配置管理
    SyncConfigPanel createConfigPanel();
    boolean validateConfig(SyncConfig config);
}

// 同步配置基类
public abstract class SyncConfig {
    private String providerId;
    private boolean enabled;
    private SyncMode syncMode;  // AUTO, MANUAL
    
    // 子类扩展具体配置项
}

// 同步结果
public class SyncResult {
    private boolean success;
    private SyncAction action;  // PUSH, PULL
    private String message;
    private long timestamp;
    private ConflictInfo conflict;  // 如果有冲突
}

// 同步状态
public class SyncStatus {
    private boolean localModified;
    private boolean remoteModified;
    private long localTimestamp;
    private long remoteTimestamp;
}
```

### 2. GitHub实现层

```java
// GitHub特定配置
public class GitHubSyncConfig extends SyncConfig {
    private String repositoryUrl;     // 独立仓库地址
    private String personalAccessToken;
    private String branch;            // 默认 "main"
    private boolean autoSync;         // 自动同步开关
}

// GitHub同步实现
public class GitHubSyncProvider implements SyncProvider {
    @Override
    public String getProviderId() { return "github"; }
    
    @Override
    public String getProviderName() { return "GitHub"; }
    
    // 使用 GitHub API 实现同步逻辑
    // 文件路径: projects/{projectHash}/CodeReadingNote.xml
}
```

### 3. 未来扩展示例

```java
// GitLab实现
public class GitLabSyncProvider implements SyncProvider { ... }

// Gitee实现
public class GiteeSyncProvider implements SyncProvider { ... }

// WebDAV实现 (如坚果云、NextCloud)
public class WebDAVSyncProvider implements SyncProvider { ... }

// 对象存储实现 (如阿里云OSS、AWS S3)
public class OSSyncProvider implements SyncProvider { ... }
```

### 4. 服务管理层

```java
// 同步服务管理器
@Service
public class SyncServiceManager {
    private final Map<String, SyncProvider> providers = new HashMap<>();
    private SyncConfig currentConfig;
    
    // 注册提供者
    public void registerProvider(SyncProvider provider) {
        providers.put(provider.getProviderId(), provider);
    }
    
    // 获取当前启用的提供者
    public SyncProvider getCurrentProvider() {
        if (currentConfig == null || !currentConfig.isEnabled()) {
            return null;
        }
        return providers.get(currentConfig.getProviderId());
    }
    
    // 执行同步
    public SyncResult sync(Project project, SyncAction action) {
        SyncProvider provider = getCurrentProvider();
        if (provider == null) {
            return SyncResult.disabled();
        }
        
        return action == SyncAction.PUSH 
            ? provider.push(project, getLocalContent(project))
            : provider.pull(project);
    }
}

// 同步协调器 - 处理冲突、合并等复杂逻辑
public class SyncCoordinator {
    public SyncResult syncWithConflictResolution(Project project) {
        // 1. 检查本地和远程状态
        // 2. 如果有冲突，弹出冲突解决UI
        // 3. 执行合并或覆盖操作
    }
}
```

### 5. UI扩展层

```java
// Settings配置面板
public class SyncSettingsConfigurable implements Configurable {
    private JComboBox<SyncProvider> providerSelector;  // 选择同步服务
    private JPanel configPanel;  // 动态加载对应提供者的配置面板
    
    // 当切换提供者时，动态加载对应的配置UI
}

// ToolWindow扩展
public class ManagementPanel {
    private JButton syncButton;  // 新增同步按钮
    private JLabel syncStatusLabel;  // 同步状态显示
    
    private void addSyncActions() {
        // Push Action
        // Pull Action
        // Sync Status Action
    }
}
```

## 数据存储设计

### 远程仓库结构
```
code-reading-notes-sync/  (独立的GitHub仓库)
├── projects/
│   ├── <project-hash-1>/
│   │   ├── CodeReadingNote.xml
│   │   └── .metadata.json  (项目信息)
│   ├── <project-hash-2>/
│   │   ├── CodeReadingNote.xml
│   │   └── .metadata.json
│   └── ...
├── .sync-config.json  (全局同步配置)
└── README.md
```

### .metadata.json 结构
```json
{
  "projectName": "MyProject",
  "projectPath": "/path/to/project",  // 仅供参考
  "lastSyncTime": 1234567890,
  "version": "2024.3.0"
}
```

### .sync-config.json 结构
```json
{
  "version": "1.0",
  "projects": {
    "hash1": {
      "name": "ProjectA",
      "lastSync": 1234567890
    }
  }
}
```

## 持久化配置

### 插件级别配置
```xml
<!-- 存储在 IDE 级别配置中 -->
<component name="CodeReadingNoteSyncSettings">
  <sync>
    <provider>github</provider>
    <enabled>true</enabled>
    <autoSync>false</autoSync>
    <!-- 具体提供者配置 -->
    <github>
      <repositoryUrl>https://github.com/user/code-reading-notes</repositoryUrl>
      <token>encrypted_token</token>
      <branch>main</branch>
    </github>
  </sync>
</component>
```

## 同步策略

### 冲突解决策略
1. **本地优先** (Local First) - 本地修改覆盖远程
2. **远程优先** (Remote First) - 远程修改覆盖本地
3. **手动解决** (Manual) - 弹出对话框让用户选择
4. **智能合并** (Smart Merge) - 尝试自动合并（基于时间戳）

### 自动同步触发时机
- 项目打开时（Pull）
- 项目关闭时（Push）
- 手动触发
- 定时同步（可配置）

## 安全性考虑

1. **Token加密** - 使用IntelliJ的PasswordSafe API加密存储
2. **HTTPS通信** - 所有API调用使用HTTPS
3. **权限控制** - 只请求必要的仓库权限
4. **私密信息过滤** - 可选择性排除敏感路径的笔记

## 实现路线图

### Phase 1: 核心框架 (第1周)
- [ ] 定义核心接口和抽象类
- [ ] 实现SyncServiceManager
- [ ] 实现配置持久化
- [ ] 基础UI框架

### Phase 2: GitHub实现 (第2周)
- [ ] GitHub API集成
- [ ] GitHub认证流程
- [ ] Push/Pull实现
- [ ] 冲突检测

### Phase 3: UI和用户体验 (第3周)
- [ ] Settings配置页面
- [ ] ToolWindow集成
- [ ] 同步状态显示
- [ ] 进度提示和错误处理

### Phase 4: 高级特性 (第4周)
- [ ] 冲突解决UI
- [ ] 自动同步
- [ ] 同步历史记录
- [ ] 测试和优化

## 扩展性验证

新增同步服务提供者只需要：
1. 实现 `SyncProvider` 接口
2. 创建对应的 `SyncConfig` 子类
3. 创建配置UI面板
4. 在服务启动时注册到 `SyncServiceManager`

**无需修改任何现有代码！**

## 依赖库

- **GitHub**: OkHttp + GitHub API v3
- **Future GitLab**: GitLab4J-API
- **Future WebDAV**: Sardine
- **JSON处理**: Gson
- **加密**: IntelliJ PasswordSafe API

---

这个设计确保了系统的可扩展性，同时保持了代码的清晰和可维护性。


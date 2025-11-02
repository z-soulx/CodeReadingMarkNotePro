# GitHub Token 认证修复

## 问题描述

用户使用 **Fine-grained personal access token** 时报错：
```
Token验证失败，请检查访问权限
```

即使已经正确配置了 `Contents: Read and write` 权限。

## 根本原因

GitHub 有两种 Token 类型，使用**不同的认证方式**：

### Classic Token（旧版）
- **Token 格式**: `ghp_xxxxxxxxxxxx`
- **认证方式**: `Authorization: token ghp_xxxxxxxxxxxx`
- **权限粒度**: 粗粒度（repo 包含所有权限）

### Fine-grained Token（新版）
- **Token 格式**: `github_pat_xxxxxxxxxxxx`  
- **认证方式**: `Authorization: Bearer github_pat_xxxxxxxxxxxx` ← **注意是 Bearer**
- **权限粒度**: 细粒度（可单独控制 Contents、Issues 等）

## 代码问题

之前的代码只支持 Classic Token：

```java
// 旧代码 - 只支持 Classic Token
conn.setRequestProperty("Authorization", "token " + token);
```

当使用 Fine-grained Token 时，GitHub API 返回 401 Unauthorized。

## 修复方案

### 自动识别 Token 类型

```java
// 新代码 - 自动识别 Token 类型
if (token.startsWith("github_pat_")) {
    // Fine-grained token
    conn.setRequestProperty("Authorization", "Bearer " + token);
} else {
    // Classic token
    conn.setRequestProperty("Authorization", "token " + token);
}
```

### 识别逻辑

- Fine-grained token 以 `github_pat_` 开头 → 使用 `Bearer`
- Classic token 以 `ghp_`、`gho_` 等开头 → 使用 `token`

## 配置文件位置（问题2答案）

### Windows
```
%USERPROFILE%\.IntelliJIdea2024.3\config\options\codeReadingNoteSync.xml

具体路径示例：
C:\Users\你的用户名\.IntelliJIdea2024.3\config\options\codeReadingNoteSync.xml
```

### Linux
```
~/.IntelliJIdea2024.3/config/options/codeReadingNoteSync.xml

具体路径示例：
/home/你的用户名/.IntelliJIdea2024.3/config/options/codeReadingNoteSync.xml
```

### macOS
```
~/Library/Application Support/JetBrains/IntelliJIdea2024.3/options/codeReadingNoteSync.xml

具体路径示例：
/Users/你的用户名/Library/Application Support/JetBrains/IntelliJIdea2024.3/options/codeReadingNoteSync.xml
```

### 配置文件内容示例

```xml
<?xml version="1.0" encoding="UTF-8"?>
<application>
  <component name="CodeReadingNoteSyncSettings">
    <State>
      <option name="enabled" value="true" />
      <option name="autoSync" value="false" />
      <option name="providerType" value="GITHUB" />
      <option name="properties">
        <map>
          <entry key="repository" value="username/my-notes" />
          <entry key="token" value="github_pat_xxxxxxxxxxxx" />
          <entry key="branch" value="main" />
          <entry key="basePath" value="code-reading-notes" />
        </map>
      </option>
    </State>
  </component>
</application>
```

### 如何找到配置文件

**方法 1: 通过 IDE 查找**
```
Help → Show Log in Explorer/Finder
→ 向上一级目录
→ 进入 config/options/
→ 找到 codeReadingNoteSync.xml
```

**方法 2: 直接访问**

Windows:
```cmd
explorer %USERPROFILE%\.IntelliJIdea2024.3\config\options
```

Linux/Mac:
```bash
cd ~/.IntelliJIdea2024.3/config/options
ls -la codeReadingNoteSync.xml
```

### 手动编辑配置文件

如果 UI 保存失败，可以：

1. **关闭 IntelliJ IDEA**
2. **手动编辑** `codeReadingNoteSync.xml`
3. **保存文件**
4. **重新启动 IDE**

## 使用步骤（修复后）

### 1. 重新编译插件
```
修改代码后需要重新构建插件
Build → Build Project
```

### 2. 配置 Fine-grained Token

在 GitHub 创建 Token 时确保：
```
Repository access:
  ● Only select repositories → 选择你的笔记仓库

Repository permissions:
  ☑ Contents: Read and write  ← 必须！
  ☑ Metadata: Read           ← 自动包含
```

### 3. 在插件中配置

```
Settings → Tools → Code Reading Note Sync
→ 仓库地址: username/my-notes
→ 访问令牌: github_pat_11AAAAAA...  ← 粘贴 Fine-grained token
→ Apply
```

### 4. 测试验证

点击 **Push to Remote** 测试推送功能。

## Token 前缀对照

| Token 类型 | 前缀 | 认证方式 |
|-----------|------|---------|
| Fine-grained | `github_pat_` | `Bearer` |
| Classic (Personal) | `ghp_` | `token` |
| OAuth App | `gho_` | `token` |
| GitHub App | `ghs_` | `token` |
| Refresh token | `ghr_` | N/A |

## 验证 Token 是否有效

### 使用 curl 测试

**Fine-grained Token**:
```bash
curl -H "Authorization: Bearer github_pat_xxxxx" \
  https://api.github.com/repos/username/repo
```

**Classic Token**:
```bash
curl -H "Authorization: token ghp_xxxxx" \
  https://api.github.com/repos/username/repo
```

如果返回 200 和仓库信息，说明 Token 有效且权限正确。

## 故障排查

### 仍然返回 401

1. **检查 Token 是否过期**
   - GitHub → Settings → Developer settings → Fine-grained tokens
   - 查看 Token 的 Expiration

2. **检查仓库访问权限**
   - Token 是否授权访问了目标仓库
   - Repository access 设置是否正确

3. **检查 Contents 权限**
   - Repository permissions → Contents: Read and write

4. **检查 Token 格式**
   - Fine-grained token 应该以 `github_pat_` 开头
   - 复制时没有多余的空格

### 返回 403

- Token 权限不足
- 或达到 API 速率限制

### 返回 404

- 仓库地址错误
- 或 Token 没有访问该仓库的权限

## 配置文件问题排查

### 配置没有保存

1. **检查文件是否存在**
   ```bash
   # 检查配置文件
   ls -la ~/.IntelliJIdea2024.3/config/options/codeReadingNoteSync.xml
   ```

2. **检查文件权限**
   ```bash
   # 确保有写入权限
   chmod 644 ~/.IntelliJIdea2024.3/config/options/codeReadingNoteSync.xml
   ```

3. **查看 IDE 日志**
   ```
   Help → Show Log in Explorer
   搜索 "CodeReadingNoteSyncSettings" 相关错误
   ```

### 手动创建配置文件

如果自动保存失败，可以手动创建：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<application>
  <component name="CodeReadingNoteSyncSettings">
    <State>
      <option name="enabled" value="true" />
      <option name="autoSync" value="false" />
      <option name="providerType" value="GITHUB" />
      <option name="properties">
        <map>
          <entry key="repository" value="你的用户名/仓库名" />
          <entry key="token" value="github_pat_你的token" />
          <entry key="branch" value="main" />
          <entry key="basePath" value="code-reading-notes" />
        </map>
      </option>
    </State>
  </component>
</application>
```

保存到正确的路径，重启 IDE。

## 总结

1. ✅ **修复了 Fine-grained Token 认证问题**
   - 自动识别 Token 类型
   - 使用正确的认证方式

2. ✅ **配置文件位置明确**
   - Windows: `%USERPROFILE%\.IntelliJIdea2024.3\config\options\codeReadingNoteSync.xml`
   - Linux/Mac: `~/.IntelliJIdea2024.3/config/options/codeReadingNoteSync.xml`

3. ✅ **向后兼容**
   - 同时支持 Classic Token 和 Fine-grained Token
   - 无需用户指定 Token 类型

现在使用 Fine-grained Token 应该可以正常工作了！🎉

---

**修复版本**: v3.4.0-fix2  
**修复日期**: 2024-11-01



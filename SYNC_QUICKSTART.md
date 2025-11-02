# 同步功能快速开始 🚀

## 5分钟配置指南

### 步骤1: 创建GitHub仓库 (2分钟)

1. 登录GitHub
2. 点击右上角 `+` → `New repository`
3. 填写信息：
   - **Repository name**: `my-code-reading-notes`
   - **Description**: (可选) "代码阅读笔记同步仓库"
   - **Private**: ✅ 勾选（推荐使用私有仓库）
4. 点击 `Create repository`

### 步骤2: 生成访问Token (2分钟)

1. GitHub右上角头像 → `Settings`
2. 左侧菜单最下方 → `Developer settings`
3. `Personal access tokens` → `Tokens (classic)`
4. `Generate new token (classic)`
5. 填写信息：
   - **Note**: `CodeReadingNoteSync`
   - **Expiration**: 选择有效期（建议90天或无限期）
   - **Select scopes**: ✅ 勾选 `repo` (完整权限)
6. 点击 `Generate token`
7. **重要**: 复制Token并保存（只显示一次！）

### 步骤3: 配置插件 (1分钟)

1. IntelliJ IDEA中打开：
   ```
   File → Settings → Tools → Code Reading Note Sync
   (Mac: IntelliJ IDEA → Preferences → Tools → Code Reading Note Sync)
   ```

2. 填写配置：
   ```
   ☑ 启用同步
   同步方式: [GitHub ▼]
   ☐ 自动同步 (可选)
   
   ━━━━ 同步配置 ━━━━
   仓库地址:     your-username/my-code-reading-notes
   访问令牌:     ghp_xxxxxxxxxxxxxxxxxxxx (粘贴刚才的Token)
   分支:         main
   基础路径:     code-reading-notes
   ```

3. 点击 `Apply` 和 `OK`

### 步骤4: 开始使用 ✨

#### 推送笔记到GitHub

1. 打开工具窗口: `View → Tool Windows → Code Reading Mark Note Pro`
2. 点击工具栏的 **⬆ Push to Remote** 按钮
3. 等待提示 "推送成功"
4. 去GitHub仓库查看，应该能看到类似这样的结构：
   ```
   my-code-reading-notes/
   └── code-reading-notes/
       └── AbC123XyZ456/
           └── CodeReadingNote.xml
   ```

#### 从GitHub拉取笔记

1. 在另一台电脑或清空本地笔记后
2. 点击工具栏的 **⬇ Pull from Remote** 按钮
3. 选择模式：
   - `合并`: 保留本地笔记，添加远程笔记
   - `覆盖`: 完全用远程笔记替换本地
4. 点击 `是` 或 `否`
5. 等待提示 "拉取成功"

## 典型使用场景

### 场景A: 公司和家里两台电脑同步

**公司电脑** (第一次):
```
1. 阅读代码，做笔记
2. Push ⬆ 推送到GitHub
```

**家里电脑**:
```
1. Pull ⬇ 拉取 (选择"合并")
2. 继续阅读，添加新笔记
3. Push ⬆ 推送更新
```

**第二天公司电脑**:
```
1. Pull ⬇ 拉取 (选择"合并")
2. 获得昨晚在家做的笔记
```

### 场景B: 多个项目使用同一个同步仓库

不用担心冲突！每个项目直接使用项目名称作为文件夹名：

```
my-code-reading-notes/
└── code-reading-notes/
    ├── myjava/                      ← 项目名称直接可见！
    │   └── CodeReadingNote.xml
    ├── spring-boot-demo/            ← 一目了然
    │   └── CodeReadingNote.xml
    └── vue-project/                 ← 清晰易读
        └── CodeReadingNote.xml
```

**优势**：
- ✅ **可读性强**：在GitHub上直接看到项目名称
- ✅ **易于管理**：需要手动处理时知道是哪个项目
- ✅ **跨设备同步**：只要项目名相同，就能命中同一笔记
- ✅ **移动项目**：更改项目路径后，仍能同步到同一位置
- ⚠️ **注意**：重命名项目会生成新文件夹（可在GitHub上手动重命名文件夹）

### 场景C: 备份笔记防止丢失

定期Push即可：
```
做笔记 → Push ⬆ → GitHub自动保存历史版本
```

如果本地数据丢失：
```
Pull ⬇ (覆盖模式) → 恢复所有笔记
```

## 常见问题速查

### ❌ "推送失败: 401 Unauthorized"
**原因**: Token无效或权限不足  
**解决**: 
1. 检查Token是否正确复制
2. 确认Token有 `repo` 权限
3. Token可能已过期，重新生成

### ❌ "推送失败: 404 Not Found"
**原因**: 仓库地址错误  
**解决**: 
1. 检查格式: `username/repo-name` (不要有空格)
2. 确认仓库已创建
3. 确认Token对该仓库有访问权限

### ❌ "远程文件不存在"
**原因**: 首次使用，还没Push过  
**解决**: 先执行一次 Push

### ⚠️ 推送很慢
**原因**: 网络问题或笔记数据量大  
**解决**: 
1. 检查网络连接
2. 如在国内，可能是GitHub访问较慢
3. 未来版本会支持Gitee等国内平台

## 进阶技巧

### 技巧1: 自动同步

启用自动同步后，每次修改笔记自动Push：
```
☑ 自动同步
```
**注意**: 需要稳定的网络连接

### 技巧2: 使用不同分支

可以创建多个配置使用不同分支：
- `main`: 稳定笔记
- `draft`: 草稿笔记
- `team`: 团队共享笔记

### 技巧3: 团队协作

1. 创建Organization仓库
2. 邀请团队成员
3. 每人配置相同的同步设置
4. 使用"合并"模式Pull，共享笔记

### 技巧4: 查看同步历史

在GitHub仓库页面：
```
Commits → 查看每次推送的历史记录
```

可以看到何时、何人推送了哪些更改。

## 下一步

- 📖 阅读 [完整使用文档](SYNC_USAGE.md)
- 🏗️ 了解 [架构设计](SYNC_DESIGN.md)
- 💡 查看 [实现总结](SYNC_IMPLEMENTATION_SUMMARY.md)

## 获取帮助

- **问题反馈**: 提交GitHub Issue
- **使用建议**: 邮件 170918810@qq.com
- **插件主页**: [JetBrains插件市场](https://plugins.jetbrains.com/plugin/24163-code-reading-mark-note-pro)

---

祝您使用愉快！🎉


# Code Reading Note Pro - 同步功能

## 📦 功能介绍

Code Reading Note Pro v3.4.0 新增了强大的**第三方同步功能**，支持将代码阅读笔记同步到独立的远程仓库。

### ✨ 核心特性

- 🔄 **跨设备同步** - 在不同机器间无缝同步笔记
- 🏢 **独立存储** - 笔记存储在独立仓库，不污染项目代码
- 🌐 **多项目支持** - 一个同步仓库管理多个项目的笔记
- 👀 **可读性强** - 直接使用项目名称，GitHub上一目了然
- 🔀 **灵活策略** - 支持合并和覆盖两种同步模式
- 🔌 **可扩展架构** - 设计支持未来添加更多同步方式
- 📝 **GitHub支持** - v1.0首发支持GitHub同步

## 🚀 快速开始

### 1分钟快速配置

```
1. 创建GitHub仓库 (私有推荐)
2. 生成Personal Access Token (需要 repo 权限)
3. 在IDE中配置: Settings → Tools → Code Reading Note Sync
4. 填写仓库地址和Token
5. 点击Apply保存
```

### 立即使用

```
推送笔记: 点击工具栏 ⬆ Push to Remote
拉取笔记: 点击工具栏 ⬇ Pull from Remote
```

详细步骤请参考 [快速开始指南](SYNC_QUICKSTART.md)

## 📚 文档

| 文档 | 说明 |
|------|------|
| [快速开始](SYNC_QUICKSTART.md) | 5分钟配置和使用指南 |
| [使用手册](SYNC_USAGE.md) | 完整的功能说明和使用技巧 |
| [架构设计](SYNC_DESIGN.md) | 技术架构和扩展指南 |
| [实现总结](SYNC_IMPLEMENTATION_SUMMARY.md) | 开发实现细节 |

## 🎯 使用场景

### 场景1: 个人跨设备同步
```
公司电脑 ⬆ Push → GitHub → ⬇ Pull 家里电脑
```

### 场景2: 团队知识共享
```
成员A ⬆ Push →  共享仓库  ← ⬇ Pull 成员B
              ↓
         ⬇ Pull 成员C
```

### 场景3: 笔记备份
```
本地笔记 ⬆ Push → GitHub永久保存 + 版本历史
```

## 🏗️ 架构特点

### 可扩展设计

采用**策略模式 + 工厂模式**，添加新同步方式只需实现接口：

```java
// 接口层
SyncProvider (接口)
├── AbstractSyncProvider (抽象基类)
└── [实现]
    ├── GitHubSyncProvider ✅ 已实现
    ├── GiteeSyncProvider ⏳ 计划中
    ├── WebDAVSyncProvider ⏳ 计划中
    └── LocalFileSyncProvider ⏳ 计划中
```

### 模块结构

```
sync/
├── SyncProvider.java           # 核心接口
├── SyncService.java            # 同步服务
├── SyncSettings.java           # 配置持久化
├── github/                     # GitHub实现
│   ├── GitHubSyncProvider.java
│   └── GitHubSyncConfig.java
└── ui/                         # 用户界面
    ├── SyncConfigurable.java
    └── SyncSettingsPanel.java
```

## 🔧 技术实现

### 核心功能

- ✅ 推送/拉取笔记数据
- ✅ 基于时间戳的智能合并
- ✅ 项目唯一标识符生成
- ✅ 配置持久化
- ✅ 后台任务执行
- ✅ 错误处理和用户反馈

### GitHub集成

- GitHub Contents API
- Base64编码传输
- SHA版本控制
- 文件创建和更新
- 时间戳查询

## 📊 版本规划

### v3.4.0 (当前版本)
- ✅ 核心同步架构
- ✅ GitHub同步支持
- ✅ 配置界面
- ✅ 推送/拉取操作

### v3.5.0 (计划中)
- ⏳ Gitee同步支持
- ⏳ 冲突检测和可视化
- ⏳ 同步历史记录

### v3.6.0 (远期)
- ⏳ WebDAV支持
- ⏳ 增量同步优化
- ⏳ 自动定时同步

## 🤝 贡献

欢迎为同步功能贡献代码！

### 添加新的同步方式

1. 实现 `SyncProvider` 接口
2. 创建对应的 `SyncConfig` 子类
3. 在 `SyncProviderFactory` 中注册
4. 添加UI配置面板
5. 编写测试用例
6. 更新文档

示例请参考 [架构设计文档](SYNC_DESIGN.md)

## ⚠️ 注意事项

### 安全性

- ✅ Token存储在IDE配置中，设置好文件权限
- ✅ 建议使用私有仓库
- ✅ 定期更换Token
- ✅ 不要将Token提交到代码仓库

### 数据保护

- ✅ 推送前自动导出备份
- ✅ 建议定期使用导出功能本地备份
- ✅ 合并模式下不会丢失本地数据
- ⚠️ 覆盖模式会清空本地数据，请谨慎使用

## 📞 支持

### 获取帮助

- 📖 查看文档: [SYNC_USAGE.md](SYNC_USAGE.md)
- 🐛 报告问题: [GitHub Issues](https://github.com/your-repo/issues)
- 📧 联系开发者: 170918810@qq.com
- 🌐 插件主页: [JetBrains插件市场](https://plugins.jetbrains.com/plugin/24163-code-reading-mark-note-pro)

### 常见问题

详见 [快速开始指南 - 常见问题](SYNC_QUICKSTART.md#常见问题速查)

## 📄 许可证

本功能遵循插件整体许可证。

## 🙏 致谢

感谢所有使用和反馈的用户！

---

**版本**: v3.4.0  
**发布日期**: 2024-11-01  
**状态**: ✅ 稳定可用


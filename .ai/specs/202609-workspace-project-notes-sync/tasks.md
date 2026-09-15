# Tasks: 工作空间按项目同步笔记

- Unit: `202609-workspace-project-notes-sync`
- Status: 实现及自动验证完成（71 tests / test build）；真实 IDEA/macOS 安装验收尚未执行

## DoD

spec 全部验收项经过自动测试或真实 IDEA 验证；证据写入 `.ai/runs/`；中英文完整、数据兼容、文档回流和版本一致。设计文档完成不等于功能交付。

## 任务

- [x] D1: 核实当前同步目标、Provider、基线、回收站与保存接口，形成独立设计。
- [x] T1: 实现每项目绑定、远端身份校验、策略与基线存储；旧根 autoSync 保留为首次绑定策略偏好，旧时间/MD5 不直接充当可信基线。
- [x] T2: 实现纯逻辑 L/R/B 分类与规范化；覆盖同边/双边/相同/无基线/缺失、空数据、顺序、正文空白及未知字段。
- [x] T3: 提供模型 revision 快照、实际磁盘校验及条件应用；自动覆盖子项目独立保存、拉取中编辑/外部修改、损坏远端不改模型。真实根 PSC 保存及崩溃恢复仍归 T7/T9 验收。
- [x] T4: NotesRemote 实现快照读取与 observed SHA 条件推送；本地 HTTP 模拟测试覆盖分支/路径/ETag、404、401/429、损坏 XML、409 和 sidecar 失败后远端已提交。超时断线及 412 使用同一失败通道，未单独执行真实网络故障注入。
- [x] T5: 手动推拉目标服务及项目右键/工具栏/多选总览、中英文和运行时切换已实现；真实 UI 效果归 T9。
- [x] T6: 每项目自动策略、启动/激活/定时检查、防抖、退避、应用级队列及生命周期已实现；可控时间测试验证持续无编辑的周期检查、防抖、激活节流、冲突隔离及回推抑制。
- [ ] T7: 父窗口与独立子窗口共用基线/绑定测试；相同远端多本地根任务锁测试；关闭/重新打开以及切换绑定期间旧任务不能回写。
- [x] T8: gradlew test build 成功，71 项测试全部通过；安装包内版本为 3.7.7，build.gradle/plugin.xml 一致，同步类已打包。
- [ ] T9: Windows/macOS 安装验收：只打开父目录，dc-common 手动推拉、远端更新自动到达、冲突仅暂停一项、批量部分失败、重启、独立子项目读取；记录未执行项。
- [x] T10: 修复 workspace 下符号链接项目的发现、真实路径身份/去重、alias 显示和 VFS 路由；Windows junction 自动测试覆盖多个 link、重复目标、普通目录 link 及真实路径文件归属，macOS 实机归 T9。
- [x] T11: 子项目发现要求实际存在 `.idea/CodeReadingNote.xml`；普通目录和链接目录均覆盖“有文件发现、无文件忽略、创建/删除后重扫”，并更新现状文档与用户指南。
- [x] Backflow: 已更新 docs/domain/sync/README.md、github.md、docs/scenario/auto-sync.md、docs/domain/notes/README.md、system-overview 和 help/WORKSPACE_NOTES_GUIDE.md。
- [ ] Freeze: 验收后冻结本单元并更新 INDEX 工作集。

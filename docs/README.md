# docs/ - 稳态知识库

描述系统**当前状态**的中文文档：架构、领域知识、场景流程、外部边界、术语与运维。
面向「未来的你」和 AI 助手，叙事性书写，持续累加。

> 想改变系统？不要在这里写计划 --去 `.ai/specs/` 建变更单元。
> 想知道某个设计为什么这样定？看 `.ai/adr/`。
> 快速路由见 `.ai/context/INDEX.md`。

## 目录地图

| 目录 | 内容 | 什么时候读 |
|------|------|-----------|
| `architecture/` | 全局拓扑、组件分层、事件通信 | 想了解系统整体长什么样 |
| `domain/notes/` | 笔记领域：主题/行/分组/回收站、行号标记 | 改笔记相关功能前 |
| `domain/sync/` | 同步域：双通道、增量推送、冲突检测 | 改同步相关功能前 |
| `domain/sync/github.md` | GitHub Provider 特有行为 | 处理 GitHub 报错、API 细节 |
| `domain/ai-config/` | AI 配置工作台：扫描/忽略/空目录/树 UI | 改 AI 配置相关功能前 |
| `scenario/` | 端到端业务流程（场景视角） | 追一个用户流程走完整链路时 |
| `integration/` | 外部系统边界（GitHub API、IntelliJ 平台） | 处理边界协议、平台依赖时 |
| `shared/glossary.md` | 统一术语表 | 术语含糊时先对齐再动手 |
| `runbooks/` | 运维 SOP（版本发布等） | 执行例行操作时 |

## 阅读路线

- **新人路线**：`shared/glossary.md` -> `architecture/overview.md` -> 任一 domain 页
- **改同步**：`domain/sync/README.md` -> `scenario/push-pull-ai-configs.md` -> `integration/github-api.md`
- **排查同步问题**：`domain/sync/github.md` -> `integration/github-api.md`

## 维护约定

- 文档内容与代码现状不一致时，以代码为准，**当场修正文档**（顺手回流）
- 每次变更单元冻结时，其 tasks 里的回流任务负责更新本目录
- 本目录是累积式知识层：旧内容只要仍真实就不删，失真的内容直接改写

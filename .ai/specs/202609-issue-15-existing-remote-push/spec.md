# Spec: 已有远端笔记更新失败与 3.7.7 交付

- Status: verification
- Type: bugfix
- Source: https://github.com/z-soulx/CodeReadingMarkNotePro/issues/15

## 意图

远端 CodeReadingNote.xml 已存在时应能正常更新，不要求用户删除文件。用户指定本次未发布的工作空间功能和修复统一交付 3.7.7。

## 背景与验收

issue 截图为 HTTP 422，错误提示仅剩 `Invalid request.\n\n\`。源码存在按默认分支读取 SHA、按配置分支 PUT，以及 SHA 读取失败返回 null（当成新建）的缺陷；JSON message 正则也会在转义引号处截断。截图未提供完整服务器报文，不能认定用户现场仅由单一因素引起。

- [x] 配置分支上的已有文件使用同分支 SHA 更新，连续推送成功，无 DELETE。
- [x] SHA 读取失败或响应缺少 SHA 时禁止作为新建提交；仅确认 404 才允许新建。
- [x] 保留并发 SHA 条件校验，不为消除 422 自动覆盖他人修改。
- [x] JSON 转义换行/引号完整解码；区分版本缺失、冲突、普通 422 校验失败；中英文提示给出操作建议，不泄露凭据。
- [x] 构建配置、描述符、变更说明及当前帮助统一 3.7.7；工作空间功能保留。
- [x] 自动回归和 test/build 通过，核对安装包版本，未执行真实用户仓库/IDEA 验收明确记录。

- [ ] 在真实用户仓库和安装后的 IDEA 中完成验收（待执行）。

自动验证证据：`.ai/runs/202609-issue-15-existing-remote-push-1.md`。

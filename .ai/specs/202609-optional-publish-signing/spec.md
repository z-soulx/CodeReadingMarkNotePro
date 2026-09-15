# 可选签名发布依赖修复

- Status: frozen
- Type: bugfix

## 意图

修复 3.7.7 发布时未配置签名仍解析 ZIP Signer 最新版本的失败；说明 Gradle 启动需使用 JDK 17。

## 验收

- [x] 未配置签名时发布前置任务无需下载 ZIP Signer。
- [x] 已配置签名时保留签名下载依赖，不静默退化为未签名发布。
- [x] 使用本机 JDK 17 验证任务图及未签名构建，不上传 Marketplace。
- [x] 更新中文发布指南和执行证据。

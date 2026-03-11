# 层级评估 — Thoughtworks Agents 开发平台

## 评估结论

所有四层都需要开发，这是一个全新项目。

## Domain 层

**评估**: ✅ 需要开发

需要设计和实现以下四个聚合：

1. **CCSession 聚合** — 管理 Claude Code CLI 子进程的生命周期，包括进程启动配置（ProcessConfig 值对象）、运行状态跟踪、输出流管理。这是最基础的聚合，被 Conversation 和 DevTask 依赖。
2. **Conversation 聚合** — 管理对话和消息记录，支持创建对话、添加消息、关联 GitHub 仓库、对话状态流转（CREATED → ACTIVE → COMPLETED）。
3. **DevTask 聚合** — 管理开发任务的三阶段流转（THINKING → WORKING → READY_TO_PUBLISH → PUBLISHING → PUBLISHED），每个阶段有独立的执行记录（TaskPhase 实体）。
4. **GitHubIntegration 聚合** — 管理 GitHub OAuth token 存储和仓库操作能力，包含 OAuthToken、Repository、Branch 值对象。

## Infrastructure 层

**评估**: ✅ 需要开发

需要实现：

1. **数据库持久化** — 四个聚合的 Repository 实现（H2 数据库），包括表设计、PO 对象、Mapper、Repository 实现
2. **Claude Code CLI 适配** — CCSession 的进程管理实现，封装 `ProcessBuilder` 调用 `claude` CLI
3. **GitHub API 客户端** — GitHub OAuth 流程实现和 API 调用（仓库列表、分支合并等）
4. **WebSocket 消息推送** — CC 输出流到 WebSocket 的桥接实现

## Application 层

**评估**: ✅ 需要开发

需要实现：

1. **ConversationApplicationService** — 创建对话、发送消息（触发 CC 会话）、获取对话历史
2. **DevTaskApplicationService** — 创建开发任务、启动开发（触发思考阶段）、推进阶段、发布（触发分支合并）
3. **CCSessionApplicationService** — 创建 CC 会话、管理会话生命周期
4. **GitHubApplicationService** — OAuth 回调处理、获取仓库列表、执行分支合并

## OHS 层

**评估**: ✅ 需要开发

需要实现：

1. **ConversationController** — 对话 CRUD 和消息发送的 REST API
2. **DevTaskController** — 任务创建、状态查询、发布操作的 REST API
3. **GitHubController** — OAuth 回调、仓库列表的 REST API
4. **WebSocketHandler** — CC 输出流的 WebSocket 推送端点

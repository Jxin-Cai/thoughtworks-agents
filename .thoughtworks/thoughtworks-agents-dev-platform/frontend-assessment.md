# 前端评估

## API 契约概要

后端 OHS 层提供 17 个 REST API 端点和 2 个 WebSocket 主题：

### Conversation API（5 端点）
- POST /api/conversations — 创建对话
- POST /api/conversations/{id}/messages — 发送消息
- GET /api/conversations — 对话列表（支持按仓库筛选）
- GET /api/conversations/{id} — 对话详情
- PUT /api/conversations/{id}/archive — 归档对话

### DevTask API（6 端点）
- POST /api/dev-tasks — 创建开发任务
- POST /api/dev-tasks/{id}/start — 启动开发
- POST /api/dev-tasks/{id}/advance — 推进到工作阶段
- POST /api/dev-tasks/{id}/publish — 执行发布
- GET /api/dev-tasks/{id} — 任务详情
- GET /api/dev-tasks — 任务列表

### CCSession API（3 端点）
- GET /api/cc-sessions/{id} — 会话详情
- GET /api/cc-sessions/active — 活跃会话列表
- POST /api/cc-sessions/{id}/terminate — 终止会话

### GitHub API（3 端点）
- GET /api/github/oauth/callback — OAuth 回调
- GET /api/github/repositories — 仓库列表
- POST /api/github/merge — 分支合并

### WebSocket 主题
- /topic/cc-sessions/{id}/output — CC 输出流实时推送
- /topic/dev-tasks/{id}/status — 任务状态变更推送

## 前端工作概要

### 页面（5 个）
1. **首页** `/` — 对话列表 + 快速创建
2. **GitHub 授权页** `/auth` — OAuth 登录
3. **仓库选择页** `/repos` — 仓库列表
4. **对话页** `/conversations/:id` — 实时对话界面
5. **开发进度面板** `/tasks/:id` — 三阶段可视化

### 全局组件
- 导航栏（顶部，深色主题）
- WebSocket 连接管理器
- 全局消息通知

### API 调用层
- Axios 实例封装（统一错误处理、Result<T> 解包）
- STOMP WebSocket 客户端封装
- 15 个 API 调用函数

### 状态管理（Pinia Stores）
- conversationStore — 对话列表和当前对话
- devTaskStore — 开发任务状态
- githubStore — GitHub 授权状态和仓库列表
- websocketStore — WebSocket 连接状态

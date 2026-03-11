# 前端需求 — Thoughtworks Agents 开发平台

## 需求要点

构建 AI 开发平台的 Web 前端，提供对话式需求澄清界面和开发进度可视化面板。用户通过 GitHub OAuth 授权后，选择仓库，与 Claude Code 对话澄清需求，然后在可视化面板上跟踪开发进度（思考→工作→发布三阶段）。

## 页面列表

### 1. 首页 / 对话列表
- **路由**: `/`
- **功能**: 展示所有对话历史，按时间倒序排列；快速创建新对话入口；展示 GitHub 授权状态
- **消费 API**: `GET /api/conversations`，`GET /api/github/repositories`

### 2. GitHub 授权页
- **路由**: `/auth`
- **功能**: GitHub OAuth 登录入口；展示当前授权状态；OAuth 回调处理
- **消费 API**: `GET /api/github/oauth/callback`

### 3. 仓库选择页
- **路由**: `/repos`
- **功能**: 展示用户 GitHub 仓库列表（名称、描述、是否私有）；点击仓库后创建对话并跳转
- **消费 API**: `GET /api/github/repositories`，`POST /api/conversations`

### 4. 对话页
- **路由**: `/conversations/:id`
- **功能**:
  - 左侧：对话历史列表（可按仓库筛选）
  - 右侧：实时对话界面（消息列表 + 输入框）
  - 支持创建新对话、发送消息
  - CC 输出通过 WebSocket 实时展示
  - 需求确认后可创建开发任务
- **消费 API**:
  - `GET /api/conversations` — 对话列表
  - `GET /api/conversations/:id` — 对话详情
  - `POST /api/conversations/:id/messages` — 发送消息
  - `POST /api/dev-tasks` — 创建开发任务
  - `PUT /api/conversations/:id/archive` — 归档对话
  - WebSocket `/topic/cc-sessions/:sessionId/output` — CC 输出流

### 5. 开发进度面板
- **路由**: `/tasks/:id`
- **功能**:
  - 三阶段进度条：思考 → 工作 → 发布
  - 思考阶段：展示设计方案产出，可查看文件内容
  - 工作阶段：展示 CC 编码日志（实时流式输出）
  - 发布阶段：手动点击"发布"按钮，确认后自动合并分支
  - 任务状态通过 WebSocket 实时更新
- **消费 API**:
  - `GET /api/dev-tasks/:id` — 任务详情
  - `POST /api/dev-tasks/:id/start` — 启动开发
  - `POST /api/dev-tasks/:id/advance` — 推进到工作阶段
  - `POST /api/dev-tasks/:id/publish` — 执行发布
  - `GET /api/cc-sessions/:sessionId` — CC 会话详情
  - WebSocket `/topic/dev-tasks/:taskId/status` — 任务状态变更
  - WebSocket `/topic/cc-sessions/:sessionId/output` — CC 输出流

## 技术栈

- **框架**: Vue 3 + Vite + TypeScript
- **UI 组件库**: Element Plus
- **路由**: Vue Router
- **状态管理**: Pinia
- **HTTP 客户端**: Axios
- **WebSocket**: STOMP.js + SockJS
- **代码高亮**: highlight.js（用于展示设计方案和代码日志）

## UI 风格

tech-futuristic — 科技未来

- 深色主题为基调（深色背景 #0a0a1a ~ #1a1a2e）
- 电光紫 (#7c3aed) 为主色，赛博蓝 (#06b6d4) 为辅色
- 微发光效果（按钮、图标、进度条带 glow/shadow 效果）
- 玻璃态面板（backdrop-filter: blur + 半透明背景）
- 等宽字体用于代码/日志展示（JetBrains Mono / Fira Code）
- Element Plus 暗黑主题 + 自定义 CSS 变量覆盖

## API 映射

| 页面 | API 端点 | 方法 | 用途 |
|------|---------|------|------|
| 首页 | /api/conversations | GET | 加载对话列表 |
| 授权页 | /api/github/oauth/callback | GET | OAuth 回调 |
| 仓库选择 | /api/github/repositories | GET | 加载仓库列表 |
| 仓库选择 | /api/conversations | POST | 创建对话 |
| 对话页 | /api/conversations/:id | GET | 加载对话详情 |
| 对话页 | /api/conversations/:id/messages | POST | 发送消息 |
| 对话页 | /api/conversations/:id/archive | PUT | 归档对话 |
| 对话页 | /api/dev-tasks | POST | 创建开发任务 |
| 进度面板 | /api/dev-tasks/:id | GET | 加载任务详情 |
| 进度面板 | /api/dev-tasks/:id/start | POST | 启动开发 |
| 进度面板 | /api/dev-tasks/:id/advance | POST | 推进阶段 |
| 进度面板 | /api/dev-tasks/:id/publish | POST | 执行发布 |
| 进度面板 | /api/cc-sessions/:id | GET | CC 会话详情 |
| 全局 | WebSocket /topic/cc-sessions/:id/output | SUB | CC 实时输出 |
| 全局 | WebSocket /topic/dev-tasks/:id/status | SUB | 任务状态变更 |

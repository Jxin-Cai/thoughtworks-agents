---
layer: frontend
order: 1
status: done
depends_on: []
description: Thoughtworks Agents 开发平台前端设计，包含 5 个页面、4 个 Pinia Store、15 个 API 调用函数、2 个 WebSocket 订阅和完整的 tech-futuristic 暗色主题 UI
---

# 前端设计

## 结论

前端消费后端 OHS 层的 17 个 REST API 端点和 2 个 WebSocket 推送主题，构建 5 个页面（首页、GitHub 授权页、仓库选择页、对话页、开发进度面板），4 个 Pinia Store（conversation、devTask、github、websocket），通过 Axios 封装统一 Result&lt;T&gt; 解包的 API 调用层，以及基于 STOMP.js + SockJS 的 WebSocket 实时通信层。采用 tech-futuristic 暗色主题风格，使用 Element Plus 暗黑模式 + 自定义 CSS 变量覆盖。

---

## 依赖契约

> 以下接口和对象定义来自 OHS 层，前端作为消费方使用。
> 来源为当前 idea 的 OHS 层设计文档导出契约。

### API 端点（来自 ohs.md 导出契约）

| HTTP 方法 | URL | 用途 | Request DTO | Response DTO | 本层用途 |
|-----------|-----|------|-------------|--------------|---------|
| POST | /api/conversations | 创建对话 | CreateConversationRequest | ConversationDTO | RepoSelectPage 选择仓库后创建对话 |
| POST | /api/conversations/{conversationId}/messages | 发送消息 | SendMessageRequest | ConversationDTO | ConversationPage 发送用户消息 |
| GET | /api/conversations | 获取对话列表 | query: repositoryFullName | List&lt;ConversationDTO&gt; | HomePage 和 ConversationPage 加载对话列表 |
| GET | /api/conversations/{conversationId} | 获取对话详情 | 无 | ConversationDTO | ConversationPage 加载当前对话消息 |
| PUT | /api/conversations/{conversationId}/archive | 归档对话 | 无 | void | ConversationPage 归档已完成对话 |
| POST | /api/dev-tasks | 创建开发任务 | CreateDevTaskRequest | DevTaskDTO | ConversationPage 需求确认后创建开发任务 |
| POST | /api/dev-tasks/{taskId}/start | 启动开发 | StartDevelopmentRequest | DevTaskDTO | DevTaskPage 启动思考阶段 |
| POST | /api/dev-tasks/{taskId}/advance | 推进到工作阶段 | AdvanceToWorkingRequest | DevTaskDTO | DevTaskPage 确认设计方案后推进 |
| POST | /api/dev-tasks/{taskId}/publish | 执行发布 | ExecutePublishRequest | DevTaskDTO | DevTaskPage 执行分支合并发布 |
| GET | /api/dev-tasks/{taskId} | 查询任务详情 | 无 | DevTaskDTO | DevTaskPage 加载任务和阶段信息 |
| GET | /api/dev-tasks | 查询任务列表 | query: conversationId | List&lt;DevTaskDTO&gt; | ConversationPage 查看关联任务 |
| GET | /api/cc-sessions/{sessionId} | 查询 CC 会话详情 | 无 | CCSessionDTO | DevTaskPage 查看阶段关联的 CC 会话状态 |
| GET | /api/cc-sessions/active | 查询活跃 CC 会话 | 无 | List&lt;CCSessionDTO&gt; | 全局状态栏显示活跃会话数 |
| POST | /api/cc-sessions/{sessionId}/terminate | 终止 CC 会话 | 无 | void | DevTaskPage 手动终止异常会话 |
| GET | /api/github/oauth/callback | OAuth 回调 | query: code | void | AuthPage 处理 OAuth 授权回调 |
| GET | /api/github/repositories | 获取仓库列表 | 无 | List&lt;RepositoryDTO&gt; | RepoSelectPage 加载用户仓库 |
| POST | /api/github/merge | 分支合并 | MergeBranchRequest | MergeResultDTO | DevTaskPage 发布阶段合并分支 |

### WebSocket 端点（来自 ohs.md 导出契约）

| 端点 | 订阅主题 | 推送 DTO | 本层用途 |
|------|---------|---------|---------|
| /ws | /topic/cc-sessions/{sessionId}/output | CCOutputMessage | ConversationPage 和 DevTaskPage 实时展示 CC 输出流 |
| /ws | /topic/dev-tasks/{taskId}/status | DevTaskStatusMessage | DevTaskPage 实时更新任务阶段状态 |

### Request DTO 定义（来自 ohs.md 导出契约）

| DTO 类名 | 字段 | 类型 | 校验规则 | 说明 | 本层用途 |
|----------|------|------|---------|------|---------|
| CreateConversationRequest | title | string | required, maxLength(200) | 对话标题 | RepoSelectPage 创建对话时构建 |
| CreateConversationRequest | repositoryFullName | string | 可选 | 关联仓库全名 | RepoSelectPage 选中仓库后填入 |
| SendMessageRequest | content | string | required, maxLength(10000) | 消息内容 | ConversationPage 输入框提交 |
| SendMessageRequest | workingDirectory | string | required | CC 会话工作目录 | 前端从配置或仓库信息推导 |
| SendMessageRequest | environmentVariables | Record&lt;string, string&gt; | required | CC 会话环境变量 | 前端从 GitHub token 等推导 |
| CreateDevTaskRequest | conversationId | string | required | 来源对话 ID | ConversationPage 创建任务时自动填入 |
| CreateDevTaskRequest | repositoryFullName | string | required | 仓库全名 | 从当前对话关联仓库获取 |
| CreateDevTaskRequest | branchName | string | required, maxLength(200) | 功能分支名 | 用户在创建任务对话框中输入 |
| CreateDevTaskRequest | requirement | string | required, maxLength(10000) | 需求描述 | 从对话消息中提取或用户编辑 |
| StartDevelopmentRequest | workingDirectory | string | required | 工作目录 | 前端从仓库信息推导 |
| StartDevelopmentRequest | environmentVariables | Record&lt;string, string&gt; | required | 环境变量 | 前端从配置推导 |
| AdvanceToWorkingRequest | designOutput | string | required | 设计产出 | 从思考阶段 CC 输出中获取 |
| AdvanceToWorkingRequest | workingDirectory | string | required | 工作目录 | 前端从仓库信息推导 |
| AdvanceToWorkingRequest | environmentVariables | Record&lt;string, string&gt; | required | 环境变量 | 前端从配置推导 |
| ExecutePublishRequest | baseBranch | string | required, maxLength(200) | 目标分支 | 默认 main，用户可修改 |
| MergeBranchRequest | repositoryFullName | string | required | 仓库全名 | 从任务信息获取 |
| MergeBranchRequest | headBranch | string | required | 来源分支 | 从任务 branchName 获取 |
| MergeBranchRequest | baseBranch | string | required | 目标分支 | 默认 main |

### Response DTO 定义（来自 ohs.md 导出契约）

| DTO 类名 | 字段 | 类型 | 说明 | 本层用途 |
|----------|------|------|------|---------|
| Result&lt;T&gt; | code | number | HTTP 状态码 | API 调用层统一解包判断 |
| Result&lt;T&gt; | message | string | 提示信息 | 错误时展示给用户 |
| Result&lt;T&gt; | data | T | 响应数据 | 解包后传递给 Store 和组件 |
| ConversationDTO | id | string | 对话 ID | 路由跳转和列表渲染 key |
| ConversationDTO | title | string | 对话标题 | 对话卡片和侧栏列表展示 |
| ConversationDTO | repositoryFullName | string | 关联仓库 | 对话卡片标签展示 |
| ConversationDTO | status | string | CREATED/ACTIVE/COMPLETED/ARCHIVED | 状态标签渲染和操作按钮控制 |
| ConversationDTO | messages | MessageDTO[] | 消息列表 | 对话页消息气泡渲染 |
| ConversationDTO | createdAt | string | 创建时间 | 列表排序和时间展示 |
| ConversationDTO | updatedAt | string | 更新时间 | 列表排序 |
| MessageDTO | id | string | 消息 ID | 消息列表渲染 key |
| MessageDTO | role | string | USER/ASSISTANT | 区分用户消息和 AI 回复的样式 |
| MessageDTO | content | string | 消息内容 | 消息气泡内容渲染，支持 Markdown |
| MessageDTO | createdAt | string | 创建时间 | 消息时间戳展示 |
| DevTaskDTO | id | string | 任务 ID | 路由跳转和列表渲染 key |
| DevTaskDTO | conversationId | string | 来源对话 ID | 关联对话导航 |
| DevTaskDTO | repositoryFullName | string | 仓库全名 | 任务信息展示 |
| DevTaskDTO | branchName | string | 功能分支名 | 任务信息展示 |
| DevTaskDTO | requirement | string | 需求描述 | 任务详情展示 |
| DevTaskDTO | status | string | CREATED/THINKING/.../PUBLISHED/FAILED | 进度条状态渲染和操作按钮控制 |
| DevTaskDTO | phases | TaskPhaseDTO[] | 阶段记录列表 | 三阶段面板内容渲染 |
| DevTaskDTO | createdAt | string | 创建时间 | 时间展示 |
| DevTaskDTO | updatedAt | string | 更新时间 | 时间展示 |
| TaskPhaseDTO | id | string | 阶段 ID | 阶段列表渲染 key |
| TaskPhaseDTO | phaseType | string | THINKING/WORKING/PUBLISHING | 阶段面板切换 |
| TaskPhaseDTO | ccSessionId | string | 关联 CC 会话 ID | 订阅 WebSocket 输出流 |
| TaskPhaseDTO | output | string | 阶段产出 | 思考阶段设计方案展示 |
| TaskPhaseDTO | startedAt | string | 开始时间 | 阶段耗时计算 |
| TaskPhaseDTO | finishedAt | string | 结束时间 | 阶段耗时计算和完成标记 |
| TaskPhaseDTO | failureReason | string | 失败原因 | 失败状态错误信息展示 |
| CCSessionDTO | id | string | 会话 ID | WebSocket 订阅标识 |
| CCSessionDTO | status | string | CREATED/RUNNING/COMPLETED/FAILED/TERMINATED | 会话状态指示器 |
| CCSessionDTO | command | string | 执行命令 | 调试信息展示 |
| CCSessionDTO | workingDirectory | string | 工作目录 | 调试信息展示 |
| CCSessionDTO | createdAt | string | 创建时间 | 时间展示 |
| CCSessionDTO | startedAt | string | 启动时间 | 运行时长计算 |
| CCSessionDTO | finishedAt | string | 结束时间 | 运行时长计算 |
| CCSessionDTO | exitCode | number | 退出码 | 成功/失败判断 |
| RepositoryDTO | fullName | string | 仓库全名 owner/repo | 仓库卡片标题 |
| RepositoryDTO | defaultBranch | string | 默认分支名 | 发布时默认 baseBranch |
| RepositoryDTO | cloneUrl | string | 克隆地址 | workingDirectory 推导 |
| RepositoryDTO | isPrivate | boolean | 是否私有 | 仓库卡片私有标签 |
| MergeResultDTO | repositoryFullName | string | 仓库全名 | 发布结果展示 |
| MergeResultDTO | headBranch | string | 来源分支 | 发布结果展示 |
| MergeResultDTO | baseBranch | string | 目标分支 | 发布结果展示 |
| MergeResultDTO | success | boolean | 是否成功 | 发布成功/失败提示 |

### WebSocket 消息 DTO 定义（来自 ohs.md 导出契约）

| DTO 类名 | 字段 | 类型 | 说明 | 本层用途 |
|----------|------|------|------|---------|
| CCOutputMessage | sessionId | string | CC 会话 ID | 匹配订阅主题 |
| CCOutputMessage | content | string | 输出内容片段 | 追加到输出日志面板 |
| CCOutputMessage | timestamp | string | 输出时间 | 日志时间戳 |
| CCOutputMessage | type | string | STDOUT/STDERR/SYSTEM | 区分输出类型样式 |
| DevTaskStatusMessage | taskId | string | 任务 ID | 匹配当前任务 |
| DevTaskStatusMessage | previousStatus | string | 变更前状态 | 状态转换动画 |
| DevTaskStatusMessage | currentStatus | string | 变更后状态 | 更新进度条和阶段面板 |
| DevTaskStatusMessage | timestamp | string | 状态变更时间 | 时间线展示 |

---

## UI 风格

**风格**：tech-futuristic — 科技未来

**设计 Token 概要**：

| Token | 值 |
|-------|-----|
| 主色 | #7C3AED（电光紫） |
| 辅色 | #06B6D4（赛博蓝） |
| 背景色 | #0F172A（深蓝黑） |
| 卡片背景 | rgba(30, 41, 59, 0.8)（半透明深蓝灰，玻璃态） |
| 正文色 | #E2E8F0（浅灰白） |
| 辅助文字色 | #94A3B8（中灰蓝） |
| 成功色 | #10B981（翡翠绿） |
| 警告色 | #F59E0B（琥珀黄） |
| 错误色 | #EF4444（霓虹红） |
| 圆角 | sm: 6px / md: 10px / lg: 16px |
| 主色发光 | 0 0 12px rgba(124,58,237,0.3) |
| 辅色发光 | 0 0 12px rgba(6,182,212,0.3) |
| 玻璃态背景 | rgba(30, 41, 59, 0.6) + blur(12px) |
| 玻璃态边框 | 1px solid rgba(148,163,184,0.15) |
| 标题字体 | JetBrains Mono, Inter, Noto Sans SC |
| 正文字体 | Inter, Noto Sans SC, sans-serif |
| 代码字体 | JetBrains Mono, Fira Code, monospace |

**组件风格要点**：
- 按钮：主按钮渐变填充 `linear-gradient(135deg, #7C3AED, #06B6D4)`，高度 42px，hover 时增加主色发光阴影；次要按钮透明底 + 紫色半透明边框
- 卡片：玻璃态背景 + backdrop-filter: blur(12px)，内边距 24px，圆角 16px，hover 时边框亮度提升并增加辅色微发光
- 表格：表头背景 rgba(30, 41, 59, 0.9)，hover 行背景 rgba(124,58,237,0.08)，选中行左侧 2px 主色发光条
- 导航：顶部导航栏玻璃态背景，logo 带微弱发光效果，当前路由项带主色底部指示条
- 输入框：高度 42px，深色背景 rgba(15,23,42,0.6)，focus 时边框变主色并外发光
- 进度条：渐变填充 #7C3AED → #06B6D4，带微发光效果，已完成阶段发光更强烈
- 消息气泡：用户消息靠右 + 主色渐变背景，AI 消息靠左 + 玻璃态背景

---

## 页面与路由

### HomePage 首页

**路由**：`/`
**用途**：展示所有对话历史，按时间倒序排列；提供快速创建新对话入口；展示 GitHub 授权状态
**组件组合**：AppNavbar + ConversationList + CreateConversationDialog
**关联 API**：`GET /api/conversations`，`GET /api/github/repositories`

**页面结构**：
- 顶部：AppNavbar 全局导航栏
- 主体区域：居中卡片布局
  - 标题区：「Thoughtworks Agents 开发平台」+ GitHub 授权状态指示
  - 操作区：「新建对话」按钮 → 未授权时跳转 /auth，已授权时跳转 /repos
  - 列表区：对话卡片列表（标题、关联仓库、状态标签、更新时间）
  - 空状态：渐变描边图标 + 引导文案 + 创建按钮

### AuthPage GitHub 授权页

**路由**：`/auth`
**用途**：GitHub OAuth 登录入口；处理 OAuth 回调；展示授权状态
**组件组合**：AppNavbar + GitHubAuthPanel
**关联 API**：`GET /api/github/oauth/callback`

**页面结构**：
- 顶部：AppNavbar
- 主体区域：居中授权面板（玻璃态卡片）
  - GitHub 图标 + 标题
  - 未授权状态：「授权 GitHub」按钮，点击跳转 GitHub OAuth 页面
  - 回调处理：URL 包含 `?code=xxx` 时自动调用 callback API，展示 loading
  - 授权成功：展示用户名 + 「选择仓库」按钮跳转 /repos

### RepoSelectPage 仓库选择页

**路由**：`/repos`
**用途**：展示用户 GitHub 仓库列表；点击仓库后创建对话并跳转到对话页
**组件组合**：AppNavbar + RepositoryGrid
**关联 API**：`GET /api/github/repositories`，`POST /api/conversations`

**页面结构**：
- 顶部：AppNavbar
- 主体区域：
  - 标题区：「选择仓库」+ 搜索筛选框
  - 仓库网格：卡片式布局（仓库全名、默认分支、私有标签、克隆地址）
  - 点击仓库卡片 → 创建对话（title 为仓库名，repositoryFullName 为仓库全名）→ 跳转 /conversations/:id
  - 空状态 / 加载状态

### ConversationPage 对话页

**路由**：`/conversations/:id`
**用途**：左右分栏的实时对话界面；左侧对话历史列表，右侧消息流 + 输入框
**组件组合**：AppNavbar + ConversationSidebar + MessageList + MessageInput + CreateTaskDialog
**关联 API**：`GET /api/conversations`，`GET /api/conversations/:id`，`POST /api/conversations/:id/messages`，`POST /api/dev-tasks`，`PUT /api/conversations/:id/archive`，WebSocket `/topic/cc-sessions/:sessionId/output`

**页面结构**：
- 顶部：AppNavbar
- 左侧栏（280px，玻璃态）：
  - 搜索框 + 「新对话」按钮
  - 对话列表：按时间倒序，高亮当前对话，显示状态标签
  - 可按仓库筛选
- 右侧主区域：
  - 顶部信息栏：对话标题、关联仓库、状态、归档按钮、「创建开发任务」按钮
  - 消息列表区：用户消息（右侧紫色气泡）和 AI 回复（左侧玻璃态气泡），CC 实时输出通过 WebSocket 追加
  - 底部输入区：多行文本输入框 + 发送按钮

### DevTaskPage 开发进度面板

**路由**：`/tasks/:id`
**用途**：三阶段（思考→工作→发布）可视化进度面板
**组件组合**：AppNavbar + TaskProgressBar + ThinkingPanel + WorkingPanel + PublishingPanel + CCOutputLog
**关联 API**：`GET /api/dev-tasks/:id`，`POST /api/dev-tasks/:id/start`，`POST /api/dev-tasks/:id/advance`，`POST /api/dev-tasks/:id/publish`，`GET /api/cc-sessions/:sessionId`，WebSocket `/topic/dev-tasks/:taskId/status`，WebSocket `/topic/cc-sessions/:sessionId/output`

**页面结构**：
- 顶部：AppNavbar
- 任务信息栏：任务标题（仓库名 + 分支名）、需求描述、当前状态
- 三阶段进度条：水平排列，思考 → 工作 → 发布，当前阶段高亮发光，已完成阶段打勾
- 阶段面板区（根据当前状态动态切换）：
  - **思考阶段面板**：
    - CREATED 状态：「启动开发」按钮
    - THINKING 状态：CC 实时输出日志（代码高亮，等宽字体）
    - 思考完成：展示设计方案产出（Markdown 渲染）+ 「确认方案，开始编码」按钮
  - **工作阶段面板**：
    - WORKING 状态：CC 编码实时日志（流式输出，自动滚动）
    - 工作完成：成功状态展示
  - **发布阶段面板**：
    - READY_TO_PUBLISH 状态：目标分支选择（默认 main）+ 「发布」确认按钮
    - PUBLISHING 状态：合并进度 loading
    - PUBLISHED 状态：发布成功展示（合并信息 + 跳转链接）
    - FAILED 状态：失败原因展示 + 重试按钮

---

## 组件设计

### 全局组件

#### AppNavbar 全局导航栏

**文件路径**：`src/components/layout/AppNavbar.vue`
**类型**：布局组件

**Props**：无

**状态**：
| 状态 | 类型 | 初始值 | 说明 |
|------|------|--------|------|
| currentRoute | string | - | 当前路由路径，用于高亮导航项 |

**结构**：
- 左侧：Logo（带发光效果）+ 应用名称
- 中间：导航链接（首页 / 仓库 / 对话）
- 右侧：GitHub 授权状态指示（已授权显示用户名，未授权显示「登录」按钮）

---

### 对话相关组件

#### ConversationList 对话列表

**文件路径**：`src/components/conversation/ConversationList.vue`
**类型**：展示组件

**Props**：
| 属性 | 类型 | 必填 | 说明 |
|------|------|------|------|
| conversations | ConversationDTO[] | 是 | 对话列表数据 |
| currentId | string | 否 | 当前选中对话 ID |
| loading | boolean | 否 | 加载状态 |

**事件**：
- `@select(id: string)` — 选中对话
- `@create` — 创建新对话

#### ConversationSidebar 对话侧栏

**文件路径**：`src/components/conversation/ConversationSidebar.vue`
**类型**：功能组件

**Props**：
| 属性 | 类型 | 必填 | 说明 |
|------|------|------|------|
| currentConversationId | string | 否 | 当前对话 ID |

**状态**：
| 状态 | 类型 | 初始值 | 说明 |
|------|------|--------|------|
| searchKeyword | string | '' | 搜索关键词 |
| filterRepo | string | '' | 仓库筛选 |

**API 调用映射**：
- 组件挂载时 → `conversationStore.loadConversations()`
- 搜索/筛选变更时 → `conversationStore.loadConversations(filterRepo)`

#### MessageList 消息列表

**文件路径**：`src/components/conversation/MessageList.vue`
**类型**：展示组件

**Props**：
| 属性 | 类型 | 必填 | 说明 |
|------|------|------|------|
| messages | MessageDTO[] | 是 | 消息列表 |
| ccOutput | string | 否 | CC 实时输出内容（追加展示） |
| loading | boolean | 否 | 加载状态 |

**状态**：
| 状态 | 类型 | 初始值 | 说明 |
|------|------|--------|------|
| autoScroll | boolean | true | 是否自动滚动到底部 |

#### MessageInput 消息输入框

**文件路径**：`src/components/conversation/MessageInput.vue`
**类型**：功能组件

**Props**：
| 属性 | 类型 | 必填 | 说明 |
|------|------|------|------|
| disabled | boolean | 否 | 禁用输入（对话未激活时） |
| sending | boolean | 否 | 发送中状态 |

**状态**：
| 状态 | 类型 | 初始值 | 说明 |
|------|------|--------|------|
| content | string | '' | 输入内容 |

**事件**：
- `@send(content: string)` — 发送消息

#### CreateTaskDialog 创建开发任务对话框

**文件路径**：`src/components/conversation/CreateTaskDialog.vue`
**类型**：功能组件

**Props**：
| 属性 | 类型 | 必填 | 说明 |
|------|------|------|------|
| visible | boolean | 是 | 对话框可见性 |
| conversationId | string | 是 | 来源对话 ID |
| repositoryFullName | string | 是 | 仓库全名 |

**状态**：
| 状态 | 类型 | 初始值 | 说明 |
|------|------|--------|------|
| branchName | string | '' | 功能分支名输入 |
| requirement | string | '' | 需求描述输入 |
| submitting | boolean | false | 提交中状态 |

**API 调用映射**：
- 提交表单 → `devTaskApi.createDevTask()` → 成功后跳转 /tasks/:id

---

### GitHub 相关组件

#### GitHubAuthPanel 授权面板

**文件路径**：`src/components/github/GitHubAuthPanel.vue`
**类型**：功能组件

**Props**：无

**状态**：
| 状态 | 类型 | 初始值 | 说明 |
|------|------|--------|------|
| processing | boolean | false | OAuth 回调处理中 |
| error | string | '' | 授权错误信息 |

**API 调用映射**：
- URL 含 code 参数时 → `githubApi.handleOAuthCallback(code)`
- 成功后 → `githubStore.setAuthenticated(true)` → 跳转 /repos

#### RepositoryGrid 仓库网格

**文件路径**：`src/components/github/RepositoryGrid.vue`
**类型**：功能组件

**Props**：
| 属性 | 类型 | 必填 | 说明 |
|------|------|------|------|
| repositories | RepositoryDTO[] | 是 | 仓库列表 |
| loading | boolean | 否 | 加载状态 |

**状态**：
| 状态 | 类型 | 初始值 | 说明 |
|------|------|--------|------|
| searchKeyword | string | '' | 搜索关键词 |
| creating | boolean | false | 创建对话中 |

**API 调用映射**：
- 点击仓库卡片 → `conversationApi.createConversation()` → 跳转 /conversations/:id

---

### 开发任务相关组件

#### TaskProgressBar 任务进度条

**文件路径**：`src/components/devtask/TaskProgressBar.vue`
**类型**：展示组件

**Props**：
| 属性 | 类型 | 必填 | 说明 |
|------|------|------|------|
| status | string | 是 | 任务当前状态 |
| phases | TaskPhaseDTO[] | 是 | 阶段记录列表 |

**结构**：
- 三个阶段节点：思考 → 工作 → 发布
- 节点之间连线
- 当前阶段：发光动画
- 已完成阶段：对勾标记 + 辅色填充
- 未开始阶段：灰色轮廓

#### ThinkingPanel 思考阶段面板

**文件路径**：`src/components/devtask/ThinkingPanel.vue`
**类型**：功能组件

**Props**：
| 属性 | 类型 | 必填 | 说明 |
|------|------|------|------|
| task | DevTaskDTO | 是 | 任务详情 |
| ccOutput | string | 否 | CC 实时输出 |

**状态**：
| 状态 | 类型 | 初始值 | 说明 |
|------|------|--------|------|
| starting | boolean | false | 启动开发中 |
| advancing | boolean | false | 推进中 |

**API 调用映射**：
- 点击「启动开发」→ `devTaskApi.startDevelopment(taskId, ...)`
- 点击「确认方案，开始编码」→ `devTaskApi.advanceToWorking(taskId, ...)`

#### WorkingPanel 工作阶段面板

**文件路径**：`src/components/devtask/WorkingPanel.vue`
**类型**：展示组件

**Props**：
| 属性 | 类型 | 必填 | 说明 |
|------|------|------|------|
| task | DevTaskDTO | 是 | 任务详情 |
| ccOutput | string | 否 | CC 实时编码日志 |

**结构**：
- CC 编码日志区域（等宽字体，代码高亮，自动滚动）
- 运行时间显示
- 状态指示器（运行中 / 已完成）

#### PublishingPanel 发布阶段面板

**文件路径**：`src/components/devtask/PublishingPanel.vue`
**类型**：功能组件

**Props**：
| 属性 | 类型 | 必填 | 说明 |
|------|------|------|------|
| task | DevTaskDTO | 是 | 任务详情 |

**状态**：
| 状态 | 类型 | 初始值 | 说明 |
|------|------|--------|------|
| baseBranch | string | 'main' | 目标分支 |
| publishing | boolean | false | 发布中 |
| confirmVisible | boolean | false | 确认对话框可见 |

**API 调用映射**：
- 点击「发布」→ 弹出确认对话框
- 确认后 → `devTaskApi.executePublish(taskId, baseBranch)`

#### CCOutputLog CC 输出日志

**文件路径**：`src/components/devtask/CCOutputLog.vue`
**类型**：展示组件

**Props**：
| 属性 | 类型 | 必填 | 说明 |
|------|------|------|------|
| output | string | 是 | 输出内容 |
| autoScroll | boolean | 否 | 自动滚动到底部，默认 true |
| maxHeight | string | 否 | 最大高度，默认 '500px' |

**结构**：
- 深色代码面板（等宽字体 JetBrains Mono）
- STDOUT 白色文字，STDERR 红色文字，SYSTEM 辅色文字
- 自动滚动开关
- 复制按钮

---

## API 调用层

### 基础封装 — client.ts

**文件路径**：`src/api/client.ts`
**用途**：创建 Axios 实例，统一配置 baseURL、超时、响应拦截器（Result&lt;T&gt; 解包和错误处理）

**核心逻辑**：
- baseURL: `/api`（通过 Vite proxy 转发到后端）
- timeout: 30000ms
- 响应拦截器：解析 `Result<T>` 格式，code !== 200 时抛出 ApiError（含 code 和 message）
- 请求拦截器：统一设置 Content-Type: application/json

### Conversation API — conversationApi.ts

**文件路径**：`src/api/conversation.ts`

#### createConversation

**端点**：`POST /api/conversations`
**参数**：
| 参数 | 类型 | 说明 |
|------|------|------|
| title | string | 对话标题 |
| repositoryFullName | string | 关联仓库全名，可选 |

**返回**：`Promise<ConversationDTO>`

#### sendMessage

**端点**：`POST /api/conversations/{conversationId}/messages`
**参数**：
| 参数 | 类型 | 说明 |
|------|------|------|
| conversationId | string | 对话 ID |
| content | string | 消息内容 |
| workingDirectory | string | CC 工作目录 |
| environmentVariables | Record&lt;string, string&gt; | 环境变量 |

**返回**：`Promise<ConversationDTO>`

#### getConversations

**端点**：`GET /api/conversations`
**参数**：
| 参数 | 类型 | 说明 |
|------|------|------|
| repositoryFullName | string | 仓库筛选，可选 |

**返回**：`Promise<ConversationDTO[]>`

#### getConversation

**端点**：`GET /api/conversations/{conversationId}`
**参数**：
| 参数 | 类型 | 说明 |
|------|------|------|
| conversationId | string | 对话 ID |

**返回**：`Promise<ConversationDTO>`

#### archiveConversation

**端点**：`PUT /api/conversations/{conversationId}/archive`
**参数**：
| 参数 | 类型 | 说明 |
|------|------|------|
| conversationId | string | 对话 ID |

**返回**：`Promise<void>`

### DevTask API — devTaskApi.ts

**文件路径**：`src/api/devTask.ts`

#### createDevTask

**端点**：`POST /api/dev-tasks`
**参数**：
| 参数 | 类型 | 说明 |
|------|------|------|
| conversationId | string | 来源对话 ID |
| repositoryFullName | string | 仓库全名 |
| branchName | string | 功能分支名 |
| requirement | string | 需求描述 |

**返回**：`Promise<DevTaskDTO>`

#### startDevelopment

**端点**：`POST /api/dev-tasks/{taskId}/start`
**参数**：
| 参数 | 类型 | 说明 |
|------|------|------|
| taskId | string | 任务 ID |
| workingDirectory | string | 工作目录 |
| environmentVariables | Record&lt;string, string&gt; | 环境变量 |

**返回**：`Promise<DevTaskDTO>`

#### advanceToWorking

**端点**：`POST /api/dev-tasks/{taskId}/advance`
**参数**：
| 参数 | 类型 | 说明 |
|------|------|------|
| taskId | string | 任务 ID |
| designOutput | string | 设计产出 |
| workingDirectory | string | 工作目录 |
| environmentVariables | Record&lt;string, string&gt; | 环境变量 |

**返回**：`Promise<DevTaskDTO>`

#### executePublish

**端点**：`POST /api/dev-tasks/{taskId}/publish`
**参数**：
| 参数 | 类型 | 说明 |
|------|------|------|
| taskId | string | 任务 ID |
| baseBranch | string | 目标分支 |

**返回**：`Promise<DevTaskDTO>`

#### getDevTask

**端点**：`GET /api/dev-tasks/{taskId}`
**参数**：
| 参数 | 类型 | 说明 |
|------|------|------|
| taskId | string | 任务 ID |

**返回**：`Promise<DevTaskDTO>`

#### getDevTasks

**端点**：`GET /api/dev-tasks`
**参数**：
| 参数 | 类型 | 说明 |
|------|------|------|
| conversationId | string | 对话 ID |

**返回**：`Promise<DevTaskDTO[]>`

### CCSession API — ccSessionApi.ts

**文件路径**：`src/api/ccSession.ts`

#### getCCSession

**端点**：`GET /api/cc-sessions/{sessionId}`
**参数**：
| 参数 | 类型 | 说明 |
|------|------|------|
| sessionId | string | 会话 ID |

**返回**：`Promise<CCSessionDTO>`

#### getActiveSessions

**端点**：`GET /api/cc-sessions/active`
**参数**：无
**返回**：`Promise<CCSessionDTO[]>`

#### terminateSession

**端点**：`POST /api/cc-sessions/{sessionId}/terminate`
**参数**：
| 参数 | 类型 | 说明 |
|------|------|------|
| sessionId | string | 会话 ID |

**返回**：`Promise<void>`

### GitHub API — githubApi.ts

**文件路径**：`src/api/github.ts`

#### handleOAuthCallback

**端点**：`GET /api/github/oauth/callback`
**参数**：
| 参数 | 类型 | 说明 |
|------|------|------|
| code | string | GitHub 授权码 |

**返回**：`Promise<void>`

#### getRepositories

**端点**：`GET /api/github/repositories`
**参数**：无
**返回**：`Promise<RepositoryDTO[]>`

#### mergeBranch

**端点**：`POST /api/github/merge`
**参数**：
| 参数 | 类型 | 说明 |
|------|------|------|
| repositoryFullName | string | 仓库全名 |
| headBranch | string | 来源分支 |
| baseBranch | string | 目标分支 |

**返回**：`Promise<MergeResultDTO>`

### WebSocket 客户端 — websocket.ts

**文件路径**：`src/api/websocket.ts`
**用途**：封装 STOMP over SockJS 连接管理，提供订阅 CC 输出流和任务状态变更的方法

**核心方法**：

#### connect

建立 STOMP 连接到 `/ws` 端点（SockJS 降级），自动重连机制。

#### subscribeCCOutput

**订阅主题**：`/topic/cc-sessions/{sessionId}/output`
**回调参数**：CCOutputMessage
**返回**：取消订阅函数

#### subscribeDevTaskStatus

**订阅主题**：`/topic/dev-tasks/{taskId}/status`
**回调参数**：DevTaskStatusMessage
**返回**：取消订阅函数

#### disconnect

断开 STOMP 连接，清理所有订阅。

---

## 状态管理（Pinia Stores）

### conversationStore

**文件路径**：`src/stores/conversation.ts`

| 状态 | 类型 | 初始值 | 说明 |
|------|------|--------|------|
| conversations | ConversationDTO[] | [] | 对话列表 |
| currentConversation | ConversationDTO \| null | null | 当前选中对话 |
| loading | boolean | false | 列表加载中 |
| sending | boolean | false | 消息发送中 |

**Actions**：
- `loadConversations(repositoryFullName?)` — 调用 `getConversations()`，更新 conversations
- `loadConversation(id)` — 调用 `getConversation(id)`，更新 currentConversation
- `sendMessage(conversationId, content, workingDirectory, envVars)` — 调用 `sendMessage()`，更新 currentConversation
- `archiveConversation(conversationId)` — 调用 `archiveConversation()`，从列表移除

### devTaskStore

**文件路径**：`src/stores/devTask.ts`

| 状态 | 类型 | 初始值 | 说明 |
|------|------|--------|------|
| currentTask | DevTaskDTO \| null | null | 当前任务 |
| tasks | DevTaskDTO[] | [] | 任务列表 |
| ccOutput | string | '' | CC 实时输出累积 |
| loading | boolean | false | 加载中 |

**Actions**：
- `loadTask(taskId)` — 调用 `getDevTask(taskId)`，更新 currentTask
- `loadTasks(conversationId)` — 调用 `getDevTasks(conversationId)`，更新 tasks
- `startDevelopment(taskId, workDir, envVars)` — 调用 `startDevelopment()`
- `advanceToWorking(taskId, designOutput, workDir, envVars)` — 调用 `advanceToWorking()`
- `executePublish(taskId, baseBranch)` — 调用 `executePublish()`
- `appendCCOutput(content)` — 追加 CC 输出内容
- `updateStatus(status)` — WebSocket 状态变更时更新 currentTask.status

### githubStore

**文件路径**：`src/stores/github.ts`

| 状态 | 类型 | 初始值 | 说明 |
|------|------|--------|------|
| authenticated | boolean | false | 是否已授权 |
| repositories | RepositoryDTO[] | [] | 仓库列表 |
| loading | boolean | false | 加载中 |

**Actions**：
- `handleCallback(code)` — 调用 `handleOAuthCallback(code)`，设置 authenticated = true
- `loadRepositories()` — 调用 `getRepositories()`，更新 repositories
- `checkAuthStatus()` — 尝试调用 `getRepositories()` 检测授权状态

### websocketStore

**文件路径**：`src/stores/websocket.ts`

| 状态 | 类型 | 初始值 | 说明 |
|------|------|--------|------|
| connected | boolean | false | WebSocket 连接状态 |
| subscriptions | Map&lt;string, () =&gt; void&gt; | new Map() | 活跃订阅映射 |

**Actions**：
- `connect()` — 建立 WebSocket 连接
- `subscribeCCOutput(sessionId, callback)` — 订阅 CC 输出流
- `subscribeDevTaskStatus(taskId, callback)` — 订阅任务状态变更
- `unsubscribe(subscriptionKey)` — 取消指定订阅
- `disconnect()` — 断开连接并清理所有订阅

---

## 实现清单

| 序号 | output_id | 层级 | 文件路径 | 类型 | 说明 |
|------|-----------|------|---------|------|------|
| 1 | Output_Frontend_ThoughtworksAgentsDevPlatform_01 | config | `vite.config.ts` | 新增 | Vite 配置，含 API proxy、路径别名 |
| 2 | Output_Frontend_ThoughtworksAgentsDevPlatform_02 | config | `tsconfig.json` | 新增 | TypeScript 配置 |
| 3 | Output_Frontend_ThoughtworksAgentsDevPlatform_03 | config | `package.json` | 新增 | 项目依赖配置 |
| 4 | Output_Frontend_ThoughtworksAgentsDevPlatform_04 | config | `index.html` | 新增 | HTML 入口，引入字体 |
| 5 | Output_Frontend_ThoughtworksAgentsDevPlatform_05 | app | `src/main.ts` | 新增 | Vue 应用入口，挂载 Element Plus、Pinia、Router |
| 6 | Output_Frontend_ThoughtworksAgentsDevPlatform_06 | app | `src/App.vue` | 新增 | 根组件，RouterView 容器 |
| 7 | Output_Frontend_ThoughtworksAgentsDevPlatform_07 | types | `src/types/api.ts` | 新增 | 所有 DTO 类型定义（Result、ConversationDTO、MessageDTO、DevTaskDTO、TaskPhaseDTO、CCSessionDTO、RepositoryDTO、MergeResultDTO、CCOutputMessage、DevTaskStatusMessage） |
| 8 | Output_Frontend_ThoughtworksAgentsDevPlatform_08 | types | `src/types/request.ts` | 新增 | 所有 Request DTO 类型定义（CreateConversationRequest、SendMessageRequest、CreateDevTaskRequest、StartDevelopmentRequest、AdvanceToWorkingRequest、ExecutePublishRequest、MergeBranchRequest） |
| 9 | Output_Frontend_ThoughtworksAgentsDevPlatform_09 | api | `src/api/client.ts` | 新增 | Axios 实例封装，统一 Result&lt;T&gt; 解包和错误处理 |
| 10 | Output_Frontend_ThoughtworksAgentsDevPlatform_10 | api | `src/api/conversation.ts` | 新增 | 对话 API（createConversation、sendMessage、getConversations、getConversation、archiveConversation） |
| 11 | Output_Frontend_ThoughtworksAgentsDevPlatform_11 | api | `src/api/devTask.ts` | 新增 | 开发任务 API（createDevTask、startDevelopment、advanceToWorking、executePublish、getDevTask、getDevTasks） |
| 12 | Output_Frontend_ThoughtworksAgentsDevPlatform_12 | api | `src/api/ccSession.ts` | 新增 | CC 会话 API（getCCSession、getActiveSessions、terminateSession） |
| 13 | Output_Frontend_ThoughtworksAgentsDevPlatform_13 | api | `src/api/github.ts` | 新增 | GitHub API（handleOAuthCallback、getRepositories、mergeBranch） |
| 14 | Output_Frontend_ThoughtworksAgentsDevPlatform_14 | api | `src/api/websocket.ts` | 新增 | STOMP WebSocket 客户端（connect、subscribeCCOutput、subscribeDevTaskStatus、disconnect） |
| 15 | Output_Frontend_ThoughtworksAgentsDevPlatform_15 | store | `src/stores/conversation.ts` | 新增 | Pinia Store — 对话列表和当前对话状态管理 |
| 16 | Output_Frontend_ThoughtworksAgentsDevPlatform_16 | store | `src/stores/devTask.ts` | 新增 | Pinia Store — 开发任务状态和 CC 输出管理 |
| 17 | Output_Frontend_ThoughtworksAgentsDevPlatform_17 | store | `src/stores/github.ts` | 新增 | Pinia Store — GitHub 授权状态和仓库列表管理 |
| 18 | Output_Frontend_ThoughtworksAgentsDevPlatform_18 | store | `src/stores/websocket.ts` | 新增 | Pinia Store — WebSocket 连接状态和订阅管理 |
| 19 | Output_Frontend_ThoughtworksAgentsDevPlatform_19 | router | `src/router/index.ts` | 新增 | Vue Router 配置（5 个路由） |
| 20 | Output_Frontend_ThoughtworksAgentsDevPlatform_20 | style | `src/styles/variables.css` | 新增 | CSS 变量定义（设计 Token、Element Plus 暗黑主题覆盖） |
| 21 | Output_Frontend_ThoughtworksAgentsDevPlatform_21 | style | `src/styles/global.css` | 新增 | 全局样式（字体引入、body 背景、滚动条、动画） |
| 22 | Output_Frontend_ThoughtworksAgentsDevPlatform_22 | component | `src/components/layout/AppNavbar.vue` | 新增 | 全局导航栏，深色玻璃态，Logo + 导航链接 + 授权状态 |
| 23 | Output_Frontend_ThoughtworksAgentsDevPlatform_23 | component | `src/components/conversation/ConversationList.vue` | 新增 | 对话卡片列表组件 |
| 24 | Output_Frontend_ThoughtworksAgentsDevPlatform_24 | component | `src/components/conversation/ConversationSidebar.vue` | 新增 | 对话页左侧栏，搜索 + 筛选 + 对话列表 |
| 25 | Output_Frontend_ThoughtworksAgentsDevPlatform_25 | component | `src/components/conversation/MessageList.vue` | 新增 | 消息气泡列表，区分 USER/ASSISTANT 角色样式 |
| 26 | Output_Frontend_ThoughtworksAgentsDevPlatform_26 | component | `src/components/conversation/MessageInput.vue` | 新增 | 消息输入框，多行文本 + 发送按钮 |
| 27 | Output_Frontend_ThoughtworksAgentsDevPlatform_27 | component | `src/components/conversation/CreateTaskDialog.vue` | 新增 | 创建开发任务对话框，分支名 + 需求描述表单 |
| 28 | Output_Frontend_ThoughtworksAgentsDevPlatform_28 | component | `src/components/github/GitHubAuthPanel.vue` | 新增 | GitHub OAuth 授权面板 |
| 29 | Output_Frontend_ThoughtworksAgentsDevPlatform_29 | component | `src/components/github/RepositoryGrid.vue` | 新增 | 仓库卡片网格，搜索 + 点击创建对话 |
| 30 | Output_Frontend_ThoughtworksAgentsDevPlatform_30 | component | `src/components/devtask/TaskProgressBar.vue` | 新增 | 三阶段进度条（思考→工作→发布），发光动画 |
| 31 | Output_Frontend_ThoughtworksAgentsDevPlatform_31 | component | `src/components/devtask/ThinkingPanel.vue` | 新增 | 思考阶段面板，启动开发 + CC 输出 + 确认方案 |
| 32 | Output_Frontend_ThoughtworksAgentsDevPlatform_32 | component | `src/components/devtask/WorkingPanel.vue` | 新增 | 工作阶段面板，CC 编码实时日志 |
| 33 | Output_Frontend_ThoughtworksAgentsDevPlatform_33 | component | `src/components/devtask/PublishingPanel.vue` | 新增 | 发布阶段面板，分支选择 + 发布确认 + 结果展示 |
| 34 | Output_Frontend_ThoughtworksAgentsDevPlatform_34 | component | `src/components/devtask/CCOutputLog.vue` | 新增 | CC 输出日志展示，等宽字体 + 自动滚动 + 类型着色 |
| 35 | Output_Frontend_ThoughtworksAgentsDevPlatform_35 | page | `src/views/HomePage.vue` | 新增 | 首页，对话列表 + 快速创建入口 |
| 36 | Output_Frontend_ThoughtworksAgentsDevPlatform_36 | page | `src/views/AuthPage.vue` | 新增 | GitHub 授权页，OAuth 登录 + 回调处理 |
| 37 | Output_Frontend_ThoughtworksAgentsDevPlatform_37 | page | `src/views/RepoSelectPage.vue` | 新增 | 仓库选择页，仓库网格 + 点击创建对话 |
| 38 | Output_Frontend_ThoughtworksAgentsDevPlatform_38 | page | `src/views/ConversationPage.vue` | 新增 | 对话页，左右分栏（侧栏 + 消息流 + 输入框） |
| 39 | Output_Frontend_ThoughtworksAgentsDevPlatform_39 | page | `src/views/DevTaskPage.vue` | 新增 | 开发进度面板，三阶段可视化 + WebSocket 实时更新 |
| 40 | Output_Frontend_ThoughtworksAgentsDevPlatform_40 | util | `src/utils/format.ts` | 新增 | 工具函数（时间格式化、状态中文映射、Markdown 渲染） |
| 41 | Output_Frontend_ThoughtworksAgentsDevPlatform_41 | env | `env.d.ts` | 新增 | Vite 环境变量类型声明 |
| 42 | Output_Frontend_ThoughtworksAgentsDevPlatform_42 | env | `.env.development` | 新增 | 开发环境变量（VITE_API_BASE_URL、VITE_WS_URL、VITE_GITHUB_CLIENT_ID） |

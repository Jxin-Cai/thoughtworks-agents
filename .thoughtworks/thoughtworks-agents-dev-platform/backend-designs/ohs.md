---
spec_id: Spec_OHS
layer: ohs
order: 1
status: done
depends_on: []
description: Thoughtworks Agents 开发平台 OHS 层设计，暴露 Conversation、DevTask、CCSession、GitHub 四组 REST API 及 WebSocket 实时推送端点
---

# OHS 层设计

## 结论

OHS 层暴露 4 个 REST Controller 和 1 个 WebSocket 端点：**ConversationController**（对话 CRUD 与消息发送，6 个端点）、**DevTaskController**（任务创建、启动开发、推进阶段、执行发布、状态查询，6 个端点）、**CCSessionController**（CC 会话查询与终止，3 个端点）、**GitHubController**（OAuth 回调、仓库列表、分支合并，3 个端点），以及 **WebSocketHandler**（CC 输出流实时推送、任务状态变更推送）。所有 REST 端点采用统一 `Result<T>` 包装响应，Request DTO 携带 JSR-303 校验注解，通过全局异常处理器将业务异常映射为标准错误响应。

---

## 依赖契约

> 以下接口和对象定义来自 Application 层和 Domain 层，OHS 层作为消费方使用。

### 来自 Application 层

#### ApplicationService 方法（来自 application.md 导出契约）

| 类名 | 方法签名 | 返回类型 | 用例说明 | 本层用途 |
|------|---------|---------|---------|---------|
| ConversationApplicationService | `createConversation(CreateConversationCommand)` | ConversationDTO | 创建对话 | ConversationController 接收请求后委托创建 |
| ConversationApplicationService | `sendMessage(SendMessageCommand)` | ConversationDTO | 发送消息，首次发送触发 CC 会话创建 | ConversationController 接收消息后委托发送 |
| ConversationApplicationService | `listConversations()` | List\<ConversationDTO\> | 获取全部对话列表 | ConversationController 查询列表 |
| ConversationApplicationService | `listConversationsByRepository(String)` | List\<ConversationDTO\> | 获取指定仓库对话列表 | ConversationController 按仓库筛选 |
| ConversationApplicationService | `getConversation(String)` | ConversationDTO | 获取对话详情（含消息） | ConversationController 查询详情 |
| ConversationApplicationService | `archiveConversation(ArchiveConversationCommand)` | void | 归档对话 | ConversationController 归档操作 |
| DevTaskApplicationService | `createDevTask(CreateDevTaskCommand)` | DevTaskDTO | 创建开发任务 | DevTaskController 接收请求后委托创建 |
| DevTaskApplicationService | `startDevelopment(StartDevelopmentCommand)` | DevTaskDTO | 启动开发，触发思考阶段 | DevTaskController 启动开发 |
| DevTaskApplicationService | `advanceToWorking(AdvanceToWorkingCommand)` | DevTaskDTO | 完成思考，推进到工作阶段 | DevTaskController 推进阶段 |
| DevTaskApplicationService | `executePublish(ExecutePublishCommand)` | DevTaskDTO | 执行发布，合并分支 | DevTaskController 执行发布 |
| DevTaskApplicationService | `getDevTask(String)` | DevTaskDTO | 查询开发任务详情 | DevTaskController 查询详情 |
| DevTaskApplicationService | `listDevTasksByConversation(String)` | List\<DevTaskDTO\> | 查询对话关联任务列表 | DevTaskController 查询列表 |
| CCSessionApplicationService | `getSession(String)` | CCSessionDTO | 查询指定 CC 会话详情 | CCSessionController 查询详情 |
| CCSessionApplicationService | `getActiveSessions()` | List\<CCSessionDTO\> | 查询所有活跃 CC 会话 | CCSessionController 查询活跃列表 |
| CCSessionApplicationService | `terminateCCSession(TerminateCCSessionCommand)` | void | 终止活跃 CC 会话 | CCSessionController 终止操作 |
| GitHubApplicationService | `handleOAuthCallback(HandleOAuthCallbackCommand)` | void | 处理 OAuth 回调，存储 token | GitHubController OAuth 回调处理 |
| GitHubApplicationService | `listRepositories()` | List\<RepositoryDTO\> | 获取用户仓库列表 | GitHubController 查询仓库 |
| GitHubApplicationService | `mergeBranch(String, String, String)` | MergeResultDTO | 执行分支合并 | GitHubController 合并操作 |

#### Command 定义（来自 application.md 导出契约）

| 类名 | 字段 | 类型 | 约束 | 本层用途 |
|------|------|------|------|---------|
| CreateConversationCommand | title | String | @NotBlank | 由 CreateConversationRequest 转换构建后传入 ApplicationService |
| CreateConversationCommand | repositoryFullName | String | （无） | 由 CreateConversationRequest 转换构建后传入 ApplicationService |
| SendMessageCommand | conversationId | String | @NotBlank | 由 URL 路径变量 + SendMessageRequest 转换构建 |
| SendMessageCommand | content | String | @NotBlank | 由 SendMessageRequest 转换构建 |
| SendMessageCommand | workingDirectory | String | @NotBlank | 由 SendMessageRequest 转换构建 |
| SendMessageCommand | environmentVariables | Map\<String, String\> | @NotNull | 由 SendMessageRequest 转换构建 |
| ArchiveConversationCommand | conversationId | String | @NotBlank | 由 URL 路径变量转换构建 |
| CreateDevTaskCommand | conversationId | String | @NotBlank | 由 CreateDevTaskRequest 转换构建 |
| CreateDevTaskCommand | repositoryFullName | String | @NotBlank | 由 CreateDevTaskRequest 转换构建 |
| CreateDevTaskCommand | branchName | String | @NotBlank | 由 CreateDevTaskRequest 转换构建 |
| CreateDevTaskCommand | requirement | String | @NotBlank | 由 CreateDevTaskRequest 转换构建 |
| StartDevelopmentCommand | taskId | String | @NotBlank | 由 URL 路径变量 + StartDevelopmentRequest 转换构建 |
| StartDevelopmentCommand | workingDirectory | String | @NotBlank | 由 StartDevelopmentRequest 转换构建 |
| StartDevelopmentCommand | environmentVariables | Map\<String, String\> | @NotNull | 由 StartDevelopmentRequest 转换构建 |
| AdvanceToWorkingCommand | taskId | String | @NotBlank | 由 URL 路径变量 + AdvanceToWorkingRequest 转换构建 |
| AdvanceToWorkingCommand | designOutput | String | @NotBlank | 由 AdvanceToWorkingRequest 转换构建 |
| AdvanceToWorkingCommand | workingDirectory | String | @NotBlank | 由 AdvanceToWorkingRequest 转换构建 |
| AdvanceToWorkingCommand | environmentVariables | Map\<String, String\> | @NotNull | 由 AdvanceToWorkingRequest 转换构建 |
| ExecutePublishCommand | taskId | String | @NotBlank | 由 URL 路径变量 + ExecutePublishRequest 转换构建 |
| ExecutePublishCommand | baseBranch | String | @NotBlank | 由 ExecutePublishRequest 转换构建 |
| TerminateCCSessionCommand | sessionId | String | @NotBlank | 由 URL 路径变量转换构建 |
| HandleOAuthCallbackCommand | code | String | @NotBlank | 由请求参数转换构建 |

#### 返回类型定义（来自 application.md 导出契约）

| 类名 | 字段 | 类型 | 说明 | 本层用途 |
|------|------|------|------|---------|
| ConversationDTO | id | String | 对话 ID | 直接嵌入 Response 返回 |
| ConversationDTO | title | String | 对话标题 | 直接嵌入 Response 返回 |
| ConversationDTO | repositoryFullName | String | 关联仓库，可为 null | 直接嵌入 Response 返回 |
| ConversationDTO | status | String | 对话状态枚举名称 | 直接嵌入 Response 返回 |
| ConversationDTO | messages | List\<MessageDTO\> | 消息列表 | 直接嵌入 Response 返回 |
| ConversationDTO | createdAt | LocalDateTime | 创建时间 | 直接嵌入 Response 返回 |
| ConversationDTO | updatedAt | LocalDateTime | 更新时间 | 直接嵌入 Response 返回 |
| MessageDTO | id | String | 消息 ID | 嵌套在 ConversationDTO 内返回 |
| MessageDTO | role | String | 角色（USER/ASSISTANT） | 嵌套在 ConversationDTO 内返回 |
| MessageDTO | content | String | 消息内容 | 嵌套在 ConversationDTO 内返回 |
| MessageDTO | createdAt | LocalDateTime | 创建时间 | 嵌套在 ConversationDTO 内返回 |
| DevTaskDTO | id | String | 任务 ID | 直接嵌入 Response 返回 |
| DevTaskDTO | conversationId | String | 来源对话 ID | 直接嵌入 Response 返回 |
| DevTaskDTO | repositoryFullName | String | 关联仓库全名 | 直接嵌入 Response 返回 |
| DevTaskDTO | branchName | String | 功能分支名 | 直接嵌入 Response 返回 |
| DevTaskDTO | requirement | String | 需求描述 | 直接嵌入 Response 返回 |
| DevTaskDTO | status | String | 任务状态枚举名称 | 直接嵌入 Response 返回 |
| DevTaskDTO | phases | List\<TaskPhaseDTO\> | 阶段记录列表 | 直接嵌入 Response 返回 |
| DevTaskDTO | createdAt | LocalDateTime | 创建时间 | 直接嵌入 Response 返回 |
| DevTaskDTO | updatedAt | LocalDateTime | 更新时间 | 直接嵌入 Response 返回 |
| TaskPhaseDTO | id | String | 阶段 ID | 嵌套在 DevTaskDTO 内返回 |
| TaskPhaseDTO | phaseType | String | 阶段类型 | 嵌套在 DevTaskDTO 内返回 |
| TaskPhaseDTO | ccSessionId | String | 关联 CC 会话 ID，可为 null | 嵌套在 DevTaskDTO 内返回 |
| TaskPhaseDTO | output | String | 阶段产出，可为 null | 嵌套在 DevTaskDTO 内返回 |
| TaskPhaseDTO | startedAt | LocalDateTime | 开始时间 | 嵌套在 DevTaskDTO 内返回 |
| TaskPhaseDTO | finishedAt | LocalDateTime | 结束时间，可为 null | 嵌套在 DevTaskDTO 内返回 |
| TaskPhaseDTO | failureReason | String | 失败原因，可为 null | 嵌套在 DevTaskDTO 内返回 |
| CCSessionDTO | id | String | 会话 ID | 直接嵌入 Response 返回 |
| CCSessionDTO | status | String | 会话状态枚举名称 | 直接嵌入 Response 返回 |
| CCSessionDTO | command | String | 执行命令 | 直接嵌入 Response 返回 |
| CCSessionDTO | workingDirectory | String | 工作目录 | 直接嵌入 Response 返回 |
| CCSessionDTO | createdAt | LocalDateTime | 创建时间 | 直接嵌入 Response 返回 |
| CCSessionDTO | startedAt | LocalDateTime | 启动时间，可为 null | 直接嵌入 Response 返回 |
| CCSessionDTO | finishedAt | LocalDateTime | 结束时间，可为 null | 直接嵌入 Response 返回 |
| CCSessionDTO | exitCode | Integer | 退出码，可为 null | 直接嵌入 Response 返回 |
| RepositoryDTO | fullName | String | 仓库全名 owner/repo | 直接嵌入 Response 返回 |
| RepositoryDTO | defaultBranch | String | 默认分支名 | 直接嵌入 Response 返回 |
| RepositoryDTO | cloneUrl | String | 克隆地址 | 直接嵌入 Response 返回 |
| RepositoryDTO | isPrivate | boolean | 是否私有 | 直接嵌入 Response 返回 |
| MergeResultDTO | repositoryFullName | String | 仓库全名 | 直接嵌入 Response 返回 |
| MergeResultDTO | headBranch | String | 来源分支 | 直接嵌入 Response 返回 |
| MergeResultDTO | baseBranch | String | 目标分支 | 直接嵌入 Response 返回 |
| MergeResultDTO | success | boolean | 是否合并成功 | 直接嵌入 Response 返回 |

---

## API 端点

### POST /api/conversations

**用途**：创建一个新对话，关联可选的 GitHub 仓库

**Request DTO** -- `CreateConversationRequest`：

| 字段 | 类型 | 校验注解 | 说明 |
|------|------|---------|------|
| title | String | @NotBlank @Size(max=200) | 对话标题 |
| repositoryFullName | String | （无） | 关联的 GitHub 仓库全名，可为 null |

**Response DTO** -- 直接返回 `ConversationDTO`（来自 Application 层）

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "title": "实现用户注册功能",
    "repositoryFullName": "owner/repo",
    "status": "CREATED",
    "messages": [],
    "createdAt": "2026-03-10T10:00:00",
    "updatedAt": "2026-03-10T10:00:00"
  }
}
```

---

### POST /api/conversations/{conversationId}/messages

**用途**：向指定对话发送消息；若为首条消息则触发 CC 会话创建并激活对话

**Request DTO** -- `SendMessageRequest`：

| 字段 | 类型 | 校验注解 | 说明 |
|------|------|---------|------|
| content | String | @NotBlank @Size(max=10000) | 消息内容 |
| workingDirectory | String | @NotBlank | CC 会话工作目录（首次发送时使用） |
| environmentVariables | Map\<String, String\> | @NotNull | CC 会话环境变量（首次发送时使用） |

**Response DTO** -- 直接返回 `ConversationDTO`（含最新消息列表）

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "title": "实现用户注册功能",
    "repositoryFullName": "owner/repo",
    "status": "ACTIVE",
    "messages": [
      {
        "id": "660e8400-e29b-41d4-a716-446655440001",
        "role": "USER",
        "content": "请帮我实现用户注册功能",
        "createdAt": "2026-03-10T10:01:00"
      }
    ],
    "createdAt": "2026-03-10T10:00:00",
    "updatedAt": "2026-03-10T10:01:00"
  }
}
```

---

### GET /api/conversations

**用途**：获取全部对话列表，支持按仓库全名筛选

**请求参数**：

| 参数 | 类型 | 位置 | 必填 | 说明 |
|------|------|------|------|------|
| repositoryFullName | String | query | 否 | 按仓库全名筛选，为空时返回全部 |

**Response DTO** -- 直接返回 `List<ConversationDTO>`

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "title": "实现用户注册功能",
      "repositoryFullName": "owner/repo",
      "status": "ACTIVE",
      "messages": [],
      "createdAt": "2026-03-10T10:00:00",
      "updatedAt": "2026-03-10T10:01:00"
    }
  ]
}
```

---

### GET /api/conversations/{conversationId}

**用途**：获取对话详情，包含完整消息列表

**Response DTO** -- 直接返回 `ConversationDTO`（含 messages）

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "title": "实现用户注册功能",
    "repositoryFullName": "owner/repo",
    "status": "ACTIVE",
    "messages": [
      {
        "id": "660e8400-e29b-41d4-a716-446655440001",
        "role": "USER",
        "content": "请帮我实现用户注册功能",
        "createdAt": "2026-03-10T10:01:00"
      },
      {
        "id": "660e8400-e29b-41d4-a716-446655440002",
        "role": "ASSISTANT",
        "content": "好的，我来帮你实现用户注册功能...",
        "createdAt": "2026-03-10T10:01:05"
      }
    ],
    "createdAt": "2026-03-10T10:00:00",
    "updatedAt": "2026-03-10T10:01:05"
  }
}
```

---

### PUT /api/conversations/{conversationId}/archive

**用途**：归档指定对话，仅 ACTIVE 或 COMPLETED 状态的对话可归档

**Response DTO** -- 无数据，仅返回成功状态

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

---

### POST /api/dev-tasks

**用途**：创建开发任务，关联来源对话、仓库、功能分支和需求描述

**Request DTO** -- `CreateDevTaskRequest`：

| 字段 | 类型 | 校验注解 | 说明 |
|------|------|---------|------|
| conversationId | String | @NotBlank | 来源对话 ID |
| repositoryFullName | String | @NotBlank | 关联的 GitHub 仓库全名 |
| branchName | String | @NotBlank @Size(max=200) | 功能分支名称 |
| requirement | String | @NotBlank @Size(max=10000) | 需求描述内容 |

**Response DTO** -- 直接返回 `DevTaskDTO`

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": "770e8400-e29b-41d4-a716-446655440000",
    "conversationId": "550e8400-e29b-41d4-a716-446655440000",
    "repositoryFullName": "owner/repo",
    "branchName": "feature/user-register",
    "requirement": "实现用户注册功能，包括邮箱验证",
    "status": "CREATED",
    "phases": [],
    "createdAt": "2026-03-10T10:05:00",
    "updatedAt": "2026-03-10T10:05:00"
  }
}
```

---

### POST /api/dev-tasks/{taskId}/start

**用途**：启动开发，创建思考阶段 CC 会话并标记任务进入 THINKING 状态

**Request DTO** -- `StartDevelopmentRequest`：

| 字段 | 类型 | 校验注解 | 说明 |
|------|------|---------|------|
| workingDirectory | String | @NotBlank | 思考阶段 CC 会话工作目录 |
| environmentVariables | Map\<String, String\> | @NotNull | 思考阶段 CC 会话环境变量 |

**Response DTO** -- 直接返回 `DevTaskDTO`

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": "770e8400-e29b-41d4-a716-446655440000",
    "conversationId": "550e8400-e29b-41d4-a716-446655440000",
    "repositoryFullName": "owner/repo",
    "branchName": "feature/user-register",
    "requirement": "实现用户注册功能，包括邮箱验证",
    "status": "THINKING",
    "phases": [
      {
        "id": "880e8400-e29b-41d4-a716-446655440000",
        "phaseType": "THINKING",
        "ccSessionId": "990e8400-e29b-41d4-a716-446655440000",
        "output": null,
        "startedAt": "2026-03-10T10:06:00",
        "finishedAt": null,
        "failureReason": null
      }
    ],
    "createdAt": "2026-03-10T10:05:00",
    "updatedAt": "2026-03-10T10:06:00"
  }
}
```

---

### POST /api/dev-tasks/{taskId}/advance

**用途**：完成思考阶段并推进到工作阶段，提交设计产出并创建工作阶段 CC 会话

**Request DTO** -- `AdvanceToWorkingRequest`：

| 字段 | 类型 | 校验注解 | 说明 |
|------|------|---------|------|
| designOutput | String | @NotBlank | 思考阶段产出的设计文档内容 |
| workingDirectory | String | @NotBlank | 工作阶段 CC 会话工作目录 |
| environmentVariables | Map\<String, String\> | @NotNull | 工作阶段 CC 会话环境变量 |

**Response DTO** -- 直接返回 `DevTaskDTO`

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": "770e8400-e29b-41d4-a716-446655440000",
    "status": "WORKING",
    "phases": [
      {
        "id": "880e8400-e29b-41d4-a716-446655440000",
        "phaseType": "THINKING",
        "ccSessionId": "990e8400-e29b-41d4-a716-446655440000",
        "output": "设计方案内容...",
        "startedAt": "2026-03-10T10:06:00",
        "finishedAt": "2026-03-10T10:10:00",
        "failureReason": null
      },
      {
        "id": "880e8400-e29b-41d4-a716-446655440001",
        "phaseType": "WORKING",
        "ccSessionId": "990e8400-e29b-41d4-a716-446655440001",
        "output": null,
        "startedAt": "2026-03-10T10:10:00",
        "finishedAt": null,
        "failureReason": null
      }
    ]
  }
}
```

---

### POST /api/dev-tasks/{taskId}/publish

**用途**：执行发布，合并功能分支到目标分支

**Request DTO** -- `ExecutePublishRequest`：

| 字段 | 类型 | 校验注解 | 说明 |
|------|------|---------|------|
| baseBranch | String | @NotBlank @Size(max=200) | 目标合并分支，通常为 main |

**Response DTO** -- 直接返回 `DevTaskDTO`

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": "770e8400-e29b-41d4-a716-446655440000",
    "status": "PUBLISHED",
    "phases": [
      {
        "phaseType": "THINKING",
        "finishedAt": "2026-03-10T10:10:00"
      },
      {
        "phaseType": "WORKING",
        "finishedAt": "2026-03-10T10:30:00"
      },
      {
        "phaseType": "PUBLISHING",
        "startedAt": "2026-03-10T10:35:00",
        "finishedAt": "2026-03-10T10:35:10"
      }
    ]
  }
}
```

---

### GET /api/dev-tasks/{taskId}

**用途**：查询开发任务详情，包含完整阶段信息

**Response DTO** -- 直接返回 `DevTaskDTO`（含 phases）

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": "770e8400-e29b-41d4-a716-446655440000",
    "conversationId": "550e8400-e29b-41d4-a716-446655440000",
    "repositoryFullName": "owner/repo",
    "branchName": "feature/user-register",
    "requirement": "实现用户注册功能",
    "status": "WORKING",
    "phases": [],
    "createdAt": "2026-03-10T10:05:00",
    "updatedAt": "2026-03-10T10:10:00"
  }
}
```

---

### GET /api/dev-tasks

**用途**：查询对话关联的开发任务列表

**请求参数**：

| 参数 | 类型 | 位置 | 必填 | 说明 |
|------|------|------|------|------|
| conversationId | String | query | 是 | 来源对话 ID |

**Response DTO** -- 直接返回 `List<DevTaskDTO>`

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": "770e8400-e29b-41d4-a716-446655440000",
      "conversationId": "550e8400-e29b-41d4-a716-446655440000",
      "repositoryFullName": "owner/repo",
      "branchName": "feature/user-register",
      "requirement": "实现用户注册功能",
      "status": "WORKING",
      "phases": [],
      "createdAt": "2026-03-10T10:05:00",
      "updatedAt": "2026-03-10T10:10:00"
    }
  ]
}
```

---

### GET /api/cc-sessions/{sessionId}

**用途**：查询指定 CC 会话的状态详情

**Response DTO** -- 直接返回 `CCSessionDTO`

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": "990e8400-e29b-41d4-a716-446655440000",
    "status": "RUNNING",
    "command": "claude -p '请帮我实现用户注册功能'",
    "workingDirectory": "/home/user/project",
    "createdAt": "2026-03-10T10:01:00",
    "startedAt": "2026-03-10T10:01:01",
    "finishedAt": null,
    "exitCode": null
  }
}
```

---

### GET /api/cc-sessions/active

**用途**：查询所有活跃的 CC 会话列表（状态为 CREATED 或 RUNNING）

**Response DTO** -- 直接返回 `List<CCSessionDTO>`

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": "990e8400-e29b-41d4-a716-446655440000",
      "status": "RUNNING",
      "command": "claude -p '...'",
      "workingDirectory": "/home/user/project",
      "createdAt": "2026-03-10T10:01:00",
      "startedAt": "2026-03-10T10:01:01",
      "finishedAt": null,
      "exitCode": null
    }
  ]
}
```

---

### POST /api/cc-sessions/{sessionId}/terminate

**用途**：手动终止一个活跃的 CC 会话

**Response DTO** -- 无数据，仅返回成功状态

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

---

### GET /api/github/oauth/callback

**用途**：处理 GitHub OAuth 授权回调，用授权码换取 access token 并持久化

**请求参数**：

| 参数 | 类型 | 位置 | 必填 | 说明 |
|------|------|------|------|------|
| code | String | query | 是 | GitHub 授权码 |

**Response DTO** -- 无数据，仅返回成功状态

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

---

### GET /api/github/repositories

**用途**：获取已认证 GitHub 用户的仓库列表

**Response DTO** -- 直接返回 `List<RepositoryDTO>`

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "fullName": "owner/repo",
      "defaultBranch": "main",
      "cloneUrl": "https://github.com/owner/repo.git",
      "isPrivate": false
    }
  ]
}
```

---

### POST /api/github/merge

**用途**：执行 GitHub 分支合并操作

**Request DTO** -- `MergeBranchRequest`：

| 字段 | 类型 | 校验注解 | 说明 |
|------|------|---------|------|
| repositoryFullName | String | @NotBlank | 仓库全名 owner/repo |
| headBranch | String | @NotBlank | 来源分支 |
| baseBranch | String | @NotBlank | 目标分支 |

**Response DTO** -- 直接返回 `MergeResultDTO`

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "repositoryFullName": "owner/repo",
    "headBranch": "feature/user-register",
    "baseBranch": "main",
    "success": true
  }
}
```

---

### WebSocket /ws/cc-sessions/{sessionId}/output

**用途**：实时推送指定 CC 会话的输出流，前端连接后持续接收 CC CLI 的标准输出

**协议**：STOMP over WebSocket

**订阅主题**：`/topic/cc-sessions/{sessionId}/output`

**推送消息格式** -- `CCOutputMessage`：

| 字段 | 类型 | 说明 |
|------|------|------|
| sessionId | String | CC 会话 ID |
| content | String | 输出内容片段 |
| timestamp | LocalDateTime | 输出时间 |
| type | String | 输出类型：STDOUT / STDERR / SYSTEM |

**推送消息示例**：
```json
{
  "sessionId": "990e8400-e29b-41d4-a716-446655440000",
  "content": "正在分析需求...",
  "timestamp": "2026-03-10T10:01:05",
  "type": "STDOUT"
}
```

---

### WebSocket /ws/dev-tasks/{taskId}/status

**用途**：实时推送指定开发任务的状态变更，前端连接后持续接收任务阶段推进通知

**协议**：STOMP over WebSocket

**订阅主题**：`/topic/dev-tasks/{taskId}/status`

**推送消息格式** -- `DevTaskStatusMessage`：

| 字段 | 类型 | 说明 |
|------|------|------|
| taskId | String | 任务 ID |
| previousStatus | String | 变更前状态 |
| currentStatus | String | 变更后状态 |
| timestamp | LocalDateTime | 状态变更时间 |

**推送消息示例**：
```json
{
  "taskId": "770e8400-e29b-41d4-a716-446655440000",
  "previousStatus": "THINKING",
  "currentStatus": "WORKING",
  "timestamp": "2026-03-10T10:10:00"
}
```

---

## 统一响应包装

### Result\<T\>

所有 REST API 使用统一响应格式包装：

| 字段 | 类型 | 说明 |
|------|------|------|
| code | int | HTTP 状态码 |
| message | String | 提示信息 |
| data | T | 响应数据，可为 null |

### 全局异常处理 -- GlobalExceptionHandler

使用 `@RestControllerAdvice` 统一处理异常并映射为标准错误响应：

| 异常类型 | HTTP 状态码 | message |
|----------|-----------|---------|
| MethodArgumentNotValidException | 400 | 校验失败具体字段信息 |
| BusinessException | 400 | 业务异常信息 |
| IllegalCCSessionStateException | 409 | CC 会话状态不允许此操作 |
| IllegalConversationStateException | 409 | 对话状态不允许此操作 |
| IllegalDevTaskStateException | 409 | 任务状态不允许此操作 |
| GitHubNotAuthenticatedException | 401 | 请先完成 GitHub OAuth 认证 |
| GitHubMergeConflictException | 409 | 分支合并冲突 |
| GitHubApiException | 502 | GitHub API 调用失败 |
| Exception | 500 | 服务器内部错误 |

**错误响应示例**：
```json
{
  "code": 409,
  "message": "对话状态不允许此操作: 当前状态 ARCHIVED 不允许发送消息",
  "data": null
}
```

---

## Controller

### ConversationController

**路径前缀**：`@RequestMapping("/api/conversations")`
**依赖**：`ConversationApplicationService`

---

#### Result\<ConversationDTO\> createConversation(@Validated @RequestBody CreateConversationRequest request)

**HTTP 映射**：`@PostMapping`

**DTO -> Command 转换**：
```java
CreateConversationCommand command = CreateConversationCommand.builder()
    .title(request.getTitle())
    .repositoryFullName(request.getRepositoryFullName())
    .build();
```

**调用**：`conversationApplicationService.createConversation(command)`

**返回**：`Result.success(conversationDTO)`

---

#### Result\<ConversationDTO\> sendMessage(@PathVariable String conversationId, @Validated @RequestBody SendMessageRequest request)

**HTTP 映射**：`@PostMapping("/{conversationId}/messages")`

**DTO -> Command 转换**：
```java
SendMessageCommand command = SendMessageCommand.builder()
    .conversationId(conversationId)
    .content(request.getContent())
    .workingDirectory(request.getWorkingDirectory())
    .environmentVariables(request.getEnvironmentVariables())
    .build();
```

**调用**：`conversationApplicationService.sendMessage(command)`

**返回**：`Result.success(conversationDTO)`

---

#### Result\<List\<ConversationDTO\>\> listConversations(@RequestParam(required = false) String repositoryFullName)

**HTTP 映射**：`@GetMapping`

**调用**：
```java
if (repositoryFullName != null && !repositoryFullName.isBlank()) {
    return Result.success(conversationApplicationService.listConversationsByRepository(repositoryFullName));
} else {
    return Result.success(conversationApplicationService.listConversations());
}
```

**返回**：`Result.success(conversationDTOList)`

---

#### Result\<ConversationDTO\> getConversation(@PathVariable String conversationId)

**HTTP 映射**：`@GetMapping("/{conversationId}")`

**调用**：`conversationApplicationService.getConversation(conversationId)`

**返回**：`Result.success(conversationDTO)`

---

#### Result\<Void\> archiveConversation(@PathVariable String conversationId)

**HTTP 映射**：`@PutMapping("/{conversationId}/archive")`

**DTO -> Command 转换**：
```java
ArchiveConversationCommand command = ArchiveConversationCommand.builder()
    .conversationId(conversationId)
    .build();
```

**调用**：`conversationApplicationService.archiveConversation(command)`

**返回**：`Result.success()`

---

### DevTaskController

**路径前缀**：`@RequestMapping("/api/dev-tasks")`
**依赖**：`DevTaskApplicationService`

---

#### Result\<DevTaskDTO\> createDevTask(@Validated @RequestBody CreateDevTaskRequest request)

**HTTP 映射**：`@PostMapping`

**DTO -> Command 转换**：
```java
CreateDevTaskCommand command = CreateDevTaskCommand.builder()
    .conversationId(request.getConversationId())
    .repositoryFullName(request.getRepositoryFullName())
    .branchName(request.getBranchName())
    .requirement(request.getRequirement())
    .build();
```

**调用**：`devTaskApplicationService.createDevTask(command)`

**返回**：`Result.success(devTaskDTO)`

---

#### Result\<DevTaskDTO\> startDevelopment(@PathVariable String taskId, @Validated @RequestBody StartDevelopmentRequest request)

**HTTP 映射**：`@PostMapping("/{taskId}/start")`

**DTO -> Command 转换**：
```java
StartDevelopmentCommand command = StartDevelopmentCommand.builder()
    .taskId(taskId)
    .workingDirectory(request.getWorkingDirectory())
    .environmentVariables(request.getEnvironmentVariables())
    .build();
```

**调用**：`devTaskApplicationService.startDevelopment(command)`

**返回**：`Result.success(devTaskDTO)`

---

#### Result\<DevTaskDTO\> advanceToWorking(@PathVariable String taskId, @Validated @RequestBody AdvanceToWorkingRequest request)

**HTTP 映射**：`@PostMapping("/{taskId}/advance")`

**DTO -> Command 转换**：
```java
AdvanceToWorkingCommand command = AdvanceToWorkingCommand.builder()
    .taskId(taskId)
    .designOutput(request.getDesignOutput())
    .workingDirectory(request.getWorkingDirectory())
    .environmentVariables(request.getEnvironmentVariables())
    .build();
```

**调用**：`devTaskApplicationService.advanceToWorking(command)`

**返回**：`Result.success(devTaskDTO)`

---

#### Result\<DevTaskDTO\> executePublish(@PathVariable String taskId, @Validated @RequestBody ExecutePublishRequest request)

**HTTP 映射**：`@PostMapping("/{taskId}/publish")`

**DTO -> Command 转换**：
```java
ExecutePublishCommand command = ExecutePublishCommand.builder()
    .taskId(taskId)
    .baseBranch(request.getBaseBranch())
    .build();
```

**调用**：`devTaskApplicationService.executePublish(command)`

**返回**：`Result.success(devTaskDTO)`

---

#### Result\<DevTaskDTO\> getDevTask(@PathVariable String taskId)

**HTTP 映射**：`@GetMapping("/{taskId}")`

**调用**：`devTaskApplicationService.getDevTask(taskId)`

**返回**：`Result.success(devTaskDTO)`

---

#### Result\<List\<DevTaskDTO\>\> listDevTasks(@RequestParam String conversationId)

**HTTP 映射**：`@GetMapping`

**调用**：`devTaskApplicationService.listDevTasksByConversation(conversationId)`

**返回**：`Result.success(devTaskDTOList)`

---

### CCSessionController

**路径前缀**：`@RequestMapping("/api/cc-sessions")`
**依赖**：`CCSessionApplicationService`

---

#### Result\<CCSessionDTO\> getSession(@PathVariable String sessionId)

**HTTP 映射**：`@GetMapping("/{sessionId}")`

**调用**：`ccSessionApplicationService.getSession(sessionId)`

**返回**：`Result.success(ccSessionDTO)`

---

#### Result\<List\<CCSessionDTO\>\> getActiveSessions()

**HTTP 映射**：`@GetMapping("/active")`

**调用**：`ccSessionApplicationService.getActiveSessions()`

**返回**：`Result.success(ccSessionDTOList)`

---

#### Result\<Void\> terminateSession(@PathVariable String sessionId)

**HTTP 映射**：`@PostMapping("/{sessionId}/terminate")`

**DTO -> Command 转换**：
```java
TerminateCCSessionCommand command = TerminateCCSessionCommand.builder()
    .sessionId(sessionId)
    .build();
```

**调用**：`ccSessionApplicationService.terminateCCSession(command)`

**返回**：`Result.success()`

---

### GitHubController

**路径前缀**：`@RequestMapping("/api/github")`
**依赖**：`GitHubApplicationService`

---

#### Result\<Void\> handleOAuthCallback(@RequestParam String code)

**HTTP 映射**：`@GetMapping("/oauth/callback")`

**DTO -> Command 转换**：
```java
HandleOAuthCallbackCommand command = HandleOAuthCallbackCommand.builder()
    .code(code)
    .build();
```

**调用**：`gitHubApplicationService.handleOAuthCallback(command)`

**返回**：`Result.success()`

---

#### Result\<List\<RepositoryDTO\>\> listRepositories()

**HTTP 映射**：`@GetMapping("/repositories")`

**调用**：`gitHubApplicationService.listRepositories()`

**返回**：`Result.success(repositoryDTOList)`

---

#### Result\<MergeResultDTO\> mergeBranch(@Validated @RequestBody MergeBranchRequest request)

**HTTP 映射**：`@PostMapping("/merge")`

**调用**：`gitHubApplicationService.mergeBranch(request.getRepositoryFullName(), request.getHeadBranch(), request.getBaseBranch())`

**返回**：`Result.success(mergeResultDTO)`

---

### WebSocketHandler

**WebSocket 配置**：`WebSocketConfig` 使用 `@EnableWebSocketMessageBroker`，配置 STOMP 端点 `/ws` 和消息代理前缀 `/topic`

**依赖**：`SimpMessagingTemplate`

---

#### void pushCCOutput(String sessionId, CCOutputMessage message)

**推送目的地**：`/topic/cc-sessions/{sessionId}/output`

**触发时机**：CC 会话进程产生标准输出/错误输出时，由 Infr 层进程管理器回调本方法

**调用**：`messagingTemplate.convertAndSend("/topic/cc-sessions/" + sessionId + "/output", message)`

---

#### void pushDevTaskStatus(String taskId, DevTaskStatusMessage message)

**推送目的地**：`/topic/dev-tasks/{taskId}/status`

**触发时机**：监听 DevTaskStatusChangedEvent 领域事件后转换并推送

**调用**：`messagingTemplate.convertAndSend("/topic/dev-tasks/" + taskId + "/status", message)`

---

## 导出契约

### API 端点

| HTTP 方法 | URL | 用途 | Request DTO | Response DTO |
|-----------|-----|------|-------------|--------------|
| POST | /api/conversations | 创建对话 | CreateConversationRequest | ConversationDTO |
| POST | /api/conversations/{conversationId}/messages | 发送消息 | SendMessageRequest | ConversationDTO |
| GET | /api/conversations | 获取对话列表 | （query: repositoryFullName） | List\<ConversationDTO\> |
| GET | /api/conversations/{conversationId} | 获取对话详情 | （无） | ConversationDTO |
| PUT | /api/conversations/{conversationId}/archive | 归档对话 | （无） | void |
| POST | /api/dev-tasks | 创建开发任务 | CreateDevTaskRequest | DevTaskDTO |
| POST | /api/dev-tasks/{taskId}/start | 启动开发 | StartDevelopmentRequest | DevTaskDTO |
| POST | /api/dev-tasks/{taskId}/advance | 推进到工作阶段 | AdvanceToWorkingRequest | DevTaskDTO |
| POST | /api/dev-tasks/{taskId}/publish | 执行发布 | ExecutePublishRequest | DevTaskDTO |
| GET | /api/dev-tasks/{taskId} | 查询任务详情 | （无） | DevTaskDTO |
| GET | /api/dev-tasks | 查询任务列表 | （query: conversationId） | List\<DevTaskDTO\> |
| GET | /api/cc-sessions/{sessionId} | 查询 CC 会话详情 | （无） | CCSessionDTO |
| GET | /api/cc-sessions/active | 查询活跃 CC 会话 | （无） | List\<CCSessionDTO\> |
| POST | /api/cc-sessions/{sessionId}/terminate | 终止 CC 会话 | （无） | void |
| GET | /api/github/oauth/callback | OAuth 回调 | （query: code） | void |
| GET | /api/github/repositories | 获取仓库列表 | （无） | List\<RepositoryDTO\> |
| POST | /api/github/merge | 分支合并 | MergeBranchRequest | MergeResultDTO |

### WebSocket 端点

| 端点 | 订阅主题 | 推送 DTO | 用途 |
|------|---------|---------|------|
| /ws | /topic/cc-sessions/{sessionId}/output | CCOutputMessage | CC 输出流实时推送 |
| /ws | /topic/dev-tasks/{taskId}/status | DevTaskStatusMessage | 任务状态变更推送 |

### Request DTO 定义

| DTO 类名 | 字段 | 类型 | 校验注解 | 说明 |
|----------|------|------|---------|------|
| CreateConversationRequest | title | String | @NotBlank @Size(max=200) | 对话标题 |
| CreateConversationRequest | repositoryFullName | String | （无） | 关联仓库，可为 null |
| SendMessageRequest | content | String | @NotBlank @Size(max=10000) | 消息内容 |
| SendMessageRequest | workingDirectory | String | @NotBlank | CC 会话工作目录 |
| SendMessageRequest | environmentVariables | Map\<String, String\> | @NotNull | CC 会话环境变量 |
| CreateDevTaskRequest | conversationId | String | @NotBlank | 来源对话 ID |
| CreateDevTaskRequest | repositoryFullName | String | @NotBlank | 仓库全名 |
| CreateDevTaskRequest | branchName | String | @NotBlank @Size(max=200) | 功能分支名 |
| CreateDevTaskRequest | requirement | String | @NotBlank @Size(max=10000) | 需求描述 |
| StartDevelopmentRequest | workingDirectory | String | @NotBlank | 工作目录 |
| StartDevelopmentRequest | environmentVariables | Map\<String, String\> | @NotNull | 环境变量 |
| AdvanceToWorkingRequest | designOutput | String | @NotBlank | 设计产出 |
| AdvanceToWorkingRequest | workingDirectory | String | @NotBlank | 工作目录 |
| AdvanceToWorkingRequest | environmentVariables | Map\<String, String\> | @NotNull | 环境变量 |
| ExecutePublishRequest | baseBranch | String | @NotBlank @Size(max=200) | 目标分支 |
| MergeBranchRequest | repositoryFullName | String | @NotBlank | 仓库全名 |
| MergeBranchRequest | headBranch | String | @NotBlank | 来源分支 |
| MergeBranchRequest | baseBranch | String | @NotBlank | 目标分支 |

### WebSocket 消息 DTO 定义

| DTO 类名 | 字段 | 类型 | 说明 |
|----------|------|------|------|
| CCOutputMessage | sessionId | String | CC 会话 ID |
| CCOutputMessage | content | String | 输出内容片段 |
| CCOutputMessage | timestamp | LocalDateTime | 输出时间 |
| CCOutputMessage | type | String | 输出类型：STDOUT / STDERR / SYSTEM |
| DevTaskStatusMessage | taskId | String | 任务 ID |
| DevTaskStatusMessage | previousStatus | String | 变更前状态 |
| DevTaskStatusMessage | currentStatus | String | 变更后状态 |
| DevTaskStatusMessage | timestamp | LocalDateTime | 状态变更时间 |

### Response 包装类型定义

| 类名 | 字段 | 类型 | 说明 |
|------|------|------|------|
| Result\<T\> | code | int | HTTP 状态码 |
| Result\<T\> | message | String | 提示信息 |
| Result\<T\> | data | T | 响应数据，可为 null |

---

## 实现清单

| # | output_id | 实现项 | 类型 | 说明 |
|---|-----------|--------|------|------|
| 1 | Output_OHS_ThoughtworksAgentsDevPlatform_01 | ConversationController | 新增 | REST 控制器，路径前缀 /api/conversations，提供创建对话、发送消息、列表查询、详情查询、归档共 5 个端点，依赖 ConversationApplicationService |
| 2 | Output_OHS_ThoughtworksAgentsDevPlatform_02 | DevTaskController | 新增 | REST 控制器，路径前缀 /api/dev-tasks，提供创建任务、启动开发、推进阶段、执行发布、详情查询、列表查询共 6 个端点，依赖 DevTaskApplicationService |
| 3 | Output_OHS_ThoughtworksAgentsDevPlatform_03 | CCSessionController | 新增 | REST 控制器，路径前缀 /api/cc-sessions，提供查询详情、查询活跃列表、终止会话共 3 个端点，依赖 CCSessionApplicationService |
| 4 | Output_OHS_ThoughtworksAgentsDevPlatform_04 | GitHubController | 新增 | REST 控制器，路径前缀 /api/github，提供 OAuth 回调、仓库列表、分支合并共 3 个端点，依赖 GitHubApplicationService |
| 5 | Output_OHS_ThoughtworksAgentsDevPlatform_05 | CreateConversationRequest | 新增 | Request DTO，携带 title（@NotBlank @Size(max=200)）/ repositoryFullName 字段 |
| 6 | Output_OHS_ThoughtworksAgentsDevPlatform_06 | SendMessageRequest | 新增 | Request DTO，携带 content（@NotBlank @Size(max=10000)）/ workingDirectory（@NotBlank）/ environmentVariables（@NotNull）字段 |
| 7 | Output_OHS_ThoughtworksAgentsDevPlatform_07 | CreateDevTaskRequest | 新增 | Request DTO，携带 conversationId / repositoryFullName / branchName / requirement 字段，全部含校验注解 |
| 8 | Output_OHS_ThoughtworksAgentsDevPlatform_08 | StartDevelopmentRequest | 新增 | Request DTO，携带 workingDirectory（@NotBlank）/ environmentVariables（@NotNull）字段 |
| 9 | Output_OHS_ThoughtworksAgentsDevPlatform_09 | AdvanceToWorkingRequest | 新增 | Request DTO，携带 designOutput（@NotBlank）/ workingDirectory（@NotBlank）/ environmentVariables（@NotNull）字段 |
| 10 | Output_OHS_ThoughtworksAgentsDevPlatform_10 | ExecutePublishRequest | 新增 | Request DTO，携带 baseBranch（@NotBlank @Size(max=200)）字段 |
| 11 | Output_OHS_ThoughtworksAgentsDevPlatform_11 | MergeBranchRequest | 新增 | Request DTO，携带 repositoryFullName / headBranch / baseBranch 字段，全部 @NotBlank |
| 12 | Output_OHS_ThoughtworksAgentsDevPlatform_12 | Result | 新增 | 统一响应包装类，泛型 Result\<T\>，包含 code / message / data 字段，提供 success() 和 fail() 静态工厂方法 |
| 13 | Output_OHS_ThoughtworksAgentsDevPlatform_13 | GlobalExceptionHandler | 新增 | 全局异常处理器，@RestControllerAdvice，将 BusinessException/状态异常/GitHub 异常统一映射为标准 Result 错误响应 |
| 14 | Output_OHS_ThoughtworksAgentsDevPlatform_14 | WebSocketConfig | 新增 | WebSocket 配置类，@EnableWebSocketMessageBroker，配置 STOMP 端点 /ws 和消息代理前缀 /topic |
| 15 | Output_OHS_ThoughtworksAgentsDevPlatform_15 | WebSocketHandler | 新增 | WebSocket 推送服务，依赖 SimpMessagingTemplate，提供 pushCCOutput / pushDevTaskStatus 方法，推送到 /topic/cc-sessions/{id}/output 和 /topic/dev-tasks/{id}/status |
| 16 | Output_OHS_ThoughtworksAgentsDevPlatform_16 | CCOutputMessage | 新增 | WebSocket 推送 DTO，封装 CC 输出流消息（sessionId / content / timestamp / type） |
| 17 | Output_OHS_ThoughtworksAgentsDevPlatform_17 | DevTaskStatusMessage | 新增 | WebSocket 推送 DTO，封装任务状态变更消息（taskId / previousStatus / currentStatus / timestamp） |

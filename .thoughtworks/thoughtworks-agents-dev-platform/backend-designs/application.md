---
spec_id: Spec_Application
layer: application
order: 1
status: done
depends_on: []
description: Thoughtworks Agents 开发平台应用层设计，编排 CCSession、Conversation、DevTask、GitHubIntegration 四大核心业务用例的完整流程
---

# Application 层设计

## 结论

应用层编排四大核心业务用例：**CCSessionApplicationService**（CC 会话生命周期管理）、**ConversationApplicationService**（对话创建与消息发送，触发 CC 会话）、**DevTaskApplicationService**（开发任务三阶段流转，协调 CC 会话与 GitHub 集成）、**GitHubApplicationService**（OAuth 认证、仓库查询与分支合并）。每个服务持有所需仓储与防腐层接口，通过 Command 对象接收指令，以 `@Transactional` 保证数据一致性。

---

## 依赖契约

> 以下接口和对象定义来自 Domain 层，Application 层作为消费方使用。
> 来源为当前 idea 的 Domain 层设计文档导出契约。

### 来自 Domain 层

#### CCSession 聚合

##### 聚合根 API（来自 domain.md 导出契约）

| 类名 | 方法签名 | 返回类型 | 说明（来自 Domain 层） | 本层用途 |
|------|---------|---------|----------------------|---------|
| CCSession | `static create(ProcessConfig)` | CCSession | 创建新会话，生成 CCSessionId，状态设为 CREATED | 在 createCCSession 用例中调用以创建新会话实例 |
| CCSession | `markRunning()` | void | 标记会话为运行中，记录 startedAt | 在 startCCSession 用例中调用以标记进程已启动 |
| CCSession | `markCompleted(int)` | void | 标记会话为正常完成，记录 exitCode 和 finishedAt | 在进程回调中标记正常退出 |
| CCSession | `markFailed(int)` | void | 标记会话为失败，记录 exitCode 和 finishedAt | 在进程回调中标记异常退出 |
| CCSession | `markTerminated()` | void | 标记会话为手动终止，记录 finishedAt | 在 terminateCCSession 用例中调用 |
| CCSession | `isActive()` | boolean | 判断会话是否活跃（CREATED 或 RUNNING） | 终止前校验会话是否可操作 |

##### Repository 接口（来自 domain.md 导出契约）

| 接口名 | 方法签名 | 返回类型 | 本层用途 |
|--------|---------|---------|---------|
| CCSessionRepository | `save(CCSession)` | void | 持久化新创建或状态变更的 CCSession |
| CCSessionRepository | `findById(CCSessionId)` | Optional\<CCSession\> | 加载会话聚合根供状态变更和查询 |
| CCSessionRepository | `findActiveSessions()` | List\<CCSession\> | 查询所有活跃会话列表 |

##### 事件发布接口（来自 domain.md 导出契约）

| 接口名 | 方法签名 | 返回类型 | 本层用途 |
|--------|---------|---------|---------|
| CCSessionEventPublisher | `publishStatusChanged(CCSessionStatusChangedEvent)` | void | 在 CC 会话状态变更后发布事件，通知 Conversation/DevTask 聚合 |

---

#### Conversation 聚合

##### 聚合根 API（来自 domain.md 导出契约）

| 类名 | 方法签名 | 返回类型 | 说明（来自 Domain 层） | 本层用途 |
|------|---------|---------|----------------------|---------|
| Conversation | `static create(String, String)` | Conversation | 创建新对话，title + repositoryFullName，状态 CREATED | 在 createConversation 用例中调用 |
| Conversation | `activate(CCSessionId)` | void | 激活对话并关联 CC 会话，CREATED→ACTIVE | 在 sendMessage 用例中创建 CC 会话后调用 |
| Conversation | `addUserMessage(String)` | Message | 添加用户消息，仅 ACTIVE 状态 | 在 sendMessage 用例中记录用户输入 |
| Conversation | `addAssistantMessage(String)` | Message | 添加 AI 助手消息，仅 ACTIVE 状态 | 在 CC 会话输出回调中记录助手回复 |
| Conversation | `complete()` | void | 标记对话完成，ACTIVE→COMPLETED | 在 CC 会话正常结束时调用 |
| Conversation | `archive()` | void | 归档对话，ACTIVE/COMPLETED→ARCHIVED | 在 archiveConversation 用例中调用 |

##### Repository 接口（来自 domain.md 导出契约）

| 接口名 | 方法签名 | 返回类型 | 本层用途 |
|--------|---------|---------|---------|
| ConversationRepository | `save(Conversation)` | void | 持久化新创建或状态/消息变更的对话 |
| ConversationRepository | `findById(ConversationId)` | Optional\<Conversation\> | 加载对话聚合根供消息添加、状态变更和详情查询 |
| ConversationRepository | `findByRepositoryFullName(String)` | List\<Conversation\> | 查询指定仓库下的对话列表 |
| ConversationRepository | `findAll()` | List\<Conversation\> | 查询全部对话列表（按 updatedAt 降序） |

---

#### DevTask 聚合

##### 聚合根 API（来自 domain.md 导出契约）

| 类名 | 方法签名 | 返回类型 | 说明（来自 Domain 层） | 本层用途 |
|------|---------|---------|----------------------|---------|
| DevTask | `static create(ConversationId, String, String, String)` | DevTask | 创建新开发任务，状态 CREATED | 在 createDevTask 用例中调用 |
| DevTask | `startThinking(CCSessionId)` | TaskPhase | 开始思考阶段，CREATED→THINKING | 在 startDevelopment 用例中创建 CC 会话后调用 |
| DevTask | `completeThinking(String)` | void | 完成思考阶段并记录设计产出，自动进入 WORKING | 在思考阶段 CC 会话完成回调中调用 |
| DevTask | `startWorking(CCSessionId)` | TaskPhase | 开始工作阶段，绑定新 CC 会话 | 在 advanceToWorking 用例中创建 CC 会话后调用 |
| DevTask | `completeWorking()` | void | 完成工作阶段，WORKING→READY_TO_PUBLISH | 在工作阶段 CC 会话完成回调中调用 |
| DevTask | `startPublishing()` | TaskPhase | 开始发布阶段，READY_TO_PUBLISH→PUBLISHING | 在 executePublish 用例中调用 |
| DevTask | `completePublishing()` | void | 完成发布，PUBLISHING→PUBLISHED | 分支合并成功后调用 |
| DevTask | `fail(String)` | void | 标记任务失败，记录失败原因 | 在各阶段异常时调用 |

##### Repository 接口（来自 domain.md 导出契约）

| 接口名 | 方法签名 | 返回类型 | 本层用途 |
|--------|---------|---------|---------|
| DevTaskRepository | `save(DevTask)` | void | 持久化新创建或状态/阶段变更的任务 |
| DevTaskRepository | `findById(DevTaskId)` | Optional\<DevTask\> | 加载任务聚合根供阶段推进和状态查询 |
| DevTaskRepository | `findByConversationId(ConversationId)` | List\<DevTask\> | 查询对话关联的开发任务列表 |
| DevTaskRepository | `findByRepositoryFullName(String)` | List\<DevTask\> | 查询指定仓库下的全部任务 |

##### 事件发布接口（来自 domain.md 导出契约）

| 接口名 | 方法签名 | 返回类型 | 本层用途 |
|--------|---------|---------|---------|
| DevTaskEventPublisher | `publishStatusChanged(DevTaskStatusChangedEvent)` | void | 在 DevTask 状态变更后发布事件，驱动后续阶段自动执行 |

---

#### GitHubIntegration 聚合

##### 聚合根 API（来自 domain.md 导出契约）

| 类名 | 方法签名 | 返回类型 | 说明（来自 Domain 层） | 本层用途 |
|------|---------|---------|----------------------|---------|
| GitHubIntegration | `static create()` | GitHubIntegration | 创建集成实例，初始未认证状态 | 首次 OAuth 回调时若无记录则创建 |
| GitHubIntegration | `authenticate(OAuthToken, String)` | void | 存储 OAuth token 和用户名 | 在 handleOAuthCallback 用例中调用 |
| GitHubIntegration | `isAuthenticated()` | boolean | 判断是否已认证 | 在 listRepositories 前校验认证状态 |
| GitHubIntegration | `getRequiredToken()` | OAuthToken | 获取 token，未认证时抛 GitHubNotAuthenticatedException | 在 listRepositories / mergeBranch 中获取 token |

##### Repository 接口（来自 domain.md 导出契约）

| 接口名 | 方法签名 | 返回类型 | 本层用途 |
|--------|---------|---------|---------|
| GitHubIntegrationRepository | `save(GitHubIntegration)` | void | 持久化认证状态变更 |
| GitHubIntegrationRepository | `find()` | Optional\<GitHubIntegration\> | 加载唯一集成记录 |

##### 防腐层接口（来自 domain.md 导出契约）

| 接口名 | 方法签名 | 返回类型 | 本层用途 |
|--------|---------|---------|---------|
| GitHubAclService | `exchangeCodeForToken(String)` | OAuthToken | OAuth 授权码换 access token |
| GitHubAclService | `getAuthenticatedUsername(OAuthToken)` | String | 获取已认证 GitHub 用户名 |
| GitHubAclService | `listRepositories(OAuthToken)` | List\<Repository\> | 获取用户仓库列表 |
| GitHubAclService | `mergeBranch(OAuthToken, String, String, String)` | void | 执行分支合并操作 |

---

## Command 对象

### CreateCCSessionCommand

**对应用例**：创建 CC 会话

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| command | String | @NotBlank | 执行命令，如 `claude -p '...'` |
| workingDirectory | String | @NotBlank | 工作目录路径 |
| environmentVariables | Map\<String, String\> | @NotNull | 环境变量，可为空 Map |

**构建方式**：`@Builder`，所有字段 final

---

### StartCCSessionCommand

**对应用例**：启动 CC 会话进程

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| sessionId | String | @NotBlank | 要启动的会话 ID |

**构建方式**：`@Builder`，所有字段 final

---

### TerminateCCSessionCommand

**对应用例**：终止 CC 会话

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| sessionId | String | @NotBlank | 要终止的会话 ID |

**构建方式**：`@Builder`，所有字段 final

---

### CreateConversationCommand

**对应用例**：创建对话

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| title | String | @NotBlank | 对话标题 |
| repositoryFullName | String | （无） | 关联的 GitHub 仓库全名，可为 null |

**构建方式**：`@Builder`，所有字段 final

---

### SendMessageCommand

**对应用例**：发送消息（如果是对话首条消息则触发 CC 会话创建与激活）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| conversationId | String | @NotBlank | 目标对话 ID |
| content | String | @NotBlank | 消息内容 |
| workingDirectory | String | @NotBlank | CC 会话工作目录（首次发送时使用） |
| environmentVariables | Map\<String, String\> | @NotNull | CC 会话环境变量（首次发送时使用） |

**构建方式**：`@Builder`，所有字段 final

---

### ArchiveConversationCommand

**对应用例**：归档对话

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| conversationId | String | @NotBlank | 要归档的对话 ID |

**构建方式**：`@Builder`，所有字段 final

---

### CreateDevTaskCommand

**对应用例**：创建开发任务

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| conversationId | String | @NotBlank | 来源对话 ID |
| repositoryFullName | String | @NotBlank | 关联的 GitHub 仓库全名 |
| branchName | String | @NotBlank | 功能分支名称 |
| requirement | String | @NotBlank | 需求描述内容 |

**构建方式**：`@Builder`，所有字段 final

---

### StartDevelopmentCommand

**对应用例**：启动开发（触发思考阶段）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| taskId | String | @NotBlank | 目标任务 ID |
| workingDirectory | String | @NotBlank | 思考阶段 CC 会话工作目录 |
| environmentVariables | Map\<String, String\> | @NotNull | 思考阶段 CC 会话环境变量 |

**构建方式**：`@Builder`，所有字段 final

---

### AdvanceToWorkingCommand

**对应用例**：推进到工作阶段（思考→工作）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| taskId | String | @NotBlank | 目标任务 ID |
| designOutput | String | @NotBlank | 思考阶段产出的设计文档内容 |
| workingDirectory | String | @NotBlank | 工作阶段 CC 会话工作目录 |
| environmentVariables | Map\<String, String\> | @NotNull | 工作阶段 CC 会话环境变量 |

**构建方式**：`@Builder`，所有字段 final

---

### ExecutePublishCommand

**对应用例**：执行发布（触发分支合并）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| taskId | String | @NotBlank | 目标任务 ID |
| baseBranch | String | @NotBlank | 目标合并分支，通常为 main |

**构建方式**：`@Builder`，所有字段 final

---

### HandleOAuthCallbackCommand

**对应用例**：处理 GitHub OAuth 回调，存储 token

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| code | String | @NotBlank | GitHub 授权码 |

**构建方式**：`@Builder`，所有字段 final

---

## 应用服务

### CCSessionApplicationService

**依赖**：

| 字段 | 类型 | 用途 |
|------|------|------|
| ccSessionRepository | CCSessionRepository | CC 会话聚合持久化 |
| ccSessionEventPublisher | CCSessionEventPublisher | 发布 CC 会话状态变更事件 |

---

#### CCSessionDTO createCCSession(CreateCCSessionCommand command)

**用例**：创建 CC 会话，将命令封装为 ProcessConfig，初始化 CCSession 聚合根并持久化
**事务**：`@Transactional(rollbackFor = Exception.class)`

**编排步骤**：
1. 构建 `ProcessConfig`：`new ProcessConfig(command.getCommand(), command.getWorkingDirectory(), command.getEnvironmentVariables())`
2. 创建会话：`CCSession session = CCSession.create(processConfig)`
3. `ccSessionRepository.save(session)` — 持久化新建会话
4. 返回 `CCSessionDTO.from(session)`

**异常处理**：
- ProcessConfig 校验失败（command/workingDirectory 为空）→ 抛出 `BusinessException("命令和工作目录不能为空")`

---

#### void startCCSession(StartCCSessionCommand command)

**用例**：标记 CC 会话为运行中（通常由进程管理器在子进程成功启动后回调）
**事务**：`@Transactional(rollbackFor = Exception.class)`

**编排步骤**：
1. `ccSessionRepository.findById(new CCSessionId(command.getSessionId()))` — 加载会话聚合根
2. `session.markRunning()` — 调用业务方法标记运行中
3. `ccSessionRepository.save(session)` — 持久化状态变更
4. `ccSessionEventPublisher.publishStatusChanged(new CCSessionStatusChangedEvent(...))` — 发布状态变更事件

**异常处理**：
- 会话不存在 → 抛出 `BusinessException("CC 会话不存在: " + command.getSessionId())`
- 非法状态转换（非 CREATED 状态调用）→ 透传 `IllegalCCSessionStateException`

---

#### void terminateCCSession(TerminateCCSessionCommand command)

**用例**：手动终止一个活跃的 CC 会话
**事务**：`@Transactional(rollbackFor = Exception.class)`

**编排步骤**：
1. `ccSessionRepository.findById(new CCSessionId(command.getSessionId()))` — 加载会话聚合根
2. 校验 `session.isActive()` — 确认会话处于活跃状态
3. `session.markTerminated()` — 标记终止
4. `ccSessionRepository.save(session)` — 持久化状态变更
5. `ccSessionEventPublisher.publishStatusChanged(new CCSessionStatusChangedEvent(...))` — 发布事件

**异常处理**：
- 会话不存在 → 抛出 `BusinessException("CC 会话不存在: " + command.getSessionId())`
- 会话非活跃（已完成/已终止）→ 抛出 `BusinessException("会话已结束，无法终止")`

---

#### CCSessionDTO getSession(String sessionId)

**用例**：查询指定 CC 会话的状态详情
**事务**：`@Transactional(readOnly = true)`

**编排步骤**：
1. `ccSessionRepository.findById(new CCSessionId(sessionId))` — 加载会话
2. 返回 `CCSessionDTO.from(session)`

**异常处理**：
- 会话不存在 → 抛出 `BusinessException("CC 会话不存在: " + sessionId)`

---

#### List\<CCSessionDTO\> getActiveSessions()

**用例**：获取所有活跃 CC 会话列表
**事务**：`@Transactional(readOnly = true)`

**编排步骤**：
1. `ccSessionRepository.findActiveSessions()` — 查询活跃会话列表
2. 返回 `sessions.stream().map(CCSessionDTO::from).toList()`

---

### ConversationApplicationService

**依赖**：

| 字段 | 类型 | 用途 |
|------|------|------|
| conversationRepository | ConversationRepository | 对话聚合持久化 |
| ccSessionRepository | CCSessionRepository | 创建关联 CC 会话 |
| ccSessionEventPublisher | CCSessionEventPublisher | 发布 CC 会话状态变更事件 |

---

#### ConversationDTO createConversation(CreateConversationCommand command)

**用例**：创建一个新对话，关联可选的 GitHub 仓库
**事务**：`@Transactional(rollbackFor = Exception.class)`

**编排步骤**：
1. `Conversation conversation = Conversation.create(command.getTitle(), command.getRepositoryFullName())` — 创建对话聚合根
2. `conversationRepository.save(conversation)` — 持久化
3. 返回 `ConversationDTO.from(conversation)`

**异常处理**：
- title 为空（由 Command 校验注解拦截，不应到达此处）→ 抛出 `BusinessException("对话标题不能为空")`

---

#### ConversationDTO sendMessage(SendMessageCommand command)

**用例**：发送消息；若对话首次发送则创建 CC 会话并激活对话，随后记录用户消息
**事务**：`@Transactional(rollbackFor = Exception.class)`

**编排步骤**：
1. `conversationRepository.findById(new ConversationId(command.getConversationId()))` — 加载对话
2. 判断对话状态是否为 CREATED：
   - 若是 CREATED（首条消息）：
     - 构建 `ProcessConfig`：`command` 为 `claude -p '{content}'`，workingDirectory 和 environmentVariables 来自 command
     - `CCSession ccSession = CCSession.create(processConfig)` — 创建 CC 会话
     - `ccSessionRepository.save(ccSession)` — 持久化 CC 会话
     - `conversation.activate(ccSession.getId())` — 激活对话并关联 CC 会话
   - 若已是 ACTIVE：直接继续
3. `conversation.addUserMessage(command.getContent())` — 记录用户消息
4. `conversationRepository.save(conversation)` — 持久化
5. 返回 `ConversationDTO.from(conversation)`

**异常处理**：
- 对话不存在 → 抛出 `BusinessException("对话不存在: " + command.getConversationId())`
- 对话已归档或完成 → 透传 `IllegalConversationStateException`

---

#### List\<ConversationDTO\> listConversations()

**用例**：获取全部对话列表（按 updatedAt 降序）
**事务**：`@Transactional(readOnly = true)`

**编排步骤**：
1. `conversationRepository.findAll()` — 查询全部对话
2. 返回 `conversations.stream().map(ConversationDTO::from).toList()`

---

#### List\<ConversationDTO\> listConversationsByRepository(String repositoryFullName)

**用例**：获取指定仓库下的对话列表
**事务**：`@Transactional(readOnly = true)`

**编排步骤**：
1. `conversationRepository.findByRepositoryFullName(repositoryFullName)` — 查询
2. 返回 `conversations.stream().map(ConversationDTO::from).toList()`

---

#### ConversationDTO getConversation(String conversationId)

**用例**：获取对话详情（含消息列表）
**事务**：`@Transactional(readOnly = true)`

**编排步骤**：
1. `conversationRepository.findById(new ConversationId(conversationId))` — 加载
2. 返回 `ConversationDTO.from(conversation)`（含 messages）

**异常处理**：
- 对话不存在 → 抛出 `BusinessException("对话不存在: " + conversationId)`

---

#### void archiveConversation(ArchiveConversationCommand command)

**用例**：归档对话
**事务**：`@Transactional(rollbackFor = Exception.class)`

**编排步骤**：
1. `conversationRepository.findById(new ConversationId(command.getConversationId()))` — 加载对话
2. `conversation.archive()` — 调用业务方法
3. `conversationRepository.save(conversation)` — 持久化

**异常处理**：
- 对话不存在 → 抛出 `BusinessException("对话不存在: " + command.getConversationId())`
- 非法状态（CREATED 状态归档）→ 透传 `IllegalConversationStateException`

---

### DevTaskApplicationService

**依赖**：

| 字段 | 类型 | 用途 |
|------|------|------|
| devTaskRepository | DevTaskRepository | 开发任务聚合持久化 |
| ccSessionRepository | CCSessionRepository | 为思考/工作阶段创建 CC 会话 |
| devTaskEventPublisher | DevTaskEventPublisher | 发布任务状态变更事件 |
| gitHubIntegrationRepository | GitHubIntegrationRepository | 发布阶段获取认证信息 |
| gitHubAclService | GitHubAclService | 发布阶段执行分支合并 |

---

#### DevTaskDTO createDevTask(CreateDevTaskCommand command)

**用例**：创建开发任务，关联来源对话、仓库、功能分支和需求描述
**事务**：`@Transactional(rollbackFor = Exception.class)`

**编排步骤**：
1. `DevTask task = DevTask.create(new ConversationId(command.getConversationId()), command.getRepositoryFullName(), command.getBranchName(), command.getRequirement())` — 创建任务聚合根
2. `devTaskRepository.save(task)` — 持久化
3. 返回 `DevTaskDTO.from(task)`

**异常处理**：
- 必填字段为空（由 Command 校验注解拦截）→ 抛出 `BusinessException("任务必填字段不能为空")`

---

#### DevTaskDTO startDevelopment(StartDevelopmentCommand command)

**用例**：启动开发，创建思考阶段 CC 会话并标记任务进入 THINKING 状态
**事务**：`@Transactional(rollbackFor = Exception.class)`

**编排步骤**：
1. `devTaskRepository.findById(new DevTaskId(command.getTaskId()))` — 加载任务
2. 构建思考阶段命令：`command` 为 `claude -p '请基于以下需求生成设计方案: {task.getRequirement()}'`
3. `CCSession thinkingSession = CCSession.create(processConfig)` — 创建思考阶段 CC 会话
4. `ccSessionRepository.save(thinkingSession)` — 持久化 CC 会话
5. `task.startThinking(thinkingSession.getId())` — 标记任务进入 THINKING，绑定 CC 会话
6. `devTaskRepository.save(task)` — 持久化任务
7. `devTaskEventPublisher.publishStatusChanged(new DevTaskStatusChangedEvent(...))` — 发布状态变更事件
8. 返回 `DevTaskDTO.from(task)`

**异常处理**：
- 任务不存在 → 抛出 `BusinessException("开发任务不存在: " + command.getTaskId())`
- 非 CREATED 状态 → 透传 `IllegalDevTaskStateException`

---

#### DevTaskDTO advanceToWorking(AdvanceToWorkingCommand command)

**用例**：完成思考阶段并推进到工作阶段，创建工作阶段 CC 会话
**事务**：`@Transactional(rollbackFor = Exception.class)`

**编排步骤**：
1. `devTaskRepository.findById(new DevTaskId(command.getTaskId()))` — 加载任务
2. `task.completeThinking(command.getDesignOutput())` — 完成思考阶段，状态自动转为 WORKING
3. 构建工作阶段命令：`command` 为 `claude -p '请基于以下设计方案开始编码实现: {designOutput}'`
4. `CCSession workingSession = CCSession.create(processConfig)` — 创建工作阶段 CC 会话
5. `ccSessionRepository.save(workingSession)` — 持久化 CC 会话
6. `task.startWorking(workingSession.getId())` — 绑定工作阶段 CC 会话
7. `devTaskRepository.save(task)` — 持久化任务
8. `devTaskEventPublisher.publishStatusChanged(new DevTaskStatusChangedEvent(...))` — 发布状态变更事件
9. 返回 `DevTaskDTO.from(task)`

**异常处理**：
- 任务不存在 → 抛出 `BusinessException("开发任务不存在: " + command.getTaskId())`
- 非 THINKING 状态 → 透传 `IllegalDevTaskStateException`

---

#### DevTaskDTO executePublish(ExecutePublishCommand command)

**用例**：用户触发发布，合并功能分支到目标分支（通常为 main），完成后标记任务为 PUBLISHED
**事务**：`@Transactional(rollbackFor = Exception.class)`

**编排步骤**：
1. `devTaskRepository.findById(new DevTaskId(command.getTaskId()))` — 加载任务
2. `task.startPublishing()` — 标记任务进入 PUBLISHING
3. `devTaskRepository.save(task)` — 持久化（先保存以防后续失败可追溯状态）
4. `gitHubIntegrationRepository.find()` — 加载 GitHub 集成记录
5. `integration.getRequiredToken()` — 获取 OAuth token（未认证抛 GitHubNotAuthenticatedException）
6. `gitHubAclService.mergeBranch(token, task.getRepositoryFullName(), task.getBranchName(), command.getBaseBranch())` — 执行分支合并
7. `task.completePublishing()` — 标记发布完成
8. `devTaskRepository.save(task)` — 持久化最终状态
9. `devTaskEventPublisher.publishStatusChanged(new DevTaskStatusChangedEvent(...))` — 发布事件
10. 返回 `DevTaskDTO.from(task)`

**异常处理**：
- 任务不存在 → 抛出 `BusinessException("开发任务不存在: " + command.getTaskId())`
- 非 READY_TO_PUBLISH 状态 → 透传 `IllegalDevTaskStateException`
- GitHub 未认证 → 透传 `GitHubNotAuthenticatedException`
- 合并冲突 → 捕获 `GitHubMergeConflictException`，调用 `task.fail("分支合并冲突")` 后持久化，再重新抛出
- GitHub API 失败 → 捕获 `GitHubApiException`，调用 `task.fail("GitHub API 调用失败")` 后持久化，再重新抛出

---

#### DevTaskDTO getDevTask(String taskId)

**用例**：查询开发任务详情（含阶段信息）
**事务**：`@Transactional(readOnly = true)`

**编排步骤**：
1. `devTaskRepository.findById(new DevTaskId(taskId))` — 加载任务
2. 返回 `DevTaskDTO.from(task)`（含 phases）

**异常处理**：
- 任务不存在 → 抛出 `BusinessException("开发任务不存在: " + taskId)`

---

#### List\<DevTaskDTO\> listDevTasksByConversation(String conversationId)

**用例**：查询对话关联的开发任务列表
**事务**：`@Transactional(readOnly = true)`

**编排步骤**：
1. `devTaskRepository.findByConversationId(new ConversationId(conversationId))` — 查询
2. 返回 `tasks.stream().map(DevTaskDTO::from).toList()`

---

### GitHubApplicationService

**依赖**：

| 字段 | 类型 | 用途 |
|------|------|------|
| gitHubIntegrationRepository | GitHubIntegrationRepository | GitHub 集成状态持久化与查询 |
| gitHubAclService | GitHubAclService | 调用 GitHub 外部 API |

---

#### void handleOAuthCallback(HandleOAuthCallbackCommand command)

**用例**：处理 GitHub OAuth 回调，用授权码换取 access token 并持久化
**事务**：`@Transactional(rollbackFor = Exception.class)`

**编排步骤**：
1. `gitHubAclService.exchangeCodeForToken(command.getCode())` — 授权码换 token
2. `gitHubAclService.getAuthenticatedUsername(token)` — 获取用户名
3. `gitHubIntegrationRepository.find()` — 尝试加载已有集成记录
4. 若不存在：`GitHubIntegration integration = GitHubIntegration.create()` — 创建新实例
5. `integration.authenticate(token, username)` — 存储认证信息
6. `gitHubIntegrationRepository.save(integration)` — 持久化

**异常处理**：
- 授权码无效或过期 → 透传 `GitHubApiException`

---

#### List\<RepositoryDTO\> listRepositories()

**用例**：获取已认证 GitHub 用户的仓库列表
**事务**：`@Transactional(readOnly = true)`

**编排步骤**：
1. `gitHubIntegrationRepository.find()` — 加载集成记录
2. 校验集成记录存在且已认证（`integration.isAuthenticated()`）
3. `OAuthToken token = integration.getRequiredToken()` — 获取 token
4. `List<Repository> repos = gitHubAclService.listRepositories(token)` — 调用 GitHub API
5. 返回 `repos.stream().map(RepositoryDTO::from).toList()`

**异常处理**：
- 未完成 GitHub OAuth 认证 → 抛出 `BusinessException("请先完成 GitHub OAuth 认证")` 或透传 `GitHubNotAuthenticatedException`
- GitHub API 失败 → 透传 `GitHubApiException`

---

#### MergeResultDTO mergeBranch(String repositoryFullName, String headBranch, String baseBranch)

**用例**：执行分支合并（供 DevTaskApplicationService 内部复用，或外部直接调用）
**事务**：`@Transactional(rollbackFor = Exception.class)`

**编排步骤**：
1. `gitHubIntegrationRepository.find()` — 加载集成记录
2. `OAuthToken token = integration.getRequiredToken()` — 获取 token
3. `gitHubAclService.mergeBranch(token, repositoryFullName, headBranch, baseBranch)` — 执行合并
4. 返回 `MergeResultDTO.success(repositoryFullName, headBranch, baseBranch)`

**异常处理**：
- 未认证 → 透传 `GitHubNotAuthenticatedException`
- 合并冲突 → 透传 `GitHubMergeConflictException`
- GitHub API 失败 → 透传 `GitHubApiException`

---

## 导出契约

### 应用服务 API

| 类名 | 方法签名 | 返回类型 | 说明 |
|------|---------|---------|------|
| CCSessionApplicationService | `createCCSession(CreateCCSessionCommand)` | CCSessionDTO | 创建 CC 会话 |
| CCSessionApplicationService | `startCCSession(StartCCSessionCommand)` | void | 标记 CC 会话为运行中 |
| CCSessionApplicationService | `terminateCCSession(TerminateCCSessionCommand)` | void | 终止活跃 CC 会话 |
| CCSessionApplicationService | `getSession(String)` | CCSessionDTO | 查询指定 CC 会话详情 |
| CCSessionApplicationService | `getActiveSessions()` | List\<CCSessionDTO\> | 查询所有活跃 CC 会话 |
| ConversationApplicationService | `createConversation(CreateConversationCommand)` | ConversationDTO | 创建对话 |
| ConversationApplicationService | `sendMessage(SendMessageCommand)` | ConversationDTO | 发送消息，首次发送触发 CC 会话创建 |
| ConversationApplicationService | `listConversations()` | List\<ConversationDTO\> | 获取全部对话列表 |
| ConversationApplicationService | `listConversationsByRepository(String)` | List\<ConversationDTO\> | 获取指定仓库对话列表 |
| ConversationApplicationService | `getConversation(String)` | ConversationDTO | 获取对话详情（含消息） |
| ConversationApplicationService | `archiveConversation(ArchiveConversationCommand)` | void | 归档对话 |
| DevTaskApplicationService | `createDevTask(CreateDevTaskCommand)` | DevTaskDTO | 创建开发任务 |
| DevTaskApplicationService | `startDevelopment(StartDevelopmentCommand)` | DevTaskDTO | 启动开发，触发思考阶段 |
| DevTaskApplicationService | `advanceToWorking(AdvanceToWorkingCommand)` | DevTaskDTO | 完成思考，推进到工作阶段 |
| DevTaskApplicationService | `executePublish(ExecutePublishCommand)` | DevTaskDTO | 执行发布，合并分支 |
| DevTaskApplicationService | `getDevTask(String)` | DevTaskDTO | 查询开发任务详情 |
| DevTaskApplicationService | `listDevTasksByConversation(String)` | List\<DevTaskDTO\> | 查询对话关联任务列表 |
| GitHubApplicationService | `handleOAuthCallback(HandleOAuthCallbackCommand)` | void | 处理 OAuth 回调，存储 token |
| GitHubApplicationService | `listRepositories()` | List\<RepositoryDTO\> | 获取用户仓库列表 |
| GitHubApplicationService | `mergeBranch(String, String, String)` | MergeResultDTO | 执行分支合并 |

---

### Command 定义

| 类名 | 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|------|
| CreateCCSessionCommand | command | String | @NotBlank | 执行命令 |
| CreateCCSessionCommand | workingDirectory | String | @NotBlank | 工作目录 |
| CreateCCSessionCommand | environmentVariables | Map\<String, String\> | @NotNull | 环境变量 |
| StartCCSessionCommand | sessionId | String | @NotBlank | 会话 ID |
| TerminateCCSessionCommand | sessionId | String | @NotBlank | 会话 ID |
| CreateConversationCommand | title | String | @NotBlank | 对话标题 |
| CreateConversationCommand | repositoryFullName | String | （无） | 关联仓库，可为 null |
| SendMessageCommand | conversationId | String | @NotBlank | 对话 ID |
| SendMessageCommand | content | String | @NotBlank | 消息内容 |
| SendMessageCommand | workingDirectory | String | @NotBlank | CC 会话工作目录 |
| SendMessageCommand | environmentVariables | Map\<String, String\> | @NotNull | CC 会话环境变量 |
| ArchiveConversationCommand | conversationId | String | @NotBlank | 对话 ID |
| CreateDevTaskCommand | conversationId | String | @NotBlank | 来源对话 ID |
| CreateDevTaskCommand | repositoryFullName | String | @NotBlank | 仓库全名 |
| CreateDevTaskCommand | branchName | String | @NotBlank | 功能分支名 |
| CreateDevTaskCommand | requirement | String | @NotBlank | 需求描述 |
| StartDevelopmentCommand | taskId | String | @NotBlank | 任务 ID |
| StartDevelopmentCommand | workingDirectory | String | @NotBlank | 工作目录 |
| StartDevelopmentCommand | environmentVariables | Map\<String, String\> | @NotNull | 环境变量 |
| AdvanceToWorkingCommand | taskId | String | @NotBlank | 任务 ID |
| AdvanceToWorkingCommand | designOutput | String | @NotBlank | 思考阶段设计产出 |
| AdvanceToWorkingCommand | workingDirectory | String | @NotBlank | 工作目录 |
| AdvanceToWorkingCommand | environmentVariables | Map\<String, String\> | @NotNull | 环境变量 |
| ExecutePublishCommand | taskId | String | @NotBlank | 任务 ID |
| ExecutePublishCommand | baseBranch | String | @NotBlank | 目标分支，如 main |
| HandleOAuthCallbackCommand | code | String | @NotBlank | GitHub 授权码 |

---

### 返回类型定义

| 类名 | 字段 | 类型 | 说明 |
|------|------|------|------|
| CCSessionDTO | id | String | 会话 ID |
| CCSessionDTO | status | String | 会话状态枚举名称 |
| CCSessionDTO | command | String | 执行命令 |
| CCSessionDTO | workingDirectory | String | 工作目录 |
| CCSessionDTO | createdAt | LocalDateTime | 创建时间 |
| CCSessionDTO | startedAt | LocalDateTime | 启动时间，可为 null |
| CCSessionDTO | finishedAt | LocalDateTime | 结束时间，可为 null |
| CCSessionDTO | exitCode | Integer | 退出码，可为 null |
| ConversationDTO | id | String | 对话 ID |
| ConversationDTO | title | String | 对话标题 |
| ConversationDTO | repositoryFullName | String | 关联仓库，可为 null |
| ConversationDTO | status | String | 对话状态枚举名称 |
| ConversationDTO | messages | List\<MessageDTO\> | 消息列表 |
| ConversationDTO | createdAt | LocalDateTime | 创建时间 |
| ConversationDTO | updatedAt | LocalDateTime | 更新时间 |
| MessageDTO | id | String | 消息 ID |
| MessageDTO | role | String | 角色（USER/ASSISTANT） |
| MessageDTO | content | String | 消息内容 |
| MessageDTO | createdAt | LocalDateTime | 创建时间 |
| DevTaskDTO | id | String | 任务 ID |
| DevTaskDTO | conversationId | String | 来源对话 ID |
| DevTaskDTO | repositoryFullName | String | 关联仓库全名 |
| DevTaskDTO | branchName | String | 功能分支名 |
| DevTaskDTO | requirement | String | 需求描述 |
| DevTaskDTO | status | String | 任务状态枚举名称 |
| DevTaskDTO | phases | List\<TaskPhaseDTO\> | 阶段记录列表 |
| DevTaskDTO | createdAt | LocalDateTime | 创建时间 |
| DevTaskDTO | updatedAt | LocalDateTime | 更新时间 |
| TaskPhaseDTO | id | String | 阶段 ID |
| TaskPhaseDTO | phaseType | String | 阶段类型（THINKING/WORKING/PUBLISHING） |
| TaskPhaseDTO | ccSessionId | String | 关联 CC 会话 ID，可为 null |
| TaskPhaseDTO | output | String | 阶段产出，可为 null |
| TaskPhaseDTO | startedAt | LocalDateTime | 开始时间 |
| TaskPhaseDTO | finishedAt | LocalDateTime | 结束时间，可为 null |
| TaskPhaseDTO | failureReason | String | 失败原因，可为 null |
| RepositoryDTO | fullName | String | 仓库全名 owner/repo |
| RepositoryDTO | defaultBranch | String | 默认分支名 |
| RepositoryDTO | cloneUrl | String | 克隆地址 |
| RepositoryDTO | isPrivate | boolean | 是否私有 |
| MergeResultDTO | repositoryFullName | String | 仓库全名 |
| MergeResultDTO | headBranch | String | 来源分支 |
| MergeResultDTO | baseBranch | String | 目标分支 |
| MergeResultDTO | success | boolean | 是否合并成功 |

---

## 实现清单

| # | output_id | 实现项 | 类型 | 说明 |
|---|-----------|--------|------|------|
| 1 | Output_Application_ThoughtworksAgentsDevPlatform_01 | CCSessionApplicationService | 新增 | 应用服务，编排 CCSession 生命周期：createCCSession / startCCSession / terminateCCSession / getSession / getActiveSessions |
| 2 | Output_Application_ThoughtworksAgentsDevPlatform_02 | CreateCCSessionCommand | 新增 | Command 对象，携带 command / workingDirectory / environmentVariables，含 @NotBlank / @NotNull 校验 |
| 3 | Output_Application_ThoughtworksAgentsDevPlatform_03 | StartCCSessionCommand | 新增 | Command 对象，携带 sessionId，含 @NotBlank 校验 |
| 4 | Output_Application_ThoughtworksAgentsDevPlatform_04 | TerminateCCSessionCommand | 新增 | Command 对象，携带 sessionId，含 @NotBlank 校验 |
| 5 | Output_Application_ThoughtworksAgentsDevPlatform_05 | CCSessionDTO | 新增 | 返回 DTO，封装 CCSession 展示字段（id/status/command/workingDirectory/时间/exitCode） |
| 6 | Output_Application_ThoughtworksAgentsDevPlatform_06 | ConversationApplicationService | 新增 | 应用服务，编排对话流程：createConversation / sendMessage（触发 CC 会话）/ listConversations / listConversationsByRepository / getConversation / archiveConversation |
| 7 | Output_Application_ThoughtworksAgentsDevPlatform_07 | CreateConversationCommand | 新增 | Command 对象，携带 title / repositoryFullName，title 含 @NotBlank 校验 |
| 8 | Output_Application_ThoughtworksAgentsDevPlatform_08 | SendMessageCommand | 新增 | Command 对象，携带 conversationId / content / workingDirectory / environmentVariables，含校验注解 |
| 9 | Output_Application_ThoughtworksAgentsDevPlatform_09 | ArchiveConversationCommand | 新增 | Command 对象，携带 conversationId，含 @NotBlank 校验 |
| 10 | Output_Application_ThoughtworksAgentsDevPlatform_10 | ConversationDTO | 新增 | 返回 DTO，封装 Conversation 展示字段（id/title/repo/status/messages/时间） |
| 11 | Output_Application_ThoughtworksAgentsDevPlatform_11 | MessageDTO | 新增 | 返回 DTO，封装 Message 展示字段（id/role/content/createdAt） |
| 12 | Output_Application_ThoughtworksAgentsDevPlatform_12 | DevTaskApplicationService | 新增 | 应用服务，编排开发任务流程：createDevTask / startDevelopment / advanceToWorking / executePublish（触发合并）/ getDevTask / listDevTasksByConversation |
| 13 | Output_Application_ThoughtworksAgentsDevPlatform_13 | CreateDevTaskCommand | 新增 | Command 对象，携带 conversationId / repositoryFullName / branchName / requirement，全字段 @NotBlank |
| 14 | Output_Application_ThoughtworksAgentsDevPlatform_14 | StartDevelopmentCommand | 新增 | Command 对象，携带 taskId / workingDirectory / environmentVariables，含校验注解 |
| 15 | Output_Application_ThoughtworksAgentsDevPlatform_15 | AdvanceToWorkingCommand | 新增 | Command 对象，携带 taskId / designOutput / workingDirectory / environmentVariables，含校验注解 |
| 16 | Output_Application_ThoughtworksAgentsDevPlatform_16 | ExecutePublishCommand | 新增 | Command 对象，携带 taskId / baseBranch，含 @NotBlank 校验 |
| 17 | Output_Application_ThoughtworksAgentsDevPlatform_17 | DevTaskDTO | 新增 | 返回 DTO，封装 DevTask 展示字段（id/conversationId/repo/branch/requirement/status/phases/时间） |
| 18 | Output_Application_ThoughtworksAgentsDevPlatform_18 | TaskPhaseDTO | 新增 | 返回 DTO，封装 TaskPhase 展示字段（id/phaseType/ccSessionId/output/时间/failureReason） |
| 19 | Output_Application_ThoughtworksAgentsDevPlatform_19 | GitHubApplicationService | 新增 | 应用服务，编排 GitHub 集成：handleOAuthCallback（存储 token）/ listRepositories / mergeBranch |
| 20 | Output_Application_ThoughtworksAgentsDevPlatform_20 | HandleOAuthCallbackCommand | 新增 | Command 对象，携带 code，含 @NotBlank 校验 |
| 21 | Output_Application_ThoughtworksAgentsDevPlatform_21 | RepositoryDTO | 新增 | 返回 DTO，封装 Repository 展示字段（fullName/defaultBranch/cloneUrl/isPrivate） |
| 22 | Output_Application_ThoughtworksAgentsDevPlatform_22 | MergeResultDTO | 新增 | 返回 DTO，封装合并结果（repositoryFullName/headBranch/baseBranch/success） |

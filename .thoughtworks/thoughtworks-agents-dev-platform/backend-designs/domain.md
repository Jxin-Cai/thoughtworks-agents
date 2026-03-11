---
spec_id: Spec_Domain
layer: domain
order: 1
status: done
depends_on: []
description: Thoughtworks Agents 开发平台领域层设计，涵盖 CCSession、Conversation、DevTask、GitHubIntegration 四个核心聚合的完整建模
---

# Domain 层设计

## 结论

领域层包含四个聚合：CCSession（管理 Claude Code CLI 子进程生命周期）、Conversation（管理对话与消息记录）、DevTask（管理开发任务三阶段流转）、GitHubIntegration（管理 GitHub OAuth 认证与仓库操作）。CCSession 是基础能力聚合，被 Conversation 和 DevTask 跨聚合引用；GitHubIntegration 通过防腐层隔离外部 GitHub API 依赖。

## 聚合: CCSession

### 聚合根与实体

#### CCSession

**职责**：管理 Claude Code CLI 子进程的完整生命周期，包括进程启动、输入发送、输出流接收和进程终止。

**字段**：

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | CCSessionId | 必填，唯一 | 会话唯一标识 |
| processConfig | ProcessConfig | 必填 | 进程配置（命令、工作目录、环境变量） |
| status | CCSessionStatus | 必填 | 会话状态枚举 |
| exitCode | Integer | 可选 | 进程退出码，仅在 COMPLETED / FAILED 时有值 |
| createdAt | LocalDateTime | 必填 | 创建时间 |
| startedAt | LocalDateTime | 可选 | 进程启动时间 |
| finishedAt | LocalDateTime | 可选 | 进程结束时间 |

**静态工厂方法**：

```java
public static CCSession create(ProcessConfig processConfig) {
    // 生成 CCSessionId，设置 status 为 CREATED，记录 createdAt
}
```

**业务方法**：

| 方法签名 | 行为描述 | 业务规则 |
|----------|----------|----------|
| `void markRunning()` | 标记会话为运行中，记录 startedAt | 仅 CREATED 状态可调用，否则抛出 IllegalCCSessionStateException |
| `void markCompleted(int exitCode)` | 标记会话为正常完成，记录 exitCode 和 finishedAt | 仅 RUNNING 状态可调用，否则抛出 IllegalCCSessionStateException |
| `void markFailed(int exitCode)` | 标记会话为失败，记录 exitCode 和 finishedAt | 仅 RUNNING 状态可调用，否则抛出 IllegalCCSessionStateException |
| `void markTerminated()` | 标记会话为手动终止，记录 finishedAt | 仅 RUNNING 状态可调用，否则抛出 IllegalCCSessionStateException |
| `boolean isActive()` | 返回会话是否处于活跃状态 | CREATED 或 RUNNING 视为活跃 |

**不变量**：
- 状态只能单向流转：CREATED → RUNNING → COMPLETED / FAILED / TERMINATED → 违反时抛出 IllegalCCSessionStateException
- exitCode 仅在终态（COMPLETED / FAILED）时有值 → 违反时抛出 IllegalArgumentException
- processConfig 创建后不可变 → 通过 final 字段保证

### 值对象

#### CCSessionId

| 字段 | 类型 | 说明 |
|------|------|------|
| value | String | UUID 格式的会话标识 |

**验证规则**：
- 创建时校验：value 不可为空且符合 UUID 格式

#### ProcessConfig

| 字段 | 类型 | 说明 |
|------|------|------|
| command | String | 执行命令，如 `claude -p '...'` |
| workingDirectory | String | 工作目录路径 |
| environmentVariables | Map\<String, String\> | 环境变量键值对 |

**验证规则**：
- 创建时校验：command 不可为空，workingDirectory 不可为空
- environmentVariables 可为空 Map，但不可为 null

#### CCSessionStatus

| 字段 | 类型 | 说明 |
|------|------|------|
| CREATED | enum | 已创建，等待启动 |
| RUNNING | enum | 进程运行中 |
| COMPLETED | enum | 正常完成 |
| FAILED | enum | 执行失败 |
| TERMINATED | enum | 手动终止 |

**业务方法**：
- `boolean canTransitionTo(CCSessionStatus target)` — 校验状态转换是否合法

### 仓储接口

#### CCSessionRepository

```java
public interface CCSessionRepository {

    /**
     * 保存或更新 CCSession。
     * 基于 id 做 insert-or-update。
     * id 冲突时执行 update。
     */
    void save(CCSession session);

    /**
     * 根据 ID 查找 CCSession。
     * 完整加载 ProcessConfig 值对象。
     * 不存在时返回 Optional.empty()。
     */
    Optional<CCSession> findById(CCSessionId id);

    /**
     * 查找所有活跃状态的会话。
     * 返回 status 为 CREATED 或 RUNNING 的会话列表。
     */
    List<CCSession> findActiveSessions();
}
```

### 领域事件

#### CCSessionStatusChangedEvent

| 字段 | 类型 | 说明 |
|------|------|------|
| sessionId | CCSessionId | 会话标识 |
| previousStatus | CCSessionStatus | 变更前状态 |
| currentStatus | CCSessionStatus | 变更后状态 |
| occurredAt | LocalDateTime | 事件发生时间 |

**触发时机**：CCSession 状态发生变更时（markRunning / markCompleted / markFailed / markTerminated）
**消费方预期**：Conversation 和 DevTask 聚合监听此事件以更新关联状态

#### CCSessionEventPublisher

```java
public interface CCSessionEventPublisher {

    /**
     * 发布会话状态变更事件。
     * 在 CCSession 状态转换成功后触发。
     * 消费方（Conversation / DevTask）据此更新自身状态。
     */
    void publishStatusChanged(CCSessionStatusChangedEvent event);
}
```

### 导出契约

#### 聚合根与实体 API

| 类名 | 方法签名 | 返回类型 | 说明 |
|------|---------|---------|------|
| CCSession | `static create(ProcessConfig)` | CCSession | 创建新会话 |
| CCSession | `markRunning()` | void | 标记为运行中 |
| CCSession | `markCompleted(int)` | void | 标记为正常完成 |
| CCSession | `markFailed(int)` | void | 标记为失败 |
| CCSession | `markTerminated()` | void | 标记为手动终止 |
| CCSession | `isActive()` | boolean | 判断是否活跃 |

#### 值对象定义

| 类名 | 字段 | 类型 | 说明 |
|------|------|------|------|
| CCSessionId | value | String | UUID 格式会话标识 |
| ProcessConfig | command | String | 执行命令 |
| ProcessConfig | workingDirectory | String | 工作目录路径 |
| ProcessConfig | environmentVariables | Map\<String, String\> | 环境变量 |

#### 接口签名

| 接口名 | 方法签名 | 返回类型 | 行为描述 |
|--------|---------|---------|---------|
| CCSessionRepository | `save(CCSession)` | void | insert-or-update，基于 id 冲突时执行 update |
| CCSessionRepository | `findById(CCSessionId)` | Optional\<CCSession\> | 完整加载 ProcessConfig，不存在返回 empty |
| CCSessionRepository | `findActiveSessions()` | List\<CCSession\> | 返回 CREATED 或 RUNNING 状态的会话 |
| CCSessionEventPublisher | `publishStatusChanged(CCSessionStatusChangedEvent)` | void | 至少一次投递，消费方需幂等处理 |

## 聚合: Conversation

### 聚合根与实体

#### Conversation

**职责**：管理对话的完整生命周期和消息记录，关联 GitHub 仓库和 CC 会话。

**字段**：

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | ConversationId | 必填，唯一 | 对话唯一标识 |
| title | String | 必填 | 对话标题 |
| repositoryFullName | String | 可选 | 关联的 GitHub 仓库全名（如 owner/repo） |
| ccSessionId | CCSessionId | 可选 | 关联的 CC 会话 ID |
| status | ConversationStatus | 必填 | 对话状态 |
| messages | List\<Message\> | 必填 | 消息列表 |
| createdAt | LocalDateTime | 必填 | 创建时间 |
| updatedAt | LocalDateTime | 必填 | 最后更新时间 |

**静态工厂方法**：

```java
public static Conversation create(String title, String repositoryFullName) {
    // 生成 ConversationId，status 设为 CREATED，初始化空 messages 列表
}
```

**业务方法**：

| 方法签名 | 行为描述 | 业务规则 |
|----------|----------|----------|
| `void activate(CCSessionId ccSessionId)` | 激活对话，关联 CC 会话 | 仅 CREATED 状态可调用，否则抛出 IllegalConversationStateException |
| `Message addUserMessage(String content)` | 添加用户消息 | 仅 ACTIVE 状态可添加，否则抛出 IllegalConversationStateException |
| `Message addAssistantMessage(String content)` | 添加 AI 助手消息 | 仅 ACTIVE 状态可添加，否则抛出 IllegalConversationStateException |
| `void complete()` | 标记对话为已完成 | 仅 ACTIVE 状态可调用，否则抛出 IllegalConversationStateException |
| `void archive()` | 归档对话 | ACTIVE 或 COMPLETED 状态可调用，否则抛出 IllegalConversationStateException |

**不变量**：
- 状态流转：CREATED → ACTIVE → COMPLETED → ARCHIVED，或 ACTIVE → ARCHIVED → 违反时抛出 IllegalConversationStateException
- 仅 ACTIVE 状态下可添加消息 → 违反时抛出 IllegalConversationStateException
- ccSessionId 一旦关联不可变更 → 违反时抛出 IllegalArgumentException

#### Message

**职责**：记录对话中的单条消息。

**字段**：

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | MessageId | 必填，唯一 | 消息唯一标识 |
| role | MessageRole | 必填 | 消息角色（USER / ASSISTANT） |
| content | String | 必填 | 消息内容 |
| createdAt | LocalDateTime | 必填 | 创建时间 |

### 值对象

#### ConversationId

| 字段 | 类型 | 说明 |
|------|------|------|
| value | String | UUID 格式的对话标识 |

**验证规则**：
- 创建时校验：value 不可为空且符合 UUID 格式

#### MessageId

| 字段 | 类型 | 说明 |
|------|------|------|
| value | String | UUID 格式的消息标识 |

**验证规则**：
- 创建时校验：value 不可为空且符合 UUID 格式

#### ConversationStatus

| 字段 | 类型 | 说明 |
|------|------|------|
| CREATED | enum | 已创建，等待激活 |
| ACTIVE | enum | 活跃中，可收发消息 |
| COMPLETED | enum | 已完成 |
| ARCHIVED | enum | 已归档 |

**业务方法**：
- `boolean canTransitionTo(ConversationStatus target)` — 校验状态转换是否合法

#### MessageRole

| 字段 | 类型 | 说明 |
|------|------|------|
| USER | enum | 用户消息 |
| ASSISTANT | enum | AI 助手消息 |

### 仓储接口

#### ConversationRepository

```java
public interface ConversationRepository {

    /**
     * 保存或更新 Conversation。
     * 基于 id 做 insert-or-update，级联保存 messages。
     * id 冲突时执行 update。
     */
    void save(Conversation conversation);

    /**
     * 根据 ID 查找 Conversation。
     * 完整加载 messages 列表。
     * 不存在时返回 Optional.empty()。
     */
    Optional<Conversation> findById(ConversationId id);

    /**
     * 查找指定仓库下的所有对话。
     * 按 createdAt 降序排列。
     */
    List<Conversation> findByRepositoryFullName(String repositoryFullName);

    /**
     * 查找所有对话列表。
     * 按 updatedAt 降序排列。
     */
    List<Conversation> findAll();
}
```

### 导出契约

#### 聚合根与实体 API

| 类名 | 方法签名 | 返回类型 | 说明 |
|------|---------|---------|------|
| Conversation | `static create(String, String)` | Conversation | 创建新对话 |
| Conversation | `activate(CCSessionId)` | void | 激活对话并关联 CC 会话 |
| Conversation | `addUserMessage(String)` | Message | 添加用户消息 |
| Conversation | `addAssistantMessage(String)` | Message | 添加 AI 助手消息 |
| Conversation | `complete()` | void | 标记对话完成 |
| Conversation | `archive()` | void | 归档对话 |

#### 值对象定义

| 类名 | 字段 | 类型 | 说明 |
|------|------|------|------|
| ConversationId | value | String | UUID 格式对话标识 |
| MessageId | value | String | UUID 格式消息标识 |
| ConversationStatus | - | enum | CREATED / ACTIVE / COMPLETED / ARCHIVED |
| MessageRole | - | enum | USER / ASSISTANT |

#### 接口签名

| 接口名 | 方法签名 | 返回类型 | 行为描述 |
|--------|---------|---------|---------|
| ConversationRepository | `save(Conversation)` | void | insert-or-update，级联保存 messages |
| ConversationRepository | `findById(ConversationId)` | Optional\<Conversation\> | 完整加载 messages 列表，不存在返回 empty |
| ConversationRepository | `findByRepositoryFullName(String)` | List\<Conversation\> | 按 createdAt 降序，返回指定仓库的对话 |
| ConversationRepository | `findAll()` | List\<Conversation\> | 按 updatedAt 降序，返回全部对话 |

## 聚合: DevTask

### 聚合根与实体

#### DevTask

**职责**：管理开发任务的全生命周期，协调思考、工作、发布三个阶段的有序流转。

**字段**：

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | DevTaskId | 必填，唯一 | 任务唯一标识 |
| conversationId | ConversationId | 必填 | 来源对话 ID |
| repositoryFullName | String | 必填 | 关联的 GitHub 仓库全名 |
| branchName | String | 必填 | 功能分支名称 |
| requirement | String | 必填 | 需求描述内容 |
| status | DevTaskStatus | 必填 | 任务状态 |
| phases | List\<TaskPhase\> | 必填 | 阶段执行记录列表 |
| createdAt | LocalDateTime | 必填 | 创建时间 |
| updatedAt | LocalDateTime | 必填 | 最后更新时间 |

**静态工厂方法**：

```java
public static DevTask create(ConversationId conversationId,
                             String repositoryFullName,
                             String branchName,
                             String requirement) {
    // 生成 DevTaskId，status 设为 CREATED，初始化空 phases 列表
}
```

**业务方法**：

| 方法签名 | 行为描述 | 业务规则 |
|----------|----------|----------|
| `TaskPhase startThinking(CCSessionId ccSessionId)` | 开始思考阶段，创建 THINKING 阶段记录 | 仅 CREATED 状态可调用，否则抛出 IllegalDevTaskStateException |
| `void completeThinking(String designOutput)` | 完成思考阶段，记录设计产出 | 仅 THINKING 状态可调用，否则抛出 IllegalDevTaskStateException |
| `TaskPhase startWorking(CCSessionId ccSessionId)` | 开始工作阶段，创建 WORKING 阶段记录 | 仅 WORKING 状态（由 completeThinking 自动转入）可调用，否则抛出 IllegalDevTaskStateException |
| `void completeWorking()` | 完成工作阶段，代码分支就绪 | 仅 WORKING 状态可调用，否则抛出 IllegalDevTaskStateException |
| `TaskPhase startPublishing()` | 开始发布阶段，创建 PUBLISHING 阶段记录 | 仅 READY_TO_PUBLISH 状态可调用，否则抛出 IllegalDevTaskStateException |
| `void completePublishing()` | 完成发布，合并成功 | 仅 PUBLISHING 状态可调用，否则抛出 IllegalDevTaskStateException |
| `void fail(String reason)` | 标记任务失败 | THINKING / WORKING / PUBLISHING 状态可调用，否则抛出 IllegalDevTaskStateException |

**不变量**：
- 状态流转：CREATED → THINKING → WORKING → READY_TO_PUBLISH → PUBLISHING → PUBLISHED，任何活跃阶段可转入 FAILED → 违反时抛出 IllegalDevTaskStateException
- 阶段顺序：phases 列表中的阶段类型必须按 THINKING → WORKING → PUBLISHING 顺序追加 → 违反时抛出 IllegalArgumentException
- completeThinking 成功后自动将 status 转为 WORKING → 内部保证
- completeWorking 成功后自动将 status 转为 READY_TO_PUBLISH → 内部保证
- requirement 创建后不可变 → 通过 final 字段保证

#### TaskPhase

**职责**：记录开发任务某个阶段的执行信息。

**字段**：

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | TaskPhaseId | 必填，唯一 | 阶段记录唯一标识 |
| phaseType | PhaseType | 必填 | 阶段类型（THINKING / WORKING / PUBLISHING） |
| ccSessionId | CCSessionId | 可选 | 关联的 CC 会话 ID（PUBLISHING 阶段可能无需 CC 会话） |
| output | String | 可选 | 阶段产出内容（如设计文档） |
| startedAt | LocalDateTime | 必填 | 阶段开始时间 |
| finishedAt | LocalDateTime | 可选 | 阶段结束时间 |
| failureReason | String | 可选 | 失败原因 |

### 值对象

#### DevTaskId

| 字段 | 类型 | 说明 |
|------|------|------|
| value | String | UUID 格式的任务标识 |

**验证规则**：
- 创建时校验：value 不可为空且符合 UUID 格式

#### TaskPhaseId

| 字段 | 类型 | 说明 |
|------|------|------|
| value | String | UUID 格式的阶段记录标识 |

**验证规则**：
- 创建时校验：value 不可为空且符合 UUID 格式

#### DevTaskStatus

| 字段 | 类型 | 说明 |
|------|------|------|
| CREATED | enum | 已创建，等待开始 |
| THINKING | enum | 思考阶段进行中 |
| WORKING | enum | 工作阶段进行中 |
| READY_TO_PUBLISH | enum | 代码就绪，等待用户发布 |
| PUBLISHING | enum | 发布进行中 |
| PUBLISHED | enum | 发布完成 |
| FAILED | enum | 任务失败 |

**业务方法**：
- `boolean canTransitionTo(DevTaskStatus target)` — 校验状态转换是否合法
- `boolean isTerminal()` — 判断是否为终态（PUBLISHED / FAILED）

#### PhaseType

| 字段 | 类型 | 说明 |
|------|------|------|
| THINKING | enum | 思考阶段 |
| WORKING | enum | 工作阶段 |
| PUBLISHING | enum | 发布阶段 |

### 仓储接口

#### DevTaskRepository

```java
public interface DevTaskRepository {

    /**
     * 保存或更新 DevTask。
     * 基于 id 做 insert-or-update，级联保存 phases。
     * id 冲突时执行 update。
     */
    void save(DevTask task);

    /**
     * 根据 ID 查找 DevTask。
     * 完整加载 phases 列表。
     * 不存在时返回 Optional.empty()。
     */
    Optional<DevTask> findById(DevTaskId id);

    /**
     * 查找指定对话关联的开发任务。
     * 按 createdAt 降序排列。
     */
    List<DevTask> findByConversationId(ConversationId conversationId);

    /**
     * 查找指定仓库下的所有开发任务。
     * 按 createdAt 降序排列。
     */
    List<DevTask> findByRepositoryFullName(String repositoryFullName);
}
```

### 领域事件

#### DevTaskStatusChangedEvent

| 字段 | 类型 | 说明 |
|------|------|------|
| taskId | DevTaskId | 任务标识 |
| previousStatus | DevTaskStatus | 变更前状态 |
| currentStatus | DevTaskStatus | 变更后状态 |
| occurredAt | LocalDateTime | 事件发生时间 |

**触发时机**：DevTask 状态发生变更时
**消费方预期**：Application 层监听此事件以触发后续阶段的自动执行（如 THINKING 完成后自动启动 WORKING）

#### DevTaskEventPublisher

```java
public interface DevTaskEventPublisher {

    /**
     * 发布任务状态变更事件。
     * 在 DevTask 状态转换成功后触发。
     * 消费方据此决定是否自动启动下一阶段。
     */
    void publishStatusChanged(DevTaskStatusChangedEvent event);
}
```

### 导出契约

#### 聚合根与实体 API

| 类名 | 方法签名 | 返回类型 | 说明 |
|------|---------|---------|------|
| DevTask | `static create(ConversationId, String, String, String)` | DevTask | 创建新开发任务 |
| DevTask | `startThinking(CCSessionId)` | TaskPhase | 开始思考阶段 |
| DevTask | `completeThinking(String)` | void | 完成思考阶段，自动进入 WORKING |
| DevTask | `startWorking(CCSessionId)` | TaskPhase | 开始工作阶段 |
| DevTask | `completeWorking()` | void | 完成工作阶段，进入 READY_TO_PUBLISH |
| DevTask | `startPublishing()` | TaskPhase | 开始发布阶段 |
| DevTask | `completePublishing()` | void | 完成发布 |
| DevTask | `fail(String)` | void | 标记任务失败 |

#### 值对象定义

| 类名 | 字段 | 类型 | 说明 |
|------|------|------|------|
| DevTaskId | value | String | UUID 格式任务标识 |
| TaskPhaseId | value | String | UUID 格式阶段标识 |
| DevTaskStatus | - | enum | CREATED / THINKING / WORKING / READY_TO_PUBLISH / PUBLISHING / PUBLISHED / FAILED |
| PhaseType | - | enum | THINKING / WORKING / PUBLISHING |

#### 接口签名

| 接口名 | 方法签名 | 返回类型 | 行为描述 |
|--------|---------|---------|---------|
| DevTaskRepository | `save(DevTask)` | void | insert-or-update，级联保存 phases |
| DevTaskRepository | `findById(DevTaskId)` | Optional\<DevTask\> | 完整加载 phases 列表，不存在返回 empty |
| DevTaskRepository | `findByConversationId(ConversationId)` | List\<DevTask\> | 按 createdAt 降序，返回指定对话的任务 |
| DevTaskRepository | `findByRepositoryFullName(String)` | List\<DevTask\> | 按 createdAt 降序，返回指定仓库的任务 |
| DevTaskEventPublisher | `publishStatusChanged(DevTaskStatusChangedEvent)` | void | 至少一次投递，消费方需幂等处理 |

## 聚合: GitHubIntegration

### 聚合根与实体

#### GitHubIntegration

**职责**：管理 GitHub OAuth 认证信息和仓库访问能力，作为单例聚合维护全局 GitHub 集成状态。

**字段**：

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | GitHubIntegrationId | 必填，唯一 | 集成标识（单用户模式下为固定值） |
| oauthToken | OAuthToken | 可选 | GitHub OAuth access token |
| authenticatedUser | String | 可选 | 已认证的 GitHub 用户名 |
| createdAt | LocalDateTime | 必填 | 创建时间 |
| updatedAt | LocalDateTime | 必填 | 最后更新时间 |

**静态工厂方法**：

```java
public static GitHubIntegration create() {
    // 生成 GitHubIntegrationId，初始化为未认证状态
}
```

**业务方法**：

| 方法签名 | 行为描述 | 业务规则 |
|----------|----------|----------|
| `void authenticate(OAuthToken token, String username)` | 存储 OAuth token 和用户名 | token 不可为 null，否则抛出 IllegalArgumentException |
| `void revokeAuthentication()` | 撤销认证，清除 token | 无前置条件 |
| `boolean isAuthenticated()` | 判断是否已认证 | oauthToken 不为 null 即视为已认证 |
| `OAuthToken getRequiredToken()` | 获取 token，未认证时抛异常 | 未认证时抛出 GitHubNotAuthenticatedException |

**不变量**：
- 认证状态与 token 一致：isAuthenticated() 为 true 时 oauthToken 必不为 null → 内部保证
- 单用户模式下全局唯一 → 由 Repository 实现保证

### 值对象

#### GitHubIntegrationId

| 字段 | 类型 | 说明 |
|------|------|------|
| value | String | 集成标识 |

**验证规则**：
- 创建时校验：value 不可为空

#### OAuthToken

| 字段 | 类型 | 说明 |
|------|------|------|
| accessToken | String | GitHub access token |
| tokenType | String | token 类型，通常为 bearer |
| scope | String | 授权范围 |
| createdAt | LocalDateTime | token 获取时间 |

**验证规则**：
- 创建时校验：accessToken 不可为空

#### Repository

| 字段 | 类型 | 说明 |
|------|------|------|
| fullName | String | 仓库全名，格式 owner/repo |
| defaultBranch | String | 默认分支名称 |
| cloneUrl | String | 仓库克隆地址 |
| isPrivate | boolean | 是否私有仓库 |

**验证规则**：
- 创建时校验：fullName 不可为空且必须包含 `/`

#### Branch

| 字段 | 类型 | 说明 |
|------|------|------|
| name | String | 分支名称 |
| sha | String | 分支最新 commit SHA |

**验证规则**：
- 创建时校验：name 不可为空

### 仓储接口

#### GitHubIntegrationRepository

```java
public interface GitHubIntegrationRepository {

    /**
     * 保存或更新 GitHubIntegration。
     * 基于 id 做 insert-or-update。
     * 单用户模式下全局仅一条记录。
     */
    void save(GitHubIntegration integration);

    /**
     * 查找当前 GitHubIntegration。
     * 单用户模式下返回唯一记录。
     * 不存在时返回 Optional.empty()。
     */
    Optional<GitHubIntegration> find();
}
```

### 防腐层接口

#### GitHubAclService

```java
public interface GitHubAclService {

    /**
     * 获取已认证用户的仓库列表。
     * 调用 GitHub API GET /user/repos。
     * 失败时抛出 GitHubApiException。
     */
    List<Repository> listRepositories(OAuthToken token);

    /**
     * 获取指定仓库的分支列表。
     * 调用 GitHub API GET /repos/{owner}/{repo}/branches。
     * 失败时抛出 GitHubApiException。
     */
    List<Branch> listBranches(OAuthToken token, String repositoryFullName);

    /**
     * 创建功能分支。
     * 调用 GitHub API POST /repos/{owner}/{repo}/git/refs。
     * 基于指定的 sourceBranch 创建新分支。
     * 失败时抛出 GitHubApiException。
     */
    Branch createBranch(OAuthToken token, String repositoryFullName,
                        String branchName, String sourceBranch);

    /**
     * 合并分支到目标分支。
     * 调用 GitHub API POST /repos/{owner}/{repo}/merges。
     * 冲突时抛出 GitHubMergeConflictException。
     * 其他失败抛出 GitHubApiException。
     */
    void mergeBranch(OAuthToken token, String repositoryFullName,
                     String headBranch, String baseBranch);

    /**
     * 使用授权码交换 access token。
     * 调用 GitHub OAuth API POST /login/oauth/access_token。
     * 失败时抛出 GitHubApiException。
     */
    OAuthToken exchangeCodeForToken(String code);

    /**
     * 获取认证用户信息。
     * 调用 GitHub API GET /user。
     * 失败时抛出 GitHubApiException。
     */
    String getAuthenticatedUsername(OAuthToken token);
}
```

### 导出契约

#### 聚合根与实体 API

| 类名 | 方法签名 | 返回类型 | 说明 |
|------|---------|---------|------|
| GitHubIntegration | `static create()` | GitHubIntegration | 创建集成实例 |
| GitHubIntegration | `authenticate(OAuthToken, String)` | void | 存储认证信息 |
| GitHubIntegration | `revokeAuthentication()` | void | 撤销认证 |
| GitHubIntegration | `isAuthenticated()` | boolean | 判断是否已认证 |
| GitHubIntegration | `getRequiredToken()` | OAuthToken | 获取 token，未认证时抛异常 |

#### 值对象定义

| 类名 | 字段 | 类型 | 说明 |
|------|------|------|------|
| GitHubIntegrationId | value | String | 集成标识 |
| OAuthToken | accessToken | String | GitHub access token |
| OAuthToken | tokenType | String | token 类型 |
| OAuthToken | scope | String | 授权范围 |
| OAuthToken | createdAt | LocalDateTime | token 获取时间 |
| Repository | fullName | String | 仓库全名 owner/repo |
| Repository | defaultBranch | String | 默认分支名称 |
| Repository | cloneUrl | String | 克隆地址 |
| Repository | isPrivate | boolean | 是否私有 |
| Branch | name | String | 分支名称 |
| Branch | sha | String | 最新 commit SHA |

#### 接口签名

| 接口名 | 方法签名 | 返回类型 | 行为描述 |
|--------|---------|---------|---------|
| GitHubIntegrationRepository | `save(GitHubIntegration)` | void | insert-or-update，单用户模式全局一条 |
| GitHubIntegrationRepository | `find()` | Optional\<GitHubIntegration\> | 返回唯一记录，不存在返回 empty |
| GitHubAclService | `listRepositories(OAuthToken)` | List\<Repository\> | 调用 GitHub API 获取仓库列表，超时 10s，失败抛 GitHubApiException |
| GitHubAclService | `listBranches(OAuthToken, String)` | List\<Branch\> | 调用 GitHub API 获取分支列表，超时 10s，失败抛 GitHubApiException |
| GitHubAclService | `createBranch(OAuthToken, String, String, String)` | Branch | 调用 GitHub API 创建分支，超时 10s，失败抛 GitHubApiException |
| GitHubAclService | `mergeBranch(OAuthToken, String, String, String)` | void | 调用 GitHub API 合并分支，冲突抛 GitHubMergeConflictException |
| GitHubAclService | `exchangeCodeForToken(String)` | OAuthToken | OAuth 授权码换 token，超时 10s，失败抛 GitHubApiException |
| GitHubAclService | `getAuthenticatedUsername(OAuthToken)` | String | 获取认证用户名，超时 10s，失败抛 GitHubApiException |

## 实现清单

| # | output_id | 实现项 | 类型 | 说明 |
|---|-----------|--------|------|------|
| 1 | Output_Domain_ThoughtworksAgentsDevPlatform_01 | CCSession | 新增 | 聚合根，管理 CC CLI 子进程生命周期，包含状态机（CREATED→RUNNING→COMPLETED/FAILED/TERMINATED）和 ProcessConfig |
| 2 | Output_Domain_ThoughtworksAgentsDevPlatform_02 | CCSessionId | 新增 | 值对象，UUID 格式会话标识 |
| 3 | Output_Domain_ThoughtworksAgentsDevPlatform_03 | ProcessConfig | 新增 | 值对象，封装命令、工作目录、环境变量 |
| 4 | Output_Domain_ThoughtworksAgentsDevPlatform_04 | CCSessionStatus | 新增 | 枚举，定义 CC 会话五种状态及合法转换规则 |
| 5 | Output_Domain_ThoughtworksAgentsDevPlatform_05 | CCSessionRepository | 新增 | 仓储接口，提供 save / findById / findActiveSessions |
| 6 | Output_Domain_ThoughtworksAgentsDevPlatform_06 | CCSessionStatusChangedEvent | 新增 | 领域事件，CC 会话状态变更时发布 |
| 7 | Output_Domain_ThoughtworksAgentsDevPlatform_07 | CCSessionEventPublisher | 新增 | 事件发布接口，发布 CC 会话状态变更事件 |
| 8 | Output_Domain_ThoughtworksAgentsDevPlatform_08 | Conversation | 新增 | 聚合根，管理对话生命周期和消息记录，关联 GitHub 仓库和 CC 会话 |
| 9 | Output_Domain_ThoughtworksAgentsDevPlatform_09 | Message | 新增 | 实体，记录对话中的单条消息（角色、内容、时间） |
| 10 | Output_Domain_ThoughtworksAgentsDevPlatform_10 | ConversationId | 新增 | 值对象，UUID 格式对话标识 |
| 11 | Output_Domain_ThoughtworksAgentsDevPlatform_11 | MessageId | 新增 | 值对象，UUID 格式消息标识 |
| 12 | Output_Domain_ThoughtworksAgentsDevPlatform_12 | ConversationStatus | 新增 | 枚举，定义对话四种状态及合法转换规则 |
| 13 | Output_Domain_ThoughtworksAgentsDevPlatform_13 | MessageRole | 新增 | 枚举，USER / ASSISTANT 两种角色 |
| 14 | Output_Domain_ThoughtworksAgentsDevPlatform_14 | ConversationRepository | 新增 | 仓储接口，提供 save / findById / findByRepositoryFullName / findAll |
| 15 | Output_Domain_ThoughtworksAgentsDevPlatform_15 | DevTask | 新增 | 聚合根，管理开发任务三阶段流转，包含 CREATED→THINKING→WORKING→READY_TO_PUBLISH→PUBLISHING→PUBLISHED/FAILED 状态机 |
| 16 | Output_Domain_ThoughtworksAgentsDevPlatform_16 | TaskPhase | 新增 | 实体，记录开发阶段执行信息（类型、CC 会话、产出、时间） |
| 17 | Output_Domain_ThoughtworksAgentsDevPlatform_17 | DevTaskId | 新增 | 值对象，UUID 格式任务标识 |
| 18 | Output_Domain_ThoughtworksAgentsDevPlatform_18 | TaskPhaseId | 新增 | 值对象，UUID 格式阶段记录标识 |
| 19 | Output_Domain_ThoughtworksAgentsDevPlatform_19 | DevTaskStatus | 新增 | 枚举，定义开发任务七种状态及合法转换规则 |
| 20 | Output_Domain_ThoughtworksAgentsDevPlatform_20 | PhaseType | 新增 | 枚举，THINKING / WORKING / PUBLISHING 三种阶段类型 |
| 21 | Output_Domain_ThoughtworksAgentsDevPlatform_21 | DevTaskRepository | 新增 | 仓储接口，提供 save / findById / findByConversationId / findByRepositoryFullName |
| 22 | Output_Domain_ThoughtworksAgentsDevPlatform_22 | DevTaskStatusChangedEvent | 新增 | 领域事件，开发任务状态变更时发布 |
| 23 | Output_Domain_ThoughtworksAgentsDevPlatform_23 | DevTaskEventPublisher | 新增 | 事件发布接口，发布开发任务状态变更事件 |
| 24 | Output_Domain_ThoughtworksAgentsDevPlatform_24 | GitHubIntegration | 新增 | 聚合根，管理 GitHub OAuth 认证信息，单用户模式全局唯一 |
| 25 | Output_Domain_ThoughtworksAgentsDevPlatform_25 | GitHubIntegrationId | 新增 | 值对象，集成标识 |
| 26 | Output_Domain_ThoughtworksAgentsDevPlatform_26 | OAuthToken | 新增 | 值对象，封装 GitHub OAuth access token 信息 |
| 27 | Output_Domain_ThoughtworksAgentsDevPlatform_27 | Repository | 新增 | 值对象，封装 GitHub 仓库信息（全名、默认分支、克隆地址） |
| 28 | Output_Domain_ThoughtworksAgentsDevPlatform_28 | Branch | 新增 | 值对象，封装 GitHub 分支信息（名称、SHA） |
| 29 | Output_Domain_ThoughtworksAgentsDevPlatform_29 | GitHubIntegrationRepository | 新增 | 仓储接口，提供 save / find（单用户模式） |
| 30 | Output_Domain_ThoughtworksAgentsDevPlatform_30 | GitHubAclService | 新增 | 防腐层接口，隔离 GitHub API 调用（仓库列表、分支操作、OAuth 认证） |
| 31 | Output_Domain_ThoughtworksAgentsDevPlatform_31 | IllegalCCSessionStateException | 新增 | 异常类，CC 会话非法状态转换时抛出 |
| 32 | Output_Domain_ThoughtworksAgentsDevPlatform_32 | IllegalConversationStateException | 新增 | 异常类，对话非法状态转换时抛出 |
| 33 | Output_Domain_ThoughtworksAgentsDevPlatform_33 | IllegalDevTaskStateException | 新增 | 异常类，开发任务非法状态转换时抛出 |
| 34 | Output_Domain_ThoughtworksAgentsDevPlatform_34 | GitHubNotAuthenticatedException | 新增 | 异常类，未认证时访问 GitHub 功能抛出 |
| 35 | Output_Domain_ThoughtworksAgentsDevPlatform_35 | GitHubApiException | 新增 | 异常类，GitHub API 调用失败时抛出 |
| 36 | Output_Domain_ThoughtworksAgentsDevPlatform_36 | GitHubMergeConflictException | 新增 | 异常类，GitHub 分支合并冲突时抛出 |

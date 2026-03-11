---
spec_id: Spec_Infr
layer: infr
order: 1
status: done
depends_on: []
description: Thoughtworks Agents 开发平台基础设施层设计，涵盖四个聚合的仓储实现、H2 数据库表结构、PO 映射、Claude Code CLI 进程管理、GitHub API 防腐层、WebSocket 消息推送及事件发布机制
---

# Infrastructure 层设计

## 结论

基础设施层为 CCSession、Conversation、DevTask、GitHubIntegration 四个聚合提供完整的技术实现。包括：5 张 H2 数据库表（cc_session、conversation、message、dev_task、task_phase、github_integration）及对应的 PO 对象和 MyBatis-Plus Mapper；4 个仓储实现类，处理领域对象与数据库之间的双向映射和级联持久化；Claude Code CLI 进程管理封装（基于 ProcessBuilder）；GitHub REST API 客户端（RestTemplate + OAuth）；WebSocket 消息推送基础设施；以及 2 个基于 Spring ApplicationEvent 的事件发布实现。

## 依赖契约

> 以下接口定义来自 Domain 层，Infr 层负责提供实现。
> 按聚合分组，每个聚合列出其需要实现的接口。

### 来自 CCSession 聚合

#### 仓储实现契约（来自 domain.md 导出契约）

| 接口名 | 方法签名 | 返回类型 | 行为描述 | 实现方案 | 本层用途 |
|--------|---------|---------|---------|---------|---------|
| CCSessionRepository | `save(CCSession)` | void | insert-or-update，基于 id 冲突时执行 update | MyBatis-Plus insertOrUpdate | 实现 save() 时根据 id 判断 insert / update，ProcessConfig 序列化为 JSON 存储 |
| CCSessionRepository | `findById(CCSessionId)` | Optional\<CCSession\> | 完整加载 ProcessConfig，不存在返回 empty | MyBatis-Plus selectById | 查询主表 PO，将 JSON 反序列化为 ProcessConfig 值对象，组装聚合根 |
| CCSessionRepository | `findActiveSessions()` | List\<CCSession\> | 返回 CREATED 或 RUNNING 状态的会话 | MyBatis-Plus 条件查询 | 使用 LambdaQueryWrapper 按 status IN (CREATED, RUNNING) 查询 |

#### 事件发布实现契约（来自 domain.md 导出契约）

| 接口名 | 方法签名 | 返回类型 | 行为描述 | 实现方案 | 本层用途 |
|--------|---------|---------|---------|---------|---------|
| CCSessionEventPublisher | `publishStatusChanged(CCSessionStatusChangedEvent)` | void | 至少一次投递，消费方需幂等处理 | Spring ApplicationEvent | 将领域事件包装为 Spring ApplicationEvent，通过 ApplicationEventPublisher 发布，同进程内同步投递 |

### 来自 Conversation 聚合

#### 仓储实现契约（来自 domain.md 导出契约）

| 接口名 | 方法签名 | 返回类型 | 行为描述 | 实现方案 | 本层用途 |
|--------|---------|---------|---------|---------|---------|
| ConversationRepository | `save(Conversation)` | void | insert-or-update，级联保存 messages | MyBatis-Plus insertOrUpdate + 批量操作 | 主表 insert/update，messages 采用先删后插策略（delete by conversation_id + batch insert） |
| ConversationRepository | `findById(ConversationId)` | Optional\<Conversation\> | 完整加载 messages 列表，不存在返回 empty | MyBatis-Plus selectById + 关联查询 | 查询 conversation 主表，再根据 conversation_id 查询 message 表，组装完整聚合 |
| ConversationRepository | `findByRepositoryFullName(String)` | List\<Conversation\> | 按 createdAt 降序，返回指定仓库的对话 | MyBatis-Plus 条件查询 | LambdaQueryWrapper 按 repository_full_name 查询，orderByDesc(createdAt)，逐个加载 messages |
| ConversationRepository | `findAll()` | List\<Conversation\> | 按 updatedAt 降序，返回全部对话 | MyBatis-Plus selectList | 全表查询 orderByDesc(updatedAt)，逐个加载 messages |

### 来自 DevTask 聚合

#### 仓储实现契约（来自 domain.md 导出契约）

| 接口名 | 方法签名 | 返回类型 | 行为描述 | 实现方案 | 本层用途 |
|--------|---------|---------|---------|---------|---------|
| DevTaskRepository | `save(DevTask)` | void | insert-or-update，级联保存 phases | MyBatis-Plus insertOrUpdate + 批量操作 | 主表 insert/update，phases 采用先删后插策略（delete by dev_task_id + batch insert） |
| DevTaskRepository | `findById(DevTaskId)` | Optional\<DevTask\> | 完整加载 phases 列表，不存在返回 empty | MyBatis-Plus selectById + 关联查询 | 查询 dev_task 主表，再根据 dev_task_id 查询 task_phase 表，组装完整聚合 |
| DevTaskRepository | `findByConversationId(ConversationId)` | List\<DevTask\> | 按 createdAt 降序，返回指定对话的任务 | MyBatis-Plus 条件查询 | LambdaQueryWrapper 按 conversation_id 查询，orderByDesc(createdAt)，逐个加载 phases |
| DevTaskRepository | `findByRepositoryFullName(String)` | List\<DevTask\> | 按 createdAt 降序，返回指定仓库的任务 | MyBatis-Plus 条件查询 | LambdaQueryWrapper 按 repository_full_name 查询，orderByDesc(createdAt)，逐个加载 phases |

#### 事件发布实现契约（来自 domain.md 导出契约）

| 接口名 | 方法签名 | 返回类型 | 行为描述 | 实现方案 | 本层用途 |
|--------|---------|---------|---------|---------|---------|
| DevTaskEventPublisher | `publishStatusChanged(DevTaskStatusChangedEvent)` | void | 至少一次投递，消费方需幂等处理 | Spring ApplicationEvent | 将领域事件包装为 Spring ApplicationEvent，通过 ApplicationEventPublisher 发布，同进程内同步投递 |

### 来自 GitHubIntegration 聚合

#### 仓储实现契约（来自 domain.md 导出契约）

| 接口名 | 方法签名 | 返回类型 | 行为描述 | 实现方案 | 本层用途 |
|--------|---------|---------|---------|---------|---------|
| GitHubIntegrationRepository | `save(GitHubIntegration)` | void | insert-or-update，单用户模式全局一条 | MyBatis-Plus insertOrUpdate | 基于 id 判断 insert/update，OAuthToken 各字段平铺存储到列 |
| GitHubIntegrationRepository | `find()` | Optional\<GitHubIntegration\> | 返回唯一记录，不存在返回 empty | MyBatis-Plus selectOne | 使用 selectOne 查询（LIMIT 1），单用户模式保证至多一条 |

#### 防腐层实现契约（来自 domain.md 导出契约）

| 接口名 | 方法签名 | 返回类型 | 行为描述 | 实现方案 | 本层用途 |
|--------|---------|---------|---------|---------|---------|
| GitHubAclService | `listRepositories(OAuthToken)` | List\<Repository\> | 调用 GitHub API 获取仓库列表，超时 10s | RestTemplate HTTP GET | GET /user/repos，Bearer token 认证，JSON 响应映射为 Repository 值对象列表 |
| GitHubAclService | `listBranches(OAuthToken, String)` | List\<Branch\> | 调用 GitHub API 获取分支列表，超时 10s | RestTemplate HTTP GET | GET /repos/{owner}/{repo}/branches，响应映射为 Branch 值对象列表 |
| GitHubAclService | `createBranch(OAuthToken, String, String, String)` | Branch | 调用 GitHub API 创建分支，超时 10s | RestTemplate HTTP POST | 先 GET /repos/{owner}/{repo}/git/ref/heads/{sourceBranch} 获取 SHA，再 POST /repos/{owner}/{repo}/git/refs 创建 ref |
| GitHubAclService | `mergeBranch(OAuthToken, String, String, String)` | void | 调用 GitHub API 合并分支，冲突抛 GitHubMergeConflictException | RestTemplate HTTP POST | POST /repos/{owner}/{repo}/merges，409 响应映射为 GitHubMergeConflictException |
| GitHubAclService | `exchangeCodeForToken(String)` | OAuthToken | OAuth 授权码换 token，超时 10s | RestTemplate HTTP POST | POST https://github.com/login/oauth/access_token，携带 client_id/client_secret/code |
| GitHubAclService | `getAuthenticatedUsername(OAuthToken)` | String | 获取认证用户名，超时 10s | RestTemplate HTTP GET | GET /user，提取 login 字段 |

## 数据库表设计

### cc_session

```sql
CREATE TABLE cc_session (
    id VARCHAR(36) PRIMARY KEY COMMENT '会话唯一标识，UUID 格式',
    command VARCHAR(2000) NOT NULL COMMENT '执行命令',
    working_directory VARCHAR(500) NOT NULL COMMENT '工作目录路径',
    environment_variables CLOB COMMENT '环境变量 JSON，格式 {"key":"value"}',
    status VARCHAR(20) NOT NULL COMMENT '会话状态: CREATED/RUNNING/COMPLETED/FAILED/TERMINATED',
    exit_code INT COMMENT '进程退出码，仅终态有值',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    started_at TIMESTAMP COMMENT '进程启动时间',
    finished_at TIMESTAMP COMMENT '进程结束时间'
) COMMENT='Claude Code 会话表';

CREATE INDEX idx_cc_session_status ON cc_session(status);
```

### conversation

```sql
CREATE TABLE conversation (
    id VARCHAR(36) PRIMARY KEY COMMENT '对话唯一标识，UUID 格式',
    title VARCHAR(200) NOT NULL COMMENT '对话标题',
    repository_full_name VARCHAR(200) COMMENT '关联的 GitHub 仓库全名 owner/repo',
    cc_session_id VARCHAR(36) COMMENT '关联的 CC 会话 ID',
    status VARCHAR(20) NOT NULL COMMENT '对话状态: CREATED/ACTIVE/COMPLETED/ARCHIVED',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后更新时间'
) COMMENT='对话表';

CREATE INDEX idx_conversation_repository ON conversation(repository_full_name);
CREATE INDEX idx_conversation_status ON conversation(status);
```

### message

```sql
CREATE TABLE message (
    id VARCHAR(36) PRIMARY KEY COMMENT '消息唯一标识，UUID 格式',
    conversation_id VARCHAR(36) NOT NULL COMMENT '所属对话 ID',
    role VARCHAR(20) NOT NULL COMMENT '消息角色: USER/ASSISTANT',
    content CLOB NOT NULL COMMENT '消息内容',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) COMMENT='对话消息表';

CREATE INDEX idx_message_conversation ON message(conversation_id);
```

### dev_task

```sql
CREATE TABLE dev_task (
    id VARCHAR(36) PRIMARY KEY COMMENT '任务唯一标识，UUID 格式',
    conversation_id VARCHAR(36) NOT NULL COMMENT '来源对话 ID',
    repository_full_name VARCHAR(200) NOT NULL COMMENT '关联的 GitHub 仓库全名',
    branch_name VARCHAR(200) NOT NULL COMMENT '功能分支名称',
    requirement CLOB NOT NULL COMMENT '需求描述内容',
    status VARCHAR(30) NOT NULL COMMENT '任务状态: CREATED/THINKING/WORKING/READY_TO_PUBLISH/PUBLISHING/PUBLISHED/FAILED',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后更新时间'
) COMMENT='开发任务表';

CREATE INDEX idx_dev_task_conversation ON dev_task(conversation_id);
CREATE INDEX idx_dev_task_repository ON dev_task(repository_full_name);
CREATE INDEX idx_dev_task_status ON dev_task(status);
```

### task_phase

```sql
CREATE TABLE task_phase (
    id VARCHAR(36) PRIMARY KEY COMMENT '阶段记录唯一标识，UUID 格式',
    dev_task_id VARCHAR(36) NOT NULL COMMENT '所属开发任务 ID',
    phase_type VARCHAR(20) NOT NULL COMMENT '阶段类型: THINKING/WORKING/PUBLISHING',
    cc_session_id VARCHAR(36) COMMENT '关联的 CC 会话 ID',
    output CLOB COMMENT '阶段产出内容',
    started_at TIMESTAMP NOT NULL COMMENT '阶段开始时间',
    finished_at TIMESTAMP COMMENT '阶段结束时间',
    failure_reason CLOB COMMENT '失败原因'
) COMMENT='任务阶段记录表';

CREATE INDEX idx_task_phase_dev_task ON task_phase(dev_task_id);
```

### github_integration

```sql
CREATE TABLE github_integration (
    id VARCHAR(36) PRIMARY KEY COMMENT '集成标识',
    access_token VARCHAR(500) COMMENT 'GitHub OAuth access token',
    token_type VARCHAR(50) COMMENT 'token 类型，通常为 bearer',
    scope VARCHAR(200) COMMENT '授权范围',
    token_created_at TIMESTAMP COMMENT 'token 获取时间',
    authenticated_user VARCHAR(100) COMMENT '已认证的 GitHub 用户名',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后更新时间'
) COMMENT='GitHub 集成配置表';
```

## PO 对象

### CCSessionPO

| PO 字段 | Java 类型 | 数据库列 | 对应领域模型字段 | 转换说明 |
|---------|----------|---------|-----------------|---------|
| id | String | id | CCSession.id | CCSessionId.value 直接映射 |
| command | String | command | CCSession.processConfig.command | 从 ProcessConfig 值对象拆解 |
| workingDirectory | String | working_directory | CCSession.processConfig.workingDirectory | 从 ProcessConfig 值对象拆解 |
| environmentVariables | String | environment_variables | CCSession.processConfig.environmentVariables | Map\<String,String\> 序列化为 JSON 字符串 |
| status | String | status | CCSession.status | CCSessionStatus 枚举 name() 存储 |
| exitCode | Integer | exit_code | CCSession.exitCode | 直接映射，可为 null |
| createdAt | LocalDateTime | created_at | CCSession.createdAt | 直接映射 |
| startedAt | LocalDateTime | started_at | CCSession.startedAt | 直接映射，可为 null |
| finishedAt | LocalDateTime | finished_at | CCSession.finishedAt | 直接映射，可为 null |

### ConversationPO

| PO 字段 | Java 类型 | 数据库列 | 对应领域模型字段 | 转换说明 |
|---------|----------|---------|-----------------|---------|
| id | String | id | Conversation.id | ConversationId.value 直接映射 |
| title | String | title | Conversation.title | 直接映射 |
| repositoryFullName | String | repository_full_name | Conversation.repositoryFullName | 直接映射，可为 null |
| ccSessionId | String | cc_session_id | Conversation.ccSessionId | CCSessionId.value 直接映射，可为 null |
| status | String | status | Conversation.status | ConversationStatus 枚举 name() 存储 |
| createdAt | LocalDateTime | created_at | Conversation.createdAt | 直接映射 |
| updatedAt | LocalDateTime | updated_at | Conversation.updatedAt | 直接映射 |

### MessagePO

| PO 字段 | Java 类型 | 数据库列 | 对应领域模型字段 | 转换说明 |
|---------|----------|---------|-----------------|---------|
| id | String | id | Message.id | MessageId.value 直接映射 |
| conversationId | String | conversation_id | 所属 Conversation.id | ConversationId.value，用于关联查询 |
| role | String | role | Message.role | MessageRole 枚举 name() 存储 |
| content | String | content | Message.content | 直接映射 |
| createdAt | LocalDateTime | created_at | Message.createdAt | 直接映射 |

### DevTaskPO

| PO 字段 | Java 类型 | 数据库列 | 对应领域模型字段 | 转换说明 |
|---------|----------|---------|-----------------|---------|
| id | String | id | DevTask.id | DevTaskId.value 直接映射 |
| conversationId | String | conversation_id | DevTask.conversationId | ConversationId.value 直接映射 |
| repositoryFullName | String | repository_full_name | DevTask.repositoryFullName | 直接映射 |
| branchName | String | branch_name | DevTask.branchName | 直接映射 |
| requirement | String | requirement | DevTask.requirement | 直接映射 |
| status | String | status | DevTask.status | DevTaskStatus 枚举 name() 存储 |
| createdAt | LocalDateTime | created_at | DevTask.createdAt | 直接映射 |
| updatedAt | LocalDateTime | updated_at | DevTask.updatedAt | 直接映射 |

### TaskPhasePO

| PO 字段 | Java 类型 | 数据库列 | 对应领域模型字段 | 转换说明 |
|---------|----------|---------|-----------------|---------|
| id | String | id | TaskPhase.id | TaskPhaseId.value 直接映射 |
| devTaskId | String | dev_task_id | 所属 DevTask.id | DevTaskId.value，用于关联查询 |
| phaseType | String | phase_type | TaskPhase.phaseType | PhaseType 枚举 name() 存储 |
| ccSessionId | String | cc_session_id | TaskPhase.ccSessionId | CCSessionId.value 直接映射，可为 null |
| output | String | output | TaskPhase.output | 直接映射，可为 null |
| startedAt | LocalDateTime | started_at | TaskPhase.startedAt | 直接映射 |
| finishedAt | LocalDateTime | finished_at | TaskPhase.finishedAt | 直接映射，可为 null |
| failureReason | String | failure_reason | TaskPhase.failureReason | 直接映射，可为 null |

### GitHubIntegrationPO

| PO 字段 | Java 类型 | 数据库列 | 对应领域模型字段 | 转换说明 |
|---------|----------|---------|-----------------|---------|
| id | String | id | GitHubIntegration.id | GitHubIntegrationId.value 直接映射 |
| accessToken | String | access_token | GitHubIntegration.oauthToken.accessToken | 从 OAuthToken 值对象拆解，可为 null |
| tokenType | String | token_type | GitHubIntegration.oauthToken.tokenType | 从 OAuthToken 值对象拆解，可为 null |
| scope | String | scope | GitHubIntegration.oauthToken.scope | 从 OAuthToken 值对象拆解，可为 null |
| tokenCreatedAt | LocalDateTime | token_created_at | GitHubIntegration.oauthToken.createdAt | 从 OAuthToken 值对象拆解，可为 null |
| authenticatedUser | String | authenticated_user | GitHubIntegration.authenticatedUser | 直接映射，可为 null |
| createdAt | LocalDateTime | created_at | GitHubIntegration.createdAt | 直接映射 |
| updatedAt | LocalDateTime | updated_at | GitHubIntegration.updatedAt | 直接映射 |

## Mapper

### CCSessionMapper

**继承**：`BaseMapper<CCSessionPO>`

**自定义方法**：

```java
@Mapper
public interface CCSessionMapper extends BaseMapper<CCSessionPO> {

    /**
     * 查找所有活跃状态（CREATED/RUNNING）的会话。
     */
    @Select("SELECT * FROM cc_session WHERE status IN ('CREATED', 'RUNNING') ORDER BY created_at DESC")
    List<CCSessionPO> selectActiveSessions();
}
```

### ConversationMapper

**继承**：`BaseMapper<ConversationPO>`

**自定义方法**：无。所有查询通过 MyBatis-Plus LambdaQueryWrapper 实现。

```java
@Mapper
public interface ConversationMapper extends BaseMapper<ConversationPO> {
}
```

### MessageMapper

**继承**：`BaseMapper<MessagePO>`

**自定义方法**：

```java
@Mapper
public interface MessageMapper extends BaseMapper<MessagePO> {

    /**
     * 根据对话 ID 查找所有消息，按创建时间升序。
     */
    @Select("SELECT * FROM message WHERE conversation_id = #{conversationId} ORDER BY created_at ASC")
    List<MessagePO> selectByConversationId(@Param("conversationId") String conversationId);

    /**
     * 根据对话 ID 删除所有消息（用于先删后插策略）。
     */
    @Delete("DELETE FROM message WHERE conversation_id = #{conversationId}")
    int deleteByConversationId(@Param("conversationId") String conversationId);
}
```

### DevTaskMapper

**继承**：`BaseMapper<DevTaskPO>`

**自定义方法**：无。所有查询通过 MyBatis-Plus LambdaQueryWrapper 实现。

```java
@Mapper
public interface DevTaskMapper extends BaseMapper<DevTaskPO> {
}
```

### TaskPhaseMapper

**继承**：`BaseMapper<TaskPhasePO>`

**自定义方法**：

```java
@Mapper
public interface TaskPhaseMapper extends BaseMapper<TaskPhasePO> {

    /**
     * 根据开发任务 ID 查找所有阶段记录，按开始时间升序。
     */
    @Select("SELECT * FROM task_phase WHERE dev_task_id = #{devTaskId} ORDER BY started_at ASC")
    List<TaskPhasePO> selectByDevTaskId(@Param("devTaskId") String devTaskId);

    /**
     * 根据开发任务 ID 删除所有阶段记录（用于先删后插策略）。
     */
    @Delete("DELETE FROM task_phase WHERE dev_task_id = #{devTaskId}")
    int deleteByDevTaskId(@Param("devTaskId") String devTaskId);
}
```

### GitHubIntegrationMapper

**继承**：`BaseMapper<GitHubIntegrationPO>`

**自定义方法**：无。

```java
@Mapper
public interface GitHubIntegrationMapper extends BaseMapper<GitHubIntegrationPO> {
}
```

## 仓储实现

### CCSessionRepositoryImpl

**实现接口**：`CCSessionRepository`
**注入依赖**：`CCSessionMapper`

#### save(CCSession)
- 根据 CCSessionMapper.selectById(id) 判断记录是否存在
- 存在 → update，不存在 → insert
- Domain → PO 转换：
  - id ← session.getId().getValue()
  - command ← session.getProcessConfig().getCommand()
  - workingDirectory ← session.getProcessConfig().getWorkingDirectory()
  - environmentVariables ← Jackson ObjectMapper 将 Map 序列化为 JSON 字符串
  - status ← session.getStatus().name()
  - exitCode ← session.getExitCode()（可为 null）
  - createdAt / startedAt / finishedAt ← 直接映射

#### findById(CCSessionId)
- 调用 CCSessionMapper.selectById(id.getValue())
- PO 为 null → 返回 Optional.empty()
- PO → Domain 转换：
  - ProcessConfig ← new ProcessConfig(command, workingDirectory, Jackson 反序列化 environmentVariables)
  - CCSessionStatus ← CCSessionStatus.valueOf(status)
  - 通过反射或包级私有构造器重建 CCSession 聚合根
- 返回 Optional.of(ccSession)

#### findActiveSessions()
- 调用 CCSessionMapper.selectActiveSessions()
- 遍历结果集，逐条执行 PO → Domain 转换
- 返回 CCSession 列表

### ConversationRepositoryImpl

**实现接口**：`ConversationRepository`
**注入依赖**：`ConversationMapper`、`MessageMapper`

#### save(Conversation)
- 根据 ConversationMapper.selectById(id) 判断记录是否存在
- 主表：存在 → update，不存在 → insert
- Domain → PO 转换（ConversationPO）：
  - id ← conversation.getId().getValue()
  - title ← conversation.getTitle()
  - repositoryFullName ← conversation.getRepositoryFullName()（可为 null）
  - ccSessionId ← conversation.getCcSessionId() != null ? ccSessionId.getValue() : null
  - status ← conversation.getStatus().name()
  - createdAt / updatedAt ← 直接映射
- 消息级联保存（先删后插）：
  - MessageMapper.deleteByConversationId(conversationId)
  - 遍历 conversation.getMessages()，逐条转换为 MessagePO 并 insert
  - MessagePO 转换：id ← messageId.getValue(), conversationId ← 父对话 ID, role ← role.name(), content, createdAt

#### findById(ConversationId)
- 调用 ConversationMapper.selectById(id.getValue())
- PO 为 null → 返回 Optional.empty()
- 调用 MessageMapper.selectByConversationId(id) 获取消息列表
- PO → Domain 转换：
  - ConversationId ← new ConversationId(po.id)
  - ConversationStatus ← ConversationStatus.valueOf(po.status)
  - CCSessionId ← po.ccSessionId != null ? new CCSessionId(po.ccSessionId) : null
  - Messages ← 逐条将 MessagePO 转换为 Message 实体
- 通过包级私有构造器重建 Conversation 聚合根
- 返回 Optional.of(conversation)

#### findByRepositoryFullName(String)
- 使用 LambdaQueryWrapper 按 repositoryFullName 查询，orderByDesc(createdAt)
- 遍历结果，逐个调用 MessageMapper.selectByConversationId 加载消息
- 组装完整聚合列表返回

#### findAll()
- 使用 LambdaQueryWrapper 全表查询，orderByDesc(updatedAt)
- 遍历结果，逐个加载消息并组装聚合
- 返回完整列表

### DevTaskRepositoryImpl

**实现接口**：`DevTaskRepository`
**注入依赖**：`DevTaskMapper`、`TaskPhaseMapper`

#### save(DevTask)
- 根据 DevTaskMapper.selectById(id) 判断记录是否存在
- 主表：存在 → update，不存在 → insert
- Domain → PO 转换（DevTaskPO）：
  - id ← task.getId().getValue()
  - conversationId ← task.getConversationId().getValue()
  - repositoryFullName ← task.getRepositoryFullName()
  - branchName ← task.getBranchName()
  - requirement ← task.getRequirement()
  - status ← task.getStatus().name()
  - createdAt / updatedAt ← 直接映射
- 阶段级联保存（先删后插）：
  - TaskPhaseMapper.deleteByDevTaskId(taskId)
  - 遍历 task.getPhases()，逐条转换为 TaskPhasePO 并 insert
  - TaskPhasePO 转换：id ← phaseId.getValue(), devTaskId ← 父任务 ID, phaseType ← phaseType.name(), ccSessionId ← 可选, output / startedAt / finishedAt / failureReason ← 直接映射

#### findById(DevTaskId)
- 调用 DevTaskMapper.selectById(id.getValue())
- PO 为 null → 返回 Optional.empty()
- 调用 TaskPhaseMapper.selectByDevTaskId(id) 获取阶段列表
- PO → Domain 转换：
  - DevTaskId ← new DevTaskId(po.id)
  - ConversationId ← new ConversationId(po.conversationId)
  - DevTaskStatus ← DevTaskStatus.valueOf(po.status)
  - Phases ← 逐条将 TaskPhasePO 转换为 TaskPhase 实体
- 通过包级私有构造器重建 DevTask 聚合根
- 返回 Optional.of(devTask)

#### findByConversationId(ConversationId)
- 使用 LambdaQueryWrapper 按 conversationId 查询，orderByDesc(createdAt)
- 遍历结果，逐个加载 phases 并组装聚合
- 返回列表

#### findByRepositoryFullName(String)
- 使用 LambdaQueryWrapper 按 repositoryFullName 查询，orderByDesc(createdAt)
- 遍历结果，逐个加载 phases 并组装聚合
- 返回列表

### GitHubIntegrationRepositoryImpl

**实现接口**：`GitHubIntegrationRepository`
**注入依赖**：`GitHubIntegrationMapper`

#### save(GitHubIntegration)
- 根据 GitHubIntegrationMapper.selectById(id) 判断记录是否存在
- 存在 → update，不存在 → insert
- Domain → PO 转换：
  - id ← integration.getId().getValue()
  - OAuthToken 字段平铺：accessToken / tokenType / scope / tokenCreatedAt ← 从 oauthToken 值对象拆解（oauthToken 为 null 时各字段均为 null）
  - authenticatedUser ← integration.getAuthenticatedUser()
  - createdAt / updatedAt ← 直接映射

#### find()
- 调用 GitHubIntegrationMapper.selectOne(new LambdaQueryWrapper<>().last("LIMIT 1"))
- PO 为 null → 返回 Optional.empty()
- PO → Domain 转换：
  - GitHubIntegrationId ← new GitHubIntegrationId(po.id)
  - OAuthToken ← po.accessToken != null ? new OAuthToken(accessToken, tokenType, scope, tokenCreatedAt) : null
  - authenticatedUser ← po.authenticatedUser
- 通过包级私有构造器重建 GitHubIntegration 聚合根
- 返回 Optional.of(integration)

## 外部集成

### ClaudeCodeCliProcessManager

**职责**：封装 Claude Code CLI 子进程的创建、输入输出管理和生命周期控制。通过 ProcessBuilder 启动 CLI 进程，通过 WebSocket 实时推送输出流。

**接口**：
```java
public interface ClaudeCodeCliProcessManager {

    /**
     * 启动 Claude Code CLI 子进程。
     * 根据 ProcessConfig 创建进程并开始执行。
     * 返回进程句柄用于后续操作。
     */
    CCProcessHandle start(CCSessionId sessionId, ProcessConfig config);

    /**
     * 向指定会话进程发送输入。
     * 写入进程的 stdin。
     */
    void sendInput(CCSessionId sessionId, String input);

    /**
     * 终止指定会话的进程。
     * 先尝试 destroy()，超时后 destroyForcibly()。
     */
    void terminate(CCSessionId sessionId);

    /**
     * 检查指定会话的进程是否仍在运行。
     */
    boolean isRunning(CCSessionId sessionId);
}
```

**实现要点（ClaudeCodeCliProcessManagerImpl）**：
- 使用 `ProcessBuilder` 构造子进程，设置 command、directory、environment
- 将 processConfig.command 拆分为命令参数列表传入 ProcessBuilder
- 设置 `redirectErrorStream(true)` 合并 stdout 和 stderr
- 维护 `ConcurrentHashMap<String, Process>` 管理活跃进程
- 启动独立线程读取进程 OutputStream，逐行解析后通过 WebSocketMessageBroker 推送
- terminate 时先 `process.destroy()`，等待 5 秒超时后 `process.destroyForcibly()`
- 进程结束时通过回调通知上层（exitCode 获取）

**配置**：
- 进程终止超时：5000ms
- 输出缓冲区：8192 bytes
- 最大并发进程数：10

### CCProcessHandle

**职责**：封装进程句柄信息，供上层获取进程状态。

```java
public class CCProcessHandle {
    private final CCSessionId sessionId;
    private final CompletableFuture<Integer> exitCodeFuture;

    /**
     * 异步获取进程退出码。
     * 进程结束后 CompletableFuture 完成。
     */
    public CompletableFuture<Integer> getExitCodeFuture();
}
```

### GitHubApiClient

**职责**：封装 GitHub REST API 的 HTTP 调用细节，作为 GitHubAclService 的实现。通过 RestTemplate 发起请求，统一处理认证头、错误响应和数据映射。

**实现类**：`GitHubAclServiceImpl implements GitHubAclService`

**配置**：
- baseUrl：`https://api.github.com`
- oauthBaseUrl：`https://github.com`
- connectTimeout：5000ms
- readTimeout：10000ms
- 认证方式：Bearer token（Header: `Authorization: Bearer {accessToken}`）
- Accept Header：`application/vnd.github.v3+json`

**方法实现**：

| 方法 | HTTP 调用 | 错误处理 |
|------|----------|---------|
| listRepositories | GET /user/repos?sort=updated&per_page=100 | 401 → GitHubApiException("Unauthorized")，其他 → GitHubApiException |
| listBranches | GET /repos/{owner}/{repo}/branches?per_page=100 | 404 → GitHubApiException("Repository not found")，其他 → GitHubApiException |
| createBranch | GET /repos/{owner}/{repo}/git/ref/heads/{source} 获取 SHA，POST /repos/{owner}/{repo}/git/refs body={"ref":"refs/heads/{name}","sha":"{sha}"} | 422 → GitHubApiException("Branch already exists")，其他 → GitHubApiException |
| mergeBranch | POST /repos/{owner}/{repo}/merges body={"base":"{base}","head":"{head}"} | 409 → GitHubMergeConflictException，404 → GitHubApiException，其他 → GitHubApiException |
| exchangeCodeForToken | POST https://github.com/login/oauth/access_token body=client_id+client_secret+code, Accept: application/json | 响应中 error 字段不为空 → GitHubApiException |
| getAuthenticatedUsername | GET /user | 401 → GitHubApiException("Unauthorized")，提取 response.login |

**GitHub OAuth 配置项**（application.yml）：

```yaml
github:
  oauth:
    client-id: ${GITHUB_CLIENT_ID}
    client-secret: ${GITHUB_CLIENT_SECRET}
    redirect-uri: ${GITHUB_REDIRECT_URI:http://localhost:8080/api/github/callback}
  api:
    base-url: https://api.github.com
    connect-timeout: 5000
    read-timeout: 10000
```

### WebSocketMessageBroker

**职责**：封装 WebSocket 消息推送能力，为 CC 进程输出流和任务状态变更提供实时通信通道。

**实现**：基于 Spring WebSocket（STOMP 协议 + SockJS 降级）。

```java
@Component
public class WebSocketMessageBroker {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 推送 CC 会话输出流消息。
     * 目标: /topic/cc-session/{sessionId}/output
     */
    public void sendCCSessionOutput(String sessionId, String outputLine);

    /**
     * 推送任务状态变更消息。
     * 目标: /topic/dev-task/{taskId}/status
     */
    public void sendDevTaskStatusUpdate(String taskId, String status);

    /**
     * 推送对话消息。
     * 目标: /topic/conversation/{conversationId}/message
     */
    public void sendConversationMessage(String conversationId, Object message);
}
```

**WebSocket 配置**：

```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}
```

**Topic 订阅规划**：

| Topic | 消息内容 | 触发场景 |
|-------|---------|---------|
| /topic/cc-session/{sessionId}/output | CC 进程的 stdout 输出行 | ClaudeCodeCliProcessManager 读取进程输出时 |
| /topic/dev-task/{taskId}/status | 任务状态变更（含阶段信息） | DevTaskEventPublisher 发布状态变更事件时 |
| /topic/conversation/{conversationId}/message | 新增消息（含角色和内容） | 对话添加消息后 |

## 事件发布实现

### CCSessionEventPublisherImpl

**实现接口**：`CCSessionEventPublisher`
**注入依赖**：`ApplicationEventPublisher`

**实现要点**：
- 将 CCSessionStatusChangedEvent 包装为 Spring ApplicationEvent 子类 `CCSessionStatusChangedApplicationEvent`
- 通过 `applicationEventPublisher.publishEvent()` 同步发布
- 同进程内同步投递，事务内执行，保证与数据库操作的一致性
- 消费方通过 `@EventListener` 或 `@TransactionalEventListener` 监听

```java
@Component
public class CCSessionEventPublisherImpl implements CCSessionEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void publishStatusChanged(CCSessionStatusChangedEvent event) {
        applicationEventPublisher.publishEvent(
            new CCSessionStatusChangedApplicationEvent(this, event));
    }
}
```

**Spring ApplicationEvent 包装类**：

```java
public class CCSessionStatusChangedApplicationEvent extends ApplicationEvent {
    private final CCSessionStatusChangedEvent domainEvent;

    public CCSessionStatusChangedApplicationEvent(Object source,
                                                   CCSessionStatusChangedEvent domainEvent) {
        super(source);
        this.domainEvent = domainEvent;
    }

    public CCSessionStatusChangedEvent getDomainEvent() {
        return domainEvent;
    }
}
```

### DevTaskEventPublisherImpl

**实现接口**：`DevTaskEventPublisher`
**注入依赖**：`ApplicationEventPublisher`、`WebSocketMessageBroker`

**实现要点**：
- 将 DevTaskStatusChangedEvent 包装为 Spring ApplicationEvent 子类 `DevTaskStatusChangedApplicationEvent`
- 通过 `applicationEventPublisher.publishEvent()` 同步发布供 Application 层监听
- 同时通过 `WebSocketMessageBroker.sendDevTaskStatusUpdate()` 向前端推送状态变更

```java
@Component
public class DevTaskEventPublisherImpl implements DevTaskEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;
    private final WebSocketMessageBroker webSocketMessageBroker;

    @Override
    public void publishStatusChanged(DevTaskStatusChangedEvent event) {
        applicationEventPublisher.publishEvent(
            new DevTaskStatusChangedApplicationEvent(this, event));
        webSocketMessageBroker.sendDevTaskStatusUpdate(
            event.getTaskId().getValue(),
            event.getCurrentStatus().name());
    }
}
```

**Spring ApplicationEvent 包装类**：

```java
public class DevTaskStatusChangedApplicationEvent extends ApplicationEvent {
    private final DevTaskStatusChangedEvent domainEvent;

    public DevTaskStatusChangedApplicationEvent(Object source,
                                                 DevTaskStatusChangedEvent domainEvent) {
        super(source);
        this.domainEvent = domainEvent;
    }

    public DevTaskStatusChangedEvent getDomainEvent() {
        return domainEvent;
    }
}
```

## 实现清单

| # | output_id | 实现项 | 类型 | 说明 |
|---|-----------|--------|------|------|
| 1 | Output_Infr_ThoughtworksAgentsDevPlatform_01 | CCSessionPO | 新增 | PO 对象，cc_session 表映射，ProcessConfig 环境变量序列化为 JSON |
| 2 | Output_Infr_ThoughtworksAgentsDevPlatform_02 | CCSessionMapper | 新增 | MyBatis-Plus Mapper，继承 BaseMapper，含 selectActiveSessions 自定义方法 |
| 3 | Output_Infr_ThoughtworksAgentsDevPlatform_03 | CCSessionRepositoryImpl | 新增 | CCSessionRepository 仓储实现，save 支持 insert-or-update，findById 完整加载 ProcessConfig |
| 4 | Output_Infr_ThoughtworksAgentsDevPlatform_04 | ConversationPO | 新增 | PO 对象，conversation 表映射 |
| 5 | Output_Infr_ThoughtworksAgentsDevPlatform_05 | MessagePO | 新增 | PO 对象，message 表映射，通过 conversation_id 关联 |
| 6 | Output_Infr_ThoughtworksAgentsDevPlatform_06 | ConversationMapper | 新增 | MyBatis-Plus Mapper，继承 BaseMapper |
| 7 | Output_Infr_ThoughtworksAgentsDevPlatform_07 | MessageMapper | 新增 | MyBatis-Plus Mapper，含 selectByConversationId 和 deleteByConversationId 自定义方法 |
| 8 | Output_Infr_ThoughtworksAgentsDevPlatform_08 | ConversationRepositoryImpl | 新增 | ConversationRepository 仓储实现，级联保存 messages（先删后插），查询时完整加载消息列表 |
| 9 | Output_Infr_ThoughtworksAgentsDevPlatform_09 | DevTaskPO | 新增 | PO 对象，dev_task 表映射 |
| 10 | Output_Infr_ThoughtworksAgentsDevPlatform_10 | TaskPhasePO | 新增 | PO 对象，task_phase 表映射，通过 dev_task_id 关联 |
| 11 | Output_Infr_ThoughtworksAgentsDevPlatform_11 | DevTaskMapper | 新增 | MyBatis-Plus Mapper，继承 BaseMapper |
| 12 | Output_Infr_ThoughtworksAgentsDevPlatform_12 | TaskPhaseMapper | 新增 | MyBatis-Plus Mapper，含 selectByDevTaskId 和 deleteByDevTaskId 自定义方法 |
| 13 | Output_Infr_ThoughtworksAgentsDevPlatform_13 | DevTaskRepositoryImpl | 新增 | DevTaskRepository 仓储实现，级联保存 phases（先删后插），查询时完整加载阶段列表 |
| 14 | Output_Infr_ThoughtworksAgentsDevPlatform_14 | GitHubIntegrationPO | 新增 | PO 对象，github_integration 表映射，OAuthToken 平铺为列 |
| 15 | Output_Infr_ThoughtworksAgentsDevPlatform_15 | GitHubIntegrationMapper | 新增 | MyBatis-Plus Mapper，继承 BaseMapper |
| 16 | Output_Infr_ThoughtworksAgentsDevPlatform_16 | GitHubIntegrationRepositoryImpl | 新增 | GitHubIntegrationRepository 仓储实现，单用户模式 LIMIT 1 查询 |
| 17 | Output_Infr_ThoughtworksAgentsDevPlatform_17 | ClaudeCodeCliProcessManager | 新增 | 接口，定义 CC CLI 子进程管理方法（start/sendInput/terminate/isRunning） |
| 18 | Output_Infr_ThoughtworksAgentsDevPlatform_18 | ClaudeCodeCliProcessManagerImpl | 新增 | ProcessBuilder 封装实现，ConcurrentHashMap 管理活跃进程，独立线程读取输出流并推送 WebSocket |
| 19 | Output_Infr_ThoughtworksAgentsDevPlatform_19 | CCProcessHandle | 新增 | 进程句柄，封装 sessionId 和 CompletableFuture exitCode |
| 20 | Output_Infr_ThoughtworksAgentsDevPlatform_20 | GitHubAclServiceImpl | 新增 | GitHubAclService 防腐层实现，RestTemplate 调用 GitHub REST API，统一 Bearer token 认证和错误处理 |
| 21 | Output_Infr_ThoughtworksAgentsDevPlatform_21 | WebSocketMessageBroker | 新增 | WebSocket 消息推送组件，封装 SimpMessagingTemplate，提供 CC 输出流/任务状态/对话消息推送 |
| 22 | Output_Infr_ThoughtworksAgentsDevPlatform_22 | WebSocketConfig | 新增 | WebSocket 配置类，STOMP + SockJS，配置 /topic 订阅和 /ws 端点 |
| 23 | Output_Infr_ThoughtworksAgentsDevPlatform_23 | CCSessionEventPublisherImpl | 新增 | CCSessionEventPublisher 实现，将领域事件包装为 Spring ApplicationEvent 同步发布 |
| 24 | Output_Infr_ThoughtworksAgentsDevPlatform_24 | CCSessionStatusChangedApplicationEvent | 新增 | Spring ApplicationEvent 包装类，封装 CCSessionStatusChangedEvent 领域事件 |
| 25 | Output_Infr_ThoughtworksAgentsDevPlatform_25 | DevTaskEventPublisherImpl | 新增 | DevTaskEventPublisher 实现，Spring ApplicationEvent 发布 + WebSocket 推送双通道 |
| 26 | Output_Infr_ThoughtworksAgentsDevPlatform_26 | DevTaskStatusChangedApplicationEvent | 新增 | Spring ApplicationEvent 包装类，封装 DevTaskStatusChangedEvent 领域事件 |
| 27 | Output_Infr_ThoughtworksAgentsDevPlatform_27 | schema.sql | 新增 | H2 数据库建表脚本，包含 cc_session/conversation/message/dev_task/task_phase/github_integration 六张表 |
| 28 | Output_Infr_ThoughtworksAgentsDevPlatform_28 | GitHubOAuthProperties | 新增 | GitHub OAuth 配置属性类，绑定 github.oauth 前缀（clientId/clientSecret/redirectUri） |

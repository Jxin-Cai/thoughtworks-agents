# Thoughtworks Agents 开发平台 — 需求文档

## 项目概述

构建一个 Web 界面的 AI 开发平台，将 Claude Code CLI 的命令行交互包装成可视化 Web 界面。用户可以通过对话澄清需求，然后由 Claude Code 自动完成从设计到编码的全流程开发。

## 技术栈

- 后端: Java Spring Boot + H2 (内嵌数据库) + WebSocket
- 前端: Vue 3 + Vite + TypeScript (后续阶段)
- 集成: Claude Code CLI (子进程调用)、GitHub OAuth、GitHub API

## 核心功能

### 1. 对话管理

用户通过 Web 对话框与 Claude Code 交互，澄清需求。

- 用户可以选择一个 GitHub 仓库
- 可以创建新对话或选择历史对话
- 对话通过后端创建 Claude Code CLI 子进程 (`claude -p '...'`) 完成
- 对话消息通过 WebSocket 实时推送到前端
- 每个对话对应一个 Claude Code 会话

### 2. 任务管理（开发流程）

当需求确认后，用户点击"开始开发"，系统创建一个开发任务，经历三个阶段：

#### 2.1 思考阶段 (Thinking)
- 创建新的 CC 会话，基于需求信息生成设计方案
- 产出设计方案文件，用户可在面板上查看
- 完成后自动进入工作阶段

#### 2.2 工作阶段 (Working)
- CC 开始写代码
- 可以查看正在实现代码的 subagent 日志
- 代码在功能分支上开发
- 完成后意味着代码分支就绪

#### 2.3 发布阶段 (Publishing)
- 需要用户在页面手动点击"发布"
- 点击后自动通过 GitHub API 合并分支到 main
- 合并成功即发布完成

### 3. GitHub 集成

- 通过 GitHub OAuth 获取用户的 access token
- 使用 token 拉取/管理代码仓库
- 支持分支创建、合并操作
- Claude Code 通过 GitHub MCP 集成进行代码操作

### 4. Claude Code 进程管理

- 后端通过子进程方式调用 Claude Code CLI
- 支持多 CC 会话并行运行（多个需求可同时开发）
- 实时捕获 CC 输出流，通过 WebSocket 推送到前端
- 会话生命周期管理（创建、运行中、完成、异常）

## 用户体系

- 单用户模式，不需要登录系统
- GitHub OAuth 仅用于获取仓库访问权限

## 数据持久化

- 使用 H2 内嵌数据库
- 持久化内容：对话记录、消息历史、任务状态、GitHub token

## 实时通信

- 使用 WebSocket 实现前后端实时通信
- CC 的流式输出实时推送到前端展示

## 聚合分析

### 识别的聚合

#### 1. Conversation（对话聚合）
- **聚合根**: Conversation
- **核心实体**: Message
- **职责**: 管理对话生命周期和消息记录
- **状态**: CREATED → ACTIVE → COMPLETED / ARCHIVED
- **依赖**: 依赖 CCSession 完成实际对话

#### 2. DevTask（开发任务聚合）
- **聚合根**: DevTask
- **核心实体**: TaskPhase (思考/工作/发布阶段的执行记录)
- **职责**: 管理开发任务的全生命周期和三阶段流转
- **状态**: CREATED → THINKING → WORKING → READY_TO_PUBLISH → PUBLISHING → PUBLISHED / FAILED
- **依赖**: 依赖 CCSession 执行思考和工作阶段

#### 3. CCSession（Claude Code 会话聚合）
- **聚合根**: CCSession
- **职责**: 管理 Claude Code CLI 子进程的生命周期
- **状态**: CREATED → RUNNING → COMPLETED / FAILED / TERMINATED
- **核心行为**: 启动进程、发送输入、接收输出流、终止进程
- **值对象**: ProcessConfig (命令、工作目录、环境变量)

#### 4. GitHubIntegration（GitHub 集成聚合）
- **聚合根**: GitHubIntegration
- **职责**: 管理 GitHub OAuth token 和仓库操作
- **核心行为**: OAuth 认证流程、仓库列表获取、分支合并
- **值对象**: OAuthToken, Repository, Branch

### 聚合间依赖关系

```
Conversation ──使用──→ CCSession（对话通过 CC 会话完成）
DevTask ──使用──→ CCSession（开发阶段通过 CC 会话执行）
DevTask ──使用──→ GitHubIntegration（发布阶段合并分支）
```

### 建议实现顺序

1. CCSession（基础能力，被其他聚合依赖）
2. Conversation（核心对话功能）
3. DevTask（开发任务管理）
4. GitHubIntegration（外部集成）

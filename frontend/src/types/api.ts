/* ===== Generic API Wrapper ===== */
export interface Result<T> {
  code: number
  message: string
  data: T
}

/* ===== Conversation ===== */
export type ConversationStatus = 'CREATED' | 'ACTIVE' | 'COMPLETED' | 'ARCHIVED'

export interface ConversationDTO {
  id: string
  title: string
  repositoryFullName: string | null
  status: ConversationStatus
  messages: MessageDTO[]
  createdAt: string
  updatedAt: string
}

export interface MessageDTO {
  id: string
  role: 'USER' | 'ASSISTANT'
  content: string
  createdAt: string
}

/* ===== DevTask ===== */
export type DevTaskStatus =
  | 'CREATED'
  | 'THINKING'
  | 'WORKING'
  | 'READY_TO_PUBLISH'
  | 'PUBLISHING'
  | 'PUBLISHED'
  | 'FAILED'

export type PhaseType = 'THINKING' | 'WORKING' | 'PUBLISHING'

export interface DevTaskDTO {
  id: string
  conversationId: string
  repositoryFullName: string
  branchName: string
  requirement: string
  status: DevTaskStatus
  phases: TaskPhaseDTO[]
  createdAt: string
  updatedAt: string
}

export interface TaskPhaseDTO {
  id: string
  phaseType: PhaseType
  ccSessionId: string | null
  output: string | null
  startedAt: string
  finishedAt: string | null
  failureReason: string | null
}

/* ===== CCSession ===== */
export interface CCSessionDTO {
  id: string
  status: string
  command: string
  workingDirectory: string
  createdAt: string
  startedAt: string | null
  finishedAt: string | null
  exitCode: number | null
}

/* ===== GitHub ===== */
export interface RepositoryDTO {
  fullName: string
  defaultBranch: string
  cloneUrl: string
  isPrivate: boolean
}

export interface MergeResultDTO {
  repositoryFullName: string
  headBranch: string
  baseBranch: string
  success: boolean
}

/* ===== WebSocket Messages ===== */
export type CCOutputType = 'STDOUT' | 'STDERR' | 'SYSTEM'

export interface CCOutputMessage {
  sessionId: string
  content: string
  timestamp: string
  type: CCOutputType
}

export interface DevTaskStatusMessage {
  taskId: string
  previousStatus: DevTaskStatus
  currentStatus: DevTaskStatus
  timestamp: string
}

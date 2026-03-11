/* ===== Conversation Requests ===== */
export interface CreateConversationRequest {
  title: string
  repositoryFullName?: string
}

export interface SendMessageRequest {
  content: string
  workingDirectory: string
  environmentVariables: Record<string, string>
}

/* ===== DevTask Requests ===== */
export interface CreateDevTaskRequest {
  conversationId: string
  repositoryFullName: string
  branchName: string
  requirement: string
}

export interface StartDevelopmentRequest {
  workingDirectory: string
  environmentVariables: Record<string, string>
}

export interface AdvanceToWorkingRequest {
  designOutput: string
  workingDirectory: string
  environmentVariables: Record<string, string>
}

export interface ExecutePublishRequest {
  baseBranch: string
}

/* ===== GitHub Requests ===== */
export interface MergeBranchRequest {
  repositoryFullName: string
  headBranch: string
  baseBranch: string
}

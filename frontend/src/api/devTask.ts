import client from './client'
import type { DevTaskDTO } from '@/types/api'
import type {
  CreateDevTaskRequest,
  StartDevelopmentRequest,
  AdvanceToWorkingRequest,
  ExecutePublishRequest,
} from '@/types/request'

export function createDevTask(data: CreateDevTaskRequest): Promise<DevTaskDTO> {
  return client.post('/dev-tasks', data)
}

export function startDevelopment(taskId: string, data: StartDevelopmentRequest): Promise<DevTaskDTO> {
  return client.post(`/dev-tasks/${taskId}/start`, data)
}

export function advanceToWorking(taskId: string, data: AdvanceToWorkingRequest): Promise<DevTaskDTO> {
  return client.post(`/dev-tasks/${taskId}/advance`, data)
}

export function executePublish(taskId: string, data: ExecutePublishRequest): Promise<DevTaskDTO> {
  return client.post(`/dev-tasks/${taskId}/publish`, data)
}

export function getDevTask(id: string): Promise<DevTaskDTO> {
  return client.get(`/dev-tasks/${id}`)
}

export function getDevTasks(conversationId?: string): Promise<DevTaskDTO[]> {
  const params = conversationId ? { conversationId } : {}
  return client.get('/dev-tasks', { params })
}

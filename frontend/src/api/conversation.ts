import client from './client'
import type { ConversationDTO } from '@/types/api'
import type { CreateConversationRequest, SendMessageRequest } from '@/types/request'

export function createConversation(data: CreateConversationRequest): Promise<ConversationDTO> {
  return client.post('/conversations', data)
}

export function sendMessage(conversationId: string, data: SendMessageRequest): Promise<ConversationDTO> {
  return client.post(`/conversations/${conversationId}/messages`, data)
}

export function getConversations(repositoryFullName?: string): Promise<ConversationDTO[]> {
  const params = repositoryFullName ? { repositoryFullName } : {}
  return client.get('/conversations', { params })
}

export function getConversation(id: string): Promise<ConversationDTO> {
  return client.get(`/conversations/${id}`)
}

export function archiveConversation(id: string): Promise<void> {
  return client.put(`/conversations/${id}/archive`)
}

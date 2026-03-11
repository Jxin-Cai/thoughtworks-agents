import client from './client'
import type { CCSessionDTO } from '@/types/api'

export function getCCSession(id: string): Promise<CCSessionDTO> {
  return client.get(`/cc-sessions/${id}`)
}

export function getActiveSessions(): Promise<CCSessionDTO[]> {
  return client.get('/cc-sessions/active')
}

export function terminateSession(id: string): Promise<void> {
  return client.post(`/cc-sessions/${id}/terminate`)
}

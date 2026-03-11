import client from './client'
import type { RepositoryDTO, MergeResultDTO } from '@/types/api'
import type { MergeBranchRequest } from '@/types/request'

export function handleOAuthCallback(code: string): Promise<void> {
  return client.get('/github/oauth/callback', { params: { code } })
}

export function getRepositories(): Promise<RepositoryDTO[]> {
  return client.get('/github/repositories')
}

export function mergeBranch(data: MergeBranchRequest): Promise<MergeResultDTO> {
  return client.post('/github/merge', data)
}

import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { RepositoryDTO } from '@/types/api'
import * as githubApi from '@/api/github'

export const useGitHubStore = defineStore('github', () => {
  const authenticated = ref(false)
  const repositories = ref<RepositoryDTO[]>([])
  const loading = ref(false)

  async function handleCallback(code: string) {
    loading.value = true
    try {
      await githubApi.handleOAuthCallback(code)
      authenticated.value = true
    } finally {
      loading.value = false
    }
  }

  async function loadRepositories() {
    loading.value = true
    try {
      repositories.value = await githubApi.getRepositories()
      authenticated.value = true
    } catch (e) {
      authenticated.value = false
      throw e
    } finally {
      loading.value = false
    }
  }

  async function checkAuthStatus() {
    try {
      await githubApi.getRepositories()
      authenticated.value = true
    } catch {
      authenticated.value = false
    }
  }

  return {
    authenticated,
    repositories,
    loading,
    handleCallback,
    loadRepositories,
    checkAuthStatus,
  }
})

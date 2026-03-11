<template>
  <div class="repo-select-page">
    <div class="page-content">
      <div class="page-header">
        <h1 class="page-title">选择仓库</h1>
        <p class="page-desc">选择一个仓库开始 AI 辅助开发</p>
      </div>

      <RepositoryGrid
        :repositories="githubStore.repositories"
        :loading="githubStore.loading"
        @select="handleSelect"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useGitHubStore } from '@/stores/github'
import { useConversationStore } from '@/stores/conversation'
import type { RepositoryDTO } from '@/types/api'
import RepositoryGrid from '@/components/github/RepositoryGrid.vue'

const router = useRouter()
const githubStore = useGitHubStore()
const conversationStore = useConversationStore()

onMounted(async () => {
  try {
    await githubStore.loadRepositories()
  } catch {
    ElMessage.warning('请先授权 GitHub')
    router.push('/auth')
  }
})

async function handleSelect(repo: RepositoryDTO) {
  try {
    const conv = await conversationStore.createConversation({
      title: `${repo.fullName} 开发对话`,
      repositoryFullName: repo.fullName,
    })
    router.push(`/conversations/${conv.id}`)
  } catch (e: any) {
    ElMessage.error(e.message || '创建对话失败')
  }
}
</script>

<style scoped>
.repo-select-page {
  flex: 1;
  padding: 32px 24px;
}

.page-content {
  max-width: 1100px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 32px;
}

.page-title {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 8px;
}

.page-desc {
  font-size: 15px;
  color: var(--text-secondary);
}
</style>

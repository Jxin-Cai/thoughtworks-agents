<template>
  <div class="repo-grid-wrapper">
    <div class="repo-search">
      <el-input
        v-model="search"
        placeholder="搜索仓库..."
        :prefix-icon="Search"
        size="large"
        clearable
      />
    </div>

    <div v-if="loading" class="repo-loading">
      <el-skeleton :rows="2" animated v-for="i in 6" :key="i" style="margin-bottom: 16px" />
    </div>

    <div v-else-if="filtered.length === 0" class="repo-empty">
      <el-icon :size="48" color="var(--text-muted)"><FolderDelete /></el-icon>
      <p>未找到仓库</p>
    </div>

    <div v-else class="repo-grid">
      <div
        v-for="repo in filtered"
        :key="repo.fullName"
        class="repo-card glass-card"
        @click="$emit('select', repo)"
      >
        <div class="repo-header">
          <span class="repo-name">{{ repo.fullName }}</span>
          <el-tag v-if="repo.isPrivate" size="small" type="warning" effect="dark">
            Private
          </el-tag>
          <el-tag v-else size="small" type="success" effect="dark">
            Public
          </el-tag>
        </div>
        <div class="repo-meta">
          <span class="repo-branch">
            <el-icon :size="14"><Connection /></el-icon>
            {{ repo.defaultBranch }}
          </span>
        </div>
        <div class="repo-action">
          <el-button size="small" type="primary" plain>
            <el-icon><ChatDotRound /></el-icon>
            开始对话
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { Search } from '@element-plus/icons-vue'
import type { RepositoryDTO } from '@/types/api'

const props = defineProps<{
  repositories: RepositoryDTO[]
  loading: boolean
}>()

defineEmits<{
  select: [repo: RepositoryDTO]
}>()

const search = ref('')

const filtered = computed(() => {
  if (!search.value.trim()) return props.repositories
  const q = search.value.toLowerCase()
  return props.repositories.filter((r) => r.fullName.toLowerCase().includes(q))
})
</script>

<style scoped>
.repo-grid-wrapper {
  width: 100%;
}

.repo-search {
  margin-bottom: 24px;
}

.repo-search :deep(.el-input__wrapper) {
  background: var(--bg-secondary);
  border: 1px solid var(--border-default);
  border-radius: var(--radius-md);
}

.repo-loading {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 16px;
}

.repo-empty {
  text-align: center;
  padding: 64px 0;
  color: var(--text-muted);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  font-size: 15px;
}

.repo-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 16px;
}

.repo-card {
  padding: 20px;
  cursor: pointer;
  transition: all 0.25s;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.repo-card:hover {
  border-color: var(--color-primary);
  box-shadow: var(--glow-primary);
  transform: translateY(-2px);
}

.repo-header {
  display: flex;
  align-items: center;
  gap: 10px;
}

.repo-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.repo-meta {
  display: flex;
  align-items: center;
  gap: 16px;
  font-size: 13px;
  color: var(--text-secondary);
}

.repo-branch {
  display: flex;
  align-items: center;
  gap: 4px;
}

.repo-action {
  margin-top: 4px;
}
</style>

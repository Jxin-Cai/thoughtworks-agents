<template>
  <aside class="conversation-sidebar glass-card">
    <div class="sidebar-header">
      <h3 class="sidebar-title">对话列表</h3>
      <el-button type="primary" size="small" @click="$emit('create')" :icon="Plus" circle />
    </div>

    <div class="sidebar-search">
      <el-input
        v-model="searchQuery"
        placeholder="搜索对话..."
        :prefix-icon="Search"
        size="small"
        clearable
      />
    </div>

    <div class="sidebar-filter">
      <el-radio-group v-model="filter" size="small">
        <el-radio-button value="all">全部</el-radio-button>
        <el-radio-button value="active">活跃</el-radio-button>
        <el-radio-button value="archived">已归档</el-radio-button>
      </el-radio-group>
    </div>

    <div class="sidebar-list">
      <ConversationList
        :conversations="filteredConversations"
        :current-id="currentId"
        :loading="loading"
        @select="(id) => $emit('select', id)"
      />
    </div>
  </aside>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { Plus, Search } from '@element-plus/icons-vue'
import type { ConversationDTO } from '@/types/api'
import ConversationList from './ConversationList.vue'

const props = defineProps<{
  conversations: ConversationDTO[]
  currentId?: string
  loading: boolean
}>()

defineEmits<{
  select: [id: string]
  create: []
}>()

const searchQuery = ref('')
const filter = ref('all')

const filteredConversations = computed(() => {
  let list = props.conversations

  if (filter.value === 'active') {
    list = list.filter((c) => c.status !== 'ARCHIVED')
  } else if (filter.value === 'archived') {
    list = list.filter((c) => c.status === 'ARCHIVED')
  }

  if (searchQuery.value.trim()) {
    const q = searchQuery.value.toLowerCase()
    list = list.filter(
      (c) =>
        c.title.toLowerCase().includes(q) ||
        (c.repositoryFullName && c.repositoryFullName.toLowerCase().includes(q)),
    )
  }

  return list
})
</script>

<style scoped>
.conversation-sidebar {
  width: 280px;
  min-width: 280px;
  height: 100%;
  display: flex;
  flex-direction: column;
  border-radius: 0;
  border-right: 1px solid var(--border-subtle);
  border-top: none;
  border-bottom: none;
  border-left: none;
}

.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  border-bottom: 1px solid var(--border-subtle);
}

.sidebar-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
}

.sidebar-search {
  padding: 12px 16px 0;
}

.sidebar-filter {
  padding: 12px 16px;
}

.sidebar-filter :deep(.el-radio-group) {
  width: 100%;
}

.sidebar-filter :deep(.el-radio-button) {
  flex: 1;
}

.sidebar-filter :deep(.el-radio-button__inner) {
  width: 100%;
  font-size: 12px;
}

.sidebar-list {
  flex: 1;
  overflow-y: auto;
  padding: 0 8px 8px;
}
</style>

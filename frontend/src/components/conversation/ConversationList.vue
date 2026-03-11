<template>
  <div class="conversation-list">
    <div v-if="loading" class="list-loading">
      <el-skeleton :rows="3" animated />
    </div>

    <div v-else-if="conversations.length === 0" class="list-empty">
      <el-icon :size="40" color="var(--text-muted)"><ChatDotRound /></el-icon>
      <p>暂无对话</p>
    </div>

    <div
      v-else
      v-for="conv in conversations"
      :key="conv.id"
      class="conversation-card glass-card"
      :class="{ 'conversation-card--active': conv.id === currentId }"
      @click="$emit('select', conv.id)"
    >
      <div class="card-header">
        <span class="card-title">{{ conv.title }}</span>
        <el-tag size="small" :type="conv.status === 'ARCHIVED' ? 'info' : 'success'" effect="dark">
          {{ conv.status === 'ARCHIVED' ? '已归档' : '活跃' }}
        </el-tag>
      </div>
      <div class="card-meta">
        <span v-if="conv.repositoryFullName" class="card-repo">
          <el-icon :size="12"><FolderOpened /></el-icon>
          {{ conv.repositoryFullName }}
        </span>
        <span class="card-time">{{ formatRelativeTime(conv.updatedAt) }}</span>
      </div>
      <div class="card-preview" v-if="conv.messages.length > 0">
        {{ lastMessage(conv) }}
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { ConversationDTO } from '@/types/api'
import { formatRelativeTime } from '@/utils/format'

defineProps<{
  conversations: ConversationDTO[]
  currentId?: string
  loading: boolean
}>()

defineEmits<{
  select: [id: string]
  create: []
}>()

function lastMessage(conv: ConversationDTO): string {
  const last = conv.messages[conv.messages.length - 1]
  if (!last) return ''
  const text = last.content
  return text.length > 80 ? text.slice(0, 80) + '...' : text
}
</script>

<style scoped>
.conversation-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.list-loading, .list-empty {
  padding: 24px;
  text-align: center;
}

.list-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  color: var(--text-muted);
  font-size: 14px;
}

.conversation-card {
  padding: 14px 16px;
  cursor: pointer;
  transition: all 0.2s;
  border-radius: var(--radius-md);
}

.conversation-card:hover {
  background: rgba(124, 58, 237, 0.08);
  border-color: rgba(124, 58, 237, 0.25);
}

.conversation-card--active {
  background: rgba(124, 58, 237, 0.15);
  border-color: var(--color-primary);
  box-shadow: var(--glow-primary);
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
}

.card-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
  margin-right: 8px;
}

.card-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
  color: var(--text-muted);
  margin-bottom: 6px;
}

.card-repo {
  display: flex;
  align-items: center;
  gap: 4px;
}

.card-time {
  flex-shrink: 0;
}

.card-preview {
  font-size: 12px;
  color: var(--text-secondary);
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>

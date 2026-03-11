<template>
  <div class="message-list" ref="listRef">
    <div v-if="messages.length === 0" class="empty-messages">
      <el-icon :size="48" color="var(--text-muted)"><ChatDotSquare /></el-icon>
      <p>开始新的对话吧</p>
    </div>

    <div
      v-for="msg in messages"
      :key="msg.id"
      class="message-row"
      :class="`message-row--${msg.role.toLowerCase()}`"
    >
      <div class="message-avatar" v-if="msg.role === 'ASSISTANT'">
        <el-icon :size="18"><Monitor /></el-icon>
      </div>

      <div class="message-bubble" :class="`bubble--${msg.role.toLowerCase()}`">
        <div class="message-content" v-html="renderMarkdown(msg.content)"></div>
        <div class="message-time">{{ formatTime(msg.createdAt) }}</div>
      </div>

      <div class="message-avatar" v-if="msg.role === 'USER'">
        <el-icon :size="18"><User /></el-icon>
      </div>
    </div>

    <!-- Streaming CC output appended to assistant messages -->
    <div v-if="streamingContent" class="message-row message-row--assistant">
      <div class="message-avatar">
        <el-icon :size="18"><Monitor /></el-icon>
      </div>
      <div class="message-bubble bubble--assistant bubble--streaming">
        <div class="message-content" v-html="renderMarkdown(streamingContent)"></div>
        <span class="streaming-indicator"></span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, nextTick, onMounted } from 'vue'
import type { MessageDTO } from '@/types/api'
import { formatTime } from '@/utils/format'

const props = defineProps<{
  messages: MessageDTO[]
  streamingContent?: string
}>()

const listRef = ref<HTMLElement>()

function renderMarkdown(text: string): string {
  // Simple markdown rendering
  let html = escapeHtml(text)
  // Code blocks
  html = html.replace(/```(\w*)\n([\s\S]*?)```/g, '<pre><code class="language-$1">$2</code></pre>')
  // Inline code
  html = html.replace(/`([^`]+)`/g, '<code>$1</code>')
  // Bold
  html = html.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
  // Italic
  html = html.replace(/\*(.+?)\*/g, '<em>$1</em>')
  // Line breaks
  html = html.replace(/\n/g, '<br>')
  return html
}

function escapeHtml(text: string): string {
  const map: Record<string, string> = {
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;',
    "'": '&#039;',
  }
  return text.replace(/[&<>"']/g, (m) => map[m])
}

function scrollToBottom() {
  nextTick(() => {
    if (listRef.value) {
      listRef.value.scrollTop = listRef.value.scrollHeight
    }
  })
}

watch(() => props.messages.length, scrollToBottom)
watch(() => props.streamingContent, scrollToBottom)
onMounted(scrollToBottom)
</script>

<style scoped>
.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.empty-messages {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: var(--text-muted);
  font-size: 15px;
}

.message-row {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  animation: fade-in 0.3s ease;
}

.message-row--user {
  flex-direction: row-reverse;
}

.message-avatar {
  flex-shrink: 0;
  width: 34px;
  height: 34px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-tertiary);
  border: 1px solid var(--border-default);
  color: var(--text-secondary);
}

.message-row--user .message-avatar {
  background: rgba(124, 58, 237, 0.2);
  border-color: rgba(124, 58, 237, 0.3);
  color: var(--color-primary-light);
}

.message-bubble {
  max-width: 70%;
  padding: 12px 16px;
  border-radius: var(--radius-lg);
  line-height: 1.6;
  font-size: 14px;
}

.bubble--user {
  background: linear-gradient(135deg, rgba(124, 58, 237, 0.25), rgba(124, 58, 237, 0.15));
  border: 1px solid rgba(124, 58, 237, 0.3);
  border-top-right-radius: 4px;
  color: var(--text-primary);
}

.bubble--assistant {
  background: var(--glass-bg);
  backdrop-filter: var(--glass-blur);
  border: 1px solid var(--glass-border);
  border-top-left-radius: 4px;
  color: var(--text-primary);
}

.bubble--streaming {
  position: relative;
}

.streaming-indicator {
  display: inline-block;
  width: 6px;
  height: 14px;
  background: var(--color-secondary);
  margin-left: 4px;
  animation: blink 1s infinite;
  vertical-align: middle;
  border-radius: 1px;
}

@keyframes blink {
  0%, 50% { opacity: 1; }
  51%, 100% { opacity: 0; }
}

.message-content {
  word-break: break-word;
}

.message-content :deep(pre) {
  margin: 8px 0;
  font-size: 12px;
}

.message-content :deep(code) {
  font-size: 0.85em;
}

.message-content :deep(strong) {
  color: var(--color-primary-light);
}

.message-time {
  font-size: 11px;
  color: var(--text-muted);
  margin-top: 6px;
  text-align: right;
}
</style>

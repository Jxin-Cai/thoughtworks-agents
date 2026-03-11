<template>
  <div class="message-input">
    <el-input
      v-model="content"
      type="textarea"
      :rows="3"
      :placeholder="disabled ? '对话已归档' : '输入消息... (Ctrl+Enter 发送)'"
      :disabled="disabled || sending"
      resize="none"
      @keydown="handleKeydown"
    />
    <div class="input-actions">
      <span class="input-hint">Ctrl + Enter 发送</span>
      <el-button
        type="primary"
        :loading="sending"
        :disabled="disabled || !content.trim()"
        @click="handleSend"
      >
        <el-icon v-if="!sending"><Promotion /></el-icon>
        发送
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

defineProps<{
  disabled?: boolean
  sending?: boolean
}>()

const emit = defineEmits<{
  send: [content: string]
}>()

const content = ref('')

function handleSend() {
  const text = content.value.trim()
  if (!text) return
  emit('send', text)
  content.value = ''
}

function handleKeydown(e: KeyboardEvent) {
  if (e.ctrlKey && e.key === 'Enter') {
    e.preventDefault()
    handleSend()
  }
}
</script>

<style scoped>
.message-input {
  padding: 16px 24px;
  border-top: 1px solid var(--border-subtle);
  background: rgba(15, 23, 42, 0.5);
}

.message-input :deep(.el-textarea__inner) {
  background: var(--bg-secondary);
  border: 1px solid var(--border-default);
  border-radius: var(--radius-md);
  color: var(--text-primary);
  font-size: 14px;
  font-family: var(--font-sans);
  padding: 12px 16px;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.message-input :deep(.el-textarea__inner:focus) {
  border-color: var(--color-primary);
  box-shadow: var(--glow-primary);
}

.input-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 10px;
}

.input-hint {
  font-size: 12px;
  color: var(--text-muted);
}
</style>

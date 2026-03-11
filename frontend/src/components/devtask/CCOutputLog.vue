<template>
  <div class="cc-output-log" ref="logRef">
    <div class="log-header">
      <span class="log-title">
        <el-icon :size="14"><Monitor /></el-icon>
        Claude Code 输出
      </span>
      <el-button size="small" text @click="copyAll">
        <el-icon><DocumentCopy /></el-icon>
        复制
      </el-button>
    </div>

    <div class="log-body" ref="bodyRef">
      <div v-if="outputs.length === 0" class="log-empty">
        等待输出...
      </div>
      <div
        v-for="(line, idx) in outputs"
        :key="idx"
        class="log-line"
        :class="`log-line--${line.type.toLowerCase()}`"
      >
        <span class="log-time">{{ formatTimestamp(line.timestamp) }}</span>
        <span class="log-content">{{ line.content }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import type { CCOutputMessage } from '@/types/api'

const props = defineProps<{
  outputs: CCOutputMessage[]
}>()

const bodyRef = ref<HTMLElement>()

function formatTimestamp(ts: string): string {
  const d = new Date(ts)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

function copyAll() {
  const text = props.outputs.map((o) => `[${o.type}] ${o.content}`).join('\n')
  navigator.clipboard.writeText(text).then(() => {
    ElMessage.success('已复制到剪贴板')
  })
}

function scrollToBottom() {
  nextTick(() => {
    if (bodyRef.value) {
      bodyRef.value.scrollTop = bodyRef.value.scrollHeight
    }
  })
}

watch(() => props.outputs.length, scrollToBottom)
</script>

<style scoped>
.cc-output-log {
  border-radius: var(--radius-md);
  overflow: hidden;
  border: 1px solid var(--border-subtle);
  background: rgba(2, 6, 23, 0.8);
}

.log-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 14px;
  background: rgba(15, 23, 42, 0.6);
  border-bottom: 1px solid var(--border-subtle);
}

.log-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 600;
  color: var(--text-secondary);
  font-family: var(--font-mono);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.log-body {
  max-height: 400px;
  overflow-y: auto;
  padding: 12px 14px;
  font-family: var(--font-mono);
  font-size: 12px;
  line-height: 1.7;
}

.log-empty {
  color: var(--text-muted);
  font-style: italic;
}

.log-line {
  display: flex;
  gap: 10px;
  white-space: pre-wrap;
  word-break: break-all;
}

.log-time {
  flex-shrink: 0;
  color: var(--text-muted);
  font-size: 11px;
}

.log-line--stdout .log-content {
  color: #E2E8F0;
}

.log-line--stderr .log-content {
  color: #F87171;
}

.log-line--system .log-content {
  color: var(--color-secondary);
}
</style>

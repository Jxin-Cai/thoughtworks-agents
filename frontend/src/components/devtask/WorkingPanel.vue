<template>
  <div class="working-panel glass-card">
    <div class="panel-header">
      <el-icon :size="20" color="var(--color-primary-light)"><Edit /></el-icon>
      <h3>编码阶段</h3>
    </div>

    <div v-if="status === 'WORKING'" class="panel-body">
      <div class="status-bar">
        <el-icon class="spin-icon"><Loading /></el-icon>
        <span>Claude Code 正在编码...</span>
      </div>
      <CCOutputLog :outputs="ccOutput" />
    </div>

    <div v-else-if="isCompleted" class="panel-body">
      <div class="success-banner">
        <el-icon :size="32" color="#22C55E"><CircleCheckFilled /></el-icon>
        <div>
          <h4>编码完成</h4>
          <p>代码已生成并提交至分支</p>
        </div>
      </div>
      <CCOutputLog v-if="ccOutput.length > 0" :outputs="ccOutput" />
    </div>

    <div v-else class="panel-body center">
      <el-icon :size="40" color="var(--text-muted)"><Clock /></el-icon>
      <p class="hint-text">等待思考阶段完成后开始编码</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { DevTaskStatus, CCOutputMessage } from '@/types/api'
import CCOutputLog from './CCOutputLog.vue'

const props = defineProps<{
  status: DevTaskStatus
  ccOutput: CCOutputMessage[]
}>()

const isCompleted = computed(() => {
  const s = props.status
  return s === 'READY_TO_PUBLISH' || s === 'PUBLISHING' || s === 'PUBLISHED'
})
</script>

<style scoped>
.working-panel {
  padding: 24px;
}

.panel-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 20px;
}

.panel-header h3 {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

.panel-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.panel-body.center {
  align-items: center;
  padding: 32px 0;
}

.status-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: var(--color-secondary);
}

.spin-icon {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.success-banner {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 20px;
  border-radius: var(--radius-md);
  background: rgba(34, 197, 94, 0.1);
  border: 1px solid rgba(34, 197, 94, 0.2);
}

.success-banner h4 {
  font-size: 15px;
  font-weight: 600;
  color: #22C55E;
}

.success-banner p {
  font-size: 13px;
  color: var(--text-secondary);
  margin-top: 2px;
}

.hint-text {
  color: var(--text-muted);
  font-size: 14px;
  margin-top: 8px;
}
</style>

<template>
  <div class="task-progress">
    <div
      v-for="(step, idx) in steps"
      :key="step.key"
      class="progress-step"
      :class="{
        'step--active': step.key === activePhase,
        'step--completed': isCompleted(step.key),
        'step--pending': isPending(step.key),
      }"
    >
      <div class="step-indicator">
        <div class="step-circle">
          <el-icon v-if="isCompleted(step.key)" :size="16"><Check /></el-icon>
          <span v-else>{{ idx + 1 }}</span>
        </div>
        <div v-if="idx < steps.length - 1" class="step-line" />
      </div>
      <div class="step-label">{{ step.label }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { DevTaskStatus } from '@/types/api'

const props = defineProps<{
  status: DevTaskStatus
}>()

const steps = [
  { key: 'THINKING', label: '思考' },
  { key: 'WORKING', label: '工作' },
  { key: 'PUBLISHING', label: '发布' },
]

const phaseOrder: Record<string, number> = {
  CREATED: 0,
  THINKING: 1,
  WORKING: 2,
  READY_TO_PUBLISH: 3,
  PUBLISHING: 3,
  PUBLISHED: 4,
  FAILED: -1,
}

const activePhase = computed(() => {
  const s = props.status
  if (s === 'CREATED' || s === 'THINKING') return 'THINKING'
  if (s === 'WORKING') return 'WORKING'
  return 'PUBLISHING'
})

function isCompleted(key: string): boolean {
  const statusOrder = phaseOrder[props.status] ?? 0
  const stepOrder = phaseOrder[key] ?? 0
  if (props.status === 'PUBLISHED') return true
  return statusOrder > stepOrder
}

function isPending(key: string): boolean {
  return !isCompleted(key) && key !== activePhase.value
}
</script>

<style scoped>
.task-progress {
  display: flex;
  align-items: flex-start;
  justify-content: center;
  gap: 0;
  padding: 24px 0;
}

.progress-step {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  position: relative;
}

.step-indicator {
  display: flex;
  align-items: center;
}

.step-circle {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
  border: 2px solid var(--border-default);
  background: var(--bg-secondary);
  color: var(--text-muted);
  transition: all 0.3s;
  z-index: 1;
}

.step-line {
  width: 120px;
  height: 2px;
  background: var(--border-default);
  transition: background 0.3s;
}

.step--active .step-circle {
  border-color: var(--color-primary);
  background: rgba(124, 58, 237, 0.2);
  color: var(--color-primary-light);
  animation: glow-pulse 2s infinite;
}

.step--completed .step-circle {
  border-color: #22C55E;
  background: rgba(34, 197, 94, 0.2);
  color: #22C55E;
}

.step--completed .step-line {
  background: #22C55E;
}

.step-label {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-muted);
}

.step--active .step-label {
  color: var(--color-primary-light);
}

.step--completed .step-label {
  color: #22C55E;
}
</style>

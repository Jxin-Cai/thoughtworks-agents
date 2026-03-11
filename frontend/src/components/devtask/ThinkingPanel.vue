<template>
  <div class="thinking-panel glass-card">
    <div class="panel-header">
      <el-icon :size="20" color="var(--color-secondary)"><Cpu /></el-icon>
      <h3>思考阶段</h3>
    </div>

    <!-- CREATED: Show start button -->
    <div v-if="status === 'CREATED'" class="panel-body center">
      <p class="hint-text">启动 Claude Code 进行需求分析与设计</p>
      <el-button type="primary" size="large" :loading="starting" @click="handleStart">
        <el-icon><VideoPlay /></el-icon>
        开始思考
      </el-button>
    </div>

    <!-- THINKING: Show CC output log -->
    <div v-else-if="status === 'THINKING'" class="panel-body">
      <div class="status-bar">
        <el-icon class="spin-icon"><Loading /></el-icon>
        <span>Claude Code 正在分析需求...</span>
      </div>
      <CCOutputLog :outputs="ccOutput" />
    </div>

    <!-- Design complete: Show output + confirm -->
    <div v-else class="panel-body">
      <div v-if="thinkingPhase?.output" class="design-output">
        <h4>设计方案</h4>
        <pre class="design-content">{{ thinkingPhase.output }}</pre>
      </div>

      <div v-if="thinkingPhase?.finishedAt && !isAdvanced" class="confirm-section">
        <el-button type="primary" size="large" :loading="advancing" @click="handleAdvance">
          <el-icon><Check /></el-icon>
          确认方案，进入编码
        </el-button>
      </div>

      <div v-if="isAdvanced" class="completed-badge">
        <el-icon :size="20" color="#22C55E"><CircleCheckFilled /></el-icon>
        <span>思考阶段已完成</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import type { DevTaskStatus, TaskPhaseDTO, CCOutputMessage } from '@/types/api'
import CCOutputLog from './CCOutputLog.vue'
import { useDevTaskStore } from '@/stores/devTask'

const props = defineProps<{
  taskId: string
  status: DevTaskStatus
  phases: TaskPhaseDTO[]
  ccOutput: CCOutputMessage[]
}>()

const devTaskStore = useDevTaskStore()
const starting = ref(false)
const advancing = ref(false)

const thinkingPhase = computed(() =>
  props.phases.find((p) => p.phaseType === 'THINKING'),
)

const isAdvanced = computed(() => {
  const s = props.status
  return s === 'WORKING' || s === 'READY_TO_PUBLISH' || s === 'PUBLISHING' || s === 'PUBLISHED'
})

async function handleStart() {
  starting.value = true
  try {
    await devTaskStore.startDevelopment(props.taskId, {
      workingDirectory: '/tmp/workspace',
      environmentVariables: {},
    })
    ElMessage.success('已开始思考')
  } catch (e: any) {
    ElMessage.error(e.message || '启动失败')
  } finally {
    starting.value = false
  }
}

async function handleAdvance() {
  advancing.value = true
  try {
    await devTaskStore.advanceToWorking(props.taskId, {
      designOutput: thinkingPhase.value?.output || '',
      workingDirectory: '/tmp/workspace',
      environmentVariables: {},
    })
    ElMessage.success('已进入编码阶段')
  } catch (e: any) {
    ElMessage.error(e.message || '推进失败')
  } finally {
    advancing.value = false
  }
}
</script>

<style scoped>
.thinking-panel {
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

.hint-text {
  color: var(--text-secondary);
  font-size: 14px;
  margin-bottom: 8px;
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

.design-output h4 {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 8px;
}

.design-content {
  max-height: 300px;
  overflow-y: auto;
  font-size: 13px;
  line-height: 1.6;
}

.confirm-section {
  display: flex;
  justify-content: center;
  padding-top: 8px;
}

.completed-badge {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #22C55E;
}
</style>

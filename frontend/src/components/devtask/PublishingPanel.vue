<template>
  <div class="publishing-panel glass-card">
    <div class="panel-header">
      <el-icon :size="20" color="var(--color-accent)"><Upload /></el-icon>
      <h3>发布阶段</h3>
    </div>

    <!-- READY_TO_PUBLISH: Show form -->
    <div v-if="status === 'READY_TO_PUBLISH'" class="panel-body">
      <p class="hint-text">选择要合并到的目标分支</p>
      <div class="publish-form">
        <el-select v-model="baseBranch" placeholder="选择目标分支" size="large" style="width: 280px">
          <el-option label="main" value="main" />
          <el-option label="master" value="master" />
          <el-option label="develop" value="develop" />
        </el-select>
        <el-button type="primary" size="large" :loading="publishing" @click="handlePublish">
          <el-icon><Upload /></el-icon>
          发布
        </el-button>
      </div>
    </div>

    <!-- PUBLISHING: Loading -->
    <div v-else-if="status === 'PUBLISHING'" class="panel-body center">
      <el-icon :size="40" class="spin-icon" color="var(--color-primary-light)"><Loading /></el-icon>
      <p class="status-text">正在发布...</p>
    </div>

    <!-- PUBLISHED: Success -->
    <div v-else-if="status === 'PUBLISHED'" class="panel-body center">
      <el-icon :size="48" color="#22C55E"><CircleCheckFilled /></el-icon>
      <h4 class="success-text">发布成功</h4>
      <p class="hint-text">代码已成功合并至目标分支</p>
    </div>

    <!-- FAILED: Error + Retry -->
    <div v-else-if="status === 'FAILED'" class="panel-body center">
      <el-icon :size="48" color="#EF4444"><CircleCloseFilled /></el-icon>
      <h4 class="error-text">发布失败</h4>
      <p class="hint-text" v-if="failureReason">{{ failureReason }}</p>
      <el-button type="primary" @click="handlePublish">
        <el-icon><RefreshRight /></el-icon>
        重试
      </el-button>
    </div>

    <!-- Pending -->
    <div v-else class="panel-body center">
      <el-icon :size="40" color="var(--text-muted)"><Clock /></el-icon>
      <p class="hint-text">等待编码阶段完成</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import type { DevTaskStatus, TaskPhaseDTO } from '@/types/api'
import { useDevTaskStore } from '@/stores/devTask'

const props = defineProps<{
  taskId: string
  status: DevTaskStatus
  phases: TaskPhaseDTO[]
}>()

const devTaskStore = useDevTaskStore()
const baseBranch = ref('main')
const publishing = ref(false)

const failureReason = computed(() => {
  const pub = props.phases.find((p) => p.phaseType === 'PUBLISHING')
  return pub?.failureReason || null
})

async function handlePublish() {
  publishing.value = true
  try {
    await devTaskStore.executePublish(props.taskId, {
      baseBranch: baseBranch.value,
    })
    ElMessage.success('发布请求已提交')
  } catch (e: any) {
    ElMessage.error(e.message || '发布失败')
  } finally {
    publishing.value = false
  }
}
</script>

<style scoped>
.publishing-panel {
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
}

.publish-form {
  display: flex;
  align-items: center;
  gap: 12px;
}

.spin-icon {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.status-text {
  color: var(--color-primary-light);
  font-size: 15px;
  font-weight: 500;
}

.success-text {
  color: #22C55E;
  font-size: 18px;
  font-weight: 600;
}

.error-text {
  color: #EF4444;
  font-size: 18px;
  font-weight: 600;
}
</style>

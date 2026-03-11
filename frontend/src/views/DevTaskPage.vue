<template>
  <div class="devtask-page">
    <div class="page-content">
      <!-- Loading -->
      <div v-if="devTaskStore.loading && !task" class="page-loading">
        <el-icon :size="32" class="spin-icon"><Loading /></el-icon>
        <p>加载任务中...</p>
      </div>

      <template v-else-if="task">
        <!-- Task Info -->
        <div class="task-info glass-card">
          <div class="task-info-header">
            <div>
              <h1 class="task-title">{{ task.branchName }}</h1>
              <p class="task-repo">
                <el-icon :size="14"><FolderOpened /></el-icon>
                {{ task.repositoryFullName }}
              </p>
            </div>
            <el-tag :type="statusType(task.status)" effect="dark" size="large">
              {{ statusLabel(task.status) }}
            </el-tag>
          </div>
          <div class="task-requirement">
            <h4>需求描述</h4>
            <p>{{ task.requirement }}</p>
          </div>
        </div>

        <!-- Progress Bar -->
        <TaskProgressBar :status="task.status" />

        <!-- Phase Panels -->
        <div class="phase-panels">
          <ThinkingPanel
            :task-id="task.id"
            :status="task.status"
            :phases="task.phases"
            :cc-output="devTaskStore.ccOutput"
          />

          <WorkingPanel
            :status="task.status"
            :cc-output="devTaskStore.ccOutput"
          />

          <PublishingPanel
            :task-id="task.id"
            :status="task.status"
            :phases="task.phases"
          />
        </div>

        <!-- Back link -->
        <div class="back-link">
          <el-button text @click="$router.push(`/conversations/${task.conversationId}`)">
            <el-icon><ArrowLeft /></el-icon>
            返回对话
          </el-button>
        </div>
      </template>

      <div v-else class="page-empty">
        <p>任务不存在</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, watch } from 'vue'
import { useDevTaskStore } from '@/stores/devTask'
import { useWebSocketStore } from '@/stores/websocket'
import { statusLabel, statusType } from '@/utils/format'
import TaskProgressBar from '@/components/devtask/TaskProgressBar.vue'
import ThinkingPanel from '@/components/devtask/ThinkingPanel.vue'
import WorkingPanel from '@/components/devtask/WorkingPanel.vue'
import PublishingPanel from '@/components/devtask/PublishingPanel.vue'

const props = defineProps<{ id: string }>()
const devTaskStore = useDevTaskStore()
const wsStore = useWebSocketStore()

const task = computed(() => devTaskStore.currentTask)

onMounted(async () => {
  devTaskStore.clearCCOutput()
  await devTaskStore.loadTask(props.id)

  // Connect WebSocket and subscribe
  wsStore.connect()

  // Wait a bit for connection then subscribe
  setTimeout(() => {
    // Subscribe to task status
    wsStore.subscribeDevTaskStatus(props.id, (msg) => {
      devTaskStore.updateStatus(msg.taskId, msg.currentStatus)
      // Reload task to get updated phases
      devTaskStore.loadTask(props.id)
    })

    // Subscribe to CC output for active phases
    if (task.value) {
      const activePhase = task.value.phases.find(
        (p) => p.ccSessionId && !p.finishedAt,
      )
      if (activePhase?.ccSessionId) {
        wsStore.subscribeCCOutput(activePhase.ccSessionId, (msg) => {
          devTaskStore.appendCCOutput(msg)
        })
      }
    }
  }, 1000)
})

watch(
  () => task.value?.status,
  (newStatus, oldStatus) => {
    if (newStatus !== oldStatus && task.value) {
      // Subscribe to new CC sessions when phase changes
      const activePhase = task.value.phases.find(
        (p) => p.ccSessionId && !p.finishedAt,
      )
      if (activePhase?.ccSessionId) {
        wsStore.subscribeCCOutput(activePhase.ccSessionId, (msg) => {
          devTaskStore.appendCCOutput(msg)
        })
      }
    }
  },
)

onUnmounted(() => {
  wsStore.unsubscribeAll()
})
</script>

<style scoped>
.devtask-page {
  flex: 1;
  padding: 32px 24px;
  overflow-y: auto;
  height: calc(100vh - 60px);
}

.page-content {
  max-width: 900px;
  margin: 0 auto;
}

.page-loading, .page-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 0;
  gap: 12px;
  color: var(--text-muted);
}

.spin-icon {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.task-info {
  padding: 24px;
  margin-bottom: 8px;
}

.task-info-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 16px;
}

.task-title {
  font-size: 22px;
  font-weight: 700;
  color: var(--text-primary);
  font-family: var(--font-mono);
  margin-bottom: 4px;
}

.task-repo {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--text-secondary);
}

.task-requirement h4 {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-secondary);
  margin-bottom: 6px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.task-requirement p {
  font-size: 14px;
  color: var(--text-primary);
  line-height: 1.6;
}

.phase-panels {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-top: 8px;
}

.back-link {
  margin-top: 24px;
  text-align: center;
}
</style>

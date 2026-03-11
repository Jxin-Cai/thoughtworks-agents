<template>
  <div class="conversation-page">
    <ConversationSidebar
      :conversations="conversationStore.conversations"
      :current-id="id"
      :loading="conversationStore.loading"
      @select="handleSelect"
      @create="handleCreate"
    />

    <main class="conversation-main">
      <!-- Info bar -->
      <div class="info-bar" v-if="currentConv">
        <div class="info-left">
          <h2 class="conv-title">{{ currentConv.title }}</h2>
          <el-tag v-if="currentConv.repositoryFullName" size="small" effect="dark" type="info">
            {{ currentConv.repositoryFullName }}
          </el-tag>
        </div>
        <div class="info-right">
          <el-button
            v-if="currentConv.repositoryFullName"
            size="small"
            type="primary"
            plain
            @click="showCreateTask = true"
          >
            <el-icon><Cpu /></el-icon>
            创建任务
          </el-button>
          <el-button
            v-if="currentConv.status !== 'ARCHIVED'"
            size="small"
            type="danger"
            plain
            @click="handleArchive"
          >
            <el-icon><Delete /></el-icon>
            归档
          </el-button>
        </div>
      </div>

      <!-- Messages -->
      <MessageList
        :messages="currentConv?.messages ?? []"
        :streaming-content="streamingContent"
      />

      <!-- Input -->
      <MessageInput
        :disabled="!currentConv || currentConv.status === 'ARCHIVED'"
        :sending="conversationStore.sending"
        @send="handleSend"
      />
    </main>

    <!-- Create Task Dialog -->
    <CreateTaskDialog
      v-if="currentConv?.repositoryFullName"
      v-model="showCreateTask"
      :conversation-id="id"
      :repository-full-name="currentConv.repositoryFullName"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useConversationStore } from '@/stores/conversation'
import { useWebSocketStore } from '@/stores/websocket'
import ConversationSidebar from '@/components/conversation/ConversationSidebar.vue'
import MessageList from '@/components/conversation/MessageList.vue'
import MessageInput from '@/components/conversation/MessageInput.vue'
import CreateTaskDialog from '@/components/conversation/CreateTaskDialog.vue'

const props = defineProps<{ id: string }>()
const router = useRouter()
const conversationStore = useConversationStore()
const wsStore = useWebSocketStore()

const showCreateTask = ref(false)
const streamingContent = ref('')

const currentConv = computed(() => conversationStore.currentConversation)

onMounted(async () => {
  await conversationStore.loadConversations()
  await conversationStore.loadConversation(props.id)

  // Connect WebSocket
  wsStore.connect()
})

watch(
  () => props.id,
  async (newId) => {
    streamingContent.value = ''
    await conversationStore.loadConversation(newId)
  },
)

onUnmounted(() => {
  wsStore.unsubscribeAll()
})

function handleSelect(id: string) {
  router.push(`/conversations/${id}`)
}

function handleCreate() {
  router.push('/')
}

async function handleSend(content: string) {
  if (!currentConv.value) return
  try {
    await conversationStore.sendMessage(props.id, {
      content,
      workingDirectory: '/tmp/workspace',
      environmentVariables: {},
    })
  } catch (e: any) {
    ElMessage.error(e.message || '发送失败')
  }
}

async function handleArchive() {
  try {
    await ElMessageBox.confirm('确定要归档此对话吗？', '归档对话', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await conversationStore.archiveConversation(props.id)
    ElMessage.success('对话已归档')
  } catch {
    // cancelled
  }
}
</script>

<style scoped>
.conversation-page {
  flex: 1;
  display: flex;
  overflow: hidden;
  height: calc(100vh - 60px);
}

.conversation-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.info-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 24px;
  border-bottom: 1px solid var(--border-subtle);
  background: rgba(15, 23, 42, 0.4);
}

.info-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.conv-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

.info-right {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>

<template>
  <div class="home-page">
    <div class="home-content">
      <div class="hero-section">
        <h1 class="hero-title glow-text">ThoughtWorks Agents</h1>
        <p class="hero-desc">AI 驱动的智能开发平台，让 Claude Code 为你编写代码</p>
        <div class="hero-actions">
          <el-button type="primary" size="large" @click="handleCreate">
            <el-icon><Plus /></el-icon>
            新建对话
          </el-button>
          <el-button size="large" @click="$router.push('/repos')">
            <el-icon><FolderOpened /></el-icon>
            浏览仓库
          </el-button>
        </div>
      </div>

      <div class="conversations-section">
        <div class="section-header">
          <h2>最近对话</h2>
          <el-button text @click="loadData">
            <el-icon><RefreshRight /></el-icon>
            刷新
          </el-button>
        </div>
        <ConversationList
          :conversations="conversationStore.conversations"
          :loading="conversationStore.loading"
          @select="handleSelect"
          @create="handleCreate"
        />
      </div>
    </div>

    <!-- Create dialog -->
    <el-dialog v-model="showCreate" title="新建对话" width="420px" :close-on-click-modal="false">
      <el-form @submit.prevent="submitCreate">
        <el-form-item label="对话标题">
          <el-input v-model="newTitle" placeholder="请输入对话标题" />
        </el-form-item>
        <el-form-item label="关联仓库 (可选)">
          <el-input v-model="newRepo" placeholder="例如: owner/repo" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreate = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="submitCreate">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useConversationStore } from '@/stores/conversation'
import ConversationList from '@/components/conversation/ConversationList.vue'

const router = useRouter()
const conversationStore = useConversationStore()

const showCreate = ref(false)
const newTitle = ref('')
const newRepo = ref('')
const creating = ref(false)

function loadData() {
  conversationStore.loadConversations()
}

onMounted(loadData)

function handleSelect(id: string) {
  router.push(`/conversations/${id}`)
}

function handleCreate() {
  newTitle.value = ''
  newRepo.value = ''
  showCreate.value = true
}

async function submitCreate() {
  if (!newTitle.value.trim()) {
    ElMessage.warning('请输入对话标题')
    return
  }
  creating.value = true
  try {
    const conv = await conversationStore.createConversation({
      title: newTitle.value.trim(),
      repositoryFullName: newRepo.value.trim() || undefined,
    })
    showCreate.value = false
    router.push(`/conversations/${conv.id}`)
  } catch (e: any) {
    ElMessage.error(e.message || '创建失败')
  } finally {
    creating.value = false
  }
}
</script>

<style scoped>
.home-page {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 48px 24px;
}

.home-content {
  width: 100%;
  max-width: 800px;
}

.hero-section {
  text-align: center;
  padding: 40px 0 48px;
}

.hero-title {
  font-size: 40px;
  font-weight: 800;
  letter-spacing: -1px;
  margin-bottom: 12px;
  color: var(--text-primary);
}

.hero-desc {
  font-size: 16px;
  color: var(--text-secondary);
  margin-bottom: 28px;
  line-height: 1.6;
}

.hero-actions {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
}

.conversations-section {
  margin-top: 16px;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.section-header h2 {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
}
</style>

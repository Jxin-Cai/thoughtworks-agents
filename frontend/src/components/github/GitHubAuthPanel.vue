<template>
  <div class="auth-panel glass-card">
    <div class="auth-icon">
      <svg viewBox="0 0 24 24" width="64" height="64" fill="currentColor">
        <path d="M12 0C5.37 0 0 5.37 0 12c0 5.31 3.435 9.795 8.205 11.385.6.105.825-.255.825-.57 0-.285-.015-1.23-.015-2.235-3.015.555-3.795-.735-4.035-1.41-.135-.345-.72-1.41-1.23-1.695-.42-.225-1.02-.78-.015-.795.945-.015 1.62.87 1.845 1.23 1.08 1.815 2.805 1.305 3.495.99.105-.78.42-1.305.765-1.605-2.67-.3-5.46-1.335-5.46-5.925 0-1.305.465-2.385 1.23-3.225-.12-.3-.54-1.53.12-3.18 0 0 1.005-.315 3.3 1.23.96-.27 1.98-.405 3-.405s2.04.135 3 .405c2.295-1.56 3.3-1.23 3.3-1.23.66 1.65.24 2.88.12 3.18.765.84 1.23 1.905 1.23 3.225 0 4.605-2.805 5.625-5.475 5.925.435.375.81 1.095.81 2.22 0 1.605-.015 2.895-.015 3.3 0 .315.225.69.825.57A12.02 12.02 0 0024 12c0-6.63-5.37-12-12-12z"/>
      </svg>
    </div>

    <h2 class="auth-title">连接 GitHub</h2>
    <p class="auth-desc">授权后即可访问您的仓库并管理开发任务</p>

    <el-button
      v-if="!githubStore.authenticated"
      type="primary"
      size="large"
      :loading="githubStore.loading"
      @click="handleAuth"
      class="auth-button"
    >
      <el-icon><Link /></el-icon>
      授权 GitHub
    </el-button>

    <div v-else class="auth-success">
      <el-icon :size="32" color="#22C55E"><CircleCheckFilled /></el-icon>
      <span>已成功授权</span>
      <el-button type="primary" @click="$router.push('/repos')">
        浏览仓库
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useGitHubStore } from '@/stores/github'

const route = useRoute()
const githubStore = useGitHubStore()

const GITHUB_CLIENT_ID = import.meta.env.VITE_GITHUB_CLIENT_ID || 'your_client_id'

function handleAuth() {
  const redirectUri = `${window.location.origin}/auth`
  const url = `https://github.com/login/oauth/authorize?client_id=${GITHUB_CLIENT_ID}&redirect_uri=${encodeURIComponent(redirectUri)}&scope=repo`
  window.location.href = url
}

onMounted(async () => {
  const code = route.query.code as string
  if (code) {
    try {
      await githubStore.handleCallback(code)
      ElMessage.success('GitHub 授权成功')
      // Remove code from URL
      window.history.replaceState({}, '', '/auth')
    } catch (e: any) {
      ElMessage.error(e.message || '授权失败')
    }
  }
})
</script>

<style scoped>
.auth-panel {
  max-width: 420px;
  margin: 0 auto;
  padding: 48px 40px;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

.auth-icon {
  color: var(--text-secondary);
  margin-bottom: 8px;
}

.auth-title {
  font-size: 24px;
  font-weight: 700;
  color: var(--text-primary);
}

.auth-desc {
  font-size: 14px;
  color: var(--text-secondary);
  line-height: 1.6;
  max-width: 300px;
}

.auth-button {
  margin-top: 16px;
  padding: 12px 32px;
  font-size: 16px;
  border-radius: var(--radius-md);
}

.auth-button:hover {
  box-shadow: var(--glow-primary-strong);
}

.auth-success {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  color: #22C55E;
  font-size: 16px;
  font-weight: 600;
}
</style>

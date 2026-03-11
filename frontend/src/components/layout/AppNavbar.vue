<template>
  <header class="app-navbar">
    <div class="navbar-left">
      <router-link to="/" class="logo-link">
        <div class="logo-icon">
          <el-icon :size="24"><Monitor /></el-icon>
        </div>
        <span class="app-name glow-text">TW Agents</span>
      </router-link>
    </div>

    <nav class="navbar-center">
      <router-link to="/" class="nav-link" active-class="nav-link--active">
        <el-icon><HomeFilled /></el-icon>
        <span>首页</span>
      </router-link>
      <router-link to="/repos" class="nav-link" active-class="nav-link--active">
        <el-icon><FolderOpened /></el-icon>
        <span>仓库</span>
      </router-link>
    </nav>

    <div class="navbar-right">
      <div v-if="githubStore.authenticated" class="auth-status auth-status--ok">
        <el-icon color="#22C55E"><CircleCheckFilled /></el-icon>
        <span>GitHub 已授权</span>
      </div>
      <router-link v-else to="/auth" class="auth-btn">
        <el-icon><Link /></el-icon>
        <span>授权 GitHub</span>
      </router-link>
    </div>
  </header>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useGitHubStore } from '@/stores/github'

const githubStore = useGitHubStore()

onMounted(() => {
  githubStore.checkAuthStatus()
})
</script>

<style scoped>
.app-navbar {
  position: sticky;
  top: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 60px;
  padding: 0 24px;
  background: rgba(15, 23, 42, 0.85);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border-bottom: 1px solid var(--border-subtle);
}

.navbar-left {
  display: flex;
  align-items: center;
}

.logo-link {
  display: flex;
  align-items: center;
  gap: 10px;
  text-decoration: none;
  color: var(--text-primary);
}

.logo-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: var(--radius-sm);
  background: linear-gradient(135deg, var(--color-primary), var(--color-secondary));
  color: #fff;
}

.app-name {
  font-size: 18px;
  font-weight: 700;
  letter-spacing: -0.5px;
  color: var(--text-primary);
}

.navbar-center {
  display: flex;
  align-items: center;
  gap: 4px;
}

.nav-link {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: var(--radius-sm);
  font-size: 14px;
  font-weight: 500;
  color: var(--text-secondary);
  text-decoration: none;
  transition: all 0.2s;
}

.nav-link:hover {
  color: var(--text-primary);
  background: rgba(124, 58, 237, 0.1);
}

.nav-link--active {
  color: var(--color-primary-light);
  background: rgba(124, 58, 237, 0.15);
}

.navbar-right {
  display: flex;
  align-items: center;
}

.auth-status {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--text-secondary);
}

.auth-status--ok {
  color: #22C55E;
}

.auth-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  border-radius: var(--radius-sm);
  font-size: 13px;
  font-weight: 500;
  color: var(--text-primary);
  background: rgba(124, 58, 237, 0.2);
  border: 1px solid rgba(124, 58, 237, 0.3);
  text-decoration: none;
  transition: all 0.2s;
}

.auth-btn:hover {
  background: rgba(124, 58, 237, 0.3);
  box-shadow: var(--glow-primary);
}
</style>

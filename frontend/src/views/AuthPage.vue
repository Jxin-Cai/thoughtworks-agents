<template>
  <div class="auth-page">
    <div class="auth-container">
      <GitHubAuthPanel />
    </div>
  </div>
</template>

<script setup lang="ts">
import GitHubAuthPanel from '@/components/github/GitHubAuthPanel.vue'
</script>

<style scoped>
.auth-page {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px 24px;
}

.auth-container {
  width: 100%;
  max-width: 500px;
}
</style>

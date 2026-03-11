import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/HomePage.vue'),
  },
  {
    path: '/auth',
    name: 'Auth',
    component: () => import('@/views/AuthPage.vue'),
  },
  {
    path: '/repos',
    name: 'Repos',
    component: () => import('@/views/RepoSelectPage.vue'),
  },
  {
    path: '/conversations/:id',
    name: 'Conversation',
    component: () => import('@/views/ConversationPage.vue'),
    props: true,
  },
  {
    path: '/tasks/:id',
    name: 'DevTask',
    component: () => import('@/views/DevTaskPage.vue'),
    props: true,
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

export default router

import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { ConversationDTO } from '@/types/api'
import * as conversationApi from '@/api/conversation'
import type { CreateConversationRequest, SendMessageRequest } from '@/types/request'

export const useConversationStore = defineStore('conversation', () => {
  const conversations = ref<ConversationDTO[]>([])
  const currentConversation = ref<ConversationDTO | null>(null)
  const loading = ref(false)
  const sending = ref(false)

  async function loadConversations(repositoryFullName?: string) {
    loading.value = true
    try {
      conversations.value = await conversationApi.getConversations(repositoryFullName)
    } finally {
      loading.value = false
    }
  }

  async function loadConversation(id: string) {
    loading.value = true
    try {
      currentConversation.value = await conversationApi.getConversation(id)
    } finally {
      loading.value = false
    }
  }

  async function createConversation(data: CreateConversationRequest): Promise<ConversationDTO> {
    const conv = await conversationApi.createConversation(data)
    conversations.value.unshift(conv)
    currentConversation.value = conv
    return conv
  }

  async function sendMessage(conversationId: string, data: SendMessageRequest) {
    sending.value = true
    try {
      const updated = await conversationApi.sendMessage(conversationId, data)
      currentConversation.value = updated
      const idx = conversations.value.findIndex((c) => c.id === conversationId)
      if (idx !== -1) {
        conversations.value[idx] = updated
      }
    } finally {
      sending.value = false
    }
  }

  async function archiveConversation(id: string) {
    await conversationApi.archiveConversation(id)
    const idx = conversations.value.findIndex((c) => c.id === id)
    if (idx !== -1) {
      conversations.value[idx].status = 'ARCHIVED'
    }
    if (currentConversation.value?.id === id) {
      currentConversation.value.status = 'ARCHIVED'
    }
  }

  return {
    conversations,
    currentConversation,
    loading,
    sending,
    loadConversations,
    loadConversation,
    createConversation,
    sendMessage,
    archiveConversation,
  }
})

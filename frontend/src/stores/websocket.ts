import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as ws from '@/api/websocket'
import type { StompSubscription } from '@/api/websocket'
import type { CCOutputMessage, DevTaskStatusMessage } from '@/types/api'

export const useWebSocketStore = defineStore('websocket', () => {
  const connected = ref(false)
  const subscriptions = ref<Map<string, StompSubscription>>(new Map())

  function connect() {
    ws.connect(
      () => {
        connected.value = true
      },
      (error) => {
        console.error('[WS Store] Connection error:', error)
        connected.value = false
      },
    )
  }

  function subscribeCCOutput(sessionId: string, callback: (msg: CCOutputMessage) => void) {
    const key = `cc-${sessionId}`
    if (subscriptions.value.has(key)) return

    const sub = ws.subscribeCCOutput(sessionId, callback)
    if (sub) {
      subscriptions.value.set(key, sub)
    }
  }

  function subscribeDevTaskStatus(taskId: string, callback: (msg: DevTaskStatusMessage) => void) {
    const key = `task-${taskId}`
    if (subscriptions.value.has(key)) return

    const sub = ws.subscribeDevTaskStatus(taskId, callback)
    if (sub) {
      subscriptions.value.set(key, sub)
    }
  }

  function unsubscribe(key: string) {
    const sub = subscriptions.value.get(key)
    if (sub) {
      sub.unsubscribe()
      subscriptions.value.delete(key)
    }
  }

  function unsubscribeAll() {
    subscriptions.value.forEach((sub) => sub.unsubscribe())
    subscriptions.value.clear()
  }

  function disconnect() {
    unsubscribeAll()
    ws.disconnect()
    connected.value = false
  }

  return {
    connected,
    subscriptions,
    connect,
    subscribeCCOutput,
    subscribeDevTaskStatus,
    unsubscribe,
    unsubscribeAll,
    disconnect,
  }
})

import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import type { CCOutputMessage, DevTaskStatusMessage } from '@/types/api'

let stompClient: Client | null = null

export interface StompSubscription {
  id: string
  unsubscribe: () => void
}

export function connect(onConnect?: () => void, onError?: (error: string) => void): Client {
  if (stompClient?.active) {
    onConnect?.()
    return stompClient
  }

  stompClient = new Client({
    webSocketFactory: () => new SockJS('/ws'),
    reconnectDelay: 5000,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
    onConnect: () => {
      console.log('[STOMP] Connected')
      onConnect?.()
    },
    onStompError: (frame) => {
      console.error('[STOMP] Error:', frame.headers['message'])
      onError?.(frame.headers['message'] || 'STOMP error')
    },
    onWebSocketClose: () => {
      console.warn('[STOMP] WebSocket closed, will auto-reconnect...')
    },
  })

  stompClient.activate()
  return stompClient
}

export function subscribeCCOutput(
  sessionId: string,
  callback: (msg: CCOutputMessage) => void,
): StompSubscription | null {
  if (!stompClient?.connected) {
    console.warn('[STOMP] Not connected, cannot subscribe to CC output')
    return null
  }

  const sub = stompClient.subscribe(
    `/topic/cc-sessions/${sessionId}/output`,
    (message) => {
      try {
        const parsed: CCOutputMessage = JSON.parse(message.body)
        callback(parsed)
      } catch (e) {
        console.error('[STOMP] Failed to parse CC output message:', e)
      }
    },
  )

  return { id: sub.id, unsubscribe: () => sub.unsubscribe() }
}

export function subscribeDevTaskStatus(
  taskId: string,
  callback: (msg: DevTaskStatusMessage) => void,
): StompSubscription | null {
  if (!stompClient?.connected) {
    console.warn('[STOMP] Not connected, cannot subscribe to task status')
    return null
  }

  const sub = stompClient.subscribe(
    `/topic/dev-tasks/${taskId}/status`,
    (message) => {
      try {
        const parsed: DevTaskStatusMessage = JSON.parse(message.body)
        callback(parsed)
      } catch (e) {
        console.error('[STOMP] Failed to parse task status message:', e)
      }
    },
  )

  return { id: sub.id, unsubscribe: () => sub.unsubscribe() }
}

export function disconnect(): void {
  if (stompClient) {
    stompClient.deactivate()
    stompClient = null
    console.log('[STOMP] Disconnected')
  }
}

export function isConnected(): boolean {
  return stompClient?.connected ?? false
}

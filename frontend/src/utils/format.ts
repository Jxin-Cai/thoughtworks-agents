import type { DevTaskStatus } from '@/types/api'

export function formatTime(isoStr: string | null | undefined): string {
  if (!isoStr) return '-'
  const d = new Date(isoStr)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

export function formatRelativeTime(isoStr: string): string {
  const now = Date.now()
  const then = new Date(isoStr).getTime()
  const diff = now - then
  const seconds = Math.floor(diff / 1000)
  const minutes = Math.floor(seconds / 60)
  const hours = Math.floor(minutes / 60)
  const days = Math.floor(hours / 24)

  if (seconds < 60) return '刚刚'
  if (minutes < 60) return `${minutes} 分钟前`
  if (hours < 24) return `${hours} 小时前`
  if (days < 30) return `${days} 天前`
  return formatTime(isoStr)
}

const STATUS_LABELS: Record<DevTaskStatus, string> = {
  CREATED: '已创建',
  THINKING: '思考中',
  WORKING: '工作中',
  READY_TO_PUBLISH: '待发布',
  PUBLISHING: '发布中',
  PUBLISHED: '已发布',
  FAILED: '失败',
}

export function statusLabel(status: DevTaskStatus): string {
  return STATUS_LABELS[status] || status
}

type ElTagType = '' | 'success' | 'warning' | 'danger' | 'info'

const STATUS_TYPES: Record<DevTaskStatus, ElTagType> = {
  CREATED: 'info',
  THINKING: 'warning',
  WORKING: '',
  READY_TO_PUBLISH: 'warning',
  PUBLISHING: '',
  PUBLISHED: 'success',
  FAILED: 'danger',
}

export function statusType(status: DevTaskStatus): ElTagType {
  return STATUS_TYPES[status] || 'info'
}

import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { DevTaskDTO, CCOutputMessage } from '@/types/api'
import * as devTaskApi from '@/api/devTask'
import type {
  CreateDevTaskRequest,
  StartDevelopmentRequest,
  AdvanceToWorkingRequest,
  ExecutePublishRequest,
} from '@/types/request'

export const useDevTaskStore = defineStore('devTask', () => {
  const currentTask = ref<DevTaskDTO | null>(null)
  const tasks = ref<DevTaskDTO[]>([])
  const ccOutput = ref<CCOutputMessage[]>([])
  const loading = ref(false)

  async function loadTask(id: string) {
    loading.value = true
    try {
      currentTask.value = await devTaskApi.getDevTask(id)
    } finally {
      loading.value = false
    }
  }

  async function loadTasks(conversationId?: string) {
    loading.value = true
    try {
      tasks.value = await devTaskApi.getDevTasks(conversationId)
    } finally {
      loading.value = false
    }
  }

  async function createTask(data: CreateDevTaskRequest): Promise<DevTaskDTO> {
    const task = await devTaskApi.createDevTask(data)
    tasks.value.unshift(task)
    currentTask.value = task
    return task
  }

  async function startDevelopment(taskId: string, data: StartDevelopmentRequest) {
    loading.value = true
    try {
      currentTask.value = await devTaskApi.startDevelopment(taskId, data)
    } finally {
      loading.value = false
    }
  }

  async function advanceToWorking(taskId: string, data: AdvanceToWorkingRequest) {
    loading.value = true
    try {
      currentTask.value = await devTaskApi.advanceToWorking(taskId, data)
    } finally {
      loading.value = false
    }
  }

  async function executePublish(taskId: string, data: ExecutePublishRequest) {
    loading.value = true
    try {
      currentTask.value = await devTaskApi.executePublish(taskId, data)
    } finally {
      loading.value = false
    }
  }

  function appendCCOutput(msg: CCOutputMessage) {
    ccOutput.value.push(msg)
  }

  function clearCCOutput() {
    ccOutput.value = []
  }

  function updateStatus(taskId: string, status: DevTaskDTO['status']) {
    if (currentTask.value?.id === taskId) {
      currentTask.value.status = status
    }
    const idx = tasks.value.findIndex((t) => t.id === taskId)
    if (idx !== -1) {
      tasks.value[idx].status = status
    }
  }

  return {
    currentTask,
    tasks,
    ccOutput,
    loading,
    loadTask,
    loadTasks,
    createTask,
    startDevelopment,
    advanceToWorking,
    executePublish,
    appendCCOutput,
    clearCCOutput,
    updateStatus,
  }
})

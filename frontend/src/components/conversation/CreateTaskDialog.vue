<template>
  <el-dialog
    v-model="visible"
    title="创建开发任务"
    width="480px"
    :close-on-click-modal="false"
    class="create-task-dialog"
  >
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-position="top"
      @submit.prevent="handleSubmit"
    >
      <el-form-item label="分支名称" prop="branchName">
        <el-input
          v-model="form.branchName"
          placeholder="例如: feature/my-feature"
          :prefix-icon="BranchIcon"
        />
      </el-form-item>

      <el-form-item label="需求描述" prop="requirement">
        <el-input
          v-model="form.requirement"
          type="textarea"
          :rows="4"
          placeholder="请描述需要实现的功能..."
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">
        创建任务
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Coin as BranchIcon } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import { useDevTaskStore } from '@/stores/devTask'

const props = defineProps<{
  modelValue: boolean
  conversationId: string
  repositoryFullName: string
}>()

const emit = defineEmits<{
  'update:modelValue': [val: boolean]
}>()

const router = useRouter()
const devTaskStore = useDevTaskStore()
const formRef = ref<FormInstance>()
const submitting = ref(false)

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val),
})

const form = reactive({
  branchName: '',
  requirement: '',
})

const rules: FormRules = {
  branchName: [{ required: true, message: '请输入分支名称', trigger: 'blur' }],
  requirement: [{ required: true, message: '请输入需求描述', trigger: 'blur' }],
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const task = await devTaskStore.createTask({
      conversationId: props.conversationId,
      repositoryFullName: props.repositoryFullName,
      branchName: form.branchName,
      requirement: form.requirement,
    })
    ElMessage.success('任务创建成功')
    visible.value = false
    router.push(`/tasks/${task.id}`)
  } catch (e: any) {
    ElMessage.error(e.message || '创建任务失败')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.create-task-dialog :deep(.el-dialog) {
  background: var(--bg-secondary);
  border: 1px solid var(--border-default);
  border-radius: var(--radius-lg);
}

.create-task-dialog :deep(.el-dialog__header) {
  border-bottom: 1px solid var(--border-subtle);
  padding-bottom: 16px;
}

.create-task-dialog :deep(.el-dialog__title) {
  color: var(--text-primary);
  font-weight: 600;
}
</style>

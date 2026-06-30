<template>
  <div class="page">
    <header class="page-top">
      <div>
        <h2>模型管理</h2>
        <p class="top-sub">版本注册、流量灰度、训练触发</p>
      </div>
      <el-button type="primary" size="large" @click="handleTrain" round>
        <el-icon><Cpu /></el-icon> 手动触发训练
      </el-button>
    </header>

    <div class="card">
      <div class="card-head">模型版本管理</div>
      <el-table :data="models" stripe v-loading="loading" class="model-table">
        <el-table-column prop="modelId" label="模型 ID" width="220" show-overflow-tooltip />
        <el-table-column prop="modelKey" label="模型标识" min-width="140" />
        <el-table-column prop="version" label="版本号" width="160" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <span class="status-tag" :class="'st-' + statusTag(row.status)">{{ row.status }}</span>
          </template>
        </el-table-column>
        <el-table-column label="流量配置" width="300">
          <template #default="{ row }">
            <div class="traffic-cell">
              <el-slider
                v-model="localTraffic[row.modelId]"
                :min="0" :max="100" :step="100"
                :show-tooltip="false"
                @change="(val: number) => handleTraffic(row.modelId, val)"
              />
              <span class="traffic-val">{{ localTraffic[row.modelId] ?? row.trafficPct }}%</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="artifactPath" label="模型路径" show-overflow-tooltip min-width="180" />
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">{{ row.createdAt?.replace('T', ' ') }}</template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && models.length === 0" description="暂无模型，请点击上方训练按钮创建" />
    </div>
    <div class="card task-card">
      <div class="card-head">训练任务记录</div>
      <el-table :data="trainingTasks" stripe v-loading="taskLoading" class="model-table">
        <el-table-column prop="taskId" label="任务 ID" width="220" show-overflow-tooltip />
        <el-table-column prop="modelKey" label="模型标识" min-width="140" />
        <el-table-column prop="modelType" label="模型类型" width="120" />
        <el-table-column label="关键超参" min-width="230" show-overflow-tooltip>
          <template #default="{ row }">{{ formatHyperParams(row.hyperParams) }}</template>
        </el-table-column>
        <el-table-column prop="datasetPath" label="数据集路径" min-width="180" show-overflow-tooltip />
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <span class="status-tag" :class="'st-' + taskStatusTag(row.status)">{{ row.status }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="modelId" label="产生模型" width="180" show-overflow-tooltip />
        <el-table-column prop="errorMessage" label="失败信息" min-width="180" show-overflow-tooltip />
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!taskLoading && trainingTasks.length === 0" description="暂无训练任务记录" />
    </div>

    <el-dialog
      v-model="trainDialogVisible"
      title="新建训练任务"
      width="620px"
      destroy-on-close
      class="train-dialog"
    >
      <el-form label-position="top" class="train-form">
        <div class="form-grid">
          <el-form-item label="模型标识">
            <el-input v-model="trainForm.modelKey" placeholder="medical-ct-unet" />
          </el-form-item>
          <el-form-item label="模型类型">
            <el-select v-model="trainForm.modelType" placeholder="请选择模型类型">
              <el-option label="U-Net" value="unet" />
              <el-option label="Attention U-Net" value="attention" />
            </el-select>
          </el-form-item>
          <el-form-item label="学习率">
            <el-input v-model="trainForm.learningRate" placeholder="0.0001" />
          </el-form-item>
          <el-form-item label="训练轮数">
            <el-input-number v-model="trainForm.epochs" :min="1" :max="1000" :step="1" controls-position="right" />
          </el-form-item>
          <el-form-item label="Batch size">
            <el-input-number v-model="trainForm.batchSize" :min="1" :max="128" :step="1" controls-position="right" />
          </el-form-item>
          <el-form-item label="优化器">
            <el-select v-model="trainForm.optimizer" placeholder="请选择优化器">
              <el-option label="AdamW" value="AdamW" />
              <el-option label="Adam" value="Adam" />
              <el-option label="SGD" value="SGD" />
            </el-select>
          </el-form-item>
        </div>
        <el-form-item label="数据集路径">
          <el-input v-model="trainForm.datasetPath" placeholder="/data/ct-artifact/" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="trainDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="training" @click="submitTrain">创建训练任务</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { triggerTraining, setModelTraffic, getModelList, getTrainingTasks } from '@/api/admin/ml'

const models = ref<any[]>([])
const trainingTasks = ref<any[]>([])
const loading = ref(false)
const taskLoading = ref(false)
const training = ref(false)
const trainDialogVisible = ref(false)
const localTraffic = reactive<Record<string, number>>({})
const trainForm = reactive({
  modelKey: 'medical-ct-unet',
  modelType: 'unet',
  learningRate: '0.0001',
  epochs: 100,
  batchSize: 8,
  optimizer: 'AdamW',
  datasetPath: '/data/ct-artifact/',
})

function statusTag(status: string) {
  const map: Record<string, string> = {
    ACTIVE: 'success',
    INACTIVE: 'info',
    '上线': 'success',
    '测试': 'warning',
    '下线': 'info',
    '废弃': 'danger',
  }
  return map[status] || 'info'
}

function taskStatusTag(status: string) {
  const map: Record<string, string> = { COMPLETED: 'success', RUNNING: 'warning', PENDING: 'info', FAILED: 'danger' }
  return map[status] || 'info'
}

function formatTime(value?: string) {
  return value ? value.replace('T', ' ') : '--'
}

function formatHyperParams(params?: Record<string, any>) {
  if (!params) return '--'
  return `lr=${params.learningRate}, epochs=${params.epochs}, batch=${params.batchSize}, opt=${params.optimizer}`
}

async function fetchModels() {
  loading.value = true
  try {
    const res = await getModelList()
    models.value = res.data || []
    models.value.forEach(m => { localTraffic[m.modelId] = m.trafficPct })
  } catch (e: any) {
    ElMessage.error(e?.message || '模型列表加载失败')
  } finally { loading.value = false }
}

async function fetchTrainingTasks() {
  taskLoading.value = true
  try {
    const res = await getTrainingTasks()
    trainingTasks.value = res.data?.tasks || []
  } catch (e: any) {
    ElMessage.error(e?.message || '训练任务加载失败')
  } finally { taskLoading.value = false }
}

function handleTrain() {
  trainDialogVisible.value = true
}

async function submitTrain() {
  const validationMessage = validateTrainForm()
  if (validationMessage) {
    ElMessage.warning(validationMessage)
    return
  }

  training.value = true
  try {
    const res = await triggerTraining({
      modelKey: trainForm.modelKey.trim(),
      modelType: trainForm.modelType,
      learningRate: trainForm.learningRate.trim(),
      epochs: String(trainForm.epochs),
      batchSize: String(trainForm.batchSize),
      optimizer: trainForm.optimizer,
      datasetPath: trainForm.datasetPath.trim(),
    })
    trainDialogVisible.value = false
    ElMessage.success(`训练任务已创建：${res.data.taskId}`)
    fetchModels()
    fetchTrainingTasks()
  } catch (e: any) {
    ElMessage.error(e?.message || '训练失败')
  } finally { training.value = false }
}

function validateTrainForm() {
  if (!trainForm.modelKey.trim()) return '模型标识不能为空'
  if (!trainForm.datasetPath.trim()) return '数据集路径不能为空'
  const learningRate = Number(trainForm.learningRate)
  if (!Number.isFinite(learningRate) || learningRate <= 0) return '学习率必须是大于 0 的数字'
  if (!Number.isInteger(trainForm.epochs) || trainForm.epochs <= 0) return '训练轮数必须是正整数'
  if (!Number.isInteger(trainForm.batchSize) || trainForm.batchSize <= 0) return 'Batch size 必须是正整数'
  return ''
}

async function handleTraffic(modelId: string, trafficPct: number) {
  try {
    await setModelTraffic({ modelId, trafficPct })
    ElMessage.success('流量配置已更新')
    fetchModels()
  } catch (e: any) {
    ElMessage.error(e?.message || '流量配置更新失败')
    fetchModels()
  }
}

fetchModels()
fetchTrainingTasks()
</script>

<style scoped>
.page { padding: 28px 36px; }
.page-top { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 28px; }
.page-top h2 { font-size: 22px; font-weight: 700; color: #0f172a; }
.top-sub { font-size: 13px; color: #94a3b8; margin-top: 4px; }

.card { background: #fff; border-radius: var(--radius); padding: 24px; box-shadow: var(--shadow); }
.task-card { margin-top: 24px; }
.card-head { font-size: 15px; font-weight: 700; color: #1e293b; margin-bottom: 18px; padding-bottom: 12px; border-bottom: 1px solid #f1f5f9; }

.model-table :deep(th) { background: #f8fafc; color: #64748b; font-weight: 600; font-size: 13px; border-bottom: none; }
.model-table :deep(td) { font-size: 14px; }

.status-tag { font-size: 12px; font-weight: 600; padding: 3px 10px; border-radius: 12px; }
.st-success { background: #d1fae5; color: #065f46; }
.st-warning { background: #fef3c7; color: #b45309; }
.st-info { background: #dbeafe; color: #1d4ed8; }
.st-danger { background: #fee2e2; color: #991b1b; }

.traffic-cell { display: flex; align-items: center; gap: 12px; }
.traffic-cell .el-slider { flex: 1; }
.traffic-val { font-size: 13px; font-weight: 700; color: #2563eb; min-width: 38px; text-align: right; }

.train-form { padding-top: 4px; }
.form-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 4px 18px; }
.train-form :deep(.el-select), .train-form :deep(.el-input-number) { width: 100%; }
</style>

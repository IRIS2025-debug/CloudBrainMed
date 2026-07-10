<template>
  <div class="page">
    <header class="page-top">
      <div class="page-top-left">
        <h2>检验医生工作台</h2>
        <p class="top-sub">管理您的检验任务，按优先级处理</p>
      </div>
      <div class="page-top-right">
        <el-button type="primary" @click="router.push('/inspection-doctor/queue')">
          <el-icon><List /></el-icon>查看队列
          <el-tag v-if="queueCount > 0" size="small" type="danger">{{ queueCount }}</el-tag>
        </el-button>
        <el-button :loading="loading" @click="fetchTasks">
          <el-icon><Refresh /></el-icon>刷新
        </el-button>
        <span class="auto-refresh-hint">自动刷新中 {{ countdown }}s</span>
      </div>
    </header>

    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-value">{{ stats.total }}</div>
        <div class="stat-label">全部任务</div>
      </div>
      <div class="stat-card sc-queued">
        <div class="stat-value">{{ stats.queued }}</div>
        <div class="stat-label">队列待分配</div>
      </div>
      <div class="stat-card sc-process">
        <div class="stat-value">{{ stats.inProcess }}</div>
        <div class="stat-label">处理中</div>
      </div>
      <div class="stat-card sc-emergency">
        <div class="stat-value">{{ stats.emergency }}</div>
        <div class="stat-label">紧急/加急</div>
      </div>
    </div>

    <div class="card">
      <el-table
        v-loading="loading"
        :data="tasks"
        stripe
        empty-text="暂无待处理检验任务"
        @sort-change="onSortChange"
      >
        <el-table-column label="紧急程度" width="100">
          <template #default="{ row }">
            <span
              class="badge"
              :class="['badge-' + row.urgencyLevel, { 'badge-aged': row.agingPromoted }]"
              :title="row.agingPromoted ? '常规任务等待超30分钟，已自动提升优先级' : ''"
            >
              {{ row.urgencyLabel || urgencyLabel(row.urgencyLevel) }}
              <span v-if="row.agingPromoted" class="aged-icon">↑</span>
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="itemName" label="检验项目" min-width="170" show-overflow-tooltip />
        <el-table-column label="患者" width="140">
          <template #default="{ row }">
            <div class="patient-cell">
              <div class="pc-avatar">{{ row.patientName?.charAt(0) || '?' }}</div>
              <div>
                <div class="pc-name">{{ row.patientName || '-' }}</div>
                <div class="pc-meta">{{ genderLabel(row.gender) }} · {{ row.age ?? '-' }}岁</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <span class="status-tag" :class="'st-' + row.status">{{ row.statusLabel || '处理中' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="等待时间" width="120" sortable="custom" prop="waitingMinutes">
          <template #default="{ row }">
            <span :class="{ 'wait-long': (row.waitingMinutes || 0) > 30 }">
              {{ formatWaiting(row.waitingMinutes) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="assignTime" label="分配时间" width="170" />
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="openTask(row)">查看详情</el-button>
            <el-button type="success" size="small" @click="openReport(row)">填写报告</el-button>
            <el-button type="info" size="small" @click="handleSkip(row)">跳过</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { List, Refresh } from '@element-plus/icons-vue'
import { getQueue, getWorkbench, skipTask } from '@/api/doctor/task'

interface WorkbenchTask {
  orderItemId: string
  itemName: string
  status: string
  statusLabel?: string
  urgencyLevel: string
  urgencyLabel?: string
  agingPromoted?: boolean
  patientName?: string
  gender?: number
  age?: number
  waitingMinutes?: number
  assignTime?: string
}

const router = useRouter()
const assignedTasks = ref<WorkbenchTask[]>([])
const tasks = computed(() => assignedTasks.value.filter(task => task.status === 'IN_PROCESS'))
const queueCount = ref(0)
const loading = ref(false)
const countdown = ref(30)
let refreshTimer: ReturnType<typeof setInterval> | undefined
let countdownTimer: ReturnType<typeof setInterval> | undefined

const stats = computed(() => ({
  total: tasks.value.length + queueCount.value,
  queued: queueCount.value,
  inProcess: tasks.value.length,
  emergency: tasks.value.filter(task =>
    ['EMERGENCY', 'URGENT'].includes(task.urgencyLevel) || task.agingPromoted
  ).length,
}))

onMounted(() => {
  fetchTasks()
  refreshTimer = setInterval(() => {
    fetchTasks()
    countdown.value = 30
  }, 30000)
  countdownTimer = setInterval(() => {
    if (countdown.value > 0) countdown.value--
  }, 1000)
})

onUnmounted(() => {
  if (refreshTimer) clearInterval(refreshTimer)
  if (countdownTimer) clearInterval(countdownTimer)
})

async function fetchTasks() {
  loading.value = true
  try {
    const [workbenchResponse, queueResponse] = await Promise.all([getWorkbench(), getQueue()])
    assignedTasks.value = workbenchResponse.data || []
    queueCount.value = queueResponse.data?.queueCount || 0
  } catch {
    ElMessage.error('加载任务列表失败')
  } finally {
    loading.value = false
  }
}

function openTask(task: WorkbenchTask) {
  sessionStorage.setItem('current_order_item_id', task.orderItemId)
  router.push(`/doctor/task/${task.orderItemId}`)
}

function openReport(task: WorkbenchTask) {
  sessionStorage.setItem('current_order_item_id', task.orderItemId)
  router.push({ path: '/inspection-doctor/report', query: { orderItemId: task.orderItemId } })
}

async function handleSkip(task: WorkbenchTask) {
  try {
    await ElMessageBox.confirm(
      `确定跳过「${task.itemName}」？该任务将放回队列，由其他医生处理。`,
      '跳过任务',
      { type: 'warning', confirmButtonText: '确定跳过', cancelButtonText: '取消' }
    )
    await skipTask(task.orderItemId)
    ElMessage.success('已跳过，任务已放回队列')
    await fetchTasks()
  } catch (error: any) {
    if (error !== 'cancel') ElMessage.error(error?.response?.data?.msg || '操作失败')
  }
}

function onSortChange({ prop, order }: { prop: string; order: string | null }) {
  if (prop !== 'waitingMinutes' || !order) return
  assignedTasks.value = [...assignedTasks.value].sort((left, right) => {
    const diff = (left.waitingMinutes || 0) - (right.waitingMinutes || 0)
    return order === 'ascending' ? diff : -diff
  })
}

function urgencyLabel(level: string) {
  return { EMERGENCY: '紧急', URGENT: '加急', NORMAL: '常规' }[level] || level || '-'
}

function genderLabel(gender?: number) {
  if (gender === 1) return '男'
  if (gender === 0 || gender === 2) return '女'
  return '未知'
}

function formatWaiting(minutes?: number) {
  if (minutes === undefined || minutes === null) return '-'
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟`
  return `${Math.floor(minutes / 60)}小时${minutes % 60}分钟`
}
</script>

<style scoped>
.page { padding: 24px; }
.page-top { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 16px; gap: 16px; }
.page-top-left h2 { margin: 0 0 4px; font-size: 20px; }
.top-sub { color: #999; font-size: 13px; margin: 0; }
.page-top-right { display: flex; gap: 8px; align-items: center; }
.page-top-right .el-icon { margin-right: 4px; }
.page-top-right .el-tag { margin-left: 6px; }
.auto-refresh-hint { font-size: 12px; color: #999; white-space: nowrap; }
.card { background: #fff; border-radius: 8px; padding: 20px; }

.stats-row { display: flex; gap: 12px; margin-bottom: 16px; }
.stat-card { background: #fff; border-radius: 8px; padding: 12px 20px; flex: 1; box-shadow: 0 1px 3px rgba(0,0,0,.06); }
.stat-value { font-size: 28px; font-weight: 700; color: #1e293b; }
.stat-label { font-size: 12px; color: #999; margin-top: 2px; }
.sc-queued .stat-value { color: #409eff; }
.sc-process .stat-value { color: #e6a23c; }
.sc-emergency .stat-value { color: #f56c6c; }

.badge { display: inline-flex; align-items: center; gap: 2px; padding: 2px 8px; border-radius: 4px; font-size: 12px; }
.badge-EMERGENCY { background: #fef0f0; color: #f56c6c; }
.badge-URGENT { background: #fdf6ec; color: #e6a23c; }
.badge-NORMAL { background: #f0f9eb; color: #67c23a; }
.badge-aged { background: #fef4e6; color: #d46b08; border: 1px solid #ffd591; }
.aged-icon { font-size: 11px; }

.status-tag { display: inline-block; padding: 2px 8px; border-radius: 4px; font-size: 12px; }
.st-IN_PROCESS { background: #fdf6ec; color: #e6a23c; }
.wait-long { color: #f56c6c; font-weight: 600; }
.patient-cell { display: flex; align-items: center; gap: 8px; }
.pc-avatar { width: 32px; height: 32px; border-radius: 50%; background: #409eff; color: #fff; display: flex; align-items: center; justify-content: center; font-size: 14px; flex-shrink: 0; }
.pc-name { font-size: 13px; }
.pc-meta { font-size: 11px; color: #999; }

@media (max-width: 760px) {
  .page { padding: 16px; }
  .page-top { flex-direction: column; }
  .page-top-right { width: 100%; flex-wrap: wrap; }
  .stats-row { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .card { padding: 12px; overflow-x: auto; }
}
</style>

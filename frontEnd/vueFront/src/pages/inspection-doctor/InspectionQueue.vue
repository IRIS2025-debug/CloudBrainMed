<template>
  <div class="page">
    <header class="page-top">
      <div class="page-top-left">
        <h2>检验队列</h2>
        <p class="top-sub">
          当前排队检验任务：<strong>{{ queueCount }}</strong> 个
          | 按紧急程度和时间排序
          <el-tag v-if="agedCount > 0" size="small" type="warning">
            {{ agedCount }}个任务已触发老化提升
          </el-tag>
        </p>
      </div>
      <div class="page-top-right">
        <el-button type="primary" @click="router.push('/inspection-doctor/workbench')">
          <el-icon><Monitor /></el-icon>我的工作台
        </el-button>
        <el-button :loading="loading" @click="fetchQueue">
          <el-icon><Refresh /></el-icon>刷新
        </el-button>
        <span class="auto-refresh-hint">自动刷新 {{ countdown }}s</span>
      </div>
    </header>

    <div class="card">
      <el-table
        v-loading="loading"
        :data="tasks"
        stripe
        empty-text="当前没有排队检验任务"
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
            <span class="status-tag st-QUEUED">{{ row.statusLabel || '排队中' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="等待时间" width="120" sortable="custom" prop="waitingMinutes">
          <template #default="{ row }">
            <span :class="{ 'wait-long': (row.waitingMinutes || 0) > 30 }">
              {{ formatWaiting(row.waitingMinutes) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Monitor, Refresh } from '@element-plus/icons-vue'
import { getQueue } from '@/api/doctor/task'

interface QueueTask {
  orderItemId: string
  itemName: string
  status?: string
  statusLabel?: string
  urgencyLevel: string
  urgencyLabel?: string
  agingPromoted?: boolean
  patientName?: string
  gender?: number
  age?: number
  waitingMinutes?: number
  createTime?: string
}

const router = useRouter()
const tasks = ref<QueueTask[]>([])
const queueCount = ref(0)
const loading = ref(false)
const countdown = ref(30)
let refreshTimer: ReturnType<typeof setInterval> | undefined
let countdownTimer: ReturnType<typeof setInterval> | undefined

const agedCount = computed(() => tasks.value.filter(task => task.agingPromoted).length)

onMounted(() => {
  fetchQueue()
  refreshTimer = setInterval(() => {
    fetchQueue()
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

async function fetchQueue() {
  loading.value = true
  try {
    const response = await getQueue()
    tasks.value = response.data?.tasks || []
    queueCount.value = response.data?.queueCount || 0
  } catch {
    ElMessage.error('加载队列失败')
  } finally {
    loading.value = false
  }
}

function onSortChange({ prop, order }: { prop: string; order: string | null }) {
  if (prop !== 'waitingMinutes' || !order) return
  tasks.value = [...tasks.value].sort((left, right) => {
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
.top-sub .el-tag { margin-left: 8px; }
.page-top-right { display: flex; gap: 8px; align-items: center; }
.page-top-right .el-icon { margin-right: 4px; }
.auto-refresh-hint { font-size: 12px; color: #999; white-space: nowrap; }
.card { background: #fff; border-radius: 8px; padding: 20px; }
.badge { display: inline-flex; align-items: center; gap: 2px; padding: 2px 8px; border-radius: 4px; font-size: 12px; }
.badge-EMERGENCY { background: #fef0f0; color: #f56c6c; }
.badge-URGENT { background: #fdf6ec; color: #e6a23c; }
.badge-NORMAL { background: #f0f9eb; color: #67c23a; }
.badge-aged { background: #fef4e6; color: #d46b08; border: 1px solid #ffd591; }
.aged-icon { font-size: 11px; }
.status-tag { display: inline-block; padding: 2px 8px; border-radius: 4px; font-size: 12px; }
.st-QUEUED { background: #ecf5ff; color: #409eff; }
.wait-long { color: #f56c6c; font-weight: 600; }
.patient-cell { display: flex; align-items: center; gap: 8px; }
.pc-avatar { width: 32px; height: 32px; border-radius: 50%; background: #409eff; color: #fff; display: flex; align-items: center; justify-content: center; font-size: 14px; flex-shrink: 0; }
.pc-name { font-size: 13px; }
.pc-meta { font-size: 11px; color: #999; }

@media (max-width: 760px) {
  .page { padding: 16px; }
  .page-top { flex-direction: column; }
  .page-top-right { width: 100%; flex-wrap: wrap; }
  .card { padding: 12px; overflow-x: auto; }
}
</style>

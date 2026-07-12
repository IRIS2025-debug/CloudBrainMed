<template>
  <div class="page">
    <header class="page-top">
      <div class="page-top-left">
        <h2>检查医生工作台</h2>
        <p class="top-sub">管理您的影像检查任务，按优先级处理</p>
      </div>
      <div class="page-top-right">
        <el-button type="primary" @click="$router.push('/examination-doctor/queue')">
          <el-icon style="margin-right:4px"><List /></el-icon>查看队列
          <el-tag size="small" type="danger" style="margin-left:6px" v-if="queueCount > 0">{{ queueCount }}</el-tag>
        </el-button>
        <el-button @click="fetchTasks" :loading="loading">
          <el-icon style="margin-right:4px"><Refresh /></el-icon>刷新
        </el-button>
        <span class="auto-refresh-hint" v-if="autoRefresh">自动刷新中 {{ countdown }}s</span>
      </div>
    </header>

    <!-- 统计卡片 -->
    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-value">{{ stats.total }}</div>
        <div class="stat-label">全部任务</div>
      </div>
      <div class="stat-card sc-queued">
        <div class="stat-value">{{ stats.queued }}</div>
        <div class="stat-label">排队中</div>
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
      <el-table :data="tasks" stripe v-loading="loading" empty-text="暂无待处理检查任务" @sort-change="onSortChange">
        <el-table-column label="紧急程度" width="90">
          <template #default="{ row }">
            <span class="badge" :class="['badge-' + row.urgencyLevel, { 'badge-aged': row.agingPromoted }]"
                  :title="row.agingPromoted ? '常规任务等待超30分钟，已自动提升优先级' : ''">
              {{ row.urgencyLabel }}
              <span v-if="row.agingPromoted" class="aged-icon">⏫</span>
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="itemName" label="检查项目" min-width="160" show-overflow-tooltip />
        <el-table-column label="患者" width="130">
          <template #default="{ row }">
            <div class="patient-cell">
              <div class="pc-avatar">{{ row.patientName?.charAt(0) || '?' }}</div>
              <div>
                <div class="pc-name">{{ row.patientName }}</div>
                <div class="pc-meta">{{ row.gender === 1 ? '男' : '女' }} · {{ row.age }}岁</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <span class="status-tag" :class="'st-' + row.status">{{ row.statusLabel }}</span>
          </template>
        </el-table-column>
        <el-table-column label="等待时间" width="120" sortable="custom" prop="waitingMinutes">
          <template #default="{ row }">
            <span :class="{ 'wait-long': row.waitingMinutes > 30 }">
              {{ formatWaiting(row.waitingMinutes) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="160" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 'IN_PROCESS'" type="success" size="small" @click="handleView(row)">
              确认检查
            </el-button>
            <el-button v-if="row.status === 'IN_PROCESS'" type="info" size="small" @click="handleSkip(row)">
              跳过
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { getWorkbench, startTask, skipTask, getQueue } from '@/api/doctor/task'
import { ElMessage, ElMessageBox } from 'element-plus'
import { List, Refresh } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const tasks = ref<any[]>([])
const loading = ref(false)
const autoRefresh = ref(true)
const countdown = ref(30)
let timer: ReturnType<typeof setInterval> | null = null
let countdownTimer: ReturnType<typeof setInterval> | null = null
const queueCount = ref(0)

const stats = computed(() => {
  const list = tasks.value || []
  return {
    total: list.length,
    queued: list.filter((t: any) => t.status === 'QUEUED').length,
    inProcess: list.filter((t: any) => t.status === 'IN_PROCESS').length,
    emergency: list.filter((t: any) => t.urgencyLevel === 'EMERGENCY' || t.urgencyLevel === 'URGENT' || t.agingPromoted).length
  }
})

onMounted(() => {
  fetchTasks()
  startAutoRefresh()
})

onUnmounted(() => {
  stopAutoRefresh()
})

// 新增：查看任务，存入orderItemId
function handleView(row: any) {
  sessionStorage.setItem('current_order_item_id', row.orderItemId)
  router.push(`/doctor/task/${row.orderItemId}`)
}

function startAutoRefresh() {
  stopAutoRefresh()
  timer = setInterval(() => {
    fetchTasks()
    countdown.value = 30
  }, 30000)
  countdownTimer = setInterval(() => {
    if (countdown.value > 0) countdown.value--
  }, 1000)
}

function stopAutoRefresh() {
  if (timer) { clearInterval(timer); timer = null }
  if (countdownTimer) { clearInterval(countdownTimer); countdownTimer = null }
}

function formatWaiting(minutes: number | null | undefined): string {
  if (minutes === null || minutes === undefined) return '-'
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟`
  const h = Math.floor(minutes / 60)
  const m = minutes % 60
  return `${h}小时${m}分钟`
}

async function fetchTasks() {
  loading.value = true
  try {
    const [workbenchRes, queueRes] = await Promise.all([getWorkbench(), getQueue()])
    tasks.value = (workbenchRes.data || []).filter((t: any) => t.status !== 'COMPLETED')
    queueCount.value = queueRes.data?.queueCount || 0
  } catch {
    ElMessage.error('加载任务列表失败')
  } finally {
    loading.value = false
  }
}

function onSortChange({ prop, order }: { prop: string, order: string }) {
  if (prop === 'waitingMinutes') {
    const list = [...tasks.value]
    list.sort((a, b) => {
      const diff = (a.waitingMinutes || 0) - (b.waitingMinutes || 0)
      return order === 'ascending' ? diff : -diff
    })
    tasks.value = list
  }
}

async function handleSkip(row: any) {
  try {
    await ElMessageBox.confirm(
      `确定跳过「${row.itemName}」？该任务将放回队列，由其他医生处理。`,
      '跳过任务',
      { type: 'warning', confirmButtonText: '确定跳过', cancelButtonText: '取消' }
    )
    await skipTask(row.orderItemId)
    ElMessage.success('已跳过，任务已放回队列')
    fetchTasks()
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error(e?.response?.data?.msg || '操作失败')
  }
}
</script>

<style scoped>
.page { padding: 24px; }
.page-top { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 16px; }
.page-top-left h2 { margin: 0 0 4px; font-size: 20px; }
.top-sub { color: #999; font-size: 13px; margin: 0; }
.page-top-right { display: flex; gap: 8px; align-items: center; }
.auto-refresh-hint { font-size: 12px; color: #999; }
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
.st-QUEUED { background: #ecf5ff; color: #409eff; }
.st-IN_PROCESS { background: #fdf6ec; color: #e6a23c; }
.st-COMPLETED { background: #f0f9eb; color: #67c23a; }
.st-WAITING_ASSIGN { background: #f4f4f5; color: #909399; }
.wait-long { color: #f56c6c; font-weight: 600; }

.patient-cell { display: flex; align-items: center; gap: 8px; }
.pc-avatar { width: 32px; height: 32px; border-radius: 50%; background: #409eff; color: #fff; display: flex; align-items: center; justify-content: center; font-size: 14px; }
.pc-name { font-size: 13px; }
.pc-meta { font-size: 11px; color: #999; }
</style>
<template>
  <div class="page">
    <header class="page-top">
      <div class="page-top-left">
        <h2>检查队列</h2>
        <p class="top-sub">
          当前排队检查任务：<strong>{{ queueCount }}</strong> 个
          | 按紧急程度和时间排序
          <el-tag size="small" type="warning" style="margin-left:8px" v-if="agedCount > 0">
            {{ agedCount }}个任务已触发老化提升
          </el-tag>
        </p>
      </div>
      <div class="page-top-right">
        <el-button type="primary" @click="$router.push('/examination-doctor/workbench')">
          <el-icon style="margin-right:4px"><Monitor /></el-icon>我的工作台
        </el-button>
        <el-button @click="fetchQueue" :loading="loading">
          <el-icon style="margin-right:4px"><Refresh /></el-icon>刷新
        </el-button>
        <span class="auto-refresh-hint" v-if="autoRefresh">自动刷新 {{ countdown }}s</span>
      </div>
    </header>

    <div class="card">
      <el-table :data="tasks" stripe v-loading="loading" empty-text="当前没有排队检查任务" @sort-change="onSortChange">
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
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleStart(row)">领取任务</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { getQueue, startTask } from '@/api/doctor/task'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Monitor, Refresh } from '@element-plus/icons-vue'

const tasks = ref<any[]>([])
const loading = ref(false)
const queueCount = ref(0)
const autoRefresh = ref(true)
const countdown = ref(30)
let timer: ReturnType<typeof setInterval> | null = null
let countdownTimer: ReturnType<typeof setInterval> | null = null

const agedCount = computed(() => (tasks.value || []).filter((t: any) => t.agingPromoted).length)

onMounted(() => {
  fetchQueue()
  startAutoRefresh()
})

onUnmounted(() => {
  stopAutoRefresh()
})

function startAutoRefresh() {
  stopAutoRefresh()
  timer = setInterval(() => {
    fetchQueue()
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

async function fetchQueue() {
  loading.value = true
  try {
    const res = await getQueue()
    tasks.value = res.data?.tasks || []
    queueCount.value = res.data?.queueCount || 0
  } catch {
    ElMessage.error('加载队列失败')
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

async function handleStart(row: any) {
  try {
    await ElMessageBox.confirm(`确认领取「${row.itemName}」？`, '领取任务', { type: 'info' })
    await startTask(row.orderItemId)
    ElMessage.success('任务领取成功，请前往工作台处理')
    fetchQueue()
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
.pc-avatar { width: 32px; height: 32px; border-radius: 50%; background: #409eff; color: #fff; display: flex; align-items: center; justify-content: center; font-size: 14px; }
.pc-name { font-size: 13px; }
.pc-meta { font-size: 11px; color: #999; }
</style>
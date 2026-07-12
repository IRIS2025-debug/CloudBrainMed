<template>
  <OverviewBoard
    :title="board.title"
    :subtitle="board.subtitle"
    :updated-at="updatedAt"
    :loading="loading"
    :error="error"
    :stats="board.stats"
    :modules="board.modules"
    :activities="activities"
    :recent-title="board.recentTitle"
    @refresh="loadData"
    @navigate="onNavigate"
  />
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowRight, Calendar, Check, CollectionTag, DataAnalysis, List, Loading, Monitor, UserFilled } from '@element-plus/icons-vue'
import OverviewBoard, { type OverviewActivity, type OverviewModule, type OverviewStat } from '@/components/OverviewBoard.vue'
import { getConsultOverview } from '@/api/doctor/consult'
import { getQueue, getWorkbench } from '@/api/doctor/task'

// 三类医生共用同一首页入口，由 doctorType 决定展示内容：1 接诊 / 2 检查 / 3 检验。
const router = useRouter()
const doctorType = computed(() => Number(sessionStorage.getItem('doctorType') || '1'))

const loading = ref(false)
const error = ref(false)
const updatedAt = ref('')
const activities = ref<OverviewActivity[]>([])

// 指标卡的值：null 表示加载中或失败（组件显示 --），数字 0 会照常显示 0。
const metrics = ref<Record<string, number | null>>({
  a: null, b: null, c: null, d: null,
})

// ==================== 接诊医生（type=1） ====================
const consultStatusText: Record<string, string> = {
  PENDING: '待接诊',
  IN_PROGRESS: '接诊中',
  RECORD_CONFIRMED: '待完成',
  COMPLETED: '已完成',
}

// ==================== 检查/检验医生（type=2/3） ====================
const urgencyText: Record<string, string> = {
  EMERGENCY: '紧急',
  URGENT: '加急',
  NORMAL: '常规',
}

const board = computed(() => {
  const dt = doctorType.value
  if (dt === 2) {
    return {
      title: '检查医生首页',
      subtitle: '影像检查任务概览',
      recentTitle: '最近检查任务',
      stats: taskStats(),
      modules: examModules,
    }
  }
  if (dt === 3) {
    return {
      title: '检验医生首页',
      subtitle: '检验申请任务概览',
      recentTitle: '最近检验任务',
      stats: taskStats(),
      modules: labModules,
    }
  }
  return {
    title: '接诊医生首页',
    subtitle: '今日接诊工作概览',
    recentTitle: '今日最近接诊',
    stats: consultStats(),
    modules: consultModules,
  }
})

function consultStats(): OverviewStat[] {
  return [
    { label: '今日接诊', value: metrics.value.a, icon: UserFilled, color: '#2563eb' },
    { label: '待接诊', value: metrics.value.b, icon: Calendar, color: '#9c27b0' },
    { label: '接诊中', value: metrics.value.c, icon: Loading, color: '#0d9488' },
    { label: '今日完成', value: metrics.value.d, icon: Check, color: '#22c55e' },
  ]
}

function taskStats(): OverviewStat[] {
  return [
    { label: '待领取', value: metrics.value.a, icon: List, color: '#2563eb' },
    { label: '处理中', value: metrics.value.b, icon: Loading, color: '#0d9488' },
    { label: '累计完成', value: metrics.value.c, icon: Check, color: '#22c55e' },
    { label: '加急/紧急', value: metrics.value.d, icon: CollectionTag, color: '#f59e0b' },
  ]
}

const consultModules: OverviewModule[] = [
  { path: '/doctor/profile', title: '医生个人信息', desc: '查看编辑资料、头像上传、密码修改', icon: UserFilled, color: '#2563eb' },
  { path: '/doctor/consult', title: '接诊工作台', desc: '患者接诊列表、病历编辑、检查申请', icon: List, color: '#0d9488' },
  { path: '/doctor/schedule', title: '值班查询', desc: '查看医生排班安排', icon: Calendar, color: '#7c3aed' },
]

const examModules: OverviewModule[] = [
  { path: '/examination-doctor/workbench', title: '检查医生工作台', desc: '处理已分配的影像检查任务', icon: Monitor, color: '#2563eb' },
  { path: '/examination-doctor/queue', title: '检查队列', desc: '查看技能范围内待分配的检查项目', icon: List, color: '#0d9488' },
  { path: '/examination-doctor/ct-inference', title: 'CT 模型推理', desc: '上传 CT 影像运行 AI 推理', icon: DataAnalysis, color: '#7c3aed' },
  { path: '/examination-doctor/report', title: '生成检查报告', desc: '为处理中任务填写并回传检查报告', icon: CollectionTag, color: '#b45309' },
  { path: '/doctor/profile', title: '医生个人信息', desc: '查看编辑资料、头像上传、密码修改', icon: UserFilled, color: '#0f766e' },
  { path: '/doctor/schedule', title: '值班查询', desc: '查看医生排班安排', icon: Calendar, color: '#2563eb' },
]

const labModules: OverviewModule[] = [
  { path: '/inspection-doctor/workbench', title: '检验医生工作台', desc: '处理已分配的检验任务', icon: Monitor, color: '#2563eb' },
  { path: '/inspection-doctor/queue', title: '检验队列', desc: '查看技能范围内待分配的检验项目', icon: List, color: '#0d9488' },
  { path: '/inspection-doctor/report', title: '生成检验报告', desc: '为处理中任务填写并回传检验报告', icon: CollectionTag, color: '#b45309' },
  { path: '/doctor/profile', title: '医生个人信息', desc: '查看编辑资料、头像上传、密码修改', icon: UserFilled, color: '#0f766e' },
  { path: '/doctor/schedule', title: '值班查询', desc: '查看医生排班安排', icon: Calendar, color: '#2563eb' },
]

function onNavigate(path: string) {
  router.push(path)
}

async function loadData() {
  loading.value = true
  error.value = false
  metrics.value = { a: null, b: null, c: null, d: null }
  try {
    if (doctorType.value === 1) {
      await loadConsultOverview()
    } else {
      await loadTaskOverview()
    }
    updatedAt.value = new Date().toLocaleTimeString('zh-CN')
  } catch {
    // 失败时保留指标 --，底部展示重试入口，不把失败伪装成零。
    error.value = true
    activities.value = []
  } finally {
    loading.value = false
  }
}

async function loadConsultOverview() {
  const res = await getConsultOverview()
  const data = res.data || ({} as any)
  // 契约字段缺失或非法时保留 null（显示 --），不伪装成 0；真实的 0 会照常显示。
  metrics.value = {
    a: numOrNull(data.todayTotal),
    b: numOrNull(data.pendingCount),
    c: numOrNull(data.inProgressCount),
    d: numOrNull(data.completedTodayCount),
  }
  const recent = Array.isArray(data.recentConsults) ? data.recentConsults : []
  activities.value = recent.slice(0, 5).map((r: any) => ({
    id: r.registerId,
    text: `${r.name || '患者'} · ${r.chiefComplaint || r.department || '接诊'}`,
    meta: consultStatusText[r.consultStatus] || r.consultStatus,
    time: r.consultTime || '',
    tone: r.consultStatus === 'COMPLETED' ? 'success' : 'info',
  }))
}

async function loadTaskOverview() {
  // 只复用现有只读接口：task/workbench（我的任务数组）与 task/queue（{tasks, queueCount}）。
  const [workbenchRes, queueRes] = await Promise.all([getWorkbench(), getQueue()])
  const workbench: any[] = Array.isArray(workbenchRes.data) ? workbenchRes.data : []
  const queueTasks: any[] = queueRes.data?.tasks || []
  const queueCount: number = queueRes.data?.queueCount ?? queueTasks.length

  const inProcess = workbench.filter((t) => t.status === 'IN_PROCESS').length
  const completed = workbench.filter((t) => t.status === 'COMPLETED').length
  // 加急/紧急只统计「未完成」任务：工作台 SQL 不按状态过滤，会返回历史已完成任务，
  // 若不排除 COMPLETED，已完成的紧急任务会长期计入当前紧急数。
  const urgent = [...workbench, ...queueTasks].filter(
    (t) => t.status !== 'COMPLETED'
      && (t.urgencyLevel === 'EMERGENCY' || t.urgencyLevel === 'URGENT' || t.agingPromoted),
  ).length

  metrics.value = {
    a: queueCount,
    b: inProcess,
    c: completed,
    d: urgent,
  }

  // 合并工作台与队列，按 orderItemId 去重，按创建时间倒序，最多 5 条。
  const merged = new Map<string, any>()
  for (const t of [...workbench, ...queueTasks]) {
    if (t.orderItemId && !merged.has(t.orderItemId)) merged.set(t.orderItemId, t)
  }
  const list = [...merged.values()].sort(
    (a, b) => String(b.createTime || '').localeCompare(String(a.createTime || '')),
  )
  activities.value = list.slice(0, 5).map((t: any) => ({
    id: t.orderItemId,
    text: `${t.patientName || '患者'} · ${t.itemName || '任务'}`,
    meta: `${urgencyText[t.urgencyLevel] || t.urgencyLevel || ''}${t.statusLabel ? ' · ' + t.statusLabel : ''}`,
    time: t.createTime || '',
    tone: t.urgencyLevel === 'EMERGENCY' || t.urgencyLevel === 'URGENT' ? 'warning' : 'info',
  }))
}

// null/undefined/空串/非数字 → null（显示 --）；数字 0 保留为 0。
function numOrNull(value: unknown): number | null {
  if (value === null || value === undefined || value === '') return null
  const n = Number(value)
  return Number.isFinite(n) ? n : null
}

onMounted(loadData)
</script>

<template>
  <div class="page">
    <header class="hero">
      <div class="hero-copy">
        <p class="hero-eyebrow">检查医生工作台</p>
        <h1>今日检查任务</h1>
        <p class="hero-desc">处理影像申请、CT 推理和报告回传。</p>
        <div class="hero-tags">
          <span>{{ currentDate }}</span>
          <span>{{ pendingCount }} 待处理</span>
          <span>{{ stats.aiReports }} 份 AI 报告</span>
        </div>
      </div>
      <div class="hero-board">
        <div class="hero-stat" v-for="stat in heroStats" :key="stat.label">
          <div class="hero-stat-icon" :style="{ background: stat.color }">
            <el-icon :size="20"><component :is="stat.icon" /></el-icon>
          </div>
          <div>
            <strong>{{ stat.value }}</strong>
            <span>{{ stat.label }}</span>
          </div>
        </div>
      </div>
    </header>

    <section class="quick-grid">
      <button
        v-for="module in modules"
        :key="module.path"
        class="quick-card"
        :class="{ primary: module.primary }"
        type="button"
        @click="navigateTo(module.path)"
      >
        <div class="quick-icon" :class="module.colorClass">
          <el-icon :size="24"><component :is="module.icon" /></el-icon>
        </div>
        <div class="quick-body">
          <h3>{{ module.title }}</h3>
          <p>{{ module.desc }}</p>
        </div>
        <span class="quick-meta">{{ module.meta }}</span>
        <el-icon class="quick-arrow" color="#94a3b8"><ArrowRight /></el-icon>
      </button>
    </section>

    <section class="content-grid">
      <article class="panel activity-panel">
        <div class="panel-head">
          <div>
            <p class="panel-kicker">最近动态</p>
            <h2>工作流进展</h2>
          </div>
          <button class="link-btn" type="button" @click="navigateTo('/examination-doctor/application')">
            查看申请
          </button>
        </div>
        <ul class="activity-list">
          <li v-for="activity in recentActivities" :key="activity.id">
            <div class="activity-dot" :style="{ background: activity.color }"></div>
            <div class="activity-main">
              <strong>{{ activity.content }}</strong>
              <span>{{ activity.detail }}</span>
            </div>
            <time>{{ activity.time }}</time>
          </li>
        </ul>
      </article>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowRight, Check, DataAnalysis, Document, UserFilled } from '@element-plus/icons-vue'
import { getInspectionOrderList } from '@/api/inspection-doctor'

const router = useRouter()

interface InspectionOrderVo {
  itemCategory: string
  itemCode?: string
  itemName?: string
  status: string
  urgencyLevel?: string
  canViewReport?: boolean
  createTime?: string
}

const currentDate = ref('')
const orders = ref<InspectionOrderVo[]>([])
const stats = ref({
  totalExaminations: 0,
  completedExaminations: 0,
  pendingExaminations: 0,
  aiReports: 0,
})
const examOrders = computed(() => orders.value.filter((item) => item.itemCategory === 'EXAM'))
const pendingCount = computed(() => stats.value.pendingExaminations)

const heroStats = computed(() => [
  { label: '待处理申请', value: stats.value.pendingExaminations, icon: Document, color: '#2563eb' },
  { label: '今日完成', value: stats.value.completedExaminations, icon: Check, color: '#22c55e' },
  { label: 'AI 报告', value: stats.value.aiReports, icon: DataAnalysis, color: '#9c27b0' },
  { label: '检查总数', value: stats.value.totalExaminations, icon: Document, color: '#f59e0b' },
])

const modules = computed(() => [
  {
    path: '/examination-doctor/application',
    title: '查看检查申请',
    desc: '筛选待分配和待执行的影像检查。',
    meta: `${pendingCount.value} 待处理`,
    icon: Document,
    colorClass: 'blue',
    primary: true,
  },
  {
    path: '/doctor/profile',
    title: '医生信息',
    desc: '维护头像、资料和登录密码。',
    meta: '个人资料',
    icon: UserFilled,
    colorClass: 'violet',
  },
  {
    path: '/doctor/schedule',
    title: '值班查询',
    desc: '查看当前医生排班和值守时间。',
    meta: '排班安排',
    icon: DataAnalysis,
    colorClass: 'green',
  },
])

const recentActivities = computed(() => examOrders.value.slice(0, 3).map((item, index) => ({
  id: index + 1,
  color: item.status === 'COMPLETED' ? '#22c55e' : '#2563eb',
  content: `${item.itemName || '检查申请'} ${statusLabel(item.status)}`,
  detail: item.status === 'COMPLETED' ? '报告已回传' : '可进入处理流程',
  time: formatShortTime(item.createTime),
})))

function setCurrentDate() {
  const now = new Date()
  const year = now.getFullYear()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  currentDate.value = `${year}-${month}-${day}`
}

function navigateTo(path: string) {
  router.push(path).catch((error) => {
    console.error('路由跳转失败:', error)
  })
}

async function fetchDashboardData() {
  try {
    const res: any = await getInspectionOrderList()
    orders.value = Array.isArray(res.data) ? res.data : []
    const list = examOrders.value
    stats.value = {
      totalExaminations: list.length,
      completedExaminations: list.filter((item) => item.status === 'COMPLETED').length,
      pendingExaminations: list.filter((item) => isPending(item.status)).length,
      aiReports: list.filter((item) => isCtItem(item) && (item.canViewReport === true || item.status === 'COMPLETED')).length,
    }
  } catch (error) {
    console.error('加载检查工作台统计失败:', error)
  }
}

function isPending(status: string) {
  return status === 'WAITING_ASSIGN' || status === 'QUEUED' || status === 'IN_PROCESS'
}

function isCtItem(item: InspectionOrderVo) {
  const code = String(item.itemCode || '').toUpperCase()
  const name = String(item.itemName || '').toLowerCase()
  return code.includes('CT') || name.includes('ct')
}

function statusLabel(status: string) {
  const map: Record<string, string> = {
    WAITING_ASSIGN: '待分配',
    QUEUED: '已排队',
    IN_PROCESS: '处理中',
    COMPLETED: '已完成',
    CANCELLED: '已取消',
  }
  return map[status] || status || '待处理'
}

function formatShortTime(time?: string) {
  if (!time) return '-'
  const date = new Date(time)
  if (Number.isNaN(date.getTime())) return '-'
  return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

onMounted(() => {
  setCurrentDate()
  fetchDashboardData()
})
</script>

<style scoped>
.page {
  min-height: 100vh;
  padding: 48px 34px 36px;
  background:
    radial-gradient(circle at top right, rgba(37, 99, 235, 0.08), transparent 24%),
    linear-gradient(180deg, #f8fbff 0%, #f8fafc 100%);
}

.hero {
  display: grid;
  grid-template-columns: minmax(0, 1.6fr) minmax(300px, 0.9fr);
  gap: 18px;
  max-width: 1280px;
  margin: 0 auto 18px;
}

.hero-copy,
.hero-board,
.panel {
  border: 1px solid #e2e8f0;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 16px 40px rgba(15, 23, 42, 0.06);
}

.hero-copy {
  padding: 26px 30px;
}

.hero-eyebrow {
  margin: 0 0 8px;
  color: #2563eb;
  font-size: 13px;
  font-weight: 800;
  letter-spacing: 0.08em;
}

.hero-copy h1 {
  margin: 0;
  max-width: none;
  color: #0f172a;
  font-size: 28px;
  line-height: 1.24;
  white-space: nowrap;
}

.hero-desc {
  max-width: 680px;
  margin: 10px 0 0;
  color: #64748b;
  font-size: 15px;
  line-height: 1.65;
}

.hero-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 16px;
}

.hero-tags span {
  display: inline-flex;
  align-items: center;
  padding: 8px 14px;
  border-radius: 999px;
  background: #eff6ff;
  color: #1d4ed8;
  font-size: 13px;
  font-weight: 700;
}

.hero-board {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  padding: 22px;
}

.hero-stat {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px;
  border-radius: 18px;
  background: linear-gradient(135deg, #f8fafc, #eef6ff);
}

.hero-stat-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  border-radius: 14px;
  color: #fff;
  flex-shrink: 0;
}

.hero-stat strong {
  display: block;
  color: #0f172a;
  font-size: 24px;
  line-height: 1;
}

.hero-stat span {
  display: block;
  margin-top: 6px;
  color: #64748b;
  font-size: 12px;
  font-weight: 700;
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  max-width: 1280px;
  margin: 0 auto 18px;
}

.quick-card {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 12px;
  min-height: 190px;
  padding: 20px;
  border: 1px solid #e2e8f0;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.05);
  text-align: left;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.quick-card:hover {
  transform: translateY(-3px);
  border-color: #bfdbfe;
  box-shadow: 0 18px 36px rgba(37, 99, 235, 0.1);
}

.quick-card.primary {
  background:
    radial-gradient(circle at top right, rgba(37, 99, 235, 0.1), transparent 36%),
    rgba(255, 255, 255, 0.96);
}

.quick-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 54px;
  height: 54px;
  border-radius: 18px;
  font-size: 22px;
  flex-shrink: 0;
}

.quick-icon.blue { background: #dbeafe; color: #2563eb; }
.quick-icon.green { background: #dcfce7; color: #16a34a; }
.quick-icon.violet { background: #ede9fe; color: #7c3aed; }

.quick-body h3 {
  margin: 0;
  color: #0f172a;
  font-size: 18px;
}

.quick-body p {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  margin: 8px 0 0;
  color: #64748b;
  font-size: 14px;
  line-height: 1.6;
}

.quick-meta {
  margin-top: auto;
  color: #2563eb;
  font-size: 13px;
  font-weight: 800;
}

.quick-arrow {
  position: absolute;
  right: 18px;
  bottom: 18px;
}

.content-grid {
  max-width: 1280px;
  margin: 0 auto;
}

.activity-panel {
  padding: 22px;
}

.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 12px;
}

.panel-kicker {
  margin: 0 0 6px;
  color: #2563eb;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.08em;
}

.panel-head h2 {
  margin: 0;
  color: #0f172a;
  font-size: 22px;
}

.link-btn {
  border: none;
  background: transparent;
  color: #2563eb;
  font-size: 13px;
  font-weight: 800;
  cursor: pointer;
}

.activity-list {
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin: 0;
  padding: 0;
}

.activity-list li {
  display: grid;
  grid-template-columns: 12px minmax(0, 1fr) auto;
  align-items: center;
  gap: 14px;
  padding: 12px 0;
  border-bottom: 1px solid #eef2f7;
}

.activity-list li:last-child {
  border-bottom: none;
}

.activity-dot {
  width: 12px;
  height: 12px;
  border-radius: 999px;
  box-shadow: 0 0 0 6px rgba(148, 163, 184, 0.08);
}

.activity-main strong {
  display: block;
  color: #0f172a;
  font-size: 15px;
}

.activity-main span {
  display: block;
  margin-top: 4px;
  color: #64748b;
  font-size: 13px;
}

.activity-list time {
  color: #94a3b8;
  font-size: 12px;
  font-weight: 700;
}

@media (max-width: 1200px) {
  .quick-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .hero {
    grid-template-columns: 1fr;
  }

  .hero-copy h1 {
    white-space: normal;
  }
}

@media (max-width: 640px) {
  .page {
    padding: 28px 18px;
  }

  .hero-copy h1 {
    font-size: 26px;
  }

  .quick-grid,
  .hero-board {
    grid-template-columns: 1fr;
  }

  .activity-list li {
    grid-template-columns: 12px minmax(0, 1fr);
  }

  .activity-list time {
    grid-column: 2;
  }
}
</style>

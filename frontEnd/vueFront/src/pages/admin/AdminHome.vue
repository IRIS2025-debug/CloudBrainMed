<template>
  <div class="dashboard">
    <header class="page-header">
      <div>
        <h1>CloudBrainMed</h1>
        <p class="header-sub">AI 智能医疗诊疗系统</p>
      </div>
      <div class="header-actions">
        <span class="header-updated" v-if="updatedAt">更新于 {{ updatedAt }}</span>
        <el-button size="small" :loading="loading" @click="loadOverview">
          <el-icon style="margin-right:4px"><Refresh /></el-icon>刷新
        </el-button>
      </div>
    </header>

    <div class="stats-row">
      <div class="stat-card" v-for="stat in stats" :key="stat.label">
        <div class="stat-icon" :style="{ background: stat.color }">
          <el-icon :size="20"><component :is="stat.icon" /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stat.value }}</div>
          <div class="stat-label">{{ stat.label }}</div>
        </div>
      </div>
    </div>

    <h2 class="section-title">功能模块</h2>
    <div class="module-grid">
      <div class="module-card" v-for="m in modules" :key="m.path" @click="$router.push(m.path)">
        <div class="mc-icon-wrap" :style="{ background: m.color }">
          <el-icon :size="22" color="#fff"><component :is="m.icon" /></el-icon>
        </div>
        <div class="mc-info">
          <div class="mc-title">{{ m.title }}</div>
          <div class="mc-desc">{{ m.desc }}</div>
        </div>
        <el-icon class="mc-arrow" color="#94a3b8"><ArrowRight /></el-icon>
      </div>
    </div>

    <h2 class="section-title">最近操作</h2>
    <div class="activity-feed">
      <div class="feed-item" v-for="(item, i) in activities" :key="i">
        <div class="feed-dot"></div>
        <span class="feed-text">{{ item.text }}</span>
        <span class="feed-time">{{ item.time }}</span>
      </div>
      <div class="feed-empty">暂无更多记录</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ArrowRight, Cpu, DataAnalysis, List, Refresh, UserFilled } from '@element-plus/icons-vue'
import { getDashboardOverview } from '@/api/admin/dashboard'
import { getInferenceStats, getModelList } from '@/api/admin/ml'

// 每张卡片对应一个真实指标；'--' 表示加载中或该项请求失败，真实零值显示 0。
const stats = ref([
  { key: 'todayScheduleCount', label: '今日排班', value: '--', icon: UserFilled, color: '#2563eb' },
  { key: 'todayInference', label: '今日推理', value: '--', icon: Cpu, color: '#7c3aed' },
  { key: 'successRate', label: '成功率', value: '--', icon: DataAnalysis, color: '#22c55e' },
  { key: 'deployedModels', label: '部署模型数', value: '--', icon: Cpu, color: '#0d9488' },
])
const loading = ref(false)
const updatedAt = ref('')

function applyMetric(key: string, value: unknown) {
  const stat = stats.value.find((item) => item.key === key)
  if (stat && typeof value === 'number') {
    stat.value = key === 'successRate' ? `${value}%` : String(value)
  }
}

const modules = [
  { path: '/admin/profile', title: '管理员个人信息', desc: '查看编辑资料、头像上传、密码修改', icon: UserFilled, color: '#2563eb' },
  { path: '/admin/userManage', title: '账号权限管理', desc: '对医生和管理员账号进行增删改查操作', icon: List, color: '#0d9488' },
  { path: '/admin/medicine', title: '药品管理', desc: '药品信息维护、库存管理', icon: List, color: '#165DFF' },
  { path: '/admin/ml/dashboard', title: 'AI 推理看板', desc: '成功率、采纳率、耗时统计', icon: DataAnalysis, color: '#7c3aed' },
  { path: '/admin/scheduling', title: 'AI智能排班', desc: '根据医生工作量和患者需求，智能排班', icon: DataAnalysis, color: '#165DFF' },
]

const activities = [
  { text: '系统初始化完成', time: '刚刚' },
]

// 四项指标来自三个数据源，使用 allSettled 保证单个请求失败只影响对应卡片，
// 不会把失败伪装成 0，也不会阻塞其它指标加载。
async function loadOverview() {
  loading.value = true
  stats.value.forEach((stat) => { stat.value = '--' })

  try {
    const [overviewResult, inferenceResult, modelsResult] = await Promise.allSettled([
      getDashboardOverview(),
      getInferenceStats(),
      getModelList(),
    ])

    if (overviewResult.status === 'fulfilled') {
      applyMetric('todayScheduleCount', overviewResult.value.data?.todayScheduleCount)
    }
    if (inferenceResult.status === 'fulfilled') {
      const data = inferenceResult.value.data as Record<string, unknown> | undefined
      applyMetric('todayInference', data?.todayTotal)
      applyMetric('successRate', data?.successRate)
    }
    if (modelsResult.status === 'fulfilled' && Array.isArray(modelsResult.value.data)) {
      applyMetric('deployedModels', modelsResult.value.data.length)
    }
  } finally {
    updatedAt.value = new Date().toLocaleTimeString('zh-CN')
    loading.value = false
  }
}

onMounted(loadOverview)
</script>

<style scoped>
.dashboard { padding: 32px 36px; }

.page-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 32px; }
.page-header h1 { font-size: 26px; font-weight: 800; color: #0f172a; letter-spacing: -.5px; }
.header-sub { font-size: 14px; color: #64748b; margin-top: 4px; }
.header-actions { display: flex; align-items: center; gap: 12px; flex-shrink: 0; }
.header-updated { font-size: 12px; color: #94a3b8; white-space: nowrap; }

.stats-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 36px; }
.stat-card { background: #fff; border-radius: var(--radius); padding: 20px 24px; display: flex; align-items: center; gap: 16px; box-shadow: var(--shadow); }
.stat-icon { width: 44px; height: 44px; border-radius: 10px; display: flex; align-items: center; justify-content: center; color: #fff; }
.stat-value { font-size: 24px; font-weight: 700; color: #0f172a; }
.stat-label { font-size: 13px; color: #94a3b8; margin-top: 2px; }

.section-title { font-size: 15px; font-weight: 700; color: #334155; margin-bottom: 16px; letter-spacing: -.2px; }

.module-grid { display: flex; flex-direction: column; gap: 6px; margin-bottom: 36px; }
.module-card {
  background: #fff; border-radius: 10px; padding: 16px 20px;
  display: flex; align-items: center; gap: 16px;
  cursor: pointer; transition: all .15s ease;
  box-shadow: var(--shadow);
}
.module-card:hover { transform: translateX(4px); box-shadow: var(--shadow-md); }
.mc-icon-wrap { width: 40px; height: 40px; border-radius: 10px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.mc-title { font-size: 15px; font-weight: 600; color: #1e293b; }
.mc-desc { font-size: 13px; color: #94a3b8; margin-top: 2px; }
.mc-arrow { margin-left: auto; }

.activity-feed { background: #fff; border-radius: var(--radius); padding: 16px 20px; box-shadow: var(--shadow); }
.feed-item { display: flex; align-items: center; gap: 10px; padding: 10px 0; border-bottom: 1px solid #f1f5f9; font-size: 14px; }
.feed-item:last-child { border-bottom: none; }
.feed-dot { width: 8px; height: 8px; border-radius: 50%; background: #cbd5e1; flex-shrink: 0; }
.feed-text { flex: 1; color: #475569; }
.feed-time { color: #94a3b8; font-size: 12px; }
.feed-empty { text-align: center; color: #cbd5e1; font-size: 13px; padding-top: 8px; }
</style>

<template>
  <div class="dashboard">
    <section class="hero-panel">
      <div class="hero-main">
        <p class="hero-eyebrow">管理员工作台</p>
        <h1>首页概况</h1>
        <p class="hero-copy">查看系统状态、常用入口与 AI 管理模块。</p>
      </div>

      <div class="hero-side">
        <div class="status-card">
          <span class="status-label">系统状态</span>
          <strong>运行正常</strong>
          <p>管理员入口、AI 模块与排班功能可继续使用。</p>
        </div>
      </div>
    </section>

    <div class="stats-row">
      <div class="stat-card" v-for="stat in stats" :key="stat.label">
        <div class="stat-icon" :style="{ background: stat.color }">
          <el-icon :size="20"><component :is="stat.icon" /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stat.value }}</div>
          <div class="stat-label">{{ stat.label }}</div>
          <div class="stat-note">{{ stat.note }}</div>
        </div>
      </div>
    </div>

    <section class="panel">
      <div class="section-head">
        <div>
          <h2>常用功能</h2>
          <p>快速进入资料、药品、AI 与排班管理页面。</p>
        </div>
      </div>

      <div class="module-grid">
        <button class="module-card" v-for="module in modules" :key="module.path" @click="$router.push(module.path)">
          <div class="module-icon" :style="{ background: module.color }">
            <el-icon :size="20" color="#fff"><component :is="module.icon" /></el-icon>
          </div>
          <div class="module-info">
            <div class="module-title">{{ module.title }}</div>
            <div class="module-desc">{{ module.desc }}</div>
          </div>
          <el-icon class="module-arrow" color="#8fa4bd"><ArrowRight /></el-icon>
        </button>
      </div>
    </section>

    <section class="panel activity-panel">
      <div class="section-head">
        <div>
          <h2>最近动态</h2>
          <p>显示当前工作台的简要状态提示。</p>
        </div>
      </div>

      <div class="activity-feed">
        <div class="feed-item" v-for="(item, index) in activities" :key="`${item.text}-${index}`">
          <div class="feed-marker"></div>
          <div class="feed-body">
            <span class="feed-text">{{ item.text }}</span>
            <span class="feed-time">{{ item.time }}</span>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { getAdminDashboardOverview } from '@/api/admin/dashboard'
import { buildAdminOverviewStats } from '@/pages/admin/dashboardOverview'
import { ArrowRight, DataAnalysis, List, UserFilled } from '@element-plus/icons-vue'

const stats = ref(buildAdminOverviewStats(null))

const modules = [
  {
    path: '/admin/profile',
    title: '管理员个人信息',
    desc: '维护头像与联系方式',
    icon: UserFilled,
    color: 'linear-gradient(135deg, #315fbb, #4f8df7)',
  },
  {
    path: '/admin/userManage',
    title: '账号权限管理',
    desc: '管理医生账号启停与编辑',
    icon: List,
    color: 'linear-gradient(135deg, #0f766e, #16a34a)',
  },
  {
    path: '/admin/medicine',
    title: '药品管理',
    desc: '查看库存与补货建议',
    icon: List,
    color: 'linear-gradient(135deg, #165dff, #3b82f6)',
  },
  {
    path: '/admin/ml/dashboard',
    title: 'AI 推理看板',
    desc: '跟踪推理量与耗时',
    icon: DataAnalysis,
    color: 'linear-gradient(135deg, #7c3aed, #9333ea)',
  },
  {
    path: '/admin/scheduling',
    title: '排班管理',
    desc: '进入医生排班周视图',
    icon: DataAnalysis,
    color: 'linear-gradient(135deg, #0284c7, #38bdf8)',
  },
]

const activities = [
  { text: '管理员工作台可正常访问。', time: '刚刚' },
  { text: 'AI 模块入口保持可用。', time: '今日' },
  { text: '药品与排班页面可继续使用。', time: '今日' },
]
onMounted(async () => {
  try {
    const response = await getAdminDashboardOverview()
    stats.value = buildAdminOverviewStats(response.data)
  } catch (error) {
    console.warn('Failed to load admin dashboard overview', error)
  }
})
</script>

<style scoped>
.dashboard {
  padding: 28px 32px 36px;
}

.hero-panel {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) 280px;
  gap: 18px;
  margin-bottom: 22px;
}

.hero-main,
.status-card,
.panel,
.stat-card {
  border-radius: 24px;
  border: 1px solid rgba(219, 228, 240, 0.95);
  box-shadow: 0 16px 36px rgba(31, 41, 55, 0.07);
}

.hero-main {
  padding: 26px 28px;
  background:
    radial-gradient(circle at top right, rgba(79, 141, 247, 0.16), transparent 32%),
    linear-gradient(135deg, #ffffff 0%, #f5f9ff 100%);
}

.hero-eyebrow {
  margin: 0 0 10px;
  color: #315fbb;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.12em;
}

.hero-main h1 {
  margin: 0;
  color: #16304d;
  font-size: 34px;
  line-height: 1.08;
  letter-spacing: -0.04em;
}

.hero-copy {
  margin: 10px 0 0;
  max-width: 520px;
  color: #647991;
  font-size: 14px;
  line-height: 1.7;
}

.status-card {
  padding: 22px 22px 20px;
  background: linear-gradient(180deg, #315fbb 0%, #4572cf 100%);
  color: #fff;
}

.status-label {
  display: inline-flex;
  align-items: center;
  height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.14);
  font-size: 12px;
  font-weight: 700;
}

.status-card strong {
  display: block;
  margin-top: 16px;
  font-size: 26px;
  letter-spacing: -0.03em;
}

.status-card p {
  margin: 10px 0 0;
  color: rgba(255, 255, 255, 0.86);
  font-size: 13px;
  line-height: 1.65;
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 22px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 20px 22px;
  background: #fff;
}

.stat-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 46px;
  height: 46px;
  border-radius: 14px;
  color: #fff;
  flex-shrink: 0;
}

.stat-value {
  color: #16304d;
  font-size: 24px;
  font-weight: 800;
}

.stat-label {
  margin-top: 4px;
  color: #5d738e;
  font-size: 13px;
  font-weight: 700;
}

.stat-note {
  margin-top: 4px;
  color: #90a1b8;
  font-size: 12px;
}

.panel {
  margin-bottom: 22px;
  padding: 22px;
  background: #fff;
}

.section-head {
  margin-bottom: 16px;
}

.section-head h2 {
  margin: 0;
  color: #16304d;
  font-size: 18px;
  font-weight: 800;
}

.section-head p {
  margin: 6px 0 0;
  color: #72859d;
  font-size: 13px;
  line-height: 1.6;
}

.module-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.module-card {
  display: flex;
  align-items: center;
  gap: 14px;
  width: 100%;
  padding: 16px;
  border: 1px solid #e6edf5;
  border-radius: 18px;
  background: #fbfdff;
  cursor: pointer;
  text-align: left;
  transition: transform 0.16s ease, box-shadow 0.16s ease, border-color 0.16s ease;
}

.module-card:hover {
  transform: translateY(-2px);
  border-color: rgba(79, 141, 247, 0.5);
  box-shadow: 0 12px 24px rgba(49, 95, 187, 0.1);
}

.module-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  border-radius: 14px;
  flex-shrink: 0;
}

.module-title {
  color: #1d3550;
  font-size: 14px;
  font-weight: 800;
}

.module-desc {
  margin-top: 4px;
  color: #74879f;
  font-size: 12px;
  line-height: 1.55;
}

.module-arrow {
  margin-left: auto;
  flex-shrink: 0;
}

.activity-panel {
  margin-bottom: 0;
  background: #fcfdff;
}

.activity-feed {
  display: grid;
  gap: 12px;
}

.feed-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px solid #eef3f8;
}

.feed-item:last-child {
  border-bottom: none;
}

.feed-marker {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: linear-gradient(135deg, #315fbb, #4f8df7);
  box-shadow: 0 0 0 4px rgba(49, 95, 187, 0.08);
  flex-shrink: 0;
}

.feed-body {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  width: 100%;
}

.feed-text {
  color: #546880;
  font-size: 13px;
}

.feed-time {
  color: #93a4ba;
  font-size: 12px;
  white-space: nowrap;
}

@media (max-width: 1180px) {
  .hero-panel {
    grid-template-columns: 1fr;
  }

  .module-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .stats-row {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .dashboard {
    padding: 18px 16px 24px;
  }

  .hero-main,
  .status-card,
  .panel {
    padding: 18px;
  }

  .stats-row,
  .module-grid {
    grid-template-columns: 1fr;
  }

  .feed-body {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>

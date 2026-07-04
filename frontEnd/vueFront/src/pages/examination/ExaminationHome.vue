<template>
  <div class="page">
    <header class="page-top">
      <div>
        <h2>检查工作台</h2>
        <p class="top-sub">管理您的影像检查任务与诊疗流程</p>
      </div>
      <div class="header-actions">
        <span class="date-badge">
          <i class="far fa-calendar-alt"></i> {{ currentDate }}
        </span>
        <span class="status-badge">
          <i class="fas fa-circle" style="color: #22c55e; font-size: 10px;"></i> 在线
        </span>
      </div>
    </header>

    <div class="content-grid">
      <!-- 功能卡片网格 -->
      <div class="left-panel">
        <div class="feature-grid">
          <div class="feature-card" @click="navigateTo('/examination-doctor/application')">
            <div class="card-icon" style="background: #e0edff; color: #1a5cff;">
              <i class="fas fa-file-medical-alt"></i>
            </div>
            <h3>查看检查申请</h3>
            <p>待处理申请 · {{ pendingCount }}</p>
            <span class="card-hint">点击进入 →</span>
          </div>

          <div class="feature-card" @click="navigateTo('/examination-doctor/ct-inference')">
            <div class="card-icon" style="background: #e6f7e6; color: #16a34a;">
              <i class="fas fa-cloud-upload-alt"></i>
            </div>
            <h3>CT 模型推理</h3>
            <p>金属伪影 / 病灶分割</p>
            <span class="card-hint">点击进入 →</span>
          </div>

          <div class="feature-card" @click="navigateTo('/examination-doctor/analysis')">
            <div class="card-icon" style="background: #f0ebff; color: #7c3aed;">
              <i class="fas fa-brain"></i>
            </div>
            <h3>影像分析</h3>
            <p>AI 辅助病灶检测</p>
            <span class="card-hint">点击进入 →</span>
          </div>

          <div class="feature-card" @click="navigateTo('/examination-doctor/report')">
            <div class="card-icon" style="background: #fef3c7; color: #d97706;">
              <i class="fas fa-robot"></i>
            </div>
            <h3>AI生成检查报告</h3>
            <p>结构化报告模板</p>
            <span class="card-hint">点击进入 →</span>
          </div>
        </div>

        <!-- 近期动态 -->
        <div class="recent-activity">
          <div class="activity-header">
            <span><i class="far fa-clock"></i> 最近动态</span>
            <span class="view-all" @click="viewAllActivities">查看全部</span>
          </div>
          <ul class="activity-list">
            <li v-for="activity in recentActivities" :key="activity.id">
              <i :class="activity.icon" :style="{ color: activity.color }"></i>
              {{ activity.content }}
              <span class="time">{{ activity.time }}</span>
            </li>
          </ul>
        </div>

        <!-- 统计概览 -->
        <div class="stats-overview">
          <div class="stat-item">
            <div class="stat-number">{{ stats.totalExaminations }}</div>
            <div class="stat-label">总检查数</div>
          </div>
          <div class="stat-item">
            <div class="stat-number">{{ stats.completedExaminations }}</div>
            <div class="stat-label">已完成</div>
          </div>
          <div class="stat-item">
            <div class="stat-number">{{ stats.pendingExaminations }}</div>
            <div class="stat-label">待处理</div>
          </div>
          <div class="stat-item">
            <div class="stat-number">{{ stats.aiReports }}</div>
            <div class="stat-label">AI报告</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 路由预览提示 -->
    <div id="route-preview-msg" class="route-preview">📍 准备跳转...</div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

// 当前日期
const currentDate = ref('')
const pendingCount = ref(12)

// 统计数据
const stats = ref({
  totalExaminations: 156,
  completedExaminations: 89,
  pendingExaminations: 12,
  aiReports: 45
})

// 最近动态
const recentActivities = ref([
  {
    id: 1,
    icon: 'fas fa-check-circle',
    color: '#22c55e',
    content: '申请 #3042 已完成影像分析',
    time: '10:32'
  },
  {
    id: 2,
    icon: 'fas fa-upload',
    color: '#1a5cff',
    content: '张医生上传了 6 幅 CT 影像',
    time: '09:15'
  },
  {
    id: 3,
    icon: 'fas fa-file-pdf',
    color: '#d97706',
    content: 'AI 报告 #129 已生成',
    time: '08:50'
  },
  {
    id: 4,
    icon: 'fas fa-user-md',
    color: '#7c3aed',
    content: '李医生提交了新的检查申请',
    time: '08:20'
  }
])

// 设置当前日期
const setCurrentDate = () => {
  const now = new Date()
  const year = now.getFullYear()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  currentDate.value = `${year}-${month}-${day}`
}

// 路由跳转
const navigateTo = (path: string) => {
  const msg = document.getElementById('route-preview-msg')
  if (msg) {
    msg.textContent = `📍 正在跳转到: ${path}`
    msg.style.opacity = '1'
    setTimeout(() => { msg.style.opacity = '0' }, 1500)
  }
  
  // 实际路由跳转
  router.push(path).catch(err => {
    console.error('路由跳转失败:', err)
  })
}

// 查看全部动态
const viewAllActivities = () => {
  console.log('查看全部动态')
  // 可以跳转到动态列表页面或展开更多
}

// 模拟获取数据
const fetchData = async () => {
  try {
    // 这里可以调用API获取真实数据
    // const res = await getDashboardData()
    // pendingCount.value = res.pendingCount
    // stats.value = res.stats
    // recentActivities.value = res.activities
  } catch (error) {
    console.error('获取数据失败:', error)
  }
}

onMounted(() => {
  setCurrentDate()
  fetchData()
})
</script>

<style scoped>
/* ===== 全局样式 ===== */
.page {
  padding: 28px 36px;
  background: #f8fafc;
  min-height: 100vh;
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

/* ===== 顶部栏 ===== */
.page-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 28px;
  flex-wrap: wrap;
  gap: 16px;
}

.page-top h2 {
  font-size: 24px;
  font-weight: 700;
  color: #0f172a;
  letter-spacing: -0.3px;
}

.top-sub {
  font-size: 14px;
  color: #94a3b8;
  margin-top: 4px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.date-badge {
  background: #ffffff;
  padding: 6px 16px;
  border-radius: 40px;
  font-size: 14px;
  color: #1e293b;
  display: flex;
  align-items: center;
  gap: 8px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);
  border: 1px solid #e2e8f0;
}

.status-badge {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: #1e293b;
  background: #ffffff;
  padding: 6px 16px;
  border-radius: 40px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);
}

/* ===== 内容网格 ===== */
.content-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 24px;
}

/* ===== 左侧面板 ===== */
.left-panel {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

/* ===== 功能卡片网格 ===== */
.feature-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.feature-card {
  background: #ffffff;
  border: 1px solid #eef2f8;
  border-radius: 20px;
  padding: 20px 18px 18px 18px;
  transition: all 0.25s ease;
  cursor: pointer;
  box-shadow: 0 2px 6px rgba(0,0,0,0.02);
  position: relative;
  overflow: hidden;
}

.feature-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, #1a5cff, #7c3aed);
  opacity: 0;
  transition: opacity 0.3s ease;
}

.feature-card:hover::before {
  opacity: 1;
}

.feature-card:hover {
  transform: translateY(-4px);
  border-color: #cbd5e1;
  box-shadow: 0 12px 28px -12px rgba(0, 0, 0, 0.10);
  background: #ffffff;
}

.card-icon {
  width: 52px;
  height: 52px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  margin-bottom: 14px;
}

.feature-card h3 {
  font-size: 16px;
  font-weight: 600;
  color: #0f172a;
  margin-bottom: 4px;
}

.feature-card p {
  font-size: 14px;
  color: #64748b;
  margin-bottom: 12px;
}

.card-hint {
  font-size: 13px;
  color: #1a5cff;
  font-weight: 500;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  transition: transform 0.2s;
}

.feature-card:hover .card-hint {
  transform: translateX(4px);
}

/* ===== 统计概览 ===== */
.stats-overview {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.stat-item {
  background: #ffffff;
  border-radius: 16px;
  padding: 20px;
  text-align: center;
  border: 1px solid #eef2f8;
  transition: all 0.3s ease;
}

.stat-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px -8px rgba(0, 0, 0, 0.08);
}

.stat-number {
  font-size: 28px;
  font-weight: 700;
  color: #0f172a;
  line-height: 1.2;
}

.stat-label {
  font-size: 14px;
  color: #94a3b8;
  margin-top: 6px;
}

/* ===== 近期动态 ===== */
.recent-activity {
  background: #ffffff;
  border-radius: 20px;
  padding: 20px 22px 16px 22px;
  border: 1px solid #eef2f8;
  box-shadow: 0 2px 6px rgba(0,0,0,0.02);
}

.activity-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
  font-size: 15px;
  font-weight: 600;
  color: #0f172a;
}

.view-all {
  font-size: 13px;
  color: #1a5cff;
  font-weight: 500;
  cursor: pointer;
  opacity: 0.7;
  transition: opacity 0.2s;
}

.view-all:hover { 
  opacity: 1; 
}

.activity-list {
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.activity-list li {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 14px;
  color: #1e293b;
  padding: 6px 0;
  border-bottom: 1px solid #f1f5f9;
}

.activity-list li:last-child {
  border-bottom: none;
}

.activity-list .time {
  margin-left: auto;
  font-size: 12px;
  color: #94a3b8;
  background: #f1f5f9;
  padding: 2px 12px;
  border-radius: 30px;
  font-weight: 500;
}

/* ===== 路由预览提示 ===== */
.route-preview {
  margin-top: 24px;
  padding: 12px 20px;
  background: #f0f5fe;
  border-radius: 40px;
  color: #1a5cff;
  font-size: 14px;
  font-weight: 500;
  text-align: center;
  opacity: 0;
  transition: opacity 0.25s ease;
  border: 1px solid rgba(26, 92, 255, 0.10);
}

/* ===== 响应式适配 ===== */
@media (max-width: 1400px) {
  .feature-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .stats-overview {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .page {
    padding: 20px 16px;
  }
  
  .page-top {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .header-actions {
    width: 100%;
    justify-content: flex-start;
  }
  
  .feature-grid {
    grid-template-columns: 1fr;
  }
  
  .stats-overview {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 480px) {
  .stats-overview {
    grid-template-columns: 1fr;
  }
}
</style>

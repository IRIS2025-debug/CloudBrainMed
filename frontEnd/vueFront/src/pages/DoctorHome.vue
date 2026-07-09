<template>
<div class="doctor-home">
<div class="welcome-section">
  <h1>欢迎，医生</h1>
  <p>{{ currentDateTime }}</p>
</div>

<div class="stats-grid">
  <div class="stat-card">
    <div class="stat-icon" style="background: #E3F2FD;">
      <el-icon style="color: #165DFF; font-size: 24px;"><DocumentCopy /></el-icon>
    </div>
    <div class="stat-content">
      <div class="stat-label">今日诊疗</div>
      <div class="stat-value">12</div>
    </div>
  </div>

  <div class="stat-card">
    <div class="stat-icon" style="background: #F3E5F5;">
      <el-icon style="color: #9C27B0; font-size: 24px;"><Calendar /></el-icon>
    </div>
    <div class="stat-content">
      <div class="stat-label">待诊患者</div>
      <div class="stat-value">5</div>
    </div>
  </div>

  <div class="stat-card">
    <div class="stat-icon" style="background: #E8F5E9;">
      <el-icon style="color: #4CAF50; font-size: 24px;"><Check /></el-icon>
    </div>
    <div class="stat-content">
      <div class="stat-label">已完成</div>
      <div class="stat-value">8</div>
    </div>
  </div>

  <div class="stat-card">
    <div class="stat-icon" style="background: #FFF3E0;">
      <el-icon style="color: #FF9800; font-size: 24px;"><Warning /></el-icon>
    </div>
    <div class="stat-content">
      <div class="stat-label">AI 诊断</div>
      <div class="stat-value">3</div>
    </div>
  </div>
</div>

<div class="quick-actions">
  <el-button type="primary" @click="$router.push('/doctor/consult')">
    <el-icon><List /></el-icon>
    接诊工作台
  </el-button>
  <el-button type="primary" @click="$router.push('/doctor/ai-medicine')">
    <el-icon><Cpu /></el-icon>
    AI 药物推荐
  </el-button>
  <el-button @click="$router.push('/doctor/profile')">
    <el-icon><UserFilled /></el-icon>
    个人信息
  </el-button>
  <el-button @click="handleLogout">
    <el-icon><SwitchButton /></el-icon>
    退出登录
  </el-button>
</div>
</div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { List, Cpu, UserFilled, SwitchButton, DocumentCopy, Calendar, Check, Warning } from '@element-plus/icons-vue'

const router = useRouter()
const currentDateTime = ref('')

onMounted(() => {
  const now = new Date()
  currentDateTime.value = now.toLocaleString('zh-CN')
})

const handleLogout = () => {
  sessionStorage.removeItem('token')
  sessionStorage.removeItem('roleType')
  ElMessage.success('已退出登录')
  router.push('/login')
}
</script>

<style scoped>
.doctor-home {
  padding: 24px;
  max-width: 1200px;
}

.welcome-section {
  margin-bottom: 32px;
}

.welcome-section h1 {
  font-size: 28px;
  font-weight: 600;
  color: #1a1f36;
  margin-bottom: 8px;
}

.welcome-section p {
  font-size: 14px;
  color: #5a6a7e;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 20px;
  margin-bottom: 32px;
}

.stat-card {
  background: white;
  border-radius: 12px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  transition: all 0.3s ease;
}

.stat-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}

.stat-icon {
  width: 60px;
  height: 60px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-content {
  flex: 1;
}

.stat-label {
  font-size: 13px;
  color: #8a99b0;
  margin-bottom: 4px;
}

.stat-value {
  font-size: 24px;
  font-weight: 600;
  color: #1a1f36;
}

.quick-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.quick-actions :deep(.el-button) {
  border-radius: 8px;
}
</style>

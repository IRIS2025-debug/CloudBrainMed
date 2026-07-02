
<template>
  <div class="login-page">
    <!-- 背景装饰，更柔和的动态渐变 -->
    <div class="bg-decoration"></div>

    <div class="login-card">
      <!-- 左侧品牌区域 -->
      <div class="brand-side">
        <div class="brand-logo">
          <svg width="48" height="48" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M12 2L2 7L12 12L22 7L12 2Z" fill="white" fill-opacity="0.95"/>
            <path d="M2 17L12 22L22 17" stroke="white" stroke-width="2" stroke-linecap="round"/>
            <path d="M2 12L12 17L22 12" stroke="white" stroke-width="2" stroke-linecap="round"/>
          </svg>
        </div>
        <h1 class="brand-title">CloudBrainMed</h1>
        <p class="brand-desc">AI 智能医疗诊疗系统</p>
        <div class="brand-features">
          <div class="feature-item">
            <el-icon><Check /></el-icon>
            <span>智能诊断辅助</span>
          </div>
          <div class="feature-item">
            <el-icon><DataAnalysis /></el-icon>
            <span>医疗数据分析</span>
          </div>

        </div>
      </div>

      <!-- 右侧登录表单区域 -->
      <div class="form-side">
        <div class="form-header">
          <h2>欢迎回来</h2>
          <p>请登录您的账号以继续访问</p>
        </div>

        <el-form :model="loginForm" class="login-form">
          <el-form-item>
            <el-select v-model="loginForm.identity" placeholder="选择身份" style="width:100%" size="large">
              <el-option label="医生" value="doctor" />
              <el-option label="管理员" value="admin" />
            </el-select>
          </el-form-item>

          <el-form-item>
            <el-input
                v-model="loginForm.phone"
                placeholder="手机号"
                :prefix-icon="User"
                size="large"
                clearable
            />
          </el-form-item>

          <el-form-item>
            <el-input
                v-model="loginForm.password"
                placeholder="密码"
                :prefix-icon="Lock"
                show-password
                size="large"
                clearable
            />
          </el-form-item>

          <el-form-item>
            <el-button type="primary" @click="handleLogin" class="login-btn" size="large" :loading="loading">
              登录系统
            </el-button>
          </el-form-item>
        </el-form>


      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { Check, DataAnalysis, Lock, User } from '@element-plus/icons-vue'

const router = useRouter()
const loading = ref(false)
const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || ''

interface LoginForm {
  phone: string
  password: string
  identity: string
}

const loginForm = ref<LoginForm>({
  phone: '',
  password: '',
  identity: 'doctor'
})

// 下拉框只区分顶层身份，医生子类型由后端根据账号匹配的 doctor_type 返回。
function resolveIdentity(identity: string): { roleType: number } {
  if (identity === 'admin') return { roleType: 3 }
  return { roleType: 2 }
}

const handleLogin = async (): Promise<void> => {
  if (!loginForm.value.phone) {
    ElMessage.warning('请输入手机号')
    return
  }
  if (!loginForm.value.password) {
    ElMessage.warning('请输入密码')
    return
  }
  if (loginForm.value.password.length < 6) {
    ElMessage.warning('密码长度不能少于6位')
    return
  }

  loading.value = true
  try {
    const { roleType } = resolveIdentity(loginForm.value.identity)
    const res = await axios.post(`${apiBaseUrl}/auth-service/login`, {
      phone: loginForm.value.phone,
      password: loginForm.value.password,
      roleType
    })

    const result = res.data
    if (result.code === 200 && result.data && result.data.token) {
      // result.data 是 { token: "xxx", roleType: N, doctorType: N }，取 token 字段
      console.log("token"+result.data.token)
      sessionStorage.setItem('token', result.data.token)
      // roleType 优先用后端返回值，兜底取解析出的身份
      sessionStorage.setItem('roleType', String(result.data.roleType ?? roleType))
      // 存储 doctorType（医生类型：1看诊 2检查 3检验），后端没返回则默认接诊医生
      sessionStorage.setItem('doctorType', String(result.data.doctorType ?? 1))
      // 同时设置 userRole 供 App.vue 侧边栏角色判断使用
      const role = result.data.roleType ?? roleType
      sessionStorage.setItem('userRole', role === 3 ? 'admin' : 'doctor')

      ElMessage.success('登录成功')

      // 根据角色类型和医生类型跳转不同页面
      if (role === 2) {
        // 医生：根据 doctorType 跳转
        const doctorType = result.data.doctorType ?? 1
        if (doctorType === 2) {
          // 检查医生 → 检查医生首页
          await router.push({ name: 'ExaminationHome' })
        } else if (doctorType === 3) {
          // 检验医生 → 检验医生工作台
          await router.push({ name: 'InspectionHome' })
        } else {
          // 接诊医生 → 医生首页
          await router.push({ name: 'DoctorHome' })
        }
      } else {
        // 管理员 → 管理首页
        await router.push({ name: 'AdminHome' })
      }

    } else {
      ElMessage.error(result.msg || '登录失败')
    }
  } catch (err: any) {
    ElMessage.error('登录失败：' + (err.response?.data?.msg || '账号或密码错误'))
  } finally {
    loading.value = false
  }
}
</script>

<style>
/* 全局视觉修复，防止 backdrop-filter 或 overlay 导致黑色区域透出 */
/* 仅外观相关，不改变组件逻辑或行为 */
html, body, #app {
  height: 100%;
  margin: 0;
  background: #f1f5f9 !important;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

/* 强制 Element Plus 弹层/遮罩透明，避免某些环境下 backdrop-filter 渲染为黑色块 */
.el-overlay,
.el-overlay__mask,
.el-popper,
.el-popover,
.v-overlay,
.popper {
  background: transparent !important;
  backdrop-filter: none !important;
  -webkit-backdrop-filter: none !important;
}

.el-select-dropdown {
  border: 1px solid #dbe3ee !important;
  border-radius: 18px !important;
  box-shadow: 0 18px 48px rgba(28, 44, 68, .16) !important;
  overflow: hidden;
}

.el-select-dropdown__item {
  height: 46px;
  padding: 0 18px;
  color: #516176;
  font-weight: 700;
}

.el-select-dropdown__item.hover,
.el-select-dropdown__item:hover {
  background: #f0f6ff !important;
  color: #315fbb !important;
}

.el-select-dropdown__item.is-selected {
  background: #315fbb !important;
  color: #fff !important;
}

/* 全局背景裁切，减少滤镜引起的合成问题 */
*,
*::before,
*::after {
  background-clip: padding-box;
  -webkit-background-clip: padding-box;
}
</style>

<style scoped>
.login-page {
  width: 100vw;
  height: 100vh;
  background: #f1f5f9;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  position: relative;
}

/* 更精致的背景装饰，消除黑色块 */
.bg-decoration {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background:
      radial-gradient(circle at 0% 0%, rgba(37, 99, 235, 0.04) 0%, transparent 50%),
      radial-gradient(circle at 100% 100%, rgba(37, 99, 235, 0.04) 0%, transparent 50%),
      radial-gradient(circle at 70% 20%, rgba(37, 99, 235, 0.02) 0%, transparent 60%);
  pointer-events: none;
  z-index: 0;
}

.login-card {
  width: 960px;
  height: 560px;
  background: #FFFFFF;
  border-radius: 30px;
  box-shadow: 0 26px 54px rgba(28, 44, 68, 0.12), 0 0 0 1px rgba(78, 101, 132, 0.06);
  display: flex;
  overflow: hidden;
  position: relative;
  z-index: 2; /* 提升以覆盖任何潜在透明层 */
  transition: transform 0.3s ease;
}

/* 左侧品牌栏 - 明确背景色与层级，避免黑色区域 */
.brand-side {
  width: 38%;
  background: linear-gradient(145deg, #315fbb 0%, #3f87dc 100%);
  background-color: #2563eb; /* 明确纯色后备，防止滤镜渲染异常 */
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #FFFFFF;
  padding: 2rem;
  position: relative;
  overflow: hidden;
  z-index: 3; /* 确保在卡片之上显示完整颜色 */
}

/* 装饰性圆形光晕 */
.brand-side::before {
  content: '';
  position: absolute;
  width: 200%;
  height: 200%;
  top: -50%;
  left: -50%;
  background: radial-gradient(circle, rgba(255,255,255,0.06) 0%, rgba(255,255,255,0) 70%);
  pointer-events: none;
}

/* 移除可能导致黑色的 backdrop-filter，仅保留视觉效果 */
.brand-logo {
  width: 72px;
  height: 72px;
  background: rgba(255, 255, 255, 0.12);
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 1.5rem;
  -webkit-backdrop-filter: none !important;
  backdrop-filter: none !important;
  transition: transform 0.3s ease;
}

.brand-logo:hover {
  transform: scale(1.02);
}

.brand-title {
  font-size: 26px;
  font-weight: 700;
  margin: 0 0 0.5rem 0;
  letter-spacing: -0.3px;
  background: linear-gradient(135deg, #fff 0%, rgba(255,255,255,0.9) 100%);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  text-shadow: 0 1px 2px rgba(0,0,0,0.1);
}

.brand-desc {
  font-size: 13px;
  opacity: 0.8;
  margin: 0 0 2rem 0;
  font-weight: 400;
  letter-spacing: 0.3px;
}

.brand-features {
  margin-top: 2rem;
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 12px;
  opacity: 0.95;
  font-weight: 400;
  -webkit-backdrop-filter: none !important;
  backdrop-filter: none !important;
}

.feature-item .el-icon {
  font-size: 14px;
}

/* 右侧表单栏 */
.form-side {
  width: 62%;
  padding: 3rem 3rem;
  display: flex;
  flex-direction: column;
  justify-content: center;
  background: #FFFFFF;
}

.form-header {
  margin-bottom: 2rem;
}

.form-header h2 {
  font-size: 26px;
  font-weight: 600;
  color: #1A1F36;
  margin: 0 0 0.5rem 0;
  letter-spacing: -0.3px;
}

.form-header p {
  font-size: 14px;
  color: #5A6A7E;
  margin: 0;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.login-form :deep(.el-input__wrapper) {
  min-height: 48px;
  border-radius: 16px;
  transition: all 0.2s ease;
  box-shadow: 0 0 0 1px #E2E8F0 inset;
}

.login-form :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #315fbb inset;
}

.login-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 3px rgba(49, 95, 187, 0.14), 0 0 0 1px #315fbb inset;
}

.login-form :deep(.el-select .el-input__wrapper) {
  border-radius: 16px;
}

.login-btn {
  width: 100%;
  height: 50px;
  background: linear-gradient(105deg, #315fbb 0%, #3f87dc 100%);
  border: none;
  border-radius: 16px;
  font-size: 15px;
  font-weight: 800;
  margin-top: 12px;
  transition: all 0.2s ease;
  box-shadow: 0 12px 24px rgba(49, 95, 187, 0.24);
}

.login-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 16px 28px rgba(49, 95, 187, 0.28);
  background: linear-gradient(105deg, #3f87dc 0%, #315fbb 100%);
}

.login-btn:active {
  transform: translateY(0);
}

.form-footer {
  margin-top: 1.5rem;
  text-align: center;
}

.tips {
  font-size: 12px;
  color: #8A99B0;
  background: #F8FAFE;
  padding: 6px 12px;
  border-radius: 20px;
  display: inline-block;
}

/* 响应式微调 */
@media (max-width: 768px) {
  .login-card {
    width: 90%;
    height: auto;
    flex-direction: column;
  }
  .brand-side,
  .form-side {
    width: 100%;
    padding: 2rem;
  }
  .brand-side {
    padding: 2rem 1rem;
  }
  .brand-features {
    display: none;
  }
}
</style>

<template>
  <div class="login-page">
    <div class="ambient-field"></div>

    <div class="login-card">
      <div class="brand-side">
        <div class="brand-top">
          <div class="brand-logo">
            <svg class="brand-logo-mark" width="48" height="48" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M7.2 8.4L12 5.6L16.8 8.4V14L12 16.8L7.2 14V8.4Z" stroke="currentColor" stroke-width="1.55" stroke-linejoin="round" />
              <path d="M9.2 12H14.8M12 9.2V14.8" stroke="#14b8a6" stroke-width="1.7" stroke-linecap="round" />
              <path d="M6.2 6.4L4.6 4.8M17.8 6.4L19.4 4.8M6.2 15.6L4.6 17.2M17.8 15.6L19.4 17.2" stroke="currentColor" stroke-width="1.25" stroke-linecap="round" />
              <circle cx="4.2" cy="4.4" r="1.35" fill="#14b8a6" />
              <circle cx="19.8" cy="4.4" r="1.35" fill="currentColor" />
              <circle cx="4.2" cy="17.6" r="1.35" fill="currentColor" />
              <circle cx="19.8" cy="17.6" r="1.35" fill="#14b8a6" />
            </svg>
          </div>
          <h1 class="brand-title">AI智慧云脑诊疗平台</h1>
          <div class="brand-chips" aria-label="平台能力">
            <span>接诊</span>
            <span>检查</span>
            <span>管理</span>
          </div>
          <div class="brand-insights" aria-label="平台说明">
            <div class="insight-card">
              <h3>AI辅助诊疗</h3>
              <p>智能分析病历与检查结果，辅助医生研判。</p>
            </div>
            <div class="insight-card">
              <h3>云端医疗协同</h3>
              <p>接诊、检查、报告在同一平台联动。</p>
            </div>
          </div>
        </div>
      </div>

      <div class="form-side">
        <div class="form-panel">
          <div class="form-header">
            <h2>欢迎回来</h2>
          </div>

          <el-form :model="loginForm" class="login-form">
            <el-form-item>
              <el-select
                v-model="loginForm.identity"
                placeholder="选择身份"
                style="width: 100%"
                size="large"
                popper-class="login-identity-dropdown"
              >
                <el-option label="医生" value="doctor" />
                <el-option label="管理员" value="admin" />
              </el-select>
            </el-form-item>

            <el-form-item>
              <el-input
                v-model="loginForm.phone"
                placeholder="手机号"
                :prefix-icon="User"
                :formatter="formatPhoneInput"
                :parser="formatPhoneInput"
                inputmode="numeric"
                maxlength="11"
                size="large"
                clearable
                @blur="validatePhoneInput"
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
              <el-button
                type="primary"
                class="login-btn"
                size="large"
                :loading="loading"
                :disabled="!isPhoneValid"
                @click="handleLogin"
              >
                登录系统
              </el-button>
            </el-form-item>
          </el-form>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { Lock, User } from '@element-plus/icons-vue'

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
  identity: 'doctor',
})

const isPhoneValid = computed(() => loginForm.value.phone.length === 11)

function formatPhoneInput(value: string): string {
  return value.replace(/\D/g, '').slice(0, 11)
}

function validatePhoneInput(): void {
  if (loginForm.value.phone && !isPhoneValid.value) {
    ElMessage.warning('请输入 11 位手机号')
  }
}

function resolveIdentity(identity: string): { roleType: number } {
  if (identity === 'admin') return { roleType: 3 }
  return { roleType: 2 }
}

const handleLogin = async (): Promise<void> => {
  if (!loginForm.value.phone) {
    ElMessage.warning('请输入手机号')
    return
  }
  if (!isPhoneValid.value) {
    ElMessage.warning('请输入 11 位手机号')
    return
  }
  if (!loginForm.value.password) {
    ElMessage.warning('请输入密码')
    return
  }
  if (loginForm.value.password.length < 6) {
    ElMessage.warning('密码长度不能少于 6 位')
    return
  }

  loading.value = true
  try {
    const { roleType } = resolveIdentity(loginForm.value.identity)
    const res = await axios.post(`${apiBaseUrl}/auth-service/login`, {
      phone: loginForm.value.phone,
      password: loginForm.value.password,
      roleType,
    })

    const result = res.data
    if (result.code === 200 && result.data && result.data.token) {
      sessionStorage.setItem('token', result.data.token)
      sessionStorage.setItem('roleType', String(result.data.roleType ?? roleType))
      sessionStorage.setItem('doctorType', String(result.data.doctorType ?? 1))

      const role = result.data.roleType ?? roleType
      sessionStorage.setItem('userRole', role === 3 ? 'admin' : 'doctor')

      ElMessage.success('登录成功')

      if (role === 2) {
        const doctorType = result.data.doctorType ?? 1
        if (doctorType === 2) {
          await router.push({ name: 'ExaminationHome' })
        } else if (doctorType === 3) {
          await router.push({ name: 'InspectionHome' })
        } else {
          await router.push({ name: 'DoctorHome' })
        }
      } else {
        await router.push({ name: 'AdminHome' })
      }
    } else {
      ElMessage.error(result.msg || '登录失败')
    }
  } catch (err: any) {
    ElMessage.error(`登录失败：${err.response?.data?.msg || '账号或密码错误'}`)
  } finally {
    loading.value = false
  }
}
</script>

<style>
html,
body,
#app {
  height: 100%;
  margin: 0;
  background: #f7fbff !important;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

.el-overlay,
.el-overlay__mask,
.v-overlay {
  background: transparent !important;
  backdrop-filter: none !important;
  -webkit-backdrop-filter: none !important;
}

.login-identity-dropdown.el-select-dropdown {
  background: #ffffff !important;
  border: 1px solid #dbeafe !important;
  border-radius: 14px !important;
  box-shadow: 0 18px 48px rgba(37, 99, 235, 0.13) !important;
  overflow: hidden;
}

.login-identity-dropdown .el-select-dropdown {
  background: #ffffff !important;
  border: 1px solid #dbeafe !important;
  border-radius: 14px !important;
  box-shadow: 0 18px 48px rgba(37, 99, 235, 0.13) !important;
  overflow: hidden;
}

.login-identity-dropdown .el-select-dropdown__wrap {
  border-radius: inherit;
  overflow: hidden;
}

.login-identity-dropdown .el-select-dropdown__list {
  padding: 0;
}

.login-identity-dropdown .el-select-dropdown__item {
  height: 46px;
  padding: 0 18px;
  color: #475569;
  border-radius: 0;
  font-weight: 700;
}

.login-identity-dropdown .el-select-dropdown__item:first-child {
  border-radius: 12px 12px 0 0;
}

.login-identity-dropdown .el-select-dropdown__item:last-child {
  border-radius: 0 0 12px 12px;
}

.login-identity-dropdown .el-select-dropdown__item.hover,
.login-identity-dropdown .el-select-dropdown__item:hover {
  background: #eef8ff !important;
  color: #2563eb !important;
}

.login-identity-dropdown .el-select-dropdown__item.is-selected {
  background: #315fbb !important;
  color: #fff !important;
}

*,
*::before,
*::after {
  background-clip: padding-box;
  -webkit-background-clip: padding-box;
}
</style>

<style scoped>
.login-page {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100vw;
  min-height: 100vh;
  overflow: hidden;
  padding: 32px;
  background:
    linear-gradient(135deg, rgba(219, 234, 254, 0.78), transparent 42%),
    linear-gradient(315deg, rgba(236, 253, 245, 0.56), transparent 48%),
    #f6f9fc;
}

.ambient-field {
  position: absolute;
  inset: 10%;
  background:
    radial-gradient(circle at 16% 20%, rgba(49, 95, 187, 0.22), transparent 30%),
    radial-gradient(circle at 80% 28%, rgba(63, 135, 220, 0.16), transparent 34%),
    radial-gradient(circle at 58% 82%, rgba(148, 163, 184, 0.18), transparent 36%);
  filter: blur(26px);
  pointer-events: none;
}

.login-card {
  position: relative;
  z-index: 2;
  display: grid;
  grid-template-columns: minmax(430px, 0.96fr) minmax(420px, 1.04fr);
  width: min(1080px, 100%);
  min-height: 540px;
  border-radius: 24px;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.86);
  border: 1px solid rgba(219, 234, 254, 0.9);
  box-shadow: 0 30px 80px rgba(15, 23, 42, 0.14);
  backdrop-filter: blur(18px);
}

.brand-side {
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 46px;
  color: #0f2b55;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.12) 0 1px, transparent 1px 28px),
    radial-gradient(circle at 18% 18%, rgba(255, 255, 255, 0.34), transparent 30%),
    linear-gradient(145deg, #c9dcff 0%, #8fb5f0 46%, #5f86d4 100%);
  background-size: 28px 28px, auto, auto;
}

.brand-side::after {
  position: absolute;
  right: 32px;
  bottom: 34px;
  width: 156px;
  height: 156px;
  content: '';
  background:
    linear-gradient(90deg, rgba(49, 95, 187, 0.22) 1px, transparent 1px),
    linear-gradient(180deg, rgba(49, 95, 187, 0.22) 1px, transparent 1px);
  background-size: 24px 24px;
  mask-image: linear-gradient(135deg, transparent 0%, #000 36%, #000 72%, transparent 100%);
  opacity: 0.62;
  pointer-events: none;
}

.brand-top {
  position: relative;
  z-index: 1;
  max-width: 430px;
}

.brand-logo {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 84px;
  height: 84px;
  margin-bottom: 30px;
  border-radius: 22px;
  color: #2b6fbd;
  background: rgba(255, 255, 255, 0.34);
  border: 1px solid rgba(255, 255, 255, 0.42);
  box-shadow: none;
}

.brand-title {
  margin: 0;
  font-size: 32px;
  line-height: 1.2;
  letter-spacing: 0;
  color: #0f2b55;
  white-space: nowrap;
}

.brand-logo svg {
  width: 46px;
  height: 46px;
}

.brand-chips {
  display: flex;
  gap: 10px;
  margin-top: 26px;
}

.brand-chips span {
  display: inline-flex;
  align-items: center;
  height: 32px;
  padding: 0 14px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.76);
  border: 1px solid rgba(49, 95, 187, 0.22);
  color: #214f9a;
  font-size: 13px;
  font-weight: 700;
}

.brand-insights {
  display: grid;
  gap: 14px;
  margin-top: 88px;
}

.insight-card {
  padding: 16px 20px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.16);
  border: 1px solid rgba(255, 255, 255, 0.24);
}

.insight-card:nth-child(2) {
  background: rgba(33, 79, 154, 0.2);
}

.insight-card h3 {
  margin: 0;
  color: #ffffff;
  font-size: 17px;
  line-height: 1.25;
  font-weight: 500;
}

.insight-card p {
  margin: 10px 0 0;
  color: rgba(255, 255, 255, 0.86);
  font-size: 13px;
  line-height: 1.45;
  font-weight: 400;
}

.form-side {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 46px;
  background: rgba(255, 255, 255, 0.84);
}

.form-panel {
  width: min(390px, 100%);
}

.form-header {
  margin-bottom: 28px;
}

.form-header h2 {
  margin: 0;
  color: #12325f;
  font-size: 34px;
  letter-spacing: 0;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.login-form :deep(.el-form-item) {
  margin-bottom: 0;
}

.login-form :deep(.el-input__wrapper),
.login-form :deep(.el-select .el-input__wrapper) {
  min-height: 54px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 0 0 1px #dbeafe inset;
  transition: box-shadow 0.2s ease, transform 0.2s ease;
}

.login-form :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #38bdf8 inset;
}

.login-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 4px rgba(56, 189, 248, 0.16), 0 0 0 1px #38bdf8 inset;
}

.login-btn {
  width: 100%;
  height: 54px;
  border: none;
  border-radius: 14px;
  background: linear-gradient(105deg, #315fbb 0%, #3f87dc 100%);
  box-shadow: 0 18px 34px rgba(49, 95, 187, 0.24);
  font-size: 15px;
  font-weight: 800;
  transition: transform 0.18s ease, box-shadow 0.18s ease;
}

.login-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 22px 40px rgba(49, 95, 187, 0.3);
}

@media (max-width: 900px) {
  .login-card {
    grid-template-columns: 1fr;
    min-height: auto;
  }

  .brand-side,
  .form-side {
    padding: 34px;
  }
}

@media (max-width: 640px) {
  .login-page {
    padding: 16px;
  }

  .form-side,
  .brand-side,
  .form-panel {
    padding: 22px;
  }

  .brand-title {
    font-size: 28px;
  }
}
</style>

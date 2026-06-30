<template>
  <div class="page">
    <header class="page-top">
      <div>
        <h2>医生个人信息</h2>
        <p class="top-sub">维护执业资料、擅长方向和账号安全信息</p>
      </div>
      <el-button type="primary" size="large" @click="handleSave" :loading="saving" round>保存资料</el-button>
    </header>

    <div class="content-grid">
      <section class="card">
        <div class="card-head">基本信息</div>
        <div class="avatar-section">
          <div class="avatar-wrap" @click="triggerUpload">
            <img v-if="form.avatar" :src="form.avatar" class="avatar-img" />
            <el-icon v-else :size="32" color="#94a3b8"><Plus /></el-icon>
            <div class="avatar-overlay"><el-icon :size="16"><Camera /></el-icon></div>
          </div>
          <input ref="fileInput" type="file" accept="image/*" hidden @change="onFileChange" />
          <div>
            <div class="doctor-name">{{ form.name || '未设置姓名' }}</div>
            <div class="doctor-dept">{{ form.deptName || '未知科室' }} · {{ form.position || '未知职称' }}</div>
          </div>
        </div>

        <el-form :model="form" label-position="top" class="profile-form">
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="姓名"><el-input v-model="form.name" disabled /></el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="科室"><el-input :model-value="form.deptName" disabled /></el-form-item>
            </el-col>
            <el-col :span="24">
              <el-form-item label="擅长领域">
                <el-input v-model="form.goodAt" type="textarea" :rows="2" placeholder="例如：脑血管病、头痛眩晕、癫痫诊疗" />
              </el-form-item>
            </el-col>
            <el-col :span="24">
              <el-form-item label="个人简介">
                <el-input v-model="form.introduction" type="textarea" :rows="3" placeholder="介绍从医经历、专业方向和门诊特色" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="电子邮箱"><el-input v-model="form.email" placeholder="doctor@cloudbrainmed.com" /></el-form-item>
            </el-col>
          </el-row>
        </el-form>
      </section>

      <section class="card security-card">
        <div class="card-head">安全设置</div>
        <el-form :model="pwdForm" label-position="top" ref="pwdFormRef">
          <el-form-item label="原密码" prop="oldPassword" :rules="[{ required: true, message: '请输入原密码' }]">
            <el-input v-model="pwdForm.oldPassword" type="password" show-password placeholder="输入原密码" />
          </el-form-item>
          <el-form-item label="新密码" prop="newPassword" :rules="[{ required: true, min: 6, message: '密码至少6位' }]">
            <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="设置新密码" />
          </el-form-item>
        </el-form>
        <el-button type="warning" @click="handleChangePwd" :loading="changingPwd" plain>修改密码</el-button>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Camera, Plus } from '@element-plus/icons-vue'
import { getDoctorInfo, updateDoctorProfile, uploadDoctorAvatar, changeDoctorPassword } from '@/api/doctor/profile'

const pwdFormRef = ref()
const fileInput = ref<HTMLInputElement>()
const saving = ref(false)
const changingPwd = ref(false)

const form = reactive({ name: '', deptName: '', position: '', avatar: '', goodAt: '', introduction: '', email: '' })
const pwdForm = reactive({ oldPassword: '', newPassword: '' })

onMounted(async () => {
  try {
    const res = await getDoctorInfo()
    Object.assign(form, res.data)
  } catch (e: any) {
    showActionError(e, '加载医生资料失败')
  }
})

function triggerUpload() {
  fileInput.value?.click()
}

async function onFileChange(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  const validationMessage = validateAvatarFile(file)
  if (validationMessage) {
    ElMessage.warning(validationMessage)
    input.value = ''
    return
  }
  try {
    const res = await uploadDoctorAvatar(file)
    form.avatar = res.data.avatarUrl
    ElMessage.success('头像已更新')
  } catch (e: any) {
    showActionError(e, '头像上传失败')
  } finally {
    input.value = ''
  }
}

async function handleSave() {
  saving.value = true
  try {
    await updateDoctorProfile({
      goodAt: form.goodAt.trim(),
      introduction: form.introduction.trim(),
      email: form.email.trim()
    })
    ElMessage.success('资料已保存')
  } catch (e: any) {
    showActionError(e, '资料保存失败')
  } finally {
    saving.value = false
  }
}

async function handleChangePwd() {
  const valid = await pwdFormRef.value?.validate().catch(() => false)
  if (!valid) return
  changingPwd.value = true
  try {
    await changeDoctorPassword({ ...pwdForm })
    ElMessage.success('密码已修改')
    pwdForm.oldPassword = ''
    pwdForm.newPassword = ''
  } catch (e: any) {
    showActionError(e, '密码修改失败')
  } finally {
    changingPwd.value = false
  }
}

function validateAvatarFile(file: File) {
  if (!file.type.startsWith('image/')) return '请选择图片文件'
  if (file.size > 2 * 1024 * 1024) return '头像图片不能超过 2MB'
  return ''
}

function showActionError(error: any, fallbackMessage: string) {
  ElMessage.error(error?.message || fallbackMessage)
}
</script>

<style scoped>
.page { padding: 28px 36px; }
.page-top { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 28px; }
.page-top h2 { font-size: 22px; font-weight: 700; color: #0f172a; margin: 0; }
.top-sub { font-size: 13px; color: #64748b; margin: 6px 0 0; }
.content-grid { display: grid; grid-template-columns: minmax(0, 1fr) 360px; gap: 20px; align-items: start; }
.card { background: #fff; border-radius: var(--radius); padding: 24px; box-shadow: var(--shadow); }
.card-head { font-size: 15px; font-weight: 700; color: #1e293b; margin-bottom: 20px; padding-bottom: 14px; border-bottom: 1px solid #f1f5f9; }
.avatar-section { display: flex; align-items: center; gap: 16px; margin-bottom: 24px; }
.avatar-wrap { width: 72px; height: 72px; border-radius: 50%; background: #f1f5f9; display: flex; align-items: center; justify-content: center; cursor: pointer; position: relative; overflow: hidden; border: 3px solid #e2e8f0; transition: border-color .2s; }
.avatar-wrap:hover { border-color: var(--brand); }
.avatar-img { width: 100%; height: 100%; object-fit: cover; }
.avatar-overlay { position: absolute; inset: 0; background: rgba(15, 23, 42, .42); display: flex; align-items: center; justify-content: center; opacity: 0; transition: opacity .2s; color: #fff; }
.avatar-wrap:hover .avatar-overlay { opacity: 1; }
.doctor-name { font-size: 17px; font-weight: 700; color: #1e293b; }
.doctor-dept { font-size: 13px; color: #64748b; margin-top: 3px; }
.profile-form :deep(.el-form-item) { margin-bottom: 16px; }
.profile-form :deep(.el-form-item__label), .security-card :deep(.el-form-item__label) { font-size: 13px; font-weight: 600; color: #64748b; padding-bottom: 4px; }
@media (max-width: 960px) {
  .page { padding: 20px; }
  .content-grid { grid-template-columns: 1fr; }
}
</style>

<template>
  <div class="page">
    <header class="page-top">
      <div>
        <h2>医生个人信息</h2>
        <p class="top-sub">维护执业资料、擅长方向和账号安全信息</p>
      </div>
      <div class="top-actions">
        <el-button type="warning" size="large" @click="openPasswordDialog" plain round>修改密码</el-button>
        <el-button type="primary" size="large" @click="handleSave" :loading="saving" round>保存资料</el-button>
      </div>
    </header>

    <div class="content-grid">
      <section class="card profile-card">
        <div class="card-head">
          <div>
            <span>基本信息</span>
            <small>用于医生端名片、接诊工作台和资料完善提醒</small>
          </div>
          <span class="head-badge">执业资料</span>
        </div>
        <div class="avatar-section">
          <div class="avatar-wrap" @click="triggerUpload">
            <img v-if="form.avatar" :src="form.avatar" class="avatar-img" />
            <el-icon v-else :size="32" color="#94a3b8"><Plus /></el-icon>
            <div class="avatar-overlay"><el-icon :size="16"><Camera /></el-icon></div>
          </div>
          <input ref="fileInput" type="file" accept="image/*" hidden @change="onFileChange" />
          <div class="doctor-summary">
            <div class="doctor-name-line">
              <div class="doctor-name">{{ form.name || '未设置姓名' }}</div>
              <span class="doctor-chip">点击头像更新</span>
            </div>
            <div class="doctor-dept">{{ form.deptName || '未知科室' }} · {{ form.position || '未知职称' }}</div>
            <p class="doctor-note">头像上传成功后会立即同步到当前医生账号展示位，支持 2MB 内图片。</p>
            <div class="summary-tags">
              <span>{{ form.deptName || '待维护科室' }}</span>
              <span>{{ form.position || '待维护职称' }}</span>
            </div>
          </div>
        </div>

        <div class="editor-surface">
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
              <el-form-item label="电子邮箱">
                <div class="email-edit-row">
                  <el-input v-model="form.email" placeholder="doctor@cloudbrainmed.com" />
                </div>
              </el-form-item>
            </el-col>
          </el-row>
          </el-form>
        </div>
      </section>
    </div>

    <el-dialog
      v-model="passwordDialogVisible"
      width="520px"
      class="password-dialog"
      :show-close="false"
      @closed="resetPasswordForm"
    >
      <template #header>
        <div class="password-dialog-head">
          <strong>安全设置</strong>
          <span class="password-dialog-tip">修改后下次登录立即生效</span>
        </div>
      </template>

      <div class="password-panel">
        <el-form :model="pwdForm" :rules="passwordRules" label-position="top" ref="pwdFormRef">
          <el-form-item label="原密码" prop="oldPassword">
            <el-input v-model="pwdForm.oldPassword" type="password" show-password placeholder="输入原密码" />
          </el-form-item>
          <el-form-item label="新密码" prop="newPassword">
            <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="设置新密码" />
          </el-form-item>
          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input v-model="pwdForm.confirmPassword" type="password" show-password placeholder="再次输入新密码" />
          </el-form-item>
        </el-form>
      </div>

      <template #footer>
        <el-button @click="passwordDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleChangePwd" :loading="changingPwd">确认修改</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Camera, Plus } from '@element-plus/icons-vue'
import { getDoctorInfo, updateDoctorProfile, uploadDoctorAvatar, changeDoctorPassword } from '@/api/doctor/profile'
import { resolveAssetUrl } from '@/utils/assets'

interface DoctorProfileInfo {
  name: string
  deptName: string
  position: string
  avatar: string
  goodAt: string
  introduction: string
  email: string
}

const pwdFormRef = ref()
const fileInput = ref<HTMLInputElement>()
const saving = ref(false)
const changingPwd = ref(false)
const passwordDialogVisible = ref(false)

const form = reactive({ name: '', deptName: '', position: '', avatar: '', goodAt: '', introduction: '', email: '' })
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

const passwordRules = {
  oldPassword: [{ required: true, message: '请输入原密码' }],
  newPassword: [{ required: true, min: 6, message: '密码至少6位' }],
  confirmPassword: [{ required: true, validator: validateConfirmPassword, trigger: 'blur' }],
}

onMounted(async () => {
  try {
    const res = await getDoctorInfo()
    applyDoctorInfo(res.data)
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
    form.avatar = resolveAssetUrl(res.data.avatarUrl)
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
    await changeDoctorPassword({
      oldPassword: pwdForm.oldPassword,
      newPassword: pwdForm.newPassword,
    })
    ElMessage.success('密码已修改')
    passwordDialogVisible.value = false
    resetPasswordForm()
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

function applyDoctorInfo(data: Partial<DoctorProfileInfo>) {
  Object.assign(form, data, {
    avatar: resolveAssetUrl(data.avatar),
  })
}

function showActionError(error: any, fallbackMessage: string) {
  ElMessage.error(error?.message || fallbackMessage)
}

function openPasswordDialog() {
  passwordDialogVisible.value = true
}

function validateConfirmPassword(rule: unknown, value: string, callback: (error?: Error) => void) {
  if (!value) {
    callback(new Error('请再次输入新密码'))
    return
  }
  if (value !== pwdForm.newPassword) {
    callback(new Error('两次输入的新密码不一致'))
    return
  }
  callback()
}

function resetPasswordForm() {
  pwdForm.oldPassword = ''
  pwdForm.newPassword = ''
  pwdForm.confirmPassword = ''
  pwdFormRef.value?.clearValidate?.()
}
</script>

<style scoped>
.page { padding: 28px 36px; }
.page-top { display: flex; justify-content: space-between; align-items: flex-start; gap: 16px; margin-bottom: 28px; }
.page-top h2 { font-size: 22px; font-weight: 700; color: #0f172a; margin: 0; }
.top-sub { font-size: 13px; color: #64748b; margin: 6px 0 0; }
.top-actions { display: flex; gap: 10px; flex-wrap: wrap; justify-content: flex-end; }
.content-grid { display: grid; grid-template-columns: minmax(0, 1fr); gap: 20px; align-items: start; }
.card { background: #fff; border-radius: 28px; padding: 24px; box-shadow: 0 20px 45px rgba(31, 41, 55, .08); border: 1px solid rgba(214, 226, 240, .8); }
.profile-card { background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%); }
.card-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; margin-bottom: 20px; padding-bottom: 16px; border-bottom: 1px solid #edf2f7; }
.card-head span { display: block; font-size: 16px; font-weight: 800; color: #16304d; }
.card-head small { display: block; margin-top: 5px; font-size: 12px; font-weight: 600; color: #7b8aa0; }
.head-badge { display: inline-flex; align-items: center; height: 30px; padding: 0 12px; border-radius: 999px; background: rgba(49, 95, 187, .1); color: #315fbb !important; font-size: 12px !important; font-weight: 700 !important; }
.avatar-section { display: flex; align-items: center; gap: 18px; margin-bottom: 24px; padding: 18px; border-radius: 24px; background: linear-gradient(135deg, rgba(237, 244, 255, .95), rgba(248, 251, 255, .92)); border: 1px solid rgba(199, 216, 237, .85); }
.avatar-wrap { width: 88px; height: 88px; border-radius: 28px; background: linear-gradient(180deg, #ffffff, #eaf1fb); display: flex; align-items: center; justify-content: center; cursor: pointer; position: relative; overflow: hidden; border: 1px solid rgba(176, 197, 226, .9); box-shadow: inset 0 1px 0 rgba(255, 255, 255, .85), 0 12px 28px rgba(49, 95, 187, .12); transition: transform .18s ease, box-shadow .18s ease, border-color .18s ease; }
.avatar-wrap:hover { transform: translateY(-1px); border-color: rgba(79, 141, 247, .88); box-shadow: inset 0 1px 0 rgba(255, 255, 255, .92), 0 16px 34px rgba(49, 95, 187, .18); }
.avatar-img { width: 100%; height: 100%; object-fit: cover; }
.avatar-overlay { position: absolute; inset: 0; background: linear-gradient(180deg, rgba(15, 23, 42, .08), rgba(15, 23, 42, .55)); display: flex; align-items: center; justify-content: center; opacity: 0; transition: opacity .2s; color: #fff; }
.avatar-wrap:hover .avatar-overlay { opacity: 1; }
.doctor-summary { min-width: 0; }
.doctor-name-line { display: flex; align-items: center; flex-wrap: wrap; gap: 10px; }
.doctor-name { font-size: 28px; font-weight: 800; color: #16304d; letter-spacing: -.02em; }
.doctor-chip { display: inline-flex; align-items: center; height: 28px; padding: 0 10px; border-radius: 999px; background: rgba(15, 118, 110, .1); color: #0f766e; font-size: 12px; font-weight: 700; }
.doctor-dept { font-size: 15px; color: #5c708c; margin-top: 6px; font-weight: 700; }
.doctor-note { margin: 10px 0 0; font-size: 13px; line-height: 1.6; color: #71839b; }
.summary-tags { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 14px; }
.summary-tags span { display: inline-flex; align-items: center; padding: 7px 12px; border-radius: 999px; background: #fff; border: 1px solid rgba(205, 216, 231, .95); color: #48617e; font-size: 12px; font-weight: 700; }
.editor-surface { padding: 18px; border-radius: 24px; background: rgba(255, 255, 255, .78); border: 1px solid rgba(219, 231, 245, .92); backdrop-filter: blur(6px); }
.profile-form :deep(.el-form-item) { margin-bottom: 18px; }
.profile-form :deep(.el-form-item__label) { font-size: 13px; font-weight: 700; color: #5f718a; padding-bottom: 6px; }
.profile-form :deep(.el-input__wrapper),
.profile-form :deep(.el-textarea__inner) {
  border-radius: 16px;
  background: #fcfdff;
  box-shadow: 0 0 0 1px #d8e2ee inset;
}
.profile-form :deep(.el-input__wrapper) { min-height: 48px; padding: 0 14px; }
.profile-form :deep(.el-textarea__inner) { min-height: 116px !important; padding: 14px 16px; line-height: 1.7; }
.profile-form :deep(.el-input__wrapper.is-focus),
.profile-form :deep(.el-textarea__inner:focus) { box-shadow: 0 0 0 3px rgba(79, 141, 247, .14), 0 0 0 1px #4f8df7 inset; }
.email-edit-row { display: grid; grid-template-columns: minmax(0, 1fr); width: 100%; }
.password-dialog-head { display: flex; flex-direction: column; gap: 10px; }
.password-dialog-head strong { color: #142a46; font-size: 24px; font-weight: 800; line-height: 1.15; }
.password-dialog-tip { color: #7d8da5; font-size: 14px; font-weight: 700; }
.password-dialog :deep(.el-form-item) { margin-bottom: 24px; }
.password-dialog :deep(.el-button) { min-height: 48px; border-radius: 16px; font-weight: 700; }
.password-panel {
  padding: 26px 28px 8px;
  border-radius: 28px;
  border: 1px solid rgba(220, 228, 239, .96);
  background: linear-gradient(180deg, rgba(255, 255, 255, .96), rgba(255, 251, 245, .9));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, .9), 0 16px 36px rgba(49, 95, 187, .09);
}
:global(.password-dialog .el-dialog) {
  border-radius: 36px;
  overflow: hidden;
  border: 1px solid rgba(214, 226, 240, .92);
  background: linear-gradient(180deg, #ffffff 0%, #fffdfa 100%);
  box-shadow: 0 28px 70px rgba(31, 41, 55, .18);
}
:global(.password-dialog .el-dialog__header) {
  margin: 0;
  padding: 34px 36px 22px;
  border-bottom: 1px solid #edf2f7;
  background: linear-gradient(180deg, #ffffff 0%, #fbfdff 100%);
}
:global(.password-dialog .el-dialog__title) {
  font-size: 20px;
  font-weight: 800;
  color: #16304d;
}
:global(.password-dialog .el-dialog__headerbtn) {
  top: 18px;
  right: 20px;
  width: 36px;
  height: 36px;
  border-radius: 12px;
}
:global(.password-dialog .el-dialog__headerbtn:hover) {
  background: rgba(49, 95, 187, .08);
}
:global(.password-dialog .el-dialog__body) {
  padding: 28px 36px 12px;
}
:global(.password-dialog .el-dialog__footer) {
  padding: 18px 36px 30px;
  border-top: 0;
  background: rgba(255, 253, 250, .9);
}
:global(.password-dialog .el-form-item__label) {
  color: #5f718a;
  font-size: 15px;
  font-weight: 700;
  padding-bottom: 10px;
}
:global(.password-dialog .el-form-item.is-required:not(.is-no-asterisk).asterisk-left > .el-form-item__label::before) {
  color: #e58b7d;
  margin-right: 8px;
}
:global(.password-dialog .el-input__wrapper) {
  min-height: 60px;
  padding: 0 20px;
  border-radius: 22px;
  background: #fcfdff;
  box-shadow: 0 0 0 1px #dce5f0 inset;
}
:global(.password-dialog .el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 4px rgba(84, 132, 245, .16), 0 0 0 2px #6f8ff8 inset;
}
:global(.password-dialog .el-button) {
  min-height: 48px;
  padding: 0 22px;
  border-radius: 16px;
  font-weight: 700;
}
:global(.password-dialog .el-button--primary) {
  border-color: #edd69f;
  background: #fffaf2;
  color: #c29135;
  box-shadow: 0 10px 22px rgba(194, 145, 53, .1);
}
:global(.password-dialog .el-button--primary:hover),
:global(.password-dialog .el-button--primary:focus) {
  border-color: #dfbd72;
  background: #fff4df;
  color: #a87422;
}
@media (max-width: 960px) {
  .page { padding: 20px; }
  .avatar-section { align-items: flex-start; }
  .doctor-name { font-size: 24px; }
}
@media (max-width: 680px) {
  .page-top { flex-direction: column; gap: 12px; }
  .top-actions { justify-content: flex-start; }
  .avatar-section { flex-direction: column; }
  .editor-surface { padding: 16px; }
  .email-edit-row { grid-template-columns: 1fr; }
}
</style>

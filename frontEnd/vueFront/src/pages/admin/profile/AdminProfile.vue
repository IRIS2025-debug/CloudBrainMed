<template>
  <div class="page">
    <header class="page-top">
      <div>
        <h2>管理员个人信息</h2>
        <p class="top-sub">维护账户资料、头像和安全设置，不改变现有资料保存与密码修改流程。</p>
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
            <span>账户概览</span>
            <small>头像会同步显示在管理员端工作台中，支持点击上传更新。</small>
          </div>
          <span class="head-badge">管理员账号</span>
        </div>

        <div class="avatar-section">
          <div class="avatar-wrap" @click="triggerUpload">
            <img v-if="form.avatar" :src="form.avatar" class="avatar-img" />
            <el-icon v-else :size="32" color="#94a3b8"><Plus /></el-icon>
            <div class="avatar-overlay"><el-icon :size="16"><Camera /></el-icon></div>
          </div>
          <input ref="fileInput" type="file" accept="image/*" hidden @change="onFileChange" />

          <div class="admin-summary">
            <div class="admin-name-line">
              <div class="admin-name">{{ form.name || '未设置姓名' }}</div>
              <span class="admin-chip">点击头像更新</span>
            </div>
            <div class="admin-position">{{ form.position || '管理员' }}</div>
            <p class="admin-note">建议上传清晰头像，大小不超过 2MB，上传成功后会立即同步到当前账号展示位。</p>
            <div class="summary-tags">
              <span>{{ form.position || '默认管理员角色' }}</span>
              <span>{{ form.phone || '待补充手机号' }}</span>
            </div>
          </div>
        </div>

        <div class="editor-surface">
          <div class="editor-head">
            <strong>基础资料</strong>
            <p>姓名和职位保持只读，联系方式仍按原有接口保存。</p>
          </div>

          <el-form :model="form" label-position="top" class="profile-form">
            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item label="姓名">
                  <el-input v-model="form.name" disabled />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="职位">
                  <el-input :model-value="form.position" disabled />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="手机号">
                  <el-input v-model="form.phone" placeholder="请输入手机号" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="电子邮箱">
                  <div class="email-edit-row">
                    <el-input v-model="form.email" placeholder="admin@cloudbrainmed.com" />
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
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Camera, Plus } from '@element-plus/icons-vue'
import { changeAdminPassword, getAdminInfo, updateAdminProfile, uploadAdminAvatar } from '@/api/admin/profile'
import { resolveAssetUrl } from '@/utils/assets'

interface AdminProfileForm {
  name: string
  position: string
  avatar: string
  phone: string
  email: string
}

const pwdFormRef = ref()
const fileInput = ref<HTMLInputElement>()
const saving = ref(false)
const changingPwd = ref(false)
const passwordDialogVisible = ref(false)

const form = reactive<AdminProfileForm>({
  name: '',
  position: '',
  avatar: '',
  phone: '',
  email: '',
})
const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const passwordRules = {
  oldPassword: [{ required: true, message: '请输入原密码' }],
  newPassword: [{ required: true, min: 6, message: '密码至少 6 位' }],
  confirmPassword: [{ required: true, validator: validateConfirmPassword, trigger: 'blur' }],
}

onMounted(async () => {
  try {
    const res = await getAdminInfo()
    applyProfileInfo(res.data)
  } catch (e: any) {
    showActionError(e, '加载管理员资料失败')
  }
})

function applyProfileInfo(data: Partial<AdminProfileForm> | undefined) {
  Object.assign(form, data, {
    avatar: resolveAssetUrl(data?.avatar),
  })
}

function triggerUpload() {
  fileInput.value?.click()
}

async function onFileChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return

  const validationMessage = validateAvatarFile(file)
  if (validationMessage) {
    ElMessage.warning(validationMessage)
    input.value = ''
    return
  }

  try {
    const res = await uploadAdminAvatar(file)
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
    await updateAdminProfile({
      email: form.email.trim(),
      phone: form.phone.trim(),
    })
    ElMessage.success('资料保存成功')
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
    await changeAdminPassword({
      oldPassword: pwdForm.oldPassword,
      newPassword: pwdForm.newPassword,
    })
    ElMessage.success('密码修改成功')
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
.page {
  padding: 28px 36px;
}

.page-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 28px;
}

.page-top h2 {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  color: #0f172a;
}

.top-sub {
  margin: 6px 0 0;
  font-size: 13px;
  color: #64748b;
}

.top-actions {
  display: flex;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 10px;
}

.content-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 20px;
  align-items: start;
}

.card {
  border-radius: 28px;
  padding: 24px;
  background: #fff;
  border: 1px solid rgba(214, 226, 240, 0.85);
  box-shadow: 0 20px 45px rgba(31, 41, 55, 0.08);
}

.profile-card {
  background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
}

.card-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #edf2f7;
}

.card-head span {
  display: block;
  color: #16304d;
  font-size: 16px;
  font-weight: 800;
}

.card-head small {
  display: block;
  margin-top: 5px;
  color: #7b8aa0;
  font-size: 12px;
  font-weight: 600;
}

.head-badge {
  display: inline-flex;
  align-items: center;
  height: 30px;
  padding: 0 12px;
  border-radius: 999px;
  background: rgba(49, 95, 187, 0.1);
  color: #315fbb !important;
  font-size: 12px !important;
  font-weight: 700 !important;
}

.avatar-section {
  display: flex;
  align-items: center;
  gap: 18px;
  margin-bottom: 24px;
  padding: 18px;
  border-radius: 24px;
  background: linear-gradient(135deg, rgba(237, 244, 255, 0.95), rgba(248, 251, 255, 0.92));
  border: 1px solid rgba(199, 216, 237, 0.85);
}

.avatar-wrap {
  width: 88px;
  height: 88px;
  border-radius: 28px;
  background: linear-gradient(180deg, #ffffff, #eaf1fb);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  border: 1px solid rgba(176, 197, 226, 0.9);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.85), 0 12px 28px rgba(49, 95, 187, 0.12);
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;
}

.avatar-wrap:hover {
  transform: translateY(-1px);
  border-color: rgba(79, 141, 247, 0.88);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.92), 0 16px 34px rgba(49, 95, 187, 0.18);
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(15, 23, 42, 0.08), rgba(15, 23, 42, 0.55));
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.2s;
  color: #fff;
}

.avatar-wrap:hover .avatar-overlay {
  opacity: 1;
}

.admin-summary {
  min-width: 0;
}

.admin-name-line {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.admin-name {
  font-size: 28px;
  font-weight: 800;
  color: #16304d;
  letter-spacing: -0.02em;
}

.admin-chip {
  display: inline-flex;
  align-items: center;
  height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  background: rgba(15, 118, 110, 0.1);
  color: #0f766e;
  font-size: 12px;
  font-weight: 700;
}

.admin-position {
  margin-top: 6px;
  color: #5c708c;
  font-size: 15px;
  font-weight: 700;
}

.admin-note {
  margin: 10px 0 0;
  color: #71839b;
  font-size: 13px;
  line-height: 1.6;
}

.summary-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 14px;
}

.summary-tags span {
  display: inline-flex;
  align-items: center;
  padding: 7px 12px;
  border-radius: 999px;
  background: #fff;
  border: 1px solid rgba(205, 216, 231, 0.95);
  color: #48617e;
  font-size: 12px;
  font-weight: 700;
}

.editor-surface {
  border-radius: 24px;
  border: 1px solid rgba(219, 231, 245, 0.92);
  background: rgba(255, 255, 255, 0.8);
}

.editor-surface {
  padding: 18px;
}

.editor-head {
  margin-bottom: 16px;
}

.editor-head strong {
  display: block;
  color: #16304d;
  font-size: 15px;
  font-weight: 800;
}

.editor-head p {
  margin: 6px 0 0;
  color: #71839b;
  font-size: 13px;
  line-height: 1.65;
}

.profile-form :deep(.el-form-item) {
  margin-bottom: 18px;
}

.profile-form :deep(.el-form-item__label) {
  color: #5f718a;
  font-size: 13px;
  font-weight: 700;
  padding-bottom: 6px;
}

.profile-form :deep(.el-input__wrapper) {
  min-height: 44px;
  border-radius: 16px;
  box-shadow: 0 0 0 1px #d9e4f0 inset;
}

.profile-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 3px rgba(49, 95, 187, 0.12), 0 0 0 1px #315fbb inset;
}

.email-edit-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  width: 100%;
}

.password-dialog :deep(.el-button) {
  min-height: 48px;
  border-radius: 16px;
  font-weight: 700;
}

.password-dialog-head {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.password-dialog-head strong {
  color: #142a46;
  font-size: 24px;
  font-weight: 800;
  line-height: 1.15;
}

.password-dialog-tip {
  color: #7d8da5;
  font-size: 14px;
  font-weight: 700;
}

.password-dialog :deep(.el-form-item) {
  margin-bottom: 24px;
}

.password-panel {
  padding: 26px 28px 8px;
  border-radius: 28px;
  border: 1px solid rgba(220, 228, 239, 0.96);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(255, 251, 245, 0.9));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.9), 0 16px 36px rgba(49, 95, 187, 0.09);
}

:global(.password-dialog .el-dialog) {
  border-radius: 36px;
  overflow: hidden;
  border: 1px solid rgba(214, 226, 240, 0.92);
  background: linear-gradient(180deg, #ffffff 0%, #fffdfa 100%);
  box-shadow: 0 28px 70px rgba(31, 41, 55, 0.18);
}

:global(.password-dialog .el-dialog__header) {
  margin: 0;
  padding: 34px 36px 22px;
  border-bottom: 1px solid #edf2f7;
  background: linear-gradient(180deg, #ffffff 0%, #fbfdff 100%);
}

:global(.password-dialog .el-dialog__title) {
  color: #16304d;
  font-size: 20px;
  font-weight: 800;
}

:global(.password-dialog .el-dialog__headerbtn) {
  top: 18px;
  right: 20px;
  width: 36px;
  height: 36px;
  border-radius: 12px;
}

:global(.password-dialog .el-dialog__headerbtn:hover) {
  background: rgba(49, 95, 187, 0.08);
}

:global(.password-dialog .el-dialog__body) {
  padding: 28px 36px 12px;
}

:global(.password-dialog .el-dialog__footer) {
  padding: 18px 36px 30px;
  border-top: 0;
  background: rgba(255, 253, 250, 0.9);
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
  box-shadow: 0 0 0 4px rgba(84, 132, 245, 0.16), 0 0 0 2px #6f8ff8 inset;
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
  box-shadow: 0 10px 22px rgba(194, 145, 53, 0.1);
}

:global(.password-dialog .el-button--primary:hover),
:global(.password-dialog .el-button--primary:focus) {
  border-color: #dfbd72;
  background: #fff4df;
  color: #a87422;
}

@media (max-width: 960px) {
  .content-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .page {
    padding: 18px 16px;
  }

  .page-top {
    flex-direction: column;
    align-items: stretch;
  }

  .top-actions {
    justify-content: flex-start;
  }

  .card {
    padding: 20px;
  }

  .avatar-section {
    flex-direction: column;
    align-items: flex-start;
  }

  .email-edit-row {
    grid-template-columns: 1fr;
  }
}
</style>

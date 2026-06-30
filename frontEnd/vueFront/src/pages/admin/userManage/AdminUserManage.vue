<template>
  <div class="page">
    <!-- 页面头部 -->
    <header class="page-top">
      <div class="page-top-left">
        <div class="page-icon">
          <el-icon :size="22"><UserFilled /></el-icon>
        </div>
        <div>
          <h2>账号权限管理</h2>
          <p class="top-sub">管理平台所有医生的账号信息与权限</p>
        </div>
      </div>
      <div class="header-actions">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索医生姓名 / 手机号…"
          clearable
          prefix-icon="Search"
          class="search-input"
          @input="handleSearch"
        />
        <el-button type="primary" size="large" @click="openAddDialog" round>
          <el-icon><Plus /></el-icon>
          <span>新增医生</span>
        </el-button>
      </div>
    </header>

    <!-- 统计卡片 -->
    <div class="stat-row">
      <div class="stat-card stat-total">
        <div class="stat-icon-box">
          <el-icon :size="22"><User /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-num">{{ doctors.length }}</div>
          <div class="stat-label">医生总数</div>
        </div>
        <div class="stat-trend">
          <span class="trend-dot"></span>
        </div>
      </div>
      <div class="stat-card stat-active">
        <div class="stat-icon-box">
          <el-icon :size="22"><CircleCheckFilled /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-num">{{ doctors.filter(d => d.status === 1).length }}</div>
          <div class="stat-label">已启用</div>
        </div>
      </div>
      <div class="stat-card stat-inactive">
        <div class="stat-icon-box">
          <el-icon :size="22"><CircleCloseFilled /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-num">{{ doctors.filter(d => d.status === 0).length }}</div>
          <div class="stat-label">已停用</div>
        </div>
      </div>
    </div>

    <!-- 医生表格 -->
    <div class="card">
      <div class="card-head">
        <span>医生列表</span>
        <span class="card-head-count">共 {{ filteredDoctors.length }} 条</span>
      </div>
      <el-table
        :data="filteredDoctors"
        stripe
        style="width: 100%"
        v-loading="loading"
        element-loading-text="加载中..."
        class="doctor-table"
        empty-text="暂无医生数据"
      >
        <el-table-column type="index" label="#" width="44" align="center" />
        <el-table-column prop="name" label="姓名" min-width="90" />
        <el-table-column label="性别" width="60" align="center">
          <template #default="{ row }">
            <span class="gender-tag" :class="row.gender === 1 ? 'male' : row.gender === 2 ? 'female' : ''">{{ row.gender === 1 ? '男' : row.gender === 2 ? '女' : '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" width="120" />
        <el-table-column prop="email" label="邮箱" min-width="130" show-overflow-tooltip />
        <el-table-column prop="position" label="职称" width="90" />
        <el-table-column prop="deptName" label="科室" width="90" />
        <el-table-column label="状态" width="70" align="center">
          <template #default="{ row }">
            <span class="status-badge" :class="row.status === 1 ? 'active' : 'inactive'">
              <span class="status-dot"></span>{{ row.status === 1 ? '启用' : '停用' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" min-width="150">
          <template #default="{ row }">
            <span class="time-text">{{ row.createTime }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180">
          <template #default="{ row }">
            <div class="action-btns">
              <el-button text type="primary" size="small" @click="openEditDialog(row)">编辑</el-button>
              <el-button text :type="row.status === 1 ? 'warning' : 'success'" size="small" @click="toggleStatus(row)">{{ row.status === 1 ? '停用' : '启用' }}</el-button>
              <el-popconfirm title="确定删除该医生？此操作不可恢复。" @confirm="handleDelete(row.doctorId)">
                <template #reference>
                  <el-button text type="danger" size="small">删除</el-button>
                </template>
              </el-popconfirm>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑医生信息' : '新增医生账号'"
      width="680px"
      :close-on-click-modal="false"
      destroy-on-close
      class="form-dialog"
    >
      <div class="dialog-tip">
        <el-icon :size="16"><InfoFilled /></el-icon>
        <span>{{ isEdit ? '修改医生信息后，点击确认保存更改。' : '创建医生账号后，默认密码为123456，医生可自行修改。' }}</span>
      </div>
      <el-form :model="form" label-position="top" ref="formRef" :rules="rules" class="dialog-form">
        <el-row :gutter="24">
          <el-col :span="12">
            <el-form-item label="姓名" prop="name">
              <el-input v-model="form.name" placeholder="请输入医生姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="性别" prop="gender">
              <el-select v-model="form.gender" placeholder="请选择性别" style="width: 100%">
                <el-option label="男" :value="1" />
                <el-option label="女" :value="2" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="form.phone" placeholder="请输入手机号" maxlength="11" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="电子邮箱" prop="email">
              <el-input v-model="form.email" placeholder="请输入邮箱地址" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="职称 / 职位" prop="position">
              <el-select v-model="form.position" placeholder="请选择职称" style="width: 100%">
                <el-option label="主任医师" value="主任医师" />
                <el-option label="副主任医师" value="副主任医师" />
                <el-option label="主治医师" value="主治医师" />
                <el-option label="住院医师" value="住院医师" />
                <el-option label="医师" value="医师" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所属科室" prop="departmentId">
              <el-select v-model="form.departmentId" placeholder="请选择科室" style="width: 100%">
                <el-option
                  v-for="d in deptList"
                  :key="d.deptId"
                  :label="d.deptName"
                  :value="d.deptId"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="擅长领域">
              <el-input v-model="form.goodAt" placeholder="如：心血管疾病、内分泌代谢等" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="个人简介">
              <el-input v-model="form.introduction" type="textarea" :rows="3" placeholder="请输入医生简介" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false" size="large">取消</el-button>
          <el-button type="primary" @click="handleSubmit" :loading="submitting" size="large">
            {{ isEdit ? '保存修改' : '确认创建' }}
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  UserFilled, Plus, User, CircleCheckFilled, CircleCloseFilled,
  InfoFilled
} from '@element-plus/icons-vue'
import { getDoctorList, addDoctor, updateDoctor, deleteDoctor } from '@/api/admin/doctor'
import { getDeptList } from '@/api/admin/dept'

interface DoctorItem {
  doctorId: string
  name: string
  gender: number
  phone: string
  email: string
  position: string
  goodAt: string
  introduction: string
  departmentId: string
  deptName: string
  status: number
  createTime: string
}

interface DeptItem {
  deptId: string
  deptName: string
}

const doctors = ref<DoctorItem[]>([])
const deptList = ref<DeptItem[]>([])
const loading = ref(false)
const searchKeyword = ref('')
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref<any>(null)

const form = reactive({
  doctorId: '',
  name: '',
  gender: null as number | null,
  phone: '',
  email: '',
  position: '',
  goodAt: '',
  introduction: '',
  departmentId: '',
})

const rules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1\d{10}$/, message: '手机号格式不正确', trigger: 'blur' },
  ],
  position: [{ required: true, message: '请选择职称', trigger: 'change' }],
  departmentId: [{ required: true, message: '请选择科室', trigger: 'change' }],
}

const filteredDoctors = computed(() => {
  const kw = searchKeyword.value.trim().toLowerCase()
  if (!kw) return doctors.value
  return doctors.value.filter(
    d => d.name.toLowerCase().includes(kw) || d.phone.includes(kw)
  )
})

onMounted(async () => {
  await Promise.all([fetchDoctors(), fetchDepts()])
})

async function fetchDoctors() {
  loading.value = true
  try {
    const res = await getDoctorList()
    doctors.value = res.data || []
  } catch { /* 拦截器已处理 */ }
  finally { loading.value = false }
}

async function fetchDepts() {
  try {
    const res = await getDeptList()
    deptList.value = res.data || []
  } catch { /* 忽略 */ }
}

function handleSearch() {
  // computed 自动响应
}

function openAddDialog() {
  isEdit.value = false
  form.doctorId = ''
  form.name = ''
  form.gender = null
  form.phone = ''
  form.email = ''
  form.position = ''
  form.goodAt = ''
  form.introduction = ''
  form.departmentId = ''
  formRef.value?.clearValidate()
  dialogVisible.value = true
}

function openEditDialog(row: DoctorItem) {
  isEdit.value = true
  form.doctorId = row.doctorId
  form.name = row.name
  form.gender = row.gender
  form.phone = row.phone
  form.email = row.email
  form.position = row.position
  form.goodAt = row.goodAt
  form.introduction = row.introduction
  form.departmentId = row.departmentId
  formRef.value?.clearValidate()
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateDoctor({ ...form, status: 1, gender: form.gender ?? 0 })
      ElMessage.success({ message: '医生信息已更新', icon: '✅' })
    } else {
      await addDoctor({ ...form, gender: form.gender ?? 0 })
      ElMessage.success({ message: '医生账号已创建（默认密码: 123456）', icon: '✅' })
    }
    dialogVisible.value = false
    await fetchDoctors()
  } catch { /* 拦截器已处理 */ }
  finally { submitting.value = false }
}

async function handleDelete(doctorId: string) {
  try {
    await deleteDoctor(doctorId)
    ElMessage.success({ message: '医生已删除', icon: '🗑️' })
    await fetchDoctors()
  } catch { /* 拦截器已处理 */ }
}

async function toggleStatus(row: DoctorItem) {
  const newStatus = row.status === 1 ? 0 : 1
  try {
    await updateDoctor({
      doctorId: row.doctorId,
      name: row.name,
      gender: row.gender,
      phone: row.phone,
      email: row.email,
      position: row.position,
      goodAt: row.goodAt,
      introduction: row.introduction,
      departmentId: row.departmentId,
      status: newStatus,
    })
    ElMessage.success({ message: newStatus === 1 ? '医生账号已启用' : '医生账号已停用', icon: '✅' })
    await fetchDoctors()
  } catch { /* 拦截器已处理 */ }
}
</script>

<style scoped>
/* ========== 页面布局 ========== */
.page {
  padding: 28px 36px;
  animation: fadeIn 0.3s ease;
}
@keyframes fadeIn {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}

/* ========== 页面头部 ========== */
.page-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 28px;
  flex-wrap: wrap;
  gap: 16px;
}
.page-top-left {
  display: flex;
  align-items: center;
  gap: 14px;
}
.page-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
  flex-shrink: 0;
}
.page-top h2 {
  font-size: 22px;
  font-weight: 700;
  color: #0f172a;
  letter-spacing: -0.3px;
}
.top-sub {
  font-size: 13px;
  color: #94a3b8;
  margin-top: 3px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}
.search-input {
  width: 240px;
}
.search-input :deep(.el-input__wrapper) {
  border-radius: 10px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
  transition: box-shadow 0.2s;
}
.search-input :deep(.el-input__wrapper:hover),
.search-input :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px rgba(59,130,246,0.15);
}

/* ========== 统计卡片 ========== */
.stat-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}
.stat-card {
  background: #fff;
  border-radius: 14px;
  padding: 22px 24px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04), 0 1px 2px rgba(0,0,0,0.03);
  transition: transform 0.2s, box-shadow 0.2s;
  position: relative;
  overflow: hidden;
}
.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(0,0,0,0.06);
}
.stat-icon-box {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.stat-total .stat-icon-box {
  background: linear-gradient(135deg, #3b82f6, #1d4ed8);
  color: #fff;
  box-shadow: 0 4px 12px rgba(59,130,246,0.25);
}
.stat-active .stat-icon-box {
  background: linear-gradient(135deg, #22c55e, #16a34a);
  color: #fff;
  box-shadow: 0 4px 12px rgba(34,197,94,0.25);
}
.stat-inactive .stat-icon-box {
  background: linear-gradient(135deg, #f59e0b, #d97706);
  color: #fff;
  box-shadow: 0 4px 12px rgba(245,158,11,0.25);
}
.stat-info {
  flex: 1;
}
.stat-num {
  font-size: 26px;
  font-weight: 800;
  color: #0f172a;
  line-height: 1.2;
  letter-spacing: -0.5px;
}
.stat-label {
  font-size: 13px;
  color: #94a3b8;
  margin-top: 3px;
}
.stat-trend {
  position: absolute;
  right: 16px;
  top: 16px;
}
.trend-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  display: inline-block;
}
.stat-total .trend-dot { background: #3b82f6; }

/* ========== 表格卡片 ========== */
.card {
  background: #fff;
  border-radius: 14px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04), 0 1px 2px rgba(0,0,0,0.03);
  overflow: hidden;
}
.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 24px 0;
  font-size: 15px;
  font-weight: 700;
  color: #1e293b;
}
.card-head-count {
  font-size: 12px;
  font-weight: 400;
  color: #94a3b8;
  background: #f1f5f9;
  padding: 2px 10px;
  border-radius: 10px;
}

/* ========== 表格样式 ========== */
.doctor-table :deep(.el-table__header th) {
  background: #f8fafc;
  color: #64748b;
  font-weight: 600;
  font-size: 12px;
  letter-spacing: 0.3px;
  padding: 12px 8px;
  border-bottom: 1px solid #e2e8f0;
}
.doctor-table :deep(.el-table__body td) {
  padding: 14px 8px;
  font-size: 13px;
  color: #334155;
}
.doctor-table :deep(.el-table__row) {
  transition: background 0.15s;
}
.doctor-table :deep(.el-table__row:hover) {
  background: #f8fafc !important;
}
.doctor-table :deep(.el-table__row--striped) {
  background: #fafbfc;
}
.doctor-table :deep(.el-table__empty-text) {
  color: #94a3b8;
  font-size: 14px;
  padding: 40px 0;
}
.doctor-table :deep(.el-loading-mask) {
  border-radius: 0 0 14px 14px;
}

.gender-tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  font-size: 12px;
  font-weight: 600;
}
.gender-tag.male {
  background: #dbeafe;
  color: #2563eb;
}
.gender-tag.female {
  background: #fce7f3;
  color: #db2777;
}

.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  font-weight: 600;
  padding: 3px 10px;
  border-radius: 12px;
}
.status-badge.active {
  background: #dcfce7;
  color: #16a34a;
}
.status-badge.inactive {
  background: #fef3c7;
  color: #d97706;
}
.status-dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
}
.status-badge.active .status-dot {
  background: #22c55e;
  box-shadow: 0 0 4px rgba(34,197,94,0.4);
}
.status-badge.inactive .status-dot {
  background: #f59e0b;
  box-shadow: 0 0 4px rgba(245,158,11,0.4);
}

.time-text {
  font-size: 12px;
  color: #94a3b8;
}

.action-btns {
  display: flex;
  gap: 4px;
}
.action-btns .el-button {
  font-size: 12px;
  padding: 4px 6px;
}

/* ========== 对话框样式 ========== */
.form-dialog :deep(.el-dialog__header) {
  padding: 24px 28px 0;
  font-size: 17px;
  font-weight: 700;
}
.form-dialog :deep(.el-dialog__body) {
  padding: 16px 28px 8px;
}
.form-dialog :deep(.el-dialog__footer) {
  padding: 8px 28px 24px;
  border-top: none;
}

.dialog-tip {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #64748b;
  background: #f0f9ff;
  border: 1px solid #b8d4fe;
  border-radius: 8px;
  padding: 10px 14px;
  margin-bottom: 20px;
  line-height: 1.5;
}
.dialog-tip .el-icon {
  color: #3b82f6;
  flex-shrink: 0;
}
.dialog-tip :deep(b) {
  color: #1e293b;
  font-weight: 700;
}

.dialog-form :deep(.el-form-item) {
  margin-bottom: 18px;
}
.dialog-form :deep(.el-form-item__label) {
  font-size: 13px;
  font-weight: 600;
  color: #475569;
  padding-bottom: 4px;
}
.dialog-form :deep(.el-input__wrapper),
.dialog-form :deep(.el-select__wrapper) {
  border-radius: 8px;
  transition: box-shadow 0.2s;
}
.dialog-form :deep(.el-input__wrapper:hover),
.dialog-form :deep(.el-select__wrapper:hover) {
  box-shadow: 0 0 0 1px rgba(59,130,246,0.3);
}
.dialog-form :deep(.el-input__wrapper.is-focus),
.dialog-form :deep(.el-select__wrapper.is-focus) {
  box-shadow: 0 0 0 2px rgba(59,130,246,0.15);
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
.dialog-footer .el-button {
  min-width: 100px;
  border-radius: 8px;
}

/* ========== 响应式适配 ========== */
@media (max-width: 900px) {
  .page { padding: 20px; }
  .page-top { flex-direction: column; }
  .header-actions { width: 100%; }
  .search-input { flex: 1; }
  .stat-row { grid-template-columns: 1fr; }
}
</style>
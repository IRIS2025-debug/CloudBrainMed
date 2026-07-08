<template>
  <div class="page">
    <header class="page-top">
      <div class="page-top-left">
        <div class="page-icon">
          <el-icon :size="20"><UserFilled /></el-icon>
        </div>
        <div>
          <h2>账号权限管理</h2>
          <p class="top-sub">管理医生账号信息、启停状态与基础资料。</p>
        </div>
      </div>

      <div class="header-actions">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索姓名 / 手机号"
          clearable
          :prefix-icon="Search"
          class="search-input"
          @input="handleSearch"
        />
        <el-button type="primary" size="large" @click="openAddDialog" round>
          <el-icon><Plus /></el-icon>
          <span>新增医生</span>
        </el-button>
      </div>
    </header>

    <div class="stat-row">
      <div class="stat-card stat-total">
        <div class="stat-icon-box">
          <el-icon :size="20"><User /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-num">{{ doctors.length }}</div>
          <div class="stat-label">医生总数</div>
        </div>
      </div>
      <div class="stat-card stat-active">
        <div class="stat-icon-box">
          <el-icon :size="20"><CircleCheckFilled /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-num">{{ doctors.filter((d) => d.status === 1).length }}</div>
          <div class="stat-label">已启用</div>
        </div>
      </div>
      <div class="stat-card stat-inactive">
        <div class="stat-icon-box">
          <el-icon :size="20"><CircleCloseFilled /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-num">{{ doctors.filter((d) => d.status === 0).length }}</div>
          <div class="stat-label">已停用</div>
        </div>
      </div>
    </div>

    <div class="card">
      <div class="card-head">
        <span>医生列表</span>
        <span class="card-head-count">共 {{ filteredDoctors.length }} 条</span>
      </div>

      <el-table
        :data="filteredDoctors"
        stripe
        class="doctor-table"
        v-loading="loading"
        element-loading-text="加载中..."
        empty-text="暂无医生数据"
      >
        <el-table-column type="index" label="#" width="52" align="center" />
        <el-table-column prop="name" label="姓名" min-width="96" />
        <el-table-column label="性别" width="72" align="center">
          <template #default="{ row }">
            <span class="gender-tag" :class="row.gender === 1 ? 'male' : row.gender === 2 ? 'female' : 'unknown'">
              {{ row.gender === 1 ? '男' : row.gender === 2 ? '女' : '未知' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" width="124" />
        <el-table-column prop="email" label="邮箱" min-width="180" show-overflow-tooltip />
        <el-table-column prop="position" label="职称" width="104" show-overflow-tooltip />
        <el-table-column prop="deptName" label="科室" width="96" show-overflow-tooltip />
        <el-table-column label="状态" width="92" align="center">
          <template #default="{ row }">
            <span class="status-badge" :class="row.status === 1 ? 'active' : 'inactive'">
              <span class="status-dot"></span>
              <span class="status-text">{{ row.status === 1 ? '启用' : '停用' }}</span>
            </span>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="146">
          <template #default="{ row }">
            <span class="time-text">{{ row.createTime || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="188" align="center">
          <template #default="{ row }">
            <div class="action-btns">
              <el-button text type="primary" size="small" @click="openEditDialog(row)">编辑</el-button>
              <el-button
                text
                :type="row.status === 1 ? 'warning' : 'success'"
                size="small"
                @click="toggleStatus(row)"
              >
                {{ row.status === 1 ? '停用' : '启用' }}
              </el-button>
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
        <span>{{ isEdit ? '修改后点击保存即可生效。' : '创建后默认密码为 123456，医生可登录后自行修改。' }}</span>
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
            <el-form-item label="职称" prop="position">
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
                <el-option v-for="dept in deptList" :key="dept.deptId" :label="dept.deptName" :value="dept.deptId" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="擅长领域">
              <el-input v-model="form.goodAt" placeholder="例如：脑科常见病、慢病管理" />
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
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  CircleCheckFilled,
  CircleCloseFilled,
  InfoFilled,
  Plus,
  Search,
  User,
  UserFilled,
} from '@element-plus/icons-vue'
import { addDoctor, deleteDoctor, getDoctorList, updateDoctor } from '@/api/admin/doctor'
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
  const keyword = searchKeyword.value.trim().toLowerCase()
  if (!keyword) return doctors.value
  return doctors.value.filter((doctor) => doctor.name.toLowerCase().includes(keyword) || doctor.phone.includes(keyword))
})

onMounted(async () => {
  await Promise.all([fetchDoctors(), fetchDepts()])
})

async function fetchDoctors() {
  loading.value = true
  try {
    const res = await getDoctorList()
    doctors.value = res.data || []
  } finally {
    loading.value = false
  }
}

async function fetchDepts() {
  const res = await getDeptList()
  deptList.value = res.data || []
}

function handleSearch() {
  // computed list reacts automatically
}

function resetForm() {
  form.doctorId = ''
  form.name = ''
  form.gender = null
  form.phone = ''
  form.email = ''
  form.position = ''
  form.goodAt = ''
  form.introduction = ''
  form.departmentId = ''
}

function openAddDialog() {
  isEdit.value = false
  resetForm()
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
      ElMessage.success('医生信息已更新')
    } else {
      await addDoctor({ ...form, gender: form.gender ?? 0 })
      ElMessage.success('医生账号已创建')
    }
    dialogVisible.value = false
    await fetchDoctors()
  } finally {
    submitting.value = false
  }
}

async function handleDelete(doctorId: string) {
  await deleteDoctor(doctorId)
  ElMessage.success('医生已删除')
  await fetchDoctors()
}

async function toggleStatus(row: DoctorItem) {
  const newStatus = row.status === 1 ? 0 : 1
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
  ElMessage.success(newStatus === 1 ? '医生账号已启用' : '医生账号已停用')
  await fetchDoctors()
}
</script>

<style scoped>
.page {
  padding: 28px 32px 36px;
}

.page-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 18px;
  margin-bottom: 24px;
  flex-wrap: wrap;
}

.page-top-left {
  display: flex;
  align-items: center;
  gap: 14px;
}

.page-icon {
  width: 48px;
  height: 48px;
  border-radius: 16px;
  background: linear-gradient(135deg, #315fbb, #4f8df7);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 12px 24px rgba(49, 95, 187, 0.22);
  flex-shrink: 0;
}

.page-top h2 {
  margin: 0;
  color: #16304d;
  font-size: 24px;
  font-weight: 800;
  letter-spacing: -0.03em;
}

.top-sub {
  margin: 6px 0 0;
  color: #73859b;
  font-size: 13px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.search-input {
  width: 260px;
}

.search-input :deep(.el-input__wrapper) {
  border-radius: 14px;
  box-shadow: 0 0 0 1px #dce6f2 inset;
}

.search-input :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 3px rgba(49, 95, 187, 0.12), 0 0 0 1px #315fbb inset;
}

.stat-row {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 22px;
}

.stat-card,
.card {
  border-radius: 22px;
  border: 1px solid rgba(219, 228, 240, 0.95);
  background: #fff;
  box-shadow: 0 16px 36px rgba(31, 41, 55, 0.06);
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px 22px;
}

.stat-icon-box {
  width: 50px;
  height: 50px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
}

.stat-total .stat-icon-box {
  background: linear-gradient(135deg, #315fbb, #4f8df7);
}

.stat-active .stat-icon-box {
  background: linear-gradient(135deg, #16a34a, #4ade80);
}

.stat-inactive .stat-icon-box {
  background: linear-gradient(135deg, #d97706, #f59e0b);
}

.stat-num {
  color: #16304d;
  font-size: 26px;
  font-weight: 800;
  line-height: 1.1;
}

.stat-label {
  margin-top: 6px;
  color: #6b7f98;
  font-size: 13px;
  font-weight: 700;
}

.card {
  overflow: hidden;
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 22px 0;
  color: #16304d;
  font-size: 16px;
  font-weight: 800;
}

.card-head-count {
  height: 28px;
  padding: 0 12px;
  border-radius: 999px;
  background: #f3f7fc;
  color: #8aa0bb;
  font-size: 12px;
  font-weight: 700;
  display: inline-flex;
  align-items: center;
}

.doctor-table :deep(.el-table__header th) {
  background: #f7faff;
  color: #64748b;
  font-size: 13px;
  font-weight: 700;
  border-bottom: 1px solid #e6edf5;
}

.doctor-table :deep(.el-table__body td) {
  padding: 16px 8px;
  font-size: 13px;
  color: #334155;
}

.doctor-table :deep(.el-table__row:hover) {
  background: #fbfdff !important;
}

.gender-tag,
.status-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  white-space: nowrap;
  font-size: 12px;
  font-weight: 700;
}

.gender-tag {
  min-width: 44px;
  height: 28px;
  padding: 0 10px;
  border-radius: 999px;
}

.gender-tag.male {
  color: #2563eb;
  background: #dbeafe;
}

.gender-tag.female {
  color: #db2777;
  background: #fce7f3;
}

.gender-tag.unknown {
  color: #64748b;
  background: #e2e8f0;
}

.status-badge {
  min-width: 58px;
  height: 28px;
  padding: 0 10px;
  border-radius: 999px;
}

.status-badge.active {
  color: #16a34a;
  background: #dcfce7;
}

.status-badge.inactive {
  color: #d97706;
  background: #fef3c7;
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
}

.status-badge.active .status-dot {
  background: #22c55e;
}

.status-badge.inactive .status-dot {
  background: #f59e0b;
}

.status-text {
  white-space: nowrap;
}

.time-text {
  color: #7f93ab;
  font-size: 12px;
}

.action-btns {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  white-space: nowrap;
}

.form-dialog :deep(.el-dialog__header) {
  padding: 24px 28px 0;
}

.form-dialog :deep(.el-dialog__body) {
  padding: 16px 28px 8px;
}

.form-dialog :deep(.el-dialog__footer) {
  padding: 8px 28px 24px;
}

.dialog-tip {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 20px;
  padding: 10px 14px;
  border: 1px solid #d8e5f4;
  border-radius: 12px;
  background: #f5f9ff;
  color: #5c718e;
  font-size: 13px;
  line-height: 1.6;
}

.dialog-tip .el-icon {
  color: #315fbb;
  flex-shrink: 0;
}

.dialog-form :deep(.el-form-item) {
  margin-bottom: 18px;
}

.dialog-form :deep(.el-form-item__label) {
  color: #536781;
  font-size: 13px;
  font-weight: 700;
}

.dialog-form :deep(.el-input__wrapper),
.dialog-form :deep(.el-textarea__inner) {
  border-radius: 12px;
  box-shadow: 0 0 0 1px #dce6f2 inset;
}

.dialog-form :deep(.el-input__wrapper.is-focus),
.dialog-form :deep(.el-textarea__inner:focus) {
  box-shadow: 0 0 0 3px rgba(49, 95, 187, 0.12), 0 0 0 1px #315fbb inset;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

@media (max-width: 980px) {
  .stat-row {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .page {
    padding: 18px 16px 24px;
  }

  .page-top {
    flex-direction: column;
    align-items: stretch;
  }

  .header-actions {
    width: 100%;
    flex-direction: column;
    align-items: stretch;
  }

  .search-input {
    width: 100%;
  }
}
</style>

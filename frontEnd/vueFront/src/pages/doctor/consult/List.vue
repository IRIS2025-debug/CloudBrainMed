<template>
  <div class="page">
    <header class="page-top">
      <div>
        <h2>接诊工作台</h2>
        <p class="top-sub">管理患者接诊队列，查看就诊记录</p>
      </div>
      <div class="filter-bar">
        <el-select v-model="filters.consultStatus" placeholder="接诊状态" clearable @change="fetchList" style="width:140px">
          <el-option label="待接诊" value="PENDING" />
          <el-option label="接诊中" value="IN_PROGRESS" />
          <el-option label="已确认" value="RECORD_CONFIRMED" />
          <el-option label="已完成" value="COMPLETED" />
        </el-select>
        <el-date-picker v-model="filters.date" type="date" placeholder="就诊日期" @change="fetchList" style="width:150px" />
        <el-checkbox v-model="filters.reportReturnedOnly" @change="fetchList">报告已回传</el-checkbox>
      </div>
    </header>

    <div class="card">
      <el-table :data="list" stripe v-loading="loading" class="consult-table">
        <el-table-column prop="registerId" label="挂号 ID" width="200" />
        <el-table-column label="患者" width="130">
          <template #default="{ row }">
            <div class="patient-cell">
              <div class="pc-avatar">{{ row.name?.charAt(0) || '?' }}</div>
              <div>
                <div class="pc-name">{{ row.name }}</div>
                <div class="pc-meta">{{ row.gender === 1 ? '男' : '女' }} · {{ row.patientAge }}岁</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="chiefComplaint" label="主诉" show-overflow-tooltip min-width="180" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <span class="status-tag" :class="'st-' + row.consultStatus">{{ statusLabel(row.consultStatus) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="报告" width="150">
          <template #default="{ row }">
            <el-tag v-if="row.hasReturnedReport || row.reportCount > 0" type="success" round>
              已回传 {{ row.reportCount || 1 }}
            </el-tag>
            <span v-else class="muted-text">待报告</span>
          </template>
        </el-table-column>
        <el-table-column prop="visitDate" label="就诊日期" width="120" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="$router.push(`/doctor/consult/${row.registerId}`)">接诊</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="table-footer">
        <span class="tf-total">共 {{ list.length }} 条</span>
        <el-pagination v-model:current-page="page" :page-size="10" :total="total" layout="prev, pager, next" @current-change="fetchList" size="small" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { getConsultList } from '@/api/doctor/consult'
import { ElMessage } from 'element-plus'
const list = ref<any[]>([]); const loading = ref(false); const page = ref(1); const total = ref(0)
const filters = reactive({ consultStatus: '', date: '', reportReturnedOnly: false })


async function fetchList() {
  loading.value = true
  try {
    const res = await getConsultList({
      consultStatus: filters.consultStatus,
      date: formatDate(filters.date),
      reportReturnedOnly: filters.reportReturnedOnly,
      page: page.value,
      limit: 10
    })
    const data = res.data
    list.value = Array.isArray(data) ? data : (data?.list || [])
    total.value = Number(data?.total ?? list.value.length)
  } catch {
    list.value = []
    total.value = 0
    ElMessage.error('接诊列表加载失败，请稍后重试')
  } finally { loading.value = false }
}
function statusLabel(s: string) {
  const m: Record<string, string> = { PENDING: '待接诊', IN_PROGRESS: '接诊中', RECORD_CONFIRMED: '已确认', COMPLETED: '已完成' }
  return m[s] || s
}

function formatDate(value: string | Date) {
  if (!value) return ''
  if (value instanceof Date) {
    const month = String(value.getMonth() + 1).padStart(2, '0')
    const day = String(value.getDate()).padStart(2, '0')
    return `${value.getFullYear()}-${month}-${day}`
  }
  return value
}
fetchList()
</script>

<style scoped>
.page { padding: 30px 38px; background: #f4f7fb; min-height: calc(100vh - 64px); }
.page-top { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 24px; }
.page-top h2 { font-size: 24px; font-weight: 800; color: #0f172a; margin: 0; }
.top-sub { font-size: 13px; color: #94a3b8; margin-top: 4px; }
.filter-bar { display: flex; gap: 10px; }
.filter-bar :deep(.el-input__wrapper),
.filter-bar :deep(.el-select__wrapper) { border-radius: 12px; box-shadow: 0 0 0 1px #dbe3ee inset; }
.card { background: #fff; border: 1px solid #e3eaf3; border-radius: 18px; box-shadow: 0 18px 42px rgba(28, 44, 68, .08); overflow: hidden; }

.consult-table :deep(th) { background: #f8fafc; color: #64748b; font-weight: 700; font-size: 13px; border-bottom: none; }
.consult-table :deep(td) { font-size: 14px; padding: 18px 0; }
.consult-table :deep(.el-table__row) { transition: background-color .18s ease; }
.consult-table :deep(.el-table__row:hover) { background: #f7faff; }

.patient-cell { display: flex; align-items: center; gap: 10px; }
.pc-avatar { width: 36px; height: 36px; border-radius: 12px; background: #e0e7ff; color: #315fbb; font-size: 13px; font-weight: 800; display: flex; align-items: center; justify-content: center; }
.pc-name { font-size: 14px; font-weight: 600; color: #1e293b; }
.pc-meta { font-size: 12px; color: #94a3b8; }

.status-tag { font-size: 12px; font-weight: 800; padding: 5px 12px; border-radius: 999px; }
.st-PENDING { background: #fef3c7; color: #b45309; }
.st-IN_PROGRESS { background: #dbeafe; color: #1d4ed8; }
.st-RECORD_CONFIRMED { background: #d1fae5; color: #065f46; }
.st-COMPLETED { background: #f1f5f9; color: #64748b; }
.consult-table :deep(.el-button.is-link) {
  min-width: 72px;
  height: 34px;
  padding: 0 16px;
  border-radius: 999px;
  background: #315fbb;
  color: #fff;
  font-weight: 800;
  box-shadow: 0 8px 18px rgba(49, 95, 187, .18);
}
.consult-table :deep(.el-button.is-link:hover),
.consult-table :deep(.el-button.is-link:focus) { background: #3f87dc; color: #fff; }

.table-footer { display: flex; justify-content: space-between; align-items: center; padding: 12px 16px; border-top: 1px solid #f1f5f9; }
.tf-total { font-size: 13px; color: #94a3b8; }
.muted-text { color: #94a3b8; font-size: 12px; }
</style>

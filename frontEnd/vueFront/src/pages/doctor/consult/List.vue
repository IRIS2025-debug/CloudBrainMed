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
const list = ref<any[]>([]); const loading = ref(false); const page = ref(1); const total = ref(0)
const filters = reactive({ consultStatus: '', date: '' })

// Mock 数据 —— API 不可达时作为开发测试数据
const mockList = [
  { registerId: 'REG-20260612-001', name: '陈建国', gender: 1, patientAge: 45, chiefComplaint: '反复头痛、眩晕一周，加重两天', consultStatus: 'PENDING', visitDate: '2026-06-12' },
  { registerId: 'REG-20260612-002', name: '林美娟', gender: 2, patientAge: 32, chiefComplaint: '右下腹持续性隐痛三天，伴恶心', consultStatus: 'IN_PROGRESS', visitDate: '2026-06-12' },
  { registerId: 'REG-20260612-003', name: '黄志强', gender: 1, patientAge: 58, chiefComplaint: '胸闷气短两周，夜间加重', consultStatus: 'RECORD_CONFIRMED', visitDate: '2026-06-11' },
  { registerId: 'REG-20260612-004', name: '赵小燕', gender: 2, patientAge: 27, chiefComplaint: '咳嗽咳痰五天，发热一天，体温38.2℃', consultStatus: 'COMPLETED', visitDate: '2026-06-11' },
  { registerId: 'REG-20260612-005', name: '周文博', gender: 1, patientAge: 66, chiefComplaint: '双下肢水肿一周，既往高血压病史10年', consultStatus: 'PENDING', visitDate: '2026-06-12' },
]

async function fetchList() {
  loading.value = true
  try {
    const res = await getConsultList({ ...filters, page: page.value, limit: 10 })
    list.value = res.data || []
  } catch {
    // API 不可达时使用 mock 数据进行开发测试
    list.value = mockList
  } finally { loading.value = false }
}
function statusLabel(s: string) {
  const m: Record<string, string> = { PENDING: '待接诊', IN_PROGRESS: '接诊中', RECORD_CONFIRMED: '已确认', COMPLETED: '已完成' }
  return m[s] || s
}
fetchList()
</script>

<style scoped>
.page { padding: 28px 36px; }
.page-top { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 24px; }
.page-top h2 { font-size: 22px; font-weight: 700; color: #0f172a; }
.top-sub { font-size: 13px; color: #94a3b8; margin-top: 4px; }
.filter-bar { display: flex; gap: 10px; }
.card { background: #fff; border-radius: var(--radius); box-shadow: var(--shadow); overflow: hidden; }

.consult-table :deep(th) { background: #f8fafc; color: #64748b; font-weight: 600; font-size: 13px; border-bottom: none; }
.consult-table :deep(td) { font-size: 14px; }

.patient-cell { display: flex; align-items: center; gap: 10px; }
.pc-avatar { width: 32px; height: 32px; border-radius: 50%; background: #e0e7ff; color: #4338ca; font-size: 13px; font-weight: 700; display: flex; align-items: center; justify-content: center; }
.pc-name { font-size: 14px; font-weight: 600; color: #1e293b; }
.pc-meta { font-size: 12px; color: #94a3b8; }

.status-tag { font-size: 12px; font-weight: 600; padding: 3px 10px; border-radius: 12px; }
.st-PENDING { background: #fef3c7; color: #b45309; }
.st-IN_PROGRESS { background: #dbeafe; color: #1d4ed8; }
.st-RECORD_CONFIRMED { background: #d1fae5; color: #065f46; }
.st-COMPLETED { background: #f1f5f9; color: #64748b; }

.table-footer { display: flex; justify-content: space-between; align-items: center; padding: 12px 16px; border-top: 1px solid #f1f5f9; }
.tf-total { font-size: 13px; color: #94a3b8; }
</style>

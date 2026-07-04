<template>
  <div class="application-page">
    <div class="page-top">
      <div>
        <p class="eyebrow">检查医生 · 检查申请</p>
        <h1>检查申请管理</h1>
        <p>查看接诊医生开具并进入医技流程的检查申请，检查医生端仅展示 EXAM 类项目。</p>
      </div>
      <el-button type="primary" :icon="Refresh" :loading="loading" plain @click="fetchData">
        刷新数据
      </el-button>
    </div>

    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-icon is-blue">
          <el-icon><Document /></el-icon>
        </div>
        <div>
          <strong>{{ displayList.length }}</strong>
          <span>总申请数</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon is-amber">
          <el-icon><Clock /></el-icon>
        </div>
        <div>
          <strong>{{ countByStatus('WAITING_ASSIGN') }}</strong>
          <span>待分配</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon is-green">
          <el-icon><Check /></el-icon>
        </div>
        <div>
          <strong>{{ countByStatus('COMPLETED') }}</strong>
          <span>已完成</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon is-red">
          <el-icon><Warning /></el-icon>
        </div>
        <div>
          <strong>{{ displayList.filter(item => item.urgencyLevel === 'URGENT' || item.urgencyLevel === 'EMERGENCY').length }}</strong>
          <span>加急/紧急</span>
        </div>
      </div>
    </div>

    <section class="panel filter-panel">
      <el-select v-model="filterStatus" placeholder="全部状态" clearable>
        <el-option label="待分配" value="WAITING_ASSIGN" />
        <el-option label="已排队" value="QUEUED" />
        <el-option label="执行中" value="IN_PROGRESS" />
        <el-option label="已完成" value="COMPLETED" />
        <el-option label="已取消" value="CANCELLED" />
      </el-select>
      <el-select v-model="filterUrgency" placeholder="全部紧急程度" clearable>
        <el-option label="常规" value="NORMAL" />
        <el-option label="加急" value="URGENT" />
        <el-option label="紧急" value="EMERGENCY" />
      </el-select>
      <el-input v-model="searchKey" placeholder="患者姓名 / 患者ID / 申请ID" clearable :prefix-icon="Search" />
      <el-button :icon="RefreshRight" @click="resetFilters">重置</el-button>
    </section>

    <section class="panel table-panel">
      <div class="table-head">
        <div>
          <strong>申请列表</strong>
          <el-tag size="small" type="info" round>共 {{ displayList.length }} 条记录</el-tag>
        </div>
      </div>

      <el-table
        :data="pagedList"
        v-loading="loading"
        stripe
        style="width: 100%"
        :header-cell-style="{ background: '#f8fafc', color: '#64748b' }"
        @row-click="showDetail"
      >
        <el-table-column prop="orderId" label="申请ID" min-width="150" show-overflow-tooltip />
        <el-table-column prop="patientName" label="患者姓名" min-width="110" />
        <el-table-column prop="patientId" label="患者ID" min-width="150" show-overflow-tooltip />
        <el-table-column prop="itemCode" label="项目编码" width="110" />
        <el-table-column prop="itemName" label="项目名称" min-width="170" show-overflow-tooltip />
        <el-table-column prop="urgencyLevel" label="紧急程度" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="urgencyTag(row.urgencyLevel)" size="small">{{ urgencyLabel(row.urgencyLevel) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="payStatus" label="支付" width="95" align="center">
          <template #default="{ row }">
            <el-tag :type="payTag(row.payStatus)" size="small">{{ payLabel(row.payStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="assignedRoom" label="执行房间" width="120">
          <template #default="{ row }">{{ row.assignedRoom || '-' }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="申请时间" min-width="170">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="110" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" link @click.stop="showDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-area">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          :total="displayList.length"
          layout="total, sizes, prev, pager, next, jumper"
          background
        />
      </div>
    </section>

    <el-dialog v-model="detailVisible" title="检查申请详情" width="720px" destroy-on-close>
      <el-descriptions v-if="currentOrder" :column="2" border>
        <el-descriptions-item label="申请ID">{{ currentOrder.orderId }}</el-descriptions-item>
        <el-descriptions-item label="挂号ID">{{ currentOrder.registerId }}</el-descriptions-item>
        <el-descriptions-item label="患者姓名">{{ currentOrder.patientName }}</el-descriptions-item>
        <el-descriptions-item label="患者ID">{{ currentOrder.patientId }}</el-descriptions-item>
        <el-descriptions-item label="项目名称">{{ currentOrder.itemName }}</el-descriptions-item>
        <el-descriptions-item label="项目编码">{{ currentOrder.itemCode }}</el-descriptions-item>
        <el-descriptions-item label="紧急程度">{{ urgencyLabel(currentOrder.urgencyLevel) }}</el-descriptions-item>
        <el-descriptions-item label="支付状态">{{ payLabel(currentOrder.payStatus) }}</el-descriptions-item>
        <el-descriptions-item label="申请状态">{{ statusLabel(currentOrder.status) }}</el-descriptions-item>
        <el-descriptions-item label="执行房间">{{ currentOrder.assignedRoom || '-' }}</el-descriptions-item>
        <el-descriptions-item label="申请医生">{{ currentOrder.doctorId }}</el-descriptions-item>
        <el-descriptions-item label="申请时间">{{ formatTime(currentOrder.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="临床摘要" :span="2">{{ currentOrder.clinicalSummary || '-' }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Check, Clock, Document, Refresh, RefreshRight, Search, Warning } from '@element-plus/icons-vue'
import { getInspectionOrderList } from '@/api/inspection-doctor'

interface InspectionOrderVo {
  orderId: string
  patientId: string
  patientName: string
  gender: number
  age: number
  registerId: string
  doctorId: string
  clinicalSummary: string
  itemName: string
  itemCode: string
  itemCategory: string
  urgencyLevel: string
  sourceType: string
  status: string
  payStatus: string
  assignedRoom?: string
  confirmedTime: string
  createTime: string
}

const loading = ref(false)
const allList = ref<InspectionOrderVo[]>([])
const filterStatus = ref('')
const filterUrgency = ref('')
const searchKey = ref('')
const currentPage = ref(1)
const pageSize = ref(20)
const detailVisible = ref(false)
const currentOrder = ref<InspectionOrderVo | null>(null)

const examList = computed(() => allList.value.filter(item => item.itemCategory === 'EXAM'))

const displayList = computed(() => {
  const keyword = searchKey.value.trim().toLowerCase()
  return examList.value.filter(item => {
    if (filterStatus.value && item.status !== filterStatus.value) return false
    if (filterUrgency.value && item.urgencyLevel !== filterUrgency.value) return false
    if (!keyword) return true
    return [item.patientName, item.patientId, item.orderId, item.registerId]
      .some(value => String(value || '').toLowerCase().includes(keyword))
  })
})

const pagedList = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return displayList.value.slice(start, start + pageSize.value)
})

watch([filterStatus, filterUrgency, searchKey, pageSize], () => {
  currentPage.value = 1
})

function resetFilters() {
  filterStatus.value = ''
  filterUrgency.value = ''
  searchKey.value = ''
}

function countByStatus(status: string) {
  return displayList.value.filter(item => item.status === status).length
}

async function fetchData() {
  loading.value = true
  try {
    const res: any = await getInspectionOrderList()
    allList.value = Array.isArray(res.data) ? res.data : []
  } catch {
    ElMessage.error('加载检查申请列表失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

function showDetail(row: InspectionOrderVo) {
  currentOrder.value = row
  detailVisible.value = true
}

function urgencyTag(level: string) {
  if (level === 'EMERGENCY') return 'danger'
  if (level === 'URGENT') return 'warning'
  return 'info'
}

function urgencyLabel(level: string) {
  const map: Record<string, string> = { NORMAL: '常规', URGENT: '加急', EMERGENCY: '紧急' }
  return map[level] || level || '-'
}

function statusTag(status: string) {
  if (status === 'COMPLETED') return 'success'
  if (status === 'IN_PROGRESS') return 'primary'
  if (status === 'CANCELLED') return 'info'
  return 'warning'
}

function statusLabel(status: string) {
  const map: Record<string, string> = {
    WAITING_ASSIGN: '待分配',
    QUEUED: '已排队',
    IN_PROGRESS: '执行中',
    COMPLETED: '已完成',
    CANCELLED: '已取消',
  }
  return map[status] || status || '-'
}

function payTag(pay: string) {
  if (pay === 'PAID') return 'success'
  if (pay === 'CANCELLED' || pay === 'REFUNDED') return 'danger'
  return 'warning'
}

function payLabel(pay: string) {
  const map: Record<string, string> = {
    WAITING: '待支付',
    PAID: '已支付',
    CANCELLED: '已取消',
    REFUNDED: '已退款',
  }
  return map[pay] || pay || '-'
}

function formatTime(time: string) {
  if (!time) return '-'
  const date = new Date(time)
  if (Number.isNaN(date.getTime())) return time
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.application-page {
  min-height: 100%;
  padding: 28px;
  color: #172033;
}

.page-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 20px;
  padding: 22px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 1px 2px rgba(15, 23, 42, .04);
}

.eyebrow {
  margin: 0 0 6px;
  color: #2563eb;
  font-size: 13px;
  font-weight: 800;
}

.page-top h1 {
  margin: 0;
  color: #102033;
  font-size: 26px;
  font-weight: 800;
}

.page-top p:last-child {
  margin: 8px 0 0;
  color: #64748b;
  font-size: 14px;
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 18px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 14px;
  min-height: 96px;
  padding: 18px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #fff;
}

.stat-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 52px;
  height: 52px;
  border-radius: 8px;
  font-size: 24px;
}

.stat-icon.is-blue { background: #eaf2ff; color: #2563eb; }
.stat-icon.is-amber { background: #fff7ed; color: #d97706; }
.stat-icon.is-green { background: #ecfdf5; color: #16a34a; }
.stat-icon.is-red { background: #fef2f2; color: #ef4444; }

.stat-card strong {
  display: block;
  color: #102033;
  font-size: 26px;
  line-height: 1.1;
}

.stat-card span {
  display: block;
  margin-top: 5px;
  color: #64748b;
  font-size: 13px;
}

.panel {
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 1px 2px rgba(15, 23, 42, .04);
}

.filter-panel {
  display: grid;
  grid-template-columns: 180px 180px minmax(260px, 1fr) auto;
  gap: 12px;
  align-items: center;
  margin-bottom: 18px;
  padding: 18px;
}

.table-panel {
  padding: 18px;
}

.table-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}

.table-head div {
  display: flex;
  align-items: center;
  gap: 10px;
}

.table-head strong {
  color: #102033;
  font-size: 16px;
}

.pagination-area {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}

@media (max-width: 980px) {
  .stats-row,
  .filter-panel {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 640px) {
  .application-page {
    padding: 18px;
  }

  .page-top {
    display: block;
  }

  .page-top .el-button {
    margin-top: 14px;
  }

  .stats-row,
  .filter-panel {
    grid-template-columns: 1fr;
  }
}
</style>

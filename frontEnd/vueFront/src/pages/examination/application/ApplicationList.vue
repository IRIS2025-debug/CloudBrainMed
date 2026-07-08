<template>
  <div class="application-page">
    <section class="page-top">
      <div class="page-top-main">
        <p class="eyebrow">检查医生 · 检查申请</p>
        <h1>查看检查申请</h1>
        <p>
          仅展示 EXAM 类检查申请，支持筛选、分派、处理和报告查看。
        </p>
        <div class="hero-tags">
          <span>总申请 {{ displayList.length }}</span>
          <span>待分配 {{ countByStatus('WAITING_ASSIGN') }}</span>
          <span>已完成 {{ countByStatus('COMPLETED') }}</span>
        </div>
      </div>
      <div class="page-top-side">
        <div class="hero-note">
          <strong>{{ displayList.filter(item => item.payStatus === 'PAID').length }}</strong>
          <span>已支付申请</span>
        </div>
        <el-button type="primary" :icon="Refresh" :loading="loading" @click="fetchData">
          刷新数据
        </el-button>
      </div>
    </section>

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
          <span>加急 / 紧急</span>
        </div>
      </div>
    </div>

    <section class="panel filter-panel">
      <div class="filter-head">
        <div>
          <strong>筛选条件</strong>
          <p>按状态、紧急程度或患者快速筛选。</p>
        </div>
        <el-button :icon="RefreshRight" @click="resetFilters">重置</el-button>
      </div>
      <div class="filter-grid">
        <label class="filter-item">
          <span>申请状态</span>
          <el-select v-model="filterStatus" placeholder="全部状态" clearable>
            <el-option label="待分配" value="WAITING_ASSIGN" />
            <el-option label="已排队" value="QUEUED" />
            <el-option label="处理中" value="IN_PROCESS" />
            <el-option label="已完成" value="COMPLETED" />
            <el-option label="已取消" value="CANCELLED" />
          </el-select>
        </label>
        <label class="filter-item">
          <span>紧急程度</span>
          <el-select v-model="filterUrgency" placeholder="全部紧急程度" clearable>
            <el-option label="常规" value="NORMAL" />
            <el-option label="加急" value="URGENT" />
            <el-option label="紧急" value="EMERGENCY" />
          </el-select>
        </label>
        <label class="filter-item wide">
          <span>搜索</span>
          <el-input v-model="searchKey" placeholder="患者姓名 / 患者ID / 申请ID" clearable :prefix-icon="Search" />
        </label>
      </div>
    </section>

    <section class="panel table-panel">
      <div class="table-head">
        <div>
          <strong>申请列表</strong>
          <p>未完成项目进入处理流程，已完成项目查看报告。</p>
        </div>
        <div class="table-summary">
          <span>共 {{ displayList.length }} 条记录</span>
          <el-tag size="small" type="warning" round>待分配 {{ countByStatus('WAITING_ASSIGN') }}</el-tag>
          <el-tag size="small" type="success" round>已完成 {{ countByStatus('COMPLETED') }}</el-tag>
        </div>
      </div>

      <el-table
        :data="pagedList"
        v-loading="loading"
        stripe
        :fit="true"
        table-layout="fixed"
        class="application-table"
        :header-cell-style="{ background: '#f7faff', color: '#64748b' }"
        @row-click="handlePrimaryAction"
      >
        <el-table-column prop="orderId" label="申请ID" width="130" show-overflow-tooltip />
        <el-table-column prop="patientName" label="患者姓名" width="110" />
        <el-table-column prop="itemName" label="项目名称" min-width="170" show-overflow-tooltip />
        <el-table-column prop="urgencyLevel" label="紧急程度" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="urgencyTag(row.urgencyLevel)" size="small">{{ urgencyLabel(row.urgencyLevel) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="payStatus" label="支付" width="95" align="center">
          <template #default="{ row }">
            <el-tag :type="payTag(row.payStatus)" size="small">{{ payLabel(row.payStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="assignedRoom" label="执行房间" width="110">
          <template #default="{ row }">{{ row.assignedRoom || '-' }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="申请时间" width="155">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="center" fixed="right">
          <template #default="{ row }">
            <el-button
              class="primary-action-button"
              :class="isCompleted(row) ? 'is-view' : 'is-process'"
              :loading="isProcessing(row)"
              :disabled="isProcessing(row) || isClaimedByOtherDoctor(row) || (!canProcess(row) && !isCompleted(row))"
              @click.stop="handlePrimaryAction(row)"
            >
              {{ actionLabel(row) }}
            </el-button>
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

  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Check, Clock, Document, Refresh, RefreshRight, Search, Warning } from '@element-plus/icons-vue'
import { assignInspectionOrder, getInspectionOrderList } from '@/api/inspection-doctor'
import { startTask } from '@/api/doctor/task'
import { isBrainCtItem } from '@/utils/brainCt'

interface InspectionOrderVo {
  orderItemId?: string
  order_item_id?: string
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
  assignedDoctorId?: string
  canStart?: boolean
  canWriteReport?: boolean
  canViewReport?: boolean
  confirmedTime: string
  createTime: string
}

const loading = ref(false)
const router = useRouter()
const allList = ref<InspectionOrderVo[]>([])
const filterStatus = ref('')
const filterUrgency = ref('')
const searchKey = ref('')
const currentPage = ref(1)
const pageSize = ref(20)
const processingIds = ref<Set<string>>(new Set())

const examList = computed(() => allList.value.filter((item) => item.itemCategory === 'EXAM'))

const displayList = computed(() => {
  const keyword = searchKey.value.trim().toLowerCase()
  return examList.value.filter((item) => {
    if (filterStatus.value && item.status !== filterStatus.value) return false
    if (filterUrgency.value && item.urgencyLevel !== filterUrgency.value) return false
    if (!keyword) return true
    return [item.patientName, item.patientId, item.orderId, item.registerId]
      .some((value) => String(value || '').toLowerCase().includes(keyword))
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
  return displayList.value.filter((item) => item.status === status).length
}

function isCompleted(row: InspectionOrderVo) {
  return hasTaskEntryId(row) && (row.canViewReport === true || row.status === 'COMPLETED')
}

function canProcess(row: InspectionOrderVo) {
  if (!hasTaskEntryId(row) || row.status === 'COMPLETED' || row.status === 'CANCELLED' || row.payStatus !== 'PAID') {
    return false
  }
  if (row.status === 'WAITING_ASSIGN') return true
  if (row.status === 'QUEUED') return row.canStart === true || row.canStart == null
  if (row.status === 'IN_PROCESS') return row.canWriteReport === true
  return false
}

function isClaimedByOtherDoctor(row: InspectionOrderVo) {
  return hasTaskEntryId(row) && row.status === 'IN_PROCESS' && row.canWriteReport !== true
}

function actionLabel(row: InspectionOrderVo) {
  if (isClaimedByOtherDoctor(row)) return '处理中'
  return isCompleted(row) ? '查看' : '处理'
}

function isProcessing(row: InspectionOrderVo) {
  return processingIds.value.has(getPrimaryOrderItemId(row))
}

async function handlePrimaryAction(row: InspectionOrderVo) {
  if (isCompleted(row)) {
    openTaskDetail(row)
    return
  }
  if (!canProcess(row)) {
    if (isClaimedByOtherDoctor(row)) {
      ElMessage.warning('该任务正在由其他医生处理')
      return
    }
    ElMessage.warning(row.payStatus === 'PAID' ? '当前状态暂不能处理' : '患者缴费后才能处理')
    return
  }
  const orderItemId = getPrimaryOrderItemId(row)
  if (processingIds.value.has(orderItemId)) return
  if (row.status === 'IN_PROCESS') {
    openProcessingWorkspaceByRow(row, orderItemId)
    return
  }
  processingIds.value = new Set(processingIds.value).add(orderItemId)
  try {
    if (row.status === 'WAITING_ASSIGN') {
      const assigned = await assignOrder(row, false)
      if (!assigned) return
    }
    if (row.status === 'WAITING_ASSIGN' || row.status === 'QUEUED') {
      await startTask(orderItemId)
      ElMessage.success('已开始处理，请填写检查报告')
    }
    openProcessingWorkspaceByRow(row, orderItemId)
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.msg || error?.message || '进入处理流程失败')
  } finally {
    const next = new Set(processingIds.value)
    next.delete(orderItemId)
    processingIds.value = next
  }
}

async function assignOrder(row: InspectionOrderVo, refreshAfterAssign = true) {
  const orderItemId = getPrimaryOrderItemId(row)
  if (!orderItemId) {
    ElMessage.warning('缺少检查明细ID，无法分配')
    return false
  }

  try {
    const result = await ElMessageBox.prompt(
      '请输入执行房间，例如 CT-1',
      '分配房间',
      {
        confirmButtonText: '分配',
        cancelButtonText: '取消',
        inputValue: row.assignedRoom || '',
        inputPattern: /\S+/,
        inputErrorMessage: '房间不能为空',
      },
    )
    await assignInspectionOrder(orderItemId, result.value)
    if (refreshAfterAssign) {
      ElMessage.success('已分配并进入排队')
      await fetchData()
    }
    return true
  } catch (error: any) {
    if (error !== 'cancel' && error?.action !== 'cancel') {
      ElMessage.error(error?.message || '分配失败')
    }
    return false
  }
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

function getPrimaryOrderItemId(row: InspectionOrderVo) {
  return row.orderItemId || row['order_item_id'] || ''
}

function hasTaskEntryId(row: InspectionOrderVo) {
  return !!getPrimaryOrderItemId(row)
}

function openTaskDetail(row: InspectionOrderVo) {
  const orderItemId = getPrimaryOrderItemId(row)
  if (!orderItemId) {
    ElMessage.warning('缺少检查明细ID，无法进入任务处理')
    return
  }
  openTaskDetailById(orderItemId)
}

function openTaskDetailById(orderItemId: string, openReport = false) {
  if (openReport) {
    router.push({
      path: `/doctor/task/${orderItemId}`,
      query: { report: '1' },
    })
    return
  }
  router.push({
    path: `/doctor/task/${orderItemId}`,
  })
}

function openProcessingWorkspaceByRow(row: InspectionOrderVo, orderItemId: string) {
  const query = {
    orderItemId,
    registerId: row.registerId || '',
    itemName: row.itemName || '',
  }
  if (isBrainCtItem(row.itemCategory, row.itemCode, row.itemName)) {
    router.push({
      path: '/examination-doctor/ct-inference',
      query,
    })
    return
  }
  router.push({
    path: '/examination-doctor/report',
    query,
  })
}

function urgencyTag(level: string) {
  if (level === 'EMERGENCY') return 'danger'
  if (level === 'URGENT') return 'warning'
  return 'info'
}

function urgencyLabel(level: string) {
  const map: Record<string, string> = {
    NORMAL: '常规',
    URGENT: '加急',
    EMERGENCY: '紧急'
  }
  return map[level] || level || '-'
}

function statusTag(status: string) {
  if (status === 'COMPLETED') return 'success'
  if (status === 'IN_PROCESS') return 'primary'
  if (status === 'CANCELLED') return 'info'
  return 'warning'
}

function statusLabel(status: string) {
  const map: Record<string, string> = {
    WAITING_ASSIGN: '待分配',
    QUEUED: '已排队',
    IN_PROCESS: '处理中',
    COMPLETED: '已完成',
    CANCELLED: '已取消'
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
    REFUNDED: '已退款'
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
    minute: '2-digit'
  })
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.application-page {
  min-height: 100%;
  padding: 48px 28px 28px;
  color: #172033;
  background:
    radial-gradient(circle at top right, rgba(37, 99, 235, 0.08), transparent 26%),
    linear-gradient(180deg, #f8fbff 0%, #f4f7fb 100%);
}

.page-top,
.panel,
.stat-card {
  border: 1px solid #dce6f3;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 16px 36px rgba(15, 23, 42, 0.05);
}

.page-top {
  display: flex;
  align-items: stretch;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 20px;
  padding: 24px 26px;
  border-radius: 24px;
}

.page-top-main {
  flex: 1;
}

.eyebrow {
  margin: 0 0 8px;
  color: #2563eb;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.1em;
}

.page-top h1 {
  margin: 0;
  color: #102033;
  font-size: 30px;
  line-height: 1.15;
}

.page-top p:last-of-type {
  max-width: 760px;
  margin: 12px 0 0;
  color: #64748b;
  font-size: 14px;
  line-height: 1.8;
}

.hero-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 18px;
}

.hero-tags span {
  display: inline-flex;
  align-items: center;
  padding: 8px 14px;
  border-radius: 999px;
  background: #eef4ff;
  color: #315fbb;
  font-size: 12px;
  font-weight: 700;
}

.page-top-side {
  display: flex;
  min-width: 220px;
  flex-direction: column;
  justify-content: space-between;
  align-items: flex-end;
  gap: 18px;
}

.hero-note {
  width: 100%;
  padding: 18px;
  border-radius: 20px;
  background: linear-gradient(145deg, #f8fbff, #eef5ff);
  border: 1px solid #d8e4f2;
}

.hero-note strong {
  display: block;
  color: #102033;
  font-size: 28px;
  line-height: 1;
}

.hero-note span {
  display: block;
  margin-top: 8px;
  color: #64748b;
  font-size: 13px;
  font-weight: 700;
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
  min-height: 92px;
  padding: 18px;
  border-radius: 22px;
}

.stat-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 54px;
  height: 54px;
  border-radius: 18px;
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
  color: #475569;
  font-size: 13px;
  font-weight: 700;
}

.panel {
  border-radius: 24px;
}

.filter-panel {
  margin-bottom: 18px;
  padding: 20px;
}

.filter-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 16px;
}

.filter-head strong,
.table-head strong {
  color: #102033;
  font-size: 17px;
}

.filter-head p,
.table-head p {
  margin: 6px 0 0;
  color: #64748b;
  font-size: 13px;
}

.filter-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 220px)) minmax(260px, 1fr);
  gap: 12px;
}

.filter-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.filter-item span {
  color: #5f718a;
  font-size: 12px;
  font-weight: 700;
}

.filter-item.wide {
  min-width: 0;
}

.filter-item :deep(.el-input__wrapper),
.filter-item :deep(.el-select__wrapper) {
  min-height: 44px;
  border-radius: 14px;
  box-shadow: 0 0 0 1px #d9e4f2 inset;
}

.table-panel {
  padding: 20px;
}

.table-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 14px;
}

.table-summary {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  align-items: center;
  gap: 8px;
  color: #64748b;
  font-size: 12px;
  font-weight: 700;
}

.application-table {
  width: 100%;
  table-layout: fixed;
}

.application-table :deep(.el-table) {
  border-radius: 18px;
}

.application-table :deep(.el-table__inner-wrapper::before) {
  display: none;
}

.application-table :deep(.el-table__row) {
  cursor: pointer;
}

.application-table :deep(.el-table__row:hover > td) {
  background: #f7faff !important;
}

.application-table :deep(.primary-action-button) {
  min-width: 72px;
  height: 34px;
  padding: 0 18px;
  border: 0;
  border-radius: 999px;
  font-weight: 800;
}

.application-table :deep(.primary-action-button.is-process) {
  background: #315fbb;
  color: #fff;
  box-shadow: 0 8px 18px rgba(49, 95, 187, .18);
}

.application-table :deep(.primary-action-button.is-process:hover),
.application-table :deep(.primary-action-button.is-process:focus) {
  background: #3f87dc;
  color: #fff;
}

.application-table :deep(.primary-action-button.is-view),
.application-table :deep(.primary-action-button.is-view:hover),
.application-table :deep(.primary-action-button.is-view:focus) {
  background: #d9fbe7;
  color: #177245;
  box-shadow: 0 8px 18px rgba(23, 114, 69, .12);
}

.application-table :deep(.primary-action-button.is-disabled),
.application-table :deep(.primary-action-button.is-disabled:hover),
.application-table :deep(.primary-action-button.is-disabled:focus) {
  background: #e2e8f0;
  color: #64748b;
  box-shadow: none;
  opacity: 1;
}

.pagination-area {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}

@media (max-width: 1100px) {
  .stats-row {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .filter-grid {
    grid-template-columns: 1fr 1fr;
  }

  .filter-item.wide {
    grid-column: 1 / -1;
  }
}

@media (max-width: 760px) {
  .application-page {
    padding: 18px;
  }

  .page-top,
  .filter-head,
  .table-head {
    flex-direction: column;
  }

  .page-top-side {
    width: 100%;
    min-width: 0;
    align-items: stretch;
  }

  .stats-row,
  .filter-grid {
    grid-template-columns: 1fr;
  }

  .table-summary {
    justify-content: flex-start;
  }
}
</style>

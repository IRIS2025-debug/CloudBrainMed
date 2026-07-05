<template>
  <div class="inspection-container">
    <el-card class="header-card">
      <div class="header-title">
        <h2>查看检查/检验申请</h2>
        <span class="subtitle">所有医技检查/检验申请单</span>
      </div>
    </el-card>

    <el-card class="table-card">
      <!-- 筛选栏 -->
      <div class="filter-bar">
        <el-input v-model="searchKey" placeholder="搜索患者姓名" clearable style="width: 200px" />
        <el-select v-model="filterUrgency" placeholder="紧急程度" clearable style="width: 140px">
          <el-option label="常规" value="NORMAL" />
          <el-option label="加急" value="URGENT" />
          <el-option label="紧急" value="EMERGENCY" />
        </el-select>
        <el-select v-model="filterStatus" placeholder="状态" clearable style="width: 140px">
          <el-option label="待分配" value="WAITING_ASSIGN" />
          <el-option label="已排队" value="QUEUED" />
          <el-option label="执行中" value="IN_PROGRESS" />
          <el-option label="已完成" value="COMPLETED" />
          <el-option label="已取消" value="CANCELLED" />
        </el-select>
      </div>

      <!-- 数据表格 -->
      <el-table :data="displayList" stripe v-loading="loading" style="width: 100%">
        <el-table-column prop="patientName" label="患者姓名" min-width="100" />
        <el-table-column prop="gender" label="性别" width="70">
          <template #default="{ row }">{{ row.gender === 1 ? '男' : '女' }}</template>
        </el-table-column>
        <el-table-column prop="age" label="年龄" width="70" />
        <el-table-column prop="itemName" label="检查/检验项目" min-width="160" show-overflow-tooltip />
        <el-table-column prop="clinicalSummary" label="临床摘要" min-width="180" show-overflow-tooltip />
        <el-table-column prop="urgencyLevel" label="紧急程度" width="100">
          <template #default="{ row }">
            <el-tag :type="urgencyTag(row.urgencyLevel)" size="small">{{ urgencyLabel(row.urgencyLevel) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="payStatus" label="支付" width="90">
          <template #default="{ row }">
            <el-tag :type="payTag(row.payStatus)" size="small">{{ payLabel(row.payStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="assignedRoom" label="分配房间" min-width="110">
          <template #default="{ row }">{{ row.assignedRoom || '-' }}</template>
        </el-table-column>
        <el-table-column prop="sourceType" label="来源" width="90">
          <template #default="{ row }">
            {{ row.sourceType === 'AI_ASSISTED' ? 'AI建议' : '医生开具' }}
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="申请时间" min-width="170" />
        <el-table-column label="操作" width="130" fixed="right">
          <template #default="{ row }">
            <el-button v-if="canAssign(row)" type="success" link @click="assignOrder(row)">分配</el-button>
            <el-button type="primary" link @click="showDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="检查/检验申请详情" width="650px">
      <el-descriptions v-if="currentOrder" :column="2" border>
        <el-descriptions-item label="申请编号">{{ currentOrder.orderId }}</el-descriptions-item>
        <el-descriptions-item label="患者姓名">{{ currentOrder.patientName }}</el-descriptions-item>
        <el-descriptions-item label="性别">{{ currentOrder.gender === 1 ? '男' : '女' }}</el-descriptions-item>
        <el-descriptions-item label="年龄">{{ currentOrder.age }}</el-descriptions-item>
        <el-descriptions-item label="检查/检验项目">{{ currentOrder.itemName }}</el-descriptions-item>
        <el-descriptions-item label="项目编码">{{ currentOrder.itemCode }}</el-descriptions-item>
        <el-descriptions-item label="紧急程度">
          <el-tag :type="urgencyTag(currentOrder.urgencyLevel)" size="small">{{ urgencyLabel(currentOrder.urgencyLevel) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="申请状态">
          <el-tag :type="statusTag(currentOrder.status)" size="small">{{ statusLabel(currentOrder.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="申请来源">{{ currentOrder.sourceType === 'AI_ASSISTED' ? 'AI建议' : '医生开具' }}</el-descriptions-item>
        <el-descriptions-item label="支付状态">{{ payLabel(currentOrder.payStatus) }}</el-descriptions-item>
        <el-descriptions-item label="分配房间">{{ currentOrder.assignedRoom || '-' }}</el-descriptions-item>
        <el-descriptions-item label="临床摘要" :span="2">
          <div class="clinical-summary-text">{{ currentOrder.clinicalSummary }}</div>
        </el-descriptions-item>
        <el-descriptions-item label="挂号编号">{{ currentOrder.registerId }}</el-descriptions-item>
        <el-descriptions-item label="开单医生">{{ currentOrder.doctorId }}</el-descriptions-item>
        <el-descriptions-item label="确认时间">{{ currentOrder.confirmedTime }}</el-descriptions-item>
        <el-descriptions-item label="申请时间">{{ currentOrder.createTime }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { assignInspectionOrder, getInspectionOrderList } from '@/api/inspection-doctor'
import { ElMessage, ElMessageBox } from 'element-plus'

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
const searchKey = ref('')
const filterUrgency = ref('')
const filterStatus = ref('')
const detailVisible = ref(false)
const currentOrder = ref<InspectionOrderVo | null>(null)

const displayList = computed(() => {
  let list = allList.value
  if (searchKey.value) {
    list = list.filter(item => item.patientName?.includes(searchKey.value))
  }
  if (filterUrgency.value) {
    list = list.filter(item => item.urgencyLevel === filterUrgency.value)
  }
  if (filterStatus.value) {
    list = list.filter(item => item.status === filterStatus.value)
  }
  return list
})

function urgencyTag(level: string) {
  if (level === 'EMERGENCY') return 'danger'
  if (level === 'URGENT') return 'warning'
  return 'info'
}

function urgencyLabel(level: string) {
  const map: Record<string, string> = { NORMAL: '常规', URGENT: '加急', EMERGENCY: '紧急' }
  return map[level] || level
}

function statusTag(status: string) {
  if (status === 'COMPLETED') return 'success'
  if (status === 'IN_PROGRESS') return ''
  if (status === 'CANCELLED') return 'danger'
  return 'warning'
}

function statusLabel(status: string) {
  const map: Record<string, string> = {
    WAITING_ASSIGN: '待分配', QUEUED: '已排队', IN_PROGRESS: '执行中',
    COMPLETED: '已完成', CANCELLED: '已取消'
  }
  return map[status] || status
}

function payLabel(pay: string) {
  const map: Record<string, string> = { WAITING: '待支付', PAID: '已支付', CANCELLED: '已取消', REFUNDED: '已退款' }
  return map[pay] || pay || '-'
}

function payTag(pay: string) {
  if (pay === 'PAID') return 'success'
  if (pay === 'CANCELLED' || pay === 'REFUNDED') return 'danger'
  return 'warning'
}

function canAssign(row: InspectionOrderVo) {
  return row.payStatus === 'PAID' && row.status === 'WAITING_ASSIGN'
}

async function assignOrder(row: InspectionOrderVo) {
  try {
    const result = await ElMessageBox.prompt(
      '请输入执行房间，例如 CT-1 或 LAB-2',
      '分配房间',
      {
        confirmButtonText: '分配',
        cancelButtonText: '取消',
        inputValue: row.assignedRoom || '',
        inputPattern: /\S+/,
        inputErrorMessage: '房间不能为空'
      }
    )
    await assignInspectionOrder(row.orderId, result.value)
    ElMessage.success('已分配并进入排队')
    await fetchData()
  } catch (error: any) {
    if (error !== 'cancel' && error?.action !== 'cancel') {
      ElMessage.error('分配失败')
    }
  }
}

function showDetail(row: InspectionOrderVo) {
  currentOrder.value = row
  detailVisible.value = true
}

async function fetchData() {
  loading.value = true
  try {
    const res: any = await getInspectionOrderList()
    allList.value = res.data || []
  } catch {
    ElMessage.error('加载检查/检验申请列表失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.inspection-container { padding: 20px; }
.header-card { margin-bottom: 16px; }
.header-title h2 { margin: 0 0 4px 0; font-size: 20px; }
.subtitle { color: #909399; font-size: 14px; }
.filter-bar { display: flex; gap: 12px; margin-bottom: 16px; }
.table-card { min-height: 400px; }
.clinical-summary-text {
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.6;
}
</style>
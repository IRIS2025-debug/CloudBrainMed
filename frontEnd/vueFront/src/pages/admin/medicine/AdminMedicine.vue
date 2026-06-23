<!-- src/pages/admin/medicine/AdminMedicine.vue -->
<template>
  <div class="medicine-page">
    <!-- 页面头部 -->
    <header class="page-header">
      <div class="header-left">
        <h2 class="page-title">药品信息管理</h2>
        <p class="page-subtitle">药品库存管理、预警与智能补货</p>
      </div>
      <div class="header-right">
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon>
          添加药品
        </el-button>
      </div>
    </header>

    <!-- 统计卡片 -->
    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-number">{{ totalCount }}</div>
        <div class="stat-label">药品总数</div>
      </div>
      <div class="stat-card warning">
        <div class="stat-number">{{ warningCount }}</div>
        <div class="stat-label">库存预警</div>
      </div>
      <div class="stat-card danger">
        <div class="stat-number">{{ outOfStockCount }}</div>
        <div class="stat-label">缺货药品</div>
      </div>
      <div class="stat-card success">
        <div class="stat-number">{{ reorderCount }}</div>
        <div class="stat-label">需补货建议</div>
      </div>
    </div>

    <!-- 工具栏 -->
    <div class="toolbar">
      <el-input
        v-model="keyword"
        placeholder="搜索药品名称"
        clearable
        style="width: 280px"
        @input="handleSearch"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
      <div class="toolbar-right">
        <el-button @click="fetchWarnings">
          <el-icon><Warning /></el-icon>
          库存预警 ({{ warningCount }})
        </el-button>
        <el-button type="warning" @click="fetchReorderSuggestions">
          <el-icon><Refresh /></el-icon>
          智能补货建议 ({{ reorderCount }})
        </el-button>
        <el-button @click="fetchData">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>
    </div>

    <!-- 药品表格 -->
    <div class="table-card">
      <el-table
        :data="tableData"
        v-loading="loading"
        stripe
        border
        style="width: 100%"
      >
        <el-table-column prop="name" label="药品名称" min-width="120" />
        <el-table-column prop="spec" label="规格" width="120" />
        <el-table-column prop="stock" label="库存" width="100" align="center">
          <template #default="{ row }">
            <span :class="getStockClass(row)">
              {{ row.stock }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="minStock" label="预警线" width="80" align="center" />
        <el-table-column prop="price" label="单价" width="100" align="center">
          <template #default="{ row }">
            ¥{{ Number(row.price).toFixed(2) }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" align="center" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="handleDeduct(row)">
              扣除
            </el-button>
            <el-button size="small" type="success" @click="handleAddStock(row)">
              补货
            </el-button>
            <el-button size="small" type="primary" @click="handleEdit(row)">
              编辑
            </el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- ===== 弹窗：添加/编辑 ===== -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="560px"
      destroy-on-close
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="100px"
      >
        <el-form-item label="药品名称" prop="name">
          <el-input v-model="formData.name" placeholder="请输入药品名称" />
        </el-form-item>
        <el-form-item label="规格" prop="spec">
          <el-input v-model="formData.spec" placeholder="如: 10mg×20片" />
        </el-form-item>
        <el-form-item label="用法用量" prop="usage">
          <el-input v-model="formData.usage" placeholder="如: 口服，每次1片" />
        </el-form-item>
        <el-form-item label="适应症" prop="indication">
          <el-input v-model="formData.indication" placeholder="请输入适应症" />
        </el-form-item>
        <el-form-item label="注意事项" prop="attention">
          <el-input
            v-model="formData.attention"
            type="textarea"
            :rows="2"
            placeholder="请输入注意事项"
          />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="库存数量" prop="stock">
              <el-input-number
                v-model="formData.stock"
                :min="0"
                :step="1"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="单价" prop="price">
              <el-input-number
                v-model="formData.price"
                :min="0"
                :precision="2"
                :step="1"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="预警线" prop="minStock">
              <el-input-number
                v-model="formData.minStock"
                :min="0"
                :step="1"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="建议补货量" prop="reorderQuantity">
              <el-input-number
                v-model="formData.reorderQuantity"
                :min="0"
                :step="5"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">
          确定
        </el-button>
      </template>
    </el-dialog>

    <!-- ===== 弹窗：扣除库存 ===== -->
    <el-dialog
      v-model="deductDialogVisible"
      title="扣除库存"
      width="400px"
      destroy-on-close
    >
      <div class="dialog-info">
        <p><strong>药品：</strong>{{ deductTarget?.name || '' }}</p>
        <p><strong>当前库存：</strong>{{ deductTarget?.stock || 0 }}</p>
      </div>
      <el-form label-width="100px">
        <el-form-item label="扣除数量">
          <el-input-number
            v-model="deductQuantity"
            :min="1"
            :max="deductTarget?.stock || 0"
            :step="1"
            controls-position="right"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="deductDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="deductLoading" @click="handleDeductConfirm">
          确认扣除
        </el-button>
      </template>
    </el-dialog>

    <!-- ===== 弹窗：补货 ===== -->
    <el-dialog
      v-model="addStockDialogVisible"
      title="补货"
      width="400px"
      destroy-on-close
    >
      <div class="dialog-info">
        <p><strong>药品：</strong>{{ addStockTarget?.name || '' }}</p>
        <p><strong>当前库存：</strong>{{ addStockTarget?.stock || 0 }}</p>
        <p v-if="addStockTarget">
          <strong>建议补货量：</strong>
          <el-tag type="warning" size="small">
            {{ addStockTarget.reorderQuantity || 50 }}
          </el-tag>
        </p>
      </div>
      <el-form label-width="100px">
        <el-form-item label="补货数量">
          <el-input-number
            v-model="addStockQuantity"
            :min="1"
            :step="5"
            controls-position="right"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addStockDialogVisible = false">取消</el-button>
        <el-button type="success" :loading="addStockLoading" @click="handleAddStockConfirm">
          确认补货
        </el-button>
      </template>
    </el-dialog>

    <!-- ===== 弹窗：预警列表 ===== -->
    <el-dialog
      v-model="warnDialogVisible"
      title="库存预警"
      width="700px"
      destroy-on-close
    >
      <el-table :data="warnList" border stripe style="width: 100%">
        <el-table-column prop="name" label="药品名称" />
        <el-table-column prop="spec" label="规格" />
        <el-table-column prop="stock" label="当前库存" align="center">
          <template #default="{ row }">
            <span style="color: #dc2626; font-weight: 600;">{{ row.stock }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="minStock" label="预警线" align="center" />
        <el-table-column prop="status" label="状态" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'OUT_OF_STOCK' ? 'danger' : 'warning'" size="small">
              {{ row.status === 'OUT_OF_STOCK' ? '缺货' : '库存不足' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center">
          <template #default="{ row }">
            <el-button size="small" type="success" @click="quickAddStock(row)">
              快速补货
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <div v-if="warnList.length === 0" class="empty-tip">
        <el-empty description="暂无库存预警" />
      </div>
    </el-dialog>

    <!-- ===== 弹窗：智能补货建议 ===== -->
    <el-dialog
      v-model="reorderDialogVisible"
      title="智能补货建议"
      width="750px"
      destroy-on-close
    >
      <div class="reorder-tip">
        <el-alert
          title="智能补货建议"
          type="info"
          description="根据当前库存、预警线和历史消耗，系统自动计算建议补货量"
          :closable="false"
          show-icon
        />
      </div>
      <el-table :data="reorderList" border stripe style="width: 100%">
        <el-table-column prop="name" label="药品名称" />
        <el-table-column prop="spec" label="规格" />
        <el-table-column prop="stock" label="当前库存" align="center">
          <template #default="{ row }">
            <span :style="{ color: row.stock === 0 ? '#dc2626' : '#d97706', fontWeight: 600 }">
              {{ row.stock }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="minStock" label="预警线" align="center" />
        <el-table-column prop="suggestedReorder" label="建议补货量" align="center">
          <template #default="{ row }">
            <el-tag type="warning" size="large">
              {{ row.suggestedReorder || 50 }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center">
          <template #default="{ row }">
            <el-button
              size="small"
              type="success"
              @click="quickAddStockBySuggestion(row)"
            >
              按建议补货
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <div v-if="reorderList.length === 0" class="empty-tip">
        <el-empty description="暂无需要补货的药品" />
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus, Search, Warning, Refresh } from '@element-plus/icons-vue'
import {
  getMedicineList,
  addMedicine,
  updateMedicine,
  deleteMedicine,
  deductStock,
  addStock,
  getWarnings,
  getReorderSuggestions
} from '@/api/admin/medicine'
import type { Medicine, MedicineDto, MedicineWarnVo } from '@/types/admin/adminMedicine'

// ============================================================
// 状态定义
// ============================================================

const loading = ref<boolean>(false)
const keyword = ref<string>('')
const tableData = ref<Medicine[]>([])

const totalCount = ref<number>(0)
const warningCount = ref<number>(0)
const outOfStockCount = ref<number>(0)
const reorderCount = ref<number>(0)

const dialogVisible = ref<boolean>(false)
const dialogTitle = ref<string>('添加药品')
const isEdit = ref<boolean>(false)
const submitLoading = ref<boolean>(false)

const formRef = ref<FormInstance | null>(null)
const formData = reactive<MedicineDto>({
  name: '',
  spec: '',
  usage: '',
  indication: '',
  attention: '',
  stock: 0,
  price: 0,
  minStock: 10,
  reorderQuantity: 50
})

const deductDialogVisible = ref<boolean>(false)
const deductTarget = ref<Medicine | null>(null)
const deductQuantity = ref<number>(1)
const deductLoading = ref<boolean>(false)

const addStockDialogVisible = ref<boolean>(false)
const addStockTarget = ref<Medicine | null>(null)
const addStockQuantity = ref<number>(1)
const addStockLoading = ref<boolean>(false)

const warnDialogVisible = ref<boolean>(false)
const warnList = ref<MedicineWarnVo[]>([])

const reorderDialogVisible = ref<boolean>(false)
const reorderList = ref<MedicineWarnVo[]>([])

// ============================================================
// 表单验证规则
// ============================================================
const formRules: FormRules = {
  name: [{ required: true, message: '请输入药品名称', trigger: 'blur' }],
  spec: [{ required: true, message: '请输入规格', trigger: 'blur' }],
  stock: [{ required: true, message: '请输入库存数量', trigger: 'blur' }],
  price: [{ required: true, message: '请输入单价', trigger: 'blur' }],
  minStock: [{ required: true, message: '请设置预警线', trigger: 'blur' }]
}

// ============================================================
// 方法
// ============================================================

function getStatusType(status: string): 'success' | 'warning' | 'danger' | 'info' {
  const statusMap: Record<string, 'success' | 'warning' | 'danger' | 'info'> = {
    NORMAL: 'success',
    LOW_STOCK: 'warning',
    OUT_OF_STOCK: 'danger'
  }
  return statusMap[status] || 'info'
}

function getStatusText(status: string): string {
  const statusMap: Record<string, string> = {
    NORMAL: '正常',
    LOW_STOCK: '库存不足',
    OUT_OF_STOCK: '缺货'
  }
  return statusMap[status] || status
}

function getStockClass(row: Medicine): string {
  if (row.stock === 0) {
    return 'stock-danger'
  }
  if (row.stock <= (row.minStock || 10)) {
    return 'stock-warning'
  }
  return 'stock-normal'
}

/**
 * 获取数据
 */
async function fetchData(): Promise<void> {
  loading.value = true
  try {
    const res = await getMedicineList(keyword.value)
    if (res.code === 200) {
      tableData.value = res.data || []
      totalCount.value = tableData.value.length
      
      warningCount.value = tableData.value.filter(
        (item) => item.status === 'LOW_STOCK' || item.status === 'OUT_OF_STOCK'
      ).length
      outOfStockCount.value = tableData.value.filter(
        (item) => item.status === 'OUT_OF_STOCK'
      ).length
      reorderCount.value = tableData.value.filter(
        (item) => item.stock <= (item.minStock || 10) + (item.reorderQuantity || 50)
      ).length
    } else {
      ElMessage.error(res.message || '获取数据失败')
    }
  } catch (error) {
    console.error('获取数据失败:', error)
    ElMessage.error('获取数据失败')
  } finally {
    loading.value = false
  }
}

/**
 * 获取预警列表
 */
async function fetchWarnings(): Promise<void> {
  try {
    const res = await getWarnings()
    if (res.code === 200) {
      warnList.value = res.data || []
      warnDialogVisible.value = true
    } else {
      ElMessage.error(res.message || '获取预警失败')
    }
  } catch (error) {
    console.error('获取预警失败:', error)
    ElMessage.error('获取预警失败')
  }
}

/**
 * 获取补货建议
 */
async function fetchReorderSuggestions(): Promise<void> {
  try {
    const res = await getReorderSuggestions()
    if (res.code === 200) {
      reorderList.value = res.data || []
      reorderDialogVisible.value = true
    } else {
      ElMessage.error(res.message || '获取补货建议失败')
    }
  } catch (error) {
    console.error('获取补货建议失败:', error)
    ElMessage.error('获取补货建议失败')
  }
}

function handleSearch(): void {
  fetchData()
}

function resetForm(): void {
  formData.name = ''
  formData.spec = ''
  formData.usage = ''
  formData.indication = ''
  formData.attention = ''
  formData.stock = 0
  formData.price = 0
  formData.minStock = 10
  formData.reorderQuantity = 50
  formData.medicineId = ''
}

function handleAdd(): void {
  isEdit.value = false
  dialogTitle.value = '添加药品'
  resetForm()
  dialogVisible.value = true
}

function handleEdit(row: Medicine): void {
  isEdit.value = true
  dialogTitle.value = '编辑药品'
  formData.medicineId = row.medicineId
  formData.name = row.name
  formData.spec = row.spec
  formData.usage = row.usage || ''
  formData.indication = row.indication || ''
  formData.attention = row.attention || ''
  formData.stock = row.stock
  formData.price = row.price
  formData.minStock = row.minStock || 10
  formData.reorderQuantity = row.reorderQuantity || 50
  dialogVisible.value = true
}

async function handleSubmit(): Promise<void> {
  if (!formRef.value) {
    return
  }
  
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  
  submitLoading.value = true
  try {
    let res
    if (isEdit.value) {
      res = await updateMedicine(formData)
    } else {
      res = await addMedicine(formData)
    }
    if (res.code === 200) {
      ElMessage.success(res.message || '操作成功')
      dialogVisible.value = false
      fetchData()
    } else {
      ElMessage.error(res.message || '操作失败')
    }
  } catch (error) {
    console.error('提交失败:', error)
    ElMessage.error('提交失败')
  } finally {
    submitLoading.value = false
  }
}

async function handleDelete(row: Medicine): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `确定要删除药品 "${row.name}" 吗？`,
      '删除确认',
      { type: 'warning' }
    )
    const res = await deleteMedicine(row.medicineId)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      fetchData()
    } else {
      ElMessage.error(res.message || '删除失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
    }
  }
}

function handleDeduct(row: Medicine): void {
  deductTarget.value = row
  deductQuantity.value = 1
  deductDialogVisible.value = true
}

async function handleDeductConfirm(): Promise<void> {
  if (!deductTarget.value) {
    return
  }
  if (deductQuantity.value <= 0) {
    ElMessage.warning('请输入有效的扣除数量')
    return
  }
  if (deductQuantity.value > deductTarget.value.stock) {
    ElMessage.warning('库存不足')
    return
  }
  
  deductLoading.value = true
  try {
    const res = await deductStock(deductTarget.value.medicineId, deductQuantity.value)
    if (res.code === 200) {
      ElMessage.success(res.message || '扣库存成功')
      deductDialogVisible.value = false
      fetchData()
    } else {
      ElMessage.error(res.message || '扣库存失败')
    }
  } catch (error) {
    console.error('扣库存失败:', error)
    ElMessage.error('扣库存失败')
  } finally {
    deductLoading.value = false
  }
}

function handleAddStock(row: Medicine): void {
  addStockTarget.value = row
  addStockQuantity.value = row.reorderQuantity || 50
  addStockDialogVisible.value = true
}

async function handleAddStockConfirm(): Promise<void> {
  if (!addStockTarget.value) {
    return
  }
  if (addStockQuantity.value <= 0) {
    ElMessage.warning('请输入有效的补货数量')
    return
  }
  
  addStockLoading.value = true
  try {
    const res = await addStock(addStockTarget.value.medicineId, addStockQuantity.value)
    if (res.code === 200) {
      ElMessage.success(res.message || '补货成功')
      addStockDialogVisible.value = false
      fetchData()
    } else {
      ElMessage.error(res.message || '补货失败')
    }
  } catch (error) {
    console.error('补货失败:', error)
    ElMessage.error('补货失败')
  } finally {
    addStockLoading.value = false
  }
}

function quickAddStock(row: MedicineWarnVo): void {
  // 只包含 Medicine 类型中存在的字段
  const target: Medicine = {
    medicineId: row.medicineId,
    name: row.name,
    spec: row.spec,
    stock: row.stock,
    price: 0,
    usage: '',
    indication: '',
    attention: '',
    minStock: row.minStock,
    reorderQuantity: row.suggestedReorder || 50,
    status: row.status as Medicine['status'],
    createTime: ''
  }
  addStockTarget.value = target
  addStockQuantity.value = row.suggestedReorder || 50
  warnDialogVisible.value = false
  addStockDialogVisible.value = true
}

function quickAddStockBySuggestion(row: MedicineWarnVo): void {
  // 只包含 Medicine 类型中存在的字段
  const target: Medicine = {
    medicineId: row.medicineId,
    name: row.name,
    spec: row.spec,
    stock: row.stock,
    price: 0,
    usage: '',
    indication: '',
    attention: '',
    minStock: row.minStock,
    reorderQuantity: row.suggestedReorder || 50,
    status: row.status as Medicine['status'],
    createTime: ''
  }
  addStockTarget.value = target
  addStockQuantity.value = row.suggestedReorder || 50
  reorderDialogVisible.value = false
  addStockDialogVisible.value = true
}

// ============================================================
// 生命周期
// ============================================================
onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.medicine-page {
  padding: 24px 32px;
  min-height: 100vh;
  background: #f1f5f9;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.header-left .page-title {
  font-size: 22px;
  font-weight: 700;
  color: #0f172a;
  margin: 0 0 2px 0;
}

.header-left .page-subtitle {
  font-size: 13px;
  color: #94a3b8;
  margin: 0;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.stat-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}

.stat-card .stat-number {
  font-size: 28px;
  font-weight: 700;
  color: #0f172a;
}

.stat-card .stat-label {
  font-size: 14px;
  color: #94a3b8;
  margin-top: 4px;
}

.stat-card.warning .stat-number {
  color: #d97706;
}

.stat-card.danger .stat-number {
  color: #dc2626;
}

.stat-card.success .stat-number {
  color: #10b981;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 12px;
}

.toolbar-right {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.table-card {
  background: #fff;
  border-radius: 12px;
  padding: 16px 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  overflow: hidden;
}

.stock-danger {
  color: #dc2626;
  font-weight: 600;
}

.stock-warning {
  color: #d97706;
  font-weight: 600;
}

.stock-normal {
  color: #0f172a;
}

.dialog-info {
  margin-bottom: 16px;
  padding: 12px 16px;
  background: #f8fafc;
  border-radius: 8px;
}

.dialog-info p {
  margin: 4px 0;
}

.empty-tip {
  padding: 20px 0;
}

.reorder-tip {
  margin-bottom: 16px;
}

@media (max-width: 1024px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .medicine-page {
    padding: 16px;
  }
  .page-header {
    flex-direction: column;
    align-items: stretch;
  }
  .header-right {
    margin-top: 8px;
  }
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .toolbar {
    flex-direction: column;
    align-items: stretch;
  }
  .toolbar-right {
    flex-wrap: wrap;
  }
}
</style>
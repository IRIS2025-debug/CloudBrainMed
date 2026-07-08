<template>
  <div class="medicine-page">
    <section class="page-top">
      <div class="page-top-copy">
        <div class="page-icon">
          <el-icon><FirstAidKit /></el-icon>
        </div>
        <div>
          <p class="page-eyebrow">药品工作台</p>
          <h2 class="page-title">药品管理</h2>
          <p class="page-subtitle">查看库存、预警与补货建议。</p>
        </div>
      </div>

      <el-button type="primary" size="large" round @click="handleAdd">
        <el-icon><Plus /></el-icon>
        <span>新增药品</span>
      </el-button>
    </section>

    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-icon total">
          <el-icon><Goods /></el-icon>
        </div>
        <div class="stat-copy">
          <span class="stat-label">药品总数</span>
          <strong class="stat-value">{{ totalCount }}</strong>
          <span class="stat-note">当前台账记录</span>
        </div>
      </div>
      <div class="stat-card warning">
        <div class="stat-icon warning">
          <el-icon><WarningFilled /></el-icon>
        </div>
        <div class="stat-copy">
          <span class="stat-label">库存预警</span>
          <strong class="stat-value">{{ warningCount }}</strong>
          <span class="stat-note">低于预警线或缺货</span>
        </div>
      </div>
      <div class="stat-card danger">
        <div class="stat-icon danger">
          <el-icon><CircleCloseFilled /></el-icon>
        </div>
        <div class="stat-copy">
          <span class="stat-label">缺货药品</span>
          <strong class="stat-value">{{ outOfStockCount }}</strong>
          <span class="stat-note">需要尽快处理</span>
        </div>
      </div>
      <div class="stat-card success">
        <div class="stat-icon success">
          <el-icon><ShoppingCartFull /></el-icon>
        </div>
        <div class="stat-copy">
          <span class="stat-label">补货建议</span>
          <strong class="stat-value">{{ reorderCount }}</strong>
          <span class="stat-note">按当前规则计算</span>
        </div>
      </div>
    </div>

    <section class="workspace-card">
      <div class="workspace-head">
        <div>
          <strong>查询与操作</strong>
          <p>库存状态按现有逻辑计算，操作流程保持不变。</p>
        </div>
      </div>

      <div class="toolbar">
        <el-input
          v-model="keyword"
          placeholder="搜索药品名称"
          clearable
          class="toolbar-search"
          @input="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>

        <div class="toolbar-actions">
          <el-button @click="fetchWarnings">
            <el-icon><Warning /></el-icon>
            <span>库存预警</span>
            <span class="btn-count">{{ warningCount }}</span>
          </el-button>
          <el-button type="warning" @click="fetchReorderSuggestions">
            <el-icon><Refresh /></el-icon>
            <span>补货建议</span>
            <span class="btn-count">{{ reorderCount }}</span>
          </el-button>
          <el-button @click="fetchData">
            <el-icon><Refresh /></el-icon>
            <span>刷新</span>
          </el-button>
        </div>
      </div>
    </section>

    <section class="table-card">
      <div class="workspace-head table-head">
        <div>
          <strong>药品列表</strong>
          <p>保留当前增删改查、扣减库存和补货流程。</p>
        </div>
      </div>

      <el-table :data="tableData" v-loading="loading" stripe class="medicine-table">
        <el-table-column prop="name" label="药品名称" min-width="160" />
        <el-table-column prop="spec" label="规格" min-width="140" />
        <el-table-column prop="usage" label="用法用量" min-width="180" show-overflow-tooltip />
        <el-table-column prop="stock" label="库存" width="96" align="center">
          <template #default="{ row }">
            <span :class="getStockClass(row)">
              {{ row.stock }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="minStock" label="预警线" width="96" align="center" />
        <el-table-column prop="price" label="单价" width="110" align="center">
          <template #default="{ row }">￥{{ Number(row.price).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="112" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" effect="light" round size="small">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="288" align="center" fixed="right">
          <template #default="{ row }">
            <div class="row-actions">
              <el-button size="small" @click="handleDeduct(row)">扣减</el-button>
              <el-button size="small" type="success" @click="handleAddStock(row)">补货</el-button>
              <el-button size="small" type="primary" @click="handleEdit(row)">编辑</el-button>
              <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="580px" destroy-on-close>
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-form-item label="药品名称" prop="name">
          <el-input v-model="formData.name" placeholder="请输入药品名称" />
        </el-form-item>
        <el-form-item label="规格" prop="spec">
          <el-input v-model="formData.spec" placeholder="例如：10mg x 20片" />
        </el-form-item>
        <el-form-item label="用法用量" prop="usage">
          <el-input v-model="formData.usage" placeholder="例如：口服，每次 1 片" />
        </el-form-item>
        <el-form-item label="适应症" prop="indication">
          <el-input v-model="formData.indication" placeholder="请输入适应症" />
        </el-form-item>
        <el-form-item label="注意事项" prop="attention">
          <el-input v-model="formData.attention" type="textarea" :rows="2" placeholder="请输入注意事项" />
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
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="deductDialogVisible" title="扣减库存" width="420px" destroy-on-close>
      <div class="dialog-info">
        <p><strong>药品：</strong>{{ deductTarget?.name || '' }}</p>
        <p><strong>当前库存：</strong>{{ deductTarget?.stock || 0 }}</p>
      </div>
      <el-form label-width="100px">
        <el-form-item label="扣减数量">
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
        <el-button type="primary" :loading="deductLoading" @click="handleDeductConfirm">确认扣减</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="addStockDialogVisible" title="补货" width="420px" destroy-on-close>
      <div class="dialog-info">
        <p><strong>药品：</strong>{{ addStockTarget?.name || '' }}</p>
        <p><strong>当前库存：</strong>{{ addStockTarget?.stock || 0 }}</p>
        <p v-if="addStockTarget">
          <strong>建议补货量：</strong>
          <el-tag type="warning" effect="light" round size="small">
            {{ addStockTarget.reorderQuantity || 50 }}
          </el-tag>
        </p>
      </div>
      <el-form label-width="100px">
        <el-form-item label="补货数量">
          <el-input-number v-model="addStockQuantity" :min="1" :step="5" controls-position="right" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addStockDialogVisible = false">取消</el-button>
        <el-button type="success" :loading="addStockLoading" @click="handleAddStockConfirm">确认补货</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="warnDialogVisible" title="库存预警" width="720px" destroy-on-close>
      <el-table :data="warnList" stripe class="dialog-table">
        <el-table-column prop="name" label="药品名称" min-width="160" />
        <el-table-column prop="spec" label="规格" min-width="140" />
        <el-table-column prop="stock" label="当前库存" width="100" align="center">
          <template #default="{ row }">
            <span class="warning-text">{{ row.stock }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="minStock" label="预警线" width="96" align="center" />
        <el-table-column prop="status" label="状态" width="112" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'OUT_OF_STOCK' ? 'danger' : 'warning'" effect="light" round size="small">
              {{ row.status === 'OUT_OF_STOCK' ? '缺货' : '库存不足' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="center">
          <template #default="{ row }">
            <el-button size="small" type="success" @click="quickAddStock(row)">快速补货</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div v-if="warnList.length === 0" class="empty-tip">
        <el-empty description="暂无库存预警" />
      </div>
    </el-dialog>

    <el-dialog v-model="reorderDialogVisible" title="补货建议" width="760px" destroy-on-close>
      <div class="reorder-tip">
        <el-alert
          title="系统根据库存、预警线和建议补货量生成当前建议。"
          type="info"
          :closable="false"
          show-icon
        />
      </div>
      <el-table :data="reorderList" stripe class="dialog-table">
        <el-table-column prop="name" label="药品名称" min-width="160" />
        <el-table-column prop="spec" label="规格" min-width="140" />
        <el-table-column prop="stock" label="当前库存" width="100" align="center">
          <template #default="{ row }">
            <span :class="row.stock === 0 ? 'warning-text' : 'stock-warning'">
              {{ row.stock }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="minStock" label="预警线" width="96" align="center" />
        <el-table-column prop="suggestedReorder" label="建议补货量" width="120" align="center">
          <template #default="{ row }">
            <el-tag type="warning" effect="light" round>
              {{ row.suggestedReorder || 50 }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="center">
          <template #default="{ row }">
            <el-button size="small" type="success" @click="quickAddStockBySuggestion(row)">按建议补货</el-button>
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
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  CircleCloseFilled,
  FirstAidKit,
  Goods,
  Plus,
  Refresh,
  Search,
  ShoppingCartFull,
  Warning,
  WarningFilled,
} from '@element-plus/icons-vue'
import {
  addMedicine,
  addStock,
  deductStock,
  deleteMedicine,
  getMedicineList,
  getReorderSuggestions,
  getWarnings,
  updateMedicine,
} from '@/api/admin/medicine'
import type { Medicine, MedicineDto, MedicineWarnVo } from '@/types/admin/adminMedicine'

const loading = ref(false)
const keyword = ref('')
const tableData = ref<Medicine[]>([])

const totalCount = ref(0)
const warningCount = ref(0)
const outOfStockCount = ref(0)
const reorderCount = ref(0)

const dialogVisible = ref(false)
const dialogTitle = ref('新增药品')
const isEdit = ref(false)
const submitLoading = ref(false)

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
  reorderQuantity: 50,
})

const deductDialogVisible = ref(false)
const deductTarget = ref<Medicine | null>(null)
const deductQuantity = ref(1)
const deductLoading = ref(false)

const addStockDialogVisible = ref(false)
const addStockTarget = ref<Medicine | null>(null)
const addStockQuantity = ref(1)
const addStockLoading = ref(false)

const warnDialogVisible = ref(false)
const warnList = ref<MedicineWarnVo[]>([])

const reorderDialogVisible = ref(false)
const reorderList = ref<MedicineWarnVo[]>([])

const formRules: FormRules = {
  name: [{ required: true, message: '请输入药品名称', trigger: 'blur' }],
  spec: [{ required: true, message: '请输入规格', trigger: 'blur' }],
  stock: [{ required: true, message: '请输入库存数量', trigger: 'blur' }],
  price: [{ required: true, message: '请输入单价', trigger: 'blur' }],
  minStock: [{ required: true, message: '请设置预警线', trigger: 'blur' }],
}

function getStatusType(status: string): 'success' | 'warning' | 'danger' | 'info' {
  const statusMap: Record<string, 'success' | 'warning' | 'danger' | 'info'> = {
    NORMAL: 'success',
    LOW_STOCK: 'warning',
    OUT_OF_STOCK: 'danger',
  }
  return statusMap[status] || 'info'
}

function getStatusText(status: string): string {
  const statusMap: Record<string, string> = {
    NORMAL: '正常',
    LOW_STOCK: '库存不足',
    OUT_OF_STOCK: '缺货',
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

async function fetchData(): Promise<void> {
  loading.value = true
  try {
    const res = await getMedicineList(keyword.value)
    if (res.code === 200) {
      tableData.value = res.data || []
      totalCount.value = tableData.value.length
      warningCount.value = tableData.value.filter(
        (item) => item.status === 'LOW_STOCK' || item.status === 'OUT_OF_STOCK',
      ).length
      outOfStockCount.value = tableData.value.filter((item) => item.status === 'OUT_OF_STOCK').length
      reorderCount.value = tableData.value.filter(
        (item) => item.stock <= (item.minStock || 10) + (item.reorderQuantity || 50),
      ).length
    } else {
      ElMessage.error(res.message || '获取数据失败')
    }
  } catch (error) {
    console.error('获取药品数据失败:', error)
    ElMessage.error('获取数据失败')
  } finally {
    loading.value = false
  }
}

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
  dialogTitle.value = '新增药品'
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
  if (!formRef.value) return

  try {
    await formRef.value.validate()
  } catch {
    return
  }

  submitLoading.value = true
  try {
    const res = isEdit.value ? await updateMedicine(formData) : await addMedicine(formData)
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
    await ElMessageBox.confirm(`确定要删除药品“${row.name}”吗？`, '删除确认', { type: 'warning' })
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
  if (!deductTarget.value) return
  if (deductQuantity.value <= 0) {
    ElMessage.warning('请输入有效的扣减数量')
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
      ElMessage.success(res.message || '库存扣减成功')
      deductDialogVisible.value = false
      fetchData()
    } else {
      ElMessage.error(res.message || '库存扣减失败')
    }
  } catch (error) {
    console.error('库存扣减失败:', error)
    ElMessage.error('库存扣减失败')
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
  if (!addStockTarget.value) return
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
    createTime: '',
  }
  addStockTarget.value = target
  addStockQuantity.value = row.suggestedReorder || 50
  warnDialogVisible.value = false
  addStockDialogVisible.value = true
}

function quickAddStockBySuggestion(row: MedicineWarnVo): void {
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
    createTime: '',
  }
  addStockTarget.value = target
  addStockQuantity.value = row.suggestedReorder || 50
  reorderDialogVisible.value = false
  addStockDialogVisible.value = true
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.medicine-page {
  min-height: 100vh;
  padding: 28px 32px 36px;
  background:
    radial-gradient(circle at top right, rgba(79, 141, 247, 0.1), transparent 24%),
    #f4f7fb;
}

.page-top,
.workspace-card,
.table-card,
.stat-card {
  border-radius: 24px;
  border: 1px solid rgba(219, 228, 240, 0.95);
  box-shadow: 0 16px 36px rgba(31, 41, 55, 0.06);
}

.page-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
  padding: 24px 28px;
  background: linear-gradient(135deg, #ffffff 0%, #f6f9ff 100%);
}

.page-top-copy {
  display: flex;
  align-items: center;
  gap: 16px;
  min-width: 0;
}

.page-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 52px;
  height: 52px;
  border-radius: 16px;
  background: linear-gradient(135deg, #315fbb, #4f8df7);
  color: #fff;
  box-shadow: 0 14px 28px rgba(49, 95, 187, 0.2);
  flex-shrink: 0;
}

.page-icon .el-icon {
  font-size: 24px;
}

.page-eyebrow {
  margin: 0 0 8px;
  color: #315fbb;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.12em;
}

.page-title {
  margin: 0;
  color: #16304d;
  font-size: 28px;
  font-weight: 800;
  letter-spacing: -0.03em;
}

.page-subtitle {
  margin: 10px 0 0;
  color: #72859d;
  font-size: 14px;
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 20px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 20px 22px;
  background: #fff;
}

.stat-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  border-radius: 16px;
  color: #fff;
  flex-shrink: 0;
}

.stat-icon.total {
  background: linear-gradient(135deg, #315fbb, #4f8df7);
}

.stat-icon.warning {
  background: linear-gradient(135deg, #d97706, #f59e0b);
}

.stat-icon.danger {
  background: linear-gradient(135deg, #dc2626, #f87171);
}

.stat-icon.success {
  background: linear-gradient(135deg, #16a34a, #4ade80);
}

.stat-copy {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 6px;
}

.stat-label {
  color: #6b7f98;
  font-size: 13px;
  font-weight: 700;
}

.stat-value {
  color: #16304d;
  font-size: 30px;
  line-height: 1;
}

.stat-note {
  color: #94a3b8;
  font-size: 12px;
}

.stat-card.warning .stat-value {
  color: #d97706;
}

.stat-card.danger .stat-value {
  color: #dc2626;
}

.stat-card.success .stat-value {
  color: #16a34a;
}

.workspace-card,
.table-card {
  margin-bottom: 18px;
  padding: 20px 22px;
  background: #fff;
}

.table-card {
  margin-bottom: 0;
  overflow: hidden;
}

.workspace-head {
  margin-bottom: 16px;
}

.workspace-head strong {
  display: block;
  color: #16304d;
  font-size: 16px;
  font-weight: 800;
}

.workspace-head p {
  margin: 6px 0 0;
  color: #72859d;
  font-size: 13px;
  line-height: 1.6;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.toolbar-search {
  width: 300px;
}

.toolbar-search :deep(.el-input__wrapper) {
  border-radius: 14px;
  box-shadow: 0 0 0 1px #dce6f2 inset;
}

.toolbar-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.btn-count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 20px;
  height: 20px;
  margin-left: 4px;
  padding: 0 6px;
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.07);
  font-size: 12px;
  font-weight: 700;
}

.table-head {
  margin-bottom: 10px;
}

.medicine-table :deep(.el-table) {
  --el-table-border-color: transparent;
  --el-table-row-hover-bg-color: #fbfdff;
}

.medicine-table :deep(.el-table__inner-wrapper::before) {
  display: none;
}

.medicine-table :deep(th) {
  background: #f7faff;
  color: #64748b;
  font-size: 13px;
  font-weight: 700;
}

.medicine-table :deep(td),
.medicine-table :deep(th.is-leaf) {
  border-right: none;
}

.medicine-table :deep(.el-table__body td) {
  padding: 15px 0;
}

.medicine-table :deep(.el-table__fixed-right) {
  box-shadow: -8px 0 18px rgba(15, 23, 42, 0.06);
}

.medicine-table :deep(.el-table__fixed-right::before) {
  display: none;
}

.medicine-table :deep(.el-table__fixed-right th),
.medicine-table :deep(.el-table__fixed-right td) {
  background: #fff;
}

.stock-danger,
.warning-text {
  color: #dc2626;
  font-weight: 700;
}

.stock-warning {
  color: #d97706;
  font-weight: 700;
}

.stock-normal {
  color: #0f172a;
  font-weight: 600;
}

.row-actions {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  white-space: nowrap;
}

.row-actions :deep(.el-button) {
  min-width: 48px;
  margin-left: 0;
  border-radius: 9px;
  font-weight: 700;
}

.dialog-info {
  margin-bottom: 16px;
  padding: 14px 16px;
  border-radius: 14px;
  border: 1px solid #dce6f2;
  background: #f7faff;
}

.dialog-info p {
  margin: 4px 0;
  color: #4a5f7a;
  font-size: 13px;
}

.dialog-table :deep(.el-table) {
  --el-table-border-color: transparent;
}

.dialog-table :deep(.el-table__inner-wrapper::before) {
  display: none;
}

.dialog-table :deep(th) {
  background: #f8fbff;
}

.empty-tip {
  padding: 20px 0;
}

.reorder-tip {
  margin-bottom: 16px;
}

@media (max-width: 1180px) {
  .stats-row {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .medicine-page {
    padding: 12px 8px 20px;
  }

  .page-top,
  .workspace-card,
  .table-card {
    padding: 14px;
  }

  .page-top {
    flex-direction: column;
    align-items: stretch;
  }

  .page-top-copy {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .page-icon {
    width: 46px;
    height: 46px;
    border-radius: 14px;
  }

  .page-title {
    font-size: 22px;
    line-height: 1.2;
    letter-spacing: 0;
  }

  .page-subtitle {
    font-size: 13px;
    line-height: 1.6;
  }

  .stat-card {
    align-items: flex-start;
    padding: 14px;
  }

  .stat-icon {
    width: 42px;
    height: 42px;
    border-radius: 14px;
  }

  .stat-value {
    font-size: 26px;
  }

  .stats-row {
    grid-template-columns: 1fr;
  }

  .toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .toolbar-search {
    width: 100%;
  }

  .toolbar-actions {
    width: 100%;
  }
}
</style>

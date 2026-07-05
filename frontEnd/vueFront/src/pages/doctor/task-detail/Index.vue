<template>
  <div class="page">
    <header class="page-top">
      <el-button @click="$router.back()">
        <el-icon style="margin-right:4px"><ArrowLeft /></el-icon>返回
      </el-button>
      <h2>任务详情</h2>
      <div></div>
    </header>

    <div class="card" v-loading="loading">
      <template v-if="task">
        <div class="detail-header">
          <div>
            <span class="badge" :class="'badge-' + task.urgencyLevel">{{ task.urgencyLabel }}</span>
            <span class="status-tag" :class="'st-' + task.status" style="margin-left:8px">{{ task.statusLabel }}</span>
            <span class="category-tag" style="margin-left:8px">{{ task.itemCategoryLabel }}</span>
          </div>
          <div class="detail-actions">
            <el-button v-if="task.status === 'QUEUED'" type="success" @click="handleStart">开始处理</el-button>
            <el-button v-if="task.status === 'IN_PROCESS'" type="warning" @click="handleComplete" :loading="submittingReport">提交报告</el-button>
          </div>
        </div>

        <div class="detail-section">
          <h3>项目信息</h3>
          <div class="info-grid">
            <div class="info-item">
              <span class="info-label">项目名称</span>
              <span class="info-value">{{ task.itemName }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">项目编码</span>
              <span class="info-value">{{ task.itemCode }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">项目类别</span>
              <span class="info-value">{{ task.itemCategoryLabel }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">价格</span>
              <span class="info-value">¥{{ task.price }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">紧急程度</span>
              <span class="info-value">{{ task.urgencyLabel }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">当前状态</span>
              <span class="info-value">{{ task.statusLabel }}</span>
            </div>
          </div>
        </div>

        <div class="detail-section">
          <h3>患者信息</h3>
          <div class="info-grid">
            <div class="info-item">
              <span class="info-label">患者姓名</span>
              <span class="info-value">{{ task.patientName }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">性别</span>
              <span class="info-value">{{ task.genderLabel }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">年龄</span>
              <span class="info-value">{{ task.age }}岁</span>
            </div>
            <div class="info-item">
              <span class="info-label">挂号ID</span>
              <span class="info-value">{{ task.registerId }}</span>
            </div>
          </div>
        </div>

        <div class="detail-section" v-if="task.clinicalSummary">
          <h3>临床摘要</h3>
          <p class="clinical-text">{{ task.clinicalSummary }}</p>
        </div>

        <div class="detail-section report-section" v-if="task.status === 'IN_PROCESS'">
          <h3>报告填写</h3>
          <el-form label-position="top">
            <el-form-item label="检查/检验描述">
              <el-input v-model="reportForm.resultSummary" type="textarea" :rows="4" placeholder="填写检查所见、检验结果摘要或关键指标" />
            </el-form-item>
            <el-form-item label="报告结论">
              <el-input v-model="reportForm.conclusion" type="textarea" :rows="3" placeholder="填写报告结论，发布后接诊医生可直接查看" />
            </el-form-item>
            <el-form-item label="异常标记">
              <el-select v-model="reportForm.abnormalFlag" style="width: 180px">
                <el-option label="正常" value="NORMAL" />
                <el-option label="异常" value="ABNORMAL" />
                <el-option label="危急" value="CRITICAL" />
                <el-option label="偏高" value="HIGH" />
                <el-option label="偏低" value="LOW" />
              </el-select>
            </el-form-item>
            <el-form-item label="附件地址">
              <el-input v-model="reportForm.attachmentUrl" placeholder="可选，填写影像或报告附件地址" />
            </el-form-item>
          </el-form>
        </div>

        <div class="detail-section">
          <h3>时间信息</h3>
          <div class="info-grid">
            <div class="info-item">
              <span class="info-label">创建时间</span>
              <span class="info-value">{{ task.createTime }}</span>
            </div>
            <div class="info-item" v-if="task.assignTime">
              <span class="info-label">分配时间</span>
              <span class="info-value">{{ task.assignTime }}</span>
            </div>
            <div class="info-item" v-if="task.completeTime">
              <span class="info-label">完成时间</span>
              <span class="info-value">{{ task.completeTime }}</span>
            </div>
          </div>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getTaskDetail, startTask, submitTaskReport } from '@/api/doctor/task'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'

const route = useRoute()
const task = ref<any>(null)
const loading = ref(false)
const submittingReport = ref(false)
const orderItemId = route.params.id as string
const reportForm = ref({
  resultSummary: '',
  conclusion: '',
  abnormalFlag: 'NORMAL',
  attachmentUrl: ''
})

onMounted(() => fetchDetail())

async function fetchDetail() {
  loading.value = true
  try {
    const res = await getTaskDetail(orderItemId)
    task.value = res.data
  } catch {
    ElMessage.error('加载任务详情失败')
  } finally {
    loading.value = false
  }
}

async function handleStart() {
  try {
    await ElMessageBox.confirm('确认开始处理此任务？', '提示', { type: 'info' })
    await startTask(orderItemId)
    ElMessage.success('已开始处理')
    fetchDetail()
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error(e?.response?.data?.msg || '操作失败')
  }
}

async function handleComplete() {
  if (!reportForm.value.resultSummary.trim() && !reportForm.value.conclusion.trim()) {
    ElMessage.warning('请先填写报告描述或结论')
    return
  }
  try {
    await ElMessageBox.confirm('确认发布报告并完成此任务？报告会回传给接诊医生。', '提示', { type: 'info' })
    submittingReport.value = true
    await submitTaskReport({ orderItemId, ...reportForm.value })
    ElMessage.success('报告已发布，任务已完成')
    fetchDetail()
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error(e?.response?.data?.msg || '操作失败')
  } finally {
    submittingReport.value = false
  }
}
</script>

<style scoped>
.page { padding: 24px; max-width: 900px; }
.page-top { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px; }
.page-top h2 { margin: 0; font-size: 20px; }
.card { background: #fff; border-radius: 8px; padding: 24px; min-height: 300px; }

.detail-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; padding-bottom: 16px; border-bottom: 1px solid #ebeef5; }
.detail-actions { display: flex; gap: 8px; }

.detail-section { margin-bottom: 24px; }
.detail-section h3 { font-size: 15px; margin: 0 0 12px; color: #303133; }

.info-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; }
.info-item { display: flex; flex-direction: column; }
.info-label { font-size: 12px; color: #999; margin-bottom: 4px; }
.info-value { font-size: 14px; color: #303133; }

.clinical-text { font-size: 14px; color: #606266; line-height: 1.8; background: #f5f7fa; padding: 12px; border-radius: 6px; }

.badge { display: inline-block; padding: 2px 8px; border-radius: 4px; font-size: 12px; }
.badge-EMERGENCY { background: #fef0f0; color: #f56c6c; }
.badge-URGENT { background: #fdf6ec; color: #e6a23c; }
.badge-NORMAL { background: #f0f9eb; color: #67c23a; }

.status-tag { display: inline-block; padding: 2px 8px; border-radius: 4px; font-size: 12px; }
.st-QUEUED { background: #ecf5ff; color: #409eff; }
.st-IN_PROCESS { background: #fdf6ec; color: #e6a23c; }
.st-COMPLETED { background: #f0f9eb; color: #67c23a; }
.st-WAITING_ASSIGN { background: #f4f4f5; color: #909399; }

.category-tag { display: inline-block; padding: 2px 8px; border-radius: 4px; font-size: 12px; background: #f4f4f5; color: #606266; }
</style>
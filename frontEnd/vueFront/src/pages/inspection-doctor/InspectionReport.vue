<template>
  <div class="report-page">
    <header class="page-header">
      <div>
        <h2>生成检验报告</h2>
        <p>核对检验申请，填写结果并回传给接诊医生</p>
      </div>
      <el-button @click="router.push('/inspection-doctor/workbench')">
        <el-icon><ArrowLeft /></el-icon>
        返回工作台
      </el-button>
    </header>

    <div v-loading="loading" class="report-shell">
      <el-empty v-if="!loading && !task" description="当前没有可填写报告的检验任务">
        <el-button type="primary" @click="router.push('/inspection-doctor/workbench')">
          查看检验工作台
        </el-button>
      </el-empty>

      <template v-else-if="task">
        <div v-if="task.itemCategory !== 'LAB'" class="state-notice state-error">
          当前任务不是检验项目，不能在此页面生成报告。
        </div>
        <div v-else-if="task.status !== 'IN_PROCESS'" class="state-notice">
          当前任务状态为"{{ task.statusLabel || task.status }}"，只有处理中的任务可以回传报告。
        </div>

        <section class="report-sheet">
          <div class="report-heading">
            <div>
              <span class="report-kicker">LABORATORY REPORT</span>
              <h3>{{ task.itemName || '检验报告' }}</h3>
              <p>申请单号：{{ task.orderItemId }}</p>
            </div>
            <div class="heading-tags">
              <el-tag type="primary">检验项目</el-tag>
              <el-tag :type="urgencyType(task.urgencyLevel)" effect="plain">
                {{ task.urgencyLabel || urgencyLabel(task.urgencyLevel) }}
              </el-tag>
            </div>
          </div>

          <div class="info-band">
            <div>
              <span>患者</span>
              <strong>{{ task.patientName || '-' }}</strong>
            </div>
            <div>
              <span>性别 / 年龄</span>
              <strong>{{ genderLabel(task.gender) }} / {{ task.age ?? '-' }}岁</strong>
            </div>
            <div>
              <span>挂号编号</span>
              <strong>{{ task.registerId || '-' }}</strong>
            </div>
            <div>
              <span>报告医生</span>
              <strong>{{ task.assignedDoctorName || '当前检验医生' }}</strong>
            </div>
          </div>

          <div class="request-section">
            <div class="section-label">申请信息</div>
            <div class="request-grid">
              <div><span>项目编码</span><strong>{{ task.itemCode || '-' }}</strong></div>
              <div><span>申请时间</span><strong>{{ formatTime(task.createTime) }}</strong></div>
              <div><span>分配时间</span><strong>{{ formatTime(task.assignTime) }}</strong></div>
              <div class="summary-item">
                <span>临床摘要</span>
                <strong>{{ task.clinicalSummary || '暂无临床摘要' }}</strong>
              </div>
            </div>
          </div>

          <el-form label-position="top" class="report-form">
            <div class="section-label">检验结果</div>
            <el-form-item label="检验结果摘要" required>
              <el-input
                v-model="form.resultSummary"
                type="textarea"
                :rows="5"
                maxlength="10000"
                show-word-limit
                placeholder="填写检验数据、主要发现及参考范围"
                :disabled="!canSubmit"
              />
            </el-form-item>

            <!-- AI生成检验结论按钮区域 -->
            <div class="ai-assist-section">
              <el-button
                type="primary"
                plain
                :loading="aiGenerating"
                :disabled="!canSubmit || !form.resultSummary.trim()"
                @click="generateConclusion"
              >
                <el-icon><MagicStick /></el-icon>
                AI 生成检验结论
              </el-button>
              <span class="ai-hint" v-if="form.resultSummary.trim()">
                基于检验结果摘要自动生成专业结论
              </span>
              <span class="ai-hint ai-hint-warning" v-else>
                请先填写检验结果摘要
              </span>
            </div>

            <el-form-item label="检验结论" required>
              <el-input
                v-model="form.conclusion"
                type="textarea"
                :rows="4"
                maxlength="10000"
                show-word-limit
                placeholder="填写检验结论"
                :disabled="!canSubmit"
              />
            </el-form-item>
            <div class="form-row">
              <el-form-item label="异常标记" required>
                <el-select v-model="form.abnormalFlag" :disabled="!canSubmit">
                  <el-option label="正常" value="NORMAL" />
                  <el-option label="异常" value="ABNORMAL" />
                  <el-option label="危急值" value="CRITICAL" />
                </el-select>
              </el-form-item>
              <el-form-item label="报告附件地址">
                <el-input
                  v-model="form.attachmentUrl"
                  maxlength="255"
                  placeholder="可选，填写已上传报告附件地址"
                  :disabled="!canSubmit"
                />
              </el-form-item>
            </div>
          </el-form>

          <footer class="report-actions">
            <el-button :disabled="!canSubmit" @click="resetForm">清空重写</el-button>
            <el-button type="primary" :loading="submitting" :disabled="!canSubmit" @click="submitReport">
              <el-icon><DocumentChecked /></el-icon>
              提交并回传报告
            </el-button>
          </footer>
        </section>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, DocumentChecked, MagicStick } from '@element-plus/icons-vue'
import { getTaskDetail, getWorkbench, submitTaskReport } from '@/api/doctor/task'
import { generateReportConclusion } from '@/api/inspection-doctor'

interface LaboratoryTask {
  orderItemId: string
  itemCode?: string
  itemName?: string
  itemCategory?: string
  urgencyLevel?: string
  urgencyLabel?: string
  status?: string
  statusLabel?: string
  createTime?: string
  assignTime?: string
  patientName?: string
  gender?: number
  age?: number
  registerId?: string
  assignedDoctorName?: string
  clinicalSummary?: string
}

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const submitting = ref(false)
const aiGenerating = ref(false)
const task = ref<LaboratoryTask | null>(null)
const form = ref({
  resultSummary: '',
  conclusion: '',
  abnormalFlag: 'NORMAL',
  attachmentUrl: '',
})

const canSubmit = computed(() =>
  task.value?.itemCategory === 'LAB' && task.value?.status === 'IN_PROCESS'
)

onMounted(loadReportTask)

async function loadReportTask() {
  loading.value = true
  try {
    const queryId = typeof route.query.orderItemId === 'string' ? route.query.orderItemId : ''
    const storedId = sessionStorage.getItem('current_order_item_id') || ''

    if (queryId) {
      await fetchTaskDetail(queryId)
      return
    }

    if (storedId) {
      try {
        await fetchTaskDetail(storedId)
        if (canSubmit.value) return
      } catch {
        task.value = null
      }
      sessionStorage.removeItem('current_order_item_id')
      task.value = null
    }

    const response = await getWorkbench()
    const activeTask = (response.data || []).find(
      (item: LaboratoryTask) => item.itemCategory === 'LAB' && item.status === 'IN_PROCESS'
    )
    if (activeTask?.orderItemId) await fetchTaskDetail(activeTask.orderItemId)
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.msg || '加载检验任务失败')
  } finally {
    loading.value = false
  }
}

async function fetchTaskDetail(orderItemId: string) {
  const response = await getTaskDetail(orderItemId)
  task.value = response.data || null
  if (task.value?.itemCategory === 'LAB' && task.value?.status === 'IN_PROCESS') {
    sessionStorage.setItem('current_order_item_id', orderItemId)
  }
}

function resetForm() {
  form.value = {
    resultSummary: '',
    conclusion: '',
    abnormalFlag: 'NORMAL',
    attachmentUrl: '',
  }
}

async function generateConclusion() {
  if (!form.value.resultSummary.trim()) {
    ElMessage.warning('请先填写检验结果摘要')
    return
  }

  aiGenerating.value = true
  try {
    const response = await generateReportConclusion({
      resultSummary: form.value.resultSummary.trim()
    })
    if (response.code === 200 && response.data?.conclusion) {
      form.value.conclusion = response.data.conclusion
      ElMessage.success(response.message || 'AI 检验结论生成成功')
    } else if (response.code !== 200) {
      ElMessage.warning(response.message || 'AI 生成结论失败，请手动填写')
    } else {
      ElMessage.warning('AI 未能生成有效结论，请手动填写')
    }
  } catch (error: any) {
    const errorMsg = error?.response?.data?.message || error?.message || 'AI 生成检验结论失败，请手动填写'
    ElMessage.error(errorMsg)
  } finally {
    aiGenerating.value = false
  }
}

async function submitReport() {
  if (!canSubmit.value || !task.value) return
  if (!form.value.resultSummary.trim() || !form.value.conclusion.trim()) {
    ElMessage.warning('请填写检验结果摘要和检验结论')
    return
  }

  try {
    await ElMessageBox.confirm(
      '报告提交后将直接回传给接诊医生，并完成当前检验任务。确认提交？',
      '提交检验报告',
      { type: 'warning', confirmButtonText: '确认提交', cancelButtonText: '继续编辑' }
    )
    submitting.value = true
    await submitTaskReport({
      orderItemId: task.value.orderItemId,
      resultSummary: form.value.resultSummary.trim(),
      conclusion: form.value.conclusion.trim(),
      abnormalFlag: form.value.abnormalFlag,
      attachmentUrl: form.value.attachmentUrl.trim() || undefined,
    })
    sessionStorage.removeItem('current_order_item_id')
    ElMessage.success('检验报告已回传，任务已完成')
    await router.replace('/inspection-doctor/workbench')
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error?.response?.data?.msg || '检验报告回传失败')
    }
  } finally {
    submitting.value = false
  }
}

function urgencyType(level?: string) {
  return ({ EMERGENCY: 'danger', URGENT: 'warning', NORMAL: 'success' } as const)[level as 'EMERGENCY' | 'URGENT' | 'NORMAL'] || 'info'
}

function urgencyLabel(level?: string) {
  return { EMERGENCY: '紧急', URGENT: '加急', NORMAL: '常规' }[level || ''] || level || '-'
}

function genderLabel(gender?: number) {
  if (gender === 1) return '男'
  if (gender === 0 || gender === 2) return '女'
  return '未知'
}

function formatTime(value?: string) {
  if (!value) return '-'
  return value.replace('T', ' ')
}
</script>

<style scoped>
.report-page { min-height: 100%; padding: 24px; background: #f5f7fa; }
.page-header { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; max-width: 1180px; margin: 0 auto 20px; padding: 0 4px; }
.page-header h2 { margin: 0 0 6px; color: #111827; font-size: 24px; font-weight: 700; }
.page-header p { margin: 0; color: #6b7280; font-size: 13px; }
.page-header .el-icon,
.report-actions .el-icon { margin-right: 6px; }
.report-shell { min-height: 420px; max-width: 1180px; margin: 0 auto; }
.report-sheet { overflow: hidden; border: 0; border-radius: 14px; background: #fff; box-shadow: 0 1px 3px rgba(0,0,0,.06); }
.report-heading { display: flex; align-items: flex-start; justify-content: space-between; gap: 20px; padding: 24px 28px 20px; border-bottom: 2px solid #e5e7eb; }
.report-kicker { color: #409eff; font-size: 11px; font-weight: 700; letter-spacing: 1.4px; }
.report-heading h3 { margin: 8px 0 6px; color: #111827; font-size: 22px; }
.report-heading p { margin: 0; color: #8791a2; font-size: 12px; font-family: monospace; }
.heading-tags { display: flex; gap: 8px; flex-wrap: wrap; }
.info-band { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); background: #f9fafb; border-bottom: 1px solid #e5e7eb; }
.info-band > div { min-width: 0; padding: 16px 20px; border-right: 1px solid #e5e7eb; }
.info-band > div:last-child { border-right: 0; }
.info-band span,
.request-grid span { display: block; margin-bottom: 5px; color: #7b8798; font-size: 12px; }
.info-band strong,
.request-grid strong { display: block; overflow-wrap: anywhere; color: #263244; font-size: 14px; font-weight: 600; }
.request-section,
.report-form { padding: 24px 32px; }
.request-section { border-bottom: 1px solid #edf0f4; }
.section-label { margin-bottom: 16px; color: #1f2937; font-size: 15px; font-weight: 600; }
.request-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 18px 24px; }
.summary-item { grid-column: 1 / -1; padding: 14px 16px; border-left: 3px solid #409eff; background: #f9fafb; }
.summary-item strong { line-height: 1.7; font-weight: 500; }
.form-row { display: grid; grid-template-columns: 220px minmax(0, 1fr); gap: 20px; }
.form-row :deep(.el-select) { width: 100%; }
.report-actions { display: flex; justify-content: flex-end; gap: 10px; padding: 20px 32px 24px; border-top: 1px solid #e5e7eb; background: #fff; }
.state-notice { margin-bottom: 14px; padding: 12px 16px; border: 1px solid #f2cf91; border-radius: 6px; background: #fff8e8; color: #8a5b12; font-size: 13px; }
.state-error { border-color: #efb4b4; background: #fff1f1; color: #a73535; }

/* AI 助手样式 */
.ai-assist-section {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 0 16px 0;
  border-bottom: 1px dashed #e5e7eb;
  margin-bottom: 18px;
  flex-wrap: wrap;
}

.ai-assist-section .el-button {
  min-width: 160px;
}

.ai-hint {
  color: #6b7280;
  font-size: 13px;
}

.ai-hint-warning {
  color: #f59e0b;
}

@media (max-width: 760px) {
  .report-page { padding: 16px; }
  .page-header { flex-direction: column; }
  .report-heading { flex-direction: column; padding: 22px 20px 18px; }
  .info-band { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .info-band > div:nth-child(2) { border-right: 0; }
  .info-band > div:nth-child(-n + 2) { border-bottom: 1px solid #e5e7eb; }
  .request-section,
  .report-form { padding: 20px; }
  .request-grid,
  .form-row { grid-template-columns: 1fr; }
  .report-actions { flex-direction: column-reverse; padding: 16px 20px 20px; }
  .report-actions .el-button { width: 100%; margin-left: 0; }
  
  .ai-assist-section {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .ai-assist-section .el-button {
    width: 100%;
  }
}
</style>
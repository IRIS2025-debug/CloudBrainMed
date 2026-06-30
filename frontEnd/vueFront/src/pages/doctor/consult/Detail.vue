<template>
  <div class="page">
    <div class="detail-head">
      <el-button text @click="$router.back()"><el-icon><ArrowLeft /></el-icon> 返回列表</el-button>
      <span class="detail-id">挂号 #{{ registerId }}</span>
    </div>

    <div class="workspace-grid">
      <main class="main-flow">
        <section class="card">
          <div class="card-head">患者信息</div>
          <div class="patient-brief">
            <div class="pb-avatar">{{ patientName.charAt(0) || '?' }}</div>
            <div>
              <div class="pb-name">{{ patientName }}</div>
              <div class="pb-meta">{{ genderLabel(detail.gender) }} · {{ detail.patientAge || '--' }}岁 · {{ detail.department || '--' }}</div>
            </div>
            <span class="pb-status" :class="'st-' + detail.consultStatus">{{ statusLabel(detail.consultStatus) }}</span>
          </div>
          <div class="info-row">
            <div class="info-item"><span class="ii-label">主诉</span><span>{{ detail.chiefComplaint || '--' }}</span></div>
            <div class="info-item"><span class="ii-label">就诊时间</span><span>{{ detail.visitDate || '--' }} {{ detail.consultTime || '' }}</span></div>
            <div class="info-item"><span class="ii-label">接诊房间</span><span>{{ detail.consultRoom || '--' }}</span></div>
          </div>
        </section>

        <section class="card">
          <div class="card-head">病历编辑</div>
          <el-input
            v-model="recordDesc"
            type="textarea"
            :rows="10"
            :disabled="isCompleted"
            placeholder="请输入病史、体格检查、诊断意见和处理计划"
            class="editor"
          />
          <div class="actions">
            <el-button @click="handleSaveDraft" :loading="saving" :disabled="isCompleted" size="large">暂存草稿</el-button>
            <el-button type="success" @click="handleConfirm" :loading="confirming" :disabled="isCompleted" size="large">确认病历</el-button>
            <el-button type="warning" @click="handleCreateExam" :disabled="isCompleted" size="large">开具检查单</el-button>
            <el-button type="success" @click="handleCreatePrescription" :disabled="isCompleted" size="large">开具处方</el-button>
            <el-button type="danger" @click="handleComplete" :loading="completing" :disabled="isCompleted" size="large" plain>完成接诊</el-button>
          </div>
        </section>

        <section class="card" v-if="showExamDialog">
          <div class="card-head">检查申请单</div>
          <el-form label-position="top">
            <div class="exam-order-toolbar">
              <el-radio-group v-model="urgencyLevel">
                <el-radio-button value="NORMAL">常规</el-radio-button>
                <el-radio-button value="URGENT">加急</el-radio-button>
                <el-radio-button value="EMERGENCY">紧急</el-radio-button>
              </el-radio-group>
              <el-button @click="addExamItem" :disabled="isCompleted">添加项目</el-button>
            </div>

            <div class="exam-order-table">
              <div class="exam-order-head">
                <span>检查/检验项目</span>
                <span>执行科室</span>
                <span></span>
              </div>
              <div class="exam-order-row" v-for="(item, index) in examItems" :key="item.id">
                <el-input v-model="item.itemName" placeholder="例如：颅脑CT平扫" />
                <el-input v-model="item.dept" placeholder="例如：影像科" />
                <el-button text type="danger" @click="removeExamItem(index)" :disabled="isCompleted || examItems.length === 1">删除</el-button>
              </div>
            </div>

            <div class="exam-order-actions">
              <el-button @click="showExamDialog = false">取消</el-button>
              <el-button type="primary" @click="submitExamOrder" :loading="examLoading" :disabled="isCompleted">提交申请</el-button>
            </div>
          </el-form>
        </section>

        <section class="card" v-if="showPrescriptionDialog">
          <div class="card-head">开具处方</div>
          <el-form label-position="top">
            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item label="药品名称"><el-input v-model="rxForm.medicineName" placeholder="例如：布洛芬缓释胶囊" /></el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="规格"><el-input v-model="rxForm.spec" placeholder="例如：0.3g×20粒" /></el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item label="用法用量"><el-input v-model="rxForm.usage" placeholder="例如：口服，每次1粒，每日2次" /></el-form-item>
              </el-col>
              <el-col :span="6">
                <el-form-item label="数量"><el-input-number v-model="rxForm.num" :min="1" style="width:100%" /></el-form-item>
              </el-col>
              <el-col :span="6">
                <el-form-item label="单价"><el-input-number v-model="rxForm.price" :min="0" :precision="2" style="width:100%" /></el-form-item>
              </el-col>
            </el-row>
            <el-button type="primary" @click="submitPrescription" :loading="rxLoading" :disabled="isCompleted">提交处方</el-button>
          </el-form>
        </section>
      </main>

      <aside class="ai-sidebar">
        <div class="ai-head">
          <div class="ai-badge"><el-icon><MagicStick /></el-icon></div>
          <div>
            <div class="ai-title">AI 辅助医生</div>
            <div class="ai-subtitle">基于接诊上下文生成参考建议</div>
          </div>
        </div>

        <div class="ai-tabs">
          <button :class="{ active: aiMode === 'reception' }" @click="aiMode = 'reception'">辅助接诊</button>
          <button :class="{ active: aiMode === 'report' }" @click="aiMode = 'report'">报告分析</button>
        </div>

        <template v-if="aiMode === 'reception'">
          <div v-if="!aiResponse" class="ai-empty">
            <div class="empty-title">等待分析</div>
            <p>点击下方按钮后，AI 会读取当前挂号的主诉和病历上下文，给出接诊追问、诊断依据和风险提醒。</p>
          </div>

          <div v-else class="ai-result">
            <div class="result-card primary">
              <div class="result-line">
                <span>AI 建议</span>
                <el-tag :type="aiResponse.fallback ? 'warning' : 'success'" round>{{ aiResponse.status || 'SUCCESS' }}</el-tag>
              </div>
              <p class="ai-answer">{{ aiResponse.answer || 'AI 已生成接诊建议' }}</p>
            </div>
          </div>

          <div class="ai-actions-panel">
            <el-button type="primary" @click="handleAiAnalyze" :loading="aiLoading" :disabled="isCompleted" class="wide-btn">
              <el-icon><MagicStick /></el-icon> {{ aiResponse ? '重新分析' : 'AI 辅助接诊' }}
            </el-button>
          </div>
        </template>

        <template v-else>
          <div class="report-form">
            <el-select v-model="reportForm.reportType" class="report-type">
              <el-option label="检验报告" value="LAB" />
              <el-option label="检查报告" value="EXAM" />
            </el-select>
            <el-input
              v-model="reportForm.reportText"
              type="textarea"
              :rows="7"
              placeholder="粘贴检查/检验报告原文，或输入关键指标、影像描述和结论"
            />
            <el-button type="primary" class="wide-btn" :loading="reportLoading" :disabled="isCompleted" @click="handleReportAnalyze">
              分析报告
            </el-button>
          </div>

          <div v-if="reportResult" class="report-result">
            <div class="result-card primary">
              <div class="result-line">
                <span>报告结论</span>
                <el-tag :type="reportRiskTag" round>{{ reportRiskLabel }}</el-tag>
              </div>
              <p class="ai-answer">{{ reportResult.summary || 'AI 已生成报告分析结果' }}</p>
            </div>

            <div class="ai-section" v-if="reportResult.abnormalIndicators?.length">
              <div class="ai-section-title">异常指标</div>
              <div class="exam-item" v-for="(item, index) in reportResult.abnormalIndicators" :key="index">
                <span class="diag-index">{{ index + 1 }}</span>
                <div>
                  <strong>{{ item.name || '异常项' }}</strong>
                  <p>{{ item.value || '--' }} · 参考 {{ item.referenceRange || '--' }}</p>
                  <p>{{ item.interpretation || '请结合临床判断' }}</p>
                </div>
              </div>
            </div>

            <div class="ai-section" v-if="reportResult.suggestions?.length">
              <div class="ai-section-title">处理建议</div>
              <div class="suggestion-item" v-for="(item, index) in reportResult.suggestions" :key="index">
                <span class="diag-index">{{ index + 1 }}</span>
                <span>{{ item }}</span>
              </div>
            </div>

            <div class="ai-warning" v-if="reportResult.followUpAdvice">
              {{ reportResult.followUpAdvice }}
            </div>

            <el-button class="wide-btn report-apply-btn" type="success" plain :loading="savingReportAnalysis" :disabled="isCompleted" @click="appendReportAnalysisToRecord">
              写入并暂存病历
            </el-button>
          </div>
        </template>
      </aside>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, MagicStick } from '@element-plus/icons-vue'
import { getConsultDetail, saveDraft, confirmRecord, createExamOrder, completeConsult, createPrescription } from '@/api/doctor/consult'
import { assistantChat, analyzeReport, type AiAssistantChatResponse, type ReportAnalysisResponse } from '@/api/doctor/ai'

const route = useRoute()
const registerId = route.params.registerId as string
const detail = ref<any>({})
const recordDesc = ref('')
const saving = ref(false)
const confirming = ref(false)
const completing = ref(false)
const examLoading = ref(false)
const showExamDialog = ref(false)
const urgencyLevel = ref('NORMAL')
const examItems = ref([{ id: Date.now(), itemName: '', dept: '' }])
const aiMode = ref<'reception' | 'report'>('reception')
const aiLoading = ref(false)
const aiResponse = ref<AiAssistantChatResponse | null>(null)
const reportLoading = ref(false)
const savingReportAnalysis = ref(false)
const reportForm = ref({ reportType: 'LAB', reportText: '' })
const reportResult = ref<ReportAnalysisResponse | null>(null)
const showPrescriptionDialog = ref(false)
const rxLoading = ref(false)
const rxForm = ref({ medicineName: '', spec: '', usage: '', num: 1, price: 0 })

const patientName = computed(() => detail.value.name || detail.value.patientName || '--')
const isCompleted = computed(() => detail.value.consultStatus === 'COMPLETED')
const reportRiskLabel = computed(() => {
  const risk = reportResult.value?.riskLevel
  if (risk === 'HIGH') return '高风险'
  if (risk === 'MEDIUM') return '中风险'
  if (risk === 'LOW') return '低风险'
  return '参考'
})
const reportRiskTag = computed(() => {
  const risk = reportResult.value?.riskLevel
  if (risk === 'HIGH') return 'danger'
  if (risk === 'MEDIUM') return 'warning'
  return 'success'
})

onMounted(async () => {
  try {
    const res = await getConsultDetail(registerId)
    detail.value = res.data
    recordDesc.value = res.data.description || ''
  } catch (e: any) {
    showActionError(e, '加载接诊详情失败')
  }
})

async function handleSaveDraft() {
  if (isCompleted.value) {
    ElMessage.warning('接诊已完成，不能继续修改病历')
    return
  }
  saving.value = true
  try {
    await saveDraft({ registerId, recordDesc: recordDesc.value })
    ElMessage.success('草稿已保存')
  } catch (e: any) {
    showActionError(e, '草稿保存失败')
  } finally {
    saving.value = false
  }
}

async function handleConfirm() {
  if (isCompleted.value) {
    ElMessage.warning('接诊已完成，不能继续确认病历')
    return
  }
  confirming.value = true
  try {
    await confirmRecord({ registerId, recordDesc: recordDesc.value })
    ElMessage.success('病历已确认')
    detail.value.consultStatus = 'RECORD_CONFIRMED'
  } catch (e: any) {
    showActionError(e, '病历确认失败')
  } finally {
    confirming.value = false
  }
}

function handleCreateExam() {
  if (isCompleted.value) {
    ElMessage.warning('接诊已完成，不能继续开具检查单')
    return
  }
  showExamDialog.value = true
  showPrescriptionDialog.value = false
  if (!examItems.value.length) addExamItem()
}

function handleCreatePrescription() {
  if (isCompleted.value) {
    ElMessage.warning('接诊已完成，不能继续开具处方')
    return
  }
  showPrescriptionDialog.value = true
  showExamDialog.value = false
}

async function submitPrescription() {
  if (isCompleted.value) {
    ElMessage.warning('接诊已完成，不能继续开具处方')
    return
  }
  if (!rxForm.value.medicineName.trim()) {
    ElMessage.warning('请输入药品名称')
    return
  }
  rxLoading.value = true
  try {
    await createPrescription({ ...rxForm.value, registerId })
    ElMessage.success('处方已开具')
    showPrescriptionDialog.value = false
    rxForm.value = { medicineName: '', spec: '', usage: '', num: 1, price: 0 }
  } catch (e: any) {
    showActionError(e, '处方提交失败')
  } finally {
    rxLoading.value = false
  }
}

function addExamItem() {
  if (isCompleted.value) return
  examItems.value.push({ id: Date.now() + Math.random(), itemName: '', dept: '' })
}

function removeExamItem(index: number) {
  if (examItems.value.length === 1) return
  examItems.value.splice(index, 1)
}

async function submitExamOrder() {
  if (isCompleted.value) {
    ElMessage.warning('接诊已完成，不能继续开具检查单')
    return
  }
  const items = examItems.value
    .map(item => ({ itemName: item.itemName.trim(), dept: item.dept.trim() }))
    .filter(item => item.itemName)
  if (!items.length) {
    ElMessage.warning('请至少填写一个检查/检验项目')
    return
  }
  examLoading.value = true
  try {
    await createExamOrder({ registerId, checkItemList: JSON.stringify(items), urgencyLevel: urgencyLevel.value })
    ElMessage.success('检查申请已生成')
    showExamDialog.value = false
    examItems.value = [{ id: Date.now(), itemName: '', dept: '' }]
  } catch (e: any) {
    showActionError(e, '检查申请提交失败')
  } finally {
    examLoading.value = false
  }
}

async function handleComplete() {
  if (isCompleted.value) return
  try {
    await ElMessageBox.confirm('确定完成本次接诊？完成后不建议继续修改病历。', '确认完成', { type: 'warning' })
  } catch {
    return
  }
  completing.value = true
  try {
    await completeConsult(registerId)
    ElMessage.success('接诊已完成')
    detail.value.consultStatus = 'COMPLETED'
  } catch (e: any) {
    showActionError(e, '完成接诊失败')
  } finally {
    completing.value = false
  }
}

async function handleAiAnalyze() {
  if (isCompleted.value) {
    ElMessage.warning('接诊已完成，不能继续使用 AI 辅助接诊')
    return
  }
  aiLoading.value = true
  try {
    const res = await assistantChat({
      registerId,
      actionType: 'DIAGNOSIS_ASSISTANT',
      message: '请基于当前接诊信息给出辅助接诊建议、诊断依据、需要补充追问的问题和需要排除的风险。',
      currentRecordDesc: recordDesc.value
    })
    aiResponse.value = res.data
    ElMessage.success('AI 分析已完成')
  } catch (e: any) {
    showActionError(e, 'AI 辅助接诊失败')
  } finally {
    aiLoading.value = false
  }
}

async function handleReportAnalyze() {
  if (isCompleted.value) {
    ElMessage.warning('接诊已完成，不能继续分析报告')
    return
  }
  if (!reportForm.value.reportText.trim()) {
    ElMessage.warning('请先输入报告文本')
    return
  }
  reportLoading.value = true
  try {
    const res = await analyzeReport({
      registerId,
      reportType: reportForm.value.reportType,
      reportText: reportForm.value.reportText
    })
    reportResult.value = res.data
    ElMessage.success('报告分析已完成')
  } catch (e: any) {
    showActionError(e, '报告分析失败')
  } finally {
    reportLoading.value = false
  }
}

async function appendReportAnalysisToRecord() {
  if (isCompleted.value) {
    ElMessage.warning('接诊已完成，不能继续修改病历')
    return
  }
  if (!reportResult.value) return
  const section = buildReportAnalysisRecord()
  if (recordDesc.value.includes(section)) {
    ElMessage.info('报告分析已在病历草稿中')
    return
  }
  recordDesc.value = [recordDesc.value.trim(), section].filter(Boolean).join('\n\n')
  savingReportAnalysis.value = true
  try {
    await saveDraft({ registerId, recordDesc: recordDesc.value })
    ElMessage.success('报告分析已写入并暂存')
  } catch (e: any) {
    showActionError(e, '已写入本地草稿，暂存失败')
  } finally {
    savingReportAnalysis.value = false
  }
}

function showActionError(error: any, fallbackMessage: string) {
  ElMessage.error(error?.message || fallbackMessage)
}

function buildReportAnalysisRecord() {
  const result = reportResult.value
  if (!result) return ''
  const lines = [
    '【AI报告分析】',
    `报告类型：${reportForm.value.reportType === 'LAB' ? '检验报告' : '检查报告'}`,
    `风险等级：${reportRiskLabel.value}`,
    `结论摘要：${result.summary || '未生成摘要'}`,
  ]
  if (result.abnormalIndicators?.length) {
    lines.push('异常指标：')
    result.abnormalIndicators.forEach((item, index) => {
      lines.push(`${index + 1}. ${item.name || '异常项'}：${item.value || '--'}，参考范围：${item.referenceRange || '--'}，解释：${item.interpretation || '--'}`)
    })
  }
  if (result.suggestions?.length) {
    lines.push('处理建议：')
    result.suggestions.forEach((item, index) => lines.push(`${index + 1}. ${item}`))
  }
  if (result.followUpAdvice) {
    lines.push(`随访/复查建议：${result.followUpAdvice}`)
  }
  return lines.join('\n')
}

function genderLabel(gender?: number) {
  if (gender === 1) return '男'
  if (gender === 2) return '女'
  return '未知'
}

function statusLabel(status: string) {
  const labels: Record<string, string> = {
    PENDING: '待接诊',
    IN_PROGRESS: '接诊中',
    RECORD_CONFIRMED: '病历已确认',
    COMPLETED: '已完成'
  }
  return labels[status] || status || '未知'
}
</script>

<style scoped>
.page { padding: 24px 28px 32px; background: #f6f8fb; min-height: calc(100vh - 64px); }
.detail-head { display: flex; align-items: center; gap: 16px; margin-bottom: 18px; }
.detail-id { font-size: 14px; color: #64748b; font-weight: 600; }
.workspace-grid { display: grid; grid-template-columns: minmax(0, 1fr) 360px; gap: 20px; align-items: start; }
.main-flow { display: flex; flex-direction: column; gap: 18px; }
.card { background: #fff; border: 1px solid #e6edf5; border-radius: 8px; padding: 22px; box-shadow: 0 12px 28px rgba(15, 23, 42, .06); }
.card-head { font-size: 15px; font-weight: 700; color: #102033; margin-bottom: 18px; padding-bottom: 12px; border-bottom: 1px solid #edf2f7; }
.patient-brief { display: flex; align-items: center; gap: 14px; margin-bottom: 18px; }
.pb-avatar { width: 48px; height: 48px; border-radius: 50%; background: linear-gradient(135deg, #dbeafe, #ccfbf1); color: #0f766e; font-size: 18px; font-weight: 700; display: flex; align-items: center; justify-content: center; }
.pb-name { font-size: 17px; font-weight: 700; color: #102033; }
.pb-meta { font-size: 13px; color: #64748b; margin-top: 2px; }
.pb-status { margin-left: auto; font-size: 12px; font-weight: 700; padding: 5px 12px; border-radius: 999px; }
.st-PENDING { background: #fef3c7; color: #92400e; }
.st-IN_PROGRESS { background: #dbeafe; color: #1d4ed8; }
.st-RECORD_CONFIRMED { background: #dcfce7; color: #166534; }
.st-COMPLETED { background: #e2e8f0; color: #475569; }
.info-row { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 14px; }
.info-item { display: flex; flex-direction: column; gap: 5px; font-size: 14px; color: #334155; min-width: 0; }
.ii-label { font-size: 12px; color: #64748b; font-weight: 700; }
.editor { margin-bottom: 16px; }
.actions { display: flex; gap: 10px; flex-wrap: wrap; }
.exam-order-toolbar { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-bottom: 14px; }
.exam-order-table { border: 1px solid #edf2f7; border-radius: 8px; overflow: hidden; }
.exam-order-head,
.exam-order-row { display: grid; grid-template-columns: minmax(220px, 1.4fr) minmax(160px, 1fr) 72px; gap: 12px; align-items: center; padding: 12px 14px; }
.exam-order-head { background: #f8fafc; color: #64748b; font-size: 12px; font-weight: 800; }
.exam-order-row + .exam-order-row { border-top: 1px solid #edf2f7; }
.exam-order-actions { display: flex; justify-content: flex-end; gap: 10px; margin-top: 14px; }
.ai-sidebar { position: sticky; top: 18px; background: #fff; border: 1px solid #c8f3df; border-radius: 8px; overflow: hidden; box-shadow: 0 16px 38px rgba(15, 118, 110, .12); }
.ai-head { display: flex; align-items: center; gap: 12px; padding: 18px 20px; color: #fff; background: linear-gradient(135deg, #087f5b, #0f9f8f); }
.ai-badge { width: 38px; height: 38px; border-radius: 50%; background: rgba(255,255,255,.16); display: flex; align-items: center; justify-content: center; }
.ai-title { font-size: 17px; font-weight: 800; }
.ai-subtitle { font-size: 12px; opacity: .86; margin-top: 2px; }
.ai-tabs { display: grid; grid-template-columns: 1fr 1fr; padding: 12px; gap: 8px; background: #f8fafc; border-bottom: 1px solid #edf2f7; }
.ai-tabs button { border: 0; border-radius: 8px; padding: 9px 8px; background: transparent; color: #64748b; font-weight: 700; cursor: pointer; }
.ai-tabs button.active { background: #0f766e; color: #fff; }
.ai-empty { margin: 18px; padding: 18px; border: 1px solid #dbeafe; border-radius: 8px; background: #f8fbff; color: #475569; line-height: 1.7; }
.empty-title { font-size: 15px; font-weight: 800; color: #102033; margin-bottom: 6px; }
.ai-result { padding: 18px; }
.result-card { border: 1px solid #dbeafe; border-radius: 8px; background: #f8fbff; padding: 14px; margin-bottom: 16px; }
.result-card.primary { border-color: #bfdbfe; }
.result-line { display: flex; align-items: center; justify-content: space-between; gap: 10px; margin-bottom: 8px; font-size: 13px; font-weight: 800; color: #2563eb; }
.result-card p, .ai-empty p { margin: 0; }
.ai-answer { white-space: pre-wrap; color: #334155; line-height: 1.7; font-size: 13px; }
.ai-section { margin-top: 16px; }
.ai-section-title { font-size: 14px; font-weight: 800; color: #102033; margin-bottom: 10px; }
.exam-item { display: grid; grid-template-columns: 28px minmax(0, 1fr); gap: 10px; align-items: start; padding: 10px 0; border-bottom: 1px solid #edf2f7; }
.exam-item:last-child { border-bottom: 0; }
.diag-index { width: 24px; height: 24px; border-radius: 6px; background: #f1f5f9; color: #475569; display: flex; align-items: center; justify-content: center; font-size: 12px; font-weight: 800; }
.exam-item strong { color: #102033; font-size: 13px; }
.exam-item p { margin: 4px 0 0; color: #64748b; font-size: 13px; line-height: 1.6; }
.ai-warning { margin-top: 16px; padding: 12px; border: 1px solid #fde68a; background: #fffdf3; color: #854d0e; border-radius: 8px; font-size: 13px; line-height: 1.6; }
.ai-actions-panel { padding: 0 18px 18px; display: flex; flex-direction: column; gap: 10px; }
.wide-btn { width: 100%; }
.report-form { padding: 18px; display: flex; flex-direction: column; gap: 12px; }
.report-type { width: 100%; }
.report-result { padding: 0 18px 18px; }
.report-apply-btn { margin-top: 14px; }
.suggestion-item { display: grid; grid-template-columns: 28px minmax(0, 1fr); gap: 10px; align-items: center; padding: 10px 0; border-bottom: 1px solid #edf2f7; color: #475569; font-size: 13px; line-height: 1.6; }
.suggestion-item:last-child { border-bottom: 0; }
@media (max-width: 1180px) {
  .workspace-grid { grid-template-columns: 1fr; }
  .ai-sidebar { position: static; }
}
@media (max-width: 760px) {
  .page { padding: 18px; }
  .info-row { grid-template-columns: 1fr; }
}
</style>

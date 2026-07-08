<template>
  <div class="task-detail-page">
    <!-- 顶部导航 -->
    <div class="page-header">
      <el-button class="back-btn" text @click="goBackToRequestList">
        <el-icon><ArrowLeft /></el-icon>
        <span>返回</span>
      </el-button>
      <div class="header-right">
        <span class="page-title">任务详情</span>
        <span class="order-id">#{{ orderItemId?.substring(0, 12) }}...</span>
      </div>
    </div>

    <div v-loading="loading" class="content-wrapper">
      <template v-if="task">
        <!-- 状态横幅 -->
        <div class="status-banner" :class="'banner-' + task.status">
          <div class="banner-left">
            <div class="banner-icon">
              <el-icon v-if="task.status === 'QUEUED'" :size="24"><Clock /></el-icon>
              <el-icon v-else-if="task.status === 'IN_PROCESS'" :size="24"><Loading /></el-icon>
              <el-icon v-else-if="task.status === 'COMPLETED'" :size="24"><CircleCheckFilled /></el-icon>
              <el-icon v-else :size="24"><InfoFilled /></el-icon>
            </div>
            <div class="banner-info">
              <div class="banner-title">{{ task.itemName }}</div>
              <div class="banner-meta">
                <el-tag :type="urgencyTagType(task.urgencyLevel)" size="small" effect="dark">
                  {{ task.urgencyLabel }}
                </el-tag>
                <el-tag :type="statusTagType(task.status)" size="small" effect="plain">
                  {{ task.statusLabel }}
                </el-tag>
                <span class="banner-code">{{ task.itemCode }}</span>
              </div>
            </div>
          </div>
          <div class="banner-actions">
            <el-button
              v-if="task.status === 'QUEUED'"
              type="success"
              size="large"
              :icon="VideoPlay"
              @click="handleStart"
            >
              开始处理
            </el-button>
            <template v-if="task.status === 'IN_PROCESS'">
              <el-button
                v-if="isBrainCtTask"
                type="info"
                size="large"
                @click="openCtInference"
              >
                CT模型推理
              </el-button>
              <el-button
                v-if="isBrainCtTask"
                type="primary"
                plain
                size="large"
                @click="openAiReportAssistant"
              >
                影像报告工作台
              </el-button>
              <el-button
                type="primary"
                size="large"
                :icon="DocumentAdd"
                @click="handleOpenReportDialog"
              >
                生成{{ reportKind }}报告
              </el-button>
              <el-button
                type="warning"
                size="large"
                :icon="CircleCheck"
                @click="handleComplete"
              >
                标记完成
              </el-button>
            </template>
          </div>
        </div>

        <div class="cards-row">
          <!-- 左列 -->
          <div class="main-column">
            <!-- 项目信息卡片 -->
            <div class="info-card">
              <div class="card-title">
                <el-icon><Collection /></el-icon>
                <span>项目信息</span>
              </div>
              <div class="kv-grid">
                <div class="kv-item">
                  <span class="kv-label">项目编码</span>
                  <span class="kv-value">{{ task.itemCode }}</span>
                </div>
                <div class="kv-item">
                  <span class="kv-label">项目类别</span>
                  <el-tag size="small" :type="task.itemCategory === 'LAB' ? 'info' : 'warning'">
                    {{ task.itemCategoryLabel }}
                  </el-tag>
                </div>
                <div class="kv-item">
                  <span class="kv-label">价格</span>
                  <span class="kv-value price">¥{{ task.price }}</span>
                </div>
                <div class="kv-item">
                  <span class="kv-label">申请医生</span>
                  <span class="kv-value">{{ task.requesterDoctorId || '-' }}</span>
                </div>
              </div>
            </div>

            <!-- 临床摘要卡片 -->
            <div class="info-card" v-if="task.clinicalSummary">
              <div class="card-title">
                <el-icon><Notebook /></el-icon>
                <span>临床摘要</span>
              </div>
              <div class="clinical-content">{{ task.clinicalSummary }}</div>
            </div>

            <div class="info-card" v-if="task.status === 'COMPLETED' && task.report">
              <div class="card-title">
                <el-icon><DocumentAdd /></el-icon>
                <span>{{ reportKind }}报告</span>
              </div>
              <div class="report-view">
                <div class="report-row">
                  <span>结果摘要</span>
                  <p>{{ task.report.resultSummary || '-' }}</p>
                </div>
                <div class="report-row">
                  <span>诊断意见</span>
                  <p>{{ task.report.conclusion || '-' }}</p>
                </div>
                <div class="report-meta">
                  <el-tag size="small" :type="task.report.abnormalFlag === 'NORMAL' ? 'success' : 'warning'">
                    {{ abnormalFlagLabel(task.report.abnormalFlag) }}
                  </el-tag>
                  <span>{{ task.report.reportTime || task.report.performedTime || '' }}</span>
                </div>
              </div>
            </div>

            <!-- 时间线卡片 -->
            <div class="info-card">
              <div class="card-title">
                <el-icon><Timer /></el-icon>
                <span>时间记录</span>
              </div>
              <div class="time-line">
                <div class="time-item">
                  <div class="time-dot"></div>
                  <div class="time-body">
                    <span class="time-label">创建时间</span>
                    <span class="time-value">{{ task.createTime }}</span>
                  </div>
                </div>
                <div class="time-item" v-if="task.assignTime">
                  <div class="time-dot active"></div>
                  <div class="time-body">
                    <span class="time-label">分配时间</span>
                    <span class="time-value">{{ task.assignTime }}</span>
                  </div>
                </div>
                <div class="time-item" v-if="task.completeTime">
                  <div class="time-dot done"></div>
                  <div class="time-body">
                    <span class="time-label">完成时间</span>
                    <span class="time-value">{{ task.completeTime }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 右列：患者卡片 -->
          <div class="side-column">
            <div class="patient-card">
              <div class="patient-avatar">
                <el-icon :size="48"><UserFilled /></el-icon>
              </div>
              <div class="patient-name">{{ task.patientName }}</div>
              <div class="patient-meta">
                <span>{{ task.genderLabel }}</span>
                <span class="dot">·</span>
                <span>{{ task.age }}岁</span>
              </div>
              <div class="patient-divider"></div>
              <div class="patient-extra">
                <div class="extra-item">
                  <span class="extra-label">患者ID</span>
                  <span class="extra-value">{{ task.patientId }}</span>
                </div>
                <div class="extra-item">
                  <span class="extra-label">挂号ID</span>
                  <span class="extra-value">{{ task.registerId }}</span>
                </div>
                <div class="extra-item" v-if="task.birthday">
                  <span class="extra-label">出生日期</span>
                  <span class="extra-value">{{ task.birthday }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </template>

      <div v-else class="empty-state">
        <el-icon :size="48"><WarningFilled /></el-icon>
        <p>任务不存在或已删除</p>
      </div>
    </div>
    <el-dialog
      v-model="reportDialogVisible"
      :title="`生成${reportKind}报告`"
      width="640px"
      destroy-on-close
      @closed="resetReportForm"
    >
      <el-form label-position="top">
        <el-form-item label="结果摘要">
          <el-input
            v-model="reportForm.resultSummary"
            type="textarea"
            :rows="4"
            maxlength="10000"
            show-word-limit
            :placeholder="`填写${reportKind}结果摘要`"
          />
        </el-form-item>
        <el-form-item label="结论">
          <el-input
            v-model="reportForm.conclusion"
            type="textarea"
            :rows="4"
            maxlength="10000"
            show-word-limit
            placeholder="填写诊断结论"
          />
        </el-form-item>
        <el-form-item label="异常标记">
          <el-select v-model="reportForm.abnormalFlag" style="width: 100%">
            <el-option
              v-for="option in abnormalFlagOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="附件地址">
          <el-input
            v-model="reportForm.attachmentUrl"
            maxlength="255"
            placeholder="可选，填写报告附件 URL"
          />
        </el-form-item>
      </el-form>

      <!-- AI 辅助分析：所有检查/检验项目可用 -->
      <div class="ai-assist">
        <div class="ai-assist-head">
          <div class="ai-assist-title">
            <el-icon><MagicStick /></el-icon>
            <span>AI 辅助分析</span>
            <small>根据已填写的结果摘要/结论生成参考建议，仅供辅助</small>
          </div>
          <el-button
            type="primary"
            plain
            :loading="aiAnalyzing"
            :icon="MagicStick"
            @click="handleAiAnalyze"
          >
            {{ aiAnalyzing ? '分析中...' : 'AI 分析' }}
          </el-button>
        </div>

        <div v-if="aiResult" class="ai-assist-result">
          <div class="ai-result-row">
            <el-tag :type="riskTagType(aiResult.riskLevel)" size="small" effect="dark">
              {{ riskLabel(aiResult.riskLevel) }}
            </el-tag>
            <el-tag v-if="aiResult.fallback" size="small" type="info">AI 服务降级</el-tag>
          </div>
          <p v-if="aiResult.summary" class="ai-result-summary">{{ aiResult.summary }}</p>

          <div v-if="aiResult.abnormalIndicators && aiResult.abnormalIndicators.length" class="ai-result-block">
            <span class="ai-block-label">异常提示</span>
            <ul>
              <li v-for="(ind, i) in aiResult.abnormalIndicators" :key="i">
                <strong>{{ ind.name }}</strong>
                <span v-if="ind.value">：{{ ind.value }}</span>
                <span v-if="ind.referenceRange" class="ai-ref">（参考 {{ ind.referenceRange }}）</span>
                <span v-if="ind.interpretation"> — {{ ind.interpretation }}</span>
              </li>
            </ul>
          </div>

          <div v-if="aiResult.suggestions && aiResult.suggestions.length" class="ai-result-block">
            <span class="ai-block-label">建议</span>
            <ul>
              <li v-for="(s, i) in aiResult.suggestions" :key="i">{{ s }}</li>
            </ul>
          </div>

          <div v-if="aiResult.followUpAdvice" class="ai-result-block">
            <span class="ai-block-label">复查/随访</span>
            <p>{{ aiResult.followUpAdvice }}</p>
          </div>

          <div class="ai-result-actions">
            <el-button size="small" type="success" plain @click="adoptAiToConclusion">
              采纳到结论
            </el-button>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="reportDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="reportSubmitting" @click="handleSubmitReport">
          提交报告
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getTaskDetail, startTask, completeTask, submitTaskReport } from '@/api/doctor/task'
import { analyzeReport } from '@/api/examination/ct'
import { isBrainCtItem } from '@/utils/brainCt'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowLeft, Clock, Loading, CircleCheckFilled, InfoFilled,
  VideoPlay, CircleCheck, DocumentAdd, Collection, Notebook,
  Timer, UserFilled, WarningFilled, MagicStick
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const task = ref<any>(null)
const loading = ref(false)
const orderItemId = route.params.id as string
const reportDialogVisible = ref(false)
const reportSubmitting = ref(false)
const reportQueryHandled = ref(false)
const reportForm = reactive({
  resultSummary: '',
  conclusion: '',
  abnormalFlag: 'NORMAL',
  attachmentUrl: '',
})
const abnormalFlagOptions = [
  { label: '正常', value: 'NORMAL' },
  { label: '偏高', value: 'HIGH' },
  { label: '偏低', value: 'LOW' },
  { label: '异常', value: 'ABNORMAL' },
  { label: '危急', value: 'CRITICAL' },
]
const reportKind = computed(() => task.value?.itemCategory === 'LAB' ? '检验' : '检查')
const isBrainCtTask = computed(() => isBrainCtItem(
  task.value?.itemCategory,
  task.value?.itemCode,
  task.value?.itemName,
))

interface AiAnalysisResult {
  summary?: string
  riskLevel?: string
  suggestions?: string[]
  followUpAdvice?: string
  fallback?: boolean
  abnormalIndicators?: Array<{
    name: string
    value?: string
    referenceRange?: string
    interpretation?: string
  }>
}

const aiAnalyzing = ref(false)
const aiResult = ref<AiAnalysisResult | null>(null)

function riskTagType(level?: string) {
  return { HIGH: 'danger', MEDIUM: 'warning', LOW: 'success' }[level || ''] || 'info'
}

function riskLabel(level?: string) {
  return { HIGH: '高风险', MEDIUM: '中风险', LOW: '低风险' }[level || ''] || (level || '未评估')
}

async function handleAiAnalyze() {
  const resultSummary = reportForm.resultSummary.trim()
  const conclusion = reportForm.conclusion.trim()
  if (!resultSummary && !conclusion) {
    ElMessage.warning('请先填写结果摘要或结论，再进行 AI 分析')
    return
  }
  if (!task.value?.registerId) {
    ElMessage.error('缺少挂号信息，无法进行 AI 分析')
    return
  }

  const reportText = [
    resultSummary ? `结果摘要：${resultSummary}` : '',
    conclusion ? `诊断结论：${conclusion}` : '',
  ].filter(Boolean).join('\n')

  aiAnalyzing.value = true
  try {
    const res: any = await analyzeReport({
      registerId: task.value.registerId,
      reportType: task.value.itemCategory === 'LAB' ? 'LAB_REPORT' : 'EXAM_REPORT',
      reportText,
    })
    aiResult.value = res.data
    ElMessage.success('AI 分析完成')
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.msg || e?.message || 'AI 分析失败')
  } finally {
    aiAnalyzing.value = false
  }
}

function adoptAiToConclusion() {
  const r = aiResult.value
  if (!r) return
  const lines: string[] = []
  if (r.summary) lines.push(r.summary)
  if (r.suggestions?.length) lines.push('建议：' + r.suggestions.join('；'))
  if (r.followUpAdvice) lines.push('复查/随访：' + r.followUpAdvice)
  const text = lines.join('\n')
  if (!text) {
    ElMessage.warning('没有可采纳的 AI 内容')
    return
  }
  reportForm.conclusion = reportForm.conclusion.trim()
    ? `${reportForm.conclusion.trim()}\n${text}`
    : text
  ElMessage.success('已采纳 AI 建议到结论，请复核后再提交')
}

onMounted(async () => {
  await fetchDetail()
  await openReportFromQuery()
})

async function fetchDetail() {
  loading.value = true
  try {
    const res = await getTaskDetail(orderItemId)
    task.value = res.data
    return true
  } catch {
    ElMessage.error('加载任务详情失败')
    return false
  } finally {
    loading.value = false
  }
}

function shouldAutoOpenReport() {
  return route.query.report === '1' || route.query.report === 'true'
}

function requestListPath() {
  if (task.value?.itemCategory === 'LAB') {
    return '/inspection-doctor/order-list'
  }
  if (task.value?.itemCategory === 'EXAM') {
    return '/examination-doctor/application'
  }
  return sessionStorage.getItem('doctorType') === '3'
    ? '/inspection-doctor/order-list'
    : '/examination-doctor/application'
}

function goBackToRequestList() {
  router.push(requestListPath())
}

async function openReportFromQuery() {
  if (!shouldAutoOpenReport() || reportQueryHandled.value || !task.value) return
  reportQueryHandled.value = true

  if (task.value.status === 'COMPLETED') return
  if (task.value.status === 'QUEUED') {
    try {
      await startTask(orderItemId)
      await fetchDetail()
    } catch (e: any) {
      ElMessage.error(e?.response?.data?.msg || e?.message || '开始处理失败')
      return
    }
  }
  if (task.value?.status === 'IN_PROCESS') {
    handleOpenReportDialog()
  }
}

function urgencyTagType(level: string) {
  return { EMERGENCY: 'danger', URGENT: 'warning', NORMAL: 'success' }[level] || 'info'
}

function statusTagType(status: string) {
  return { QUEUED: '', IN_PROCESS: 'warning', COMPLETED: 'success' }[status] || 'info'
}

function abnormalFlagLabel(flag: string) {
  return {
    NORMAL: '正常',
    ABNORMAL: '异常',
    HIGH: '偏高',
    LOW: '偏低',
    CRITICAL: '危急',
  }[flag] || flag || '未标记'
}

async function handleStart() {
  try {
    await ElMessageBox.confirm('确认开始处理此任务？', '提示', { type: 'info' })
    await startTask(orderItemId)
    ElMessage.success('已开始处理')
    await fetchDetail()
    handleOpenReportDialog()
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error(e?.response?.data?.msg || '操作失败')
  }
}

async function handleComplete() {
  try {
    await ElMessageBox.confirm('确认完成此任务？', '提示', { type: 'info' })
    await completeTask(orderItemId)
    ElMessage.success('任务已完成')
    fetchDetail()
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error(e?.response?.data?.msg || '操作失败')
  }
}

function handleOpenReportDialog() {
  openAiReportAssistant()
}

function taskContextQuery() {
  return {
    orderItemId,
    registerId: task.value?.registerId || '',
    itemName: task.value?.itemName || '',
  }
}

function openCtInference() {
  router.push({
    path: '/examination-doctor/ct-inference',
    query: taskContextQuery(),
  })
}

function openAiReportAssistant() {
  router.push({
    path: '/examination-doctor/report',
    query: taskContextQuery(),
  })
}

function resetReportForm() {
  reportForm.resultSummary = ''
  reportForm.conclusion = ''
  reportForm.abnormalFlag = 'NORMAL'
  reportForm.attachmentUrl = ''
  aiResult.value = null
  aiAnalyzing.value = false
}

async function handleSubmitReport() {
  const resultSummary = reportForm.resultSummary.trim()
  const conclusion = reportForm.conclusion.trim()
  const attachmentUrl = reportForm.attachmentUrl.trim()
  if (!resultSummary && !conclusion) {
    ElMessage.warning('请至少填写结果摘要或结论')
    return
  }

  reportSubmitting.value = true
  try {
    await submitTaskReport({
      orderItemId,
      resultSummary: resultSummary || undefined,
      conclusion: conclusion || undefined,
      abnormalFlag: reportForm.abnormalFlag,
      attachmentUrl: attachmentUrl || undefined,
    })
    ElMessage.success(`${reportKind.value}报告已提交`)
    reportDialogVisible.value = false
    resetReportForm()
    await fetchDetail()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.msg || e?.message || '提交报告失败')
  } finally {
    reportSubmitting.value = false
  }
}
</script>

<style scoped>
.task-detail-page {
  min-height: 100vh;
  background: #f0f2f5;
  padding: 0 0 32px;
}

/* ── 顶部导航 ── */
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 28px;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0,0,0,.04);
  position: sticky;
  top: 0;
  z-index: 10;
}
.back-btn {
  font-size: 14px;
  color: #606266;
}
.back-btn:hover { color: #409eff; }
.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}
.page-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}
.order-id {
  font-size: 12px;
  color: #c0c4cc;
  font-family: monospace;
}

/* ── 内容区 ── */
.content-wrapper {
  max-width: 1100px;
  margin: 0 auto;
  padding: 24px 28px 0;
}

/* ── 状态横幅 ── */
.status-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24px 28px;
  border-radius: 12px;
  margin-bottom: 20px;
  gap: 20px;
}
.banner-QUEUED     { background: linear-gradient(135deg, #ecf5ff, #d9ecff); }
.banner-IN_PROCESS { background: linear-gradient(135deg, #fdf6ec, #faecd8); }
.banner-COMPLETED  { background: linear-gradient(135deg, #f0f9eb, #e1f3d8); }
.banner-WAITING_ASSIGN { background: linear-gradient(135deg, #f4f4f5, #e9e9eb); }

.banner-left {
  display: flex;
  align-items: center;
  gap: 16px;
}
.banner-icon {
  width: 52px; height: 52px;
  display: flex; align-items: center; justify-content: center;
  border-radius: 12px;
  background: rgba(255,255,255,.7);
  color: #409eff;
}
.banner-QUEUED .banner-icon     { color: #409eff; }
.banner-IN_PROCESS .banner-icon { color: #e6a23c; }
.banner-COMPLETED .banner-icon  { color: #67c23a; }

.banner-title {
  font-size: 20px;
  font-weight: 700;
  color: #303133;
  margin-bottom: 6px;
}
.banner-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}
.banner-code {
  font-size: 12px;
  color: #909399;
  font-family: monospace;
}
.banner-actions {
  display: flex;
  gap: 10px;
  flex-shrink: 0;
}

/* ── 双列布局 ── */
.cards-row {
  display: flex;
  gap: 20px;
  align-items: flex-start;
}
.main-column {
  flex: 1;
  min-width: 0;
}
.side-column {
  width: 300px;
  flex-shrink: 0;
}

/* ── 信息卡片 ── */
.info-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px 24px;
  margin-bottom: 16px;
  box-shadow: 0 1px 3px rgba(0,0,0,.04);
}
.card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}
.card-title .el-icon { color: #909399; }

/* ── KV 网格 ── */
.kv-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px 20px;
}
.kv-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.kv-label {
  font-size: 12px;
  color: #909399;
}
.kv-value {
  font-size: 14px;
  color: #303133;
  font-weight: 500;
}
.kv-value.price {
  color: #f56c6c;
  font-size: 16px;
  font-weight: 700;
}

/* ── 临床摘要 ── */
.clinical-content {
  font-size: 14px;
  color: #606266;
  line-height: 1.8;
  background: #fafbfc;
  padding: 14px 16px;
  border-radius: 8px;
  border-left: 3px solid #409eff;
}

.report-view {
  display: grid;
  gap: 14px;
}

.report-row {
  display: grid;
  gap: 6px;
}

.report-row span {
  font-size: 12px;
  color: #909399;
}

.report-row p {
  margin: 0;
  padding: 12px 14px;
  border-radius: 8px;
  background: #fafbfc;
  color: #303133;
  line-height: 1.7;
  white-space: pre-wrap;
}

.report-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: #909399;
  font-size: 12px;
}

/* ── AI 辅助分析（报告弹窗内） ── */
.ai-assist {
  margin-top: 8px;
  padding: 16px;
  border-radius: 12px;
  background: #f7f9fc;
  border: 1px solid #ebeef5;
}
.ai-assist-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}
.ai-assist-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}
.ai-assist-title .el-icon { color: #7c3aed; }
.ai-assist-title small {
  flex-basis: 100%;
  margin-top: 4px;
  font-weight: 400;
  color: #909399;
  font-size: 12px;
}
.ai-assist-result {
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px dashed #dcdfe6;
  display: grid;
  gap: 12px;
}
.ai-result-row {
  display: flex;
  align-items: center;
  gap: 8px;
}
.ai-result-summary {
  margin: 0;
  color: #303133;
  font-size: 13px;
  line-height: 1.7;
}
.ai-result-block {
  display: grid;
  gap: 6px;
}
.ai-block-label {
  font-size: 12px;
  font-weight: 700;
  color: #7c3aed;
}
.ai-result-block ul {
  margin: 0;
  padding-left: 18px;
  color: #4e5969;
  font-size: 13px;
  line-height: 1.7;
}
.ai-result-block p {
  margin: 0;
  color: #4e5969;
  font-size: 13px;
  line-height: 1.7;
}
.ai-ref { color: #909399; }
.ai-result-actions {
  display: flex;
  justify-content: flex-end;
}

/* ── 时间线 ── */
.time-line {
  padding-left: 4px;
}
.time-item {
  display: flex;
  gap: 14px;
  padding-bottom: 16px;
  position: relative;
}
.time-item:not(:last-child)::after {
  content: '';
  position: absolute;
  left: 5px;
  top: 14px;
  bottom: 0;
  width: 1px;
  background: #e4e7ed;
}
.time-dot {
  width: 10px; height: 10px;
  border-radius: 50%;
  background: #dcdfe6;
  margin-top: 4px;
  flex-shrink: 0;
}
.time-dot.active { background: #409eff; }
.time-dot.done   { background: #67c23a; }
.time-body {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.time-label {
  font-size: 12px;
  color: #909399;
}
.time-value {
  font-size: 14px;
  color: #303133;
}

/* ── 患者卡片 ── */
.patient-card {
  background: #fff;
  border-radius: 12px;
  padding: 28px 24px 24px;
  text-align: center;
  box-shadow: 0 1px 3px rgba(0,0,0,.04);
}
.patient-avatar {
  width: 72px; height: 72px;
  margin: 0 auto 12px;
  border-radius: 50%;
  background: #ecf5ff;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #409eff;
}
.patient-name {
  font-size: 18px;
  font-weight: 700;
  color: #303133;
  margin-bottom: 4px;
}
.patient-meta {
  font-size: 13px;
  color: #909399;
  margin-bottom: 16px;
}
.patient-meta .dot { margin: 0 6px; }
.patient-divider {
  height: 1px;
  background: #f0f0f0;
  margin: 0 0 16px;
}
.patient-extra {
  text-align: left;
}
.extra-item {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
}
.extra-item + .extra-item {
  border-top: 1px solid #fafafa;
}
.extra-label {
  font-size: 12px;
  color: #909399;
}
.extra-value {
  font-size: 13px;
  color: #303133;
  font-family: monospace;
  max-width: 160px;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* ── 空状态 ── */
.empty-state {
  text-align: center;
  padding: 80px 0;
  color: #c0c4cc;
}
.empty-state p {
  margin-top: 12px;
  font-size: 14px;
}

/* ── 响应式 ── */
@media (max-width: 768px) {
  .cards-row { flex-direction: column-reverse; }
  .side-column { width: 100%; }
  .status-banner { flex-direction: column; text-align: center; }
  .banner-left { flex-direction: column; }
  .kv-grid { grid-template-columns: 1fr; }
}
</style>

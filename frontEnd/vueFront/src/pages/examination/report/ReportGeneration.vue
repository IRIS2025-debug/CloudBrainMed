<template>
  <div class="report-generation-page">
    <section class="page-header">
      <div class="header-copy">
        <p class="header-eyebrow">AI 报告工作台</p>
        <h2 class="page-title">{{ pageTitle }}</h2>
        <p class="page-subtitle">
          普通检查/检验直接填写报告；脑部 CT 可联动模型推理结果，复核后回传给接诊医生。
        </p>
        <div class="header-tags">
          <span v-if="taskContext.orderItemId">当前任务 {{ taskContext.orderItemId }}</span>
          <span>{{ patientInfo.examItem }}</span>
          <span>{{ patientInfo.department }}</span>
          <span v-if="taskContext.registerId">挂号 {{ taskContext.registerId }}</span>
          <span>报告编号 {{ reportId }}</span>
        </div>
      </div>

      <div class="header-side">
        <div class="hero-stats">
          <div class="hero-stat">
            <strong>{{ imageList.length }}</strong>
            <span>{{ isBrainCtReport ? '影像张数' : '报告模块' }}</span>
          </div>
          <div class="hero-stat">
            <strong>{{ analysisStatusText }}</strong>
            <span>AI分析状态</span>
          </div>
          <div class="hero-stat">
            <strong>{{ chatMessages.length }}</strong>
            <span>对话消息</span>
          </div>
        </div>
        <div class="header-actions">
          <el-button type="primary" @click="handleGenerate" :loading="generating">
            {{ generateButtonText }}
          </el-button>
          <el-button @click="goBack">返回</el-button>
        </div>
      </div>
    </section>

    <el-row :gutter="22" class="main-row">
      <el-col :xl="15" :lg="14" :md="24" class="left-col">
        <el-card shadow="hover" class="report-card">
          <template #header>
            <div class="card-header">
              <div>
                <span class="card-title"><el-icon><Document /></el-icon> {{ reportSectionTitle }}</span>
                <small>结构化整理患者信息、检查检验所见与最终文字报告</small>
              </div>
              <el-tag size="small" type="primary" effect="dark">最终报告</el-tag>
            </div>
          </template>

          <div class="report-content">
            <section class="report-title-section">
              <div>
                <p class="title-kicker">报告主题</p>
                <h3 class="report-title">{{ reportTitle || '检查报告' }}</h3>
              </div>
              <div class="report-title-meta">
                <span>患者 {{ patientInfo.name }}</span>
                <span>{{ patientInfo.visitDate }}</span>
              </div>
            </section>

            <section class="report-section patient-section">
              <div class="section-title">
                <el-icon><User /></el-icon>
                <span>患者个人信息</span>
              </div>
              <div class="section-shell">
                <el-descriptions :column="3" border size="small">
                  <el-descriptions-item label="姓名">{{ patientInfo.name }}</el-descriptions-item>
                  <el-descriptions-item label="性别">{{ patientInfo.gender }}</el-descriptions-item>
                  <el-descriptions-item label="年龄">{{ patientInfo.age }}岁</el-descriptions-item>
                  <el-descriptions-item label="就诊科室">{{ patientInfo.department }}</el-descriptions-item>
                  <el-descriptions-item label="申请医师">{{ patientInfo.doctor }}</el-descriptions-item>
                  <el-descriptions-item label="就诊日期">{{ patientInfo.visitDate }}</el-descriptions-item>
                  <el-descriptions-item label="检查项目" :span="3">{{ patientInfo.examItem }}</el-descriptions-item>
                </el-descriptions>
              </div>
            </section>

            <section v-if="isBrainCtReport" class="report-section analysis-section">
              <div class="section-title">
                <el-icon><DataAnalysis /></el-icon>
                <span>影像分析结果</span>
                <el-tag size="small" type="warning">可编辑</el-tag>
              </div>

              <div class="analysis-display">
                <div class="analysis-stage">
                  <div class="panel-caption">
                    <strong>影像缩略图</strong>
                    <span>点击可预览当前序列</span>
                  </div>
                  <div class="image-thumbnails">
                    <div v-for="(img, idx) in imageList" :key="idx" class="thumb-item" @click="previewImage(idx)">
                      <img :src="img.url" alt="影像" />
                      <span>{{ img.name }}</span>
                    </div>
                    <div v-if="imageList.length === 0" class="empty-thumbs">暂无影像</div>
                  </div>
                </div>

                <div class="ct-result-cards">
                  <div
                    v-for="card in ctResultCards"
                    :key="card.key"
                    class="ct-result-card"
                    :class="{ missing: !card.ready }"
                  >
                    <span>{{ card.title }}</span>
                    <strong>{{ card.ready ? card.summary : '待补充' }}</strong>
                  </div>
                </div>

                <div v-if="analysisResult" class="analysis-result-display">
                  <div class="panel-caption">
                    <strong>分析概览</strong>
                    <span>保留 AI 输出，同时支持人工修订</span>
                  </div>

                  <div class="analysis-stats-grid">
                    <div class="analysis-stat">
                      <div class="stat-label">分析模型</div>
                      <el-input v-model="analysisResult.model" size="small" class="stat-input" />
                    </div>
                    <div class="analysis-stat">
                      <div class="stat-label">置信度</div>
                      <div class="inline-field">
                        <el-input-number
                          v-model="analysisResult.confidence"
                          :min="0"
                          :max="100"
                          size="small"
                          class="stat-input-number"
                        />
                        <span>%</span>
                      </div>
                    </div>
                    <div class="analysis-stat">
                      <div class="stat-label">病灶数量</div>
                      <div class="inline-field">
                        <el-input-number
                          v-model="analysisResult.lesions"
                          :min="0"
                          :max="20"
                          size="small"
                          class="stat-input-number"
                        />
                        <span>处</span>
                      </div>
                    </div>
                    <div class="analysis-stat">
                      <div class="stat-label">处理时间</div>
                      <div class="inline-field">
                        <el-input v-model="analysisResult.processingTime" size="small" class="stat-input" />
                        <span>ms</span>
                      </div>
                    </div>
                  </div>

                  <div class="analysis-conclusion">
                    <span class="stat-label">结论</span>
                    <el-select v-model="analysisResult.conclusionType" size="small" class="conclusion-select">
                      <el-option label="成功" value="success" />
                      <el-option label="警告" value="warning" />
                      <el-option label="危险" value="danger" />
                      <el-option label="信息" value="info" />
                    </el-select>
                    <el-input
                      v-model="analysisResult.conclusion"
                      size="small"
                      class="conclusion-input"
                      placeholder="输入结论"
                    />
                  </div>

                  <div class="analysis-details">
                    <div v-for="(detail, idx) in analysisResult.details" :key="idx" class="detail-item">
                      <el-input
                        v-model="detail.label"
                        size="small"
                        class="detail-label-input"
                        placeholder="标签"
                      />
                      <span class="detail-divider">：</span>
                      <el-input
                        v-model="detail.value"
                        size="small"
                        class="detail-value-input"
                        placeholder="值"
                      />
                      <el-button size="small" type="danger" text @click="removeDetail(idx)">
                        <el-icon><Close /></el-icon>
                      </el-button>
                    </div>
                    <el-button size="small" type="primary" text @click="addDetail" class="add-detail-btn">
                      <el-icon><Plus /></el-icon> 添加详情
                    </el-button>
                  </div>
                </div>

                <div v-else class="empty-analysis-result">
                  <el-empty description="暂无分析结果，请通过右侧 AI 助手进行分析" :image-size="60" />
                </div>
              </div>
            </section>

            <section v-if="!isBrainCtReport" class="report-section offline-section">
              <div class="section-title">
                <el-icon><DataAnalysis /></el-icon>
                <span>线下{{ reportKindText }}结果</span>
              </div>
              <div class="section-shell offline-note">
                当前项目不需要 CT 模型推理。完成线下检查或检验后，直接在下方填写结果摘要、诊断意见和建议，提交后会自动回传给接诊医生。
              </div>
            </section>

            <section class="report-section editor-section">
              <div class="section-title">
                <el-icon><Edit /></el-icon>
                <span>{{ reportSectionTitle }}</span>
              </div>
              <div class="section-shell report-editor">
                <el-form label-width="84px" size="small">
                  <el-form-item :label="isLabReport ? '检验结果' : '检查所见'">
                    <el-input
                      v-model="reportFindings"
                      type="textarea"
                      :rows="4"
                      :placeholder="isLabReport ? '请输入检验结果、关键指标或结果摘要...' : '请输入检查所见描述...'"
                    />
                  </el-form-item>
                  <el-form-item label="诊断意见">
                    <el-input
                      v-model="reportDiagnosis"
                      type="textarea"
                      :rows="4"
                      placeholder="请输入诊断意见..."
                    />
                  </el-form-item>
                  <el-form-item label="建议">
                    <el-input
                      v-model="reportAdvice"
                      type="textarea"
                      :rows="3"
                      placeholder="请输入建议..."
                    />
                  </el-form-item>
                  <el-form-item label="报告医师">
                    <el-input v-model="reportDoctor" placeholder="请输入报告医师姓名" class="doctor-input" />
                  </el-form-item>
                </el-form>
              </div>
            </section>
          </div>
        </el-card>
      </el-col>

      <el-col :xl="9" :lg="10" :md="24" class="right-col">
        <el-card shadow="hover" class="ai-chat-card">
          <template #header>
            <div class="card-header ai-card-header">
              <div>
                <span class="card-title ai-header">
                  <span class="ai-avatar">AI</span>
                  智能助手
                </span>
                <small>辅助完成影像分析、报告草稿和诊断建议</small>
              </div>
              <el-tag size="small" color="#315fbb" effect="dark">对话中</el-tag>
            </div>
          </template>

          <div class="chat-container">
            <div class="chat-summary">
              <div>
                <strong>{{ chatMessages.length }}</strong>
                <span>消息记录</span>
              </div>
              <div>
                <strong>{{ analysisResult ? '已联动' : '待联动' }}</strong>
                <span>报告状态</span>
              </div>
            </div>

            <div class="chat-messages" ref="chatMessagesRef">
              <div v-for="(msg, idx) in chatMessages" :key="idx" class="chat-message" :class="msg.role">
                <div class="message-avatar">
                  {{ msg.role === 'user' ? '医' : 'AI' }}
                </div>
                <div class="message-content">
                  <div class="message-text" v-html="formatMessage(msg.content)"></div>
                  <div v-if="msg.loading" class="typing-indicator">
                    <span></span><span></span><span></span>
                  </div>
                  <div v-if="msg.role === 'assistant' && !msg.loading && msg.showActions && msg.reportData" class="message-actions">
                    <el-button size="small" type="success" @click="acceptAll(msg, idx)">全部采纳</el-button>
                    <el-button size="small" type="primary" @click="openPartialAccept(msg, idx)">部分采纳</el-button>
                    <el-button size="small" text @click="dismissMessage(idx)">忽略</el-button>
                  </div>
                  <div class="message-time">{{ msg.time }}</div>
                </div>
              </div>
              <div v-if="chatMessages.length === 0" class="empty-chat">
                <div class="empty-chat-icon">AI</div>
                <div class="empty-chat-text">开始与 AI 助手对话<br />发送消息或选择快捷指令</div>
              </div>
            </div>

            <div class="quick-actions">
              <el-button size="small" @click="sendQuickMessage('AI 分析当前报告')">AI 分析</el-button>
              <el-button size="small" @click="sendQuickMessage('给出诊断建议')">诊断建议</el-button>
              <el-button size="small" @click="sendQuickMessage('报告模板')">报告模板</el-button>
            </div>

            <div class="chat-input-area">
              <el-input
                v-model="chatInput"
                placeholder="输入消息，与 AI 助手对话..."
                @keyup.enter="sendMessage"
                clearable
              >
                <template #append>
                  <el-button type="primary" @click="sendMessage" :disabled="!chatInput.trim() || aiLoading">
                    <el-icon><Promotion /></el-icon> 发送
                  </el-button>
                </template>
              </el-input>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog
      v-model="partialDialogVisible"
      title="部分采纳 - 选择要应用的内容"
      width="60%"
      :close-on-click-modal="false"
    >
      <div class="partial-accept-content">
        <el-alert
          title="请选择希望应用到左侧报告中的内容"
          type="info"
          :closable="false"
          style="margin-bottom:16px;"
        />
        <div v-if="pendingPartialData" class="partial-items">
          <div class="partial-item">
            <el-checkbox v-model="partialSelections.findings">
              <strong>检查所见</strong>
            </el-checkbox>
            <div class="partial-preview">{{ pendingPartialData.findings || '（空）' }}</div>
          </div>
          <div class="partial-item">
            <el-checkbox v-model="partialSelections.diagnosis">
              <strong>诊断意见</strong>
            </el-checkbox>
            <div class="partial-preview">{{ pendingPartialData.diagnosis || '（空）' }}</div>
          </div>
          <div class="partial-item">
            <el-checkbox v-model="partialSelections.advice">
              <strong>建议</strong>
            </el-checkbox>
            <div class="partial-preview">{{ pendingPartialData.advice || '（空）' }}</div>
          </div>
          <div v-if="isBrainCtReport" class="partial-item">
            <el-checkbox v-model="partialSelections.analysis">
              <strong>影像分析结果</strong>
            </el-checkbox>
            <div class="partial-preview">
              模型：{{ pendingPartialData.analysis?.model || '—' }}，
              置信度：{{ pendingPartialData.analysis?.confidence || '—' }}%，
              病灶数：{{ pendingPartialData.analysis?.lesions || '—' }}处
            </div>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="partialDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="applyPartialAccept">确认应用</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Document,
  User,
  DataAnalysis,
  Edit,
  Promotion,
  Close,
  Plus
} from '@element-plus/icons-vue'
import { submitTaskReport, getTaskDetail } from '@/api/doctor/task'
import { analyzeReport } from '@/api/examination/ct'
import { getStoredAuthHeaders } from '@/api/request'
import { useExamReportStore, type ExamReportPayload, type CtStructuredResult } from '@/stores/examReport'
import { isBrainCtItem } from '@/utils/brainCt'

const route = useRoute()
const router = useRouter()
const examReportStore = useExamReportStore()
const taskContext = computed(() => ({
  orderItemId: String(route.query.orderItemId || ''),
  registerId: String(route.query.registerId || ''),
  itemName: String(route.query.itemName || ''),
}))
const reportId = ref('RPT-2026-06-26-001')
const reportTitle = ref(`${taskContext.value.itemName || '检查'}报告`)
const taskItemCode = ref('')
const taskItemCategory = ref('')

const patientInfo = ref({
  name: '',
  gender: '',
  age: 0,
  department: '',
  doctor: '',
  visitDate: '',
  examItem: taskContext.value.itemName || ''
})

const imageList = ref<{ name: string; url: string }[]>([])
const loadingDetail = ref(false)
const publishedAiResultJson = ref('')
const ctReportPayload = ref<ExamReportPayload | null>(null)
const isLabReport = computed(() => taskItemCategory.value === 'LAB')
const isBrainCtReport = computed(() => isBrainCtItem(
  taskItemCategory.value,
  taskItemCode.value,
  patientInfo.value.examItem,
))
const reportKindText = computed(() => (isLabReport.value ? '检验' : '检查'))
const pageTitle = computed(() => `生成${reportKindText.value}报告`)
const reportSectionTitle = computed(() => `${reportKindText.value}报告`)
const analysisStatusText = computed(() => (isBrainCtReport.value && analysisResult.value ? '已分析' : '待分析'))
const ctResultCards = computed(() => {
  const structured = ctReportPayload.value?.structured || {}
  return [
    ctResultCard('artifact', '金属伪影识别', structured.artifact),
    ctResultCard('lesion', '病灶识别分割', structured.lesion),
  ]
})
const generateButtonText = computed(() => {
  if (generating.value) return '提交中...'
  return taskContext.value.orderItemId
    ? `提交${reportKindText.value}报告`
    : `生成${reportKindText.value}报告`
})

interface AnalysisDetail {
  label: string
  value: string
}

interface AnalysisResult {
  model: string
  confidence: number
  lesions: number
  processingTime: string
  conclusion: string
  conclusionType: 'success' | 'warning' | 'danger' | 'info'
  details: AnalysisDetail[]
}

const analysisResult = ref<AnalysisResult | null>(null)

const reportFindings = ref('')
const reportDiagnosis = ref('')
const reportAdvice = ref('')
const reportDoctor = ref('')

interface ReportData {
  findings?: string
  diagnosis?: string
  advice?: string
  analysis?: AnalysisResult
}

interface ChatMessage {
  role: 'user' | 'assistant'
  content: string
  time: string
  loading?: boolean
  showActions?: boolean
  reportData?: ReportData
  applied?: boolean
}

const chatMessages = ref<ChatMessage[]>([])
const chatInput = ref('')
const aiLoading = ref(false)
const generating = ref(false)
const chatMessagesRef = ref<HTMLElement>()
const isAnalyzed = ref(false)

const partialDialogVisible = ref(false)
const pendingPartialData = ref<ReportData | null>(null)
const pendingMessageIndex = ref(-1)

const partialSelections = reactive({
  findings: true,
  diagnosis: true,
  advice: true,
  analysis: true
})

const getCurrentTime = () => {
  const now = new Date()
  return now.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

function goBack() {
  router.push(requestListPath())
}

function requestListPath() {
  if (taskItemCategory.value === 'LAB') {
    return '/inspection-doctor/order-list'
  }
  if (taskItemCategory.value === 'EXAM') {
    return '/examination-doctor/application'
  }
  return sessionStorage.getItem('doctorType') === '3'
    ? '/inspection-doctor/order-list'
    : '/examination-doctor/application'
}

async function fetchProtectedBlobUrl(url: string) {
  const response = await fetch(url, { headers: getStoredAuthHeaders() })
  if (!response.ok) throw new Error(`资源加载失败：${response.status}`)
  return URL.createObjectURL(await response.blob())
}

async function loadTaskDetail() {
  const orderItemId = taskContext.value.orderItemId
  if (!orderItemId) return
  loadingDetail.value = true
  try {
    const res: any = await getTaskDetail(orderItemId)
    const t = res.data || {}
    taskItemCode.value = t.itemCode || ''
    taskItemCategory.value = t.itemCategory || ''
    patientInfo.value = {
      name: t.patientName || '—',
      gender: t.genderLabel || (t.gender === 1 ? '男' : t.gender === 0 ? '女' : '未知'),
      age: t.age ?? 0,
      department: t.assignedDeptName || t.departmentName || '—',
      doctor: t.requesterDoctorName || t.requesterDoctorId || '—',
      visitDate: (t.createTime || '').toString().slice(0, 10),
      examItem: t.itemName || taskContext.value.itemName || '—',
    }
    reportTitle.value = `${t.itemName || taskContext.value.itemName || '检查'}报告`
    // 已完成任务：带出已有报告内容供查看
    if (t.report) {
      reportFindings.value = t.report.resultSummary || ''
      reportDiagnosis.value = t.report.conclusion || ''
      publishedAiResultJson.value = t.report.aiResultJson || ''
    }
  } catch {
    ElMessage.error('加载任务信息失败')
  } finally {
    loadingDetail.value = false
  }
}

async function loadCtResultFromStore() {
  const orderItemId = taskContext.value.orderItemId
  if (!orderItemId || !isBrainCtReport.value) return
  const payload = examReportStore.getCtResult(orderItemId) || restoreCtResultFromPublishedReport()
  if (!payload) return
  ctReportPayload.value = payload

  // 影像缩略图：当前会话的 blob URL 直接使用；持久化 URL 需要带 token 重新读取。
  if (payload.images.length) {
    try {
      imageList.value = await Promise.all(payload.images.map(async (img) => ({
        name: img.name,
        url: isDirectBrowserImage(img.url) ? img.url : await fetchProtectedBlobUrl(img.url),
      })))
    } catch {
      imageList.value = []
    }
  } else if (payload.previewImageUrl) {
    try {
      const url = await fetchProtectedBlobUrl(payload.previewImageUrl)
      imageList.value = [{ name: 'CT 推理预览', url }]
    } catch {
      // 忽略预览加载失败，不阻断报告填写
    }
  }

  // 结构化结果映射到影像分析区
  const artifact = payload.structured.artifact
  const lesion = payload.structured.lesion
  if (artifact || lesion) {
    analysisResult.value = {
      model: [artifact?.modelText, lesion?.modelText].filter(Boolean).join(' / ') || 'CT 模型',
      confidence: 0,
      lesions: lesion?.positivePixels ? 1 : 0,
      processingTime: '',
      conclusion: [artifact?.summaryText, lesion?.summaryText].filter(Boolean).join('\n') || '',
      conclusionType: ((artifact?.ratio || 0) > 0 || (lesion?.ratio || 0) > 0) ? 'warning' : 'success',
      details: [
        ...ctStructuredDetails('金属伪影', artifact),
        ...ctStructuredDetails('病灶分割', lesion),
      ],
    }
    isAnalyzed.value = true
  }
}

function isDirectBrowserImage(url: string) {
  return url.startsWith('blob:') || url.startsWith('data:')
}

function restoreCtResultFromPublishedReport(): ExamReportPayload | null {
  if (!publishedAiResultJson.value) return null
  try {
    const parsed = JSON.parse(publishedAiResultJson.value)
    if (!parsed || typeof parsed !== 'object') return null
    return {
      orderItemId: String(parsed.orderItemId || taskContext.value.orderItemId || ''),
      registerId: String(parsed.registerId || taskContext.value.registerId || ''),
      previewImageUrl: String(parsed.previewImageUrl || ''),
      images: Array.isArray(parsed.images) ? parsed.images : [],
      structured: parsed.structured || {},
    }
  } catch {
    return null
  }
}

function ctResultCard(key: 'artifact' | 'lesion', title: string, data?: CtStructuredResult) {
  return {
    key,
    title,
    ready: Boolean(data),
    summary: data?.summaryText || data?.reportInput?.summary || '已有模型结果',
  }
}

function ctStructuredDetails(prefix: string, data?: any): AnalysisDetail[] {
  if (!data) return []
  return [
    data.positivePixels !== undefined ? { label: `${prefix}阳性像素`, value: String(data.positivePixels) } : null,
    data.totalPixels !== undefined ? { label: `${prefix}总像素`, value: String(data.totalPixels) } : null,
    data.ratio !== undefined ? { label: `${prefix}占比`, value: `${data.ratio}%` } : null,
    data.sliceText ? { label: `${prefix}切片`, value: data.sliceText } : null,
  ].filter(Boolean) as AnalysisDetail[]
}

function buildPersistentCtImages(payload: ExamReportPayload) {
  const images: Array<{ name: string; url: string }> = []
  const artifact = payload.structured.artifact
  const lesion = payload.structured.lesion
  if (artifact?.previewImageUrl) {
    images.push({ name: artifact.previewImageFile || '金属伪影预览', url: artifact.previewImageUrl })
  }
  if (lesion?.previewImageUrl) {
    images.push({ name: lesion.previewImageFile || '病灶分割预览', url: lesion.previewImageUrl })
  }
  return images
}

function buildAiResultJson() {
  if (!isBrainCtReport.value || !taskContext.value.orderItemId) return undefined
  const payload = ctReportPayload.value || examReportStore.getCtResult(taskContext.value.orderItemId)
  if (!payload) return undefined
  return JSON.stringify({
    orderItemId: payload.orderItemId,
    registerId: payload.registerId,
    previewImageUrl: payload.previewImageUrl,
    images: buildPersistentCtImages(payload),
    structured: payload.structured,
  })
}

onMounted(async () => {
  await loadTaskDetail()
  await loadCtResultFromStore()
})

const formatMessage = (content: string) => content.replace(/\n/g, '<br>')

const scrollToBottom = async () => {
  await nextTick()
  if (chatMessagesRef.value) {
    chatMessagesRef.value.scrollTop = chatMessagesRef.value.scrollHeight
  }
}

const addUserMessage = (content: string) => {
  chatMessages.value.push({
    role: 'user',
    content,
    time: getCurrentTime()
  })
  scrollToBottom()
}

const addDetail = () => {
  if (analysisResult.value) {
    analysisResult.value.details.push({ label: '新增标签', value: '新增值' })
  }
}

const removeDetail = (index: number) => {
  if (analysisResult.value) {
    analysisResult.value.details.splice(index, 1)
  }
}

const sendMessage = async () => {
  const text = chatInput.value.trim()
  if (!text) return

  addUserMessage(text)
  chatInput.value = ''
  await processAIResponse(text)
}

const sendQuickMessage = async (command: string) => {
  addUserMessage(command)
  await processAIResponse(command)
}

const processAIResponse = async (userInput: string) => {
  aiLoading.value = true

  const loadingIndex = chatMessages.value.length
  chatMessages.value.push({
    role: 'assistant',
    content: '',
    time: getCurrentTime(),
    loading: true,
    showActions: false,
    reportData: undefined,
    applied: false
  })
  scrollToBottom()

  // 报告模板为纯本地提示，无需调用后端
  if (userInput.includes('报告模板')) {
    const tpl = [
      '检查报告模板：',
      '',
      '【检查所见】描述病灶位置、大小、形态、密度或信号特征等。',
      '【诊断意见】给出明确诊断结论，并说明需要鉴别的风险点。',
      '【建议】补充进一步检查、复查周期或临床处理建议。'
    ].join('\n')
    const msg = chatMessages.value[loadingIndex]
    if (msg) { msg.content = tpl; msg.loading = false }
    aiLoading.value = false
    scrollToBottom()
    return
  }

  const registerId = taskContext.value.registerId
  if (!registerId) {
    const msg = chatMessages.value[loadingIndex]
    if (msg) { msg.content = '缺少挂号信息，无法调用 AI 分析。请从任务详情页进入本工作台。'; msg.loading = false }
    aiLoading.value = false
    scrollToBottom()
    return
  }

  // 组织送入 AI 的报告文本：已填写内容 + 影像结构化结论
  const parts: string[] = []
  if (reportFindings.value.trim()) parts.push(`检查所见：${reportFindings.value.trim()}`)
  if (reportDiagnosis.value.trim()) parts.push(`诊断意见：${reportDiagnosis.value.trim()}`)
  if (analysisResult.value?.conclusion) parts.push(`影像模型结论：${analysisResult.value.conclusion}`)
  if (analysisResult.value?.details?.length) {
    parts.push('影像结构化结果：' + analysisResult.value.details
      .map((d) => `${d.label}=${d.value}`).join('，'))
  }
  const reportText = parts.join('\n') || `${patientInfo.value.examItem} 影像/检查资料待分析`

  try {
    const res: any = await analyzeReport({
      registerId,
      reportType: taskContext.value.itemName?.includes('CT') ? 'CT_LESION_REPORT' : 'EXAM_REPORT',
      reportText,
    })
    const data = res.data || {}
    const reportData: ReportData = {}

    // AI 建议映射到「诊断意见 / 建议」，供采纳
    const diagnosisLines: string[] = []
    if (data.summary) diagnosisLines.push(data.summary)
    if (data.abnormalIndicators?.length) {
      diagnosisLines.push(...data.abnormalIndicators.map((i: any) =>
        `${i.name}${i.value ? '：' + i.value : ''}${i.interpretation ? '（' + i.interpretation + '）' : ''}`))
    }
    if (diagnosisLines.length) reportData.diagnosis = diagnosisLines.join('\n')
    if (data.suggestions?.length || data.followUpAdvice) {
      reportData.advice = [
        ...(data.suggestions || []),
        data.followUpAdvice ? `随访：${data.followUpAdvice}` : '',
      ].filter(Boolean).join('\n')
    }

    const response = [
      data.fallback ? 'AI 服务降级，以下为参考提示：' : 'AI 分析完成：',
      '',
      data.riskLevel ? `风险等级：${data.riskLevel}` : '',
      data.summary ? `概述：${data.summary}` : '',
      reportData.advice ? `\n建议：\n${reportData.advice}` : '',
      '',
      Object.keys(reportData).length ? '可点击下方「全部采纳」或「部分采纳」应用到左侧报告。' : '',
    ].filter(Boolean).join('\n')

    const lastMsg = chatMessages.value[loadingIndex]
    if (lastMsg) {
      lastMsg.content = response
      lastMsg.loading = false
      lastMsg.showActions = Object.keys(reportData).length > 0
      lastMsg.reportData = Object.keys(reportData).length > 0 ? reportData : undefined
      lastMsg.applied = false
    }
  } catch (e: any) {
    const lastMsg = chatMessages.value[loadingIndex]
    if (lastMsg) {
      lastMsg.content = e?.response?.data?.msg || e?.message || 'AI 分析失败，请稍后重试。'
      lastMsg.loading = false
      lastMsg.showActions = false
    }
  }

  aiLoading.value = false
  scrollToBottom()
}

const acceptAll = (msg: ChatMessage, index: number) => {
  if (!msg.reportData) {
    ElMessage.warning('没有可采纳的数据')
    return
  }

  const data = msg.reportData
  let appliedCount = 0

  if (data.findings) {
    reportFindings.value = data.findings
    appliedCount++
  }
  if (data.diagnosis) {
    reportDiagnosis.value = data.diagnosis
    appliedCount++
  }
  if (data.advice) {
    reportAdvice.value = data.advice
    appliedCount++
  }
  if (isBrainCtReport.value && data.analysis) {
    analysisResult.value = JSON.parse(JSON.stringify(data.analysis))
    appliedCount++
  }

  msg.applied = true
  msg.showActions = false
  chatMessages.value[index] = { ...msg }

  ElMessage.success(`已应用 ${appliedCount} 项内容到左侧报告`)
}

const openPartialAccept = (msg: ChatMessage, index: number) => {
  if (!msg.reportData) {
    ElMessage.warning('没有可采纳的数据')
    return
  }

  pendingPartialData.value = { ...msg.reportData }
  pendingMessageIndex.value = index
  partialSelections.findings = true
  partialSelections.diagnosis = true
  partialSelections.advice = true
  partialSelections.analysis = isBrainCtReport.value
  partialDialogVisible.value = true
}

const applyPartialAccept = () => {
  if (!pendingPartialData.value) return

  const data = pendingPartialData.value
  let appliedCount = 0

  if (partialSelections.findings && data.findings) {
    reportFindings.value = data.findings
    appliedCount++
  }
  if (partialSelections.diagnosis && data.diagnosis) {
    reportDiagnosis.value = data.diagnosis
    appliedCount++
  }
  if (partialSelections.advice && data.advice) {
    reportAdvice.value = data.advice
    appliedCount++
  }
  if (isBrainCtReport.value && partialSelections.analysis && data.analysis) {
    analysisResult.value = JSON.parse(JSON.stringify(data.analysis))
    appliedCount++
  }

  partialDialogVisible.value = false

  if (pendingMessageIndex.value >= 0 && pendingMessageIndex.value < chatMessages.value.length) {
    const msg = chatMessages.value[pendingMessageIndex.value]
    if (!msg) return
    msg.showActions = false
    msg.applied = true
    chatMessages.value[pendingMessageIndex.value] = { ...msg }
  }

  ElMessage.success(`已应用 ${appliedCount} 项内容到左侧报告`)
  pendingPartialData.value = null
  pendingMessageIndex.value = -1
}

const dismissMessage = (index: number) => {
  const msg = chatMessages.value[index]
  if (!msg) return
  msg.showActions = false
  msg.applied = true
  chatMessages.value[index] = { ...msg }
  ElMessage.info('已忽略该建议')
}

const previewImage = (index: number) => {
  ElMessage.info(`预览影像 ${index + 1}`)
}

function resolveAbnormalFlag() {
  const conclusionType = analysisResult.value?.conclusionType
  if (conclusionType === 'danger') return 'CRITICAL'
  if (conclusionType === 'warning') return 'ABNORMAL'
  return 'NORMAL'
}

function buildResultSummary() {
  const parts = [reportFindings.value, reportAdvice.value].filter(Boolean)
  if (isBrainCtReport.value && analysisResult.value) {
    const analysisLines = [
      '影像分析结果：',
      analysisResult.value.conclusion ? `模型结论：${analysisResult.value.conclusion}` : '',
      ...analysisResult.value.details.map((item) => `${item.label}：${item.value}`),
    ].filter(Boolean)
    parts.push(analysisLines.join('\n'))
  }
  return parts.join('\n\n')
}

const handleGenerate = async () => {
  if (!reportFindings.value || !reportDiagnosis.value) {
    ElMessage.warning(isLabReport.value ? '请填写检验结果和诊断意见' : '请填写检查所见和诊断意见')
    return
  }

  try {
    await ElMessageBox.confirm(`确认生成最终${reportKindText.value}报告？`, '提示', {
      confirmButtonText: '确认生成',
      cancelButtonText: '取消',
      type: 'info'
    })

    generating.value = true
    if (taskContext.value.orderItemId) {
      await submitTaskReport({
        orderItemId: taskContext.value.orderItemId,
        resultSummary: buildResultSummary(),
        conclusion: reportDiagnosis.value,
        abnormalFlag: resolveAbnormalFlag(),
        aiResultJson: buildAiResultJson(),
      })
    } else {
      await new Promise((resolve) => setTimeout(resolve, 1500))
    }

    chatMessages.value.push({
      role: 'assistant',
      content: [
        taskContext.value.orderItemId ? `${reportKindText.value}报告已提交并回传。` : `${reportKindText.value}报告已成功生成。`,
        '',
        `报告编号：${reportId.value}`,
        `生成时间：${new Date().toLocaleString('zh-CN')}`,
        taskContext.value.orderItemId ? `任务明细：${taskContext.value.orderItemId}` : '',
        '',
        taskContext.value.orderItemId ? '接诊医生可在本次接诊报告中查看。' : '请检查左侧报告内容，确认无误后即可使用。'
      ].filter(Boolean).join('\n'),
      time: getCurrentTime(),
      loading: false,
      showActions: false,
      reportData: undefined,
      applied: false
    })

    ElMessage.success(taskContext.value.orderItemId ? `${reportKindText.value}报告已提交` : `${reportKindText.value}报告已生成`)
    scrollToBottom()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error?.response?.data?.msg || error?.message || `提交${reportKindText.value}报告失败`)
    }
  } finally {
    generating.value = false
  }
}
</script>

<style scoped>
.report-generation-page {
  min-height: 100vh;
  padding: 28px 30px 34px;
  background:
    radial-gradient(circle at top right, rgba(49, 95, 187, 0.08), transparent 24%),
    linear-gradient(180deg, #f6f9fd 0%, #f4f7fb 100%);
}

.page-header {
  display: grid;
  grid-template-columns: minmax(0, 1.6fr) minmax(280px, 0.9fr);
  gap: 18px;
  margin-bottom: 22px;
}

.header-copy,
.header-side {
  padding: 24px 26px;
  border: 1px solid #dce6f3;
  border-radius: 26px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 18px 40px rgba(28, 44, 68, 0.06);
}

.header-eyebrow {
  margin: 0 0 8px;
  color: #315fbb;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.1em;
}

.page-title {
  margin: 0;
  color: #0f172a;
  font-size: 32px;
  line-height: 1.1;
}

.page-subtitle {
  max-width: 760px;
  margin: 12px 0 0;
  color: #64748b;
  font-size: 14px;
  line-height: 1.8;
}

.header-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 18px;
}

.header-tags span {
  display: inline-flex;
  align-items: center;
  padding: 8px 14px;
  border-radius: 999px;
  background: #eef4ff;
  color: #315fbb;
  font-size: 12px;
  font-weight: 700;
}

.header-side {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 16px;
}

.hero-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.hero-stat {
  padding: 16px 14px;
  border-radius: 18px;
  background: linear-gradient(145deg, #f8fbff, #eef5ff);
  border: 1px solid rgba(203, 219, 239, 0.92);
}

.hero-stat strong {
  display: block;
  color: #102033;
  font-size: 18px;
  line-height: 1.1;
  white-space: nowrap;
}

.hero-stat span {
  display: block;
  margin-top: 6px;
  color: #6b7b8f;
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
}

.header-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.main-row {
  min-height: calc(100vh - 252px);
}

.left-col,
.right-col {
  display: flex;
}

.report-card,
.ai-chat-card {
  width: 100%;
  border-radius: 24px;
  border: 1px solid #dbe5f1;
  box-shadow: 0 20px 42px rgba(28, 44, 68, 0.07);
  overflow: hidden;
}

.report-card {
  background: linear-gradient(180deg, #ffffff 0%, #fbfdff 100%);
}

.report-card :deep(.el-card__header),
.ai-chat-card :deep(.el-card__header) {
  padding: 18px 22px;
  border-bottom: 1px solid #edf2f7;
}

.report-card :deep(.el-card__body) {
  height: 100%;
  padding: 22px;
  overflow-y: auto;
}

.ai-chat-card :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 18px;
  overflow: hidden;
}

.card-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
}

.card-title {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: #102033;
  font-size: 15px;
  font-weight: 800;
}

.card-header small {
  display: block;
  margin-top: 5px;
  color: #708197;
  font-size: 12px;
  font-weight: 600;
}

.report-content {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.report-title-section {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 20px 22px;
  border-radius: 22px;
  border: 1px solid #d8e4f3;
  background: linear-gradient(145deg, rgba(239, 246, 255, 0.94), rgba(248, 251, 255, 0.98));
}

.title-kicker {
  margin: 0 0 8px;
  color: #4d6b92;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.08em;
}

.report-title {
  margin: 0;
  color: #0f172a;
  font-size: 24px;
  line-height: 1.25;
}

.report-title-meta {
  display: flex;
  flex-direction: column;
  gap: 8px;
  align-items: flex-end;
}

.report-title-meta span {
  display: inline-flex;
  align-items: center;
  padding: 7px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(209, 220, 235, 0.95);
  color: #536b88;
  font-size: 12px;
  font-weight: 700;
}

.report-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #102033;
  font-size: 15px;
  font-weight: 800;
}

.section-title .el-icon {
  color: #315fbb;
  font-size: 17px;
}

.section-shell,
.analysis-stage,
.analysis-result-display,
.empty-analysis-result {
  border: 1px solid #dce7f3;
  border-radius: 20px;
  background: #fff;
  box-shadow: 0 10px 28px rgba(28, 44, 68, 0.04);
}

.section-shell {
  padding: 18px;
}

.patient-section .section-shell {
  padding: 16px;
}

.patient-section :deep(.el-descriptions__body) {
  border-radius: 16px;
  overflow: hidden;
}

.patient-section :deep(.el-descriptions__label) {
  color: #607187;
  font-weight: 700;
}

.analysis-display {
  display: grid;
  gap: 14px;
}

.ct-result-cards {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.ct-result-card {
  display: grid;
  gap: 8px;
  padding: 14px 16px;
  border: 1px solid #dbe7f6;
  border-radius: 16px;
  background: #f8fbff;
}

.ct-result-card span {
  color: #315fbb;
  font-size: 12px;
  font-weight: 800;
}

.ct-result-card strong {
  color: #102033;
  font-size: 13px;
  line-height: 1.7;
  font-weight: 700;
}

.ct-result-card.missing {
  border-style: dashed;
  background: #fbfdff;
}

.ct-result-card.missing span,
.ct-result-card.missing strong {
  color: #94a3b8;
}

.analysis-stage,
.analysis-result-display,
.empty-analysis-result {
  padding: 16px 18px;
}

.panel-caption {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 14px;
}

.panel-caption strong {
  color: #102033;
  font-size: 14px;
  font-weight: 800;
}

.panel-caption span {
  color: #7b8aa0;
  font-size: 12px;
  font-weight: 600;
}

.image-thumbnails {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(110px, 1fr));
  gap: 12px;
}

.thumb-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 10px;
  border: 1px solid #dbe5f1;
  border-radius: 16px;
  background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
  cursor: pointer;
  transition: transform 0.18s ease, border-color 0.18s ease, box-shadow 0.18s ease;
}

.thumb-item:hover {
  transform: translateY(-2px);
  border-color: #8eb7f5;
  box-shadow: 0 12px 26px rgba(49, 95, 187, 0.12);
}

.thumb-item img {
  width: 100%;
  height: 82px;
  border-radius: 12px;
  object-fit: cover;
  display: block;
}

.thumb-item span {
  color: #546b87;
  font-size: 12px;
  font-weight: 700;
  line-height: 1.5;
  word-break: break-all;
}

.empty-thumbs {
  grid-column: 1 / -1;
  padding: 18px;
  border: 1px dashed #cbd8e7;
  border-radius: 14px;
  color: #7b8aa0;
  font-size: 13px;
  text-align: center;
}

.analysis-stats-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.analysis-stat {
  padding: 14px 12px;
  border-radius: 16px;
  background: #f8fbff;
  border: 1px solid #dbe7f6;
}

.stat-label {
  display: block;
  margin-bottom: 8px;
  color: #6b7b8f;
  font-size: 12px;
  font-weight: 700;
}

.inline-field {
  display: flex;
  align-items: center;
  gap: 8px;
}

.inline-field span {
  color: #7b8aa0;
  font-size: 12px;
  font-weight: 700;
}

.stat-input,
.stat-input-number {
  width: 100%;
}

.analysis-conclusion {
  display: grid;
  grid-template-columns: auto 120px minmax(0, 1fr);
  align-items: center;
  gap: 12px;
  margin: 14px 0;
  padding: 14px 16px;
  border-radius: 18px;
  background: #f7faff;
  border: 1px solid #dde8f5;
}

.conclusion-select {
  width: 100%;
}

.conclusion-input {
  width: 100%;
}

.analysis-details {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 14px;
  border-radius: 18px;
  background: #fbfdff;
  border: 1px dashed #cfdaea;
}

.detail-item {
  display: grid;
  grid-template-columns: 138px auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 8px;
}

.detail-divider {
  color: #9aa7b8;
  font-weight: 700;
}

.add-detail-btn {
  align-self: flex-start;
}

.empty-analysis-result {
  padding: 18px;
}

.report-editor :deep(.el-form-item) {
  margin-bottom: 14px;
}

.report-editor :deep(.el-form-item:last-child) {
  margin-bottom: 0;
}

.doctor-input {
  width: 220px;
  max-width: 100%;
}

.report-editor :deep(.el-form-item__label) {
  font-weight: 700;
  color: #5f718a;
}

.report-editor :deep(.el-input__wrapper),
.report-editor :deep(.el-textarea__inner),
.analysis-result-display :deep(.el-input__wrapper),
.analysis-result-display :deep(.el-select__wrapper) {
  border-radius: 14px;
  box-shadow: 0 0 0 1px #d8e2ee inset;
}

.report-editor :deep(.el-textarea__inner) {
  min-height: 108px !important;
  line-height: 1.75;
  padding: 14px 16px;
}

.ai-card-header {
  align-items: center;
}

.ai-header {
  gap: 10px;
}

.ai-avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border-radius: 12px;
  background: linear-gradient(135deg, #315fbb, #3f87dc);
  color: #fff;
  font-size: 13px;
  font-weight: 800;
  letter-spacing: 0.06em;
}

.chat-container {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
}

.chat-summary {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 14px;
}

.chat-summary div {
  padding: 14px;
  border-radius: 16px;
  background: linear-gradient(145deg, #f8fbff, #eff5ff);
  border: 1px solid #dbe6f4;
}

.chat-summary strong {
  display: block;
  color: #102033;
  font-size: 18px;
  line-height: 1.1;
}

.chat-summary span {
  display: block;
  margin-top: 6px;
  color: #6f8197;
  font-size: 12px;
  font-weight: 700;
}

.chat-messages {
  flex: 1;
  min-height: 0;
  padding: 6px 4px 8px;
  overflow-y: auto;
}

.chat-message {
  display: flex;
  gap: 12px;
  margin-bottom: 14px;
  animation: fadeIn 0.28s ease;
}

.chat-message.user {
  flex-direction: row-reverse;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}

.message-avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 14px;
  flex-shrink: 0;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.04em;
  background: #e8eef8;
  color: #315fbb;
}

.chat-message.user .message-avatar {
  background: #315fbb;
  color: #fff;
}

.chat-message.assistant .message-avatar {
  background: #e8f1ff;
}

.message-content {
  max-width: 84%;
}

.chat-message.user .message-content {
  text-align: right;
}

.message-text {
  padding: 12px 14px;
  border-radius: 18px;
  background: #f8fbff;
  border: 1px solid #dbe6f4;
  color: #1d2b3d;
  font-size: 13px;
  line-height: 1.8;
  word-break: break-word;
  white-space: pre-wrap;
}

.chat-message.user .message-text {
  background: linear-gradient(135deg, #315fbb, #3f87dc);
  border-color: transparent;
  color: #fff;
}

.message-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 8px;
}

.message-time {
  margin-top: 4px;
  padding: 0 4px;
  color: #8b99aa;
  font-size: 11px;
}

.typing-indicator {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 8px 0 0;
}

.typing-indicator span {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #4f8df7;
  animation: typingBounce 1.4s infinite;
}

.typing-indicator span:nth-child(2) { animation-delay: 0.2s; }
.typing-indicator span:nth-child(3) { animation-delay: 0.4s; }

@keyframes typingBounce {
  0%, 60%, 100% { transform: translateY(0); opacity: 0.4; }
  30% { transform: translateY(-8px); opacity: 1; }
}

.empty-chat {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  padding: 22px;
  border: 1px dashed #cfdaea;
  border-radius: 20px;
  background: #fbfdff;
  color: #7b8aa0;
}

.empty-chat-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 58px;
  height: 58px;
  margin-bottom: 14px;
  border-radius: 20px;
  background: linear-gradient(135deg, #315fbb, #3f87dc);
  color: #fff;
  font-size: 18px;
  font-weight: 800;
}

.empty-chat-text {
  font-size: 13px;
  text-align: center;
  line-height: 1.8;
}

.quick-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 14px 0;
  margin-top: 10px;
  border-top: 1px solid #edf2f7;
  border-bottom: 1px solid #edf2f7;
}

.quick-actions .el-button {
  min-width: 0;
  flex: 1 1 calc(50% - 4px);
  margin-left: 0 !important;
  border-radius: 14px;
  font-weight: 700;
}

.chat-input-area {
  padding-top: 14px;
}

.chat-input-area :deep(.el-input__wrapper) {
  border-radius: 16px 0 0 16px;
  box-shadow: 0 0 0 1px #d9e4f2 inset;
}

.chat-input-area :deep(.el-input-group__append) {
  padding: 0 12px;
  border-radius: 0 16px 16px 0;
}

.partial-accept-content {
  padding: 4px 0;
}

.partial-items {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.partial-item {
  padding: 12px 16px;
  background: #f7faff;
  border-radius: 14px;
  border-left: 3px solid #315fbb;
}

.partial-preview {
  margin-top: 6px;
  padding: 10px 12px;
  background: #fff;
  border-radius: 10px;
  font-size: 13px;
  color: #4e5969;
  line-height: 1.7;
  white-space: pre-wrap;
  max-height: 100px;
  overflow-y: auto;
}

@media (max-width: 1200px) {
  .page-header {
    grid-template-columns: 1fr;
  }

  .main-row {
    min-height: auto;
  }
}

@media (max-width: 992px) {
  .report-generation-page {
    padding: 20px 18px 26px;
  }

  .left-col,
  .right-col {
    margin-bottom: 18px;
  }

  .analysis-stats-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .chat-container {
    min-height: 440px;
  }
}

@media (max-width: 680px) {
  .page-title {
    font-size: 26px;
  }

  .header-side,
  .header-copy,
  .report-card :deep(.el-card__body),
  .ai-chat-card :deep(.el-card__body) {
    padding: 18px;
  }

  .hero-stats,
  .chat-summary,
  .analysis-stats-grid,
  .analysis-conclusion {
    grid-template-columns: 1fr;
  }

  .report-title-section,
  .panel-caption {
    flex-direction: column;
    align-items: flex-start;
  }

  .report-title-meta {
    align-items: flex-start;
  }

  .header-actions {
    justify-content: stretch;
    flex-wrap: wrap;
  }

  .header-actions .el-button,
  .quick-actions .el-button {
    flex: 1 1 100%;
  }

  .detail-item {
    grid-template-columns: 1fr;
    align-items: stretch;
  }

  .detail-divider {
    display: none;
  }

  .message-content {
    max-width: calc(100% - 48px);
  }
}
</style>

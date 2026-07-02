<template>
  <div class="report-generation-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <h2 class="page-title">📄 生成检查报告</h2>
      <div class="header-actions">
        <el-button type="primary" @click="handleGenerate" :loading="generating">
          {{ generating ? '生成中...' : '生成检查报告' }}
        </el-button>
        <el-button @click="goBack">返回</el-button>
      </div>
    </div>

    <!-- 主要内容 -->
    <el-row :gutter="20" class="main-row">
      <!-- 左侧：最终报告 -->
      <el-col :span="13" class="left-col">
        <el-card shadow="hover" class="report-card">
          <template #header>
            <div class="card-header">
              <span><el-icon><Document /></el-icon> 检查报告</span>
              <el-tag size="small" type="primary">最终报告</el-tag>
            </div>
          </template>

          <div class="report-content">
            <!-- 报告标题 -->
            <div class="report-title-section">
              <h3 class="report-title">{{ reportTitle || '检查报告' }}</h3>
              <div class="report-id">报告编号：{{ reportId }}</div>
            </div>

            <!-- 患者个人信息 -->
            <div class="report-section">
              <div class="section-title">
                <el-icon><User /></el-icon> 患者个人信息
              </div>
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

            <!-- 影像分析结果 - 可编辑 -->
            <div class="report-section">
              <div class="section-title">
                <el-icon><DataAnalysis /></el-icon> 影像分析结果
                <el-tag size="small" type="warning" style="margin-left: 8px">可编辑</el-tag>
              </div>
              <div class="analysis-display">
                <!-- 影像缩略图 -->
                <div class="image-thumbnails">
                  <div v-for="(img, idx) in imageList" :key="idx" class="thumb-item">
                    <img :src="img.url" alt="影像" @click="previewImage(idx)" />
                  </div>
                  <div v-if="imageList.length === 0" class="empty-thumbs">暂无影像</div>
                </div>

                <!-- 分析结果 - 可编辑 -->
                <div v-if="analysisResult" class="analysis-result-display">
                  <el-row :gutter="12">
                    <el-col :span="6">
                      <div class="analysis-stat">
                        <div class="stat-label">分析模型</div>
                        <el-input
                          v-model="analysisResult.model"
                          size="small"
                          class="stat-input"
                        />
                      </div>
                    </el-col>
                    <el-col :span="6">
                      <div class="analysis-stat">
                        <div class="stat-label">置信度</div>
                        <el-input-number
                          v-model="analysisResult.confidence"
                          :min="0"
                          :max="100"
                          size="small"
                          class="stat-input-number"
                        />
                        <span style="font-size:12px;color:#86909c;">%</span>
                      </div>
                    </el-col>
                    <el-col :span="6">
                      <div class="analysis-stat">
                        <div class="stat-label">病灶数量</div>
                        <el-input-number
                          v-model="analysisResult.lesions"
                          :min="0"
                          :max="20"
                          size="small"
                          class="stat-input-number"
                        />
                        <span style="font-size:12px;color:#86909c;">处</span>
                      </div>
                    </el-col>
                    <el-col :span="6">
                      <div class="analysis-stat">
                        <div class="stat-label">处理时间</div>
                        <el-input
                          v-model="analysisResult.processingTime"
                          size="small"
                          class="stat-input"
                        />
                        <span style="font-size:12px;color:#86909c;">ms</span>
                      </div>
                    </el-col>
                  </el-row>
                  <div class="analysis-conclusion">
                    <span class="stat-label" style="margin-right:8px;">结论：</span>
                    <el-select v-model="analysisResult.conclusionType" size="small" style="width:100px;">
                      <el-option label="成功" value="success" />
                      <el-option label="警告" value="warning" />
                      <el-option label="危险" value="danger" />
                      <el-option label="信息" value="info" />
                    </el-select>
                    <el-input
                      v-model="analysisResult.conclusion"
                      size="small"
                      style="width:300px;margin-left:8px;"
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
                      <span style="color:#86909c;margin:0 4px;">：</span>
                      <el-input
                        v-model="detail.value"
                        size="small"
                        class="detail-value-input"
                        placeholder="值"
                      />
                      <el-button
                        size="small"
                        type="danger"
                        text
                        @click="removeDetail(idx)"
                        style="margin-left:4px;"
                      >
                        <el-icon><Close /></el-icon>
                      </el-button>
                    </div>
                    <el-button size="small" type="primary" text @click="addDetail" style="margin-top:4px;">
                      <el-icon><Plus /></el-icon> 添加详情
                    </el-button>
                  </div>
                </div>
                <div v-else class="empty-analysis-result">
                  <el-empty description="暂无分析结果，请通过右侧AI助手进行分析" :image-size="60" />
                </div>
              </div>
            </div>

            <!-- 检查报告 -->
            <div class="report-section">
              <div class="section-title">
                <el-icon><Edit /></el-icon> 检查报告
              </div>
              <div class="report-editor">
                <el-form label-width="80px" size="small">
                  <el-form-item label="检查所见">
                    <el-input
                      v-model="reportFindings"
                      type="textarea"
                      :rows="3"
                      placeholder="请输入检查所见描述..."
                    />
                  </el-form-item>
                  <el-form-item label="诊断意见">
                    <el-input
                      v-model="reportDiagnosis"
                      type="textarea"
                      :rows="3"
                      placeholder="请输入诊断意见..."
                    />
                  </el-form-item>
                  <el-form-item label="建议">
                    <el-input
                      v-model="reportAdvice"
                      type="textarea"
                      :rows="2"
                      placeholder="请输入建议..."
                    />
                  </el-form-item>
                  <el-form-item label="报告医师">
                    <el-input v-model="reportDoctor" placeholder="请输入报告医师姓名" style="width: 200px" />
                  </el-form-item>
                </el-form>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 右侧：AI 智能体对话 -->
      <el-col :span="11" class="right-col">
        <el-card shadow="hover" class="ai-chat-card">
          <template #header>
            <div class="card-header">
              <span class="ai-header">
                <span class="ai-avatar">🤖</span>
                AI 智能助手
              </span>
              <el-tag size="small" color="#409eff" effect="dark">对话中</el-tag>
            </div>
          </template>

          <div class="chat-container">
            <!-- 对话消息列表 -->
            <div class="chat-messages" ref="chatMessagesRef">
              <div v-for="(msg, idx) in chatMessages" :key="idx" class="chat-message" :class="msg.role">
                <div class="message-avatar">
                  {{ msg.role === 'user' ? '👤' : '🤖' }}
                </div>
                <div class="message-content">
                  <div class="message-text" v-html="formatMessage(msg.content)"></div>
                  <div v-if="msg.loading" class="typing-indicator">
                    <span></span><span></span><span></span>
                  </div>
                  <!-- AI生成结果后的采纳按钮 -->
                  <div v-if="msg.role === 'assistant' && !msg.loading && msg.showActions && msg.reportData" class="message-actions">
                    <el-button size="small" type="success" @click="acceptAll(msg, idx)">
                      ✅ 全部采纳
                    </el-button>
                    <el-button size="small" type="primary" @click="openPartialAccept(msg, idx)">
                      📝 部分采纳
                    </el-button>
                    <el-button size="small" text @click="dismissMessage(idx)">
                      忽略
                    </el-button>
                  </div>
                  <div class="message-time">{{ msg.time }}</div>
                </div>
              </div>
              <div v-if="chatMessages.length === 0" class="empty-chat">
                <div class="empty-chat-icon">💬</div>
                <div class="empty-chat-text">开始与AI助手对话<br />发送消息或选择快捷指令</div>
              </div>
            </div>

            <!-- 快捷指令 -->
            <div class="quick-actions">
              <el-button size="small" @click="sendQuickMessage('分析影像')">📊 分析影像</el-button>
              <el-button size="small" @click="sendQuickMessage('生成报告')">📄 生成报告</el-button>
              <el-button size="small" @click="sendQuickMessage('诊断建议')">💊 诊断建议</el-button>
              <el-button size="small" @click="sendQuickMessage('报告模板')">📋 报告模板</el-button>
            </div>

            <!-- 输入框 -->
            <div class="chat-input-area">
              <el-input
                v-model="chatInput"
                placeholder="输入消息，与AI助手对话..."
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

    <!-- 部分采纳弹窗 -->
    <el-dialog
      v-model="partialDialogVisible"
      title="📝 部分采纳 - 选择要应用的内容"
      width="60%"
      :close-on-click-modal="false"
    >
      <div class="partial-accept-content">
        <el-alert
          title="请勾选您希望采纳到左侧报告中的内容"
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
          <div class="partial-item">
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
import { ref, nextTick, reactive } from 'vue'
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

// ---------- 报告信息 ----------
const reportId = ref('RPT-2026-06-26-001')
const reportTitle = ref('头颅CT平扫检查报告')

// ---------- 患者信息 ----------
const patientInfo = ref({
  name: '王小明',
  gender: '男',
  age: 45,
  department: '神经内科',
  doctor: '李敏',
  visitDate: '2026-06-26',
  examItem: '头颅CT平扫'
})

// ---------- 影像列表 ----------
const imageList = ref([
  { name: 'CT_001.dcm', url: 'https://picsum.photos/200/200?random=1' },
  { name: 'CT_002.dcm', url: 'https://picsum.photos/200/200?random=2' },
  { name: 'CT_003.dcm', url: 'https://picsum.photos/200/200?random=3' }
])

// ---------- 分析结果 - 可编辑 ----------
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

// ---------- 报告内容 ----------
const reportFindings = ref('')
const reportDiagnosis = ref('')
const reportAdvice = ref('')
const reportDoctor = ref('李敏')

// ---------- 聊天相关 ----------
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
  applied?: boolean // 标记是否已被采纳
}

const chatMessages = ref<ChatMessage[]>([])
const chatInput = ref('')
const aiLoading = ref(false)
const generating = ref(false)
const chatMessagesRef = ref<HTMLElement>()

// 是否已分析影像
const isAnalyzed = ref(false)

// ---------- 部分采纳相关 ----------
const partialDialogVisible = ref(false)
const pendingPartialData = ref<ReportData | null>(null)
const pendingMessageIndex = ref(-1)

const partialSelections = reactive({
  findings: true,
  diagnosis: true,
  advice: true,
  analysis: true
})

// ---------- 方法 ----------
const getCurrentTime = () => {
  const now = new Date()
  return now.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

const formatMessage = (content: string) => {
  return content.replace(/\n/g, '<br>')
}

const scrollToBottom = async () => {
  await nextTick()
  if (chatMessagesRef.value) {
    chatMessagesRef.value.scrollTop = chatMessagesRef.value.scrollHeight
  }
}

const addAssistantMessage = (content: string, loading = false, reportData?: ReportData) => {
  chatMessages.value.push({
    role: 'assistant',
    content,
    time: getCurrentTime(),
    loading,
    showActions: false,
    reportData,
    applied: false
  })
  scrollToBottom()
}

const addUserMessage = (content: string) => {
  chatMessages.value.push({
    role: 'user',
    content,
    time: getCurrentTime()
  })
  scrollToBottom()
}

// 添加详情
const addDetail = () => {
  if (analysisResult.value) {
    analysisResult.value.details.push({ label: '新标签', value: '新值' })
  }
}

const removeDetail = (index: number) => {
  if (analysisResult.value) {
    analysisResult.value.details.splice(index, 1)
  }
}

// 发送消息
const sendMessage = async () => {
  const text = chatInput.value.trim()
  if (!text) return

  addUserMessage(text)
  chatInput.value = ''

  await processAIResponse(text)
}

// 快捷指令
const sendQuickMessage = async (command: string) => {
  addUserMessage(command)
  await processAIResponse(command)
}

// AI 响应处理
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

  try {
    await new Promise(resolve => setTimeout(resolve, 1000 + Math.random() * 1000))

    let response = ''
    const reportData: ReportData = {}

    if (userInput.includes('分析影像') || userInput.includes('分析')) {
      if (imageList.value.length === 0) {
        response = '⚠️ 请先上传影像文件，我才能进行分析。\n\n请点击「上传影像」按钮上传患者的检查影像。'
      } else {
        const newResult: AnalysisResult = {
          model: '肺结节检测 (CNN)',
          confidence: 87,
          lesions: 2,
          processingTime: '235',
          conclusion: '检测到2处可疑结节，建议进一步检查',
          conclusionType: 'warning',
          details: [
            { label: '结节1位置', value: '右肺上叶 (RUL)，大小 5.2mm' },
            { label: '结节2位置', value: '左肺下叶 (LLL)，大小 3.8mm' },
            { label: '影像质量', value: '良好' },
            { label: '建议', value: '建议3个月后复查' }
          ]
        }
        reportData.analysis = newResult
        isAnalyzed.value = true

        response = '🔍 影像分析完成！\n\n' +
          '**分析结果：**\n' +
          '• 模型：肺结节检测 (CNN)\n' +
          '• 置信度：87%\n' +
          '• 病灶数量：2 处\n' +
          '• 处理时间：235 ms\n\n' +
          '**结论：** 检测到2处可疑结节，建议进一步检查\n\n' +
          '💡 您可以点击下方「全部采纳」或「部分采纳」将结果应用到报告中。'
      }
    } else if (userInput.includes('生成报告')) {
      if (!isAnalyzed.value && !analysisResult.value) {
        response = '⚠️ 请先进行影像分析，我才能生成报告。\n\n请发送「分析影像」或点击快捷按钮开始分析。'
      } else {
        const findings = '头颅CT平扫显示：双侧大脑半球对称，灰白质界限清晰。右侧额叶可见一大小约5mm的低密度灶，边界欠清。左侧顶叶可见一大小约3mm的结节影。脑室系统未见明显扩张，中线结构居中。'
        const diagnosis = '1. 右侧额叶低密度灶，考虑良性病变可能性大，建议增强扫描进一步明确。\n2. 左侧顶叶小结节，建议定期随访观察。'
        const advice = '建议：1. 完善头颅增强MRI检查。2. 3个月后复查头颅CT。3. 如有头痛、恶心等症状及时就诊。'

        reportData.findings = findings
        reportData.diagnosis = diagnosis
        reportData.advice = advice

        response = '📄 已为您生成报告草稿：\n\n' +
          '**【检查所见】**\n' + findings + '\n\n' +
          '**【诊断意见】**\n' + diagnosis + '\n\n' +
          '**【建议】**\n' + advice + '\n\n' +
          '💡 您可以点击下方「全部采纳」或「部分采纳」将内容应用到左侧报告。'
      }
    } else if (userInput.includes('诊断建议')) {
      response = '💊 **诊断建议：**\n\n' +
        '基于影像分析结果，提供以下诊断建议：\n\n' +
        '1. **右侧额叶低密度灶**\n' +
        '   - 考虑为良性病变（如血管瘤、脂肪瘤）\n' +
        '   - 建议进行增强MRI进一步明确诊断\n\n' +
        '2. **左侧顶叶小结节**\n' +
        '   - 大小约3mm，边界清晰\n' +
        '   - 建议3个月后复查CT，观察变化\n\n' +
        '3. **临床建议**\n' +
        '   - 密切关注患者有无头痛、恶心等症状\n' +
        '   - 如有症状加重，建议及时就诊'
    } else if (userInput.includes('报告模板')) {
      response = '📋 **检查报告模板：**\n\n' +
        '**【检查所见】**\n' +
        '（描述影像所见，包括位置、大小、形态、密度/信号特征等）\n\n' +
        '**【诊断意见】**\n' +
        '（给出明确的诊断结论，分级描述）\n\n' +
        '**【建议】**\n' +
        '（给出后续检查或治疗建议）\n\n' +
        '---\n' +
        '💡 您可以在左侧报告区域直接编辑，或让我帮你生成完整报告。'
    } else {
      response = '您好！我是AI智能助手，可以帮助您：\n\n' +
        '📊 **分析影像** - 对上传的影像进行智能分析\n' +
        '📄 **生成报告** - 基于分析结果生成检查报告\n' +
        '💊 **诊断建议** - 提供专业的诊断参考建议\n' +
        '📋 **报告模板** - 查看标准报告模板格式\n\n' +
        '请选择上方快捷按钮，或直接输入您的需求。'
    }

    const lastMsg = chatMessages.value[loadingIndex]
    if (lastMsg) {
      lastMsg.content = response
      lastMsg.loading = false
      // 如果有报告数据，显示操作按钮
      if (Object.keys(reportData).length > 0) {
        lastMsg.showActions = true
        lastMsg.reportData = reportData
        lastMsg.applied = false
      } else {
        lastMsg.showActions = false
      }
    }

  } catch (error) {
    const lastMsg = chatMessages.value[loadingIndex]
    if (lastMsg) {
      lastMsg.content = '❌ 抱歉，处理您的请求时出现错误，请稍后重试。'
      lastMsg.loading = false
      lastMsg.showActions = false
    }
  }

  aiLoading.value = false
  scrollToBottom()
}

// ---------- 采纳功能 ----------
// 全部采纳 - 直接应用到左侧报告
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
  if (data.analysis) {
    analysisResult.value = JSON.parse(JSON.stringify(data.analysis))
    appliedCount++
  }

  // 标记消息已应用
  msg.applied = true
  msg.showActions = false

  // 更新数组
  chatMessages.value[index] = { ...msg }

  ElMessage.success(`✅ 已应用 ${appliedCount} 项内容到左侧报告`)
}

// 部分采纳 - 打开弹窗
const openPartialAccept = (msg: ChatMessage, index: number) => {
  if (!msg.reportData) {
    ElMessage.warning('没有可采纳的数据')
    return
  }

  pendingPartialData.value = { ...msg.reportData }
  pendingMessageIndex.value = index

  // 重置选择状态（默认全部选中）
  partialSelections.findings = true
  partialSelections.diagnosis = true
  partialSelections.advice = true
  partialSelections.analysis = true

  partialDialogVisible.value = true
}

// 应用部分采纳
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
  if (partialSelections.analysis && data.analysis) {
    analysisResult.value = JSON.parse(JSON.stringify(data.analysis))
    appliedCount++
  }

  partialDialogVisible.value = false

  // 隐藏该消息的操作按钮
  if (pendingMessageIndex.value >= 0 && pendingMessageIndex.value < chatMessages.value.length) {
    const msg = chatMessages.value[pendingMessageIndex.value]
    msg.showActions = false
    msg.applied = true
    chatMessages.value[pendingMessageIndex.value] = { ...msg }
  }

  ElMessage.success(`✅ 已应用 ${appliedCount} 项内容到左侧报告`)
  pendingPartialData.value = null
  pendingMessageIndex.value = -1
}

// 忽略消息
const dismissMessage = (index: number) => {
  const msg = chatMessages.value[index]
  msg.showActions = false
  msg.applied = true
  chatMessages.value[index] = { ...msg }
  ElMessage.info('已忽略该建议')
}

// 预览影像
const previewImage = (index: number) => {
  ElMessage.info(`预览影像 ${index + 1}`)
}

const goBack = () => {
  ElMessage.info('返回上一页')
}

// 生成检查报告
const handleGenerate = async () => {
  if (!reportFindings.value || !reportDiagnosis.value) {
    ElMessage.warning('请填写检查所见和诊断意见')
    return
  }

  try {
    await ElMessageBox.confirm('确认生成最终检查报告？', '提示', {
      confirmButtonText: '确认生成',
      cancelButtonText: '取消',
      type: 'info'
    })

    generating.value = true
    await new Promise(resolve => setTimeout(resolve, 1500))

    const msg: ChatMessage = {
      role: 'assistant',
      content: '✅ 检查报告已成功生成！\n\n' +
        '报告编号：' + reportId.value + '\n' +
        '生成时间：' + new Date().toLocaleString() + '\n\n' +
        '📄 请检查左侧报告内容，确认无误后即可使用。',
      time: getCurrentTime(),
      loading: false,
      showActions: false,
      reportData: undefined,
      applied: false
    }
    chatMessages.value.push(msg)

    ElMessage.success('✅ 检查报告已生成！')
    generating.value = false
    scrollToBottom()
  } catch {
    generating.value = false
  }
}
</script>

<style scoped>
.report-generation-page {
  padding: 24px;
  background: #f0f2f5;
  min-height: 100vh;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding: 0 4px;
}

.page-title {
  font-size: 22px;
  font-weight: 600;
  color: #1d2129;
  margin: 0;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.el-card {
  border-radius: 12px;
  overflow: hidden;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 15px;
  font-weight: 500;
  color: #1d2129;
}

.card-header .el-icon {
  margin-right: 6px;
}

.main-row {
  height: calc(100vh - 140px);
  min-height: 600px;
}

.left-col,
.right-col {
  height: 100%;
}

/* ========== 左侧 ========== */
.report-card {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.report-card :deep(.el-card__body) {
  flex: 1;
  overflow-y: auto;
  padding: 16px 20px;
}

.report-content {
  height: 100%;
}

.report-title-section {
  text-align: center;
  border-bottom: 2px solid #e8eaed;
  padding-bottom: 12px;
  margin-bottom: 16px;
}

.report-title {
  font-size: 20px;
  font-weight: 600;
  color: #1d2129;
  margin: 0 0 4px 0;
}

.report-id {
  font-size: 12px;
  color: #86909c;
}

.report-section {
  margin-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
  padding-bottom: 16px;
}

.report-section:last-child {
  border-bottom: none;
  margin-bottom: 0;
  padding-bottom: 0;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #1d2129;
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.section-title .el-icon {
  font-size: 16px;
  color: #409eff;
}

/* 影像缩略图 */
.image-thumbnails {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}

.thumb-item {
  width: 70px;
  cursor: pointer;
  border-radius: 6px;
  overflow: hidden;
  border: 2px solid #e5e6eb;
  transition: border-color 0.3s;
  background: #f7f8fa;
}

.thumb-item:hover {
  border-color: #409eff;
}

.thumb-item img {
  width: 100%;
  height: 60px;
  object-fit: cover;
  display: block;
}

.empty-thumbs {
  color: #86909c;
  font-size: 13px;
}

/* 分析结果 - 可编辑 */
.analysis-result-display {
  background: #f7f8fa;
  border-radius: 8px;
  padding: 12px 16px;
}

.analysis-stat {
  text-align: center;
}

.stat-label {
  font-size: 12px;
  color: #86909c;
  display: block;
  margin-bottom: 4px;
}

.stat-input {
  width: 80px;
}

.stat-input-number {
  width: 80px;
}

.analysis-conclusion {
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 12px 0 8px 0;
  flex-wrap: wrap;
  gap: 4px;
}

.analysis-details {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 8px 12px;
  background: #fff;
  border-radius: 6px;
}

.detail-item {
  display: flex;
  align-items: center;
  padding: 2px 0;
}

.detail-label-input {
  width: 120px;
}

.detail-value-input {
  flex: 1;
  min-width: 150px;
}

.empty-analysis-result {
  padding: 8px 0;
}

/* 报告编辑 */
.report-editor {
  padding: 4px 0;
}

.report-editor :deep(.el-form-item) {
  margin-bottom: 12px;
}

.report-editor :deep(.el-form-item:last-child) {
  margin-bottom: 0;
}

/* ========== 右侧：AI 聊天 ========== */
.ai-chat-card {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.ai-chat-card :deep(.el-card__body) {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 12px 16px 12px 16px;
  overflow: hidden;
}

.ai-header {
  display: flex;
  align-items: center;
  gap: 8px;
}

.ai-avatar {
  font-size: 20px;
}

.chat-container {
  display: flex;
  flex-direction: column;
  flex: 1;
  height: 100%;
  min-height: 0;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 8px 4px 8px 4px;
  min-height: 0;
  max-height: none;
}

.chat-message {
  display: flex;
  gap: 10px;
  margin-bottom: 14px;
  animation: fadeIn 0.3s ease;
}

.chat-message.user {
  flex-direction: row-reverse;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}

.message-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  flex-shrink: 0;
  background: #f0f2f5;
}

.chat-message.user .message-avatar {
  background: #409eff;
}

.chat-message.assistant .message-avatar {
  background: #e8f5e9;
}

.message-content {
  max-width: 80%;
}

.chat-message.user .message-content {
  text-align: right;
}

.message-text {
  background: #f7f8fa;
  padding: 10px 14px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.7;
  color: #1d2129;
  word-break: break-word;
  white-space: pre-wrap;
}

.chat-message.user .message-text {
  background: #409eff;
  color: #fff;
}

.message-time {
  font-size: 11px;
  color: #86909c;
  margin-top: 2px;
  padding: 0 4px;
}

/* 消息操作按钮 */
.message-actions {
  margin-top: 8px;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

/* 打字动画 */
.typing-indicator {
  display: flex;
  gap: 4px;
  padding: 8px 0;
  align-items: center;
}

.typing-indicator span {
  width: 8px;
  height: 8px;
  background: #409eff;
  border-radius: 50%;
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
  color: #86909c;
}

.empty-chat-icon {
  font-size: 48px;
  margin-bottom: 12px;
}

.empty-chat-text {
  font-size: 14px;
  text-align: center;
  line-height: 1.8;
}

.quick-actions {
  display: flex;
  gap: 8px;
  padding: 8px 0;
  flex-wrap: wrap;
  border-top: 1px solid #e8eaed;
  border-bottom: 1px solid #e8eaed;
  margin: 4px 0;
  flex-shrink: 0;
}

.quick-actions .el-button {
  flex: 1;
  min-width: 70px;
}

.chat-input-area {
  padding-top: 10px;
  flex-shrink: 0;
}

.chat-input-area :deep(.el-input-group__append) {
  padding: 0 12px;
}

/* ========== 部分采纳弹窗 ========== */
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
  background: #f7f8fa;
  border-radius: 8px;
  border-left: 3px solid #409eff;
}

.partial-preview {
  margin-top: 4px;
  padding: 6px 10px;
  background: #fff;
  border-radius: 4px;
  font-size: 13px;
  color: #4e5969;
  line-height: 1.6;
  white-space: pre-wrap;
  max-height: 80px;
  overflow-y: auto;
}

/* 响应式 */
@media (max-width: 992px) {
  .main-row {
    height: auto;
    min-height: auto;
  }

  .left-col,
  .right-col {
    height: auto;
    margin-bottom: 20px;
  }

  .el-col {
    flex: 0 0 100%;
    max-width: 100%;
  }

  .chat-container {
    height: 420px;
    min-height: 350px;
  }

  .analysis-details {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 576px) {
  .page-header {
    flex-direction: column;
    align-items: stretch;
    gap: 12px;
  }

  .header-actions {
    justify-content: stretch;
  }

  .header-actions .el-button {
    flex: 1;
  }

  .quick-actions {
    flex-direction: column;
  }

  .quick-actions .el-button {
    flex: none;
  }

  .chat-container {
    height: 360px;
    min-height: 280px;
  }

  .detail-item {
    flex-wrap: wrap;
  }

  .detail-label-input {
    width: 80px;
  }

  .analysis-conclusion {
    flex-direction: column;
    align-items: stretch;
  }

  .analysis-conclusion .el-select {
    width: 100% !important;
  }

  .analysis-conclusion .el-input {
    width: 100% !important;
    margin-left: 0 !important;
    margin-top: 4px;
  }
}
</style>
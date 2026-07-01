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
          <div class="record-section-grid">
            <div class="record-section" v-for="section in recordSectionDefs" :key="section.key" :class="{ required: requiredRecordSectionKeys.includes(section.key) }">
              <div class="record-section-head">
                <span>{{ section.label }}</span>
                <small>{{ section.hint }}</small>
              </div>
              <el-input
                v-model="recordSections[section.key]"
                type="textarea"
                :rows="section.rows"
                :disabled="isCompleted"
                :placeholder="section.placeholder"
                class="editor"
              />
            </div>
          </div>
          <div class="actions">
            <el-button @click="handleSaveDraft" :loading="saving" :disabled="isCompleted" size="large">暂存草稿</el-button>
            <el-button type="success" @click="handleConfirm" :loading="confirming" :disabled="isCompleted" size="large">确认病历</el-button>
            <el-button type="primary" @click="handleRecommendExamItems" :loading="examRecommendLoading" :disabled="isCompleted" size="large" plain>AI推荐检查/检验</el-button>
            <el-button type="warning" @click="handleCreateExam" :disabled="isCompleted" size="large">开具检查单</el-button>
            <el-button type="success" @click="handleCreatePrescription" :disabled="isCompleted" size="large">开具处方</el-button>
            <el-button type="danger" @click="handleComplete" :loading="completing" :disabled="isCompleted" size="large" plain>完成接诊</el-button>
          </div>
        </section>

        <section class="card ai-exam-card" v-if="examRecommendations.length">
          <div class="card-head ai-exam-head">
            <span>AI 推荐检查/检验</span>
            <small>医生确认后再生成正式申请单</small>
          </div>
          <p v-if="examRecommendSummary" class="ai-exam-summary">{{ examRecommendSummary }}</p>
          <div class="ai-exam-list">
            <label
              v-for="item in examRecommendations"
              :key="item.id"
              class="ai-exam-recommendation"
              :class="{ selected: item.selected }"
            >
              <el-checkbox v-model="item.selected" :disabled="isCompleted" />
              <div class="ai-exam-content">
                <div class="ai-exam-title-line">
                  <strong>{{ item.itemName }}</strong>
                  <el-tag size="small" :type="item.category === 'LAB' ? 'success' : 'primary'" round>{{ item.category === 'LAB' ? '检验' : '检查' }}</el-tag>
                  <el-tag size="small" :type="urgencyTag(item.urgencyLevel)" round>{{ urgencyLabel(item.urgencyLevel) }}</el-tag>
                </div>
                <p>{{ item.reason || 'AI 建议结合当前病历进一步确认。' }}</p>
                <small>{{ item.dept || '待分配科室' }}</small>
              </div>
            </label>
          </div>
          <div class="ai-exam-actions">
            <el-button @click="examRecommendations = []">收起</el-button>
            <el-button type="primary" :disabled="isCompleted || !selectedExamRecommendations.length" @click="applyExamRecommendations">
              带入检查单
            </el-button>
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
                <el-form-item label="药品名称">
                  <el-select
                    v-model="rxForm.medicineId"
                    filterable
                    clearable
                    :loading="medicineLoading"
                    placeholder="请选择药品"
                    style="width: 100%"
                    @visible-change="loadMedicinesOnOpen"
                    @change="handleMedicineChange"
                  >
                    <el-option
                      v-for="item in medicineOptions"
                      :key="item.medicineId"
                      :label="`${item.name} ${item.spec || ''}`"
                      :value="item.medicineId"
                    >
                      <div class="medicine-option">
                        <span>{{ item.name }}</span>
                        <small>{{ item.spec || '无规格' }} · 库存 {{ item.stock ?? '--' }}</small>
                      </div>
                    </el-option>
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="规格"><el-input v-model="rxForm.spec" placeholder="选择药品后自动带出，可调整" /></el-form-item>
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
            <div class="prescription-actions">
              <el-button type="primary" @click="submitPrescription" :loading="rxLoading" :disabled="isCompleted">提交处方</el-button>
              <el-button @click="sendQuickAction(prescriptionReviewAction)" :loading="aiLoading" :disabled="isCompleted">AI处方审查</el-button>
            </div>
          </el-form>
        </section>
      </main>

      <aside class="ai-sidebar">
        <div class="ai-head">
          <div class="ai-badge"><el-icon><MagicStick /></el-icon></div>
          <div>
            <div class="ai-title">AI 辅助医生</div>
            <div class="ai-subtitle">聊天式接诊辅助与报告分析</div>
          </div>
        </div>

        <div class="ai-tabs">
          <button :class="{ active: aiMode === 'reception' }" @click="aiMode = 'reception'">辅助接诊</button>
          <button :class="{ active: aiMode === 'report' }" @click="aiMode = 'report'">报告分析</button>
        </div>

        <template v-if="aiMode === 'reception'">
          <div class="chat-list">
            <div v-if="!chatMessages.length" class="chat-empty">
              <div class="empty-title">向 AI 提问</div>
              <p>可以直接输入问题，也可以使用下方快捷功能。AI 会带上当前病历分层内容和接诊上下文。</p>
            </div>
            <div v-for="message in chatMessages" :key="message.id" class="chat-message" :class="'is-' + message.role">
              <div class="bubble">
                <div class="bubble-meta">
                  <span>{{ message.role === 'doctor' ? '医生' : 'AI' }}</span>
                  <el-tag v-if="message.response?.status" :type="message.response.fallback ? 'warning' : 'success'" size="small" round>{{ message.response.status }}</el-tag>
                </div>
                <p>{{ message.content }}</p>
                <div v-if="message.response?.moduleResult" class="module-result">
                  <p v-for="line in moduleResultLines(message.response.moduleResult)" :key="line">{{ line }}</p>
                </div>
                <el-button
                  v-if="draftRecordFromResponse(message.response)"
                  type="success"
                  plain
                  size="small"
                  @click="applyAiRecordDraft(draftRecordFromResponse(message.response)!)"
                >
                  应用到病历
                </el-button>
              </div>
            </div>
          </div>

          <div class="chat-input-shell">
            <el-input
              v-model="chatInput"
              type="textarea"
              :rows="3"
              :disabled="isCompleted || aiLoading"
              resize="none"
              placeholder="输入想问 AI 的接诊问题"
              @keydown.ctrl.enter.prevent="sendChatQuestion"
            />
            <div class="chat-input-tools">
              <div class="chat-tool-left">
                <el-button
                  v-for="action in visibleAiActions"
                  :key="action.actionType"
                  size="small"
                  class="quick-action-chip"
                  :disabled="isCompleted || aiLoading"
                  @click="sendQuickAction(action)"
                >
                  {{ action.label }}
                </el-button>
                <el-dropdown v-if="moreAiActions.length" trigger="click" placement="top" @command="handleMoreAction">
                  <el-button size="small" class="quick-action-chip quick-more" :disabled="isCompleted || aiLoading">
                    更多<el-icon class="el-icon--right"><MoreFilled /></el-icon>
                  </el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item v-for="action in moreAiActions" :key="action.actionType" :command="action.actionType">
                        {{ action.label }}
                      </el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </div>
              <el-button
                type="primary"
                circle
                class="send-button"
                :loading="aiLoading"
                :disabled="isCompleted || !chatInput.trim()"
                @click="sendChatQuestion"
              >
                <el-icon><Promotion /></el-icon>
              </el-button>
            </div>
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
              class="report-textarea"
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
import { ArrowLeft, MagicStick, MoreFilled, Promotion } from '@element-plus/icons-vue'
import { getConsultDetail, saveDraft, confirmRecord, createExamOrder, completeConsult, createPrescription } from '@/api/doctor/consult'
import { assistantChat, analyzeReport, recommendExamItems, type AiAssistantActionType, type AiAssistantChatResponse, type AiExamRecommendationItem, type AiExamRecommendResponse, type ReportAnalysisResponse } from '@/api/doctor/ai'
import { getMedicineList } from '@/api/doctor/medicine'
import type { Medicine } from '@/types/admin/adminMedicine'

type RecordSectionKey = 'chiefComplaint' | 'presentHistory' | 'pastHistory' | 'physicalExam' | 'auxiliaryExam' | 'diagnosis' | 'treatmentPlan'

interface RecordSectionDef {
  key: RecordSectionKey
  label: string
  hint: string
  placeholder: string
  rows: number
}

interface AiQuickAction {
  label: string
  actionType: AiAssistantActionType
  prompt: string
}

interface ChatMessage {
  id: number
  role: 'doctor' | 'assistant'
  content: string
  response?: AiAssistantChatResponse
}

interface NormalizedExamRecommendation {
  id: number
  itemName: string
  category: string
  dept: string
  urgencyLevel: string
  reason: string
  selected: boolean
}

const route = useRoute()
const registerId = route.params.registerId as string
const detail = ref<any>({})
const saving = ref(false)
const confirming = ref(false)
const completing = ref(false)
const examLoading = ref(false)
const examRecommendLoading = ref(false)
const showExamDialog = ref(false)
const urgencyLevel = ref('NORMAL')
const examItems = ref([{ id: Date.now(), itemName: '', dept: '' }])
const examRecommendations = ref<NormalizedExamRecommendation[]>([])
const examRecommendSummary = ref('')
const aiMode = ref<'reception' | 'report'>('reception')
const aiLoading = ref(false)
const chatInput = ref('')
const chatMessages = ref<ChatMessage[]>([])
const reportLoading = ref(false)
const savingReportAnalysis = ref(false)
const reportForm = ref({ reportType: 'LAB', reportText: '' })
const reportResult = ref<ReportAnalysisResponse | null>(null)
const showPrescriptionDialog = ref(false)
const rxLoading = ref(false)
const medicineLoading = ref(false)
const medicineOptions = ref<Medicine[]>([])
const rxForm = ref({ medicineId: '', medicineName: '', spec: '', usage: '', num: 1, price: 0 })

const emptyRecordSections = (): Record<RecordSectionKey, string> => ({
  chiefComplaint: '',
  presentHistory: '',
  pastHistory: '',
  physicalExam: '',
  auxiliaryExam: '',
  diagnosis: '',
  treatmentPlan: ''
})

const recordSections = ref<Record<RecordSectionKey, string>>(emptyRecordSections())
const requiredRecordSectionKeys: RecordSectionKey[] = ['chiefComplaint', 'presentHistory', 'diagnosis', 'treatmentPlan']

const recordSectionDefs: RecordSectionDef[] = [
  { key: 'chiefComplaint', label: '主诉', hint: '患者核心诉求', placeholder: '例如：反复头痛伴眩晕三天', rows: 2 },
  { key: 'presentHistory', label: '现病史', hint: '本次起病经过', placeholder: '记录起病时间、诱因、症状变化、已处理情况', rows: 4 },
  { key: 'pastHistory', label: '既往史', hint: '既往疾病与过敏', placeholder: '记录既往病史、手术史、过敏史、用药史', rows: 3 },
  { key: 'physicalExam', label: '体格检查', hint: '查体阳性/阴性体征', placeholder: '记录生命体征、专科查体和重要阴性体征', rows: 3 },
  { key: 'auxiliaryExam', label: '辅助检查', hint: '检查检验与报告', placeholder: '记录已有检查、检验、影像报告及 AI 报告分析摘要', rows: 3 },
  { key: 'diagnosis', label: '诊断意见', hint: '初步诊断/鉴别诊断', placeholder: '记录初步诊断、诊断依据和待排除风险', rows: 3 },
  { key: 'treatmentPlan', label: '处理计划', hint: '医嘱、处方、随访', placeholder: '记录处理意见、检查计划、用药建议和随访安排', rows: 3 }
]

const aiQuickActions: AiQuickAction[] = [
  { label: '追问建议', actionType: 'FOLLOW_UP_QUESTION', prompt: '请给出下一步需要追问患者的问题。' },
  { label: '信息缺失', actionType: 'MISSING_INFORMATION', prompt: '请判断当前问诊信息还缺少哪些关键内容。' },
  { label: '上下文总结', actionType: 'CONTEXT_SUMMARY', prompt: '请整理当前患者已知信息、历史资料和待确认事项。' },
  { label: '病情问答', actionType: 'CONTEXT_QA', prompt: '请基于当前患者上下文回答我的接诊问题。' },
  { label: 'AI病历生成', actionType: 'MEDICAL_RECORD_DRAFT', prompt: '请根据当前接诊内容生成病历草稿。' },
  { label: 'AI处方审查', actionType: 'PRESCRIPTION_REVIEW', prompt: '请审查当前处方用药风险。' },
  { label: '诊断辅助', actionType: 'DIAGNOSIS_ASSISTANT', prompt: '请给出可能诊断、支持依据、证据不足和下一步确认问题。' }
]

const visibleAiActionTypes: AiAssistantActionType[] = ['DIAGNOSIS_ASSISTANT', 'PRESCRIPTION_REVIEW']
const visibleAiActions = visibleAiActionTypes
  .map(actionType => aiQuickActions.find(action => action.actionType === actionType))
  .filter((action): action is AiQuickAction => Boolean(action))
const moreAiActions = aiQuickActions.filter(action => !visibleAiActionTypes.includes(action.actionType))
const prescriptionReviewAction = aiQuickActions.find(action => action.actionType === 'PRESCRIPTION_REVIEW')!

const patientName = computed(() => detail.value.name || detail.value.patientName || '--')
const isCompleted = computed(() => detail.value.consultStatus === 'COMPLETED')
const recordDesc = computed({
  get: () => serializeRecordDesc(recordSections.value),
  set: (value: string) => {
    recordSections.value = parseRecordDesc(value)
  }
})
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
const selectedExamRecommendations = computed(() => examRecommendations.value.filter(item => item.selected))

onMounted(async () => {
  try {
    const res = await getConsultDetail(registerId)
    detail.value = res.data
    recordSections.value = parseRecordDesc(res.data.description || '')
    if (!recordSections.value.chiefComplaint && res.data.chiefComplaint) {
      recordSections.value.chiefComplaint = res.data.chiefComplaint
    }
    loadMedicineOptions()
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

async function handleRecommendExamItems() {
  if (isCompleted.value) {
    ElMessage.warning('接诊已完成，不能继续推荐检查/检验')
    return
  }
  if (!recordDesc.value.trim() && !detail.value.chiefComplaint) {
    ElMessage.warning('请先填写主诉或病历内容')
    return
  }

  examRecommendLoading.value = true
  try {
    const res = await recommendExamItems({
      registerId,
      chiefComplaint: recordSections.value.chiefComplaint || detail.value.chiefComplaint || '',
      recordDesc: recordDesc.value,
      patientAge: String(detail.value.patientAge || ''),
      patientGender: genderLabel(detail.value.gender),
      structuredParameters: buildStructuredParameters()
    })
    const result = normalizeExamRecommendResponse(res.data)
    examRecommendations.value = result.items
    examRecommendSummary.value = result.summary
    if (!result.items.length) {
      ElMessage.info('AI 暂未给出检查/检验项目建议')
      return
    }
    ElMessage.success('AI 推荐已生成，请医生确认')
  } catch (e: any) {
    if (e?.response?.status === 404) {
      ElMessage.error('AI检查/检验推荐接口暂未接入，请联系模块二确认接口地址')
    } else {
      showActionError(e, 'AI检查/检验推荐失败')
    }
  } finally {
    examRecommendLoading.value = false
  }
}

function applyExamRecommendations() {
  const selectedItems = selectedExamRecommendations.value
  if (!selectedItems.length) {
    ElMessage.warning('请先选择推荐项目')
    return
  }
  const existingItems = examItems.value.filter(item => item.itemName.trim())
  const newItems = selectedItems.map(item => ({
    id: Date.now() + Math.random(),
    itemName: item.itemName,
    dept: item.dept
  }))
  examItems.value = [...existingItems, ...newItems]
  showExamDialog.value = true
  showPrescriptionDialog.value = false
  ElMessage.success('已带入检查申请单，请确认后提交')
}

function handleCreatePrescription() {
  if (isCompleted.value) {
    ElMessage.warning('接诊已完成，不能继续开具处方')
    return
  }
  showPrescriptionDialog.value = true
  showExamDialog.value = false
  loadMedicineOptions()
}

async function submitPrescription() {
  if (isCompleted.value) {
    ElMessage.warning('接诊已完成，不能继续开具处方')
    return
  }
  if (!rxForm.value.medicineId) {
    ElMessage.warning('请选择药品')
    return
  }
  if (!rxForm.value.usage.trim()) {
    ElMessage.warning('请输入用法用量')
    return
  }
  rxLoading.value = true
  try {
    await createPrescription({ ...rxForm.value, registerId })
    ElMessage.success('处方已开具')
    showPrescriptionDialog.value = false
    rxForm.value = { medicineId: '', medicineName: '', spec: '', usage: '', num: 1, price: 0 }
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

async function sendChatQuestion() {
  const message = chatInput.value.trim()
  if (!message) return
  chatInput.value = ''
  await sendAiMessage(message)
}

async function sendQuickAction(action: AiQuickAction) {
  if (action.actionType === 'PRESCRIPTION_REVIEW' && !validatePrescriptionReviewInput()) {
    return
  }
  await sendAiMessage(action.prompt, action)
}

function handleMoreAction(actionType: string) {
  const action = aiQuickActions.find(item => item.actionType === actionType)
  if (action) sendQuickAction(action)
}

async function sendAiMessage(message: string, action?: AiQuickAction) {
  if (isCompleted.value) {
    ElMessage.warning('接诊已完成，不能继续使用 AI 辅助接诊')
    return
  }
  const displayText = action ? action.label : message
  chatMessages.value.push({ id: Date.now() + Math.random(), role: 'doctor', content: displayText })
  aiLoading.value = true
  try {
    const res = await assistantChat(buildAiRequest(message, action))
    const response = res.data
    chatMessages.value.push({
      id: Date.now() + Math.random(),
      role: 'assistant',
      content: response.answer || 'AI 已返回处理结果',
      response
    })
  } catch (e: any) {
    showActionError(e, 'AI 辅助接诊失败')
    chatMessages.value.push({
      id: Date.now() + Math.random(),
      role: 'assistant',
      content: 'AI 请求失败，请稍后重试。'
    })
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
  if (recordSections.value.auxiliaryExam.includes(section)) {
    ElMessage.info('报告分析已在病历草稿中')
    return
  }
  recordSections.value.auxiliaryExam = [recordSections.value.auxiliaryExam.trim(), section].filter(Boolean).join('\n\n')
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

async function loadMedicineOptions() {
  if (medicineLoading.value || medicineOptions.value.length) return
  medicineLoading.value = true
  try {
    const res = await getMedicineList()
    medicineOptions.value = res.data || []
  } catch (e: any) {
    showActionError(e, '药品列表加载失败')
  } finally {
    medicineLoading.value = false
  }
}

function loadMedicinesOnOpen(open: boolean) {
  if (open) loadMedicineOptions()
}

function handleMedicineChange(medicineId: string) {
  const medicine = medicineOptions.value.find(item => item.medicineId === medicineId)
  if (!medicine) {
    rxForm.value.medicineName = ''
    return
  }
  rxForm.value.medicineName = medicine.name
  rxForm.value.spec = medicine.spec || ''
  rxForm.value.usage = medicine.usage || ''
  rxForm.value.price = Number(medicine.price || 0)
}

function validatePrescriptionReviewInput() {
  if (!rxForm.value.medicineId) {
    ElMessage.warning('AI处方审查需要先选择药品')
    showPrescriptionDialog.value = true
    loadMedicineOptions()
    return false
  }
  if (!rxForm.value.usage.trim()) {
    ElMessage.warning('AI处方审查需要填写用法用量')
    showPrescriptionDialog.value = true
    return false
  }
  return true
}

function buildAiRequest(message: string, action?: AiQuickAction) {
  const structuredParameters = buildStructuredParameters()
  const shouldSendMedicines = action?.actionType === 'PRESCRIPTION_REVIEW' || isPrescriptionReviewQuestion(message)
  return {
    registerId,
    actionType: action?.actionType,
    message,
    currentRecordDesc: recordDesc.value,
    symptomDescription: recordSections.value.chiefComplaint || detail.value.chiefComplaint || '',
    conversationText: buildConversationText(message),
    structuredParameters,
    patientInformation: buildPatientInformation(),
    medicines: shouldSendMedicines && hasPrescriptionReviewInput()
      ? [{ medicineId: rxForm.value.medicineId, usage: rxForm.value.usage, quantity: rxForm.value.num }]
      : undefined
  }
}

function hasPrescriptionReviewInput() {
  return Boolean(rxForm.value.medicineId && rxForm.value.usage.trim() && rxForm.value.num)
}

function isPrescriptionReviewQuestion(message: string) {
  return /处方|用药|药品|药物|审方|审查|禁忌|相互作用|风险/.test(message)
}

function buildStructuredParameters() {
  return {
    chiefComplaint: recordSections.value.chiefComplaint || detail.value.chiefComplaint || '',
    presentHistory: recordSections.value.presentHistory,
    pastHistory: recordSections.value.pastHistory,
    physicalExam: recordSections.value.physicalExam,
    auxiliaryExam: recordSections.value.auxiliaryExam,
    diagnosis: recordSections.value.diagnosis,
    treatmentPlan: recordSections.value.treatmentPlan
  }
}

function buildPatientInformation() {
  return {
    name: patientName.value,
    gender: genderLabel(detail.value.gender),
    age: String(detail.value.patientAge || ''),
    department: detail.value.department || '',
    chiefComplaint: detail.value.chiefComplaint || ''
  }
}

function buildConversationText(message: string) {
  const recentMessages = chatMessages.value
    .slice(-6)
    .map(item => `${item.role === 'doctor' ? '医生' : 'AI'}：${item.content}`)
  return [
    `本次问题：${message}`,
    `患者主诉：${detail.value.chiefComplaint || recordSections.value.chiefComplaint || '未填写'}`,
    `当前病历：\n${recordDesc.value || '未填写'}`,
    recentMessages.length ? `近期对话：\n${recentMessages.join('\n')}` : ''
  ].filter(Boolean).join('\n\n')
}

function normalizeExamRecommendResponse(data: AiExamRecommendResponse | AiExamRecommendationItem[] | undefined) {
  if (Array.isArray(data)) {
    return {
      summary: '',
      items: data.map(normalizeExamRecommendation).filter((item): item is NormalizedExamRecommendation => Boolean(item))
    }
  }
  const rawItems = data?.recommendations || data?.items || data?.examItems || []
  return {
    summary: data?.summary || '',
    items: rawItems.map(normalizeExamRecommendation).filter((item): item is NormalizedExamRecommendation => Boolean(item))
  }
}

function normalizeExamRecommendation(item: AiExamRecommendationItem): NormalizedExamRecommendation | null {
  const itemName = String(item.itemName || item.name || '').trim()
  if (!itemName) return null
  const urgency = String(item.urgencyLevel || item.urgency || 'NORMAL').trim().toUpperCase()
  return {
    id: Date.now() + Math.random(),
    itemName,
    category: String(item.itemCategory || item.category || 'EXAM').toUpperCase(),
    dept: String(item.dept || item.deptName || item.departmentName || '').trim(),
    urgencyLevel: ['NORMAL', 'URGENT', 'EMERGENCY'].includes(urgency) ? urgency : 'NORMAL',
    reason: String(item.reason || item.purpose || '').trim(),
    selected: true
  }
}

function applyAiRecordDraft(draft: string) {
  recordSections.value = parseRecordDesc(draft)
  ElMessage.success('AI病历草稿已应用，请确认后保存')
}

function draftRecordFromResponse(response?: AiAssistantChatResponse) {
  const result = response?.moduleResult
  if (!result || typeof result !== 'object') return ''
  const draft = (result as { draftRecordDesc?: unknown }).draftRecordDesc
  return typeof draft === 'string' ? draft : ''
}

function moduleResultLines(result: unknown) {
  if (!result || typeof result !== 'object') return []
  const data = result as Record<string, any>
  const lines: string[] = []
  if (typeof data.informationCompleteness === 'string') lines.push(`完整度：${data.informationCompleteness}`)
  if (typeof data.riskLevel === 'string') lines.push(`风险等级：${data.riskLevel}`)
  if (typeof data.summary === 'string') lines.push(`摘要：${data.summary}`)
  if (typeof data.overallRiskLevel === 'string') lines.push(`用药风险：${data.overallRiskLevel}`)
  if (typeof data.passed === 'boolean') lines.push(`审查结果：${data.passed ? '通过' : '需关注'}`)
  if (Array.isArray(data.missingInformation) && data.missingInformation.length) lines.push(`缺失信息：${data.missingInformation.join('、')}`)
  if (Array.isArray(data.recommendations) && data.recommendations.length) lines.push(`建议：${data.recommendations.join('；')}`)
  return lines.slice(0, 5)
}

function parseRecordDesc(value: string): Record<RecordSectionKey, string> {
  const sections = emptyRecordSections()
  const text = value.trim()
  if (!text) return sections

  const labelToKey = new Map(recordSectionDefs.map(item => [item.label, item.key]))
  const headingPattern = /(?:^|\n)(?:【)?(主诉|现病史|既往史|体格检查|辅助检查|诊断意见|处理计划)(?:】)?[：:\n]/g
  const matches = Array.from(text.matchAll(headingPattern))
  if (!matches.length) {
    sections.presentHistory = text
    return sections
  }

  matches.forEach((match, index) => {
    const label = match[1] || ''
    const key = labelToKey.get(label)
    if (!key) return
    const contentStart = (match.index || 0) + match[0].length
    const nextMatch = matches[index + 1]
    const contentEnd = nextMatch ? nextMatch.index || text.length : text.length
    sections[key] = text.slice(contentStart, contentEnd).trim()
  })
  return sections
}

function serializeRecordDesc(sections: Record<RecordSectionKey, string>) {
  return recordSectionDefs
    .map(section => {
      const value = sections[section.key]?.trim()
      return value ? `${section.label}：\n${value}` : ''
    })
    .filter(Boolean)
    .join('\n\n')
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

function urgencyLabel(value: string) {
  const labels: Record<string, string> = {
    NORMAL: '常规',
    URGENT: '加急',
    EMERGENCY: '紧急'
  }
  return labels[value] || '常规'
}

function urgencyTag(value: string) {
  if (value === 'EMERGENCY') return 'danger'
  if (value === 'URGENT') return 'warning'
  return 'info'
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
.page { padding: 24px 28px 32px; background: #f4f7fb; min-height: calc(100vh - 64px); }
.detail-head { display: flex; align-items: center; gap: 16px; margin-bottom: 18px; }
.detail-id { font-size: 14px; color: #64748b; font-weight: 600; }
.workspace-grid { display: grid; grid-template-columns: minmax(0, 1fr) 420px; gap: 22px; align-items: start; }
.main-flow { display: flex; flex-direction: column; gap: 18px; }
.card { background: #fff; border: 1px solid #e3eaf3; border-radius: 18px; padding: 24px; box-shadow: 0 18px 42px rgba(28, 44, 68, .08); }
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
.record-section-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 14px; margin-bottom: 16px; }
.record-section:first-child { grid-column: 1 / -1; }
.record-section-head { display: flex; align-items: baseline; justify-content: space-between; gap: 10px; margin-bottom: 8px; color: #102033; font-size: 14px; font-weight: 800; }
.record-section.required .record-section-head span::after { content: '*'; color: #ef4444; margin-left: 4px; font-weight: 900; }
.record-section-head small { color: #94a3b8; font-size: 12px; font-weight: 600; }
.editor { width: 100%; }
.editor :deep(.el-textarea__inner) { border-radius: 13px; border-color: #dbe3ee; box-shadow: 0 1px 0 rgba(15, 23, 42, .02); transition: border-color .18s ease, box-shadow .18s ease; }
.editor :deep(.el-textarea__inner:focus) { border-color: #4f8df7; box-shadow: 0 0 0 3px rgba(79, 141, 247, .14); }
.actions { display: grid; grid-template-columns: repeat(6, minmax(0, 1fr)); gap: 10px; padding-top: 6px; }
.actions :deep(.el-button) {
  width: 100%;
  min-width: 0;
  min-height: 52px;
  margin-left: 0 !important;
  padding: 8px 10px;
  border-radius: 15px;
  font-size: clamp(12px, .92vw, 15px);
  font-weight: 800;
  line-height: 1;
  white-space: nowrap;
  text-align: center;
  box-shadow: 0 10px 22px rgba(27, 42, 64, .08);
}
.actions :deep(.el-button > span) { justify-content: center; white-space: nowrap; }
.actions :deep(.el-button--success) { --el-button-bg-color: #4f9f45; --el-button-border-color: #4f9f45; --el-button-hover-bg-color: #438d3a; --el-button-hover-border-color: #438d3a; }
.actions :deep(.el-button--warning) { --el-button-bg-color: #d99a34; --el-button-border-color: #d99a34; --el-button-hover-bg-color: #c78824; --el-button-hover-border-color: #c78824; }
.ai-exam-card { border-color: #cfe0f7; background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%); }
.ai-exam-head { display: flex; align-items: baseline; justify-content: space-between; gap: 12px; }
.ai-exam-head small { color: #94a3b8; font-size: 12px; font-weight: 700; }
.ai-exam-summary { margin: -4px 0 14px; color: #475569; font-size: 13px; line-height: 1.7; }
.ai-exam-list { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; }
.ai-exam-recommendation { display: grid; grid-template-columns: 24px minmax(0, 1fr); gap: 10px; padding: 14px; border: 1px solid #dbe7f6; border-radius: 14px; background: #fff; cursor: pointer; transition: border-color .18s ease, box-shadow .18s ease, transform .18s ease; }
.ai-exam-recommendation:hover { border-color: #91b9f6; box-shadow: 0 12px 26px rgba(49, 95, 187, .12); transform: translateY(-1px); }
.ai-exam-recommendation.selected { border-color: #4f8df7; background: #f8fbff; }
.ai-exam-content { min-width: 0; }
.ai-exam-title-line { display: flex; align-items: center; flex-wrap: wrap; gap: 8px; color: #102033; }
.ai-exam-title-line strong { font-size: 14px; }
.ai-exam-content p { margin: 8px 0 6px; color: #475569; font-size: 13px; line-height: 1.65; }
.ai-exam-content small { color: #94a3b8; font-size: 12px; font-weight: 700; }
.ai-exam-actions { display: flex; justify-content: flex-end; gap: 10px; margin-top: 14px; }
.exam-order-toolbar { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-bottom: 14px; }
.exam-order-table { border: 1px solid #edf2f7; border-radius: 8px; overflow: hidden; }
.exam-order-head,
.exam-order-row { display: grid; grid-template-columns: minmax(220px, 1.4fr) minmax(160px, 1fr) 72px; gap: 12px; align-items: center; padding: 12px 14px; }
.exam-order-head { background: #f8fafc; color: #64748b; font-size: 12px; font-weight: 800; }
.exam-order-row + .exam-order-row { border-top: 1px solid #edf2f7; }
.exam-order-actions,
.prescription-actions { display: flex; justify-content: flex-end; gap: 10px; margin-top: 14px; }
.medicine-option { display: flex; justify-content: space-between; gap: 12px; }
.medicine-option small { color: #64748b; }
.ai-sidebar { position: sticky; top: 18px; display: flex; flex-direction: column; min-height: calc(100vh - 104px); max-height: calc(100vh - 36px); background: #fff; border: 1px solid #d7e6fa; border-radius: 18px; overflow: hidden; box-shadow: 0 22px 52px rgba(36, 73, 123, .14); }
.ai-head { display: flex; align-items: center; gap: 12px; padding: 18px 20px; color: #fff; background: linear-gradient(135deg, #315fbb, #3f87dc); }
.ai-badge { width: 38px; height: 38px; border-radius: 50%; background: rgba(255,255,255,.16); display: flex; align-items: center; justify-content: center; }
.ai-title { font-size: 17px; font-weight: 800; }
.ai-subtitle { font-size: 12px; opacity: .86; margin-top: 2px; }
.ai-tabs { display: grid; grid-template-columns: 1fr 1fr; padding: 12px; gap: 8px; background: #f8fafc; border-bottom: 1px solid #edf2f7; }
.ai-tabs button { border: 0; border-radius: 13px; padding: 10px 8px; background: transparent; color: #66758a; font-weight: 800; cursor: pointer; }
.ai-tabs button.active { background: #315fbb; color: #fff; box-shadow: 0 10px 20px rgba(49, 95, 187, .18); }
.chat-list { display: flex; flex: 1 1 auto; flex-direction: column; gap: 12px; min-height: 360px; overflow: auto; padding: 18px; background: #f7faff; }
.chat-empty { padding: 18px; border: 1px solid #dbeafe; border-radius: 14px; background: #fff; color: #475569; line-height: 1.7; }
.chat-empty p { margin: 0; font-size: 13px; }
.empty-title { font-size: 15px; font-weight: 800; color: #102033; margin-bottom: 6px; }
.chat-message { display: flex; }
.chat-message.is-doctor { justify-content: flex-end; }
.chat-message.is-assistant { justify-content: flex-start; }
.bubble { max-width: 88%; border: 1px solid #dbeafe; border-radius: 12px; padding: 11px 12px; background: #fff; color: #334155; line-height: 1.65; font-size: 13px; }
.is-doctor .bubble { background: #315fbb; color: #fff; border-color: #315fbb; }
.bubble p { margin: 0; white-space: pre-wrap; }
.bubble-meta { display: flex; align-items: center; justify-content: space-between; gap: 8px; margin-bottom: 6px; font-size: 12px; font-weight: 800; opacity: .82; }
.module-result { margin-top: 8px; padding-top: 8px; border-top: 1px solid #e2e8f0; color: #475569; }
.module-result p + p { margin-top: 4px; }
.chat-input-shell {
  margin: 14px 18px 18px;
  padding: 14px;
  border: 1px solid #e2e8f0;
  border-radius: 24px;
  background: #fff;
  box-shadow: 0 14px 34px rgba(36, 73, 123, .12);
}
.chat-input-shell :deep(.el-textarea__inner) {
  min-height: 68px !important;
  padding: 4px 6px 10px;
  border: 0;
  border-radius: 0;
  box-shadow: none;
  resize: none;
  color: #102033;
  line-height: 1.6;
}
.chat-input-shell :deep(.el-textarea__inner::placeholder) { color: #94a3b8; }
.chat-input-shell :deep(.el-textarea__inner:focus) { box-shadow: none; }
.chat-input-tools { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding-top: 6px; border-top: 1px solid #f1f5f9; }
.chat-tool-left { display: flex; align-items: center; gap: 8px; min-width: 0; overflow-x: auto; scrollbar-width: none; }
.chat-tool-left::-webkit-scrollbar { display: none; }
.quick-action-chip {
  flex: 0 0 auto;
  height: 32px;
  margin-left: 0 !important;
  padding: 0 10px;
  border: 0;
  border-radius: 999px;
  background: #f8fafc;
  color: #334155;
  font-weight: 700;
}
.quick-action-chip:hover,
.quick-action-chip:focus { background: #edf4ff; color: #315fbb; }
.send-button {
  flex: 0 0 auto;
  width: 38px;
  height: 38px;
  border: 0;
  background: #315fbb;
  box-shadow: 0 8px 18px rgba(49, 95, 187, .22);
}
.send-button:hover,
.send-button:focus { background: #3f87dc; }
.result-card { border: 1px solid #dbeafe; border-radius: 8px; background: #f8fbff; padding: 14px; margin-bottom: 16px; }
.result-card.primary { border-color: #bfdbfe; }
.result-line { display: flex; align-items: center; justify-content: space-between; gap: 10px; margin-bottom: 8px; font-size: 13px; font-weight: 800; color: #2563eb; }
.result-card p { margin: 0; }
.ai-answer { white-space: pre-wrap; color: #334155; line-height: 1.7; font-size: 13px; }
.ai-section { margin-top: 16px; }
.ai-section-title { font-size: 14px; font-weight: 800; color: #102033; margin-bottom: 10px; }
.exam-item { display: grid; grid-template-columns: 28px minmax(0, 1fr); gap: 10px; align-items: start; padding: 10px 0; border-bottom: 1px solid #edf2f7; }
.exam-item:last-child { border-bottom: 0; }
.diag-index { width: 24px; height: 24px; border-radius: 6px; background: #f1f5f9; color: #475569; display: flex; align-items: center; justify-content: center; font-size: 12px; font-weight: 800; }
.exam-item strong { color: #102033; font-size: 13px; }
.exam-item p { margin: 4px 0 0; color: #64748b; font-size: 13px; line-height: 1.6; }
.ai-warning { margin-top: 16px; padding: 12px; border: 1px solid #fde68a; background: #fffdf3; color: #854d0e; border-radius: 8px; font-size: 13px; line-height: 1.6; }
.wide-btn { width: 100%; }
.report-form {
  margin: 18px;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  border: 1px solid #dbe7f6;
  border-radius: 18px;
  background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
  box-shadow: 0 14px 32px rgba(36, 73, 123, .08);
}
.report-type { width: 100%; }
.report-type :deep(.el-select__wrapper) {
  min-height: 46px;
  border-radius: 13px;
  box-shadow: 0 0 0 1px #d9e4f2 inset;
}
.report-type :deep(.el-select__wrapper.is-focused) {
  box-shadow: 0 0 0 3px rgba(79, 141, 247, .14), 0 0 0 1px #4f8df7 inset;
}
.report-textarea :deep(.el-textarea__inner) {
  min-height: 172px !important;
  border-radius: 14px;
  border-color: #d9e4f2;
  background: #fff;
  box-shadow: 0 1px 0 rgba(15, 23, 42, .02);
  line-height: 1.7;
}
.report-textarea :deep(.el-textarea__inner:focus) {
  border-color: #4f8df7;
  box-shadow: 0 0 0 3px rgba(79, 141, 247, .14);
}
.report-form .wide-btn {
  height: 44px;
  border: 0;
  border-radius: 13px;
  font-weight: 800;
  background: #4f8df7;
  box-shadow: 0 12px 24px rgba(79, 141, 247, .22);
}
.report-form .wide-btn:hover,
.report-form .wide-btn:focus { background: #315fbb; }
.report-result { margin: 0 18px 18px; padding: 14px; border: 1px solid #dbe7f6; border-radius: 18px; background: #fff; }
.report-result .result-card { border-radius: 14px; }
.report-apply-btn { margin-top: 14px; width: 100%; height: 40px; border-radius: 12px; font-weight: 800; }
.suggestion-item { display: grid; grid-template-columns: 28px minmax(0, 1fr); gap: 10px; align-items: center; padding: 10px 0; border-bottom: 1px solid #edf2f7; color: #475569; font-size: 13px; line-height: 1.6; }
.suggestion-item:last-child { border-bottom: 0; }
@media (max-width: 1180px) {
  .workspace-grid { grid-template-columns: 1fr; }
  .ai-sidebar { position: static; }
}
@media (max-width: 760px) {
  .page { padding: 18px; }
  .info-row,
  .record-section-grid,
  .ai-exam-list { grid-template-columns: 1fr; }
  .workspace-grid { gap: 16px; }
}
@media (max-width: 520px) {
  .actions { grid-template-columns: 1fr; }
}
</style>

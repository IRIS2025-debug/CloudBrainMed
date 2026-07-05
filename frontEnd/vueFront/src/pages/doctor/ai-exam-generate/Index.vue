<template>
  <div class="page">
    <!-- ==================== 页面头部 ==================== -->
    <header class="page-top">
      <div class="page-top-left">
        <div class="page-icon">
          <el-icon :size="22"><Checked /></el-icon>
        </div>
        <div>
          <h2>AI 检查检验生成</h2>
          <p class="top-sub">AI Agent 根据病历信息智能推荐检查项目，医生确认后生成检查单</p>
        </div>
      </div>
      <el-button text @click="$router.back()" class="back-btn">
        <el-icon><ArrowLeft /></el-icon> 返回接诊
      </el-button>
    </header>

    <!-- ==================== 步骤指示器 ==================== -->
    <div class="steps-bar" v-if="stepFlow.length">
      <div
        v-for="(step, i) in stepFlow"
        :key="i"
        class="step"
        :class="{ done: step.done, active: step.active }"
      >
        <div class="step-circle">
          <el-icon v-if="step.done" :size="14"><Check /></el-icon>
          <span v-else>{{ i + 1 }}</span>
        </div>
        <div class="step-label">{{ step.label }}</div>
        <div class="step-line" v-if="i < stepFlow.length - 1"></div>
      </div>
    </div>

    <!-- ==================== 主体内容区 ==================== -->
    <div class="main-grid">
      <!-- ========== 左侧：病历信息 ========== -->
      <div class="left-col">
        <div class="card patient-card">
          <div class="card-head">
            <el-icon class="card-head-icon" :size="16"><User /></el-icon>
            患者信息
          </div>
          <div class="patient-brief">
            <div class="pb-avatar">{{ (detail.name || detail.patientName)?.charAt(0) || '?' }}</div>
            <div class="pb-info">
              <div class="pb-name">{{ detail.name || detail.patientName || '--' }}</div>
              <div class="pb-meta">
                {{ detail.gender === 1 ? '男' : '女' }} · {{ detail.patientAge }}岁 · {{ detail.department }}
              </div>
            </div>
            <span class="pb-status" :class="'st-' + detail.consultStatus">
              {{ statusLabel(detail.consultStatus) }}
            </span>
          </div>
          <div class="info-row">
            <div class="info-item">
              <span class="ii-label">主诉</span>
              <span class="ii-value">{{ detail.chiefComplaint || '--' }}</span>
            </div>
            <div class="info-item">
              <span class="ii-label">就诊时间</span>
              <span class="ii-value">{{ detail.visitDate }} {{ detail.consultTime }}</span>
            </div>
            <div class="info-item">
              <span class="ii-label">挂号 ID</span>
              <span class="ii-value">{{ registerId }}</span>
            </div>
          </div>
        </div>

        <div class="card record-card">
          <div class="card-head">
            <el-icon class="card-head-icon" :size="16"><Document /></el-icon>
            病历内容
          </div>
          <div class="record-content">
            {{ detail.description || '暂无病历内容' }}
          </div>
        </div>
      </div>

      <!-- ========== 右侧：AI 检查建议 ========== -->
      <div class="right-col">
        <div class="card ai-card">
          <div class="card-head">
            <div class="card-head-left">
              <el-icon class="card-head-icon ai-icon" :size="16"><MagicStick /></el-icon>
              AI 检查检验建议
            </div>
            <el-tag v-if="examResult" :type="urgencyTagType(examResult.urgencyLevel)" size="small" effect="dark">
              {{ urgencyLabel(examResult.urgencyLevel) }}
            </el-tag>
          </div>

          <!-- 空状态 -->
          <div class="empty-state" v-if="!examResult && !generating">
            <div class="empty-icon-wrap">
              <el-icon :size="40"><MagicStick /></el-icon>
            </div>
            <p class="empty-title">AI 智能检查推荐</p>
            <p class="empty-desc">点击下方"生成AI检查建议"按钮，Agent 将根据病历内容<br/>智能分析并推荐最合适的检查检验项目</p>
          </div>

          <!-- 加载中 -->
          <div class="loading-state" v-if="generating">
            <div class="loading-pulse">
              <span></span><span></span><span></span>
            </div>
            <p>AI Agent 正在分析病历...</p>
            <span class="loading-sub">Planner → RAG检索 → LLM推理 → 结构化输出</span>
          </div>

          <!-- 生成结果 -->
          <div v-if="examResult" class="exam-result">
            <!-- AI 临床总结 -->
            <div class="ai-summary">
              <div class="ai-section-title">
                <span class="ai-dot"></span> 临床总结
              </div>
              <p>{{ examResult.clinicalSummary }}</p>
            </div>

            <!-- 检查项目列表 -->
            <div class="check-list">
              <div class="ai-section-title">
                <span class="ai-dot"></span> 建议检查项目
                <span class="check-count">{{ selectedChecks.length }}/{{ examResult.checkItems?.length || 0 }} 项已选</span>
              </div>
              <el-checkbox-group v-model="selectedChecks">
                <div
                  v-for="item in examResult.checkItems"
                  :key="item.itemName"
                  class="check-item"
                  :class="{ selected: selectedChecks.includes(item.itemName) }"
                  @click="selectedChecks.includes(item.itemName)
                    ? selectedChecks = selectedChecks.filter(c => c !== item.itemName)
                    : selectedChecks.push(item.itemName)"
                >
                  <el-checkbox :label="item.itemName" :value="item.itemName" class="check-checkbox" />
                  <span class="check-name">{{ item.itemName }}</span>
                </div>
              </el-checkbox-group>
            </div>

            <!-- 推理追踪 -->
            <div class="reasoning-section" v-if="examResult.reasoningTrace">
              <div class="reasoning-toggle" @click="reasoningExpanded = !reasoningExpanded">
                <span class="ai-section-title" style="margin-bottom:0">
                  <span class="ai-dot"></span> AI 推理追踪
                </span>
                <el-icon :class="{ rotated: reasoningExpanded }" class="toggle-icon"><ArrowDown /></el-icon>
              </div>
              <div class="reasoning-content" v-show="reasoningExpanded">
                <pre>{{ examResult.reasoningTrace }}</pre>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- ==================== 底部操作栏 ==================== -->
    <div class="bottom-bar">
      <div class="bottom-actions">
        <el-button
          type="primary"
          @click="handleGenerate"
          :loading="generating"
          size="large"
          class="action-btn"
        >
          <el-icon><MagicStick /></el-icon> 生成AI检查建议
        </el-button>
        <el-button
          type="success"
          @click="handleCreateExamOrder"
          :loading="creating"
          :disabled="!examResult || selectedChecks.length === 0 || examOrderCreated"
          size="large"
          class="action-btn"
        >
          <el-icon v-if="examOrderCreated"><Check /></el-icon>
          {{ examOrderCreated ? '检查单已生成' : '确认并生成检查单' }}
        </el-button>
        <el-button
          type="info"
          @click="handleComplete"
          :disabled="!examOrderCreated || detail.consultStatus === 'COMPLETED'"
          size="large"
          plain
          class="action-btn"
        >
          <el-icon><Finished /></el-icon>
          返回接诊工作台
        </el-button>
        <el-button
          type="primary"
          plain
          @click="router.push(`/doctor/consult/${registerId}`)"
          :disabled="!examOrderCreated"
          size="large"
          class="action-btn"
        >
          回到本次接诊
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, ArrowDown, Check, Checked, Document, Finished, MagicStick, User } from '@element-plus/icons-vue'
import { getConsultDetail, createExamOrder } from '@/api/doctor/consult'

const route = useRoute()
const router = useRouter()
const registerId = (route.query.registerId as string) || ''
const detail = ref<any>({})
const examResult = ref<any>(null)
const selectedChecks = ref<string[]>([])
const generating = ref(false)
const creating = ref(false)
const examOrderCreated = ref(false)
const reasoningExpanded = ref(false)

const stepFlow = computed(() => {
  const steps: { label: string; done: boolean; active: boolean }[] = []
  if (!examResult.value && !examOrderCreated.value) {
    steps.push({ label: 'AI 生成建议', done: false, active: true })
    steps.push({ label: '创建检查单', done: false, active: false })
    steps.push({ label: '返回接诊', done: false, active: false })
  } else if (examResult.value && !examOrderCreated.value) {
    steps.push({ label: 'AI 生成建议', done: true, active: false })
    steps.push({ label: '创建检查单', done: false, active: true })
    steps.push({ label: '返回接诊', done: false, active: false })
  } else if (examOrderCreated.value) {
    steps.push({ label: 'AI 生成建议', done: true, active: false })
    steps.push({ label: '创建检查单', done: true, active: false })
    steps.push({ label: '返回接诊', done: false, active: true })
  }
  return steps
})

onMounted(async () => {
  if (!registerId) {
    ElMessage.error('缺少挂号ID参数')
    return
  }
  try {
    const res = await getConsultDetail(registerId)
    detail.value = res.data
  } catch {
    ElMessage.error('获取病历信息失败')
  }
})

function statusLabel(status: string) {
  const map: Record<string, string> = {
    WAITING: '待接诊',
    IN_PROGRESS: '接诊中',
    RECORD_CONFIRMED: '病历已确认',
    COMPLETED: '已完成'
  }
  return map[status] || status
}

function urgencyLabel(level: string) {
  const map: Record<string, string> = { NORMAL: '常规', URGENT: '加急', EMERGENCY: '紧急' }
  return map[level] || level
}

function urgencyTagType(level: string) {
  if (level === 'EMERGENCY') return 'danger'
  if (level === 'URGENT') return 'warning'
  return 'info'
}

async function handleGenerate() {
  generating.value = true
  examResult.value = null
  examOrderCreated.value = false
  try {
    const { generateExamSuggestions } = await import('@/api/ai/examAgent')
    const res = await generateExamSuggestions({
      registerId,
      context: {
        patientId: detail.value.patientId || '',
        visitAge: detail.value.patientAge || 0,
        description: detail.value.description || ''
      }
    })
    examResult.value = res.data
    selectedChecks.value = (examResult.value.checkItems || [])
      .filter((item: any) => item.selected)
      .map((item: any) => item.itemName)
    ElMessage.success('AI 检查建议已生成')
  } catch {
    ElMessage.error('AI 生成失败，请重试')
  } finally {
    generating.value = false
  }
}

async function handleCreateExamOrder() {
  if (selectedChecks.value.length === 0) {
    ElMessage.warning('请至少选择一个检查项目')
    return
  }
  creating.value = true
  try {
    const checkItemList = JSON.stringify(
      selectedChecks.value.map(name => ({ itemName: name }))
    )
    await createExamOrder({
      registerId,
      checkItemList,
      urgencyLevel: examResult.value?.urgencyLevel || 'NORMAL'
    })
    examOrderCreated.value = true
    ElMessage.success('检查单已生成')
  } catch {
    ElMessage.error('检查单创建失败')
  } finally {
    creating.value = false
  }
}

function handleComplete() {
  router.push({ name: 'doctorConsult' })
}
</script>

<style scoped>
/* ========== 页面容器 ========== */
.page {
  padding: 28px 36px;
  min-height: 100vh;
}

/* ========== 页面头部 ========== */
.page-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
}
.page-top-left {
  display: flex;
  align-items: center;
  gap: 14px;
}
.page-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: linear-gradient(135deg, #dbeafe 0%, #bfdbfe 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #2563eb;
  flex-shrink: 0;
}
.page-top-left h2 {
  font-size: 20px;
  font-weight: 700;
  color: #0f172a;
  margin: 0 0 2px 0;
}
.top-sub {
  font-size: 13px;
  color: #94a3b8;
  margin: 0;
}
.back-btn {
  color: #64748b;
  font-size: 14px;
}

/* ========== 步骤条 ========== */
.steps-bar {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 24px;
  padding: 16px 24px;
  background: #fff;
  border-radius: var(--radius);
  box-shadow: var(--shadow);
}
.step {
  display: flex;
  align-items: center;
  gap: 0;
  position: relative;
}
.step-circle {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #f1f5f9;
  color: #94a3b8;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 700;
  transition: all 0.3s ease;
  flex-shrink: 0;
}
.step.done .step-circle {
  background: #dcfce7;
  color: #16a34a;
}
.step.active .step-circle {
  background: #2563eb;
  color: #fff;
  box-shadow: 0 0 0 4px rgba(37, 99, 235, 0.15);
}
.step-label {
  margin-left: 10px;
  font-size: 13px;
  font-weight: 600;
  color: #94a3b8;
  white-space: nowrap;
}
.step.done .step-label {
  color: #16a34a;
}
.step.active .step-label {
  color: #2563eb;
}
.step-line {
  width: 60px;
  height: 2px;
  background: #e2e8f0;
  margin: 0 16px;
  transition: background 0.3s ease;
}
.step.done .step-line {
  background: #16a34a;
}

/* ========== 主体网格 ========== */
.main-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  margin-bottom: 24px;
  height: calc(100vh - 320px);
  min-height: 500px;
}
.left-col {
  display: flex;
  flex-direction: column;
  gap: 20px;
  min-height: 0;
}
.right-col {
  display: flex;
  flex-direction: column;
  min-height: 0;
}

/* ========== 卡片 ========== */
.card {
  background: #fff;
  border-radius: var(--radius);
  padding: 24px;
  box-shadow: var(--shadow);
  transition: box-shadow 0.2s ease;
}
.card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}
.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 15px;
  font-weight: 700;
  color: #1e293b;
  margin-bottom: 18px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f1f5f9;
}
.card-head-left {
  display: flex;
  align-items: center;
  gap: 8px;
}
.card-head-icon {
  color: #64748b;
}
.card-head-icon.ai-icon {
  color: #2563eb;
}

/* AI 卡片特殊样式 */
.ai-card {
  border: 1px solid #e0e7ff;
  background: linear-gradient(180deg, #ffffff 0%, #f8faff 100%);
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.ai-card > .card-head {
  flex-shrink: 0;
}
.ai-card > .empty-state,
.ai-card > .loading-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}
.ai-card > .exam-result {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding-right: 4px;
}
.ai-card > .exam-result::-webkit-scrollbar {
  width: 5px;
}
.ai-card > .exam-result::-webkit-scrollbar-track {
  background: transparent;
}
.ai-card > .exam-result::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 10px;
}
.ai-card > .exam-result::-webkit-scrollbar-thumb:hover {
  background: #94a3b8;
}

/* ========== 病历卡片（左侧第二块，弹性填充） ========== */
.record-card {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 0;
}
.record-card > .card-head {
  flex-shrink: 0;
}
.record-card > .record-content {
  flex: 1;
  overflow-y: auto;
  min-height: 0;
}
.record-card > .record-content::-webkit-scrollbar {
  width: 5px;
}
.record-card > .record-content::-webkit-scrollbar-track {
  background: transparent;
}
.record-card > .record-content::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 10px;
}
.record-card > .record-content::-webkit-scrollbar-thumb:hover {
  background: #94a3b8;
}

/* ========== 患者信息 ========== */
.patient-card {
  flex-shrink: 0;
}
.patient-brief {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 20px;
}
.pb-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: linear-gradient(135deg, #2563eb 0%, #3b82f6 100%);
  color: #fff;
  font-size: 18px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 2px 8px rgba(37, 99, 235, 0.25);
}
.pb-name {
  font-size: 17px;
  font-weight: 700;
  color: #1e293b;
}
.pb-meta {
  font-size: 13px;
  color: #94a3b8;
  margin-top: 2px;
}
.pb-status {
  margin-left: auto;
  font-size: 12px;
  font-weight: 600;
  padding: 4px 12px;
  border-radius: 20px;
}
.st-WAITING { background: #fef3c7; color: #b45309; }
.st-IN_PROGRESS { background: #dbeafe; color: #1d4ed8; }
.st-RECORD_CONFIRMED { background: #d1fae5; color: #065f46; }
.st-COMPLETED { background: #f1f5f9; color: #64748b; }

.info-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}
.info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.ii-label {
  font-size: 12px;
  color: #94a3b8;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}
.ii-value {
  font-size: 14px;
  color: #334155;
}

/* ========== 病历内容 ========== */
.record-content {
  color: #334155;
  line-height: 1.9;
  min-height: 100px;
  white-space: pre-wrap;
  font-size: 14px;
}

/* ========== 空状态 ========== */
.empty-state {
  text-align: center;
  padding: 48px 20px;
}
.empty-icon-wrap {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 16px;
  color: #2563eb;
}
.empty-title {
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
  margin: 0 0 8px 0;
}
.empty-desc {
  font-size: 13px;
  color: #94a3b8;
  line-height: 1.7;
  margin: 0;
}

/* ========== 加载状态 ========== */
.loading-state {
  text-align: center;
  padding: 48px 20px;
}
.loading-pulse {
  display: flex;
  gap: 8px;
  justify-content: center;
  margin-bottom: 20px;
}
.loading-pulse span {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #2563eb;
  animation: pulse 1.4s ease-in-out infinite both;
}
.loading-pulse span:nth-child(1) { animation-delay: -0.32s; }
.loading-pulse span:nth-child(2) { animation-delay: -0.16s; }
.loading-pulse span:nth-child(3) { animation-delay: 0s; }
@keyframes pulse {
  0%, 80%, 100% { transform: scale(0.6); opacity: 0.4; }
  40% { transform: scale(1); opacity: 1; }
}
.loading-state p {
  font-size: 15px;
  font-weight: 600;
  color: #1e293b;
  margin: 0 0 8px 0;
}
.loading-sub {
  font-size: 12px;
  color: #94a3b8;
  font-family: 'SF Mono', 'Cascadia Code', 'Consolas', monospace;
}

/* ========== AI 结果 ========== */
.ai-section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 700;
  color: #1e293b;
  margin-bottom: 10px;
}
.ai-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #2563eb;
  flex-shrink: 0;
}
.ai-summary {
  margin-bottom: 20px;
  padding: 14px 16px;
  background: #f8fafc;
  border-radius: 10px;
  border: 1px solid #e2e8f0;
}
.ai-summary p {
  color: #334155;
  line-height: 1.8;
  margin: 0;
  font-size: 14px;
}

/* ========== 检查项目列表 ========== */
.check-list {
  margin-bottom: 16px;
}
.check-count {
  font-size: 12px;
  font-weight: 500;
  color: #94a3b8;
  margin-left: auto;
}
.check-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  margin-bottom: 6px;
  border-radius: 10px;
  border: 1px solid #e2e8f0;
  cursor: pointer;
  transition: all 0.15s ease;
  background: #fff;
}
.check-item:hover {
  border-color: #93c5fd;
  background: #f8faff;
}
.check-item.selected {
  border-color: #2563eb;
  background: #eff6ff;
}
.check-checkbox {
  pointer-events: none;
}
.check-name {
  font-size: 14px;
  font-weight: 500;
  color: #334155;
  user-select: none;
}
.check-item.selected .check-name {
  color: #1d4ed8;
  font-weight: 600;
}

/* ========== 推理追踪 ========== */
.reasoning-section {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #f1f5f9;
}
.reasoning-toggle {
  display: flex;
  align-items: center;
  justify-content: space-between;
  cursor: pointer;
  padding: 4px 0;
  user-select: none;
}
.toggle-icon {
  color: #64748b;
  transition: transform 0.2s ease;
}
.toggle-icon.rotated {
  transform: rotate(180deg);
}
.reasoning-content {
  margin-top: 10px;
  padding: 14px 16px;
  background: #f8fafc;
  border-radius: 10px;
  border: 1px solid #e2e8f0;
}
.reasoning-content pre {
  margin: 0;
  font-size: 12px;
  color: #475569;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-all;
  font-family: 'SF Mono', 'Cascadia Code', 'Consolas', monospace;
}

/* ========== 底部操作栏 ========== */
.bottom-bar {
  background: #fff;
  border-radius: var(--radius);
  padding: 20px 24px;
  box-shadow: 0 -1px 3px rgba(0, 0, 0, 0.04);
  position: sticky;
  bottom: 20px;
}
.bottom-actions {
  display: flex;
  gap: 14px;
  justify-content: center;
}
.action-btn {
  min-width: 180px;
  border-radius: 10px;
  font-weight: 600;
  font-size: 14px;
  height: 44px;
}

/* ========== 响应式 ========== */
@media (max-width: 1100px) {
  .main-grid {
    grid-template-columns: 1fr;
    height: auto;
    min-height: 0;
  }
  .left-col, .right-col {
    min-height: 400px;
  }
  .page {
    padding: 20px;
  }
  .steps-bar {
    overflow-x: auto;
    justify-content: flex-start;
  }
  .step-line {
    width: 32px;
    margin: 0 8px;
  }
  .bottom-actions {
    flex-wrap: wrap;
  }
  .action-btn {
    min-width: 0;
    flex: 1;
  }
}
</style>

<template>
  <div class="page">
    <div class="detail-head">
      <el-button text @click="$router.back()"><el-icon><ArrowLeft /></el-icon> 返回列表</el-button>
      <span class="detail-id">挂号 #{{ registerId }}</span>
    </div>

    <div class="detail-grid">
      <div class="card">
        <div class="card-head">患者信息</div>
        <div class="patient-brief">
          <div class="pb-avatar">{{ (detail.name || detail.patientName)?.charAt(0) || '?' }}</div>
          <div>
            <div class="pb-name">{{ detail.name || detail.patientName || '--' }}</div>
            <div class="pb-meta">{{ detail.gender === 1 ? '男' : '女' }} · {{ detail.patientAge }}岁 · {{ detail.department }}</div>
          </div>
          <span class="pb-status" :class="'st-' + detail.consultStatus">{{ statusLabel(detail.consultStatus) }}</span>
        </div>
        <div class="info-row">
          <div class="info-item"><span class="ii-label">主诉</span><span>{{ detail.chiefComplaint || '--' }}</span></div>
          <div class="info-item"><span class="ii-label">就诊时间</span><span>{{ detail.visitDate }} {{ detail.consultTime }}</span></div>
          <div class="info-item"><span class="ii-label">接诊房间</span><span>{{ detail.consultRoom || '--' }}</span></div>
        </div>
      </div>

      <div class="card">
        <div class="card-head">病历编辑</div>
        <el-input v-model="recordDesc" type="textarea" :rows="10" placeholder="请输入病历内容，包含病史、体格检查、诊断意见等..." class="editor" />
        <div class="actions">
          <el-button @click="handleSaveDraft" :loading="saving" size="large">暂存草稿</el-button>
          <el-button type="success" @click="handleConfirm" :loading="confirming" size="large">确认病历</el-button>
          <el-button-group size="large">
            <el-button type="warning" @click="handleCreateExam">
              <el-icon><MagicStick /></el-icon> AI 开具检查单
            </el-button>
            <el-dropdown trigger="click" @command="handleExamCommand">
              <el-button type="warning">
                <el-icon><ArrowDown /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="ai">
                    <el-icon><MagicStick /></el-icon> AI 智能推荐
                  </el-dropdown-item>
                  <el-dropdown-item command="manual">
                    <el-icon><Edit /></el-icon> 手动输入检查单
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </el-button-group>
          <el-button type="success" @click="handleCreatePrescription" size="large">开具处方</el-button>
          <el-button type="primary" @click="handleAiAnalyze" :loading="aiLoading" size="large">
            <el-icon><MagicStick /></el-icon> AI 分析
          </el-button>
          <el-button type="danger" @click="handleComplete" :loading="completing" size="large" plain>完成接诊</el-button>
        </div>

        <!-- AI 分析结果面板 -->
        <div class="ai-panel" v-if="aiResult">
          <div class="ai-panel-head">
            <span class="ai-panel-icon">🧠</span>
            <span>AI 辅助诊断分析</span>
            <el-button text size="small" @click="aiResult = null" style="margin-left:auto">收起</el-button>
          </div>

          <div class="ai-section" v-if="aiResult.diagnosis?.length">
            <div class="ai-section-title">【疑似诊断】</div>
            <div class="ai-diag-item" v-for="(d, i) in aiResult.diagnosis" :key="i">
              <span class="ai-diag-name">{{ d.name }}</span>
              <el-tag :type="d.probability === '高' ? 'danger' : d.probability === '中' ? 'warning' : 'info'" size="small">{{ d.probability }}概率</el-tag>
              <span class="ai-diag-basis">{{ d.basis }}</span>
            </div>
          </div>

          <div class="ai-section" v-if="aiResult.exams?.length">
            <div class="ai-section-title">【建议检查】</div>
            <div class="ai-exam-item" v-for="(e, i) in aiResult.exams" :key="i">
              <span class="ai-exam-name">{{ e.name }}</span>
              <el-tag :type="e.urgency === '紧急' ? 'danger' : 'info'" size="small">{{ e.urgency }}</el-tag>
              <span class="ai-exam-purpose">— {{ e.purpose }}</span>
            </div>
          </div>

          <div class="ai-section" v-if="aiResult.advice">
            <div class="ai-section-title">【临床建议】</div>
            <p class="ai-advice-text">{{ aiResult.advice }}</p>
          </div>

          <div class="ai-section ai-risk" v-if="aiResult.risk && aiResult.risk !== '暂无特殊风险'">
            <div class="ai-section-title">⚠️ 风险提示</div>
            <p class="ai-risk-text">{{ aiResult.risk }}</p>
          </div>

          <div class="ai-actions">
            <el-button type="success" @click="handleAiConfirm">✓ 确认采纳</el-button>
            <el-button @click="aiResult = null">✗ 驳回</el-button>
          </div>
        </div>
      </div>

      <div class="card" v-if="showExamDialog">
        <div class="card-head">检查申请单</div>
        <el-form label-position="top">
          <el-row :gutter="16">
            <el-col :span="16">
              <el-form-item label="检查项目"><el-input v-model="checkItemList" type="textarea" :rows="3" placeholder='[{"itemName":"血常规","dept":"检验科"}]' /></el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="紧急程度">
                <el-radio-group v-model="urgencyLevel">
                  <el-radio value="NORMAL">常规</el-radio>
                  <el-radio value="URGENT">加急</el-radio>
                  <el-radio value="EMERGENCY">紧急</el-radio>
                </el-radio-group>
              </el-form-item>
              <el-button type="primary" @click="submitExamOrder" :loading="examLoading" style="margin-top:12px">提交申请</el-button>
            </el-col>
          </el-row>
        </el-form>
      </div>

      <div class="card" v-if="showPrescriptionDialog">
        <div class="card-head">开具处方</div>
        <el-form label-position="top">
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="药品名称"><el-input v-model="rxForm.medicineName" placeholder="如：布洛芬缓释胶囊" /></el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="规格"><el-input v-model="rxForm.spec" placeholder="如：0.3g×20粒" /></el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="用法用量"><el-input v-model="rxForm.usage" placeholder="如：口服，每次1粒，每日2次" /></el-form-item>
            </el-col>
            <el-col :span="6">
              <el-form-item label="数量"><el-input-number v-model="rxForm.num" :min="1" style="width:100%" /></el-form-item>
            </el-col>
            <el-col :span="6">
              <el-form-item label="单价"><el-input-number v-model="rxForm.price" :min="0" :precision="2" style="width:100%" /></el-form-item>
            </el-col>
          </el-row>
          <el-button type="primary" @click="submitPrescription" :loading="rxLoading" style="margin-top:8px">提交处方</el-button>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowDown, ArrowLeft, Edit, MagicStick } from '@element-plus/icons-vue'
import { getConsultDetail, saveDraft, confirmRecord, createExamOrder, completeConsult, aiAnalyze, createPrescription } from '@/api/doctor/consult'

const route = useRoute()
const router = useRouter()
const registerId = route.params.registerId as string
const detail = ref<any>({})
const recordDesc = ref('')
const saving = ref(false); const confirming = ref(false); const completing = ref(false)
const examLoading = ref(false); const showExamDialog = ref(false)
const checkItemList = ref(''); const urgencyLevel = ref('NORMAL')
const aiLoading = ref(false); const aiResult = ref<any>(null)
// 处方
const showPrescriptionDialog = ref(false); const rxLoading = ref(false)
const rxForm = ref({ medicineName: '', spec: '', usage: '', num: 1, price: 0 })

onMounted(async () => {
  try { const res = await getConsultDetail(registerId); detail.value = res.data; recordDesc.value = res.data.description || '' } catch {}
})

async function handleSaveDraft() {
  saving.value = true
  try { await saveDraft({ registerId, recordDesc: recordDesc.value }); ElMessage.success('草稿已保存') } catch {} finally { saving.value = false }
}
async function handleConfirm() {
  confirming.value = true
  try { await confirmRecord({ registerId, recordDesc: recordDesc.value }); ElMessage.success('病历已确认'); detail.value.consultStatus = 'RECORD_CONFIRMED' } catch {} finally { confirming.value = false }
}
function handleCreateExam() {
  router.push({ path: '/doctor/ai-exam-generate', query: { registerId } })
}
function handleExamCommand(cmd: string) {
  if (cmd === 'ai') {
    router.push({ path: '/doctor/ai-exam-generate', query: { registerId } })
  } else {
    showExamDialog.value = true
    showPrescriptionDialog.value = false
  }
}
function handleCreatePrescription() { showPrescriptionDialog.value = true; showExamDialog.value = false }
async function submitPrescription() {
  if (!rxForm.value.medicineName.trim()) { ElMessage.warning('请输入药品名称'); return }
  rxLoading.value = true
  try {
    await createPrescription({ ...rxForm.value, registerId })
    ElMessage.success('处方已开具')
    showPrescriptionDialog.value = false
    rxForm.value = { medicineName: '', spec: '', usage: '', num: 1, price: 0 }
  } catch {} finally { rxLoading.value = false }
}
async function submitExamOrder() {
  examLoading.value = true
  try { await createExamOrder({ registerId, checkItemList: checkItemList.value, urgencyLevel: urgencyLevel.value }); ElMessage.success('检查申请已生成'); showExamDialog.value = false } catch {} finally { examLoading.value = false }
}
async function handleComplete() {
  try { await ElMessageBox.confirm('确定完成本次接诊？完成后不可修改。', '确认', { type: 'warning' }) } catch { return }
  completing.value = true
  try { await completeConsult(registerId); ElMessage.success('接诊已完成'); detail.value.consultStatus = 'COMPLETED' } catch {} finally { completing.value = false }
}
async function handleAiAnalyze() {
  if (!recordDesc.value.trim()) { ElMessage.warning('请先填写病历内容'); return }
  aiLoading.value = true; aiResult.value = null
  try {
    const res = await aiAnalyze({
      registerId,
      chiefComplaint: detail.value.chiefComplaint || '',
      recordDesc: recordDesc.value,
      patientAge: String(detail.value.patientAge || ''),
      patientGender: detail.value.gender === 1 ? '男' : '女'
    })
    aiResult.value = res.data
    ElMessage.success('AI 分析完成')
  } catch {} finally { aiLoading.value = false }
}
function handleAiConfirm() {
  ElMessage.success('AI 分析结果已采纳，即将跳转至检查建议页面')
  router.push({ path: '/doctor/ai-exam-generate', query: { registerId } })
}
function statusLabel(s: string) { const m: Record<string, string> = { PENDING: '待接诊', IN_PROGRESS: '接诊中', RECORD_CONFIRMED: '已确认', COMPLETED: '已完成' }; return m[s] || s }
</script>

<style scoped>
.page { padding: 28px 36px; }
.detail-head { display: flex; align-items: center; gap: 16px; margin-bottom: 24px; }
.detail-id { font-size: 14px; color: #94a3b8; font-weight: 500; }
.detail-grid { display: flex; flex-direction: column; gap: 20px; }
.card { background: #fff; border-radius: var(--radius); padding: 24px; box-shadow: var(--shadow); }
.card-head { font-size: 15px; font-weight: 700; color: #1e293b; margin-bottom: 18px; padding-bottom: 12px; border-bottom: 1px solid #f1f5f9; }

.patient-brief { display: flex; align-items: center; gap: 14px; margin-bottom: 20px; }
.pb-avatar { width: 48px; height: 48px; border-radius: 50%; background: #dbeafe; color: #1d4ed8; font-size: 18px; font-weight: 700; display: flex; align-items: center; justify-content: center; }
.pb-name { font-size: 17px; font-weight: 700; color: #1e293b; }
.pb-meta { font-size: 13px; color: #94a3b8; margin-top: 2px; }
.pb-status { margin-left: auto; font-size: 12px; font-weight: 600; padding: 4px 12px; border-radius: 12px; }
.st-PENDING { background: #fef3c7; color: #b45309; }
.st-IN_PROGRESS { background: #dbeafe; color: #1d4ed8; }
.st-RECORD_CONFIRMED { background: #d1fae5; color: #065f46; }
.st-COMPLETED { background: #f1f5f9; color: #64748b; }

.info-row { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; }
.info-item { display: flex; flex-direction: column; gap: 4px; font-size: 14px; color: #334155; }
.ii-label { font-size: 12px; color: #94a3b8; font-weight: 600; }

.editor { margin-bottom: 16px; }
.actions { display: flex; gap: 12px; flex-wrap: wrap; }

/* 检查单按钮组 */
.actions :deep(.el-button-group .el-button:first-child) {
  padding-right: 8px;
}
.actions :deep(.el-button-group .el-button:last-child) {
  padding-left: 6px;
  padding-right: 10px;
  border-left: 1px solid rgba(255, 255, 255, 0.3);
}

/* AI 分析面板 */
.ai-panel { margin-top: 20px; background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%); border: 1px solid #bae6fd; border-radius: 12px; padding: 24px; }
.ai-panel-head { display: flex; align-items: center; gap: 8px; font-size: 16px; font-weight: 700; color: #0369a1; margin-bottom: 20px; padding-bottom: 12px; border-bottom: 1px solid #bae6fd; }
.ai-panel-icon { font-size: 22px; }
.ai-section { margin-bottom: 16px; }
.ai-section-title { font-size: 14px; font-weight: 700; color: #0f172a; margin-bottom: 8px; }
.ai-diag-item { display: flex; align-items: center; gap: 10px; padding: 8px 0; border-bottom: 1px dashed #e2e8f0; }
.ai-diag-item:last-child { border-bottom: none; }
.ai-diag-name { font-size: 14px; font-weight: 600; color: #1e293b; min-width: 100px; }
.ai-diag-basis { font-size: 13px; color: #64748b; margin-left: 4px; }
.ai-exam-item { display: flex; align-items: center; gap: 10px; padding: 6px 0; }
.ai-exam-name { font-size: 14px; font-weight: 600; color: #1e293b; }
.ai-exam-purpose { font-size: 13px; color: #64748b; }
.ai-advice-text { font-size: 14px; color: #334155; line-height: 1.7; }
.ai-risk { background: #fef2f2; border-radius: 8px; padding: 12px 16px; }
.ai-risk-text { font-size: 14px; color: #dc2626; font-weight: 600; }
.ai-actions { display: flex; gap: 12px; margin-top: 20px; padding-top: 16px; border-top: 1px solid #bae6fd; }
</style>
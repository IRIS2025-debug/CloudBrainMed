<template>
  <div class="page">
    <div class="page-head">
      <el-button text @click="$router.back()"><el-icon><ArrowLeft /></el-icon> 返回</el-button>
      <div>
        <h2>AI检查检验建议</h2>
        <p>根据接诊病历生成可复核的检查/检验项目建议。</p>
      </div>
    </div>

    <div class="workspace">
      <section class="panel">
        <div class="panel-title">病历上下文</div>
        <el-form label-position="top">
          <el-form-item label="挂号ID">
            <el-input v-model="form.registerId" placeholder="请输入挂号ID" />
          </el-form-item>
          <el-form-item label="患者ID">
            <el-input v-model="form.context.patientId" placeholder="请输入患者ID" />
          </el-form-item>
          <el-form-item label="就诊年龄">
            <el-input-number v-model="form.context.visitAge" :min="0" :max="130" style="width: 100%" />
          </el-form-item>
          <el-form-item label="病历描述">
            <el-input
              v-model="form.context.description"
              type="textarea"
              :rows="10"
              resize="none"
              placeholder="粘贴主诉、现病史、体格检查、初步诊断等内容"
            />
          </el-form-item>
          <el-button type="primary" class="generate-btn" :loading="loading" @click="handleGenerate">
            生成建议
          </el-button>
        </el-form>
      </section>

      <section class="panel result-panel">
        <div class="panel-title">生成结果</div>
        <el-empty v-if="!result" description="填写病历上下文后生成检查建议" />
        <template v-else>
          <div class="summary-card">
            <div class="summary-meta">
              <span>追踪ID：{{ result.traceId || '--' }}</span>
              <el-tag :type="urgencyTag(result.urgencyLevel)" round>{{ urgencyLabel(result.urgencyLevel) }}</el-tag>
            </div>
            <p>{{ result.clinicalSummary || '暂无摘要' }}</p>
          </div>

          <div class="item-list">
            <label v-for="item in result.checkItems || []" :key="item.itemName" class="item-row">
              <el-checkbox v-model="item.selected" />
              <span>{{ item.itemName }}</span>
            </label>
          </div>

          <el-collapse v-if="result.reasoningTrace">
            <el-collapse-item title="查看推理追踪" name="trace">
              <pre>{{ result.reasoningTrace }}</pre>
            </el-collapse-item>
          </el-collapse>
        </template>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { generateExamSuggestions } from '@/api/ai/examAgent'

interface CheckItem {
  itemName: string
  selected: boolean
}

interface ExamGenerateResponse {
  traceId?: string
  clinicalSummary?: string
  checkItems?: CheckItem[]
  urgencyLevel?: string
  reasoningTrace?: string
}

const route = useRoute()
const loading = ref(false)
const result = ref<ExamGenerateResponse | null>(null)

const form = reactive({
  registerId: String(route.query.registerId || ''),
  context: {
    patientId: '',
    visitAge: 0,
    description: '',
  },
})

async function handleGenerate() {
  if (!form.registerId.trim()) {
    ElMessage.warning('请输入挂号ID')
    return
  }
  if (!form.context.patientId.trim()) {
    ElMessage.warning('请输入患者ID')
    return
  }
  if (!form.context.description.trim()) {
    ElMessage.warning('请输入病历描述')
    return
  }

  loading.value = true
  try {
    const res = await generateExamSuggestions({
      registerId: form.registerId.trim(),
      context: {
        patientId: form.context.patientId.trim(),
        visitAge: Number(form.context.visitAge || 0),
        description: form.context.description.trim(),
      },
    })
    result.value = res.data
    ElMessage.success('检查建议已生成')
  } finally {
    loading.value = false
  }
}

function urgencyLabel(value?: string) {
  const labels: Record<string, string> = {
    NORMAL: '常规',
    URGENT: '加急',
    EMERGENCY: '紧急',
  }
  return labels[value || 'NORMAL'] || '常规'
}

function urgencyTag(value?: string) {
  if (value === 'EMERGENCY') return 'danger'
  if (value === 'URGENT') return 'warning'
  return 'info'
}
</script>

<style scoped>
.page { min-height: calc(100vh - 64px); padding: 24px 28px 32px; background: #f4f7fb; }
.page-head { display: flex; align-items: center; gap: 18px; margin-bottom: 20px; }
.page-head h2 { margin: 0; color: #102033; font-size: 22px; }
.page-head p { margin: 4px 0 0; color: #64748b; font-size: 13px; }
.workspace { display: grid; grid-template-columns: 420px minmax(0, 1fr); gap: 18px; align-items: start; }
.panel { background: #fff; border: 1px solid #e3eaf3; border-radius: 8px; padding: 22px; box-shadow: 0 14px 34px rgba(28, 44, 68, .08); }
.panel-title { margin-bottom: 18px; padding-bottom: 12px; border-bottom: 1px solid #edf2f7; color: #102033; font-weight: 800; }
.generate-btn { width: 100%; height: 42px; }
.result-panel { min-height: 440px; }
.summary-card { padding: 16px; border: 1px solid #dbeafe; border-radius: 8px; background: #f8fbff; }
.summary-card p { margin: 10px 0 0; color: #334155; line-height: 1.7; }
.summary-meta { display: flex; align-items: center; justify-content: space-between; gap: 12px; color: #64748b; font-size: 13px; }
.item-list { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px; margin: 16px 0; }
.item-row { display: flex; align-items: center; gap: 10px; padding: 12px; border: 1px solid #e2e8f0; border-radius: 8px; color: #102033; }
pre { margin: 0; white-space: pre-wrap; color: #475569; line-height: 1.6; }
@media (max-width: 960px) {
  .workspace { grid-template-columns: 1fr; }
}
@media (max-width: 560px) {
  .page { padding: 18px; }
  .item-list { grid-template-columns: 1fr; }
}
</style>

<script setup lang="ts">
import { computed, onBeforeUnmount, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { UploadFilled, ArrowLeft } from '@element-plus/icons-vue'
import { analyzeCtReportInput, predictCtArtifact, predictCtLesion } from '@/api/examination/ct'
import { getStoredAuthHeaders } from '@/api/request'
import { buildDoctorCtAssetUrl, resolveBinaryFilename } from '@/pages/examination/ctAsset'
import { buildCtDatasetSummary, isCtDatasetFileName } from '@/pages/examination/ctDataset'
import { useExamReportStore } from '@/stores/examReport'

type CtTaskType = 'artifact' | 'lesion'

const route = useRoute()
const router = useRouter()
const examReportStore = useExamReportStore()
const uploading = ref(false)
const analyzing = ref(false)
const analysisResult = ref<any>(null)
const selectedFile = ref<File | null>(null)
const taskType = ref<CtTaskType>('artifact')
const results = reactive<Record<CtTaskType, any | null>>({
  artifact: null,
  lesion: null,
})
const result = computed<any | null>({
  get: () => results[taskType.value],
  set: (value) => {
    results[taskType.value] = value
  },
})
const registerId = ref(String(route.query.registerId || ''))
const fileInputRef = ref<HTMLInputElement | null>(null)
const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL || '').replace(/\/$/, '')
const taskContext = computed(() => ({
  orderItemId: String(route.query.orderItemId || ''),
  registerId: String(route.query.registerId || ''),
  itemName: String(route.query.itemName || ''),
}))

const taskMeta = {
  artifact: {
    title: '金属伪影识别',
    subtitle: 'CT Metal Artifact',
    description: '识别 CT 中的金属伪影区域，输出伪影 mask、预览图和像素占比。',
    endpoint: 'ct-artifact',
    reportType: 'CT_ARTIFACT_REPORT',
    positiveLabel: '伪影像素',
    ratioLabel: '伪影占比',
    detectedTitle: '检测到金属伪影候选区',
    cleanTitle: '未检测到明显金属伪影',
    overlayText: '红色区域为模型标记的金属伪影候选区',
    sliceLabel: '伪影切片',
    buttonText: '开始金属伪影识别',
  },
  lesion: {
    title: '病灶识别分割',
    subtitle: 'CT Lesion Segmentation',
    description: '分割 CT 中的病灶候选区域，输出病灶 mask、切片位置和结构化结果。',
    endpoint: 'ct-lesion',
    reportType: 'CT_LESION_REPORT',
    positiveLabel: '病灶像素',
    ratioLabel: '病灶占比',
    detectedTitle: '检测到病灶候选区',
    cleanTitle: '未检测到明显病灶候选区',
    overlayText: '红色区域为模型标记的病灶候选区',
    sliceLabel: '病灶切片',
    buttonText: '开始病灶识别分割',
  },
} as const

const currentMeta = computed(() => taskMeta[taskType.value])
const hasAnyModelResult = computed(() => Boolean(results.artifact || results.lesion))
const selectedDatasetSummary = computed(() => {
  return buildCtDatasetSummary(
    selectedFile.value ? { name: selectedFile.value.name, size: selectedFile.value.size } : null,
    result.value?.shapeText || '',
  )
})
const resultStatusType = computed(() => (result.value?.ratio || 0) > 0 ? 'warning' : 'success')
const resultStatusTitle = computed(() => {
  if (!result.value || result.value.ratio === undefined) return '推理已完成'
  return result.value.ratio > 0 ? result.value.meta.detectedTitle : result.value.meta.cleanTitle
})
const resultStatusDescription = computed(() => {
  if (!result.value || result.value.ratio === undefined) {
    return 'Python 推理服务已生成分割结果，当前结果未返回像素统计。'
  }
  if (result.value.ratio > 0) {
    return `在 ${formatNumber(result.value.totalPixels)} 个像素中检测到 ${formatNumber(result.value.positivePixels)} 个${result.value.meta.positiveLabel}。`
  }
  return '当前 CT 影像中未发现明显候选区域。'
})

function handleTaskChange() {
  analysisResult.value = null
}

function handleFileRemove() {
  clearAllResults()
  selectedFile.value = null
  analysisResult.value = null
}

function applySelectedFile(file: File | null) {
  if (!file) return
  if (!isCtDatasetFileName(file.name)) {
    ElMessage.warning('仅支持 .nii 或 .nii.gz CT 数据集')
    return
  }

  selectedFile.value = file
  clearAllResults()
  analysisResult.value = null
}

function openFilePicker() {
  fileInputRef.value?.click()
}

function handleNativeFileChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0] || null
  applySelectedFile(file)
  input.value = ''
}

function handleModelCardClick(type: CtTaskType) {
  if (uploading.value) return

  if (taskType.value !== type) {
    taskType.value = type
    handleTaskChange()
    return
  }

  openFilePicker()
}

function handleDatasetDragOver(event: DragEvent) {
  event.preventDefault()
}

function handleDatasetDrop(type: CtTaskType, event: DragEvent) {
  event.preventDefault()
  if (uploading.value) return

  if (taskType.value !== type) {
    taskType.value = type
    handleTaskChange()
  }

  applySelectedFile(event.dataTransfer?.files?.[0] || null)
}

function clearSelectedFile() {
  handleFileRemove()
  if (fileInputRef.value) {
    fileInputRef.value.value = ''
  }
}

async function runInference() {
  if (!selectedFile.value) {
    ElMessage.warning('请先选择 CT 数据集')
    return
  }

  uploading.value = true
  revokeResultObjectUrl(taskType.value)
  result.value = null
  analysisResult.value = null

  try {
    const response = taskType.value === 'lesion'
      ? await predictCtLesion(selectedFile.value)
      : await predictCtArtifact(selectedFile.value)
    const payload = (response as any)?.data?.status ? (response as any).data : response

    if (payload.success || payload.status === 'success') {
      result.value = normalizeResult(payload, taskType.value)
      await loadPreviewObjectUrl()
      ElMessage.success('推理完成')
      return
    }

    ElMessage.error(payload.error || payload.message || '推理失败')
  } catch (error: any) {
    ElMessage.error(error?.message || '推理服务暂不可用，请检查后端和 Python 推理服务')
  } finally {
    uploading.value = false
  }
}

function goBackToTask() {
  const orderItemId = taskContext.value.orderItemId
  if (orderItemId) {
    router.push(`/doctor/task/${orderItemId}`)
    return
  }
  router.push('/examination-doctor/application')
}

function syncResultToStore() {
  const orderItemId = taskContext.value.orderItemId
  if (!orderItemId || !hasAnyModelResult.value) return
  const images = (Object.entries(results) as Array<[CtTaskType, any]>)
    .filter(([, item]) => item?.previewImageUrl)
    .map(([type, item]) => ({
      name: item.previewImageFile || `${taskMeta[type].title}预览`,
      url: item.previewImageUrl,
    }))
  const structured: { artifact?: any; lesion?: any } = {}
  for (const [type, item] of Object.entries(results) as Array<[CtTaskType, any]>) {
    if (!item) continue
    structured[type] = {
      reportType: item.reportInput?.task || taskMeta[type].reportType,
      reportInput: item.reportInput,
      previewImageUrl: item.previewImageUrl || '',
      previewImageFile: item.previewImageFile || '',
      maskFile: item.maskFile || '',
      downloadUrl: item.downloadUrl || '',
      positivePixels: item.positivePixels,
      totalPixels: item.totalPixels,
      ratio: item.ratio,
      modelText: item.modelText,
      summaryText: item.summaryText,
      sliceText: item.sliceText,
    }
  }
  const firstPreview = results.artifact?.previewImageUrl || results.lesion?.previewImageUrl || ''
  examReportStore.mergeCtResult({
    orderItemId,
    registerId: registerId.value.trim() || taskContext.value.registerId,
    previewImageUrl: firstPreview,
    images,
    structured,
  })
}

function saveResultsToReport() {
  const orderItemId = taskContext.value.orderItemId
  if (!orderItemId) {
    ElMessage.warning('缺少任务明细ID，无法保存到报告单')
    return
  }
  if (!hasAnyModelResult.value) {
    ElMessage.warning('请先至少完成一项 CT 模型推理')
    return
  }
  syncResultToStore()
  ElMessage.success('CT 模型结果已保存到报告单')
}

function enterReportWorkspace() {
  const orderItemId = taskContext.value.orderItemId
  if (!orderItemId) {
    ElMessage.warning('缺少任务明细ID，无法进入报告页面')
    return
  }
  if (hasAnyModelResult.value) {
    syncResultToStore()
  }
  router.push({
    path: '/examination-doctor/report',
    query: {
      orderItemId,
      registerId: registerId.value.trim() || taskContext.value.registerId,
      itemName: taskContext.value.itemName,
    },
  })
}

async function runAiAnalysis() {
  if (!result.value?.reportInput) {
    ElMessage.warning('当前推理结果缺少标准化 JSON')
    return
  }
  if (!registerId.value.trim()) {
    ElMessage.warning('请输入挂号 ID')
    return
  }

  analyzing.value = true
  analysisResult.value = null
  try {
    const response = await analyzeCtReportInput({
      registerId: registerId.value.trim(),
      reportType: result.value.reportInput.task || currentMeta.value.reportType,
      reportInput: result.value.reportInput,
    })
    analysisResult.value = (response as any)?.data || response
    ElMessage.success('AI 分析完成')
  } catch (error: any) {
    ElMessage.error(error?.message || 'AI 分析暂不可用，请确认登录状态和挂号 ID')
  } finally {
    analyzing.value = false
  }
}

function normalizeResult(data: Record<string, any>, task: CtTaskType) {
  const meta = taskMeta[task]
  const positivePixels = task === 'lesion'
    ? readNumber(data, ['lesion_pixels', 'lesionPixels'])
    : readNumber(data, ['positive_pixels', 'positivePixels'])
  const totalPixels = readNumber(data, ['total_pixels', 'totalPixels'])
  const ratio = task === 'lesion'
    ? readNumber(data, ['ratio', 'lesion_ratio', 'lesionRatio'])
    : readNumber(data, ['ratio', 'artifact_ratio', 'artifactRatio'])
  const maskFile = data.mask_file || data.maskFile || ''
  const previewImageFile = data.preview_image_file || data.previewImageFile || ''
  const previewImageUrl = data.preview_image_url || data.previewImageUrl || previewImageFile
  const previewSliceIndex = readNumber(data, ['preview_slice_index', 'previewSliceIndex'])
  const sliceIndices = task === 'lesion'
    ? (data.lesion_slice_indices || data.lesionSliceIndices || [])
    : (data.artifact_slice_indices || data.artifactSliceIndices || [])
  const reportInput = data.report_input || data.reportInput

  return {
    ...data,
    task,
    meta,
    positivePixels,
    totalPixels,
    ratio: ratio ?? computeRatio(positivePixels, totalPixels),
    maskFile,
    previewImageFile,
    previewImageUrl: buildDoctorCtAssetUrl(apiBaseUrl, meta.endpoint, 'preview', previewImageUrl),
    previewImageObjectUrl: '',
    previewSliceIndex,
    sliceIndices,
    sliceText: formatSliceIndices(sliceIndices),
    reportInput,
    downloadUrl: buildDoctorCtAssetUrl(
      apiBaseUrl,
      meta.endpoint,
      'result',
      data.download_url || data.downloadUrl || maskFile,
    ),
    shapeText: formatArray(data.shape),
    spacingText: formatArray(data.spacing),
    modelText: [data.model_type || data.modelType, data.model_version || data.modelVersion]
      .filter(Boolean)
      .join(' / '),
    summaryText: data.summary || reportInput?.summary || '',
  }
}

function readNumber(data: Record<string, any>, keys: string[]) {
  for (const key of keys) {
    const value = data[key]
    if (value === null || value === undefined || value === '') continue
    const num = Number(value)
    if (Number.isFinite(num)) return num
  }
  return undefined
}

function computeRatio(positivePixels?: number, totalPixels?: number) {
  if (positivePixels === undefined || !totalPixels) return undefined
  return Number(((positivePixels / totalPixels) * 100).toFixed(2))
}

function formatArray(value: any) {
  return Array.isArray(value) ? value.join(' x ') : ''
}

function formatNumber(value?: number) {
  return value === undefined ? '--' : value.toLocaleString()
}

function formatRatio(value?: number) {
  return value === undefined ? '--' : `${value}%`
}

function formatSliceIndices(value: any) {
  return Array.isArray(value) && value.length ? value.join(', ') : ''
}

function revokeResultObjectUrl(type: CtTaskType) {
  const item = results[type]
  if (item?.previewImageObjectUrl) {
    URL.revokeObjectURL(item.previewImageObjectUrl)
    item.previewImageObjectUrl = ''
  }
}

function clearAllResults() {
  revokeResultObjectUrl('artifact')
  revokeResultObjectUrl('lesion')
  results.artifact = null
  results.lesion = null
}

async function fetchProtectedBlobUrl(url: string) {
  const response = await fetch(url, {
    headers: getStoredAuthHeaders(),
  })
  if (!response.ok) {
    throw new Error(`资源加载失败：${response.status}`)
  }
  return URL.createObjectURL(await response.blob())
}

async function loadPreviewObjectUrl() {
  revokeResultObjectUrl(taskType.value)
  if (!result.value?.previewImageUrl) return

  try {
    result.value.previewImageObjectUrl = await fetchProtectedBlobUrl(result.value.previewImageUrl)
  } catch (error: any) {
    result.value.previewImageObjectUrl = ''
    ElMessage.warning(error?.message || '预览图加载失败')
  }
}

async function downloadProtectedResult() {
  if (!result.value?.downloadUrl) return
  let objectUrl = ''
  try {
    objectUrl = await fetchProtectedBlobUrl(result.value.downloadUrl)
    const link = document.createElement('a')
    link.href = objectUrl
    link.download = resolveBinaryFilename(result.value.maskFile || result.value.downloadUrl, 'ct-result.bin')
    link.rel = 'noopener'
    link.click()
  } catch (error: any) {
    ElMessage.error(error?.message || '下载结果失败')
  } finally {
    if (objectUrl) {
      setTimeout(() => URL.revokeObjectURL(objectUrl), 0)
    }
  }
}

onBeforeUnmount(() => {
  clearAllResults()
})
</script>

<template>
  <div class="ct-workbench">
    <div class="page-top">
      <div class="page-top-left">
        <el-button class="back-btn" text :icon="ArrowLeft" @click="goBackToTask">返回任务</el-button>
        <p class="eyebrow">检查医生 · CV 模型</p>
        <h1>CT 双模型推理工作台</h1>
        <p class="page-desc">
          同一份 CT 数据集可按业务需要执行金属伪影识别或病灶识别分割，结果可继续进入 AI 报告分析。
        </p>
      </div>
      <div class="page-actions">
        <div v-if="taskContext.orderItemId" class="workflow-actions">
          <el-button
            type="primary"
            plain
            :disabled="!hasAnyModelResult"
            @click="saveResultsToReport"
          >
            保存结果到报告
          </el-button>
          <el-button type="primary" @click="enterReportWorkspace">
            进入报告单
          </el-button>
        </div>
        <div class="service-badge">
          <span></span>
          推理服务
        </div>
      </div>
    </div>

    <div v-if="taskContext.orderItemId" class="task-context">
      <span>当前任务</span>
      <strong>{{ taskContext.itemName || '检查项目' }}</strong>
      <em>明细 {{ taskContext.orderItemId }}</em>
      <em v-if="taskContext.registerId">挂号 {{ taskContext.registerId }}</em>
    </div>

    <div class="model-switch">
      <button
        v-for="(meta, type) in taskMeta"
        :key="type"
        class="model-option"
        :class="{ active: taskType === type, 'upload-ready': taskType === type && !!selectedDatasetSummary }"
        :disabled="uploading"
        type="button"
        @click="handleModelCardClick(type as CtTaskType)"
        @dragover="handleDatasetDragOver"
        @drop="handleDatasetDrop(type as CtTaskType, $event)"
      >
        <strong>{{ meta.title }}</strong>
        <span>{{ meta.subtitle }}</span>
        <div v-if="taskType === type" class="model-dataset">
          <template v-if="selectedDatasetSummary">
            <div class="dataset-head">
              <span class="dataset-badge">当前数据集</span>
              <button
                class="dataset-clear"
                type="button"
                :disabled="uploading"
                @click.stop="clearSelectedFile"
              >
                ×
              </button>
            </div>
            <div class="dataset-name">{{ selectedDatasetSummary.fileName }}</div>
            <div class="dataset-meta">
              <span>{{ selectedDatasetSummary.fileSize }}</span>
              <span v-if="selectedDatasetSummary.shapeText">{{ selectedDatasetSummary.shapeText }}</span>
            </div>
            <div class="dataset-hint">拖入新数据集可直接替换，或点击卡片重新选择</div>
          </template>
          <template v-else>
            <div class="model-dataset-placeholder">
              <el-icon><UploadFilled /></el-icon>
              <p>把 CT 数据集拖到这里，或点击选择</p>
              <small>支持 .nii / .nii.gz</small>
            </div>
          </template>
        </div>
      </button>
    </div>

    <div class="workspace-grid">
      <section class="panel input-panel">
        <div class="panel-head">
          <div>
            <h2>{{ currentMeta.title }}</h2>
            <p>{{ currentMeta.description }}</p>
          </div>
          <el-tag size="small" type="info">.nii / .nii.gz</el-tag>
        </div>

        <el-input
          v-model="registerId"
          placeholder="挂号 ID，用于把模型结果送入 AI 检查报告分析"
          :disabled="uploading || analyzing"
          clearable
          class="register-input"
        />

        <div class="upload-note">
          当前模型直接使用上方大卡片接收数据集，支持拖拽替换和清空后重新选择。
        </div>

        <el-button
          type="primary"
          size="large"
          :loading="uploading"
          :disabled="!selectedFile"
          class="run-button"
          @click="runInference"
        >
          {{ uploading ? '模型推理中...' : currentMeta.buttonText }}
        </el-button>

        <input
          ref="fileInputRef"
          class="hidden-file-input"
          type="file"
          accept=".nii,.nii.gz"
          @change="handleNativeFileChange"
        />
      </section>

      <section class="panel result-panel">
        <div class="panel-head">
          <div>
            <h2>模型输出</h2>
            <p>预览图、mask 文件、像素统计和结构化报告输入集中展示。</p>
          </div>
        </div>

        <div v-if="!result" class="empty-state">
          <div class="empty-icon">
            <UploadFilled />
          </div>
          <h3>等待推理结果</h3>
          <p>选择上方模型并放入 CT 数据集后，这里会显示分割预览、候选区域统计和后续报告分析入口。</p>
        </div>

        <template v-else>
          <div v-if="result.previewImageObjectUrl" class="preview-panel">
            <div class="preview-panel-head">
              <span>推理预览</span>
              <strong v-if="result.previewSliceIndex !== undefined">Z={{ result.previewSliceIndex }}</strong>
            </div>
            <div class="preview-stage">
              <img :src="result.previewImageObjectUrl" :alt="`${result.meta.title}预览图`" />
            </div>
            <div class="preview-caption">
              <span>{{ result.meta.overlayText }}</span>
            </div>
          </div>
          <div v-else class="preview-fallback">
            当前结果未返回可用预览图，请结合下方结构化结果继续判断。
          </div>

          <div class="result-cards">
            <div class="metric-card">
              <span>{{ result.meta.positiveLabel }}</span>
              <strong>{{ formatNumber(result.positivePixels) }}</strong>
            </div>
            <div class="metric-card">
              <span>总像素</span>
              <strong>{{ formatNumber(result.totalPixels) }}</strong>
            </div>
            <div class="metric-card">
              <span>{{ result.meta.ratioLabel }}</span>
              <strong class="ratio-value">{{ formatRatio(result.ratio) }}</strong>
            </div>
          </div>

          <el-alert
            :title="resultStatusTitle"
            :type="resultStatusType"
            :description="resultStatusDescription"
            show-icon
            class="status-alert"
          />

          <div class="result-meta">
            <div v-if="result.maskFile"><span>Mask 文件</span>{{ result.maskFile }}</div>
            <div v-if="result.previewImageFile"><span>预览图</span>{{ result.previewImageFile }}</div>
            <div v-if="result.sliceText"><span>{{ result.meta.sliceLabel }}</span>{{ result.sliceText }}</div>
            <div v-if="result.lesionCount !== undefined"><span>候选区数量</span>{{ result.lesionCount }}</div>
            <div v-if="result.largestLesionPixels !== undefined"><span>最大候选区</span>{{ formatNumber(result.largestLesionPixels) }} 像素</div>
            <div v-if="result.fallback"><span>模型状态</span>未加载训练权重，当前为启发式候选结果</div>
            <div v-if="result.shapeText"><span>影像维度</span>{{ result.shapeText }}</div>
            <div v-if="result.spacingText"><span>像素间距</span>{{ result.spacingText }}</div>
            <div v-if="result.modelText"><span>模型版本</span>{{ result.modelText }}</div>
            <div v-if="result.latencyMs"><span>推理耗时</span>{{ result.latencyMs }} ms</div>
            <div v-if="result.summaryText"><span>报告输入</span>{{ result.summaryText }}</div>
            <a v-if="result.downloadUrl" href="" @click.prevent="downloadProtectedResult">下载 mask 结果</a>
          </div>

          <div class="analysis-actions">
            <el-button
              type="success"
              :loading="analyzing"
              :disabled="!result.reportInput || !registerId.trim()"
              @click="runAiAnalysis"
            >
              {{ analyzing ? 'AI 分析中...' : 'AI 分析结构化结果' }}
            </el-button>
          </div>

          <div v-if="analysisResult" class="analysis-panel">
            <div class="analysis-title">
              <strong>AI 分析结论</strong>
              <el-tag v-if="analysisResult.riskLevel" size="small" type="warning">
                {{ analysisResult.riskLevel }}
              </el-tag>
            </div>
            <p v-if="analysisResult.summary">{{ analysisResult.summary }}</p>
            <div v-if="analysisResult.suggestions?.length" class="analysis-list">
              <span>处理建议</span>
              <ul>
                <li v-for="item in analysisResult.suggestions" :key="item">{{ item }}</li>
              </ul>
            </div>
            <div v-if="analysisResult.followUpAdvice" class="analysis-follow">
              <span>随访建议</span>{{ analysisResult.followUpAdvice }}
            </div>
          </div>
        </template>
      </section>
    </div>
  </div>
</template>

<style scoped>
.ct-workbench {
  min-height: 100%;
  padding: 28px;
  color: #172033;
}

.page-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  max-width: 1240px;
  margin: 0 auto 18px;
}

.page-top-left {
  min-width: 0;
}

.back-btn {
  margin-bottom: 8px;
  padding-left: 0;
  color: #64748b;
  font-weight: 700;
}

.back-btn:hover {
  color: #2563eb;
}

.eyebrow {
  margin: 0 0 6px;
  color: #2563eb;
  font-size: 13px;
  font-weight: 800;
}

.page-top h1 {
  margin: 0;
  color: #102033;
  font-size: 30px;
  font-weight: 800;
}

.page-desc {
  max-width: 720px;
  margin: 8px 0 0;
  color: #64748b;
  font-size: 14px;
  line-height: 1.7;
}

.page-actions {
  display: flex;
  flex: 0 0 auto;
  flex-direction: column;
  align-items: flex-end;
  gap: 10px;
}

.workflow-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
}

.service-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  flex: 0 0 auto;
  padding: 8px 12px;
  border: 1px solid #dbeafe;
  border-radius: 999px;
  background: #fff;
  color: #315fbb;
  font-size: 13px;
  font-weight: 700;
}

.service-badge span {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #22c55e;
}

.task-context {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  max-width: 1240px;
  margin: 0 auto 16px;
  padding: 12px 16px;
  border: 1px solid #dbe3ef;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 10px 26px rgba(15, 23, 42, 0.05);
}

.task-context span {
  color: #2563eb;
  font-size: 12px;
  font-weight: 800;
}

.task-context strong {
  color: #102033;
  font-size: 14px;
}

.task-context em {
  font-style: normal;
  color: #64748b;
  font-size: 12px;
  font-weight: 700;
}

.model-switch {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  max-width: 1240px;
  margin: 0 auto 16px;
}

.model-option {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
  min-height: 124px;
  padding: 18px 20px;
  border: 1px solid #dbe3ef;
  border-radius: 18px;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.96), rgba(248, 250, 252, 0.94)),
    #fff;
  color: #334155;
  cursor: pointer;
  text-align: left;
  transition: border-color 0.18s ease, box-shadow 0.18s ease, transform 0.18s ease;
}

.model-option strong {
  font-size: 17px;
  font-weight: 800;
}

.model-option > span {
  color: #64748b;
  font-size: 12px;
  font-weight: 700;
}

.model-option.active {
  border-color: #2563eb;
  background:
    radial-gradient(circle at top right, rgba(37, 99, 235, 0.09), transparent 34%),
    linear-gradient(135deg, #f8fbff, #ffffff);
  box-shadow: 0 12px 28px rgba(37, 99, 235, 0.14);
}

.model-option.upload-ready {
  transform: translateY(-1px);
}

.model-option:disabled {
  cursor: not-allowed;
  opacity: 0.7;
}

.model-dataset {
  width: 100%;
  margin-top: 10px;
  padding: 14px 14px 12px;
  border: 1px dashed #bfdbfe;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.8);
}

.dataset-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.dataset-badge {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 999px;
  background: #dbeafe;
  color: #1d4ed8;
  font-size: 11px;
  font-weight: 800;
}

.dataset-clear {
  width: 28px;
  height: 28px;
  border: none;
  border-radius: 999px;
  background: #eff6ff;
  color: #64748b;
  font-size: 18px;
  line-height: 1;
  cursor: pointer;
}

.dataset-clear:hover {
  background: #dbeafe;
  color: #1d4ed8;
}

.dataset-name {
  margin-top: 10px;
  color: #102033;
  font-size: 14px;
  font-weight: 700;
  word-break: break-all;
}

.dataset-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 8px;
}

.dataset-meta span {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 999px;
  background: #f8fafc;
  color: #475569;
  font-size: 12px;
  font-weight: 700;
}

.dataset-hint {
  margin-top: 10px;
  color: #64748b;
  font-size: 12px;
  line-height: 1.6;
}

.model-dataset-placeholder {
  width: 100%;
  margin-top: 10px;
  padding: 14px;
  border: 1px dashed #cbd5e1;
  border-radius: 14px;
  background: rgba(248, 250, 252, 0.8);
  color: #64748b;
  text-align: center;
}

.model-dataset-placeholder .el-icon {
  font-size: 24px;
  color: #2563eb;
}

.model-dataset-placeholder p {
  margin: 8px 0 4px;
  color: #102033;
  font-size: 13px;
  font-weight: 700;
}

.model-dataset-placeholder small {
  font-size: 12px;
}

.workspace-grid {
  display: grid;
  grid-template-columns: minmax(340px, 420px) minmax(0, 1fr);
  gap: 18px;
  align-items: start;
  max-width: 1240px;
  margin: 0 auto;
}

.panel {
  border: 1px solid #e2e8f0;
  border-radius: 18px;
  background: #fff;
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.05);
}

.input-panel,
.result-panel {
  padding: 22px;
}

.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.panel-head h2 {
  margin: 0 0 6px;
  color: #102033;
  font-size: 18px;
  font-weight: 800;
}

.panel-head p {
  margin: 0;
  color: #64748b;
  font-size: 13px;
  line-height: 1.6;
}

.register-input {
  margin-bottom: 14px;
}

.upload-note {
  margin-bottom: 14px;
  padding: 12px 14px;
  border-radius: 12px;
  background: #f8fafc;
  color: #64748b;
  font-size: 12px;
  line-height: 1.6;
}

.run-button {
  width: 100%;
}

.hidden-file-input {
  display: none;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 420px;
  border: 1px dashed #cbd5e1;
  border-radius: 16px;
  background: #f8fafc;
  text-align: center;
  padding: 32px;
}

.empty-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 64px;
  height: 64px;
  margin-bottom: 14px;
  border-radius: 50%;
  background: #e0edff;
  color: #2563eb;
  font-size: 30px;
}

.empty-state h3 {
  margin: 0 0 8px;
  color: #102033;
  font-size: 18px;
}

.empty-state p {
  max-width: 460px;
  margin: 0;
  color: #64748b;
  font-size: 13px;
  line-height: 1.7;
}

.preview-panel {
  margin-bottom: 16px;
  border: 1px solid #d9e2ec;
  border-radius: 16px;
  background: linear-gradient(180deg, #081120 0%, #101a2e 100%);
  overflow: hidden;
}

.preview-panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 16px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.16);
  color: #dbeafe;
  font-size: 12px;
  font-weight: 800;
}

.preview-panel-head strong {
  color: #fca5a5;
  font-size: 13px;
}

.preview-stage {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 320px;
  padding: 20px;
  background:
    radial-gradient(circle at center, rgba(37, 99, 235, 0.08), transparent 52%),
    #020617;
}

.preview-stage img {
  display: block;
  max-width: 100%;
  max-height: min(68vh, 720px);
  width: auto;
  height: auto;
  object-fit: contain;
}

.preview-caption {
  padding: 12px 16px 14px;
  color: #cbd5e1;
  font-size: 13px;
  line-height: 1.7;
}

.preview-fallback {
  margin-bottom: 16px;
  padding: 14px 16px;
  border: 1px dashed #cbd5e1;
  border-radius: 16px;
  background: #f8fafc;
  color: #64748b;
  font-size: 13px;
  line-height: 1.7;
}

.result-cards {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 14px;
}

.metric-card {
  padding: 14px;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  background: #f8fafc;
}

.metric-card span {
  display: block;
  margin-bottom: 8px;
  color: #64748b;
  font-size: 12px;
  font-weight: 700;
}

.metric-card strong {
  color: #102033;
  font-size: 24px;
  font-weight: 800;
}

.metric-card .ratio-value {
  color: #d97706;
}

.status-alert {
  margin-bottom: 14px;
}

.result-meta {
  padding: 14px 16px;
  border: 1px solid #eef2f7;
  border-radius: 14px;
  background: #f8fafc;
  color: #475569;
  font-size: 13px;
  line-height: 1.9;
}

.result-meta span {
  display: inline-block;
  min-width: 82px;
  color: #94a3b8;
  font-weight: 700;
}

.result-meta a {
  display: inline-block;
  margin-top: 6px;
  color: #2563eb;
  font-weight: 800;
}

.analysis-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}

.analysis-panel {
  margin-top: 14px;
  padding: 14px 16px;
  border: 1px solid #bbf7d0;
  border-radius: 14px;
  background: #f6fff7;
  color: #334155;
  font-size: 14px;
}

.analysis-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.analysis-panel p {
  margin: 10px 0;
}

.analysis-list span,
.analysis-follow span {
  display: inline-block;
  min-width: 72px;
  color: #64748b;
  font-weight: 700;
}

.analysis-list ul {
  margin: 6px 0 0;
  padding-left: 20px;
}

.analysis-follow {
  margin-top: 10px;
}

@media (max-width: 980px) {
  .workspace-grid {
    grid-template-columns: 1fr;
  }

  .page-top {
    display: block;
  }

  .page-actions {
    align-items: flex-start;
    margin-top: 12px;
  }

  .workflow-actions {
    justify-content: flex-start;
  }

  .service-badge {
    margin-top: 12px;
  }
}

@media (max-width: 640px) {
  .ct-workbench {
    padding: 18px;
  }

  .model-switch,
  .result-cards {
    grid-template-columns: 1fr;
  }

  .preview-panel-head,
  .preview-caption {
    padding-left: 14px;
    padding-right: 14px;
  }

  .preview-stage {
    min-height: 220px;
    padding: 14px;
  }
}
</style>

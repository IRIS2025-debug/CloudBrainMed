<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import { predictCtArtifact, predictCtLesion } from '@/api/examination/ct'

const uploading = ref(false)
const result = ref<any>(null)
const selectedFile = ref<File | null>(null)
const taskType = ref<'artifact' | 'lesion'>('artifact')
const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL || '').replace(/\/$/, '')

const taskMeta = {
  artifact: {
    title: 'CT 伪影检测',
    description: '上传 CT 影像（.nii.gz），U-Net 模型自动检测金属伪影区域',
    endpoint: 'ct-artifact',
    positiveLabel: '伪影像素',
    ratioLabel: '伪影占比',
    detectedTitle: '检测到伪影',
    cleanTitle: '未检测到伪影',
    overlayText: '红色区域为模型标出的金属伪影候选区',
    sliceLabel: '伪影切片',
  },
  lesion: {
    title: 'CT 病灶识别与分割',
    description: '上传 CT 影像（.nii.gz），模型输出病灶候选 mask、切片位置和结构化结果',
    endpoint: 'ct-lesion',
    positiveLabel: '病灶像素',
    ratioLabel: '病灶占比',
    detectedTitle: '检测到病灶候选区',
    cleanTitle: '未检测到明显病灶候选区',
    overlayText: '红色区域为模型标出的病灶候选区',
    sliceLabel: '病灶切片',
  },
}

function handleFileChange(uploadFile: any) {
  selectedFile.value = uploadFile.raw || uploadFile
}

async function runInference() {
  if (!selectedFile.value) {
    ElMessage.warning('请先选择 CT 文件')
    return
  }
  uploading.value = true
  result.value = null

  try {
    const res = taskType.value === 'lesion'
      ? await predictCtLesion(selectedFile.value)
      : await predictCtArtifact(selectedFile.value)
    const response = res as any
    const data = response?.data?.status ? response.data : response

    if (data.success || data.status === 'success') {
      result.value = normalizeResult(data, taskType.value)
      ElMessage.success('推理完成')
    } else {
      ElMessage.error(data.error || data.message || '推理失败')
    }
  } catch (e: any) {
    ElMessage.error(e?.message || '推理服务暂不可用，请检查后端服务和 Python 推理服务')
  } finally {
    uploading.value = false
  }
}

function normalizeResult(data: Record<string, any>, task: 'artifact' | 'lesion') {
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
    previewImageUrl: resolvePreviewUrl(previewImageUrl, meta.endpoint),
    previewSliceIndex,
    sliceIndices,
    sliceText: formatSliceIndices(sliceIndices),
    reportInput,
    downloadUrl: resolveDownloadUrl(data.download_url || data.downloadUrl || maskFile, meta.endpoint),
    shapeText: formatArray(data.shape),
    spacingText: formatArray(data.spacing),
    modelText: [data.model_type || data.modelType, data.model_version || data.modelVersion].filter(Boolean).join(' / '),
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
  return Array.isArray(value) ? value.join(' × ') : ''
}

function formatNumber(value?: number) {
  return value === undefined ? '--' : value.toLocaleString()
}

function formatRatio(value?: number) {
  return value === undefined ? '--' : `${value}%`
}

function resolveDownloadUrl(url: string, endpoint: string) {
  if (!url) return ''
  const path = /^https?:\/\//i.test(url) ? new URL(url).pathname : url
  const filename = path.split('/').filter(Boolean).pop()
  return filename ? `${apiBaseUrl}/doctor-service/exam/${endpoint}/result/${encodeURIComponent(filename)}` : ''
}

function resolvePreviewUrl(url: string, endpoint: string) {
  if (!url) return ''
  const path = /^https?:\/\//i.test(url) ? new URL(url).pathname : url
  const filename = path.split('/').filter(Boolean).pop()
  return filename ? `${apiBaseUrl}/doctor-service/exam/${endpoint}/preview/${encodeURIComponent(filename)}` : ''
}

function formatSliceIndices(value: any) {
  return Array.isArray(value) && value.length ? value.join(', ') : ''
}
</script>

<template>
  <div class="ct-inference">
    <div class="page-header">
      <h2>{{ taskMeta[taskType].title }}</h2>
      <p>{{ taskMeta[taskType].description }}</p>
    </div>

    <div class="upload-section">
      <el-radio-group v-model="taskType" class="task-tabs" :disabled="uploading" @change="result = null">
        <el-radio-button label="artifact">伪影检测</el-radio-button>
        <el-radio-button label="lesion">病灶识别分割</el-radio-button>
      </el-radio-group>

      <el-upload
        drag
        :auto-upload="false"
        :on-change="handleFileChange"
        accept=".nii,.nii.gz"
        :limit="1"
      >
        <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
        <div class="el-upload__text">
          将 CT 文件拖到此处，或<em>点击上传</em>
        </div>
        <template #tip>
          <div class="el-upload__tip">支持 .nii.gz / .nii 格式</div>
        </template>
      </el-upload>

      <el-button
        type="primary"
        size="large"
        :loading="uploading"
        @click="runInference"
        :disabled="!selectedFile"
        style="margin-top: 16px; width: 100%"
      >
        {{ uploading ? '推理中...' : '开始检测' }}
      </el-button>
    </div>

    <div v-if="result" class="result-section">
      <el-divider />
      <div v-if="result.previewImageUrl" class="preview-panel">
        <img :src="result.previewImageUrl" :alt="`${result.meta.title}预览图`" />
        <div class="preview-caption">
          <span>{{ result.meta.overlayText }}</span>
          <strong v-if="result.previewSliceIndex !== undefined">Z 轴切片 {{ result.previewSliceIndex }}</strong>
        </div>
      </div>

      <div class="result-cards">
        <el-card shadow="hover">
          <template #header>{{ result.meta.positiveLabel }}</template>
          <div class="card-value">{{ formatNumber(result.positivePixels) }}</div>
        </el-card>

        <el-card shadow="hover">
          <template #header>总像素</template>
          <div class="card-value">{{ formatNumber(result.totalPixels) }}</div>
        </el-card>

        <el-card shadow="hover">
          <template #header>{{ result.meta.ratioLabel }}</template>
          <div class="card-value" :class="{ highlight: (result.ratio || 0) > 0 }">
            {{ formatRatio(result.ratio) }}
          </div>
        </el-card>
      </div>

      <el-alert
        v-if="result.ratio !== undefined && result.ratio > 0"
        :title="result.meta.detectedTitle"
        type="warning"
        :description="`在 ${formatNumber(result.totalPixels)} 个像素中检测到 ${formatNumber(result.positivePixels)} 个${result.meta.positiveLabel}`"
        show-icon
        style="margin-top: 16px"
      />
      <el-alert
        v-else-if="result.ratio !== undefined"
        :title="result.meta.cleanTitle"
        type="success"
        description="当前 CT 影像中未发现明显候选区域"
        show-icon
        style="margin-top: 16px"
      />
      <el-alert
        v-else
        title="推理已完成"
        type="success"
        description="Python 推理服务已生成分割结果，当前结果未返回像素统计。"
        show-icon
        style="margin-top: 16px"
      />

      <div class="result-meta">
        <div v-if="result.maskFile"><span>掩码文件</span>{{ result.maskFile }}</div>
        <div v-if="result.previewImageFile"><span>预览图</span>{{ result.previewImageFile }}</div>
        <div v-if="result.previewSliceIndex !== undefined"><span>预览切片</span>Z={{ result.previewSliceIndex }}</div>
        <div v-if="result.sliceText"><span>{{ result.meta.sliceLabel }}</span>{{ result.sliceText }}</div>
        <div v-if="result.lesionCount !== undefined"><span>候选区数量</span>{{ result.lesionCount }}</div>
        <div v-if="result.largestLesionPixels !== undefined"><span>最大候选区</span>{{ formatNumber(result.largestLesionPixels) }} 像素</div>
        <div v-if="result.fallback"><span>模型状态</span>未加载病灶训练权重，当前为启发式候选结果</div>
        <div v-if="result.shapeText"><span>影像维度</span>{{ result.shapeText }}</div>
        <div v-if="result.spacingText"><span>像素间距</span>{{ result.spacingText }}</div>
        <div v-if="result.modelText"><span>模型版本</span>{{ result.modelText }}</div>
        <div v-if="result.latencyMs"><span>推理耗时</span>{{ result.latencyMs }} ms</div>
        <div v-if="result.summaryText"><span>报告输入</span>{{ result.summaryText }}</div>
        <a v-if="result.downloadUrl" :href="result.downloadUrl" target="_blank" rel="noopener">下载掩码结果</a>
      </div>
    </div>
  </div>
</template>

<style scoped>
.ct-inference {
  padding: 24px;
  max-width: 800px;
  margin: 0 auto;
}
.page-header h2 {
  font-size: 24px;
  color: #303133;
  margin-bottom: 8px;
}
.page-header p {
  color: #909399;
  font-size: 14px;
}
.upload-section {
  margin-top: 24px;
}
.task-tabs {
  margin-bottom: 16px;
}
.result-cards {
  display: flex;
  gap: 16px;
}
.preview-panel {
  margin-bottom: 18px;
  border: 1px solid #d9e2ec;
  border-radius: 8px;
  background: #0f172a;
  overflow: hidden;
}
.preview-panel img {
  display: block;
  width: 100%;
  max-height: 520px;
  object-fit: contain;
  background: #020617;
}
.preview-caption {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 14px;
  color: #e2e8f0;
  font-size: 13px;
}
.preview-caption strong {
  white-space: nowrap;
  color: #fca5a5;
}
.result-cards .el-card {
  flex: 1;
  text-align: center;
}
.card-value {
  font-size: 28px;
  font-weight: bold;
  color: #409EFF;
}
.card-value.highlight {
  color: #E6A23C;
}
.result-meta {
  margin-top: 18px;
  padding: 14px 16px;
  background: #f8fafc;
  border: 1px solid #eef2f7;
  border-radius: 8px;
  color: #475569;
  font-size: 13px;
  line-height: 1.9;
}
.result-meta span {
  display: inline-block;
  min-width: 72px;
  color: #94a3b8;
  font-weight: 600;
}
.result-meta a {
  display: inline-block;
  margin-top: 6px;
  color: #409EFF;
  font-weight: 600;
}
@media (max-width: 640px) {
  .result-cards,
  .preview-caption {
    flex-direction: column;
  }
}
</style>

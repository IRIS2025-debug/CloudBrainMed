<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { predictCtArtifact } from '@/api/admin/ml'

const uploading = ref(false)
const result = ref<any>(null)
const selectedFile = ref<File | null>(null)
const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL || '').replace(/\/$/, '')

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
    const res = await predictCtArtifact(selectedFile.value)
    const data = res.data

    if (data.success || data.status === 'success') {
      result.value = normalizeResult(data)
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

function normalizeResult(data: Record<string, any>) {
  const positivePixels = readNumber(data, ['positive_pixels', 'positivePixels'])
  const totalPixels = readNumber(data, ['total_pixels', 'totalPixels'])
  const ratio = readNumber(data, ['ratio', 'artifact_ratio', 'artifactRatio'])
  const maskFile = data.mask_file || data.maskFile || ''
  return {
    ...data,
    positivePixels,
    totalPixels,
    ratio: ratio ?? computeRatio(positivePixels, totalPixels),
    maskFile,
    downloadUrl: resolveDownloadUrl(data.download_url || data.downloadUrl || maskFile),
    shapeText: formatArray(data.shape),
    spacingText: formatArray(data.spacing),
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

function resolveDownloadUrl(url: string) {
  if (!url) return ''
  const path = /^https?:\/\//i.test(url) ? new URL(url).pathname : url
  const filename = path.split('/').filter(Boolean).pop()
  return filename ? `${apiBaseUrl}/admin-service/ml/inference/ct-artifact/result/${encodeURIComponent(filename)}` : ''
}
</script>

<template>
  <div class="ct-inference">
    <div class="page-header">
      <h2>CT 伪影检测</h2>
      <p>上传 CT 影像（.nii.gz），U-Net 3D 模型自动检测伪影区域</p>
    </div>

    <!-- 上传区 -->
    <div class="upload-section">
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

    <!-- 结果 -->
    <div v-if="result" class="result-section">
      <el-divider />

      <div class="result-cards">
        <el-card shadow="hover">
          <template #header>阳性像素</template>
          <div class="card-value">{{ formatNumber(result.positivePixels) }}</div>
        </el-card>

        <el-card shadow="hover">
          <template #header>总像素</template>
          <div class="card-value">{{ formatNumber(result.totalPixels) }}</div>
        </el-card>

        <el-card shadow="hover">
          <template #header>伪影占比</template>
          <div class="card-value" :class="{ highlight: (result.ratio || 0) > 0 }">
            {{ formatRatio(result.ratio) }}
          </div>
        </el-card>
      </div>

      <el-alert
        v-if="result.ratio !== undefined && result.ratio > 0"
        title="检测到伪影"
        type="warning"
        :description="`在 ${formatNumber(result.totalPixels)} 个像素中检测到 ${formatNumber(result.positivePixels)} 个伪影像素`"
        show-icon
        style="margin-top: 16px"
      />
      <el-alert
        v-else-if="result.ratio !== undefined"
        title="未检测到伪影"
        type="success"
        description="当前 CT 影像中未发现明显的伪影区域"
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
        <div v-if="result.shapeText"><span>影像维度</span>{{ result.shapeText }}</div>
        <div v-if="result.spacingText"><span>像素间距</span>{{ result.spacingText }}</div>
        <div v-if="result.latencyMs"><span>推理耗时</span>{{ result.latencyMs }} ms</div>
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
.result-cards {
  display: flex;
  gap: 16px;
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
</style>

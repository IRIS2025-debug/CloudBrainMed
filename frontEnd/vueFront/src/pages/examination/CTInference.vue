<template>
  <div class="ct-workbench">
    <div class="page-top">
      <div>
        <p class="eyebrow">检查医生 · CV 模型</p>
        <h1>CT 双模型推理工作台</h1>
        <p class="page-desc">同一份 CT 文件可按业务需要执行金属伪影识别或病灶识别分割，结果可复制文本用于AI报告分析。</p>
      </div>
      <div class="page-top-right">
        <el-button @click="goBackPrev" class="back-btn">返回</el-button>
        <div class="service-badge">
          <span></span>
          推理服务
        </div>
      </div>
    </div>

    <!-- 顶部操作栏：两个模型的切换 + 保存按钮 -->
    <div class="model-switch">
      <div v-for="(meta, type) in taskMeta" :key="type" class="model-item-wrap">
        <div class="model-btn-row">
          <button
            class="model-option"
            :class="{ active: taskType === type }"
            :disabled="uploading"
            type="button"
            @click="taskType = type as CtTaskType"
          >
            <strong>{{ meta.title }}</strong>
            <span>{{ meta.subtitle }}</span>
          </button>
          <div class="model-btn-group">
            <el-button 
              class="copy-btn"
              size="small" 
              text
              color="#409EFF"
              @click="handleCopyClick(type)"
              :disabled="uploading || analyzing || !hasModelData(type)"
            >
              复制结果
            </el-button>
            <el-button 
              class="clear-btn"
              size="small" 
              text
              color="#f56c6c"
              @click="handleClearClick(type)"
              :disabled="uploading || analyzing"
            >
              清空数据
            </el-button>
          </div>
        </div>
      </div>
      <!-- 保存按钮 - 放在模型切换行右侧 -->
      <div class="save-report-wrap">
        <el-button
          type="success"
          size="large"
          :loading="saving"
          :disabled="!canSaveToReport"
          @click="handleSaveToReport"
          class="save-report-btn"
        >
          <el-icon><Document /></el-icon>
          {{ saving ? '保存中...' : '保存CT检查结果至报告中' }}
        </el-button>
        <el-tag v-if="canSaveToReport" size="small" type="success" class="save-hint">两模型均已就绪</el-tag>
        <el-tag v-else size="small" type="warning" class="save-hint">需完成双模型推理+AI分析</el-tag>
      </div>
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
        挂号ID:
        <el-input
          v-model="registerId"
          placeholder="挂号ID，用于把模型结果送入 AI 检查报告分析"
          :disabled="uploading || analyzing"
          clearable
          class="register-input"
          @change="persistRegisterId"
        />

        <el-upload
          class="ct-upload"
          drag
          :auto-upload="false"
          :on-change="handleFileChange"
          :on-remove="handleFileRemove"
          accept=".nii,.nii.gz"
          :limit="1"
        >
          <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
          <div class="el-upload__text">
            将 CT 文件拖到此处，或 <em>点击选择</em>
          </div>
          <template #tip>
            <div class="el-upload__tip">仅接入真实 CT 模型接口，不再走旧 JPG/PNG 原型上传接口。</div>
          </template>
        </el-upload>

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
          <p>选择左侧模型并上传 CT 文件后，这里会显示分割预览、候选区域统计，可点击上方复制按钮提取报告文本。</p>
        </div>

        <template v-else>
          <div v-if="result.previewImageUrl" class="preview-panel">
            <img :src="result.previewImageUrl" :alt="`${result.meta.title}预览图`" />
            <div class="preview-caption">
              <span>{{ result.meta.overlayText }}</span>
              <strong v-if="result.previewSliceIndex !== undefined">Z={{ result.previewSliceIndex }}</strong>
            </div>
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
            <div v-if="result.fallback"><span>模型状态</span>未加载病灶训练权重，当前为启发式候选结果</div>
            <div v-if="result.shapeText"><span>影像维度</span>{{ result.shapeText }}</div>
            <div v-if="result.spacingText"><span>像素间距</span>{{ result.spacingText }}</div>
            <div v-if="result.modelText"><span>模型版本</span>{{ result.modelText }}</div>
            <div v-if="result.latencyMs"><span>推理耗时</span>{{ result.latencyMs }} ms</div>
            <div v-if="result.summaryText"><span>报告输入</span>{{ result.summaryText }}</div>
            <a v-if="result.downloadUrl" :href="result.downloadUrl" target="_blank" rel="noopener">下载 mask 结果</a>
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

          <!-- AI分析面板 -->
          <div v-if="analysisResult" class="analysis-panel">
            <div class="analysis-title">
              <strong>AI 分析结论</strong>
              <el-tag v-if="analysisResult.riskLevel" size="small" type="warning">{{ analysisResult.riskLevel }}</el-tag>
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

<script setup lang="ts">
defineOptions({
  name: 'CtWorkbench'
})
import { computed, ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { UploadFilled, Document } from '@element-plus/icons-vue'
import { analyzeCtReportInput, predictCtArtifact, predictCtLesion } from '@/api/examination/ct'

type CtTaskType = 'artifact' | 'lesion'

// ========= sessionStorage 键名 =========
const STORAGE_KEY = 'ct_workbench_cache'
const REGISTER_ID_KEY = 'ct_workbench_register_id'

const router = useRouter()
const route = useRoute()
const uploading = ref(false)
const analyzing = ref(false)
const saving = ref(false)
const selectedFile = ref<File | null>(null)
const taskType = ref<CtTaskType>('artifact')
const registerId = ref('')
const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL || '').replace(/\/$/, '')

// ========= 分模型缓存，切换模型保留各自数据 =========
interface ModelCacheItem {
  result: any
  analysisResult: any
}
const modelCache = ref<Record<CtTaskType, ModelCacheItem>>({
  artifact: { result: null, analysisResult: null },
  lesion: { result: null, analysisResult: null }
})

// 当前页面展示数据（绑定模板）
const result = computed({
  get() {
    return modelCache.value[taskType.value].result
  },
  set(val) {
    modelCache.value[taskType.value].result = val
    saveToStorage()
  }
})
const analysisResult = computed({
  get() {
    return modelCache.value[taskType.value].analysisResult
  },
  set(val) {
    modelCache.value[taskType.value].analysisResult = val
    saveToStorage()
  }
})

// ========= 数据持久化函数 =========
function saveToStorage() {
  try {
    const data = {
      modelCache: modelCache.value,
      registerId: registerId.value
    }
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(data))
  } catch (e) {
    console.warn('保存缓存失败:', e)
  }
}

function loadFromStorage() {
  try {
    const stored = sessionStorage.getItem(STORAGE_KEY)
    if (stored) {
      const data = JSON.parse(stored)
      if (data.modelCache) {
        modelCache.value = data.modelCache
      }
      if (data.registerId) {
        registerId.value = data.registerId
      }
    }
  } catch (e) {
    console.warn('读取缓存失败:', e)
  }
}

function persistRegisterId() {
  try {
    sessionStorage.setItem(REGISTER_ID_KEY, registerId.value)
    saveToStorage()
  } catch (e) {
    console.warn('保存挂号ID失败:', e)
  }
}

// ========= 判断模型是否存在推理或AI分析数据 =========
const hasModelData = (type: CtTaskType) => {
  const cache = modelCache.value[type]
  return !!cache.result || !!cache.analysisResult
}

// 判断是否两个模型都有完整的推理和AI分析结果
const canSaveToReport = computed(() => {
  const artifact = modelCache.value.artifact
  const lesion = modelCache.value.lesion
  return !!(artifact.result && artifact.analysisResult && lesion.result && lesion.analysisResult)
})

// 切换模型时不需要额外操作，因为数据已缓存

// 封装清空点击处理，增加多层判断提示
const handleClearClick = (type: CtTaskType) => {
  if (uploading.value || analyzing.value) {
    ElMessage.warning("任务执行中，暂无法清空数据，请等待完成后操作")
    return
  }
  const cache = modelCache.value[type]
  if (!cache.result && !cache.analysisResult) {
    ElMessage.info("该模型暂无推理与AI分析数据，无需清空")
    return
  }
  modelCache.value[type].result = null
  modelCache.value[type].analysisResult = null
  saveToStorage()
  ElMessage.success(`${taskMeta[type].title} 推理及AI分析数据已清空`)
}

// 复制当前模型整套报告文本
const handleCopyClick = async (type: CtTaskType) => {
  const cache = modelCache.value[type]
  const meta = taskMeta[type]
  const inference = cache.result
  const aiRes = cache.analysisResult

  if (!inference && !aiRes) {
    ElMessage.warning("暂无模型结果可复制，请先执行推理与AI分析")
    return
  }

  // 组装完整报告文本
  const blockTitle = `【${meta.title}分析结果】`
  let textContent = blockTitle + "\n"

  // 推理影像描述
  if (inference) {
    const ratio = inference.ratio ?? 0
    const sliceCount = inference.sliceIndices?.length ?? 0
    const summary = inference.summaryText || "无影像总结信息"
    textContent += `本次${meta.title}CT扫描：${summary}\n阳性像素占比${ratio}%，共涉及${sliceCount}个影像切片。\n\n`
  }

  // AI诊断与建议
  if (aiRes) {
    textContent += `风险等级：${aiRes.riskLevel || "未评估"}\n`
    if (aiRes.suggestions?.length) {
      aiRes.suggestions.forEach((item: string, idx: number) => {
        textContent += `${idx + 1}.${item}\n`
      })
    }
    if (aiRes.followUpAdvice) {
      textContent += `\n随访建议：${aiRes.followUpAdvice}`
    }
  }

  try {
    await navigator.clipboard.writeText(textContent)
    ElMessage.success(`${meta.title}报告文本已复制到剪贴板，可粘贴至报告页面`)
  } catch (err) {
    console.error("复制失败", err)
    ElMessage.error("复制失败，请手动选中文字复制")
  }
}

// ========= 保存到报告 =========
const handleSaveToReport = async () => {
  if (!canSaveToReport.value) {
    ElMessage.warning('请先完成两个模型的推理和AI分析')
    return
  }

  saving.value = true
  try {
    // 构造保存数据
    const artifactData = modelCache.value.artifact
    const lesionData = modelCache.value.lesion

    const reportData = {
      registerId: registerId.value,
      artifact: {
        result: artifactData.result,
        analysis: artifactData.analysisResult
      },
      lesion: {
        result: lesionData.result,
        analysis: lesionData.analysisResult
      },
      // 提取关键文本字段用于报告填充
      findings: {
        artifactSummary: artifactData.analysisResult?.summary || artifactData.result?.summaryText || '',
        lesionSummary: lesionData.analysisResult?.summary || lesionData.result?.summaryText || '',
        artifactSuggestions: artifactData.analysisResult?.suggestions || [],
        lesionSuggestions: lesionData.analysisResult?.suggestions || [],
        artifactFollowUp: artifactData.analysisResult?.followUpAdvice || '',
        lesionFollowUp: lesionData.analysisResult?.followUpAdvice || '',
      },
      // 影像预览图URL
      previewImages: [
        artifactData.result?.previewImageUrl,
        lesionData.result?.previewImageUrl
      ].filter(Boolean),
      // 模型输出文件
      maskFiles: [
        artifactData.result?.maskFile,
        lesionData.result?.maskFile
      ].filter(Boolean),
      timestamp: new Date().toISOString()
    }

    // 保存到 sessionStorage 供报告页面读取
    sessionStorage.setItem('ct_report_data', JSON.stringify(reportData))
    
    ElMessage.success('CT检查结果已保存，即将跳转到报告生成页面')
    // 跳转到报告生成页面
    router.push({
      path: '/examination-doctor/report',
      query: { 
        registerId: registerId.value,
        fromCtWorkbench: 'true'
      }
    })
  } catch (error: any) {
    console.error('保存报告数据失败:', error)
    ElMessage.error(error?.message || '保存失败，请重试')
  } finally {
    saving.value = false
  }
}

// 返回上一页
const goBackPrev = () => {
  router.back()
}

const fillRegisterId = () => {
  const query = route.query
  if (query.registerId) {
    registerId.value = query.registerId as string
    persistRegisterId()
  } else {
    // 尝试从缓存读取
    try {
      const cached = sessionStorage.getItem(REGISTER_ID_KEY)
      if (cached) {
        registerId.value = cached
      }
    } catch (e) {
      console.warn('读取挂号ID缓存失败:', e)
    }
  }
}

onMounted(() => {
  // 先加载缓存数据
  loadFromStorage()
  // 再从URL参数获取挂号ID（优先）
  fillRegisterId()
})

watch(
  () => route.fullPath,
  () => fillRegisterId(),
  { immediate: true }
)

// 监听registerId变化，自动保存
watch(registerId, () => {
  persistRegisterId()
})

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
    overlayText: '红色区域为模型标出的金属伪影候选区',
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
    overlayText: '红色区域为模型标出的病灶候选区',
    sliceLabel: '病灶切片',
    buttonText: '开始病灶识别分割',
  },
}

const currentMeta = computed(() => taskMeta[taskType.value])
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

function handleFileChange(uploadFile: any) {
  selectedFile.value = uploadFile.raw || uploadFile
}

function handleFileRemove() {
  selectedFile.value = null
}

async function runInference() {
  if (!selectedFile.value) {
    ElMessage.warning('请先选择 CT 文件')
    return
  }

  uploading.value = true
  // 推理前清空当前模型旧结果
  result.value = null
  analysisResult.value = null

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

async function runAiAnalysis() {
  console.log("进入runAiAnalysis")
  if (!result.value?.reportInput) {
    ElMessage.warning('当前推理结果缺少标准化 JSON')
    return
  }
  if (!registerId.value.trim()) {
    ElMessage.warning('请输入挂号ID')
    return
  }

  analyzing.value = true
  analysisResult.value = null
  
  try {
    const res = await analyzeCtReportInput({
      registerId: registerId.value.trim(),
      reportType: result.value.reportInput.task || currentMeta.value.reportType,
      reportInput: result.value.reportInput,
    })
    
    console.log('📥 AI分析原始响应:', res)
    
    let data = res
    if (res && typeof res === 'object' && 'code' in res) {
      const response = res as { code: number; message?: string; msg?: string; data?: any }
      if (response.code === 200 || response.code === 0) {
        data = response.data !== undefined ? response.data : res
      } else {
        ElMessage.error(response.message || response.msg || '请求失败')
        return
      }
    }
    
    if (data && typeof data === 'object' && 'summary' in data) {
      analysisResult.value = data
      ElMessage.success('AI 分析完成')
    } else {
      console.error('AI分析响应格式异常:', data)
      ElMessage.error('AI 分析返回数据格式异常')
    }
    
  } catch (e: any) {
    console.error('❌ AI分析错误:', e)
    let errorMsg = 'AI 分析暂不可用，请确认登录状态和挂号ID'
    if (e?.response?.data?.detail) {
      errorMsg = e.response.data.detail
    } else if (e?.message) {
      errorMsg = e.message
    }
    ElMessage.error(errorMsg)
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
  return Array.isArray(value) ? value.join(' x ') : ''
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

<style scoped>
.model-switch {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr)) auto;
  gap: 12px;
  max-width: 1240px;
  margin: 0 auto 16px;
  align-items: start;
}

.model-item-wrap {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.model-btn-row {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.model-option {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
  min-height: 72px;
  padding: 16px 18px;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #fff;
  color: #334155;
  cursor: pointer;
  text-align: left;
  transition: border-color .18s ease, box-shadow .18s ease, background .18s ease;
}

.model-btn-group {
  display: flex;
  flex-direction: column;
  gap: 4px;
  flex-shrink: 0;
  margin-top: 4px;
}

.copy-btn {
  padding: 4px 8px;
}
.copy-btn:hover {
  background-color: rgba(64, 158, 255, 0.1) !important;
}
.copy-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.clear-btn {
  padding: 4px 8px;
}
.clear-btn:hover {
  background-color: rgba(245, 108, 108, 0.1) !important;
}
.clear-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

/* 保存按钮区域 */
.save-report-wrap {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 6px;
  padding-top: 4px;
  min-width: 200px;
}

.save-report-btn {
  width: 100%;
  font-weight: 600;
  border-radius: 8px;
  padding: 12px 20px;
}

.save-report-btn .el-icon {
  margin-right: 6px;
}

.save-hint {
  font-size: 11px;
  white-space: nowrap;
}

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

.eyebrow {
  margin: 0 0 6px;
  color: #2563eb;
  font-size: 13px;
  font-weight: 800;
}

.page-top h1 {
  margin: 0;
  color: #102033;
  font-size: 28px;
  font-weight: 800;
}

.page-desc {
  max-width: 720px;
  margin: 8px 0 0;
  color: #64748b;
  font-size: 14px;
  line-height: 1.7;
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

.model-option strong {
  font-size: 16px;
  font-weight: 800;
}

.model-option span {
  color: #64748b;
  font-size: 12px;
  font-weight: 700;
}

.model-option.active {
  border-color: #2563eb;
  background: #f8fbff;
  box-shadow: 0 8px 20px rgba(37, 99, 235, .12);
}

.model-option:disabled {
  cursor: not-allowed;
  opacity: .7;
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
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 1px 2px rgba(15, 23, 42, .04);
}

.input-panel,
.result-panel {
  padding: 20px;
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

.ct-upload {
  margin-bottom: 14px;
}

.ct-upload :deep(.el-upload-dragger) {
  border-radius: 8px;
  padding: 34px 18px;
}

.ct-upload :deep(.el-upload__tip) {
  color: #64748b;
  font-size: 12px;
}

.run-button {
  width: 100%;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 420px;
  border: 1px dashed #cbd5e1;
  border-radius: 8px;
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
  border-radius: 8px;
  background: #0f172a;
  overflow: hidden;
}

.preview-panel img {
  display: block;
  width: 100%;
  max-height: 500px;
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
  flex: 0 0 auto;
  color: #fca5a5;
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
  border-radius: 8px;
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
  border-radius: 8px;
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
  border-radius: 8px;
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
  .model-switch {
    grid-template-columns: 1fr;
  }
  
  .save-report-wrap {
    align-items: stretch;
    min-width: unset;
    padding-top: 8px;
    border-top: 1px solid #e2e8f0;
  }
  
  .page-top,
  .workspace-grid {
    grid-template-columns: 1fr;
  }

  .page-top {
    display: block;
  }

  .service-badge {
    margin-top: 12px;
  }
  .model-btn-row {
    flex-direction: column;
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

  .preview-caption {
    flex-direction: column;
  }
}
</style>
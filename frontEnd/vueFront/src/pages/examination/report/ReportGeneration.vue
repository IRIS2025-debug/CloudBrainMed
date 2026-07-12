<template>
  <div class="report-generation-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <h2 class="page-title">📄 生成检查报告</h2>
      <div class="header-actions">
        <el-button @click="goBack">返回工作台</el-button>
        <el-button type="warning" plain @click="handleResetReport">
          <template #icon>
            <el-icon><RefreshRight /></el-icon>
          </template>
          重新书写报告
        </el-button>
      </div>
    </div>

    <!-- 主要内容 -->
    <el-row :gutter="20" class="main-row">
      <el-col :span="24" class="left-col">
        <el-card shadow="hover" class="report-card" v-loading="patientLoading || isLoading">
          <template #header>
            <div class="card-header">
              <span><el-icon><Document /></el-icon> CT综合检查报告</span>
              <div class="header-tags">
                <el-tag size="small" type="primary">最终报告</el-tag>
                <el-tag v-if="hasCtData" size="small" type="success">CT数据已加载</el-tag>
                <el-tag v-if="hasCtData" size="small" type="info">{{ ctDataCount }}套模型结果</el-tag>
              </div>
            </div>
          </template>

          <div class="report-content">
            <!-- 报告标题 -->
            <div class="report-title-section">
              <h3 class="report-title">{{ reportTitle || 'CT检查报告' }}</h3>
              <div class="report-id">报告编号：{{ reportId }}</div>
            </div>

            <!-- 患者个人信息 精简，只保留接口返回字段 -->
            <div class="report-section patient-section">
              <div class="section-title">
                <el-icon><User /></el-icon> 患者基础信息
              </div>
              <el-descriptions :column="3" border size="small">
                <el-descriptions-item label="挂号ID">{{ registerId || '--' }}</el-descriptions-item>
                <el-descriptions-item label="姓名">{{ patientInfo.name || '--' }}</el-descriptions-item>
                <el-descriptions-item label="性别">{{ patientInfo.gender || '--' }}</el-descriptions-item>
                <el-descriptions-item label="年龄">{{ patientInfo.age || '--' }}岁</el-descriptions-item>
                <el-descriptions-item label="检查项目" :span="3">{{ patientInfo.itemName || 'CT平扫检查' }}</el-descriptions-item>
              </el-descriptions>
            </div>

            <!-- 影像资料 - 双图并排各占50% -->
            <div class="report-section image-section">
              <div class="section-title">
                <el-icon><Picture /></el-icon> AI分割影像结果
              </div>

              <div v-if="artifactImage || lesionImage" class="image-row">
                <!-- 金属伪影识别影像 50%宽度 -->
                <div v-if="artifactImage" class="image-col">
                  <div class="model-image-section">
                    <div class="model-image-header">
                      <el-tag type="warning" size="small">金属伪影识别</el-tag>
                      <span class="model-image-desc">红色区域：金属伪影候选区</span>
                    </div>
                    <div class="model-image-wrapper">
                      <img :src="artifactImage" alt="金属伪影识别结果" @click="previewImage(artifactImage)" />
                      <div class="image-overlay-info" v-if="artifactData">
                        <span>伪影占比: {{ formatRatio(artifactData.result?.ratio) }}</span>
                        <span>伪影像素: {{ formatNumber(artifactData.result?.positivePixels) }}</span>
                      </div>
                    </div>
                  </div>
                </div>

                <!-- 病灶识别分割影像 50%宽度 -->
                <div v-if="lesionImage" class="image-col">
                  <div class="model-image-section">
                    <div class="model-image-header">
                      <el-tag type="danger" size="small">病灶识别分割</el-tag>
                      <span class="model-image-desc">红色区域：病灶候选区</span>
                    </div>
                    <div class="model-image-wrapper">
                      <img :src="lesionImage" alt="病灶识别分割结果" @click="previewImage(lesionImage)" />
                      <div class="image-overlay-info" v-if="lesionData">
                        <span>病灶占比: {{ formatRatio(lesionData.result?.ratio) }}</span>
                        <span>病灶像素: {{ formatNumber(lesionData.result?.positivePixels) }}</span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              <div v-if="!hasCtData" class="empty-thumbs">暂无影像数据，请返回CT工作台完成推理后保存导入</div>
            </div>

            <!-- 检查报告 - 分模型展示 -->
            <div class="report-section result-section">
              <div class="section-title">
                <el-icon><Edit /></el-icon> 模型AI分析详情
              </div>
              <div class="report-editor">
                <el-form label-width="100px" size="small">
                  <!-- 金属伪影识别结果 -->
                  <div v-if="artifactData" class="model-result-section">
                    <div class="model-result-header">
                      <el-tag type="warning" size="small">金属伪影识别模型</el-tag>
                      <el-tag :type="artifactData.result?.ratio > 0 ? 'warning' : 'success'" size="small">
                        {{ artifactData.result?.ratio > 0 ? '检出伪影' : '无明显伪影' }}
                      </el-tag>
                    </div>
                    <el-form-item label="影像所见">
                      <el-input
                        v-model="artifactFindings"
                        type="textarea"
                        :rows="3"
                        placeholder="金属伪影识别结果..."
                        readonly
                      />
                    </el-form-item>
                    <el-form-item label="AI诊断">
                      <el-input
                        v-model="artifactDiagnosis"
                        type="textarea"
                        :rows="2"
                        placeholder="金属伪影诊断意见..."
                        readonly
                      />
                    </el-form-item>
                    <el-form-item label="处理建议">
                      <el-input
                        v-model="artifactAdvice"
                        type="textarea"
                        :rows="2"
                        placeholder="金属伪影处理建议..."
                        readonly
                      />
                    </el-form-item>
                    <el-form-item label="风险分级">
                      <el-tag :type="getRiskType(artifactData.analysis?.riskLevel)" size="small">
                        {{ artifactData.analysis?.riskLevel || '未评估' }}
                      </el-tag>
                    </el-form-item>
                  </div>

                  <!-- 病灶识别分割结果 -->
                  <div v-if="lesionData" class="model-result-section">
                    <div class="model-result-header">
                      <el-tag type="danger" size="small">病灶分割识别模型</el-tag>
                      <el-tag :type="lesionData.result?.ratio > 0 ? 'danger' : 'success'" size="small">
                        {{ lesionData.result?.ratio > 0 ? '检出病灶' : '无明显病灶' }}
                      </el-tag>
                    </div>
                    <el-form-item label="影像所见">
                      <el-input
                        v-model="lesionFindings"
                        type="textarea"
                        :rows="3"
                        placeholder="病灶识别分割结果..."
                        readonly
                      />
                    </el-form-item>
                    <el-form-item label="AI诊断">
                      <el-input
                        v-model="lesionDiagnosis"
                        type="textarea"
                        :rows="2"
                        placeholder="病灶诊断意见..."
                        readonly
                      />
                    </el-form-item>
                    <el-form-item label="处理建议">
                      <el-input
                        v-model="lesionAdvice"
                        type="textarea"
                        :rows="2"
                        placeholder="病灶处理建议..."
                        readonly
                      />
                    </el-form-item>
                    <el-form-item label="风险分级">
                      <el-tag :type="getRiskType(lesionData.analysis?.riskLevel)" size="small">
                        {{ lesionData.analysis?.riskLevel || '未评估' }}
                      </el-tag>
                    </el-form-item>
                  </div>

                  <!-- 综合报告 -->
                  <div class="model-result-section comprehensive-block">
                    <div class="model-result-header">
                      <el-tag type="primary" size="small">综合诊断报告（医师编辑）</el-tag>
                    </div>
                    <el-form-item label="综合影像所见">
                      <el-input
                        v-model="reportFindings"
                        type="textarea"
                        :rows="4"
                        placeholder="整合两套模型输出，填写综合CT影像所见..."
                      />
                      <div class="textarea-actions">
                        <el-button size="small" text @click="clearFindings">清空</el-button>
                        <el-button v-if="hasCtData" size="small" type="primary" @click="autoFillFindings">
                          一键自动填充
                        </el-button>
                      </div>
                    </el-form-item>
                    <el-form-item label="综合诊断意见">
                      <el-input
                        v-model="reportDiagnosis"
                        type="textarea"
                        :rows="4"
                        placeholder="结合AI结果给出综合诊断结论..."
                      />
                      <div class="textarea-actions">
                        <el-button size="small" text @click="clearDiagnosis">清空</el-button>
                        <el-button v-if="hasCtData" size="small" type="primary" @click="autoFillDiagnosis">
                          一键自动填充
                        </el-button>
                      </div>
                    </el-form-item>
                    <el-form-item label="综合随访建议">
                      <el-input
                        v-model="reportAdvice"
                        type="textarea"
                        :rows="3"
                        placeholder="综合随访、复查、诊疗建议..."
                      />
                      <div class="textarea-actions">
                        <el-button size="small" text @click="clearAdvice">清空</el-button>
                        <el-button v-if="hasCtData" size="small" type="primary" @click="autoFillAdvice">
                          一键自动填充
                        </el-button>
                      </div>
                    </el-form-item>
                    <el-form-item label="报告医师">
                      <el-input v-model="reportDoctor" placeholder="填写签发报告医师姓名" style="width: 240px" />
                    </el-form-item>
                  </div>
                </el-form>
              </div>
            </div>

            <!-- 生成报告按钮 放置页面最底部 -->
            <div class="generate-footer">
              <el-button type="primary" size="large" @click="handleGenerate" :loading="generating">
                <template #icon>
                  <el-icon><Document /></el-icon>
                </template>
                {{ generating ? '报告生成中，请稍候...' : '生成完整CT检查报告' }}
              </el-button>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图片预览对话框 -->
    <el-dialog v-model="previewDialogVisible" title="影像大图预览" width="75%" :close-on-click-modal="true">
      <div class="preview-dialog-content">
        <img :src="previewImageUrl" alt="预览图" />
      </div>
    </el-dialog>

    <!-- 重置确认对话框 -->
    <el-dialog v-model="resetDialogVisible" title="⚠️ 重新书写报告" width="440px" :close-on-click-modal="false">
      <div class="reset-dialog-content">
        <el-icon class="reset-warning-icon" color="#e6a23c" :size="48">
          <WarningFilled />
        </el-icon>
        <p class="reset-dialog-text">
          此操作将清空所有已填写的报告内容，包括：
        </p>
        <ul class="reset-dialog-list">
          <li>综合影像所见</li>
          <li>综合诊断意见</li>
          <li>综合随访建议</li>
          <li>报告医师</li>
          <li>AI模型分析详情（只读内容）</li>
          <li>影像预览图</li>
        </ul>
        <p class="reset-dialog-text" style="color: #409eff; font-weight: 500;">
          ✅ 患者基础信息将完整保留
        </p>
        <p class="reset-dialog-text" style="color: #e6a23c; font-weight: 500;">
          ⚠️ 清空后需要重新从CT工作台导入数据
        </p>
      </div>
      <template #footer>
        <el-button @click="resetDialogVisible = false">取消</el-button>
        <el-button type="warning" @click="confirmResetReport" :loading="resetLoading">
          确认重新书写
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
defineOptions({
  name: 'ReportGeneration'
})

import { ref, onMounted, computed, onBeforeUnmount } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Document,
  User,
  Edit,
  Picture,
  RefreshRight,
  WarningFilled
} from '@element-plus/icons-vue'
import { useRouter, useRoute } from 'vue-router'
import { getTaskDetail } from '@/api/doctor/task'
import { examApi } from '@/api/examination/examApi'

const router = useRouter()
const route = useRoute()

// ---------- 报告基础信息 ----------
const reportId = ref(`RPT-${new Date().toISOString().slice(0, 10).replace(/-/g, '')}-001`)
const reportTitle = ref('头颅CT平扫检查报告')

// ---------- 患者加载状态 ----------
const patientLoading = ref(false)
const isLoading = ref(false)

// ---------- 挂号ID ----------
const registerId = ref('')

// ---------- 患者信息（精简，仅接口返回字段） ----------
const patientInfo = ref({
  name: '',
  gender: '',
  age: '',
  itemName: ''
})

// ---------- 图片预览弹窗 ----------
const previewDialogVisible = ref(false)
const previewImageUrl = ref('')

// ---------- 重置对话框 ----------
const resetDialogVisible = ref(false)
const resetLoading = ref(false)

const orderItemId = ref('')
// ---------- CT工作台存储的数据类型定义 ----------
interface ModelData {
  result: any
  analysis: any
}

interface CtReportData {
  registerId: string
  artifact: ModelData
  lesion: ModelData
  findings: {
    artifactSummary: string
    lesionSummary: string
    artifactSuggestions: string[]
    lesionSuggestions: string[]
    artifactFollowUp: string
    lesionFollowUp: string
  }
  previewImages: string[]
  maskFiles: string[]
  timestamp: string
}

// 全局CT缓存数据
const ctData = ref<CtReportData | null>(null)
const hasCtData = computed(() => ctData.value !== null)
const ctDataCount = computed(() => {
  if (!ctData.value) return 0
  let count = 0
  if (ctData.value.artifact?.result) count++
  if (ctData.value.lesion?.result) count++
  return count
})

// 分模型快捷取值
const artifactData = computed(() => ctData.value?.artifact)
const lesionData = computed(() => ctData.value?.lesion)

// 分离两张预览图
const artifactImage = computed(() => {
  if (!ctData.value?.previewImages || ctData.value.previewImages.length === 0) return ''
  return ctData.value.previewImages[0] || ''
})
const lesionImage = computed(() => {
  if (!ctData.value?.previewImages || ctData.value.previewImages.length < 2) return ''
  return ctData.value.previewImages[1] || ''
})

// mask文件数组（页面已删除此模块，变量保留不影响逻辑）
const maskFiles = computed(() => ctData.value?.maskFiles || [])

// 单模型只读文本域绑定值
const artifactFindings = ref('')
const artifactDiagnosis = ref('')
const artifactAdvice = ref('')

const lesionFindings = ref('')
const lesionDiagnosis = ref('')
const lesionAdvice = ref('')

// 综合报告编辑框
const reportFindings = ref('')
const reportDiagnosis = ref('')
const reportAdvice = ref('')
const reportDoctor = ref('')

// 生成按钮loading
const generating = ref(false)

// ========= 通用格式化工具函数 =========
const formatNumber = (value?: number) => {
  return value === undefined || value === null ? '--' : value.toLocaleString()
}
const formatRatio = (value?: number) => {
  return value === undefined || value === null ? '--' : `${value}%`
}
// 风险等级标签样式映射
const getRiskType = (level?: string) => {
  if (!level) return 'info'
  const map: Record<string, string> = {
    '低风险': 'success',
    '中风险': 'warning',
    '高风险': 'danger',
    '极高风险': 'danger'
  }
  return map[level] || 'info'
}

// ========= 清空所有报告内容的统一方法 =========
const clearAllReportContent = (keepPatientInfo = false) => {
  // 1. 清空CT数据
  ctData.value = null
  
  // 2. 清空单模型只读文本
  artifactFindings.value = ''
  artifactDiagnosis.value = ''
  artifactAdvice.value = ''
  lesionFindings.value = ''
  lesionDiagnosis.value = ''
  lesionAdvice.value = ''
  
  // 3. 清空综合报告编辑框
  reportFindings.value = ''
  reportDiagnosis.value = ''
  reportAdvice.value = ''
  reportDoctor.value = ''
  
  // 4. 清空预览图
  previewImageUrl.value = ''
  
  // 5. 根据参数决定是否保留患者信息
  if (!keepPatientInfo) {
    orderItemId.value = ''
    registerId.value = ''
    patientInfo.value = {
      name: '',
      gender: '',
      age: '',
      itemName: ''
    }
  }
  
  // 6. 重置报告编号
  reportId.value = `RPT-${new Date().toISOString().slice(0, 10).replace(/-/g, '')}-${String(Math.floor(Math.random() * 1000)).padStart(3, '0')}`
  
  console.log(`✅ 所有报告内容已清空${keepPatientInfo ? '（保留患者信息）' : ''}`)
}

// ========= 综合文本框清空方法 =========
const clearFindings = () => {
  reportFindings.value = ''
  ElMessage.info('已清空综合影像所见')
}
const clearDiagnosis = () => {
  reportDiagnosis.value = ''
  ElMessage.info('已清空综合诊断意见')
}
const clearAdvice = () => {
  reportAdvice.value = ''
  ElMessage.info('已清空综合随访建议')
}

// ========= 自动填充综合报告文本 =========
const autoFillFindings = () => {
  if (!ctData.value) {
    ElMessage.warning('暂无CT模型数据，无法自动填充')
    return
  }
  const data = ctData.value
  const parts: string[] = []

  if (data.findings.artifactSummary) {
    parts.push(`【金属伪影识别】${data.findings.artifactSummary}`)
  }
  if (data.findings.lesionSummary) {
    parts.push(`【病灶识别分割】${data.findings.lesionSummary}`)
  }

  if (parts.length === 0) {
    ElMessage.warning('未读取到影像总结内容')
    return
  }

  reportFindings.value = parts.join('\n\n')
  ElMessage.success('综合影像所见自动填充完成')
}

const autoFillDiagnosis = () => {
  if (!ctData.value) {
    ElMessage.warning('暂无CT模型数据，无法自动填充')
    return
  }
  const data = ctData.value
  const parts: string[] = []

  const allSuggestions = [
    ...(data.findings.artifactSuggestions || []),
    ...(data.findings.lesionSuggestions || [])
  ]

  if (allSuggestions.length > 0) {
    allSuggestions.forEach((s, i) => {
      parts.push(`${i + 1}. ${s}`)
    })
  }

  if (data.artifact.analysis?.riskLevel) {
    parts.push(`金属伪影风险等级：${data.artifact.analysis.riskLevel}`)
  }
  if (data.lesion.analysis?.riskLevel) {
    parts.push(`病灶风险等级：${data.lesion.analysis.riskLevel}`)
  }

  if (parts.length === 0) {
    ElMessage.warning('未读取到诊断建议内容')
    return
  }

  reportDiagnosis.value = parts.join('\n')
  ElMessage.success('综合诊断意见自动填充完成')
}

const autoFillAdvice = () => {
  if (!ctData.value) {
    ElMessage.warning('暂无CT模型数据，无法自动填充')
    return
  }
  const data = ctData.value
  const parts: string[] = []

  if (data.findings.artifactFollowUp) {
    parts.push(`【金属伪影随访建议】${data.findings.artifactFollowUp}`)
  }
  if (data.findings.lesionFollowUp) {
    parts.push(`【病灶随访建议】${data.findings.lesionFollowUp}`)
  }

  if (parts.length === 0) {
    ElMessage.warning('未读取到随访建议内容')
    return
  }

  reportAdvice.value = parts.join('\n')
  ElMessage.success('综合随访建议自动填充完成')
}

// ========= 大图预览弹窗 =========
const previewImage = (url: string) => {
  previewImageUrl.value = url
  previewDialogVisible.value = true
}

// ========= 返回工作台页面 =========
const goBack = () => {
  router.back()
}

// ========= 读取CT推理缓存数据（核心方法，可多次调用） =========
const loadCtData = (showMessage = false) => {
  const stored = sessionStorage.getItem('ct_report_data')
  if (stored) {
    try {
      const data = JSON.parse(stored) as CtReportData
      ctData.value = data

      // 同步挂号ID
      if (data.registerId) {
        registerId.value = data.registerId
      }

      // 填充金属伪影模块只读文本
      if (data.artifact) {
        if (data.artifact.analysis?.summary) {
          artifactFindings.value = data.artifact.analysis.summary
        } else if (data.artifact.result?.summaryText) {
          artifactFindings.value = data.artifact.result.summaryText
        }
        if (data.artifact.analysis?.suggestions?.length) {
          artifactDiagnosis.value = data.artifact.analysis.suggestions.join('\n')
        }
        if (data.artifact.analysis?.followUpAdvice) {
          artifactAdvice.value = data.artifact.analysis.followUpAdvice
        }
      }

      // 填充病灶模块只读文本
      if (data.lesion) {
        if (data.lesion.analysis?.summary) {
          lesionFindings.value = data.lesion.analysis.summary
        } else if (data.lesion.result?.summaryText) {
          lesionFindings.value = data.lesion.result.summaryText
        }
        if (data.lesion.analysis?.suggestions?.length) {
          lesionDiagnosis.value = data.lesion.analysis.suggestions.join('\n')
        }
        if (data.lesion.analysis?.followUpAdvice) {
          lesionAdvice.value = data.lesion.analysis.followUpAdvice
        }
      }

      // 自动填充综合三栏
      autoFillFindings()
      autoFillDiagnosis()
      autoFillAdvice()

      if (showMessage) {
        ElMessage.success('已加载CT工作台双模型推理数据')
      }
      return true
    } catch (e) {
      console.error('解析CT缓存数据失败:', e)
      if (showMessage) {
        ElMessage.warning('CT数据解析失败，请返回工作台重新保存')
      }
      return false
    }
  }
  return false
}

// ========= 带重试的加载方法 =========
const loadCtDataWithRetry = async (maxRetries = 5, interval = 300) => {
  isLoading.value = true
  try {
    // 首先尝试直接加载
    if (loadCtData(false)) {
      console.log('✅ CT数据加载成功')
      return
    }

    // 如果是从CT工作台跳转过来的，等待数据写入
    const fromCtWorkbench = route.query.fromCtWorkbench === 'true'
    if (fromCtWorkbench) {
      console.log('⏳ 从CT工作台跳转，等待数据写入...')
      let retries = 0
      while (retries < maxRetries) {
        await new Promise(resolve => setTimeout(resolve, interval))
        if (loadCtData(false)) {
          console.log(`✅ CT数据加载成功 (第${retries + 1}次尝试)`)
          ElMessage.success('已加载CT工作台数据')
          return
        }
        retries++
        console.log(`⏳ 第${retries}次等待，数据尚未就绪...`)
      }
      ElMessage.warning('数据加载超时，请手动刷新页面重试')
    } else {
      // 非CT工作台跳转，显示空状态
      console.log('📭 无CT数据，等待用户导入')
    }
  } finally {
    isLoading.value = false
  }
}

// ========= 新增：读取orderItemId，拉取患者详情 =========
const loadPatientInfo = async () => {
  const storedOrderItemId = sessionStorage.getItem('current_order_item_id')
  if (!storedOrderItemId) {
    console.warn('未找到 orderItemId')
    return
  }
  
  orderItemId.value = storedOrderItemId
  
  patientLoading.value = true
  try {
    const res = await getTaskDetail(storedOrderItemId)
    const detail = res.data
    console.log('患者详情:', detail)
    console.log(detail.assignedDoctorName)
    registerId.value = detail.registerId
    patientInfo.value = {
      name: detail.patientName,
      gender: detail.gender === 1 ? '男' : '女',
      age: detail.age,
      itemName: detail.itemName
    }
    
    // ===== 自动填充报告医师（使用新字段） =====
    if (detail.assignedDoctorName) {
      reportDoctor.value = detail.assignedDoctorName
      console.log('✅ 报告医师自动填充:', detail.assignedDoctorName)
    } else {
      console.warn('⚠️ 未获取到医生姓名，请手动填写')
    }
    
  } catch (err) {
    console.error('获取患者信息失败', err)
    ElMessage.warning('加载患者信息失败，请重新从任务列表进入')
  } finally {
    patientLoading.value = false
  }
}

// ========= 重新书写报告 - 清空所有报告内容（保留患者信息） =========
const handleResetReport = () => {
  // 检查是否有内容需要清空
  const hasContent = 
    reportFindings.value || 
    reportDiagnosis.value || 
    reportAdvice.value || 
    reportDoctor.value ||
    artifactFindings.value ||
    artifactDiagnosis.value ||
    artifactAdvice.value ||
    lesionFindings.value ||
    lesionDiagnosis.value ||
    lesionAdvice.value ||
    ctData.value !== null

  if (!hasContent) {
    ElMessage.info('当前报告内容已为空，无需重新书写')
    return
  }

  resetDialogVisible.value = true
}

// ========= 确认重新书写报告 =========
const confirmResetReport = async () => {
  resetLoading.value = true
  try {
    await new Promise(resolve => setTimeout(resolve, 300))

    // 使用统一的清空方法，但保留患者信息
    clearAllReportContent(true)
    
    // 清除sessionStorage
    sessionStorage.removeItem('ct_report_data')
    sessionStorage.removeItem('final_report')
    sessionStorage.removeItem('current_order_item_id')

    ElMessage.success('✅ 已清空所有报告内容，请重新从CT工作台导入数据并书写报告')
    resetDialogVisible.value = false
  } catch (error) {
    ElMessage.error('重置失败，请重试')
  } finally {
    resetLoading.value = false
  }
}

// ========= 生成最终完整报告 =========
const handleGenerate = async () => {
  if (!artifactData.value && !lesionData.value) {
    ElMessage.warning('请先从CT工作台导入AI推理数据')
    return
  }
  if (!reportFindings.value || !reportDiagnosis.value) {
    ElMessage.warning('请完善综合影像所见、综合诊断意见后再生成报告')
    return
  }

  try {
    await ElMessageBox.confirm('确认生成正式CT检查报告？', '操作确认', {
      confirmButtonText: '确认生成',
      cancelButtonText: '取消',
      type: 'info'
    })
    
    generating.value = true

    // 构建请求体
    const requestData = {
      orderItemId: orderItemId.value,
      registerId: registerId.value,
      reportTitle: reportTitle.value,
      artifact: {
        findings: artifactFindings.value,
        diagnosis: artifactDiagnosis.value,
        advice: artifactAdvice.value,
        riskLevel: artifactData.value?.analysis?.riskLevel,
        rawResult: artifactData.value?.result
      },
      lesion: {
        findings: lesionFindings.value,
        diagnosis: lesionDiagnosis.value,
        advice: lesionAdvice.value,
        riskLevel: lesionData.value?.analysis?.riskLevel,
        rawResult: lesionData.value?.result
      },
      comprehensive: {
        findings: reportFindings.value,
        diagnosis: reportDiagnosis.value,
        advice: reportAdvice.value
      },
      images: {
        artifact: artifactImage.value,
        lesion: lesionImage.value
      },
      reportDoctor: reportDoctor.value
    }

    // 调用后端保存接口
    const response = await examApi.saveReport(requestData)
    
    if (response.code === 200) {
      ElMessage.success({
        message: '✅ CT检查报告保存成功，报告内容已清空',
        duration: 3000
      })
      
      // ===== 清空所有报告内容，但保留患者信息 =====
      clearAllReportContent(true)
      
      // 保存成功后清除临时数据
      sessionStorage.removeItem('ct_report_data')
      sessionStorage.removeItem('current_order_item_id')
      
      // 显示提示信息
      ElMessage.info('报告已清空，可继续为当前患者生成其他报告')
      
      // 可选：跳转到报告列表或详情页
      // router.push('/report/list')
      
    } else {
      ElMessage.error(response.msg || '报告保存失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('报告保存异常，请重试')
      console.error('保存报告失败:', error)
    }
  } finally {
    generating.value = false
  }
}

// ========= 监听 storage 变化（跨标签页/同一页面不同标签） =========
const handleStorageChange = (event: StorageEvent) => {
  if (event.key === 'ct_report_data') {
    console.log('📦 storage变化检测到，重新加载CT数据')
    if (event.newValue) {
      loadCtData(true)
    } else {
      // 数据被清空
      ctData.value = null
    }
  }
}

// ========= 页面初始化读取数据 =========
onMounted(async () => {
  // 1、先加载患者信息
  await loadPatientInfo()
  
  // 2、加载CT影像推理数据（带重试机制）
  await loadCtDataWithRetry()
  
  // 3、监听storage变化（以便其他标签页更新数据时同步）
  window.addEventListener('storage', handleStorageChange)
})

// ========= 页面卸载时移除监听 =========
onBeforeUnmount(() => {
  window.removeEventListener('storage', handleStorageChange)
})
</script>

<style scoped>
.report-generation-page {
  padding: 24px;
  background: #f5f7fa;
  min-height: 100vh;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding: 0 4px;
}

.page-title {
  font-size: 24px;
  font-weight: 700;
  color: #111827;
  margin: 0;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.el-card {
  border-radius: 14px;
  overflow: hidden;
  border: none;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 16px;
  font-weight: 600;
  color: #111827;
}

.card-header .el-icon {
  margin-right: 6px;
}

.header-tags {
  display: flex;
  gap: 8px;
}

.main-row {
  min-height: 600px;
}

.left-col {
  width: 100%;
}

.report-card {
  display: flex;
  flex-direction: column;
}

.report-card :deep(.el-card__body) {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
}

.report-content {
  width: 100%;
}

.report-title-section {
  text-align: center;
  border-bottom: 2px solid #e5e7eb;
  padding-bottom: 14px;
  margin-bottom: 24px;
}

.report-title {
  font-size: 22px;
  font-weight: 700;
  color: #111827;
  margin: 0 0 6px 0;
}

.report-id {
  font-size: 13px;
  color: #6b7280;
}

.report-section {
  margin-bottom: 24px;
  border-bottom: 1px solid #e5e7eb;
  padding-bottom: 20px;
}
.report-section:last-of-type {
  border-bottom: none;
  margin-bottom: 0;
  padding-bottom: 0;
}
.patient-section {
  background: #f9fafb;
  padding: 16px;
  border-radius: 10px;
}
.image-section {
  padding-bottom: 24px;
}
.result-section {
  padding-bottom: 24px;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.section-title .el-icon {
  font-size: 18px;
  color: #409eff;
}

/* 双图并排布局 各占50% */
.image-row {
  display: flex;
  gap: 16px;
  width: 100%;
}
.image-col {
  flex: 1;
  width: 50%;
}

.model-image-section {
  margin-bottom: 0;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  overflow: hidden;
  background: #fff;
}

.model-image-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  background: #f9fafb;
  border-bottom: 1px solid #e5e7eb;
}

.model-image-desc {
  font-size: 12px;
  color: #6b7280;
}

.model-image-wrapper {
  position: relative;
  background: #0f172a;
  padding: 10px;
}

.model-image-wrapper img {
  width: 100%;
  max-height: 320px;
  object-fit: contain;
  cursor: pointer;
  border-radius: 6px;
  transition: opacity 0.24s ease;
}
.model-image-wrapper img:hover {
  opacity: 0.88;
}

.image-overlay-info {
  position: absolute;
  bottom: 16px;
  right: 16px;
  display: flex;
  gap: 12px;
  padding: 8px 14px;
  background: rgba(0, 0, 0, 0.75);
  border-radius: 6px;
  color: #fff;
  font-size: 12px;
}
.image-overlay-info span {
  padding: 3px 8px;
  background: rgba(255, 255, 255, 0.12);
  border-radius: 4px;
}

.empty-thumbs {
  color: #6b7280;
  font-size: 14px;
  padding: 40px 20px;
  text-align: center;
  border: 1px dashed #d1d5db;
  border-radius: 10px;
  background: #f9fafb;
}

/* 模型结果区域美化 */
.model-result-section {
  margin-bottom: 22px;
  padding: 16px 20px;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  background: #f9fafb;
}
.model-result-section:last-child {
  margin-bottom: 0;
}
.comprehensive-block {
  background: #ecf5ff;
  border-color: #b3d8ff;
}

.model-result-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
  padding-bottom: 10px;
  border-bottom: 1px solid #e5e7eb;
}

.model-result-section .el-form-item {
  margin-bottom: 14px;
}
.model-result-section .el-form-item:last-child {
  margin-bottom: 0;
}

.textarea-actions {
  display: flex;
  gap: 10px;
  margin-top: 6px;
  justify-content: flex-end;
}

/* 底部生成按钮区域 */
.generate-footer {
  margin-top: 30px;
  padding-top: 20px;
  border-top: 1px solid #e5e7eb;
  text-align: center;
}
.generate-footer .el-button {
  padding: 14px 40px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 10px;
}

/* 预览对话框 */
.preview-dialog-content {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 400px;
  background: #0f172a;
  border-radius: 8px;
  padding: 20px;
}
.preview-dialog-content img {
  max-width: 100%;
  max-height: 75vh;
  object-fit: contain;
}

/* 重置对话框样式 */
.reset-dialog-content {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  padding: 12px 0;
}

.reset-warning-icon {
  align-self: center;
  margin-bottom: 16px;
}

.reset-dialog-text {
  font-size: 15px;
  color: #4b5563;
  text-align: left;
  line-height: 1.8;
  margin: 0 0 8px 0;
}
.reset-dialog-text strong {
  color: #1f2937;
}

.reset-dialog-list {
  padding-left: 20px;
  margin: 8px 0 12px 0;
  color: #4b5563;
  font-size: 14px;
  line-height: 2;
}
.reset-dialog-list li {
  list-style-type: disc;
}

/* 响应式适配 */
@media (max-width: 992px) {
  .image-row {
    flex-direction: column;
  }
  .image-col {
    width: 100%;
  }
  .model-image-wrapper img {
    max-height: 260px;
  }
  .image-overlay-info {
    position: static;
    margin-top: 10px;
    justify-content: center;
    flex-wrap: wrap;
  }
}

@media (max-width: 768px) {
  .report-generation-page {
    padding: 16px;
  }
  .page-header {
    flex-direction: column;
    align-items: stretch;
    gap: 16px;
  }
  .header-actions {
    width: 100%;
    flex-wrap: wrap;
  }
  .header-actions .el-button {
    flex: 1;
  }
  .report-card :deep(.el-card__body) {
    padding: 16px;
  }
  .generate-footer .el-button {
    width: 100%;
  }
}

@media (max-width: 576px) {
  .card-header {
    flex-direction: column;
    align-items: stretch;
    gap: 10px;
  }
  .header-tags {
    flex-wrap: wrap;
  }
  .model-image-header {
    flex-wrap: wrap;
  }
  .reset-dialog-list {
    padding-left: 16px;
  }
}
</style>
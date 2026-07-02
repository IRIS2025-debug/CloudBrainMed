<template>
  <div class="image-upload-page">
    <!-- 上半部分：影像上传 -->
    <div class="top-section">
      <div class="card upload-card">
        <div class="card-header">影像上传</div>
        <div class="card-body">
          <div class="upload-area">
            <el-upload
              class="upload-demo"
              drag
              action="/api/upload/images"
              multiple
              :on-success="handleUploadSuccess"
              :on-error="handleUploadError"
              :on-progress="handleUploadProgress"
              :file-list="fileList"
              name="files"
            >
              <div class="upload-content">
                <el-icon class="upload-icon"><UploadFilled /></el-icon>
                <div class="upload-text">拖拽或点击上传影像</div>
                <div class="upload-hint">支持 DICOM, JPG, PNG 格式</div>
              </div>
            </el-upload>
          </div>
          
          <!-- 患者信息 -->
          <div class="patient-info">
            <el-form :inline="true" class="patient-form">
              <el-form-item label="患者姓名">
                <el-input v-model="patientInfo.name" placeholder="请输入姓名" />
              </el-form-item>
              <el-form-item label="年龄">
                <el-input-number v-model="patientInfo.age" :min="0" :max="120" />
              </el-form-item>
              <el-form-item label="性别">
                <el-select v-model="patientInfo.gender" placeholder="请选择">
                  <el-option label="男" value="男" />
                  <el-option label="女" value="女" />
                </el-select>
              </el-form-item>
            </el-form>
          </div>
          
          <!-- 三个并列按钮 -->
          <div class="action-buttons">
            <el-button type="primary" @click="handleManualUpload">上传影像</el-button>
            <el-button type="info" @click="showImageViewer">查看上传影像</el-button>
            <el-button type="warning" @click="startAnalysis" :loading="isAnalyzing">
              影像分析
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- 下半部分：影像分析与结果 -->
    <div class="bottom-section">
      <div class="card analysis-card">
        <div class="card-header">
          <span>影像分析</span>
          <el-tag v-if="analysisStatus" :type="analysisStatus === 'completed' ? 'success' : 'warning'">
            {{ analysisStatus === 'completed' ? '分析完成' : '分析中...' }}
          </el-tag>
        </div>
        <div class="card-body">
          <!-- 分析结果 -->
          <div v-if="analysisResult" class="analysis-result">
            <div class="result-item">
              <strong>检测结果:</strong>
              <div v-for="(lesion, idx) in analysisResult.lesions" :key="idx" class="lesion-item">
                <el-tag :type="getTagType(lesion.type)" size="small">{{ lesion.type }}</el-tag>
                <span>位置: {{ lesion.location }}</span>
                <span v-if="lesion.size">大小: {{ lesion.size }}</span>
                <span v-if="lesion.probability">置信度: {{ (lesion.probability * 100).toFixed(1) }}%</span>
              </div>
            </div>
            <div class="result-item">
              <strong>异常发现:</strong>
              <el-tag v-for="(abn, idx) in analysisResult.abnormalities" :key="idx" type="danger" size="small">
                {{ abn }}
              </el-tag>
            </div>
            <div class="result-item">
              <strong>综合置信度:</strong> {{ (analysisResult.confidence * 100).toFixed(1) }}%
            </div>
          </div>
          <div v-else class="empty-analysis">暂无分析结果，请点击"影像分析"按钮</div>

          <!-- 结果操作 -->
          <div class="result-section">
            <div v-if="reportContent" class="result-content">
              <div class="report-header">
                <span>诊断报告</span>
                <el-button type="primary" size="small" @click="downloadReport">下载报告</el-button>
              </div>
              <el-input 
                type="textarea" 
                :rows="8" 
                v-model="reportContent" 
                readonly 
                class="report-textarea"
              />
            </div>
            <div class="result-footer">
              <el-button type="warning" @click="reAnalyze">重新分析</el-button>
              <el-button type="success" @click="saveResult">保存</el-button>
              <el-button type="primary" @click="exportResult" :disabled="!reportContent">导出报告</el-button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 影像查看弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      title="查看上传影像"
      width="80%"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <div class="image-viewer">
        <div v-if="imageList.length === 0" class="empty-text">暂无影像</div>
        <div v-else class="image-viewer-grid">
          <div v-for="(img, idx) in imageList" :key="idx" class="viewer-item">
            <el-image
              :src="img.url"
              :preview-src-list="imageUrls"
              :initial-index="idx"
              fit="contain"
              class="viewer-image"
            >
              <template #error>
                <div class="image-error">加载失败</div>
              </template>
            </el-image>
            <div class="viewer-item-name">{{ img.name }}</div>
            <div class="viewer-item-status">
              <el-tag v-if="img.analyzed" type="success" size="small">已分析</el-tag>
              <el-tag v-else type="info" size="small">待分析</el-tag>
            </div>
          </div>
        </div>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">关闭</el-button>
          <el-button type="primary" @click="dialogVisible = false">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import axios from 'axios'

// 文件列表
const fileList = ref<any[]>([])
const imageList = ref<{ name: string; url: string; analyzed?: boolean }[]>([])

// 患者信息
const patientInfo = reactive({
  name: '',
  age: null as number | null,
  gender: ''
})

// 分析结果
const analysisResult = ref<any>(null)
const reportContent = ref('')
const analysisStatus = ref('')
const isAnalyzing = ref(false)

// 弹窗控制
const dialogVisible = ref(false)
const uploadedFiles = ref<any[]>([])

// 计算所有图片URL用于预览
const imageUrls = computed(() => {
  return imageList.value.map(img => img.url)
})

// 上传成功
const handleUploadSuccess = (response: any, file: any) => {
  if (response.status === 'success') {
    const fileData = response.files[0]
    if (fileData) {
      // 添加到图片列表
      const reader = new FileReader()
      reader.onload = (e) => {
        imageList.value.push({
          name: fileData.filename,
          url: e.target?.result as string,
          analyzed: false
        })
      }
      reader.readAsDataURL(file.raw)
      
      uploadedFiles.value.push(fileData)
      ElMessage.success(`上传成功: ${fileData.filename}`)
    }
  }
}

const handleUploadError = (error: any) => {
  ElMessage.error('上传失败: ' + (error.message || '未知错误'))
}

const handleUploadProgress = (event: any) => {
  // 处理上传进度
}

// 手动上传
const handleManualUpload = () => {
  if (fileList.value.length === 0) {
    ElMessage.warning('请先选择影像文件')
    return
  }
  ElMessage.success(`成功上传 ${fileList.value.length} 个文件`)
}

// 查看上传影像
const showImageViewer = () => {
  if (imageList.value.length === 0) {
    ElMessage.warning('暂无影像可查看')
    return
  }
  dialogVisible.value = true
}

// 获取标签类型
const getTagType = (type: string) => {
  const typeMap: Record<string, string> = {
    '脑肿瘤': 'danger',
    '水肿': 'warning',
    '占位性病变': 'danger'
  }
  return typeMap[type] || 'info'
}

// 开始分析
const startAnalysis = async () => {
  if (imageList.value.length === 0) {
    ElMessage.warning('请先上传影像')
    return
  }

  if (!uploadedFiles.value.length) {
    ElMessage.warning('请先上传影像文件')
    return
  }

  try {
    isAnalyzing.value = true
    analysisStatus.value = 'analyzing'
    
    // 使用第一个上传的文件进行分析
    const imagePath = uploadedFiles.value[0].path
    
    const response = await axios.post('/api/analysis/image', {
      image_path: imagePath,
      patient_info: {
        name: patientInfo.name || '未知',
        age: patientInfo.age || '未知',
        gender: patientInfo.gender || '未知'
      }
    })
    
    if (response.data.status === 'success') {
      analysisResult.value = response.data.detection_result
      reportContent.value = response.data.report
      analysisStatus.value = 'completed'
      
      // 标记图片已分析
      if (imageList.value.length > 0) {
        imageList.value[0].analyzed = true
      }
      
      ElMessage.success('分析完成，报告已生成')
    } else {
      ElMessage.error(response.data.message || '分析失败')
    }
  } catch (error: any) {
    console.error('分析失败:', error)
    ElMessage.error('分析失败: ' + (error.response?.data?.detail || error.message))
  } finally {
    isAnalyzing.value = false
  }
}

// 重新分析
const reAnalyze = () => {
  ElMessageBox.confirm('确定要重新分析吗？当前结果将被清除。', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    analysisResult.value = null
    reportContent.value = ''
    analysisStatus.value = ''
    ElMessage.info('已重置，可重新分析')
  }).catch(() => {})
}

// 保存结果
const saveResult = async () => {
  if (!reportContent.value) {
    ElMessage.warning('请先进行影像分析')
    return
  }
  
  try {
    // 保存到本地存储
    const savedReports = JSON.parse(localStorage.getItem('diagnosis_reports') || '[]')
    savedReports.push({
      id: Date.now(),
      content: reportContent.value,
      patient: patientInfo,
      result: analysisResult.value,
      savedAt: new Date().toISOString()
    })
    localStorage.setItem('diagnosis_reports', JSON.stringify(savedReports))
    ElMessage.success('报告已保存')
  } catch (error) {
    ElMessage.error('保存失败')
  }
}

// 导出报告
const exportResult = () => {
  if (!reportContent.value) {
    ElMessage.warning('请先进行影像分析')
    return
  }
  
  // 创建下载
  const blob = new Blob([reportContent.value], { type: 'text/plain;charset=utf-8' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = `诊断报告_${new Date().toISOString().slice(0,10)}.txt`
  link.click()
  URL.revokeObjectURL(link.href)
  
  ElMessage.success('报告导出成功')
}

// 下载报告
const downloadReport = exportResult
</script>

<style scoped>
/* 保持原有样式，添加一些新的样式 */

.patient-info {
  margin: 16px 0;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 6px;
}

.patient-form {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.patient-form .el-form-item {
  margin-bottom: 0;
}

.upload-hint {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.result-item {
  padding: 8px 0;
  border-bottom: 1px solid #e5e6eb;
}

.result-item:last-child {
  border-bottom: none;
}

.lesion-item {
  padding: 4px 8px;
  margin: 4px 0;
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

.report-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.report-textarea :deep(.el-textarea__inner) {
  font-family: 'Courier New', monospace;
  font-size: 13px;
  line-height: 1.6;
  background-color: #fafbfc;
}

.viewer-item-status {
  margin-top: 4px;
}
</style>
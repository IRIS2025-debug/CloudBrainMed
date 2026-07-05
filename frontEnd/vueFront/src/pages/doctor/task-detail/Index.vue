<template>
  <div class="task-detail-page">
    <!-- 顶部导航 -->
    <div class="page-header">
      <el-button class="back-btn" text @click="$router.back()">
        <el-icon><ArrowLeft /></el-icon>
        <span>返回</span>
      </el-button>
      <div class="header-right">
        <span class="page-title">任务详情</span>
        <span class="order-id">#{{ orderItemId?.substring(0, 12) }}...</span>
      </div>
    </div>

    <div v-loading="loading" class="content-wrapper">
      <template v-if="task">
        <!-- 状态横幅 -->
        <div class="status-banner" :class="'banner-' + task.status">
          <div class="banner-left">
            <div class="banner-icon">
              <el-icon v-if="task.status === 'QUEUED'" :size="24"><Clock /></el-icon>
              <el-icon v-else-if="task.status === 'IN_PROCESS'" :size="24"><Loading /></el-icon>
              <el-icon v-else-if="task.status === 'COMPLETED'" :size="24"><CircleCheckFilled /></el-icon>
              <el-icon v-else :size="24"><InfoFilled /></el-icon>
            </div>
            <div class="banner-info">
              <div class="banner-title">{{ task.itemName }}</div>
              <div class="banner-meta">
                <el-tag :type="urgencyTagType(task.urgencyLevel)" size="small" effect="dark">
                  {{ task.urgencyLabel }}
                </el-tag>
                <el-tag :type="statusTagType(task.status)" size="small" effect="plain">
                  {{ task.statusLabel }}
                </el-tag>
                <span class="banner-code">{{ task.itemCode }}</span>
              </div>
            </div>
          </div>
          <div class="banner-actions">
            <el-button
              v-if="task.status === 'QUEUED'"
              type="success"
              size="large"
              :icon="VideoPlay"
              @click="handleStart"
            >
              开始处理
            </el-button>
            <template v-if="task.status === 'IN_PROCESS'">
              <el-button
                v-if="task.itemCategory === 'LAB'"
                type="primary"
                size="large"
                :icon="DocumentAdd"
                @click="handleGenerateReport"
              >
                生成检验报告
              </el-button>
              <el-button
                type="warning"
                size="large"
                :icon="CircleCheck"
                @click="handleComplete"
              >
                标记完成
              </el-button>
            </template>
          </div>
        </div>

        <div class="cards-row">
          <!-- 左列 -->
          <div class="main-column">
            <!-- 项目信息卡片 -->
            <div class="info-card">
              <div class="card-title">
                <el-icon><Collection /></el-icon>
                <span>项目信息</span>
              </div>
              <div class="kv-grid">
                <div class="kv-item">
                  <span class="kv-label">项目编码</span>
                  <span class="kv-value">{{ task.itemCode }}</span>
                </div>
                <div class="kv-item">
                  <span class="kv-label">项目类别</span>
                  <el-tag size="small" :type="task.itemCategory === 'LAB' ? 'info' : 'warning'">
                    {{ task.itemCategoryLabel }}
                  </el-tag>
                </div>
                <div class="kv-item">
                  <span class="kv-label">价格</span>
                  <span class="kv-value price">¥{{ task.price }}</span>
                </div>
                <div class="kv-item">
                  <span class="kv-label">申请医生</span>
                  <span class="kv-value">{{ task.requesterDoctorId || '-' }}</span>
                </div>
              </div>
            </div>

            <!-- 临床摘要卡片 -->
            <div class="info-card" v-if="task.clinicalSummary">
              <div class="card-title">
                <el-icon><Notebook /></el-icon>
                <span>临床摘要</span>
              </div>
              <div class="clinical-content"><pre>{{ task.clinicalSummary }}</pre></div>
            </div>

            <!-- 时间线卡片 -->
            <div class="info-card">
              <div class="card-title">
                <el-icon><Timer /></el-icon>
                <span>时间记录</span>
              </div>
              <div class="time-line">
                <div class="time-item">
                  <div class="time-dot"></div>
                  <div class="time-body">
                    <span class="time-label">创建时间</span>
                    <span class="time-value">{{ task.createTime }}</span>
                  </div>
                </div>
                <div class="time-item" v-if="task.assignTime">
                  <div class="time-dot active"></div>
                  <div class="time-body">
                    <span class="time-label">分配时间</span>
                    <span class="time-value">{{ task.assignTime }}</span>
                  </div>
                </div>
                <div class="time-item" v-if="task.completeTime">
                  <div class="time-dot done"></div>
                  <div class="time-body">
                    <span class="time-label">完成时间</span>
                    <span class="time-value">{{ task.completeTime }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 右列：患者卡片 -->
          <div class="side-column">
            <div class="patient-card">
              <div class="patient-avatar">
                <el-icon :size="48"><UserFilled /></el-icon>
              </div>
              <div class="patient-name">{{ task.patientName }}</div>
              <div class="patient-meta">
                <span>{{ task.genderLabel }}</span>
                <span class="dot">·</span>
                <span>{{ task.age }}岁</span>
              </div>
              <div class="patient-divider"></div>
              <div class="patient-extra">
                <div class="extra-item">
                  <span class="extra-label">患者ID</span>
                  <span class="extra-value">{{ task.patientId }}</span>
                </div>
                <div class="extra-item">
                  <span class="extra-label">挂号ID</span>
                  <span class="extra-value">{{ task.registerId }}</span>
                </div>
                <div class="extra-item" v-if="task.birthday">
                  <span class="extra-label">出生日期</span>
                  <span class="extra-value">{{ task.birthday }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </template>

      <div v-else class="empty-state">
        <el-icon :size="48"><WarningFilled /></el-icon>
        <p>任务不存在或已删除</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getTaskDetail, startTask, completeTask } from '@/api/doctor/task'
import { generateLabReport } from '@/api/doctor/task'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowLeft, Clock, Loading, CircleCheckFilled, InfoFilled,
  VideoPlay, CircleCheck, DocumentAdd, Collection, Notebook,
  Timer, UserFilled, WarningFilled
} from '@element-plus/icons-vue'

const route = useRoute()
const task = ref<any>(null)
const loading = ref(false)
const orderItemId = route.params.id as string

onMounted(() => fetchDetail())

async function fetchDetail() {
  loading.value = true
  try {
    const res = await getTaskDetail(orderItemId)
    task.value = res.data
  } catch {
    ElMessage.error('加载任务详情失败')
  } finally {
    loading.value = false
  }
}

function urgencyTagType(level: string) {
  return { EMERGENCY: 'danger', URGENT: 'warning', NORMAL: 'success' }[level] || 'info'
}

function statusTagType(status: string) {
  return { QUEUED: '', IN_PROCESS: 'warning', COMPLETED: 'success' }[status] || 'info'
}

async function handleStart() {
  try {
    await ElMessageBox.confirm('确认开始处理此任务？', '提示', { type: 'info' })
    await startTask(orderItemId)
    ElMessage.success('已开始处理')
    fetchDetail()
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error(e?.response?.data?.msg || '操作失败')
  }
}

async function handleComplete() {
  try {
    await ElMessageBox.confirm('确认完成此任务？', '提示', { type: 'info' })
    await completeTask(orderItemId)
    ElMessage.success('任务已完成')
    fetchDetail()
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error(e?.response?.data?.msg || '操作失败')
  }
}

async function handleGenerateReport() {
  ElMessage.info('生成检验报告功能开发中，请稍后...')
}
</script>

<style scoped>
.task-detail-page {
  min-height: 100vh;
  background: #f0f2f5;
  padding: 0 0 32px;
}

/* ── 顶部导航 ── */
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 28px;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0,0,0,.04);
  position: sticky;
  top: 0;
  z-index: 10;
}
.back-btn {
  font-size: 14px;
  color: #606266;
}
.back-btn:hover { color: #409eff; }
.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}
.page-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}
.order-id {
  font-size: 12px;
  color: #c0c4cc;
  font-family: monospace;
}

/* ── 内容区 ── */
.content-wrapper {
  max-width: 1100px;
  margin: 0 auto;
  padding: 24px 28px 0;
}

/* ── 状态横幅 ── */
.status-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24px 28px;
  border-radius: 12px;
  margin-bottom: 20px;
  gap: 20px;
}
.banner-QUEUED     { background: linear-gradient(135deg, #ecf5ff, #d9ecff); }
.banner-IN_PROCESS { background: linear-gradient(135deg, #fdf6ec, #faecd8); }
.banner-COMPLETED  { background: linear-gradient(135deg, #f0f9eb, #e1f3d8); }
.banner-WAITING_ASSIGN { background: linear-gradient(135deg, #f4f4f5, #e9e9eb); }

.banner-left {
  display: flex;
  align-items: center;
  gap: 16px;
}
.banner-icon {
  width: 52px; height: 52px;
  display: flex; align-items: center; justify-content: center;
  border-radius: 12px;
  background: rgba(255,255,255,.7);
  color: #409eff;
}
.banner-QUEUED .banner-icon     { color: #409eff; }
.banner-IN_PROCESS .banner-icon { color: #e6a23c; }
.banner-COMPLETED .banner-icon  { color: #67c23a; }

.banner-title {
  font-size: 20px;
  font-weight: 700;
  color: #303133;
  margin-bottom: 6px;
}
.banner-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}
.banner-code {
  font-size: 12px;
  color: #909399;
  font-family: monospace;
}
.banner-actions {
  display: flex;
  gap: 10px;
  flex-shrink: 0;
}

/* ── 双列布局 ── */
.cards-row {
  display: flex;
  gap: 20px;
  align-items: flex-start;
}
.main-column {
  flex: 1;
  min-width: 0;
}
.side-column {
  width: 300px;
  flex-shrink: 0;
}

/* ── 信息卡片 ── */
.info-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px 24px;
  margin-bottom: 16px;
  box-shadow: 0 1px 3px rgba(0,0,0,.04);
}
.card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}
.card-title .el-icon { color: #909399; }

/* ── KV 网格 ── */
.kv-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px 20px;
}
.kv-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.kv-label {
  font-size: 12px;
  color: #909399;
}
.kv-value {
  font-size: 14px;
  color: #303133;
  font-weight: 500;
}
.kv-value.price {
  color: #f56c6c;
  font-size: 16px;
  font-weight: 700;
}

/* ── 临床摘要 ── */
.clinical-content {
  font-size: 14px;
  color: #606266;
  line-height: 1.8;
  background: #fafbfc;
  padding: 14px 16px;
  border-radius: 8px;
  border-left: 3px solid #409eff;
}
.clinical-content pre {
  margin: 0;
  font-family: inherit;
  font-size: inherit;
  color: inherit;
  line-height: inherit;
  white-space: pre-wrap;
  word-break: break-word;
}

/* ── 时间线 ── */
.time-line {
  padding-left: 4px;
}
.time-item {
  display: flex;
  gap: 14px;
  padding-bottom: 16px;
  position: relative;
}
.time-item:not(:last-child)::after {
  content: '';
  position: absolute;
  left: 5px;
  top: 14px;
  bottom: 0;
  width: 1px;
  background: #e4e7ed;
}
.time-dot {
  width: 10px; height: 10px;
  border-radius: 50%;
  background: #dcdfe6;
  margin-top: 4px;
  flex-shrink: 0;
}
.time-dot.active { background: #409eff; }
.time-dot.done   { background: #67c23a; }
.time-body {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.time-label {
  font-size: 12px;
  color: #909399;
}
.time-value {
  font-size: 14px;
  color: #303133;
}

/* ── 患者卡片 ── */
.patient-card {
  background: #fff;
  border-radius: 12px;
  padding: 28px 24px 24px;
  text-align: center;
  box-shadow: 0 1px 3px rgba(0,0,0,.04);
}
.patient-avatar {
  width: 72px; height: 72px;
  margin: 0 auto 12px;
  border-radius: 50%;
  background: #ecf5ff;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #409eff;
}
.patient-name {
  font-size: 18px;
  font-weight: 700;
  color: #303133;
  margin-bottom: 4px;
}
.patient-meta {
  font-size: 13px;
  color: #909399;
  margin-bottom: 16px;
}
.patient-meta .dot { margin: 0 6px; }
.patient-divider {
  height: 1px;
  background: #f0f0f0;
  margin: 0 0 16px;
}
.patient-extra {
  text-align: left;
}
.extra-item {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
}
.extra-item + .extra-item {
  border-top: 1px solid #fafafa;
}
.extra-label {
  font-size: 12px;
  color: #909399;
}
.extra-value {
  font-size: 13px;
  color: #303133;
  font-family: monospace;
  max-width: 160px;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* ── 空状态 ── */
.empty-state {
  text-align: center;
  padding: 80px 0;
  color: #c0c4cc;
}
.empty-state p {
  margin-top: 12px;
  font-size: 14px;
}

/* ── 响应式 ── */
@media (max-width: 768px) {
  .cards-row { flex-direction: column-reverse; }
  .side-column { width: 100%; }
  .status-banner { flex-direction: column; text-align: center; }
  .banner-left { flex-direction: column; }
  .kv-grid { grid-template-columns: 1fr; }
}
</style>
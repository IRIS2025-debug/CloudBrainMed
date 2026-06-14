<template>
  <div class="page">
    <header class="page-top">
      <div>
        <h2>样本标注</h2>
        <p class="top-sub">AI 反馈样本、标签管理</p>
      </div>
    </header>

    <div class="card">
      <div class="card-head">反馈样本池</div>
      <el-table :data="samples" stripe v-loading="loading" class="sample-table">
        <el-table-column prop="sampleId" label="样本 ID" width="220" show-overflow-tooltip />
        <el-table-column label="标签" width="120">
          <template #default="{ row }">
            <span v-if="row.labelTag" class="label-tag" :class="row.usedForTraining ? 'tag-trained' : 'tag-pending'">
              {{ row.labelTag }}
            </span>
            <span v-else class="label-tag tag-none">未标注</span>
          </template>
        </el-table-column>
        <el-table-column label="采纳" width="70">
          <template #default="{ row }">
            <span class="adopt-dot" :class="row.isAdopted ? 'adopt-yes' : 'adopt-no'">
              {{ row.isAdopted ? '是' : '否' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="diffScore" label="差异分" width="90" />
        <el-table-column prop="doctorId" label="修正医生" min-width="180" show-overflow-tooltip />
        <el-table-column label="已训练" width="80">
          <template #default="{ row }">
            <el-icon v-if="row.usedForTraining" color="#22c55e" :size="18"><Check /></el-icon>
            <span v-else class="dash">--</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row }">
            <el-button v-if="!row.labelTag" type="primary" link @click="openLabel(row)">标注</el-button>
            <span v-else class="time-text">{{ row.createdAt?.slice(0, 10) }}</span>
          </template>
        </el-table-column>
      </el-table>

      <div class="table-footer">
        <span class="tf-total">共 {{ samples.length }} 条</span>
        <el-pagination v-model:current-page="page" :page-size="20" layout="prev, pager, next" @current-change="fetchSamples" size="small" />
      </div>
    </div>

    <el-dialog v-model="dialogVisible" title="样本标注" width="520px" class="label-dialog">
      <el-form label-position="top">
        <el-form-item label="AI 输出">
          <div class="code-block">{{ currentSample?.aiOutputJson || '--' }}</div>
        </el-form-item>
        <el-form-item label="最终输出">
          <div class="code-block">{{ currentSample?.finalOutputJson || '--' }}</div>
        </el-form-item>
        <el-form-item label="选择标签">
          <div class="tag-select">
            <button v-for="opt in tagOptions" :key="opt" class="tag-btn" :class="{ active: labelTag === opt }" @click="labelTag = opt">{{ opt }}</button>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitLabel" :loading="labeling">确认标注</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getSampleList, labelSample } from '@/api/admin/ml'

const samples = ref<any[]>([])
const loading = ref(false)
const page = ref(1)
const dialogVisible = ref(false)
const currentSample = ref<any>(null)
const labelTag = ref('')
const labeling = ref(false)
const tagOptions = ['问诊', '分诊', '报告', '药品']

async function fetchSamples() {
  loading.value = true
  try {
    const res = await getSampleList({ page: page.value, limit: 20 })
    samples.value = res.data || []
  } finally { loading.value = false }
}

function openLabel(row: any) {
  currentSample.value = row
  labelTag.value = ''
  dialogVisible.value = true
}

async function submitLabel() {
  if (!labelTag.value) return
  labeling.value = true
  try {
    await labelSample({ sampleId: currentSample.value.sampleId, labelTag: labelTag.value })
    ElMessage.success('标注成功')
    dialogVisible.value = false
    fetchSamples()
  } finally { labeling.value = false }
}

fetchSamples()
</script>

<style scoped>
.page { padding: 28px 36px; }
.page-top { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 28px; }
.page-top h2 { font-size: 22px; font-weight: 700; color: #0f172a; }
.top-sub { font-size: 13px; color: #94a3b8; margin-top: 4px; }

.card { background: #fff; border-radius: var(--radius); padding: 24px; box-shadow: var(--shadow); }
.card-head { font-size: 15px; font-weight: 700; color: #1e293b; margin-bottom: 18px; padding-bottom: 12px; border-bottom: 1px solid #f1f5f9; }

.sample-table :deep(th) { background: #f8fafc; color: #64748b; font-weight: 600; font-size: 13px; border-bottom: none; }
.sample-table :deep(td) { font-size: 14px; }

.label-tag { font-size: 12px; font-weight: 600; padding: 3px 10px; border-radius: 12px; }
.tag-trained { background: #d1fae5; color: #065f46; }
.tag-pending { background: #fef3c7; color: #b45309; }
.tag-none { background: #f1f5f9; color: #94a3b8; }

.adopt-dot { font-size: 13px; font-weight: 600; }
.adopt-yes { color: #22c55e; }
.adopt-no { color: #ef4444; }

.dash { color: #cbd5e1; }
.time-text { font-size: 12px; color: #94a3b8; }

.table-footer { display: flex; justify-content: space-between; align-items: center; padding-top: 14px; margin-top: 6px; border-top: 1px solid #f1f5f9; }
.tf-total { font-size: 13px; color: #94a3b8; }

.label-dialog :deep(.el-dialog__header) { border-bottom: 1px solid #f1f5f9; padding-bottom: 16px; }
.code-block { background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px; padding: 12px; font-size: 13px; color: #475569; max-height: 120px; overflow-y: auto; white-space: pre-wrap; word-break: break-all; font-family: 'SF Mono', 'Cascadia Code', monospace; }
.tag-select { display: flex; gap: 8px; flex-wrap: wrap; }
.tag-btn { padding: 6px 18px; border: 1.5px solid #e2e8f0; border-radius: 20px; background: #fff; color: #475569; font-size: 13px; cursor: pointer; transition: all .15s ease; font-family: inherit; }
.tag-btn:hover { border-color: #93c5fd; color: #2563eb; }
.tag-btn.active { background: #2563eb; border-color: #2563eb; color: #fff; }
</style>

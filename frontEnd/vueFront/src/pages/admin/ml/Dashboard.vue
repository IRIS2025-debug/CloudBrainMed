<template>
  <div class="page">
    <header class="page-top">
      <div>
        <h2>AI 推理看板</h2>
        <p class="top-sub">成功率、采纳率、耗时统计</p>
      </div>
    </header>

    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-icon" style="background:#2563eb;"><el-icon :size="18"><DataAnalysis /></el-icon></div>
        <div>
          <div class="stat-value">{{ stats.todayTotal ?? '--' }}</div>
          <div class="stat-label">今日推理总量</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background:#22c55e;"><el-icon :size="18"><Check /></el-icon></div>
        <div>
          <div class="stat-value">{{ stats.successRate ?? '--' }}</div>
          <div class="stat-label">成功率</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background:#7c3aed;"><el-icon :size="18"><Star /></el-icon></div>
        <div>
          <div class="stat-value">{{ stats.adoptionRate ?? 0 }}</div>
          <div class="stat-label">采纳率</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background:#f59e0b;"><el-icon :size="18"><Timer /></el-icon></div>
        <div>
          <div class="stat-value">{{ stats.avgLatencyMs ?? stats.avgLatency ?? '--' }}<span class="unit"> ms</span></div>
          <div class="stat-label">平均耗时</div>
        </div>
      </div>
    </div>

    <div class="card">
      <div class="card-head">已注册模型</div>
      <el-table :data="models" stripe class="model-table" v-loading="loadingModels">
        <el-table-column prop="modelKey" label="模型名称" min-width="180" />
        <el-table-column prop="version" label="版本" width="140" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <span class="status-tag" :class="'st-' + statusTag(row.status)">{{ row.status }}</span>
          </template>
        </el-table-column>
        <el-table-column label="流量占比" width="260">
          <template #default="{ row }">
            <el-progress :percentage="row.trafficPct" :stroke-width="10" :color="progressColor(row.trafficPct)" />
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Check, DataAnalysis, Star, Timer } from '@element-plus/icons-vue'
import { getInferenceStats, getModelList } from '@/api/admin/ml'

const stats = ref<any>({})
const models = ref<any[]>([])
const loadingModels = ref(false)

function statusTag(status: string) {
  const map: Record<string, string> = { '上线': 'success', '测试': 'warning', '下线': 'info', '废弃': 'danger' }
  return map[status] || 'info'
}

function progressColor(pct: number) {
  if (pct >= 80) return '#2563eb'
  if (pct >= 40) return '#0d9488'
  return '#94a3b8'
}

onMounted(async () => {
  try {
    const res = await getInferenceStats()
    stats.value = res.data || {}
  } catch {
    // 统一请求层负责错误提示。
  }

  loadingModels.value = true
  try {
    const res = await getModelList()
    models.value = res.data || []
  } catch {
    // 统一请求层负责错误提示。
  } finally {
    loadingModels.value = false
  }
})
</script>

<style scoped>
.page { padding: 28px 36px; }
.page-top { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 28px; }
.page-top h2 { font-size: 22px; font-weight: 700; color: #0f172a; }
.top-sub { font-size: 13px; color: #94a3b8; margin-top: 4px; }

.stats-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 28px; }
.stat-card { background: #fff; border-radius: var(--radius); padding: 20px 24px; display: flex; align-items: center; gap: 16px; box-shadow: var(--shadow); }
.stat-icon { width: 44px; height: 44px; border-radius: 10px; display: flex; align-items: center; justify-content: center; color: #fff; flex-shrink: 0; }
.stat-value { font-size: 24px; font-weight: 700; color: #0f172a; }
.stat-value .unit { font-size: 14px; font-weight: 500; color: #94a3b8; }
.stat-label { font-size: 13px; color: #94a3b8; margin-top: 2px; }

.card { background: #fff; border-radius: var(--radius); padding: 24px; box-shadow: var(--shadow); }
.card-head { font-size: 15px; font-weight: 700; color: #1e293b; margin-bottom: 18px; padding-bottom: 12px; border-bottom: 1px solid #f1f5f9; }

.model-table :deep(th) { background: #f8fafc; color: #64748b; font-weight: 600; font-size: 13px; border-bottom: none; }
.model-table :deep(td) { font-size: 14px; }

.status-tag { font-size: 12px; font-weight: 600; padding: 3px 10px; border-radius: 12px; }
.st-success { background: #d1fae5; color: #065f46; }
.st-warning { background: #fef3c7; color: #b45309; }
.st-info { background: #dbeafe; color: #1d4ed8; }
.st-danger { background: #fee2e2; color: #991b1b; }
</style>

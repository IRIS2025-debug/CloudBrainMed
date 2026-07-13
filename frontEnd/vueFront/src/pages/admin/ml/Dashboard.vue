<template>
  <div class="page">
    <header class="page-top">
      <div>
        <h2>AI 推理看板</h2>
        <p class="top-sub">今日推理量、成功率与两个业务模型状态</p>
      </div>
      <el-button size="small" :loading="loadingStats || loadingModels" @click="loadAll">
        <el-icon style="margin-right:4px"><Refresh /></el-icon>刷新
      </el-button>
    </header>

    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-icon" style="background:#2563eb;"><el-icon :size="18"><DataAnalysis /></el-icon></div>
        <div>
          <div class="stat-value">{{ metric(stats.todayTotal) }}</div>
          <div class="stat-label">今日推理总量</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background:#22c55e;"><el-icon :size="18"><Check /></el-icon></div>
        <div>
          <div class="stat-value">{{ metric(stats.successRate) }}<span class="unit" v-if="stats.successRate != null"> %</span></div>
          <div class="stat-label">成功率</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background:#f59e0b;"><el-icon :size="18"><Timer /></el-icon></div>
        <div>
          <div class="stat-value">{{ metric(stats.avgLatency) }}<span class="unit" v-if="stats.avgLatency != null"> ms</span></div>
          <div class="stat-label">平均耗时</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background:#7c3aed;"><el-icon :size="18"><Cpu /></el-icon></div>
        <div>
          <!-- 请求失败显示 --，绝不把失败伪装成 0 个部署模型 -->
          <div class="stat-value">{{ modelsError ? '--' : models.length }}</div>
          <div class="stat-label">部署模型数</div>
        </div>
      </div>
    </div>

    <div class="card">
      <div class="card-head">已注册模型</div>
      <!-- 模型列表加载失败：显示重试入口，不展示空表伪装成“没有模型” -->
      <div class="model-error" v-if="modelsError && !loadingModels">
        <span>模型列表加载失败</span>
        <el-button size="small" text type="primary" @click="loadModels">重试</el-button>
      </div>
      <el-table v-else :data="models" stripe class="model-table" v-loading="loadingModels">
        <el-table-column label="模型名称" min-width="200">
          <template #default="{ row }">
            <div class="model-name">{{ row.displayName }}</div>
            <div class="model-key">{{ row.modelKey }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="version" label="版本" min-width="160" show-overflow-tooltip />
        <el-table-column prop="modelType" label="类型" width="140" show-overflow-tooltip />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <span class="status-tag" :class="'st-' + statusClass(row.status)">{{ statusLabel(row.status) }}</span>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Check, Cpu, DataAnalysis, Refresh, Timer } from '@element-plus/icons-vue'
import { getInferenceStats, getModelList } from '@/api/admin/ml'

// null 表示加载中或请求失败，模板显示 --；真实 0 会照常显示。
const stats = ref<{ todayTotal: number | null; successRate: number | null; avgLatency: number | null }>({
  todayTotal: null,
  successRate: null,
  avgLatency: null,
})
const models = ref<Array<Record<string, any>>>([])
const loadingStats = ref(false)
const loadingModels = ref(false)
const modelsError = ref(false)

// 状态值 READY/FALLBACK/OFFLINE 映射为中文，单行显示。
const STATUS_TEXT: Record<string, string> = {
  READY: '就绪',
  FALLBACK: '降级',
  OFFLINE: '离线',
}
const STATUS_CLASS: Record<string, string> = {
  READY: 'success',
  FALLBACK: 'warning',
  OFFLINE: 'info',
}

function statusLabel(status: string) {
  return STATUS_TEXT[status] || status
}
function statusClass(status: string) {
  return STATUS_CLASS[status] || 'info'
}
function metric(value: number | null | undefined): string {
  return value === null || value === undefined ? '--' : String(value)
}

async function loadStats() {
  loadingStats.value = true
  try {
    const res = await getInferenceStats()
    const data = res.data || {}
    stats.value = {
      todayTotal: numOrNull(data.todayTotal),
      successRate: numOrNull(data.successRate),
      avgLatency: numOrNull(data.avgLatency),
    }
  } catch {
    stats.value = { todayTotal: null, successRate: null, avgLatency: null }
  } finally {
    loadingStats.value = false
  }
}

async function loadModels() {
  loadingModels.value = true
  modelsError.value = false
  try {
    const res = await getModelList()
    models.value = Array.isArray(res.data) ? res.data : []
  } catch {
    // 失败时清空并置错误位，部署模型数显示 --、表格显示重试。
    models.value = []
    modelsError.value = true
  } finally {
    loadingModels.value = false
  }
}

function numOrNull(value: unknown): number | null {
  if (value === null || value === undefined || value === '') return null
  const n = Number(value)
  return Number.isFinite(n) ? n : null
}

function loadAll() {
  loadStats()
  loadModels()
}

onMounted(loadAll)
</script>

<style scoped>
.page { padding: 28px 36px; }
.page-top { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 28px; gap: 16px; }
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
.model-name { font-weight: 600; color: #1e293b; }
.model-key { font-size: 12px; color: #94a3b8; margin-top: 2px; }
.model-error { display: flex; align-items: center; justify-content: center; gap: 8px; color: #ef4444; font-size: 13px; padding: 28px 0; }

/* 状态标签强制单行，长文本不换行 */
.status-tag { display: inline-block; font-size: 12px; font-weight: 600; padding: 3px 10px; border-radius: 12px; white-space: nowrap; }
.st-success { background: #d1fae5; color: #065f46; }
.st-warning { background: #fef3c7; color: #b45309; }
.st-info { background: #e2e8f0; color: #475569; }
</style>

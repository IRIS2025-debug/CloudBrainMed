<template>
  <div class="page">
    <section class="hero-panel">
      <div>
        <p class="hero-eyebrow">AI 推理看板</p>
        <h2>模型表现总览</h2>
        <p class="hero-copy">查看推理量、成功率与平均耗时，并在下方快速核对当前可用推理模块。</p>
      </div>
      <div class="hero-pill"><span class="pill-dot"></span><span>实时数据展示</span></div>
    </section>

    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-icon is-blue"><el-icon :size="18"><DataAnalysis /></el-icon></div>
        <div><div class="stat-value">{{ stats.todayTotal ?? '--' }}</div><div class="stat-label">今日推理总量</div><div class="stat-note">当日 AI 推理调用次数</div></div>
      </div>
      <div class="stat-card">
        <div class="stat-icon is-green"><el-icon :size="18"><Check /></el-icon></div>
        <div><div class="stat-value">{{ stats.successRate ?? '--' }}<span class="unit"> %</span></div><div class="stat-label">成功率</div><div class="stat-note">推理流程成功返回占比</div></div>
      </div>
      <div class="stat-card">
        <div class="stat-icon is-amber"><el-icon :size="18"><Timer /></el-icon></div>
        <div><div class="stat-value">{{ stats.avgLatencyMs ?? stats.avgLatency ?? '--' }}<span class="unit"> ms</span></div><div class="stat-label">平均耗时</div><div class="stat-note">单次推理平均响应时间</div></div>
      </div>
    </div>

    <section class="panel">
      <div class="panel-head"><div><h3>已注册模型</h3><p>模型注册与流量切换已下线，这里仅展示当前保留的推理模块。</p></div></div>
      <el-table :data="models" stripe class="model-table">
        <el-table-column label="模型名称" min-width="180"><template #default="{ row }">{{ modelName(row) }}</template></el-table-column>
        <el-table-column prop="modelKey" label="模型标识" min-width="160" />
        <el-table-column prop="version" label="版本" width="140" />
        <el-table-column label="状态" width="120" align="center"><template #default="{ row }"><span class="status-tag" :class="'st-' + statusTag(row.status)">{{ statusText(row.status) }}</span></template></el-table-column>
        <el-table-column label="流量占比" width="260"><template #default="{ row }"><el-progress :percentage="row.trafficPct" :stroke-width="10" :color="progressColor(row.trafficPct)" /></template></el-table-column>
      </el-table>
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Check, DataAnalysis, Timer } from '@element-plus/icons-vue'
import { getInferenceStats } from '@/api/admin/ml'

const stats = ref<Record<string, any>>({})
const models = ref([
  { businessName: 'CT金属伪影识别', modelKey: 'ct-artifact-model', version: 'Python服务配置', status: 'ACTIVE', trafficPct: 100 },
  { businessName: 'CT病灶识别分割', modelKey: 'ct-lesion-model', version: 'Python服务配置', status: 'ACTIVE', trafficPct: 100 },
])

function statusTag(status: string) {
  const map: Record<string, string> = { ACTIVE: 'success', REGISTERED: 'info', INACTIVE: 'info', FAILED: 'danger' }
  return map[status] || 'info'
}
function statusText(status: string) { return status || '未知' }
function progressColor(pct: number) { if (pct >= 80) return '#315fbb'; if (pct >= 40) return '#0f766e'; return '#94a3b8' }
function modelName(row: Record<string, any>) {
  if (row.businessName) return row.businessName
  if (row.modelKey === 'ct-lesion-model') return 'CT病灶识别分割'
  if (row.modelKey === 'ct-artifact-model') return 'CT金属伪影识别'
  return row.modelKey || '未知模型'
}

onMounted(async () => {
  try { const res = await getInferenceStats(); stats.value = res.data || {} } catch (e: any) { ElMessage.error(e?.message || '推理统计加载失败') }
})
</script>

<style scoped>
.page { padding: 28px 36px; }
.hero-panel, .stat-card, .panel { border-radius: 28px; border: 1px solid rgba(214, 226, 240, 0.9); box-shadow: 0 20px 45px rgba(31, 41, 55, 0.08); }
.hero-panel { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; padding: 28px 30px; margin-bottom: 24px; background: linear-gradient(135deg, #ffffff 0%, #f6f2ff 100%); }
.hero-eyebrow { margin: 0 0 10px; color: #7c3aed; font-size: 12px; font-weight: 800; letter-spacing: 0.1em; }
.hero-panel h2 { margin: 0; color: #16304d; font-size: 28px; font-weight: 800; }
.hero-copy { margin: 12px 0 0; max-width: 720px; color: #627790; font-size: 14px; line-height: 1.75; }
.hero-pill { display: inline-flex; align-items: center; gap: 8px; padding: 10px 14px; border-radius: 999px; background: rgba(124, 58, 237, 0.08); color: #6d28d9; font-size: 12px; font-weight: 700; white-space: nowrap; }
.pill-dot { width: 8px; height: 8px; border-radius: 50%; background: #8b5cf6; }
.stats-row { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 16px; margin-bottom: 24px; }
.stat-card { display: flex; align-items: center; gap: 16px; padding: 22px 24px; background: #fff; }
.stat-icon { width: 46px; height: 46px; border-radius: 14px; display: flex; align-items: center; justify-content: center; color: #fff; flex-shrink: 0; }
.is-blue { background: #315fbb; } .is-green { background: #16a34a; } .is-amber { background: #f59e0b; }
.stat-value { color: #16304d; font-size: 24px; font-weight: 800; } .unit { color: #8ba0bb; font-size: 14px; font-weight: 600; }
.stat-label { margin-top: 4px; color: #5e748f; font-size: 13px; font-weight: 700; } .stat-note { margin-top: 4px; color: #90a1b8; font-size: 12px; }
.panel { padding: 24px; background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%); }
.panel-head { margin-bottom: 18px; } .panel-head h3 { margin: 0; color: #16304d; font-size: 18px; font-weight: 800; } .panel-head p { margin: 6px 0 0; color: #72859d; font-size: 13px; }
.model-table :deep(th) { background: #f7faff; color: #64748b; font-weight: 700; font-size: 13px; border-bottom: none; } .model-table :deep(td) { font-size: 14px; }
.status-tag { display: inline-flex; align-items: center; justify-content: center; min-width: 58px; padding: 4px 10px; border-radius: 999px; font-size: 12px; font-weight: 700; }
.st-success { background: #d1fae5; color: #065f46; } .st-info { background: #dbeafe; color: #1d4ed8; } .st-danger { background: #fee2e2; color: #991b1b; }
@media (max-width: 1100px) { .stats-row { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
@media (max-width: 768px) { .page { padding: 18px 16px; } .hero-panel, .panel { padding: 20px; } .hero-panel { flex-direction: column; } .hero-pill { white-space: normal; } .stats-row { grid-template-columns: 1fr; } }
</style>

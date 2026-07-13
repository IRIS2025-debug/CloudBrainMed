<template>
  <div class="overview-board">
    <header class="ob-header">
      <div>
        <h1>{{ title }}</h1>
        <p class="ob-sub">{{ subtitle }}</p>
      </div>
      <div class="ob-refresh">
        <span class="ob-refresh-time" v-if="updatedAt">更新于 {{ updatedAt }}</span>
        <el-button size="small" :loading="loading" @click="$emit('refresh')">
          <el-icon style="margin-right:4px"><Refresh /></el-icon>刷新
        </el-button>
      </div>
    </header>

    <!-- 指标卡：真实零值显示 0，加载中/失败显示 -- -->
    <div class="ob-stats">
      <div class="ob-stat-card" v-for="stat in stats" :key="stat.label">
        <div class="ob-stat-icon" :style="{ background: stat.color }">
          <el-icon :size="20"><component :is="stat.icon" /></el-icon>
        </div>
        <div class="ob-stat-info">
          <div class="ob-stat-value">{{ formatMetric(stat.value) }}<span v-if="stat.unit" class="ob-unit"> {{ stat.unit }}</span></div>
          <div class="ob-stat-label">{{ stat.label }}</div>
        </div>
      </div>
    </div>

    <h2 class="ob-section-title">功能模块</h2>
    <div class="ob-module-grid">
      <div class="ob-module-card" v-for="m in modules" :key="m.path" @click="$emit('navigate', m.path)">
        <div class="ob-mc-icon" :style="{ background: m.color }">
          <el-icon :size="22" color="#fff"><component :is="m.icon" /></el-icon>
        </div>
        <div class="ob-mc-info">
          <div class="ob-mc-title">{{ m.title }}</div>
          <div class="ob-mc-desc">{{ m.desc }}</div>
        </div>
        <el-icon class="ob-mc-arrow" color="#94a3b8"><ArrowRight /></el-icon>
      </div>
    </div>

    <h2 class="ob-section-title">{{ recentTitle }}</h2>
    <div class="ob-feed">
      <!-- 加载中 -->
      <div class="ob-feed-state" v-if="loading">
        <el-icon class="is-loading"><Loading /></el-icon>
        <span>加载中…</span>
      </div>
      <!-- 请求失败：显示重试入口，绝不把失败伪装成零 -->
      <div class="ob-feed-state ob-feed-error" v-else-if="error">
        <span>数据加载失败</span>
        <el-button size="small" text type="primary" @click="$emit('refresh')">重试</el-button>
      </div>
      <!-- 空数据 -->
      <div class="ob-feed-state" v-else-if="!activities || activities.length === 0">
        暂无记录
      </div>
      <!-- 真实动态 -->
      <template v-else>
        <div class="ob-feed-item" v-for="(item, i) in activities" :key="item.id ?? i">
          <div class="ob-feed-dot" :class="item.tone ? 'dot-' + item.tone : ''"></div>
          <div class="ob-feed-body">
            <span class="ob-feed-text">{{ item.text }}</span>
            <span class="ob-feed-meta" v-if="item.meta">{{ item.meta }}</span>
          </div>
          <span class="ob-feed-time" v-if="item.time">{{ item.time }}</span>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ArrowRight, Loading, Refresh } from '@element-plus/icons-vue'

// 纯展示组件：不读取身份、不调接口、不判断权限，全部数据由各角色页面准备后传入。
export interface OverviewStat {
  label: string
  value: number | string | null | undefined
  icon: unknown
  color: string
  unit?: string
}
export interface OverviewModule {
  path: string
  title: string
  desc: string
  icon: unknown
  color: string
}
export interface OverviewActivity {
  id?: string | number
  text: string
  meta?: string
  time?: string
  tone?: 'success' | 'danger' | 'warning' | 'info'
}

withDefaults(defineProps<{
  title: string
  subtitle?: string
  updatedAt?: string
  loading?: boolean
  error?: boolean
  stats: OverviewStat[]
  modules: OverviewModule[]
  activities: OverviewActivity[]
  recentTitle?: string
}>(), {
  subtitle: '',
  updatedAt: '',
  loading: false,
  error: false,
  recentTitle: '最近动态',
})

defineEmits<{
  (e: 'refresh'): void
  (e: 'navigate', path: string): void
}>()

// null/undefined（加载中或失败）显示 --，其余按原值展示；真实的数字 0 会照常显示 0。
function formatMetric(value: number | string | null | undefined): string {
  if (value === null || value === undefined || value === '') return '--'
  return String(value)
}
</script>

<style scoped>
.overview-board { padding: 32px 36px; max-width: 1280px; }

.ob-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 32px; gap: 16px; }
.ob-header h1 { font-size: 26px; font-weight: 800; color: #0f172a; letter-spacing: -.5px; }
.ob-sub { font-size: 14px; color: #64748b; margin-top: 4px; }
.ob-refresh { display: flex; align-items: center; gap: 12px; flex-shrink: 0; }
.ob-refresh-time { font-size: 12px; color: #94a3b8; white-space: nowrap; }

.ob-stats { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 36px; }
.ob-stat-card { background: #fff; border-radius: var(--radius); padding: 20px 24px; display: flex; align-items: center; gap: 16px; box-shadow: var(--shadow); }
.ob-stat-icon { width: 44px; height: 44px; border-radius: 10px; display: flex; align-items: center; justify-content: center; color: #fff; flex-shrink: 0; }
.ob-stat-value { font-size: 24px; font-weight: 700; color: #0f172a; }
.ob-stat-value .ob-unit { font-size: 14px; font-weight: 500; color: #94a3b8; }
.ob-stat-label { font-size: 13px; color: #94a3b8; margin-top: 2px; }

.ob-section-title { font-size: 15px; font-weight: 700; color: #334155; margin-bottom: 16px; letter-spacing: -.2px; }

.ob-module-grid { display: flex; flex-direction: column; gap: 6px; margin-bottom: 36px; }
.ob-module-card {
  background: #fff; border-radius: 10px; padding: 16px 20px;
  display: flex; align-items: center; gap: 16px;
  cursor: pointer; transition: all .15s ease;
  box-shadow: var(--shadow);
}
.ob-module-card:hover { transform: translateX(4px); box-shadow: var(--shadow-md); }
.ob-mc-icon { width: 40px; height: 40px; border-radius: 10px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.ob-mc-title { font-size: 15px; font-weight: 600; color: #1e293b; }
.ob-mc-desc { font-size: 13px; color: #94a3b8; margin-top: 2px; }
.ob-mc-arrow { margin-left: auto; }

.ob-feed { background: #fff; border-radius: var(--radius); padding: 8px 20px; box-shadow: var(--shadow); }
.ob-feed-item { display: flex; align-items: center; gap: 12px; padding: 12px 0; border-bottom: 1px solid #f1f5f9; font-size: 14px; }
.ob-feed-item:last-child { border-bottom: none; }
.ob-feed-dot { width: 8px; height: 8px; border-radius: 50%; background: #cbd5e1; flex-shrink: 0; }
.ob-feed-dot.dot-success { background: #22c55e; }
.ob-feed-dot.dot-danger { background: #ef4444; }
.ob-feed-dot.dot-warning { background: #f59e0b; }
.ob-feed-dot.dot-info { background: #3b82f6; }
.ob-feed-body { flex: 1; display: flex; flex-direction: column; gap: 2px; min-width: 0; }
.ob-feed-text { color: #475569; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.ob-feed-meta { color: #94a3b8; font-size: 12px; }
.ob-feed-time { color: #94a3b8; font-size: 12px; flex-shrink: 0; }
.ob-feed-state { display: flex; align-items: center; justify-content: center; gap: 8px; color: #94a3b8; font-size: 13px; padding: 28px 0; }
.ob-feed-error { color: #ef4444; }

@media (max-width: 900px) {
  .overview-board { padding: 20px 16px; }
  .ob-stats { grid-template-columns: repeat(2, 1fr); }
  .ob-header { flex-direction: column; }
}
</style>

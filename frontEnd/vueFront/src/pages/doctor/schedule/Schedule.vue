<!-- src/pages/doctor/schedule/Schedule.vue -->
<template>
  <div class="schedule-page">
    <!-- ===== 页面头部 ===== -->
    <header class="page-header">
      <div class="header-left">
        <h2 class="page-title">我的值班表</h2>
        <p class="page-subtitle">查看本周排班安排</p>
      </div>
      <div class="header-right">
        <el-button-group>
          <el-button size="default" @click="handlePrevWeek">
            <el-icon><ArrowLeft /></el-icon>
            上一周
          </el-button>
          <el-button size="default" @click="handleCurrentWeek" type="primary">本周</el-button>
          <el-button size="default" @click="handleNextWeek">
            下一周
            <el-icon><ArrowRight /></el-icon>
          </el-button>
        </el-button-group>
        <el-date-picker
          v-model="selectedDate"
          type="date"
          placeholder="选择日期"
          format="YYYY-MM-DD"
          value-format="YYYY-MM-DD"
          @change="handleDateChange"
          style="width: 160px; margin-left: 12px"
        />
      </div>
    </header>

    <!-- ===== 统计信息 ===== -->
    <div v-if="!loading && weekData.length > 0" class="stats-bar">
      <span class="stats-range">{{ weekStartDisplay }} ~ {{ weekEndDisplay }}</span>
      <span class="stats-divider">|</span>
      <span class="stats-item">
        <span class="stats-dot" style="background: #4f46e5" />
        排班时段：{{ totalSchedules }}
      </span>
      <span class="stats-item">
        <span class="stats-dot" style="background: #f59e0b" />
        剩余号源：{{ totalRemain }}
      </span>
      <span class="stats-item">
        <span class="stats-dot" style="background: #10b981" />
        已预约：{{ totalBooked }}
      </span>
    </div>

    <!-- ===== 周视图表格 ===== -->
    <div class="schedule-card" v-loading="loading">
      <!-- 表格主体 -->
      <div v-if="weekData.length > 0" class="schedule-grid">
        <!-- 时间轴列 -->
        <div class="grid-time-axis">
          <div class="time-header">时间</div>
          <div v-for="hour in timeSlots" :key="hour" class="time-cell">
            {{ hour }}
          </div>
        </div>

        <!-- 每天列 -->
        <div
          v-for="day in weekData"
          :key="day.date"
          class="grid-day-column"
        >
          <div class="day-header" :class="{ 'is-today': isToday(day.date) }">
            <div class="day-name">{{ day.dayName }}</div>
            <div class="day-date">{{ formatDateShort(day.date) }}</div>
          </div>
          <div class="day-body">
            <!-- 排班块 -->
            <div
              v-for="schedule in day.schedules"
              :key="schedule.scheduleId"
              class="schedule-block"
              :style="getScheduleStyle(schedule)"
              @click="handleScheduleClick(schedule)"
            >
              <div class="schedule-time">
                {{ formatTime(schedule.startTime) }} - {{ formatTime(schedule.endTime) }}
              </div>
              <div class="schedule-room">
                <el-icon><Location /></el-icon>
                {{ schedule.room || '未指定' }}
              </div>
              <div class="schedule-remain">
                <el-tag :type="getRemainTagType(schedule.remainNum, schedule.maxNum)" size="small">
                  {{ schedule.remainNum }}/{{ schedule.maxNum }}
                </el-tag>
              </div>
            </div>

            <!-- 空状态 -->
            <div v-if="day.schedules.length === 0" class="day-empty">
              <span>暂无排班</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 全局空状态 -->
      <el-empty v-else description="本周暂无排班" />
    </div>

    <!-- ===== 排班详情弹窗 ===== -->
    <el-dialog
      v-model="detailVisible"
      title="排班详情"
      width="480px"
      destroy-on-close
    >
      <div v-if="selectedSchedule" class="detail-content">
        <div class="detail-row">
          <span class="detail-label">日期</span>
          <span class="detail-value">{{ formatDateShort(selectedSchedule.workDate) }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">时段</span>
          <span class="detail-value">
            {{ formatTime(selectedSchedule.startTime) }} - {{ formatTime(selectedSchedule.endTime) }}
          </span>
        </div>
        <div class="detail-row">
          <span class="detail-label">诊室</span>
          <span class="detail-value">{{ selectedSchedule.room || '未指定' }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">号源</span>
          <span class="detail-value">
            总号 <strong>{{ selectedSchedule.maxNum }}</strong>，
            剩余 <strong :class="getRemainClass(selectedSchedule.remainNum)">
              {{ selectedSchedule.remainNum }}
            </strong>
          </span>
        </div>
        <div class="detail-row">
          <span class="detail-label">挂号费</span>
          <span class="detail-value">¥{{ selectedSchedule.price.toFixed(2) }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">来源</span>
          <span class="detail-value">
            <el-tag :type="selectedSchedule.sourceType === 'AI_GENERATED' ? 'warning' : 'info'" size="small">
              {{ selectedSchedule.sourceType === 'AI_GENERATED' ? '🤖 AI 排班' : '✏️ 人工创建' }}
            </el-tag>
          </span>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { ArrowLeft, ArrowRight, Location } from '@element-plus/icons-vue'
import { getWeeklySchedule } from '@/api/doctor/schedule'
import type { DoctorSchedule, WeekDayVo } from '@/types/doctor/schedule'
import {
  formatDateShort,
  formatTime,
  isToday,
  getThisWeekStart,
  formatDate
} from '@/utils/date'

// ============================================================
// 状态
// ============================================================
const loading = ref<boolean>(false)
const selectedDate = ref<string>(getThisWeekStart())
const weekData = ref<WeekDayVo[]>([])
const detailVisible = ref<boolean>(false)
const selectedSchedule = ref<DoctorSchedule | null>(null)

// ============================================================
// 计算属性
// ============================================================

/** 时间轴 (08:00 - 20:00) */
const timeSlots = computed<string[]>(() => {
  const slots: string[] = []
  for (let h = 8; h <= 20; h++) {
    slots.push(`${String(h).padStart(2, '0')}:00`)
  }
  return slots
})

/** 周范围显示 - 使用非空断言确保数组有值 */
const weekStartDisplay = computed<string>(() => {
  if (weekData.value.length === 0) return ''
  // 使用 ! 非空断言，因为已经检查了 length > 0
  return formatDateShort(weekData.value[0]!.date)
})

const weekEndDisplay = computed<string>(() => {
  if (weekData.value.length === 0) return ''
  // 使用 ! 非空断言，因为已经检查了 length > 0
  return formatDateShort(weekData.value[weekData.value.length - 1]!.date)
})

/** 排班时段总数 */
const totalSchedules = computed<number>(() => {
  return weekData.value.reduce((sum, day) => sum + day.schedules.length, 0)
})

/** 剩余号源总数 */
const totalRemain = computed<number>(() => {
  return weekData.value.reduce((sum, day) => {
    return sum + day.schedules.reduce((s, item) => s + item.remainNum, 0)
  }, 0)
})

/** 已预约总数 */
const totalBooked = computed<number>(() => {
  return weekData.value.reduce((sum, day) => {
    return sum + day.schedules.reduce((s, item) => s + (item.maxNum - item.remainNum), 0)
  }, 0)
})

// ============================================================
// 方法
// ============================================================

/**
 * 获取排班块样式
 * 使用非空断言 ! 确保时间字段存在
 */
function getScheduleStyle(schedule: DoctorSchedule): Record<string, string> {
  // 使用非空断言确保时间字符串存在
  const startTime = schedule.startTime!
  const endTime = schedule.endTime!
  
  const startHour = parseInt(startTime.split(':')[0] || '0')
  const startMin = parseInt(startTime.split(':')[1] || '0')
  const endHour = parseInt(endTime.split(':')[0] || '0')
  const endMin = parseInt(endTime.split(':')[1] || '0')

  // 从 08:00 开始计算偏移 (单位: px)
  const topOffset = ((startHour - 8) * 60 + startMin) / 60 * 60
  const height = ((endHour - startHour) * 60 + (endMin - startMin)) / 60 * 60

  // 根据 scheduleId 生成不同颜色
  const colors = ['#4f46e5', '#7c3aed', '#0891b2', '#059669', '#d97706', '#dc2626']
  const lastChar = schedule.scheduleId.slice(-1)
  const colorIndex = parseInt(lastChar, 16) % colors.length
  const color = colors[colorIndex] || '#4f46e5' // 默认颜色

  return {
    top: topOffset + 'px',
    height: height + 'px',
    backgroundColor: color + '15',
    borderLeft: `3px solid ${color}`,
    borderColor: color
  }
}

/**
 * 获取剩余号源的 Tag 类型
 */
function getRemainTagType(remain: number, max: number): 'danger' | 'warning' | 'success' | 'info' {
  if (remain === 0) return 'danger'
  const ratio = remain / max
  if (ratio < 0.2) return 'danger'
  if (ratio < 0.4) return 'warning'
  if (ratio < 0.7) return 'info'
  return 'success'
}

/**
 * 获取剩余号源的样式类
 */
function getRemainClass(remain: number): string {
  if (remain === 0) return 'remain-full'
  if (remain < 3) return 'remain-low'
  return 'remain-normal'
}

/**
 * 获取排班数据
 */
async function fetchSchedule(): Promise<void> {
  loading.value = true
  try {
    const response = await getWeeklySchedule(selectedDate.value)
    if (response.code === 0 || response.code === 200) {
      weekData.value = response.data?.weekData || []
    } else {
      ElMessage.error(response.message || response.msg || '获取排班数据失败')
      weekData.value = []
    }
  } catch (error) {
    console.error('获取排班数据失败:', error)
    weekData.value = []
  } finally {
    loading.value = false
  }
}

/**
 * 切换到上一周
 */
function handlePrevWeek(): void {
  const date = new Date(selectedDate.value)
  date.setDate(date.getDate() - 7)
  selectedDate.value = formatDate(date)
  fetchSchedule()
}

/**
 * 切换到下一周
 */
function handleNextWeek(): void {
  const date = new Date(selectedDate.value)
  date.setDate(date.getDate() + 7)
  selectedDate.value = formatDate(date)
  fetchSchedule()
}

/**
 * 切换到本周
 */
function handleCurrentWeek(): void {
  selectedDate.value = getThisWeekStart()
  fetchSchedule()
}

/**
 * 日期选择变化
 */
function handleDateChange(val: string): void {
  if (val) {
    // 将选择的日期转换为所在周的周一
    const date = new Date(val)
    const day = date.getDay()
    const diff = date.getDate() - day + (day === 0 ? -6 : 1)
    const monday = new Date(date)
    monday.setDate(diff)
    selectedDate.value = formatDate(monday)
    fetchSchedule()
  }
}

/**
 * 点击排班块查看详情
 */
function handleScheduleClick(schedule: DoctorSchedule): void {
  selectedSchedule.value = schedule
  detailVisible.value = true
}

// ============================================================
// 生命周期
// ============================================================
onMounted(() => {
  fetchSchedule()
})
</script>

<style scoped lang="scss">
.schedule-page {
  padding: 24px 32px;
  min-height: 100vh;
  background: #f1f5f9;
}

// ===== 页面头部 =====
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 12px;
}

.header-left {
  .page-title {
    font-size: 22px;
    font-weight: 700;
    color: #0f172a;
    margin: 0 0 2px 0;
  }
  .page-subtitle {
    font-size: 13px;
    color: #94a3b8;
    margin: 0;
  }
}

.header-right {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

// ===== 统计栏 =====
.stats-bar {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 20px;
  background: #ffffff;
  border-radius: 8px;
  margin-bottom: 16px;
  font-size: 14px;
  color: #475569;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);

  .stats-range {
    font-weight: 600;
    color: #0f172a;
  }

  .stats-divider {
    color: #e2e8f0;
  }

  .stats-item {
    display: flex;
    align-items: center;
    gap: 6px;
  }

  .stats-dot {
    display: inline-block;
    width: 8px;
    height: 8px;
    border-radius: 50%;
  }
}

// ===== 排班卡片 =====
.schedule-card {
  background: #ffffff;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  padding: 16px 20px 20px;
  overflow: hidden;
}

// ===== 周视图网格 =====
.schedule-grid {
  display: grid;
  grid-template-columns: 72px repeat(7, 1fr);
  gap: 2px;
  min-height: 460px;
  overflow-x: auto;
}

// ===== 时间轴列 =====
.grid-time-axis {
  display: flex;
  flex-direction: column;
  background: #f8fafc;
  border-radius: 8px 0 0 8px;
  overflow: hidden;
  flex-shrink: 0;

  .time-header {
    height: 44px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 12px;
    font-weight: 600;
    color: #94a3b8;
    background: #f1f5f9;
    border-bottom: 1px solid #e2e8f0;
    flex-shrink: 0;
  }

  .time-cell {
    height: 60px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 11px;
    color: #94a3b8;
    border-bottom: 1px solid #f1f5f9;
    flex-shrink: 0;
  }
}

// ===== 每天列 =====
.grid-day-column {
  display: flex;
  flex-direction: column;
  background: #fafbfc;
  border-radius: 0 0 8px 8px;
  overflow: hidden;
  min-width: 100px;

  .day-header {
    height: 44px;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    background: #f1f5f9;
    border-bottom: 2px solid #e2e8f0;
    padding: 4px 0;
    flex-shrink: 0;

    &.is-today {
      background: #eef2ff;
      border-bottom-color: #4f46e5;

      .day-name {
        color: #4f46e5;
      }
    }

    .day-name {
      font-size: 13px;
      font-weight: 600;
      color: #334155;
      line-height: 1.2;
    }

    .day-date {
      font-size: 11px;
      color: #94a3b8;
      line-height: 1.2;
    }
  }

  .day-body {
    flex: 1;
    position: relative;
    min-height: 420px;
    background: #fafbfc;
  }
}

// ===== 排班块 =====
.schedule-block {
  position: absolute;
  left: 4px;
  right: 4px;
  border-radius: 6px;
  padding: 4px 8px;
  cursor: pointer;
  transition: all 0.2s;
  min-height: 28px;
  overflow: hidden;
  border-left: 3px solid;

  &:hover {
    transform: scale(1.03);
    z-index: 10;
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
  }

  .schedule-time {
    font-size: 11px;
    font-weight: 600;
    color: #1e293b;
    white-space: nowrap;
  }

  .schedule-room {
    font-size: 10px;
    color: #64748b;
    display: flex;
    align-items: center;
    gap: 2px;
    margin-top: 1px;
    white-space: nowrap;

    .el-icon {
      font-size: 12px;
    }
  }

  .schedule-remain {
    margin-top: 2px;
  }
}

// ===== 空状态 =====
.day-empty {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: #cbd5e1;
}

// ===== 详情弹窗 =====
.detail-content {
  padding: 4px 0;
}

.detail-row {
  display: flex;
  justify-content: space-between;
  padding: 12px 0;
  border-bottom: 1px solid #f1f5f9;

  &:last-child {
    border-bottom: none;
  }

  .detail-label {
    color: #94a3b8;
    font-size: 14px;
  }

  .detail-value {
    color: #0f172a;
    font-size: 14px;
    text-align: right;
  }
}

.remain-full {
  color: #dc2626;
  font-weight: 600;
}

.remain-low {
  color: #d97706;
  font-weight: 600;
}

.remain-normal {
  color: #0f172a;
  font-weight: 600;
}

// ===== 响应式 =====
@media (max-width: 1200px) {
  .schedule-grid {
    grid-template-columns: 60px repeat(7, 160px);
  }
}

@media (max-width: 768px) {
  .schedule-page {
    padding: 16px;
  }

  .page-header {
    flex-direction: column;
    align-items: stretch;
  }

  .header-right {
    flex-wrap: wrap;
  }

  .stats-bar {
    flex-wrap: wrap;
    gap: 8px 16px;
    font-size: 13px;
    padding: 10px 16px;
  }

  .schedule-grid {
    grid-template-columns: 50px repeat(7, 120px);
  }

  .grid-day-column {
    min-width: 80px;
  }

  .schedule-block {
    padding: 2px 4px;
    left: 2px;
    right: 2px;

    .schedule-time {
      font-size: 10px;
    }
    .schedule-room {
      font-size: 9px;
    }
  }
}
</style>
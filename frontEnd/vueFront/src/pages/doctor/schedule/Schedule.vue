<template>
  <div class="page">
    <header class="page-top">
      <div>
        <h2>值班查询</h2>
        <p class="top-sub">查看医生排班安排</p>
      </div>
      <div class="filter-bar">
        <el-date-picker v-model="filters.weekStart" type="week" placeholder="选择周" @change="fetchSchedule" style="width:180px" />
        <el-select v-model="filters.department" placeholder="科室筛选" clearable @change="fetchSchedule" style="width:150px">
          <el-option label="内科" value="内科" />
          <el-option label="外科" value="外科" />
          <el-option label="儿科" value="儿科" />
          <el-option label="急诊科" value="急诊科" />
        </el-select>
      </div>
    </header>

    <div class="card">
      <el-table :data="scheduleList" stripe v-loading="loading" class="schedule-table">
        <el-table-column prop="doctorName" label="医生" width="120" />
        <el-table-column prop="department" label="科室" width="100" />
        <el-table-column prop="shiftDate" label="值班日期" width="130" />
        <el-table-column label="班次" width="100">
          <template #default="{ row }">
            <span class="shift-tag" :class="row.shiftType === 'DAY' ? 'shift-day' : 'shift-night'">
              {{ row.shiftType === 'DAY' ? '白班' : '夜班' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="startTime" label="开始时间" width="100" />
        <el-table-column prop="endTime" label="结束时间" width="100" />
        <el-table-column prop="remark" label="备注" show-overflow-tooltip min-width="180" />
      </el-table>
      <div class="table-footer">
        <span class="tf-total">共 {{ scheduleList.length }} 条</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'

interface ScheduleItem {
  doctorName: string
  department: string
  shiftDate: string
  shiftType: 'DAY' | 'NIGHT'
  startTime: string
  endTime: string
  remark: string
}

const scheduleList = ref<ScheduleItem[]>([])
const loading = ref(false)
const filters = reactive({ weekStart: '', department: '' })

// Mock 数据 —— API 就绪后替换为真实接口调用
const mockData: ScheduleItem[] = [
  { doctorName: '张明远', department: '内科', shiftDate: '2026-06-15', shiftType: 'DAY', startTime: '08:00', endTime: '16:00', remark: '普通门诊' },
  { doctorName: '李思雨', department: '内科', shiftDate: '2026-06-15', shiftType: 'NIGHT', startTime: '16:00', endTime: '00:00', remark: '含急诊备班' },
  { doctorName: '王建华', department: '外科', shiftDate: '2026-06-15', shiftType: 'DAY', startTime: '08:00', endTime: '16:00', remark: '' },
  { doctorName: '赵晓丽', department: '儿科', shiftDate: '2026-06-16', shiftType: 'DAY', startTime: '08:00', endTime: '16:00', remark: '专家门诊' },
  { doctorName: '张明远', department: '内科', shiftDate: '2026-06-16', shiftType: 'NIGHT', startTime: '16:00', endTime: '00:00', remark: '' },
  { doctorName: '陈志强', department: '急诊科', shiftDate: '2026-06-16', shiftType: 'DAY', startTime: '08:00', endTime: '20:00', remark: '12小时长班' },
  { doctorName: '李思雨', department: '内科', shiftDate: '2026-06-17', shiftType: 'DAY', startTime: '08:00', endTime: '16:00', remark: '' },
  { doctorName: '王建华', department: '外科', shiftDate: '2026-06-17', shiftType: 'NIGHT', startTime: '16:00', endTime: '00:00', remark: '含急诊备班' },
]

async function fetchSchedule() {
  loading.value = true
  try {
    // TODO: 替换为真实 API 调用
    // const res = await getSchedule({ ...filters })
    // scheduleList.value = res.data || []
    await new Promise(r => setTimeout(r, 300)) // 模拟网络延迟
    scheduleList.value = mockData
  } catch {
    scheduleList.value = mockData
  } finally {
    loading.value = false
  }
}

fetchSchedule()
</script>

<style scoped>
.page { padding: 28px 36px; }
.page-top { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 24px; }
.page-top h2 { font-size: 22px; font-weight: 700; color: #0f172a; }
.top-sub { font-size: 13px; color: #94a3b8; margin-top: 4px; }
.filter-bar { display: flex; gap: 10px; }
.card { background: #fff; border-radius: var(--radius); box-shadow: var(--shadow); overflow: hidden; }

.schedule-table :deep(th) { background: #f8fafc; color: #64748b; font-weight: 600; font-size: 13px; border-bottom: none; }
.schedule-table :deep(td) { font-size: 14px; }

.shift-tag { font-size: 12px; font-weight: 600; padding: 3px 10px; border-radius: 12px; }
.shift-day { background: #dbeafe; color: #1d4ed8; }
.shift-night { background: #1e293b; color: #e2e8f0; }

.table-footer { display: flex; justify-content: space-between; align-items: center; padding: 12px 16px; border-top: 1px solid #f1f5f9; }
.tf-total { font-size: 13px; color: #94a3b8; }
</style>

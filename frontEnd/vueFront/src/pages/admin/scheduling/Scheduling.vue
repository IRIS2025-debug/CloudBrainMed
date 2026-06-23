<!-- src/pages/admin/scheduling/ScheduleManage.vue -->
<template>
  <div class="schedule-manage-page">
    <!-- ===== 页面头部 ===== -->
    <header class="page-header">
      <div class="header-left">
        <h2 class="page-title">排班管理</h2>
        <p class="page-subtitle">管理所有医生排班，支持新增、编辑、删除</p>
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

    <!-- ===== 筛选与操作栏 ===== -->
    <div class="filter-bar">
      <div class="filter-left">
        <el-select
          v-model="filterDoctorId"
          placeholder="选择医生筛选"
          clearable
          filterable
          @change="fetchSchedule"
          style="width: 200px"
        >
          <el-option
            v-for="doctor in doctorList"
            :key="doctor.doctorId"
            :label="doctor.name"
            :value="doctor.doctorId"
          />
        </el-select>
        <el-select
          v-model="filterDeptId"
          placeholder="选择科室筛选"
          clearable
          @change="fetchSchedule"
          style="width: 160px; margin-left: 8px"
        >
          <el-option
            v-for="dept in deptList"
            :key="dept.deptId"
            :label="dept.deptName"
            :value="dept.deptId"
          />
        </el-select>
        <span class="filter-result">共 {{ totalDoctors }} 位医生</span>
      </div>
      <div class="filter-right">
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon> 新增排班
        </el-button>
        <el-button @click="handleRefresh">
          <el-icon><Refresh /></el-icon> 刷新
        </el-button>
      </div>
    </div>

    <!-- ===== 统计信息 ===== -->
    <div v-if="!loading && doctorScheduleData.length > 0" class="stats-bar">
      <span class="stats-range">{{ weekStartDisplay || '--' }} ~ {{ weekEndDisplay || '--' }}</span>
      <span class="stats-divider">|</span>
      <span class="stats-item">
        <span class="stats-dot" style="background: #4f46e5" />
        总排班：{{ totalSchedules }}
      </span>
      <span class="stats-item">
        <span class="stats-dot" style="background: #f59e0b" />
        剩余号源：{{ totalRemain }}
      </span>
      <span class="stats-item">
        <span class="stats-dot" style="background: #10b981" />
        已预约：{{ totalBooked }}
      </span>
      <span class="stats-item">
        <span class="stats-dot" style="background: #ef4444" />
        号源已满：{{ fullSchedules }}
      </span>
    </div>

    <!-- ===== 周视图表格 ===== -->
    <div class="schedule-card" v-loading="loading">
      <!-- 表格主体 -->
      <div v-if="doctorScheduleData.length > 0" class="schedule-grid">
        <!-- 医生名列 -->
        <div class="grid-doctor-column">
          <div class="doctor-header">医生</div>
          <div
            v-for="doctor in doctorScheduleData"
            :key="doctor.doctorId"
            class="doctor-cell"
          >
            <div class="doctor-avatar">{{ getDoctorName(doctor.doctorId)?.charAt(0) || '?' }}</div>
            <div class="doctor-name">{{ getDoctorName(doctor.doctorId) || '未知医生' }}</div>
            <div class="doctor-dept">{{ getDeptName(doctor.deptId) || '' }}</div>
            <el-button
              size="small"
              type="primary"
              link
              @click="handleEditDoctor(doctor)"
              class="edit-btn"
            >
              编辑
            </el-button>
          </div>
        </div>

        <!-- 每天列 -->
        <div
          v-for="day in weekDays"
          :key="day.date"
          class="grid-day-column"
        >
          <div class="day-header" :class="{ 'is-today': isToday(day.date) }">
            <div class="day-name">{{ day.dayName || '--' }}</div>
            <div class="day-date">{{ formatDateShort(day.date) }}</div>
          </div>
          <div class="day-body">
            <!-- 每个医生的排班 -->
            <div
              v-for="doctor in doctorScheduleData"
              :key="doctor.doctorId"
              class="doctor-schedule-cell"
            >
              <!-- 排班块 -->
              <template v-if="getSchedulesForDoctorAndDay(doctor.doctorId, day.date).length > 0">
                <!-- 排班块 -->
                <div
                  v-for="schedule in getSchedulesForDoctorAndDay(doctor.doctorId, day.date)"
                  :key="schedule.scheduleId"
                  class="schedule-block"
                  :class="{
                    'is-full': schedule.remainNum === 0,
                    'is-low': schedule.remainNum > 0 && schedule.remainNum <= 2,
                    'is-past': !canModifySchedule(schedule.workDate),
                    'is-disabled': schedule.status === 0
                  }"
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
                  <!-- status=1 显示编辑/删除 -->
                  <div class="schedule-actions" v-if="schedule.status === 1 && canModifySchedule(schedule.workDate)">
                    <el-button size="small" type="primary" link @click.stop="handleEdit(schedule)">
                      编辑
                    </el-button>
                    <el-button size="small" type="danger" link @click.stop="handleDelete(schedule)">
                      删除
                    </el-button>
                  </div>
                  <!-- status=0 显示启用按钮 -->
                  <div class="schedule-actions" v-else-if="schedule.status === 0 && canModifySchedule(schedule.workDate)">
                    <el-button size="small" type="success" link @click.stop="handleEnable(schedule)">
                      启用
                    </el-button>
                  </div>
                  <!-- 过去的排班显示已过期标签 -->
                  <div class="schedule-past-tag" v-else-if="!canModifySchedule(schedule.workDate)">
                    <el-tag size="small" type="danger">已过期</el-tag>
                  </div>
                  <!-- 停用的排班显示已停用标签 -->
                  <div class="schedule-disabled-tag" v-else-if="schedule.status === 0">
                    <el-tag size="small" type="info">已停用</el-tag>
                  </div>
                </div>
              </template>

              <!-- 添加排班按钮 -->
              <div
                v-else
                class="add-schedule-btn"
                @click="handleAddForDoctorAndDay(doctor.doctorId, day.date)"
              >
                <el-icon><Plus /></el-icon>
                <span>添加排班</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 全局空状态 -->
      <el-empty v-else description="本周暂无排班数据" />
    </div>

    <!-- ===== 编辑排班弹窗 ===== -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="520px"
      destroy-on-close
      :append-to-body="true"
      :modal-append-to-body="true"
      :z-index="3000"
      top="8vh"
      @close="resetForm"
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="100px"
        class="schedule-form"
      >
        <el-form-item label="医生" prop="doctorId">
          <el-select
            v-model="formData.doctorId"
            placeholder="请选择医生"
            filterable
            @change="handleDoctorChange"
            style="width: 100%"
            teleported
          >
            <el-option
              v-for="doctor in doctorList"
              :key="doctor.doctorId"
              :label="doctor.name"
              :value="doctor.doctorId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="科室" prop="deptId">
          <el-select
            v-model="formData.deptId"
            placeholder="请选择科室"
            style="width: 100%"
            :disabled="!!formData.doctorId"
            teleported
          >
            <el-option
              v-for="dept in deptList"
              :key="dept.deptId"
              :label="dept.deptName"
              :value="dept.deptId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="日期" prop="workDate">
          <el-date-picker
            v-model="formData.workDate"
            type="date"
            placeholder="选择日期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
            :disabled-date="disabledPastDate"
            teleported
          />
        </el-form-item>
        <el-form-item label="开始时间" prop="startTime">
          <el-time-picker
            v-model="formData.startTime"
            placeholder="选择开始时间"
            value-format="HH:mm:ss"
            format="HH:mm"
            style="width: 100%"
            teleported
          />
        </el-form-item>
        <el-form-item label="结束时间" prop="endTime">
          <el-time-picker
            v-model="formData.endTime"
            placeholder="选择结束时间"
            value-format="HH:mm:ss"
            format="HH:mm"
            style="width: 100%"
            teleported
          />
        </el-form-item>
        <el-form-item label="诊室" prop="room">
          <el-input v-model="formData.room" placeholder="请输入诊室，如：门诊楼A101" />
        </el-form-item>
        <el-form-item label="最大号源" prop="maxNum">
          <el-input-number
            v-model="formData.maxNum"
            :min="1"
            :max="50"
            style="width: 100%"
            controls-position="right"
          />
        </el-form-item>
        <el-form-item label="剩余号源" prop="remainNum" v-if="isEdit">
          <el-input-number
            v-model="formData.remainNum"
            :min="0"
            :max="formData.maxNum || 20"
            style="width: 100%"
            controls-position="right"
          />
        </el-form-item>
        <el-form-item label="挂号费" prop="price">
          <el-input-number
            v-model="formData.price"
            :min="0"
            :precision="2"
            :step="5"
            style="width: 100%"
            controls-position="right"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">
          {{ isEdit ? '更新' : '创建' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- ===== 排班详情弹窗 ===== -->
    <el-dialog
      v-model="detailVisible"
      title="排班详情"
      width="480px"
      destroy-on-close
      :append-to-body="true"
      :modal-append-to-body="true"
      top="10vh"
    >
      <div v-if="selectedSchedule" class="detail-content">
        <div class="detail-row">
          <span class="detail-label">医生</span>
          <span class="detail-value">{{ getDoctorName(selectedSchedule.doctorId) || selectedSchedule.doctorName || selectedSchedule.doctorId }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">科室</span>
          <span class="detail-value">{{ getDeptName(selectedSchedule.deptId) || selectedSchedule.deptId }}</span>
        </div>
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
          <span class="detail-value">¥{{ (selectedSchedule.price || 0).toFixed(2) }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">状态</span>
          <span class="detail-value">
            <el-tag :type="selectedSchedule.status === 1 ? 'success' : 'danger'" size="small">
              {{ selectedSchedule.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </span>
        </div>
        <div class="detail-row">
          <span class="detail-label">来源</span>
          <span class="detail-value">
            <el-tag :type="selectedSchedule.sourceType === 'AI_GENERATED' ? 'warning' : 'info'" size="small">
              {{ selectedSchedule.sourceType === 'AI_GENERATED' ? '🤖 AI 排班' : '✏️ 人工创建' }}
            </el-tag>
          </span>
        </div>
        <div v-if="!canModifySchedule(selectedSchedule.workDate)" class="detail-row past-warning">
          <span class="detail-label">提示</span>
          <span class="detail-value">
            <el-tag type="danger" size="default">⛔ 过去的排班不可修改</el-tag>
          </span>
        </div>
        
        <div class="detail-actions" v-if="canModifySchedule(selectedSchedule.workDate)">
          <el-button type="primary" @click="handleEdit(selectedSchedule); detailVisible = false">
            编辑排班
          </el-button>
          <el-button type="danger" @click="handleDelete(selectedSchedule); detailVisible = false">
            删除排班
          </el-button>
        </div>
        <div class="detail-actions" v-else>
          <el-button @click="detailVisible = false">关闭</el-button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, ArrowRight, Location, Plus, Refresh } from '@element-plus/icons-vue'
import {
  queryScheduleList,
  getWeeklySchedule,
  getAllWeeklySchedule,
  getScheduleDetail,
  createSchedule,
  updateSchedule,
  enableSchedule,
  deleteSchedule,
  type DoctorSchedule,
  type ScheduleQueryDto,
  type ScheduleSaveDto,
  type ScheduleUpdateDto
} from '@/api/admin/schedule'
import { getDoctorList } from '@/api/admin/doctor'
import { getDeptList } from '@/api/admin/dept'
import {
  formatDateShort,
  formatTime,
  isToday,
  getThisWeekStart,
  formatDate,
  getWeekDays,
  getWeekStart
} from '@/utils/date'

// ============================================================
// 类型定义
// ============================================================
interface DoctorInfo {
  doctorId: string
  name: string
  deptId: string
}

interface DeptInfo {
  deptId: string
  deptName: string
}

interface DoctorScheduleData extends DoctorInfo {
  schedules: Record<string, DoctorSchedule[]>
}

interface WeekDay {
  date: string
  dayName: string
}

// ============================================================
// 状态
// ============================================================
const loading = ref(false)
const submitting = ref(false)
const selectedDate = ref(getThisWeekStart())

const filterDoctorId = ref('')
const filterDeptId = ref('')

const doctorList = ref<DoctorInfo[]>([])
const deptList = ref<DeptInfo[]>([])
const doctorScheduleData = ref<DoctorScheduleData[]>([])

const dialogVisible = ref(false)
const detailVisible = ref(false)
const isEdit = ref(false)
const selectedSchedule = ref<DoctorSchedule | null>(null)

const dialogTitle = computed(() => isEdit.value ? '编辑排班' : '新增排班')

const formData = reactive<ScheduleUpdateDto & ScheduleSaveDto & { remainNum?: number }>({
  scheduleId: '',
  doctorId: '',
  doctorName: '',
  deptId: '',
  workDate: '',
  startTime: '',
  endTime: '',
  maxNum: 20,
  remainNum: 20,
  price: 0,
  room: ''
})

const formRef = ref()

const formRules = {
  doctorId: [{ required: true, message: '请选择医生', trigger: 'change' }],
  deptId: [{ required: true, message: '请选择科室', trigger: 'change' }],
  workDate: [{ required: true, message: '请选择日期', trigger: 'change' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }],
  maxNum: [{ required: true, message: '请输入最大号源', trigger: 'blur' }]
}

// ============================================================
// 工具方法 - 获取名称
// ============================================================

/**
 * 根据医生ID获取医生姓名
 */
function getDoctorName(doctorId: string): string {
  if (!doctorId) return ''
  const doctor = doctorList.value.find(d => d.doctorId === doctorId)
  return doctor?.name || ''
}

/**
 * 根据科室ID获取科室名称
 */
function getDeptName(deptId: string): string {
  if (!deptId) return ''
  const dept = deptList.value.find(d => d.deptId === deptId)
  return dept?.deptName || ''
}


/**
 * 判断是否可以修改该排班（只能修改当天及之后的）
 */
function canModifySchedule(workDate: string): boolean {
  if (!workDate) return false
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const date = new Date(workDate)
  date.setHours(0, 0, 0, 0)
  return date >= today
}

// ============================================================
// 计算属性 - 使用 getWeekDays 生成周数据
// ============================================================

/**
 * 生成一周的日期和星期名称
 */
const weekDays = computed<WeekDay[]>(() => {
  const weekStart = selectedDate.value
  const dateStrings = getWeekDays(weekStart)
  
  return dateStrings.map((date, index) => {
    const d = new Date(date)
    const dayOfWeek = d.getDay() === 0 ? 7 : d.getDay()
    const dayNames: Record<number, string> = {
      1: '周一',
      2: '周二',
      3: '周三',
      4: '周四',
      5: '周五',
      6: '周六',
      7: '周日'
    }
    return {
      date,
      dayName: dayNames[dayOfWeek] || `周${['日','一','二','三','四','五','六'][d.getDay()]}`
    }
  })
})

/**
 * 禁用过去的日期（只能选择今天及之后）
 */
function disabledPastDate(time: Date): boolean {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return time < today
}

/**
 * 周范围显示
 */
const weekStartDisplay = computed<string>(() => {
  const days = weekDays.value
  if (days.length === 0) return ''
  return formatDateShort(days[0]?.date || '')
})

const weekEndDisplay = computed<string>(() => {
  const days = weekDays.value
  if (days.length === 0) return ''
  return formatDateShort(days[days.length - 1]?.date || '')
})

/**
 * 统计信息
 */
const totalSchedules = computed(() => {
  return doctorScheduleData.value.reduce((sum, doctor) => {
    const daySchedules = Object.values(doctor.schedules || {})
    return sum + daySchedules.reduce((s, list) => s + (list?.length || 0), 0)
  }, 0)
})

const totalRemain = computed(() => {
  return doctorScheduleData.value.reduce((sum, doctor) => {
    const daySchedules = Object.values(doctor.schedules || {})
    return sum + daySchedules.reduce((s, list) => {
      return s + (list?.reduce((total, item) => total + (item?.remainNum || 0), 0) || 0)
    }, 0)
  }, 0)
})

const totalBooked = computed(() => {
  return doctorScheduleData.value.reduce((sum, doctor) => {
    const daySchedules = Object.values(doctor.schedules || {})
    return sum + daySchedules.reduce((s, list) => {
      return s + (list?.reduce((total, item) => total + ((item?.maxNum || 0) - (item?.remainNum || 0)), 0) || 0)
    }, 0)
  }, 0)
})

const fullSchedules = computed(() => {
  return doctorScheduleData.value.reduce((sum, doctor) => {
    const daySchedules = Object.values(doctor.schedules || {})
    return sum + daySchedules.reduce((s, list) => {
      return s + (list?.filter(item => (item?.remainNum || 0) === 0).length || 0)
    }, 0)
  }, 0)
})

const totalDoctors = computed(() => doctorScheduleData.value.length)

// ============================================================
// 方法
// ============================================================

function getSchedulesForDoctorAndDay(doctorId: string, date: string): DoctorSchedule[] {
  const doctor = doctorScheduleData.value.find(d => d.doctorId === doctorId)
  if (!doctor) return []
  return doctor.schedules?.[date] || []
}

function getRemainTagType(remain: number | undefined, max: number | undefined): 'danger' | 'warning' | 'success' | 'info' {
  const r = remain ?? 0
  const m = max ?? 1
  if (r === 0) return 'danger'
  const ratio = r / m
  if (ratio < 0.2) return 'danger'
  if (ratio < 0.4) return 'warning'
  if (ratio < 0.7) return 'info'
  return 'success'
}

function getRemainClass(remain: number | undefined): string {
  const r = remain ?? 0
  if (r === 0) return 'remain-full'
  if (r < 3) return 'remain-low'
  return 'remain-normal'
}

// ===== 数据获取 =====

async function fetchSchedule() {
  loading.value = true
  try {
    const weekStart = selectedDate.value
    
    // 1. 先获取所有医生列表（作为基础）
    let allDoctors = [...doctorList.value]
    
    // 2. 如果有筛选条件，过滤医生列表
    if (filterDoctorId.value) {
      allDoctors = allDoctors.filter(d => d.doctorId === filterDoctorId.value)
    }
    if (filterDeptId.value) {
      allDoctors = allDoctors.filter(d => d.deptId === filterDeptId.value)
    }
    
    // 3. 获取排班数据
    let schedulesData: Record<string, Record<string, DoctorSchedule[]>> = {}
    
    if (filterDoctorId.value) {
      // 单个医生：获取该医生的排班
      const response = await getWeeklySchedule(filterDoctorId.value, weekStart)
      schedulesData = {
        [filterDoctorId.value]: response.data || {}
      }
    } else {
      // 所有医生：获取全部排班
      const response = await getAllWeeklySchedule(weekStart)
      schedulesData = response.data || {}
    }
    
    // 4. 合并：所有医生 + 他们的排班
    doctorScheduleData.value = allDoctors.map(doctor => {
      const doctorSchedules = schedulesData[doctor.doctorId] || {}
      return {
        ...doctor,
        schedules: doctorSchedules
      }
    })
    
  } catch (error) {
    console.error('获取排班数据失败:', error)
    ElMessage.error('获取排班数据失败')
    doctorScheduleData.value = []
  } finally {
    loading.value = false
  }
}

/**
 * 获取医生列表（只保留在职医生 status=1）
 */
async function fetchDoctorList() {
  try {
    const res = await getDoctorList()
    const list = res.data || []
    // 只保留在职医生（status === 1）
    doctorList.value = list
      .filter((item: any) => item.status === 1)
      .map((item: any) => ({
        doctorId: item.doctorId,
        name: item.name,
        deptId: item.deptId || item.departmentId || ''
      }))
    console.log('在职医生列表:', doctorList.value)
  } catch (error) {
    console.error('获取医生列表失败:', error)
    ElMessage.error('获取医生列表失败')
  }
}

/**
 * 获取科室列表
 */
async function fetchDeptList() {
  try {
    const res = await getDeptList()
    // 后端返回格式: { code: 0, data: [{ deptId, deptName, ... }] }
    const list = res.data || []
    deptList.value = list.map((item: any) => ({
      deptId: item.deptId,
      deptName: item.deptName
    }))
  } catch (error) {
    console.error('获取科室列表失败:', error)
    ElMessage.error('获取科室列表失败')
  }
}

async function handleEnable(schedule: DoctorSchedule) {
  try {
    await ElMessageBox.confirm(
      `确认启用排班吗？\n医生：${getDoctorName(schedule.doctorId) || schedule.doctorName}\n日期：${formatDateShort(schedule.workDate)}\n时段：${formatTime(schedule.startTime)} - ${formatTime(schedule.endTime)}`,
      '启用确认',
      { type: 'info' }
    )
    await enableSchedule(schedule.scheduleId)
    ElMessage.success('启用成功')
    fetchSchedule()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('启用失败')
    }
  }
}

// ===== 周导航 =====

function handlePrevWeek() {
  const date = new Date(selectedDate.value)
  date.setDate(date.getDate() - 7)
  selectedDate.value = formatDate(date)
  fetchSchedule()
}

function handleNextWeek() {
  const date = new Date(selectedDate.value)
  date.setDate(date.getDate() + 7)
  selectedDate.value = formatDate(date)
  fetchSchedule()
}

function handleCurrentWeek() {
  selectedDate.value = getThisWeekStart()
  fetchSchedule()
}

function handleDateChange(val: string) {
  if (val) {
    selectedDate.value = getWeekStart(val)
    fetchSchedule()
  }
}

function handleRefresh() {
  fetchSchedule()
}

// ===== 排班操作 =====

function handleAdd() {
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

function handleAddForDoctorAndDay(doctorId: string, date: string) {
  isEdit.value = false
  resetForm()
  formData.doctorId = doctorId
  formData.workDate = date
  const doctor = doctorList.value.find(d => d.doctorId === doctorId)
  if (doctor) {
    formData.doctorName = doctor.name
    formData.deptId = doctor.deptId
  }
  dialogVisible.value = true
}

async function handleEdit(schedule: DoctorSchedule) {
  isEdit.value = true
  try {
    const res = await getScheduleDetail(schedule.scheduleId)
    const data = res.data
    Object.assign(formData, {
      scheduleId: data.scheduleId,
      doctorId: data.doctorId,
      doctorName: data.doctorName || getDoctorName(data.doctorId),
      deptId: data.deptId,
      workDate: data.workDate,
      startTime: data.startTime,
      endTime: data.endTime,
      maxNum: data.maxNum,
      remainNum: data.remainNum,
      price: data.price || 0,
      room: data.room
    })
    dialogVisible.value = true
  } catch (error) {
    ElMessage.error('获取排班详情失败')
  }
}

function handleEditDoctor(doctor: DoctorScheduleData) {
  ElMessage.info(`编辑医生: ${getDoctorName(doctor.doctorId)}`)
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate?.()
  
  submitting.value = true
  try {
    if (!formData.doctorId || !formData.deptId || !formData.workDate || 
        !formData.startTime || !formData.endTime || !formData.maxNum) {
      ElMessage.error('请完整填写表单')
      return
    }
    
    // 确保医生名称正确
    const doctor = doctorList.value.find(d => d.doctorId === formData.doctorId)
    if (doctor) {
      formData.doctorName = doctor.name
    }
    
    if (isEdit.value) {
      await updateSchedule(formData as ScheduleUpdateDto)
      ElMessage.success('更新排班成功')
    } else {
      await createSchedule(formData as ScheduleSaveDto)
      ElMessage.success('创建排班成功')
    }
    dialogVisible.value = false
    fetchSchedule()
  } catch (error: any) {
    ElMessage.error(error.message || (isEdit.value ? '更新失败' : '创建失败'))
  } finally {
    submitting.value = false
  }
}

async function handleDelete(schedule: DoctorSchedule) {
  try {
    await ElMessageBox.confirm(
      `确认删除排班吗？\n医生：${getDoctorName(schedule.doctorId) || schedule.doctorName}\n日期：${formatDateShort(schedule.workDate)}\n时段：${formatTime(schedule.startTime)} - ${formatTime(schedule.endTime)}`,
      '删除确认',
      { type: 'warning' }
    )
    await deleteSchedule(schedule.scheduleId)
    ElMessage.success('删除成功')
    fetchSchedule()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

function handleScheduleClick(schedule: DoctorSchedule) {
  selectedSchedule.value = schedule
  detailVisible.value = true
}

function handleDoctorChange(doctorId: string) {
  const doctor = doctorList.value.find(d => d.doctorId === doctorId)
  if (doctor) {
    formData.doctorName = doctor.name
    formData.deptId = doctor.deptId
  }
}

function resetForm() {
  formData.scheduleId = ''
  formData.doctorId = ''
  formData.doctorName = ''
  formData.deptId = ''
  formData.workDate = ''
  formData.startTime = ''
  formData.endTime = ''
  formData.maxNum = 20
  formData.remainNum = 20
  formData.price = 0
  formData.room = ''
  formRef.value?.resetFields()
}

// ============================================================
// 生命周期
// ============================================================
onMounted(async () => {
  await Promise.all([fetchDoctorList(), fetchDeptList()])
  await fetchSchedule()
})
</script>

<style scoped lang="scss">
.schedule-manage-page {
  padding: 24px 32px;
  min-height: 100vh;
  background: #f1f5f9;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
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

.filter-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #ffffff;
  border-radius: 8px;
  margin-bottom: 12px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
  flex-wrap: wrap;
  gap: 8px;

  .filter-left {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 8px;

    .filter-result {
      font-size: 13px;
      color: #94a3b8;
      margin-left: 8px;
    }
  }

  .filter-right {
    display: flex;
    gap: 8px;
  }
}

.stats-bar {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 10px 20px;
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

.schedule-card {
  background: #ffffff;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  padding: 16px 20px 20px;
  overflow: hidden;
}

.schedule-grid {
  display: grid;
  grid-template-columns: 140px repeat(7, 1fr);
  gap: 2px;
  min-height: 460px;
  overflow-x: auto;
}

.grid-doctor-column {
  display: flex;
  flex-direction: column;
  background: #f8fafc;
  border-radius: 8px 0 0 8px;
  overflow: hidden;
  flex-shrink: 0;

  .doctor-header {
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

  .doctor-cell {
    height: 80px;
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 0 10px;
    border-bottom: 1px solid #f1f5f9;
    flex-shrink: 0;

    .doctor-avatar {
      width: 32px;
      height: 32px;
      border-radius: 50%;
      background: #eef2ff;
      color: #4f46e5;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 14px;
      font-weight: 600;
      flex-shrink: 0;
    }

    .doctor-name {
      font-size: 13px;
      font-weight: 500;
      color: #0f172a;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
      min-width: 30px;
    }

    .doctor-dept {
      font-size: 11px;
      color: #94a3b8;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
      flex: 1;
      text-align: left;
    }

    .edit-btn {
      font-size: 12px;
      opacity: 0;
      transition: opacity 0.2s;
      flex-shrink: 0;
    }

    &:hover .edit-btn {
      opacity: 1;
    }
  }
}

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
    display: flex;
    flex-direction: column;
  }
}

.doctor-schedule-cell {
  height: 80px;
  padding: 4px;
  border-bottom: 1px solid #f1f5f9;
  position: relative;
  display: flex;
  align-items: center;
  gap: 4px;
  flex-wrap: wrap;
}

.schedule-block {
  flex: 1;
  min-width: 60px;
  max-width: 100%;
  border-radius: 6px;
  padding: 4px 8px;
  cursor: pointer;
  transition: all 0.2s;
  background: #eef2ff;
  border-left: 3px solid #4f46e5;
  position: relative;

  &:hover {
    transform: scale(1.02);
    z-index: 10;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);

    .schedule-actions {
      opacity: 1;
    }
  }

  &.is-full {
    background: #fef2f2;
    border-left-color: #ef4444;
  }

  &.is-low {
    background: #fffbeb;
    border-left-color: #f59e0b;
  }

  &.is-past {
    opacity: 0.7;
    background: #f1f5f9;
    border-left-color: #94a3b8;
    cursor: default;

    &:hover {
      transform: none;
      box-shadow: none;
    }
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
      font-size: 11px;
    }
  }

  .schedule-remain {
    margin-top: 2px;
  }

  .schedule-actions {
    position: absolute;
    top: 2px;
    right: 4px;
    opacity: 0;
    transition: opacity 0.2s;
    display: flex;
    gap: 2px;
    background: rgba(255, 255, 255, 0.9);
    border-radius: 4px;
    padding: 0 4px;

    .el-button {
      font-size: 11px;
      padding: 0 4px;
    }
  }

  .schedule-past-tag {
    margin-top: 2px;
  }

  // 已停用（status=0）的排班样式
  &.is-disabled {
    opacity: 0.5;
    background: #f1f5f9;
    border-left-color: #94a3b8;
    
    &:hover {
      transform: none;
      box-shadow: none;
    }
    
    .schedule-time {
      color: #94a3b8;
    }
    
    .schedule-room {
      color: #b0b8c4;
    }
  }

  .schedule-disabled-tag {
    margin-top: 2px;
  }

}

.add-schedule-btn {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #94a3b8;
  font-size: 12px;
  cursor: pointer;
  border: 2px dashed #e2e8f0;
  border-radius: 6px;
  transition: all 0.2s;

  &:hover {
    border-color: #4f46e5;
    color: #4f46e5;
    background: #f8fafc;
  }

  .el-icon {
    font-size: 18px;
    margin-bottom: 2px;
  }
}

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

.detail-actions {
  display: flex;
  gap: 12px;
  margin-top: 16px;
  justify-content: flex-end;
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

// ============================================================
// 修复弹窗中选择框透明问题（使用 ::v-deep 代替 :global）
// ============================================================

// 修复弹窗中下拉选择框透明问题
.schedule-form {
  // 确保所有输入框背景不透明
  ::v-deep(.el-input__wrapper) {
    background-color: #ffffff !important;
    box-shadow: 0 0 0 1px #dcdfe6 inset;
  }

  ::v-deep(.el-input__wrapper:hover) {
    box-shadow: 0 0 0 1px #c0c4cc inset;
  }

  ::v-deep(.el-input__wrapper.is-focus) {
    box-shadow: 0 0 0 1px #409eff inset;
  }

  ::v-deep(.el-textarea__inner) {
    background-color: #ffffff !important;
  }

  ::v-deep(.el-input-number .el-input__wrapper) {
    background-color: #ffffff !important;
  }
}

// 修复 el-dialog 本身的样式
::v-deep(.el-dialog) {
  border-radius: 12px;

  .el-dialog__body {
    padding: 20px 24px;
  }

  .el-dialog__footer {
    padding: 10px 24px 20px;
  }
}

// ============================================================
// 响应式
// ============================================================

@media (max-width: 1200px) {
  .schedule-grid {
    grid-template-columns: 120px repeat(7, 160px);
  }
}

@media (max-width: 768px) {
  .schedule-manage-page {
    padding: 16px;
  }

  .page-header {
    flex-direction: column;
    align-items: stretch;
  }

  .header-right {
    flex-wrap: wrap;
  }

  .filter-bar {
    flex-direction: column;
    align-items: stretch;

    .filter-left {
      flex-wrap: wrap;
    }

    .filter-right {
      justify-content: flex-end;
    }
  }

  .stats-bar {
    flex-wrap: wrap;
    gap: 8px 16px;
    font-size: 13px;
    padding: 10px 16px;
  }

  .schedule-grid {
    grid-template-columns: 90px repeat(7, 100px);
  }

  .grid-doctor-column .doctor-cell {
    height: 100px;
    flex-direction: column;
    padding: 6px;

    .doctor-dept {
      text-align: center;
    }
  }

  .doctor-schedule-cell {
    height: 100px;
  }

  .schedule-block {
    .schedule-time {
      font-size: 9px;
    }
    .schedule-room {
      font-size: 8px;
    }
    .schedule-actions {
      opacity: 1;
      position: static;
      background: transparent;
      padding: 0;
    }
  }
}
</style>

<style>
/* 确保所有 teleported 组件的弹出层在弹窗之上 */
.el-select-dropdown,
.el-picker-panel,
.el-time-panel,
.el-popper {
  z-index: 9999 !important;
}

/* 确保下拉菜单背景为白色 */
.el-select-dropdown {
  background-color: #ffffff !important;
  border: 1px solid #dcdfe6 !important;
  border-radius: 4px !important;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1) !important;
}

.el-select-dropdown__item {
  background-color: #ffffff !important;
  color: #303133 !important;
}

.el-select-dropdown__item:hover {
  background-color: #f5f7fa !important;
}

.el-select-dropdown__item.is-selected {
  background-color: #ecf5ff !important;
  color: #409eff !important;
}

/* 日期选择器面板 */
.el-picker-panel {
  background-color: #ffffff !important;
  border: 1px solid #dcdfe6 !important;
  border-radius: 4px !important;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1) !important;
}

.el-picker-panel .el-date-table td {
  background-color: #ffffff !important;
}

.el-picker-panel .el-date-table td:hover {
  background-color: #f5f7fa !important;
}

.el-picker-panel .el-date-table td.current:not(.disabled) .el-date-table-cell {
  background-color: #409eff !important;
  color: #ffffff !important;
}

/* 时间选择器面板 */
.el-time-panel {
  background-color: #ffffff !important;
  border: 1px solid #dcdfe6 !important;
  border-radius: 4px !important;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1) !important;
}

.el-time-panel .el-time-spinner__item {
  background-color: #ffffff !important;
}

.el-time-panel .el-time-spinner__item:hover {
  background-color: #f5f7fa !important;
}

.el-time-panel .el-time-spinner__item.active:not(.disabled) {
  background-color: #ecf5ff !important;
  color: #409eff !important;
}
</style>
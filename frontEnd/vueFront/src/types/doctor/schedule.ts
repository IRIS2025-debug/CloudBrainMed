// src/types/schedule.ts

/**
 * 医生排班实体
 */
export interface DoctorSchedule {
  scheduleId: string
  planId?: string | null
  doctorId: string
  doctorName: string
  deptId: string
  workDate: string
  startTime: string
  endTime: string
  maxNum: number
  remainNum: number
  status: number
  price: number
  room: string
  sourceType: 'MANUAL' | 'AI_GENERATED'
  scheduleStatus: string
  createTime: string
  updateTime?: string | null
}

/**
 * 周视图 - 每天数据
 */
export interface WeekDayVo {
  date: string
  dayOfWeek: number
  dayName: string
  schedules: DoctorSchedule[]
}

/**
 * 周视图响应数据
 */
export interface WeeklyScheduleVo {
  weekStart: string
  weekEnd: string
  weekData: WeekDayVo[]
}

/**
 * API 统一响应
 */
export interface ApiResponse<T = any> {
  code: number
  message?: string
  msg?: string
  data: T
}
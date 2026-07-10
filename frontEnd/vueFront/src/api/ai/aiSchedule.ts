// src/api/admin/aiSchedule.ts
import request from '../request'

export interface AiScheduleGenerateRequest {
  doctorId: string                    // 单个医生ID（注意是单数）
  doctorName: string                  // 医生姓名
  deptId: string                      // 科室ID
  periodStart: string                 // 开始日期（对应 startDate）
  periodEnd: string                   // 结束日期（对应 endDate）
  requirement?: string                // 额外要求
  defaultMaxNum?: number              // 每时段号源（对应 maxNumPerSlot）
  defaultPrice?: number               // 挂号费（对应 price）
  rooms?: string[]                    // 诊室列表
  timeWindows?: {                     // 时段列表（对应 timeSlots）
    startTime: string
    endTime: string
  }[]
  unavailableDates?: string[]         // 不可排班日期
}

// 注意：后端没有 daysOfWeek、roomPrefix、avoidConflicts 字段
// 所以这些字段不需要发送

export interface AiScheduleConflictItem {
  doctorId: string
  doctorName: string
  workDate: string
  startTime: string
  endTime: string
  conflictType: string
  conflictDetail: string
}

export interface AiScheduleGenerateResponse {
  generatedCount: number
  schedules: Array<{
    doctorId: string
    doctorName: string
    deptId: string
    workDate: string
    startTime: string
    endTime: string
    maxNum: number
    price: number
    room: string
    conflict?: boolean
    conflictType?: string | null
    conflictReason?: string | null
  }>
  conflicts: Array<{
    doctorId: string
    doctorName: string
    workDate: string
    startTime: string
    endTime: string
    conflictType: string
    conflictDetail: string
  }>
}

// ✅ 修改：发布请求包含 items 数组，每个 item 是完整的排班数据
export interface AiSchedulePublishRequest {
  items: Array<{
    doctorId: string
    doctorName: string
    deptId: string
    workDate: string
    startTime: string
    endTime: string
    maxNum: number
    price: number
    room: string
    conflict?: boolean
    conflictType?: string | null
    conflictReason?: string | null
  }>
}

export interface AiSchedulePublishResponse {
  traceId?: string
  status: string
  submittedCount: number
  createdCount: number
  createdSchedules: any[]
  warnings: string[]
  failedItems?: Array<{
    index: number
    doctorId?: string
    doctorName?: string
    workDate?: string
    startTime?: string
    endTime?: string
    room?: string
    reason: string
    conflictType?: string
  }>
}

// AI排班预览
export function previewAiSchedule(data: AiScheduleGenerateRequest) {
  return request.post<AiScheduleGenerateResponse>(
    '/ai-service/schedule/preview',
    data
  )
}

// 冲突检查
export function checkConflicts(data: {
  items: AiScheduleGenerateResponse['schedules']
}) {
  return request.post<AiScheduleGenerateResponse>(
    '/ai-service/schedule/conflict-check',
    data
  )
}

// ✅ 修改：发布AI排班，发送 items 数组
export function publishAiSchedule(data: AiSchedulePublishRequest) {
  return request.post<AiSchedulePublishResponse>(
    '/ai-service/schedule/publish',
    data
  )
}

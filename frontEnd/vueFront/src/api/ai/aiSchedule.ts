// src/api/ai/aiSchedule.ts
import request from '../request'

export interface AiScheduleGenerateRequest {
  doctorId: string
  doctorName: string
  deptId: string
  periodStart: string
  periodEnd: string
  requirement?: string
  defaultMaxNum?: number
  defaultPrice?: number
  rooms?: string[]
  timeWindows?: {
    startTime: string
    endTime: string
  }[]
  unavailableDates?: string[]
}

export type ConflictType = 
  | 'DOCTOR_TIME' 
  | 'ROOM_TIME' 
  | 'BATCH_DOCTOR_TIME' 
  | 'BATCH_ROOM_TIME'
  | 'TIME_CONFLICT'
  | 'SAME_DOCTOR'
  | 'SAME_ROOM'
  | 'CHECK_FAILED'
  | 'CHECK_EXCEPTION'

export interface AiScheduleConflictItem {
  doctorId: string
  doctorName: string
  workDate: string
  startTime: string
  endTime: string
  conflictType: ConflictType
  conflictDetail: string
}

export interface AiScheduleItem {
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
  conflictType?: ConflictType | null
  conflictReason?: string | null
}

export interface AiScheduleGenerateResponse {
  generatedCount: number
  schedules: AiScheduleItem[]
  conflicts: AiScheduleConflictItem[]
  summary?: string
  status?: string
  fallback?: boolean
  modelVersion?: string
  warnings?: string[]
  optimizationReasons?: string[]
}

export interface AiSchedulePublishRequest {
  items: AiScheduleItem[]
  traceId?: string
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
    conflictType?: ConflictType
  }>
}

// ===== 工具函数 =====

/**
 * 判断是否为房间冲突
 * 包括 ROOM_TIME 和 BATCH_ROOM_TIME
 */
export function isRoomConflict(conflictType?: string | null): boolean {
  return conflictType === 'ROOM_TIME' || conflictType === 'BATCH_ROOM_TIME'
}

/**
 * 判断是否为医生时间冲突
 * 包括 DOCTOR_TIME 和 BATCH_DOCTOR_TIME
 */
export function isDoctorConflict(conflictType?: string | null): boolean {
  return conflictType === 'DOCTOR_TIME' || conflictType === 'BATCH_DOCTOR_TIME'
}

/**
 * 获取冲突类型显示文本
 */
export function getConflictTypeText(type?: string | null): string {
  if (!type) return '未知冲突'
  const map: Record<string, string> = {
    'DOCTOR_TIME': '医生时间冲突',
    'ROOM_TIME': '诊室时间冲突',
    'BATCH_DOCTOR_TIME': '批量医生冲突',
    'BATCH_ROOM_TIME': '批量诊室冲突',
    'TIME_CONFLICT': '时间冲突',
    'SAME_DOCTOR': '医生重复',
    'SAME_ROOM': '诊室占用',
    'CHECK_FAILED': '检查失败',
    'CHECK_EXCEPTION': '检查异常'
  }
  return map[type] || type
}

/**
 * 获取冲突标签类型
 */
export function getConflictTagType(type?: string | null): 'danger' | 'warning' | 'success' | 'info' {
  if (!type) return 'warning'
  if (isRoomConflict(type) || isDoctorConflict(type)) {
    return 'danger'
  }
  return 'warning'
}

/**
 * 获取冲突图标
 */
export function getConflictIcon(type?: string | null): string {
  if (!type) return '⚠️'
  if (isRoomConflict(type)) return '🏠'
  if (isDoctorConflict(type)) return '👨‍⚕️'
  return '⚠️'
}

// ===== API 函数 =====

export function previewAiSchedule(data: AiScheduleGenerateRequest) {
  return request.post<AiScheduleGenerateResponse>(
    '/ai-service/schedule/preview',
    data
  )
}

export function checkConflicts(data: {
  items: AiScheduleItem[]
}) {
  return request.post<AiScheduleGenerateResponse>(
    '/ai-service/schedule/conflict-check',
    data
  )
}

export function publishAiSchedule(data: AiSchedulePublishRequest) {
  return request.post<AiSchedulePublishResponse>(
    '/ai-service/schedule/publish',
    data
  )
}
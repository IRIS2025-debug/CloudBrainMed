
import request from '../request'

export interface ScheduleQueryDto {
  doctorId?: string
  deptId?: string
  startDate?: string
  endDate?: string
  page?: number
  limit?: number
}

export interface ScheduleSaveDto {
  doctorId: string
  doctorName: string
  deptId: string
  workDate: string
  startTime: string
  endTime: string
  maxNum: number
  price: number
  room: string
}

export interface ScheduleUpdateDto {
  scheduleId: string
  doctorId?: string
  doctorName?: string
  deptId?: string
  workDate?: string
  startTime?: string
  endTime?: string
  maxNum?: number
  remainNum?: number
  price?: number
  room?: string
  status?: number
}

export interface DoctorSchedule {
  scheduleId: string
  planId: string
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
  sourceType: string
  scheduleStatus: string
  createTime: string
  updateTime: string
}

// 分页查询排班列表
export function queryScheduleList(params: ScheduleQueryDto) {
  return request.post<{ records: DoctorSchedule[], total: number }>(
    '/admin-service/schedule/list',
    params
  )
}

// 获取医生周排班
export function getWeeklySchedule(doctorId: string, weekStart?: string) {
  return request.get<Record<string, DoctorSchedule[]>>(
    `/admin-service/schedule/weekly/${doctorId}`,
    { params: { weekStart } }
  )
}

// 获取所有医生周排班（总览）
export function getAllWeeklySchedule(weekStart?: string) {
  return request.get<Record<string, Record<string, DoctorSchedule[]>>>(
    '/admin-service/schedule/weekly/all',
    { params: { weekStart } }
  )
}

// 获取排班详情
export function getScheduleDetail(scheduleId: string) {
  return request.get<DoctorSchedule>(`/admin-service/schedule/detail/${scheduleId}`)
}

// 创建排班
export function createSchedule(data: ScheduleSaveDto) {
  return request.post<DoctorSchedule>('/admin-service/schedule/create', data)
}

// 更新排班
export function updateSchedule(data: ScheduleUpdateDto) {
  return request.put<DoctorSchedule>('/admin-service/schedule/update', data)
}

export function enableSchedule(scheduleId: string) {
  return request.put<boolean>(`/admin-service/schedule/enable/${scheduleId}`)
}

// 删除排班
export function deleteSchedule(scheduleId: string) {
  return request.delete<boolean>(`/admin-service/schedule/delete/${scheduleId}`)
}
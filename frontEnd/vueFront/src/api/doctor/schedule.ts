// src/api/doctor/schedule.ts

import request from '@/api/request'
import type { ApiResponse, WeeklyScheduleVo } from '@/types/doctor/schedule'

/**
 * 获取医生周排班
 * @param weekStart 周起始日期 (可选，格式: YYYY-MM-DD)
 */
export function getWeeklySchedule(weekStart?: string): Promise<ApiResponse<WeeklyScheduleVo>> {
  return request({
    url: '/doctor-service/schedule/weekly',
    method: 'get',
    params: weekStart ? { weekStart } : {}
  })
}
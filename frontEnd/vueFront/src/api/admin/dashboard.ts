import request from '../request'

export interface DashboardOverview {
  doctorCount: number
  departmentCount: number
  todayScheduleCount: number
  medicineCount: number
}

export function getDashboardOverview() {
  return request.get<DashboardOverview>('/admin-service/dashboard/overview')
}

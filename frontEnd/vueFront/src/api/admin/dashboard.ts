import request, { type ApiResponse } from '@/api/request'

export interface AdminDashboardOverview {
  doctorCount: number
  patientCount: number
  todayRegistrationCount: number
  pendingMedicalOrderCount: number
  lowStockMedicineCount: number
}

export function getAdminDashboardOverview(): Promise<ApiResponse<AdminDashboardOverview>> {
  return request({
    url: '/admin-service/dashboard/overview',
    method: 'get',
  })
}

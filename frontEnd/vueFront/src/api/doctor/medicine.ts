import request, { type ApiResponse } from '@/api/request'
import type { Medicine } from '@/types/admin/adminMedicine'

export function getMedicineList(keyword?: string): Promise<ApiResponse<Medicine[]>> {
  return request({
    url: '/doctor-service/medicine/list',
    method: 'get',
    params: { keyword }
  })
}

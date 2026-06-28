// src/api/admin/medicine.ts

import request, { type ApiResponse } from '@/api/request'
import type { Medicine, MedicineDto, MedicineWarnVo } from '@/types/admin/adminMedicine'

/**
 * 获取药品列表
 */
export function getMedicineList(keyword?: string): Promise<ApiResponse<Medicine[]>> {
  return request({
    url: '/admin-service/medicine/list',
    method: 'get',
    params: { keyword }
  })
}

/**
 * 获取药品详情
 */
export function getMedicineById(medicineId: string): Promise<ApiResponse<Medicine>> {
  return request({
    url: `/admin-service/medicine/${medicineId}`,
    method: 'get'
  })
}

/**
 * 添加药品
 */
export function addMedicine(data: MedicineDto): Promise<ApiResponse<null>> {
  return request({
    url: '/admin-service/medicine/add',
    method: 'post',
    data
  })
}

/**
 * 更新药品
 */
export function updateMedicine(data: MedicineDto): Promise<ApiResponse<null>> {
  return request({
    url: '/admin-service/medicine/update',
    method: 'put',
    data
  })
}

/**
 * 删除药品
 */
export function deleteMedicine(medicineId: string): Promise<ApiResponse<null>> {
  return request({
    url: `/admin-service/medicine/${medicineId}`,
    method: 'delete'
  })
}

/**
 * 扣除库存
 */
export function deductStock(medicineId: string, quantity: number): Promise<ApiResponse<null>> {
  return request({
    url: '/admin-service/medicine/deduct',
    method: 'post',
    data: { medicineId, quantity }
  })
}

/**
 * 增加库存（补货）
 */
export function addStock(medicineId: string, quantity: number): Promise<ApiResponse<null>> {
  return request({
    url: '/admin-service/medicine/add-stock',
    method: 'post',
    data: { medicineId, quantity }
  })
}

/**
 * 获取库存预警列表
 */
export function getWarnings(): Promise<ApiResponse<MedicineWarnVo[]>> {
  return request({
    url: '/admin-service/medicine/warnings',
    method: 'get'
  })
}

/**
 * 获取智能补货建议
 */
export function getReorderSuggestions(): Promise<ApiResponse<MedicineWarnVo[]>> {
  return request({
    url: '/admin-service/medicine/reorder-suggestions',
    method: 'get'
  })
}
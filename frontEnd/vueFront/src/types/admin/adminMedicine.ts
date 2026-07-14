// src/types/admin/adminMedicine.ts

// 导出状态类型
export type MedicineStatus = 'NORMAL' | 'LOW_STOCK' | 'OUT_OF_STOCK'

export interface Medicine {
  medicineId: string
  name: string
  spec: string
  usage: string
  indication: string
  attention: string
  stock: number
  price: number
  minStock: number      // 预警线（实际数量）
  minStockPercent?: number // 预警线百分比（前端计算展示）
  reorderQuantity: number // 前端配置，不存数据库
  status: MedicineStatus // 使用导出的类型
  createTime: string
}

export interface MedicineDto {
  medicineId?: string
  name: string
  spec: string
  usage: string
  indication: string
  attention: string
  stock: number
  price: number
  minStock?: number
  reorderQuantity?: number
  status?: string
}

export interface BatchEditDto {
  price: number | null
  warnPercent: number | null  // 预警线百分比
  reorderQuantity: number | null
}

export interface MedicineWarnVo {
  medicineId: string
  name: string
  spec: string
  stock: number
  minStock: number
  minStockPercent?: number
  status: string
  warnType: 'STOCK_WARNING' | 'REORDER_SUGGEST'
  suggestedReorder: number | null
}
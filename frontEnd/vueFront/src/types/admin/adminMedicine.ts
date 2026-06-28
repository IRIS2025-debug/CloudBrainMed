// src/types/admin/adminMedicine.ts

export interface Medicine {
  medicineId: string
  name: string
  spec: string
  usage: string
  indication: string
  attention: string
  stock: number
  price: number
  minStock: number      // 前端配置，不存数据库
  reorderQuantity: number // 前端配置，不存数据库
  status: 'NORMAL' | 'LOW_STOCK' | 'OUT_OF_STOCK' // 前端计算
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

export interface MedicineWarnVo {
  medicineId: string
  name: string
  spec: string
  stock: number
  minStock: number
  status: string
  warnType: 'STOCK_WARNING' | 'REORDER_SUGGEST'
  suggestedReorder: number | null
}
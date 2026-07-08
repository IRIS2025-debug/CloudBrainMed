import { defineStore } from 'pinia'

export interface CtImageThumb {
  name: string
  url: string
}

export interface CtStructuredResult {
  reportType?: string
  reportInput?: Record<string, any>
  previewImageUrl?: string
  previewImageFile?: string
  maskFile?: string
  downloadUrl?: string
  positivePixels?: number
  totalPixels?: number
  ratio?: number
  modelText?: string
  summaryText?: string
  sliceText?: string
}

export interface ExamReportPayload {
  orderItemId: string
  registerId: string
  // 受保护的预览图原始 URL（报告页自行 fetch 成 blob 展示）
  previewImageUrl: string
  images: CtImageThumb[]
  structured: {
    artifact?: CtStructuredResult
    lesion?: CtStructuredResult
  }
}

/**
 * 检查报告工作台联动 store：
 * CTInference 完成推理后把结果写入这里，ReportGeneration 按 orderItemId 取用，
 * 从而把真实 CT 推理结果导入影像分析区，替代占位假数据。
 */
export const useExamReportStore = defineStore('examReport', {
  state: () => ({
    payloadByOrderItem: {} as Record<string, ExamReportPayload>,
  }),
  actions: {
    setCtResult(payload: ExamReportPayload) {
      if (!payload.orderItemId) return
      this.payloadByOrderItem[payload.orderItemId] = payload
    },
    mergeCtResult(payload: ExamReportPayload) {
      if (!payload.orderItemId) return
      const existing = this.payloadByOrderItem[payload.orderItemId]
      this.payloadByOrderItem[payload.orderItemId] = {
        orderItemId: payload.orderItemId,
        registerId: payload.registerId || existing?.registerId || '',
        previewImageUrl: payload.previewImageUrl || existing?.previewImageUrl || '',
        images: [...(existing?.images || []), ...payload.images],
        structured: {
          ...(existing?.structured || {}),
          ...payload.structured,
        },
      }
    },
    getCtResult(orderItemId: string): ExamReportPayload | null {
      return this.payloadByOrderItem[orderItemId] || null
    },
    clearCtResult(orderItemId: string) {
      delete this.payloadByOrderItem[orderItemId]
    },
  },
})

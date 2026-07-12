import request from '../request'

/** 获取所有检验申请列表 */
export function getInspectionOrderList() {
  return request.get('/inspection-doctor/orders')
}

/** 获取检验申请详情 */
export function getInspectionOrderDetail(orderId: string) {
  return request.get(`/inspection-doctor/order/${orderId}`)
}

/** 分配检查/检验申请到房间并进入排队 */
export function assignInspectionOrder(orderId: string, assignedRoom: string) {
  return request.post(`/inspection-doctor/order/${orderId}/assign`, { assignedRoom })
}

export interface ApiResponse<T = any> {
  code: number;
  message: string;
  data: T;
}
export interface GenerateConclusionParams {
  resultSummary: string
}

export interface GenerateConclusionData {
  conclusion: string
  confidence?: number
  suggestions?: string[]
}
/**
 * AI 生成检验结论
 */
export function generateReportConclusion(data: GenerateConclusionParams): Promise<ApiResponse> {
  return request.post('/exam-service/report/create', data)
}

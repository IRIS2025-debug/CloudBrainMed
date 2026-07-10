import request from '../request'

export function getConsultList(params: { consultStatus?: string; date?: string; reportReturnedOnly?: boolean; page: number; limit: number }) {
  return request.get('/doctor-service/consult/list', { params })
}

export function getConsultDetail(registerId: string) {
  return request.get('/doctor-service/consult/detail', { params: { registerId } })
}

export function saveDraft(data: { registerId: string; recordDesc: string }) {
  return request.post('/doctor-service/consult/save-draft', data)
}

export function confirmRecord(data: { registerId: string; recordDesc: string }) {
  return request.post('/doctor-service/consult/confirm-record', data)
}

export function createExamOrder(data: { registerId: string; checkItemList: string; urgencyLevel: string }) {
  return request.post('/doctor-service/consult/create-exam-order', data)
}

export interface MedicalOrderConfirmResult {
  orderId: string
  sourceType: 'AI_ASSISTED' | 'MANUAL'
  itemCount: number
  totalAmount: number
  status: 'WAITING_ASSIGN' | 'QUEUED'
  payStatus: 'WAITING' | 'PAID'
  queueReady: boolean
  paymentMessage?: string | null
}

export function confirmMedicalOrder(data: {
  registerId: string
  aiTraceId?: string
  clinicalSummary: string
  urgencyLevel: string
  items: Array<{ itemCode: string; urgencyLevel?: string }>
}) {
  return request.post<MedicalOrderConfirmResult>('/doctor-service/consult/medical-order/confirm', data)
}

export function getConsultReports(registerId: string) {
  return request.get('/doctor-service/consult/reports', { params: { registerId } })
}

export function completeConsult(registerId: string) {
  return request.post('/doctor-service/consult/complete', { registerId })
}

export function createPrescription(data: {
  registerId: string
  medicineId?: string
  medicineName: string
  spec: string
  usage: string
  num: number
  price: number
}) {
  return request.post('/doctor-service/consult/create-prescription', data)
}

export function getPrescriptionList(registerId: string) {
  return request.get('/doctor-service/consult/prescription-list', { params: { registerId } })
}

import request from '../request'

export function getConsultList(params: { consultStatus?: string; date?: string; page: number; limit: number }) {
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

export function completeConsult(registerId: string) {
  return request.post('/doctor-service/consult/complete', { registerId })
}

export function aiAnalyze(data: {
  registerId: string
  chiefComplaint: string
  recordDesc: string
  patientAge: string
  patientGender: string
}) {

  return request.post('/doctor-service/consult/ai-analyze', data)
}

export function createPrescription(data: {
  registerId: string
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

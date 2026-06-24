import request from '../request'

export function assistantChat(data: {
  registerId: string
  message: string
  currentRecordDesc: string
  symptomDescription?: string
  conversationText?: string
  structuredParameters?: Record<string, string>
  followUpAnswers: Record<string, string>
  patientInformation?: Record<string, string>
  medicines?: Array<{
    medicineId: string
    usage: string
    quantity: number
  }>
}) {
  return request.post('/api/ai/reception/chat', data, { timeout: 120000 })
}

export function generateMedicalRecord(data: {
  registerId: string
  conversationText?: string
  structuredParameters?: Record<string, string>
  currentRecordDesc?: string
}) {
  return request.post('/api/ai/reception/record/generate', data, { timeout: 120000 })
}

export function reviewPrescription(data: {
  registerId: string
  currentRecordDesc?: string
  patientInformation?: Record<string, string>
  medicines: Array<{
    medicineId: string
    usage: string
    quantity: number
  }>
}) {
  return request.post('/api/ai/prescription/review', data, { timeout: 120000 })
}

export function submitAiFeedback(data: {
  traceId: string
  finalRecordDesc: string
  adoptionType: 'FULL' | 'PARTIAL' | 'REJECTED'
}) {
  return request.post('/api/ai/reception/feedback', data)
}

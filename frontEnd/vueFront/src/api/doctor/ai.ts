import request from '../request'

export type AiAssistantActionType =
  | 'FOLLOW_UP_QUESTION'
  | 'MISSING_INFORMATION'
  | 'CONTEXT_SUMMARY'
  | 'CONTEXT_QA'
  | 'MEDICAL_RECORD_DRAFT'
  | 'PRESCRIPTION_REVIEW'
  | 'DIAGNOSIS_ASSISTANT'

export interface AiAssistantChatResponse {
  traceId?: string
  intent?: string
  answer?: string
  status?: string
  handledModule?: string
  moduleResult?: unknown
  modelVersion?: string
  handledByAssistant?: boolean
  fallback?: boolean
}

export function assistantChat(data: {
  registerId: string
  message?: string
  actionType?: AiAssistantActionType
  currentRecordDesc?: string
  symptomDescription?: string
  conversationText?: string
  structuredParameters?: Record<string, string>
  followUpAnswers?: Record<string, string>
  patientInformation?: Record<string, string>
  medicines?: Array<{
    medicineId: string
    usage: string
    quantity: number
  }>
}) {
  return request.post<AiAssistantChatResponse>('/ai-service/reception/chat', data, { timeout: 120000 })
}

export interface PrescriptionDraftMedicine {
  medicineId: string
  medicineName?: string
  spec?: string
  usage?: string
  quantity?: number
  reason?: string
  warnings?: string[]
}

export interface PrescriptionDraftResponse {
  status?: string
  summary?: string
  overallRiskLevel?: string
  medicines?: PrescriptionDraftMedicine[]
  missingInformation?: string[]
  warnings?: string[]
  modelVersion?: string
  fallback?: boolean
}

export interface PrescriptionDraftRequest {
  registerId: string
  message?: string
  currentRecordDesc?: string
  symptomDescription?: string
  conversationText?: string
  structuredParameters?: Record<string, string>
  followUpAnswers?: Record<string, string>
  patientInformation?: Record<string, string>
}

export function generatePrescriptionDraft(data: PrescriptionDraftRequest) {
  return request.post<PrescriptionDraftResponse>('/ai-service/prescription/draft', data, { timeout: 120000 })
}

export interface ReportAnalysisResponse {
  summary?: string
  riskLevel?: 'LOW' | 'MEDIUM' | 'HIGH' | string
  abnormalIndicators?: Array<{
    name?: string
    value?: string
    referenceRange?: string
    interpretation?: string
  }>
  suggestions?: string[]
  followUpAdvice?: string
  fallback?: boolean
}

export function analyzeReport(data: {
  registerId: string
  reportType: string
  reportText: string
}) {
  return request.post<ReportAnalysisResponse>('/ai-service/report/analyze', data, { timeout: 120000 })
}

export interface AiExamRecommendationItem {
  itemCode?: string
  itemName?: string
  name?: string
  itemCategory?: 'EXAM' | 'LAB' | string
  category?: string
  dept?: string
  deptName?: string
  departmentName?: string
  urgencyLevel?: string
  urgency?: string
  reason?: string
  purpose?: string
  selected?: boolean
  estimatedFee?: number
}

export interface AiExamRecommendResponse {
  traceId?: string
  status?: string
  summary?: string
  clinicalSummary?: string
  fallback?: boolean
  urgencyLevel?: string
  recommendations?: AiExamRecommendationItem[]
  items?: AiExamRecommendationItem[]
  examItems?: AiExamRecommendationItem[]
  checkItems?: AiExamRecommendationItem[]
}

export function recommendExamItems(data: {
  registerId: string
  patientId?: string
  chiefComplaint?: string
  recordDesc?: string
  patientAge?: string
  patientGender?: string
  structuredParameters?: Record<string, string>
}) {
  return request.post<AiExamRecommendResponse | AiExamRecommendationItem[]>('/ai-service/agent/exam/generate', {
    registerId: data.registerId,
    context: {
      patientId: data.patientId || '',
      visitAge: Number(data.patientAge || 0),
      description: data.recordDesc || data.chiefComplaint || ''
    }
  }, { timeout: 120000 })
}

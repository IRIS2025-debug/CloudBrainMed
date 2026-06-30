import request from '../request'

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
  actionType?: string
  currentRecordDesc?: string
  symptomDescription?: string
  conversationText?: string
  structuredParameters?: Record<string, string>
  followUpAnswers?: Record<string, string>
}) {
  return request.post<AiAssistantChatResponse>('/ai-service/reception/chat', data, { timeout: 120000 })
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

import request from '../request'

export function generateExamSuggestions(data: {
  registerId: string
  context: {
    patientId: string
    visitAge: number
    description: string
  }
}) {
  return request.post('/ai-service/agent/exam/generate', data, { timeout: 120000 })
}
import request from '../request'

export function getTaskDetail(orderItemId: string) {
  return request.get('/doctor-service/task/detail', { params: { orderItemId } })
}

export function startTask(orderItemId: string) {
  return request.post('/doctor-service/task/start', { orderItemId })
}

export function completeTask(orderItemId: string) {
  return request.post('/doctor-service/task/complete', { orderItemId })
}

export function submitTaskReport(data: {
  orderItemId: string
  resultSummary?: string
  conclusion?: string
  abnormalFlag?: string
  attachmentUrl?: string
  aiResultJson?: string
}) {
  return request.post('/doctor-service/task/report', data)
}

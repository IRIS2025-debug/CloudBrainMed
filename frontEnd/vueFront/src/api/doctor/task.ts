import request from '../request'

export function getWorkbench() {
  return request.get('/doctor-service/task/workbench')
}

export function getQueue() {
  return request.get('/doctor-service/task/queue')
}

export function getTaskDetail(orderItemId: string) {
  return request.get('/doctor-service/task/detail', { params: { orderItemId } })
}

export function startTask(orderItemId: string) {
  return request.post('/doctor-service/task/start', { orderItemId })
}

export function completeTask(orderItemId: string) {
  return request.post('/doctor-service/task/complete', { orderItemId })
}

/**
 * 生成检验报告（待后端实现）
 * TODO: 等待后端同学提供接口后替换 URL 和参数
 */
export function generateLabReport(orderItemId: string) {
  return request.post('/doctor-service/task/generate-report', { orderItemId })
}
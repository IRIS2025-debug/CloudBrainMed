import request from '../request'

export function getInferenceStats() {
  return request.get('/admin-service/ml/dashboard/inference-stats')
}

export function getModelStats() {
  return request.get('/admin-service/ml/dashboard/model-stats')
}

export function getInferenceLogs(params: { page: number; limit: number }) {
  return request.get('/admin-service/ml/inference/logs', { params })
}

/** 固定两个业务模型（CT 金属伪影 / CT 病灶），版本与状态来自 Python 健康响应 */
export function getModelList() {
  return request.get('/admin-service/ml/model/list')
}

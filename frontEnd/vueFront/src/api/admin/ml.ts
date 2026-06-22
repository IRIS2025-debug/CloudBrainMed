import request from '../request'

export function getInferenceStats() {
  return request.get('/admin-service/ml/dashboard/inference-stats')
}

export function getInferenceLogs(params: { page: number; limit: number }) {
  return request.get('/admin-service/ml/inference/logs', { params })
}

export function getSampleList(params: { page: number; limit: number }) {
  return request.get('/admin-service/ml/sample/list', { params })
}

export function labelSample(data: { sampleId: string; labelTag: string }) {
  return request.put('/admin-service/ml/sample/label', data)
}

export function triggerTraining() {
  return request.post('/admin-service/ml/model/train')
}

export function setModelTraffic(data: { modelId: string; trafficPct: number }) {
  return request.put('/admin-service/ml/model/traffic', data)
}
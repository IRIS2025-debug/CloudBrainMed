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

export function getSampleList(params: { page: number; limit: number }) {
  return request.get('/admin-service/ml/sample/list', { params })
}

export function labelSample(data: { sampleId: string; labelTag: string }) {
  return request.put('/admin-service/ml/sample/label', data)
}

export interface TrainingParams {
  modelKey: string
  modelType: string
  learningRate: string
  epochs: string
  batchSize: string
  optimizer: string
  datasetPath: string
}

export function triggerTraining(data?: TrainingParams) {
  return request.post('/admin-service/ml/model/train', data)
}

export function getTrainingTasks() {
  return request.get('/admin-service/ml/models/tasks')
}

export function getModelList() {
  return request.get('/admin-service/ml/model/list')
}

export function setModelTraffic(data: { modelId: string; trafficPct: number }) {
  return request.put('/admin-service/ml/model/traffic', data)
}

export function predictCtArtifact(file: File) {
  const form = new FormData()
  form.append('file', file)
  return request.post('/admin-service/ml/inference/ct-artifact', form, {
    timeout: 300000,
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

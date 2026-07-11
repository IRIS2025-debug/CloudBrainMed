import request from '../request'

export function getInferenceStats() {
  return request.get('/admin-service/ml/dashboard/inference-stats')
}

export function getModelList() {
  return request.get('/admin-service/ml/model/list')
}

export function predictCtArtifact(file: File) {
  const form = new FormData()
  form.append('file', file)
  return request.post('/admin-service/ml/inference/ct-artifact', form, {
    timeout: 300000,
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

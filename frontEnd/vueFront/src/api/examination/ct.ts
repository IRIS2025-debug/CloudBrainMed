import request from '../request'

/**
 * CT 伪影检测，检查医生端（doctorType=2）使用。
 * 前端调用医生语义路径，由网关或 Vite 开发代理转发到 AI 服务。
 */
export function predictCtArtifact(file: File) {
  const form = new FormData()
  form.append('file', file)
  return request.post('/doctor-service/exam/ct-artifact', form, {
    timeout: 300000,
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export function predictCtLesion(file: File) {
  const form = new FormData()
  form.append('file', file)
  return request.post('/doctor-service/exam/ct-lesion', form, {
    timeout: 300000,
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

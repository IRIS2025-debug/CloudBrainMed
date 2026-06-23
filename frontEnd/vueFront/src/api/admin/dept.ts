import request from '../request'

export function getDeptList() {
  return request.get('/admin-service/dept/list')
}
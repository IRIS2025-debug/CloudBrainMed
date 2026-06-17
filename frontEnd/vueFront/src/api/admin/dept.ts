import request from '../request'

export function getDeptList() {
  return request.get('/api/admin/dept/list')
}
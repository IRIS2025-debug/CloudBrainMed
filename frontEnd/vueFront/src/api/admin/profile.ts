import request from '../request'

/** 获取管理员个人信息 */
export function getAdminInfo() {
  return request.get('/admin-service/profile/info')
}

/** 更新管理员个人信息 */
export function updateAdminProfile(data: { email: string; phone: string }) {
  return request.put('/admin-service/profile/update', data)
}

/** 修改密码 */
export function changeAdminPassword(data: { oldPassword: string; newPassword: string }) {
  return request.post('/admin-service/profile/change-password', data)
}

/** 上传管理员头像 */
export function uploadAdminAvatar(file: File) {
  const form = new FormData()
  form.append('file', file)
  return request.post('/admin-service/profile/avatar-upload', form, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}
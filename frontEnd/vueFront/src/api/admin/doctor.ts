import request from '../request'

/** 获取医生列表 */
export function getDoctorList() {
  return request.get('/api/admin/doctor-manage/list')
}

/** 获取医生详情 */
export function getDoctorDetail(doctorId: string) {
  return request.get(`/api/admin/doctor-manage/detail/${doctorId}`)
}

/** 新增医生 */
export function addDoctor(data: {
  name: string
  gender: number
  phone: string
  email: string
  position: string
  goodAt: string
  introduction: string
  departmentId: string
}) {
  return request.post('/api/admin/doctor-manage/add', data)
}

/** 修改医生 */
export function updateDoctor(data: {
  doctorId: string
  name: string
  gender: number
  phone: string
  email: string
  position: string
  goodAt: string
  introduction: string
  departmentId: string
  status: number
}) {
  return request.put('/api/admin/doctor-manage/update', data)
}

/** 删除医生 */
export function deleteDoctor(doctorId: string) {
  return request.delete(`/api/admin/doctor-manage/delete/${doctorId}`)
}
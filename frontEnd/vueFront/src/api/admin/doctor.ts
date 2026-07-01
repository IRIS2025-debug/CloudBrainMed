import request from '../request'

/** 获取医生列表 */
export function getDoctorList() {
  return request.get('/admin-service/doctor-manage/list')
}

/** 获取医生详情 */
export function getDoctorDetail(doctorId: string) {
  return request.get(`/admin-service/doctor-manage/detail/${doctorId}`)
}

/** 新增医生 */
export function addDoctor(data: {
  name: string
  gender: number | null
  phone: string
  email: string
  position: string
  goodAt: string
  introduction: string
  departmentId: string
}) {
  return request.post('/admin-service/doctor-manage/add', data)
}

/** 修改医生 */
export function updateDoctor(data: {
  doctorId: string
  name: string
  gender: number | null
  phone: string
  email: string
  position: string
  goodAt: string
  introduction: string
  departmentId: string
  status: number
}) {
  return request.put('/admin-service/doctor-manage/update', data)
}

/** 删除医生 */
export function deleteDoctor(doctorId: string) {
  return request.delete(`/admin-service/doctor-manage/delete/${doctorId}`)
}

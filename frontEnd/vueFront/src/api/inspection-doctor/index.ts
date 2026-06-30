import request from '../request'

/** 获取所有检验申请列表 */
export function getInspectionOrderList() {
  return request.get('/inspection-doctor/lab-orders')
}

/** 获取检验申请详情 */
export function getInspectionOrderDetail(orderId: string) {
  return request.get(`/inspection-doctor/order/${orderId}`)
}

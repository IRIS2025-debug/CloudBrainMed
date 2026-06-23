import request from '../request'

/** 获取所有检验申请列表 */
export function getInspectionOrderList() {
  return request.get('/api/inspection-doctor/order/list')
}

/** 获取检验申请详情 */
export function getInspectionOrderDetail(orderId: string) {
  return request.get(`/api/inspection-doctor/order/detail/${orderId}`)
}

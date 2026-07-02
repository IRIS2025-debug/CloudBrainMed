import request from '../request'

/** 获取所有检验申请列表 */
export function getInspectionOrderList() {
  return request.get('/inspection-doctor/orders')
}

/** 获取检验申请详情 */
export function getInspectionOrderDetail(orderId: string) {
  return request.get(`/inspection-doctor/order/${orderId}`)
}

/** 分配检查/检验申请到房间并进入排队 */
export function assignInspectionOrder(orderId: string, assignedRoom: string) {
  return request.post(`/inspection-doctor/order/${orderId}/assign`, { assignedRoom })
}

import request from '../request'

/** 获取所有检验申请列表 */
export function getInspectionOrderList() {
  return request.get('/inspection-doctor/orders')
}

/** 获取检验申请详情 */
export function getInspectionOrderDetail(orderItemId: string) {
  return request.get(`/inspection-doctor/order-item/${orderItemId}`)
}

/** 分配检查/检验申请到房间并进入排队 */
export function assignInspectionOrder(orderItemId: string, assignedRoom: string) {
  return request.post(`/inspection-doctor/order-item/${orderItemId}/assign`, { assignedRoom })
}

export type DoctorMenuItem = {
  path: string
  title: string
  icon: string
  group: string
}

const consultationDoctorMenus: DoctorMenuItem[] = [
  { path: '/doctor/home', title: '首页概况', icon: 'HomeFilled', group: '接诊医生' },
  { path: '/doctor/consult', title: '接诊工作台', icon: 'List', group: '接诊医生' },
  { path: '/doctor/profile', title: '医生个人信息', icon: 'UserFilled', group: '接诊医生' },
  { path: '/doctor/schedule', title: '值班查询', icon: 'List', group: '接诊医生' },
]

const examinationDoctorMenus: DoctorMenuItem[] = [
  { path: '/examination-doctor/home', title: '检查工作台', icon: 'HomeFilled', group: '检查医生' },
  { path: '/examination-doctor/application', title: '查看检查申请', icon: 'List', group: '检查医生' },
  { path: '/doctor/profile', title: '医生个人信息', icon: 'UserFilled', group: '检查医生' },
  { path: '/doctor/schedule', title: '值班查询', icon: 'List', group: '检查医生' },
]

const inspectionDoctorMenus: DoctorMenuItem[] = [
  { path: '/inspection-doctor/home', title: '检验工作台', icon: 'HomeFilled', group: '检验医生' },
  { path: '/inspection-doctor/order-list', title: '查看检验申请', icon: 'List', group: '检验医生' },
  { path: '/doctor/profile', title: '医生个人信息', icon: 'UserFilled', group: '检验医生' },
  { path: '/doctor/schedule', title: '值班查询', icon: 'List', group: '检验医生' },
]

export function getDoctorMenuItems(doctorType: number): DoctorMenuItem[] {
  if (doctorType === 2) return examinationDoctorMenus
  if (doctorType === 3) return inspectionDoctorMenus
  return consultationDoctorMenus
}

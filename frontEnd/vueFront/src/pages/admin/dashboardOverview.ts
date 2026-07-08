import { UserFilled, Cpu, DataAnalysis, CollectionTag, List } from '@element-plus/icons-vue'
import type { AdminDashboardOverview } from '@/api/admin/dashboard'

export type AdminOverviewStat = {
  label: string
  value: string
  note: string
  icon: typeof UserFilled
  color: string
}

type OverviewStatDefinition = {
  key: keyof AdminDashboardOverview
  label: string
  note: string
  icon: typeof UserFilled
  color: string
}

const overviewStatDefinitions: OverviewStatDefinition[] = [
  {
    key: 'doctorCount',
    label: '医生总数',
    note: '当前可用医生账号',
    icon: UserFilled,
    color: '#315fbb',
  },
  {
    key: 'patientCount',
    label: '患者总数',
    note: '已建档患者数量',
    icon: DataAnalysis,
    color: '#0f766e',
  },
  {
    key: 'todayRegistrationCount',
    label: '今日挂号',
    note: '今日就诊安排',
    icon: CollectionTag,
    color: '#7c3aed',
  },
  {
    key: 'pendingMedicalOrderCount',
    label: '待处理医技单',
    note: '待分配或排队项目',
    icon: Cpu,
    color: '#f59e0b',
  },
  {
    key: 'lowStockMedicineCount',
    label: '低库存药品',
    note: '库存低于预警线',
    icon: List,
    color: '#ef4444',
  },
]

export function buildAdminOverviewStats(
  overview: AdminDashboardOverview | null | undefined,
): AdminOverviewStat[] {
  return overviewStatDefinitions.map((definition) => ({
    label: definition.label,
    value: formatCount(overview?.[definition.key]),
    note: definition.note,
    icon: definition.icon,
    color: definition.color,
  }))
}

function formatCount(value: number | null | undefined) {
  return value === null || value === undefined ? '--' : String(value)
}

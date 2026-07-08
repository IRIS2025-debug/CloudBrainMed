import test from 'node:test'
import assert from 'node:assert/strict'

import {
  buildAdminOverviewStats,
} from '../src/pages/admin/dashboardOverview.ts'

test('admin overview stats labels match backend overview fields', () => {
  const stats = buildAdminOverviewStats({
    doctorCount: 12,
    patientCount: 34,
    todayRegistrationCount: 5,
    pendingMedicalOrderCount: 6,
    lowStockMedicineCount: 7,
  })

  assert.deepEqual(
    stats.map((item) => [item.label, item.value]),
    [
      ['医生总数', '12'],
      ['患者总数', '34'],
      ['今日挂号', '5'],
      ['待处理医技单', '6'],
      ['低库存药品', '7'],
    ],
  )
})

test('admin overview stats keep placeholders when overview is missing', () => {
  const stats = buildAdminOverviewStats(null)

  assert.equal(stats.length, 5)
  assert.deepEqual(stats.map((item) => item.value), ['--', '--', '--', '--', '--'])
})

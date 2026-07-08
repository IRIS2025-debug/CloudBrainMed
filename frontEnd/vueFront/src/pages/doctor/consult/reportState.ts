export type ConsultListReportStateSource = {
  medicalOrderCount?: number | null
  reportCount?: number | null
  hasMedicalOrder?: boolean | null
  hasReturnedReport?: boolean | null
}

export function getConsultReportState(row: ConsultListReportStateSource) {
  const medicalOrderCount = Number(row.medicalOrderCount ?? 0)
  const reportCount = Number(row.reportCount ?? 0)
  const hasMedicalOrder = row.hasMedicalOrder === true || medicalOrderCount > 0
  const hasReturnedReport = row.hasReturnedReport === true || reportCount > 0

  if (hasReturnedReport) {
    return {
      kind: 'returned' as const,
      text: reportCount > 1 ? `已回传 ${reportCount}` : '已回传',
    }
  }

  if (hasMedicalOrder) {
    return {
      kind: 'pending' as const,
      text: '待回传',
    }
  }

  return {
    kind: 'none' as const,
    text: '未开单',
  }
}

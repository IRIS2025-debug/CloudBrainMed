export type ConsultListEntryStateSource = {
  consultStatus?: string | null
  entryAllowed?: boolean | null
  entryActionText?: string | null
  entryBlockedReason?: string | null
  readOnly?: boolean | null
}

export function getConsultEntryState(row: ConsultListEntryStateSource) {
  const text = row.entryActionText?.trim()
    || (row.consultStatus === 'COMPLETED' ? '查看' : '接诊')
  const disabled = row.entryAllowed === false
    || (row.readOnly === true && row.consultStatus !== 'COMPLETED')
  const reason = disabled ? (row.entryBlockedReason || '') : ''
  const tone = disabled ? 'muted' : (row.readOnly === true ? 'success' : 'primary')

  return {
    disabled,
    text,
    reason,
    reasonVisible: reason.length > 0,
    tone,
  }
}

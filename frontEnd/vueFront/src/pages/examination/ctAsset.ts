export type CtAssetKind = 'preview' | 'result'

function extractTerminalPathValue(value: string) {
  if (!value) return ''
  const path = /^https?:\/\//i.test(value) ? new URL(value).pathname : value
  return path.split('/').filter(Boolean).pop() || ''
}

export function buildDoctorCtAssetUrl(
  apiBaseUrl: string,
  endpoint: string,
  kind: CtAssetKind,
  value: string,
  token?: string,
) {
  const filename = extractTerminalPathValue(value)
  if (!filename) return ''

  const normalizedBaseUrl = apiBaseUrl.replace(/\/$/, '')
  void token
  return `${normalizedBaseUrl}/doctor-service/exam/${endpoint}/${kind}/${encodeURIComponent(filename)}`
}

export function resolveBinaryFilename(value: string, fallback: string) {
  return extractTerminalPathValue(value) || fallback
}

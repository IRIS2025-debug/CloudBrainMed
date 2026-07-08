export function resolveAssetUrl(
  path: string | null | undefined,
  origin = typeof window === 'undefined' ? 'http://localhost' : window.location.origin,
) {
  if (!path) {
    return ''
  }
  if (/^https?:\/\//i.test(path) || path.startsWith('data:')) {
    return path
  }
  return new URL(path, origin).toString()
}

export type CtDatasetFile = {
  name: string
  size: number
}

export function isCtDatasetFileName(fileName: string) {
  return /\.nii(\.gz)?$/i.test(fileName)
}

export function formatCtDatasetSize(size: number) {
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / (1024 * 1024)).toFixed(1)} MB`
}

export function buildCtDatasetSummary(file: CtDatasetFile | null, shapeText = '') {
  if (!file) return null

  return {
    fileName: file.name,
    fileSize: formatCtDatasetSize(file.size),
    shapeText,
  }
}

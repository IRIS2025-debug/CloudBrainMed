export interface AiExamRecommendationItem {
  itemCode?: string
  itemName?: string
  name?: string
  itemCategory?: 'EXAM' | 'LAB' | string
  category?: string
  dept?: string
  deptName?: string
  departmentName?: string
  urgencyLevel?: string
  urgency?: string
  reason?: string
  purpose?: string
  selected?: boolean
}

export interface AiExamRecommendResponse {
  traceId?: string
  status?: string
  summary?: string
  clinicalSummary?: string
  fallback?: boolean
  urgencyLevel?: string
  recommendations?: AiExamRecommendationItem[]
  items?: AiExamRecommendationItem[]
  examItems?: AiExamRecommendationItem[]
  checkItems?: AiExamRecommendationItem[]
}

export interface NormalizedExamRecommendation {
  id: number
  itemCode: string
  itemName: string
  category: string
  dept: string
  urgencyLevel: string
  reason: string
  selected: boolean
  needsMapping: boolean
}

export interface MedicalItemOption {
  itemCode: string
  itemName: string
  category: 'EXAM' | 'LAB'
  dept: string
}

export const medicalItemOptions: MedicalItemOption[] = [
  // 🏥 EXAM（检查类）
  { itemCode: 'NEURO_CT_001', itemName: '头颅CT平扫', category: 'EXAM', dept: '影像科' },
  { itemCode: 'NEURO_CT_002', itemName: '头颅CT增强', category: 'EXAM', dept: '影像科' },
  { itemCode: 'NEURO_MRI_001', itemName: '脑部MRI平扫', category: 'EXAM', dept: '影像科' },
  { itemCode: 'NEURO_MRI_002', itemName: '脑部MRI增强', category: 'EXAM', dept: '影像科' },
  { itemCode: 'NEURO_MRA_001', itemName: '脑血管MRA检查', category: 'EXAM', dept: '影像科' },
  // 🧪 LAB（检验类）
  { itemCode: 'NEURO_LAB_001', itemName: '血常规检查', category: 'LAB', dept: '检验科' },
  { itemCode: 'NEURO_LAB_002', itemName: 'C反应蛋白CRP', category: 'LAB', dept: '检验科' },
  { itemCode: 'NEURO_LAB_003', itemName: '脑脊液常规检查', category: 'LAB', dept: '检验科' },
]

const itemByCode = new Map(medicalItemOptions.map(item => [item.itemCode, item]))
const medicalItemCodeByName: Record<string, string> = medicalItemOptions.reduce<Record<string, string>>((acc, item) => {
  acc[item.itemName.replace(/\s+/g, '')] = item.itemCode
  return acc
}, {})

const labItemCodes = new Set([
  'NEURO_LAB_001',
  'NEURO_LAB_002',
  'NEURO_LAB_003'
])

export function normalizeExamRecommendResponse(data: AiExamRecommendResponse | AiExamRecommendationItem[] | undefined) {
  if (Array.isArray(data)) {
    return {
      summary: '',
      items: data.map(item => normalizeExamRecommendation(item, '')).filter((item): item is NormalizedExamRecommendation => Boolean(item))
    }
  }
  const rawItems = data?.recommendations || data?.items || data?.examItems || data?.checkItems || []
  const fallbackUrgency = data?.urgencyLevel || ''
  return {
    summary: data?.summary || data?.clinicalSummary || '',
    items: rawItems
      .map(item => normalizeExamRecommendation(item, fallbackUrgency))
      .filter((item): item is NormalizedExamRecommendation => Boolean(item))
  }
}

function normalizeExamRecommendation(item: AiExamRecommendationItem, fallbackUrgency: string): NormalizedExamRecommendation | null {
  const itemName = String(item.itemName || item.name || item.itemCode || '').trim()
  if (!itemName) return null
  const itemCode = String(item.itemCode || inferMedicalItemCode(itemName)).trim().toUpperCase()
  const matchedItem = itemByCode.get(itemCode)
  const urgency = String(item.urgencyLevel || item.urgency || fallbackUrgency || 'NORMAL').trim().toUpperCase()
  return {
    id: Date.now() + Math.random(),
    itemCode,
    itemName: matchedItem?.itemName || itemName,
    category: inferCategory(item, itemCode),
    dept: String(item.dept || item.deptName || item.departmentName || matchedItem?.dept || '').trim(),
    urgencyLevel: ['NORMAL', 'URGENT', 'EMERGENCY'].includes(urgency) ? urgency : 'NORMAL',
    reason: String(item.reason || item.purpose || '').trim(),
    selected: item.selected !== false,
    needsMapping: !matchedItem
  }
}

function inferMedicalItemCode(itemName: string) {
  const normalized = itemName.replace(/\s+/g, '')
  return medicalItemCodeByName[normalized] || ''
}

function inferCategory(item: AiExamRecommendationItem, itemCode: string) {
  const explicitCategory = String(item.itemCategory || item.category || '').trim().toUpperCase()
  if (explicitCategory === 'EXAM' || explicitCategory === 'LAB') return explicitCategory
  return labItemCodes.has(itemCode) ? 'LAB' : 'EXAM'
}
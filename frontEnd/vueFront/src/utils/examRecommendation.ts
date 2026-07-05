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
  { itemCode: 'CRANIAL_CT_PLAIN', itemName: '颅脑CT平扫', category: 'EXAM', dept: '影像科' },
  { itemCode: 'CRANIAL_CT_ENHANCED', itemName: '颅脑CT增强', category: 'EXAM', dept: '影像科' },
  { itemCode: 'CRANIAL_MRI_PLAIN', itemName: '颅脑MRI平扫', category: 'EXAM', dept: '影像科' },
  { itemCode: 'CRANIAL_MRI_ENHANCED', itemName: '颅脑MRI增强', category: 'EXAM', dept: '影像科' },
  { itemCode: 'HEAD_NECK_CTA', itemName: '头颈部CTA', category: 'EXAM', dept: '影像科' },
  { itemCode: 'CRANIAL_MRA', itemName: '颅脑MRA', category: 'EXAM', dept: '影像科' },
  { itemCode: 'CEREBRAL_DSA', itemName: '全脑血管DSA', category: 'EXAM', dept: '介入科' },
  { itemCode: 'TCD', itemName: '经颅多普勒', category: 'EXAM', dept: '功能检查科' },
  { itemCode: 'ROUTINE_EEG', itemName: '常规脑电图', category: 'EXAM', dept: '功能检查科' },
  { itemCode: 'VIDEO_EEG', itemName: '视频脑电图', category: 'EXAM', dept: '功能检查科' },
  { itemCode: 'EMG', itemName: '肌电图', category: 'EXAM', dept: '功能检查科' },
  { itemCode: 'EVOKED_POTENTIAL', itemName: '诱发电位', category: 'EXAM', dept: '功能检查科' },
  { itemCode: 'CSF_ROUTINE', itemName: '脑脊液常规', category: 'LAB', dept: '检验科' },
  { itemCode: 'CSF_BIOCHEMISTRY', itemName: '脑脊液生化', category: 'LAB', dept: '检验科' },
  { itemCode: 'CSF_CYTOLOGY', itemName: '脑脊液细胞学', category: 'LAB', dept: '检验科' },
  { itemCode: 'CSF_CULTURE', itemName: '脑脊液培养', category: 'LAB', dept: '检验科' },
  { itemCode: 'CSF_OLIGOCLONAL_BANDS', itemName: '脑脊液寡克隆带', category: 'LAB', dept: '检验科' },
  { itemCode: 'AUTOIMMUNE_ENCEPHALITIS_ANTIBODY', itemName: '自身免疫性脑炎抗体', category: 'LAB', dept: '检验科' },
  { itemCode: 'DEMYELINATING_DISEASE_ANTIBODY', itemName: '脱髓鞘疾病相关抗体', category: 'LAB', dept: '检验科' }
]

const itemByCode = new Map(medicalItemOptions.map(item => [item.itemCode, item]))
const medicalItemCodeByName: Record<string, string> = medicalItemOptions.reduce<Record<string, string>>((acc, item) => {
  acc[item.itemName.replace(/\s+/g, '')] = item.itemCode
  return acc
}, {
  头颅CT平扫: 'CRANIAL_CT_PLAIN',
  头颅CT增强: 'CRANIAL_CT_ENHANCED',
  头颅MRI平扫: 'CRANIAL_MRI_PLAIN',
  脑血管DSA: 'CEREBRAL_DSA',
  脱髓鞘疾病抗体: 'DEMYELINATING_DISEASE_ANTIBODY'
})

const labItemCodes = new Set([
  'CSF_ROUTINE',
  'CSF_BIOCHEMISTRY',
  'CSF_CYTOLOGY',
  'CSF_CULTURE',
  'CSF_OLIGOCLONAL_BANDS',
  'AUTOIMMUNE_ENCEPHALITIS_ANTIBODY',
  'DEMYELINATING_DISEASE_ANTIBODY'
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

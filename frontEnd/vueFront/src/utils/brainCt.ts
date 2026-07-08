const brainCtCodeTokens = ['NEURO_CT', 'CRANIAL_CT', 'BRAIN_CT', 'HEAD_CT']
const brainCtNamePatterns = [/头颅\s*CT/i, /颅脑\s*CT/i, /脑部\s*CT/i, /头部\s*CT/i]

export function isBrainCtItem(itemCategory?: string, itemCode?: string, itemName?: string) {
  if (String(itemCategory || '').toUpperCase() !== 'EXAM') return false

  const code = String(itemCode || '').toUpperCase()
  const name = String(itemName || '')
  return brainCtCodeTokens.some((token) => code.includes(token))
    || brainCtNamePatterns.some((pattern) => pattern.test(name))
}

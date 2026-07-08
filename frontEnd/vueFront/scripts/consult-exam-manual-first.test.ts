import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = dirname(fileURLToPath(import.meta.url))
const detailSource = readFileSync(
  resolve(__dirname, '..', 'src', 'pages', 'doctor', 'consult', 'Detail.vue'),
  'utf8',
)

test('consult workflow uses doctor-owned labels instead of AI-owned labels', () => {
  assert.match(detailSource, />检查\/检验项<\/button>/)
  assert.match(detailSource, />开具处方<\/button>/)
  assert.equal(detailSource.includes('>AI检查/检验</button>'), false)
  assert.equal(detailSource.includes('>处方/用药AI</button>'), false)
})

test('exam step shows the manual order panel before the AI suggestion panel', () => {
  const manualPanelIndex = detailSource.indexOf('manual-exam-panel')
  const aiPanelIndex = detailSource.indexOf('ai-suggestion-panel')

  assert.notEqual(manualPanelIndex, -1)
  assert.notEqual(aiPanelIndex, -1)
  assert.ok(manualPanelIndex < aiPanelIndex)
})

test('AI exam generation merges recommendations into the editable order list', () => {
  assert.match(detailSource, /mergeExamRecommendationsToOrder\(normalized\.items\)/)
  assert.equal(detailSource.includes('examRecommendations.value = normalized.items'), false)
})

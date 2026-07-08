import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const dashboardSource = readFileSync(
  new URL('../src/pages/admin/ml/Dashboard.vue', import.meta.url),
  'utf8',
)

test('admin ml dashboard shows percent unit for success ratio card only', () => {
  assert.match(
    dashboardSource,
    /stats\.successRate \?\? '--' }}<span class="unit"> %<\/span>/,
  )
  assert.doesNotMatch(dashboardSource, /adoptionRate/)
})

test('admin ml dashboard shows model name before model key and hides artifact path', () => {
  const modelNameIndex = dashboardSource.indexOf('label="模型名称"')
  const modelKeyIndex = dashboardSource.indexOf('label="模型标识"')

  assert.notEqual(modelNameIndex, -1)
  assert.notEqual(modelKeyIndex, -1)
  assert.ok(modelNameIndex < modelKeyIndex)
  assert.doesNotMatch(dashboardSource, /label="模型路径"/)
  assert.doesNotMatch(dashboardSource, /prop="artifactPath"/)
})
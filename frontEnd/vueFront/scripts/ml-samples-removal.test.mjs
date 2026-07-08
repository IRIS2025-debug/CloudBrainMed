import test from 'node:test'
import assert from 'node:assert/strict'
import { existsSync, readFileSync } from 'node:fs'

const appSource = readFileSync(
  new URL('../src/App.vue', import.meta.url),
  'utf8',
)
const routerSource = readFileSync(
  new URL('../src/router/index.ts', import.meta.url),
  'utf8',
)
const adminHomeSource = readFileSync(
  new URL('../src/pages/admin/AdminHome.vue', import.meta.url),
  'utf8',
)
const mlApiSource = readFileSync(
  new URL('../src/api/admin/ml.ts', import.meta.url),
  'utf8',
)
const samplesPageUrl = new URL('../src/pages/admin/ml/Samples.vue', import.meta.url)

test('sample label feature is removed from admin navigation and routes', () => {
  for (const source of [appSource, routerSource, adminHomeSource]) {
    assert.doesNotMatch(source, /\/admin\/ml\/samples/)
    assert.doesNotMatch(source, /样本标注/)
  }
})

test('sample label frontend API and page are removed', () => {
  assert.doesNotMatch(mlApiSource, /getSampleList/)
  assert.doesNotMatch(mlApiSource, /labelSample/)
  assert.doesNotMatch(mlApiSource, /sample\/list|sample\/label|samples\/list|samples\/update/)
  assert.equal(existsSync(samplesPageUrl), false)
})

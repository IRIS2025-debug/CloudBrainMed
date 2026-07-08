import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const viteConfigSource = readFileSync(
  new URL('../vite.config.ts', import.meta.url),
  'utf8',
)
const routerSource = readFileSync(new URL('../src/router/index.ts', import.meta.url), 'utf8')
const appSource = readFileSync(new URL('../src/App.vue', import.meta.url), 'utf8')
const adminHomeSource = readFileSync(new URL('../src/pages/admin/AdminHome.vue', import.meta.url), 'utf8')
const mlApiSource = readFileSync(new URL('../src/api/admin/ml.ts', import.meta.url), 'utf8')

test('dev proxy sends admin ml requests to ai-service before generic admin-service proxy', () => {
  const mlProxyIndex = viteConfigSource.indexOf("'/admin-service/ml'")
  const adminProxyIndex = viteConfigSource.indexOf("'/admin-service'")

  assert.notEqual(mlProxyIndex, -1)
  assert.ok(mlProxyIndex < adminProxyIndex)

  const mlProxyBlock = viteConfigSource.slice(
    mlProxyIndex,
    viteConfigSource.indexOf('},', mlProxyIndex) + 2,
  )

  assert.match(mlProxyBlock, /target:\s*'http:\/\/localhost:8001'/)
})

test('admin model management page and entry points are removed', () => {
  assert.doesNotMatch(routerSource, /\/admin\/ml\/models/)
  assert.doesNotMatch(routerSource, /Models\.vue/)
  assert.doesNotMatch(appSource, /\/admin\/ml\/models/)
  assert.doesNotMatch(adminHomeSource, /\/admin\/ml\/models/)
  assert.doesNotMatch(appSource, /妯″瀷绠＄悊|模型管理/)
  assert.doesNotMatch(adminHomeSource, /妯″瀷绠＄悊|模型管理/)
})

test('admin ml frontend api does not expose removed model management calls', () => {
  assert.doesNotMatch(mlApiSource, /getModelList|triggerTraining|getTrainingTasks|setModelTraffic/)
  assert.doesNotMatch(mlApiSource, /\/model\/list|\/models\/list|\/model\/train|\/models\/train|\/model\/traffic|\/models\/tasks/)
})

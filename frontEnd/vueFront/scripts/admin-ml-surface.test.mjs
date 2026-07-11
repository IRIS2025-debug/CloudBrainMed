import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'
import { fileURLToPath } from 'node:url'

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

test('routes MLOps requests to ai-service before the general admin proxy', () => {
  const vite = read('vite.config.ts')
  const mlProxy = vite.indexOf("'/admin-service/ml'")
  const adminProxy = vite.indexOf("'/admin-service'")

  assert.ok(mlProxy >= 0)
  assert.ok(adminProxy >= 0)
  assert.ok(mlProxy < adminProxy)
  assert.match(vite.slice(mlProxy, adminProxy), /localhost:8001/)
})

test('keeps sample and model management pages alongside the dashboard', () => {
  const app = read('src/App.vue')
  const router = read('src/router/index.ts')

  assert.match(app, /\/admin\/ml\/dashboard/)
  assert.match(app, /\/admin\/ml\/samples/)
  assert.match(app, /\/admin\/ml\/models/)
  assert.match(router, /\/admin\/ml\/samples/)
  assert.match(router, /\/admin\/ml\/models/)
  assert.equal(fs.existsSync(path.join(root, 'src/pages/admin/ml/Samples.vue')), true)
  assert.equal(fs.existsSync(path.join(root, 'src/pages/admin/ml/Models.vue')), true)
})

test('keeps the model list but displays modelKey as the first model name column', () => {
  const dashboard = read('src/pages/admin/ml/Dashboard.vue')
  const modelKeyColumn = dashboard.indexOf('prop="modelKey"')
  const versionColumn = dashboard.indexOf('prop="version"')

  assert.ok(modelKeyColumn >= 0)
  assert.ok(modelKeyColumn < versionColumn)
  assert.match(dashboard.slice(modelKeyColumn, versionColumn), /label="模型名称"/)
  assert.doesNotMatch(dashboard, /prop="artifactPath"/)
  assert.doesNotMatch(dashboard, /ElMessage/)
})

test('loads the four dashboard metrics from their real sources without faking zeros', () => {
  const home = read('src/pages/admin/AdminHome.vue')
  const mlApi = read('src/api/admin/ml.ts')

  // 首页并行拉取三个数据源，且用 allSettled 隔离单点失败。
  assert.match(home, /Promise\.allSettled/)
  assert.match(home, /getDashboardOverview/)
  assert.match(home, /getModelStats/)
  assert.match(home, /getSampleList/)

  // ml.ts 保留完整的样本/模型管理页面所需接口。
  assert.match(mlApi, /getInferenceStats/)
  assert.match(mlApi, /getModelStats/)
  assert.match(mlApi, /getModelList/)
  assert.match(mlApi, /getSampleList/)
  assert.match(mlApi, /labelSample/)
  assert.match(mlApi, /triggerTraining/)
  assert.match(mlApi, /getTrainingTasks/)
  assert.match(mlApi, /setModelTraffic/)
})

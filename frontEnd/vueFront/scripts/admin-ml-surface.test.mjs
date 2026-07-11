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

test('removes sample and model management pages while keeping the dashboard', () => {
  const app = read('src/App.vue')
  const router = read('src/router/index.ts')
  const home = read('src/pages/admin/AdminHome.vue')

  assert.match(app, /\/admin\/ml\/dashboard/)
  assert.doesNotMatch(app, /\/admin\/ml\/(samples|models)/)
  assert.doesNotMatch(router, /\/admin\/ml\/(samples|models)/)
  assert.doesNotMatch(home, /\/admin\/ml\/(samples|models)/)
  assert.equal(fs.existsSync(path.join(root, 'src/pages/admin/ml/Samples.vue')), false)
  assert.equal(fs.existsSync(path.join(root, 'src/pages/admin/ml/Models.vue')), false)
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

test('loads the admin overview and removes page-only MLOps API calls', () => {
  const home = read('src/pages/admin/AdminHome.vue')
  const mlApi = read('src/api/admin/ml.ts')

  assert.match(home, /getDashboardOverview/)
  assert.match(mlApi, /getInferenceStats/)
  assert.match(mlApi, /getModelList/)
  assert.doesNotMatch(mlApi, /getSampleList|labelSample|triggerTraining|getTrainingTasks|setModelTraffic/)
})

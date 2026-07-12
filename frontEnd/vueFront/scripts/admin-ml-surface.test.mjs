import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'
import { fileURLToPath } from 'node:url'

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')
const exists = (relativePath) => fs.existsSync(path.join(root, relativePath))

test('routes MLOps requests to ai-service before the general admin proxy', () => {
  const vite = read('vite.config.ts')
  const mlProxy = vite.indexOf("'/admin-service/ml'")
  const adminProxy = vite.indexOf("'/admin-service'")

  assert.ok(mlProxy >= 0)
  assert.ok(adminProxy >= 0)
  assert.ok(mlProxy < adminProxy)
  assert.match(vite.slice(mlProxy, adminProxy), /localhost:8001/)
})

test('removes sample-labeling and model-management pages, routes and menu entries', () => {
  const app = read('src/App.vue')
  const router = read('src/router/index.ts')

  // 侧边栏与路由都不再包含样本标注 / 模型管理入口。
  assert.doesNotMatch(app, /\/admin\/ml\/samples/)
  assert.doesNotMatch(app, /\/admin\/ml\/models/)
  assert.doesNotMatch(router, /\/admin\/ml\/samples/)
  assert.doesNotMatch(router, /\/admin\/ml\/models/)
  // AI 推理看板保留。
  assert.match(app, /\/admin\/ml\/dashboard/)

  // 页面文件被删除。
  assert.equal(exists('src/pages/admin/ml/Samples.vue'), false)
  assert.equal(exists('src/pages/admin/ml/Models.vue'), false)
  // Dashboard 保留。
  assert.equal(exists('src/pages/admin/ml/Dashboard.vue'), true)
})

test('ml api keeps read-only + dashboard endpoints and drops write/train/traffic ones', () => {
  const mlApi = read('src/api/admin/ml.ts')

  assert.match(mlApi, /getInferenceStats/)
  assert.match(mlApi, /getModelStats/)
  assert.match(mlApi, /getInferenceLogs/)
  assert.match(mlApi, /getModelList/)

  // 样本标注 / 训练 / 流量切换 / 训练任务接口已从前端 API 删除。
  assert.doesNotMatch(mlApi, /getSampleList/)
  assert.doesNotMatch(mlApi, /labelSample/)
  assert.doesNotMatch(mlApi, /triggerTraining/)
  assert.doesNotMatch(mlApi, /getTrainingTasks/)
  assert.doesNotMatch(mlApi, /setModelTraffic/)
})

test('admin home drops the sample metric, sample call and the two deleted ML module entries', () => {
  const home = read('src/pages/admin/AdminHome.vue')

  // Zhang 自己写的行：删除样本查询导入/请求、训练样本指标、两个已删除页面的入口。
  assert.doesNotMatch(home, /getSampleList/)
  assert.doesNotMatch(home, /训练样本/)
  assert.doesNotMatch(home, /sampleTotal/)
  assert.doesNotMatch(home, /\/admin\/ml\/samples/)
  assert.doesNotMatch(home, /\/admin\/ml\/models/)

  // 队友维护的模板/样式与“系统初始化完成”动态保持不动。
  assert.match(home, /系统初始化完成/)
})

test('AI board top metrics drop 采纳率 and use 今日推理/成功率/平均耗时/部署模型数', () => {
  const dashboard = read('src/pages/admin/ml/Dashboard.vue')

  assert.match(dashboard, /今日推理总量/)
  assert.match(dashboard, /成功率/)
  assert.match(dashboard, /平均耗时/)
  assert.match(dashboard, /部署模型数/)
  // 采纳率没有稳定数据流，已移除。
  assert.doesNotMatch(dashboard, /采纳率/)
  assert.doesNotMatch(dashboard, /adoptionRate/)
})

test('AI board does not fake a zero deployed-model count on request failure', () => {
  const dashboard = read('src/pages/admin/ml/Dashboard.vue')

  // 失败置错误位，部署模型数显示 --、模型表显示重试，而不是空表/0。
  assert.match(dashboard, /modelsError/)
  assert.match(dashboard, /modelsError \? '--' : models\.length/)
  assert.match(dashboard, /模型列表加载失败/)
  assert.match(dashboard, /重试/)
})

test('AI board model table shows name/key, version, type, status and drops 流量占比', () => {
  const dashboard = read('src/pages/admin/ml/Dashboard.vue')

  assert.match(dashboard, /label="模型名称"/)
  assert.match(dashboard, /prop="version"/)
  assert.match(dashboard, /label="类型"/)
  assert.match(dashboard, /label="状态"/)

  // 两个独立模型不做互相分流，删除流量占比列。
  assert.doesNotMatch(dashboard, /流量占比/)
  assert.doesNotMatch(dashboard, /trafficPct/)
  assert.doesNotMatch(dashboard, /el-progress/)

  // 状态中英文映射 + 强制单行。
  assert.match(dashboard, /就绪/)
  assert.match(dashboard, /降级/)
  assert.match(dashboard, /离线/)
  assert.match(dashboard, /white-space:\s*nowrap/)
})

test('admin home uses current inference stats and the two deployed models', () => {
  const home = read('src/pages/admin/AdminHome.vue')

  assert.match(home, /getDashboardOverview/)
  assert.match(home, /getInferenceStats/)
  assert.match(home, /getModelList/)
  assert.match(home, /今日排班/)
  assert.match(home, /今日推理/)
  assert.match(home, /成功率/)
  assert.match(home, /部署模型数/)
  assert.doesNotMatch(home, /getModelStats/)
  assert.doesNotMatch(home, /activeModels/)
})

test('admin home appends a percent sign only to the success-rate metric', () => {
  const home = read('src/pages/admin/AdminHome.vue')

  assert.match(home, /key === 'successRate' \? `\$\{value\}%` : String\(value\)/)
})

test('admin home uses the same timestamped refresh action as doctor overviews', () => {
  const home = read('src/pages/admin/AdminHome.vue')

  assert.match(home, /更新于 \{\{ updatedAt \}\}/)
  assert.match(home, /:loading="loading"/)
  assert.match(home, /@click="loadOverview"/)
  assert.match(home, /<Refresh \/>/)
  assert.match(home, /updatedAt\.value = new Date\(\)\.toLocaleTimeString\('zh-CN'\)/)
  assert.doesNotMatch(home, /系统运行中/)
})

test('admin scheduling sidebar entry is named 排班管理', () => {
  const app = read('src/App.vue')

  assert.match(app, /path: '\/admin\/scheduling', title: '排班管理'/)
  assert.doesNotMatch(app, /path: '\/admin\/scheduling', title: '值班管理'/)
})

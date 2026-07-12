import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'
import { fileURLToPath } from 'node:url'

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')
const exists = (relativePath) => fs.existsSync(path.join(root, relativePath))

test('shared overview board component exists and is presentational only', () => {
  assert.equal(exists('src/components/OverviewBoard.vue'), true)
  const board = read('src/components/OverviewBoard.vue')

  // 纯展示：不调接口、不读身份。
  assert.doesNotMatch(board, /sessionStorage/)
  assert.doesNotMatch(board, /request\./)
  assert.doesNotMatch(board, /@\/api\//)
  // 覆盖加载、失败重试、空数据三种状态。
  assert.match(board, /加载中/)
  assert.match(board, /重试/)
  assert.match(board, /暂无记录/)
})

test('doctor home is role-adaptive by doctorType and reuses read-only interfaces', () => {
  const home = read('src/pages/HomeView.vue')

  assert.match(home, /OverviewBoard/)
  assert.match(home, /doctorType/)
  // 接诊医生走只读概览接口。
  assert.match(home, /getConsultOverview/)
  // 检查/检验医生复用现有工作台 + 队列接口，不新增队列接口。
  assert.match(home, /getWorkbench/)
  assert.match(home, /getQueue/)
  // 三类角色标题都在。
  assert.match(home, /接诊医生首页/)
  assert.match(home, /检查医生首页/)
  assert.match(home, /检验医生首页/)
  // 最近任务按 orderItemId 去重、最多 5 条。
  assert.match(home, /orderItemId/)
  assert.match(home, /slice\(0, 5\)/)
})

test('task overview excludes completed tasks from the urgent count and preserves null metrics', () => {
  const home = read('src/pages/HomeView.vue')

  // 加急/紧急排除 COMPLETED（工作台 SQL 不按状态过滤，历史已完成任务不该计入）。
  assert.match(home, /t\.status !== 'COMPLETED'/)
  // 契约字段缺失时保留 null 显示 --，不再用 Number(value) || 0 伪装成 0。
  assert.match(home, /numOrNull/)
  assert.doesNotMatch(home, /Number\(value\) \|\| 0/)
})

test('router unifies doctor home entry and removes deleted ML routes', () => {
  const router = read('src/router/index.ts')

  // /doctor/home 渲染角色自适应首页（HomeView.vue，路由块保持 HEAD 原样，未改 meta）。
  assert.match(router, /path: '\/doctor\/home'/)
  assert.match(router, /HomeView\.vue/)
  // 检查/检验医生首页重定向到 /doctor/home（这两条 redirect 属于 Zhang 的改动）。
  assert.match(router, /path: '\/examination-doctor\/home'[\s\S]*?redirect: '\/doctor\/home'/)
  assert.match(router, /path: '\/inspection-doctor\/home'[\s\S]*?redirect: '\/doctor\/home'/)
  // 删除的 ML 路由不存在。
  assert.doesNotMatch(router, /mlSamples/)
  assert.doesNotMatch(router, /mlModels/)
})

test('consult overview api targets the read-only overview endpoint', () => {
  const consultApi = read('src/api/doctor/consult.ts')

  assert.match(consultApi, /getConsultOverview/)
  assert.match(consultApi, /\/doctor-service\/consult\/overview/)
})

test('doctor home modules match the sidebar entries per role', () => {
  const home = read('src/pages/HomeView.vue')

  // 接诊医生：个人信息 / 接诊工作台 / 值班查询，且不含侧边栏没有的 AI 药品推荐入口。
  assert.match(home, /consultModules/)
  assert.doesNotMatch(home, /\/doctor\/ai-medicine/)

  // 检查医生模块与侧边栏一致。
  for (const p of [
    '/examination-doctor/workbench',
    '/examination-doctor/queue',
    '/examination-doctor/ct-inference',
    '/examination-doctor/report',
  ]) {
    assert.ok(home.includes(p), `exam module missing ${p}`)
  }

  // 检验医生模块与侧边栏一致。
  for (const p of [
    '/inspection-doctor/workbench',
    '/inspection-doctor/queue',
    '/inspection-doctor/report',
  ]) {
    assert.ok(home.includes(p), `lab module missing ${p}`)
  }
})

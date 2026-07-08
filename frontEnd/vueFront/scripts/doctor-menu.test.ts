import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { dirname, resolve } from 'node:path'

import { getDoctorMenuItems } from '../src/app/doctorMenu.ts'

const __dirname = dirname(fileURLToPath(import.meta.url))
const srcRoot = resolve(__dirname, '..', 'src')

test('examination doctor sidebar only keeps examination entries plus profile and schedule', () => {
  assert.deepEqual(
    getDoctorMenuItems(2).map((item) => item.path),
    [
      '/examination-doctor/home',
      '/examination-doctor/application',
      '/doctor/profile',
      '/doctor/schedule',
    ],
  )
})

test('examination doctor sidebar does not expose a separate task workbench entry', () => {
  const paths = getDoctorMenuItems(2).map((item) => item.path)

  assert.equal(paths.includes('/examination-doctor/workbench'), false)
})

test('examination doctor sidebar does not expose queue as a separate primary entry', () => {
  const paths = getDoctorMenuItems(2).map((item) => item.path)

  assert.equal(paths.includes('/examination-doctor/queue'), false)
})

test('examination doctor sidebar keeps CT inference and report generation inside task context', () => {
  const paths = getDoctorMenuItems(2).map((item) => item.path)

  assert.equal(paths.includes('/examination-doctor/ct-inference'), false)
  assert.equal(paths.includes('/examination-doctor/report'), false)
})

test('inspection doctor sidebar follows examination order without CV entries', () => {
  const items = getDoctorMenuItems(3)

  assert.deepEqual(
    items.map((item) => item.path),
    [
      '/inspection-doctor/home',
      '/inspection-doctor/order-list',
      '/doctor/profile',
      '/doctor/schedule',
    ],
  )

  assert.deepEqual(
    items.map((item) => item.title),
    [
      '检验工作台',
      '查看检验申请',
      '医生个人信息',
      '值班查询',
    ],
  )
})

test('inspection doctor sidebar does not expose task workbench or queue as primary entries', () => {
  const paths = getDoctorMenuItems(3).map((item) => item.path)

  assert.equal(paths.includes('/doctor/workbench'), false)
  assert.equal(paths.includes('/doctor/queue'), false)
})

test('examination doctor sidebar no longer exposes consult or doctor home entries', () => {
  const paths = getDoctorMenuItems(2).map((item) => item.path)

  assert.equal(paths.includes('/doctor/home'), false)
  assert.equal(paths.includes('/doctor/consult'), false)
})

test('consultation doctor home no longer links to standalone AI medicine page', () => {
  const homeSource = readFileSync(resolve(srcRoot, 'pages', 'HomeView.vue'), 'utf8')

  assert.equal(homeSource.includes('/doctor/ai-medicine'), false)
})

test('legacy doctor home no longer links to standalone AI medicine page', () => {
  const homeSource = readFileSync(resolve(srcRoot, 'pages', 'DoctorHome.vue'), 'utf8')

  assert.equal(homeSource.includes('/doctor/ai-medicine'), false)
})

test('standalone AI medicine route is no longer reachable from doctor routes', () => {
  const routerSource = readFileSync(resolve(srcRoot, 'router', 'index.ts'), 'utf8')

  assert.equal(routerSource.includes("path: '/doctor/ai-medicine'"), false)
})

test('shared task detail route allows examination and inspection doctors', () => {
  const routerSource = readFileSync(resolve(srcRoot, 'router', 'index.ts'), 'utf8')
  const routeStart = routerSource.indexOf("path: '/doctor/task/:id'")
  const routeEnd = routerSource.indexOf('},', routeStart)
  const routeBlock = routerSource.slice(routeStart, routeEnd)

  assert.notEqual(routeStart, -1)
  assert.match(routeBlock, /doctorTypes:\s*\[\s*2,\s*3\s*\]/)
  assert.equal(routeBlock.includes('doctorType: 3'), false)
})

test('router uses hash history so deep links do not hit Spring backend routes', () => {
  const routerSource = readFileSync(resolve(srcRoot, 'router', 'index.ts'), 'utf8')

  assert.match(routerSource, /createWebHashHistory/)
  assert.doesNotMatch(routerSource, /createWebHistory/)
})

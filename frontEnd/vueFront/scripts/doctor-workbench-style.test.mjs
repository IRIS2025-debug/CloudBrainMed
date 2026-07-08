import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const examinationSource = readFileSync(
  new URL('../src/pages/examination/ExaminationHome.vue', import.meta.url),
  'utf8',
)
const inspectionSource = readFileSync(
  new URL('../src/pages/inspection-doctor/InspectionHome.vue', import.meta.url),
  'utf8',
)

function cssBlock(source, selector) {
  const start = source.indexOf(selector)
  assert.notEqual(start, -1, `${selector} should exist`)

  const openBrace = source.indexOf('{', start)
  assert.notEqual(openBrace, -1, `${selector} should have a declaration block`)

  let depth = 0
  for (let index = openBrace; index < source.length; index += 1) {
    const char = source[index]
    if (char === '{') depth += 1
    if (char === '}') depth -= 1
    if (depth === 0) {
      return source.slice(openBrace + 1, index)
    }
  }

  throw new Error(`${selector} declaration block was not closed`)
}

test('inspection doctor home uses the same hero and quick-card surface as examination home', () => {
  assert.match(inspectionSource, /<div class="page">/)
  assert.match(inspectionSource, /<header class="hero lab-hero">/)
  assert.match(inspectionSource, /class="hero-copy"/)
  assert.match(inspectionSource, /class="quick-grid lab-quick-grid"/)
  assert.match(inspectionSource, /class="quick-card"/)
  assert.match(inspectionSource, /:class="\{ primary: module\.primary \}"/)
  assert.match(inspectionSource, /primary:\s*true/)
  assert.doesNotMatch(inspectionSource, /class="dashboard"/)
  assert.doesNotMatch(inspectionSource, /class="page-header"/)
})

test('examination hero title is compact and stays on one line on desktop', () => {
  const titleBlock = cssBlock(examinationSource, '.hero-copy h1')

  assert.match(titleBlock, /font-size:\s*28px/)
  assert.match(titleBlock, /line-height:\s*1\.24/)
  assert.match(titleBlock, /white-space:\s*nowrap/)
  assert.match(titleBlock, /max-width:\s*none/)
})

test('doctor technician home headlines stay concise', () => {
  const examinationTitle = examinationSource.match(/<h1>([^<]+)<\/h1>/)?.[1] || ''
  const inspectionTitle = inspectionSource.match(/<h1>([^<]+)<\/h1>/)?.[1] || ''

  assert.ok(examinationTitle.length > 0 && examinationTitle.length <= 18)
  assert.ok(inspectionTitle.length > 0 && inspectionTitle.length <= 18)
})

test('examination and inspection request pages share the same list layout shell', () => {
  const examinationListSource = readFileSync(
    new URL('../src/pages/examination/application/ApplicationList.vue', import.meta.url),
    'utf8',
  )
  const inspectionListSource = readFileSync(
    new URL('../src/pages/inspection-doctor/InspectionOrderList.vue', import.meta.url),
    'utf8',
  )

  for (const source of [examinationListSource, inspectionListSource]) {
    assert.match(source, /class="page-top"/)
    assert.match(source, /class="stats-row"/)
    assert.match(source, /class="panel filter-panel"/)
    assert.match(source, /class="panel table-panel"/)
    assert.match(source, /class="pagination-area"/)
  }
})

test('examination doctor home uses the same three primary shortcuts as inspection doctor home', () => {
  assert.match(examinationSource, /path:\s*'\/examination-doctor\/application'/)
  assert.match(examinationSource, /path:\s*'\/doctor\/profile'/)
  assert.match(examinationSource, /path:\s*'\/doctor\/schedule'/)
  assert.doesNotMatch(examinationSource, /\/examination-doctor\/queue/)
  assert.doesNotMatch(examinationSource, /\/examination-doctor\/workbench/)
})

test('examination doctor sidebar keeps CT inference and report generation out of primary navigation', () => {
  const menuSource = readFileSync(new URL('../src/app/doctorMenu.ts', import.meta.url), 'utf8')

  assert.doesNotMatch(menuSource, /\/examination-doctor\/ct-inference/)
  assert.doesNotMatch(menuSource, /\/examination-doctor\/report/)
  assert.match(menuSource, /\/examination-doctor\/application/)
  assert.match(menuSource, /\/doctor\/profile/)
  assert.match(menuSource, /\/doctor\/schedule/)
})

test('obsolete examination queue and workbench routes are removed', () => {
  const routerSource = readFileSync(new URL('../src/router/index.ts', import.meta.url), 'utf8')

  assert.doesNotMatch(routerSource, /path:\s*'\/examination-doctor\/queue'/)
  assert.doesNotMatch(routerSource, /path:\s*'\/examination-doctor\/workbench'/)
  assert.doesNotMatch(routerSource, /pages\/examination\/queue\/Index\.vue/)
  assert.doesNotMatch(routerSource, /pages\/examination\/workbench\/Index\.vue/)
})

test('shared task detail lets examination and inspection tasks submit reports', () => {
  const taskDetailSource = readFileSync(
    new URL('../src/pages/doctor/task-detail/Index.vue', import.meta.url),
    'utf8',
  )

  assert.doesNotMatch(taskDetailSource, /v-if="task\.itemCategory === 'LAB'"/)
  assert.match(taskDetailSource, /reportKind/)
  assert.match(taskDetailSource, /生成\{\{ reportKind \}\}报告/)
  assert.match(taskDetailSource, /submitTaskReport/)
  assert.match(taskDetailSource, /reportKind\.value/)
  assert.match(taskDetailSource, /openCtInference/)
  assert.match(taskDetailSource, /openAiReportAssistant/)
  assert.match(taskDetailSource, /taskContextQuery/)
  assert.match(taskDetailSource, /orderItemId/)
})

test('examination and inspection request lists expose the task processing flow', () => {
  const examinationListSource = readFileSync(
    new URL('../src/pages/examination/application/ApplicationList.vue', import.meta.url),
    'utf8',
  )
  const inspectionListSource = readFileSync(
    new URL('../src/pages/inspection-doctor/InspectionOrderList.vue', import.meta.url),
    'utf8',
  )

  for (const source of [examinationListSource, inspectionListSource]) {
    assert.match(source, /处理/)
    assert.match(source, /查看/)
    assert.match(source, /primary-action-button/)
    assert.match(source, /actionLabel/)
    assert.doesNotMatch(source, /@click\.stop="showDetail\(row\)">详情/)
    assert.match(source, /openTaskDetail/)
    assert.match(source, /getPrimaryOrderItemId/)
    assert.match(source, /\/doctor\/task\/\$\{orderItemId\}/)
  }
})

test('inspection doctor request page keeps readable Chinese copy', () => {
  const inspectionListSource = readFileSync(
    new URL('../src/pages/inspection-doctor/InspectionOrderList.vue', import.meta.url),
    'utf8',
  )

  assert.match(inspectionListSource, /检验医生 · 检验申请/)
  assert.match(inspectionListSource, /查看检验申请/)
  assert.match(inspectionListSource, /申请状态/)
  assert.match(inspectionListSource, /分配房间/)
  assert.doesNotMatch(inspectionListSource, /[鐢鎵妫楠璇鍖宸鍒鍙鏃涓].{0,8}[€?]/)
})

test('CT inference consumes task context from task detail query', () => {
  const ctSource = readFileSync(
    new URL('../src/pages/examination/CTInference.vue', import.meta.url),
    'utf8',
  )

  assert.match(ctSource, /useRoute/)
  assert.match(ctSource, /route\.query\.registerId/)
  assert.match(ctSource, /route\.query\.orderItemId/)
  assert.match(ctSource, /route\.query\.itemName/)
  assert.match(ctSource, /当前任务/)
})

test('AI report assistant submits report for the current task context', () => {
  const reportSource = readFileSync(
    new URL('../src/pages/examination/report/ReportGeneration.vue', import.meta.url),
    'utf8',
  )

  assert.match(reportSource, /useRoute/)
  assert.match(reportSource, /submitTaskReport/)
  assert.match(reportSource, /route\.query\.orderItemId/)
  assert.match(reportSource, /route\.query\.registerId/)
  assert.match(reportSource, /route\.query\.itemName/)
  assert.match(reportSource, /当前任务/)
  assert.match(reportSource, /await submitTaskReport/)
})

test('technician homes calculate dashboard stats from inspection order API data', () => {
  for (const source of [examinationSource, inspectionSource]) {
    assert.match(source, /getInspectionOrderList/)
    assert.match(source, /async function fetchDashboardData\(/)
    assert.match(source, /onMounted\(\(\) => \{[\s\S]{0,120}fetchDashboardData\(\)/)
    assert.match(source, /filter\(.*itemCategory ===/)
    assert.match(source, /status === 'IN_PROCESS'/)
    assert.match(source, /status === 'QUEUED'/)
    assert.match(source, /status === 'COMPLETED'/)
  }

  assert.doesNotMatch(examinationSource, /pendingCount = ref\(12\)/)
  assert.doesNotMatch(examinationSource, /totalExaminations:\s*156/)
  assert.doesNotMatch(examinationSource, /completedExaminations:\s*89/)
  assert.doesNotMatch(examinationSource, /aiReports:\s*45/)
  assert.doesNotMatch(inspectionSource, /value:\s*'--'/)
})

test('shared report workspace route is available to exam and lab doctors', () => {
  const routerSource = readFileSync(new URL('../src/router/index.ts', import.meta.url), 'utf8')
  const reportSource = readFileSync(
    new URL('../src/pages/examination/report/ReportGeneration.vue', import.meta.url),
    'utf8',
  )

  assert.match(routerSource, /path:\s*'\/examination-doctor\/report'/)
  assert.match(routerSource, /doctorTypes:\s*\[2,\s*3\]/)
  assert.doesNotMatch(routerSource, /path:\s*'\/examination-doctor\/report'[\s\S]{0,180}doctorType:\s*2/)
  assert.match(reportSource, /\/inspection-doctor\/order-list/)
  assert.match(reportSource, /\/examination-doctor\/application/)
})

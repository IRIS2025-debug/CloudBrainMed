import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = dirname(fileURLToPath(import.meta.url))
const srcRoot = resolve(__dirname, '..', 'src')

function readSource(...segments) {
  return readFileSync(resolve(srcRoot, ...segments), 'utf8')
}

test('examination application list exposes the same assignment step as lab orders', () => {
  const source = readSource('pages', 'examination', 'application', 'ApplicationList.vue')

  assert.match(source, /assignInspectionOrder/)
  assert.match(source, /function assignOrder/)
  assert.match(source, /row\.status === 'WAITING_ASSIGN'/)
  assert.match(source, /await assignOrder\(row(?:,\s*false)?\)/)
})

test('request lists derive task actions from item status when server action flags are missing', () => {
  const examinationSource = readSource('pages', 'examination', 'application', 'ApplicationList.vue')
  const inspectionSource = readSource('pages', 'inspection-doctor', 'InspectionOrderList.vue')

  for (const source of [examinationSource, inspectionSource]) {
    assert.match(source, /function canProcess\(/)
    assert.match(source, /function isCompleted\(/)
    assert.match(source, /function actionLabel\(/)
    assert.match(source, /row\.status === 'WAITING_ASSIGN'/)
    assert.match(source, /row\.status === 'COMPLETED'/)
    assert.match(source, /row\.status === 'COMPLETED'/)
    assert.match(source, /row\.status === 'CANCELLED'/)
    assert.match(source, /handlePrimaryAction/)
  }
})

test('request list process action starts queued tasks and opens the report form', () => {
  const examinationSource = readSource('pages', 'examination', 'application', 'ApplicationList.vue')
  const inspectionSource = readSource('pages', 'inspection-doctor', 'InspectionOrderList.vue')

  for (const source of [inspectionSource]) {
    assert.match(source, /import\s*\{\s*startTask\s*\}\s*from ['"]@\/api\/doctor\/task['"]/)
    assert.match(source, /await startTask\(orderItemId\)/)
    assert.match(source, /path:\s*'\/examination-doctor\/report'/)
    assert.match(source, /row\.status === 'QUEUED'/)
    assert.match(source, /row\.status === 'IN_PROCESS'/)
  }

  assert.match(examinationSource, /import\s*\{\s*isBrainCtItem\s*\}\s*from ['"]@\/utils\/brainCt['"]/)
  assert.match(examinationSource, /function openProcessingWorkspaceByRow\(/)
  assert.match(examinationSource, /isBrainCtItem\(row\.itemCategory,\s*row\.itemCode,\s*row\.itemName\)/)
  assert.match(examinationSource, /path:\s*'\/examination-doctor\/ct-inference'/)
  assert.match(examinationSource, /path:\s*'\/examination-doctor\/report'/)
})

test('request list process action is guarded by per-row loading state', () => {
  const examinationSource = readSource('pages', 'examination', 'application', 'ApplicationList.vue')
  const inspectionSource = readSource('pages', 'inspection-doctor', 'InspectionOrderList.vue')

  for (const source of [examinationSource, inspectionSource]) {
    assert.match(source, /const processingIds = ref<Set<string>>\(new Set\(\)\)/)
    assert.match(source, /function isProcessing\(/)
    assert.match(source, /:loading="isProcessing\(row\)"/)
    assert.match(source, /processingIds\.value\.has\(orderItemId\)/)
    assert.match(source, /next\.delete\(orderItemId\)/)
  }
})

test('task detail can auto-open report form from list process navigation', () => {
  const source = readSource('pages', 'doctor', 'task-detail', 'Index.vue')

  assert.match(source, /route\.query\.report === '1'/)
  assert.match(source, /function shouldAutoOpenReport\(/)
  assert.match(source, /async function openReportFromQuery\(/)
  assert.match(source, /await startTask\(orderItemId\)/)
  assert.match(source, /handleOpenReportDialog\(\)/)
})

test('task detail uses shared report workspace and restricts CT inference to brain CT items', () => {
  const source = readSource('pages', 'doctor', 'task-detail', 'Index.vue')

  assert.match(source, /import\s*\{\s*isBrainCtItem\s*\}\s*from ['"]@\/utils\/brainCt['"]/)
  assert.match(source, /const isBrainCtTask = computed\(/)
  assert.match(source, /v-if="isBrainCtTask"/)
  assert.match(source, /path:\s*'\/examination-doctor\/report'/)
  assert.match(source, /query:\s*taskContextQuery\(\)/)
  assert.doesNotMatch(source, /code\.includes\('CT'\) \|\| name\.includes\('ct'\)/)
  assert.doesNotMatch(source, /v-if="isExamTask"[\s\S]{0,120}CT模型推理/)
})

test('shared report workspace hides image analysis for non brain CT tasks', () => {
  const source = readSource('pages', 'examination', 'report', 'ReportGeneration.vue')

  assert.match(source, /import\s*\{\s*isBrainCtItem\s*\}\s*from ['"]@\/utils\/brainCt['"]/)
  assert.match(source, /const isBrainCtReport = computed\(/)
  assert.match(source, /v-if="isBrainCtReport"/)
  assert.match(source, /v-if="!isBrainCtReport"/)
  assert.doesNotMatch(source, /code\.includes\('CT'\) \|\| name\.includes\('ct'\)/)
  assert.match(source, /<div v-if="isBrainCtReport" class="partial-item">[\s\S]{0,240}<strong>影像分析结果<\/strong>/)
  assert.match(source, /影像分析结果/)
  assert.match(source, /const pageTitle = computed\(/)
  assert.match(source, /const reportSectionTitle = computed\(/)
})

test('partial accept keeps findings available while gating only CT analysis', () => {
  const source = readSource('pages', 'examination', 'report', 'ReportGeneration.vue')

  assert.match(source, /<div class="partial-item">[\s\S]{0,240}partialSelections\.findings/)
  assert.match(source, /<div v-if="isBrainCtReport" class="partial-item">[\s\S]{0,240}partialSelections\.analysis/)
})

test('CT report store preserves artifact and lesion inference results for one order item', () => {
  const storeSource = readSource('stores', 'examReport.ts')
  const ctSource = readSource('pages', 'examination', 'CTInference.vue')
  const reportSource = readSource('pages', 'examination', 'report', 'ReportGeneration.vue')

  assert.match(storeSource, /artifact\?: CtStructuredResult/)
  assert.match(storeSource, /lesion\?: CtStructuredResult/)
  assert.match(storeSource, /mergeCtResult/)
  assert.match(ctSource, /const results = reactive<Record<CtTaskType,\s*any \| null>>/)
  assert.match(ctSource, /examReportStore\.mergeCtResult/)
  assert.match(ctSource, /function saveResultsToReport\(/)
  assert.match(ctSource, /hasAnyModelResult/)
  assert.match(reportSource, /payload\.structured\.artifact/)
  assert.match(reportSource, /payload\.structured\.lesion/)
  assert.match(reportSource, /aiResultJson/)
  assert.match(reportSource, /restoreCtResultFromPublishedReport/)
})

test('request lists only expose task actions when an order item id is available', () => {
  const examinationSource = readSource('pages', 'examination', 'application', 'ApplicationList.vue')
  const inspectionSource = readSource('pages', 'inspection-doctor', 'InspectionOrderList.vue')

  for (const source of [examinationSource, inspectionSource]) {
    assert.match(source, /function getPrimaryOrderItemId\(/)
    assert.match(source, /row\.orderItemId/)
    assert.match(source, /row\['order_item_id'\]/)
    assert.match(source, /function hasTaskEntryId\(/)
    assert.match(source, /hasTaskEntryId\(row\)[\s\S]{0,220}row\.status === 'CANCELLED'/)
    assert.match(source, /hasTaskEntryId\(row\)[\s\S]{0,220}row\.status === 'COMPLETED'/)
  }
})

test('request list process action does not open tasks claimed by another doctor', () => {
  const examinationSource = readSource('pages', 'examination', 'application', 'ApplicationList.vue')
  const inspectionSource = readSource('pages', 'inspection-doctor', 'InspectionOrderList.vue')

  for (const source of [examinationSource, inspectionSource]) {
    assert.match(source, /row\.canStart === true/)
    assert.match(source, /row\.canWriteReport === true/)
    assert.match(source, /function isClaimedByOtherDoctor\(/)
    assert.match(source, /row\.status === 'IN_PROCESS'[\s\S]{0,180}row\.canWriteReport !== true/)
    assert.match(source, /isClaimedByOtherDoctor\(row\)[\s\S]{0,180}'处理中'/)
    assert.match(source, /:disabled="[^"]*isClaimedByOtherDoctor\(row\)/)
  }
})

test('inspection request operation column remains visible without horizontal scrolling', () => {
  const inspectionSource = readSource('pages', 'inspection-doctor', 'InspectionOrderList.vue')

  assert.match(inspectionSource, /<el-table-column label="操作"[^>]*fixed="right"/)
  assert.match(inspectionSource, /:fit="true"/)
  assert.match(inspectionSource, /class="application-table"/)
  assert.match(inspectionSource, /table-layout:\s*fixed/)
})

test('request list operation column replaces detail buttons with process or view', () => {
  const examinationSource = readSource('pages', 'examination', 'application', 'ApplicationList.vue')
  const inspectionSource = readSource('pages', 'inspection-doctor', 'InspectionOrderList.vue')

  for (const source of [examinationSource, inspectionSource]) {
    assert.match(source, /{{ actionLabel\(row\) }}/)
    assert.match(source, /处理/)
    assert.match(source, /查看/)
    assert.match(source, /@click\.stop="handlePrimaryAction\(row\)"/)
    assert.doesNotMatch(source, /@click\.stop="showDetail\(row\)">详情/)
  }
})

test('secondary task and report pages return to request lists instead of browser-history loops', () => {
  const taskDetailSource = readSource('pages', 'doctor', 'task-detail', 'Index.vue')
  const reportSource = readSource('pages', 'examination', 'report', 'ReportGeneration.vue')

  assert.match(taskDetailSource, /@click="goBackToRequestList"/)
  assert.match(taskDetailSource, /function requestListPath\(\)/)
  assert.match(taskDetailSource, /task\.value\?\.itemCategory === 'LAB'[\s\S]{0,140}\/inspection-doctor\/order-list/)
  assert.match(taskDetailSource, /task\.value\?\.itemCategory === 'EXAM'[\s\S]{0,140}\/examination-doctor\/application/)
  assert.doesNotMatch(taskDetailSource, /\$router\.back\(\)/)

  assert.match(reportSource, /function goBack\(\)[\s\S]{0,80}router\.push\(requestListPath\(\)\)/)
  assert.match(reportSource, /function requestListPath\(\)/)
  assert.match(reportSource, /taskItemCategory\.value === 'LAB'[\s\S]{0,140}\/inspection-doctor\/order-list/)
  assert.match(reportSource, /taskItemCategory\.value === 'EXAM'[\s\S]{0,140}\/examination-doctor\/application/)
  assert.doesNotMatch(reportSource, /router\.push\(`\/doctor\/task\/\$\{orderItemId\}`\)/)
})

test('shared report workspace uses role-specific report copy and compact status cards', () => {
  const reportSource = readSource('pages', 'examination', 'report', 'ReportGeneration.vue')

  assert.match(reportSource, /const pageTitle = computed\(/)
  assert.match(reportSource, /const reportSectionTitle = computed\(/)
  assert.match(reportSource, /\{\{ pageTitle \}\}/)
  assert.match(reportSource, /\{\{ reportSectionTitle \}\}/)
  assert.doesNotMatch(reportSource, /<h2 class="page-title">生成检查\/检验报告<\/h2>/)
  assert.match(reportSource, /AI分析状态/)
  assert.match(reportSource, /待分析/)
  assert.match(reportSource, /white-space:\s*nowrap/)
})

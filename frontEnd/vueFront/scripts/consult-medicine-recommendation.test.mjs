import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'
import { fileURLToPath } from 'node:url'

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const detail = read('src/pages/doctor/consult/Detail.vue')
const aiApi = read('src/api/doctor/ai.ts')

test('consult recommendation uses the database-constrained prescription draft flow', () => {
  assert.match(aiApi, /generatePrescriptionDraft/)
  assert.match(aiApi, /\/ai-service\/prescription\/draft/)
  assert.match(detail, /generatePrescriptionDraft/)
  assert.match(detail, /applyMedicineRecommendation/)
  assert.match(detail, /带入处方/)

  // 自由文本“咨询用药”继续走药品知识问答，不能承担当次处方推荐。
  assert.match(detail, /fetch\('\/ai-service\/medicine\/chat'/)
  const recommendationHandler = detail.slice(
    detail.indexOf('async function askMedicineRecommendation()'),
    detail.indexOf('async function sendMedicineQuestion()'),
  )
  assert.doesNotMatch(recommendationHandler, /sendMedicineQuery\(/)
})

test('recommendation handoff re-resolves the medicine id before filling prescription fields', () => {
  assert.match(detail, /medicineOptions\.value\.find\([\s\S]*?item\.medicineId/)
  assert.match(detail, /loadMedicineOptions\(true\)/)
  assert.match(detail, /该推荐药品已不在当前药品库中/)
  assert.match(detail, /medicineId:\s*source\.medicineId/)
  assert.match(detail, /showPrescriptionDialog\.value\s*=\s*true/)
})

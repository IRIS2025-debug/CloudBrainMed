import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { Buffer } from 'node:buffer'
import ts from 'typescript'

const source = readFileSync(new URL('../src/utils/examRecommendation.ts', import.meta.url), 'utf8')
const compiled = ts.transpileModule(source, {
  compilerOptions: {
    module: ts.ModuleKind.ES2022,
    target: ts.ScriptTarget.ES2022
  }
}).outputText

const moduleUrl = `data:text/javascript;base64,${Buffer.from(compiled).toString('base64')}`
const { normalizeExamRecommendResponse } = await import(moduleUrl)

const result = normalizeExamRecommendResponse({
  clinicalSummary: '反复腹痛，需排除神经系统相关风险。',
  urgencyLevel: 'URGENT',
  checkItems: [
    { itemName: '颅脑CT平扫', selected: true },
    { itemName: '血常规检查', selected: true },
    { itemName: '脑脊液常规', selected: false }
  ]
})

assert.equal(result.summary, '反复腹痛，需排除神经系统相关风险。')
assert.equal(result.items.length, 3)
assert.equal(result.items[0].itemCode, 'CRANIAL_CT_PLAIN')
assert.equal(result.items[0].category, 'EXAM')
assert.equal(result.items[0].urgencyLevel, 'URGENT')
assert.equal(result.items[0].selected, true)
assert.equal(result.items[1].itemName, '血常规检查')
assert.equal(result.items[1].itemCode, '')
assert.equal(result.items[1].needsMapping, true)
assert.equal(result.items[2].itemCode, 'CSF_ROUTINE')
assert.equal(result.items[2].category, 'LAB')
assert.equal(result.items[2].selected, false)

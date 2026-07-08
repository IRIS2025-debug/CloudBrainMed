import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const source = readFileSync(new URL('../vite.config.ts', import.meta.url), 'utf8')

test('element plus form related dependencies are split into smaller chunks', () => {
  assert.match(source, /return 'element-form-vendor'/)
  assert.match(source, /return 'element-input-vendor'/)
  assert.match(source, /return 'element-select-vendor'/)
  assert.match(source, /return 'element-picker-vendor'/)
  assert.match(source, /return 'element-choice-vendor'/)
  assert.match(source, /return 'element-upload-vendor'/)
})

test('chunk warning is fixed by splitting chunks rather than hiding the warning', () => {
  assert.doesNotMatch(source, /chunkSizeWarningLimit/)
})

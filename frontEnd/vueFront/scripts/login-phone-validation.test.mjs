import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const loginViewSource = readFileSync(
  new URL('../src/views/LoginView.vue', import.meta.url),
  'utf8',
)

test('login phone input keeps only digits and caps input at 11 characters', () => {
  assert.match(loginViewSource, /:formatter="formatPhoneInput"/)
  assert.match(loginViewSource, /:parser="formatPhoneInput"/)
  assert.match(loginViewSource, /function formatPhoneInput\(value: string\): string \{[\s\S]*?\.slice\(0, 11\)/)
  assert.match(loginViewSource, /replace\([^)]*\\D[^)]*, ''\)/)
})

test('login button is disabled until phone has exactly 11 digits', () => {
  assert.match(loginViewSource, /const isPhoneValid = computed\(\(\) => loginForm\.value\.phone\.length === 11\)/)
  assert.match(loginViewSource, /:disabled="!isPhoneValid"/)
})

test('login handler blocks non-11-digit phone numbers before sending request', () => {
  assert.match(loginViewSource, /if \(!isPhoneValid\.value\) \{[\s\S]*?ElMessage\.warning\('请输入 11 位手机号'\)[\s\S]*?return[\s\S]*?\}/)
  assert.match(loginViewSource, /phone: loginForm\.value\.phone/)
})

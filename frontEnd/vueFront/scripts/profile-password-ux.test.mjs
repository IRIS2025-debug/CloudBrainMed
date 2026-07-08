import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const doctorProfileSource = readFileSync(
  new URL('../src/pages/doctor/profile/Index.vue', import.meta.url),
  'utf8',
)
const adminProfileSource = readFileSync(
  new URL('../src/pages/admin/profile/AdminProfile.vue', import.meta.url),
  'utf8',
)

test('doctor editable profile fields keep saved values in the inputs', () => {
  assert.match(doctorProfileSource, /v-model="form\.goodAt"/)
  assert.match(doctorProfileSource, /v-model="form\.introduction"/)
  assert.match(doctorProfileSource, /v-model="form\.email"/)
  assert.match(doctorProfileSource, /goodAt:\s*form\.goodAt\.trim\(\)/)
  assert.match(doctorProfileSource, /introduction:\s*form\.introduction\.trim\(\)/)
  assert.match(doctorProfileSource, /email:\s*form\.email\.trim\(\)/)
  assert.doesNotMatch(doctorProfileSource, /class="field-current"/)
  assert.doesNotMatch(doctorProfileSource, /当前：/)
  assert.doesNotMatch(doctorProfileSource, /const profileSnapshot = reactive/)
  assert.doesNotMatch(doctorProfileSource, /v-model="editForm\.goodAt"/)
})

test('admin editable contact fields keep saved values in the inputs', () => {
  assert.match(adminProfileSource, /v-model="form\.phone"/)
  assert.match(adminProfileSource, /v-model="form\.email"/)
  assert.match(adminProfileSource, /phone:\s*form\.phone\.trim\(\)/)
  assert.match(adminProfileSource, /email:\s*form\.email\.trim\(\)/)
  assert.doesNotMatch(adminProfileSource, /class="field-current"/)
  assert.doesNotMatch(adminProfileSource, /当前：/)
  assert.doesNotMatch(adminProfileSource, /const profileSnapshot = reactive/)
  assert.doesNotMatch(adminProfileSource, /v-model="editForm\.phone"/)
})

test('doctor and admin password forms are shown in a dialog with confirmation validation', () => {
  for (const source of [doctorProfileSource, adminProfileSource]) {
    assert.match(source, /<el-dialog[\s\S]+v-model="passwordDialogVisible"/)
    assert.match(source, /:show-close="false"/)
    assert.match(source, /class="password-panel"/)
    assert.match(source, /v-model="pwdForm\.confirmPassword"/)
    assert.match(source, /confirmPassword:\s*\[\{\s*required:\s*true,\s*validator:\s*validateConfirmPassword/)
    assert.match(source, /validateConfirmPassword/)
    assert.match(source, /两次输入的新密码不一致/)
    assert.match(source, /passwordDialogVisible\.value = false/)
    assert.doesNotMatch(source, /<section class="card security-card">/)
  }
})

test('password button only appears in the page header and dialog keeps the rounded visual treatment', () => {
  for (const source of [doctorProfileSource, adminProfileSource]) {
    assert.match(source, /class="top-actions"[\s\S]+@click="openPasswordDialog"[\s\S]+保存资料/)
    assert.doesNotMatch(source, /class="email-edit-row"[\s\S]*@click="openPasswordDialog"[\s\S]*<\/div>/)
    assert.match(source, /class="password-dialog-head"/)
    assert.match(source, /class="password-dialog-tip"/)
    assert.match(source, /:global\(\.password-dialog \.el-dialog\)/)
    assert.match(source, /border-radius:\s*36px/)
    assert.match(source, /box-shadow:\s*0 28px 70px/)
    assert.match(source, /:global\(\.password-dialog \.el-dialog__header\)/)
    assert.match(source, /:global\(\.password-dialog \.el-dialog__footer\)/)
    assert.match(source, /:global\(\.password-dialog \.el-input__wrapper\)/)
    assert.match(source, /\.password-panel\s*{[\s\S]+border-radius:\s*28px/)
  }
})

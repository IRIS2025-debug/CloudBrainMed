import test from 'node:test'
import assert from 'node:assert/strict'

import { getConsultEntryState } from '../src/pages/doctor/consult/entryState.ts'
import { getConsultReportState } from '../src/pages/doctor/consult/reportState.ts'

test('completed historical consults stay viewable with a success-tone button', () => {
  assert.deepEqual(
    getConsultEntryState({
      consultStatus: 'COMPLETED',
      readOnly: true,
    }),
    {
      disabled: false,
      text: '查看',
      reason: '',
      reasonVisible: false,
      tone: 'success',
    },
  )
})

test('consults blocked by missing visit date use a muted disabled button', () => {
  assert.deepEqual(
    getConsultEntryState({
      consultStatus: 'IN_PROGRESS',
      entryAllowed: false,
      entryActionText: '接诊',
      entryBlockedReason: '非就诊当天不可接诊：挂号缺少就诊日期',
    }),
    {
      disabled: true,
      text: '接诊',
      reason: '非就诊当天不可接诊：挂号缺少就诊日期',
      reasonVisible: true,
      tone: 'muted',
    },
  )
})

test('unfinished historical consults are disabled even when older payloads only mark readOnly', () => {
  assert.deepEqual(
    getConsultEntryState({
      consultStatus: 'PENDING',
      readOnly: true,
    }),
    {
      disabled: true,
      text: '接诊',
      reason: '',
      reasonVisible: false,
      tone: 'muted',
    },
  )
})

test('blocked consults use the backend-computed reason directly with a muted button tone', () => {
  assert.deepEqual(
    getConsultEntryState({
      consultStatus: 'IN_PROGRESS',
      entryAllowed: false,
      entryActionText: '接诊',
      entryBlockedReason: '非就诊当天不可接诊：该挂号已过期',
      readOnly: false,
    }),
    {
      disabled: true,
      text: '接诊',
      reason: '非就诊当天不可接诊：该挂号已过期',
      reasonVisible: true,
      tone: 'muted',
    },
  )
})

test('report state is none when no medical order exists', () => {
  assert.deepEqual(
    getConsultReportState({
      medicalOrderCount: 0,
      reportCount: 0,
      hasMedicalOrder: false,
      hasReturnedReport: false,
    }),
    {
      kind: 'none',
      text: '未开单',
    },
  )
})

test('report state is pending when medical order exists but no published report exists', () => {
  assert.deepEqual(
    getConsultReportState({
      medicalOrderCount: 2,
      reportCount: 0,
      hasMedicalOrder: true,
      hasReturnedReport: false,
    }),
    {
      kind: 'pending',
      text: '待回传',
    },
  )
})

test('report state is returned when published reports exist', () => {
  assert.deepEqual(
    getConsultReportState({
      medicalOrderCount: 2,
      reportCount: 2,
      hasMedicalOrder: true,
      hasReturnedReport: true,
    }),
    {
      kind: 'returned',
      text: '已回传 2',
    },
  )
})

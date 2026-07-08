import test from 'node:test'
import assert from 'node:assert/strict'

import { isBrainCtItem } from '../src/utils/brainCt.ts'

test('brain CT helper only matches cranial or brain CT exam items', () => {
  assert.equal(isBrainCtItem('EXAM', 'NEURO_CT_001', '头颅CT平扫'), true)
  assert.equal(isBrainCtItem('EXAM', 'CRANIAL_CT_PLAIN', '颅脑CT增强'), true)
  assert.equal(isBrainCtItem('EXAM', 'HEAD_CT_001', '头部CT'), true)
  assert.equal(isBrainCtItem('EXAM', 'BRAIN_CT_001', '脑部CT'), true)

  assert.equal(isBrainCtItem('EXAM', 'CHEST_CT_001', '胸部CT'), false)
  assert.equal(isBrainCtItem('EXAM', 'NEURO_MRI_001', '脑部MRI平扫'), false)
  assert.equal(isBrainCtItem('LAB', 'NEURO_CT_001', '头颅CT平扫'), false)
})

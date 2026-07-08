import test from 'node:test'
import assert from 'node:assert/strict'

import {
  buildCtDatasetSummary,
  formatCtDatasetSize,
  isCtDatasetFileName,
} from '../src/pages/examination/ctDataset.ts'

test('ct dataset file names only accept nii and nii.gz', () => {
  assert.equal(isCtDatasetFileName('case_001.nii'), true)
  assert.equal(isCtDatasetFileName('case_001.nii.gz'), true)
  assert.equal(isCtDatasetFileName('case_001.png'), false)
})

test('ct dataset summary exposes file name, size, and optional shape', () => {
  assert.deepEqual(
    buildCtDatasetSummary(
      { name: 'lesion_case_050_ct.nii.gz', size: 15728640 },
      '120 x 512 x 512',
    ),
    {
      fileName: 'lesion_case_050_ct.nii.gz',
      fileSize: '15.0 MB',
      shapeText: '120 x 512 x 512',
    },
  )
})

test('ct dataset size formatter uses compact units', () => {
  assert.equal(formatCtDatasetSize(950), '950 B')
  assert.equal(formatCtDatasetSize(2048), '2.0 KB')
})

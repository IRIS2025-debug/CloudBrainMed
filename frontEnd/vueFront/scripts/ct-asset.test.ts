import test from 'node:test'
import assert from 'node:assert/strict'

import {
  buildDoctorCtAssetUrl,
  resolveBinaryFilename,
} from '../src/pages/examination/ctAsset.ts'

test('buildDoctorCtAssetUrl normalizes preview and result filenames without appending token', () => {
  assert.equal(
    buildDoctorCtAssetUrl(
      '',
      'ct-lesion',
      'preview',
      '/previews/scan_lesion_preview_z14.png',
      'doctor-token',
    ),
    '/doctor-service/exam/ct-lesion/preview/scan_lesion_preview_z14.png',
  )

  assert.equal(
    buildDoctorCtAssetUrl(
      'https://api.example.com',
      'ct-artifact',
      'result',
      'https://python.example.com/result/scan_mask.nii.gz',
      'doctor-token',
    ),
    'https://api.example.com/doctor-service/exam/ct-artifact/result/scan_mask.nii.gz',
  )
})

test('buildDoctorCtAssetUrl returns a plain endpoint url when token is missing', () => {
  assert.equal(
    buildDoctorCtAssetUrl('', 'ct-lesion', 'preview', 'scan_preview_z1.png'),
    '/doctor-service/exam/ct-lesion/preview/scan_preview_z1.png',
  )
})

test('resolveBinaryFilename extracts the final filename from local and remote paths', () => {
  assert.equal(
    resolveBinaryFilename('/doctor-service/exam/ct-lesion/result/scan_mask.nii.gz', 'fallback.bin'),
    'scan_mask.nii.gz',
  )
  assert.equal(
    resolveBinaryFilename('https://api.example.com/files/scan_preview_z9.png', 'fallback.bin'),
    'scan_preview_z9.png',
  )
  assert.equal(resolveBinaryFilename('', 'fallback.bin'), 'fallback.bin')
})

import test from 'node:test'
import assert from 'node:assert/strict'

import viteConfig from '../vite.config.ts'
import { resolveAssetUrl } from '../src/utils/assets.ts'

test('doctor avatars are proxied to doctor-service during local development', () => {
  const proxy = viteConfig.server?.proxy
  assert.ok(proxy)
  assert.equal(proxy['/files/avatar/doctor']?.target, 'http://localhost:8003')
  assert.equal(proxy['/files/avatar/admin']?.target, 'http://localhost:8000')
  assert.equal(proxy['/files/avatar/patient']?.target, 'http://localhost:8004')
})

test('relative asset paths resolve against the current app origin', () => {
  assert.equal(
    resolveAssetUrl('/files/avatar/doctor/demo.png', 'http://localhost:5173'),
    'http://localhost:5173/files/avatar/doctor/demo.png',
  )
  assert.equal(
    resolveAssetUrl('https://cdn.example.com/avatar.png', 'http://localhost:5173'),
    'https://cdn.example.com/avatar.png',
  )
  assert.equal(resolveAssetUrl('', 'http://localhost:5173'), '')
})

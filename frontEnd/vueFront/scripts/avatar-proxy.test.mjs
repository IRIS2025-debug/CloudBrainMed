import test from 'node:test'
import assert from 'node:assert/strict'

import viteConfig from '../vite.config.ts'

test('doctor avatars use the doctor-service proxy before the generic avatar proxy', () => {
  const proxy = viteConfig.server?.proxy

  assert.ok(proxy)
  assert.equal(proxy['/files/avatar/doctor']?.target, 'http://localhost:8003')
  assert.equal(proxy['/files/avatar']?.target, 'http://localhost:8000')

  const proxyPaths = Object.keys(proxy)
  assert.ok(
    proxyPaths.indexOf('/files/avatar/doctor') < proxyPaths.indexOf('/files/avatar'),
    'doctor avatar proxy must be registered before the generic avatar proxy',
  )
})

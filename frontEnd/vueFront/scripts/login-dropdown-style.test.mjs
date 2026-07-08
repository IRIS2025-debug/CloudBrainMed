import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const loginViewSource = readFileSync(
  new URL('../src/views/LoginView.vue', import.meta.url),
  'utf8',
)

test('login select dropdown keeps an opaque panel and does not force poppers transparent', () => {
  assert.match(
    loginViewSource,
    /popper-class="login-identity-dropdown"/,
  )

  assert.doesNotMatch(
    loginViewSource,
    /\.el-popper[\s\S]*?background:\s*transparent\s*!important/i,
  )

  assert.match(
    loginViewSource,
    /\.login-identity-dropdown\.el-select-dropdown\s*{[^}]*background:\s*(?:#(?:fff|ffffff)|white|var\(--el-bg-color-overlay\))/i,
  )

  assert.match(
    loginViewSource,
    /\.login-identity-dropdown\s+\.el-select-dropdown\s*{[^}]*border-radius:\s*14px\s*!important/i,
  )
})

test('login select dropdown rounds the internal option surface consistently', () => {
  assert.match(
    loginViewSource,
    /\.login-identity-dropdown\s+\.el-select-dropdown__list\s*{[^}]*padding:\s*0/i,
  )
  assert.match(
    loginViewSource,
    /\.login-identity-dropdown\s+\.el-select-dropdown__item:first-child\s*{[^}]*border-radius:\s*12px\s+12px\s+0\s+0/i,
  )
  assert.match(
    loginViewSource,
    /\.login-identity-dropdown\s+\.el-select-dropdown__item:last-child\s*{[^}]*border-radius:\s*0\s+0\s+12px\s+12px/i,
  )
})

test('login view removes explanatory blocks and old dark clinical palette', () => {
  assert.doesNotMatch(loginViewSource, /class="signal-line"/)
  assert.doesNotMatch(loginViewSource, /class="platform-metrics"/)
  assert.doesNotMatch(loginViewSource, /class="brand-panels"/)
  assert.doesNotMatch(loginViewSource, /class="brand-desc"/)
  assert.doesNotMatch(loginViewSource, /class="form-kicker"/)
  assert.doesNotMatch(loginViewSource, /class="tips"/)
  assert.doesNotMatch(loginViewSource, /#173d7a|#0f766e|#4f7cff|#22d3ee|#111827|#172554/i)
})

test('login brand panel uses a stronger coordinated blue palette', () => {
  const brandSideBlock = loginViewSource.match(/\.brand-side\s*{[^}]+}/i)?.[0] ?? ''

  assert.doesNotMatch(brandSideBlock, /#f1f8ff|#c8ddff|#d9eaff|#b7d3fb/i)
  assert.match(brandSideBlock, /#8fb5f0/i)
  assert.match(brandSideBlock, /#5f86d4/i)
})

test('login brand panel has concise lower insight cards and keeps the product name on one line', () => {
  assert.match(loginViewSource, /class="brand-chips"/)
  assert.doesNotMatch(loginViewSource, /class="brand-flow"/)
  assert.match(loginViewSource, /class="brand-insights"/)
  assert.doesNotMatch(loginViewSource, /一体化协同/)
  assert.doesNotMatch(loginViewSource, /结果直达/)
  assert.match(loginViewSource, /接诊/)
  assert.match(loginViewSource, /检查/)
  assert.match(loginViewSource, /管理/)
  assert.match(loginViewSource, /AI辅助诊疗/)
  assert.match(loginViewSource, /智能分析病历与检查结果/)
  assert.match(loginViewSource, /云端医疗协同/)
  assert.match(loginViewSource, /接诊、检查、报告在同一平台联动/)
  assert.match(
    loginViewSource,
    /\.brand-title\s*{[^}]*white-space:\s*nowrap/i,
  )
})

test('login brand logo is lighter and insight cards sit lower with flatter styling', () => {
  const brandLogoBlock = loginViewSource.match(/\.brand-logo\s*{[^}]+}/i)?.[0] ?? ''
  const brandInsightsBlock = loginViewSource.match(/\.brand-insights\s*{[^}]+}/i)?.[0] ?? ''
  const insightCardBlock = loginViewSource.match(/\.insight-card\s*{[^}]+}/i)?.[0] ?? ''
  const insightTitleBlock = loginViewSource.match(/\.insight-card h3\s*{[^}]+}/i)?.[0] ?? ''
  const insightTextBlock = loginViewSource.match(/\.insight-card p\s*{[^}]+}/i)?.[0] ?? ''

  assert.match(loginViewSource, /class="brand-logo-mark"/)
  assert.doesNotMatch(loginViewSource, /M12 3L4 7\.4L12 11\.8L20 7\.4L12 3Z/)
  assert.doesNotMatch(loginViewSource, /fill-opacity="0\.86"/)
  assert.doesNotMatch(brandLogoBlock, /background:\s*linear-gradient\(135deg,\s*#315fbb,\s*#3f87dc\)/i)
  assert.match(brandLogoBlock, /background:\s*rgba\(255,\s*255,\s*255,\s*0\.34\)/i)
  assert.match(brandLogoBlock, /color:\s*#2b6fbd/i)
  assert.match(brandInsightsBlock, /margin-top:\s*88px/i)
  assert.match(insightCardBlock, /border-radius:\s*12px/i)
  assert.match(insightTitleBlock, /font-weight:\s*500/i)
  assert.match(insightTextBlock, /font-weight:\s*400/i)
  assert.doesNotMatch(insightCardBlock, /box-shadow/i)
})

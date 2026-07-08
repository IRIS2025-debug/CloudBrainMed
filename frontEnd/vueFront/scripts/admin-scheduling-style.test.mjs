import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const schedulingSource = readFileSync(
  new URL('../src/pages/admin/scheduling/Scheduling.vue', import.meta.url),
  'utf8',
)
const appSource = readFileSync(
  new URL('../src/App.vue', import.meta.url),
  'utf8',
)
const adminHomeSource = readFileSync(
  new URL('../src/pages/admin/AdminHome.vue', import.meta.url),
  'utf8',
)

function cssBlock(selector) {
  const start = schedulingSource.indexOf(selector)
  assert.notEqual(start, -1, `${selector} should exist`)

  const openBrace = schedulingSource.indexOf('{', start)
  assert.notEqual(openBrace, -1, `${selector} should have a declaration block`)

  let depth = 0
  for (let index = openBrace; index < schedulingSource.length; index += 1) {
    const char = schedulingSource[index]
    if (char === '{') depth += 1
    if (char === '}') depth -= 1
    if (depth === 0) {
      return schedulingSource.slice(openBrace + 1, index)
    }
  }

  throw new Error(`${selector} declaration block was not closed`)
}

function cssBlockAfter(selector, afterSelector) {
  const after = schedulingSource.indexOf(afterSelector)
  assert.notEqual(after, -1, `${afterSelector} should exist`)

  const start = schedulingSource.indexOf(selector, after)
  assert.notEqual(start, -1, `${selector} should exist after ${afterSelector}`)

  const openBrace = schedulingSource.indexOf('{', start)
  assert.notEqual(openBrace, -1, `${selector} should have a declaration block`)

  let depth = 0
  for (let index = openBrace; index < schedulingSource.length; index += 1) {
    const char = schedulingSource[index]
    if (char === '{') depth += 1
    if (char === '}') depth -= 1
    if (depth === 0) {
      return schedulingSource.slice(openBrace + 1, index)
    }
  }

  throw new Error(`${selector} declaration block was not closed`)
}

test('schedule grid cells allow stacked schedule cards without overflowing the row', () => {
  const cellBlock = cssBlock('.doctor-schedule-cell')
  const scheduleBlock = cssBlock('.schedule-block')

  assert.match(cellBlock, /min-height:\s*112px/)
  assert.doesNotMatch(cellBlock, /height:\s*80px/)
  assert.match(cellBlock, /flex-direction:\s*column/)
  assert.match(cellBlock, /align-items:\s*stretch/)
  assert.match(cellBlock, /overflow:\s*hidden/)

  assert.match(scheduleBlock, /width:\s*100%/)
  assert.match(scheduleBlock, /box-sizing:\s*border-box/)
  assert.match(scheduleBlock, /overflow:\s*hidden/)
  assert.doesNotMatch(scheduleBlock, /transform:\s*scale/)
})

test('schedule dialogs use grouped form surfaces instead of a flat list of rows', () => {
  assert.match(schedulingSource, /class="schedule-form-panel"/)
  assert.match(schedulingSource, /class="schedule-form-grid"/)
  assert.match(schedulingSource, /class="schedule-dialog-footer"/)
  assert.match(schedulingSource, /\.schedule-form-panel\s*{/)
  assert.match(schedulingSource, /\.schedule-dialog-footer\s*{/)
})

test('ai schedule dialog keeps time slot and room inputs card-like and responsive', () => {
  const timeSlotBlock = cssBlock('.time-slot-item,')
  const roomBlock = cssBlock('.room-item {')

  assert.match(timeSlotBlock, /background:\s*#ffffff/)
  assert.match(timeSlotBlock, /box-shadow:\s*0 10px 24px/)
  assert.match(roomBlock, /background:\s*#ffffff/)
  assert.match(roomBlock, /box-shadow:\s*0 10px 24px/)
})

test('admin sidebar names the scheduling entry as schedule management', () => {
  assert.match(appSource, /path:\s*'\/admin\/scheduling',\s*title:\s*'排班管理'/)
  assert.doesNotMatch(appSource, /path:\s*'\/admin\/scheduling',\s*title:\s*'值班管理'/)
})

test('admin dashboard names the scheduling shortcut as schedule management', () => {
  assert.match(adminHomeSource, /path:\s*'\/admin\/scheduling',\s*title:\s*'排班管理'/)
  assert.doesNotMatch(adminHomeSource, /path:\s*'\/admin\/scheduling',\s*title:\s*'值班管理'/)
})

test('schedule hover actions are visible as separate compact action buttons', () => {
  const actionsBlock = cssBlockAfter('.schedule-actions', '.schedule-ai-tag')

  assert.match(actionsBlock, /gap:\s*6px/)
  assert.match(actionsBlock, /background:\s*rgba\(255,\s*255,\s*255,\s*0\.96\)/)
  assert.match(actionsBlock, /box-shadow:\s*0 8px 18px/)
  assert.match(actionsBlock, /border:\s*1px solid rgba\(148,\s*163,\s*184,\s*0\.28\)/)
  assert.match(actionsBlock, /backdrop-filter:\s*blur\(8px\)/)
})

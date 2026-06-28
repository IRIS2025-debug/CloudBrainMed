// src/utils/date.ts

/**
 * 获取指定日期所在周的周一
 */
export function getWeekStart(date: Date | string): string {
  const d = typeof date === 'string' ? new Date(date) : new Date(date)
  const day = d.getDay()
  const diff = d.getDate() - day + (day === 0 ? -6 : 1)
  const monday = new Date(d)
  monday.setDate(diff)
  return formatDate(monday)
}

/**
 * 获取本周一
 */
export function getThisWeekStart(): string {
  return getWeekStart(new Date())
}

/**
 * 格式化日期为 YYYY-MM-DD
 */
export function formatDate(date: Date): string {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

/**
 * 格式化日期显示 MM/DD
 */
export function formatDateShort(dateStr: string): string {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const month = d.getMonth() + 1
  const day = d.getDate()
  return `${month}/${day}`
}

/**
 * 格式化时间显示 HH:mm
 */
export function formatTime(timeStr: string): string {
  if (!timeStr) return ''
  return timeStr.substring(0, 5)
}

/**
 * 判断是否为今天
 */
export function isToday(dateStr: string): boolean {
  if (!dateStr) return false
  return dateStr === formatDate(new Date())
}

/**
 * 获取周几的中文名称
 */
export function getChineseDayName(dayOfWeek: number): string {
  const names: Record<number, string> = {
    1: '周一',
    2: '周二',
    3: '周三',
    4: '周四',
    5: '周五',
    6: '周六',
    7: '周日'
  }
  return names[dayOfWeek] || ''
}

/**
 * 获取一周的日期范围 (周一 ~ 周日)
 */
export function getWeekDays(weekStart: string): string[] {
  const days: string[] = []
  const start = new Date(weekStart)
  for (let i = 0; i < 7; i++) {
    const date = new Date(start)
    date.setDate(start.getDate() + i)
    days.push(formatDate(date))
  }
  return days
}
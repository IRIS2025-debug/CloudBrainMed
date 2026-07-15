import type { Result } from '../examination/examApi'
import request from '../request'

// // 后端返回统一外层Result结构
// export type Result<T> = {
//   code: number
//   msg: string
//   data: T
// }

export type AdminAnalysisRequest = {
  question: string
  startTime?: string | null
  endTime?: string | null
}

// 单条指标摘要
export type SummaryItem = {
  title: string
  value: string | number
}

// 图表统一类型
// @/api/admin/analyze
export interface ChartItem {
  type: 'line' | 'pie' | 'bar' | 'statistic'
  title: string
  series: Array<{
    name: string
    data: number[]
  }> | null
  data: Array<{
    name: string
    value: number
  }> | null
  value: number | null  // statistic 类型使用
  xaxis: string[] | null // 注意后端用的是 xaxis 不是 xAxis
}

// 后端data内部结构
export type AdminAnalysisResponse = {
  analysis: string
  summary: SummaryItem[]
  charts: ChartItem[]
}

// 接口返回外层是Result，内层data是AdminAnalysisResponse
export function getAdminAnalysis(data: AdminAnalysisRequest):Promise<Result<AdminAnalysisResponse>> {
  return request.post('/ai-service/ai/admin/analyze', data)
}
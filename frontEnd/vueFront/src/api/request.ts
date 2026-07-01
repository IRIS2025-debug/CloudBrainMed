// src/api/request.ts

import axios from 'axios'
import { ElMessage } from 'element-plus'

// 定义统一的响应格式类型
export interface ApiResponse<T = any> {
  code: number
  message?: string
  msg?: string
  data: T
}


const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  timeout: 15000,
})

request.interceptors.request.use((config) => {
  const token = sessionStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
    config.headers.token = token
  }
  return config
})

request.interceptors.response.use(
  (res) => {
    const data = res.data
    // 支持 code === 0 或 code === 200 两种成功状态
    if (data.code === 0 || data.code === 200) {
      return data
    }
    // 处理 401 未登录
    if (data.code === 401) {
      ElMessage.error('登录已过期，请重新登录')
      sessionStorage.removeItem('token')
      window.location.href = '/login'
      return Promise.reject(new Error('登录已过期'))
    }
    ElMessage.error(data.msg || data.message || '请求失败')
    return Promise.reject(new Error(data.msg || data.message))
  },
  (err) => {
    if (err.response) {
      const { status } = err.response
      if (status === 401) {
        ElMessage.error('登录已过期，请重新登录')
        sessionStorage.removeItem('token')
        window.location.href = '/login'
      } else if (status === 403) {
        ElMessage.error('没有权限访问')
      } else if (status >= 500) {
        ElMessage.error('服务器错误，请稍后重试')
      } else {
        ElMessage.error('网络异常，请稍后重试')
      }
    } else {
      ElMessage.error('网络异常，请检查网络连接')
    }
    return Promise.reject(err)
  },
)

export default request

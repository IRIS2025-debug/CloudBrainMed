// src/api/examination/examApi.ts
import request from '../request';

export interface ExamOrderQueryParams {
  status?: string
  urgency_level?: string
  patient_id?: string
  page: number
  page_size: number
}

export interface ApiResponse<T = any> {
  code: number;
  message: string;
  data: T;
}

export const examApi = {
  /**
   * 获取检查申请列表（只调用这一个接口）
   */
  getExamOrders(params: ExamOrderQueryParams): Promise<ApiResponse> {
    return request.get('/exam-service/exam-orders/list', { params });
  }
};
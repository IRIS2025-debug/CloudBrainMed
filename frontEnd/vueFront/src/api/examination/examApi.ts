// src/api/examination/examApi.ts
import request from '../request';

export interface ExamOrderQueryParams {
  status?: string
  urgencyLevel?: string
  patientId?: string
  page: number
  pageSize: number
}

export interface ApiResponse<T = any> {
  code: number;
  message: string;
  data: T;
}

export const examApi = {
  /**
   * 获取检查/检验申请列表。
   *
   * 后端当前统一使用 /inspection-doctor/orders 承载检查医生和检验医生的医技申请查询，
   * 前端按 doctorType 或 itemCategory 再做视角过滤。
   */
  getExamOrders(_params?: Partial<ExamOrderQueryParams>): Promise<ApiResponse> {
    return request.get('/inspection-doctor/orders');
  }
};

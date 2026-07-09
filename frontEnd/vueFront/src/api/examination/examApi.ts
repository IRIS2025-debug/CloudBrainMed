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

// ===== Result 响应格式（后端使用） =====
export interface Result<T = any> {
  code: number;
  msg: string;      // Result 用 msg，不是 message
  data: T;
}

// ===== 保存报告请求体 =====
export interface SaveReportRequest {
  orderItemId: string   // 检查项目ID（外键关联 medical_order_item）
  registerId: string    // 挂号ID（用于展示）
  reportTitle?: string
  artifact?: {
    findings?: string
    diagnosis?: string
    advice?: string
    riskLevel?: string
    rawResult?: any
  }
  lesion?: {
    findings?: string
    diagnosis?: string
    advice?: string
    riskLevel?: string
    rawResult?: any
  }
  comprehensive: {
    findings: string
    diagnosis: string
    advice?: string
  }
  images?: {
    artifact?: string
    lesion?: string
  }
  reportDoctor?: string
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
  },

  /**
   * 保存CT检查报告
   */
  saveReport(data: SaveReportRequest): Promise<Result> {
    return request.post('/doctor-service/exam-order/report/save', data);
  }
};



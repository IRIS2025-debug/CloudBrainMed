// common/Config.ets
export class APIConfig {
  // ===== 服务端基础URL =====
  // 修改这里的IP地址即可，所有页面都会生效
  static readonly BASE_URL: string = 'http://192.168.209.143';

  // ===== 各个服务的具体路径 =====
  static readonly ADMIN_SERVICE_URL: string = `${APIConfig.BASE_URL}:8000/admin-service`;
  static readonly AI_SERVICE_URL: string = `${APIConfig.BASE_URL}:8001/ai-service`;
  static readonly AUTH_SERVICE_URL: string = `${APIConfig.BASE_URL}:8002/auth-service`;
  static readonly DOCTOR_SERVICE_URL: string = `${APIConfig.BASE_URL}:8003/doctor-service`;
  static readonly PATIENT_SERVICE_URL: string = `${APIConfig.BASE_URL}:8004/patient-service`;
  static readonly PAYMENT_SERVICE_URL: string = `${APIConfig.BASE_URL}:8005/payment-service`;

}
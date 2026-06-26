export interface PatientInfo {
  patientId: string;
  name: string;
  phone: string;
  token?: string;
  gender: number;
  genderText: string;
  age: number;
  birthday?: string;
  loginTime?: number;
  expiresIn?: number;
}

export class PatientManager {
  private static readonly KEY = 'patientInfo';

  /**
   * 保存患者信息（仅存 AppStorage，内存级）
   * 注意：使用静态方法，不能使用 this，要用 PatientManager.xxx
   */
  static savePatientInfo(info: PatientInfo): void {
    // 使用 PatientManager.KEY 而不是 this.KEY
    AppStorage.setOrCreate<PatientInfo>(PatientManager.KEY, info);
    console.info('PatientManager: 已保存到 AppStorage');
  }

  /**
   * 获取患者信息
   */
  static getPatientInfo(): PatientInfo | null {
    // 使用 PatientManager.KEY 而不是 this.KEY
    const info = AppStorage.get<PatientInfo>(PatientManager.KEY);
    if (info) {
      return info;
    }
    console.info('PatientManager: AppStorage 中无数据');
    return null;
  }

  /**
   * 清除患者信息
   */
  static clearPatientInfo(): void {
    // 使用 PatientManager.KEY 而不是 this.KEY
    AppStorage.delete(PatientManager.KEY);
    console.info('PatientManager: 已清除');
  }

  /**
   * 检查 Token 是否有效
   */
  static isTokenValid(): boolean {
    const info = PatientManager.getPatientInfo();  // 使用 PatientManager. 而不是 this.
    if (!info?.token) {
      return false;
    }
    if (!info.loginTime || !info.expiresIn) {
      return true;
    }
    const now = Date.now();
    const elapsed = (now - info.loginTime) / 1000;
    return elapsed < info.expiresIn;
  }

  /**
   * 获取 Token 剩余有效时间（秒）
   */
  static getTokenRemainingTime(): number {
    const info = PatientManager.getPatientInfo();
    if (!info?.loginTime || !info?.expiresIn) {
      return -1;
    }
    const now = Date.now();
    const elapsed = (now - info.loginTime) / 1000;
    const remaining = info.expiresIn - elapsed;
    return remaining > 0 ? Math.floor(remaining) : -1;
  }

  // 便捷方法
  static getPatientName(): string {
    const info = PatientManager.getPatientInfo();
    return info?.name ?? '未知';
  }

  static getPatientId(): string {
    const info = PatientManager.getPatientInfo();
    return info?.patientId ?? '';
  }

  static getToken(): string {
    const info = PatientManager.getPatientInfo();
    return info?.token ?? '';
  }

  static getGenderText(): string {
    const info = PatientManager.getPatientInfo();
    const gender = info?.gender ?? 0;
    if (gender === 1) return '男';
    if (gender === 2) return '女';
    return '未知';
  }

  static getAge(): number {
    const info = PatientManager.getPatientInfo();
    return info?.age ?? 0;
  }

  static getPhone(): string {
    const info = PatientManager.getPatientInfo();
    return info?.phone ?? '';
  }
}
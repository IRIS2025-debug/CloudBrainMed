// PersonalCenter.ets
import http from '@ohos.net.http';
import promptAction from '@ohos.promptAction';
import router from '@ohos.router';
import picker from '@ohos.file.picker';
import fs from '@ohos.file.fs';
import { BusinessError } from '@ohos.base';
import preferences from '@ohos.data.preferences';
import { APIConfig } from '../common/APIConfig';
// 定义请求数据类型
interface UpdateProfileRequest {
  name: string;
  gender: string;
  address: string;
  birthday: string;
}

// 数据类型定义
interface PatientProfile {
  patientId: string;
  name: string;
  gender: string;
  age: number;
  phone: string;
  address: string;
  birthday: string;
  avatarUrl: string;
  idCard: string;
}

interface FunctionItem {
  title: string;
  icon: string;
  desc: string;
  action: () => void;
}

const TOKEN_KEY = 'patient_token';

@Component
export struct PersonalCenter {
  // 接收外部参数
  @Prop patientId: string = '';
  @Prop userName: string = '';
  @Prop userPhone: string = '';

  @State profileData: PatientProfile = {
    patientId: '',
    name: '',
    gender: '',
    age: 0,
    phone: '',
    address: '',
    birthday: '',
    avatarUrl: '',
    idCard: ''
  };
  @State isLoading: boolean = true;

  // 修改手机号相关状态
  @State showPhoneDialog: boolean = false;
  @State oldPhone: string = '';
  @State newPhone: string = '';
  @State smsCode: string = '';
  @State isSendingCode: boolean = false;
  @State countdown: number = 0;
  @State countdownTimer: number = -1;

  // 对话框控制
  @State showEditDialog: boolean = false;
  @State showPasswordDialog: boolean = false;
  @State showIdCardDialog: boolean = false;
  @State showViewDialog: boolean = false;
  // 编辑字段
  @State editName: string = '';
  @State editGender: string = '';
  @State editAddress: string = '';
  @State editBirthday: string = '';
  @State oldPassword: string = '';
  @State newPassword: string = '';
  @State verifyPassword: string = '';

  @State rawPhone: string = '';
  @State newIdCard: string = '';
  private functionList: FunctionItem[] = [];
  // 配置文件
  private baseUrl: string = APIConfig.PATIENT_SERVICE_URL;

  private Url: string = APIConfig.BASE_URL;
  // 性别映射常量：女=2，男=1
  private readonly GENDER_MAP: Record<string, string> = {
    '2': '女',
    '1': '男'
  };

  // 数字转文字
  private genderToText(gender: string): string {
    return this.GENDER_MAP[gender] || gender || '--';
  }

  // 文字转数字
  private genderToNumber(genderText: string): string {
    const map: Record<string, string> = {
      '女': '2',
      '男': '1'
    };
    return map[genderText] || genderText;
  }

  // 获取性别选择器索引（Select 组件使用）
  private getGenderIndex(): number {
    const genderText = this.genderToText(this.editGender);
    if (genderText === '女') return 0;  // 女在第一位，索引0
    if (genderText === '男') return 1;  // 男在第二位，索引1
    return 0; // 默认选中"女"
  }

  // 根据出生日期计算年龄
  private calculateAge(birthday: string): number {
    if (!birthday) return 0;
    try {
      const birthDate = new Date(birthday);
      const today = new Date();
      let age = today.getFullYear() - birthDate.getFullYear();
      const monthDiff = today.getMonth() - birthDate.getMonth();
      if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < birthDate.getDate())) {
        age--;
      }
      return age >= 0 ? age : 0;
    } catch (e) {
      console.error('计算年龄失败:', e);
      return 0;
    }
  }

  // 获取显示年龄
  private getDisplayAge(): string {
    if (this.profileData.age && this.profileData.age > 0) {
      return `${this.profileData.age}岁`;
    }
    if (this.profileData.birthday) {
      const age = this.calculateAge(this.profileData.birthday);
      return age > 0 ? `${age}岁` : '--';
    }
    return '--';
  }

  aboutToAppear() {
    if (this.patientId) {
      this.profileData.patientId = this.patientId;
    }
    if (this.userName) {
      this.profileData.name = this.userName;
    }
    if (this.userPhone) {
      this.profileData.phone = this.userPhone;
    }
    this.initFunctionList();
    this.loadProfileData();
  }

  // 初始化功能列表
  initFunctionList() {
    this.functionList = [
      {
        title: '编辑个人信息',
        icon: '✏️',
        desc: '修改姓名、性别、地址、出生日期等',
        action: () => {
          this.editName = this.profileData.name || '';
          this.editGender = this.profileData.gender || '2';  // 默认 '2'（女）
          this.editAddress = this.profileData.address || '';
          this.editBirthday = this.profileData.birthday || '';
          this.showEditDialog = true;
        }
      },
      {
        title: '修改手机号',
        icon: '📱',
        desc: '更换绑定的手机号码',
        action: () => {
          this.oldPhone = this.profileData.phone;
          this.newPhone = '';
          this.smsCode = '';
          this.showPhoneDialog = true;
        }
      },
      {
        title: '修改登录密码',
        icon: '🔒',
        desc: '修改账户登录密码',
        action: () => {
          this.oldPassword = '';
          this.newPassword = '';
          this.showPasswordDialog = true;
        }
      },
      {
        title: '挂号记录',
        icon: '📋',
        desc: '查看历史挂号记录',
        action: () => {
          router.pushUrl({
            url: 'pages/profile/RegisterHistoryPage',
            params: { patientId: this.patientId }
          }).catch((err: Error) => {
            console.error('跳转失败:', err);
            promptAction.showToast({ message: '页面跳转失败，请重试', duration: 1500 });
          });
        }
      },
      {
        title: '缴费记录',
        icon: '💰',
        desc: '查看缴费明细记录',
        action: () => {
          router.pushUrl({
            url: 'pages/profile/PaymentHistoryPage',
            params: { patientId: this.patientId }
          }).catch(() => {
            promptAction.showToast({ message: '页面开发中...', duration: 1500 });
          });
        }
      },
      {
        title: '身份验证',
        icon: '🛡️',
        desc: '验证身份后可修改身份证信息',
        action: () => {
          this.verifyPassword = '';
          this.showIdCardDialog = true;
        }
      }
    ];
  }

  // 获取存储的token - 从 Preferences 读取
  async getToken(): Promise<string> {
    try {
      const context = getContext(this);
      const prefs = await preferences.getPreferences(context, 'user_prefs');
      const token = await prefs.get('token', '') as string;
      console.info('从 Preferences 读取 Token:', token ? '已获取' : '为空');
      return token;
    } catch (e) {
      console.error('获取Token失败:', JSON.stringify(e));
      return '';
    }
  }

  // 加载数据方法，计算年龄
  async loadProfileData() {
    this.isLoading = true;
    try {
      const token = await this.getToken();

      // 第一步：获取基本信息（不包含敏感信息）
      const httpRequest1 = http.createHttp();
      const response1 = await httpRequest1.request(
        this.baseUrl + `/profile/info`,
        {
          method: http.RequestMethod.GET,
          header: {
            'Content-Type': 'application/json',
            'token': token
          }
        }
      );

      if (response1.responseCode === 200) {
        const result1: Record<string, Object> = JSON.parse(response1.result as string);
        const code1 = result1['code'] as number;
        if (code1 === 200) {
          const data = result1['data'] as PatientProfile;
          if (!data.age && data.birthday) {
            data.age = this.calculateAge(data.birthday);
          }
          this.profileData = data;
          this.oldPhone = data.phone || '';
        }
      }
      httpRequest1.destroy();

      // 第二步：调用 info-raw 获取原始敏感信息（手机号、身份证号等）
      const httpRequest2 = http.createHttp();
      const response2 = await httpRequest2.request(
        this.baseUrl + `/profile/info-raw`,
        {
          method: http.RequestMethod.GET,
          header: {
            'Content-Type': 'application/json',
            'token': token
          }
        }
      );

      if (response2.responseCode === 200) {
        const result2: Record<string, Object> = JSON.parse(response2.result as string);
        const code2 = result2['code'] as number;
        if (code2 === 200) {
          const data2 = result2['data'] as PatientProfile;
          // ✅ 更新手机号
          if (data2.phone) {
            this.oldPhone = data2.phone;
            this.profileData.phone = data2.phone;
            console.info('获取原始手机号成功:', data2.phone);
          }
          // ✅ 更新身份证号（从 info-raw 获取完整身份证号）
          if (data2.idCard) {
            this.profileData.idCard = data2.idCard;
            console.info('获取原始身份证号成功:', data2.idCard);
          }
        }
      }
      httpRequest2.destroy();

    } catch (error) {
      const err = error as BusinessError;
      console.error('加载数据失败:', err.message);
    } finally {
      this.isLoading = false;
    }
  }

  // 获取原始完整信息（用于修改手机号回填）
  async loadRawProfileData() {
    try {
      const token = await this.getToken();
      const httpRequest = http.createHttp();

      const response = await httpRequest.request(
        this.baseUrl + `/profile/info-raw`,
        {
          method: http.RequestMethod.GET,
          header: {
            'Content-Type': 'application/json',
            'token': token
          }
        }
      );

      if (response.responseCode === 200) {
        const result: Record<string, Object> = JSON.parse(response.result as string);
        const code = result['code'] as number;
        if (code === 200) {
          const data = result['data'] as PatientProfile;
          // 更新手机号为原始完整手机号（未脱敏）
          if (data.phone) {
            this.oldPhone = data.phone;
            this.rawPhone = data.phone;
            this.profileData.phone = data.phone;
          }
          // ✅ 更新身份证号为原始完整身份证号（未脱敏）
          if (data.idCard) {
            this.profileData.idCard = data.idCard;
          }
          console.info('获取原始信息成功 - 手机号:', this.oldPhone, '身份证号:', this.profileData.idCard);
        }
      }
      httpRequest.destroy();
    } catch (error) {
      console.error('获取原始信息失败:', error);
    }
  }

  async updateProfile() {
    try {
      const token = await this.getToken();
      const httpRequest = http.createHttp();

      let finalGender = '2';

      let genderStr = '';
      if (this.editGender !== undefined && this.editGender !== null) {
        genderStr = String(this.editGender).trim();
      }

      console.info('性别原始值:', this.editGender, '处理后:', genderStr);

      if (genderStr === '女' || genderStr === '2') {
        finalGender = '2';
      } else if (genderStr === '男' || genderStr === '1') {
        finalGender = '1';
      } else {
        // 未知值，使用默认值并记录警告
        console.warn('未知性别值，使用默认 "2":', genderStr);
        finalGender = '2';
      }

      const requestData: UpdateProfileRequest = {
        name: this.editName || '',
        gender: finalGender,
        address: this.editAddress || '',
        birthday: this.editBirthday || ''
      };

      console.info('提交数据:', JSON.stringify(requestData));

      const response = await httpRequest.request(
        this.baseUrl + `/profile/update`,
        {
          method: http.RequestMethod.PUT,
          header: {
            'Content-Type': 'application/json',
            'token': token
          },
          extraData: requestData
        }
      );

      if (response.responseCode === 200) {
        const result: Record<string, Object> = JSON.parse(response.result as string);
        const code = result['code'] as number;
        if (code === 200) {
          promptAction.showToast({ message: '更新成功' });
          this.showEditDialog = false;
          this.loadProfileData();
        } else {
          const message = result['msg'] as string;
          promptAction.showToast({ message: message || '更新失败' });
          console.error('业务错误:', message);
        }
      }
      httpRequest.destroy();
    } catch (error) {
      console.error('更新失败:', error);
      promptAction.showToast({ message: '更新失败' });
    }
  }

  // 上传头像
  async uploadAvatar() {
    try {
      const photoPicker = new picker.PhotoViewPicker();
      const photoResult = await photoPicker.select({
        MIMEType: picker.PhotoViewMIMETypes.IMAGE_TYPE,
        maxSelectNumber: 1
      });

      if (photoResult.photoUris.length === 0) {
        return;
      }

      const uri = photoResult.photoUris[0];
      const file = fs.openSync(uri, fs.OpenMode.READ_ONLY);
      const stat = fs.statSync(file.fd);
      const buffer = new ArrayBuffer(stat.size);
      fs.readSync(file.fd, buffer);
      fs.closeSync(file);

      const token = await this.getToken();
      const httpRequest = http.createHttp();

      const response = await httpRequest.request(
        this.baseUrl + `/profile/avatar-upload`,
        {
          method: http.RequestMethod.POST,
          header: {
            'Content-Type': 'multipart/form-data',
            'token': token
          },
          extraData: buffer
        }
      );

      if (response.responseCode === 200) {
        const result: Record<string, Object> = JSON.parse(response.result as string);
        const code = result['code'] as number;
        if (code === 200) {
          const data = result['data'] as Record<string, Object>;
          this.profileData.avatarUrl = data['avatarUrl'] as string;
          promptAction.showToast({ message: '头像上传成功' });
        }
      }
      httpRequest.destroy();
    } catch (error) {
      promptAction.showToast({ message: '上传头像失败' });
    }
  }

  // 发送验证码
  async sendSmsCode() {
    const phone = this.newPhone.trim();
    if (!phone) {
      promptAction.showToast({ message: '请输入新手机号' });
      return;
    }

    if (phone === this.oldPhone.trim()) {
      promptAction.showToast({ message: '新手机号与原手机号相同' });
      return;
    }

    if (this.isSendingCode || this.countdown > 0) {
      return;
    }

    this.isSendingCode = true;

    try {
      const token = await this.getToken();
      const httpRequest = http.createHttp();

      const response = await httpRequest.request(
        this.Url + `:8002/auth-service/patient/send-code`,
        {
          method: http.RequestMethod.POST,
          header: {
            'Content-Type': 'application/json',
            'token': token
          },
          extraData: {
            phone: phone,
            skipRegisterCheck: true
          },
          connectTimeout: 10000,
          readTimeout: 10000
        }
      );

      console.info('=== 发送验证码响应 ===');
      console.info('状态码:', response.responseCode);
      console.info('响应内容:', response.result);

      if (response.responseCode === 200) {
        const result = JSON.parse(response.result as string) as Record<string, Object>;
        const code = result['code'] as number;
        const message = result['msg'] as string;

        console.info('业务码:', code);
        console.info('业务消息:', message);

        if (code === 200) {
          // 发送成功
          promptAction.showToast({ message: '验证码已发送，请注意查收' });
          this.smsCode = '';
          this.startCountdown();
        } else {
          this.handleSendCodeError(code, message);
        }
      } else if (response.responseCode === 401) {
        this.handleTokenExpired();
      } else {
        // HTTP 错误
        promptAction.showToast({ message: `网络异常，请稍后重试 (${response.responseCode})` });
      }

      httpRequest.destroy();
    } catch (error) {
      console.error('发送验证码异常:', JSON.stringify(error));
      const err = error as BusinessError;
      if (err.code === 2300028 || err.message?.includes('Timeout')) {
        promptAction.showToast({ message: '网络连接超时，请检查网络后重试' });
      } else {
        promptAction.showToast({ message: '网络异常，请稍后重试' });
      }
    } finally {
      this.isSendingCode = false;
    }
  }

  // 完整的错误处理
  private handleSendCodeError(code: number, message: string) {
    console.info('=== 处理错误 ===');
    console.info('错误码:', code);
    console.info('错误信息:', message);

    // 如果 message 为空，使用默认提示
    if (!message) {
      promptAction.showToast({ message: '发送验证码失败，请重试' });
      return;
    }

    // ============ 根据后端返回的 message 精准处理 ============

    // 1. 手机号为空
    if (message.includes('手机号不能为空')) {
      promptAction.showToast({ message: '手机号不能为空' });
      this.newPhone = '';
      return;
    }

    // 2. 手机号格式不正确
    if (message.includes('手机号格式不正确')) {
      promptAction.showToast({ message: '手机号格式不正确，请输入11位有效手机号' });
      this.newPhone = '';
      return;
    }

    // 3. 手机号未注册
    if (message.includes('该手机号未注册')) {
      promptAction.showToast({ message: '该手机号未注册' });
      this.newPhone = '';
      return;
    }

    // 4. 手机号已被注册（改手机号场景）
    if (message.includes('该手机号已被注册')) {
      promptAction.showToast({ message: '该手机号已被注册，无法使用' });
      this.newPhone = '';
      return;
    }

    // 5. 发送过于频繁（429）
    if (code === 429 || message.includes('过于频繁') || message.includes('稍后再试')) {
      promptAction.showToast({ message: '发送过于频繁，请稍后再试' });
      this.startCountdown(60);
      return;
    }

    // 6. 其他错误 - 直接显示后端返回的信息
    promptAction.showToast({ message: message });
  }

  // 倒计时
  private startCountdown(seconds: number = 60) {
    if (this.countdownTimer !== -1) {
      clearInterval(this.countdownTimer);
    }
    this.countdown = seconds;
    this.countdownTimer = setInterval(() => {
      this.countdown--;
      if (this.countdown <= 0) {
        clearInterval(this.countdownTimer);
        this.countdownTimer = -1;
        this.countdown = 0;
      }
    }, 1000);
  }

  // 组件销毁时清理定时器
  aboutToDisappear() {
    if (this.countdownTimer !== -1) {
      clearInterval(this.countdownTimer);
      this.countdownTimer = -1;
    }
  }
  // 修改手机号
  async changePhone() {
    // 只做最基本的非空校验
    const oldPhone = this.oldPhone.trim();
    const newPhone = this.newPhone.trim();
    const smsCode = this.smsCode.trim();

    if (!oldPhone) {
      promptAction.showToast({ message: '请填写原手机号' });
      return;
    }

    if (!newPhone) {
      promptAction.showToast({ message: '请填写新手机号' });
      return;
    }

    if (!smsCode) {
      promptAction.showToast({ message: '请输入验证码' });
      return;
    }

    // 验证码格式校验（6位数字，这个后端通常也会校验，但前端可以提前提示）
    const codeRegex = /^\d{6}$/;
    if (!codeRegex.test(smsCode)) {
      promptAction.showToast({ message: '验证码格式不正确（6位数字）' });
      return;
    }

    try {
      const token = await this.getToken();
      const httpRequest = http.createHttp();

      const response = await httpRequest.request(
        this.baseUrl + `/profile/change-phone`,
        {
          method: http.RequestMethod.POST,
          header: {
            'Content-Type': 'application/json',
            'token': token
          },
          extraData: {
            oldPhone: oldPhone,
            newPhone: newPhone,
            smsCode: smsCode
          }
        }
      );

      if (response.responseCode === 200) {
        const result: Record<string, Object> = JSON.parse(response.result as string);
        const code = result['code'] as number;
        const message = result['msg'] as string;

        if (code === 200) {
          // 修改成功
          promptAction.showToast({ message: '手机号修改成功' });
          this.showPhoneDialog = false;
          this.smsCode = '';
          this.isSendingCode = false;
          // 重新加载数据
          this.loadProfileData();
        } else {
          // 后端返回的业务错误
          this.handlePhoneChangeError(code, message);
        }
      } else {
        promptAction.showToast({ message: '网络异常，请稍后重试' });
      }

      httpRequest.destroy();
    } catch (error) {
      console.error('修改手机号失败:', error);
      promptAction.showToast({ message: '修改失败，请重试' });
    }
  }

  // 处理修改手机号错误
  private handlePhoneChangeError(code: number, message: string) {
    // 根据错误信息进行特殊处理
    if (message && (message.includes('验证码') || message.includes('smsCode'))) {
      // 验证码错误，清空输入框让用户重新输入
      this.smsCode = '';
      promptAction.showToast({ message: message });
    } else if (message && (message.includes('token') || message.includes('未登录') || message.includes('401'))) {
      // Token过期
      promptAction.showToast({ message: '登录已过期，请重新登录' });
      setTimeout(() => {
        router.replaceUrl({ url: 'pages/login/LoginPage' });
      }, 1500);
    } else {
      // 其他错误直接显示后端返回的信息
      promptAction.showToast({ message: message || '修改失败，请重试' });
    }
  }

  // 修改密码
  async changePassword() {
    if (!this.oldPassword || !this.newPassword) {
      promptAction.showToast({ message: '请填写完整信息' });
      return;
    }

    try {
      const token = await this.getToken();
      const httpRequest = http.createHttp();

      const response = await httpRequest.request(
        this.baseUrl + `/profile/change-password`,
        {
          method: http.RequestMethod.POST,
          header: {
            'Content-Type': 'application/json',
            'token': token
          },
          extraData: {
            oldPassword: this.oldPassword,
            newPassword: this.newPassword
          }
        }
      );

      if (response.responseCode === 200) {
        const result: Record<string, Object> = JSON.parse(response.result as string);
        const code = result['code'] as number;
        if (code === 200) {
          promptAction.showToast({ message: '密码修改成功，请重新登录' });
          this.showPasswordDialog = false;
          AppStorage.delete(TOKEN_KEY);
          router.replaceUrl({ url: 'pages/login/LoginPage' });
        } else {
          const message = result['msg'] as string;
          promptAction.showToast({ message: message || '修改失败' });
        }
      }
      httpRequest.destroy();
    } catch (error) {
      promptAction.showToast({ message: '修改失败' });
    }
  }

  // 验证身份证
  async verifyIdCard() {
    // 1. 校验密码
    if (!this.verifyPassword || !this.verifyPassword.trim()) {
      promptAction.showToast({ message: '请输入登录密码' });
      return;
    }

    // 2. 校验身份证号
    const idCard = this.newIdCard.trim();
    if (!idCard) {
      promptAction.showToast({ message: '请输入身份证号' });
      return;
    }

    // 3. 身份证格式校验（18位，最后一位可以是X或x）
    const idCardRegex = /^\d{17}[\dXx]$/;
    if (!idCardRegex.test(idCard)) {
      promptAction.showToast({ message: '身份证号格式不正确，请输入18位有效身份证号' });
      return;
    }

    // 4. 检查是否与当前身份证相同（前端提前拦截）
    const currentIdCard = this.profileData.idCard || '';
    if (idCard === currentIdCard) {
      promptAction.showToast({ message: '新身份证号与当前身份证号相同，无需修改' });
      return;
    }

    try {
      const token = await this.getToken();
      const httpRequest = http.createHttp();

      const response = await httpRequest.request(
        this.baseUrl + `/profile/change-idcard`,
        {
          method: http.RequestMethod.POST,
          header: {
            'Content-Type': 'application/json',
            'token': token
          },
          extraData: {
            idCard: idCard,
            password: this.verifyPassword.trim()
          }
        }
      );

      console.info('修改身份证响应状态码:', response.responseCode);
      console.info('修改身份证响应内容:', response.result);

      //  HTTP 200（包括业务成功和业务失败）
      if (response.responseCode === 200) {
        const result: Record<string, Object> = JSON.parse(response.result as string);
        const code = result['code'] as number;
        const message = result['msg'] as string;

        if (code === 200) {
          // 修改成功
          promptAction.showToast({ message: '身份证修改成功' });
          this.showIdCardDialog = false;
          this.verifyPassword = '';
          this.newIdCard = '';
          this.loadProfileData();
        } else {
          // 业务错误（code: 400 或 500）
          this.handleIdCardError(code, message);
        }
      } else if (response.responseCode === 401) {
        this.handleTokenExpired();
      } else {
        // 其他 HTTP 错误
        promptAction.showToast({ message: `网络异常 (${response.responseCode})，请稍后重试` });
      }

      httpRequest.destroy();
    } catch (error) {
      console.error('修改身份证失败:', JSON.stringify(error));
      promptAction.showToast({ message: '修改失败，请重试' });
    }
  }

  //  修改：处理身份证修改错误
  private handleIdCardError(code: number, message: string) {
    console.info('=== 处理身份证修改错误 ===');
    console.info('错误码:', code);
    console.info('错误信息:', message);

    if (!message) {
      promptAction.showToast({ message: '修改失败，请重试' });
      return;
    }

    //  根据后端返回的具体错误信息处理
    // 注意：后端 BusinessException 的 message 会通过 'msg' 字段返回

    // 1. 密码错误
    if (message.includes('密码错误') || message.includes('密码不正确')) {
      promptAction.showToast({ message: '密码错误，请重新输入' });
      this.verifyPassword = '';  // 清空密码，让用户重新输入
      return;
    }

    // 2. 密码为空
    if (message.includes('密码不能为空')) {
      promptAction.showToast({ message: '密码不能为空' });
      return;
    }

    // 3. 身份证号格式不正确
    if (message.includes('身份证号格式不正确')) {
      promptAction.showToast({ message: '身份证号格式不正确，请输入18位有效身份证号' });
      this.newIdCard = '';  // 清空身份证号
      return;
    }

    // 4. 新旧身份证相同
    if (message.includes('新身份证号与当前身份证号相同')) {
      // promptAction.showToast({ message: message });
      //  不关闭弹窗，不清空输入，让用户看到已输入的内容
      promptAction.showToast({ message: '新身份证号与当前身份证号相同，无需修改' });
      return;
    }

    // 5. 身份证已被使用
    if (message.includes('已被使用')) {
      promptAction.showToast({ message: '该身份证号已被其他账号使用' });
      this.newIdCard = '';
      return;
    }

    // 6. 患者不存在
    if (message.includes('患者不存在')) {
      promptAction.showToast({ message: '用户信息异常，请重新登录' });
      setTimeout(() => {
        router.replaceUrl({ url: 'pages/login/LoginPage' });
      }, 1500);
      return;
    }

    // 7. Token相关错误
    if (message.includes('token') || message.includes('未登录')) {
      promptAction.showToast({ message: '登录已过期，请重新登录' });
      setTimeout(() => {
        router.replaceUrl({ url: 'pages/login/LoginPage' });
      }, 1500);
      return;
    }

    // 8. 其他错误直接显示后端返回的信息
    promptAction.showToast({ message: message });
  }

  // Token过期处理（复用已有的方法）
  private handleTokenExpired() {
    promptAction.showToast({ message: '登录已过期，请重新登录' });
    setTimeout(() => {
      router.replaceUrl({ url: 'pages/login/LoginPage' });
    }, 1500);
  }

  build() {
    Column() {
      if (this.isLoading) {
        Column() {
          LoadingProgress()
            .width(40)
            .height(40)
            .color('#007AFF')
          Text('加载中...')
            .fontSize(14)
            .fontColor('#999999')
            .margin({ top: 12 })
        }
        .width('100%')
        .layoutWeight(1)
        .justifyContent(FlexAlign.Center)
      } else {
        Scroll() {
          Column() {
            this.UserInfoCard()
            this.FunctionListSection()
          }
          .width('100%')
        }
        .layoutWeight(1)
        .scrollBar(BarState.Off)
        .backgroundColor('#F5F5F5')
      }

      if (this.showEditDialog) {
        this.EditProfileDialog()
      }
      if (this.showPhoneDialog) {
        this.ChangePhoneDialog()
      }
      if (this.showPasswordDialog) {
        this.ChangePasswordDialog()
      }
      if (this.showIdCardDialog) {
        this.VerifyIdCardDialog()
      }
      if (this.showViewDialog) {
        this.ViewProfileDialog()
      }
    }
    .width('100%')
    .height('100%')
    .backgroundColor('#F5F5F5')
  }

  // 用户信息卡片
  @Builder
  UserInfoCard() {
    Column() {
      Row() {
        if (this.profileData.avatarUrl) {
          Image(this.profileData.avatarUrl)
            .width(72)
            .height(72)
            .borderRadius(36)
            .objectFit(ImageFit.Cover)
            .onClick(() => {
              this.uploadAvatar();
            })
        } else {
          Image($r('app.media.default_avatar'))
            .width(72)
            .height(72)
            .borderRadius(36)
            .objectFit(ImageFit.Cover)
            .onClick(() => {
              this.uploadAvatar();
            })
        }

        Column() {
          Text(this.profileData.name || '未设置姓名')
            .fontSize(18)
            .fontWeight(FontWeight.Medium)
            .fontColor('#333333')

          Text(`${this.genderToText(this.profileData.gender)} · ${this.getDisplayAge()}`)
            .fontSize(13)
            .fontColor('#666666')
            .margin({ top: 4 })
        }
        .alignItems(HorizontalAlign.Start)
        .margin({ left: 16 })
        .layoutWeight(1)

        Row() {
          Image($r('app.media.arrow_right'))
            .width(20)
            .height(20)
            .fillColor('#CCCCCC')
        }
        .padding({ left: 8, right: 4 })
        .onClick(() => {
          this.showViewDialog = true;
        })
      }
      .width('100%')
      .padding(20)
      .onClick(() => {
        this.showViewDialog = true;
      })
    }
    .width('100%')
    .backgroundColor('#FFFFFF')
    .borderRadius(12)
    .margin({ top: 16, left: 16, right: 16 })
  }

  // 功能列表区域
  @Builder
  FunctionListSection() {
    Column() {
      ForEach(this.functionList, (item: FunctionItem, index: number) => {
        this.FunctionRow(item, index === this.functionList.length - 1)
      })
    }
    .width('100%')
    .backgroundColor('#FFFFFF')
    .borderRadius(12)
    .margin({ top: 16, left: 16, right: 16, bottom: 16 })
  }

  // 单个功能行
  @Builder
  FunctionRow(item: FunctionItem, isLast: boolean) {
    Row() {
      Text(item.icon)
        .fontSize(28)
        .width(44)
        .height(44)
        .borderRadius(12)
        .backgroundColor('#F5F7FA')
        .textAlign(TextAlign.Center)

      Column() {
        Text(item.title)
          .fontSize(15)
          .fontWeight(FontWeight.Medium)
          .fontColor('#1A1A1A')

        Text(item.desc)
          .fontSize(12)
          .fontColor('#999999')
          .margin({ top: 2 })
          .maxLines(1)
          .textOverflow({ overflow: TextOverflow.Ellipsis })
      }
      .alignItems(HorizontalAlign.Start)
      .margin({ left: 12 })
      .layoutWeight(1)

      Image($r('app.media.arrow_right'))
        .width(16)
        .height(16)
        .fillColor('#CCCCCC')
    }
    .width('100%')
    .height(72)
    .padding({ left: 16, right: 16 })
    .border({
      width: { bottom: isLast ? 0 : 0.5 },
      color: '#F0F0F0'
    })
    .onClick(() => {
      item.action();
    })
  }

  // ==================== 对话框 ====================

  @Builder
  EditProfileDialog() {
    Column() {
      Column() {
        Text('编辑个人信息')
          .fontSize(18)
          .fontWeight(FontWeight.Medium)
          .margin({ bottom: 20 })

        Row() {
          Text('姓名').fontSize(14).fontColor('#666').width(80)
          TextInput({ text: this.editName, placeholder: '请输入姓名' })
            .layoutWeight(1).height(40).fontSize(14)
            .backgroundColor('#F5F5F5').borderRadius(4)
            .onChange((value: string) => { this.editName = value; })
        }
        .width('100%').margin({ bottom: 12 })

        // 性别选择器：女=2，男=1
        Row() {
          Text('性别').fontSize(14).fontColor('#666').width(80)
          Select([
            { value: '女', icon: '' },
            { value: '男', icon: '' }
          ])
            .layoutWeight(1)
            .height(40)
            .backgroundColor('#F5F5F5')
            .borderRadius(4)
            .selected(this.getGenderIndex())
            .onSelect((index: number) => {
              //  女=2，男=1
              this.editGender = index === 0 ? '2' : '1';
            })
        }
        .width('100%').margin({ bottom: 12 })

        Row() {
          Text('地址').fontSize(14).fontColor('#666').width(80)
          TextInput({ text: this.editAddress, placeholder: '请输入地址' })
            .layoutWeight(1).height(40).fontSize(14)
            .backgroundColor('#F5F5F5').borderRadius(4)
            .onChange((value: string) => { this.editAddress = value; })
        }
        .width('100%').margin({ bottom: 12 })

        Row() {
          Text('出生日期').fontSize(14).fontColor('#666').width(80)
          TextInput({ text: this.editBirthday, placeholder: '请输入出生日期' })
            .layoutWeight(1).height(40).fontSize(14)
            .backgroundColor('#F5F5F5').borderRadius(4)
            .onChange((value: string) => { this.editBirthday = value; })
        }
        .width('100%').margin({ bottom: 12 })

        Row() {
          Button('取消')
            .layoutWeight(1).height(40).fontSize(15)
            .backgroundColor('#F5F5F5').fontColor('#666')
            .onClick(() => { this.showEditDialog = false; })

          Button('保存')
            .layoutWeight(1).height(40).fontSize(15)
            .backgroundColor('#007AFF').margin({ left: 12 })
            .onClick(() => { this.updateProfile(); })
        }
        .width('100%').margin({ top: 20 })
      }
      .width('80%').padding(20)
      .backgroundColor('#FFFFFF').borderRadius(12)
    }
    .width('100%').height('100%')
    .justifyContent(FlexAlign.Center)
    .backgroundColor('#00000080')
    .onClick(() => { this.showEditDialog = false; })
  }

  @Builder
  ChangePhoneDialog() {
    Column() {
      Column() {
        Text('修改手机号')
          .fontSize(18)
          .fontWeight(FontWeight.Medium)
          .margin({ bottom: 20 })
          .fontColor('#1A1A1A')

        // 原手机号 - 使用从 info-raw 获取的原始手机号
        Row() {
          Text('原手机号')
            .fontSize(14)
            .fontColor('#666666')
            .width(80)
          TextInput({
            text: this.oldPhone,  // 从 info-raw 获取的原始手机号
            placeholder: '请输入原手机号'
          })
            .layoutWeight(1)
            .height(40)
            .fontSize(14)
            .backgroundColor('#F5F5F5')
            .borderRadius(4)
            .maxLength(11)
            .type(InputType.Normal)
            .showPasswordIcon(false)
            .id('oldPhoneInput')
            .enabled(false)  // 原手机号不可编辑，只读显示
            .onChange((value: string) => {
              this.oldPhone = value;
            })
        }
        .width('100%')
        .margin({ bottom: 12 })

        // 新手机号（其他部分不变）
        Row() {
          Text('新手机号')
            .fontSize(14)
            .fontColor('#666666')
            .width(80)
          TextInput({
            text: this.newPhone,
            placeholder: '请输入新手机号'
          })
            .layoutWeight(1)
            .height(40)
            .fontSize(14)
            .backgroundColor('#F5F5F5')
            .borderRadius(4)
            .maxLength(11)
            .type(InputType.PhoneNumber)
            .onChange((value: string) => {
              this.newPhone = value;
            })
        }
        .width('100%')
        .margin({ bottom: 12 })

        // 验证码（带发送按钮）
        Row() {
          Text('验证码')
            .fontSize(14)
            .fontColor('#666666')
            .width(80)

          TextInput({
            text: this.smsCode,
            placeholder: '请输入6位验证码'
          })
            .layoutWeight(1)
            .height(40)
            .fontSize(14)
            .backgroundColor('#F5F5F5')
            .borderRadius(4)
            .maxLength(6)
            .type(InputType.Number)
            .onChange((value: string) => {
              this.smsCode = value;
            })

          // 支持倒计时显示
          Button(this.countdown > 0 ? `${this.countdown}s` : (this.isSendingCode ? '发送中...' : '获取验证码'))
            .height(40)
            .fontSize(13)
            .fontColor((this.countdown > 0 || this.isSendingCode) ? '#999999' : '#007AFF')
            .backgroundColor((this.countdown > 0 || this.isSendingCode) ? '#F0F0F0' : '#E8F4FF')
            .borderRadius(4)
            .margin({ left: 8 })
            .enabled(this.countdown === 0 && !this.isSendingCode)
            .onClick(() => {
              this.sendSmsCode();
            })
        }
        .width('100%')
        .margin({ bottom: 20 })

        // 操作按钮
        Row() {
          Button('取消')
            .layoutWeight(1)
            .height(40)
            .fontSize(15)
            .fontColor('#666666')
            .backgroundColor('#F5F5F5')
            .borderRadius(4)
            .onClick(() => {
              this.showPhoneDialog = false;
              this.smsCode = '';
              this.isSendingCode = false;
            })

          Button('确认修改')
            .layoutWeight(1)
            .height(40)
            .fontSize(15)
            .fontColor('#FFFFFF')
            .backgroundColor('#007AFF')
            .borderRadius(4)
            .margin({ left: 12 })
            .onClick(() => {
              this.changePhone();
            })
        }
        .width('100%')
      }
      .width('85%')
      .padding(24)
      .backgroundColor('#FFFFFF')
      .borderRadius(16)
    }
    .width('100%')
    .height('100%')
    .justifyContent(FlexAlign.Center)
    .alignItems(HorizontalAlign.Center)
    .backgroundColor('#00000080')
    .onClick((event: ClickEvent) => {
      if (event.target.id === 'dialogBg') {
        this.showPhoneDialog = false;
        this.smsCode = '';
        this.isSendingCode = false;
      }
    })
  }

  @Builder
  ChangePasswordDialog() {
    Column() {
      Column() {
        Text('修改登录密码')
          .fontSize(18).fontWeight(FontWeight.Medium).margin({ bottom: 20 })

        Row() {
          Text('原密码').fontSize(14).fontColor('#666').width(80)
          TextInput({ text: this.oldPassword, placeholder: '请输入原密码' })
            .layoutWeight(1).height(40).fontSize(14)
            .backgroundColor('#F5F5F5').borderRadius(4)
            .type(InputType.Password)
            .onChange((value: string) => { this.oldPassword = value; })
        }
        .width('100%').margin({ bottom: 12 })

        Row() {
          Text('新密码').fontSize(14).fontColor('#666').width(80)
          TextInput({ text: this.newPassword, placeholder: '请输入新密码' })
            .layoutWeight(1).height(40).fontSize(14)
            .backgroundColor('#F5F5F5').borderRadius(4)
            .type(InputType.Password)
            .onChange((value: string) => { this.newPassword = value; })
        }
        .width('100%').margin({ bottom: 12 })

        Row() {
          Button('取消')
            .layoutWeight(1).height(40).fontSize(15)
            .backgroundColor('#F5F5F5').fontColor('#666')
            .onClick(() => { this.showPasswordDialog = false; })

          Button('确认修改')
            .layoutWeight(1).height(40).fontSize(15)
            .backgroundColor('#007AFF').margin({ left: 12 })
            .onClick(() => { this.changePassword(); })
        }
        .width('100%').margin({ top: 20 })
      }
      .width('80%').padding(20)
      .backgroundColor('#FFFFFF').borderRadius(12)
    }
    .width('100%').height('100%')
    .justifyContent(FlexAlign.Center)
    .backgroundColor('#00000080')
    .onClick(() => { this.showPasswordDialog = false; })
  }

  @Builder
  VerifyIdCardDialog() {
    Column() {
      Column() {
        Text('修改身份证')
          .fontSize(18)
          .fontWeight(FontWeight.Medium)
          .margin({ bottom: 8 })

        Text('请输入新身份证号和登录密码以验证身份')
          .fontSize(13)
          .fontColor('#999')
          .margin({ bottom: 20 })
          .textAlign(TextAlign.Center)
          .maxLines(2)
        
        Row() {
          Text('身份证号')
            .fontSize(14)
            .fontColor('#666')
            .width(80)
          TextInput({
            text: this.newIdCard,
            placeholder: '请输入18位新身份证号'
          })
            .layoutWeight(1)
            .height(40)
            .fontSize(14)
            .backgroundColor('#F5F5F5')
            .borderRadius(4)
            .maxLength(18)
            .type(InputType.Normal)
            .onChange((value: string) => {
              this.newIdCard = value;
            })
        }
        .width('100%')
        .margin({ bottom: 12 })

        // 当前身份证号显示（只读）- 使用脱敏显示
        Row() {
          Text('当前身份证')
            .fontSize(14)
            .fontColor('#666')
            .width(80)
          Text(this.maskIdCard(this.profileData.idCard || '未设置'))
            .fontSize(14)
            .fontColor('#999')
            .layoutWeight(1)
            .textAlign(TextAlign.End)
        }
        .width('100%')
        .margin({ bottom: 16 })

        // 密码输入
        Row() {
          Text('登录密码')
            .fontSize(14)
            .fontColor('#666')
            .width(80)
          TextInput({
            text: this.verifyPassword,
            placeholder: '请输入登录密码'
          })
            .layoutWeight(1)
            .height(40)
            .fontSize(14)
            .backgroundColor('#F5F5F5')
            .borderRadius(4)
            .type(InputType.Password)
            .onChange((value: string) => {
              this.verifyPassword = value;
            })
        }
        .width('100%')
        .margin({ bottom: 12 })

        Row() {
          Button('取消')
            .layoutWeight(1)
            .height(40)
            .fontSize(15)
            .backgroundColor('#F5F5F5')
            .fontColor('#666')
            .onClick(() => {
              this.showIdCardDialog = false;
              this.verifyPassword = '';
              this.newIdCard = '';
            })

          Button('确认修改')
            .layoutWeight(1)
            .height(40)
            .fontSize(15)
            .backgroundColor('#007AFF')
            .margin({ left: 12 })
            .onClick(() => {
              this.verifyIdCard();
            })
        }
        .width('100%')
        .margin({ top: 20 })
      }
      .width('85%')
      .padding(24)
      .backgroundColor('#FFFFFF')
      .borderRadius(16)
    }
    .width('100%')
    .height('100%')
    .justifyContent(FlexAlign.Center)
    .backgroundColor('#00000080')
    .onClick((event: ClickEvent) => {

      if (event.target.id === 'dialogBg') {
        this.showIdCardDialog = false;
        this.verifyPassword = '';
        this.newIdCard = '';
      }
    })
  }

  // ==================== 查看个人信息对话框 ====================
  @Builder
  ViewProfileDialog() {
    Column() {
      Column() {
        Row() {
          Text('个人信息')
            .fontSize(18)
            .fontWeight(FontWeight.Medium)
            .fontColor('#1A1A1A')

          Blank()

          Image($r('app.media.back'))
            .width(24)
            .height(24)
            .fillColor('#999999')
            .onClick(() => {
              this.showViewDialog = false;
            })
        }
        .width('100%')
        .margin({ bottom: 20 })

        Column() {
          if (this.profileData.avatarUrl) {
            Image(this.profileData.avatarUrl)
              .width(80)
              .height(80)
              .borderRadius(40)
              .objectFit(ImageFit.Cover)
          } else {
            Image($r('app.media.default_avatar'))
              .width(80)
              .height(80)
              .borderRadius(40)
              .objectFit(ImageFit.Cover)
          }
        }
        .width('100%')
        .alignItems(HorizontalAlign.Center)
        .margin({ bottom: 16 })

        Column() {
          this.InfoRow('患者ID', this.profileData.patientId || '--')
          this.DividerLine()
          this.InfoRow('姓名', this.profileData.name || '--')
          this.DividerLine()
          this.InfoRow('性别', this.genderToText(this.profileData.gender))
          this.DividerLine()
          this.InfoRow('年龄', this.getDisplayAge())
          this.DividerLine()
          this.InfoRow('手机号', this.profileData.phone || '--')
          this.DividerLine()
          this.InfoRow('出生日期', this.profileData.birthday || '--')
          this.DividerLine()
          this.InfoRow('身份证号', this.maskIdCard(this.profileData.idCard || '--'))
          this.DividerLine()
          this.InfoRow('地址', this.profileData.address || '--')
        }
        .width('100%')

        Button('编辑个人信息')
          .width('100%')
          .height(44)
          .fontSize(16)
          .fontWeight(FontWeight.Medium)
          .backgroundColor('#007AFF')
          .borderRadius(8)
          .margin({ top: 20 })
          .onClick(() => {
            this.showViewDialog = false;
            this.editName = this.profileData.name || '';
            this.editGender = this.profileData.gender || '2';  // 默认 '2'（女）
            this.editAddress = this.profileData.address || '';
            this.editBirthday = this.profileData.birthday || '';
            setTimeout(() => {
              this.showEditDialog = true;
            }, 100);
          })
      }
      .width('85%')
      .padding(24)
      .backgroundColor('#FFFFFF')
      .borderRadius(16)
      .shadow({
        radius: 20,
        color: '#00000030'
      })
    }
    .width('100%')
    .height('100%')
    .justifyContent(FlexAlign.Center)
    .alignItems(HorizontalAlign.Center)
    .backgroundColor('#00000080')
    .onClick((event: ClickEvent) => {
      if (event.target.id === 'dialogBg') {
        this.showViewDialog = false;
      }
    })
  }

  // 信息行组件
  @Builder
  InfoRow(label: string, value: string) {
    Row() {
      Text(label)
        .fontSize(14)
        .fontColor('#666666')
        .width(80)

      Text(value)
        .fontSize(14)
        .fontColor('#1A1A1A')
        .layoutWeight(1)
        .textAlign(TextAlign.End)
    }
    .width('100%')
    .padding({ top: 8, bottom: 8 })
  }

  // 分割线
  @Builder
  DividerLine() {
    Divider()
      .color('#F0F0F0')
      .height(0.5)
      .width('100%')
  }

  // 身份证号脱敏工具方法
  private maskIdCard(idCard: string): string {
    if (!idCard || idCard.length < 10) {
      return idCard || '--';
    }
    return idCard.substring(0, 6) + '****' + idCard.substring(idCard.length - 4);
  }
}
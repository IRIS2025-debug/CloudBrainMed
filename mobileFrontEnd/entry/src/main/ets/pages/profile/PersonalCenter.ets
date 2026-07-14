// pages/profile/PersonalCenter.ets
import { promptAction, router } from '@kit.ArkUI';
import { picker } from '@kit.CoreFileKit';
import { fileIo } from '@kit.CoreFileKit';
import { util } from '@kit.ArkTS';
import { http } from '@kit.NetworkKit';
import { BusinessError } from '@kit.BasicServicesKit';
import { APIConfig } from '../common/APIConfig';
import { PatientManager, PatientInfo } from '../common/PatientManager';
import { HttpUtil } from '../common/HttpUtil';

// ==================== 类型定义 ====================

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

interface ApiResult {
  code: number;
  msg: string;
  data?: Object;
}

// ==================== 请求体接口定义 ====================

interface SendCodeRequest {
  phone: string;
  skipRegisterCheck: boolean;
}

interface ChangePhoneRequest {
  oldPhone: string;
  newPhone: string;
  smsCode: string;
}

interface ChangePasswordRequest {
  oldPassword: string;
  newPassword: string;
}

interface UpdateAddressRequest {
  address: string;
}

interface UploadAvatarHeaders {
  'Content-Type': string;
  'Accept': string;
  'token': string;
}

// ==================== 性别映射 ====================
const GENDER_MAP: Record<string, string> = {
  '2': '女',
  '1': '男'
};

// ==================== 医疗主题色 ====================
interface GeneratedObjectLiteralInterface_1 {
  primary: string;
  primaryLight: string;
  primaryDark: string;
  gradientStart: string;
  gradientEnd: string;
  bgGrey: string;
  cardWhite: string;
  textPrimary: string;
  textSecondary: string;
  textHint: string;
  divider: string;
  danger: string;
  white: string;
  overlay: string;
  shadowCard: string;
  shadowButton: string;
}

const COLORS: GeneratedObjectLiteralInterface_1 = {
  primary: '#1E88E5',
  primaryLight: '#E3F2FD',
  primaryDark: '#1565C0',
  gradientStart: '#1E88E5',
  gradientEnd: '#42A5F5',
  bgGrey: '#F5F7FA',
  cardWhite: '#FFFFFF',
  textPrimary: '#212121',
  textSecondary: '#666666',
  textHint: '#999999',
  divider: '#E8ECF1',
  danger: '#FF5252',
  white: '#FFFFFF',
  overlay: 'rgba(0, 0, 0, 0.5)',
  shadowCard: 'rgba(33, 150, 243, 0.1)',
  shadowButton: 'rgba(255, 82, 82, 0.08)'
};

// ==================== 页面组件 ====================

@Entry
@Component
export struct PersonalCenter {
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
  @State loadError: string = '';

  // 修改手机号相关
  @State showPhoneDialog: boolean = false;
  @State oldPhone: string = '';
  @State newPhone: string = '';
  @State smsCode: string = '';
  @State isSendingCode: boolean = false;
  @State countdown: number = 0;
  private countdownTimer: number = -1;

  // 修改密码相关
  @State showPasswordDialog: boolean = false;
  @State oldPassword: string = '';
  @State newPassword: string = '';
  @State confirmPassword: string = '';

  // 修改地址相关
  @State showAddressDialog: boolean = false;
  @State newAddress: string = '';

  @State showVerifyCodeHint: boolean = false;
  @State displayedVerifyCode: string = '';

  private baseUrl: string = APIConfig.PATIENT_SERVICE_URL;
  private authUrl: string = APIConfig.AUTH_SERVICE_URL;

  aboutToAppear(): void {
    const patientInfo = PatientManager.getPatientInfo();
    if (patientInfo) {
      this.patientId = patientInfo.patientId || this.patientId;
      this.userName = patientInfo.name || this.userName;
      this.userPhone = patientInfo.phone || this.userPhone;
    }

    if (this.patientId) {
      this.profileData.patientId = this.patientId;
    }
    if (this.userName) {
      this.profileData.name = this.userName;
    }
    if (this.userPhone) {
      this.profileData.phone = this.userPhone;
    }
    this.loadProfileData();
  }

  aboutToDisappear(): void {
    if (this.countdownTimer !== -1) {
      clearInterval(this.countdownTimer);
      this.countdownTimer = -1;
    }
  }

  // ==================== 工具方法 ====================

  private genderToText(gender: string): string {
    return GENDER_MAP[gender] || gender || '--';
  }

  private genderToNumber(genderText: string): number {
    if (genderText === '男' || genderText === '1') {
      return 1;
    }
    if (genderText === '女' || genderText === '2') {
      return 2;
    }
    return 0;
  }

  private calculateAge(birthday: string): number {
    if (!birthday) {
      return 0;
    }
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
      return 0;
    }
  }

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

  private maskIdCard(idCard: string): string {
    if (!idCard || idCard.length < 10) {
      return idCard || '--';
    }
    return idCard.substring(0, 6) + '****' + idCard.substring(idCard.length - 4);
  }

  private getToken(): string {
    return PatientManager.getToken();
  }

  // ==================== 数据加载 ====================

  private parseProfileResponse(raw: Record<string, Object>): PatientProfile {
    const avatar = raw['avatar'] as string;
    let avatarUrl = raw['avatarUrl'] as string || '';
    if (!avatarUrl && avatar) {
      const resolvedAvatar = avatar.startsWith('http') ? avatar : `${APIConfig.BASE_URL}:8004${avatar}`;
      avatarUrl = `${resolvedAvatar}${resolvedAvatar.includes('?') ? '&' : '?'}t=${Date.now()}`;
    }
    const birthday = (raw['birthday'] as string) || '';
    const profile: PatientProfile = {
      patientId: (raw['patientId'] as string) || this.patientId || '',
      name: (raw['name'] as string) || '',
      gender: raw['gender'] !== undefined && raw['gender'] !== null ? String(raw['gender']) : '',
      age: (raw['age'] as number) || 0,
      phone: (raw['phone'] as string) || '',
      address: (raw['address'] as string) || '',
      birthday: birthday,
      avatarUrl: avatarUrl,
      idCard: (raw['idCard'] as string) || ''
    };
    if (!profile.age && profile.birthday) {
      profile.age = this.calculateAge(profile.birthday);
    }
    return profile;
  }

  private syncLocalPatientInfo(): void {
    const info = PatientManager.getPatientInfo();
    if (!info) {
      return;
    }

    info.name = this.profileData.name;
    info.phone = this.profileData.phone;
    info.address = this.profileData.address;

    if (this.profileData.gender) {
      info.gender = this.genderToNumber(this.profileData.gender);
      info.genderText = this.genderToText(this.profileData.gender);
    }
    if (this.profileData.birthday) {
      info.birthday = this.profileData.birthday;
    }
    if (this.profileData.age > 0) {
      info.age = this.profileData.age;
    }

    PatientManager.savePatientInfo(info);
  }

  async loadProfileData(): Promise<void> {
    this.isLoading = true;
    this.loadError = '';

    try {
      // 使用 HttpUtil 获取脱敏信息
      const profileResult = await HttpUtil.get<ApiResult>(
        this.baseUrl + '/profile/info',
        getContext(this)
      );

      if (profileResult.code === 200 && profileResult.data) {
        this.profileData = this.parseProfileResponse(profileResult.data as Record<string, Object>);
        this.syncLocalPatientInfo();
      } else {
        this.loadError = profileResult.msg || '加载个人信息失败';
      }

      // 使用 HttpUtil 获取完整信息
      const rawResult = await HttpUtil.get<ApiResult>(
        this.baseUrl + '/profile/info-raw',
        getContext(this)
      );

      if (rawResult.code === 200 && rawResult.data) {
        const rawData = rawResult.data as Record<string, Object>;
        if (rawData['phone']) {
          this.oldPhone = rawData['phone'] as string;
          this.profileData.phone = rawData['phone'] as string;
        }
        if (rawData['idCard']) {
          this.profileData.idCard = rawData['idCard'] as string;
        }
        if (rawData['address']) {
          this.profileData.address = rawData['address'] as string;
        }
      }
    } catch (error) {
      console.error('加载数据失败:', error);
      const errMsg = error instanceof Error ? error.message : '加载失败，请检查网络后重试';

      // 判断是否是token过期
      if (errMsg.includes('Unauthorized') || errMsg.includes('401')) {
        this.handleTokenExpired();
        return;
      }
      this.loadError = errMsg;
    } finally {
      this.isLoading = false;
    }
  }

  // ==================== 业务逻辑 ====================

  private goToProfileDetail(): void {
    router.pushUrl({
      url: 'pages/profile/ProfileDetail',
      params: {
        profileData: JSON.stringify(this.profileData),
        patientId: this.patientId
      }
    });
  }

  private openPhoneDialog(): void {
    this.oldPhone = this.profileData.phone;
    this.newPhone = '';
    this.smsCode = '';
    this.showVerifyCodeHint = false;
    this.displayedVerifyCode = '';
    this.showPhoneDialog = true;
  }

  private openPasswordDialog(): void {
    this.oldPassword = '';
    this.newPassword = '';
    this.confirmPassword = '';
    this.showPasswordDialog = true;
  }

  private openAddressDialog(): void {
    this.newAddress = this.profileData.address || '';
    this.showAddressDialog = true;
  }

  private startCountdown(seconds: number = 60): void {
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

  async sendSmsCode(): Promise<void> {
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
    this.showVerifyCodeHint = false;
    this.displayedVerifyCode = '';

    try {
      const requestBody: SendCodeRequest = {
        phone: phone,
        skipRegisterCheck: true
      };

      const result = await HttpUtil.post<Record<string, Object>>(
        this.authUrl + '/patient/send-code',
        requestBody,
        getContext(this)
      );

      if (result['code'] === 200) {
        const data = result['data'] as Record<string, Object>;

        if (data && data['verifyCode']) {
          this.displayedVerifyCode = data['verifyCode'] as string;
          this.smsCode = data['verifyCode'] as string;
          this.showVerifyCodeHint = true;

          setTimeout(() => {
            this.showVerifyCodeHint = false;
          }, 8000);

          promptAction.showToast({
            message: '验证码已自动填入',
            duration: 3000
          });
        } else {
          promptAction.showToast({ message: '验证码已发送' });
        }
        this.startCountdown();
      } else {
        promptAction.showToast({ message: (result['msg'] as string) || '发送失败' });
      }
    } catch (error) {
      promptAction.showToast({ message: '发送失败，请重试' });
    } finally {
      this.isSendingCode = false;
    }
  }

  async changePhone(): Promise<void> {
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
    if (!/^\d{6}$/.test(smsCode)) {
      promptAction.showToast({ message: '验证码格式不正确（6位数字）' });
      return;
    }

    try {
      const requestBody: ChangePhoneRequest = {
        oldPhone: oldPhone,
        newPhone: newPhone,
        smsCode: smsCode
      };

      const result = await HttpUtil.post<ApiResult>(
        this.baseUrl + '/profile/change-phone',
        requestBody,
        getContext(this)
      );

      if (result.code === 200) {
        promptAction.showToast({ message: '手机号修改成功' });
        this.showPhoneDialog = false;
        this.smsCode = '';
        this.loadProfileData();
      } else {
        promptAction.showToast({ message: result.msg || '修改失败' });
      }
    } catch (error) {
      promptAction.showToast({ message: '修改失败，请重试' });
    }
  }

  async changePassword(): Promise<void> {
    if (!this.oldPassword || !this.newPassword || !this.confirmPassword) {
      promptAction.showToast({ message: '请填写完整信息' });
      return;
    }
    if (this.newPassword.length < 6) {
      promptAction.showToast({ message: '新密码长度不能少于6位' });
      return;
    }
    if (this.newPassword !== this.confirmPassword) {
      promptAction.showToast({ message: '两次输入的新密码不一致' });
      return;
    }

    try {
      const requestBody: ChangePasswordRequest = {
        oldPassword: this.oldPassword,
        newPassword: this.newPassword
      };

      const result = await HttpUtil.post<ApiResult>(
        this.baseUrl + '/profile/change-password',
        requestBody,
        getContext(this)
      );

      if (result.code === 200) {
        promptAction.showToast({ message: '密码修改成功，请重新登录' });
        this.showPasswordDialog = false;
        this.oldPassword = '';
        this.newPassword = '';
        this.confirmPassword = '';
        await PatientManager.clearSession(getContext(this));
        router.replaceUrl({ url: 'pages/login/LoginPage' });
      } else {
        promptAction.showToast({ message: result.msg || '修改失败' });
      }
    } catch (error) {
      promptAction.showToast({ message: '修改失败' });
    }
  }

  async updateAddress(): Promise<void> {
    const address = this.newAddress.trim();
    if (!address) {
      promptAction.showToast({ message: '请输入地址' });
      return;
    }
    if (address.length > 100) {
      promptAction.showToast({ message: '地址长度不能超过100个字符' });
      return;
    }

    try {
      const requestBody: UpdateAddressRequest = {
        address: address
      };

      const result = await HttpUtil.put<ApiResult>(
        this.baseUrl + '/profile/update-address',
        requestBody,
        getContext(this)
      );

      if (result.code === 200) {
        promptAction.showToast({ message: result.msg || '地址修改成功' });
        this.showAddressDialog = false;
        this.profileData.address = address;
        this.syncLocalPatientInfo();
        this.updateFunctionListDesc(address);
        this.loadProfileData();
      } else {
        promptAction.showToast({ message: result.msg || '修改失败' });
      }
    } catch (error) {
      console.error('修改地址失败:', error);
      const errMsg = error instanceof Error ? error.message : '修改失败，请重试';

      if (errMsg.includes('Unauthorized') || errMsg.includes('401')) {
        this.handleTokenExpired();
        return;
      }
      promptAction.showToast({ message: errMsg });
    }
  }

  private updateFunctionListDesc(address: string): void {
    const newList: FunctionItem[] = [];
    for (let i = 0; i < this.functionList.length; i++) {
      if (i === 0) {
        newList.push({
          title: this.functionList[i].title,
          icon: this.functionList[i].icon,
          desc: address || '暂未设置',
          action: this.functionList[i].action
        });
      } else {
        newList.push(this.functionList[i]);
      }
    }
    this.functionList = newList;
  }

  private handleTokenExpired(): void {
    PatientManager.clearSession(getContext(this));
    promptAction.showToast({ message: '登录已过期，请重新登录' });
    setTimeout(() => {
      router.replaceUrl({ url: 'pages/login/LoginPage' });
    }, 1500);
  }

  async logout(): Promise<void> {
    try {
      await PatientManager.clearSession(getContext(this));
      promptAction.showToast({ message: '已退出登录' });
      router.replaceUrl({ url: 'pages/login/LoginPage' });
    } catch (error) {
      promptAction.showToast({ message: '退出失败，请重试' });
    }
  }

  // ==================== 上传头像 ====================

  private async pickAvatarImage(): Promise<string> {
    const photoPicker = new picker.PhotoViewPicker();
    const photoResult = await photoPicker.select({
      MIMEType: picker.PhotoViewMIMETypes.IMAGE_TYPE,
      maxSelectNumber: 1
    });
    if (!photoResult || !photoResult.photoUris || photoResult.photoUris.length === 0) {
      throw new Error('用户取消选择');
    }
    return photoResult.photoUris[0];
  }

  private buildMultipartBody(boundary: string, fileName: string, fileBytes: ArrayBuffer): ArrayBuffer {
    const encoder = new util.TextEncoder();
    const crlf = '\r\n';
    let contentType = 'image/jpeg';
    if (fileName.endsWith('.png')) {
      contentType = 'image/png';
    } else if (fileName.endsWith('.webp')) {
      contentType = 'image/webp';
    }
    const head = `--${boundary}${crlf}` +
      `Content-Disposition: form-data; name="file"; filename="${fileName}"${crlf}` +
      `Content-Type: ${contentType}${crlf}${crlf}`;
    const tail = `${crlf}--${boundary}--${crlf}`;
    const headBytes = encoder.encode(head);
    const tailBytes = encoder.encode(tail);
    const fileBytesView = new Uint8Array(fileBytes);
    const bodyBytes = new Uint8Array(headBytes.length + fileBytesView.length + tailBytes.length);
    bodyBytes.set(headBytes, 0);
    bodyBytes.set(fileBytesView, headBytes.length);
    bodyBytes.set(tailBytes, headBytes.length + fileBytesView.length);
    return bodyBytes.buffer;
  }

  async uploadAvatar(): Promise<void> {
    try {
      promptAction.showDialog({
        title: '更换头像',
        message: '是否更换头像？',
        buttons: [
          { text: '取消', color: '#666666' },
          { text: '确认', color: '#007AFF' }
        ]
      }).then(async (value) => {
        if (value.index !== 1) {
          return;
        }
        try {
          const uri = await this.pickAvatarImage();
          const file = fileIo.openSync(uri, fileIo.OpenMode.READ_ONLY);
          const stat = fileIo.statSync(file.fd);
          if (stat.size <= 0) {
            fileIo.closeSync(file);
            promptAction.showToast({ message: '图片为空' });
            return;
          }
          const buffer = new ArrayBuffer(stat.size);
          fileIo.readSync(file.fd, buffer);
          fileIo.closeSync(file);

          const boundary = `----CloudBrainMed${Date.now()}`;
          let fileName = 'avatar.jpg';
          if (uri.includes('.png')) {
            fileName = 'avatar.png';
          } else if (uri.includes('.webp')) {
            fileName = 'avatar.webp';
          }
          const body = this.buildMultipartBody(boundary, fileName, buffer);

          const headers: UploadAvatarHeaders = {
            'Content-Type': `multipart/form-data; boundary=${boundary}`,
            'Accept': 'application/json',
            'token': this.getToken()
          };

          const httpRequest = http.createHttp();
          const response = await httpRequest.request(
            this.baseUrl + '/profile/avatar/upload',
            {
              method: http.RequestMethod.POST,
              header: headers,
              extraData: body,
              connectTimeout: 15000,
              readTimeout: 15000
            }
          );

          if (response.responseCode === 200) {
            const result: Record<string, Object> = JSON.parse(response.result as string);
            if (result['code'] === 200 && result['data']) {
              const data = result['data'] as Record<string, Object>;
              const avatar = (data['avatar'] as string) || (data['avatarUrl'] as string) || '';
              if (avatar) {
                const resolvedAvatar = avatar.startsWith('http') ? avatar : `${APIConfig.BASE_URL}:8004${avatar}`;
                this.profileData.avatarUrl = `${resolvedAvatar}${resolvedAvatar.includes('?') ? '&' : '?'}t=${Date.now()}`;
                this.syncLocalPatientInfo();
                this.loadProfileData();
                promptAction.showToast({ message: '头像上传成功' });
              }
            }
          }
          httpRequest.destroy();
        } catch (innerError) {
          const msg = String(innerError);
          if (!msg.includes('用户取消选择')) {
            promptAction.showToast({ message: '上传头像失败，请重试' });
          }
        }
      });
    } catch (error) {
      promptAction.showToast({ message: '上传头像失败，请重试' });
    }
  }

  // ==================== 功能列表数据 ====================

  @State functionList: FunctionItem[] = [
    {
      title: '收货地址',
      icon: '📍',
      desc: '暂未设置',
      action: (): void => {
        this.openAddressDialog();
      }
    },
    {
      title: '修改手机号',
      icon: '📱',
      desc: '更换绑定的手机号码',
      action: (): void => {
        this.openPhoneDialog();
      }
    },
    {
      title: '修改登录密码',
      icon: '🔒',
      desc: '定期更换密码更安全',
      action: (): void => {
        this.openPasswordDialog();
      }
    },
    {
      title: '缴费记录',
      icon: '📋',
      desc: '查看历史缴费记录',
      action: (): void => {
        router.pushUrl({
          url: 'pages/pay/PayHistory',
          params: {
            patientId: this.patientId
          }
        }).catch((err: BusinessError) => {
          console.error('跳转缴费记录失败:', JSON.stringify(err));
          promptAction.showToast({
            message: '页面跳转失败',
            duration: 1500
          });
        });
      }
    }
  ];

  // ==================== 自定义构建器 ====================

  @Builder
  private InfoItem(icon: string, label: string, value: string): void {
    Column() {
      Text(icon)
        .fontSize(14)
        .fontColor(COLORS.textHint)
        .margin({ bottom: 6 })
      Text(label)
        .fontSize(11)
        .fontColor(COLORS.textHint)
        .margin({ bottom: 4 })
      Text(value)
        .fontSize(13)
        .fontWeight(FontWeight.Medium)
        .fontColor(COLORS.textPrimary)
        .maxLines(1)
        .textOverflow({ overflow: TextOverflow.Ellipsis })
    }
    .layoutWeight(1)
    .padding({ top: 10, bottom: 10 })
    .alignItems(HorizontalAlign.Center)
  }

  @Builder
  private FunctionRow(item: FunctionItem, isLast: boolean): void {
    Row() {
      Stack() {
        Circle()
          .width(40)
          .height(40)
          .fill(COLORS.primaryLight)
        Text(item.icon)
          .fontSize(20)
      }

      Column() {
        Text(item.title)
          .fontSize(15)
          .fontWeight(FontWeight.Medium)
          .fontColor(COLORS.textPrimary)
        Text(item.desc)
          .fontSize(12)
          .fontColor(COLORS.textHint)
          .margin({ top: 2 })
          .maxLines(1)
          .textOverflow({ overflow: TextOverflow.Ellipsis })
      }
      .alignItems(HorizontalAlign.Start)
      .margin({ left: 14 })
      .layoutWeight(1)

      Text('›')
        .fontSize(18)
        .fontColor(COLORS.textHint)
    }
    .width('100%')
    .height(64)
    .padding({ left: 16, right: 16 })
    .border({
      width: { bottom: isLast ? 0 : 0.5 },
      color: COLORS.divider
    })
    .onClick(() => {
      item.action();
    })
  }

  @Builder
  private UserInfoCard(): void {
    Column() {
      Stack({ alignContent: Alignment.TopStart }) {
        Column()
          .width('100%')
          .height(160)
          .linearGradient({
            angle: 135,
            colors: [
              [COLORS.gradientStart, 0],
              [COLORS.primary, 0.5],
              [COLORS.gradientEnd, 1]
            ]
          })
          .borderRadius({
            topLeft: 16,
            topRight: 16
          })

        Column() {
          Stack({ alignContent: Alignment.BottomEnd }) {
            if (this.profileData.avatarUrl) {
              Image(this.profileData.avatarUrl)
                .width(76)
                .height(76)
                .borderRadius(38)
                .objectFit(ImageFit.Cover)
                .border({
                  width: 3,
                  color: COLORS.white
                })
            } else {
              Image($r('app.media.default_avatar'))
                .width(76)
                .height(76)
                .borderRadius(38)
                .objectFit(ImageFit.Cover)
                .border({
                  width: 3,
                  color: COLORS.white
                })
            }

            Stack() {
              Circle()
                .width(26)
                .height(26)
                .fill(COLORS.white)
                .border({
                  width: 2,
                  color: COLORS.primaryLight
                })
              Text('📷')
                .fontSize(13)
            }
            .margin({ right: -2, bottom: -2 })
          }
          .onClick(() => {
            this.uploadAvatar();
          })

          Text(this.profileData.name || '未设置姓名')
            .fontSize(20)
            .fontWeight(FontWeight.Bold)
            .fontColor(COLORS.white)
            .margin({ top: 12 })

          Row() {
            Text(`${this.genderToText(this.profileData.gender)} · ${this.getDisplayAge()}`)
              .fontSize(13)
              .fontColor('rgba(255, 255, 255, 0.9)')
          }
          .padding({
            left: 12,
            right: 12,
            top: 4,
            bottom: 4
          })
          .backgroundColor('rgba(255, 255, 255, 0.2)')
          .borderRadius(12)
          .margin({ top: 8 })
        }
        .width('100%')
        .height(160)
        .justifyContent(FlexAlign.Center)
        .alignItems(HorizontalAlign.Center)
      }

      Column() {
        Row() {
          this.InfoItem('📱', '手机号', this.profileData.phone || '未绑定')
          Divider()
            .vertical(true)
            .height(30)
            .color(COLORS.divider)
            .margin({ left: 8, right: 8 })
          this.InfoItem('🆔', '身份证', this.maskIdCard(this.profileData.idCard))
        }
        .width('100%')
        .padding({ left: 16, right: 16 })

        Divider()
          .width('100%')
          .height(0.5)
          .color(COLORS.divider)
          .margin({ left: 16, right: 16 })

        Row() {
          Text('📍')
            .fontSize(14)
            .margin({ right: 6 })
          Text(this.profileData.address || '点击添加地址')
            .fontSize(13)
            .fontColor(this.profileData.address ? COLORS.textSecondary : COLORS.textHint)
            .layoutWeight(1)
            .maxLines(1)
            .textOverflow({ overflow: TextOverflow.Ellipsis })
          Text('编辑 ›')
            .fontSize(12)
            .fontColor(COLORS.primary)
        }
        .width('100%')
        .padding({
          left: 16,
          right: 16,
          top: 12,
          bottom: 12
        })
        .onClick(() => {
          this.openAddressDialog();
        })

        Divider()
          .width('100%')
          .height(0.5)
          .color(COLORS.divider)
          .margin({ left: 16, right: 16 })

        Row() {
          Text('个人信息查看')
            .fontSize(13)
            .fontWeight(FontWeight.Medium)
            .fontColor(COLORS.primary)
          Blank()
          Text('›')
            .fontSize(16)
            .fontColor(COLORS.primary)
        }
        .width('100%')
        .padding({
          left: 16,
          right: 16,
          top: 12,
          bottom: 14
        })
        .onClick(() => {
          this.goToProfileDetail();
        })
      }
      .width('100%')
      .backgroundColor(COLORS.cardWhite)
      .borderRadius({
        bottomLeft: 16,
        bottomRight: 16
      })
    }
    .width('100%')
    .margin({
      left: 16,
      right: 16,
      top: 8
    })
    .borderRadius(16)
    .shadow({
      radius: 12,
      color: COLORS.shadowCard,
      offsetX: 0,
      offsetY: 4
    })
  }

  @Builder
  private FunctionListSection(): void {
    Column() {
      Row() {
        Text('⚙️')
          .fontSize(16)
          .margin({ right: 6 })
        Text('账户管理')
          .fontSize(14)
          .fontWeight(FontWeight.Bold)
          .fontColor(COLORS.textPrimary)
      }
      .width('100%')
      .padding({
        left: 16,
        right: 16,
        top: 16,
        bottom: 10
      })

      Column() {
        ForEach(this.functionList, (item: FunctionItem, index: number) => {
          this.FunctionRow(item, index === this.functionList.length - 1)
        })
      }
      .width('100%')
      .backgroundColor(COLORS.cardWhite)
      .borderRadius(12)
    }
    .width('100%')
    .margin({
      left: 16,
      right: 16,
      top: 12
    })
    .padding({ bottom: 16 })
    .backgroundColor(COLORS.cardWhite)
    .borderRadius(16)
    .shadow({
      radius: 8,
      color: 'rgba(0, 0, 0, 0.04)',
      offsetX: 0,
      offsetY: 2
    })
  }

  @Builder
  private LogoutSection(): void {
    Button() {
      Row() {
        Text('🚪')
          .fontSize(18)
          .margin({ right: 8 })
        Text('退出登录')
          .fontSize(15)
          .fontWeight(FontWeight.Medium)
      }
      .justifyContent(FlexAlign.Center)
    }
    .width('100%')
    .height(48)
    .fontColor(COLORS.danger)
    .backgroundColor(COLORS.cardWhite)
    .borderRadius(16)
    .margin({
      top: 16,
      left: 16,
      right: 16,
      bottom: 24
    })
    .shadow({
      radius: 6,
      color: COLORS.shadowButton,
      offsetX: 0,
      offsetY: 2
    })
    .onClick(() => {
      this.logout();
    })
  }

  @Builder
  private DialogHeader(title: string, onClose: () => void): void {
    Row() {
      Circle()
        .width(4)
        .height(18)
        .fill(COLORS.primary)
        .margin({ right: 8 })
      Text(title)
        .fontSize(17)
        .fontWeight(FontWeight.Bold)
        .fontColor(COLORS.textPrimary)
      Blank()
      Text('✕')
        .fontSize(18)
        .fontColor(COLORS.textHint)
        .onClick(onClose)
    }
    .width('100%')
    .padding({ bottom: 14 })
  }

  @Builder
  private DialogButtonGroup(
    cancelText: string,
    confirmText: string,
    onCancel: () => void,
    onConfirm: () => void
  ): void {
    Row() {
      Button(cancelText)
        .layoutWeight(1)
        .height(44)
        .fontSize(15)
        .backgroundColor(COLORS.bgGrey)
        .fontColor(COLORS.textSecondary)
        .borderRadius(10)
        .onClick(onCancel)

      Button(confirmText)
        .layoutWeight(1)
        .height(44)
        .fontSize(15)
        .fontWeight(FontWeight.Medium)
        .backgroundColor(COLORS.primary)
        .fontColor(COLORS.white)
        .borderRadius(10)
        .margin({ left: 12 })
        .onClick(onConfirm)
    }
    .width('100%')
    .margin({ top: 22 })
  }

  @Builder
  private ChangePhoneDialog(): void {
    Column() {
      Column() {
        this.DialogHeader('修改手机号', () => {
          this.showPhoneDialog = false;
        })

        Row() {
          Text('当前号码')
            .fontSize(13)
            .fontColor(COLORS.textHint)
            .width(70)
          Text(this.oldPhone || '--')
            .fontSize(14)
            .fontWeight(FontWeight.Medium)
            .fontColor(COLORS.textPrimary)
            .layoutWeight(1)
            .textAlign(TextAlign.End)
        }
        .width('100%')
        .padding({ top: 6, bottom: 6 })
        .margin({ bottom: 14 })

        TextInput({
          text: this.newPhone,
          placeholder: '请输入新手机号'
        })
          .width('100%')
          .height(44)
          .fontSize(14)
          .backgroundColor(COLORS.bgGrey)
          .borderRadius(10)
          .padding({ left: 14 })
          .maxLength(11)
          .type(InputType.PhoneNumber)
          .placeholderColor(COLORS.textHint)
          .onChange((value: string) => {
            this.newPhone = value;
          })

        Row() {
          TextInput({
            text: this.smsCode,
            placeholder: '请输入6位验证码'
          })
            .layoutWeight(1)
            .height(44)
            .fontSize(14)
            .backgroundColor(COLORS.bgGrey)
            .borderRadius(10)
            .padding({ left: 14 })
            .maxLength(6)
            .type(InputType.Number)
            .placeholderColor(COLORS.textHint)
            .onChange((value: string) => {
              this.smsCode = value;
            })

          Button(this.countdown > 0 ? `${this.countdown}s` : '发送验证码')
            .height(44)
            .fontSize(13)
            .fontWeight(FontWeight.Medium)
            .fontColor(this.countdown > 0 ? COLORS.textHint : COLORS.white)
            .backgroundColor(this.countdown > 0 ? COLORS.bgGrey : COLORS.primary)
            .borderRadius(10)
            .margin({ left: 10 })
            .enabled(this.countdown === 0)
            .onClick(() => {
              this.sendSmsCode();
            })
        }
        .width('100%')
        .margin({ top: 14 })

        this.DialogButtonGroup(
          '取消',
          '确认修改',
          () => {
            this.showPhoneDialog = false;
          },
          () => {
            this.changePhone();
          }
        )
      }
      .width('88%')
      .padding({
        left: 22,
        right: 22,
        top: 20,
        bottom: 22
      })
      .backgroundColor(COLORS.cardWhite)
      .borderRadius(16)
    }
    .width('100%')
    .height('100%')
    .justifyContent(FlexAlign.Center)
    .alignItems(HorizontalAlign.Center)
    .backgroundColor(COLORS.overlay)
  }

  @Builder
  private ChangePasswordDialog(): void {
    Column() {
      Column() {
        this.DialogHeader('修改登录密码', () => {
          this.showPasswordDialog = false;
        })

        TextInput({
          text: this.oldPassword,
          placeholder: '请输入原密码'
        })
          .width('100%')
          .height(44)
          .fontSize(14)
          .backgroundColor(COLORS.bgGrey)
          .borderRadius(10)
          .padding({ left: 14 })
          .type(InputType.Password)
          .placeholderColor(COLORS.textHint)
          .onChange((value: string) => {
            this.oldPassword = value;
          })

        TextInput({
          text: this.newPassword,
          placeholder: '新密码（至少6位）'
        })
          .width('100%')
          .height(44)
          .fontSize(14)
          .backgroundColor(COLORS.bgGrey)
          .borderRadius(10)
          .padding({ left: 14 })
          .type(InputType.Password)
          .placeholderColor(COLORS.textHint)
          .onChange((value: string) => {
            this.newPassword = value;
          })
          .margin({ top: 14 })

        TextInput({
          text: this.confirmPassword,
          placeholder: '请再次输入新密码'
        })
          .width('100%')
          .height(44)
          .fontSize(14)
          .backgroundColor(COLORS.bgGrey)
          .borderRadius(10)
          .padding({ left: 14 })
          .type(InputType.Password)
          .placeholderColor(COLORS.textHint)
          .onChange((value: string) => {
            this.confirmPassword = value;
          })
          .margin({ top: 14 })

        this.DialogButtonGroup(
          '取消',
          '确认修改',
          () => {
            this.showPasswordDialog = false;
          },
          () => {
            this.changePassword();
          }
        )
      }
      .width('88%')
      .padding({
        left: 22,
        right: 22,
        top: 20,
        bottom: 22
      })
      .backgroundColor(COLORS.cardWhite)
      .borderRadius(16)
    }
    .width('100%')
    .height('100%')
    .justifyContent(FlexAlign.Center)
    .alignItems(HorizontalAlign.Center)
    .backgroundColor(COLORS.overlay)
  }

  @Builder
  private ChangeAddressDialog(): void {
    Column() {
      Column() {
        this.DialogHeader('修改地址', () => {
          this.showAddressDialog = false;
        })

        TextArea({
          text: this.newAddress,
          placeholder: '请输入详细地址（如：xx省xx市xx区xx路xx号）'
        })
          .width('100%')
          .height(110)
          .fontSize(14)
          .fontColor(COLORS.textPrimary)
          .backgroundColor(COLORS.bgGrey)
          .borderRadius(10)
          .padding(14)
          .maxLength(100)
          .placeholderColor(COLORS.textHint)
          .onChange((value: string) => {
            this.newAddress = value;
          })

        Row() {
          Blank()
          Text(`${this.newAddress.length}/100`)
            .fontSize(11)
            .fontColor(this.newAddress.length >= 90 ? COLORS.danger : COLORS.textHint)
        }
        .width('100%')
        .margin({ top: 4, bottom: 4 })

        this.DialogButtonGroup(
          '取消',
          '保存地址',
          () => {
            this.showAddressDialog = false;
          },
          () => {
            this.updateAddress();
          }
        )
      }
      .width('88%')
      .padding({
        left: 22,
        right: 22,
        top: 20,
        bottom: 22
      })
      .backgroundColor(COLORS.cardWhite)
      .borderRadius(16)
    }
    .width('100%')
    .height('100%')
    .justifyContent(FlexAlign.Center)
    .alignItems(HorizontalAlign.Center)
    .backgroundColor(COLORS.overlay)
  }

  @Builder
  private LoadingState(): void {
    Column() {
      LoadingProgress()
        .width(44)
        .height(44)
        .color(COLORS.primary)

      Text('正在加载信息...')
        .fontSize(14)
        .fontColor(COLORS.textHint)
        .margin({ top: 14 })
    }
    .width('100%')
    .height('100%')
    .justifyContent(FlexAlign.Center)
    .backgroundColor(COLORS.bgGrey)
  }

  @Builder
  private ErrorState(): void {
    Column() {
      Text('😕')
        .fontSize(48)
        .margin({ bottom: 14 })

      Text(this.loadError)
        .fontSize(14)
        .fontColor(COLORS.textHint)
        .margin({ bottom: 18 })

      Button('重新加载')
        .width(130)
        .height(40)
        .fontSize(15)
        .fontWeight(FontWeight.Medium)
        .backgroundColor(COLORS.primary)
        .fontColor(COLORS.white)
        .borderRadius(20)
        .onClick(() => {
          this.loadProfileData();
        })
    }
    .width('100%')
    .height('100%')
    .justifyContent(FlexAlign.Center)
    .backgroundColor(COLORS.bgGrey)
  }

  build() {
    Stack() {
      Column()
        .width('100%')
        .height('100%')
        .backgroundColor(COLORS.bgGrey)

      if (this.isLoading) {
        this.LoadingState()
      } else if (this.loadError) {
        this.ErrorState()
      } else {
        Scroll() {
          Column() {
            Column()
              .width('100%')
              .height(8)

            this.UserInfoCard()
            this.FunctionListSection()
            this.LogoutSection()
          }
          .width('100%')
        }
        .scrollBar(BarState.Off)
        .edgeEffect(EdgeEffect.Spring)
      }

      if (this.showPhoneDialog) {
        this.ChangePhoneDialog()
      }
      if (this.showPasswordDialog) {
        this.ChangePasswordDialog()
      }
      if (this.showAddressDialog) {
        this.ChangeAddressDialog()
      }
    }
    .width('100%')
    .height('100%')
  }
}
// pages/profile/PersonalCenter.ets
import http from '@ohos.net.http';
import promptAction from '@ohos.promptAction';
import router from '@ohos.router';
import picker from '@ohos.file.picker';
import fs from '@ohos.file.fs';
import { BusinessError } from '@ohos.base';
import { APIConfig } from '../common/APIConfig';
import { PatientManager } from '../common/PatientManager';

// ==================== 类型定义 ====================

interface UpdateProfileRequest {
  name: string;
  gender: string;
  address: string;
  birthday: string;
}

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

// ==================== 性别映射 ====================
const GENDER_MAP: Record<string, string> = {
  '2': '女',
  '1': '男'
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
  @State countdownTimer: number = -1;

  // 修改密码相关
  @State showPasswordDialog: boolean = false;
  @State oldPassword: string = '';
  @State newPassword: string = '';


  @State showVerifyCodeHint: boolean = false;
  @State displayedVerifyCode: string = '';



  private baseUrl: string = APIConfig.PATIENT_SERVICE_URL;
  private authUrl: string = APIConfig.AUTH_SERVICE_URL;

  aboutToAppear() {
    if (this.patientId) this.profileData.patientId = this.patientId;
    if (this.userName) this.profileData.name = this.userName;
    if (this.userPhone) this.profileData.phone = this.userPhone;
    this.loadProfileData();
  }

  aboutToDisappear() {
    if (this.countdownTimer !== -1) {
      clearInterval(this.countdownTimer);
      this.countdownTimer = -1;
    }
  }

  // ==================== 工具方法 ====================

  private genderToText(gender: string): string {
    return GENDER_MAP[gender] || gender || '--';
  }

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
    if (!idCard || idCard.length < 10) return idCard || '--';
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
      avatarUrl = avatar.startsWith('http') ? avatar : `${APIConfig.BASE_URL}:8004${avatar}`;
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
    if (!info) return;
    info.name = this.profileData.name;
    info.phone = this.profileData.phone;
    if (this.profileData.gender) {
      info.gender = Number(this.profileData.gender);
      info.genderText = this.genderToText(this.profileData.gender);
    }
    if (this.profileData.birthday) info.birthday = this.profileData.birthday;
    if (this.profileData.age > 0) info.age = this.profileData.age;
    PatientManager.savePatientInfo(info);
  }

  async loadProfileData() {
    this.isLoading = true;
    this.loadError = '';
    const token = this.getToken();
    if (!token) {
      this.isLoading = false;
      this.loadError = '未登录，请先登录';
      this.handleTokenExpired();
      return;
    }

    try {
      // 第一步：获取脱敏信息（用于展示）
      const httpRequest1 = http.createHttp();
      const response1 = await httpRequest1.request(
        this.baseUrl + `/profile/info`,
        {
          method: http.RequestMethod.GET,
          header: { 'Content-Type': 'application/json', 'token': token }
        }
      );

      if (response1.responseCode === 401) {
        this.handleTokenExpired();
        return;
      }

      if (response1.responseCode === 200) {
        const result: ApiResult = JSON.parse(response1.result as string);
        if (result.code === 200 && result.data) {
          this.profileData = this.parseProfileResponse(result.data as Record<string, Object>);
          this.syncLocalPatientInfo();
        } else {
          this.loadError = result.msg || '加载个人信息失败';
        }
      } else {
        this.loadError = `网络异常 (${response1.responseCode})`;
      }
      httpRequest1.destroy();

      // 第二步：获取完整信息（用于修改手机号等操作）
      const httpRequest2 = http.createHttp();
      const response2 = await httpRequest2.request(
        this.baseUrl + `/profile/info-raw`,
        {
          method: http.RequestMethod.GET,
          header: { 'Content-Type': 'application/json', 'token': token }
        }
      );

      if (response2.responseCode === 200) {
        const result: ApiResult = JSON.parse(response2.result as string);
        if (result.code === 200 && result.data) {
          const rawData = result.data as Record<string, Object>;
          // 保存完整手机号
          if (rawData['phone']) {
            this.oldPhone = rawData['phone'] as string;
            this.profileData.phone = rawData['phone'] as string;
          }
          if (rawData['idCard']) {
            this.profileData.idCard = rawData['idCard'] as string;
          }
        }
      }
      httpRequest2.destroy();

    } catch (error) {
      console.error('加载数据失败:', error);
      this.loadError = '加载失败，请检查网络后重试';
    } finally {
      this.isLoading = false;
    }
  }

  // ==================== 跳转到个人信息详情页 ====================

  private goToProfileDetail(): void {
    router.pushUrl({
      url: 'pages/profile/ProfileDetail',
      params: {
        profileData: JSON.stringify(this.profileData),
        patientId: this.patientId
      }
    });
  }

  // ==================== 修改手机号 ====================

  private startCountdown(seconds: number = 60) {
    if (this.countdownTimer !== -1) clearInterval(this.countdownTimer);
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

  async sendSmsCode() {
    const phone = this.newPhone.trim();
    if (!phone) { promptAction.showToast({ message: '请输入新手机号' }); return; }
    if (phone === this.oldPhone.trim()) { promptAction.showToast({ message: '新手机号与原手机号相同' }); return; }
    if (this.isSendingCode || this.countdown > 0) return;

    this.isSendingCode = true;
    this.showVerifyCodeHint = false;
    this.displayedVerifyCode = '';

    try {
      const token = this.getToken();
      const httpRequest = http.createHttp();
      const response = await httpRequest.request(
        this.authUrl + `/patient/send-code`,
        {
          method: http.RequestMethod.POST,
          header: { 'Content-Type': 'application/json', 'token': token },
          extraData: { phone: phone, skipRegisterCheck: true },
          connectTimeout: 10000,
          readTimeout: 10000
        }
      );

      if (response.responseCode === 200) {
        const result = JSON.parse(response.result as string) as Record<string, Object>;
        if (result['code'] === 200) {
          const data = result['data'] as Record<string, Object>;

          // 如果返回了验证码，显示在界面上
          if (data && data['verifyCode']) {
            this.displayedVerifyCode = data['verifyCode'] as string;
            this.smsCode = data['verifyCode'] as string;
            this.showVerifyCodeHint = true;

            // 5秒后自动隐藏
            setTimeout(() => {
              this.showVerifyCodeHint = false;
            }, 8000);

            promptAction.showToast({
              message: `验证码已自动填入`,
              duration: 3000
            });
          } else {
            promptAction.showToast({ message: '验证码已发送' });
          }
          this.smsCode = '';
          this.startCountdown();
        } else {
          promptAction.showToast({ message: (result['msg'] as string) || '发送失败' });
        }
      } else {
        promptAction.showToast({ message: '网络异常，请重试' });
      }
      httpRequest.destroy();
    } catch (error) {
      promptAction.showToast({ message: '发送失败，请重试' });
    } finally {
      this.isSendingCode = false;
    }
  }

  async changePhone() {
    const oldPhone = this.oldPhone.trim();
    const newPhone = this.newPhone.trim();
    const smsCode = this.smsCode.trim();

    if (!oldPhone) { promptAction.showToast({ message: '请填写原手机号' }); return; }
    if (!newPhone) { promptAction.showToast({ message: '请填写新手机号' }); return; }
    if (!smsCode) { promptAction.showToast({ message: '请输入验证码' }); return; }
    if (!/^\d{6}$/.test(smsCode)) { promptAction.showToast({ message: '验证码格式不正确（6位数字）' }); return; }

    try {
      const token = this.getToken();
      const httpRequest = http.createHttp();
      const response = await httpRequest.request(
        this.baseUrl + `/profile/change-phone`,
        {
          method: http.RequestMethod.POST,
          header: { 'Content-Type': 'application/json', 'token': token },
          extraData: { oldPhone, newPhone, smsCode }
        }
      );

      if (response.responseCode === 200) {
        const result: Record<string, Object> = JSON.parse(response.result as string);
        if (result['code'] === 200) {
          promptAction.showToast({ message: '手机号修改成功' });
          this.showPhoneDialog = false;
          this.smsCode = '';
          this.loadProfileData();
        } else {
          promptAction.showToast({ message: (result['msg'] as string) || '修改失败' });
        }
      } else {
        promptAction.showToast({ message: '网络异常，请重试' });
      }
      httpRequest.destroy();
    } catch (error) {
      promptAction.showToast({ message: '修改失败，请重试' });
    }
  }

  // ==================== 修改密码 ====================

  async changePassword() {
    if (!this.oldPassword || !this.newPassword) {
      promptAction.showToast({ message: '请填写完整信息' });
      return;
    }
    if (this.newPassword.length < 6) {
      promptAction.showToast({ message: '新密码长度不能少于6位' });
      return;
    }

    try {
      const token = this.getToken();
      const httpRequest = http.createHttp();
      const response = await httpRequest.request(
        this.baseUrl + `/profile/change-password`,
        {
          method: http.RequestMethod.POST,
          header: { 'Content-Type': 'application/json', 'token': token },
          extraData: { oldPassword: this.oldPassword, newPassword: this.newPassword }
        }
      );

      if (response.responseCode === 200) {
        const result: Record<string, Object> = JSON.parse(response.result as string);
        if (result['code'] === 200) {
          promptAction.showToast({ message: '密码修改成功，请重新登录' });
          this.showPasswordDialog = false;
          await PatientManager.clearSession(getContext(this));
          router.replaceUrl({ url: 'pages/login/LoginPage' });
        } else {
          promptAction.showToast({ message: (result['msg'] as string) || '修改失败' });
        }
      }
      httpRequest.destroy();
    } catch (error) {
      promptAction.showToast({ message: '修改失败' });
    }
  }

  // ==================== Token过期处理 ====================

  private handleTokenExpired() {
    PatientManager.clearPatientInfo();
    promptAction.showToast({ message: '登录已过期，请重新登录' });
    setTimeout(() => {
      router.replaceUrl({ url: 'pages/login/LoginPage' });
    }, 1500);
  }

  // ==================== 退出登录 ====================

  async logout() {
    try {
      await PatientManager.clearSession(getContext(this));
      promptAction.showToast({ message: '已退出登录' });
      router.replaceUrl({ url: 'pages/login/LoginPage' });
    } catch (error) {
      promptAction.showToast({ message: '退出失败，请重试' });
    }
  }

  // ==================== UI构建 ====================

  @Builder
  private StatusTag(status: string) {
    Row() {
      Circle()
        .width(6)
        .height(6)
        .fill('#52C41A')
        .margin({ right: 4 })
      Text(status)
        .fontSize(12)
        .fontColor('#52C41A')
        .fontWeight(FontWeight.Medium)
    }
    .padding({ left: 10, right: 10, top: 4, bottom: 4 })
    .backgroundColor('#F6FFED')
    .borderRadius(12)
  }

  @Builder
  private InfoRow(label: string, value: string) {
    Row() {
      Text(label)
        .fontSize(14)
        .fontColor('#8C8C8C')
        .width(70)
      Text(value || '--')
        .fontSize(14)
        .fontColor('#1A1A1A')
        .layoutWeight(1)
        .textAlign(TextAlign.End)
    }
    .width('100%')
    .padding({ top: 6, bottom: 6 })
  }

  @Builder
  private DividerLine() {
    Divider()
      .color('#F0F0F0')
      .height(0.5)
      .width('100%')
  }

  // ==================== 用户信息卡片 ====================

  @Builder
  private UserInfoCard() {
    Column() {
      Row() {
        if (this.profileData.avatarUrl) {
          Image(this.profileData.avatarUrl)
            .width(72)
            .height(72)
            .borderRadius(36)
            .objectFit(ImageFit.Cover)
            .onClick(() => { this.uploadAvatar(); })
        } else {
          Image($r('app.media.default_avatar'))
            .width(72)
            .height(72)
            .borderRadius(36)
            .objectFit(ImageFit.Cover)
            .onClick(() => { this.uploadAvatar(); })
        }

        Column() {
          Text(this.profileData.name || '未设置姓名')
            .fontSize(18)
            .fontWeight(FontWeight.Medium)
            .fontColor('#1A1A1A')

          Text(`${this.genderToText(this.profileData.gender)} · ${this.getDisplayAge()}`)
            .fontSize(13)
            .fontColor('#8C8C8C')
            .margin({ top: 4 })
        }
        .alignItems(HorizontalAlign.Start)
        .margin({ left: 16 })
        .layoutWeight(1)

        Row() {
          Text('›')
            .fontSize(20)
            .fontColor('#CCCCCC')
        }
        .padding({ left: 8, right: 4 })
        .onClick(() => { this.goToProfileDetail(); })
      }
      .width('100%')
      .padding(20)
      .onClick(() => { this.goToProfileDetail(); })
    }
    .width('100%')
    .backgroundColor('#FFFFFF')
    .borderRadius(12)
    .margin({ left: 16, right: 16, top: 0 })  // top: 0 完全贴顶
  }

  // ==================== 功能列表 ====================

  @Builder
  private FunctionRow(item: FunctionItem, isLast: boolean) {
    Row() {
      Text(item.icon)
        .fontSize(24)
        .width(44)
        .height(44)
        .borderRadius(12)
        .backgroundColor('#F5F7FA')
        .textAlign(TextAlign.Center)
        .lineHeight(44)

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
    .onClick(() => { item.action(); })
  }

  @Builder
  private FunctionListSection() {
    Column() {
      ForEach(this.functionList, (item: FunctionItem, index: number) => {
        this.FunctionRow(item, index === this.functionList.length - 1)
      })
    }
    .width('100%')
    .backgroundColor('#FFFFFF')
    .borderRadius(12)
    .margin({ left: 16, right: 16, top: 12 })  // 只保留顶部小间距
  }

  private functionList: FunctionItem[] = [
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
      title: '缴费记录',
      icon: '📋',
      desc: '查看历史缴费记录',
      action: () => {
        router.pushUrl({
          url: 'pages/pay/PayHistory',
          params: {
            patientId: this.patientId
          }
        }).catch((err: Error) => {
          console.error('跳转缴费记录失败:', JSON.stringify(err));
          promptAction.showToast({
            message: '页面跳转失败',
            duration: 1500
          });
        });
      }
    }
  ];

  // ==================== 退出登录 ====================

  @Builder
  private LogoutSection() {
    Button('退出登录')
      .width('100%')
      .height(48)
      .fontSize(16)
      .fontWeight(FontWeight.Medium)
      .fontColor('#FF3B30')
      .backgroundColor('#FFFFFF')
      .borderRadius(12)
      .margin({ top: 16, left: 16, right: 16, bottom: 24 })
      .onClick(() => { this.logout(); })
  }

  // ==================== 上传头像 ====================

  async uploadAvatar() {
    try {
      const photoPicker = new picker.PhotoViewPicker();
      const photoResult = await photoPicker.select({
        MIMEType: picker.PhotoViewMIMETypes.IMAGE_TYPE,
        maxSelectNumber: 1
      });
      if (photoResult.photoUris.length === 0) return;

      const uri = photoResult.photoUris[0];
      const file = fs.openSync(uri, fs.OpenMode.READ_ONLY);
      const stat = fs.statSync(file.fd);
      const buffer = new ArrayBuffer(stat.size);
      fs.readSync(file.fd, buffer);
      fs.closeSync(file);

      const token = this.getToken();
      const httpRequest = http.createHttp();
      const response = await httpRequest.request(
        this.baseUrl + `/profile/avatar-upload`,
        {
          method: http.RequestMethod.POST,
          header: { 'Content-Type': 'multipart/form-data', 'token': token },
          extraData: buffer
        }
      );

      if (response.responseCode === 200) {
        const result: Record<string, Object> = JSON.parse(response.result as string);
        if (result['code'] === 200) {
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

  // ==================== 对话框 ====================

  @Builder
  private ChangePhoneDialog() {
    Column() {
      Column() {
        Text('修改手机号')
          .fontSize(18)
          .fontWeight(FontWeight.Medium)
          .margin({ bottom: 20 })
          .fontColor('#1A1A1A')

        // 原手机号 - 显示完整号码（从 info-raw 获取）
        Row() {
          Text('原手机号').fontSize(14).fontColor('#8C8C8C').width(80)
          Text(this.oldPhone || '--')
            .fontSize(14)
            .fontColor('#1A1A1A')
            .layoutWeight(1)
            .textAlign(TextAlign.End)
        }
        .width('100%')
        .padding({ top: 8, bottom: 8 })
        .margin({ bottom: 12 })

        Row() {
          Text('新手机号').fontSize(14).fontColor('#8C8C8C').width(80)
          TextInput({ text: this.newPhone, placeholder: '请输入新手机号' })
            .layoutWeight(1).height(40).fontSize(14)
            .backgroundColor('#F5F6FA').borderRadius(4)
            .maxLength(11)
            .type(InputType.PhoneNumber)
            .onChange((value: string) => { this.newPhone = value; })
        }
        .width('100%').margin({ bottom: 12 })

        Row() {
          Text('验证码').fontSize(14).fontColor('#8C8C8C').width(80)
          TextInput({ text: this.smsCode, placeholder: '请输入6位验证码' })
            .layoutWeight(1).height(40).fontSize(14)
            .backgroundColor('#F5F6FA').borderRadius(4)
            .maxLength(6)
            .type(InputType.Number)
            .onChange((value: string) => { this.smsCode = value; })

          Button(this.countdown > 0 ? `${this.countdown}s` : (this.isSendingCode ? '发送中' : '获取验证码'))
            .height(40)
            .fontSize(13)
            .fontColor((this.countdown > 0 || this.isSendingCode) ? '#999999' : '#007AFF')
            .backgroundColor((this.countdown > 0 || this.isSendingCode) ? '#F0F0F0' : '#E8F4FF')
            .borderRadius(4)
            .margin({ left: 8 })
            .enabled(this.countdown === 0 && !this.isSendingCode)
            .onClick(() => { this.sendSmsCode(); })
        }
        .width('100%').margin({ bottom: 20 })

        // 显示验证码提示（开发环境）
        if (this.showVerifyCodeHint && this.displayedVerifyCode) {
          Row() {
            Text('📱 验证码：')
              .fontSize(13)
              .fontColor('#666666')
            Text(this.displayedVerifyCode)
              .fontSize(18)
              .fontWeight(FontWeight.Bold)
              .fontColor('#007AFF')
              .letterSpacing(4)
              .margin({ left: 4 })
          }
          .width('100%')
          .padding({ top: 8, bottom: 8, left: 12, right: 12 })
          .backgroundColor('#F0F7FF')
          .borderRadius(8)
          .margin({ bottom: 16 })
          .justifyContent(FlexAlign.Start)
        }

        Row() {
          Button('取消')
            .layoutWeight(1).height(40).fontSize(15)
            .backgroundColor('#F5F6FA').fontColor('#666666')
            .borderRadius(4)
            .onClick(() => {
              this.showPhoneDialog = false;
              this.smsCode = '';
              this.showVerifyCodeHint = false;
              this.displayedVerifyCode = '';
            })

          Button('确认修改')
            .layoutWeight(1).height(40).fontSize(15)
            .backgroundColor('#007AFF').fontColor('#FFFFFF')
            .borderRadius(4).margin({ left: 12 })
            .onClick(() => { this.changePhone(); })
        }
        .width('100%')
      }
      .width('85%').padding(24)
      .backgroundColor('#FFFFFF').borderRadius(16)
    }
    .width('100%').height('100%')
    .justifyContent(FlexAlign.Center)
    .alignItems(HorizontalAlign.Center)
    .backgroundColor('#00000080')
    .onClick((event: ClickEvent) => {
      if (event.target.id === 'dialogBg') {
        this.showPhoneDialog = false;
        this.smsCode = '';
        this.showVerifyCodeHint = false;
        this.displayedVerifyCode = '';
      }
    })
  }

  @Builder
  private ChangePasswordDialog() {
    Column() {
      Column() {
        Text('修改登录密码')
          .fontSize(18)
          .fontWeight(FontWeight.Medium)
          .margin({ bottom: 20 })
          .fontColor('#1A1A1A')

        Row() {
          Text('原密码').fontSize(14).fontColor('#8C8C8C').width(80)
          TextInput({ text: this.oldPassword, placeholder: '请输入原密码' })
            .layoutWeight(1).height(40).fontSize(14)
            .backgroundColor('#F5F6FA').borderRadius(4)
            .type(InputType.Password)
            .onChange((value: string) => { this.oldPassword = value; })
        }
        .width('100%').margin({ bottom: 12 })

        Row() {
          Text('新密码').fontSize(14).fontColor('#8C8C8C').width(80)
          TextInput({ text: this.newPassword, placeholder: '请输入新密码（至少6位）' })
            .layoutWeight(1).height(40).fontSize(14)
            .backgroundColor('#F5F6FA').borderRadius(4)
            .type(InputType.Password)
            .onChange((value: string) => { this.newPassword = value; })
        }
        .width('100%').margin({ bottom: 20 })

        Row() {
          Button('取消')
            .layoutWeight(1).height(40).fontSize(15)
            .backgroundColor('#F5F6FA').fontColor('#666666')
            .borderRadius(4)
            .onClick(() => { this.showPasswordDialog = false; })

          Button('确认修改')
            .layoutWeight(1).height(40).fontSize(15)
            .backgroundColor('#007AFF').fontColor('#FFFFFF')
            .borderRadius(4).margin({ left: 12 })
            .onClick(() => { this.changePassword(); })
        }
        .width('100%')
      }
      .width('85%').padding(24)
      .backgroundColor('#FFFFFF').borderRadius(16)
    }
    .width('100%').height('100%')
    .justifyContent(FlexAlign.Center)
    .alignItems(HorizontalAlign.Center)
    .backgroundColor('#00000080')
    .onClick((event: ClickEvent) => {
      if (event.target.id === 'dialogBg') {
        this.showPasswordDialog = false;
      }
    })
  }

  // ==================== 主构建 ====================

  build() {
    Column() {
      if (this.isLoading) {
        Column() {
          LoadingProgress().width(40).height(40).color('#007AFF')
          Text('加载中...').fontSize(14).fontColor('#999999').margin({ top: 12 })
        }
        .width('100%')
        .height('100%')
        .justifyContent(FlexAlign.Center)
        .backgroundColor('#F5F6FA')
      } else if (this.loadError) {
        Column() {
          Text(this.loadError).fontSize(14).fontColor('#999999').margin({ bottom: 16 })
          Button('重新加载').height(40).fontSize(15).backgroundColor('#007AFF')
            .onClick(() => { this.loadProfileData(); })
        }
        .width('100%')
        .height('100%')
        .justifyContent(FlexAlign.Center)
        .backgroundColor('#F5F6FA')
      } else {
        // 直接用 Column，不用 Scroll，用 Flex 布局实现内容顶对齐
        Column() {
          // 用户信息卡片 - 直接放在最顶部
          this.UserInfoCard()

          // 功能列表
          this.FunctionListSection()

          // 退出登录按钮 - 放到底部
          this.LogoutSection()

        }
        .width('100%')
        .height('100%')
        .justifyContent(FlexAlign.Start)
        .alignItems(HorizontalAlign.Center)
        .backgroundColor('#F5F6FA')
      }

      if (this.showPhoneDialog) { this.ChangePhoneDialog() }
      if (this.showPasswordDialog) { this.ChangePasswordDialog() }
    }
    .width('100%')
    .height('100%')
    .backgroundColor('#F5F6FA')
  }
}
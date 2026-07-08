// PersonalCenter.ets
import http from '@ohos.net.http';
import promptAction from '@ohos.promptAction';
import router from '@ohos.router';
import { photoAccessHelper } from '@kit.MediaLibraryKit';
import { fileIo } from '@kit.CoreFileKit';
import { abilityAccessCtrl, common, Permissions } from '@kit.AbilityKit';
import { BusinessError } from '@kit.BasicServicesKit';
import { APIConfig } from '../common/APIConfig';
import { HttpUtil, UploadFileOptions } from '../common/HttpUtil';
import { PatientManager } from '../common/PatientManager';
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

interface ApiResult {
  code: number;
  msg: string;
  data?: Object;
}

interface ImageMeta {
  fileName: string;
  mimeType: string;
}

interface DialogButton {
  text: string;
  color: string;
}

interface DialogResult {
  index: number;
}

interface AvatarUploadData {
  avatar?: string;
  avatarUrl?: string;
}

interface VerifyPasswordRequest {
  password: string;
}

interface ShowDialogOptions {
  title: string;
  message: string;
  buttons: DialogButton[];
}

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
  @State isSaving: boolean = false;
  @State isUploadingAvatar: boolean = false;
  @State loadError: string = '';

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
  @State showVerifyBeforeEditDialog: boolean = false;
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
  @State editVerifyPassword: string = '';

  @State rawPhone: string = '';
  @State newIdCard: string = '';
  private functionList: FunctionItem[] = [];
  // 配置文件
  private baseUrl: string = APIConfig.PATIENT_SERVICE_URL;
  private authUrl: string = APIConfig.AUTH_SERVICE_URL;
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
        desc: '修改姓名、性别、地址、出生日期（需身份验证）',
        action: () => {
          this.editVerifyPassword = '';
          this.showVerifyBeforeEditDialog = true;
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
        title: '修改身份证',
        icon: '🛡️',
        desc: '验证身份后可修改身份证信息',
        action: () => {
          this.verifyPassword = '';
          this.newIdCard = '';
          this.showIdCardDialog = true;
        }
      }
    ];
  }

  // 解析后端返回的患者信息
  private parseProfileResponse(raw: Record<string, Object>): PatientProfile {
    const genderVal = raw['gender'];
    const avatar = raw['avatar'] as string;
    const avatarUrlField = raw['avatarUrl'] as string || '';
    const avatarPath = avatar || avatarUrlField;
    const birthday = (raw['birthday'] as string) || '';
    const profile: PatientProfile = {
      patientId: (raw['patientId'] as string) || this.patientId || '',
      name: (raw['name'] as string) || '',
      gender: genderVal !== undefined && genderVal !== null ? String(genderVal) : '',
      age: (raw['age'] as number) || 0,
      phone: (raw['phone'] as string) || '',
      address: (raw['address'] as string) || '',
      birthday: birthday,
      avatarUrl: avatarPath ? `${APIConfig.resolvePatientAvatarUrl(avatarPath)}?t=${Date.now()}` : '',
      idCard: (raw['idCard'] as string) || ''
    };
    if (!profile.age && profile.birthday) {
      profile.age = this.calculateAge(profile.birthday);
    }
    return profile;
  }

  // 获取 Token（统一从 PatientManager 读取）
  getToken(): string {
    return PatientManager.getToken();
  }

  // 同步本地缓存中的用户基本信息
  private syncLocalPatientInfo(): void {
    const info = PatientManager.getPatientInfo();
    if (!info) {
      return;
    }
    info.name = this.profileData.name;
    info.phone = this.profileData.phone;
    if (this.profileData.gender) {
      info.gender = Number(this.profileData.gender);
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

  // 加载数据方法，计算年龄
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

      if (response1.responseCode === 401) {
        this.handleTokenExpired();
        return;
      }

      if (response1.responseCode === 200) {
        const result1: ApiResult = JSON.parse(response1.result as string);
        if (result1.code === 200 && result1.data) {
          const data = this.parseProfileResponse(result1.data as Record<string, Object>);
          this.profileData = data;
          this.oldPhone = data.phone || '';
        } else {
          this.loadError = result1.msg || '加载个人信息失败';
        }
      } else {
        this.loadError = `网络异常 (${response1.responseCode})`;
      }
      httpRequest1.destroy();

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
        const result2: ApiResult = JSON.parse(response2.result as string);
        if (result2.code === 200 && result2.data) {
          const rawData = result2.data as Record<string, Object>;
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

      if (!this.loadError) {
        this.syncLocalPatientInfo();
      }
    } catch (error) {
      const err = error as BusinessError;
      console.error('加载数据失败:', err.message);
      this.loadError = '加载失败，请检查网络后重试';
    } finally {
      this.isLoading = false;
    }
  }

  // 获取原始完整信息（用于修改手机号回填）
  async loadRawProfileData() {
    try {
      const token = this.getToken();
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

  // 编辑个人信息前的身份验证
  async verifyBeforeEdit() {
    if (!this.editVerifyPassword.trim()) {
      promptAction.showToast({ message: '请输入登录密码' });
      return;
    }

    try {
      const token = this.getToken();
      const httpRequest = http.createHttp();
      const verifyBody: VerifyPasswordRequest = {
        password: this.editVerifyPassword.trim()
      };
      const response = await httpRequest.request(
        this.baseUrl + `/profile/verify-idcard`,
        {
          method: http.RequestMethod.POST,
          header: {
            'Content-Type': 'application/json',
            'token': token
          },
          extraData: verifyBody
        }
      );

      if (response.responseCode === 200) {
        const result: ApiResult = JSON.parse(response.result as string);
        if (result.code === 200) {
          this.showVerifyBeforeEditDialog = false;
          this.editVerifyPassword = '';
          this.editName = this.profileData.name || '';
          this.editGender = this.profileData.gender || '2';
          this.editAddress = this.profileData.address || '';
          this.editBirthday = this.profileData.birthday || '';
          this.showEditDialog = true;
        } else {
          promptAction.showToast({ message: result.msg || '身份验证失败' });
          this.editVerifyPassword = '';
        }
      } else if (response.responseCode === 401) {
        this.handleTokenExpired();
      } else {
        promptAction.showToast({ message: '验证失败，请稍后重试' });
      }
      httpRequest.destroy();
    } catch (error) {
      console.error('身份验证失败:', error);
      promptAction.showToast({ message: '验证失败，请稍后重试' });
    }
  }

  async updateProfile() {
    if (this.isSaving) {
      return;
    }
    this.isSaving = true;
    try {
      const token = this.getToken();
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
          await this.loadProfileData();
        } else {
          const message = result['msg'] as string;
          promptAction.showToast({ message: message || '更新失败' });
          console.error('业务错误:', message);
        }
      } else {
        promptAction.showToast({ message: '更新失败，请稍后重试' });
      }
      httpRequest.destroy();
    } catch (error) {
      console.error('更新失败:', error);
      promptAction.showToast({ message: '更新失败' });
    } finally {
      this.isSaving = false;
    }
  }

  // 点击头像：确认后选择并上传
  onAvatarClick() {
    if (this.isUploadingAvatar) {
      return;
    }
    // 鸿蒙规范：取消在左(index=0)，确认在右(index=1)
    const cancelBtn: DialogButton = { text: '取消', color: '#999999' };
    const confirmBtn: DialogButton = { text: '确认', color: '#007AFF' };
    const dialogButtons: DialogButton[] = [cancelBtn, confirmBtn];
    const dialogOptions: ShowDialogOptions = {
      title: '更换头像',
      message: '是否更换头像？',
      buttons: dialogButtons
    };
    promptAction.showDialog(dialogOptions).then((result: DialogResult) => {
      if (result.index === 1) {
        // 等待对话框完全关闭后再打开相册，避免被遮挡无响应
        setTimeout(() => {
          this.pickAndUploadAvatar();
        }, 300);
      }
    }).catch(() => {
      // 用户取消对话框
    });
  }

  private async ensureImagePermission(): Promise<boolean> {
    const context = getContext(this) as common.UIAbilityContext;
    const atManager = abilityAccessCtrl.createAtManager();
    const permission: Permissions = 'ohos.permission.READ_IMAGEVIDEO';
    try {
      const tokenId = context.applicationInfo.accessTokenId;
      const grantStatus = await atManager.checkAccessToken(tokenId, permission);
      if (grantStatus === abilityAccessCtrl.GrantStatus.PERMISSION_GRANTED) {
        return true;
      }
      const requestResult = await atManager.requestPermissionsFromUser(context, [permission]);
      const granted = requestResult.authResults[0] === abilityAccessCtrl.GrantStatus.PERMISSION_GRANTED;
      if (!granted) {
        promptAction.showToast({ message: '需要相册权限才能更换头像' });
      }
      return granted;
    } catch (error) {
      console.error('申请相册权限失败:', JSON.stringify(error));
      // 权限申请异常时不阻断，系统相册选择器可能仍可正常使用
      return true;
    }
  }

  private isUserCancelError(err: BusinessError): boolean {
    if (err.code === 13900042 || err.code === 13900001) {
      return true;
    }
    const msg = err.message || '';
    return msg.includes('cancel') || msg.includes('Cancel') || msg.includes('取消');
  }

  private getImageMetaFromUri(uri: string): ImageMeta {
    const lower = uri.toLowerCase();
    if (lower.includes('.png')) {
      const meta: ImageMeta = { fileName: 'avatar.png', mimeType: 'image/png' };
      return meta;
    }
    if (lower.includes('.gif')) {
      const meta: ImageMeta = { fileName: 'avatar.gif', mimeType: 'image/gif' };
      return meta;
    }
    if (lower.includes('.webp')) {
      const meta: ImageMeta = { fileName: 'avatar.webp', mimeType: 'image/webp' };
      return meta;
    }
    if (lower.includes('.jpeg')) {
      const meta: ImageMeta = { fileName: 'avatar.jpeg', mimeType: 'image/jpeg' };
      return meta;
    }
    const defaultMeta: ImageMeta = { fileName: 'avatar.jpg', mimeType: 'image/jpeg' };
    return defaultMeta;
  }

  private isAllowedImageType(fileName: string): boolean {
    const lower = fileName.toLowerCase();
    return lower.endsWith('.jpg') || lower.endsWith('.jpeg') ||
      lower.endsWith('.png') || lower.endsWith('.gif') || lower.endsWith('.webp');
  }

  async pickAndUploadAvatar() {
    if (this.isUploadingAvatar) {
      return;
    }

    const hasPermission = await this.ensureImagePermission();
    if (!hasPermission) {
      promptAction.showToast({ message: '请授予相册权限后重试' });
      return;
    }

    try {
      console.info('开始打开相册选择器');
      const photoPicker = new photoAccessHelper.PhotoViewPicker();
      const selectOptions = new photoAccessHelper.PhotoSelectOptions();
      selectOptions.MIMEType = photoAccessHelper.PhotoViewMIMETypes.IMAGE_TYPE;
      selectOptions.maxSelectNumber = 1;

      promptAction.showToast({ message: '请选择图片', duration: 1500 });
      const photoResult = await photoPicker.select(selectOptions);

      if (!photoResult.photoUris || photoResult.photoUris.length === 0) {
        return;
      }

      const uri = photoResult.photoUris[0];
      const imageMeta = this.getImageMetaFromUri(uri);
      if (!this.isAllowedImageType(imageMeta.fileName)) {
        promptAction.showToast({ message: '仅支持 jpg、jpeg、png、gif、webp 格式' });
        return;
      }

      let fileSize = 0;
      const fileHandle = fileIo.openSync(uri, fileIo.OpenMode.READ_ONLY);
      try {
        fileSize = fileIo.statSync(fileHandle.fd).size;
      } finally {
        fileIo.closeSync(fileHandle);
      }

      const maxSize = 2 * 1024 * 1024;
      if (fileSize <= 0) {
        promptAction.showToast({ message: '图片文件为空，请重新选择' });
        return;
      }
      if (fileSize > maxSize) {
        promptAction.showToast({ message: '图片不能超过 2MB' });
        return;
      }

      this.isUploadingAvatar = true;
      const uploadOptions: UploadFileOptions = {
        url: this.baseUrl + '/profile/avatar-upload',
        fileUri: uri,
        fieldName: 'file',
        fileName: imageMeta.fileName,
        mimeType: imageMeta.mimeType
      };
      const responseText = await HttpUtil.uploadFile(uploadOptions);

      const result = JSON.parse(responseText) as ApiResult;
      if (result.code === 200 && result.data) {
        const data = result.data as AvatarUploadData;
        const avatarPath = data.avatar || data.avatarUrl || '';
        if (avatarPath) {
          this.profileData.avatarUrl =
            APIConfig.resolvePatientAvatarUrl(avatarPath) + `?t=${Date.now()}`;
          this.syncLocalPatientInfo();
        }
        promptAction.showToast({ message: '头像上传成功' });
        await this.loadProfileData();
      } else {
        promptAction.showToast({ message: result.msg || '头像上传失败' });
      }
    } catch (error) {
      const err = error as BusinessError;
      if (this.isUserCancelError(err)) {
        return;
      }
      console.error('上传头像失败:', JSON.stringify(error));
      const message = err.message && err.message !== 'Unauthorized'
        ? err.message
        : '上传头像失败，请检查网络后重试';
      if (err.message !== 'Unauthorized') {
        promptAction.showToast({ message: message });
      }
    } finally {
      this.isUploadingAvatar = false;
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
      const token = this.getToken();
      const httpRequest = http.createHttp();

      const response = await httpRequest.request(
        this.authUrl + `/patient/send-code`,
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
      const token = this.getToken();
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
      const token = this.getToken();
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
          await PatientManager.clearSession(getContext(this));
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
      const token = this.getToken();
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
    PatientManager.clearPatientInfo();
    promptAction.showToast({ message: '登录已过期，请重新登录' });
    setTimeout(() => {
      router.replaceUrl({ url: 'pages/login/LoginPage' });
    }, 1500);
  }

  // 退出登录
  async logout() {
    try {
      await PatientManager.clearSession(getContext(this));
      promptAction.showToast({ message: '已退出登录' });
      router.replaceUrl({ url: 'pages/login/LoginPage' });
    } catch (error) {
      console.error('退出登录失败:', error);
      promptAction.showToast({ message: '退出失败，请重试' });
    }
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
      } else if (this.loadError) {
        Column() {
          Text(this.loadError)
            .fontSize(14)
            .fontColor('#999999')
            .margin({ bottom: 16 })
          Button('重新加载')
            .height(40)
            .fontSize(15)
            .backgroundColor('#007AFF')
            .onClick(() => {
              this.loadProfileData();
            })
        }
        .width('100%')
        .layoutWeight(1)
        .justifyContent(FlexAlign.Center)
      } else {
        Scroll() {
          Column() {
            this.UserInfoCard()
            this.ProfileInfoSection()
            this.FunctionListSection()
            this.LogoutSection()
          }
          .width('100%')
        }
        .layoutWeight(1)
        .scrollBar(BarState.Off)
        .backgroundColor('#F5F5F5')
      }

      if (this.showVerifyBeforeEditDialog) {
        this.VerifyBeforeEditDialog()
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
          Stack() {
            Image(this.profileData.avatarUrl)
              .width(72)
              .height(72)
              .borderRadius(36)
              .objectFit(ImageFit.Cover)
            if (this.isUploadingAvatar) {
              Column() {
                LoadingProgress()
                  .width(24)
                  .height(24)
                  .color('#FFFFFF')
              }
              .width(72)
              .height(72)
              .borderRadius(36)
              .backgroundColor('#00000060')
              .justifyContent(FlexAlign.Center)
            }
          }
          .onClick(() => {
            this.onAvatarClick();
          })
        } else {
          Stack() {
            Image($r('app.media.default_avatar'))
              .width(72)
              .height(72)
              .borderRadius(36)
              .objectFit(ImageFit.Cover)
            if (this.isUploadingAvatar) {
              Column() {
                LoadingProgress()
                  .width(24)
                  .height(24)
                  .color('#FFFFFF')
              }
              .width(72)
              .height(72)
              .borderRadius(36)
              .backgroundColor('#00000060')
              .justifyContent(FlexAlign.Center)
            }
          }
          .onClick(() => {
            this.onAvatarClick();
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

  // 个人信息详情区域
  @Builder
  ProfileInfoSection() {
    Column() {
      Row() {
        Text('基本信息')
          .fontSize(16)
          .fontWeight(FontWeight.Medium)
          .fontColor('#1A1A1A')
        Blank()
        Text('查看全部')
          .fontSize(13)
          .fontColor('#007AFF')
          .onClick(() => {
            this.showViewDialog = true;
          })
      }
      .width('100%')
      .margin({ bottom: 12 })

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
    }
    .width('100%')
    .padding(16)
    .backgroundColor('#FFFFFF')
    .borderRadius(12)
    .margin({ top: 12, left: 16, right: 16 })
  }

  // 退出登录区域
  @Builder
  LogoutSection() {
    Button('退出登录')
      .width('100%')
      .height(48)
      .fontSize(16)
      .fontWeight(FontWeight.Medium)
      .fontColor('#FF3B30')
      .backgroundColor('#FFFFFF')
      .borderRadius(12)
      .margin({ top: 16, left: 16, right: 16, bottom: 24 })
      .onClick(() => {
        this.logout();
      })
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
  VerifyBeforeEditDialog() {
    Column() {
      Column() {
        Text('身份验证')
          .fontSize(18)
          .fontWeight(FontWeight.Medium)
          .margin({ bottom: 8 })

        Text('修改姓名等身份信息前，请输入登录密码验证')
          .fontSize(13)
          .fontColor('#999999')
          .margin({ bottom: 20 })
          .textAlign(TextAlign.Center)

        Row() {
          Text('登录密码').fontSize(14).fontColor('#666').width(80)
          TextInput({ text: this.editVerifyPassword, placeholder: '请输入登录密码' })
            .layoutWeight(1).height(40).fontSize(14)
            .backgroundColor('#F5F5F5').borderRadius(4)
            .type(InputType.Password)
            .onChange((value: string) => { this.editVerifyPassword = value; })
        }
        .width('100%').margin({ bottom: 20 })

        Row() {
          Button('取消')
            .layoutWeight(1).height(40).fontSize(15)
            .backgroundColor('#F5F5F5').fontColor('#666')
            .onClick(() => {
              this.showVerifyBeforeEditDialog = false;
              this.editVerifyPassword = '';
            })

          Button('验证并继续')
            .layoutWeight(1).height(40).fontSize(15)
            .backgroundColor('#007AFF').margin({ left: 12 })
            .onClick(() => { this.verifyBeforeEdit(); })
        }
        .width('100%')
      }
      .width('80%').padding(20)
      .backgroundColor('#FFFFFF').borderRadius(12)
    }
    .width('100%').height('100%')
    .justifyContent(FlexAlign.Center)
    .backgroundColor('#00000080')
    .onClick(() => {
      this.showVerifyBeforeEditDialog = false;
      this.editVerifyPassword = '';
    })
  }

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

          Button(this.isSaving ? '保存中...' : '保存')
            .layoutWeight(1).height(40).fontSize(15)
            .backgroundColor('#007AFF').margin({ left: 12 })
            .enabled(!this.isSaving)
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
              .onClick(() => {
                this.showViewDialog = false;
                this.onAvatarClick();
              })
          } else {
            Image($r('app.media.default_avatar'))
              .width(80)
              .height(80)
              .borderRadius(40)
              .objectFit(ImageFit.Cover)
              .onClick(() => {
                this.showViewDialog = false;
                this.onAvatarClick();
              })
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
            this.editVerifyPassword = '';
            setTimeout(() => {
              this.showVerifyBeforeEditDialog = true;
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
// pages/login/RegisterPage.ets
import router from '@ohos.router';
import promptAction from '@ohos.promptAction';
import { HttpUtil } from '../common/HttpUtil';
import { LogoIcon } from '../common/LogoIcon';
import { APIConfig } from '../common/APIConfig';

interface RegisterParams {
  name: string;
  phone: string;
  idCard: string;
  password: string;
  confirmPassword: string;
}

interface RegisterData {
  patientId: string;
  name: string;
  phone: string;
  birthday: string;
  age: number;
}

interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
}

@Entry
@Component
struct RegisterPage {
  @State name: string = '';
  @State phone: string = '';
  @State idCard: string = '';
  @State password: string = '';
  @State confirmPassword: string = '';
  @State isLoading: boolean = false;
  @State agreeProtocol: boolean = false;

  @State nameError: string = '';
  @State phoneError: string = '';
  @State idCardError: string = '';
  @State passwordError: string = '';

  @State nameFocused: boolean = false;
  @State phoneFocused: boolean = false;
  @State idCardFocused: boolean = false;
  @State passwordFocused: boolean = false;
  @State confirmPasswordFocused: boolean = false;

  @State showPassword: boolean = false;
  @State showConfirmPassword: boolean = false;

  private baseUrl: string = APIConfig.AUTH_SERVICE_URL;

  private validateForm(): boolean {
    let isValid = true;

    if (!this.name.trim()) {
      this.nameError = '请输入姓名';
      isValid = false;
    } else if (this.name.length < 2 || this.name.length > 20) {
      this.nameError = '姓名长度应在2-20字符之间';
      isValid = false;
    } else {
      this.nameError = '';
    }

    const phoneRegex = /^1[3-9]\d{9}$/;
    if (!this.phone.trim()) {
      this.phoneError = '请输入手机号';
      isValid = false;
    } else if (!phoneRegex.test(this.phone)) {
      this.phoneError = '请输入有效的手机号码';
      isValid = false;
    } else {
      this.phoneError = '';
    }

    const idCardRegex = /^[1-9]\d{5}(18|19|20)\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\d|3[01])\d{3}[\dXx]$/;
    if (!this.idCard.trim()) {
      this.idCardError = '请输入身份证号';
      isValid = false;
    } else if (!idCardRegex.test(this.idCard)) {
      this.idCardError = '请输入有效的身份证号码';
      isValid = false;
    } else {
      this.idCardError = '';
    }

    if (!this.password.trim()) {
      this.passwordError = '请输入密码';
      isValid = false;
    } else if (this.password.length < 6 || this.password.length > 20) {
      this.passwordError = '密码长度应为6-20位';
      isValid = false;
    } else if (this.password !== this.confirmPassword) {
      this.passwordError = '两次输入的密码不一致';
      isValid = false;
    } else {
      this.passwordError = '';
    }

    if (!this.agreeProtocol) {
      promptAction.showToast({
        message: '请同意用户协议和隐私政策',
        duration: 2000
      });
      isValid = false;
    }

    return isValid;
  }

  private async register(): Promise<void> {
    if (!this.validateForm()) {
      return;
    }

    this.isLoading = true;
    const context = getContext(this);

    const requestData: RegisterParams = {
      name: this.name,
      phone: this.phone,
      idCard: this.idCard,
      password: this.password,
      confirmPassword: this.confirmPassword
    };

    try {
      const result = await HttpUtil.post<ApiResponse<RegisterData>>(
        this.baseUrl + '/patient/register',
        requestData,
        context
      );

      if (result.code === 200) {
        promptAction.showToast({
          message: '🎉 注册成功！请登录',
          duration: 2000
        });

        setTimeout(() => {
          router.pushUrl({
            url: 'pages/login/LoginPage',
            params: {
              phone: this.phone,
              password: this.password
            }
          }).catch((err: Error) => {
            console.error('跳转失败：' + JSON.stringify(err));
          });
        }, 1500);
      } else {
        promptAction.showToast({
          message: result.message || '注册失败，请稍后重试',
          duration: 2000
        });
      }
    } catch (error) {
      console.error('注册失败：' + JSON.stringify(error));
      promptAction.showToast({
        message: '网络请求失败，请检查网络连接',
        duration: 2000
      });
    } finally {
      this.isLoading = false;
    }
  }

  private goToLogin(): void {
    router.pushUrl({
      url: 'pages/login/LoginPage',
      params: {
        phone: this.phone,
        password: this.password
      }
    }).catch((err: Error) => {
      console.error('跳转失败：' + JSON.stringify(err));
    });
  }

  build() {
    Stack() {
      // ======= 顶部蓝色渐变背景 =======
      Column()
        .width('100%')
        .height('40%')
        .linearGradient({
          angle: 180,
          colors: [
            ['#1a3a8a', 0],
            ['#2563eb', 0.4],
            ['rgba(37, 99, 235, 0.3)', 0.7],
            ['rgba(37, 99, 235, 0.05)', 1]
          ]
        })
        .position({ x: 0, y: 0 })

      // ======= 底部白色背景 =======
      Column()
        .width('100%')
        .height('55%')
        .backgroundColor('#ffffff')
        .position({ x: 0, y: '55%' })

      Scroll() {
        Column() {
          // ======= 顶部间距 =======
          Blank()
            .height(40)

          // ======= Logo区域 =======
          Column() {
            LogoIcon({
              iconSize: 50
            })
          }
          .width(60)
          .height(60)
          .margin({ bottom: 10 })
          .borderRadius(14)
          .backgroundColor('rgba(255, 255, 255, 0.15)')
          .backdropBlur(10)
          .border({
            width: 1,
            color: 'rgba(255, 255, 255, 0.2)'
          })
          .justifyContent(FlexAlign.Center)
          .alignItems(HorizontalAlign.Center)

          // ======= 标题 =======
          Text('CloudBrainMed')
            .fontSize(20)
            .fontWeight(FontWeight.Bold)
            .fontColor('#ffffff')
            .letterSpacing(1.5)
            .margin({ bottom: 2 })

          Text('AI智能医疗诊疗系统')
            .fontSize(11)
            .fontColor('rgba(255, 255, 255, 0.6)')
            .letterSpacing(3)
            .margin({ bottom: 28 })

          // ======= 白色卡片区域 =======
          Column() {
            // ===== 姓名 =====
            Column() {
              Text('姓名')
                .fontSize(14)
                .fontColor('#333333')
                .fontWeight(500)
                .margin({ bottom: 8 })
                .width('100%')

              TextInput({ placeholder: '请输入您的姓名', text: this.name })
                .width('100%')
                .height(46)
                .backgroundColor('#f5f6fa')
                .borderRadius(10)
                .padding({ left: 16, right: 16 })
                .fontSize(15)
                .fontColor('#1a1a1a')
                .placeholderColor('#bfbfbf')
                .border({
                  width: 1.5,
                  color: this.nameFocused ? '#2563eb' : 'transparent'
                })
                .onChange((value: string) => {
                  this.name = value;
                  if (this.nameError) this.nameError = '';
                })
                .onFocus(() => {
                  this.nameFocused = true;
                })
                .onBlur(() => {
                  this.nameFocused = false;
                })

              if (this.nameError) {
                Text(this.nameError)
                  .fontSize(12)
                  .fontColor('#ff4d4f')
                  .margin({ top: 6 })
                  .width('100%')
              }
            }
            .margin({ bottom: 16 })
            .width('100%')

            // ===== 手机号 =====
            Column() {
              Text('手机号')
                .fontSize(14)
                .fontColor('#333333')
                .fontWeight(500)
                .margin({ bottom: 8 })
                .width('100%')

              TextInput({ placeholder: '请输入您的手机号', text: this.phone })
                .width('100%')
                .height(46)
                .backgroundColor('#f5f6fa')
                .borderRadius(10)
                .padding({ left: 16, right: 16 })
                .type(InputType.PhoneNumber)
                .maxLength(11)
                .fontSize(15)
                .fontColor('#1a1a1a')
                .placeholderColor('#bfbfbf')
                .border({
                  width: 1.5,
                  color: this.phoneFocused ? '#2563eb' : 'transparent'
                })
                .onChange((value: string) => {
                  this.phone = value;
                  if (this.phoneError) this.phoneError = '';
                })
                .onFocus(() => {
                  this.phoneFocused = true;
                })
                .onBlur(() => {
                  this.phoneFocused = false;
                })

              if (this.phoneError) {
                Text(this.phoneError)
                  .fontSize(12)
                  .fontColor('#ff4d4f')
                  .margin({ top: 6 })
                  .width('100%')
              }
            }
            .margin({ bottom: 16 })
            .width('100%')

            // ===== 身份证号 =====
            Column() {
              Text('身份证号')
                .fontSize(14)
                .fontColor('#333333')
                .fontWeight(500)
                .margin({ bottom: 8 })
                .width('100%')

              TextInput({ placeholder: '请输入您的身份证号', text: this.idCard })
                .width('100%')
                .height(46)
                .backgroundColor('#f5f6fa')
                .borderRadius(10)
                .padding({ left: 16, right: 16 })
                .maxLength(18)
                .fontSize(15)
                .fontColor('#1a1a1a')
                .placeholderColor('#bfbfbf')
                .border({
                  width: 1.5,
                  color: this.idCardFocused ? '#2563eb' : 'transparent'
                })
                .onChange((value: string) => {
                  this.idCard = value;
                  if (this.idCardError) this.idCardError = '';
                })
                .onFocus(() => {
                  this.idCardFocused = true;
                })
                .onBlur(() => {
                  this.idCardFocused = false;
                })

              if (this.idCardError) {
                Text(this.idCardError)
                  .fontSize(12)
                  .fontColor('#ff4d4f')
                  .margin({ top: 6 })
                  .width('100%')
              }
            }
            .margin({ bottom: 16 })
            .width('100%')

            // ===== 密码 =====
            Column() {
              Text('密码')
                .fontSize(14)
                .fontColor('#333333')
                .fontWeight(500)
                .margin({ bottom: 8 })
                .width('100%')

              TextInput({ placeholder: '请输入密码（6-20位）', text: this.password })
                .width('100%')
                .height(46)
                .backgroundColor('#f5f6fa')
                .borderRadius(10)
                .padding({ left: 16, right: 16 })
                .type(this.showPassword ? InputType.Normal : InputType.Password)
                .fontSize(15)
                .fontColor('#1a1a1a')
                .placeholderColor('#bfbfbf')
                .border({
                  width: 1.5,
                  color: this.passwordFocused ? '#2563eb' : 'transparent'
                })
                .showPasswordIcon(true)  // 显示密码眼睛图标
                .onChange((value: string) => {
                  this.password = value;
                  if (this.passwordError) this.passwordError = '';
                })
                .onFocus(() => {
                  this.passwordFocused = true;
                })
                .onBlur(() => {
                  this.passwordFocused = false;
                })
            }
            .margin({ bottom: 16 })
            .width('100%')

            // ===== 确认密码 =====
            Column() {
              Text('确认密码')
                .fontSize(14)
                .fontColor('#333333')
                .fontWeight(500)
                .margin({ bottom: 8 })
                .width('100%')

              TextInput({ placeholder: '请再次输入密码', text: this.confirmPassword })
                .width('100%')
                .height(46)
                .backgroundColor('#f5f6fa')
                .borderRadius(10)
                .padding({ left: 16, right: 16 })
                .type(this.showConfirmPassword ? InputType.Normal : InputType.Password)
                .fontSize(15)
                .fontColor('#1a1a1a')
                .placeholderColor('#bfbfbf')
                .border({
                  width: 1.5,
                  color: this.confirmPasswordFocused ? '#2563eb' : 'transparent'
                })
                .showPasswordIcon(true)  // 显示密码眼睛图标
                .onChange((value: string) => {
                  this.confirmPassword = value;
                  if (this.passwordError) this.passwordError = '';
                })
                .onFocus(() => {
                  this.confirmPasswordFocused = true;
                })
                .onBlur(() => {
                  this.confirmPasswordFocused = false;
                })

              if (this.passwordError) {
                Text(this.passwordError)
                  .fontSize(12)
                  .fontColor('#ff4d4f')
                  .margin({ top: 6 })
                  .width('100%')
              }
            }
            .margin({ bottom: 8 })
            .width('100%')

            // ===== 用户协议 =====
            Row() {
              Checkbox({ name: 'agree' })
                .width(18)
                .height(18)
                .select(this.agreeProtocol)
                .onChange((value: boolean) => {
                  this.agreeProtocol = value;
                })
              Text('我已阅读并同意')
                .fontSize(13)
                .fontColor('#8a8a8a')
                .margin({ left: 6 })
              Text('《用户协议》')
                .fontSize(13)
                .fontColor('#2563eb')
              Text('和')
                .fontSize(13)
                .fontColor('#8a8a8a')
              Text('《隐私政策》')
                .fontSize(13)
                .fontColor('#2563eb')
            }
            .margin({ top: 20, bottom: 24 })

            // ===== 注册按钮 =====
            Button('注 册')
              .width('100%')
              .height(50)
              .fontSize(17)
              .fontWeight(FontWeight.Medium)
              .fontColor('#ffffff')
              .backgroundColor(this.isLoading ? '#d9d9d9' : '#2563eb')
              .borderRadius(12)
              .enabled(!this.isLoading)
              .onClick(() => this.register())

            if (this.isLoading) {
              LoadingProgress()
                .width(28)
                .height(28)
                .color('#2563eb')
                .margin({ top: 12 })
            }

            // ===== 已有账号跳转 =====
            Row() {
              Text('已有账号？')
                .fontSize(14)
                .fontColor('#8a8a8a')
              Text('立即登录')
                .fontSize(14)
                .fontColor('#2563eb')
                .fontWeight(FontWeight.Medium)
                .margin({ left: 6 })
                .onClick(() => this.goToLogin())
            }
            .margin({ top: 16, bottom: 8 })
          }
          .width('100%')
          .padding({ left: 28, right: 28, top: 28, bottom: 24 })
          .backgroundColor('rgba(255, 255, 255, 0.92)')
          .borderRadius(20)
          .backdropBlur(20)

          Blank()
            .height(30)
        }
        .width('100%')
        .padding({ left: 24, right: 24 })
      }
      .width('100%')
      .height('100%')
      .scrollable(ScrollDirection.Vertical)
      .scrollBar(BarState.Off)
    }
    .width('100%')
    .height('100%')
  }
}
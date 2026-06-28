// pages/login/LoginPage.ets
import router from '@ohos.router';
import preferences from '@ohos.data.preferences';
import promptAction from '@ohos.promptAction';
import { HttpUtil } from '../common/HttpUtil';
import { LogoIcon } from '../common/LogoIcon';
import { APIConfig } from '../common/APIConfig';
import { PatientInfo, PatientManager } from '../common/PatientManager';

// 定义请求参数接口
interface SendCodeParams {
  phone: string;
}
interface VerifyCodeData {
  verifyCode?: string;
  message: string;
}


interface LoginParams {
  phone: string;
  password?: string;
  verifyCode?: string;
  loginType: number;
}

interface LoginData {
  patientId: string;
  name: string;
  phone: string;
  token: string;
  expireTime: number;
  // 添加性别字段 - 后端返回的是数字类型
  gender?: number;
  genderText?: string;
  age?: number;
}

interface ApiResponse<T> {
  code: number;
  msg: string;
  data: T;
}



@Entry
@Component
struct LoginPage {
  @State phone: string = '';
  @State password: string = '';
  @State verifyCode: string = '';
  @State isLoading: boolean = false;
  @State isCodeSending: boolean = false;
  @State countdown: number = 0;
  @State phoneError: string = '';
  @State passwordError: string = '';
  @State codeError: string = '';
  @State showPassword: boolean = false;
  @State loginType: number = 1;
  @State phoneFocused: boolean = false;
  @State passwordFocused: boolean = false;
  @State codeFocused: boolean = false;

  private baseUrl: string = APIConfig.AUTH_SERVICE_URL;
  private countdownTimer: number = -1;


  aboutToAppear(): void {
    const params = router.getParams() as Record<string, string>;
    if (params) {
      const phoneParam = params['phone'];
      const passwordParam = params['password'];
      if (phoneParam) {
        this.phone = phoneParam;
      }
      if (passwordParam) {
        this.password = passwordParam;
      }
      if (phoneParam && passwordParam) {
        promptAction.showToast({
          message: '注册成功，请登录',
          duration: 2000
        });
      }
    }
  }

  aboutToDisappear(): void {
    if (this.countdownTimer !== -1) {
      clearInterval(this.countdownTimer);
    }
  }

  private validateForm(): boolean {
    let isValid: boolean = true;
    if (this.phone.length === 0) {
      this.phoneError = '请输入手机号';
      isValid = false;
    } else if (this.phone.length !== 11) {
      this.phoneError = '手机号长度必须为11位';
      isValid = false;
    } else {
      this.phoneError = '';
    }
    if (this.loginType === 1) {
      if (this.password.length === 0) {
        this.passwordError = '请输入密码';
        isValid = false;
      } else {
        this.passwordError = '';
      }
    } else {
      if (this.verifyCode.length === 0) {
        this.codeError = '请输入验证码';
        isValid = false;
      } else if (this.verifyCode.length !== 6) {
        this.codeError = '验证码长度必须为6位';
        isValid = false;
      } else {
        this.codeError = '';
      }
    }
    return isValid;
  }

  // pages/login/LoginPage.ets
  private async saveUserInfo(userInfo: LoginData): Promise<void> {
    try {
      // 构建患者信息
      const patientInfo: PatientInfo = {
        patientId: userInfo.patientId,
        name: userInfo.name,
        phone: userInfo.phone,
        token: userInfo.token,
        gender: userInfo.gender ?? 0,
        genderText: userInfo.genderText ?? '未知',
        age: userInfo.age ?? 0,
        loginTime: Date.now(),
        expiresIn: 7 * 24 * 60 * 60
      };

      // 仅保存到 AppStorage
      PatientManager.savePatientInfo(patientInfo);
      console.log('患者信息保存成功');

    } catch (error) {
      console.error('保存用户信息失败：' + JSON.stringify(error));
    }
  }

  private async sendVerifyCode(): Promise<void> {

    if (this.phone.length === 0) {
      promptAction.showToast({
        message: '请先输入手机号',
        duration: 2000
      });
      return;
    }
    if (this.phone.length !== 11) {
      promptAction.showToast({
        message: '请输入正确的手机号',
        duration: 2000
      });
      return;
    }
    this.isCodeSending = true;
    try {
      const url: string = this.baseUrl + '/patient/send-code';

      // 使用接口类型
      const params: SendCodeParams = {
        phone: this.phone
      };

      const result: ApiResponse<VerifyCodeData> = await HttpUtil.post<ApiResponse<VerifyCodeData>>(
        url,
        params,
        getContext(this)
      );

      if (result.code === 200) {
        promptAction.showToast({
          message: '验证码已发送，请注意查收',
          duration: 3000
        });
        this.startCountdown();
      } else if (result.code === 429) {
        promptAction.showToast({
          message: '发送过于频繁，请稍后再试',
          duration: 2000
        });
      } else {
        promptAction.showToast({
          message: result.msg || '发送失败',
          duration: 2000
        });
      }
    } catch (error) {
      promptAction.showToast({
        message: '发送失败，请稍后重试',
        duration: 2000
      });
    } finally {
      this.isCodeSending = false;
    }
  }

  private startCountdown(): void {
    this.countdown = 60;
    if (this.countdownTimer !== -1) {
      clearInterval(this.countdownTimer);
    }
    this.countdownTimer = setInterval((): void => {
      if (this.countdown > 0) {
        this.countdown--;
      } else {
        if (this.countdownTimer !== -1) {
          clearInterval(this.countdownTimer);
          this.countdownTimer = -1;
        }
      }
    }, 1000);
  }

  private async login(): Promise<void> {
    if (!this.validateForm()) {
      return;
    }
    this.isLoading = true;
    const requestData: LoginParams = {
      phone: this.phone,
      loginType: this.loginType
    };
    if (this.loginType === 1) {
      requestData.password = this.password;
    } else {
      requestData.verifyCode = this.verifyCode;
    }
    try {
      const result: ApiResponse<LoginData> = await HttpUtil.post<ApiResponse<LoginData>>(
        this.baseUrl + '/patient/login',
        requestData,
        getContext(this)
      );
      if (result.code === 200) {
        await this.saveUserInfo(result.data);
        promptAction.showToast({
          message: '登录成功',
          duration: 2000
        });
        setTimeout((): void => {
          // 传递完整用户信息到首页
          router.pushUrl({
            url: 'pages/Index',
            params: {
              patientId: result.data.patientId,
              name: result.data.name,
              phone: result.data.phone,
              gender: result.data.gender !== undefined ? result.data.gender : 0,
              genderText: result.data.genderText || '未知',
              age: result.data.age !== undefined ? result.data.age : 0
            }
          });
        }, 500);
      } else {
        promptAction.showToast({
          message: result.msg,
          duration: 2000
        });
      }
    } catch (error) {
      console.error('登录失败：' + JSON.stringify(error));
      promptAction.showToast({
        message: '网络请求失败，请稍后重试',
        duration: 2000
      });
    } finally {
      this.isLoading = false;
    }
  }

  private goToRegister(): void {
    router.pushUrl({
      url: 'pages/login/RegisterPage'
    });
  }

  private goToForgotPassword(): void {
    router.pushUrl({
      url: 'pages/login/ForgotPasswordPage'
    });
  }

  private switchLoginType(type: number): void {
    this.loginType = type;
    this.passwordError = '';
    this.codeError = '';
    this.password = '';
    this.verifyCode = '';
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
            .height(50)

          // ======= Logo区域 =======
          Column() {
            LogoIcon({
              iconSize: 56 // 登录页不显示文字，因为已经有标题了
            })
          }
          .width(64)
          .height(64)
          .margin({ bottom: 12 })
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
            .fontSize(22)
            .fontWeight(FontWeight.Bold)
            .fontColor('#ffffff')
            .letterSpacing(1.5)
            .margin({ bottom: 2 })

          Text('AI智能医疗诊疗系统')
            .fontSize(12)
            .fontColor('rgba(255, 255, 255, 0.6)')
            .letterSpacing(3)
            .margin({ bottom: 32 })

          // ======= 白色卡片区域 =======
          Column() {
            // ===== 登录方式切换 =====
            Row() {
              Text('密码登录')
                .fontSize(16)
                .fontWeight(this.loginType === 1 ? FontWeight.Bold : FontWeight.Normal)
                .fontColor(this.loginType === 1 ? '#2563eb' : '#999999')
                .padding({ bottom: 10 })
                .border({
                  width: { bottom: 2.5 },
                  color: this.loginType === 1 ? '#2563eb' : 'transparent'
                })
                .onClick((): void => {
                  this.switchLoginType(1);
                })

              Text('验证码登录')
                .fontSize(16)
                .fontWeight(this.loginType === 2 ? FontWeight.Bold : FontWeight.Normal)
                .fontColor(this.loginType === 2 ? '#2563eb' : '#999999')
                .margin({ left: 36 })
                .padding({ bottom: 10 })
                .border({
                  width: { bottom: 2.5 },
                  color: this.loginType === 2 ? '#2563eb' : 'transparent'
                })
                .onClick((): void => {
                  this.switchLoginType(2);
                })
            }
            .width('100%')
            .justifyContent(FlexAlign.Center)
            .margin({ bottom: 28 })

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
                .height(48)
                .backgroundColor('rgba(255, 255, 255, 0.8)')
                .borderRadius(12)
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
                .onChange((value: string): void => {
                  this.phone = value;
                  if (this.phoneError.length > 0) {
                    this.phoneError = '';
                  }
                })
                .onFocus((): void => {
                  this.phoneFocused = true;
                })
                .onBlur((): void => {
                  this.phoneFocused = false;
                })

              if (this.phoneError.length > 0) {
                Text(this.phoneError)
                  .fontSize(12)
                  .fontColor('#ff4d4f')
                  .margin({ top: 6 })
                  .width('100%')
              }
            }
            .margin({ bottom: 18 })
            .width('100%')

            // ===== 密码登录 =====
            if (this.loginType === 1) {
              Column() {
                Text('密码')
                  .fontSize(14)
                  .fontColor('#333333')
                  .fontWeight(500)
                  .margin({ bottom: 8 })
                  .width('100%')

                TextInput({ placeholder: '请输入密码', text: this.password })
                  .width('100%')
                  .height(48)
                  .backgroundColor('rgba(255, 255, 255, 0.8)')
                  .borderRadius(12)
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
                  .onChange((value: string): void => {
                    this.password = value;
                    if (this.passwordError.length > 0) {
                      this.passwordError = '';
                    }
                  })
                  .onFocus((): void => {
                    this.passwordFocused = true;
                  })
                  .onBlur((): void => {
                    this.passwordFocused = false;
                  })

                if (this.passwordError.length > 0) {
                  Text(this.passwordError)
                    .fontSize(12)
                    .fontColor('#ff4d4f')
                    .margin({ top: 6 })
                    .width('100%')
                }

                Row() {
                  Text('忘记密码？')
                    .fontSize(14)
                    .fontColor('#2563eb')
                    .onClick((): void => {
                      this.goToForgotPassword();
                    })
                }
                .width('100%')
                .justifyContent(FlexAlign.End)
                .margin({ top: 12 })
              }
              .margin({ bottom: 8 })
              .width('100%')
            }

            // ===== 验证码登录 =====
            if (this.loginType === 2) {
              Column() {
                Text('验证码')
                  .fontSize(14)
                  .fontColor('#333333')
                  .fontWeight(500)
                  .margin({ bottom: 8 })
                  .width('100%')

                Row() {
                  TextInput({ placeholder: '请输入验证码', text: this.verifyCode })
                    .layoutWeight(1)
                    .height(48)
                    .backgroundColor('#f5f6fa')
                    .borderRadius(12)
                    .padding({ left: 16, right: 16 })
                    .type(InputType.Number)
                    .maxLength(6)
                    .fontSize(15)
                    .fontColor('#1a1a1a')
                    .placeholderColor('#bfbfbf')
                    .border({
                      width: 1.5,
                      color: this.codeFocused ? '#2563eb' : 'transparent'
                    })
                    .onChange((value: string): void => {
                      this.verifyCode = value;
                      if (this.codeError.length > 0) {
                        this.codeError = '';
                      }
                    })
                    .onFocus((): void => {
                      this.codeFocused = true;
                    })
                    .onBlur((): void => {
                      this.codeFocused = false;
                    })

                  if (this.countdown > 0) {
                    Text(this.countdown + 's')
                      .fontSize(13)
                      .fontColor('#999999')
                      .margin({ left: 12 })
                      .width(70)
                      .textAlign(TextAlign.End)
                  } else {
                    Text('获取验证码')
                      .fontSize(13)
                      .fontColor('#2563eb')
                      .fontWeight(500)
                      .margin({ left: 12 })
                      .width(70)
                      .textAlign(TextAlign.End)
                      .onClick((): void => {
                        this.sendVerifyCode();
                      })
                  }
                }
                .width('100%')
                .alignItems(VerticalAlign.Center)

                if (this.codeError.length > 0) {
                  Text(this.codeError)
                    .fontSize(12)
                    .fontColor('#ff4d4f')
                    .margin({ top: 6 })
                    .width('100%')
                }

                Text('仿真提示：验证码为 123456')
                  .fontSize(12)
                  .fontColor('#ff9800')
                  .margin({ top: 8 })
                  .width('100%')
              }
              .margin({ bottom: 8 })
              .width('100%')
            }

            // ===== 登录按钮 =====
            Button('登 录')
              .width('100%')
              .height(50)
              .fontSize(17)
              .fontWeight(FontWeight.Medium)
              .fontColor('#ffffff')
              .backgroundColor(this.isLoading ? '#d9d9d9' : '#2563eb')
              .borderRadius(12)
              .enabled(!this.isLoading)
              .onClick((): void => {
                this.login();
              })
              .margin({ top: 28, bottom: 16 })

            if (this.isLoading) {
              LoadingProgress()
                .width(28)
                .height(28)
                .color('#2563eb')
                .margin({ bottom: 8 })
            }

            // ===== 注册跳转 =====
            Row() {
              Text('还没有账号？')
                .fontSize(14)
                .fontColor('#8a8a8a')
              Text('立即注册')
                .fontSize(14)
                .fontColor('#2563eb')
                .fontWeight(FontWeight.Medium)
                .margin({ left: 6 })
                .onClick((): void => {
                  this.goToRegister();
                })
            }
            .margin({ top: 8, bottom: 8 })
          }
          .width('100%')
          .padding({ left: 28, right: 28, top: 28, bottom: 24 })
          .backgroundColor('rgba(255, 255, 255, 0.72)')
          .borderRadius(20)
          .backdropBlur(30)
          .border({
            width: 1,
            color: 'rgba(255, 255, 255, 0.2)'
          })

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
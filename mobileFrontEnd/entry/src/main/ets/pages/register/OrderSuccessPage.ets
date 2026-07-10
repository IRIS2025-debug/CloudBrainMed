// pages/register/OrderSuccessPage.ets
import router from '@ohos.router';
import { HttpUtil } from '../common/HttpUtil';
import promptAction from '@ohos.promptAction';
import { APIConfig } from '../common/APIConfig';
import { PatientInfo, PatientManager } from '../common/PatientManager';

interface Registration {
  registerId: string;
  patientId: string;
  doctorId: string;
  name: string;
  gender: number;
  birthday: string;
  chiefComplaint: string;
  department: string;
  consultRoom: string;
  visitDate: string;
  consultTime: string;
  price: number;
  payStatus: string;
  createTime: string;
}

interface SubmitRegisterRequest {
  patientId: string;
  doctorId: string;
  scheduleId: string;
  chiefComplaint: string;
  visitDate: string;
  consultTime: string;
}

interface UnifiedPayDto {
  patientId: string;
  patientName: string;
  orderType: string;
  businessId: string;
  description?: string;
  amount: number;
  payMethod?: string;
}

interface PayResultVo {
  payId: string;
  orderType: string;
  businessId: string;
  patientId: string;
  patientName: string;
  totalAmount: number;
  payStatus: string;
  payStatusText: string;
  payTime: string;
  description: string;
}

interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
}

@Entry
@Component
export struct OrderSuccessPage {
  @State registerId: string = '';
  @State orderInfo: Registration | null = null;
  @State isLoading: boolean = true;
  @State isSubmitting: boolean = false;
  @State payStatus: string = 'WAITING';
  @State refreshFlag: number = 0;

  @State pendingRegisterData: SubmitRegisterRequest | null = null;
  @State pendingPrice: number = 0;
  @State pendingRoom: string = '';
  @State pendingDepartment: string = '';
  @State pendingDoctorName: string = '';
  @State isPendingConfirm: boolean = false;

  private baseUrl: string = APIConfig.PATIENT_SERVICE_URL;
  private payBaseUrl: string = APIConfig.PAYMENT_SERVICE_URL;
  private pollTimer: number = -1;
  private isPolling: boolean = false;
  private isDestroying: boolean = false;

  aboutToAppear() {
    const params = router.getParams() as Record<string, string>;
    this.registerId = params?.['registerId'] || '';

    const shouldRefresh = params?.['refresh'] === 'true';
    const status = params?.['status'] || '';
    this.isPendingConfirm = status === 'pending_confirm';

    const doctorName = params?.['doctorName'] || '';
    const department = params?.['department'] || '';
    const room = params?.['room'] || '';

    if (doctorName) {
      this.pendingDoctorName = doctorName;
      console.info('从参数中恢复医生姓名：', doctorName);
    }
    if (department) {
      this.pendingDepartment = department;
    }
    if (room) {
      this.pendingRoom = room;
    }


    if (this.isPendingConfirm) {
      this.pendingRegisterData = {
        patientId: params?.['patientId'] || '',
        doctorId: params?.['doctorId'] || '',
        scheduleId: params?.['scheduleId'] || '',
        chiefComplaint: params?.['chiefComplaint'] || '',
        visitDate: params?.['visitDate'] || '',
        consultTime: params?.['consultTime'] || ''
      };
      this.pendingPrice = Number(params?.['price']) || 0;
      this.pendingRoom = params?.['room'] || '待定';
      this.pendingDepartment = params?.['department'] || '待获取';
      this.pendingDoctorName = params?.['doctorName'] || '';

      console.info('收到待确认挂号信息：', JSON.stringify(this.pendingRegisterData));
      console.info('科室：', this.pendingDepartment);

      this.isLoading = false;
      this.payStatus = 'WAITING';

      const patientInfo = PatientManager.getPatientInfo();
      this.orderInfo = {
        registerId: '待确认',
        patientId: this.pendingRegisterData.patientId,
        doctorId: this.pendingRegisterData.doctorId,
        name: patientInfo?.name || '未知',
        gender: patientInfo?.gender || 0,
        birthday: '',
        chiefComplaint: this.pendingRegisterData.chiefComplaint,
        department: this.pendingDepartment,
        consultRoom: this.pendingRoom,
        visitDate: this.pendingRegisterData.visitDate,
        consultTime: this.pendingRegisterData.consultTime,
        price: this.pendingPrice,
        payStatus: 'WAITING',
        createTime: new Date().toISOString()
      };

      return;
    }

    if (this.registerId) {
      this.loadOrderDetail();
      this.startPolling();
    } else {
      this.isLoading = false;
      promptAction.showToast({
        message: '订单信息不完整',
        duration: 2000
      });
    }
  }

  aboutToDisappear() {
    this.isDestroying = true;
    this.stopPolling();
  }

  private startPolling() {
    if (this.isPolling || this.isPendingConfirm) return;

    this.isPolling = true;
    let retryCount = 0;
    const maxRetries = 20;

    this.pollTimer = setInterval(() => {
      retryCount++;

      if (this.isDestroying) {
        this.stopPolling();
        return;
      }

      if (this.payStatus === 'PAID' || this.payStatus === 'CANCELLED' || this.payStatus === 'REFUNDED') {
        this.stopPolling();
        return;
      }

      if (retryCount >= maxRetries) {
        console.info('轮询达到最大次数，停止轮询');
        this.stopPolling();
        return;
      }

      this.pollOrderStatus();
    }, 3000);
  }

  private stopPolling() {
    if (this.pollTimer !== -1) {
      clearInterval(this.pollTimer);
      this.pollTimer = -1;
    }
    this.isPolling = false;
  }

  private async pollOrderStatus() {
    try {
      const context = getContext(this);
      const result = await HttpUtil.get<ApiResponse<Registration>>(
        this.baseUrl + '/register/detail/' + this.registerId,
        context
      );

      if (result.code === 200 && result.data) {
        const newStatus = result.data.payStatus || 'WAITING';

        if (newStatus !== this.payStatus) {
          console.info('支付状态已更新：', this.payStatus, '->', newStatus);
          this.payStatus = newStatus;
          this.orderInfo = result.data;
          this.refreshFlag++;

          if (newStatus === 'PAID') {
            this.stopPolling();
            promptAction.showToast({
              message: '支付已成功！',
              duration: 2000
            });
          }
        }
      }
    } catch (error) {
      console.error('轮询支付状态失败：', JSON.stringify(error));
    }
  }

  async loadOrderDetail() {
    this.isLoading = true;
    const context = getContext(this);
    try {
      const result = await HttpUtil.get<ApiResponse<Registration>>(
        this.baseUrl + '/register/detail/' + this.registerId,
        context
      );
      if (result.code === 200) {
        this.orderInfo = result.data;
        this.payStatus = result.data.payStatus || 'WAITING';
        this.ensurePatientInfo();
        console.info('订单详情加载成功，支付状态：', this.payStatus);
      } else {
        promptAction.showToast({
          message: result.message || '加载订单详情失败',
          duration: 2000
        });
      }
    } catch (error) {
      console.error('加载订单详情失败：' + JSON.stringify(error));
    } finally {
      this.isLoading = false;
    }
  }

  ensurePatientInfo() {
    let patientInfo: PatientInfo | null = PatientManager.getPatientInfo();

    if (!patientInfo && this.orderInfo) {
      const info: PatientInfo = {
        patientId: this.orderInfo.patientId,
        name: this.orderInfo.name,
        phone: '',
        gender: this.orderInfo.gender ?? 0,
        genderText: this.orderInfo.gender === 1 ? '男' : this.orderInfo.gender === 2 ? '女' : '未知',
        age: 0
      };
      PatientManager.savePatientInfo(info);
      console.info('OrderSuccessPage: 从订单信息恢复患者数据');
    } else if (patientInfo) {
      console.info('OrderSuccessPage: 患者信息已存在');
    }
  }

  async confirmRegister() {
    if (this.isSubmitting) return;

    if (!this.pendingRegisterData) {
      promptAction.showToast({
        message: '挂号信息不完整',
        duration: 2000
      });
      return;
    }

    this.isSubmitting = true;
    const context = getContext(this);

    try {
      // 1. 创建挂号
      const registerResult = await HttpUtil.post<ApiResponse<Registration>>(
        this.baseUrl + '/register/submit',
        this.pendingRegisterData,
        context
      );

      if (registerResult.code !== 200) {
        promptAction.showToast({
          message: registerResult.message || '创建挂号失败',
          duration: 2000
        });
        this.isSubmitting = false;
        return;
      }

      const registration = registerResult.data;
      this.registerId = registration.registerId;
      this.orderInfo = registration;
      this.payStatus = registration.payStatus || 'WAITING';

      console.info('挂号创建成功，registerId:', this.registerId);

      // 2. 创建支付订单
      const patientInfo = PatientManager.getPatientInfo();
      if (!patientInfo) {
        promptAction.showToast({
          message: '请先登录',
          duration: 2000
        });
        this.isSubmitting = false;
        return;
      }

      const payDto: UnifiedPayDto = {
        patientId: patientInfo.patientId,
        patientName: patientInfo.name,
        orderType: 'REGISTER',
        businessId: this.registerId,
        description: '挂号费 - ' + (this.pendingDepartment || ''),
        amount: this.pendingPrice,
        payMethod: 'WECHAT'
      };

      console.info('创建支付订单请求:', JSON.stringify(payDto));

      const payResult = await HttpUtil.post<ApiResponse<PayResultVo>>(
        this.payBaseUrl + '/pay/create',
        payDto,
        context
      );

      console.info('创建支付订单返回：', JSON.stringify(payResult));

      if (payResult.code === 200 && payResult.data) {
        const payData = payResult.data;
        console.info('支付订单创建成功:', JSON.stringify(payData));

        this.isPendingConfirm = false;
        this.payStatus = 'WAITING';

        promptAction.showToast({
          message: '预约成功，请前往支付',
          duration: 1500
        });

        setTimeout(() => {
          router.pushUrl({
            url: 'pages/pay/PayPage',
            params: {
              payId: payData.payId,
              businessId: this.registerId,
              orderType: 'REGISTER',
              amount: this.pendingPrice,
              description: '挂号费 - ' + (this.pendingDepartment || ''),
              patientId: patientInfo.patientId,
              patientName: patientInfo.name,
              doctorName: this.pendingDoctorName,
              department: this.pendingDepartment,
              room: this.pendingRoom,
              returnUrl: 'pages/register/OrderSuccessPage'
            }
          }).then(() => {
            console.info('跳转支付页面成功');
            this.isSubmitting = false;
          }).catch((err: Error) => {
            console.error('跳转支付页面失败：' + JSON.stringify(err));
            this.isSubmitting = false;
            promptAction.showToast({
              message: '页面跳转失败，请重试',
              duration: 2000
            });
          });
        }, 1500);

      } else {
        const errorMsg = payResult.message || '创建支付订单失败';
        if (errorMsg.includes('已存在待支付订单')) {
          promptAction.showToast({
            message: '已有待支付订单，请继续支付',
            duration: 2000
          });
          await this.checkExistingPayOrder();
        } else {
          promptAction.showToast({
            message: errorMsg,
            duration: 2000
          });
        }
        this.isSubmitting = false;
      }

    } catch (error) {
      console.error('确认预约失败：' + JSON.stringify(error));
      promptAction.showToast({
        message: '网络请求失败，请稍后重试',
        duration: 2000
      });
      this.isSubmitting = false;
    }
  }

  private async checkExistingPayOrder() {
    try {
      const context = getContext(this);
      const result = await HttpUtil.get<ApiResponse<PayResultVo>>(
        this.payBaseUrl + '/pay/business?businessId=' + this.registerId + '&orderType=REGISTER',
        context
      );

      if (result.code === 200 && result.data) {
        const payResult = result.data;
        const patientInfo = PatientManager.getPatientInfo();

        router.pushUrl({
          url: 'pages/pay/PayPage',
          params: {
            payId: payResult.payId,
            businessId: this.registerId,
            orderType: 'REGISTER',
            amount: this.pendingPrice || this.orderInfo?.price || 0,
            description: '挂号费 - ' + (this.pendingDepartment || this.orderInfo?.department || ''),
            patientId: patientInfo?.patientId || '',
            patientName: patientInfo?.name || '',
            returnUrl: 'pages/register/OrderSuccessPage'
          }
        });
      }
    } catch (error) {
      console.error('查询已有支付订单失败：', JSON.stringify(error));
    }
  }

  async handlePay() {
    if (this.isSubmitting) return;

    if (this.payStatus === 'PAID') {
      promptAction.showToast({
        message: '该订单已支付，无需重复支付',
        duration: 2000
      });
      return;
    }

    if (this.payStatus === 'CANCELLED' || this.payStatus === 'REFUNDED') {
      promptAction.showToast({
        message: '该订单状态异常，无法支付',
        duration: 2000
      });
      return;
    }

    if (!this.orderInfo) return;

    this.isSubmitting = true;
    const context = getContext(this);

    try {
      const patientInfo = PatientManager.getPatientInfo();
      if (!patientInfo) {
        promptAction.showToast({
          message: '请先登录',
          duration: 2000
        });
        this.isSubmitting = false;
        return;
      }

      const payDto: UnifiedPayDto = {
        patientId: patientInfo.patientId,
        patientName: patientInfo.name,
        orderType: 'REGISTER',
        businessId: this.registerId,
        description: '挂号费 - ' + this.orderInfo.department,
        amount: this.orderInfo.price,
        payMethod: 'WECHAT'
      };

      console.info('创建支付订单请求:', JSON.stringify(payDto));

      const result = await HttpUtil.post<ApiResponse<PayResultVo>>(
        this.payBaseUrl + '/pay/create',
        payDto,
        context
      );

      console.info('创建支付订单返回：', JSON.stringify(result));

      if (result.code === 200 && result.data) {
        const payResult = result.data;
        console.info('支付订单创建成功:', JSON.stringify(payResult));

        router.pushUrl({
          url: 'pages/pay/PayPage',
          params: {
            payId: payResult.payId,
            businessId: this.registerId,
            orderType: 'REGISTER',
            amount: this.orderInfo.price,
            description: '挂号费 - ' + this.orderInfo.department,
            patientId: patientInfo.patientId,
            patientName: patientInfo.name,
            doctorName: this.pendingDoctorName,
            department: this.pendingDepartment,
            room: this.pendingRoom,
            returnUrl: 'pages/register/OrderSuccessPage'
          }
        }).then(() => {
          console.info('跳转支付页面成功');
          this.isSubmitting = false;
        }).catch((err: Error) => {
          console.error('跳转支付页面失败：' + JSON.stringify(err));
          this.isSubmitting = false;
          promptAction.showToast({
            message: '页面跳转失败，请重试',
            duration: 2000
          });
        });
      } else {
        const errorMsg = result.message || '创建支付订单失败';
        if (errorMsg.includes('已存在待支付订单')) {
          promptAction.showToast({
            message: '已有待支付订单，请继续支付',
            duration: 2000
          });
          await this.checkExistingPayOrder();
        } else if (errorMsg.includes('已完成支付')) {
          promptAction.showToast({
            message: '该订单已完成支付',
            duration: 2000
          });
          await this.loadOrderDetail();
        } else {
          promptAction.showToast({
            message: errorMsg,
            duration: 2000
          });
        }
        this.isSubmitting = false;
      }
    } catch (error) {
      console.error('创建支付订单失败：' + JSON.stringify(error));
      promptAction.showToast({
        message: '网络请求失败，请稍后重试',
        duration: 2000
      });
      this.isSubmitting = false;
    }
  }

  getPayStatusText(status: string): string {
    const map: Record<string, string> = {
      'WAITING': '待支付',
      'PAID': '已支付 ✓',
      'CANCELLED': '已取消 ✗',
      'REFUNDED': '已退款 ↩'
    };
    return map[status] || status;
  }

  getPayStatusColor(status: string): string {
    const map: Record<string, string> = {
      'WAITING': '#4A90D9',
      'PAID': '#2ECC71',
      'CANCELLED': '#95A5A6',
      'REFUNDED': '#95A5A6'
    };
    return map[status] || '#95A5A6';
  }

  getPayStatusBgColor(status: string): string {
    const map: Record<string, string> = {
      'WAITING': '#E8F0FE',
      'PAID': '#E8F8F0',
      'CANCELLED': '#F0F0F0',
      'REFUNDED': '#F0F0F0'
    };
    return map[status] || '#F0F0F0';
  }

  @Builder
  InfoItem(label: string, value: string, isPrice: boolean = false) {
    Row() {
      Text(label)
        .fontSize(13)
        .fontColor('#8E8E93')
        .width(80)
      Text(value)
        .fontSize(14)
        .fontColor(isPrice ? '#4A90D9' : '#1A1A2E')
        .layoutWeight(1)
        .fontWeight(isPrice ? FontWeight.Bold : FontWeight.Normal)
        .textAlign(TextAlign.End)
    }
    .width('100%')
    .padding({ top: 10, bottom: 10 })
    .border({ width: { bottom: 0.5 }, color: '#F0F0F0' })
  }

  build() {
    Column() {
      // 顶部导航
      Row() {
        Stack() {
          Row()
            .width(36)
            .height(36)
            .borderRadius(18)
            .backgroundColor('rgba(0,0,0,0.04)')
          Text('‹')
            .fontSize(22)
            .fontColor('#1A1A2E')
            .fontWeight(FontWeight.Medium)
        }
        .width(40)
        .height(40)
        .onClick(() => {
          router.back();
        })

        Text(this.isPendingConfirm ? '确认挂号信息' : '预约成功')
          .fontSize(18)
          .fontWeight(FontWeight.Medium)
          .fontColor('#1A1A2E')
          .layoutWeight(1)
          .textAlign(TextAlign.Center)

        Row().width(40).height(40)
      }
      .width('100%')
      .height(52)
      .padding({ left: 12, right: 12 })
      .backgroundColor('#FFFFFF')

      if (this.isLoading) {
        Column() {
          LoadingProgress()
            .width(40)
            .height(40)
            .color('#4A90D9')
          Text('加载中...')
            .fontSize(14)
            .fontColor('#8E8E93')
            .margin({ top: 12 })
        }
        .width('100%')
        .height('100%')
        .justifyContent(FlexAlign.Center)
        .backgroundColor('#F8F9FA')
      } else if (this.orderInfo) {
        Scroll() {
          Column() {
            // 柔和的状态提示卡片
            Column() {
              Row() {
                Text(this.isPendingConfirm ? '📋' : '✅')
                  .fontSize(22)
                  .margin({ right: 10 })

                Column() {
                  Text(this.isPendingConfirm ? '请确认挂号信息' : '预约成功')
                    .fontSize(16)
                    .fontWeight(FontWeight.Medium)
                    .fontColor('#1A1A2E')

                  if (!this.isPendingConfirm) {
                    Text('订单号：' + this.registerId)
                      .fontSize(12)
                      .fontColor('#8E8E93')
                      .margin({ top: 2 })
                  }
                }
                .alignItems(HorizontalAlign.Start)

                Blank()

                if (this.isPendingConfirm) {
                  Row() {
                    Circle()
                      .width(6)
                      .height(6)
                      .fill('#4A90D9')
                      .margin({ right: 4 })
                    Text('待确认')
                      .fontSize(11)
                      .fontColor('#4A90D9')
                      .fontWeight(FontWeight.Medium)
                  }
                  .padding({ left: 10, right: 10, top: 4, bottom: 4 })
                  .backgroundColor('#E8F0FE')
                  .borderRadius(12)
                }
              }
              .width('100%')

              Divider()
                .color('#F0F0F0')
                .margin({ top: 10, bottom: 10 })

              Text(this.isPendingConfirm ? '请核对以下信息，确认后创建挂号单' : '请按预约时间凭有效证件到院就诊')
                .fontSize(12)
                .fontColor('#8E8E93')
                .width('100%')
                .textAlign(TextAlign.Center)
            }
            .width('100%')
            .padding(14)
            .backgroundColor('#F8F9FA')
            .borderRadius(12)
            .margin({ bottom: 12 })

            // 预约详情卡片
            Column() {
              Text('📋 预约详情')
                .fontSize(15)
                .fontWeight(FontWeight.Medium)
                .fontColor('#1A1A2E')
                .margin({ bottom: 12 })
                .width('100%')

              this.InfoItem('就诊人', this.orderInfo.name || '待确认')
              this.InfoItem('就诊科室', this.orderInfo.department || this.pendingDepartment || '待获取')
              this.InfoItem('就诊医生', this.pendingDoctorName || '待确认')
              this.InfoItem('就诊日期', this.orderInfo.visitDate || '待确认')
              this.InfoItem('就诊时间', this.orderInfo.consultTime || '待确认')
              this.InfoItem('就诊诊室', this.orderInfo.consultRoom || this.pendingRoom || '待确认')
              this.InfoItem('挂号费', '¥' + (this.orderInfo.price || this.pendingPrice || 0).toFixed(2), true)

              if (!this.isPendingConfirm) {
                Row() {
                  Text('支付状态')
                    .fontSize(13)
                    .fontColor('#8E8E93')
                    .width(80)
                  Text(this.getPayStatusText(this.payStatus))
                    .fontSize(13)
                    .fontColor(this.getPayStatusColor(this.payStatus))
                    .padding({ left: 10, right: 10, top: 4, bottom: 4 })
                    .borderRadius(12)
                    .backgroundColor(this.getPayStatusBgColor(this.payStatus))
                    .layoutWeight(1)
                    .textAlign(TextAlign.End)
                }
                .width('100%')
                .padding({ top: 10, bottom: 10 })
                .border({ width: { bottom: 0.5 }, color: '#F0F0F0' })
              }
            }
            .width('100%')
            .padding(16)
            .backgroundColor('#FFFFFF')
            .borderRadius(16)
            .margin({ bottom: 80 })
            .shadow({
              radius: 8,
              color: 'rgba(0,0,0,0.04)',
              offsetY: 2
            })
          }
          .width('100%')
          .padding(16)
        }
        .scrollable(ScrollDirection.Vertical)
        .scrollBar(BarState.Off)
        .layoutWeight(1)
        .backgroundColor('#F8F9FA')

        // 底部按钮
        Column() {
          if (this.isPendingConfirm) {
            Button(this.isSubmitting ? '创建中...' : '确认预约')
              .width('100%')
              .height(48)
              .fontSize(16)
              .fontWeight(FontWeight.Medium)
              .fontColor('#FFFFFF')
              .backgroundColor(this.isSubmitting ? '#B2BEC3' : '#4A90D9')
              .borderRadius(24)
              .enabled(!this.isSubmitting)
              .onClick(() => {
                this.confirmRegister();
              })
              .margin({ bottom: 10 })

            Button('返回修改')
              .width('100%')
              .height(44)
              .fontSize(14)
              .fontColor('#8E8E93')
              .backgroundColor('transparent')
              .borderRadius(24)
              .border({ width: 0.5, color: '#D5D8DC' })
              .onClick(() => {
                router.back();
              })
          } else if (this.payStatus === 'WAITING') {
            Button(this.isSubmitting ? '创建中...' : '去支付 ¥' + (this.orderInfo?.price || 0).toFixed(2))
              .width('100%')
              .height(48)
              .fontSize(16)
              .fontWeight(FontWeight.Medium)
              .fontColor('#FFFFFF')
              .backgroundColor(this.isSubmitting ? '#B2BEC3' : '#4A90D9')
              .borderRadius(24)
              .enabled(!this.isSubmitting)
              .onClick(() => {
                this.handlePay();
              })
              .margin({ bottom: 10 })
          } else if (this.payStatus === 'PAID') {
            Row() {
              Text('✅ 已支付 ✓')
                .fontSize(16)
                .fontColor('#2ECC71')
                .fontWeight(FontWeight.Medium)
            }
            .width('100%')
            .padding(12)
            .backgroundColor('#E8F8F0')
            .borderRadius(12)
            .margin({ bottom: 10 })
            .justifyContent(FlexAlign.Center)
          } else if (this.payStatus === 'CANCELLED' || this.payStatus === 'REFUNDED') {
            Text('❌ 订单已' + this.getPayStatusText(this.payStatus))
              .fontSize(14)
              .fontColor('#95A5A6')
              .width('100%')
              .padding(12)
              .backgroundColor('#F0F0F0')
              .borderRadius(12)
              .textAlign(TextAlign.Center)
              .margin({ bottom: 10 })
          }

          Button('返回首页')
            .width('100%')
            .height(44)
            .fontSize(14)
            .fontColor('#4A90D9')
            .backgroundColor('transparent')
            .borderRadius(24)
            .border({ width: 0.5, color: '#4A90D9' })
            .onClick(() => {
              router.clear();
              router.pushUrl({
                url: 'pages/Index'
              });
            })
        }
        .width('100%')
        .padding(16)
        .backgroundColor('#FFFFFF')
        .shadow({
          radius: 12,
          color: 'rgba(0,0,0,0.08)',
          offsetY: -4
        })
      }
    }
    .width('100%')
    .height('100%')
    .backgroundColor('#FFFFFF')
  }
}
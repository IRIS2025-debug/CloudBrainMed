// pages/pay/PayPage.ets
import router from '@ohos.router';
import promptAction from '@ohos.promptAction';
import { HttpUtil } from '../common/HttpUtil';
import { APIConfig } from '../common/APIConfig';

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

interface EmptyRequest {}

const PayStatusText: Record<string, string> = {
  'WAITING': '待支付',
  'PAID': '已支付',
  'CANCELLED': '已取消',
  'REFUNDED': '已退款'
};

const PayStatusColor: Record<string, string> = {
  'WAITING': '#4A90D9',
  'PAID': '#2ECC71',
  'CANCELLED': '#95A5A6',
  'REFUNDED': '#95A5A6'
};

const PayStatusBgColor: Record<string, string> = {
  'WAITING': '#E8F0FE',
  'PAID': '#E8F8F0',
  'CANCELLED': '#F0F0F0',
  'REFUNDED': '#F0F0F0'
};

@Entry
@Component
export struct PayPage {
  @State payId: string = '';
  @State businessId: string = '';
  @State orderType: string = '';
  @State amount: number = 0;
  @State description: string = '';
  @State patientId: string = '';
  @State patientName: string = '';
  @State payMethod: string = 'WECHAT';
  @State isLoading: boolean = true;
  @State isPaying: boolean = false;
  @State payResult: PayResultVo | null = null;
  @State payStatus: string = 'WAITING';
  @State errorMessage: string = '';
  @State returnUrl: string = 'pages/register/OrderSuccessPage';

  private baseUrl: string = APIConfig.PAYMENT_SERVICE_URL;

  aboutToAppear() {
    const params = router.getParams() as Record<string, string>;
    console.info('PayPage 接收到的参数：', JSON.stringify(params));

    this.payId = params?.['payId'] || '';
    this.businessId = params?.['businessId'] || '';
    this.orderType = params?.['orderType'] || '';
    this.amount = Number(params?.['amount']) || 0;
    this.description = params?.['description'] || '';
    this.patientId = params?.['patientId'] || '';
    this.patientName = params?.['patientName'] || '';
    this.returnUrl = params?.['returnUrl'] || 'pages/register/OrderSuccessPage';

    console.info('PayPage 解析后：', {
      payId: this.payId,
      businessId: this.businessId,
      orderType: this.orderType,
      amount: this.amount,
      description: this.description,
      patientId: this.patientId,
      patientName: this.patientName,
      returnUrl: this.returnUrl
    });

    if (this.payId) {
      this.loadPayDetail();
    } else {
      this.isLoading = false;
      this.errorMessage = '支付信息不完整，缺少支付ID';
    }
  }

  async loadPayDetail() {
    this.isLoading = true;
    this.errorMessage = '';
    try {
      const context = getContext(this);
      const url = this.baseUrl + '/pay/' + this.payId;
      console.info('====== 请求支付详情 ======');
      console.info('完整URL：', url);

      const result = await HttpUtil.get<ApiResponse<PayResultVo>>(url, context);
      console.info('支付详情返回状态码：', result.code);
      console.info('支付详情返回数据：', JSON.stringify(result));

      if (result.code === 200 && result.data) {
        this.payResult = result.data;
        this.payStatus = result.data.payStatus;
        this.amount = result.data.totalAmount;
        this.description = result.data.description || this.description;
        this.patientName = result.data.patientName || this.patientName;
        console.info('支付详情加载成功');
      } else {
        this.errorMessage = result.message || '加载支付详情失败';
        console.error('加载支付详情失败：', result.message);
        promptAction.showToast({
          message: this.errorMessage,
          duration: 2000
        });
      }
    } catch (error) {
      console.error('加载支付详情异常：', JSON.stringify(error));
      const errorStr = JSON.stringify(error);
      if (errorStr.includes('CONNECTION_REFUSED') || errorStr.includes('ECONNREFUSED')) {
        this.errorMessage = '无法连接到支付服务，请检查网络或服务器是否启动';
      } else if (errorStr.includes('404')) {
        this.errorMessage = '支付接口不存在，请检查服务路径是否正确';
      } else {
        this.errorMessage = '网络请求失败，请稍后重试';
      }
      promptAction.showToast({
        message: this.errorMessage,
        duration: 3000
      });
    } finally {
      this.isLoading = false;
    }
  }

  async handlePay() {
    if (this.isPaying) return;

    if (this.payStatus === 'PAID') {
      promptAction.showToast({
        message: '该订单已支付，无需重复支付',
        duration: 2000
      });
      return;
    }

    if (!this.payId) {
      promptAction.showToast({
        message: '支付订单不存在',
        duration: 2000
      });
      return;
    }

    this.isPaying = true;
    this.isLoading = true;

    try {
      const context = getContext(this);

      promptAction.showToast({
        message: '正在处理支付...',
        duration: 2000
      });

      setTimeout(async () => {
        try {
          const checkResult = await HttpUtil.get<ApiResponse<PayResultVo>>(
            this.baseUrl + '/pay/' + this.payId,
            context
          );

          if (checkResult.code === 200 && checkResult.data) {
            if (checkResult.data.payStatus === 'PAID') {
              promptAction.showToast({
                message: '该订单已支付完成',
                duration: 2000
              });
              this.payStatus = 'PAID';
              this.payResult = checkResult.data;
              this.isPaying = false;
              this.isLoading = false;
              return;
            }

            if (checkResult.data.payStatus !== 'WAITING') {
              promptAction.showToast({
                message: '订单状态异常，无法支付',
                duration: 2000
              });
              this.isPaying = false;
              this.isLoading = false;
              return;
            }
          }

          const url = this.baseUrl + '/pay/success/' + this.payId;
          console.info('====== 调用支付回调 ======');
          console.info('完整URL：', url);

          const emptyData: EmptyRequest = {};
          const result = await HttpUtil.post<ApiResponse<boolean>>(
            url,
            emptyData,
            context
          );
          console.info('支付回调返回状态码：', result.code);
          console.info('支付回调返回数据：', JSON.stringify(result));

          if (result.code === 200 && result.data) {
            const detailResult = await HttpUtil.get<ApiResponse<PayResultVo>>(
              this.baseUrl + '/pay/' + this.payId,
              context
            );
            if (detailResult.code === 200 && detailResult.data) {
              this.payResult = detailResult.data;
              this.payStatus = detailResult.data.payStatus;
            }

            promptAction.showToast({
              message: '支付成功！',
              duration: 2000
            });

            setTimeout(() => {
              router.pushUrl({
                url: this.returnUrl,
                params: {
                  refresh: 'true',
                  registerId: this.businessId
                }
              }).then(() => {
                console.info('返回订单详情页成功');
              }).catch((err: Error) => {
                console.error('返回订单详情页失败：' + JSON.stringify(err));
                router.back();
              });
            }, 1500);
          } else {
            const errorMsg = result.message || '支付失败';
            console.error('支付失败：', errorMsg);
            promptAction.showToast({
              message: errorMsg,
              duration: 2000
            });
          }
        } catch (error) {
          console.error('支付回调异常：', JSON.stringify(error));
          promptAction.showToast({
            message: '支付结果确认失败，请检查网络',
            duration: 2000
          });
        } finally {
          this.isLoading = false;
          this.isPaying = false;
        }
      }, 2000);

    } catch (error) {
      console.error('支付异常：', JSON.stringify(error));
      promptAction.showToast({
        message: '支付失败，请稍后重试',
        duration: 2000
      });
      this.isLoading = false;
      this.isPaying = false;
    }
  }

  async cancelPay() {
    if (!this.payId) return;

    try {
      const context = getContext(this);
      const url = this.baseUrl + '/pay/cancel/' + this.payId;
      console.info('====== 取消支付 ======');
      console.info('完整URL：', url);

      const emptyData: EmptyRequest = {};
      const result = await HttpUtil.post<ApiResponse<boolean>>(url, emptyData, context);
      console.info('取消支付返回：', JSON.stringify(result));

      if (result.code === 200 && result.data) {
        this.payStatus = 'CANCELLED';
        promptAction.showToast({
          message: '已取消支付',
          duration: 2000
        });
        setTimeout(() => {
          router.back();
        }, 500);
      } else {
        promptAction.showToast({
          message: result.message || '取消支付失败',
          duration: 2000
        });
      }
    } catch (error) {
      console.error('取消支付异常：', JSON.stringify(error));
      promptAction.showToast({
        message: '取消支付失败，请稍后重试',
        duration: 2000
      });
    }
  }

  getStatusText(): string {
    return PayStatusText[this.payStatus] || '未知状态';
  }

  getStatusColor(): string {
    return PayStatusColor[this.payStatus] || '#95A5A6';
  }

  getStatusBgColor(): string {
    return PayStatusBgColor[this.payStatus] || '#F0F0F0';
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

        Text('支付确认')
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
          Text('处理中...')
            .fontSize(14)
            .fontColor('#8E8E93')
            .margin({ top: 12 })
        }
        .width('100%')
        .height('100%')
        .justifyContent(FlexAlign.Center)
        .backgroundColor('#F8F9FA')
      } else if (this.errorMessage) {
        Column() {
          Text('⚠️')
            .fontSize(48)
            .margin({ bottom: 12 })
          Text(this.errorMessage)
            .fontSize(16)
            .fontColor('#666666')
            .textAlign(TextAlign.Center)
            .padding({ left: 20, right: 20 })
          Button('返回')
            .width(120)
            .height(40)
            .fontSize(14)
            .fontColor('#FFFFFF')
            .backgroundColor('#4A90D9')
            .borderRadius(20)
            .margin({ top: 20 })
            .onClick(() => {
              router.back();
            })
        }
        .width('100%')
        .height('100%')
        .justifyContent(FlexAlign.Center)
        .backgroundColor('#F8F9FA')
      } else {
        Scroll() {
          Column() {
            // 支付金额卡片
            Column() {
              Text('¥' + this.amount.toFixed(2))
                .fontSize(44)
                .fontWeight(FontWeight.Bold)
                .fontColor('#1A1A2E')
                .margin({ top: 16 })

              Text('待支付金额')
                .fontSize(13)
                .fontColor('#8E8E93')
                .margin({ top: 6 })
            }
            .width('100%')
            .padding({ top: 24, bottom: 24 })
            .backgroundColor('#FFFFFF')
            .borderRadius(16)
            .margin({ bottom: 12 })
            .shadow({
              radius: 8,
              color: 'rgba(0,0,0,0.04)',
              offsetY: 2
            })

            // 支付信息卡片
            Column() {
              this.InfoItem('📋 支付项目', this.description || '未知项目')
              this.InfoItem('👤 患者姓名', this.patientName)
              this.InfoItem('📌 支付状态', this.getStatusText(), false, this.getStatusColor())
              if (this.payId) {
                this.InfoItem('🔢 订单号', this.payId)
              }
            }
            .width('100%')
            .padding(16)
            .backgroundColor('#FFFFFF')
            .borderRadius(16)
            .margin({ bottom: 12 })
            .shadow({
              radius: 8,
              color: 'rgba(0,0,0,0.04)',
              offsetY: 2
            })

            // 支付方式卡片
            Column() {
              Text('选择支付方式')
                .fontSize(15)
                .fontWeight(FontWeight.Medium)
                .fontColor('#1A1A2E')
                .margin({ bottom: 12 })
                .width('100%')

              this.PayMethodItem('WECHAT', '微信支付', '💚')
              this.PayMethodItem('ALIPAY', '支付宝', '💙')
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

            // 按钮区域 - 使用绝对定位到页面底部
          }
          .width('100%')
          .padding(16)
        }
        .scrollable(ScrollDirection.Vertical)
        .scrollBar(BarState.Off)
        .layoutWeight(1)
        .backgroundColor('#F8F9FA')

        // 底部按钮 - 固定在底部
        Column() {
          if (this.payStatus === 'WAITING') {
            Button(this.isPaying ? '支付中...' : '确认支付')
              .width('100%')
              .height(48)
              .fontSize(16)
              .fontWeight(FontWeight.Medium)
              .fontColor('#FFFFFF')
              .backgroundColor(this.isPaying ? '#B2BEC3' : '#4A90D9')
              .borderRadius(24)
              .enabled(!this.isPaying)
              .onClick(() => {
                this.handlePay();
              })
              .margin({ bottom: 10 })

            Button('取消支付')
              .width('100%')
              .height(44)
              .fontSize(14)
              .fontColor('#8E8E93')
              .backgroundColor('transparent')
              .borderRadius(24)
              .border({ width: 0.5, color: '#D5D8DC' })
              .onClick(() => {
                this.cancelPay();
              })
          } else if (this.payStatus === 'PAID') {
            Column() {
              Row() {
                Text('✅')
                  .fontSize(18)
                  .margin({ right: 8 })
                Text('支付已完成')
                  .fontSize(16)
                  .fontColor('#2ECC71')
                  .fontWeight(FontWeight.Medium)
              }
              .justifyContent(FlexAlign.Center)
              .padding(12)
              .backgroundColor('#E8F8F0')
              .borderRadius(12)
              .margin({ bottom: 12 })
              .width('100%')

              if (this.payResult?.payTime) {
                Text('支付时间：' + this.payResult.payTime)
                  .fontSize(12)
                  .fontColor('#8E8E93')
                  .margin({ bottom: 12 })
                  .width('100%')
                  .textAlign(TextAlign.Center)
              }

              Button('返回')
                .width('100%')
                .height(48)
                .fontSize(16)
                .fontColor('#FFFFFF')
                .backgroundColor('#4A90D9')
                .borderRadius(24)
                .onClick(() => {
                  router.pushUrl({
                    url: this.returnUrl,
                    params: {
                      refresh: 'true',
                      registerId: this.businessId
                    }
                  }).catch(() => {
                    router.back();
                  });
                })
            }
            .width('100%')
          } else if (this.payStatus === 'CANCELLED') {
            Column() {
              Row() {
                Text('✖️')
                  .fontSize(18)
                  .margin({ right: 8 })
                Text('已取消支付')
                  .fontSize(16)
                  .fontColor('#95A5A6')
                  .fontWeight(FontWeight.Medium)
              }
              .justifyContent(FlexAlign.Center)
              .padding(12)
              .backgroundColor('#F0F0F0')
              .borderRadius(12)
              .margin({ bottom: 12 })
              .width('100%')

              Button('返回')
                .width('100%')
                .height(48)
                .fontSize(16)
                .fontColor('#FFFFFF')
                .backgroundColor('#4A90D9')
                .borderRadius(24)
                .onClick(() => {
                  router.back();
                })
            }
            .width('100%')
          }
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
    .backgroundColor('#F8F9FA')
  }

  @Builder
  InfoItem(label: string, value: string, isPrice: boolean = false, color: string = '') {
    Row() {
      Text(label)
        .fontSize(13)
        .fontColor('#8E8E93')
        .width(100)
      Text(value)
        .fontSize(14)
        .fontColor(color || (isPrice ? '#4A90D9' : '#1A1A2E'))
        .layoutWeight(1)
        .fontWeight(isPrice ? FontWeight.Bold : FontWeight.Normal)
        .textAlign(TextAlign.End)
    }
    .width('100%')
    .padding({ top: 10, bottom: 10 })
    .border({ width: { bottom: 0.5 }, color: '#F0F0F0' })
  }

  @Builder
  PayMethodItem(method: string, name: string, icon: string) {
    Row() {
      Text(icon)
        .fontSize(22)
        .margin({ right: 12 })
      Text(name)
        .fontSize(14)
        .fontColor('#1A1A2E')
        .layoutWeight(1)
      if (this.payMethod === method) {
        Stack() {
          Circle()
            .width(22)
            .height(22)
            .fill('#4A90D9')
          Text('✓')
            .fontSize(12)
            .fontColor('#FFFFFF')
            .fontWeight(FontWeight.Bold)
        }
        .width(22)
        .height(22)
      } else {
        Circle()
          .width(22)
          .height(22)
          .fill('transparent')
          .borderWidth(1.5)
          .borderColor('#D5D8DC')
      }
    }
    .width('100%')
    .padding(12)
    .borderRadius(10)
    .backgroundColor(this.payMethod === method ? '#E8F0FE' : 'transparent')
    .onClick(() => {
      this.payMethod = method;
    })
    .margin({ bottom: 8 })
  }
}
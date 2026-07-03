// pages/pay/PayPage.ets
import router from '@ohos.router';
import promptAction from '@ohos.promptAction';
import { HttpUtil } from '../common/HttpUtil';
import { APIConfig } from '../common/APIConfig';
import { PatientManager } from '../common/PatientManager';

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

// 定义空请求体类型
interface EmptyRequest {
  // 空接口，用于POST请求没有请求体的情况
}

const PayStatusText: Record<string, string> = {
  'WAITING': '待支付',
  'PAID': '已支付',
  'CANCELLED': '已取消',
  'REFUNDED': '已退款'
};

const PayStatusColor: Record<string, string> = {
  'WAITING': '#FAAD14',
  'PAID': '#52C41A',
  'CANCELLED': '#FF4D4F',
  'REFUNDED': '#999999'
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

  private baseUrl: string = APIConfig.PATIENT_SERVICE_URL;

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

    console.info('PayPage 解析后：', {
      payId: this.payId,
      businessId: this.businessId,
      orderType: this.orderType,
      amount: this.amount,
      description: this.description,
      patientId: this.patientId,
      patientName: this.patientName
    });


    if (this.payId) {
      this.loadPayDetail();
    } else {
      this.isLoading = false;
    }
  }

  async loadPayDetail() {
    this.isLoading = true;
    try {
      const context = getContext(this);
      const result = await HttpUtil.get<ApiResponse<PayResultVo>>(
        this.baseUrl + '/pay/' + this.payId,
        context
      );
      if (result.code === 200 && result.data) {
        this.payResult = result.data;
        this.payStatus = result.data.payStatus;
        this.amount = result.data.totalAmount;
        this.description = result.data.description || this.description;
        this.patientName = result.data.patientName || this.patientName;
      } else {
        promptAction.showToast({
          message: result.message || '加载支付详情失败',
          duration: 2000
        });
      }
    } catch (error) {
      console.error('加载支付详情失败：' + JSON.stringify(error));
    } finally {
      this.isLoading = false;
    }
  }

  async handlePay() {
    if (this.isPaying) return;
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
          // 调用支付成功回调 - 传递空对象
          const emptyData: EmptyRequest = {};
          const result = await HttpUtil.post<ApiResponse<boolean>>(
            this.baseUrl + '/pay/success/' + this.payId,
            emptyData,
            context
          );

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
          } else {
            promptAction.showToast({
              message: result.message || '支付失败',
              duration: 2000
            });
          }
        } catch (error) {
          console.error('支付回调失败：' + JSON.stringify(error));
          promptAction.showToast({
            message: '支付结果确认失败',
            duration: 2000
          });
        } finally {
          this.isLoading = false;
          this.isPaying = false;
        }
      }, 2000);

    } catch (error) {
      console.error('支付失败：' + JSON.stringify(error));
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
      // 传递空对象
      const emptyData: EmptyRequest = {};
      const result = await HttpUtil.post<ApiResponse<boolean>>(
        this.baseUrl + '/pay/cancel/' + this.payId,
        emptyData,
        context
      );
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
      console.error('取消支付失败：' + JSON.stringify(error));
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
    return PayStatusColor[this.payStatus] || '#999999';
  }

  build() {
    Column() {
      Row() {
        Text('←')
          .fontSize(24)
          .onClick(() => {
            router.back();
          })
        Text('支付确认')
          .fontSize(20)
          .fontWeight(FontWeight.Bold)
          .layoutWeight(1)
          .textAlign(TextAlign.Center)
        Blank().width(24)
      }
      .width('100%')
      .padding(16)
      .backgroundColor('#FFFFFF')

      if (this.isLoading) {
        Column() {
          LoadingProgress()
            .width(60)
            .height(60)
            .color('#007AFF')
          Text('处理中...')
            .fontSize(14)
            .fontColor('#999')
            .margin({ top: 16 })
        }
        .width('100%')
        .height('100%')
        .justifyContent(FlexAlign.Center)
      } else {
        Scroll() {
          Column() {
            Column() {
              Text('¥' + this.amount.toFixed(2))
                .fontSize(48)
                .fontWeight(FontWeight.Bold)
                .fontColor('#333333')
                .margin({ top: 20 })

              Text('待支付金额')
                .fontSize(14)
                .fontColor('#999999')
                .margin({ top: 8 })
            }
            .width('100%')
            .padding(20)
            .backgroundColor('#FFFFFF')
            .borderRadius(12)
            .margin({ bottom: 16 })

            Column() {
              this.InfoItem('支付项目', this.description || '未知项目')
              this.InfoItem('患者姓名', this.patientName)
              this.InfoItem('支付状态', this.getStatusText(), false, this.getStatusColor())
              if (this.payId) {
                this.InfoItem('订单号', this.payId)
              }
            }
            .width('100%')
            .padding(16)
            .backgroundColor('#F8F9FA')
            .borderRadius(12)
            .margin({ bottom: 16 })

            Column() {
              Text('选择支付方式')
                .fontSize(16)
                .fontWeight(FontWeight.Medium)
                .fontColor('#333333')
                .margin({ bottom: 12 })
                .width('100%')

              this.PayMethodItem('WECHAT', '微信支付', '💚')
              this.PayMethodItem('ALIPAY', '支付宝', '💙')
            }
            .width('100%')
            .padding(16)
            .backgroundColor('#FFFFFF')
            .borderRadius(12)
            .margin({ bottom: 16 })

            if (this.payStatus === 'WAITING') {
              Button(this.isPaying ? '支付中...' : '确认支付 ¥' + this.amount.toFixed(2))
                .width('100%')
                .height(50)
                .fontSize(17)
                .fontWeight(FontWeight.Medium)
                .fontColor('#FFFFFF')
                .backgroundColor(this.isPaying ? '#999999' : '#007AFF')
                .borderRadius(24)
                .enabled(!this.isPaying)
                .onClick(() => {
                  this.handlePay();
                })
                .margin({ bottom: 12 })

              Button('取消支付')
                .width('100%')
                .height(44)
                .fontSize(15)
                .fontColor('#999999')
                .backgroundColor('#F5F5F5')
                .borderRadius(24)
                .onClick(() => {
                  this.cancelPay();
                })
            } else if (this.payStatus === 'PAID') {
              Column() {
                Text('✅ 支付已完成')
                  .fontSize(18)
                  .fontColor('#52C41A')
                  .fontWeight(FontWeight.Bold)

                if (this.payResult?.payTime) {
                  Text('支付时间：' + this.payResult.payTime)
                    .fontSize(14)
                    .fontColor('#999999')
                    .margin({ top: 8 })
                }

                Button('返回')
                  .width('100%')
                  .height(44)
                  .fontSize(15)
                  .fontColor('#FFFFFF')
                  .backgroundColor('#007AFF')
                  .borderRadius(24)
                  .margin({ top: 16 })
                  .onClick(() => {
                    router.back();
                  })
              }
              .width('100%')
              .padding(20)
              .backgroundColor('#F6FFED')
              .borderRadius(12)
              .margin({ bottom: 16 })
            } else if (this.payStatus === 'CANCELLED') {
              Column() {
                Text('❌ 已取消支付')
                  .fontSize(18)
                  .fontColor('#FF4D4F')
                  .fontWeight(FontWeight.Bold)

                Button('返回')
                  .width('100%')
                  .height(44)
                  .fontSize(15)
                  .fontColor('#FFFFFF')
                  .backgroundColor('#007AFF')
                  .borderRadius(24)
                  .margin({ top: 16 })
                  .onClick(() => {
                    router.back();
                  })
              }
              .width('100%')
              .padding(20)
              .backgroundColor('#FFF2F0')
              .borderRadius(12)
              .margin({ bottom: 16 })
            }
          }
          .width('100%')
          .padding(16)
        }
        .scrollable(ScrollDirection.Vertical)
        .scrollBar(BarState.Off)
        .layoutWeight(1)
      }
    }
    .width('100%')
    .height('100%')
    .backgroundColor('#F5F5F5')
  }

  @Builder
  InfoItem(label: string, value: string, isPrice: boolean = false, color: string = '') {
    Row() {
      Text(label)
        .fontSize(14)
        .fontColor('#666666')
        .width(100)
      Text(value)
        .fontSize(14)
        .fontColor(color || (isPrice ? '#FF6B6B' : '#333333'))
        .layoutWeight(1)
        .fontWeight(isPrice ? FontWeight.Bold : FontWeight.Normal)
        .textAlign(TextAlign.End)
    }
    .width('100%')
    .padding({ top: 8, bottom: 8 })
    .border({ width: { bottom: 0.5 }, color: '#E5E5E5' })
  }

  @Builder
  PayMethodItem(method: string, name: string, icon: string) {
    Row() {
      Text(icon)
        .fontSize(24)
        .margin({ right: 12 })
      Text(name)
        .fontSize(15)
        .fontColor('#333333')
        .layoutWeight(1)
      if (this.payMethod === method) {
        Circle()
          .width(20)
          .height(20)
          .fill('#007AFF')
        Text('✓')
          .fontSize(12)
          .fontColor('#FFFFFF')
          .position({ x: 4, y: 4 })
      } else {
        Circle()
          .width(20)
          .height(20)
          .fill('#E5E5E5')
      }
    }
    .width('100%')
    .padding(12)
    .borderRadius(8)
    .backgroundColor(this.payMethod === method ? '#F0F7FF' : 'transparent')
    .onClick(() => {
      this.payMethod = method;
    })
    .margin({ bottom: 8 })
  }
}
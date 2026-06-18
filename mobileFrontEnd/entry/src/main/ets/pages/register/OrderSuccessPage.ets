// pages/register/OrderSuccessPage.ets
import router from '@ohos.router';
import { HttpUtil } from '../common/HttpUtil';
import promptAction from '@ohos.promptAction';

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
  private baseUrl: string = 'http://192.168.25.143:8004/patient-service';

  aboutToAppear() {
    const params = router.getParams() as Record<string, string>;
    this.registerId = params?.['registerId'] || '';
    this.loadOrderDetail();
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

  getPayStatusText(status: string): string {
    if (status === 'WAITING') {
      return '待支付';
    } else if (status === 'PAID') {
      return '已支付';
    } else if (status === 'CANCELLED') {
      return '已取消';
    }
    return status;
  }

  getPayStatusColor(status: string): string {
    if (status === 'WAITING') {
      return '#FAAD14';
    } else if (status === 'PAID') {
      return '#52C41A';
    } else if (status === 'CANCELLED') {
      return '#FF4D4F';
    }
    return '#999999';
  }

  build() {
    Column() {
      if (this.isLoading) {
        Column() {
          LoadingProgress()
            .width(60)
            .height(60)
            .color('#007AFF')
          Text('加载中...')
            .fontSize(14)
            .fontColor('#999')
            .margin({ top: 16 })
        }
        .width('100%')
        .height('100%')
        .justifyContent(FlexAlign.Center)
      } else if (this.orderInfo) {
        Scroll() {
          Column() {
            // 成功图标
            Column() {
              Circle()
                .width(80)
                .height(80)
                .fill('#52C41A20')
              Text('✓')
                .fontSize(48)
                .fontColor('#52C41A')
                .position({ x: 16, y: 16 })
            }
            .width(80)
            .height(80)
            .margin({ top: 40 })

            Text('预约成功')
              .fontSize(24)
              .fontWeight(FontWeight.Bold)
              .fontColor('#333333')
              .margin({ top: 24 })

            Text('订单号：' + this.registerId)
              .fontSize(14)
              .fontColor('#999999')
              .margin({ top: 8 })

            Text('请按预约时间凭有效证件到院就诊')
              .fontSize(14)
              .fontColor('#666666')
              .margin({ top: 16 })

            // 订单信息卡片
            Column() {
              this.InfoItem('就诊人', this.orderInfo.name)
              this.InfoItem('就诊科室', this.orderInfo.department)
              this.InfoItem('就诊日期', this.orderInfo.visitDate)
              this.InfoItem('就诊时间', this.orderInfo.consultTime)
              this.InfoItem('就诊诊室', this.orderInfo.consultRoom)
              this.InfoItem('挂号费', '¥' + this.orderInfo.price, true)
              this.InfoItem('支付状态', this.getPayStatusText(this.orderInfo.payStatus), false, this.getPayStatusColor(this.orderInfo.payStatus))
            }
            .width('90%')
            .padding(16)
            .backgroundColor('#F8F9FA')
            .borderRadius(12)
            .margin({ top: 24 })

            // 按钮组
            Column() {
              Button('查看订单详情')
                .width('100%')
                .height(48)
                .fontSize(16)
                .fontWeight(FontWeight.Medium)
                .fontColor('#FFFFFF')
                .backgroundColor('#007AFF')
                .borderRadius(24)
                .onClick(() => {
                  router.pushUrl({
                    url: 'pages/profile/OrderDetailPage',
                    params: { registerId: this.registerId }
                  });
                })

              Button('返回首页')
                .width('100%')
                .height(48)
                .fontSize(16)
                .fontWeight(FontWeight.Medium)
                .fontColor('#007AFF')
                .backgroundColor('#F0F0F0')
                .borderRadius(24)
                .margin({ top: 12 })
                .onClick(() => {
                  router.clear();
                  router.pushUrl({ url: 'pages/Index' });
                })
            }
            .width('80%')
            .margin({ top: 40, bottom: 40 })
          }
          .width('100%')
        }
        .scrollable(ScrollDirection.Vertical)
        .scrollBar(BarState.Off)
        .layoutWeight(1)
      }
    }
    .width('100%')
    .height('100%')
    .backgroundColor('#FFFFFF')
  }

  @Builder
  InfoItem(label: string, value: string, isPrice: boolean = false, color: string = '') {
    Row() {
      Text(label)
        .fontSize(14)
        .fontColor('#666666')
        .width(80)
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
}
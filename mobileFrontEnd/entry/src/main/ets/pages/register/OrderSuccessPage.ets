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

// 统一支付请求DTO
interface UnifiedPayDto {
  patientId: string;
  patientName: string;
  orderType: string;
  businessId: string;
  description?: string;
  amount: number;
  payMethod?: string;
}

// 支付结果VO
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
  @State isPaying: boolean = false;
  private baseUrl: string = APIConfig.PATIENT_SERVICE_URL;
  private payBaseUrl: string = APIConfig.PAYMENT_SERVICE_URL;

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
        this.ensurePatientInfo();
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

  /**
   * 创建支付订单并跳转到支付页面
   */
  async handlePay() {
    if (this.isPaying) return;
    if (!this.orderInfo) return;

    this.isPaying = true;
    const context = getContext(this);

    try {
      // 获取患者信息
      const patientInfo = PatientManager.getPatientInfo();
      if (!patientInfo) {
        promptAction.showToast({
          message: '请先登录',
          duration: 2000
        });
        return;
      }

      // 构建支付请求
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

      // 调用后端创建支付订单
      const result = await HttpUtil.post<ApiResponse<PayResultVo>>(
        this.payBaseUrl + '/pay/create',
        payDto,
        context
      );

      console.info('创建支付订单返回：', JSON.stringify(result));

      if (result.code === 200 && result.data) {
        const payResult = result.data;
        console.info('支付订单创建成功:', JSON.stringify(payResult));

        // OrderSuccessPage.handlePay() 中跳转部分
        console.info('准备跳转到支付页面，参数：', JSON.stringify({
          payId: payResult.payId,
          businessId: this.registerId,
          orderType: 'REGISTER',
          amount: this.orderInfo.price,
          description: '挂号费 - ' + this.orderInfo.department,
          patientId: patientInfo.patientId,
          patientName: patientInfo.name
        }));

        // 跳转到支付页面
        router.pushUrl({
          url: 'pages/pay/PayPage',
          params: {
            payId: payResult.payId,
            businessId: this.registerId,
            orderType: 'REGISTER',
            amount: this.orderInfo.price,
            description: '挂号费 - ' + this.orderInfo.department,
            patientId: patientInfo.patientId,
            patientName: patientInfo.name
          }
        }).then(() => {
          console.info('跳转支付页面成功');
        }).catch((err : Error) => {
          console.error('跳转支付页面失败：' + JSON.stringify(err));
          promptAction.showToast({
            message: '页面跳转失败：' + err.message,
            duration: 3000
          });
        });
      } else {
        console.error('创建支付订单失败，code:', result.code, 'message:', result.message);
        promptAction.showToast({
          message: result.message || '创建支付订单失败',
          duration: 2000
        });
      }
    } catch (error) {
      console.error('创建支付订单失败：' + JSON.stringify(error));
      promptAction.showToast({
        message: '网络请求失败，请稍后重试',
        duration: 2000
      });
    } finally {
      this.isPaying = false;
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
              // 如果支付状态是待支付，显示"去支付"按钮
              if (this.orderInfo.payStatus === 'WAITING') {
                Button(this.isPaying ? '创建订单中...' : '去支付 ¥' + this.orderInfo.price.toFixed(2))
                  .width('100%')
                  .height(48)
                  .fontSize(16)
                  .fontWeight(FontWeight.Medium)
                  .fontColor('#FFFFFF')
                  .backgroundColor(this.isPaying ? '#999999' : '#007AFF')
                  .borderRadius(24)
                  .enabled(!this.isPaying)
                  .onClick(() => {
                    this.handlePay();
                  })
              } else if (this.orderInfo.payStatus === 'PAID') {
                Text('✅ 已支付')
                  .fontSize(16)
                  .fontColor('#52C41A')
                  .width('100%')
                  .textAlign(TextAlign.Center)
                  .padding(12)
              }

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
                  router.pushUrl({
                    url: 'pages/Index'
                  });
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
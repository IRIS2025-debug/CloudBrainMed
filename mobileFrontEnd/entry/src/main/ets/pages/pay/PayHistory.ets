// pages/pay/PayHistory.ets
import router from '@ohos.router';
import promptAction from '@ohos.promptAction';
import { HttpUtil } from '../common/HttpUtil';
import { APIConfig } from '../common/APIConfig';

// ==================== 类型定义 ====================

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

// ==================== 常量定义 ====================

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

const OrderTypeText: Record<string, string> = {
  'REGISTER': '挂号费',
  'MEDICAL': '检查费',
  'PRESCRIPTION': '药品费'
};

// ==================== 页面组件 ====================

@Entry
@Component
export struct PayHistory {
  @State currentTab: number = 0; // 0: 待支付, 1: 历史记录
  @State pendingPayments: PayResultVo[] = [];
  @State historyPayments: PayResultVo[] = [];
  @State isLoading: boolean = true;
  @State patientId: string = '';
  @State patientName: string = '';
  @State patientPhone: string = '';
  @State patientGender: number = 0;
  @State patientGenderText: string = '';
  @State patientAge: number = 0;

  private baseUrl: string = APIConfig.PATIENT_SERVICE_URL;

  aboutToAppear(): void {
    const params = router.getParams() as Record<string, Object>;
    console.info('PayHistory 接收到的参数：', JSON.stringify(params));

    if (params) {
      this.patientId = (params['patientId'] as string) || '';
      this.patientName = (params['patientName'] as string) ||
        (params['userName'] as string) ||
        (params['name'] as string) || '';
      this.patientPhone = (params['patientPhone'] as string) || (params['userPhone'] as string) || '';
      this.patientGender = (params['patientGender'] as number) || (params['userGender'] as number) || 0;
      this.patientGenderText = (params['patientGenderText'] as string) ||
        (params['userGenderText'] as string) ||
        (this.patientGender === 1 ? '男' : this.patientGender === 2 ? '女' : '未知');
      this.patientAge = (params['patientAge'] as number) || (params['userAge'] as number) || 0;
    }

    console.info('PayHistory 解析后 patientName:', this.patientName);

    this.loadPayments();
  }

  /**
   * 加载缴费记录
   */
  async loadPayments(): Promise<void> {
    this.isLoading = true;
    try {
      const context = getContext(this);
      const fullUrl: string = `${this.baseUrl}/profile/payments`;
      console.info('请求缴费记录: ' + fullUrl);

      const result = await HttpUtil.get<ApiResponse<PayResultVo[]>>(fullUrl, context);
      console.info('缴费记录响应:', JSON.stringify(result));

      if (result.code === 200 && result.data) {
        const data: PayResultVo[] = result.data;

        if (data.length > 0) {
          console.info('第一条缴费记录:', JSON.stringify(data[0]));
        }

        // 如果后端返回的 patientName 为空，使用前端的 patientName
        const processedData = data.map((item: PayResultVo) => {
          if (!item.patientName || item.patientName === '') {
            item.patientName = this.patientName || '未知患者';
          }
          return item;
        });

        // 前端分类：待支付和历史记录
        this.pendingPayments = processedData.filter((item: PayResultVo) =>
        item.payStatus === 'WAITING' || item.payStatus === '待支付'
        );
        this.historyPayments = processedData.filter((item: PayResultVo) =>
        item.payStatus !== 'WAITING' && item.payStatus !== '待支付'
        );
        console.info(`缴费记录加载成功: 待支付 ${this.pendingPayments.length} 条, 历史 ${this.historyPayments.length} 条`);
      } else {
        console.error('接口返回失败:', result.message || '未知错误');
        promptAction.showToast({
          message: result.message || '加载缴费记录失败',
          duration: 2000
        });
        // 清空数据，显示空状态
        this.pendingPayments = [];
        this.historyPayments = [];
      }
    } catch (error) {
      console.error('网络请求异常:', JSON.stringify(error));
      promptAction.showToast({
        message: '网络异常，请稍后重试',
        duration: 2000
      });
      this.pendingPayments = [];
      this.historyPayments = [];
    } finally {
      this.isLoading = false;
    }
  }

  /**
   * 跳转到支付详情页
   */
  goToPayDetail(payItem: PayResultVo): void {
    router.pushUrl({
      url: 'pages/pay/PayPage',
      params: {
        payId: payItem.payId,
        businessId: payItem.businessId,
        orderType: payItem.orderType,
        amount: payItem.totalAmount,
        description: payItem.description || OrderTypeText[payItem.orderType] || '未知项目',
        patientId: this.patientId,
        patientName: payItem.patientName || this.patientName || '未知患者'
      }
    }).catch((err: Error) => {
      console.error('跳转支付详情失败：' + JSON.stringify(err));
      promptAction.showToast({
        message: '跳转失败，请稍后重试',
        duration: 1500
      });
    });
  }

  /**
   * 返回上一页
   */
  goBack(): void {
    router.back();
  }

  /**
   * 获取状态颜色
   */
  private getStatusColor(status: string): string {
    return PayStatusColor[status] || '#999999';
  }

  /**
   * 获取状态文本
   */
  private getStatusText(status: string): string {
    return PayStatusText[status] || status;
  }

  /**
   * 获取订单类型文本
   */
  private getOrderTypeText(orderType: string): string {
    return OrderTypeText[orderType] || orderType;
  }

  /**
   * 格式化金额
   */
  private formatAmount(amount: number): string {
    return amount.toFixed(2);
  }

  // ==================== UI构建 ====================

  @Builder
  PayItemCard(item: PayResultVo) {
    Column() {
      Row() {
        Column() {
          Row() {
            Text(this.getOrderTypeText(item.orderType))
              .fontSize(16)
              .fontWeight(FontWeight.Medium)
              .fontColor('#333333')

            Text(this.getStatusText(item.payStatus))
              .fontSize(12)
              .fontColor(this.getStatusColor(item.payStatus))
              .padding({ left: 8, right: 8, top: 2, bottom: 2 })
              .borderRadius(10)
              .backgroundColor(`${this.getStatusColor(item.payStatus)}15`)
              .margin({ left: 8 })
          }

          Text(item.description || this.getOrderTypeText(item.orderType))
            .fontSize(13)
            .fontColor('#999999')
            .margin({ top: 4 })

          if (item.patientName && item.patientName.length > 0) {
            Text(`患者：${item.patientName}`)
              .fontSize(12)
              .fontColor('#999999')
              .margin({ top: 2 })
          }
        }
        .alignItems(HorizontalAlign.Start)

        Blank()

        Column() {
          Text(`¥${this.formatAmount(item.totalAmount)}`)
            .fontSize(18)
            .fontWeight(FontWeight.Bold)
            .fontColor('#333333')

          Text(item.payTime || '')
            .fontSize(11)
            .fontColor('#999999')
            .margin({ top: 2 })
        }
        .alignItems(HorizontalAlign.End)
      }
      .width('100%')
      .padding(12)

      if (this.currentTab === 0) {
        Row() {
          Button('立即支付')
            .fontSize(14)
            .fontColor('#FFFFFF')
            .backgroundColor('#007AFF')
            .borderRadius(20)
            .height(32)
            .padding({ left: 20, right: 20 })
            .onClick(() => {
              this.goToPayDetail(item);
            })

          Button('取消订单')
            .fontSize(14)
            .fontColor('#999999')
            .backgroundColor('transparent')
            .borderRadius(20)
            .height(32)
            .padding({ left: 16, right: 16 })
            .margin({ left: 8 })
            .onClick(() => {
              promptAction.showToast({
                message: '订单已取消',
                duration: 1500
              });
            })
        }
        .width('100%')
        .justifyContent(FlexAlign.End)
        .margin({ top: 8 })
      }
    }
    .width('100%')
    .backgroundColor('#FFFFFF')
    .borderRadius(12)
    .padding(12)
    .margin({ bottom: 12 })
    .shadow({
      radius: 4,
      color: 'rgba(0,0,0,0.04)',
      offsetY: 2
    })
  }

  @Builder
  EmptyState(icon: string, title: string, desc: string = '暂无记录') {
    Column() {
      Text(icon)
        .fontSize(48)
        .margin({ bottom: 12 })

      Text(title)
        .fontSize(16)
        .fontColor('#333333')
        .fontWeight(FontWeight.Medium)
        .margin({ bottom: 4 })

      Text(desc)
        .fontSize(13)
        .fontColor('#999999')
    }
    .width('100%')
    .padding(60)
    .justifyContent(FlexAlign.Center)
    .alignItems(HorizontalAlign.Center)
  }

  build() {
    Column() {
      // ===== 顶部导航栏 =====
      Row() {
        Text('←')
          .fontSize(24)
          .fontColor('#333333')
          .onClick(() => {
            this.goBack();
          })

        Text('缴费记录')
          .fontSize(20)
          .fontWeight(FontWeight.Bold)
          .fontColor('#333333')
          .layoutWeight(1)
          .textAlign(TextAlign.Center)

        if (this.currentTab === 0) {
          Text('历史 ›')
            .fontSize(14)
            .fontColor('#007AFF')
            .onClick(() => {
              this.currentTab = 1;
            })
        } else {
          Text('待支付 ›')
            .fontSize(14)
            .fontColor('#007AFF')
            .onClick(() => {
              this.currentTab = 0;
            })
        }
      }
      .width('100%')
      .height(56)
      .padding({ left: 16, right: 16 })
      .backgroundColor('#FFFFFF')
      .borderRadius({ bottomLeft: 12, bottomRight: 12 })
      .shadow({
        radius: 4,
        color: 'rgba(0,0,0,0.04)',
        offsetY: 2
      })

      // ===== Tab切换栏 =====
      Row() {
        Row() {
          Text(`待支付 (${this.pendingPayments.length})`)
            .fontSize(15)
            .fontWeight(this.currentTab === 0 ? FontWeight.Bold : FontWeight.Normal)
            .fontColor(this.currentTab === 0 ? '#007AFF' : '#999999')
            .padding({ bottom: 8 })
            .border({
              width: { bottom: this.currentTab === 0 ? 3 : 0 },
              color: '#007AFF'
            })
        }
        .onClick(() => {
          this.currentTab = 0;
        })

        Row() {
          Text(`历史记录 (${this.historyPayments.length})`)
            .fontSize(15)
            .fontWeight(this.currentTab === 1 ? FontWeight.Bold : FontWeight.Normal)
            .fontColor(this.currentTab === 1 ? '#007AFF' : '#999999')
            .padding({ bottom: 8 })
            .border({
              width: { bottom: this.currentTab === 1 ? 3 : 0 },
              color: '#007AFF'
            })
        }
        .margin({ left: 24 })
        .onClick(() => {
          this.currentTab = 1;
        })

        Blank()
      }
      .width('100%')
      .padding({ left: 16, right: 16, top: 12, bottom: 0 })
      .backgroundColor('#FFFFFF')

      // ===== 内容区域 =====
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
        .height(200)
        .justifyContent(FlexAlign.Center)
        .backgroundColor('#F5F5F5')
      } else if (this.currentTab === 0) {
        List() {
          if (this.pendingPayments.length > 0) {
            ForEach(this.pendingPayments, (item: PayResultVo) => {
              ListItem() {
                this.PayItemCard(item)
              }
            }, (item: PayResultVo) => item.payId)
          } else {
            ListItem() {
              this.EmptyState('🎉', '暂无待支付订单', '您当前没有待支付的订单')
            }
          }
        }
        .width('100%')
        .padding({ left: 16, right: 16, top: 12 })
        .backgroundColor('#F5F5F5')
        .layoutWeight(1)
      } else {
        List() {
          if (this.historyPayments.length > 0) {
            ForEach(this.historyPayments, (item: PayResultVo) => {
              ListItem() {
                this.PayItemCard(item)
              }
            }, (item: PayResultVo) => item.payId)
          } else {
            ListItem() {
              this.EmptyState('📋', '暂无历史记录', '您还没有完成支付的订单')
            }
          }
        }
        .width('100%')
        .padding({ left: 16, right: 16, top: 12 })
        .backgroundColor('#F5F5F5')
        .layoutWeight(1)
      }
    }
    .width('100%')
    .height('100%')
    .backgroundColor('#F5F5F5')
  }
}
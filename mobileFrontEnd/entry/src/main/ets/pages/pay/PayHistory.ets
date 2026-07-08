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

interface EmptyRequest {}

// ==================== 常量定义 ====================

const PayStatusText: Record<string, string> = {
  'WAITING': '待支付',
  'PAID': '已支付',
  'CANCELLED': '已取消',
  'REFUNDED': '已退款'
};

// 色盲友好的颜色方案
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

const OrderTypeText: Record<string, string> = {
  'REGISTER': '挂号费',
  'MEDICAL': '检查费',
  'PRESCRIPTION': '药品费'
};

// ==================== 页面组件 ====================

@Entry
@Component
export struct PayHistory {
  @State currentTab: number = 0;
  @State pendingPayments: PayResultVo[] = [];
  @State historyPayments: PayResultVo[] = [];
  @State isLoading: boolean = true;
  @State isCancelling: boolean = false;
  @State cancelPayId: string = '';
  @State patientId: string = '';
  @State patientName: string = '';
  @State patientPhone: string = '';
  @State patientGender: number = 0;
  @State patientGenderText: string = '';
  @State patientAge: number = 0;

  private baseUrl: string = APIConfig.PAYMENT_SERVICE_URL;
  private patientBaseUrl: string = APIConfig.PATIENT_SERVICE_URL;

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

  async loadPayments(): Promise<void> {
    this.isLoading = true;
    try {
      const context = getContext(this);
      const fullUrl: string = `${this.patientBaseUrl}/profile/payments`;
      console.info('请求缴费记录: ' + fullUrl);

      const result = await HttpUtil.get<ApiResponse<PayResultVo[]>>(fullUrl, context);
      console.info('缴费记录响应:', JSON.stringify(result));

      if (result.code === 200 && result.data) {
        const data: PayResultVo[] = result.data;

        const processedData: PayResultVo[] = [];
        for (let i = 0; i < data.length; i++) {
          const item: PayResultVo = data[i];
          if (!item.patientName || item.patientName === '') {
            item.patientName = this.patientName || '未知患者';
          }
          processedData.push(item);
        }

        const pending: PayResultVo[] = [];
        const history: PayResultVo[] = [];
        for (let i = 0; i < processedData.length; i++) {
          const item: PayResultVo = processedData[i];
          if (item.payStatus === 'WAITING' || item.payStatus === '待支付') {
            pending.push(item);
          } else {
            history.push(item);
          }
        }
        this.pendingPayments = pending;
        this.historyPayments = history;
        console.info(`缴费记录加载成功: 待支付 ${this.pendingPayments.length} 条, 历史 ${this.historyPayments.length} 条`);
      } else {
        console.error('接口返回失败:', result.message || '未知错误');
        promptAction.showToast({
          message: result.message || '加载缴费记录失败',
          duration: 2000
        });
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

  async cancelPayOrder(payItem: PayResultVo): Promise<void> {
    if (this.isCancelling) return;

    try {
      const result = await promptAction.showDialog({
        title: '提示',
        message: '确定要取消该订单吗？取消后订单将无法恢复。',
        buttons: [
          { text: '取消', color: '#95A5A6' },
          { text: '确定取消', color: '#4A90D9' }
        ]
      });

      if (result.index !== 1) {
        return;
      }
    } catch (error) {
      console.error('显示对话框失败：', JSON.stringify(error));
      return;
    }

    this.isCancelling = true;
    this.cancelPayId = payItem.payId;

    try {
      const context = getContext(this);
      const url = `${this.baseUrl}/pay/cancel/${payItem.payId}`;
      console.info('====== 取消支付订单 ======');
      console.info('取消URL：', url);

      const emptyData: EmptyRequest = {};
      const result = await HttpUtil.post<ApiResponse<boolean>>(url, emptyData, context);
      console.info('取消订单返回：', JSON.stringify(result));

      if (result.code === 200 && result.data) {
        promptAction.showToast({
          message: '订单已取消成功',
          duration: 2000
        });

        const newPending: PayResultVo[] = [];
        for (let i = 0; i < this.pendingPayments.length; i++) {
          if (this.pendingPayments[i].payId !== payItem.payId) {
            newPending.push(this.pendingPayments[i]);
          }
        }
        this.pendingPayments = newPending;

        const cancelledItem: PayResultVo = {
          payId: payItem.payId,
          orderType: payItem.orderType,
          businessId: payItem.businessId,
          patientId: payItem.patientId,
          patientName: payItem.patientName,
          totalAmount: payItem.totalAmount,
          payStatus: 'CANCELLED',
          payStatusText: '已取消',
          payTime: payItem.payTime,
          description: payItem.description
        };

        const newHistory: PayResultVo[] = [cancelledItem];
        for (let i = 0; i < this.historyPayments.length; i++) {
          newHistory.push(this.historyPayments[i]);
        }
        this.historyPayments = newHistory;

        if (this.currentTab === 0 && this.pendingPayments.length === 0) {
          this.currentTab = 1;
        }
      } else {
        promptAction.showToast({
          message: result.message || '取消订单失败，请稍后重试',
          duration: 2000
        });
      }
    } catch (error) {
      console.error('取消订单异常：', JSON.stringify(error));
      promptAction.showToast({
        message: '取消订单失败，请检查网络后重试',
        duration: 2000
      });
    } finally {
      this.isCancelling = false;
      this.cancelPayId = '';
    }
  }

  goToPayDetail(payItem: PayResultVo): void {
    if (payItem.payStatus === 'CANCELLED' || payItem.payStatus === 'PAID') {
      promptAction.showToast({
        message: '该订单状态异常，无法支付',
        duration: 2000
      });
      return;
    }

    router.pushUrl({
      url: 'pages/pay/PayPage',
      params: {
        payId: payItem.payId,
        businessId: payItem.businessId,
        orderType: payItem.orderType,
        amount: payItem.totalAmount,
        description: payItem.description || OrderTypeText[payItem.orderType] || '未知项目',
        patientId: this.patientId,
        patientName: payItem.patientName || this.patientName || '未知患者',
        returnUrl: 'pages/pay/PayHistory'
      }
    }).catch((err: Error) => {
      console.error('跳转支付详情失败：' + JSON.stringify(err));
      promptAction.showToast({
        message: '跳转失败，请稍后重试',
        duration: 1500
      });
    });
  }

  goBack(): void {
    router.back();
  }

  private getStatusColor(status: string): string {
    return PayStatusColor[status] || '#95A5A6';
  }

  private getStatusBgColor(status: string): string {
    return PayStatusBgColor[status] || '#F0F0F0';
  }

  private getStatusText(status: string): string {
    return PayStatusText[status] || status;
  }

  private getOrderTypeText(orderType: string): string {
    return OrderTypeText[orderType] || orderType;
  }

  private formatAmount(amount: number): string {
    return amount.toFixed(2);
  }

  private getStatusIcon(status: string): string {
    const iconMap: Record<string, string> = {
      'WAITING': '📋',
      'PAID': '✅',
      'CANCELLED': '✖️',
      'REFUNDED': '↩️'
    };
    return iconMap[status] || '📋';
  }

  private getStatusDisplayText(status: string): string {
    const textMap: Record<string, string> = {
      'WAITING': '待支付',
      'PAID': '已支付 ✓',
      'CANCELLED': '已取消 ✗',
      'REFUNDED': '已退款 ↩'
    };
    return textMap[status] || status;
  }

  private formatDate(dateStr: string): string {
    if (!dateStr) return '';
    try {
      const date = new Date(dateStr);
      if (isNaN(date.getTime())) return dateStr;
      const month = String(date.getMonth() + 1).padStart(2, '0');
      const day = String(date.getDate()).padStart(2, '0');
      const hours = String(date.getHours()).padStart(2, '0');
      const minutes = String(date.getMinutes()).padStart(2, '0');
      return `${month}-${day} ${hours}:${minutes}`;
    } catch {
      return dateStr;
    }
  }

  // ==================== UI构建 ====================

  @Builder
  PayItemCard(item: PayResultVo) {
    Column() {
      Row() {
        // 左侧图标 - 温和的圆形背景
        Column() {
          Text(this.getStatusIcon(item.payStatus))
            .fontSize(18)
        }
        .width(40)
        .height(40)
        .borderRadius(20)
        .backgroundColor(this.getStatusBgColor(item.payStatus))
        .justifyContent(FlexAlign.Center)
        .alignItems(HorizontalAlign.Center)

        // 中间信息
        Column() {
          Row() {
            Text(this.getOrderTypeText(item.orderType))
              .fontSize(15)
              .fontWeight(FontWeight.Medium)
              .fontColor('#1A1A2E')

            Text(this.getStatusDisplayText(item.payStatus))
              .fontSize(10)
              .fontColor(this.getStatusColor(item.payStatus))
              .padding({ left: 8, right: 8, top: 2, bottom: 2 })
              .borderRadius(10)
              .backgroundColor(this.getStatusBgColor(item.payStatus))
              .margin({ left: 6 })
          }

          Text(item.description || this.getOrderTypeText(item.orderType))
            .fontSize(12)
            .fontColor('#8E8E93')
            .margin({ top: 2 })

          if (item.patientName && item.patientName.length > 0) {
            Row() {
              Text('👤')
                .fontSize(10)
                .margin({ right: 3 })
              Text(item.patientName)
                .fontSize(11)
                .fontColor('#8E8E93')
            }
            .margin({ top: 2 })
          }
        }
        .alignItems(HorizontalAlign.Start)
        .layoutWeight(1)
        .margin({ left: 10 })

        // 右侧金额和时间
        Column() {
          Text(`¥${this.formatAmount(item.totalAmount)}`)
            .fontSize(17)
            .fontWeight(FontWeight.Bold)
            .fontColor('#1A1A2E')

          Text(this.formatDate(item.payTime))
            .fontSize(10)
            .fontColor('#C7C7CC')
            .margin({ top: 2 })
        }
        .alignItems(HorizontalAlign.End)
      }
      .width('100%')

      // 按钮区域 - 只有待支付状态才显示
      if (this.currentTab === 0 && (item.payStatus === 'WAITING' || item.payStatus === '待支付')) {
        Row() {
          Button('取消订单')
            .fontSize(12)
            .fontColor('#8E8E93')
            .backgroundColor('transparent')
            .borderRadius(16)
            .height(30)
            .padding({ left: 14, right: 14 })
            .border({ width: 0.5, color: '#D5D8DC' })
            .enabled(!(this.isCancelling && this.cancelPayId === item.payId))
            .onClick(() => {
              this.cancelPayOrder(item);
            })
            .margin({ right: 8 })

          Button(this.isCancelling && this.cancelPayId === item.payId ? '取消中...' : '立即支付')
            .fontSize(12)
            .fontWeight(FontWeight.Medium)
            .fontColor('#FFFFFF')
            .backgroundColor('#4A90D9')
            .borderRadius(16)
            .height(30)
            .padding({ left: 18, right: 18 })
            .enabled(!(this.isCancelling && this.cancelPayId === item.payId))
            .onClick(() => {
              this.goToPayDetail(item);
            })
        }
        .width('100%')
        .justifyContent(FlexAlign.End)
        .margin({ top: 10 })
      }
    }
    .width('100%')
    .padding(14)
    .backgroundColor('#FFFFFF')
    .borderRadius(14)
    .margin({ bottom: 10 })
    .shadow({
      radius: 6,
      color: 'rgba(0,0,0,0.03)',
      offsetY: 2
    })
  }

  @Builder
  EmptyState(icon: string, title: string, desc: string) {
    Column() {
      Column() {
        Text(icon)
          .fontSize(48)
          .margin({ bottom: 12 })

        Text(title)
          .fontSize(16)
          .fontWeight(FontWeight.Medium)
          .fontColor('#1A1A2E')
          .margin({ bottom: 4 })

        Text(desc)
          .fontSize(13)
          .fontColor('#C7C7CC')
      }
      .width('100%')
      .padding({ top: 50, bottom: 50 })
      .justifyContent(FlexAlign.Center)
      .alignItems(HorizontalAlign.Center)
    }
    .width('100%')
    .backgroundColor('#FFFFFF')
    .borderRadius(14)
    .margin({ top: 16 })
  }

  build() {
    Column() {
      // ===== 顶部导航栏 =====
      Row() {
        // 返回按钮 - 使用Stack确保居中
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
          this.goBack();
        })

        Text('缴费记录')
          .fontSize(18)
          .fontWeight(FontWeight.Medium)
          .fontColor('#1A1A2E')
          .layoutWeight(1)
          .textAlign(TextAlign.Center)

        Row()
          .width(40)
          .height(40)
      }
      .width('100%')
      .height(52)
      .padding({ left: 12, right: 12 })
      .backgroundColor('#FFFFFF')

      // ===== Tab切换栏 =====
      Row() {
        Row() {
          Text(`待支付 (${this.pendingPayments.length})`)
            .fontSize(15)
            .fontWeight(this.currentTab === 0 ? FontWeight.Medium : FontWeight.Normal)
            .fontColor(this.currentTab === 0 ? '#4A90D9' : '#8E8E93')
            .padding({ bottom: 10 })
            .border({
              width: { bottom: this.currentTab === 0 ? 3 : 0 },
              color: '#4A90D9'
            })
        }
        .onClick(() => {
          this.currentTab = 0;
        })

        Row() {
          Text(`历史记录 (${this.historyPayments.length})`)
            .fontSize(15)
            .fontWeight(this.currentTab === 1 ? FontWeight.Medium : FontWeight.Normal)
            .fontColor(this.currentTab === 1 ? '#4A90D9' : '#8E8E93')
            .padding({ bottom: 10 })
            .border({
              width: { bottom: this.currentTab === 1 ? 3 : 0 },
              color: '#4A90D9'
            })
        }
        .margin({ left: 24 })
        .onClick(() => {
          this.currentTab = 1;
        })

        Blank()
      }
      .width('100%')
      .padding({ left: 20, right: 20, top: 4, bottom: 0 })
      .backgroundColor('#FFFFFF')

      // ===== 内容区域 =====
      if (this.isLoading) {
        Column() {
          LoadingProgress()
            .width(36)
            .height(36)
            .color('#4A90D9')
          Text('加载中...')
            .fontSize(13)
            .fontColor('#8E8E93')
            .margin({ top: 10 })
        }
        .width('100%')
        .height(300)
        .justifyContent(FlexAlign.Center)
        .backgroundColor('#F8F9FA')
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
        .padding({ left: 16, right: 16, top: 16 })
        .backgroundColor('#F8F9FA')
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
        .padding({ left: 16, right: 16, top: 16 })
        .backgroundColor('#F8F9FA')
        .layoutWeight(1)
      }
    }
    .width('100%')
    .height('100%')
    .backgroundColor('#F8F9FA')
  }
}
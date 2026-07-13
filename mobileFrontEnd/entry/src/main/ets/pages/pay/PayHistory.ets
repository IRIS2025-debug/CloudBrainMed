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
  businessStatus?: string;
  canRefund?: boolean;
  isExpired?: boolean;
}

interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
}

// ==================== 常量定义 ====================

const OrderTypeText: Record<string, string> = {
  'REGISTER': '挂号费',
  'MEDICAL': '检查费',
  'PRESCRIPTION': '药品费'
};

// ==================== 页面组件 ====================

@Entry
@Component
export struct PayHistory {
  @State currentTabIndex: number = 0;
  @State pendingList: PayResultVo[] = [];
  @State paidList: PayResultVo[] = [];
  @State expiredList: PayResultVo[] = [];
  @State refundedList: PayResultVo[] = [];
  @State isLoading: boolean = true;
  @State refundingPayId: string = '';
  @State cancellingPayId: string = '';
  @State patientId: string = '';
  @State patientName: string = '';

  private baseUrl: string = APIConfig.PAYMENT_SERVICE_URL;
  private patientBaseUrl: string = APIConfig.PATIENT_SERVICE_URL;

  aboutToAppear(): void {
    const params = router.getParams() as Record<string, Object>;
    if (params) {
      this.patientId = (params['patientId'] as string) || '';
      this.patientName = (params['patientName'] as string) ||
        (params['userName'] as string) ||
        (params['name'] as string) || '';
    }
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
        console.info(`后端返回 ${data.length} 条记录`);

        // 打印每条记录的关键信息，便于调试
        for (let i = 0; i < data.length; i++) {
          console.info(`记录${i + 1}: payId=${data[i].payId}, payStatus=${data[i].payStatus}, businessStatus=${data[i].businessStatus}, canRefund=${data[i].canRefund}`);
        }

        // 清空各列表
        this.pendingList = [];
        this.paidList = [];
        this.expiredList = [];
        this.refundedList = [];

        // ===== 统一分类逻辑 =====
        for (let i = 0; i < data.length; i++) {
          const item: PayResultVo = data[i];
          const payStatus: string = item.payStatus;           // 支付状态（英文）
          const businessStatus: string | undefined = item.businessStatus; // 业务状态
          const isExpired: boolean = item.isExpired === true; // 是否过期

          // 构建统一的数据结构
          const processedItem: PayResultVo = {
            payId: item.payId,
            orderType: item.orderType,
            businessId: item.businessId,
            patientId: item.patientId,
            patientName: item.patientName || this.patientName || '未知患者',
            totalAmount: item.totalAmount,
            payStatus: payStatus,
            payStatusText: item.payStatusText || this.getPayStatusDisplayText(payStatus),
            payTime: item.payTime,
            description: item.description || this.getOrderTypeText(item.orderType),
            businessStatus: businessStatus,
            // ✅ 退款资格：支付状态为PAID，且业务状态也为PAID
            // 业务状态为CANCELLED表示业务已被取消（如超过visit_date），不可退款
            canRefund: payStatus === 'PAID' && businessStatus === 'PAID',
            isExpired: isExpired
          };

          // ===== 按状态分类到不同Tab =====
          // 优先级：REFUNDED > CANCELLED > WAITING(过期) > WAITING > PAID

          if (payStatus === 'REFUNDED') {
            // 1. 已退款
            this.refundedList.push(processedItem);
            console.info(`分类[已退款]: payId=${item.payId}`);
          } else if (payStatus === 'CANCELLED') {
            // 2. 已取消（payStatus直接是CANCELLED）
            this.expiredList.push(processedItem);
            console.info(`分类[已取消]: payId=${item.payId}`);
          } else if (payStatus === 'WAITING' && isExpired) {
            // 3. 待支付但已过期 → 归入"已取消"Tab
            this.expiredList.push(processedItem);
            console.info(`分类[已取消(过期)]: payId=${item.payId}`);
          } else if (payStatus === 'WAITING') {
            // 4. 正常待支付
            this.pendingList.push(processedItem);
            console.info(`分类[待支付]: payId=${item.payId}`);
          } else if (payStatus === 'PAID') {
            // 5. 已支付（全部保留在已支付Tab）
            //    包含 businessStatus=CANCELLED 的记录（业务被系统自动取消）
            //    但 canRefund 已根据 businessStatus 正确设置为 false
            this.paidList.push(processedItem);
            console.info(`分类[已支付]: payId=${item.payId}, businessStatus=${businessStatus}, canRefund=${processedItem.canRefund}`);
          } else {
            // 6. 未知状态 → 归入待支付（兜底处理）
            console.warn(`分类[未知状态]: payId=${item.payId}, payStatus=${payStatus}`);
            this.pendingList.push(processedItem);
          }
        }

        console.info(`分类完成: 待支付 ${this.pendingList.length}, 已支付 ${this.paidList.length}, 已取消 ${this.expiredList.length}, 已退款 ${this.refundedList.length}`);
        console.info(`总计: ${this.pendingList.length + this.paidList.length + this.expiredList.length + this.refundedList.length} 条`);
      } else {
        promptAction.showToast({
          message: result.message || '加载缴费记录失败',
          duration: 2000
        });
      }
    } catch (error) {
      console.error('网络请求异常:', JSON.stringify(error));
      promptAction.showToast({
        message: '网络异常，请稍后重试',
        duration: 2000
      });
    } finally {
      this.isLoading = false;
    }
  }

  // ===== 取消订单 =====
  async cancelPayOrder(payItem: PayResultVo): Promise<void> {
    // 防止重复点击
    if (this.cancellingPayId !== '') {
      promptAction.showToast({
        message: '正在处理，请稍后',
        duration: 1500
      });
      return;
    }

    // 确认对话框
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
      return;
    }

    // 记录当前操作的订单
    this.cancellingPayId = payItem.payId;

    try {
      const context = getContext(this);
      const url = `${this.baseUrl}/pay/cancel/${payItem.payId}`;
      const emptyRequest: Record<string, never> = {};
      const result = await HttpUtil.post<ApiResponse<boolean>>(url, emptyRequest, context);

      if (result.code === 200 && result.data) {
        promptAction.showToast({
          message: '订单已取消成功',
          duration: 2000
        });
        // 重新加载数据
        await this.loadPayments();
      } else {
        promptAction.showToast({
          message: result.message || '取消订单失败',
          duration: 2000
        });
      }
    } catch (error) {
      console.error('取消订单异常:', JSON.stringify(error));
      promptAction.showToast({
        message: '取消订单失败，请检查网络',
        duration: 2000
      });
    } finally {
      // 操作完成后清空
      this.cancellingPayId = '';
    }
  }

  // ===== 申请退款 =====
  async applyRefund(payItem: PayResultVo): Promise<void> {
    // 防止重复点击
    if (this.refundingPayId !== '') {
      promptAction.showToast({
        message: '正在处理退款，请稍后',
        duration: 1500
      });
      return;
    }

    // 确认对话框
    try {
      const result = await promptAction.showDialog({
        title: '退款确认',
        message: `确定要申请退款吗？\n\n退款金额：¥${this.formatAmount(payItem.totalAmount)}\n款项将原路返回至您的支付账户。`,
        buttons: [
          { text: '我再想想', color: '#95A5A6' },
          { text: '确认退款', color: '#E74C3C' }
        ]
      });
      if (result.index !== 1) {
        return;
      }
    } catch (error) {
      return;
    }

    // 记录当前操作的订单
    this.refundingPayId = payItem.payId;

    try {
      const context = getContext(this);
      const url = `${this.baseUrl}/pay/refund/apply/${payItem.payId}`;
      const emptyRequest: Record<string, never> = {};
      const result = await HttpUtil.post<ApiResponse<boolean>>(url, emptyRequest, context);

      if (result.code === 200 && result.data) {
        promptAction.showToast({
          message: '✅ 退款成功！款项将原路返回',
          duration: 2500
        });
        // 重新加载数据
        await this.loadPayments();
      } else {
        promptAction.showToast({
          message: result.message || '退款失败，请稍后重试',
          duration: 2000
        });
      }
    } catch (error) {
      console.error('退款异常:', JSON.stringify(error));
      promptAction.showToast({
        message: '退款失败，请检查网络',
        duration: 2000
      });
    } finally {
      // 操作完成后清空
      this.refundingPayId = '';
    }
  }

  goToPayDetail(payItem: PayResultVo): void {
    // 已过期或已取消的订单不允许支付
    if (payItem.isExpired || payItem.payStatus === 'CANCELLED') {
      promptAction.showToast({
        message: '该订单已过期或已取消，无法支付',
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
        description: payItem.description || this.getOrderTypeText(payItem.orderType),
        patientId: this.patientId,
        patientName: payItem.patientName || this.patientName || '未知患者',
        returnUrl: 'pages/pay/PayHistory'
      }
    });
  }

  goBack(): void {
    router.back();
  }

  // ===== 工具方法 =====

  private formatAmount(amount: number): string {
    if (amount === undefined || amount === null) {
      return '0.00';
    }
    return amount.toFixed(2);
  }

  private formatDate(dateStr: string): string {
    if (!dateStr) {
      return '';
    }
    try {
      const date = new Date(dateStr);
      if (isNaN(date.getTime())) {
        return dateStr;
      }
      const year = String(date.getFullYear());
      const month = String(date.getMonth() + 1).padStart(2, '0');
      const day = String(date.getDate()).padStart(2, '0');
      const hours = String(date.getHours()).padStart(2, '0');
      const minutes = String(date.getMinutes()).padStart(2, '0');
      return `${year}-${month}-${day} ${hours}:${minutes}`;
    } catch {
      return dateStr;
    }
  }

  private getOrderTypeText(orderType: string): string {
    return OrderTypeText[orderType] || orderType || '未知项目';
  }

  private getStatusColor(status: string): string {
    const map: Record<string, string> = {
      'WAITING': '#4A90D9',
      'PAID': '#2ECC71',
      'CANCELLED': '#95A5A6',
      'REFUNDED': '#95A5A6'
    };
    return map[status] || '#95A5A6';
  }

  private getStatusBgColor(status: string): string {
    const map: Record<string, string> = {
      'WAITING': '#E8F0FE',
      'PAID': '#E8F8F0',
      'CANCELLED': '#F0F0F0',
      'REFUNDED': '#F0F0F0'
    };
    return map[status] || '#F0F0F0';
  }

  private getStatusIcon(status: string): string {
    const map: Record<string, string> = {
      'WAITING': '📋',
      'PAID': '✅',
      'CANCELLED': '✖️',
      'REFUNDED': '↩️'
    };
    return map[status] || '📋';
  }

  private getStatusDisplayText(status: string): string {
    const map: Record<string, string> = {
      'WAITING': '待支付',
      'PAID': '已支付',
      'CANCELLED': '已取消',
      'REFUNDED': '已退款'
    };
    return map[status] || status;
  }

  private getBusinessStatusText(status: string | undefined): string {
    if (!status) {
      return '未知';
    }
    const map: Record<string, string> = {
      'WAITING': '待处理',
      'PAID': '已支付',
      'SEEN': '已接诊',
      'PROCESSING': '处理中',
      'COMPLETED': '已完成',
      'DISPENSED': '已发药',
      'CANCELLED': '已取消',
      'REFUNDED': '已退款'
    };
    return map[status] || status;
  }

  private getPayStatusDisplayText(status: string): string {
    return this.getStatusDisplayText(status);
  }

  // ===== Tab相关方法 =====

  private getTabCount(index: number): number {
    switch (index) {
      case 0:
        return this.pendingList.length;
      case 1:
        return this.paidList.length;
      case 2:
        return this.expiredList.length;
      case 3:
        return this.refundedList.length;
      default:
        return 0;
    }
  }

  private getEmptyIcon(index: number): string {
    const icons: string[] = ['📋', '💳', '⏰', '↩️'];
    return icons[index] || '📭';
  }

  private getEmptyTitle(index: number): string {
    const titles: string[] = ['暂无待支付订单', '暂无已支付订单', '暂无已过期订单', '暂无已退款订单'];
    return titles[index] || '暂无记录';
  }

  private getEmptyDesc(index: number): string {
    const descs: string[] = [
      '您当前没有待支付的订单',
      '您还没有已支付的订单',
      '没有过期的订单',
      '没有已退款的订单'
    ];
    return descs[index] || '';
  }

  // ==================== UI构建 ====================

  @Builder
  PayItemCard(item: PayResultVo) {
    Column() {
      // ===== 卡片主体：图标 + 信息 + 金额 =====
      Row() {
        // 左侧状态图标
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

        // 中间信息区域
        Column() {
          // 第一行：类型 + 状态标签
          Row() {
            Text(this.getOrderTypeText(item.orderType))
              .fontSize(15)
              .fontWeight(FontWeight.Medium)
              .fontColor('#1A1A2E')
              .maxLines(1)
              .textOverflow({ overflow: TextOverflow.Ellipsis })

            Text(this.getStatusDisplayText(item.payStatus))
              .fontSize(10)
              .fontColor(this.getStatusColor(item.payStatus))
              .padding({ left: 8, right: 8, top: 2, bottom: 2 })
              .borderRadius(10)
              .backgroundColor(this.getStatusBgColor(item.payStatus))
              .margin({ left: 6 })

            // 过期标记
            if (item.isExpired) {
              Text('⏰ 已过期')
                .fontSize(10)
                .fontColor('#E74C3C')
                .padding({ left: 8, right: 8, top: 2, bottom: 2 })
                .borderRadius(10)
                .backgroundColor('#FDE8E8')
                .margin({ left: 6 })
            }
          }
          .alignItems(VerticalAlign.Center)

          // 第二行：描述信息
          Text(item.description || this.getOrderTypeText(item.orderType))
            .fontSize(12)
            .fontColor('#8E8E93')
            .margin({ top: 2 })
            .maxLines(1)
            .textOverflow({ overflow: TextOverflow.Ellipsis })

          // 第三行：患者姓名
          if (item.patientName) {
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

          // 第四行：业务状态（仅已支付时显示）
          if (item.payStatus === 'PAID' && item.businessStatus) {
            Row() {
              Text('业务状态: ')
                .fontSize(10)
                .fontColor('#8E8E93')
              Text(this.getBusinessStatusText(item.businessStatus))
                .fontSize(10)
                .fontColor(this.getBusinessStatusColor(item.businessStatus))
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
        .flexShrink(0)
      }
      .width('100%')

      // ===== 分割线 =====
      if (item.payStatus === 'WAITING' || item.payStatus === 'PAID') {
        Divider()
          .color('#F0F0F0')
          .strokeWidth(0.5)
          .margin({ top: 10, bottom: 6 })
      }

      // ===== 按钮区域 =====

      // 1. 待支付（未过期）：显示"取消订单" + "立即支付"
      if (item.payStatus === 'WAITING' && !item.isExpired) {
        Row() {
          Button('取消订单')
            .fontSize(12)
            .fontColor('#8E8E93')
            .backgroundColor('transparent')
            .borderRadius(16)
            .height(30)
            .padding({ left: 14, right: 14 })
            .border({ width: 0.5, color: '#D5D8DC' })
            .enabled(this.cancellingPayId === '')
            .onClick(() => {
              this.cancelPayOrder(item);
            })
            .margin({ right: 8 })

          Button(this.cancellingPayId === item.payId ? '取消中...' : '立即支付')
            .fontSize(12)
            .fontWeight(FontWeight.Medium)
            .fontColor('#FFFFFF')
            .backgroundColor('#4A90D9')
            .borderRadius(16)
            .height(30)
            .padding({ left: 18, right: 18 })
            .enabled(this.cancellingPayId === '')
            .onClick(() => {
              this.goToPayDetail(item);
            })
        }
        .width('100%')
        .justifyContent(FlexAlign.End)
      }

      // 2. 待支付（已过期）：显示提示信息
      if (item.payStatus === 'WAITING' && item.isExpired) {
        Row() {
          Text('⏰ 订单已过期，无法操作')
            .fontSize(12)
            .fontColor('#E74C3C')
            .backgroundColor('#FDE8E8')
            .padding({ left: 12, right: 12, top: 6, bottom: 6 })
            .borderRadius(12)
        }
        .width('100%')
        .justifyContent(FlexAlign.End)
      }

      // 3. 已支付 + 可退款：显示"申请退款"按钮
      if (item.payStatus === 'PAID' && item.canRefund) {
        Row() {
          Text('💡 可申请退款')
            .fontSize(11)
            .fontColor('#2ECC71')
            .margin({ right: 10 })

          Button(this.refundingPayId === item.payId ? '退款中...' : '申请退款')
            .fontSize(12)
            .fontWeight(FontWeight.Medium)
            .fontColor('#E74C3C')
            .backgroundColor('transparent')
            .borderRadius(16)
            .height(30)
            .padding({ left: 16, right: 16 })
            .border({ width: 1, color: '#E74C3C' })
            .enabled(this.refundingPayId === '')
            .onClick(() => {
              this.applyRefund(item);
            })
        }
        .width('100%')
        .justifyContent(FlexAlign.End)
      }

      // 4. 已支付 + 不可退款：根据businessStatus展示不同信息
      if (item.payStatus === 'PAID' && !item.canRefund) {
        Row() {
          if (item.businessStatus === 'CANCELLED') {
            // 业务已被系统取消（如超过visit_date）
            Text('⏰ 业务已取消，不可退款')
              .fontSize(12)
              .fontColor('#E67E22')
              .backgroundColor('#FDF2E9')
              .padding({ left: 12, right: 12, top: 6, bottom: 6 })
              .borderRadius(12)
          } else if (item.businessStatus === 'COMPLETED' || item.businessStatus === 'DISPENSED') {
            // 业务已完成或已发药
            Text('✅ 业务已完成，不可退款')
              .fontSize(12)
              .fontColor('#2ECC71')
              .backgroundColor('#E8F8F0')
              .padding({ left: 12, right: 12, top: 6, bottom: 6 })
              .borderRadius(12)
          } else if (item.businessStatus === 'SEEN' || item.businessStatus === 'PROCESSING') {
            // 业务进行中
            Text('📋 业务处理中，暂不可退款')
              .fontSize(12)
              .fontColor('#3498DB')
              .backgroundColor('#EAF2FD')
              .padding({ left: 12, right: 12, top: 6, bottom: 6 })
              .borderRadius(12)
          } else {
            // 其他状态
            Text('🔒 当前状态不可退款')
              .fontSize(12)
              .fontColor('#95A5A6')
              .backgroundColor('#F0F0F0')
              .padding({ left: 12, right: 12, top: 6, bottom: 6 })
              .borderRadius(12)
          }
        }
        .width('100%')
        .justifyContent(FlexAlign.End)
      }

      // 5. 已退款：显示状态信息
      if (item.payStatus === 'REFUNDED') {
        Row() {
          Text('↩️ 已退款')
            .fontSize(12)
            .fontColor('#2ECC71')
            .backgroundColor('#E8F8F0')
            .padding({ left: 12, right: 12, top: 6, bottom: 6 })
            .borderRadius(12)
        }
        .width('100%')
        .justifyContent(FlexAlign.End)
      }

      // 6. 已取消：显示状态信息
      if (item.payStatus === 'CANCELLED') {
        Row() {
          Text('✖️ 已取消')
            .fontSize(12)
            .fontColor('#95A5A6')
            .backgroundColor('#F0F0F0')
            .padding({ left: 12, right: 12, top: 6, bottom: 6 })
            .borderRadius(12)
        }
        .width('100%')
        .justifyContent(FlexAlign.End)
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

  // ===== 获取业务状态颜色 =====
  private getBusinessStatusColor(status: string): string {
    const map: Record<string, string> = {
      'WAITING': '#4A90D9',
      'PAID': '#2ECC71',
      'SEEN': '#3498DB',
      'PROCESSING': '#3498DB',
      'COMPLETED': '#2ECC71',
      'DISPENSED': '#2ECC71',
      'CANCELLED': '#E67E22',
      'REFUNDED': '#95A5A6'
    };
    return map[status] || '#8E8E93';
  }

  @Builder
  EmptyState(icon: string, title: string, desc: string) {
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
    .backgroundColor('#FFFFFF')
    .borderRadius(14)
    .margin({ top: 16 })
  }

  build() {
    Column() {
      // ===== 顶部导航栏 =====
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

      // ===== Tab切换栏（4个Tab） =====
      Row() {
        this.TabItemBuilder(0, '📋', '待支付')
        this.TabItemBuilder(1, '✅', '已支付')
        this.TabItemBuilder(2, '⏰', '已取消')
        this.TabItemBuilder(3, '↩️', '已退款')
      }
      .width('100%')
      .padding({ left: 16, right: 16, top: 8 })
      .backgroundColor('#FFFFFF')

      // ===== 分割线 =====
      Divider()
        .color('#F0F0F0')
        .strokeWidth(0.5)

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
        .layoutWeight(1)
        .justifyContent(FlexAlign.Center)
        .backgroundColor('#F8F9FA')
      } else {
        // 使用Scroll + Column替代List，避免ListItem嵌套限制
        Scroll() {
          Column() {
            if (this.currentTabIndex === 0) {
              // 待支付Tab
              if (this.pendingList.length > 0) {
                ForEach(this.pendingList, (item: PayResultVo) => {
                  this.PayItemCard(item)
                }, (item: PayResultVo) => item.payId)
              } else {
                this.EmptyState(
                  this.getEmptyIcon(0),
                  this.getEmptyTitle(0),
                  this.getEmptyDesc(0)
                )
              }
            } else if (this.currentTabIndex === 1) {
              // 已支付Tab
              if (this.paidList.length > 0) {
                ForEach(this.paidList, (item: PayResultVo) => {
                  this.PayItemCard(item)
                }, (item: PayResultVo) => item.payId)
              } else {
                this.EmptyState(
                  this.getEmptyIcon(1),
                  this.getEmptyTitle(1),
                  this.getEmptyDesc(1)
                )
              }
            } else if (this.currentTabIndex === 2) {
              // 已取消Tab
              if (this.expiredList.length > 0) {
                ForEach(this.expiredList, (item: PayResultVo) => {
                  this.PayItemCard(item)
                }, (item: PayResultVo) => item.payId)
              } else {
                this.EmptyState(
                  this.getEmptyIcon(2),
                  this.getEmptyTitle(2),
                  this.getEmptyDesc(2)
                )
              }
            } else {
              // 已退款Tab
              if (this.refundedList.length > 0) {
                ForEach(this.refundedList, (item: PayResultVo) => {
                  this.PayItemCard(item)
                }, (item: PayResultVo) => item.payId)
              } else {
                this.EmptyState(
                  this.getEmptyIcon(3),
                  this.getEmptyTitle(3),
                  this.getEmptyDesc(3)
                )
              }
            }
          }
          .width('100%')
          .padding({ left: 16, right: 16, top: 12, bottom: 20 })
        }
        .width('100%')
        .layoutWeight(1)
        .backgroundColor('#F8F9FA')
      }
    }
    .width('100%')
    .height('100%')
    .backgroundColor('#F8F9FA')
  }

  // ===== Tab 项构建器 =====
  @Builder
  TabItemBuilder(index: number, icon: string, label: string) {
    Row() {
      Text(`${icon} ${label} (${this.getTabCount(index)})`)
        .fontSize(13)
        .fontWeight(this.currentTabIndex === index ? FontWeight.Medium : FontWeight.Normal)
        .fontColor(this.currentTabIndex === index ? '#4A90D9' : '#8E8E93')
        .padding({ bottom: 8 })
        .border({
          width: { bottom: this.currentTabIndex === index ? 3 : 0 },
          color: '#4A90D9'
        })
    }
    .padding({ left: 6, right: 6 })
    .onClick(() => {
      this.currentTabIndex = index;
    })
    .margin({ right: 12 })
  }
}
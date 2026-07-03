// pages/medical/MedicalOrderDetail.ets
import router from '@ohos.router';
import promptAction from '@ohos.promptAction';
import { HttpUtil } from '../common/HttpUtil';
import { APIConfig } from '../common/APIConfig';

// ==================== 类型定义 ====================

interface MedicalOrderItem {
  orderItemId: string;
  orderId: string;
  itemCode: string;
  itemName: string;
  itemCategory: string;
  urgencyLevel: string;
  price: number;
  status: string;
  createTime: string;
  reportId?: string;
  reportStatus?: string;
  conclusion?: string;
  abnormalFlag?: string;
  reportTime?: string;
}

interface Indicator {
  indicatorId: string;
  indicatorCode: string;
  indicatorName: string;
  resultValue: string;
  unit: string;
  referenceMin: number;
  referenceMax: number;
  referenceText: string;
  abnormalFlag: string;
  sortNo: number;
}

interface ReportDetail {
  reportId: string;
  orderItemId: string;
  orderId: string;
  patientId: string;
  patientName: string;
  doctorName: string;
  registerId: string;
  itemCode: string;
  itemName: string;
  itemCategory: string;
  price: number;
  urgencyLevel: string;
  resultSummary: string;
  conclusion: string;
  abnormalFlag: string;
  attachmentUrl: string;
  status: string;
  performedTime: string;
  reportTime: string;
  indicators: Indicator[];
}

interface ApiResponse<T> {
  code: number;
  msg: string;
  data: T;
}

interface DetailParams {
  registerId?: string;
  patientName?: string;
  doctorName?: string;
  visitDate?: string;
  totalAmount?: number;
  status?: string;
  items?: MedicalOrderItem[];
}

// ==================== 常量 ====================

const CATEGORY_TEXT: Record<string, string> = {
  'EXAM': '检查',
  'LAB': '检验'
};

const ABNORMAL_COLOR: Record<string, string> = {
  '正常': '#52C41A',
  '偏高': '#FAAD14',
  '偏低': '#FAAD14',
  '异常': '#FF4D4F',
  '危急': '#FF0000'
};

const STATUS_COLOR_MAP: Record<string, string> = {
  '已完成': '#52C41A',
  '检查中': '#1890FF',
  '已取消': '#FF4D4F',
  '待检查': '#FAAD14'
};

const STATUS_BG_MAP: Record<string, string> = {
  '已完成': '#F6FFED',
  '检查中': '#E6F7FF',
  '已取消': '#FFF1F0',
  '待检查': '#FFF7E6'
};

// ==================== 页面组件 ====================

@Entry
@Component
export struct MedicalOrderDetail {
  @State registerId: string = '';
  @State patientName: string = '';
  @State doctorName: string = '';
  @State visitDate: string = '';
  @State totalAmount: number = 0;
  @State status: string = '';
  @State items: MedicalOrderItem[] = [];

  @State reportDetails: Map<string, ReportDetail> = new Map();
  @State loadingItems: Set<string> = new Set();
  @State expandedItemId: string = '';

  private baseUrl: string = APIConfig.PATIENT_SERVICE_URL;

  aboutToAppear(): void {
    const params = router.getParams() as DetailParams;
    if (params) {
      this.registerId = params.registerId || '';
      this.patientName = params.patientName || '';
      this.doctorName = params.doctorName || '';
      this.visitDate = params.visitDate || '';
      this.totalAmount = params.totalAmount || 0;
      this.status = params.status || '';
      this.items = params.items || [];
    }
  }

  private goBack(): void {
    router.back();
  }

  private async loadReportDetail(orderItemId: string): Promise<void> {
    if (this.reportDetails.has(orderItemId)) {
      this.expandedItemId = this.expandedItemId === orderItemId ? '' : orderItemId;
      return;
    }

    if (this.loadingItems.has(orderItemId)) {
      return;
    }

    this.loadingItems.add(orderItemId);

    try {
      const context = getContext(this);
      const url = `${this.baseUrl}/medical-order/report/${orderItemId}`;
      const result = await HttpUtil.get<ApiResponse<ReportDetail>>(url, context);

      if (result.code === 200 && result.data) {
        this.reportDetails.set(orderItemId, result.data);
        this.expandedItemId = orderItemId;
      } else {
        promptAction.showToast({
          message: result.msg || '加载报告失败',
          duration: 1500
        });
      }
    } catch (error) {
      console.error('加载报告详情异常:', JSON.stringify(error));
      promptAction.showToast({
        message: '网络请求失败',
        duration: 1500
      });
    } finally {
      this.loadingItems.delete(orderItemId);
    }
  }

  private toggleExpand(itemId: string): void {
    if (this.expandedItemId === itemId) {
      this.expandedItemId = '';
    } else {
      this.loadReportDetail(itemId);
    }
  }

  private getCategoryText(category: string): string {
    return CATEGORY_TEXT[category] || category;
  }

  private getAbnormalColor(flag: string): string {
    return ABNORMAL_COLOR[flag] || '#666666';
  }

  private formatAmount(amount: number): string {
    return (amount || 0).toFixed(2);
  }

  private getStatusColor(status: string): string {
    return STATUS_COLOR_MAP[status] || '#FAAD14';
  }

  private getStatusBgColor(status: string): string {
    return STATUS_BG_MAP[status] || '#FFF7E6';
  }

  private shouldShowAbnormal(flag: string | undefined): boolean {
    return flag !== undefined && flag !== null && flag !== '正常' && flag !== 'NORMAL' && flag !== '';
  }

  private getStatusText(): string {
    return this.status || '未知';
  }

  private getReferenceText(ind: Indicator): string {
    if (ind.referenceText) {
      return `参考范围: ${ind.referenceText}`;
    }
    if (ind.referenceMin !== undefined || ind.referenceMax !== undefined) {
      let refText: string = '参考范围: ';
      if (ind.referenceMin !== undefined) {
        refText += ind.referenceMin;
      }
      if (ind.referenceMax !== undefined) {
        if (ind.referenceMin !== undefined) {
          refText += ` - ${ind.referenceMax}`;
        } else {
          refText += ind.referenceMax;
        }
      }
      return refText;
    }
    return '';
  }

  private hasReportDetail(itemId: string): boolean {
    return this.reportDetails.has(itemId);
  }

  private isLoading(itemId: string): boolean {
    return this.loadingItems.has(itemId);
  }

  private isExpanded(itemId: string): boolean {
    return this.expandedItemId === itemId;
  }

  private getItemStatusColor(status: string): string {
    return status === '已完成' ? '#52C41A' : '#8C8C8C';
  }

  // 获取异常标记（处理 undefined）
  private getAbnormalFlag(flag: string | undefined): string {
    return flag || '';
  }

  // 获取展开按钮文本
  private getExpandText(itemId: string): string {
    return this.isExpanded(itemId) ? '收起详情 ▲' : '查看报告 ▼';
  }

  // 判断是否有报告内容可显示
  private hasReportContent(itemId: string): boolean {
    return this.hasReportDetail(itemId) && this.getReportDetail(itemId) !== undefined;
  }

  // 获取报告详情（供 @Builder 调用）
  private getReportDetail(itemId: string): ReportDetail | undefined {
    return this.reportDetails.get(itemId);
  }

  @Builder
  private AbnormalTag(flag: string) {
    if (this.shouldShowAbnormal(flag)) {
      Row() {
        Text(flag)
          .fontSize(12)
          .fontColor(this.getAbnormalColor(flag))
          .fontWeight(FontWeight.Medium)
      }
      .padding({ left: 10, right: 10, top: 3, bottom: 3 })
      .backgroundColor(`${this.getAbnormalColor(flag)}15`)
      .borderRadius(10)
    }
  }

  @Builder
  private IndicatorItem(ind: Indicator) {
    Column() {
      Row() {
        Text(ind.indicatorName)
          .fontSize(13)
          .fontColor('#333333')
          .layoutWeight(1)

        if (this.shouldShowAbnormal(ind.abnormalFlag)) {
          this.AbnormalTag(ind.abnormalFlag)
        }
      }
      .width('100%')

      Row() {
        Text(ind.resultValue || '-')
          .fontSize(14)
          .fontWeight(FontWeight.Medium)
          .fontColor('#333333')

        if (ind.unit) {
          Text(ind.unit)
            .fontSize(12)
            .fontColor('#8C8C8C')
            .margin({ left: 4 })
        }

        Blank()

        if (this.getReferenceText(ind) !== '') {
          Text(this.getReferenceText(ind))
            .fontSize(11)
            .fontColor('#8C8C8C')
        }
      }
      .width('100%')
      .margin({ top: 2 })
    }
    .width('100%')
    .padding(10)
    .backgroundColor('#FFFFFF')
    .borderRadius(6)
    .margin({ bottom: 6 })
  }

  @Builder
  private ReportContent(detail: ReportDetail) {
    Column() {
      // 结果摘要
      if (detail.resultSummary) {
        Column() {
          Text('📝 结果摘要')
            .fontSize(13)
            .fontWeight(FontWeight.Medium)
            .fontColor('#333333')
            .width('100%')
            .margin({ bottom: 4 })
          Text(detail.resultSummary)
            .fontSize(13)
            .fontColor('#555555')
            .width('100%')
            .padding(10)
            .backgroundColor('#F5F5F5')
            .borderRadius(6)
        }
        .width('100%')
        .margin({ bottom: 10 })
      }

      // 报告结论
      if (detail.conclusion) {
        Column() {
          Text('📋 报告结论')
            .fontSize(13)
            .fontWeight(FontWeight.Medium)
            .fontColor('#333333')
            .width('100%')
            .margin({ bottom: 4 })
          Text(detail.conclusion)
            .fontSize(13)
            .fontColor('#555555')
            .width('100%')
            .padding(10)
            .backgroundColor('#F5F5F5')
            .borderRadius(6)
        }
        .width('100%')
        .margin({ bottom: 10 })
      }

      // 检验指标
      if (detail.itemCategory === 'LAB' && detail.indicators && detail.indicators.length > 0) {
        Column() {
          Text('🔬 检验指标')
            .fontSize(13)
            .fontWeight(FontWeight.Medium)
            .fontColor('#333333')
            .width('100%')
            .margin({ bottom: 6 })

          ForEach(detail.indicators, (ind: Indicator) => {
            this.IndicatorItem(ind)
          }, (ind: Indicator) => ind.indicatorId)
        }
        .width('100%')
        .margin({ bottom: 10 })
      }

      // 报告时间
      if (detail.reportTime) {
        Text(`报告时间: ${detail.reportTime}`)
          .fontSize(12)
          .fontColor('#999999')
          .width('100%')
          .margin({ top: 6 })
      }
    }
    .width('100%')
  }

  @Builder
  private ItemCard(item: MedicalOrderItem) {
    Column() {
      // 项目头部
      Row() {
        Column() {
          Row() {
            Text(item.itemName)
              .fontSize(15)
              .fontWeight(FontWeight.Medium)
              .fontColor('#333333')

            if (item.urgencyLevel && item.urgencyLevel !== '常规') {
              Text(item.urgencyLevel)
                .fontSize(10)
                .fontColor('#FF4D4F')
                .backgroundColor('#FFF1F0')
                .padding({ left: 6, right: 6, top: 1, bottom: 1 })
                .borderRadius(8)
                .margin({ left: 6 })
            }
          }

          Row() {
            Text(this.getCategoryText(item.itemCategory))
              .fontSize(12)
              .fontColor('#8C8C8C')

            // 修复：传递处理后的字符串，确保不是 undefined
            if (this.shouldShowAbnormal(item.abnormalFlag)) {
              this.AbnormalTag(this.getAbnormalFlag(item.abnormalFlag))
            }
          }
          .margin({ top: 2 })
        }
        .alignItems(HorizontalAlign.Start)

        Blank()

        Column() {
          Text(item.status || '未知')
            .fontSize(13)
            .fontColor(this.getItemStatusColor(item.status))
            .fontWeight(FontWeight.Medium)

          Text(`¥${this.formatAmount(item.price)}`)
            .fontSize(13)
            .fontColor('#EF4444')
            .margin({ top: 2 })
        }
        .alignItems(HorizontalAlign.End)
      }
      .width('100%')

      // 展开按钮
      Row() {
        Text(this.getExpandText(item.orderItemId))
          .fontSize(12)
          .fontColor('#007AFF')
      }
      .width('100%')
      .margin({ top: 8 })
      .onClick(() => {
        this.toggleExpand(item.orderItemId)
      })

      // 展开内容
      if (this.isExpanded(item.orderItemId)) {
        Divider()
          .color('#F0F0F0')
          .margin({ top: 10, bottom: 10 })

        if (this.isLoading(item.orderItemId)) {
          Row() {
            LoadingProgress()
              .width(20)
              .height(20)
              .color('#007AFF')
            Text('加载报告中...')
              .fontSize(13)
              .fontColor('#999999')
              .margin({ left: 10 })
          }
          .width('100%')
          .height(60)
          .justifyContent(FlexAlign.Center)
        } else if (this.hasReportContent(item.orderItemId)) {
          // 修复：在 @Builder 中不能使用 const，直接调用方法获取值
          // 使用状态变量或方法调用
          this.ReportContent(this.getReportDetail(item.orderItemId)!)
        } else {
          Text('暂无报告内容')
            .fontSize(13)
            .fontColor('#999999')
            .width('100%')
            .textAlign(TextAlign.Center)
            .padding(20)
        }
      }
    }
    .width('100%')
    .padding(14)
    .backgroundColor('#F8FAFF')
    .borderRadius(10)
    .margin({ bottom: 10 })
  }

  @Builder
  private StatusTag() {
    Row() {
      Circle()
        .width(6)
        .height(6)
        .fill(this.getStatusColor(this.status))
        .margin({ right: 4 })
      Text(this.getStatusText())
        .fontSize(13)
        .fontColor(this.getStatusColor(this.status))
        .fontWeight(FontWeight.Medium)
    }
    .padding({ left: 12, right: 12, top: 4, bottom: 4 })
    .backgroundColor(this.getStatusBgColor(this.status))
    .borderRadius(12)
  }

  build() {
    Column() {
      // 顶部导航
      Row() {
        Text('‹')
          .fontSize(28)
          .fontColor('#333333')
          .onClick(() => {
            this.goBack();
          })

        Text('检查报告详情')
          .fontSize(20)
          .fontWeight(FontWeight.Bold)
          .fontColor('#333333')
          .layoutWeight(1)
          .textAlign(TextAlign.Center)

        Row()
          .width(28)
          .height(28)
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

      // 内容
      List() {
        // 基本信息
        ListItem() {
          Column() {
            Row() {
              Column() {
                Text(`就诊日期: ${this.visitDate || '未知'}`)
                  .fontSize(14)
                  .fontColor('#1A1A1A')
                  .fontWeight(FontWeight.Medium)
                Text(`医生: ${this.doctorName || '未知医生'}`)
                  .fontSize(13)
                  .fontColor('#8C8C8C')
                  .margin({ top: 2 })
                Text(`患者: ${this.patientName || '未知'}`)
                  .fontSize(13)
                  .fontColor('#8C8C8C')
                  .margin({ top: 2 })
              }
              .alignItems(HorizontalAlign.Start)

              Blank()

              Column() {
                this.StatusTag()
                Text(`总计 ¥${this.formatAmount(this.totalAmount)}`)
                  .fontSize(14)
                  .fontColor('#EF4444')
                  .fontWeight(FontWeight.Bold)
                  .margin({ top: 4 })
              }
              .alignItems(HorizontalAlign.End)
            }
            .width('100%')
          }
          .width('100%')
          .padding(16)
          .backgroundColor('#FFFFFF')
          .borderRadius(12)
          .margin({ top: 12, bottom: 12 })
        }

        // 检查项目列表
        ListItem() {
          Column() {
            Row() {
              Text('📋 检查项目')
                .fontSize(16)
                .fontWeight(FontWeight.Bold)
                .fontColor('#333333')

              Blank()

              Text(`共 ${this.items.length} 项`)
                .fontSize(13)
                .fontColor('#999999')
            }
            .width('100%')
            .margin({ bottom: 12 })

            ForEach(this.items, (item: MedicalOrderItem) => {
              this.ItemCard(item)
            }, (item: MedicalOrderItem) => item.orderItemId)
          }
          .width('100%')
          .padding(16)
          .backgroundColor('#FFFFFF')
          .borderRadius(12)
          .margin({ bottom: 20 })
        }
      }
      .width('100%')
      .padding({ left: 16, right: 16 })
      .backgroundColor('#F5F5F5')
      .layoutWeight(1)
      .scrollBar(BarState.Off)
    }
    .width('100%')
    .height('100%')
    .backgroundColor('#F5F5F5')
  }
}
// pages/history/MedicalOrderDetail.ets
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
  @State isLoading: boolean = false;

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

  private goToReportDetail(item: MedicalOrderItem): void {
    // 跳转到报告详情页面
    router.pushUrl({
      url: 'pages/history/ReportDetailPage',
      params: {
        orderItemId: item.orderItemId,
        itemName: item.itemName,
        itemCategory: item.itemCategory,
        price: item.price,
        status: item.status,
        abnormalFlag: item.abnormalFlag
      }
    }).catch(() => {
      promptAction.showToast({
        message: '页面跳转失败',
        duration: 1500
      });
    });
  }

  private getCategoryText(category: string): string {
    return CATEGORY_TEXT[category] || category;
  }

  private formatAmount(amount: number): string {
    return (amount || 0).toFixed(2);
  }

  private getStatusColor(status: string): string {
    return STATUS_COLOR_MAP[status] || '#999999';
  }

  private getStatusBgColor(status: string): string {
    return STATUS_BG_MAP[status] || '#F5F5F5';
  }

  private getStatusText(): string {
    return this.status || '未知';
  }

  private formatDate(dateStr: string): string {
    if (!dateStr) return '';
    const parts = dateStr.split('T');
    return parts[0] || dateStr;
  }

  private getItemStatusColor(status: string): string {
    return status === '已完成' ? '#52C41A' : '#8C8C8C';
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
        .fontSize(12)
        .fontColor(this.getStatusColor(this.status))
        .fontWeight(FontWeight.Medium)
    }
    .padding({ left: 10, right: 10, top: 4, bottom: 4 })
    .backgroundColor(this.getStatusBgColor(this.status))
    .borderRadius(12)
  }

  @Builder
  private ItemCard(item: MedicalOrderItem) {
    Row() {
      Column() {
        Row() {
          Text(item.itemName)
            .fontSize(15)
            .fontWeight(FontWeight.Medium)
            .fontColor('#1A1A1A')

          Text(this.getCategoryText(item.itemCategory))
            .fontSize(11)
            .fontColor('#8C8C8C')
            .padding({ left: 8, right: 8, top: 2, bottom: 2 })
            .backgroundColor('#F5F5F5')
            .borderRadius(10)
            .margin({ left: 6 })
        }

        Text(item.status || '未知')
          .fontSize(12)
          .fontColor(this.getItemStatusColor(item.status))
          .margin({ top: 2 })
      }
      .alignItems(HorizontalAlign.Start)

      Blank()

      Column() {
        Text(`¥${this.formatAmount(item.price)}`)
          .fontSize(16)
          .fontColor('#EF4444')
          .fontWeight(FontWeight.Bold)

        Row() {
          Text('查看详情')
            .fontSize(12)
            .fontColor('#007AFF')
          Text('›')
            .fontSize(14)
            .fontColor('#007AFF')
            .margin({ left: 2 })
        }
        .margin({ top: 4 })
        .padding({ left: 10, right: 4, top: 4, bottom: 4 })
        .backgroundColor('#E6F7FF')
        .borderRadius(12)
      }
      .alignItems(HorizontalAlign.End)
    }
    .width('100%')
    .padding(14)
    .backgroundColor('#FFFFFF')
    .borderRadius(10)
    .onClick(() => {
      this.goToReportDetail(item);
    })
  }

  @Builder
  private LoadingState() {
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
  }

  build() {
    Column() {
      // 顶部导航栏 - 统一风格
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

        Text('检查报告详情')
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

      // 内容
      if (this.isLoading) {
        this.LoadingState()
      } else {
        List({ space: 12 }) {
          // 基本信息卡片
          ListItem() {
            Column() {
              Row() {
                Column() {
                  Text('📋 报告概览')
                    .fontSize(15)
                    .fontWeight(FontWeight.Medium)
                    .fontColor('#1A1A2E')
                    .margin({ bottom: 8 })

                  Row() {
                    Text('📅 就诊日期：')
                      .fontSize(13)
                      .fontColor('#8C8C8C')
                    Text(this.visitDate ? this.formatDate(this.visitDate) : '未知日期')
                      .fontSize(13)
                      .fontColor('#1A1A1A')
                  }
                  .width('100%')

                  Row() {
                    Text('👨‍⚕️ 医生：')
                      .fontSize(13)
                      .fontColor('#8C8C8C')
                    Text(this.doctorName || '未知医生')
                      .fontSize(13)
                      .fontColor('#1A1A1A')
                  }
                  .width('100%')
                  .margin({ top: 2 })

                  Row() {
                    Text('👤 患者：')
                      .fontSize(13)
                      .fontColor('#8C8C8C')
                    Text(this.patientName || '未知患者')
                      .fontSize(13)
                      .fontColor('#1A1A1A')
                  }
                  .width('100%')
                  .margin({ top: 2 })
                }
                .alignItems(HorizontalAlign.Start)

                Blank()

                Column() {
                  this.StatusTag()

                  Text(`合计 ¥${this.formatAmount(this.totalAmount)}`)
                    .fontSize(16)
                    .fontColor('#EF4444')
                    .fontWeight(FontWeight.Bold)
                    .margin({ top: 6 })
                }
                .alignItems(HorizontalAlign.End)
              }
              .width('100%')
            }
            .width('100%')
            .padding(16)
            .backgroundColor('#FFFFFF')
            .borderRadius(12)
            .shadow({
              radius: 8,
              color: 'rgba(0, 0, 0, 0.06)',
              offsetX: 0,
              offsetY: 2
            })
          }

          // 检查项目列表
          ListItem() {
            Column() {
              Row() {
                Text('🔬 检查项目')
                  .fontSize(15)
                  .fontWeight(FontWeight.Medium)
                  .fontColor('#1A1A2E')

                Blank()

                Text(`共 ${this.items.length} 项`)
                  .fontSize(12)
                  .fontColor('#8C8C8C')
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
            .shadow({
              radius: 8,
              color: 'rgba(0, 0, 0, 0.06)',
              offsetX: 0,
              offsetY: 2
            })
          }
          .margin({ bottom: 8 })
        }
        .width('100%')
        .padding({ left: 16, right: 16, top: 12 })
        .backgroundColor('#F5F5F5')
        .layoutWeight(1)
        .scrollBar(BarState.Off)
      }
    }
    .width('100%')
    .height('100%')
    .backgroundColor('#FFFFFF')
  }
}
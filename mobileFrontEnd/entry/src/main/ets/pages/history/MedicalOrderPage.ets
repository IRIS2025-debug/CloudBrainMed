// pages/medical/MedicalOrderPage.ets
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

interface MedicalOrderGroup {
  registerId: string;
  patientName: string;
  doctorName: string;
  visitDate: string;
  totalAmount: number;
  itemCount: number;
  status: string;
  items: MedicalOrderItem[];
}

interface ApiResponse<T> {
  code: number;
  msg: string;
  data: T;
}

// ==================== 常量 ====================

const STATUS_COLOR: Record<string, string> = {
  '待检查': '#FAAD14',
  '检查中': '#1890FF',
  '已完成': '#52C41A',
  '已取消': '#FF4D4F'
};

const STATUS_BG_COLOR: Record<string, string> = {
  '待检查': '#FFF7E6',
  '检查中': '#E6F7FF',
  '已完成': '#F6FFED',
  '已取消': '#FFF1F0'
};

// ==================== 页面组件 ====================

@Entry
@Component
export struct MedicalOrderPage {
  @State patientId: string = '';
  @State userName: string = '';
  @State groups: MedicalOrderGroup[] = [];
  @State isLoading: boolean = true;
  @State errorMessage: string = '';

  private baseUrl: string = APIConfig.PATIENT_SERVICE_URL;

  aboutToAppear(): void {
    const params = router.getParams() as Record<string, Object>;
    if (params) {
      this.patientId = (params['patientId'] as string) || '';
      this.userName = (params['userName'] as string) || (params['name'] as string) || '';
    }
    this.loadOrders();
  }

  async loadOrders(): Promise<void> {
    if (!this.patientId) {
      this.isLoading = false;
      this.errorMessage = '未获取到患者信息，请重新登录';
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';

    try {
      const context = getContext(this);
      const url = `${this.baseUrl}/medical-order/grouped/${this.patientId}`;
      const result = await HttpUtil.get<ApiResponse<MedicalOrderGroup[]>>(url, context);

      if (result.code === 200 && result.data) {
        this.groups = result.data;
      } else {
        this.errorMessage = result.msg || '加载失败';
        this.groups = [];
      }
    } catch (error) {
      console.error('加载检查报告异常:', JSON.stringify(error));
      this.errorMessage = '网络请求失败，请检查网络连接';
      this.groups = [];
    } finally {
      this.isLoading = false;
    }
  }

  private goBack(): void {
    router.back();
  }

  private goToDetail(group: MedicalOrderGroup): void {
    router.pushUrl({
      url: 'pages/medical/MedicalOrderDetail',
      params: {
        registerId: group.registerId,
        patientName: group.patientName,
        doctorName: group.doctorName,
        visitDate: group.visitDate,
        totalAmount: group.totalAmount,
        status: group.status,
        items: group.items
      }
    }).catch(() => {
      promptAction.showToast({
        message: '页面跳转失败',
        duration: 1500
      });
    });
  }

  private getStatusColor(status: string): string {
    return STATUS_COLOR[status] || '#999999';
  }

  private getStatusBgColor(status: string): string {
    return STATUS_BG_COLOR[status] || '#F5F5F5';
  }

  private formatAmount(amount: number): string {
    return (amount || 0).toFixed(2);
  }

  private getItemNames(items: MedicalOrderItem[]): string {
    if (!items || items.length === 0) {
      return '暂无项目';
    }
    return items.map(item => item.itemName).join('、');
  }

  @Builder
  private StatusTag(status: string) {
    Row() {
      Circle()
        .width(6)
        .height(6)
        .fill(this.getStatusColor(status))
        .margin({ right: 4 })
      Text(status)
        .fontSize(12)
        .fontColor(this.getStatusColor(status))
        .fontWeight(FontWeight.Medium)
    }
    .padding({ left: 10, right: 10, top: 4, bottom: 4 })
    .backgroundColor(this.getStatusBgColor(status))
    .borderRadius(12)
  }

  @Builder
  private OrderCard(group: MedicalOrderGroup) {
    Column() {
      Row() {
        Column() {
          Text(group.visitDate || '未知日期')
            .fontSize(15)
            .fontColor('#1A1A1A')
            .fontWeight(FontWeight.Medium)
          Text(`医生: ${group.doctorName || '未知医生'}`)
            .fontSize(13)
            .fontColor('#8C8C8C')
            .margin({ top: 2 })
        }
        .alignItems(HorizontalAlign.Start)

        Blank()

        this.StatusTag(group.status)
      }
      .width('100%')

      Divider()
        .color('#F0F0F0')
        .margin({ top: 10, bottom: 10 })

      Row() {
        Text('📋')
          .fontSize(18)
          .margin({ right: 8 })
        Text(this.getItemNames(group.items))
          .fontSize(14)
          .fontColor('#333333')
          .maxLines(1)
          .textOverflow({ overflow: TextOverflow.Ellipsis })
          .layoutWeight(1)
      }
      .width('100%')

      Row() {
        Text(`共 ${group.itemCount} 个项目`)
          .fontSize(13)
          .fontColor('#8C8C8C')

        Blank()

        Text(`¥${this.formatAmount(group.totalAmount)}`)
          .fontSize(18)
          .fontColor('#EF4444')
          .fontWeight(FontWeight.Bold)

        Text('›')
          .fontSize(18)
          .fontColor('#CCCCCC')
          .margin({ left: 4 })
      }
      .width('100%')
      .margin({ top: 8 })
    }
    .width('100%')
    .padding({ left: 16, right: 16, top: 14, bottom: 14 })
    .backgroundColor('#FFFFFF')
    .borderRadius(12)
    .shadow({
      radius: 8,
      color: 'rgba(0, 0, 0, 0.06)',
      offsetX: 0,
      offsetY: 2
    })
    .margin({ bottom: 12 })
    .onClick(() => {
      this.goToDetail(group);
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

  @Builder
  private ErrorState() {
    Column() {
      Text('⚠️')
        .fontSize(48)
        .margin({ bottom: 16 })
      Text(this.errorMessage)
        .fontSize(14)
        .fontColor('#8C8C8C')
        .textAlign(TextAlign.Center)
        .margin({ bottom: 20 })
      Button('重新加载')
        .fontSize(14)
        .fontColor('#FFFFFF')
        .backgroundColor('#007AFF')
        .borderRadius(20)
        .height(36)
        .padding({ left: 24, right: 24 })
        .onClick(() => {
          this.loadOrders();
        })
    }
    .width('100%')
    .height(200)
    .justifyContent(FlexAlign.Center)
    .alignItems(HorizontalAlign.Center)
  }

  @Builder
  private EmptyState() {
    Column() {
      Text('📋')
        .fontSize(56)
        .margin({ bottom: 16 })
      Text('暂无检查报告')
        .fontSize(18)
        .fontColor('#1A1A1A')
        .fontWeight(FontWeight.Medium)
      Text('您当前没有检查或检验记录')
        .fontSize(14)
        .fontColor('#8C8C8C')
        .margin({ top: 4 })
    }
    .width('100%')
    .padding(60)
    .justifyContent(FlexAlign.Center)
    .alignItems(HorizontalAlign.Center)
  }

  build() {
    Column() {
      // 顶部导航栏
      Row() {
        Text('‹')
          .fontSize(28)
          .fontColor('#333333')
          .onClick(() => {
            this.goBack();
          })

        Text('检查报告')
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

      // 内容区域
      if (this.isLoading) {
        this.LoadingState()
      } else if (this.errorMessage) {
        this.ErrorState()
      } else if (this.groups.length === 0) {
        this.EmptyState()
      } else {
        List() {
          ListItem() {
            Row() {
              Text(`共 ${this.groups.length} 次检查`)
                .fontSize(13)
                .fontColor('#999999')
            }
            .width('100%')
            .padding({ left: 4, top: 4, bottom: 8 })
          }

          ForEach(this.groups, (group: MedicalOrderGroup) => {
            ListItem() {
              this.OrderCard(group)
            }
          }, (group: MedicalOrderGroup) => group.registerId)
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
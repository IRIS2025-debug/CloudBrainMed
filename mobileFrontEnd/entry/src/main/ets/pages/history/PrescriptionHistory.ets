// pages/medical/PrescriptionHistory.ets
import router from '@ohos.router';
import promptAction from '@ohos.promptAction';
import { HttpUtil } from '../common/HttpUtil';
import { APIConfig } from '../common/APIConfig';

// ==================== 类型定义 ====================

interface PrescriptionItem {
  prescriptionId: string;
  registerId: string;
  patientId: string;
  doctorId: string;
  medicineId: string;
  patientName: string;
  doctorName: string;
  medicineName: string;
  spec: string;
  usage_: string;
  num: number;
  prescriptionDate: string;
  price: number;
  payStatus: string;
  createTime: string;
}

interface RegisterPrescriptionGroup {
  registerId: string;
  patientName: string;
  doctorName: string;
  prescriptionDate: string;
  totalAmount: number;
  prescriptionCount: number;
  payStatus: string;
  prescriptions: PrescriptionItem[];
}

interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
}

// ==================== 常量 ====================

const STATUS_COLOR: Record<string, string> = {
  'WAITING': '#FAAD14',
  'PAID': '#52C41A',
  'CANCELLED': '#FF4D4F',
  'REFUNDED': '#999999'
};

const STATUS_BG_COLOR: Record<string, string> = {
  'WAITING': '#FFF7E6',
  'PAID': '#F6FFED',
  'CANCELLED': '#FFF1F0',
  'REFUNDED': '#F5F5F5'
};

const STATUS_TEXT: Record<string, string> = {
  'WAITING': '待支付',
  'PAID': '已支付',
  'CANCELLED': '已取消',
  'REFUNDED': '已退款'
};

// ==================== 页面组件 ====================

@Entry
@Component
export struct PrescriptionHistory {
  @State patientId: string = '';
  @State userName: string = '';
  @State groups: RegisterPrescriptionGroup[] = [];
  @State isLoading: boolean = true;
  @State errorMessage: string = '';

  private baseUrl: string = APIConfig.PATIENT_SERVICE_URL;

  aboutToAppear(): void {
    const params = router.getParams() as Record<string, Object>;
    if (params) {
      this.patientId = (params['patientId'] as string) || '';
      this.userName = (params['userName'] as string) || (params['name'] as string) || '';
    }
    this.loadPrescriptionHistory();
  }

  async loadPrescriptionHistory(): Promise<void> {
    if (!this.patientId) {
      this.isLoading = false;
      this.errorMessage = '未获取到患者信息，请重新登录';
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';

    try {
      const context = getContext(this);
      const url = `${this.baseUrl}/prescription/grouped/${this.patientId}`;
      const result = await HttpUtil.get<ApiResponse<RegisterPrescriptionGroup[]>>(url, context);

      if (result.code === 200 && result.data) {
        this.groups = result.data;
        console.info('处方数据:', JSON.stringify(this.groups));
      } else {
        this.errorMessage = result.message || '加载失败';
        this.groups = [];
      }
    } catch (error) {
      console.error('加载处方历史异常:', JSON.stringify(error));
      this.errorMessage = '网络请求失败，请检查网络连接';
      this.groups = [];
    } finally {
      this.isLoading = false;
    }
  }

  private goBack(): void {
    router.back();
  }

  private goToDetail(group: RegisterPrescriptionGroup): void {
    router.pushUrl({
      url: 'pages/history/PrescriptionDetail',
      params: {
        registerId: group.registerId,
        patientName: group.patientName,
        doctorName: group.doctorName,
        prescriptionDate: group.prescriptionDate,
        totalAmount: group.totalAmount,
        payStatus: group.payStatus,
        prescriptions: group.prescriptions
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

  private getStatusText(status: string): string {
    return STATUS_TEXT[status] || status;
  }

  private formatAmount(amount: number): string {
    return (amount || 0).toFixed(2);
  }

  private getMedicineNames(prescriptions: PrescriptionItem[]): string {
    if (!prescriptions || prescriptions.length === 0) {
      return '暂无药品';
    }
    return prescriptions.map(p => p.medicineName).join('、');
  }

  private formatDate(dateStr: string): string {
    if (!dateStr) return '';
    const parts = dateStr.split('T');
    return parts[0] || dateStr;
  }

  @Builder
  private StatusTag(status: string) {
    Row() {
      Circle()
        .width(6)
        .height(6)
        .fill(this.getStatusColor(status))
        .margin({ right: 4 })
      Text(this.getStatusText(status))
        .fontSize(12)
        .fontColor(this.getStatusColor(status))
        .fontWeight(FontWeight.Medium)
    }
    .padding({ left: 10, right: 10, top: 4, bottom: 4 })
    .backgroundColor(this.getStatusBgColor(status))
    .borderRadius(12)
  }

  @Builder
  private PrescriptionCard(group: RegisterPrescriptionGroup) {
    Column() {
      Row() {
        Column() {
          Text(this.formatDate(group.prescriptionDate) || '未知日期')
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

        this.StatusTag(group.payStatus)
      }
      .width('100%')

      Divider()
        .color('#F0F0F0')
        .margin({ top: 10, bottom: 10 })

      Row() {
        Text('💊')
          .fontSize(18)
          .margin({ right: 8 })
        Text(this.getMedicineNames(group.prescriptions))
          .fontSize(14)
          .fontColor('#333333')
          .maxLines(1)
          .textOverflow({ overflow: TextOverflow.Ellipsis })
          .layoutWeight(1)
      }
      .width('100%')

      Row() {
        Text(`共 ${group.prescriptionCount} 种药品`)
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
          this.loadPrescriptionHistory();
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
      Text('💊')
        .fontSize(56)
        .margin({ bottom: 16 })
      Text('暂无处方记录')
        .fontSize(18)
        .fontColor('#1A1A1A')
        .fontWeight(FontWeight.Medium)
      Text('您当前没有处方历史记录')
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
      // 顶部导航栏 - 使用统一风格
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

        Text('处方记录')
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
              Text(`共 ${this.groups.length} 张处方`)
                .fontSize(13)
                .fontColor('#999999')
            }
            .width('100%')
            .padding({ left: 4, top: 4, bottom: 8 })
          }

          ForEach(this.groups, (group: RegisterPrescriptionGroup) => {
            ListItem() {
              this.PrescriptionCard(group)
            }
          }, (group: RegisterPrescriptionGroup) => group.registerId)
        }
        .width('100%')
        .padding({ left: 16, right: 16, top: 12 })
        .backgroundColor('#F5F5F5')
        .layoutWeight(1)
      }
    }
    .width('100%')
    .height('100%')
    .backgroundColor('#FFFFFF')
  }
}
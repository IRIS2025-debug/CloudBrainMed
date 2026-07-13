// pages/history/RegisterDetail.ets
import router from '@ohos.router';
import promptAction from '@ohos.promptAction';
import { HttpUtil } from '../common/HttpUtil';
import { APIConfig } from '../common/APIConfig';

// ===== 数据模型定义 =====

interface RegisterRecord {
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
  consultStatus: string;
}

interface RegisterReport {
  recordId: string;
  patientId: string;
  doctorId: string;
  registerId: string;
  doctorName: string;
  patientName: string;
  visitAge: number;
  description: string;
  visitDate: string;
  payStatus: string;
  createTime: string;
}

interface MedicalOrderItem {
  orderItemId: string;
  orderId: string;
  itemId: string;
  itemCode: string;
  itemName: string;
  itemCategory: string;
  assignedDeptId: string;
  urgencyLevel: string;
  price: number;
  status: string;
  createTime: string;
  updateTime: string;
}

interface MedicalOrder {
  orderId: string;
  patientId: string;
  registerId: string;
  doctorId: string;
  clinicalSummary: string;
  urgencyLevel: string;
  sourceType: string;
  aiTraceId: string;
  status: string;
  payStatus: string;
  confirmedTime: string;
  createTime: string;
  updateTime: string;
}

interface Prescription {
  prescriptionId: string;
  registerId: string;
  patientId: string;
  doctorId: string;
  medicineId: string;
  patientName: string;
  doctorName: string;
  medicineName: string;
  spec: string;
  usage: string;
  num: number;
  createDate: string;
  price: number;
  payStatus: string;
  createTime: string;
}

interface VisitDetail {
  register: RegisterRecord | null;
  report: RegisterReport | null;
  orders: MedicalOrder[];
  orderItems: MedicalOrderItem[];
  prescriptions: Prescription[];
}

interface ApiResponse<T> {
  code: number;
  msg: string;
  data: T;
}

// ===== 常量 =====

const STATUS_COLOR: Record<string, string> = {
  'WAITING': '#FF9F43',
  'PENDING': '#FF9F43',
  'CONFIRMED': '#4A90D9',
  'COMPLETED': '#2ECC71',
  'CANCELLED': '#95A5A6'
};

const STATUS_BG_COLOR: Record<string, string> = {
  'WAITING': '#FFF3E0',
  'PENDING': '#FFF3E0',
  'CONFIRMED': '#E8F0FE',
  'COMPLETED': '#E8F8F0',
  'CANCELLED': '#F0F0F0'
};

const STATUS_TEXT: Record<string, string> = {
  'WAITING': '候诊中',
  'PENDING': '待就诊',
  'CONFIRMED': '已确认',
  'COMPLETED': '已完成',
  'CANCELLED': '已取消'
};

const PAY_STATUS_TEXT: Record<string, string> = {
  'WAITING': '待支付',
  'PAID': '已支付',
  'REFUNDED': '已退款'
};

const PAY_STATUS_COLOR: Record<string, string> = {
  'WAITING': '#4A90D9',
  'PAID': '#2ECC71',
  'REFUNDED': '#95A5A6'
};

const PAY_STATUS_BG: Record<string, string> = {
  'WAITING': '#E8F0FE',
  'PAID': '#E8F8F0',
  'REFUNDED': '#F0F0F0'
};

const ORDER_STATUS_COLOR: Record<string, string> = {
  'WAITING_ASSIGN': '#FF9F43',
  'ASSIGNED': '#4A90D9',
  'IN_PROGRESS': '#722ED1',
  'COMPLETED': '#2ECC71',
  'CANCELLED': '#95A5A6'
};

const ORDER_STATUS_TEXT: Record<string, string> = {
  'WAITING_ASSIGN': '待分配',
  'ASSIGNED': '已分配',
  'IN_PROGRESS': '检查中',
  'COMPLETED': '已完成',
  'CANCELLED': '已取消'
};

const URGENCY_TEXT: Record<string, string> = {
  'NORMAL': '常规',
  'URGENT': '加急',
  'EMERGENCY': '紧急'
};

// ===== 折叠卡片组件 =====

@Component
struct CollapsibleCardComponent {
  @Prop title: string = '';
  @Prop icon: string = '';
  @Prop count: number | undefined = undefined;
  @Prop isExpanded: boolean = false;
  onToggle: () => void = () => {};
  @BuilderParam contentBuilder: () => void = this.defaultContent;

  @Builder
  defaultContent() {
    Text('空内容')
      .fontSize(14)
      .fontColor('#C7C7CC')
  }

  private getExpandText(isExpanded: boolean): string {
    return isExpanded ? '收起' : '展开';
  }

  private getExpandTextColor(isExpanded: boolean): string {
    return isExpanded ? '#8E8E93' : '#4A90D9';
  }

  private getExpandedIcon(isExpanded: boolean): string {
    return isExpanded ? '▾' : '▸';
  }

  build() {
    Column() {
      // 标题栏
      Row() {
        Row() {
          Text(this.icon)
            .fontSize(18)
            .margin({ right: 8 })
          Text(this.title)
            .fontSize(16)
            .fontWeight(FontWeight.Medium)
            .fontColor('#1A1A2E')
          if (this.count !== undefined && this.count > 0) {
            Text('(' + this.count + ')')
              .fontSize(13)
              .fontColor('#4A90D9')
              .margin({ left: 4 })
          }
        }

        Blank()

        Row() {
          Text(this.getExpandText(this.isExpanded))
            .fontSize(12)
            .fontColor(this.getExpandTextColor(this.isExpanded))
            .margin({ right: 6 })
          Text(this.getExpandedIcon(this.isExpanded))
            .fontSize(16)
            .fontColor(this.getExpandTextColor(this.isExpanded))
        }
      }
      .width('100%')
      .padding({ left: 16, right: 16, top: 14, bottom: 14 })
      .onClick(() => {
        this.onToggle();
      })

      Divider()
        .color('#F0F0F0')
        .margin({ left: 16, right: 16 })

      // 内容区域 - 使用 Column 包裹，并且 padding 在内容区域内部处理
      if (this.isExpanded) {
        Column() {
          this.contentBuilder()
        }
        .width('100%')
        .padding({ left: 16, right: 16, bottom: 16, top: 12 })
      }
    }
    .width('100%')
    .backgroundColor('#FFFFFF')
    .borderRadius(14)
    .shadow({
      radius: 6,
      color: 'rgba(0,0,0,0.04)',
      offsetY: 2
    })
    .margin({ bottom: 12 })
  }
}

// ===== 页面组件 =====

@Entry
@Component
export struct RegisterDetail {
  @State registerId: string = '';
  @State patientName: string = '';
  @State isLoading: boolean = true;
  @State errorMessage: string = '';
  @State detail: VisitDetail | null = null;

  // 默认全部收起
  @State isBasicInfoExpanded: boolean = false;
  @State isReportExpanded: boolean = false;
  @State isOrderExpanded: boolean = false;
  @State isPrescriptionExpanded: boolean = false;

  private baseUrl: string = APIConfig.PATIENT_SERVICE_URL;

  aboutToAppear(): void {
    const params = router.getParams() as Record<string, Object>;
    if (params) {
      this.registerId = (params['registerId'] as string) || '';
      this.patientName = (params['patientName'] as string) || '';
    }
    if (this.registerId) {
      this.loadDetail();
    } else {
      this.isLoading = false;
      this.errorMessage = '参数错误，请返回重试';
    }
  }

  async loadDetail(): Promise<void> {
    this.isLoading = true;
    this.errorMessage = '';

    try {
      const context = getContext(this);
      const url = `${this.baseUrl}/register/visit-detail/${this.registerId}`;
      const result = await HttpUtil.get<ApiResponse<VisitDetail>>(url, context);

      if (result.code === 200 && result.data) {
        this.detail = result.data;
        console.info('加载就诊详情成功');
      } else {
        this.errorMessage = result.msg || '加载详情失败';
      }
    } catch (error) {
      console.error('加载就诊详情异常:', JSON.stringify(error));
      this.errorMessage = '网络请求失败，请检查网络连接';
    } finally {
      this.isLoading = false;
    }
  }

  private goBack(): void {
    router.back();
  }

  private formatDate(dateStr: string): string {
    if (!dateStr) return '';
    const parts = dateStr.split('T');
    return parts[0] || dateStr;
  }

  private getGenderText(gender: number): string {
    if (gender === 1) return '男';
    if (gender === 2) return '女';
    return '未知';
  }

  private getStatusColor(status: string): string {
    return STATUS_COLOR[status] || '#95A5A6';
  }

  private getStatusBgColor(status: string): string {
    return STATUS_BG_COLOR[status] || '#F0F0F0';
  }

  private getStatusText(status: string): string {
    return STATUS_TEXT[status] || status || '未知';
  }

  private getPayStatusText(status: string): string {
    return PAY_STATUS_TEXT[status] || status || '未知';
  }

  private getPayStatusColor(status: string): string {
    return PAY_STATUS_COLOR[status] || '#95A5A6';
  }

  private getPayStatusBg(status: string): string {
    return PAY_STATUS_BG[status] || '#F0F0F0';
  }

  private getOrderStatusColor(status: string): string {
    return ORDER_STATUS_COLOR[status] || '#95A5A6';
  }

  private getOrderStatusText(status: string): string {
    return ORDER_STATUS_TEXT[status] || status || '未知';
  }

  private getUrgencyLabel(level: string): string {
    return URGENCY_TEXT[level] || level || '常规';
  }

  private calcAge(birthday: string): number {
    if (!birthday) return 0;
    const birth = new Date(birthday);
    const now = new Date();
    let age = now.getFullYear() - birth.getFullYear();
    const m = now.getMonth() - birth.getMonth();
    if (m < 0 || (m === 0 && now.getDate() < birth.getDate())) {
      age--;
    }
    return age;
  }

  private hasOrderItems(orderId: string): boolean {
    if (!this.detail?.orderItems) {
      return false;
    }
    for (let i = 0; i < this.detail.orderItems.length; i++) {
      if (this.detail.orderItems[i].orderId === orderId) {
        return true;
      }
    }
    return false;
  }

  private getOrderItemsByOrderId(orderId: string): MedicalOrderItem[] {
    if (!this.detail?.orderItems) {
      return [];
    }
    const result: MedicalOrderItem[] = [];
    for (let i = 0; i < this.detail.orderItems.length; i++) {
      if (this.detail.orderItems[i].orderId === orderId) {
        result.push(this.detail.orderItems[i]);
      }
    }
    return result;
  }

  // ===== 构建方法 =====

  @Builder
  private StatusTag(status: string) {
    Row() {
      Circle()
        .width(5)
        .height(5)
        .fill(this.getStatusColor(status))
        .margin({ right: 4 })
      Text(this.getStatusText(status))
        .fontSize(11)
        .fontColor(this.getStatusColor(status))
        .fontWeight(FontWeight.Medium)
    }
    .padding({ left: 10, right: 10, top: 3, bottom: 3 })
    .backgroundColor(this.getStatusBgColor(status))
    .borderRadius(12)
  }

  @Builder
  private PayStatusTag(payStatus: string) {
    Row() {
      Circle()
        .width(5)
        .height(5)
        .fill(this.getPayStatusColor(payStatus))
        .margin({ right: 4 })
      Text(this.getPayStatusText(payStatus))
        .fontSize(11)
        .fontColor(this.getPayStatusColor(payStatus))
        .fontWeight(FontWeight.Medium)
    }
    .padding({ left: 10, right: 10, top: 3, bottom: 3 })
    .backgroundColor(this.getPayStatusBg(payStatus))
    .borderRadius(12)
  }

  @Builder
  private InfoItem(label: string, value: string) {
    Column() {
      Text(label)
        .fontSize(12)
        .fontColor('#8E8E93')
        .margin({ bottom: 2 })
      Text(value)
        .fontSize(14)
        .fontColor('#1A1A2E')
        .fontWeight(FontWeight.Medium)
    }
    .width('100%')
    .alignItems(HorizontalAlign.Start)
  }

  // ===== 基础信息卡片内容 =====

  @Builder
  private BasicInfoContent() {
    Column() {
      Row() {
        Column() {
          Text(this.detail?.register?.department || '未知科室')
            .fontSize(18)
            .fontWeight(FontWeight.Bold)
            .fontColor('#1A1A2E')
          Text(this.detail?.register?.consultRoom || '未知诊室')
            .fontSize(14)
            .fontColor('#8E8E93')
            .margin({ top: 2 })
        }
        .alignItems(HorizontalAlign.Start)

        Blank()

        this.StatusTag(this.detail?.register?.consultStatus || '')
      }
      .width('100%')

      Divider()
        .color('#F0F0F0')
        .margin({ top: 12, bottom: 12 })

      Grid() {
        GridItem() {
          this.InfoItem('患者', this.detail?.register?.name || '未知')
        }
        GridItem() {
          this.InfoItem('性别', this.getGenderText(this.detail?.register?.gender || 0))
        }
        GridItem() {
          this.InfoItem('年龄', this.detail?.register?.birthday ? this.calcAge(this.detail.register.birthday) + '岁' : '未知')
        }
        GridItem() {
          this.InfoItem('就诊时间', (this.detail?.register?.visitDate ? this.formatDate(this.detail.register.visitDate) : '') + ' ' + (this.detail?.register?.consultTime || ''))
        }
      }
      .columnsTemplate('1fr 1fr')
      .rowsGap(8)
      .columnsGap(16)

      if (this.detail?.register?.chiefComplaint) {
        Divider()
          .color('#F0F0F0')
          .margin({ top: 8, bottom: 10 })

        Row() {
          Text('主诉')
            .fontSize(13)
            .fontColor('#8E8E93')
            .width(60)
          Text(this.detail.register.chiefComplaint)
            .fontSize(14)
            .fontColor('#1A1A2E')
            .layoutWeight(1)
            .textAlign(TextAlign.End)
        }
        .width('100%')
      }

      Divider()
        .color('#F0F0F0')
        .margin({ top: 10, bottom: 10 })

      Row() {
        this.PayStatusTag(this.detail?.register?.payStatus || '')
        Blank()
        Text('¥' + (this.detail?.register?.price || 0).toFixed(2))
          .fontSize(20)
          .fontWeight(FontWeight.Bold)
          .fontColor('#4A90D9')
      }
      .width('100%')
    }
    .width('100%')
  }

  // ===== 病历信息卡片内容 =====

  @Builder
  private ReportContent() {
    Column() {
      if (this.detail?.report) {
        Row() {
          Text('👨‍⚕️ ' + (this.detail.report.doctorName || '未知医生'))
            .fontSize(14)
            .fontColor('#1A1A2E')
            .fontWeight(FontWeight.Medium)
          Blank()
          Text('就诊年龄：' + (this.detail.report.visitAge || 0) + '岁')
            .fontSize(13)
            .fontColor('#8E8E93')
        }
        .width('100%')
        .margin({ bottom: 10 })

        Text('病历描述')
          .fontSize(12)
          .fontColor('#8E8E93')
          .margin({ bottom: 6 })
          .alignSelf(ItemAlign.Start)

        Text(this.detail.report.description || '无病历描述')
          .fontSize(14)
          .fontColor('#1A1A2E')
          .lineHeight(26)
          .padding(12)
          .backgroundColor('#F8F9FA')
          .borderRadius(8)
          .width('100%')
      } else {
        Column() {
          Text('📝')
            .fontSize(40)
            .margin({ bottom: 12 })
          Text('暂无病历记录')
            .fontSize(15)
            .fontColor('#8E8E93')
        }
        .width('100%')
        .padding({ top: 20, bottom: 20 })
        .alignItems(HorizontalAlign.Center)
      }
    }
    .width('100%')
  }

  // ===== 检查检验卡片内容 =====

  @Builder
  private OrderContent() {
    Column() {
      if (this.detail?.orders && this.detail.orders.length > 0) {
        ForEach(this.detail.orders, (order: MedicalOrder, orderIndex: number) => {
          this.OrderItemBuilder(order, orderIndex)
        }, (order: MedicalOrder) => order.orderId)
      } else {
        Column() {
          Text('🔬')
            .fontSize(40)
            .margin({ bottom: 12 })
          Text('暂无检查检验项')
            .fontSize(15)
            .fontColor('#8E8E93')
        }
        .width('100%')
        .padding({ top: 20, bottom: 20 })
        .alignItems(HorizontalAlign.Center)
      }
    }
    .width('100%')
  }

  @Builder
  private OrderItemBuilder(order: MedicalOrder, orderIndex: number) {
    Column() {
      Row() {
        Row() {
          Text('申请单 #' + (orderIndex + 1))
            .fontSize(14)
            .fontWeight(FontWeight.Medium)
            .fontColor('#1A1A2E')
          Text(this.getUrgencyLabel(order.urgencyLevel))
            .fontSize(10)
            .fontColor('#FFFFFF')
            .backgroundColor(order.urgencyLevel === 'URGENT' ? '#FF6B6B' : '#4A90D9')
            .padding({ left: 8, right: 8, top: 2, bottom: 2 })
            .borderRadius(10)
            .margin({ left: 8 })
        }

        Blank()

        Text(this.getOrderStatusText(order.status))
          .fontSize(11)
          .fontColor('#FFFFFF')
          .backgroundColor(this.getOrderStatusColor(order.status))
          .padding({ left: 10, right: 10, top: 2, bottom: 2 })
          .borderRadius(10)
      }
      .width('100%')

      if (order.clinicalSummary) {
        Text('📝 ' + order.clinicalSummary)
          .fontSize(13)
          .fontColor('#666666')
          .width('100%')
          .margin({ top: 6 })
      }

      this.OrderItemsList(order.orderId)
    }
    .width('100%')
    .padding(14)
    .backgroundColor('#F8F9FA')
    .borderRadius(10)
    .margin({ bottom: 10 })
  }

  @Builder
  private OrderItemsList(orderId: string) {
    Column() {
      if (this.hasOrderItems(orderId)) {
        Divider()
          .color('#F0F0F0')
          .margin({ top: 10, bottom: 10 })

        Text('检查项目')
          .fontSize(12)
          .fontColor('#8E8E93')
          .margin({ bottom: 6 })
          .alignSelf(ItemAlign.Start)

        ForEach(this.getOrderItemsByOrderId(orderId), (item: MedicalOrderItem) => {
          Row() {
            Column() {
              Text(item.itemName || '未知项目')
                .fontSize(14)
                .fontColor('#1A1A2E')
              Text(item.itemCategory || '-')
                .fontSize(12)
                .fontColor('#8E8E93')
            }
            .alignItems(HorizontalAlign.Start)
            .layoutWeight(1)

            Text('¥' + (item.price || 0).toFixed(2))
              .fontSize(14)
              .fontColor('#4A90D9')
              .fontWeight(FontWeight.Medium)
          }
          .width('100%')
          .padding({ top: 5, bottom: 5 })
          .border({
            width: { bottom: 0.5 },
            color: { bottom: '#F0F0F0' }
          })
        }, (item: MedicalOrderItem) => item.orderItemId)
      } else {
        Text('暂无检查项目明细')
          .fontSize(13)
          .fontColor('#C7C7CC')
          .padding({ top: 8 })
          .width('100%')
          .textAlign(TextAlign.Center)
      }
    }
    .width('100%')
  }

  // ===== 处方卡片内容 =====

  @Builder
  private PrescriptionContent() {
    Column() {
      if (this.detail?.prescriptions && this.detail.prescriptions.length > 0) {
        ForEach(this.detail.prescriptions, (prescription: Prescription, index: number) => {
          this.PrescriptionItemBuilder(prescription, index)
        }, (prescription: Prescription) => prescription.prescriptionId)
      } else {
        Column() {
          Text('💊')
            .fontSize(40)
            .margin({ bottom: 12 })
          Text('暂无处方项')
            .fontSize(15)
            .fontColor('#8E8E93')
        }
        .width('100%')
        .padding({ top: 20, bottom: 20 })
        .alignItems(HorizontalAlign.Center)
      }
    }
    .width('100%')
  }

  @Builder
  private PrescriptionItemBuilder(prescription: Prescription, index: number) {
    Column() {
      Row() {
        Row() {
          Text('#' + (index + 1))
            .fontSize(13)
            .fontColor('#2ECC71')
            .fontWeight(FontWeight.Bold)
            .margin({ right: 8 })
          Text(prescription.medicineName || '未知药品')
            .fontSize(15)
            .fontWeight(FontWeight.Medium)
            .fontColor('#1A1A2E')
        }

        Blank()

        Text(this.getPayStatusText(prescription.payStatus))
          .fontSize(11)
          .fontColor('#FFFFFF')
          .backgroundColor(this.getPayStatusColor(prescription.payStatus))
          .padding({ left: 10, right: 10, top: 2, bottom: 2 })
          .borderRadius(10)
      }
      .width('100%')

      Row() {
        Text('规格：' + (prescription.spec || '无'))
          .fontSize(12)
          .fontColor('#8E8E93')
        Blank()
        Text('数量：' + prescription.num + ' 单位')
          .fontSize(12)
          .fontColor('#8E8E93')
      }
      .width('100%')
      .margin({ top: 4 })

      Row() {
        Text('用法：' + (prescription.usage || '无'))
          .fontSize(12)
          .fontColor('#666666')
          .layoutWeight(1)
        Text('¥' + (prescription.price || 0).toFixed(2))
          .fontSize(15)
          .fontColor('#4A90D9')
          .fontWeight(FontWeight.Bold)
      }
      .width('100%')
      .margin({ top: 4 })

      Row() {
        Text('👨‍⚕️ ' + (prescription.doctorName || '未知'))
          .fontSize(11)
          .fontColor('#8E8E93')
        Blank()
        Text(this.formatDate(prescription.createDate))
          .fontSize(11)
          .fontColor('#8E8E93')
      }
      .width('100%')
      .margin({ top: 4 })
    }
    .width('100%')
    .padding(14)
    .backgroundColor('#F8F9FA')
    .borderRadius(10)
    .margin({ bottom: 10 })
  }

  // ===== 状态页 =====

  @Builder
  private LoadingState() {
    Column() {
      LoadingProgress()
        .width(40)
        .height(40)
        .color('#4A90D9')
      Text('加载就诊详情...')
        .fontSize(14)
        .fontColor('#8E8E93')
        .margin({ top: 12 })
    }
    .width('100%')
    .height('100%')
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
        .fontColor('#8E8E93')
        .textAlign(TextAlign.Center)
        .margin({ bottom: 20 })
      Button('重新加载')
        .fontSize(14)
        .fontColor('#FFFFFF')
        .backgroundColor('#4A90D9')
        .borderRadius(20)
        .height(36)
        .padding({ left: 24, right: 24 })
        .onClick(() => {
          this.loadDetail();
        })
    }
    .width('100%')
    .height('100%')
    .justifyContent(FlexAlign.Center)
    .alignItems(HorizontalAlign.Center)
  }

  // ===== 主构建 =====

  build() {
    Column() {
      // 顶部导航栏
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

        Text('就诊详情')
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
      } else if (this.detail) {
        Scroll() {
          Column({ space: 0 }) {
            // 就诊信息卡片 - 默认收起
            CollapsibleCardComponent({
              title: '就诊信息',
              icon: '📋',
              count: undefined,
              isExpanded: this.isBasicInfoExpanded,
              onToggle: () => { this.isBasicInfoExpanded = !this.isBasicInfoExpanded; },
              contentBuilder: () => {
                this.BasicInfoContent();
              }
            })

            // 病历信息卡片 - 默认收起
            CollapsibleCardComponent({
              title: '病历信息',
              icon: '📄',
              count: this.detail?.report ? 1 : 0,
              isExpanded: this.isReportExpanded,
              onToggle: () => { this.isReportExpanded = !this.isReportExpanded; },
              contentBuilder: () => {
                this.ReportContent();
              }
            })

            // 检查检验卡片 - 默认收起
            CollapsibleCardComponent({
              title: '检查检验',
              icon: '🔬',
              count: this.detail?.orders?.length || 0,
              isExpanded: this.isOrderExpanded,
              onToggle: () => { this.isOrderExpanded = !this.isOrderExpanded; },
              contentBuilder: () => {
                this.OrderContent();
              }
            })

            // 处方卡片 - 默认收起
            CollapsibleCardComponent({
              title: '处方项',
              icon: '💊',
              count: this.detail?.prescriptions?.length || 0,
              isExpanded: this.isPrescriptionExpanded,
              onToggle: () => { this.isPrescriptionExpanded = !this.isPrescriptionExpanded; },
              contentBuilder: () => {
                this.PrescriptionContent();
              }
            })

            Row().height(20)
          }
          .width('100%')
          .padding({ left: 16, right: 16, top: 12, bottom: 12 })
        }
        .width('100%')
        .layoutWeight(1)
        .backgroundColor('#F8F9FA')
        .scrollBar(BarState.Off)
      }
    }
    .width('100%')
    .height('100%')
    .backgroundColor('#FFFFFF')
  }
}
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
  'PAID': '已支付 ✓',
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

// ===== 页面组件 =====

@Entry
@Component
export struct RegisterDetail {
  @State registerId: string = '';
  @State patientName: string = '';
  @State isLoading: boolean = true;
  @State errorMessage: string = '';
  @State detail: VisitDetail | null = null;

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
  private SectionTitle(title: string, count?: number, icon: string = '📋') {
    Row() {
      Text(icon + ' ' + title)
        .fontSize(16)
        .fontWeight(FontWeight.Medium)
        .fontColor('#1A1A2E')
      if (count !== undefined && count > 0) {
        Text(' (' + count + ')')
          .fontSize(13)
          .fontColor('#8E8E93')
      }
    }
    .width('100%')
    .margin({ bottom: 12 })
  }

  @Builder
  private InfoRow(label: string, value: string, color: string = '#1A1A2E') {
    Row() {
      Text(label)
        .fontSize(13)
        .fontColor('#8E8E93')
        .width(72)
      Text(value)
        .fontSize(14)
        .fontColor(color)
        .layoutWeight(1)
        .textAlign(TextAlign.End)
    }
    .width('100%')
    .padding({ top: 6, bottom: 6 })
    .border({
      width: { bottom: 0.5 },
      color: { bottom: '#F0F0F0' }
    })
  }

  @Builder
  buildReport(report: RegisterReport | null) {
    Column() {
      this.SectionTitle('病历信息', undefined, '📋')

      if (report) {
        Row() {
          Text('👨‍⚕️ ' + (report.doctorName || '未知医生'))
            .fontSize(13)
            .fontColor('#8E8E93')
          Blank()
          Text('就诊年龄：' + (report.visitAge || 0) + '岁')
            .fontSize(13)
            .fontColor('#8E8E93')
        }
        .width('100%')
        .margin({ bottom: 8 })

        Text('主诉')
          .fontSize(12)
          .fontColor('#8E8E93')
          .margin({ bottom: 4 })
          .alignSelf(ItemAlign.Start)

        Text(report.description || '无病历描述')
          .fontSize(14)
          .fontColor('#1A1A2E')
          .lineHeight(24)
          .padding(12)
          .backgroundColor('#F8F9FA')
          .borderRadius(8)
          .width('100%')
      } else {
        Text('暂无病历记录')
          .fontSize(14)
          .fontColor('#C7C7CC')
          .padding({ top: 16, bottom: 16 })
          .width('100%')
          .textAlign(TextAlign.Center)
      }
    }
    .width('100%')
    .padding(16)
    .backgroundColor('#FFFFFF')
    .borderRadius(14)
    .shadow({
      radius: 6,
      color: 'rgba(0,0,0,0.04)',
      offsetY: 2
    })
  }

  @Builder
  buildOrderList(orders: MedicalOrder[], orderItems: MedicalOrderItem[]) {
    Column() {
      this.SectionTitle('检查检验', orders?.length, '🔬')

      if (orders && orders.length > 0) {
        ForEach(orders, (order: MedicalOrder, orderIndex: number) => {
          Column() {
            Row() {
              Text('申请单 #' + (orderIndex + 1))
                .fontSize(14)
                .fontWeight(FontWeight.Medium)
                .fontColor('#1A1A2E')
              Blank()
              Text(this.getOrderStatusText(order.status))
                .fontSize(10)
                .fontColor('#FFFFFF')
                .backgroundColor(this.getOrderStatusColor(order.status))
                .padding({ left: 10, right: 10, top: 2, bottom: 2 })
                .borderRadius(10)
            }
            .width('100%')
            .margin({ bottom: 6 })

            if (order.clinicalSummary) {
              Text('📝 ' + order.clinicalSummary)
                .fontSize(13)
                .fontColor('#666666')
                .width('100%')
                .margin({ bottom: 4 })
            }

            if (order.urgencyLevel) {
              Row() {
                Text('紧急程度：')
                  .fontSize(12)
                  .fontColor('#8E8E93')
                Text(this.getUrgencyLabel(order.urgencyLevel))
                  .fontSize(12)
                  .fontColor(order.urgencyLevel === 'URGENT' ? '#FF6B6B' : '#666666')
                  .fontWeight(order.urgencyLevel === 'URGENT' ? FontWeight.Bold : FontWeight.Normal)
              }
              .width('100%')
              .margin({ bottom: 8 })
            }

            // 明细列表
            Column() {
              Text('检查项目明细')
                .fontSize(12)
                .fontColor('#8E8E93')
                .alignSelf(ItemAlign.Start)
                .margin({ bottom: 6 })

              if (orderItems && orderItems.length > 0) {
                ForEach(orderItems, (item: MedicalOrderItem) => {
                  if (item.orderId === order.orderId) {
                    Row() {
                      Text('•')
                        .fontSize(14)
                        .fontColor('#4A90D9')
                        .margin({ right: 8 })

                      Column() {
                        Text(item.itemName || '未知项目')
                          .fontSize(13)
                          .fontColor('#1A1A2E')
                        Text(item.itemCategory || '-')
                          .fontSize(11)
                          .fontColor('#8E8E93')
                      }
                      .alignItems(HorizontalAlign.Start)
                      .layoutWeight(1)

                      Text('¥' + (item.price || 0).toFixed(2))
                        .fontSize(13)
                        .fontColor('#4A90D9')
                        .fontWeight(FontWeight.Medium)
                    }
                    .width('100%')
                    .padding({ top: 5, bottom: 5 })
                    .border({
                      width: { bottom: 0.5 },
                      color: { bottom: '#F0F0F0' }
                    })
                  }
                }, (item: MedicalOrderItem) => item.orderItemId)
              } else {
                Text('暂无检查项目明细')
                  .fontSize(13)
                  .fontColor('#C7C7CC')
                  .padding(10)
                  .width('100%')
                  .textAlign(TextAlign.Center)
              }
            }
            .width('100%')
            .padding(12)
            .backgroundColor('#F8F9FA')
            .borderRadius(8)
          }
          .width('100%')
          .padding(14)
          .backgroundColor('#FFFFFF')
          .borderRadius(10)
          .margin({ bottom: 10 })
          .border({ width: 0.5, color: '#E8F0FE' })
        }, (order: MedicalOrder) => order.orderId)
      } else {
        Text('暂无检查检验项')
          .fontSize(14)
          .fontColor('#C7C7CC')
          .padding({ top: 16, bottom: 16 })
          .width('100%')
          .textAlign(TextAlign.Center)
      }
    }
    .width('100%')
    .padding(16)
    .backgroundColor('#FFFFFF')
    .borderRadius(14)
    .shadow({
      radius: 6,
      color: 'rgba(0,0,0,0.04)',
      offsetY: 2
    })
  }

  @Builder
  buildPrescriptionList(prescriptions: Prescription[]) {
    Column() {
      this.SectionTitle('处方项', prescriptions?.length, '💊')

      if (prescriptions && prescriptions.length > 0) {
        ForEach(prescriptions, (prescription: Prescription, index: number) => {
          Column() {
            Row() {
              Column() {
                Text('#' + (index + 1))
                  .fontSize(12)
                  .fontColor('#2ECC71')
                  .fontWeight(FontWeight.Bold)
                Text(this.getPayStatusText(prescription.payStatus))
                  .fontSize(10)
                  .fontColor('#FFFFFF')
                  .backgroundColor(this.getPayStatusColor(prescription.payStatus))
                  .padding({ left: 8, right: 8, top: 2, bottom: 2 })
                  .borderRadius(10)
                  .margin({ top: 2 })
              }
              .alignItems(HorizontalAlign.Center)
              .margin({ right: 12 })

              Column() {
                Text(prescription.medicineName || '未知药品')
                  .fontSize(14)
                  .fontWeight(FontWeight.Medium)
                  .fontColor('#1A1A2E')
                Text('规格：' + (prescription.spec || '无'))
                  .fontSize(12)
                  .fontColor('#8E8E93')
              }
              .alignItems(HorizontalAlign.Start)
              .layoutWeight(1)

              Column() {
                Text('数量')
                  .fontSize(11)
                  .fontColor('#8E8E93')
                Text(prescription.num + ' 单位')
                  .fontSize(13)
                  .fontColor('#1A1A2E')
              }
              .alignItems(HorizontalAlign.End)
              .margin({ left: 8 })
            }
            .width('100%')

            Divider()
              .color('#F0F0F0')
              .margin({ top: 6, bottom: 6 })

            Row() {
              Text('用法：' + (prescription.usage || '无'))
                .fontSize(12)
                .fontColor('#666666')
                .layoutWeight(1)
              Text('单价：¥' + (prescription.price || 0).toFixed(2))
                .fontSize(13)
                .fontColor('#4A90D9')
                .fontWeight(FontWeight.Medium)
            }
            .width('100%')

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
        }, (prescription: Prescription) => prescription.prescriptionId)
      } else {
        Text('暂无处方项')
          .fontSize(14)
          .fontColor('#C7C7CC')
          .padding({ top: 16, bottom: 16 })
          .width('100%')
          .textAlign(TextAlign.Center)
      }
    }
    .width('100%')
    .padding(16)
    .backgroundColor('#FFFFFF')
    .borderRadius(14)
    .shadow({
      radius: 6,
      color: 'rgba(0,0,0,0.04)',
      offsetY: 2
    })
  }

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
    .height(300)
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
    .height(300)
    .justifyContent(FlexAlign.Center)
    .alignItems(HorizontalAlign.Center)
  }

  build() {
    Column() {
      // 顶部导航栏 - 与其他页面统一
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
          Column({ space: 12 }) {
            // 挂号信息卡片
            Column() {
              Row() {
                Column() {
                  Text(this.detail.register?.department || '未知科室')
                    .fontSize(17)
                    .fontWeight(FontWeight.Bold)
                    .fontColor('#1A1A2E')
                  Text(this.detail.register?.consultRoom || '未知诊室')
                    .fontSize(13)
                    .fontColor('#8E8E93')
                    .margin({ top: 2 })
                }
                .alignItems(HorizontalAlign.Start)

                Blank()

                this.StatusTag(this.detail.register?.consultStatus || '')
              }
              .width('100%')

              Divider()
                .color('#F0F0F0')
                .margin({ top: 12, bottom: 12 })

              this.InfoRow('患者', this.detail.register?.name || '未知')
              this.InfoRow('性别', this.getGenderText(this.detail.register?.gender || 0))
              this.InfoRow('出生日期', this.detail.register?.birthday ? this.formatDate(this.detail.register.birthday) : '')
              this.InfoRow('就诊时间', (this.detail.register?.visitDate ? this.formatDate(this.detail.register.visitDate) : '') + ' ' + (this.detail.register?.consultTime || ''))

              if (this.detail.register?.chiefComplaint) {
                Row() {
                  Text('主诉')
                    .fontSize(13)
                    .fontColor('#8E8E93')
                    .width(72)
                  Text(this.detail.register.chiefComplaint)
                    .fontSize(14)
                    .fontColor('#1A1A2E')
                    .layoutWeight(1)
                    .textAlign(TextAlign.End)
                }
                .width('100%')
                .padding({ top: 6, bottom: 6 })
                .border({
                  width: { bottom: 0.5 },
                  color: { bottom: '#F0F0F0' }
                })
              }

              Row() {
                this.PayStatusTag(this.detail.register?.payStatus || '')
                Blank()
                Text('¥' + (this.detail.register?.price || 0).toFixed(2))
                  .fontSize(18)
                  .fontWeight(FontWeight.Bold)
                  .fontColor('#4A90D9')
              }
              .width('100%')
              .margin({ top: 4 })
            }
            .width('100%')
            .padding(16)
            .backgroundColor('#FFFFFF')
            .borderRadius(14)
            .shadow({
              radius: 6,
              color: 'rgba(0,0,0,0.04)',
              offsetY: 2
            })

            // 病历信息
            this.buildReport(this.detail.report)

            // 检查检验
            this.buildOrderList(this.detail.orders, this.detail.orderItems)

            // 处方项
            this.buildPrescriptionList(this.detail.prescriptions)
          }
          .width('100%')
          .padding(16)
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
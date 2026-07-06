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
  'WAITING': '#FAAD14',
  'PENDING': '#FAAD14',
  'CONFIRMED': '#1890FF',
  'COMPLETED': '#52C41A',
  'CANCELLED': '#FF4D4F'
};

const STATUS_BG_COLOR: Record<string, string> = {
  'WAITING': '#FFF7E6',
  'PENDING': '#FFF7E6',
  'CONFIRMED': '#E6F7FF',
  'COMPLETED': '#F6FFED',
  'CANCELLED': '#FFF1F0'
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

const ORDER_STATUS_COLOR: Record<string, string> = {
  'WAITING_ASSIGN': '#FAAD14',
  'ASSIGNED': '#1890FF',
  'IN_PROGRESS': '#722ED1',
  'COMPLETED': '#52C41A',
  'CANCELLED': '#FF4D4F'
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
    return STATUS_COLOR[status] || '#999999';
  }

  private getStatusBgColor(status: string): string {
    return STATUS_BG_COLOR[status] || '#F5F5F5';
  }

  private getStatusText(status: string): string {
    return STATUS_TEXT[status] || status || '未知';
  }

  private getPayStatusText(status: string): string {
    return PAY_STATUS_TEXT[status] || status || '未知';
  }

  private getOrderStatusColor(status: string): string {
    return ORDER_STATUS_COLOR[status] || '#999999';
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
  private PayStatusTag(payStatus: string) {
    Row() {
      Circle()
        .width(6)
        .height(6)
        .fill(payStatus === 'PAID' ? '#52C41A' : '#FAAD14')
        .margin({ right: 4 })
      Text(this.getPayStatusText(payStatus))
        .fontSize(12)
        .fontColor(payStatus === 'PAID' ? '#52C41A' : '#FAAD14')
        .fontWeight(FontWeight.Medium)
    }
    .padding({ left: 10, right: 10, top: 4, bottom: 4 })
    .backgroundColor(payStatus === 'PAID' ? '#F6FFED' : '#FFF7E6')
    .borderRadius(12)
  }

  @Builder
  buildReport(report: RegisterReport | null) {
    Column() {
      if (report) {
        Row() {
          Text('📋 病历信息')
            .fontSize(16)
            .fontWeight(FontWeight.Bold)
            .fontColor('#1A1A1A')
          Blank()
          Text('医生：' + (report.doctorName || '未知'))
            .fontSize(14)
            .fontColor('#666666')
        }
        .width('100%')
        .margin({ bottom: 10 })

        Text('主诉')
          .fontSize(13)
          .fontColor('#666666')
          .margin({ bottom: 4 })
          .alignSelf(ItemAlign.Start)

        Text(report.description || '无病历描述')
          .fontSize(14)
          .fontColor('#1A1A1A')
          .lineHeight(24)
          .padding(12)
          .backgroundColor('#F8FAFF')
          .borderRadius(8)
          .width('100%')
          .margin({ bottom: 8 })

        Row() {
          Text('就诊年龄：' + (report.visitAge || 0) + '岁')
            .fontSize(13)
            .fontColor('#999999')
          Blank()
          Text('就诊日期：' + this.formatDate(report.visitDate))
            .fontSize(13)
            .fontColor('#999999')
        }
        .width('100%')
        .margin({ top: 4 })
      } else {
        Text('📋 病历信息')
          .fontSize(16)
          .fontWeight(FontWeight.Bold)
          .fontColor('#1A1A1A')
          .margin({ bottom: 10 })
          .alignSelf(ItemAlign.Start)

        Text('暂无病历记录')
          .fontSize(14)
          .fontColor('#999999')
          .padding({ top: 20, bottom: 20 })
          .width('100%')
          .textAlign(TextAlign.Center)
      }
    }
    .width('100%')
    .padding(16)
    .backgroundColor('#FFFFFF')
    .borderRadius(12)
    .border({ width: 1, color: '#F0F0F0' })
  }

  @Builder
  buildOrderList(orders: MedicalOrder[], orderItems: MedicalOrderItem[]) {
    Column() {
      Row() {
        Text('🔬 检查检验 (' + (orders?.length || 0) + '项)')
          .fontSize(16)
          .fontWeight(FontWeight.Bold)
          .fontColor('#1A1A1A')
      }
      .width('100%')
      .margin({ bottom: 10 })

      if (orders && orders.length > 0) {
        ForEach(orders, (order: MedicalOrder, orderIndex: number) => {
          Column() {
            Row() {
              Text('申请单 #' + (orderIndex + 1))
                .fontSize(15)
                .fontWeight(FontWeight.Medium)
                .fontColor('#1A1A1A')
              Blank()
              Text(this.getOrderStatusText(order.status))
                .fontSize(12)
                .fontColor('#FFFFFF')
                .backgroundColor(this.getOrderStatusColor(order.status))
                .padding({ left: 10, right: 10, top: 4, bottom: 4 })
                .borderRadius(12)
            }
            .width('100%')
            .margin({ bottom: 6 })

            if (order.clinicalSummary) {
              Text('临床摘要：' + order.clinicalSummary)
                .fontSize(14)
                .fontColor('#666666')
                .width('100%')
                .margin({ bottom: 4 })
            }

            if (order.urgencyLevel) {
              Row() {
                Text('紧急程度：')
                  .fontSize(13)
                  .fontColor('#999999')
                Text(this.getUrgencyLabel(order.urgencyLevel))
                  .fontSize(13)
                  .fontColor(order.urgencyLevel === 'URGENT' ? '#EF4444' : '#666666')
                  .fontWeight(order.urgencyLevel === 'URGENT' ? FontWeight.Bold : FontWeight.Normal)
              }
              .width('100%')
              .margin({ bottom: 8 })
            }

            // 明细列表
            Column() {
              Text('检查项目明细：')
                .fontSize(13)
                .fontColor('#999999')
                .alignSelf(ItemAlign.Start)
                .margin({ bottom: 6 })

              if (orderItems && orderItems.length > 0) {
                ForEach(orderItems, (item: MedicalOrderItem) => {
                  if (item.orderId === order.orderId) {
                    Row() {
                      Column() {
                        Text('•')
                          .fontSize(16)
                          .fontColor('#2563EB')
                      }
                      .width(24)
                      .alignItems(HorizontalAlign.Center)

                      Column() {
                        Text(item.itemName || '未知项目')
                          .fontSize(14)
                          .fontColor('#1A1A1A')
                          .fontWeight(FontWeight.Medium)
                        Row() {
                          Text('编码：' + (item.itemCode || '-'))
                            .fontSize(12)
                            .fontColor('#999999')
                          Text('|')
                            .fontSize(12)
                            .fontColor('#E0E0E0')
                            .margin({ left: 6, right: 6 })
                          Text('分类：' + (item.itemCategory || '-'))
                            .fontSize(12)
                            .fontColor('#999999')
                        }
                      }
                      .alignItems(HorizontalAlign.Start)
                      .layoutWeight(1)
                      .margin({ left: 4 })

                      Blank()

                      Text('¥' + (item.price || 0).toFixed(2))
                        .fontSize(14)
                        .fontColor('#2563EB')
                        .fontWeight(FontWeight.Medium)
                    }
                    .width('100%')
                    .padding({ top: 6, bottom: 6 })
                    .border({
                      width: { bottom: 1 },
                      color: { bottom: '#F0F0F0' }
                    })
                  }
                }, (item: MedicalOrderItem) => item.orderItemId)
              } else {
                Text('暂无检查项目明细')
                  .fontSize(13)
                  .fontColor('#999999')
                  .padding(10)
                  .width('100%')
                  .textAlign(TextAlign.Center)
              }
            }
            .width('100%')
            .padding(12)
            .backgroundColor('#F8FAFF')
            .borderRadius(8)
          }
          .width('100%')
          .padding(14)
          .backgroundColor('#FFFFFF')
          .borderRadius(8)
          .margin({ bottom: 10 })
          .border({ width: 1, color: '#E8F0FE' })
        }, (order: MedicalOrder) => order.orderId)
      } else {
        Text('暂无检查检验项')
          .fontSize(14)
          .fontColor('#999999')
          .padding({ top: 16, bottom: 16 })
          .width('100%')
          .textAlign(TextAlign.Center)
      }
    }
    .width('100%')
    .padding(16)
    .backgroundColor('#FFFFFF')
    .borderRadius(12)
    .border({ width: 1, color: '#F0F0F0' })
  }

  @Builder
  buildPrescriptionList(prescriptions: Prescription[]) {
    Column() {
      Row() {
        Text('💊 处方项 (' + (prescriptions?.length || 0) + ')')
          .fontSize(16)
          .fontWeight(FontWeight.Bold)
          .fontColor('#1A1A1A')
      }
      .width('100%')
      .margin({ bottom: 10 })

      if (prescriptions && prescriptions.length > 0) {
        ForEach(prescriptions, (prescription: Prescription, index: number) => {
          Column() {
            Row() {
              Column() {
                Text('#' + (index + 1))
                  .fontSize(13)
                  .fontColor('#10B981')
                  .fontWeight(FontWeight.Bold)
                Text(this.getPayStatusText(prescription.payStatus))
                  .fontSize(11)
                  .fontColor('#FFFFFF')
                  .backgroundColor(prescription.payStatus === 'PAID' ? '#10B981' : '#FAAD14')
                  .padding({ left: 8, right: 8, top: 2, bottom: 2 })
                  .borderRadius(10)
                  .margin({ top: 4 })
              }
              .alignItems(HorizontalAlign.Center)
              .margin({ right: 12 })

              Column() {
                Text('药品名称')
                  .fontSize(12)
                  .fontColor('#999999')
                Text(prescription.medicineName || '未知药品')
                  .fontSize(15)
                  .fontColor('#1A1A1A')
                  .fontWeight(FontWeight.Medium)
              }
              .alignItems(HorizontalAlign.Start)
              .layoutWeight(1)

              Blank()

              Column() {
                Text('数量')
                  .fontSize(12)
                  .fontColor('#999999')
                Text(prescription.num + ' 单位')
                  .fontSize(14)
                  .fontColor('#1A1A1A')
              }
              .alignItems(HorizontalAlign.End)
            }
            .width('100%')

            Row() {
              Text('规格：' + (prescription.spec || '无'))
                .fontSize(13)
                .fontColor('#666666')
              Blank()
              Text('单价：¥' + (prescription.price || 0).toFixed(2))
                .fontSize(13)
                .fontColor('#2563EB')
            }
            .width('100%')
            .margin({ top: 6 })

            Text('用法：' + (prescription.usage || '无'))
              .fontSize(13)
              .fontColor('#666666')
              .width('100%')
              .margin({ top: 4 })

            Row() {
              Text('开方医生：' + (prescription.doctorName || '未知'))
                .fontSize(12)
                .fontColor('#999999')
              Blank()
              Text('开方日期：' + this.formatDate(prescription.createDate))
                .fontSize(12)
                .fontColor('#999999')
            }
            .width('100%')
            .margin({ top: 6 })
          }
          .width('100%')
          .padding(14)
          .backgroundColor('#F0FDF4')
          .borderRadius(8)
          .margin({ bottom: 10 })
          .border({ width: 1, color: '#DCFCE7' })
        }, (prescription: Prescription) => prescription.prescriptionId)
      } else {
        Text('暂无处方项')
          .fontSize(14)
          .fontColor('#999999')
          .padding({ top: 16, bottom: 16 })
          .width('100%')
          .textAlign(TextAlign.Center)
      }
    }
    .width('100%')
    .padding(16)
    .backgroundColor('#FFFFFF')
    .borderRadius(12)
    .border({ width: 1, color: '#F0F0F0' })
  }

  @Builder
  private LoadingState() {
    Column() {
      LoadingProgress()
        .width(48)
        .height(48)
        .color('#2563EB')
      Text('加载就诊详情...')
        .fontSize(14)
        .fontColor('#999999')
        .margin({ top: 16 })
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
        .fontColor('#8C8C8C')
        .textAlign(TextAlign.Center)
        .margin({ bottom: 20 })
      Button('重新加载')
        .fontSize(14)
        .fontColor('#FFFFFF')
        .backgroundColor('#2563EB')
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
      // 顶部导航栏
      Row() {
        Text('‹')
          .fontSize(28)
          .fontColor('#333333')
          .onClick(() => {
            this.goBack();
          })

        Text('就诊详情')
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
      } else if (this.detail) {
        Scroll() {
          Column() {
            // 挂号信息卡片
            Column() {
              Row() {
                Column() {
                  Text(this.detail.register?.department || '未知科室')
                    .fontSize(18)
                    .fontWeight(FontWeight.Bold)
                    .fontColor('#1A1A1A')
                  Text(this.detail.register?.consultRoom || '未知诊室')
                    .fontSize(14)
                    .fontColor('#666666')
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

              Row() {
                Text('患者：' + (this.detail.register?.name || '未知'))
                  .fontSize(15)
                  .fontColor('#333333')
                Text('|')
                  .fontSize(13)
                  .fontColor('#E0E0E0')
                  .margin({ left: 8, right: 8 })
                Text(this.getGenderText(this.detail.register?.gender || 0))
                  .fontSize(15)
                  .fontColor('#333333')
                Text('|')
                  .fontSize(13)
                  .fontColor('#E0E0E0')
                  .margin({ left: 8, right: 8 })
                Text(this.detail.register?.birthday ? this.formatDate(this.detail.register.birthday) : '')
                  .fontSize(14)
                  .fontColor('#666666')
              }
              .width('100%')

              Row() {
                Text('就诊时间：' + (this.detail.register?.visitDate ? this.formatDate(this.detail.register.visitDate) : '') + ' ' + (this.detail.register?.consultTime || ''))
                  .fontSize(14)
                  .fontColor('#666666')
              }
              .width('100%')
              .margin({ top: 4 })

              if (this.detail.register?.chiefComplaint) {
                Row() {
                  Text('主诉：' + this.detail.register.chiefComplaint)
                    .fontSize(14)
                    .fontColor('#666666')
                    .maxLines(2)
                    .textOverflow({ overflow: TextOverflow.Ellipsis })
                }
                .width('100%')
                .margin({ top: 4 })
              }

              Row() {
                this.PayStatusTag(this.detail.register?.payStatus || '')
                Blank()
                Text('¥' + (this.detail.register?.price || 0).toFixed(2))
                  .fontSize(20)
                  .fontColor('#EF4444')
                  .fontWeight(FontWeight.Bold)
              }
              .width('100%')
              .margin({ top: 8 })
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
            .margin({ bottom: 12 })

            // 病历信息 - 用 Column 包裹后设置 margin
            Column() {
              this.buildReport(this.detail.report)
            }
            .width('100%')
            .margin({ bottom: 12 })

            // 检查检验 - 用 Column 包裹后设置 margin
            Column() {
              this.buildOrderList(this.detail.orders, this.detail.orderItems)
            }
            .width('100%')
            .margin({ bottom: 12 })

            // 处方项 - 用 Column 包裹后设置 margin
            Column() {
              this.buildPrescriptionList(this.detail.prescriptions)
            }
            .width('100%')
            .margin({ bottom: 12 })
          }
          .width('100%')
          .padding(16)
        }
        .width('100%')
        .layoutWeight(1)
        .backgroundColor('#F5F5F5')
        .scrollBar(BarState.Auto)
      }
    }
    .width('100%')
    .height('100%')
    .backgroundColor('#F5F5F5')
  }
}
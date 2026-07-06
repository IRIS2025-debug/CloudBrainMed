// pages/history/RegisterHistory.ets
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

// Tab类型
type TabType = 'all' | 'pending_pay' | 'pending_visit' | 'expired';

// Tab配置
interface TabItem {
  key: TabType;
  title: string;
}

// ===== 页面组件 =====

@Entry
@Component
export struct RegisterHistory {
  @Prop patientId: string = '';
  @Prop userName: string = '';
  @Prop userPhone: string = '';
  @Prop userGender: number = 0;
  @Prop userGenderText: string = '';
  @Prop userAge: number = 0;

  @State registerList: RegisterRecord[] = [];
  @State filteredList: RegisterRecord[] = [];
  @State isLoading: boolean = true;
  // @State expandedRegisterId: string = '';
  // @State visitDetails: Record<string, VisitDetail> = {};
  // @State loadingDetailId: string = '';
  @State errorMessage: string = '';
  @State currentTab: TabType = 'all';

  private baseUrl: string = APIConfig.PATIENT_SERVICE_URL;

  private tabs: TabItem[] = [
    { key: 'all', title: '全部' },
    { key: 'pending_pay', title: '待支付' },
    { key: 'pending_visit', title: '待接诊' },
    { key: 'expired', title: '已过期' }
  ];

  aboutToAppear(): void {
    const params = router.getParams() as Record<string, Object>;
    if (params) {
      this.patientId = (params['patientId'] as string) || '';
      this.userName = (params['userName'] as string) || (params['name'] as string) || '';
      this.userPhone = (params['userPhone'] as string) || '';
      this.userGender = (params['userGender'] as number) || 0;
      this.userGenderText = (params['userGenderText'] as string) || '未知';
      this.userAge = (params['userAge'] as number) || 0;
    }
    console.info('RegisterHistory 接收参数: patientId=' + this.patientId + ', userName=' + this.userName);
    this.loadRegisterList();
  }

  /**
   * 加载挂号记录列表
   */
  async loadRegisterList(): Promise<void> {
    if (!this.patientId) {
      this.isLoading = false;
      this.errorMessage = '未获取到患者信息，请重新登录';
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';

    try {
      const context = getContext(this);
      const url = `${this.baseUrl}/register/history/${this.patientId}`;
      const result = await HttpUtil.get<ApiResponse<RegisterRecord[]>>(url, context);

      if (result.code === 200 && result.data) {
        this.registerList = result.data;
        console.info('加载挂号记录成功，共 ' + this.registerList.length + ' 条');
        this.applyFilter();
      } else {
        this.errorMessage = result.msg || '加载失败';
        this.registerList = [];
        this.filteredList = [];
      }
    } catch (error) {
      console.error('加载挂号记录异常:', JSON.stringify(error));
      this.errorMessage = '网络请求失败，请检查网络连接';
      this.registerList = [];
      this.filteredList = [];
    } finally {
      this.isLoading = false;
    }
  }

  /**
   * 切换Tab并过滤数据
   */
  private switchTab(tab: TabType): void {
    this.currentTab = tab;
    this.applyFilter();
  }

  /**
   * 应用过滤
   */
  private applyFilter(): void {
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    if (this.currentTab === 'all') {
      this.filteredList = [...this.registerList];
      return;
    }

    this.filteredList = this.registerList.filter((item) => {
      if (!item.visitDate) return false;
      const visitDate = new Date(item.visitDate);
      visitDate.setHours(0, 0, 0, 0);

      switch (this.currentTab) {
        case 'pending_pay':
          // 待支付：就诊日期 >= 今天 且 支付状态为 WAITING
          return visitDate >= today && item.payStatus === 'WAITING';
        case 'pending_visit':
          // 待接诊：就诊日期 >= 今天 且 支付状态为 PAID
          return visitDate >= today && item.payStatus === 'PAID';
        case 'expired':
          // 已过期：就诊日期 < 今天
          return visitDate < today;
        default:
          return true;
      }
    });
  }

  /**
   * 获取各Tab的数量
   */
  private getTabCount(tab: TabType): number {
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    if (tab === 'all') {
      return this.registerList.length;
    }

    return this.registerList.filter((item) => {
      if (!item.visitDate) return false;
      const visitDate = new Date(item.visitDate);
      visitDate.setHours(0, 0, 0, 0);

      switch (tab) {
        case 'pending_pay':
          return visitDate >= today && item.payStatus === 'WAITING';
        case 'pending_visit':
          return visitDate >= today && item.payStatus === 'PAID';
        case 'expired':
          return visitDate < today;
        default:
          return false;
      }
    }).length;
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
  buildRegisterCard(register: RegisterRecord) {
    Column() {
      Column() {
        Row() {
          Column() {
            Text(register.department || '未知科室')
              .fontSize(16)
              .fontWeight(FontWeight.Bold)
              .fontColor('#1A1A1A')
            Text(register.consultRoom || '未知诊室')
              .fontSize(13)
              .fontColor('#666666')
          }
          .alignItems(HorizontalAlign.Start)

          Blank()

          this.StatusTag(register.consultStatus)
        }
        .width('100%')

        Row() {
          Text('患者：' + (register.name || '未知'))
            .fontSize(14)
            .fontColor('#333333')
          Text('|')
            .fontSize(13)
            .fontColor('#E0E0E0')
            .margin({ left: 6, right: 6 })
          Text(this.getGenderText(register.gender))
            .fontSize(14)
            .fontColor('#333333')
          Text('|')
            .fontSize(13)
            .fontColor('#E0E0E0')
            .margin({ left: 6, right: 6 })
          Text(register.birthday ? this.formatDate(register.birthday) : '')
            .fontSize(13)
            .fontColor('#666666')
        }
        .width('100%')
        .margin({ top: 4 })

        Row() {
          Text('就诊时间：' + (register.visitDate ? this.formatDate(register.visitDate) : '') + ' ' + (register.consultTime || ''))
            .fontSize(13)
            .fontColor('#666666')
        }
        .width('100%')
        .margin({ top: 2 })

        if (register.chiefComplaint) {
          Row() {
            Text('主诉：' + register.chiefComplaint)
              .fontSize(13)
              .fontColor('#666666')
              .maxLines(1)
              .textOverflow({ overflow: TextOverflow.Ellipsis })
          }
          .width('100%')
          .margin({ top: 2 })
        }

        Row() {
          this.PayStatusTag(register.payStatus)
          Blank()
          Text(register.createTime ? '挂号时间：' + this.formatDate(register.createTime) : '')
            .fontSize(11)
            .fontColor('#999999')
        }
        .width('100%')
        .margin({ top: 4 })

        Divider()
          .color('#F0F0F0')
          .margin({ top: 10, bottom: 10 })

        Row() {
          Text('¥' + (register.price || 0).toFixed(2))
            .fontSize(18)
            .fontColor('#EF4444')
            .fontWeight(FontWeight.Bold)

          Blank()

          Text('查看详情 ›')
            .fontSize(14)
            .fontColor('#2563EB')
            .fontWeight(FontWeight.Medium)
        }
        .width('100%')
        .onClick(() => {
          // 跳转到详情页面
          router.pushUrl({
            url: 'pages/history/RegisterDetail',
            params: {
              registerId: register.registerId,
              patientName: register.name
            }
          }).catch(() => {
            promptAction.showToast({
              message: '跳转失败，请重试',
              duration: 1500
            });
          });
        })
      }
      .width('100%')
      .padding(16)
      .backgroundColor('#FFFFFF')
      .borderRadius(12)
    }
    .width('100%')
    .backgroundColor('#FFFFFF')
    .borderRadius(12)
    .shadow({
      radius: 8,
      color: 'rgba(0, 0, 0, 0.06)',
      offsetX: 0,
      offsetY: 2
    })
    .margin({ bottom: 12 })
  }

  @Builder
  private LoadingState() {
    Column() {
      LoadingProgress()
        .width(40)
        .height(40)
        .color('#2563EB')
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
        .backgroundColor('#2563EB')
        .borderRadius(20)
        .height(36)
        .padding({ left: 24, right: 24 })
        .onClick(() => {
          this.loadRegisterList();
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
      Text('暂无就诊记录')
        .fontSize(18)
        .fontColor('#1A1A1A')
        .fontWeight(FontWeight.Medium)
      Text('您还没有挂号记录，请先预约挂号')
        .fontSize(14)
        .fontColor('#8C8C8C')
        .margin({ top: 4 })
      Button('去挂号')
        .width(160)
        .height(44)
        .fontSize(15)
        .backgroundColor('#2563EB')
        .borderRadius(22)
        .margin({ top: 24 })
        .onClick(() => {
          router.pushUrl({ url: 'pages/register/DeptListPage' })
            .catch(() => {
              promptAction.showToast({ message: '跳转失败', duration: 1500 });
            });
        })
    }
    .width('100%')
    .padding(60)
    .justifyContent(FlexAlign.Center)
    .alignItems(HorizontalAlign.Center)
  }

  @Builder
  private TabBar() {
    Row() {
      ForEach(this.tabs, (tab: TabItem) => {
        Column() {
          Row() {
            Text(tab.title)
              .fontSize(14)
              .fontColor(this.currentTab === tab.key ? '#2563EB' : '#666666')
              .fontWeight(this.currentTab === tab.key ? FontWeight.Medium : FontWeight.Normal)
            if (this.getTabCount(tab.key) > 0) {
              Text(' (' + this.getTabCount(tab.key) + ')')
                .fontSize(12)
                .fontColor(this.currentTab === tab.key ? '#2563EB' : '#999999')
            }
          }
          .padding({ left: 12, right: 12, top: 8, bottom: 8 })

          Divider()
            .width(this.currentTab === tab.key ? 24 : 0)
            .height(3)
            .color('#2563EB')
            .borderRadius(2)
            .margin({ top: 2 })
        }
        .width(`${100 / this.tabs.length}%`)
        .alignItems(HorizontalAlign.Center)
        .onClick(() => {
          this.switchTab(tab.key);
        })
      })
    }
    .width('100%')
    .height(48)
    .backgroundColor('#FFFFFF')
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

        Text('就诊记录')
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

      // 患者信息
      if (this.userName) {
        Row() {
          Text('👤 ' + this.userName)
            .fontSize(14)
            .fontColor('#666666')
          Text('|')
            .fontSize(13)
            .fontColor('#E0E0E0')
            .margin({ left: 10, right: 10 })
          Text('门诊号：' + this.patientId)
            .fontSize(13)
            .fontColor('#666666')
        }
        .width('100%')
        .padding({ left: 16, right: 16, top: 10, bottom: 10 })
        .backgroundColor('#F8FAFF')
      }

      // Tab栏
      this.TabBar()

      // 内容区域
      if (this.isLoading) {
        this.LoadingState()
      } else if (this.errorMessage) {
        this.ErrorState()
      } else if (this.filteredList.length === 0) {
        this.EmptyState()
      } else {
        List() {
          ListItem() {
            Row() {
              Text(`共 ${this.filteredList.length} 条就诊记录`)
                .fontSize(13)
                .fontColor('#999999')
            }
            .width('100%')
            .padding({ left: 4, top: 4, bottom: 8 })
          }

          ForEach(this.filteredList, (register: RegisterRecord) => {
            ListItem() {
              this.buildRegisterCard(register)
            }
          }, (register: RegisterRecord) => register.registerId)
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

  private getEmptyText(): string {
    switch (this.currentTab) {
      case 'pending_pay':
        return '暂无待支付记录';
      case 'pending_visit':
        return '暂无待接诊记录';
      case 'expired':
        return '暂无已过期记录';
      default:
        return '暂无就诊记录';
    }
  }

  private getEmptySubText(): string {
    switch (this.currentTab) {
      case 'pending_pay':
        return '您当前没有需要支付的挂号';
      case 'pending_visit':
        return '您当前没有待接诊的挂号';
      case 'expired':
        return '您没有已过期的挂号记录';
      default:
        return '您还没有挂号记录，请先预约挂号';
    }
  }
}
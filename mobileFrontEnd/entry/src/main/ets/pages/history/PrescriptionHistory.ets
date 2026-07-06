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

// ==================== 常量定义 ====================

const PayStatusText: Record<string, string> = {
  'WAITING': '待支付',
  'PAID': '已支付',
  'CANCELLED': '已取消',
  'REFUNDED': '已退款'
};

const PayStatusColor: Record<string, string> = {
  'WAITING': '#FAAD14',
  'PAID': '#52C41A',
  'CANCELLED': '#FF4D4F',
  'REFUNDED': '#999999'
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
    console.info('PrescriptionHistory 接收参数:', JSON.stringify(params));

    if (params) {
      this.patientId = (params['patientId'] as string) || '';
      this.userName = (params['userName'] as string) || (params['name'] as string) || '';
    }

    this.loadPrescriptionHistory();
  }

  /**
   * 加载处方历史
   */
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
      const url: string = `${this.baseUrl}/prescription/grouped/${this.patientId}`;
      console.info('请求处方历史:', url);

      const result = await HttpUtil.get<ApiResponse<RegisterPrescriptionGroup[]>>(url, context);
      console.info('处方历史响应:', JSON.stringify(result));

      if (result.code === 200 && result.data) {
        this.groups = result.data;
        console.info(`加载成功，共 ${this.groups.length} 张处方`);
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

  /**
   * 返回上一页
   */
  goBack(): void {
    router.back();
  }

  /**
   * 查看处方详情
   */
  goToDetail(group: RegisterPrescriptionGroup): void {
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
    }).catch((err: Error) => {
      console.error('跳转处方详情失败:', JSON.stringify(err));
      promptAction.showToast({
        message: '页面跳转失败',
        duration: 1500
      });
    });
  }

  /**
   * 获取状态颜色
   */
  private getStatusColor(status: string): string {
    return PayStatusColor[status] || '#999999';
  }

  /**
   * 获取状态文本
   */
  private getStatusText(status: string): string {
    return PayStatusText[status] || status;
  }

  /**
   * 格式化金额
   */
  private formatAmount(amount: number): string {
    return (amount || 0).toFixed(2);
  }

  /**
   * 获取药品名称摘要
   */
  private getMedicineNames(prescriptions: PrescriptionItem[]): string {
    if (!prescriptions || prescriptions.length === 0) {
      return '暂无药品';
    }
    return prescriptions.map(p => p.medicineName).join('、');
  }

  // ==================== UI构建 ====================

  @Builder
  StatusTag(status: string) {
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
    .backgroundColor(`${this.getStatusColor(status)}15`)
    .borderRadius(12)
  }

  @Builder
  PrescriptionCard(group: RegisterPrescriptionGroup) {
    Column() {
      // ===== 顶部：日期、医生、状态 =====
      Row() {
        Column() {
          Text(group.prescriptionDate || '未知日期')
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

      // ===== 药品摘要 =====
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

      // ===== 底部：数量、总金额 =====
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
  EmptyState() {
    Column() {
      Text('💊')
        .fontSize(56)
        .margin({ bottom: 16 })
      Text('暂无处方记录')
        .fontSize(18)
        .fontColor('#1A1A1A')
        .fontWeight(FontWeight.Medium)
      Text('您当前没有中医处方历史记录')
        .fontSize(14)
        .fontColor('#8C8C8C')
        .margin({ top: 4 })
    }
    .width('100%')
    .padding(60)
    .justifyContent(FlexAlign.Center)
    .alignItems(HorizontalAlign.Center)
  }

  @Builder
  LoadingState() {
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
    .backgroundColor('#F5F5F5')
  }

  @Builder
  ErrorState() {
    Column() {
      Text('📋')
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

  build() {
    Column() {
      // ===== 顶部导航栏 =====
      Row() {
        Text('‹')
          .fontSize(28)
          .fontColor('#333333')
          .onClick(() => {
            this.goBack();
          })

        Text('中医处方')
          .fontSize(20)
          .fontWeight(FontWeight.Bold)
          .fontColor('#333333')
          .layoutWeight(1)
          .textAlign(TextAlign.Center)

        // 右侧占位保持对称
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

      // ===== 内容区域 =====
      if (this.isLoading) {
        this.LoadingState()
      } else if (this.errorMessage) {
        this.ErrorState()
      } else if (this.groups.length === 0) {
        this.EmptyState()
      } else {
        List() {
          // 统计信息
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
    .backgroundColor('#F5F5F5')
  }
}
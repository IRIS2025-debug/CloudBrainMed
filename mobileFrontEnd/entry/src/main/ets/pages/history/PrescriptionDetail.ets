// pages/history/PrescriptionDetail.ets
import router from '@ohos.router';
import promptAction from '@ohos.promptAction';

// ==================== 类型定义 ====================

interface PrescriptionItem {
  prescriptionId: string;
  medicineName: string;
  spec: string;
  usage_: string;
  num: number;
  price: number;
}

interface DetailParams {
  registerId?: string;
  patientName?: string;
  doctorName?: string;
  prescriptionDate?: string;
  totalAmount?: number;
  payStatus?: string;
  prescriptions?: PrescriptionItem[];
}

// ==================== 常量定义 ====================

const PayStatusText: Record<string, string> = {
  'WAITING': '待支付',
  'PAID': '已支付 ✓',
  'CANCELLED': '已取消 ✗',
  'REFUNDED': '已退款 ↩'
};

const PayStatusColor: Record<string, string> = {
  'WAITING': '#4A90D9',
  'PAID': '#2ECC71',
  'CANCELLED': '#95A5A6',
  'REFUNDED': '#95A5A6'
};

const PayStatusBgColor: Record<string, string> = {
  'WAITING': '#E8F0FE',
  'PAID': '#E8F8F0',
  'CANCELLED': '#F0F0F0',
  'REFUNDED': '#F0F0F0'
};

// ==================== 页面组件 ====================

@Entry
@Component
export struct PrescriptionDetail {
  @State registerId: string = '';
  @State patientName: string = '';
  @State doctorName: string = '';
  @State prescriptionDate: string = '';
  @State totalAmount: number = 0;
  @State payStatus: string = '';
  @State prescriptions: PrescriptionItem[] = [];

  aboutToAppear(): void {
    const params = router.getParams() as DetailParams;
    console.info('PrescriptionDetail 接收参数:', JSON.stringify(params));

    if (params) {
      this.registerId = params.registerId || '';
      this.patientName = params.patientName || '';
      this.doctorName = params.doctorName || '';
      this.prescriptionDate = params.prescriptionDate || '';
      this.totalAmount = params.totalAmount || 0;
      this.payStatus = params.payStatus || '';
      this.prescriptions = params.prescriptions || [];
    }
  }

  private goBack(): void {
    router.back();
  }

  private getStatusColor(): string {
    return PayStatusColor[this.payStatus] || '#95A5A6';
  }

  private getStatusBgColor(): string {
    return PayStatusBgColor[this.payStatus] || '#F0F0F0';
  }

  private getStatusText(): string {
    return PayStatusText[this.payStatus] || this.payStatus || '未知';
  }

  private formatAmount(amount: number): string {
    return (amount || 0).toFixed(2);
  }

  private formatDate(dateStr: string): string {
    if (!dateStr) return '';
    const parts = dateStr.split('T');
    return parts[0] || dateStr;
  }

  private goToPay(): void {
    router.pushUrl({
      url: 'pages/pay/PayPage',
      params: {
        businessId: this.registerId,
        orderType: 'PRESCRIPTION',
        amount: this.totalAmount,
        description: '处方费',
        patientName: this.patientName,
        returnUrl: 'pages/history/PrescriptionDetail'
      }
    }).catch((err: Error) => {
      console.error('跳转支付失败:', JSON.stringify(err));
      promptAction.showToast({
        message: '跳转支付失败',
        duration: 1500
      });
    });
  }

  // ==================== UI构建 ====================

  @Builder
  private StatusTag() {
    Row() {
      Circle()
        .width(5)
        .height(5)
        .fill(this.getStatusColor())
        .margin({ right: 4 })
      Text(this.getStatusText())
        .fontSize(11)
        .fontColor(this.getStatusColor())
        .fontWeight(FontWeight.Medium)
    }
    .padding({ left: 10, right: 10, top: 3, bottom: 3 })
    .backgroundColor(this.getStatusBgColor())
    .borderRadius(12)
  }

  @Builder
  private InfoRow(label: string, value: string) {
    Row() {
      Text(label)
        .fontSize(13)
        .fontColor('#8E8E93')
        .width(72)
      Text(value || '未知')
        .fontSize(14)
        .fontColor('#1A1A2E')
        .layoutWeight(1)
        .textAlign(TextAlign.End)
    }
    .width('100%')
    .padding({ top: 8, bottom: 8 })
    .border({ width: { bottom: 0.5 }, color: '#F0F0F0' })
  }

  @Builder
  private MedicineItem(item: PrescriptionItem, index: number) {
    Column() {
      Row() {
        Column() {
          Text(`${index + 1}`)
            .fontSize(11)
            .fontColor('#FFFFFF')
            .width(22)
            .height(22)
            .borderRadius(11)
            .backgroundColor('#4A90D9')
            .textAlign(TextAlign.Center)
            .lineHeight(22)
        }
        .width(22)
        .height(22)

        Column() {
          Text(item.medicineName || '未知药品')
            .fontSize(15)
            .fontWeight(FontWeight.Medium)
            .fontColor('#1A1A2E')
          Row() {
            if (item.spec) {
              Text('规格: ' + item.spec)
                .fontSize(11)
                .fontColor('#8E8E93')
            }
            if (item.spec && item.usage_) {
              Text('·')
                .fontSize(11)
                .fontColor('#C7C7CC')
                .margin({ left: 4, right: 4 })
            }
            if (item.usage_) {
              Text('用法: ' + item.usage_)
                .fontSize(11)
                .fontColor('#8E8E93')
            }
            if (item.num > 0) {
              Text('· 数量: ' + item.num)
                .fontSize(11)
                .fontColor('#8E8E93')
                .margin({ left: 4 })
            }
          }
          .margin({ top: 2 })
        }
        .alignItems(HorizontalAlign.Start)
        .layoutWeight(1)
        .margin({ left: 10 })

        Text(`¥${this.formatAmount(item.price)}`)
          .fontSize(15)
          .fontColor('#4A90D9')
          .fontWeight(FontWeight.Bold)
      }
      .width('100%')
    }
    .width('100%')
    .padding(14)
    .backgroundColor('#F8F9FA')
    .borderRadius(10)
    .margin({ bottom: 8 })
  }

  build() {
    Column() {
      // 顶部导航栏 - 与之前界面统一
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

        Text('处方详情')
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
      List({ space: 12 }) {
        // 基本信息卡片
        ListItem() {
          Column() {
            Row() {
              Text('📋 处方信息')
                .fontSize(15)
                .fontWeight(FontWeight.Medium)
                .fontColor('#1A1A2E')

              Blank()

              this.StatusTag()
            }
            .width('100%')
            .margin({ bottom: 12 })

            this.InfoRow('处方号', this.registerId)
            this.InfoRow('患者姓名', this.patientName)
            this.InfoRow('开方医生', this.doctorName)
            this.InfoRow('开方日期', this.formatDate(this.prescriptionDate))
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

        // 药品列表卡片
        ListItem() {
          Column() {
            Row() {
              Text('💊 药品明细')
                .fontSize(15)
                .fontWeight(FontWeight.Medium)
                .fontColor('#1A1A2E')

              Blank()

              Text(`共 ${this.prescriptions.length} 种`)
                .fontSize(12)
                .fontColor('#8E8E93')
            }
            .width('100%')
            .margin({ bottom: 12 })

            if (this.prescriptions.length > 0) {
              ForEach(this.prescriptions, (item: PrescriptionItem, index: number) => {
                this.MedicineItem(item, index)
              }, (item: PrescriptionItem, index: number) => item.prescriptionId || `medicine_${index}`)
            } else {
              Text('暂无药品信息')
                .fontSize(14)
                .fontColor('#C7C7CC')
                .padding(30)
                .width('100%')
                .textAlign(TextAlign.Center)
                .backgroundColor('#F8F9FA')
                .borderRadius(10)
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

        // 合计金额卡片
        ListItem() {
          Column() {
            Row() {
              Text('💰 合计金额')
                .fontSize(15)
                .fontColor('#1A1A2E')
                .fontWeight(FontWeight.Medium)

              Blank()

              Text(`¥${this.formatAmount(this.totalAmount)}`)
                .fontSize(22)
                .fontColor('#4A90D9')
                .fontWeight(FontWeight.Bold)
            }
            .width('100%')
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

        // 去支付按钮
        if (this.payStatus === 'WAITING') {
          ListItem() {
            Button('去支付 ¥' + this.formatAmount(this.totalAmount))
              .width('100%')
              .height(48)
              .fontSize(16)
              .fontWeight(FontWeight.Medium)
              .fontColor('#FFFFFF')
              .backgroundColor('#4A90D9')
              .borderRadius(24)
              .onClick(() => {
                this.goToPay();
              })
          }
          .margin({ bottom: 4 })
        }
      }
      .width('100%')
      .padding({ left: 16, right: 16, top: 16 })
      .backgroundColor('#F8F9FA')
      .layoutWeight(1)
      .scrollBar(BarState.Off)
    }
    .width('100%')
    .height('100%')
    .backgroundColor('#FFFFFF')
  }
}
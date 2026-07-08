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

  /**
   * 返回上一页
   */
  goBack(): void {
    router.back();
  }

  /**
   * 获取状态颜色
   */
  private getStatusColor(): string {
    return PayStatusColor[this.payStatus] || '#999999';
  }

  /**
   * 获取状态文本
   */
  private getStatusText(): string {
    return PayStatusText[this.payStatus] || this.payStatus || '未知';
  }

  /**
   * 格式化金额
   */
  private formatAmount(amount: number): string {
    return (amount || 0).toFixed(2);
  }

  /**
   * 去支付
   */
  private goToPay(): void {
    router.pushUrl({
      url: 'pages/pay/PayPage',
      params: {
        businessId: this.registerId,
        orderType: 'PRESCRIPTION',
        amount: this.totalAmount,
        description: `处方 ${this.registerId}`,
        patientName: this.patientName
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
  StatusTag() {
    Row() {
      Circle()
        .width(6)
        .height(6)
        .fill(this.getStatusColor())
        .margin({ right: 4 })
      Text(this.getStatusText())
        .fontSize(13)
        .fontColor(this.getStatusColor())
        .fontWeight(FontWeight.Medium)
    }
    .padding({ left: 12, right: 12, top: 4, bottom: 4 })
    .backgroundColor(`${this.getStatusColor()}15`)
    .borderRadius(12)
  }

  @Builder
  InfoRow(label: string, value: string) {
    Row() {
      Text(label)
        .fontSize(14)
        .fontColor('#999999')
        .width(80)
      Text(value || '未知')
        .fontSize(14)
        .fontColor('#333333')
        .layoutWeight(1)
    }
    .width('100%')
    .padding({ top: 4, bottom: 4 })
  }

  @Builder
  MedicineItem(item: PrescriptionItem, index: number) {
    Column() {
      Row() {
        Text(`${index + 1}`)
          .fontSize(12)
          .fontColor('#FFFFFF')
          .width(22)
          .height(22)
          .borderRadius(11)
          .backgroundColor('#007AFF')
          .textAlign(TextAlign.Center)

        Text(item.medicineName || '未知药品')
          .fontSize(15)
          .fontWeight(FontWeight.Medium)
          .fontColor('#333333')
          .margin({ left: 10 })
          .layoutWeight(1)

        Text(`¥${this.formatAmount(item.price)}`)
          .fontSize(15)
          .fontColor('#FF4D4F')
          .fontWeight(FontWeight.Bold)
      }
      .width('100%')

      if (item.spec) {
        Row() {
          Text('规格')
            .fontSize(13)
            .fontColor('#999999')
            .width(36)
          Text(item.spec)
            .fontSize(13)
            .fontColor('#666666')
            .layoutWeight(1)
        }
        .width('100%')
        .margin({ top: 4 })
      }

      if (item.usage_) {
        Row() {
          Text('用法')
            .fontSize(13)
            .fontColor('#999999')
            .width(36)
          Text(item.usage_)
            .fontSize(13)
            .fontColor('#666666')
            .layoutWeight(1)
        }
        .width('100%')
        .margin({ top: 2 })
      }

      if (item.num > 0) {
        Row() {
          Text('数量')
            .fontSize(13)
            .fontColor('#999999')
            .width(36)
          Text(`${item.num}`)
            .fontSize(13)
            .fontColor('#666666')
            .layoutWeight(1)
        }
        .width('100%')
        .margin({ top: 2 })
      }
    }
    .width('100%')
    .padding(14)
    .backgroundColor('#F8FAFF')
    .borderRadius(10)
    .margin({ bottom: 10 })
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

        Text('处方详情')
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

      // ===== 内容 =====
      List() {
        // 基本信息卡片
        ListItem() {
          Column() {
            Row() {
              Column() {
                Text('处方信息')
                  .fontSize(16)
                  .fontWeight(FontWeight.Bold)
                  .fontColor('#333333')
                Text(`处方号: ${this.registerId}`)
                  .fontSize(12)
                  .fontColor('#999999')
                  .margin({ top: 2 })
              }
              .alignItems(HorizontalAlign.Start)

              Blank()

              this.StatusTag()
            }
            .width('100%')

            Divider()
              .color('#F0F0F0')
              .margin({ top: 12, bottom: 12 })

            this.InfoRow('患者姓名', this.patientName)
            this.InfoRow('开方医生', this.doctorName)
            this.InfoRow('开方日期', this.prescriptionDate)
          }
          .width('100%')
          .padding(16)
          .backgroundColor('#FFFFFF')
          .borderRadius(12)
          .margin({ top: 12, bottom: 12 })
        }

        // 药品列表卡片
        ListItem() {
          Column() {
            Row() {
              Text('💊 药品明细')
                .fontSize(16)
                .fontWeight(FontWeight.Bold)
                .fontColor('#333333')

              Blank()

              Text(`共 ${this.prescriptions.length} 种`)
                .fontSize(13)
                .fontColor('#999999')
            }
            .width('100%')
            .margin({ bottom: 12 })

            if (this.prescriptions.length > 0) {
              ForEach(this.prescriptions, (item: PrescriptionItem, index: number) => {
                this.MedicineItem(item, index)
              }, (item: PrescriptionItem, index: number) => item.prescriptionId || `medicine_${index}`)
            } else {
              Column() {
                Text('暂无药品信息')
                  .fontSize(14)
                  .fontColor('#999999')
                  .padding(20)
              }
              .width('100%')
              .backgroundColor('#F8FAFF')
              .borderRadius(10)
            }
          }
          .width('100%')
          .padding(16)
          .backgroundColor('#FFFFFF')
          .borderRadius(12)
          .margin({ bottom: 12 })
        }

        // 合计金额卡片
        ListItem() {
          Column() {
            Row() {
              Text('合计金额')
                .fontSize(15)
                .fontColor('#333333')
                .fontWeight(FontWeight.Medium)

              Blank()

              Text(`¥${this.formatAmount(this.totalAmount)}`)
                .fontSize(22)
                .fontColor('#FF4D4F')
                .fontWeight(FontWeight.Bold)
            }
            .width('100%')
          }
          .width('100%')
          .padding(16)
          .backgroundColor('#FFFFFF')
          .borderRadius(12)
          .margin({ bottom: 12 })
        }

        // 去支付按钮（待支付状态）
        if (this.payStatus === 'WAITING') {
          ListItem() {
            Button('去支付')
              .width('100%')
              .height(48)
              .fontSize(16)
              .fontColor('#FFFFFF')
              .backgroundColor('#007AFF')
              .borderRadius(12)
              .onClick(() => {
                this.goToPay();
              })
          }
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
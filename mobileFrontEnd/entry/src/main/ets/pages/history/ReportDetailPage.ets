// pages/history/ReportDetailPage.ets
import router from '@ohos.router';
import promptAction from '@ohos.promptAction';
import { HttpUtil } from '../common/HttpUtil';
import { APIConfig } from '../common/APIConfig';

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

const ABNORMAL_COLOR: Record<string, string> = {
  '正常': '#52C41A',
  'NORMAL': '#52C41A',
  '偏高': '#FAAD14',
  '偏低': '#FAAD14',
  '异常': '#FF4D4F',
  '危急': '#FF0000'
};

@Entry
@Component
export struct ReportDetailPage {
  @State orderItemId: string = '';
  @State itemName: string = '';
  @State itemCategory: string = '';
  @State price: number = 0;
  @State status: string = '';
  @State abnormalFlag: string = '';
  @State reportDetail: ReportDetail | null = null;
  @State isLoading: boolean = true;
  @State errorMessage: string = '';

  private baseUrl: string = APIConfig.PATIENT_SERVICE_URL;

  aboutToAppear(): void {
    const params = router.getParams() as Record<string, string>;
    if (params) {
      this.orderItemId = params['orderItemId'] || '';
      this.itemName = params['itemName'] || '';
      this.itemCategory = params['itemCategory'] || '';
      this.price = Number(params['price']) || 0;
      this.status = params['status'] || '';
      this.abnormalFlag = params['abnormalFlag'] || '';
    }
    if (this.orderItemId) {
      this.loadReportDetail();
    } else {
      this.isLoading = false;
      this.errorMessage = '参数错误，请返回重试';
    }
  }

  async loadReportDetail(): Promise<void> {
    this.isLoading = true;
    this.errorMessage = '';

    try {
      const context = getContext(this);
      const url = `${this.baseUrl}/medical-order/report/${this.orderItemId}`;
      const result = await HttpUtil.get<ApiResponse<ReportDetail>>(url, context);

      if (result.code === 200 && result.data) {
        this.reportDetail = result.data;
        console.info('报告详情加载成功');
      } else {
        this.errorMessage = result.msg || '加载报告失败';
      }
    } catch (error) {
      console.error('加载报告详情异常:', JSON.stringify(error));
      this.errorMessage = '网络请求失败，请检查网络连接';
    } finally {
      this.isLoading = false;
    }
  }

  private goBack(): void {
    router.back();
  }

  private getAbnormalColor(flag: string): string {
    return ABNORMAL_COLOR[flag] || '#666666';
  }

  private shouldShowAbnormal(flag: string | undefined): boolean {
    return flag !== undefined && flag !== null && flag !== '正常' && flag !== 'NORMAL' && flag !== '';
  }

  private formatDate(dateStr: string): string {
    if (!dateStr) return '';
    const parts = dateStr.split('T');
    return parts[0] || dateStr;
  }

  private getReferenceText(ind: Indicator): string {
    if (ind.referenceText) {
      return '参考范围: ' + ind.referenceText;
    }
    if (ind.referenceMin !== undefined || ind.referenceMax !== undefined) {
      let refText: string = '参考范围: ';
      if (ind.referenceMin !== undefined) {
        refText += ind.referenceMin;
      }
      if (ind.referenceMax !== undefined) {
        if (ind.referenceMin !== undefined) {
          refText += ' - ' + ind.referenceMax;
        } else {
          refText += ind.referenceMax;
        }
      }
      return refText;
    }
    return '';
  }

  @Builder
  private AbnormalTag(flag: string) {
    if (this.shouldShowAbnormal(flag)) {
      Row() {
        Text('⚠️ ' + flag)
          .fontSize(12)
          .fontColor(this.getAbnormalColor(flag))
          .fontWeight(FontWeight.Medium)
      }
      .padding({ left: 10, right: 10, top: 3, bottom: 3 })
      .backgroundColor(`${this.getAbnormalColor(flag)}15`)
      .borderRadius(10)
    }
  }

  @Builder
  private IndicatorItem(ind: Indicator) {
    Row() {
      Text(ind.indicatorName)
        .fontSize(13)
        .fontColor('#333333')
        .width(80)

      Text(ind.resultValue || '-')
        .fontSize(14)
        .fontWeight(FontWeight.Medium)
        .fontColor('#1A1A1A')
        .width(80)
        .textAlign(TextAlign.Center)

      Text(ind.unit || '')
        .fontSize(12)
        .fontColor('#8C8C8C')
        .width(50)

      Text(this.getReferenceText(ind))
        .fontSize(12)
        .fontColor('#8C8C8C')
        .layoutWeight(1)
        .textAlign(TextAlign.End)
    }
    .width('100%')
    .padding(10)
    .border({
      width: { bottom: 0.5 },
      color: { bottom: '#F0F0F0' }
    })
  }

  @Builder
  private LoadingState() {
    Column() {
      LoadingProgress()
        .width(40)
        .height(40)
        .color('#007AFF')
      Text('加载报告中...')
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
          this.loadReportDetail();
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
      Text('暂无报告内容')
        .fontSize(18)
        .fontColor('#1A1A1A')
        .fontWeight(FontWeight.Medium)
      Text('该检查项目暂无报告数据')
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

        Text('报告详情')
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

      if (this.isLoading) {
        this.LoadingState()
      } else if (this.errorMessage) {
        this.ErrorState()
      } else if (this.reportDetail) {
        // 直接使用 this.reportDetail，不需要 const 赋值
        Scroll() {
          Column() {
            // PDF风格报告
            Column() {
              // 报告标题
              Column() {
                Text(this.reportDetail.itemName || '检查报告')
                  .fontSize(20)
                  .fontWeight(FontWeight.Bold)
                  .fontColor('#1A1A1A')
                  .margin({ bottom: 4 })

                if (this.shouldShowAbnormal(this.reportDetail.abnormalFlag)) {
                  this.AbnormalTag(this.reportDetail.abnormalFlag)
                }

                Text('报告编号: ' + (this.reportDetail.reportId || '-'))
                  .fontSize(12)
                  .fontColor('#8C8C8C')
                  .margin({ top: 6 })
              }
              .width('100%')
              .padding({ bottom: 16 })
              .border({ width: { bottom: 1 }, color: '#E5E5E5' })
              .alignItems(HorizontalAlign.Center)

              // 患者信息
              Column() {
                Row() {
                  Text('患者姓名')
                    .fontSize(13)
                    .fontColor('#666666')
                    .width(80)
                  Text(this.reportDetail.patientName || '-')
                    .fontSize(13)
                    .fontColor('#1A1A1A')
                    .layoutWeight(1)
                }
                .width('100%')
                .padding({ top: 6, bottom: 6 })

                Row() {
                  Text('检查项目')
                    .fontSize(13)
                    .fontColor('#666666')
                    .width(80)
                  Text(this.reportDetail.itemName || '-')
                    .fontSize(13)
                    .fontColor('#1A1A1A')
                    .layoutWeight(1)
                }
                .width('100%')
                .padding({ top: 6, bottom: 6 })

                Row() {
                  Text('检查类别')
                    .fontSize(13)
                    .fontColor('#666666')
                    .width(80)
                  Text(this.reportDetail.itemCategory || '-')
                    .fontSize(13)
                    .fontColor('#1A1A1A')
                    .layoutWeight(1)
                }
                .width('100%')
                .padding({ top: 6, bottom: 6 })

                Row() {
                  Text('检查时间')
                    .fontSize(13)
                    .fontColor('#666666')
                    .width(80)
                  Text(this.reportDetail.performedTime ? this.formatDate(this.reportDetail.performedTime) : '-')
                    .fontSize(13)
                    .fontColor('#1A1A1A')
                    .layoutWeight(1)
                }
                .width('100%')
                .padding({ top: 6, bottom: 6 })

                Row() {
                  Text('报告时间')
                    .fontSize(13)
                    .fontColor('#666666')
                    .width(80)
                  Text(this.reportDetail.reportTime ? this.formatDate(this.reportDetail.reportTime) : '-')
                    .fontSize(13)
                    .fontColor('#1A1A1A')
                    .layoutWeight(1)
                }
                .width('100%')
                .padding({ top: 6, bottom: 6 })
              }
              .width('100%')
              .padding({ top: 12, bottom: 12 })

              Divider()
                .color('#E5E5E5')
                .margin({ top: 8, bottom: 12 })

              // 结果摘要
              if (this.reportDetail.resultSummary) {
                Column() {
                  Text('📝 结果摘要')
                    .fontSize(14)
                    .fontWeight(FontWeight.Medium)
                    .fontColor('#1A1A1A')
                    .margin({ bottom: 6 })
                    .alignSelf(ItemAlign.Start)

                  Text(this.reportDetail.resultSummary)
                    .fontSize(13)
                    .fontColor('#333333')
                    .lineHeight(22)
                    .padding(12)
                    .backgroundColor('#F8FAFC')
                    .borderRadius(8)
                    .width('100%')
                }
                .width('100%')
                .margin({ bottom: 12 })
              }

              // 报告结论
              if (this.reportDetail.conclusion) {
                Column() {
                  Text('📋 报告结论')
                    .fontSize(14)
                    .fontWeight(FontWeight.Medium)
                    .fontColor('#1A1A1A')
                    .margin({ bottom: 6 })
                    .alignSelf(ItemAlign.Start)

                  Text(this.reportDetail.conclusion)
                    .fontSize(13)
                    .fontColor('#333333')
                    .lineHeight(22)
                    .padding(12)
                    .backgroundColor('#F8FAFC')
                    .borderRadius(8)
                    .width('100%')
                }
                .width('100%')
                .margin({ bottom: 12 })
              }

              // 检验指标表格
              if (this.reportDetail.itemCategory === 'LAB' && this.reportDetail.indicators && this.reportDetail.indicators.length > 0) {
                Column() {
                  Text('🔬 检验指标')
                    .fontSize(14)
                    .fontWeight(FontWeight.Medium)
                    .fontColor('#1A1A1A')
                    .margin({ bottom: 8 })
                    .alignSelf(ItemAlign.Start)

                  // 表头
                  Row() {
                    Text('项目')
                      .fontSize(12)
                      .fontColor('#666666')
                      .fontWeight(FontWeight.Medium)
                      .width(80)
                    Text('结果')
                      .fontSize(12)
                      .fontColor('#666666')
                      .fontWeight(FontWeight.Medium)
                      .width(80)
                      .textAlign(TextAlign.Center)
                    Text('单位')
                      .fontSize(12)
                      .fontColor('#666666')
                      .fontWeight(FontWeight.Medium)
                      .width(50)
                    Text('参考范围')
                      .fontSize(12)
                      .fontColor('#666666')
                      .fontWeight(FontWeight.Medium)
                      .layoutWeight(1)
                      .textAlign(TextAlign.End)
                  }
                  .width('100%')
                  .padding(8)
                  .backgroundColor('#F5F5F5')
                  .borderRadius(6)

                  ForEach(this.reportDetail.indicators, (ind: Indicator) => {
                    this.IndicatorItem(ind)
                  }, (ind: Indicator) => ind.indicatorId)
                }
                .width('100%')
                .margin({ bottom: 12 })
              }

              // 底部水印
              Text('—— 报告结束 ——')
                .fontSize(12)
                .fontColor('#CCCCCC')
                .width('100%')
                .textAlign(TextAlign.Center)
                .margin({ top: 16, bottom: 8 })
            }
            .width('100%')
            .padding(20)
            .backgroundColor('#FFFFFF')
            .borderRadius(12)
            .shadow({
              radius: 8,
              color: 'rgba(0,0,0,0.06)',
              offsetY: 2
            })
          }
          .width('100%')
          .padding(16)
        }
        .width('100%')
        .layoutWeight(1)
        .backgroundColor('#F5F5F5')
        .scrollBar(BarState.Off)
      } else {
        this.EmptyState()
      }
    }
    .width('100%')
    .height('100%')
    .backgroundColor('#FFFFFF')
  }
}
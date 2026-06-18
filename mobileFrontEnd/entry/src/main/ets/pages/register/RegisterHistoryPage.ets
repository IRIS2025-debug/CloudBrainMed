// pages/register/RegisterHistoryPage.ets
import router from '@ohos.router';
import { HttpUtil } from '../common/HttpUtil';
import promptAction from '@ohos.promptAction';

interface Registration {
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
}

interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
}

@Entry
@Component
export struct RegisterHistoryPage {
  @State registrations: Registration[] = [];
  @State isLoading: boolean = true;
  @State selectedTab: number = 0;
  private patientId: string = '';
  private baseUrl: string = 'http://192.168.25.143:8004/patient-service';

  aboutToAppear() {
    const params = router.getParams() as Record<string, string>;
    this.patientId = params?.['patientId'] || '';
    this.loadHistory();
  }

  async loadHistory() {
    this.isLoading = true;
    const context = getContext(this);
    try {
      const result = await HttpUtil.get<ApiResponse<Registration[]>>(
        this.baseUrl + '/register/history/' + this.patientId,
        context
      );
      if (result.code === 200) {
        this.registrations = result.data;
      } else {
        promptAction.showToast({
          message: result.message || '加载挂号记录失败',
          duration: 2000
        });
      }
    } catch (error) {
      console.error('加载挂号记录失败：' + JSON.stringify(error));
      promptAction.showToast({
        message: '网络请求失败，请稍后重试',
        duration: 2000
      });
    } finally {
      this.isLoading = false;
    }
  }

  getFilteredList(): Registration[] {
    if (this.selectedTab === 0) {
      return this.registrations;
    } else if (this.selectedTab === 1) {
      const waitingList: Registration[] = [];
      for (let i = 0; i < this.registrations.length; i++) {
        if (this.registrations[i].payStatus === 'WAITING') {
          waitingList.push(this.registrations[i]);
        }
      }
      return waitingList;
    } else {
      const paidList: Registration[] = [];
      for (let i = 0; i < this.registrations.length; i++) {
        if (this.registrations[i].payStatus === 'PAID') {
          paidList.push(this.registrations[i]);
        }
      }
      return paidList;
    }
  }

  getPayStatusText(status: string): string {
    if (status === 'WAITING') {
      return '待支付';
    } else if (status === 'PAID') {
      return '已支付';
    } else if (status === 'CANCELLED') {
      return '已取消';
    }
    return status;
  }

  getPayStatusColor(status: string): string {
    if (status === 'WAITING') {
      return '#FAAD14';
    } else if (status === 'PAID') {
      return '#52C41A';
    } else if (status === 'CANCELLED') {
      return '#FF4D4F';
    }
    return '#999999';
  }

  getPayStatusBgColor(status: string): string {
    if (status === 'WAITING') {
      return '#FFF7E6';
    } else if (status === 'PAID') {
      return '#F6FFED';
    } else if (status === 'CANCELLED') {
      return '#FFF0F0';
    }
    return '#F5F5F5';
  }

  build() {
    Column() {
      // 标题栏
      Row() {
        Text('←')
          .fontSize(24)
          .onClick(() => {
            router.back();
          })
        Text('挂号记录')
          .fontSize(20)
          .fontWeight(FontWeight.Bold)
          .layoutWeight(1)
          .textAlign(TextAlign.Center)
        Blank().width(24)
      }
      .width('100%')
      .padding(16)
      .backgroundColor('#FFFFFF')

      // Tab栏
      Row() {
        Text('全部')
          .fontSize(16)
          .fontWeight(this.selectedTab === 0 ? FontWeight.Bold : FontWeight.Normal)
          .fontColor(this.selectedTab === 0 ? '#007AFF' : '#666666')
          .padding({ left: 16, right: 16, bottom: 12 })
          .border({ width: { bottom: 2 }, color: this.selectedTab === 0 ? '#007AFF' : 'transparent' })
          .onClick(() => {
            this.selectedTab = 0;
          })

        Text('待支付')
          .fontSize(16)
          .fontWeight(this.selectedTab === 1 ? FontWeight.Bold : FontWeight.Normal)
          .fontColor(this.selectedTab === 1 ? '#007AFF' : '#666666')
          .padding({ left: 16, right: 16, bottom: 12 })
          .border({ width: { bottom: 2 }, color: this.selectedTab === 1 ? '#007AFF' : 'transparent' })
          .onClick(() => {
            this.selectedTab = 1;
          })

        Text('已完成')
          .fontSize(16)
          .fontWeight(this.selectedTab === 2 ? FontWeight.Bold : FontWeight.Normal)
          .fontColor(this.selectedTab === 2 ? '#007AFF' : '#666666')
          .padding({ left: 16, right: 16, bottom: 12 })
          .border({ width: { bottom: 2 }, color: this.selectedTab === 2 ? '#007AFF' : 'transparent' })
          .onClick(() => {
            this.selectedTab = 2;
          })
      }
      .width('100%')
      .padding({ top: 16 })
      .justifyContent(FlexAlign.SpaceEvenly)

      if (this.isLoading) {
        Column() {
          LoadingProgress()
            .width(60)
            .height(60)
            .color('#007AFF')
          Text('加载中...')
            .fontSize(14)
            .fontColor('#999999')
            .margin({ top: 16 })
        }
        .width('100%')
        .height('100%')
        .justifyContent(FlexAlign.Center)
      } else if (this.getFilteredList().length === 0) {
        Column() {
          Text('📋')
            .fontSize(64)
            .margin({ bottom: 16 })
          Text('暂无挂号记录')
            .fontSize(16)
            .fontColor('#999999')
        }
        .width('100%')
        .height('100%')
        .justifyContent(FlexAlign.Center)
      } else {
        Scroll() {
          Column() {
            ForEach(this.getFilteredList(), (record: Registration, index: number) => {
              Column() {
                // 头部：日期和状态
                Row() {
                  Column() {
                    Text(record.visitDate)
                      .fontSize(16)
                      .fontWeight(FontWeight.Bold)
                      .fontColor('#333333')
                    Text(record.department)
                      .fontSize(12)
                      .fontColor('#666666')
                      .margin({ top: 4 })
                  }
                  .alignItems(HorizontalAlign.Start)

                  Blank()

                  Text(this.getPayStatusText(record.payStatus))
                    .fontSize(12)
                    .fontColor(this.getPayStatusColor(record.payStatus))
                    .backgroundColor(this.getPayStatusBgColor(record.payStatus))
                    .padding({ left: 8, right: 8, top: 4, bottom: 4 })
                    .borderRadius(12)
                }
                .width('100%')

                Divider()
                  .margin({ top: 12, bottom: 12 })

                // 信息行
                Row() {
                  this.InfoItem('就诊人', record.name)
                  this.InfoItem('就诊时间', record.consultTime)
                }
                .width('100%')
                .margin({ bottom: 12 })

                // 主诉
                if (record.chiefComplaint && record.chiefComplaint.length > 0) {
                  Row() {
                    Text('主诉')
                      .fontSize(14)
                      .fontColor('#999999')
                      .width(60)
                    Text(record.chiefComplaint)
                      .fontSize(14)
                      .fontColor('#333333')
                      .layoutWeight(1)
                      .maxLines(2)
                      .textOverflow({ overflow: TextOverflow.Ellipsis })
                  }
                  .width('100%')
                  .margin({ bottom: 12 })
                }

                // 按钮行
                Row() {
                  if (record.payStatus === 'WAITING') {
                    Button('去支付')
                      .width('48%')
                      .height(36)
                      .backgroundColor('#007AFF')
                      .fontColor('#FFFFFF')
                      .borderRadius(18)
                      .fontSize(14)
                      .onClick(() => {
                        router.pushUrl({
                          url: 'pages/pay/PaymentPage',
                          params: {
                            registerId: record.registerId,
                            amount: record.price
                          }
                        });
                      })

                    Button('查看详情')
                      .width('48%')
                      .height(36)
                      .backgroundColor('#F0F0F0')
                      .fontColor('#007AFF')
                      .borderRadius(18)
                      .fontSize(14)
                      .margin({ left: '4%' })
                      .onClick(() => {
                        router.pushUrl({
                          url: 'pages/register/OrderSuccessPage',
                          params: { registerId: record.registerId }
                        });
                      })
                  } else {
                    Button('查看详情')
                      .width('100%')
                      .height(36)
                      .backgroundColor('#F0F0F0')
                      .fontColor('#007AFF')
                      .borderRadius(18)
                      .fontSize(14)
                      .onClick(() => {
                        router.pushUrl({
                          url: 'pages/register/OrderSuccessPage',
                          params: { registerId: record.registerId }
                        });
                      })
                  }
                }
                .width('100%')
                .margin({ top: 8 })
              }
              .width('100%')
              .backgroundColor('#FFFFFF')
              .borderRadius(16)
              .padding(16)
              .margin({ bottom: 12 })
            }, (record: Registration, index: number) => record.registerId)
          }
          .width('100%')
          .padding(16)
        }
        .scrollable(ScrollDirection.Vertical)
        .scrollBar(BarState.Off)
        .layoutWeight(1)
      }
    }
    .width('100%')
    .height('100%')
    .backgroundColor('#F5F5F5')
  }

  @Builder
  InfoItem(label: string, value: string) {
    Row() {
      Text(label)
        .fontSize(14)
        .fontColor('#999999')
        .width(60)
      Text(value)
        .fontSize(14)
        .fontColor('#333333')
        .layoutWeight(1)
    }
    .width('50%')
  }
}
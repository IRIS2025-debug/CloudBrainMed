// pages/HomePage.ets
import router from '@ohos.router';
import promptAction from '@ohos.promptAction';
import pasteboard from '@ohos.pasteboard';
import noticesData from './common/notices.json';

// ===== 定义接口 =====
interface FunctionItem {
  name: string;
  icon: string;
  route: string;
  bgColor: string[];
}

interface NoticeItem {
  id: number;
  title: string;
  time: string;
  summary?: string;
  type?: string;
}

interface NoticesData {
  notices: NoticeItem[];
}

@Entry
@Component
export struct HomePage {
  @Prop patientId: string = '';
  @Prop userName: string = '';
  @Prop userPhone: string = '';
  @Prop userGender: number = 0;
  @Prop userGenderText: string = '';
  @Prop userAge: number = 0;
  @State currentNoticeIndex: number = 0;
  @State notices: NoticeItem[] = [];
  private timer: number = -1;

  private functions: FunctionItem[] = [
    { name: '预约挂号', icon: '🏥', route: 'pages/register/DeptListPage', bgColor: ['#E8F0FE', '#D4E4FF'] },
    { name: '在线问诊', icon: '💬', route: 'pages/aiConsult/AIConsultation', bgColor: ['#E6F7F0', '#C8F0E0'] },
    { name: '检查报告', icon: '📊', route: 'pages/medical/MedicalReport', bgColor: ['#E6F3FA', '#C8E6F5'] },
    { name: '中医外方', icon: '🌿', route: 'pages/medical/PrescriptionHistory', bgColor: ['#EEF7EC', '#DCEFD8'] },
    { name: '用药助手', icon: '💊', route: 'pages/medicineAi/MedicineAiChatPage', bgColor: ['#F5EEF8', '#EBDCF0'] },
    { name: '缴费服务', icon: '💳', route: 'pages/pay/PayHistory', bgColor: ['#FFF5E6', '#FFEBCD'] }
  ];

  aboutToAppear() {
    console.info('HomePage接收参数: patientId=' + this.patientId + ', name=' + this.userName);
    this.loadNotices();
  }

  aboutToDisappear() {
    if (this.timer !== -1) {
      clearInterval(this.timer);
      this.timer = -1;
    }
  }

  private loadNotices(): void {
    try {
      const data: NoticesData = noticesData as NoticesData;
      this.notices = data.notices || [];
      console.info('加载健康资讯成功，共 ' + this.notices.length + ' 条');
      if (this.notices.length > 0) {
        this.startNoticeCarousel();
      }
    } catch (error) {
      console.error('加载健康资讯失败：' + JSON.stringify(error));
      this.notices = this.getDefaultNotices();
      if (this.notices.length > 0) {
        this.startNoticeCarousel();
      }
    }
  }

  private getDefaultNotices(): NoticeItem[] {
    return [
      { id: 1, title: '关于2026年端午节放假安排的通知', time: '2026-05-30', summary: '', type: '通知' },
      { id: 2, title: '智慧医疗系统升级公告', time: '2026-05-28', summary: '', type: '公告' },
      { id: 3, title: '新功能上线：AI智能问诊助手', time: '2026-05-25', summary: '', type: '新功能' }
    ];
  }

  private startNoticeCarousel(): void {
    if (this.timer !== -1) {
      clearInterval(this.timer);
    }
    if (this.notices.length > 0) {
      this.timer = setInterval(() => {
        this.currentNoticeIndex = (this.currentNoticeIndex + 1) % this.notices.length;
      }, 4000);
    }
  }

  private copyPatientId(): void {
    if (this.patientId) {
      const pasteData: pasteboard.PasteData = pasteboard.createData(pasteboard.MIMETYPE_TEXT_PLAIN, this.patientId);
      const systemPasteboard: pasteboard.SystemPasteboard = pasteboard.getSystemPasteboard();
      systemPasteboard.setData(pasteData, (err: Error) => {
        if (err) {
          console.error('复制失败: ' + JSON.stringify(err));
          promptAction.showToast({
            message: '复制失败，请手动复制',
            duration: 1500
          });
        } else {
          promptAction.showToast({
            message: '门诊号已复制',
            duration: 1500
          });
        }
      });
    }
  }

  // ✅ @Builder 接收 item 参数，内部只有 UI 组件
  @Builder
  NoticeItemBuilder(item: NoticeItem) {
    Column() {
      if (item.type && item.type.length > 0) {
        Text(item.type)
          .fontSize(10)
          .fontColor('#FFFFFF')
          .backgroundColor('#2563EB')
          .padding({ left: 8, right: 8, top: 2, bottom: 2 })
          .borderRadius(10)
          .margin({ bottom: 6 })
          .alignSelf(ItemAlign.Start)
      }

      Text(item.title)
        .fontSize(14)
        .fontColor('#1A1A1A')
        .fontWeight(FontWeight.Medium)
        .maxLines(1)
        .textOverflow({ overflow: TextOverflow.Ellipsis })
        .width('100%')

      Row() {
        Text(item.time)
          .fontSize(12)
          .fontColor('#999999')
        Blank()
        Text('查看详情 ›')
          .fontSize(12)
          .fontColor('#2563EB')
      }
      .width('100%')
      .margin({ top: 4 })
    }
    .width('100%')
    .padding({ top: 4, bottom: 4 })
    .transition(TransitionEffect.OPACITY.animation({ duration: 500 }))
  }

  build() {
    Column() {
      // ===== 电子就诊卡 =====
      Column() {
        Row() {
          Column() {
            Text(this.userName || '未登录')
              .fontSize(20)
              .fontWeight(FontWeight.Bold)
              .fontColor('#FFFFFF')

            Row() {
              Text(this.userGenderText || '未知')
                .fontSize(14)
                .fontColor('rgba(255,255,255,0.85)')
              if (this.userAge > 0) {
                Text('|')
                  .fontSize(14)
                  .fontColor('rgba(255,255,255,0.5)')
                  .margin({ left: 8, right: 8 })
                Text(`${this.userAge}岁`)
                  .fontSize(14)
                  .fontColor('rgba(255,255,255,0.85)')
              }
            }
            .margin({ top: 2 })
          }
          .alignItems(HorizontalAlign.Start)

          Blank()

          Column() {
            Text('电子就诊卡')
              .fontSize(12)
              .fontColor('rgba(255,255,255,0.7)')

            Row() {
              Text('📋')
                .fontSize(12)
              Text(`门诊号：${this.patientId || '暂无'}`)
                .fontSize(13)
                .fontColor('#FFFFFF')
                .fontWeight(FontWeight.Medium)
                .margin({ left: 4 })
            }
            .padding({ left: 12, right: 16, top: 4, bottom: 4 })
            .backgroundColor('rgba(255,255,255,0.2)')
            .borderRadius(16)
            .margin({ top: 4 })
            .onClick(() => {
              this.copyPatientId();
            })
          }
          .alignItems(HorizontalAlign.End)
        }
        .width('100%')

        Row() {
          Text('出示就诊码')
            .fontSize(15)
            .fontWeight(FontWeight.Medium)
            .fontColor('#2563EB')
        }
        .width('100%')
        .height(40)
        .backgroundColor('#FFFFFF')
        .borderRadius(20)
        .justifyContent(FlexAlign.Center)
        .margin({ top: 12 })
        .onClick(() => {
          promptAction.showToast({
            message: '出示就诊码',
            duration: 1500
          });
        })
      }
      .width('92%')
      .padding({ left: 20, right: 20, top: 20, bottom: 18 })
      .linearGradient({
        angle: 135,
        colors: [
          ['#2563EB', 0],
          ['#3B82F6', 0.5],
          ['#60A5FA', 1]
        ]
      })
      .borderRadius(16)
      .shadow({
        radius: 16,
        color: 'rgba(37, 99, 235, 0.25)',
        offsetX: 0,
        offsetY: 6
      })
      .margin({ top: 16 })

      // ===== 快捷服务和健康资讯 =====
      Column() {
        // ===== 快捷服务 =====
        Column() {
          Row() {
            Text('快捷服务')
              .fontSize(17)
              .fontWeight(FontWeight.Bold)
              .fontColor('#1A1A1A')

            Blank()

            Row() {
              Text('全部')
                .fontSize(13)
                .fontColor('#8C8C8C')
              Text('›')
                .fontSize(16)
                .fontColor('#8C8C8C')
                .margin({ left: 2 })
            }
            .onClick(() => {})
          }
          .width('100%')
          .margin({ bottom: 14 })

          Grid() {
            ForEach(this.functions, (item: FunctionItem) => {
              GridItem() {
                Column() {
                  Row() {
                    Text(item.icon)
                      .fontSize(26)
                  }
                  .width(56)
                  .height(56)
                  .borderRadius(16)
                  .linearGradient({
                    angle: 135,
                    colors: [
                      [item.bgColor[0], 0],
                      [item.bgColor[1], 1]
                    ]
                  })
                  .justifyContent(FlexAlign.Center)
                  .alignItems(VerticalAlign.Center)

                  Text(item.name)
                    .fontSize(12)
                    .fontColor('#333333')
                    .margin({ top: 6 })
                    .textAlign(TextAlign.Center)
                }
                .width('100%')
                .padding({ top: 2, bottom: 2 })
                .onClick(() => {
                  router.pushUrl({
                    url: item.route,
                    params: { patientId: this.patientId }
                  }).catch(() => {
                    promptAction.showToast({
                      message: '页面跳转中...',
                      duration: 1500
                    });
                  });
                })
              }
            }, (item: FunctionItem) => item.name)
          }
          .columnsTemplate('1fr 1fr 1fr')
          .rowsTemplate('1fr 1fr')
          .rowsGap(10)
          .columnsGap(10)
          .height(190)
        }
        .width('100%')
        .padding(18)
        .backgroundColor('#FFFFFF')
        .borderRadius(16)
        .margin({ top: 0, left: 16, right: 16 })
        .shadow({
          radius: 12,
          color: 'rgba(0, 0, 0, 0.04)',
          offsetX: 0,
          offsetY: 4
        })

        // ===== 健康资讯 =====
        Column() {
          Row() {
            Text('健康资讯')
              .fontSize(17)
              .fontWeight(FontWeight.Bold)
              .fontColor('#1A1A1A')

            Blank()

            Row() {
              ForEach(this.notices, (item: NoticeItem, index: number) => {
                Circle()
                  .width(6)
                  .height(6)
                  .fill(this.currentNoticeIndex === index ? '#2563EB' : '#E0E0E0')
                  .margin({ left: 3, right: 3 })
              }, (item: NoticeItem) => item.id.toString())
            }
          }
          .width('100%')
          .margin({ bottom: 10 })

          // ✅ build 中直接判断，然后传参给 @Builder
          if (this.notices.length > 0) {
            Stack() {
              Column() {
                this.NoticeItemBuilder(this.notices[this.currentNoticeIndex])
              }
              .width('100%')
              .padding(14)
              .backgroundColor('#F8FAFF')
              .borderRadius(12)
              .border({
                width: 1,
                color: '#EEF3FF'
              })
            }
            .width('100%')
            .onClick(() => {
              const currentItem: NoticeItem = this.notices[this.currentNoticeIndex];
              if (currentItem) {
                router.pushUrl({
                  url: 'pages/NoticeDetail',
                  params: {
                    id: currentItem.id,
                    title: currentItem.title,
                    time: currentItem.time,
                    summary: currentItem.summary || '',
                    type: currentItem.type || ''
                  }
                }).catch(() => {
                  promptAction.showToast({
                    message: '正在加载资讯详情...',
                    duration: 1500
                  });
                });
              }
            })
          } else {
            Column() {
              Text('暂无资讯')
                .fontSize(14)
                .fontColor('#999999')
                .padding(20)
            }
            .width('100%')
            .padding(14)
            .backgroundColor('#F8FAFF')
            .borderRadius(12)
            .border({
              width: 1,
              color: '#EEF3FF'
            })
          }
        }
        .width('100%')
        .padding(18)
        .backgroundColor('#FFFFFF')
        .borderRadius(16)
        .margin({ top: 12, left: 16, right: 16, bottom: 16 })
        .shadow({
          radius: 12,
          color: 'rgba(0, 0, 0, 0.04)',
          offsetX: 0,
          offsetY: 4
        })
      }
      .width('100%')
      .padding(0)
      .backgroundColor('#F5F7FA')
      .layoutWeight(1)
    }
    .width('100%')
    .height('100%')
    .backgroundColor('#F5F7FA')
  }
}
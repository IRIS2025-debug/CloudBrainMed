// pages/medical/MedicalRecordsPage.ets
import router from '@ohos.router';

interface RecordItem {
  id: string;
  date: string;
  hospital: string;
  doctor: string;
  department: string;
  diagnosis: string;
  status: string;
}

@Component
export struct MedicalRecordsPage {
  patientId: string = '';
  userName: string = '';
  @State selectedTab: number = 0;
  @State records: RecordItem[] = [];

  aboutToAppear() {
    console.log('就诊记录 - 患者ID: ' + this.patientId);
  }

  build() {
    Column() {
      Row() {
        ForEach(['全部', '进行中', '已完成'], (tab: string, index: number) => {
          Text(tab)
            .fontSize(16)
            .fontWeight(this.selectedTab === index ? FontWeight.Bold : FontWeight.Normal)
            .fontColor(this.selectedTab === index ? '#007AFF' : '#666666')
            .padding({ left: 16, right: 16, bottom: 12 })
            .border({ width: { bottom: 2 }, color: this.selectedTab === index ? '#007AFF' : 'transparent' })
            .onClick(() => {
              this.selectedTab = index;
            })
        })
      }
      .width('100%')
      .padding({ top: 16 })
      .justifyContent(FlexAlign.SpaceEvenly)

      if (this.records.length === 0) {
        Column() {
          Text('📋')
            .fontSize(64)
            .margin({ bottom: 16 })
          Text('暂无就诊记录')
            .fontSize(16)
            .fontColor('#999')
        }
        .width('100%')
        .height('100%')
        .justifyContent(FlexAlign.Center)
      } else {
        Scroll() {
          Column() {
            ForEach(this.records, (record: RecordItem) => {
              Column() {
                Row() {
                  Column() {
                    Text(record.date)
                      .fontSize(16)
                      .fontWeight(FontWeight.Bold)
                      .fontColor('#333')
                    Text(record.hospital)
                      .fontSize(12)
                      .fontColor('#666')
                      .margin({ top: 4 })
                  }
                  .alignItems(HorizontalAlign.Start)

                  Blank()

                  Text(record.status === 'completed' ? '已完成' : '进行中')
                    .fontSize(12)
                    .fontColor(record.status === 'completed' ? '#52C41A' : '#FF9800')
                    .backgroundColor(record.status === 'completed' ? '#F6FFED' : '#FFF7E6')
                    .padding({ left: 8, right: 8, top: 4, bottom: 4 })
                    .borderRadius(12)
                }
                .width('100%')

                Divider().margin({ top: 12, bottom: 12 })

                Row() {
                  this.InfoItem('医生', record.doctor)
                  this.InfoItem('科室', record.department)
                }
                .width('100%')
                .margin({ bottom: 12 })

                if (record.diagnosis) {
                  Row() {
                    Text('诊断')
                      .fontSize(14)
                      .fontColor('#999')
                      .width(60)
                    Text(record.diagnosis)
                      .fontSize(14)
                      .fontColor('#333')
                      .layoutWeight(1)
                  }
                  .width('100%')
                  .margin({ bottom: 12 })
                }

                Button('查看详情')
                  .width('100%')
                  .height(36)
                  .backgroundColor('#F0F0F0')
                  .fontColor('#007AFF')
                  .borderRadius(8)
                  .fontSize(14)
                  .onClick(() => {
                    router.pushUrl({ url: 'pages/medical/RecordDetail', params: { id: record.id } });
                  })
              }
              .width('100%')
              .backgroundColor('#FFFFFF')
              .borderRadius(12)
              .padding(16)
              .margin({ bottom: 12 })
            })
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
        .fontColor('#999')
        .width(60)
      Text(value)
        .fontSize(14)
        .fontColor('#333')
        .layoutWeight(1)
    }
    .width('50%')
  }
}
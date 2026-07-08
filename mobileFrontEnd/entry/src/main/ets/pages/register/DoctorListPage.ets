// pages/register/DoctorListPage.ets
import router from '@ohos.router';
import { HttpUtil } from '../common/HttpUtil';
import promptAction from '@ohos.promptAction';
import { APIConfig } from '../common/APIConfig';

interface Doctor {
  doctorId: string;
  name: string;
  gender: number;
  position: string;
  goodAt: string;
  introduction: string;
  avatar: string;
  departmentId: string;
}

interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
}

@Entry
@Component
export struct DoctorListPage {
  @State doctors: Doctor[] = [];
  @State isLoading: boolean = true;
  @State deptName: string = '';
  @State expandedDoctorIds: Set<string> = new Set();
  private deptId: string = '';
  private patientId: string = '';
  private baseUrl: string = APIConfig.PATIENT_SERVICE_URL;

  aboutToAppear() {
    const params = router.getParams() as Record<string, string>;
    this.deptId = params?.['deptId'] || '';
    this.deptName = params?.['deptName'] || '';
    this.patientId = params?.['patientId'] || '';
    this.loadDoctors();
  }

  async loadDoctors() {
    this.isLoading = true;
    const context = getContext(this);
    try {
      const result = await HttpUtil.get<ApiResponse<Doctor[]>>(
        this.baseUrl + '/register/doctors/' + this.deptId,
        context
      );
      if (result.code === 200) {
        this.doctors = result.data;
      } else {
        promptAction.showToast({
          message: result.message || '加载医生列表失败',
          duration: 2000
        });
      }
    } catch (error) {
      console.error('加载医生列表失败：' + JSON.stringify(error));
      promptAction.showToast({
        message: '网络请求失败，请稍后重试',
        duration: 2000
      });
    } finally {
      this.isLoading = false;
    }
  }

  private getGenderText(gender: number): string {
    if (gender === 1) return '男';
    if (gender === 2) return '女';
    return '';
  }

  private getInitial(name: string): string {
    return name ? name.charAt(0) : '';
  }

  private toggleExpand(doctorId: string): void {
    if (this.expandedDoctorIds.has(doctorId)) {
      this.expandedDoctorIds.delete(doctorId);
    } else {
      this.expandedDoctorIds.add(doctorId);
    }
    this.expandedDoctorIds = new Set(this.expandedDoctorIds);
  }

  build() {
    Column() {
      Row() {
        Text('‹')
          .fontSize(28)
          .fontColor('#333333')
          .width(44)
          .height(44)
          .textAlign(TextAlign.Center)
          .lineHeight(44)
          .onClick(() => router.back())

        Text(this.deptName || '选择医生')
          .fontSize(18)
          .fontWeight(FontWeight.Medium)
          .fontColor('#333333')
          .layoutWeight(1)
          .textAlign(TextAlign.Center)

        Row().width(44).height(44)
      }
      .width('100%')
      .height(56)
      .padding({ left: 8, right: 8 })
      .backgroundColor('#FFFFFF')

      if (this.isLoading) {
        this.LoadingView()
      } else if (this.doctors.length === 0) {
        this.EmptyView()
      } else {
        Scroll() {
          Column() {
            ForEach(this.doctors, (doctor: Doctor) => {
              this.DoctorCard(doctor)
            })
          }
          .width('100%')
          .padding(16)
        }
        .scrollable(ScrollDirection.Vertical)
        .scrollBar(BarState.Off)
        .layoutWeight(1)
        .backgroundColor('#F5F6FA')
      }
    }
    .width('100%')
    .height('100%')
    .backgroundColor('#F5F6FA')
  }

  @Builder
  LoadingView() {
    Column() {
      LoadingProgress()
        .width(36)
        .height(36)
        .color('#007AFF')
      Text('加载中...')
        .fontSize(14)
        .fontColor('#999999')
        .margin({ top: 12 })
    }
    .width('100%')
    .layoutWeight(1)
    .justifyContent(FlexAlign.Center)
  }

  @Builder
  EmptyView() {
    Column() {
      Text('🏥')
        .fontSize(48)
        .margin({ bottom: 12 })
      Text('暂无医生')
        .fontSize(16)
        .fontColor('#666666')
    }
    .width('100%')
    .layoutWeight(1)
    .justifyContent(FlexAlign.Center)
  }

  @Builder
  DoctorCard(doctor: Doctor) {
    Column() {
      // ===== 卡片主体 =====
      Row() {
        Column() {
          Text(this.getInitial(doctor.name))
            .fontSize(22)
            .fontWeight(FontWeight.Medium)
            .fontColor('#FFFFFF')
        }
        .width(52)
        .height(52)
        .borderRadius(26)
        .backgroundColor('#B8C5D6')
        .justifyContent(FlexAlign.Center)
        .alignItems(HorizontalAlign.Center)

        Column() {
          Text(doctor.name)
            .fontSize(16)
            .fontWeight(FontWeight.Medium)
            .fontColor('#1A1A1A')

          Row() {
            Text(doctor.position || '医师')
              .fontSize(12)
              .fontColor('#888888')

            Text('·')
              .fontSize(12)
              .fontColor('#CCCCCC')
              .margin({ left: 4, right: 4 })

            Text(this.getGenderText(doctor.gender))
              .fontSize(12)
              .fontColor('#888888')
          }
          .margin({ top: 2 })

          // 展开/收起提示（放在信息底部）
          Text(this.expandedDoctorIds.has(doctor.doctorId) ? '▲ 点击收起' : '▼ 点击展开')
            .fontSize(11)
            .fontColor('#BBBBBB')
            .margin({ top: 6 })
        }
        .alignItems(HorizontalAlign.Start)
        .layoutWeight(1)
        .margin({ left: 14 })

        Column() {
          Text('预约')
            .fontSize(13)
            .fontWeight(FontWeight.Medium)
            .fontColor('#007AFF')
        }
        .width(60)
        .height(32)
        .borderRadius(16)
        .border({ width: 1, color: '#007AFF' })
        .justifyContent(FlexAlign.Center)
        .alignItems(HorizontalAlign.Center)
        .onClick(() => {
          router.pushUrl({
            url: 'pages/register/ScheduleSelectPage',
            params: {
              doctorId: doctor.doctorId,
              doctorName: doctor.name,
              patientId: this.patientId,
              doctorPosition: doctor.position || '',
              doctorGender: this.getGenderText(doctor.gender),
              doctorGoodAt: doctor.goodAt || '',
              doctorIntroduction: doctor.introduction || '',
              department: this.deptName || ''
            }
          });
        })
      }
      .width('100%')
      .padding(16)
      .backgroundColor('#FFFFFF')
      .borderRadius(this.expandedDoctorIds.has(doctor.doctorId) ?
        { topLeft: 12, topRight: 12, bottomLeft: 0, bottomRight: 0 } : 12)
      .onClick(() => {
        this.toggleExpand(doctor.doctorId);
      })

      // ===== 展开的详细信息 =====
      if (this.expandedDoctorIds.has(doctor.doctorId)) {
        Column() {
          Divider()
            .color('#F0F0F0')
            .margin({ top: 0, bottom: 12 })

          // 简介
          if (doctor.introduction && doctor.introduction.length > 0) {
            Column() {
              Text('医生简介')
                .fontSize(13)
                .fontWeight(FontWeight.Medium)
                .fontColor('#666666')
                .margin({ bottom: 6 })
                .alignSelf(ItemAlign.Start)

              Text(doctor.introduction)
                .fontSize(13)
                .fontColor('#555555')
                .lineHeight(20)
                .width('100%')
                .textAlign(TextAlign.Start)
            }
            .width('100%')
            .alignItems(HorizontalAlign.Start)
          }

          // 擅长领域
          if (doctor.goodAt && doctor.goodAt.length > 0) {
            Column() {
              if (doctor.introduction && doctor.introduction.length > 0) {
                Divider()
                  .color('#F5F5F5')
                  .margin({ top: 10, bottom: 10 })
              }

              Text('擅长领域')
                .fontSize(13)
                .fontWeight(FontWeight.Medium)
                .fontColor('#666666')
                .margin({ bottom: 6 })
                .alignSelf(ItemAlign.Start)

              Text(doctor.goodAt)
                .fontSize(13)
                .fontColor('#555555')
                .lineHeight(20)
                .width('100%')
                .textAlign(TextAlign.Start)
            }
            .width('100%')
            .alignItems(HorizontalAlign.Start)
          }

          // 职称和性别
          Row() {
            Text('职称：')
              .fontSize(12)
              .fontColor('#999999')
            Text(doctor.position || '医师')
              .fontSize(12)
              .fontColor('#555555')
              .margin({ right: 16 })

            Text('性别：')
              .fontSize(12)
              .fontColor('#999999')
            Text(this.getGenderText(doctor.gender))
              .fontSize(12)
              .fontColor('#555555')
          }
          .width('100%')
          .margin({ top: (doctor.introduction || doctor.goodAt) ? 10 : 0 })
          .alignSelf(ItemAlign.Start)

          // 收起按钮
          Row() {
            Text('点击收起')
              .fontSize(11)
              .fontColor('#BBBBBB')
            Text('▲')
              .fontSize(10)
              .fontColor('#BBBBBB')
              .margin({ left: 4 })
          }
          .width('100%')
          .justifyContent(FlexAlign.Center)
          .margin({ top: 12 })
          .onClick(() => {
            this.toggleExpand(doctor.doctorId);
          })
        }
        .width('100%')
        .padding({ left: 16, right: 16, bottom: 16 })
        .backgroundColor('#FFFFFF')
        .borderRadius({ bottomLeft: 12, bottomRight: 12 })
      }
    }
    .width('100%')
    .backgroundColor('transparent')
    .margin({ bottom: 12 })
    .borderRadius(12)
    .clip(true)
  }
}
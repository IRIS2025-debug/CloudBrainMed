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

  build() {
    Column() {
      // ===== 顶部导航 =====
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

      // ===== 内容 =====
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
    Row() {
      // ===== 头像 =====
      Column() {
        Text(this.getInitial(doctor.name))
          .fontSize(22)
          .fontWeight(FontWeight.Medium)
          .fontColor('#FFFFFF')
      }
      .width(52)
      .height(52)
      .borderRadius(26)
      .backgroundColor('#B8C5D6')  // 统一灰蓝色，不刺眼
      .justifyContent(FlexAlign.Center)
      .alignItems(HorizontalAlign.Center)

      // ===== 信息 =====
      Column() {
        // 姓名
        Text(doctor.name)
          .fontSize(16)
          .fontWeight(FontWeight.Medium)
          .fontColor('#1A1A1A')

        // 职称 + 性别（灰色调，统一和谐）
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

        // 擅长（灰色）
        if (doctor.goodAt && doctor.goodAt.length > 0) {
          Text(doctor.goodAt)
            .fontSize(12)
            .fontColor('#AAAAAA')
            .maxLines(1)
            .textOverflow({ overflow: TextOverflow.Ellipsis })
            .margin({ top: 4 })
            .width('100%')
        }

        // 简介
        if (doctor.introduction && doctor.introduction.length > 0) {
          Text(doctor.introduction)
            .fontSize(12)
            .fontColor('#AAAAAA')
            .maxLines(1)
            .textOverflow({ overflow: TextOverflow.Ellipsis })
            .margin({ top: 2 })
            .width('100%')
        }
      }
      .alignItems(HorizontalAlign.Start)
      .layoutWeight(1)
      .margin({ left: 14 })

      // ===== 预约按钮 =====
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
            patientId: this.patientId
          }
        });
      })
    }
    .width('100%')
    .padding(16)
    .backgroundColor('#FFFFFF')
    .borderRadius(12)
    .margin({ bottom: 12 })
  }
}
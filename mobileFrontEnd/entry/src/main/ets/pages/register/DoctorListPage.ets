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
    if (gender === 1) {
      return '男';
    } else if (gender === 2) {
      return '女';
    }
    return '未知';
  }

  private getGenderIcon(gender: number): string {
    if (gender === 1) {
      return '👨';
    } else if (gender === 2) {
      return '👩';
    }
    return '👤';
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
        Text(this.deptName)
          .fontSize(20)
          .fontWeight(FontWeight.Bold)
          .layoutWeight(1)
          .textAlign(TextAlign.Center)
        Blank().width(24)
      }
      .width('100%')
      .padding(16)
      .backgroundColor('#FFFFFF')

      if (this.isLoading) {
        Column() {
          LoadingProgress()
            .width(60)
            .height(60)
            .color('#007AFF')
          Text('加载中...')
            .fontSize(14)
            .fontColor('#999')
            .margin({ top: 16 })
        }
        .width('100%')
        .height('100%')
        .justifyContent(FlexAlign.Center)
      } else if (this.doctors.length === 0) {
        Column() {
          Text('📋')
            .fontSize(60)
            .margin({ bottom: 16 })
          Text('暂无医生')
            .fontSize(16)
            .fontColor('#999999')
          Text('请选择其他科室')
            .fontSize(12)
            .fontColor('#CCCCCC')
            .margin({ top: 8 })
        }
        .width('100%')
        .height('100%')
        .justifyContent(FlexAlign.Center)
      } else {
        Scroll() {
          // 关键修改：设置 Column 宽度100% 和 alignItems 为 Start
          Column() {
            ForEach(this.doctors, (doctor: Doctor) => {
              // 医生卡片 - 宽度100%，内容左对齐
              Column() {
                // 医生信息行
                Row() {
                  // 头像区域
                  Column() {
                    Text(doctor.avatar || this.getGenderIcon(doctor.gender))
                      .fontSize(40)
                      .backgroundColor('#E3F2FD')
                      .width(56)
                      .height(56)
                      .borderRadius(28)
                      .textAlign(TextAlign.Center)
                      .lineHeight(56)
                  }

                  // 医生详细信息区域 - 左对齐
                  Column() {
                    // 姓名和职称行
                    Row() {
                      Text(doctor.name)
                        .fontSize(16)
                        .fontWeight(FontWeight.Bold)
                        .fontColor('#333333')
                      Text(doctor.position)
                        .fontSize(11)
                        .fontColor('#FF6B6B')
                        .backgroundColor('#FFF0F0')
                        .padding({ left: 6, right: 6, top: 2, bottom: 2 })
                        .borderRadius(4)
                        .margin({ left: 8 })
                    }
                    .width('100%')
                    .margin({ bottom: 8 })

                    // 性别行
                    Row() {
                      Text('性别：')
                        .fontSize(12)
                        .fontColor('#999999')
                      Text(this.getGenderText(doctor.gender))
                        .fontSize(12)
                        .fontColor('#666666')
                    }
                    .width('100%')
                    .margin({ bottom: 6 })

                    // 擅长领域
                    if (doctor.goodAt && doctor.goodAt.length > 0) {
                      Row() {
                        Text('擅长：')
                          .fontSize(12)
                          .fontColor('#999999')
                        Text(doctor.goodAt)
                          .fontSize(12)
                          .fontColor('#666666')
                          .layoutWeight(1)
                          .maxLines(2)
                          .textOverflow({ overflow: TextOverflow.Ellipsis })
                      }
                      .width('100%')
                      .margin({ bottom: 6 })
                    }

                    // 简介
                    if (doctor.introduction && doctor.introduction.length > 0) {
                      Row() {
                        Text('简介：')
                          .fontSize(12)
                          .fontColor('#999999')
                        Text(doctor.introduction)
                          .fontSize(12)
                          .fontColor('#666666')
                          .layoutWeight(1)
                          .maxLines(2)
                          .textOverflow({ overflow: TextOverflow.Ellipsis })
                      }
                      .width('100%')
                    }
                  }
                  .alignItems(HorizontalAlign.Start)
                  .layoutWeight(1)
                  .margin({ left: 12 })
                }
                .width('100%')
                .padding(16)
                .alignItems(VerticalAlign.Top)

                // 分割线
                Divider()
                  .color('#F0F0F0')
                  .margin({ left: 16, right: 16 })

                // 预约按钮
                Row() {
                  Text('立即预约')
                    .fontSize(14)
                    .fontColor('#FFFFFF')
                    .fontWeight(FontWeight.Medium)
                }
                .width('100%')
                .height(40)
                .backgroundColor('#007AFF')
                .borderRadius(20)
                .justifyContent(FlexAlign.Center)
                .margin({ top: 12, bottom: 16, left: 16, right: 16 })
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
              .backgroundColor('#FFFFFF')
              .borderRadius(16)
              .margin({ bottom: 12 })
              .alignItems(HorizontalAlign.Start)  // 关键：卡片内容左对齐
            })
          }
          .width('100%')  // 关键：设置宽度100%
          .alignItems(HorizontalAlign.Start)  // 关键：Column内容左对齐
          .padding(16)
        }
        .scrollable(ScrollDirection.Vertical)
        .scrollBar(BarState.Off)
        .layoutWeight(1)
        .align(Alignment.TopStart)  // 关键：Scroll内容从左上角开始
      }
    }
    .width('100%')
    .height('100%')
    .backgroundColor('#F5F5F5')
  }
}
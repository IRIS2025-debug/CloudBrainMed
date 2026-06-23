// pages/register/DeptListPage.ets
import router from '@ohos.router';
import { HttpUtil } from '../common/HttpUtil';
import promptAction from '@ohos.promptAction';
import { APIConfig } from '../common/APIConfig';

interface Dept {
  deptId: string;
  deptName: string;
  roomId: string;
  maxCapacity: number;
  freeCapacity: number;
  status: number;
  createTime: string;
}

interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
}

@Entry
@Component
export struct DeptListPage {
  @State depts: Dept[] = [];
  @State isLoading: boolean = true;
  private patientId: string = '';
  private baseUrl: string = APIConfig.PATIENT_SERVICE_URL;

  aboutToAppear() {
    const params = router.getParams() as Record<string, string>;
    this.patientId = params?.['patientId'] || '';
    this.loadDepts();
  }

  async loadDepts() {
    this.isLoading = true;
    const context = getContext(this);
    try {
      const result = await HttpUtil.get<ApiResponse<Dept[]>>(
        this.baseUrl + '/register/depts',
        context
      );
      if (result.code === 200) {
        this.depts = result.data;
      } else {
        promptAction.showToast({
          message: result.message || '加载科室失败',
          duration: 2000
        });
      }
    } catch (error) {
      console.error('加载科室失败：' + JSON.stringify(error));
      promptAction.showToast({
        message: '网络请求失败，请稍后重试',
        duration: 2000
      });
    } finally {
      this.isLoading = false;
    }
  }

  private getDeptIcon(deptName: string): string {
    const iconMap: Record<string, string> = {
      '内科': '🏥',
      '外科': '🔪',
      '放射科': '📷',
      '儿科': '👶',
      '妇产科': '👩',
      '眼科': '👁️',
      '口腔科': '🦷',
      '皮肤科': '🧴',
      '中医科': '🌿',
      '急诊科': '🚑'
    };
    return iconMap[deptName] || '🏥';
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
        Text('选择科室')
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
      } else {
        Scroll() {
          // 关键：设置Column宽度100%和左对齐
          Column() {
            ForEach(this.depts, (dept: Dept) => {
              // 科室卡片 - 一行一行展示，左对齐
              Column() {
                Row() {
                  // 科室图标
                  Text(this.getDeptIcon(dept.deptName))
                    .fontSize(32)
                    .margin({ right: 16 })

                  // 科室信息区域 - 左对齐
                  Column() {
                    Text(dept.deptName)
                      .fontSize(16)
                      .fontWeight(FontWeight.Medium)
                      .fontColor('#333333')
                      .margin({ bottom: 6 })

                    Row() {
                      Text('剩余号源')
                        .fontSize(12)
                        .fontColor('#999999')
                      Text(String(dept.freeCapacity || 0))
                        .fontSize(12)
                        .fontColor('#007AFF')
                        .fontWeight(FontWeight.Medium)
                        .margin({ left: 4 })
                    }
                  }
                  .alignItems(HorizontalAlign.Start)
                  .layoutWeight(1)

                  // 状态标签
                  if ((dept.freeCapacity || 0) > 0) {
                    Text('可预约')
                      .fontSize(11)
                      .fontColor('#52C41A')
                      .backgroundColor('#F6FFED')
                      .padding({ left: 8, right: 8, top: 4, bottom: 4 })
                      .borderRadius(12)
                  } else {
                    Text('已满')
                      .fontSize(11)
                      .fontColor('#FF4D4F')
                      .backgroundColor('#FFF2F0')
                      .padding({ left: 8, right: 8, top: 4, bottom: 4 })
                      .borderRadius(12)
                  }

                  // 箭头
                  Text('›')
                    .fontSize(20)
                    .fontColor('#CCCCCC')
                    .margin({ left: 12 })
                }
                .width('100%')
                .padding(16)
                .onClick(() => {
                  if ((dept.freeCapacity || 0) > 0) {
                    router.pushUrl({
                      url: 'pages/register/DoctorListPage',
                      params: {
                        deptId: dept.deptId,
                        deptName: dept.deptName,
                        patientId: this.patientId
                      }
                    });
                  } else {
                    promptAction.showToast({
                      message: '该科室号源已满',
                      duration: 2000
                    });
                  }
                })
              }
              .width('100%')
              .backgroundColor('#FFFFFF')
              .borderRadius(12)
              .margin({ bottom: 12 })
              .alignItems(HorizontalAlign.Start)  // 卡片内容左对齐
            })
          }
          .width('100%')  // 宽度占满
          .alignItems(HorizontalAlign.Start)  // 内容左对齐
          .padding(16)
        }
        .scrollable(ScrollDirection.Vertical)
        .scrollBar(BarState.Off)
        .layoutWeight(1)
        .align(Alignment.TopStart)  // Scroll内容从左上角开始
      }
    }
    .width('100%')
    .height('100%')
    .backgroundColor('#F5F5F5')
  }
}
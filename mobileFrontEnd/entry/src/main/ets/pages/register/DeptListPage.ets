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
          router.back();
        })

        Text('选择科室')
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

      // 内容区域
      if (this.isLoading) {
        Column() {
          LoadingProgress()
            .width(40)
            .height(40)
            .color('#4A90D9')
          Text('加载中...')
            .fontSize(14)
            .fontColor('#8E8E93')
            .margin({ top: 12 })
        }
        .width('100%')
        .height('100%')
        .justifyContent(FlexAlign.Center)
        .backgroundColor('#F8F9FA')
      } else {
        List({ space: 12 }) {
          ForEach(this.depts, (dept: Dept) => {
            ListItem() {
              this.DeptItem(dept)
            }
          }, (dept: Dept) => dept.deptId)
        }
        .width('100%')
        .padding({ left: 16, right: 16, top: 16, bottom: 16 })
        .layoutWeight(1)
        .backgroundColor('#F8F9FA')
      }
    }
    .width('100%')
    .height('100%')
    .backgroundColor('#FFFFFF')
  }

  @Builder
  DeptItem(dept: Dept) {
    Row() {
      // 科室名称
      Text(dept.deptName)
        .fontSize(16)
        .fontWeight(FontWeight.Medium)
        .fontColor('#1A1A2E')
        .layoutWeight(1)

      // 右侧箭头
      Text('›')
        .fontSize(20)
        .fontColor('#C7C7CC')
        .fontWeight(FontWeight.Normal)
    }
    .width('100%')
    .padding({ left: 18, right: 18, top: 18, bottom: 18 })
    .backgroundColor('#FFFFFF')
    .borderRadius(14)
    .shadow({
      radius: 6,
      color: 'rgba(0,0,0,0.04)',
      offsetY: 2
    })
    .onClick(() => {
      router.pushUrl({
        url: 'pages/register/DoctorListPage',
        params: {
          deptId: dept.deptId,
          deptName: dept.deptName,
          patientId: this.patientId
        }
      });
    })
  }
}
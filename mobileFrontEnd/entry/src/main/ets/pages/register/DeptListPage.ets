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
      '脑科': '🧠',
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

  @Builder
  DeptItem(dept: Dept) {
    Row() {
      Text(this.getDeptIcon(dept.deptName))
        .fontSize(36)
        .margin({ right: 18 })

      Text(dept.deptName)
        .fontSize(19)
        .fontWeight(FontWeight.Normal)
        .fontColor('#222222')
        .layoutWeight(1)

      Text('›')
        .fontSize(28)
        .fontColor('#CCCCCC')
    }
    .width('100%')
    .padding({ left: 20, right: 20, top: 22, bottom: 22 })
    .backgroundColor('#FFFFFF')
    .borderRadius(14)
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

  build() {
    Column() {
      // 顶部导航栏
      Row() {
        Text('←')
          .fontSize(26)
          .fontColor('#000000')
          .onClick(() => {
            router.back();
          })
          .width(40)

        Text('选择科室')
          .fontSize(22)
          .fontWeight(FontWeight.Medium)
          .fontColor('#000000')
          .layoutWeight(1)
          .textAlign(TextAlign.Center)

        Blank().width(40)
      }
      .width('100%')
      .padding({ left: 12, right: 12, top: 10, bottom: 10 })
      .backgroundColor('#FFFFFF')

      // 内容区域
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
        .layoutWeight(1)
        .justifyContent(FlexAlign.Center)
      } else {
        List({ space: 14 }) {
          ForEach(this.depts, (dept: Dept) => {
            ListItem() {
              this.DeptItem(dept)
            }
          }, (dept: Dept) => dept.deptId)
        }
        .width('100%')
        .padding({ left: 16, right: 16, top: 20, bottom: 20 })
        .layoutWeight(1)
        .backgroundColor('#F8F8F8')
        .divider({
          strokeWidth: 0,
          color: '#F0F0F0'
        })
      }
    }
    .width('100%')
    .height('100%')
    .backgroundColor('#F8F8F8')
  }
}
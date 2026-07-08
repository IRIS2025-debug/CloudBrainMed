// pages/profile/RegisterHistoryPage.ets
import router from '@ohos.router';
import http from '@ohos.net.http';
import promptAction from '@ohos.promptAction';
import preferences from '@ohos.data.preferences';
import { BusinessError } from '@ohos.base';
import { APIConfig } from '../common/APIConfig';

// 挂号记录实体类 - 与后端保持一致
interface Registration {
  registerId: string;
  patientId: string;
  doctorId: string;
  name: string;
  gender: number;      // 1-男, 2-女 （与后端数据库一致）
  birthday: string;
  chiefComplaint: string;
  department: string;
  consultRoom: string;
  visitDate: string;
  consultTime: string;
  price: number;
  payStatus: string;   // PAID, UNPAID, REFUNDED
  createTime: string;
}

// 统一响应格式
interface ApiResponse<T> {
  code: number;
  msg: string;
  data: T;
}

@Entry
@Component
struct RegisterHistoryPage {
  @State patientId: string = '';
  @State registerList: Registration[] = [];
  @State isLoading: boolean = false;
  @State isRefreshing: boolean = false;

  // 使用与 PersonalCenter 相同的 baseUrl
  private baseUrl: string = APIConfig.PATIENT_SERVICE_URL;

  // 性别映射常量：与后端保持一致 1=男, 2=女
  private readonly GENDER_MAP: Record<number, string> = {
    1: '男',
    2: '女'
  };

  aboutToAppear() {
    // 获取路由参数
    const params = router.getParams() as Record<string, string>;
    if (params && params.patientId) {
      this.patientId = params.patientId;
      this.loadRegisterHistory();
    } else {
      promptAction.showToast({
        message: '参数错误，请重试',
        duration: 1500
      });
      setTimeout(() => {
        router.back();
      }, 1500);
    }
  }

  /**
   * 获取存储的token - 从 Preferences 读取（与 PersonalCenter 保持一致）
   */
  async getToken(): Promise<string> {
    try {
      const context = getContext(this);
      const prefs = await preferences.getPreferences(context, 'user_prefs');
      const token = await prefs.get('token', '') as string;
      console.info('从 Preferences 读取 Token:', token ? '已获取' : '为空');
      return token;
    } catch (e) {
      console.error('获取Token失败:', JSON.stringify(e));
      return '';
    }
  }

  /**
   * 加载挂号记录
   */
  async loadRegisterHistory(): Promise<void> {
    if (this.isLoading) return;

    this.isLoading = true;
    try {
      const token: string = await this.getToken();
      const result: ApiResponse<Registration[]> = await this.fetchRegisterHistory(token);

      if (result && result.code === 200) {
        this.registerList = result.data || [];
        // ✅ 在 Console 中执行以下命令查看
        console.info('this.registerList:', this.registerList);
        console.info('this.registerList 长度:', this.registerList.length);
        console.info('第一条数据:', JSON.stringify(this.registerList[0]));
      } else {
        promptAction.showToast({
          message: result?.msg || '加载失败',
          duration: 1500
        });
      }
    } catch (error) {
      const err = error as BusinessError;
      console.error('加载挂号记录失败:', err.message);
      promptAction.showToast({
        message: '网络请求失败，请重试',
        duration: 1500
      });
    } finally {
      this.isLoading = false;
      this.isRefreshing = false;
    }
  }

  /**
   * 调用后端接口
   * @param token 用户令牌
   * @returns Promise<ApiResponse<Registration[]>> 挂号记录列表
   */
  private fetchRegisterHistory(token: string): Promise<ApiResponse<Registration[]>> {
    return new Promise((resolve, reject) => {
      const httpRequest = http.createHttp();

      httpRequest.request(
        `${this.baseUrl}/profile/registers`,
        {
          method: http.RequestMethod.GET,
          header: {
            'Content-Type': 'application/json',
            'token': token
          },
          connectTimeout: 1000000,
          readTimeout: 1000000
        },
        (err: BusinessError, data: http.HttpResponse) => {
          console.info('=== 进入回调 ===');

          if (err) {
            console.error('请求失败:', JSON.stringify(err));
            reject(err);
          } else {
            // ✅ 在外部声明变量
            let response: ApiResponse<Registration[]> | null = null;

            try {
              console.info('原始响应:', data.result);
              response = JSON.parse(data.result as string);
              console.info('解析成功:', JSON.stringify(response));
            } catch (e) {
              console.error('解析异常:', JSON.stringify(e));
              reject(e);
              httpRequest.destroy();
              return;
            }

            // ✅ 在 try-catch 外部使用 response
            if (response) {
              resolve(response);
            } else {
              reject(new Error('响应数据为空'));
            }
          }
          httpRequest.destroy();
        }
      );
    });
  }

  /**
   * 下拉刷新处理
   */
  handleRefresh(): void {
    this.isRefreshing = true;
    this.loadRegisterHistory();
  }

  /**
   * 查看挂号详情
   */
  goToDetail(register: Registration): void {
    router.pushUrl({
      url: 'pages/profile/RegisterDetailPage',
      params: {
        registerId: register.registerId
      }
    }).catch((err: Error) => {
      console.error('跳转失败:', JSON.stringify(err));
      promptAction.showToast({
        message: '页面跳转失败',
        duration: 1500
      });
    });
  }

  /**
   * 获取状态显示文本
   */
  getPayStatusText(status: string): string {
    const statusMap: Record<string, string> = {
      'PAID': '已支付',
      'UNPAID': '待支付',
      'REFUNDED': '已退款'
    };
    return statusMap[status] || status;
  }

  /**
   * 获取状态颜色
   */
  getPayStatusColor(status: string): string {
    const colorMap: Record<string, string> = {
      'PAID': '#52c41a',
      'UNPAID': '#faad14',
      'REFUNDED': '#999'
    };
    return colorMap[status] || '#999';
  }

  /**
   * 格式化日期
   */
  formatDate(dateStr: string): string {
    if (!dateStr) return '';
    const date = new Date(dateStr);
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
  }

  /**
   * 获取性别文本
   * 1-男, 2-女
   */
  getGenderText(gender: number): string {
    return this.GENDER_MAP[gender] || '未知';
  }

  build() {
    Column() {
      // 顶部标题栏 - 与 PersonalCenter 风格一致
      Row() {
        Image($r('app.media.back'))
          .width(24)
          .height(24)
          .fillColor('#333333')
          .onClick(() => {
            router.back();
          })

        Text('挂号记录')
          .fontSize(18)
          .fontWeight(FontWeight.Medium)
          .fontColor('#1A1A1A')
          .layoutWeight(1)
          .textAlign(TextAlign.Center)

        // 占位，保持标题居中
        Blank()
          .width(24)
      }
      .width('100%')
      .height(56)
      .padding({ left: 16, right: 16 })
      .backgroundColor('#FFFFFF')
      .shadow({ radius: 4, color: '#00000010' })

      // 内容区域
      if (this.isLoading && !this.isRefreshing) {
        // 加载中
        Column() {
          LoadingProgress()
            .width(40)
            .height(40)
            .color('#007AFF')
          Text('加载中...')
            .fontSize(14)
            .fontColor('#999999')
            .margin({ top: 12 })
        }
        .width('100%')
        .layoutWeight(1)
        .justifyContent(FlexAlign.Center)
        .alignItems(HorizontalAlign.Center)
      } else if (this.registerList.length === 0) {
        // 空状态
        Column() {
          Text('📋')
            .fontSize(80)
            .opacity(0.4)
          Text('暂无挂号记录')
            .fontSize(16)
            .fontColor('#999999')
            .margin({ top: 16 })
          Text('快去预约挂号吧')
            .fontSize(14)
            .fontColor('#CCCCCC')
            .margin({ top: 8 })
        }
        .width('100%')
        .layoutWeight(1)
        .justifyContent(FlexAlign.Center)
        .alignItems(HorizontalAlign.Center)
      } else {
        // 使用 Refresh 组件实现下拉刷新
        // ✅ 正确的用法
        Refresh({
          refreshing: this.isRefreshing
        }) {
          this.buildListContent()  // ✅ 直接在闭包中调用
        }
        .onStateChange((state: RefreshStatus) => {
          if (state === RefreshStatus.Inactive) {
            // 刷新结束
          }
        })
        .onRefreshing(() => {
          this.handleRefresh();
        })
        .layoutWeight(1)
      }
    }
    .width('100%')
    .height('100%')
    .backgroundColor('#F5F5F5')
  }

  @Builder
  buildListContent() {
    List() {
      ForEach(this.registerList, (item: Registration, index: number) => {
        ListItem() {
          this.buildListItem(item)
        }
        .onClick(() => {
          this.goToDetail(item);
        })
      }, (item: Registration) => item.registerId)
    }
    .width('100%')
    .height('100%')
    .backgroundColor('#F5F5F5')
    .padding({ top: 12, left: 16, right: 16 })
    .scrollBar(BarState.Off)
  }

  @Builder
  buildListItem(item: Registration) {
    Column() {
      // 顶部：科室 + 状态
      Row() {
        Text(item.department || '未分类')
          .fontSize(16)
          .fontWeight(FontWeight.Medium)
          .fontColor('#1A1A1A')

        Blank()

        Text(this.getPayStatusText(item.payStatus))
          .fontSize(12)
          .fontColor(this.getPayStatusColor(item.payStatus))
          .padding({ left: 12, right: 12, top: 4, bottom: 4 })
          .borderRadius(12)
          .backgroundColor(this.getPayStatusColor(item.payStatus) + '20')
      }
      .width('100%')
      .margin({ bottom: 12 })

      // 患者信息：姓名 + 性别 + 出生日期
      Row() {
        Text(`患者：${item.name || '未知'}`)
          .fontSize(14)
          .fontColor('#666666')

        Blank()

        Text(this.getGenderText(item.gender))
          .fontSize(14)
          .fontColor('#666666')
          .margin({ right: 16 })

        Text(this.formatDate(item.birthday) || '--')
          .fontSize(14)
          .fontColor('#666666')
      }
      .width('100%')
      .margin({ bottom: 8 })

      // 就诊信息
      Row() {
        Text(`就诊时间：${item.visitDate || ''} ${item.consultTime || ''}`)
          .fontSize(13)
          .fontColor('#888888')

        Blank()

        Text(`诊室：${item.consultRoom || '未知'}`)
          .fontSize(13)
          .fontColor('#888888')
      }
      .width('100%')
      .margin({ bottom: 8 })

      // 主诉
      if (item.chiefComplaint) {
        Text(`主诉：${item.chiefComplaint}`)
          .fontSize(13)
          .fontColor('#888888')
          .width('100%')
          .margin({ bottom: 8 })
          .maxLines(2)
          .textOverflow({ overflow: TextOverflow.Ellipsis })
      }

      // 底部：费用 + 时间
      Row() {
        Text(`¥${item.price?.toFixed(2) || '0.00'}`)
          .fontSize(16)
          .fontWeight(FontWeight.Bold)
          .fontColor('#FF4D4F')

        Blank()

        Text(`预约时间：${this.formatDate(item.createTime)}`)
          .fontSize(12)
          .fontColor('#BBBBBB')
      }
      .width('100%')
      .margin({ top: 4 })
    }
    .width('100%')
    .padding({ left: 16, right: 16, top: 16, bottom: 16 })
    .backgroundColor('#FFFFFF')
    .borderRadius(12)
    .margin({ bottom: 12 })
    .shadow({ radius: 2, color: '#00000008' })
  }
}
// pages/Index.ets
import router from '@ohos.router';
import { HttpUtil } from '../pages/common/HttpUtil';
import { HomePage } from './HomePage';
import { ConsultationAssistantPage } from './aiConsult/ConsultationAssistantPage';
import { MedicalRecordsPage } from './medical/MedicalRecordsPage';
import { PersonalCenter } from './profile/PersonalCenter';

// 定义类型接口
interface TabItem {
  title: string;
  icon: string;
  selectedIcon: string;
}

@Entry
@Component
struct Index {
  @State currentTab: number = 0;
  @State patientId: string = '';
  @State userName: string = '';
  @State userPhone: string = '';
  @State userGender: number = 0; // 0:未知, 1:男, 2:女
  @State userGenderText: string = '未知';
  @State userAge: number = 0;
  @State isLoading: boolean = true;

  private pages: TabItem[] = [
    { title: '首页', icon: '📋', selectedIcon: '📋' },
    { title: '问诊助手', icon: '💬', selectedIcon: '💬' },
    { title: '就诊记录', icon: '📅', selectedIcon: '📅' },
    { title: '个人中心', icon: '👤', selectedIcon: '👤' }
  ];

  aboutToAppear() {
    const params = router.getParams() as Record<string, string | number>;
    console.log('Index接收参数: ' + JSON.stringify(params));

    if (params) {
      this.patientId = (params['patientId'] as string) || '';
      this.userName = (params['name'] as string) || '';
      this.userPhone = (params['phone'] as string) || '';
      this.userGender = (params['gender'] as number) || 0;
      this.userGenderText = (params['genderText'] as string) || '未知';
      this.userAge = (params['age'] as number) || 0;

      if (this.patientId) {
        console.log('患者ID: ' + this.patientId);
        console.log('患者姓名: ' + this.userName);
        console.log('患者性别: ' + this.userGenderText);
        console.log('患者年龄: ' + this.userAge);
      } else {
        console.error('未接收到患者ID');
        setTimeout(() => {
          router.replaceUrl({ url: 'pages/login/LoginPage' });
        }, 2000);
      }
    } else {
      console.error('未接收到参数');
      setTimeout(() => {
        router.replaceUrl({ url: 'pages/login/LoginPage' });
      }, 2000);
    }

    this.isLoading = false;
  }

  build() {
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
      Flex({ direction: FlexDirection.Column }) {
        // 内容区域 - 可滚动
        if (this.currentTab === 0) {
          HomePage({
            patientId: this.patientId,
            userName: this.userName,
            userPhone: this.userPhone,
            userGender: this.userGender,
            userGenderText: this.userGenderText,
            userAge: this.userAge
          })
        } else if (this.currentTab === 1) {
          ConsultationAssistantPage({
            patientId: this.patientId,
            userName: this.userName
          })
        } else if (this.currentTab === 2) {
          MedicalRecordsPage({
            patientId: this.patientId,
            userName: this.userName
          })
        } else {
          PersonalCenter({
            patientId: this.patientId,
            userName: this.userName,
            userPhone: this.userPhone
          })
        }

        // 底部导航栏 - 固定在底部
        Row() {
          ForEach(this.pages, (item: TabItem, index: number) => {
            Column() {
              Text(item.icon)
                .fontSize(24)
                .margin({ bottom: 4 })
              Text(item.title)
                .fontSize(12)
                .fontColor(this.currentTab === index ? '#007AFF' : '#8E8E93')
            }
            .width('25%')
            .height(60)
            .justifyContent(FlexAlign.Center)
            .alignItems(HorizontalAlign.Center)
            .onClick(() => {
              this.currentTab = index;
            })
          })
        }
        .width('100%')
        .height(60)
        .backgroundColor('#FFFFFF')
        .border({ width: { top: 0.5 }, color: '#E5E5E5' })
      }
      .width('100%')
      .height('100%')
      .backgroundColor('#F5F5F5')
    }
  }
}
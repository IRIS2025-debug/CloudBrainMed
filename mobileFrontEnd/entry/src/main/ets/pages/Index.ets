// pages/Index.ets
import router from '@ohos.router';
import { HomePage } from './HomePage';
import { ConsultationAssistantPage } from './aiConsult/ConsultationAssistantPage';
import { PersonalCenter } from './profile/PersonalCenter';
import { PatientManager } from './common/PatientManager';
import { RegisterHistory } from './history/RegisterHistory';

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
  @State userGender: number = 0;
  @State userGenderText: string = '未知';
  @State userAge: number = 0;
  @State isLoading: boolean = true;
  @State isLoggedIn: boolean = false;

  private pages: TabItem[] = [
    { title: '首页', icon: '🏠', selectedIcon: '🏠' },
    { title: '问诊助手', icon: '💬', selectedIcon: '💬' },
    { title: '就诊记录', icon: '📋', selectedIcon: '📋' },
    { title: '个人中心', icon: '👤', selectedIcon: '👤' }
  ];

  aboutToAppear(): void {
    console.info('Index 页面加载...');

    const patientInfo = PatientManager.getPatientInfo();

    if (patientInfo) {
      if (!PatientManager.isTokenValid()) {
        console.warn('Token 已过期');
        PatientManager.clearPatientInfo();
        setTimeout(() => {
          router.replaceUrl({ url: 'pages/login/LoginPage' });
        }, 500);
        return;
      }

      this.patientId = patientInfo.patientId;
      this.userName = patientInfo.name;
      this.userPhone = patientInfo.phone;
      this.userGender = patientInfo.gender;
      this.userGenderText = patientInfo.genderText;
      this.userAge = patientInfo.age;
      this.isLoggedIn = true;
      console.info('患者信息加载成功: ' + this.userName);
    } else {
      console.warn('未找到患者信息');
      this.isLoggedIn = false;
      setTimeout(() => {
        router.replaceUrl({ url: 'pages/login/LoginPage' });
      }, 500);
    }

    this.isLoading = false;
  }

  // 切换Tab时传递参数
  private getHomePage(): void {
    // 直接返回组件，参数通过属性传递
  }

  build() {
    if (this.isLoading) {
      Column() {
        LoadingProgress()
          .width(60)
          .height(60)
          .color('#2563EB')
        Text('加载中...')
          .fontSize(14)
          .fontColor('#999999')
          .margin({ top: 16 })
      }
      .width('100%')
      .height('100%')
      .justifyContent(FlexAlign.Center)
      .backgroundColor('#F5F7FA')
    } else {
      Flex({ direction: FlexDirection.Column }) {
        // 内容区域
        Stack() {
          // 首页
          if (this.currentTab === 0) {
            HomePage({
              patientId: this.patientId,
              userName: this.userName,
              userPhone: this.userPhone,
              userGender: this.userGender,
              userGenderText: this.userGenderText,
              userAge: this.userAge
            })
          }

          // 问诊助手
          if (this.currentTab === 1) {
            ConsultationAssistantPage({
              patientId: this.patientId,
              userName: this.userName
            })
          }

          // 就诊记录
          if (this.currentTab === 2) {
            RegisterHistory({
              patientId: this.patientId,
              userName: this.userName,
              userPhone: this.userPhone,
              userGender: this.userGender,
              userGenderText: this.userGenderText,
              userAge: this.userAge
            })
          }

          // 个人中心
          if (this.currentTab === 3) {
            PersonalCenter({
              patientId: this.patientId,
              userName: this.userName,
              userPhone: this.userPhone
            })
          }
        }
        .width('100%')
        .layoutWeight(1)

        // 底部导航栏
        Row() {
          ForEach(this.pages, (item: TabItem, index: number) => {
            Column() {
              Text(item.icon)
                .fontSize(24)
                .margin({ bottom: 2 })
              Text(item.title)
                .fontSize(11)
                .fontColor(this.currentTab === index ? '#2563EB' : '#8E8E93')
            }
            .width('25%')
            .height(56)
            .justifyContent(FlexAlign.Center)
            .alignItems(HorizontalAlign.Center)
            .onClick(() => {
              this.currentTab = index;
            })
          })
        }
        .width('100%')
        .height(56)
        .backgroundColor('#FFFFFF')
        .border({
          width: { top: 0.5 },
          color: { top: '#E5E5E5' }
        })
      }
      .width('100%')
      .height('100%')
      .backgroundColor('#F5F7FA')
    }
  }
}
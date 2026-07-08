// pages/profile/ProfileDetail.ets
import router from '@ohos.router';
import { APIConfig } from '../common/APIConfig';
import { PatientManager } from '../common/PatientManager';

interface PatientProfile {
  patientId: string;
  name: string;
  gender: string;
  age: number;
  phone: string;
  address: string;
  birthday: string;
  avatarUrl: string;
  idCard: string;
}

@Entry
@Component
export struct ProfileDetail {
  @State profileData: PatientProfile = {
    patientId: '',
    name: '',
    gender: '',
    age: 0,
    phone: '',
    address: '',
    birthday: '',
    avatarUrl: '',
    idCard: ''
  };

  private patientId: string = '';
  private readonly GENDER_MAP: Record<string, string> = { '2': '女', '1': '男' };

  aboutToAppear() {
    const params = router.getParams() as Record<string, Object>;
    if (params) {
      const dataStr = params['profileData'] as string;
      this.patientId = (params['patientId'] as string) || '';
      if (dataStr) {
        try {
          this.profileData = JSON.parse(dataStr);
        } catch (e) {
          console.error('解析数据失败:', e);
        }
      }
    }
  }

  private goBack(): void {
    router.back();
  }

  private genderToText(gender: string): string {
    return this.GENDER_MAP[gender] || gender || '--';
  }

  private getDisplayAge(): string {
    if (this.profileData.age && this.profileData.age > 0) {
      return `${this.profileData.age}岁`;
    }
    if (this.profileData.birthday) {
      const age = this.calculateAge(this.profileData.birthday);
      return age > 0 ? `${age}岁` : '--';
    }
    return '--';
  }

  private calculateAge(birthday: string): number {
    if (!birthday) return 0;
    try {
      const birthDate = new Date(birthday);
      const today = new Date();
      let age = today.getFullYear() - birthDate.getFullYear();
      const monthDiff = today.getMonth() - birthDate.getMonth();
      if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < birthDate.getDate())) {
        age--;
      }
      return age >= 0 ? age : 0;
    } catch (e) {
      return 0;
    }
  }

  private maskIdCard(idCard: string): string {
    if (!idCard || idCard.length < 10) return idCard || '--';
    return idCard.substring(0, 6) + '****' + idCard.substring(idCard.length - 4);
  }

  @Builder
  private DividerLine() {
    Divider()
      .color('#F0F0F0')
      .height(0.5)
      .width('100%')
  }

  build() {
    Column() {
      // 顶部导航栏
      Row() {
        Text('‹')
          .fontSize(28)
          .fontColor('#333333')
          .width(40)
          .height(40)
          .textAlign(TextAlign.Center)
          .lineHeight(40)
          .onClick(() => { this.goBack(); })

        Text('个人信息')
          .fontSize(18)
          .fontWeight(FontWeight.Medium)
          .fontColor('#333333')
          .layoutWeight(1)
          .textAlign(TextAlign.Center)

        Row().width(40).height(40)
      }
      .width('100%')
      .height(56)
      .padding({ left: 12, right: 12 })
      .backgroundColor('#FFFFFF')

      // 内容 - 贴顶显示，无头像
      Column() {
        // 信息卡片 - 直接贴顶
        Column() {
          Row() {
            Text('患者ID')
              .fontSize(14)
              .fontColor('#8C8C8C')
              .width(100)
            Text(this.profileData.patientId || '--')
              .fontSize(14)
              .fontColor('#1A1A1A')
              .layoutWeight(1)
              .textAlign(TextAlign.End)
          }
          .width('100%')
          .padding({ top: 10, bottom: 10 })

          this.DividerLine()

          Row() {
            Text('姓名')
              .fontSize(14)
              .fontColor('#8C8C8C')
              .width(100)
            Text(this.profileData.name || '--')
              .fontSize(14)
              .fontColor('#1A1A1A')
              .layoutWeight(1)
              .textAlign(TextAlign.End)
          }
          .width('100%')
          .padding({ top: 10, bottom: 10 })

          this.DividerLine()

          Row() {
            Text('性别')
              .fontSize(14)
              .fontColor('#8C8C8C')
              .width(100)
            Text(this.genderToText(this.profileData.gender) || '--')
              .fontSize(14)
              .fontColor('#1A1A1A')
              .layoutWeight(1)
              .textAlign(TextAlign.End)
          }
          .width('100%')
          .padding({ top: 10, bottom: 10 })

          this.DividerLine()

          Row() {
            Text('年龄')
              .fontSize(14)
              .fontColor('#8C8C8C')
              .width(100)
            Text(this.getDisplayAge())
              .fontSize(14)
              .fontColor('#1A1A1A')
              .layoutWeight(1)
              .textAlign(TextAlign.End)
          }
          .width('100%')
          .padding({ top: 10, bottom: 10 })

          this.DividerLine()

          Row() {
            Text('出生日期')
              .fontSize(14)
              .fontColor('#8C8C8C')
              .width(100)
            Text(this.profileData.birthday || '--')
              .fontSize(14)
              .fontColor('#1A1A1A')
              .layoutWeight(1)
              .textAlign(TextAlign.End)
          }
          .width('100%')
          .padding({ top: 10, bottom: 10 })

          this.DividerLine()

          Row() {
            Text('地址')
              .fontSize(14)
              .fontColor('#8C8C8C')
              .width(100)
            Text(this.profileData.address || '--')
              .fontSize(14)
              .fontColor('#1A1A1A')
              .layoutWeight(1)
              .textAlign(TextAlign.End)
          }
          .width('100%')
          .padding({ top: 10, bottom: 10 })

          this.DividerLine()

          Row() {
            Text('手机号')
              .fontSize(14)
              .fontColor('#8C8C8C')
              .width(100)
            Text(this.profileData.phone || '--')
              .fontSize(14)
              .fontColor('#1A1A1A')
              .layoutWeight(1)
              .textAlign(TextAlign.End)
          }
          .width('100%')
          .padding({ top: 10, bottom: 10 })

          this.DividerLine()

          Row() {
            Text('身份证号')
              .fontSize(14)
              .fontColor('#8C8C8C')
              .width(100)
            Text(this.maskIdCard(this.profileData.idCard || '--'))
              .fontSize(14)
              .fontColor('#1A1A1A')
              .layoutWeight(1)
              .textAlign(TextAlign.End)
          }
          .width('100%')
          .padding({ top: 10, bottom: 10 })
        }
        .width('100%')
        .padding(16)
        .backgroundColor('#FFFFFF')
        .borderRadius(12)
        .margin({ left: 16, right: 16, top: 12 })  // 只保留顶部小间距
      }
      .width('100%')
      .height('100%')
      .justifyContent(FlexAlign.Start)
      .backgroundColor('#F5F6FA')
    }
    .width('100%')
    .height('100%')
    .backgroundColor('#F5F6FA')
  }
}
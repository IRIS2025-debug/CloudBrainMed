// pages/profile/PersonalCenter.ets
import router from '@ohos.router';
import preferences from '@ohos.data.preferences';

interface MenuItem {
  icon: string;
  title: string;
  route: string;
}

@Entry
@Component
export struct PersonalCenter {
  @State patientId: string = '';
  @State userName: string = '';
  @State userPhone: string = '';
  @State userAvatar: string = '无';

  private menuItems: MenuItem[] = [
    { icon: '👨‍⚕️', title: '我的医生', route: 'pages/profile/MyDoctors' },
    { icon: '💳', title: '我的就诊卡', route: 'pages/profile/MyCards' },
    { icon: '🏥', title: '我的预约', route: 'pages/profile/MyAppointments' },
    { icon: '⚙️', title: '设置', route: 'pages/profile/Settings' }
  ];

  async aboutToAppear() {
    // 从路由参数获取
    const params = router.getParams() as Record<string, string>;
    if (params) {
      this.patientId = params['patientId'] || '';
      this.userName = params['userName'] || '用户';
      this.userPhone = params['userPhone'] || '';
    }

    // 如果路由参数没有手机号，从本地存储获取
    if (!this.userPhone) {
      await this.loadUserInfo();
    }

    console.log('个人中心 - 患者ID: ' + this.patientId + ', 用户名: ' + this.userName + ', 手机号: ' + this.userPhone);
  }

  // 从本地存储加载用户信息
  // 从本地存储加载用户信息
  async loadUserInfo() {
    try {
      const context = getContext(this);
      const prefs = await preferences.getPreferences(context, 'user_prefs');
      const phone = await prefs.get('userPhone', '') as string;
      const name = await prefs.get('userName', '张思') as string;

      if (phone) {
        this.userPhone = phone;
        // 如果用户名是默认值，尝试从存储获取
        if (this.userName === '张思') {
          this.userName = await prefs.get('userName', '张思') as string;
        }
      }

      // 如果还没有patientId，从存储获取
      if (!this.patientId) {
        this.patientId = await prefs.get('patientId', '') as string;
      }
    } catch (err) {
      console.error('加载用户信息失败: ' + JSON.stringify(err));
    }
  }

  // 格式化手机号（中间4位隐藏）
  formatPhone(phone: string): string {
    if (!phone || phone.length < 11) return phone || '未绑定手机号';
    return phone.substring(0, 3) + '****' + phone.substring(7);
  }

  build() {
    Column() {
      // 顶部标题栏
      this.TopBar()

      // 可滚动内容
      Scroll() {
        Column() {
          // 1. 个人信息卡片
          this.ProfileCard()

          // 2. 统计卡片
          this.StatCard()

          // 3. 功能菜单（列表形式）
          this.MenuCard()
        }
        .width('100%')
        .padding({ left: 16, right: 16, bottom: 20 })
      }
      .scrollable(ScrollDirection.Vertical)
      .scrollBar(BarState.Off)
      .layoutWeight(1)
      .backgroundColor('#F5F5F5')

    }
    .width('100%')
    .height('100%')
    .backgroundColor('#F5F5F5')
  }

  // 顶部标题栏
  @Builder
  TopBar() {
    Row() {
      Text('个人中心')
        .fontSize(20)
        .fontWeight(FontWeight.Bold)
        .fontColor('#333333')
      Blank()
      Text('⚙️')
        .fontSize(22)
        .onClick(() => {
          router.pushUrl({ url: 'pages/profile/Settings' });
        })
    }
    .width('100%')
    .height(56)
    .padding({ left: 16, right: 16 })
    .backgroundColor('#FFFFFF')
    .borderWidth({ bottom: 1 })
    .borderColor({ bottom: '#E8E8E8' })
  }

  // 1. 个人信息卡片
  @Builder
  ProfileCard() {
    Row() {
      // 头像（圆形，显示姓氏）
      Column() {
        Text(this.userAvatar)
          .fontSize(28)
          .fontColor('#FFFFFF')
          .fontWeight(FontWeight.Bold)
      }
      .width(60)
      .height(60)
      .borderRadius(30)
      .backgroundColor('#007AFF')
      .justifyContent(FlexAlign.Center)

      // 用户名 + 手机号
      Column() {
        Row() {
          Text(this.userName)
            .fontSize(20)
            .fontWeight(FontWeight.Bold)
            .fontColor('#333333')
          Text('编辑')
            .fontSize(13)
            .fontColor('#007AFF')
            .margin({ left: 12 })
            .padding({ left: 8, right: 8, top: 2, bottom: 2 })
            .border({ width: 1, color: '#007AFF', radius: 12 })
            .onClick(() => {
              router.pushUrl({ url: 'pages/profile/EditProfile', params: {
                patientId: this.patientId,
                userName: this.userName,
                userPhone: this.userPhone
              } });
            })
        }
        .alignItems(VerticalAlign.Center)
        .margin({ bottom: 4 })

        Row() {
          Text('📱')
            .fontSize(14)
            .margin({ right: 4 })
          Text(this.formatPhone(this.userPhone))
            .fontSize(14)
            .fontColor('#666666')
        }
      }
      .alignItems(HorizontalAlign.Start)
      .padding({ left: 16 })
      .layoutWeight(1)
    }
    .width('100%')
    .padding(16)
    .backgroundColor('#FFFFFF')
    .borderRadius(12)
    .margin({ top: 16 })
    .border({
      width: { top: 3 },
      color: { top: '#007AFF' }
    })
  }

  // 2. 统计卡片
  @Builder
  StatCard() {
    Row() {
      this.StatItem('我的预约', '12')
      this.StatDivider()
      this.StatItem('进行中', '3')
      this.StatDivider()
      this.StatItem('已完成', '9')
    }
    .width('100%')
    .padding({ top: 16, bottom: 16 })
    .backgroundColor('#FFFFFF')
    .borderRadius(12)
    .margin({ top: 12 })
    .border({
      width: { top: 3 },
      color: { top: '#FF6B6B' }
    })
    .justifyContent(FlexAlign.SpaceAround)
  }

  // 3. 功能菜单卡片（列表形式，一行一行展示）
  @Builder
  MenuCard() {
    Column() {
      // 功能服务标题
      Row() {
        Text('功能服务')
          .fontSize(16)
          .fontWeight(FontWeight.Medium)
          .fontColor('#333333')
      }
      .width('100%')
      .padding({ bottom: 8 })

      // 菜单列表 - 一行一行展示
      ForEach(this.menuItems, (item: MenuItem, index: number) => {
        Row() {
          Text(item.icon)
            .fontSize(22)
            .margin({ right: 14 })

          Text(item.title)
            .fontSize(16)
            .fontColor('#333333')
            .layoutWeight(1)

          Text('›')
            .fontSize(20)
            .fontColor('#CCCCCC')
        }
        .width('100%')
        .height(48)
        .padding({ left: 4, right: 4 })
        .onClick(() => {
          router.pushUrl({
            url: item.route,
            params: {
              patientId: this.patientId,
              userName: this.userName,
              userPhone: this.userPhone
            }
          });
        })

        // 菜单项之间的分隔线
        if (index < this.menuItems.length - 1) {
          Divider()
            .margin({ left: 40 })
            .color('#F0F0F0')
        }
      })
    }
    .width('100%')
    .padding({ top: 12, bottom: 8, left: 16, right: 16 })
    .backgroundColor('#FFFFFF')
    .borderRadius(12)
    .margin({ top: 12 })
    .border({
      width: { top: 3 },
      color: { top: '#4CAF50' }
    })
  }

  // 统计项
  @Builder
  StatItem(label: string, value: string) {
    Column() {
      Text(value)
        .fontSize(22)
        .fontWeight(FontWeight.Bold)
        .fontColor('#333333')
      Text(label)
        .fontSize(13)
        .fontColor('#999999')
        .margin({ top: 4 })
    }
    .alignItems(HorizontalAlign.Center)
  }

  // 统计分隔线
  @Builder
  StatDivider() {
    Divider()
      .vertical(true)
      .height(30)
      .color('#E8E8E8')
  }

  // // 底部导航栏
  // @Builder
  // BottomNav() {
  //   Row() {
  //     this.NavItem('🏠', '首页', false)
  //     this.NavItem('💬', '问诊助手', false)
  //     this.NavItem('📋', '就诊记录', false)
  //     this.NavItem('👤', '个人中心', true)
  //   }
  //   .width('100%')
  //   .height(60)
  //   .backgroundColor('#FFFFFF')
  //   .border({
  //     width: { top: 1 },
  //     color: { top: '#E8E8E8' }
  //   })
  //   .justifyContent(FlexAlign.SpaceAround)
  // }

  @Builder
  NavItem(icon: string, label: string, isActive: boolean) {
    Column() {
      Text(icon)
        .fontSize(24)
      Text(label)
        .fontSize(11)
        .fontColor(isActive ? '#007AFF' : '#999999')
        .margin({ top: 2 })
    }
    .alignItems(HorizontalAlign.Center)
    .onClick(() => {
      if (label === '首页') {
        router.pushUrl({ url: 'pages/HomePage' });
      } else if (label === '问诊助手') {
        router.pushUrl({ url: 'pages/consult/ConsultHome' });
      } else if (label === '就诊记录') {
        router.pushUrl({ url: 'pages/records/MedicalRecords' });
      }
    })
  }
}
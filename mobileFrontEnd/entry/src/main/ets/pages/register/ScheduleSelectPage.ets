// pages/register/ScheduleSelectPage.ets
import router from '@ohos.router';
import { HttpUtil } from '../common/HttpUtil';
import promptAction from '@ohos.promptAction';
import { APIConfig } from '../common/APIConfig';

interface DoctorSchedule {
  scheduleId: string;
  doctorId: string;
  doctorName: string;
  workDate: string;
  startTime: string;
  endTime: string;
  maxNum: number;
  remainNum: number;
  status: number;
  price: number;
  room: string;
}

interface Registration {
  registerId: string;
  patientId: string;
  doctorId: string;
  name: string;
  gender: number;
  birthday: string;
  chiefComplaint: string;
  department: string;
  consultRoom: string;
  visitDate: string;
  consultTime: string;
  price: number;
  payStatus: string;
  createTime: string;
}

interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
}

interface SubmitRegisterRequest {
  patientId: string;
  doctorId: string;
  scheduleId: string;
  chiefComplaint: string;
  visitDate: string;
  consultTime: string;
}

interface TimeSlot {
  scheduleId: string;
  time: string;
  startTime: string;
  endTime: string;
  room: string;
  remainNum: number;
  price: number;
}

interface PatientCheckData {
  isComplete: boolean;
  missingFields: string;
}

interface PatientCheckResponse {
  code: number;
  message: string;
  data: PatientCheckData;
}

interface GeneratedTypeLiteralInterface_1 {
  index: number;
}

// 日期数据项
interface DateItem {
  fullDate: string;
  displayDay: string;
  weekday: string;
  isAvailable: boolean;
  isToday: boolean;
  isSelected: boolean;
  month: string;
}

@Entry
@Component
export struct ScheduleSelectPage {
  @State schedules: DoctorSchedule[] = [];
  @State availableDateSet: Set<string> = new Set();
  @State dateItems: DateItem[] = [];
  @State selectedDate: string = '';
  @State selectedDateSlots: TimeSlot[] = [];
  @State selectedSlotId: string = '';
  @State selectedTime: string = '';
  @State chiefComplaint: string = '';
  @State isLoading: boolean = true;
  @State isSubmitting: boolean = false;
  private doctorId: string = '';
  private doctorName: string = '';
  private patientId: string = '';
  private baseUrl: string = APIConfig.PATIENT_SERVICE_URL;
  private scrollCtrl: Scroller = new Scroller();
  private totalDays: number = 90; // 生成90天，足够翻
  private today: Date = new Date();

  aboutToAppear(): void {
    const params = router.getParams() as Record<string, string>;
    this.doctorId = params?.['doctorId'] || '';
    this.doctorName = params?.['doctorName'] || '';
    this.patientId = params?.['patientId'] || '';
    this.loadSchedules();
  }

  async loadSchedules(): Promise<void> {
    this.isLoading = true;
    const context = getContext(this);
    try {
      const result = await HttpUtil.get<ApiResponse<DoctorSchedule[]>>(
        this.baseUrl + '/register/schedules/' + this.doctorId,
        context
      );
      if (result.code === 200) {
        const allSchedules: DoctorSchedule[] = result.data;
        const availableSchedules: DoctorSchedule[] = [];
        const dateSet: Set<string> = new Set();

        for (let i = 0; i < allSchedules.length; i++) {
          if (allSchedules[i].remainNum > 0 && allSchedules[i].status === 1) {
            availableSchedules.push(allSchedules[i]);
            dateSet.add(allSchedules[i].workDate);
          }
        }

        this.schedules = availableSchedules;
        this.availableDateSet = dateSet;

        this.buildDateItems();

        const firstAvailable = this.dateItems.find(item => item.isAvailable);
        if (firstAvailable) {
          this.selectedDate = firstAvailable.fullDate;
          this.updateSlotsForDate(this.selectedDate);
        }
      } else {
        promptAction.showToast({
          message: result.message || '加载排班失败',
          duration: 2000
        });
      }
    } catch (error) {
      console.error('加载排班失败：' + JSON.stringify(error));
      promptAction.showToast({
        message: '网络请求失败，请稍后重试',
        duration: 2000
      });
    } finally {
      this.isLoading = false;
    }
  }

  // ===== 构建日期列表（从今天开始往后90天） =====
  private buildDateItems(): void {
    const items: DateItem[] = [];
    const today = new Date();
    const currentYear = today.getFullYear();
    const currentMonth = today.getMonth();

    for (let i = 0; i < this.totalDays; i++) {
      const date = new Date(currentYear, currentMonth, today.getDate() + i);
      const year = date.getFullYear();
      const month = String(date.getMonth() + 1).padStart(2, '0');
      const day = String(date.getDate()).padStart(2, '0');
      const fullDate = year + '-' + month + '-' + day;

      const todayStr = today.getFullYear() + '-' +
      String(today.getMonth() + 1).padStart(2, '0') + '-' +
      String(today.getDate()).padStart(2, '0');

      const isToday = fullDate === todayStr;
      const isAvailable = this.availableDateSet.has(fullDate);

      const weekdays = ['日', '一', '二', '三', '四', '五', '六'];
      const weekday = weekdays[date.getDay()];

      // 每月第一天显示月份
      const monthDisplay = (i === 0 || date.getDate() === 1) ? (date.getMonth() + 1) + '月' : '';

      items.push({
        fullDate: fullDate,
        displayDay: String(date.getDate()),
        weekday: '周' + weekday,
        isAvailable: isAvailable,
        isToday: isToday,
        isSelected: false,
        month: monthDisplay
      });
    }

    this.dateItems = items;
  }

  // ===== 加载更多日期（滚动到底部时触发） =====
  private loadMoreDates(): void {
    const lastItem = this.dateItems[this.dateItems.length - 1];
    if (!lastItem) return;

    // 从最后一天的后一天开始追加
    const lastDate = new Date(lastItem.fullDate);
    const startOffset = 1;

    for (let i = startOffset; i <= 10; i++) {
      const date = new Date(lastDate);
      date.setDate(date.getDate() + i);
      const year = date.getFullYear();
      const month = String(date.getMonth() + 1).padStart(2, '0');
      const day = String(date.getDate()).padStart(2, '0');
      const fullDate = year + '-' + month + '-' + day;

      const today = new Date();
      const todayStr = today.getFullYear() + '-' +
      String(today.getMonth() + 1).padStart(2, '0') + '-' +
      String(today.getDate()).padStart(2, '0');

      const isToday = fullDate === todayStr;
      const isAvailable = this.availableDateSet.has(fullDate);

      const weekdays = ['日', '一', '二', '三', '四', '五', '六'];
      const weekday = weekdays[date.getDay()];

      const monthDisplay = (date.getDate() === 1) ? (date.getMonth() + 1) + '月' : '';

      this.dateItems.push({
        fullDate: fullDate,
        displayDay: String(date.getDate()),
        weekday: '周' + weekday,
        isAvailable: isAvailable,
        isToday: isToday,
        isSelected: false,
        month: monthDisplay
      });
    }
  }

  private updateSlotsForDate(date: string): void {
    const slots: TimeSlot[] = [];
    for (let i = 0; i < this.schedules.length; i++) {
      const s = this.schedules[i];
      if (s.workDate === date) {
        slots.push({
          scheduleId: s.scheduleId,
          time: s.startTime + '-' + s.endTime,
          startTime: s.startTime,
          endTime: s.endTime,
          room: s.room || '待定',
          remainNum: s.remainNum,
          price: s.price
        });
      }
    }
    slots.sort((a: TimeSlot, b: TimeSlot) => {
      return a.startTime.localeCompare(b.startTime);
    });
    this.selectedDateSlots = slots;
    this.selectedSlotId = '';
    this.selectedTime = '';

    this.dateItems.forEach(item => {
      item.isSelected = item.fullDate === date;
    });
  }

  private selectDate(date: string): void {
    if (!this.availableDateSet.has(date)) {
      return;
    }
    this.selectedDate = date;
    this.updateSlotsForDate(date);
  }

  private selectTimeSlot(slot: TimeSlot): void {
    this.selectedSlotId = slot.scheduleId;
    this.selectedTime = slot.time;
  }

  private formatDateDisplay(dateStr: string): string {
    if (!dateStr) return '';
    const parts = dateStr.split('-');
    if (parts.length === 3) {
      return parseInt(parts[1]) + '月' + parseInt(parts[2]) + '日';
    }
    return dateStr;
  }

  private getSelectedPrice(): number {
    for (let i = 0; i < this.selectedDateSlots.length; i++) {
      if (this.selectedDateSlots[i].scheduleId === this.selectedSlotId) {
        return this.selectedDateSlots[i].price;
      }
    }
    return 0;
  }

  private getSelectedRoom(): string {
    for (let i = 0; i < this.selectedDateSlots.length; i++) {
      if (this.selectedDateSlots[i].scheduleId === this.selectedSlotId) {
        return this.selectedDateSlots[i].room;
      }
    }
    return '';
  }

  private async checkPatientInfo(): Promise<boolean> {
    const context = getContext(this);
    try {
      const result = await HttpUtil.get<PatientCheckResponse>(
        this.baseUrl + '/register/check-patient/' + this.patientId,
        context
      );
      if (result.code === 200) {
        return result.data.isComplete;
      } else {
        return false;
      }
    } catch (error) {
      console.error('检查患者信息失败：' + JSON.stringify(error));
      return false;
    }
  }

  private showIncompleteDialog(): void {
    promptAction.showDialog({
      title: '提示',
      message: '您的个人信息不完善，请先完善个人信息后再预约挂号',
      buttons: [
        {
          text: '去完善',
          color: '#007AFF'
        },
        {
          text: '取消',
          color: '#999999'
        }
      ]
    }).then((result: GeneratedTypeLiteralInterface_1) => {
      if (result.index === 0) {
        router.pushUrl({
          url: 'pages/profile/PersonalCenter',
          params: {
            patientId: this.patientId
          }
        });
      }
    }).catch(() => {
      // 忽略错误
    });
  }

  async submitRegister(): Promise<void> {
    if (!this.selectedSlotId) {
      promptAction.showToast({
        message: '请选择就诊时间',
        duration: 2000
      });
      return;
    }
    if (!this.chiefComplaint.trim()) {
      promptAction.showToast({
        message: '请填写主诉',
        duration: 2000
      });
      return;
    }

    this.isSubmitting = true;
    try {
      const isComplete = await this.checkPatientInfo();

      if (!isComplete) {
        this.isSubmitting = false;
        this.showIncompleteDialog();
        return;
      }

      const now = new Date();
      const year = now.getFullYear();
      const month = String(now.getMonth() + 1).padStart(2, '0');
      const day = String(now.getDate()).padStart(2, '0');
      const visitDate = year + '-' + month + '-' + day;

      const requestData: SubmitRegisterRequest = {
        patientId: this.patientId,
        doctorId: this.doctorId,
        scheduleId: this.selectedSlotId,
        chiefComplaint: this.chiefComplaint,
        visitDate: visitDate,
        consultTime: this.selectedTime
      };

      const context = getContext(this);

      const result = await HttpUtil.post<ApiResponse<Registration>>(
        this.baseUrl + '/register/submit',
        requestData,
        context
      );

      if (result.code === 200) {
        promptAction.showToast({
          message: '预约成功，请前往支付',
          duration: 2000
        });
        setTimeout((): void => {
          router.pushUrl({
            url: 'pages/register/OrderSuccessPage',
            params: {
              registerId: result.data.registerId
            }
          });
        }, 1500);
      } else {
        if (result.message && result.message.indexOf('患者信息不完整') !== -1) {
          this.showIncompleteDialog();
        } else {
          promptAction.showToast({
            message: result.message || '预约失败',
            duration: 2000
          });
        }
      }
    } catch (error) {
      console.error('提交挂号失败：' + JSON.stringify(error));
      const errorStr: string = JSON.stringify(error);
      if (errorStr.indexOf('患者信息不完整') !== -1) {
        this.showIncompleteDialog();
      } else {
        promptAction.showToast({
          message: '网络请求失败，请稍后重试',
          duration: 2000
        });
      }
    } finally {
      this.isSubmitting = false;
    }
  }

  @Builder
  DateItemBuilder(item: DateItem) {
    Column() {
      if (item.month) {
        Text(item.month)
          .fontSize(11)
          .fontColor('#999999')
          .margin({ bottom: 4 })
      } else {
        Text('')
          .height(18)
          .margin({ bottom: 4 })
      }

      Text(item.displayDay)
        .fontSize(18)
        .fontWeight(item.isSelected ? FontWeight.Bold : FontWeight.Normal)
        .fontColor(item.isAvailable ? (item.isSelected ? '#FFFFFF' : '#1A1A1A') : '#CCCCCC')
        .width(40)
        .height(40)
        .borderRadius(20)
        .backgroundColor(item.isSelected ? '#007AFF' : 'transparent')
        .textAlign(TextAlign.Center)
        .lineHeight(40)

      Text(item.weekday)
        .fontSize(11)
        .fontColor(item.isAvailable ? (item.isSelected ? '#007AFF' : '#888888') : '#DDDDDD')
        .margin({ top: 4 })
    }
    .width(50)
    .padding({ top: 4, bottom: 8 })
    .borderRadius(12)
    .backgroundColor(item.isSelected ? '#E6F2FF' : 'transparent')
    .onClick(() => {
      if (item.isAvailable) {
        this.selectDate(item.fullDate);
      }
    })
    .opacity(item.isAvailable ? 1.0 : 0.5)
  }

  build() {
    Column() {
      Row() {
        Text('‹')
          .fontSize(28)
          .fontColor('#333333')
          .width(44)
          .height(44)
          .textAlign(TextAlign.Center)
          .lineHeight(44)
          .onClick((): void => {
            router.back();
          })

        Text('选择就诊时间')
          .fontSize(18)
          .fontWeight(FontWeight.Medium)
          .fontColor('#1A1A1A')
          .layoutWeight(1)
          .textAlign(TextAlign.Center)

        Row().width(44).height(44)
      }
      .width('100%')
      .height(56)
      .padding({ left: 8, right: 8 })
      .backgroundColor('#FFFFFF')

      if (this.isLoading) {
        Column() {
          LoadingProgress()
            .width(36)
            .height(36)
            .color('#007AFF')
          Text('加载中...')
            .fontSize(14)
            .fontColor('#999')
            .margin({ top: 12 })
        }
        .width('100%')
        .height('100%')
        .justifyContent(FlexAlign.Center)
      } else {
        Scroll() {
          Column() {
            // ===== 医生信息 =====
            Row() {
              Text(this.doctorName.charAt(0) || '医')
                .fontSize(22)
                .fontWeight(FontWeight.Medium)
                .fontColor('#FFFFFF')
                .width(48)
                .height(48)
                .borderRadius(24)
                .backgroundColor('#B8C5D6')
                .textAlign(TextAlign.Center)
                .lineHeight(48)

              Column() {
                Text(this.doctorName)
                  .fontSize(17)
                  .fontWeight(FontWeight.Medium)
                  .fontColor('#1A1A1A')
                Text('就诊医生')
                  .fontSize(12)
                  .fontColor('#999999')
                  .margin({ top: 2 })
              }
              .alignItems(HorizontalAlign.Start)
              .margin({ left: 14 })
              .layoutWeight(1)
            }
            .width('100%')
            .padding(16)
            .backgroundColor('#FFFFFF')
            .borderRadius(12)
            .margin({ bottom: 12 })

            // ===== 日期选择器 =====
            Column() {
              if (this.dateItems.length === 0) {
                Column() {
                  Text('📋')
                    .fontSize(48)
                    .margin({ bottom: 12 })
                  Text('暂无可用排班')
                    .fontSize(16)
                    .fontColor('#999999')
                }
                .width('100%')
                .padding(40)
                .justifyContent(FlexAlign.Center)
                .alignItems(HorizontalAlign.Center)
              } else {
                // ---- 周标题 ----
                Row() {
                  ForEach(['日', '一', '二', '三', '四', '五', '六'], (day: string) => {
                    Text(day)
                      .fontSize(12)
                      .fontColor('#999999')
                      .flexGrow(1)
                      .textAlign(TextAlign.Center)
                  })
                }
                .width('100%')
                .padding({ bottom: 8, top: 4 })

                // ---- 日期横向滚动 ----
                Scroll(this.scrollCtrl) {
                  Row() {
                    ForEach(this.dateItems, (item: DateItem) => {
                      this.DateItemBuilder(item)
                    }, (item: DateItem) => item.fullDate)

                    // 加载更多占位（透明）
                    Column()
                      .width(10)
                      .height(1)
                      .onAppear(() => {
                        this.loadMoreDates();
                      })
                  }
                  .padding({ left: 4, right: 4 })
                }
                .scrollable(ScrollDirection.Horizontal)
                .scrollBar(BarState.Off)
                .width('100%')
                .height(110)
                .margin({ bottom: 4 })
                .onScrollEnd(() => {
                  // 使用 ESObject 或 any 绕过类型检查（ArkTS 允许）
                  const offset: ESObject = this.scrollCtrl.currentOffset;
                  if (offset && (offset as Record<string, number>)['xOffset'] && (offset as Record<string, number>)['xOffset'] > 500) {
                    this.loadMoreDates();
                  }
                })

                // ---- 月份指示 ----
                Row() {
                  Text(this.dateItems.find(d => d.isSelected)?.month || '')
                    .fontSize(13)
                    .fontColor('#666666')
                }
                .width('100%')
                .margin({ top: 4, bottom: 12 })
                .justifyContent(FlexAlign.Center)

                // ===== 时间段选择 =====
                if (this.selectedDateSlots.length > 0) {
                  Column() {
                    Text(this.formatDateDisplay(this.selectedDate) + ' 可预约时段')
                      .fontSize(14)
                      .fontColor('#666666')
                      .margin({ bottom: 12 })
                      .alignSelf(ItemAlign.Start)

                    Grid() {
                      ForEach(this.selectedDateSlots, (slot: TimeSlot) => {
                        GridItem() {
                          Column() {
                            Text(slot.time)
                              .fontSize(13)
                              .fontWeight(this.selectedSlotId === slot.scheduleId ? FontWeight.Medium : FontWeight.Normal)
                              .fontColor(this.selectedSlotId === slot.scheduleId ? '#007AFF' : '#333333')
                              .padding({ left: 12, right: 12, top: 6, bottom: 6 })
                              .borderRadius(16)
                              .backgroundColor(this.selectedSlotId === slot.scheduleId ? '#E6F2FF' : '#F5F6FA')
                              .width('100%')
                              .textAlign(TextAlign.Center)

                            Row() {
                              Text('剩余')
                                .fontSize(10)
                                .fontColor('#999999')
                              Text(String(slot.remainNum))
                                .fontSize(12)
                                .fontWeight(FontWeight.Medium)
                                .fontColor(slot.remainNum <= 3 ? '#FF4D4F' : '#52C41A')
                                .margin({ left: 2, right: 2 })
                              Text('号')
                                .fontSize(10)
                                .fontColor('#999999')
                            }
                            .margin({ top: 4 })
                          }
                          .width('100%')
                          .padding({ top: 10, bottom: 10 })
                          .borderRadius(12)
                          .backgroundColor(this.selectedSlotId === slot.scheduleId ? '#F0F7FF' : 'transparent')
                          .onClick((): void => {
                            this.selectTimeSlot(slot);
                          })
                        }
                      }, (slot: TimeSlot) => slot.scheduleId)
                    }
                    .columnsTemplate('1fr 1fr')
                    .rowsGap(10)
                    .columnsGap(10)
                    .width('100%')
                  }
                  .width('100%')
                  .alignItems(HorizontalAlign.Start)
                }

                // ===== 选中详情 =====
                if (this.selectedSlotId.length > 0) {
                  Divider()
                    .color('#F0F0F0')
                    .margin({ top: 16, bottom: 16 })

                  Row() {
                    Column() {
                      Text('就诊时段')
                        .fontSize(12)
                        .fontColor('#999999')
                      Text(this.selectedTime)
                        .fontSize(14)
                        .fontColor('#1A1A1A')
                        .fontWeight(FontWeight.Medium)
                        .margin({ top: 4 })
                    }
                    .alignItems(HorizontalAlign.Start)
                    .layoutWeight(1)

                    Column() {
                      Text('诊室')
                        .fontSize(12)
                        .fontColor('#999999')
                      Text(this.getSelectedRoom())
                        .fontSize(14)
                        .fontColor('#1A1A1A')
                        .fontWeight(FontWeight.Medium)
                        .margin({ top: 4 })
                    }
                    .alignItems(HorizontalAlign.Center)
                    .layoutWeight(1)

                    Column() {
                      Text('挂号费')
                        .fontSize(12)
                        .fontColor('#999999')
                      Text('¥' + this.getSelectedPrice().toFixed(2))
                        .fontSize(15)
                        .fontColor('#FF6B6B')
                        .fontWeight(FontWeight.Medium)
                        .margin({ top: 4 })
                    }
                    .alignItems(HorizontalAlign.End)
                    .layoutWeight(1)
                  }
                  .width('100%')
                }
              }
            }
            .width('100%')
            .padding(16)
            .backgroundColor('#FFFFFF')
            .borderRadius(12)
            .margin({ bottom: 12 })
            .alignItems(HorizontalAlign.Start)

            // ===== 主诉 =====
            Column() {
              Text('主诉')
                .fontSize(16)
                .fontWeight(FontWeight.Medium)
                .fontColor('#1A1A1A')
                .margin({ bottom: 12 })
                .alignSelf(ItemAlign.Start)

              TextArea({ placeholder: '请简要描述您的症状（如：头痛、发热等）', text: this.chiefComplaint })
                .width('100%')
                .height(100)
                .backgroundColor('#F5F6FA')
                .borderRadius(12)
                .padding(14)
                .fontSize(14)
                .placeholderColor('#BFBFBF')
                .onChange((value: string): void => {
                  this.chiefComplaint = value;
                })
            }
            .width('100%')
            .padding(16)
            .backgroundColor('#FFFFFF')
            .borderRadius(12)
            .margin({ bottom: 80 })
            .alignItems(HorizontalAlign.Start)
          }
          .width('100%')
          .padding(16)
        }
        .scrollable(ScrollDirection.Vertical)
        .scrollBar(BarState.Off)
        .layoutWeight(1)
        .backgroundColor('#F5F6FA')

        Button(this.isSubmitting ? '提交中...' : '确认预约')
          .width('90%')
          .height(50)
          .fontSize(16)
          .fontWeight(FontWeight.Medium)
          .fontColor('#FFFFFF')
          .backgroundColor(this.isSubmitting ? '#CCCCCC' : '#007AFF')
          .borderRadius(25)
          .position({ x: '5%', y: '92%' })
          .enabled(!this.isSubmitting)
          .onClick((): void => {
            this.submitRegister();
          })
      }
    }
    .width('100%')
    .height('100%')
    .backgroundColor('#F5F6FA')
  }
}
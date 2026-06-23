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

interface DateGroup {
  workDate: string;
  slots: TimeSlot[];
}

interface TimeSlot {
  id: string;
  scheduleId: string;
  time: string;
  startTime: string;
  endTime: string;
  room: string;
  remainNum: number;
  price: number;
}

// 患者信息校验响应接口
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

@Entry
@Component
export struct ScheduleSelectPage {
  @State schedules: DoctorSchedule[] = [];
  @State dateGroups: DateGroup[] = [];
  @State isLoading: boolean = true;
  @State selectedSlotId: string = '';
  @State selectedTime: string = '';
  @State chiefComplaint: string = '';
  @State isSubmitting: boolean = false;
  private doctorId: string = '';
  private doctorName: string = '';
  private patientId: string = '';
  private baseUrl: string = APIConfig.PATIENT_SERVICE_URL;

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
        for (let i = 0; i < allSchedules.length; i++) {
          if (allSchedules[i].remainNum > 0 && allSchedules[i].status === 1) {
            availableSchedules.push(allSchedules[i]);
          }
        }
        this.schedules = availableSchedules;
        this.groupByDate();
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

  private groupByDate(): void {
    const dateMap: Record<string, TimeSlot[]> = {};

    for (let i = 0; i < this.schedules.length; i++) {
      const s = this.schedules[i];
      const workDate = s.workDate || '';

      if (!dateMap[workDate]) {
        dateMap[workDate] = [];
      }

      const timeStr = s.startTime + '-' + s.endTime;

      const slot: TimeSlot = {
        id: s.scheduleId,
        scheduleId: s.scheduleId,
        time: timeStr,
        startTime: s.startTime,
        endTime: s.endTime,
        room: s.room || '待定',
        remainNum: s.remainNum,
        price: s.price
      };

      dateMap[workDate].push(slot);
    }

    const groups: DateGroup[] = [];
    const dateKeys = Object.keys(dateMap);

    for (let i = 0; i < dateKeys.length; i++) {
      const workDate = dateKeys[i];
      const slots = dateMap[workDate];

      slots.sort((a: TimeSlot, b: TimeSlot) => {
        return a.startTime.localeCompare(b.startTime);
      });

      groups.push({
        workDate: workDate,
        slots: slots
      });
    }

    groups.sort((a: DateGroup, b: DateGroup) => {
      return a.workDate.localeCompare(b.workDate);
    });

    this.dateGroups = groups;
  }

  private formatDate(dateStr: string): string {
    if (!dateStr) return '未知日期';
    const parts = dateStr.split('-');
    if (parts.length === 3) {
      return parts[0] + '年' + parseInt(parts[1]) + '月' + parseInt(parts[2]) + '日';
    }
    return dateStr;
  }

  private selectTimeSlot(slot: TimeSlot): void {
    this.selectedSlotId = slot.id;
    this.selectedTime = slot.time;
  }

  private getSelectedScheduleId(): string {
    return this.selectedSlotId;
  }

  private getSelectedRoom(): string {
    for (let i = 0; i < this.dateGroups.length; i++) {
      const group = this.dateGroups[i];
      for (let j = 0; j < group.slots.length; j++) {
        if (group.slots[j].id === this.selectedSlotId) {
          return group.slots[j].room;
        }
      }
    }
    return '';
  }

  private getSelectedRemainNum(): number {
    for (let i = 0; i < this.dateGroups.length; i++) {
      const group = this.dateGroups[i];
      for (let j = 0; j < group.slots.length; j++) {
        if (group.slots[j].id === this.selectedSlotId) {
          return group.slots[j].remainNum;
        }
      }
    }
    return 0;
  }

  private getSelectedPrice(): number {
    for (let i = 0; i < this.dateGroups.length; i++) {
      const group = this.dateGroups[i];
      for (let j = 0; j < group.slots.length; j++) {
        if (group.slots[j].id === this.selectedSlotId) {
          return group.slots[j].price;
        }
      }
    }
    return 0;
  }

  /**
   * 检查患者信息是否完整
   */
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

  /**
   * 显示信息不完整弹框
   */
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
      // 检查患者信息是否完整
      const isComplete = await this.checkPatientInfo();

      if (!isComplete) {
        this.isSubmitting = false;
        this.showIncompleteDialog();
        return;
      }

      // 信息完整，继续提交预约
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

  build() {
    Column() {
      Row() {
        Text('←')
          .fontSize(24)
          .onClick((): void => {
            router.back();
          })
        Text('选择就诊时间')
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
          Column() {
            Column() {
              Row() {
                Text('👨‍⚕️')
                  .fontSize(48)
                  .margin({ right: 16 })
                Column() {
                  Text(this.doctorName)
                    .fontSize(18)
                    .fontWeight(FontWeight.Bold)
                    .fontColor('#333333')
                  Text('就诊医生')
                    .fontSize(13)
                    .fontColor('#999999')
                    .margin({ top: 4 })
                }
                .alignItems(HorizontalAlign.Start)
              }
              .width('100%')
              .padding(16)
            }
            .width('100%')
            .backgroundColor('#FFFFFF')
            .borderRadius(16)
            .margin({ bottom: 12 })
            .alignItems(HorizontalAlign.Start)

            Column() {
              Text('请选择就诊时间')
                .fontSize(16)
                .fontWeight(FontWeight.Bold)
                .fontColor('#333333')
                .margin({ bottom: 16 })
                .alignSelf(ItemAlign.Start)

              if (this.dateGroups.length === 0) {
                Column() {
                  Text('📋')
                    .fontSize(48)
                    .margin({ bottom: 12 })
                  Text('暂无可用排班')
                    .fontSize(16)
                    .fontColor('#999999')
                  Text('请选择其他医生或稍后再试')
                    .fontSize(12)
                    .fontColor('#CCCCCC')
                    .margin({ top: 4 })
                }
                .width('100%')
                .padding(40)
                .justifyContent(FlexAlign.Center)
                .alignItems(HorizontalAlign.Center)
              } else {
                ForEach(this.dateGroups, (group: DateGroup) => {
                  Column() {
                    Text(this.formatDate(group.workDate))
                      .fontSize(15)
                      .fontWeight(FontWeight.Medium)
                      .fontColor('#333333')
                      .padding({ left: 4, bottom: 10 })
                      .alignSelf(ItemAlign.Start)

                    Grid() {
                      ForEach(group.slots, (slot: TimeSlot) => {
                        GridItem() {
                          Column() {
                            Text(slot.time)
                              .height(36)
                              .fontSize(13)
                              .textAlign(TextAlign.Center)
                              .backgroundColor(this.selectedSlotId === slot.id ? '#007AFF' : '#F5F5F5')
                              .fontColor(this.selectedSlotId === slot.id ? '#FFFFFF' : '#333333')
                              .borderRadius(18)
                              .padding({ left: 16, right: 16 })
                              .width('100%')

                            if (slot.remainNum > 0 && slot.remainNum <= 5) {
                              Text('仅剩' + slot.remainNum + '号')
                                .fontSize(10)
                                .fontColor('#FF6B6B')
                                .margin({ top: 2 })
                            }
                          }
                          .width('100%')
                          .onClick((): void => {
                            this.selectTimeSlot(slot);
                          })
                        }
                      }, (slot: TimeSlot) => slot.id)
                    }
                    .columnsTemplate('1fr 1fr 1fr')
                    .rowsGap(12)
                    .columnsGap(12)
                    .width('100%')
                  }
                  .width('100%')
                  .margin({ bottom: 20 })
                }, (group: DateGroup) => group.workDate)
              }

              if (this.selectedSlotId.length > 0) {
                Divider()
                  .color('#F0F0F0')
                  .margin({ top: 8, bottom: 16 })

                Row() {
                  Column() {
                    Text('就诊时段')
                      .fontSize(12)
                      .fontColor('#999999')
                    Text(this.selectedTime)
                      .fontSize(14)
                      .fontColor('#007AFF')
                      .fontWeight(FontWeight.Bold)
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
                      .fontColor('#333333')
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
                      .fontSize(14)
                      .fontColor('#FF6B6B')
                      .fontWeight(FontWeight.Bold)
                      .margin({ top: 4 })
                  }
                  .alignItems(HorizontalAlign.End)
                  .layoutWeight(1)
                }
                .width('100%')
              }
            }
            .width('100%')
            .padding(16)
            .backgroundColor('#FFFFFF')
            .borderRadius(16)
            .margin({ bottom: 12 })
            .alignItems(HorizontalAlign.Start)

            Column() {
              Text('主诉')
                .fontSize(16)
                .fontWeight(FontWeight.Bold)
                .fontColor('#333333')
                .margin({ bottom: 12 })
                .alignSelf(ItemAlign.Start)

              TextArea({ placeholder: '请简要描述您的症状（如：头痛、发热等）', text: this.chiefComplaint })
                .width('100%')
                .height(100)
                .backgroundColor('#F8F9FA')
                .borderRadius(12)
                .padding(12)
                .fontSize(14)
                .placeholderColor('#BFBFBF')
                .onChange((value: string): void => {
                  this.chiefComplaint = value;
                })
            }
            .width('100%')
            .padding(16)
            .backgroundColor('#FFFFFF')
            .borderRadius(16)
            .margin({ bottom: 80 })
            .alignItems(HorizontalAlign.Start)
          }
          .width('100%')
          .padding(16)
          .alignItems(HorizontalAlign.Start)
        }
        .scrollable(ScrollDirection.Vertical)
        .scrollBar(BarState.Off)
        .layoutWeight(1)
        .align(Alignment.TopStart)

        Button(this.isSubmitting ? '提交中...' : '确认预约')
          .width('90%')
          .height(48)
          .fontSize(16)
          .fontWeight(FontWeight.Medium)
          .fontColor('#FFFFFF')
          .backgroundColor(this.isSubmitting ? '#D9D9D9' : '#007AFF')
          .borderRadius(24)
          .margin({ bottom: 20 })
          .position({ x: '5%', y: '92%' })
          .enabled(!this.isSubmitting)
          .onClick((): void => {
            this.submitRegister();
          })
      }
    }
    .width('100%')
    .height('100%')
    .backgroundColor('#F5F5F5')
  }
}
// pages/aiConsult/ConsultationAssistantPage.ets

import { http } from '@kit.NetworkKit';
import { promptAction } from '@kit.ArkUI';
import { util } from '@kit.ArkTS';
import { APIConfig } from '../common/APIConfig';
import router from '@ohos.router';

// ============ 类型定义 ============

interface MessageItem {
  msgId: string;
  type: 'user' | 'bot';
  content: string;
  time: string;
  loading?: boolean;
}

interface DoctorRanking {
  doctorId: string;
  name: string;
  position: string;
  goodAt: string;
  introduction: string;
  avatar: string;
  departmentId: string;
  departmentName: string;
  matchScore: number;
  matchReason: string;
}

interface AiRecommendData {
  parsedDiagnosis: string;
  recommendedDepartment: string;
  departmentReason: string;
  doctorRanking: DoctorRanking[];
  aiAnalysisTime: string;
}

interface RawApiResponse {
  code: number;
  msg?: string;
  message?: string;
  data: AiRecommendData;
}

interface ApiResponse {
  code: number;
  message: string;
  data: AiRecommendData;
}

interface ConsultRequest {
  chiefComplaint: string;
  patientId?: string;
}

// ============ 组件 ============

@Component
export struct ConsultationAssistantPage {
  @Prop patientId: string = '';
  @Prop userName: string = '';
  @State inputMessage: string = '';
  @State isSending: boolean = false;
  @State messages: MessageItem[] = [
    {
      msgId: this.genMsgId(),
      type: 'bot',
      content: '您好！我是智能问诊助手 🤖\n请描述您的症状或不适，我会帮您推荐合适的科室和医生。',
      time: this.getCurrentTime()
    }
  ];
  private scroller: Scroller = new Scroller();
  private sessionId: string = Date.now().toString() + Math.random().toString(36).slice(2);

  aboutToAppear() {
    console.info('问诊助手 - 患者ID: ' + this.patientId);
    console.info('问诊助手 - 用户名: ' + this.userName);
  }

  private genMsgId(): string {
    return Date.now().toString() + Math.random().toString(36).slice(2);
  }

  private goBack(): void {
    router.pushUrl({
      url: 'pages/Index'
    }).catch(() => {
      router.back();
    });
  }

  private async sendMessage() {
    if (!this.inputMessage.trim() || this.isSending) {
      return;
    }

    const userContent: string = this.inputMessage.trim();

    const userMessage: MessageItem = {
      msgId: this.genMsgId(),
      type: 'user',
      content: userContent,
      time: this.getCurrentTime()
    };
    this.messages = [...this.messages, userMessage];
    this.inputMessage = '';
    this.isSending = true;

    const loadingMessage: MessageItem = {
      msgId: this.genMsgId(),
      type: 'bot',
      content: '🤔 正在分析您的症状，请稍候...',
      time: this.getCurrentTime(),
      loading: true
    };
    const newMsgListWithLoading = [...this.messages, loadingMessage];
    this.messages = newMsgListWithLoading;
    const loadingIndex = newMsgListWithLoading.length - 1;
    this.scrollToBottom();

    try {
      const response: ApiResponse = await this.callAiConsultApi(userContent);

      let newMsgList = [...this.messages];
      newMsgList.splice(loadingIndex, 1);

      const botContent: string = this.buildBotResponse(response);

      const botMessage: MessageItem = {
        msgId: this.genMsgId(),
        type: 'bot',
        content: botContent,
        time: this.getCurrentTime()
      };
      newMsgList.push(botMessage);
      this.messages = newMsgList;

      this.scrollToBottom();

    } catch (error) {
      let newMsgList = [...this.messages];
      newMsgList.splice(loadingIndex, 1);

      const errorMessage: MessageItem = {
        msgId: this.genMsgId(),
        type: 'bot',
        content: '😅 抱歉，服务暂时不可用，请稍后重试。\n您也可以直接前往医院挂号就诊。',
        time: this.getCurrentTime()
      };
      newMsgList.push(errorMessage);
      this.messages = newMsgList;
      this.scrollToBottom();

      let errorMsg: string = '未知错误';
      if (error instanceof Error) {
        errorMsg = error.message;
      }
      console.error('AI问诊失败:', errorMsg);
      promptAction.showToast({
        message: 'AI服务暂时不可用，请稍后重试',
        duration: 2000
      });
    } finally {
      this.isSending = false;
    }
  }

  private async callAiConsultApi(chiefComplaint: string): Promise<ApiResponse> {
    const url: string = APIConfig.AI_SERVICE_URL + '/ai/consult/recommend?sessionId=' + this.sessionId;

    const requestData: ConsultRequest = {
      chiefComplaint: chiefComplaint,
      patientId: this.patientId || undefined
    };

    console.info('完整请求URL:', url);
    console.info('当前会话sessionId:', this.sessionId);

    try {
      const httpRequest = http.createHttp();

      const response: http.HttpResponse = await httpRequest.request(url, {
        method: http.RequestMethod.POST,
        header: {
          'Content-Type': 'application/json'
        },
        extraData: JSON.stringify(requestData),
        connectTimeout: 30000,
        readTimeout: 30000
      });

      let resultStr: string = '';
      const result = response.result;

      if (typeof result === 'string') {
        resultStr = result;
      } else if (result instanceof ArrayBuffer) {
        const decoder: util.TextDecoder = new util.TextDecoder('utf-8');
        const uint8Array: Uint8Array = new Uint8Array(result);
        resultStr = decoder.decode(uint8Array);
      } else {
        throw new Error('响应格式异常，后端返回非文本数据');
      }

      if (response.responseCode !== 200) {
        throw new Error(`接口HTTP异常，状态码:${response.responseCode},返回内容:${resultStr}`);
      }

      const rawResult: RawApiResponse = JSON.parse(resultStr) as RawApiResponse;

      if (rawResult.code === undefined || rawResult.code === null || rawResult.code === 200) {
        const data = rawResult.data || rawResult;

        const apiResponse: ApiResponse = {
          code: 200,
          message: rawResult.msg || rawResult.message || '成功',
          data: data as AiRecommendData
        };
        return apiResponse;
      } else {
        throw new Error(`业务异常 code=${rawResult.code}，提示：${rawResult.msg || rawResult.message || '未知业务错误'}`);
      }
    } catch (error) {
      if (error instanceof Error) {
        console.error('错误message:', error.message);
        throw error;
      } else {
        throw new Error('未知网络/服务异常');
      }
    }
  }

  private buildBotResponse(response: ApiResponse): string {
    if (!response || !response.data) {
      return '😅 抱歉，AI服务返回的数据格式异常，请稍后重试。';
    }

    const data: AiRecommendData = response.data;
    let content: string = '';

    content += '🏥 **推荐科室**\n' + (data.recommendedDepartment || '未获取到推荐科室') + '\n';
    content += '💡 ' + (data.departmentReason || '') + '\n\n';

    if (data.doctorRanking && data.doctorRanking.length > 0) {
      content += '👨‍⚕️ **推荐医生** (按匹配度排序)\n';
      for (let i: number = 0; i < data.doctorRanking.length; i++) {
        const doctor: DoctorRanking = data.doctorRanking[i];
        let rankEmoji: string;
        if (i === 0) {
          rankEmoji = '🥇';
        } else if (i === 1) {
          rankEmoji = '🥈';
        } else if (i === 2) {
          rankEmoji = '🥉';
        } else {
          rankEmoji = (i + 1) + '.';
        }
        content += rankEmoji + ' **' + doctor.name + '** ' + (doctor.position || '') + '\n';
        content += '  科室：' + doctor.departmentName + '\n';
        if (doctor.goodAt) {
          content += '  擅长：' + doctor.goodAt + '\n';
        }
        content += '  匹配度：' + doctor.matchScore + '分\n';
        if (doctor.matchReason) {
          content += '  理由：' + doctor.matchReason + '\n';
        }
        content += '\n';
      }
    } else {
      content += '😅 当前暂无可推荐的医生，建议您直接到医院挂号就诊。\n\n';
    }

    content += '---\n⏰ 分析时间：' + (data.aiAnalysisTime || this.getCurrentTime());

    return content;
  }

  private scrollToBottom(): void {
    setTimeout(() => {
      this.scroller.scrollEdge(Edge.Bottom);
    }, 300);
    setTimeout(() => {
      this.scroller.scrollEdge(Edge.Bottom);
    }, 600);
  }

  private getCurrentTime(): string {
    const now: Date = new Date();
    const hours: string = now.getHours().toString().padStart(2, '0');
    const minutes: string = now.getMinutes().toString().padStart(2, '0');
    return hours + ':' + minutes;
  }

  private clearChat(): void {
    this.sessionId = Date.now().toString() + Math.random().toString(36).slice(2);
    this.messages = [
      {
        msgId: this.genMsgId(),
        type: 'bot',
        content: '您好！我是智能问诊助手 🤖\n请描述您的症状或不适，我会帮您推荐合适的科室和医生。',
        time: this.getCurrentTime()
      }
    ];
    promptAction.showToast({
      message: '对话已清空，开启新问诊',
      duration: 1500
    });
  }

  private parseContentToLines(content: string): string[] {
    if (!content) {
      return [];
    }
    return content.split('\n');
  }

  private isTitleLine(line: string): boolean {
    return line.includes('📋') || line.includes('🏥') || line.includes('👨‍⚕️');
  }

  private isDividerLine(line: string): boolean {
    return line.trim() === '---';
  }

  private isDoctorRankLine(line: string): boolean {
    return line.includes('🥇') || line.includes('🥈') || line.includes('🥉') || /^\d+\./.test(line.trim());
  }

  private isDoctorDetailLine(line: string): boolean {
    const trimmed: string = line.trim();
    return trimmed.startsWith('科室：') ||
    trimmed.startsWith('擅长：') ||
    trimmed.startsWith('匹配度：') ||
    trimmed.startsWith('理由：') ||
    trimmed.startsWith('  科室：') ||
    trimmed.startsWith('  擅长：') ||
    trimmed.startsWith('  匹配度：') ||
    trimmed.startsWith('  理由：');
  }

  private getCleanLine(line: string): string {
    return line.replace(/\*\*/g, '').trimEnd();
  }

  // ============ UI构建 ============

  build() {
    Column() {
      this.buildHeader();

      Scroll(this.scroller) {
        Column() {
          ForEach(this.messages, (message: MessageItem) => {
            if (message.type === 'bot') {
              this.buildBotMessage(message);
            } else {
              this.buildUserMessage(message);
            }
          }, (message: MessageItem) => message.msgId)
        }
        .width('100%')
        .padding({ left: 16, right: 16, top: 12, bottom: 12 })
        .alignItems(HorizontalAlign.Start)
      }
      .layoutWeight(1)
      .backgroundColor('#F8F9FA')
      .scrollBar(BarState.Off)

      this.buildInputBar();
    }
    .width('100%')
    .height('100%')
    .backgroundColor('#F8F9FA')
  }

  @Builder
  buildHeader() {
    Row() {
      // 返回按钮 - 与其他页面统一
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
        this.goBack();
      })

      Text('AI智能问诊')
        .fontSize(18)
        .fontWeight(FontWeight.Medium)
        .fontColor('#1A1A2E')
        .layoutWeight(1)
        .textAlign(TextAlign.Center)

      Text('清空')
        .fontSize(13)
        .fontColor('#4A90D9')
        .fontWeight(FontWeight.Medium)
        .width(44)
        .height(36)
        .textAlign(TextAlign.Center)
        .borderRadius(18)
        .backgroundColor('#E8F0FE')
        .onClick(() => {
          this.clearChat();
        })
    }
    .width('100%')
    .height(52)
    .padding({ left: 12, right: 12 })
    .backgroundColor('#FFFFFF')
  }

  @Builder
  buildInputBar() {
    Row() {
      TextInput({
        placeholder: '描述您的症状，如：头痛、发热...',
        text: this.inputMessage
      })
        .layoutWeight(1)
        .height(44)
        .backgroundColor('#F1F5F9')
        .borderRadius(22)
        .padding({ left: 18, right: 18 })
        .fontSize(14)
        .fontColor('#1A1A2E')
        .placeholderColor('#8E8E93')
        .onChange((value: string) => {
          this.inputMessage = value;
        })
        .onSubmit(() => {
          this.sendMessage();
        })

      Button(this.isSending ? '发送中' : '发送')
        .height(44)
        .margin({ left: 10 })
        .backgroundColor(this.isSending || !this.inputMessage.trim() ? '#CBD5E1' : '#4A90D9')
        .borderRadius(22)
        .padding({ left: 20, right: 20 })
        .fontColor('#FFFFFF')
        .fontSize(14)
        .fontWeight(FontWeight.Medium)
        .enabled(!this.isSending && !!this.inputMessage.trim())
        .onClick(() => {
          this.sendMessage();
        })
    }
    .width('100%')
    .padding({ left: 16, right: 16, top: 10, bottom: 16 })
    .backgroundColor('#FFFFFF')
    .shadow({
      radius: 8,
      color: 'rgba(0,0,0,0.04)',
      offsetY: -2
    })
  }

  @Builder
  buildBotMessage(message: MessageItem) {
    Row() {
      Column() {
        // 头像和名称
        Row() {
          Column() {
            Text('🤖')
              .fontSize(18)
          }
          .width(32)
          .height(32)
          .borderRadius(16)
          .backgroundColor('#E8F0FE')
          .justifyContent(FlexAlign.Center)
          .alignItems(HorizontalAlign.Center)

          Text('AI问诊助手')
            .fontSize(12)
            .fontColor('#8E8E93')
            .margin({ left: 8 })
            .fontWeight(FontWeight.Medium)
        }
        .width('100%')
        .margin({ bottom: 6 })

        if (message.loading) {
          Row() {
            LoadingProgress()
              .width(20)
              .height(20)
              .margin({ right: 10 })
              .color('#4A90D9')
            Text(message.content)
              .fontSize(14)
              .fontColor('#8E8E93')
          }
          .width('100%')
          .padding(14)
          .backgroundColor('#FFFFFF')
          .borderRadius(12)
          .shadow({
            radius: 4,
            color: 'rgba(0,0,0,0.04)',
            offsetY: 1
          })
        } else {
          Column() {
            ForEach(this.parseContentToLines(message.content), (line: string, index: number) => {
              this.buildMessageLine(line, index);
            }, (line: string, index: number) => index.toString())
          }
          .width('100%')
          .padding(14)
          .backgroundColor('#FFFFFF')
          .borderRadius(12)
          .shadow({
            radius: 4,
            color: 'rgba(0,0,0,0.04)',
            offsetY: 1
          })
          .alignItems(HorizontalAlign.Start)
        }

        Text(message.time)
          .fontSize(10)
          .fontColor('#C7C7CC')
          .width('100%')
          .margin({ top: 4 })
      }
      .alignItems(HorizontalAlign.Start)
      .margin({ right: 40 })
    }
    .width('100%')
    .margin({ bottom: 16 })
  }

  @Builder
  buildMessageLine(line: string, index: number) {
    if (this.isDividerLine(line)) {
      Divider()
        .color('#F0F0F0')
        .margin({ top: 8, bottom: 8 })
        .strokeWidth(0.5)
    } else if (this.isTitleLine(line)) {
      Text(this.getCleanLine(line))
        .fontSize(15)
        .fontWeight(FontWeight.Bold)
        .fontColor('#1A1A2E')
        .margin({ top: index > 0 ? 8 : 0 })
        .width('100%')
        .lineHeight(24)
    } else if (this.isDoctorRankLine(line)) {
      Text(this.getCleanLine(line))
        .fontSize(14)
        .fontWeight(FontWeight.Medium)
        .fontColor('#4A90D9')
        .margin({ top: 4 })
        .width('100%')
        .lineHeight(22)
    } else if (this.isDoctorDetailLine(line)) {
      Text(this.getCleanLine(line))
        .fontSize(13)
        .fontColor('#666666')
        .margin({ left: 8, top: 2 })
        .width('100%')
        .lineHeight(20)
    } else if (line.trim().startsWith('💡')) {
      Text(this.getCleanLine(line))
        .fontSize(13)
        .fontColor('#4A90D9')
        .margin({ top: 2 })
        .width('100%')
        .lineHeight(22)
    } else {
      Text(this.getCleanLine(line))
        .fontSize(14)
        .fontColor('#1A1A2E')
        .margin({ top: 2 })
        .width('100%')
        .lineHeight(22)
    }
  }

  @Builder
  buildUserMessage(message: MessageItem) {
    Row() {
      Column() {
        Text(message.content)
          .fontSize(14)
          .fontColor('#FFFFFF')
          .backgroundColor('#4A90D9')
          .borderRadius(12)
          .padding(14)
          .textOverflow({ overflow: TextOverflow.None })
          .lineHeight(22)
          .shadow({
            radius: 4,
            color: 'rgba(74,144,217,0.20)',
            offsetY: 1
          })

        Text(message.time)
          .fontSize(10)
          .fontColor('#C7C7CC')
          .width('100%')
          .textAlign(TextAlign.End)
          .margin({ top: 4 })
      }
      .alignItems(HorizontalAlign.End)
      .margin({ left: 40 })
    }
    .width('100%')
    .justifyContent(FlexAlign.End)
    .margin({ bottom: 16 })
  }
}
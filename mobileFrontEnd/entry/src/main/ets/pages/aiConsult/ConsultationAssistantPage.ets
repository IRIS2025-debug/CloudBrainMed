// pages/aiConsult/ConsultationAssistantPage.ets

import { http } from '@kit.NetworkKit';
import { promptAction } from '@kit.ArkUI';
import { util } from '@kit.ArkTS';
import { APIConfig } from '../common/APIConfig';

// ============ 类型定义 ============

interface MessageItem {
  msgId: string; // 新增唯一消息ID，解决列表渲染复用bug
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

// 后端实际返回的响应格式
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

  // 新增：页面内全局唯一会话ID，整段对话共用，实现后端多轮记忆
  private sessionId: string = Date.now().toString() + Math.random().toString(36).slice(2);
  aboutToAppear() {
    console.info('问诊助手 - 患者ID: ' + this.patientId);
    console.info('问诊助手 - 用户名: ' + this.userName);
  }

  /**
   * 生成唯一消息ID
   */
  private genMsgId(): string {
    return Date.now().toString() + Math.random().toString(36).slice(2);
  }

  /**
   * 发送消息并调用AI接口
   */
  private async sendMessage() {
    if (!this.inputMessage.trim() || this.isSending) {
      return;
    }

    const userContent: string = this.inputMessage.trim();

    // 构建用户消息
    const userMessage: MessageItem = {
      msgId: this.genMsgId(),
      type: 'user',
      content: userContent,
      time: this.getCurrentTime()
    };
    // 解构新建数组赋值，强制刷新
    this.messages = [...this.messages, userMessage];
    this.inputMessage = '';
    this.isSending = true;

    // 构建加载消息
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

      // 1. 删除加载消息
      let newMsgList = [...this.messages];
      newMsgList.splice(loadingIndex, 1);
      console.info('删除加载项后消息数量：', newMsgList.length);

      // 2. 拼接AI回复文本
      const botContent: string = this.buildBotResponse(response);
      console.info('生成的bot内容长度:', botContent.length, botContent);

      // 3. 构建机器人消息并追加
      const botMessage: MessageItem = {
        msgId: this.genMsgId(),
        type: 'bot',
        content: botContent,
        time: this.getCurrentTime()
      };
      newMsgList.push(botMessage);
      // 整体赋值触发响应式更新
      this.messages = newMsgList;
      console.info('最终消息列表：', JSON.stringify(this.messages));

      this.scrollToBottom();

    } catch (error) {
      // 异常分支移除加载项
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

  /**
   * 调用后端AI问诊接口（修复：携带sessionId参数）
   */
  private async callAiConsultApi(chiefComplaint: string): Promise<ApiResponse> {
    // 拼接URL，追加sessionId请求参数
    const url: string = APIConfig.AI_SERVICE_URL + '/ai/consult/recommend?sessionId=' + this.sessionId;

    const requestData: ConsultRequest = {
      chiefComplaint: chiefComplaint,
      patientId: this.patientId || undefined
    };

    console.info('完整请求URL:', url);
    console.info('当前会话sessionId:', this.sessionId);
    console.info('请求参数body:', JSON.stringify(requestData));

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

      console.info('=== 接口返回完整信息 ===');
      console.info('响应状态码 responseCode:', response.responseCode);
      console.info('原始响应result:', response.result);

      // 处理响应结果 - response.result 可能是 string 或 ArrayBuffer
      let resultStr: string = '';
      const result = response.result;

      if (typeof result === 'string') {
        resultStr = result;
      } else if (result instanceof ArrayBuffer) {
        // 使用 util.TextDecoder 将 ArrayBuffer 转换为字符串
        const decoder: util.TextDecoder = new util.TextDecoder('utf-8');
        const uint8Array: Uint8Array = new Uint8Array(result);
        resultStr = decoder.decode(uint8Array);
      } else {
        throw new Error('响应格式异常，后端返回非文本数据');
      }

      console.info('解析后的响应字符串 resultStr:', resultStr);

      // 状态码非200直接抛错
      if (response.responseCode !== 200) {
        throw new Error(`接口HTTP异常，状态码:${response.responseCode},返回内容:${resultStr}`);
      }

      const rawResult: RawApiResponse = JSON.parse(resultStr) as RawApiResponse;
      console.info('JSON解析后的完整返回体:', JSON.stringify(rawResult));

      // 检查是否有 code 字段，如果没有或者 code 为 undefined，当作成功处理
      if (rawResult.code === undefined || rawResult.code === null || rawResult.code === 200) {
        // 如果 data 字段不存在，说明整个响应就是数据本身
        const data = rawResult.data || rawResult;

        const apiResponse: ApiResponse = {
          code: 200, // 默认成功
          message: rawResult.msg || rawResult.message || '成功',
          data: data as AiRecommendData
        };
        console.info('转换后的业务响应:', JSON.stringify(apiResponse));
        return apiResponse;
      } else {
        // 后端业务码非200，抛出后端返回的错误信息
        throw new Error(`业务异常 code=${rawResult.code}，提示：${rawResult.msg || rawResult.message || '未知业务错误'}`);
      }
    } catch (error) {
      // 打印完整错误堆栈，方便定位真实原因
      console.error('===== API调用完整异常捕获 =====');
      if (error instanceof Error) {
        console.error('错误message:', error.message);
        console.error('完整错误对象:', error);
        throw error;
      } else {
        const errorMsg: string = '未知网络/服务异常';
        console.error(errorMsg, error);
        throw new Error(errorMsg);
      }
    }
  }

  /**
   * 构建机器人回复内容
   */
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

  /**
   * 滚动到底部（双层延时兜底）
   */
  private scrollToBottom(): void {
    setTimeout(() => {
      this.scroller.scrollEdge(Edge.Bottom);
    }, 300);
    setTimeout(() => {
      this.scroller.scrollEdge(Edge.Bottom);
    }, 600);
  }

  /**
   * 获取当前时间
   */
  private getCurrentTime(): string {
    const now: Date = new Date();
    const hours: string = now.getHours().toString().padStart(2, '0');
    const minutes: string = now.getMinutes().toString().padStart(2, '0');
    return hours + ':' + minutes;
  }

  /**
   * 清空对话
   */
  private clearChat(): void {
    // 重置会话ID，新对话重新开启独立记忆
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

  /**
   * 将内容按行分割，并过滤纯空行，减少无效渲染节点
   */
  private parseContentToLines(content: string): string[] {
    if (!content) {
      return [];
    }
    return content.split('\n').filter(item => item.trim() !== '');
  }

  /**
   * 判断是否为标题行
   */
  private isTitleLine(line: string): boolean {
    return line.includes('📋') || line.includes('🏥') || line.includes('👨‍⚕️');
  }

  /**
   * 判断是否为分割线
   */
  private isDividerLine(line: string): boolean {
    return line.trim() === '---';
  }

  /**
   * 判断是否为医生排名行
   */
  private isDoctorRankLine(line: string): boolean {
    return line.includes('🥇') || line.includes('🥈') || line.includes('🥉') || /^\d+\./.test(line.trim());
  }

  /**
   * 判断是否为医生详情行
   */
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

  /**
   * 获取干净的行内容
   */
  private getCleanLine(line: string): string {
    return line.replace(/\*\*/g, '').trimEnd();
  }

  // ============ UI构建 ============

  build() {
    Column() {
      this.buildHeader();

      Scroll(this.scroller) {
        Column() {
          // 使用 msgId 作为唯一key，不再使用index
          ForEach(this.messages, (message: MessageItem) => {
            if (message.type === 'bot') {
              this.buildBotMessage(message);
            } else {
              this.buildUserMessage(message);
            }
          }, (message: MessageItem) => message.msgId)
        }
        .width('100%')
        .padding({ left: 12, right: 12, top: 10, bottom: 10 })
        .alignItems(HorizontalAlign.Start)
      }
      .layoutWeight(1)
      .backgroundColor('#F8FAFC')
      .scrollBar(BarState.Auto)

      this.buildInputBar();
    }
    .width('100%')
    .height('100%')
    .backgroundColor('#F8FAFC')
  }

  @Builder
  buildHeader() {
    Row() {
      Row() {
        Text('←')
          .fontSize(24)
          .fontColor('#2D3748')
      }
      .width(44)
      .height(44)
      .justifyContent(FlexAlign.Center)
      .borderRadius(22)
      .backgroundColor('#F1F5F9')
      .onClick(() => {
        console.info('返回上一页');
      })

      Text('💬 AI智能问诊')
        .fontSize(19)
        .fontWeight(FontWeight.Bold)
        .fontColor('#1E293B')
        .layoutWeight(1)
        .textAlign(TextAlign.Center)

      Text('清空对话')
        .fontSize(14)
        .fontColor('#0284C7')
        .width(60)
        .height(44)
        .textAlign(TextAlign.Center)
        .borderRadius(22)
        .backgroundColor('#EFF6FF')
        .onClick(() => {
          this.clearChat();
        })
    }
    .width('100%')
    .height(60)
    .padding({ left: 10, right: 10 })
    .backgroundColor('#FFFFFF')
    .shadow({ radius: 6, color: '#00000010', offsetY: 2 })
  }

  @Builder
  buildInputBar() {
    Row() {
      TextInput({
        placeholder: '请描述您的身体症状，我将为您分析推荐科室...',
        text: this.inputMessage
      })
        .layoutWeight(1)
        .height(46)
        .backgroundColor('#F1F5F9')
        .borderRadius(24)
        .padding({ left: 18, right: 18 })
        .fontSize(14)
        .fontColor('#334155')
        .placeholderColor('#94A3B8')
        .onChange((value: string) => {
          this.inputMessage = value;
        })
        .onSubmit(() => {
          this.sendMessage();
        })

      Button(this.isSending ? '发送中' : '发送')
        .height(46)
        .margin({ left: 10 })
        .backgroundColor(this.isSending || !this.inputMessage.trim() ? '#CBD5E1' : '#0284C7')
        .borderRadius(24)
        .padding({ left: 22, right: 22 })
        .fontColor('#FFFFFF')
        .fontSize(14)
        .fontWeight(FontWeight.Medium)
        .enabled(!this.isSending && !!this.inputMessage.trim())
        .shadow({ radius: 4, color: '#0284C730', offsetY: 2 })
        .onClick(() => {
          this.sendMessage();
        })
    }
    .width('100%')
    .padding({ left: 14, right: 14, top: 10, bottom: 10 })
    .backgroundColor('#FFFFFF')
    .shadow({ radius: 6, color: '#00000010', offsetY: -2 })
  }

  @Builder
  buildBotMessage(message: MessageItem) {
    Row() {
      Column() {
        Row() {
          Text('🤖')
            .fontSize(18)
          Text('AI问诊助手')
            .fontSize(12)
            .fontColor('#64748B')
            .margin({ left: 6 })
            .fontWeight(FontWeight.Medium)
        }
        .width('100%')
        .margin({ bottom: 6 })

        if (message.loading) {
          Row() {
            LoadingProgress()
              .width(22)
              .height(22)
              .margin({ right: 10 })
              .color('#0284C7')
            Text(message.content)
              .fontSize(14)
              .fontColor('#475569')
          }
          .width('100%')
          .padding(14)
          .backgroundColor('#FFFFFF')
          .borderRadius(16)
          .shadow({ radius: 4, color: '#0000000D', offsetY: 1 })
        } else {
          Column() {
            ForEach(this.parseContentToLines(message.content), (line: string, index: number) => {
              this.buildMessageLine(line, index);
            }, (line: string, index: number) => index.toString())
          }
          .width('100%')
          .padding(14)
          .backgroundColor('#FFFFFF')
          .borderRadius(16)
          .shadow({ radius: 4, color: '#0000000D', offsetY: 1 })
          .alignItems(HorizontalAlign.Start)
        }

        Text(message.time)
          .fontSize(10)
          .fontColor('#94A3B8')
          .width('100%')
          .margin({ top: 6 })
      }
      .alignItems(HorizontalAlign.Start)
      .margin({ right: 50 })
    }
    .width('100%')
    .margin({ bottom: 20 })
  }

  @Builder
  buildMessageLine(line: string, index: number) {
    if (this.isDividerLine(line)) {
      Divider()
        .color('#E2E8F0')
        .margin({ top: 10, bottom: 10 })
        .strokeWidth(1)
    } else if (this.isTitleLine(line)) {
      Text(this.getCleanLine(line))
        .fontSize(15)
        .fontWeight(FontWeight.Bold)
        .fontColor('#1E293B')
        .margin({ top: index > 0 ? 10 : 0 })
        .width('100%')
        .lineHeight(26)
    } else if (this.isDoctorRankLine(line)) {
      Text(this.getCleanLine(line))
        .fontSize(14)
        .fontWeight(FontWeight.Medium)
        .fontColor('#0369A1')
        .margin({ top: 6 })
        .width('100%')
        .lineHeight(24)
    } else if (this.isDoctorDetailLine(line)) {
      Text(this.getCleanLine(line))
        .fontSize(13)
        .fontColor('#475569')
        .margin({ left: 6, top: 2 })
        .width('100%')
        .lineHeight(22)
    } else {
      Text(this.getCleanLine(line))
        .fontSize(14)
        .fontColor('#334155')
        .margin({ top: 3 })
        .width('100%')
        .lineHeight(24)
    }
  }

  @Builder
  buildUserMessage(message: MessageItem) {
    Row() {
      Column() {
        Text(message.content)
          .fontSize(14)
          .fontColor('#FFFFFF')
          .backgroundColor('#0284C7')
          .borderRadius(16)
          .padding(14)
          .textOverflow({ overflow: TextOverflow.None })
          .lineHeight(24)
          .shadow({ radius: 4, color: '#0284C730', offsetY: 1 })

        Text(message.time)
          .fontSize(10)
          .fontColor('#94A3B8')
          .width('100%')
          .textAlign(TextAlign.End)
          .margin({ top: 6 })
      }
      .alignItems(HorizontalAlign.End)
      .margin({ left: 50 })
    }
    .width('100%')
    .justifyContent(FlexAlign.End)
    .margin({ bottom: 20 })
  }
}
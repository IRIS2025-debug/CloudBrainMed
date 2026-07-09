// pages/aiConsult/MedicineAiChatPage.ets
import router from '@ohos.router';
import promptAction from '@ohos.promptAction';
import pasteboard from '@ohos.pasteboard';
import { PatientManager } from '../common/PatientManager';
import { MedicineAiService } from './MedicineAiService';

interface ChatMessage {
  role: string;
  content: string;
  question?: string;
  expanded?: boolean;
}

@Entry
@Component
struct MedicineAiChatPage {
  @State patientId: string = '';
  @State messages: ChatMessage[] = [
    {
      role: 'bot',
      content: '您好！我是AI用药助手 💊\n可以为您解答药品用法、用量、注意事项和副作用等问题。\n如有严重不适，请及时就医。'
    }
  ];
  @State isLoading: boolean = false;
  @State answerVersion: number = 0;
  private sessionId: string = '';
  private scroller: Scroller = new Scroller();
  private botIndex: number = -1;
  private lastQuestion: string = '';

  aboutToAppear(): void {
    const params = router.getParams() as Record<string, string>;
    this.patientId = params?.['patientId'] || PatientManager.getPatientId();
    this.sessionId = 'patient_' + this.patientId + '_' + Date.now();
    console.info('MedicineAiChatPage patientId=' + this.patientId);
  }

  private goBack(): void {
    router.back();
  }

  private scrollToBottom(): void {
    setTimeout(() => {
      this.scroller.scrollEdge(Edge.Bottom);
    }, 50);
  }

  private formatAnswer(content: string): string {
    if (!content) {
      return '';
    }
    return content
      .replace(/\*\*(.*?)\*\*/g, '$1')
      .replace(/\*\*/g, '')
      .replace(/__([^_]+)__/g, '$1')
      .replace(/^#{1,6}\s*/gm, '')
      .replace(/\n{3,}/g, '\n\n')
      .trim();
  }

  private isSectionHeader(line: string): boolean {
    const text: string = line.trim();
    return text.endsWith('：') || text.endsWith(':') ||
      text.includes('用药建议') || text.includes('注意事项') ||
      text.includes('不良反应') || text.includes('禁忌') ||
      text.includes('建议') ||
      text.includes('风险') || text.includes('提醒');
  }

  private isWarningLine(line: string): boolean {
    const text: string = line.trim();
    return text.includes('立即就医') || text.includes('尽快就医') ||
      text.includes('咨询医生') || text.includes('咨询药师') ||
      text.includes('过敏') || text.includes('胸痛') ||
      text.includes('呼吸困难') || text.includes('意识异常') ||
      text.includes('禁忌') || text.includes('慎用');
  }

  private normalizeBulletLine(line: string): string {
    let text: string = line.trim();
    text = text.replace(/^[-*•·]\s*/, '');
    text = text.replace(/^\d+[.、)]\s*/, '');
    text = text.replace(/^\d+\s+/, '');
    return text;
  }

  private appendBotChunk(chunk: string): void {
    if (this.botIndex < 0 || this.botIndex >= this.messages.length) {
      return;
    }
    const current: ChatMessage = this.messages[this.botIndex];
    const updated: ChatMessage[] = [...this.messages];
    updated[this.botIndex] = {
      role: current.role,
      content: this.formatAnswer(chunk)
    };
    this.messages = updated;
    this.scrollToBottom();
  }

  private copyText(text: string): void {
    try {
      const pasteData = pasteboard.createData('text/plain', text);
      pasteboard.getSystemPasteboard().setData(pasteData);
      promptAction.showToast({ message: '已复制回复', duration: 1500 });
    } catch (error) {
      promptAction.showToast({ message: '复制失败，请重试', duration: 1500 });
    }
  }

  private regenerateAnswer(): void {
    if (!this.lastQuestion || this.isLoading) {
      return;
    }
    this.sendMessage(this.lastQuestion);
  }

  private updateBotMessage(content: string): void {
    if (this.botIndex < 0 || this.botIndex >= this.messages.length) {
      return;
    }
    const current: ChatMessage = this.messages[this.botIndex];
    const updated: ChatMessage[] = [...this.messages];
    updated[this.botIndex] = {
      role: current.role,
      content: this.formatAnswer(content)
    };
    this.messages = updated;
  }

  private sendMessage(question: string): void {
    if (!question || this.isLoading) {
      return;
    }
    if (!this.patientId) {
      promptAction.showToast({ message: '未获取到患者信息，请重新登录', duration: 2000 });
      return;
    }

    this.lastQuestion = question;
    const userMessages: ChatMessage[] = [...this.messages];
    userMessages.push({ role: 'user', content: question, question: question, expanded: true });
    userMessages.push({ role: 'bot', content: '', question: question, expanded: true });
    this.messages = userMessages;
    this.botIndex = this.messages.length - 1;
    this.isLoading = true;
    this.scrollToBottom();

    MedicineAiService.streamChat({
      sessionId: this.sessionId,
      question: question,
      userRole: 'patient',
      patientId: this.patientId
    }, (chunk: string) => {
      this.appendBotChunk(chunk);
    }, () => {
      this.isLoading = false;
      const botContent: string = this.messages[this.botIndex].content.trim();
      if (botContent.length === 0) {
        this.updateBotMessage('抱歉，暂未获取到回复，请稍后重试。');
      }
      this.scrollToBottom();
    }, (message: string) => {
      this.isLoading = false;
      this.updateBotMessage('抱歉，请求失败：' + message);
      promptAction.showToast({ message: '发送失败，请检查网络', duration: 2000 });
      this.scrollToBottom();
    });
  }

  private parseContentToLines(content: string): string[] {
    if (!content) {
      return [];
    }
    return content.split('\n');
  }

  private isTitleLine(line: string): boolean {
    const trimmed = line.trim();
    return trimmed.startsWith('💊') ||
      trimmed.startsWith('📋') ||
      trimmed.startsWith('⚠️') ||
      trimmed.startsWith('💡') ||
      this.isSectionHeader(trimmed);
  }

  private formatLine(line: string): string {
    return this.normalizeBulletLine(line.trim());
  }

  private toggleExpanded(index: number): void {
    const updated: ChatMessage[] = [...this.messages];
    const current = updated[index];
    updated[index] = {
      role: current.role,
      content: current.content,
      question: current.question,
      expanded: !(current.expanded ?? true)
    };
    this.messages = updated;
  }

  private getDisplayLines(content: string, expanded: boolean): string[] {
    const lines = this.parseContentToLines(content);
    if (expanded) {
      return lines;
    }
    return lines.slice(0, 8);
  }

  @Builder
  BotMessage(content: string, index: number) {
    Row() {
      Column() {
        Row() {
          Column() {
            Text('💊')
              .fontSize(18)
          }
          .width(32)
          .height(32)
          .borderRadius(16)
          .backgroundColor('#E8F0FE')
          .justifyContent(FlexAlign.Center)
          .alignItems(HorizontalAlign.Center)

          Text('AI用药助手')
            .fontSize(12)
            .fontColor('#8E8E93')
            .margin({ left: 8 })
            .fontWeight(FontWeight.Medium)
        }
        .width('100%')
        .margin({ bottom: 8 })

        Column() {
          ForEach(this.getDisplayLines(content, this.messages[index].expanded ?? true), (line: string, lineIndex: number) => {
            if (line.trim() === '') {
              Blank().height(8)
            } else if (this.isSectionHeader(line.trim())) {
              Text(this.normalizeBulletLine(line.trim()))
                .fontSize(15)
                .fontWeight(FontWeight.Bold)
                .fontColor('#1D4ED8')
                .margin({ top: lineIndex > 0 ? 8 : 0, bottom: 4 })
                .width('100%')
                .lineHeight(22)
            } else if (this.isWarningLine(line.trim())) {
              Row() {
                Text('⚠️')
                  .fontSize(14)
                  .margin({ right: 6 })
                Text(this.normalizeBulletLine(line.trim()))
                  .fontSize(14)
                  .fontColor('#B45309')
                  .lineHeight(22)
                  .layoutWeight(1)
              }
              .width('100%')
              .padding({ left: 10, right: 10, top: 8, bottom: 8 })
              .margin({ top: 4, bottom: 4 })
              .backgroundColor('#FFF7ED')
              .borderRadius(10)
            } else {
              Row() {
                Text('•')
                  .fontSize(14)
                  .fontColor('#94A3B8')
                  .margin({ right: 8, top: 1 })
                Text(this.normalizeBulletLine(line.trim()))
                  .fontSize(14)
                  .fontColor('#334155')
                  .lineHeight(22)
                  .layoutWeight(1)
              }
              .width('100%')
              .padding({ left: 0, right: 0, top: 2, bottom: 2 })
            }
          }, (line: string, lineIndex: number) => lineIndex.toString())
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

        Row() {
          Button() {
            Text('📋')
              .fontSize(14)
          }
          .width(30)
          .height(30)
          .backgroundColor('#EFF6FF')
          .fontColor('#2563EB')
          .borderRadius(15)
          .onClick(() => { this.copyText(content); })

          Button() {
            Text('↻')
              .fontSize(14)
          }
          .width(30)
          .height(30)
          .margin({ left: 8 })
          .backgroundColor('#F1F5F9')
          .fontColor('#475569')
          .borderRadius(15)
          .enabled(!this.isLoading)
          .onClick(() => { this.regenerateAnswer(); })

          Button(this.messages[index].expanded ?? true ? '收起' : '展开')
            .height(30)
            .padding({ left: 10, right: 10 })
            .margin({ left: 8 })
            .backgroundColor('#F8FAFC')
            .fontColor('#0F172A')
            .borderRadius(15)
            .onClick(() => { this.toggleExpanded(index); })
        }
        .width('100%')
        .justifyContent(FlexAlign.End)
        .margin({ top: 8 })
      }
      .alignItems(HorizontalAlign.Start)
      .margin({ right: 40 })
    }
    .width('100%')
    .margin({ bottom: 16 })
  }

  @Builder
  UserMessage(content: string) {
    Row() {
      Column() {
        Text(content)
          .fontSize(14)
          .fontColor('#FFFFFF')
          .backgroundColor('#4A90D9')
          .borderRadius(12)
          .padding(14)
          .lineHeight(22)
          .shadow({
            radius: 4,
            color: 'rgba(74,144,217,0.20)',
            offsetY: 1
          })
      }
      .alignItems(HorizontalAlign.End)
      .margin({ left: 40 })
    }
    .width('100%')
    .justifyContent(FlexAlign.End)
    .margin({ bottom: 16 })
  }

  @Builder
  LoadingMessage() {
    Row() {
      Column() {
        Row() {
          Column() {
            Text('💊')
              .fontSize(18)
          }
          .width(32)
          .height(32)
          .borderRadius(16)
          .backgroundColor('#E8F0FE')
          .justifyContent(FlexAlign.Center)
          .alignItems(HorizontalAlign.Center)

          Text('AI用药助手')
            .fontSize(12)
            .fontColor('#8E8E93')
            .margin({ left: 8 })
            .fontWeight(FontWeight.Medium)
        }
        .width('100%')
        .margin({ bottom: 6 })

        Row() {
          LoadingProgress()
            .width(20)
            .height(20)
            .margin({ right: 10 })
            .color('#4A90D9')
          Text('正在思考...')
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
      }
      .alignItems(HorizontalAlign.Start)
      .margin({ right: 40 })
    }
    .width('100%')
    .margin({ bottom: 16 })
  }

  build() {
    Column() {
      // 顶部导航栏 - 与其他页面统一
      Row() {
        Stack() {
          Row()
            .width(36)
            .height(36)
            .borderRadius(18)
            .backgroundColor('rgba(255,255,255,0.15)')
          Text('‹')
            .fontSize(22)
            .fontColor('#FFFFFF')
            .fontWeight(FontWeight.Medium)
        }
        .width(40)
        .height(40)
        .onClick(() => {
          this.goBack();
        })

        Text('AI用药助手')
          .fontSize(18)
          .fontWeight(FontWeight.Medium)
          .fontColor('#FFFFFF')
          .layoutWeight(1)
          .textAlign(TextAlign.Center)

        Row().width(40).height(40)
      }
      .width('100%')
      .height(52)
      .padding({ left: 12, right: 12 })
      .backgroundColor('#4A90D9')

      // 消息列表
      Scroll(this.scroller) {
        Column() {
          ForEach(this.messages, (msg: ChatMessage, index: number) => {
            if (msg.role === 'bot') {
              if (index === this.messages.length - 1 && this.isLoading && msg.content === '') {
                this.LoadingMessage()
              } else {
                this.BotMessage(msg.content, index)
              }
            } else {
              this.UserMessage(msg.content)
            }
          }, (msg: ChatMessage, index: number) => index + '_' + msg.role + '_' + msg.content.length)
        }
        .width('100%')
        .padding({ left: 16, right: 16, top: 12, bottom: 12 })
      }
      .layoutWeight(1)
      .backgroundColor('#F8F9FA')
      .scrollBar(BarState.Off)
      .edgeEffect(EdgeEffect.Spring)

      // 输入栏
      MedicineChatInputBar({
        isLoading: this.isLoading,
        onSend: (text: string): void => {
          this.sendMessage(text);
        }
      })
    }
    .width('100%')
    .height('100%')
    .backgroundColor('#FFFFFF')
  }
}

@Component
struct MedicineChatInputBar {
  @Prop isLoading: boolean = false;
  onSend: (text: string) => void = (_text: string): void => {};
  @State inputText: string = '';

  private handleSend(): void {
    const text: string = this.inputText.trim();
    if (text.length === 0 || this.isLoading) {
      return;
    }
    this.onSend(text);
    this.inputText = '';
  }

  build() {
    Row() {
      TextInput({ text: $$this.inputText, placeholder: '请输入用药相关问题...' })
        .layoutWeight(1)
        .height(44)
        .backgroundColor('#F1F5F9')
        .borderRadius(22)
        .padding({ left: 18, right: 18 })
        .fontSize(14)
        .fontColor('#1A1A2E')
        .placeholderColor('#8E8E93')
        .type(InputType.Normal)
        .enterKeyType(EnterKeyType.Send)
        .enableKeyboardOnFocus(true)
        .enabled(!this.isLoading)
        .onSubmit((): void => {
          this.handleSend();
        })

      Button(this.isLoading ? '回复中' : '发送')
        .height(44)
        .margin({ left: 10 })
        .backgroundColor(this.isLoading ? '#CBD5E1' : '#4A90D9')
        .borderRadius(22)
        .padding({ left: 20, right: 20 })
        .fontSize(14)
        .fontWeight(FontWeight.Medium)
        .fontColor('#FFFFFF')
        .enabled(!this.isLoading)
        .onClick((): void => {
          this.handleSend();
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
}
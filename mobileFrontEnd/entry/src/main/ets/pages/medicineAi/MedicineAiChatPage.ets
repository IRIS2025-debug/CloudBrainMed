import router from '@ohos.router';
import promptAction from '@ohos.promptAction';
import { PatientManager } from '../common/PatientManager';
import { MedicineAiService } from './MedicineAiService';

interface ChatMessage {
  role: string;
  content: string;
}

@Entry
@Component
struct MedicineAiChatPage {
  @State patientId: string = '';
  @State messages: ChatMessage[] = [
    {
      role: 'bot',
      content: '您好！我是AI用药助手，可以为您解答药品用法、用量、注意事项和副作用等问题。如有严重不适，请及时就医。'
    }
  ];
  @State isLoading: boolean = false;
  private sessionId: string = '';
  private scroller: Scroller = new Scroller();
  private botIndex: number = -1;

  aboutToAppear(): void {
    const params = router.getParams() as Record<string, string>;
    this.patientId = params?.['patientId'] || PatientManager.getPatientId();
    this.sessionId = 'patient_' + this.patientId + '_' + Date.now();
    console.info('MedicineAiChatPage patientId=' + this.patientId);
  }

  private scrollToBottom(): void {
    setTimeout(() => {
      this.scroller.scrollEdge(Edge.Bottom);
    }, 50);
  }

  private appendBotChunk(chunk: string): void {
    if (this.botIndex < 0 || this.botIndex >= this.messages.length) {
      return;
    }
    const current: ChatMessage = this.messages[this.botIndex];
    const updated: ChatMessage[] = [...this.messages];
    updated[this.botIndex] = {
      role: current.role,
      content: current.content + chunk
    };
    this.messages = updated;
    this.scrollToBottom();
  }

  private updateBotMessage(content: string): void {
    if (this.botIndex < 0 || this.botIndex >= this.messages.length) {
      return;
    }
    const current: ChatMessage = this.messages[this.botIndex];
    const updated: ChatMessage[] = [...this.messages];
    updated[this.botIndex] = {
      role: current.role,
      content: content
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

    const userMessages: ChatMessage[] = [...this.messages];
    userMessages.push({ role: 'user', content: question });
    userMessages.push({ role: 'bot', content: '' });
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

  @Builder
  BotMessage(content: string) {
    Row() {
      Column() {
        Text('🤖')
          .fontSize(22)
          .width(36)
          .height(36)
          .textAlign(TextAlign.Center)
          .backgroundColor('#E8F0FE')
          .borderRadius(18)
          .margin({ right: 10 })

        Column() {
          Text('AI用药助手')
            .fontSize(11)
            .fontColor('#999999')
            .margin({ bottom: 4 })
          Text(content.length > 0 ? content : '...')
            .fontSize(14)
            .fontColor('#333333')
            .backgroundColor('#F0F4FA')
            .borderRadius(12)
            .padding(12)
            .maxLines(100)
            .textOverflow({ overflow: TextOverflow.Ellipsis })
        }
        .alignItems(HorizontalAlign.Start)
        .layoutWeight(1)
      }
      .alignItems(HorizontalAlign.Start)
      .width('85%')
    }
    .width('100%')
    .justifyContent(FlexAlign.Start)
    .margin({ bottom: 16 })
  }

  @Builder
  UserMessage(content: string) {
    Row() {
      Blank()
      Text(content)
        .fontSize(14)
        .fontColor('#FFFFFF')
        .backgroundColor('#2563EB')
        .borderRadius(12)
        .padding(12)
        .maxLines(100)
        .constraintSize({ maxWidth: '75%' })
    }
    .width('100%')
    .margin({ bottom: 16 })
  }

  build() {
    Column() {
      Row() {
        Text('‹')
          .fontSize(28)
          .fontColor('#FFFFFF')
          .width(40)
          .height(40)
          .textAlign(TextAlign.Center)
          .onClick(() => {
            router.back();
          })

        Text('AI用药助手')
          .fontSize(18)
          .fontWeight(FontWeight.Bold)
          .fontColor('#FFFFFF')
          .layoutWeight(1)
          .textAlign(TextAlign.Center)

        Blank()
          .width(40)
      }
      .width('100%')
      .height(52)
      .padding({ left: 8, right: 8 })
      .backgroundColor('#2563EB')

      Scroll(this.scroller) {
        Column() {
          ForEach(this.messages, (msg: ChatMessage) => {
            if (msg.role === 'bot') {
              this.BotMessage(msg.content)
            } else {
              this.UserMessage(msg.content)
            }
          }, (msg: ChatMessage, index: number) => index + '_' + msg.role + '_' + msg.content.length)
        }
        .width('100%')
        .padding({ left: 16, right: 16, top: 16, bottom: 16 })
      }
      .layoutWeight(1)
      .scrollBar(BarState.Auto)
      .edgeEffect(EdgeEffect.Spring)

      MedicineChatInputBar({
        isLoading: this.isLoading,
        onSend: (text: string): void => {
          this.sendMessage(text);
        }
      })
    }
    .width('100%')
    .height('100%')
    .backgroundColor('#F8F9FA')
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
        .height(42)
        .backgroundColor('#F5F5F5')
        .borderRadius(21)
        .padding({ left: 16, right: 16 })
        .type(InputType.Normal)
        .enterKeyType(EnterKeyType.Done)
        .enableKeyboardOnFocus(true)
        .enabled(!this.isLoading)
        .onSubmit((): void => {
          this.handleSend();
        })

      Button(this.isLoading ? '回复中' : '发送')
        .height(42)
        .margin({ left: 10 })
        .backgroundColor(this.isLoading ? '#93B4F5' : '#2563EB')
        .borderRadius(21)
        .padding({ left: 18, right: 18 })
        .enabled(!this.isLoading)
        .onClick((): void => {
          this.handleSend();
        })
    }
    .width('100%')
    .padding({ left: 12, right: 12, top: 10, bottom: 12 })
    .backgroundColor('#FFFFFF')
    .border({ width: { top: 0.5 }, color: '#E5E5E5' })
  }
}
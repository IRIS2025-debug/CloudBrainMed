import http from '@ohos.net.http';
import { APIConfig } from '../common/APIConfig';
import { PatientManager } from '../common/PatientManager';

export interface MedicineChatRequest {
  sessionId: string;
  question: string;
  userRole: string;
  patientId: string;
}

export class MedicineAiService {
  private static readonly CHAT_URL: string = APIConfig.AI_SERVICE_URL + '/medicine/chat';

  static streamChat(
    request: MedicineChatRequest,
    onChunk: (chunk: string) => void,
    onComplete: () => void,
    onError: (message: string) => void
  ): void {
    const httpRequest = http.createHttp();
    const token: string = PatientManager.getToken();
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
      'Accept': 'text/event-stream'
    };
    if (token.length > 0) {
      headers['token'] = token;
    }

    httpRequest.request(MedicineAiService.CHAT_URL, {
      method: http.RequestMethod.POST,
      header: headers,
      extraData: JSON.stringify(request),
      expectDataType: http.HttpDataType.STRING,
      connectTimeout: 60000,
      readTimeout: 300000
    }).then((response: http.HttpResponse) => {
      if (response.responseCode !== 200) {
        onError('请求失败，状态码: ' + response.responseCode);
        return;
      }
      const body: string = typeof response.result === 'string'
        ? response.result
        : JSON.stringify(response.result);
      MedicineAiService.typewriterEmit(body, onChunk, onComplete);
    }).catch((err: Error) => {
      onError(err.message || '网络请求失败');
    }).finally(() => {
      httpRequest.destroy();
    });
  }

  private static extractText(body: string): string {
    const lines: string[] = body.split('\n');
    let result: string = '';
    for (let i = 0; i < lines.length; i++) {
      const line: string = lines[i];
      if (line.startsWith('data:')) {
        result += line.substring(5);
      } else if (line.length > 0 && !line.startsWith(':') && !line.startsWith('event:') && !line.startsWith('id:')) {
        result += line;
      }
    }
    return result;
  }

  private static typewriterEmit(
    body: string,
    onChunk: (chunk: string) => void,
    onComplete: () => void
  ): void {
    const fullText: string = MedicineAiService.extractText(body);
    if (fullText.length === 0) {
      onComplete();
      return;
    }
    let index: number = 0;
    const timer: number = setInterval(() => {
      if (index < fullText.length) {
        onChunk(fullText.charAt(index));
        index++;
      } else {
        clearInterval(timer);
        onComplete();
      }
    }, 30);
  }
}

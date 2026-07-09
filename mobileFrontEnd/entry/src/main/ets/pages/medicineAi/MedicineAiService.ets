import http from '@ohos.net.http';
import { APIConfig } from '../common/APIConfig';

export interface MedicineAiChatRequest {
  sessionId: string;
  question: string;
  userRole: string;
  patientId: string;
}

export class MedicineAiService {
  static streamChat(
    request: MedicineAiChatRequest,
    onChunk: (chunk: string) => void,
    onComplete: () => void,
    onError: (message: string) => void
  ): void {
    const httpRequest = http.createHttp();
    httpRequest.request(
      `${APIConfig.AI_SERVICE_URL}/ai/medicine/chat`,
      {
        method: http.RequestMethod.POST,
        header: {
          'Content-Type': 'application/json',
          'Accept': 'application/json'
        },
        extraData: {
          patientId: request.patientId,
          message: request.question,
          sessionId: request.sessionId
        },
        expectDataType: http.HttpDataType.STRING,
        connectTimeout: 15000,
        readTimeout: 120000
      },
      (error: Error, response: http.HttpResponse) => {
        httpRequest.destroy();
        if (error) {
          onError(error.message || '网络请求失败');
          return;
        }
        if (response.responseCode !== 200) {
          onError(`服务异常(${response.responseCode})`);
          return;
        }
        const text: string = String(response.result || '').trim();
        if (!text) {
          onError('AI未返回有效内容，请稍后重试');
          return;
        }
        onChunk(text);
        onComplete();
      }
    );
  }
}

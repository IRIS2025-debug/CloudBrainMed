// common/HttpUtil.ets
import http from '@ohos.net.http';
import { fileIo } from '@kit.CoreFileKit';
import util from '@ohos.util';
import promptAction from '@ohos.promptAction';
import router from '@ohos.router';
import { PatientManager } from './PatientManager';

// 定义请求配置接口
export interface RequestConfig {
  url: string;
  method: http.RequestMethod;
  data?: object;
  header?: Record<string, string>;
}

export interface UploadFileOptions {
  url: string;
  fileUri: string;
  fieldName?: string;
  fileName?: string;
  mimeType?: string;
}

export class HttpUtil {

  /**
   * 核心请求方法
   */
  static async request<T>(options: RequestConfig, context: Context): Promise<T> {
    const httpRequest = http.createHttp();

    // 直接从 PatientManager 获取 Token
    const token = PatientManager.getToken();

    // 构建请求头
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
      'Accept': 'application/json'
    };

    // 添加自定义header
    if (options.header) {
      const headerKeys = Object.keys(options.header);
      for (let i = 0; i < headerKeys.length; i++) {
        const key = headerKeys[i];
        headers[key] = options.header[key];
      }
    }

    // 添加token
    if (token && token.length > 0) {
      headers['token'] = token;
    }

    try {
      const response = await httpRequest.request(options.url, {
        method: options.method,
        header: headers,
        extraData: options.data ? JSON.stringify(options.data) : undefined,
        connectTimeout: 30000,
        readTimeout: 30000
      });

      if (response.responseCode === 200) {
        const result: T = JSON.parse(response.result as string) as T;
        return result;
      } else if (response.responseCode === 401) {
        PatientManager.clearPatientInfo();
        promptAction.showToast({
          message: '登录已过期，请重新登录',
          duration: 2000
        });
        setTimeout(() => {
          router.replaceUrl({
            url: 'pages/login/LoginPage'
          });
        }, 500);
        throw new Error('Unauthorized');
      } else {
        // 尝试解析错误信息
        try {
          const errorResult = JSON.parse(response.result as string) as Record<string, string>;
          const errorMsg: string = errorResult?.msg || errorResult?.message || `请求失败 (${response.responseCode})`;
          throw new Error(errorMsg);
        } catch (parseError) {
          throw new Error(`请求失败，状态码: ${response.responseCode}`);
        }
      }
    } catch (error) {
      console.error('网络请求失败：' + JSON.stringify(error));
      if (error instanceof Error) {
        throw error;
      } else {
        throw new Error(String(error));
      }
    } finally {
      httpRequest.destroy();
    }
  }

  /**
   * GET请求
   */
  static async get<T>(url: string, context: Context, params?: Record<string, string>): Promise<T> {
    let fullUrl: string = url;
    if (params) {
      const keys: string[] = Object.keys(params);
      const queryParts: string[] = [];
      for (let i = 0; i < keys.length; i++) {
        const key: string = keys[i];
        const value: string | undefined = params[key];
        if (value !== undefined && value !== null && value !== '') {
          queryParts.push(encodeURIComponent(key) + '=' + encodeURIComponent(value));
        }
      }
      if (queryParts.length > 0) {
        fullUrl = url + '?' + queryParts.join('&');
      }
    }

    const requestConfig: RequestConfig = {
      url: fullUrl,
      method: http.RequestMethod.GET
    };
    return HttpUtil.request<T>(requestConfig, context);
  }

  /**
   * POST请求
   */
  static async post<T>(url: string, data: object, context: Context): Promise<T> {
    const requestConfig: RequestConfig = {
      url: url,
      method: http.RequestMethod.POST,
      data: data
    };
    return HttpUtil.request<T>(requestConfig, context);
  }

  /**
   * PUT请求
   */
  static async put<T>(url: string, data: object, context: Context): Promise<T> {
    const requestConfig: RequestConfig = {
      url: url,
      method: http.RequestMethod.PUT,
      data: data
    };
    return HttpUtil.request<T>(requestConfig, context);
  }

  /**
   * DELETE请求
   */
  static async delete<T>(url: string, context: Context): Promise<T> {
    const requestConfig: RequestConfig = {
      url: url,
      method: http.RequestMethod.DELETE
    };
    return HttpUtil.request<T>(requestConfig, context);
  }

  /**
   * Multipart 文件上传（用于头像等场景）
   */
  static async uploadFile(options: UploadFileOptions): Promise<string> {
    const fieldName = options.fieldName ?? 'file';
    const fileName = options.fileName ?? 'avatar.jpg';
    const mimeType = options.mimeType ?? 'image/jpeg';
    const boundary = `----HarmonyOSBoundary${Date.now()}`;
    const token = PatientManager.getToken();

    let fileHandle: fileIo.File | null = null;
    try {
      fileHandle = fileIo.openSync(options.fileUri, fileIo.OpenMode.READ_ONLY);
      const stat = fileIo.statSync(fileHandle.fd);
      const fileBuffer = new ArrayBuffer(stat.size);
      fileIo.readSync(fileHandle.fd, fileBuffer);

      const headerPart =
        `--${boundary}\r\n` +
        `Content-Disposition: form-data; name="${fieldName}"; filename="${fileName}"\r\n` +
        `Content-Type: ${mimeType}\r\n\r\n`;
      const footerPart = `\r\n--${boundary}--\r\n`;

      const headerBytes = HttpUtil.encodeText(headerPart);
      const contentBytes = new Uint8Array(fileBuffer);
      const footerBytes = HttpUtil.encodeText(footerPart);
      const body = new Uint8Array(headerBytes.length + contentBytes.length + footerBytes.length);
      body.set(headerBytes, 0);
      body.set(contentBytes, headerBytes.length);
      body.set(footerBytes, headerBytes.length + contentBytes.length);

      const headers: Record<string, string> = {
        'Content-Type': `multipart/form-data; boundary=${boundary}`,
        'Accept': 'application/json'
      };
      if (token && token.length > 0) {
        headers['token'] = token;
      }

      const httpRequest = http.createHttp();
      try {
        const response = await httpRequest.request(options.url, {
          method: http.RequestMethod.POST,
          header: headers,
          extraData: body.buffer,
          connectTimeout: 30000,
          readTimeout: 30000
        });

        if (response.responseCode === 401) {
          PatientManager.clearPatientInfo();
          promptAction.showToast({ message: '登录已过期，请重新登录', duration: 2000 });
          setTimeout(() => {
            router.replaceUrl({ url: 'pages/login/LoginPage' });
          }, 500);
          throw new Error('Unauthorized');
        }

        if (response.responseCode === 200) {
          return response.result as string;
        }

        try {
          const errorResult = JSON.parse(response.result as string) as Record<string, string>;
          throw new Error(errorResult?.msg || errorResult?.message || `上传失败 (${response.responseCode})`);
        } catch (parseError) {
          throw new Error(`上传失败，状态码: ${response.responseCode}`);
        }
      } finally {
        httpRequest.destroy();
      }
    } finally {
      if (fileHandle !== null) {
        fileIo.closeSync(fileHandle);
      }
    }
  }

  private static encodeText(text: string): Uint8Array {
    const encoder = new util.TextEncoder();
    return encoder.encodeInto(text);
  }
}
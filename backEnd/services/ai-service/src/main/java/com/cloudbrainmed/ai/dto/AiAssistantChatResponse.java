package com.cloudbrainmed.ai.dto;

import lombok.Data;

/**
 * AI辅助接诊聊天响应。
 *
 * <p>当问题由辅助接诊模块直接回答时，answer就是对话框展示内容；
 * 当问题被编排到病历、检查或处方等专业模块时，answer给出简短说明，
 * moduleResult承载专业模块的结构化响应。</p>
 */
@Data
public class AiAssistantChatResponse {
    /** 本次AI请求的追踪ID，用于日志审计和反馈关联。 */
    private String traceId;

    /** 识别出的意图，对应AiAssistantIntentEnum。 */
    private String intent;

    /** 可直接展示在医生AI对话框中的文本回复。 */
    private String answer;

    /** 实际处理该请求的模块；为空表示由辅助接诊模块直接处理。 */
    private String handledModule;

    /** 专业模块返回的结构化结果，例如病历草稿、处方审核结果。 */
    private Object moduleResult;

    /** 当前使用的大模型版本或模型标识。 */
    private String modelVersion;

    /** 是否由AI辅助接诊模块自身处理，而不是委派给专业模块。 */
    private boolean handledByAssistant;

    /** 是否为异常降级结果。 */
    private boolean fallback;
}

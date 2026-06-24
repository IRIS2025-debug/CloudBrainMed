package com.cloudbrainmed.ai.enums;

/**
 * AI辅助接诊聊天意图。
 *
 * <p>其中问诊补全、信息缺失、上下文整理、普通问答和诊断辅助由
 * AI辅助接诊模块直接处理；病历生成、处方审核由聊天服务
 * 编排调用对应专业模块。</p>
 */
public enum AiAssistantIntentEnum {
    /** 生成医生下一步应该追问的问题。 */
    FOLLOW_UP_QUESTION,

    /** 判断当前问诊信息还缺少哪些关键内容。 */
    MISSING_INFORMATION,

    /** 整理患者已知信息、历史资料和待确认事项。 */
    CONTEXT_SUMMARY,

    /** 基于当前患者上下文回答普通接诊问题。 */
    CONTEXT_QA,

    /** 生成病历草稿，委派给AI病历自动生成模块。 */
    MEDICAL_RECORD_DRAFT,

    /** 审核处方或用药风险，委派给AI处方审核模块。 */
    PRESCRIPTION_REVIEW,

    /** 输出可能诊断、疑似诊断或鉴别诊断建议。 */
    DIAGNOSIS_ASSISTANT,

    /** 未识别意图。 */
    UNKNOWN;

    /**
     * 判断该意图是否由AI辅助接诊模块内部直接处理。
     */
    public boolean isAssistantHandled() {
        return this == FOLLOW_UP_QUESTION
                || this == MISSING_INFORMATION
                || this == CONTEXT_SUMMARY
                || this == CONTEXT_QA
                || this == DIAGNOSIS_ASSISTANT;
    }

    /**
     * 将前端快捷按钮传入的actionType转换为枚举。
     */
    public static AiAssistantIntentEnum fromActionType(String value) {
        if (value == null || value.isBlank()) {
            return UNKNOWN;
        }
        try {
            return AiAssistantIntentEnum.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            return UNKNOWN;
        }
    }
}

package com.cloudbrainmed.ai.service;

import com.cloudbrainmed.ai.dto.AiAssistantChatRequest;
import com.cloudbrainmed.ai.dto.AiAssistantChatResponse;

/**
 * AI辅助接诊聊天服务。
 *
 * <p>作为医生AI对话框的后端统一入口，负责基于患者上下文回答问诊类问题，
 * 并在需要时编排调用病历生成、处方审核等专业AI模块。</p>
 */
public interface AiAssistantChatService {

    /**
     * 处理一次医生AI对话请求。
     *
     * @param request 医生问题、当前病历草稿和专业模块所需参数
     * @param doctorId 当前登录医生ID
     * @return 可展示的AI回复及可能存在的专业模块结构化结果
     */
    AiAssistantChatResponse chat(
            AiAssistantChatRequest request, String doctorId);
}

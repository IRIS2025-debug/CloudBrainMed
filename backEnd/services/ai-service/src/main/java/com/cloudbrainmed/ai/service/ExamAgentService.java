package com.cloudbrainmed.ai.service;

import com.cloudbrainmed.ai.dto.ExamGenerateRequest;
import com.cloudbrainmed.ai.dto.ExamGenerateResponse;

public interface ExamAgentService {

    /**
     * AI检查检验生成Agent
     * 流程：Planner → RAG检索 → LLM推理 → Formatter → Trace记录
     *
     * @param request 包含registerId和病历上下文
     * @return 检查建议响应
     */
    ExamGenerateResponse generateExamSuggestions(ExamGenerateRequest request);
}
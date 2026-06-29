package com.cloudbrainmed.ai.service;

import java.util.Map;

public interface AiReceptionService {
    /**
     * AI 智能接诊分析（前端直调）
     * @param registerId 挂号记录 ID
     * @return { diagnosis, exams, advice, risk }
     */
    Map<String, Object> analyze(String registerId);
}

package com.cloudbrainmed.ai.service;

import com.cloudbrainmed.ai.dto.ConsultRecommendDto;
import com.cloudbrainmed.ai.vo.AiRecommendResponseVo;

public interface AiConsultService {

    // 新增带sessionId多轮问诊方法
    AiRecommendResponseVo recommendDoctor(String sessionId, ConsultRecommendDto consultRecommendDto);

    // 可选：清空当前会话记忆
    void clearSessionMemory(String sessionId);
}

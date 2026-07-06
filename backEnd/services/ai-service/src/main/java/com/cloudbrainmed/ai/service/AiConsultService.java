package com.cloudbrainmed.ai.service;

import com.cloudbrainmed.ai.dto.ConsultRecommendDto;
import com.cloudbrainmed.ai.vo.AiRecommendResponseVo;

public interface AiConsultService {

    /**
     * AI问诊推荐医生
     * @param consultRecommendDto 问诊请求
     * @return 推荐结果
     */
    AiRecommendResponseVo recommendDoctor(ConsultRecommendDto consultRecommendDto);

    /**
     * 获取所有科室列表
     */
    java.util.List<String> getAllDepartments();

    // 新增带sessionId多轮问诊方法
    AiRecommendResponseVo recommendDoctor(String sessionId, ConsultRecommendDto consultRecommendDto);

    // 可选：清空当前会话记忆
    void clearSessionMemory(String sessionId);
}

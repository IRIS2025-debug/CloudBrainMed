package com.cloudbrainmed.ai.controller;

import com.cloudbrainmed.ai.dto.ConsultRecommendDto;
import com.cloudbrainmed.ai.service.AiConsultService;
import com.cloudbrainmed.ai.vo.AiRecommendResponseVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ai-service/ai/consult")
@Slf4j
@RequiredArgsConstructor
public class AiConsultController {

    private final AiConsultService aiConsultService;

    /**
     * 多轮连贯问诊接口，前端传递sessionId维持会话记忆
     * @param sessionId 前端自定义唯一会话ID（前端生成uuid）
     * @param dto 主诉参数
     */
    @PostMapping("/recommend")
    public AiRecommendResponseVo getDoctorRecommendation(
            @RequestParam String sessionId,
            @RequestBody ConsultRecommendDto dto) {
        log.info("AI问诊请求，会话ID：{}，主诉：{}", sessionId, dto.getChiefComplaint());
        return aiConsultService.recommendDoctor(sessionId, dto);
    }

    /**
     * 清空当前会话临时记忆
     */
    @PostMapping("/clearMemory")
    public String clearMemory(@RequestParam String sessionId) {
        aiConsultService.clearSessionMemory(sessionId);
        return "会话" + sessionId + "临时记忆已清空";
    }

//    /**
//     * 兼容旧接口，无记忆单轮问诊
//     */
//    @PostMapping("/recommend/single")
//    public AiRecommendResponseVo getSingleRecommend(@RequestBody ConsultRecommendDto dto) {
//        log.info("单轮无记忆AI问诊，主诉：{}", dto.getChiefComplaint());
//        return aiConsultService.recommendDoctor(dto);
//    }
}
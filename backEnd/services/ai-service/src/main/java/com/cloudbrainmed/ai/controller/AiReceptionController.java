package com.cloudbrainmed.ai.controller;

import com.cloudbrainmed.ai.service.AiReceptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * AI 智能接诊（前端直调入口）
 * 替代原先 doctor-service 中转 → /api/ai/consult/analyze 的旧链路。
 */
@RestController
@RequestMapping("/ai-service/assistant")
public class AiReceptionController {

    @Autowired
    private AiReceptionService aiReceptionService;

    /**
     * AI 接诊分析
     * 入参: { registerId }
     * ai-service 通过 Feign 反调 doctor-service 获取病历上下文，
     * 再调用 DeepSeek 返回分析结果。
     *
     * @param request { "registerId": "xxx" }
     * @return { diagnosis, exams, advice, risk }
     */
    @PostMapping("/analyze")
    public Map<String, Object> analyze(@RequestBody Map<String, String> request) {
        String registerId = request.get("registerId");
        return aiReceptionService.analyze(registerId);
    }
}
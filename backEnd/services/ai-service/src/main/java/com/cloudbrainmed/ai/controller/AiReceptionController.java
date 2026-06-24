package com.cloudbrainmed.ai.controller;

import com.cloudbrainmed.ai.service.AiReceptionService;
import com.cloudbrainmed.common.utils.DoctorJwtUtil;
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
    public Map<String, Object> analyze(
            @RequestHeader(value = "token", required = false) String token,
            @RequestBody Map<String, String> request) {
        String registerId = request.get("registerId");
        return aiReceptionService.analyze(registerId, extractDoctorId(token));
    }

    private String extractDoctorId(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("未登录，请先登录");
        }
        try {
            if (!Integer.valueOf(2).equals(DoctorJwtUtil.getRoleType(token))) {
                throw new IllegalArgumentException("仅医生可以使用AI辅助接诊");
            }
            return DoctorJwtUtil.getUserId(token);
        } catch (Exception exception) {
            if (exception instanceof IllegalArgumentException argumentException) {
                throw argumentException;
            }
            throw new IllegalArgumentException("医生登录凭证无效");
        }
    }
}

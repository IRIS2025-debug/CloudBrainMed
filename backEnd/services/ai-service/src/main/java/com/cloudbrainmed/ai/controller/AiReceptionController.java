package com.cloudbrainmed.ai.controller;

import com.cloudbrainmed.ai.dto.AiAssistantChatRequest;
import com.cloudbrainmed.ai.dto.AiAssistantChatResponse;
import com.cloudbrainmed.ai.dto.AiRecordGenerateRequest;
import com.cloudbrainmed.ai.dto.AiRecordGenerateResponse;
import com.cloudbrainmed.ai.service.AiAssistantChatService;
import com.cloudbrainmed.ai.service.AiMedicalRecordService;
import com.cloudbrainmed.ai.support.DoctorAuthHelper;
import com.cloudbrainmed.common.result.Result;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI接诊相关接口入口。
 *
 * <p>当前类承载三类能力：
 * AI辅助接诊聊天、AI输出采纳反馈、AI病历自动生成。所有对医生开放的接口
 * 都会先从医生token中校验医生身份，避免患者或其他角色直接调用。</p>
 */
@RestController
public class AiReceptionController {

    private final AiMedicalRecordService aiMedicalRecordService;
    private final AiAssistantChatService aiAssistantChatService;

    public AiReceptionController(
            AiMedicalRecordService aiMedicalRecordService,
            AiAssistantChatService aiAssistantChatService) {
        this.aiMedicalRecordService = aiMedicalRecordService;
        this.aiAssistantChatService = aiAssistantChatService;
    }

    @PostMapping("/ai-service/reception/chat")
    public Result<AiAssistantChatResponse> chat(
            @RequestHeader(value = "token", required = false) String token,
            @Valid @RequestBody AiAssistantChatRequest request) {
        return Result.ok(aiAssistantChatService.chat(
                request, extractDoctorId(token)));
    }


    /**
     * 独立的AI病历自动生成接口。
     *
     * <p>聊天入口识别到病历生成意图时也会在服务层复用同一个业务服务。</p>
     */
    @PostMapping("/ai-service/reception/record/generate")
    public Result<AiRecordGenerateResponse> generateRecord(
            @RequestHeader(value = "token", required = false) String token,
            @Valid @RequestBody AiRecordGenerateRequest request) {
        return Result.ok(aiMedicalRecordService.generate(
                request, extractDoctorId(token)));
    }

    /**
     * 从医生登录token中提取医生ID，并限制只有医生角色可以使用本控制器能力。
     */
    private String extractDoctorId(String token) {
        return DoctorAuthHelper.requireDoctorId(token, "AI辅助接诊");
    }
}

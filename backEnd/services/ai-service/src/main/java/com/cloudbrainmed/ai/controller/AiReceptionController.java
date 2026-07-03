package com.cloudbrainmed.ai.controller;

import com.cloudbrainmed.ai.dto.AiAssistantChatRequest;
import com.cloudbrainmed.ai.dto.AiAssistantChatResponse;
import com.cloudbrainmed.ai.service.AiAssistantChatService;
import com.cloudbrainmed.ai.support.DoctorAuthHelper;
import com.cloudbrainmed.common.result.Result;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AiReceptionController {

    private final AiAssistantChatService aiAssistantChatService;

    public AiReceptionController(
            AiAssistantChatService aiAssistantChatService) {
        this.aiAssistantChatService = aiAssistantChatService;
    }

    @PostMapping("/ai-service/reception/chat")
    public Result<AiAssistantChatResponse> chat(
            @RequestHeader(value = "token", required = false) String token,
            @Valid @RequestBody AiAssistantChatRequest request) {
        return Result.ok(aiAssistantChatService.chat(
                request, extractDoctorId(token)));
    }

    private String extractDoctorId(String token) {
        return DoctorAuthHelper.requireDoctorId(token, "AI reception");
    }
}

package com.cloudbrainmed.ai.controller;

import com.cloudbrainmed.ai.dto.AiScheduleConflictCheckRequest;
import com.cloudbrainmed.ai.dto.AiScheduleGenerateRequest;
import com.cloudbrainmed.ai.dto.AiScheduleGenerateResponse;
import com.cloudbrainmed.ai.dto.AiSchedulePublishRequest;
import com.cloudbrainmed.ai.dto.AiSchedulePublishResponse;
import com.cloudbrainmed.ai.service.AiScheduleService;
import com.cloudbrainmed.ai.support.AdminAuthHelper;
import com.cloudbrainmed.common.result.Result;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class AiScheduleController {

    private final AiScheduleService aiScheduleService;

    public AiScheduleController(AiScheduleService aiScheduleService) {
        this.aiScheduleService = aiScheduleService;
    }

    @PostMapping("/ai-service/schedule/preview")
    public Result<AiScheduleGenerateResponse> preview(
            @RequestHeader Map<String, String> headers,
            @Valid @RequestBody AiScheduleGenerateRequest request) {
        return Result.ok(aiScheduleService.preview(
                request, extractAdminId(headers)));
    }

    @PostMapping("/ai-service/schedule/conflict-check")
    public Result<AiScheduleGenerateResponse> checkConflicts(
            @RequestHeader Map<String, String> headers,
            @Valid @RequestBody AiScheduleConflictCheckRequest request) {
        extractAdminId(headers);
        return Result.ok(aiScheduleService.checkConflicts(request));
    }

    @PostMapping("/ai-service/schedule/publish")
    public Result<AiSchedulePublishResponse> publish(
            @RequestHeader Map<String, String> headers,
            @Valid @RequestBody AiSchedulePublishRequest request) {
        return Result.ok(aiScheduleService.publish(
                request, extractAdminId(headers)));
    }

    private String extractAdminId(Map<String, String> headers) {
        return AdminAuthHelper.requireAdminId(resolveToken(headers),
                "AI智能排班");
    }

    private String resolveToken(Map<String, String> headers) {
        if (headers == null || headers.isEmpty()) {
            return "";
        }
        String authorization = firstHeader(
                headers, HttpHeaders.AUTHORIZATION, "authorization");
        if (authorization != null && !authorization.isBlank()) {
            String value = authorization.trim();
            return value.regionMatches(true, 0, "Bearer ", 0, 7)
                    ? value.substring(7).trim()
                    : value;
        }
        String legacyToken = firstHeader(headers, "token", "Token");
        return legacyToken == null ? "" : legacyToken.trim();
    }

    private String firstHeader(Map<String, String> headers, String... names) {
        for (String name : names) {
            String value = headers.get(name);
            if (value != null) {
                return value;
            }
        }
        return null;
    }
}

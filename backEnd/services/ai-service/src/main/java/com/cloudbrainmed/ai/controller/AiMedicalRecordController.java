package com.cloudbrainmed.ai.controller;

import com.cloudbrainmed.ai.dto.AiRecordGenerateRequest;
import com.cloudbrainmed.ai.dto.AiRecordGenerateResponse;
import com.cloudbrainmed.ai.service.AiMedicalRecordService;
import com.cloudbrainmed.ai.support.DoctorAuthHelper;
import com.cloudbrainmed.common.result.Result;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai-service/medical-record")
public class AiMedicalRecordController {

    private final AiMedicalRecordService aiMedicalRecordService;

    public AiMedicalRecordController(
            AiMedicalRecordService aiMedicalRecordService) {
        this.aiMedicalRecordService = aiMedicalRecordService;
    }

    @PostMapping("/draft")
    public Result<AiRecordGenerateResponse> draft(
            @RequestHeader(value = "token", required = false) String token,
            @Valid @RequestBody AiRecordGenerateRequest request) {
        return Result.ok(aiMedicalRecordService.generate(
                request, extractDoctorId(token)));
    }

    private String extractDoctorId(String token) {
        return DoctorAuthHelper.requireDoctorId(token, "AI medical record");
    }
}

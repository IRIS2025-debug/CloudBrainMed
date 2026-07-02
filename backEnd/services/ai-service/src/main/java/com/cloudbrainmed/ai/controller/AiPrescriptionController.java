package com.cloudbrainmed.ai.controller;

import com.cloudbrainmed.ai.dto.AiAssistantChatRequest;
import com.cloudbrainmed.ai.dto.PrescriptionDraftRequest;
import com.cloudbrainmed.ai.dto.PrescriptionDraftResponse;
import com.cloudbrainmed.ai.dto.PrescriptionReviewRequest;
import com.cloudbrainmed.ai.dto.PrescriptionReviewResponse;
import com.cloudbrainmed.ai.enums.AiAssistantIntentEnum;
import com.cloudbrainmed.ai.service.AiPrescriptionDraftService;
import com.cloudbrainmed.ai.service.AiPrescriptionReviewService;
import com.cloudbrainmed.ai.support.DoctorAuthHelper;
import com.cloudbrainmed.common.result.Result;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai-service/prescription")
public class AiPrescriptionController {

    private final AiPrescriptionDraftService draftService;
    private final AiPrescriptionReviewService reviewService;

    public AiPrescriptionController(
            AiPrescriptionDraftService draftService,
            AiPrescriptionReviewService reviewService) {
        this.draftService = draftService;
        this.reviewService = reviewService;
    }

    @PostMapping("/draft")
    public Result<PrescriptionDraftResponse> draft(
            @RequestHeader(value = "token", required = false) String token,
            @Valid @RequestBody PrescriptionDraftRequest request) {
        return Result.ok(draftService.generate(
                toAssistantRequest(request), extractDoctorId(token)));
    }

    @PostMapping("/review")
    public Result<PrescriptionReviewResponse> review(
            @RequestHeader(value = "token", required = false) String token,
            @Valid @RequestBody PrescriptionReviewRequest request) {
        return Result.ok(reviewService.review(
                request, extractDoctorId(token)));
    }

    private String extractDoctorId(String token) {
        return DoctorAuthHelper.requireDoctorId(token, "AI prescription");
    }

    private AiAssistantChatRequest toAssistantRequest(
            PrescriptionDraftRequest request) {
        AiAssistantChatRequest assistantRequest = new AiAssistantChatRequest();
        assistantRequest.setRegisterId(request.getRegisterId());
        assistantRequest.setActionType(
                AiAssistantIntentEnum.PRESCRIPTION_DRAFT.name());
        assistantRequest.setMessage(request.getMessage());
        assistantRequest.setCurrentRecordDesc(request.getCurrentRecordDesc());
        assistantRequest.setSymptomDescription(
                request.getSymptomDescription());
        assistantRequest.setConversationText(request.getConversationText());
        assistantRequest.setStructuredParameters(
                request.getStructuredParameters());
        assistantRequest.setFollowUpAnswers(request.getFollowUpAnswers());
        assistantRequest.setPatientInformation(
                request.getPatientInformation());
        return assistantRequest;
    }
}

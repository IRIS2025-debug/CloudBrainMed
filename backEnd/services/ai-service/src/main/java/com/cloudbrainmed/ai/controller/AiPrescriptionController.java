package com.cloudbrainmed.ai.controller;

import com.cloudbrainmed.ai.dto.PrescriptionReviewRequest;
import com.cloudbrainmed.ai.dto.PrescriptionReviewResponse;
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
@RequestMapping("/api/ai/prescription")
public class AiPrescriptionController {

    private final AiPrescriptionReviewService reviewService;

    public AiPrescriptionController(
            AiPrescriptionReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping("/review")
    public Result<PrescriptionReviewResponse> review(
            @RequestHeader(value = "token", required = false) String token,
            @Valid @RequestBody PrescriptionReviewRequest request) {
        return Result.ok(reviewService.review(
                request, extractDoctorId(token)));
    }

    private String extractDoctorId(String token) {
        return DoctorAuthHelper.requireDoctorId(token, "AI处方审核");
    }
}

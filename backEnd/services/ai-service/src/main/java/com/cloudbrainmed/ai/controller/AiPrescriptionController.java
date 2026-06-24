package com.cloudbrainmed.ai.controller;

import com.cloudbrainmed.ai.dto.PrescriptionReviewRequest;
import com.cloudbrainmed.ai.dto.PrescriptionReviewResponse;
import com.cloudbrainmed.ai.service.AiPrescriptionReviewService;
import com.cloudbrainmed.common.result.Result;
import com.cloudbrainmed.common.utils.DoctorJwtUtil;
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
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("未登录，请先登录");
        }
        try {
            if (!Integer.valueOf(2).equals(
                    DoctorJwtUtil.getRoleType(token))) {
                throw new IllegalArgumentException(
                        "仅医生可以使用AI处方审核");
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

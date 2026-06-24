package com.cloudbrainmed.ai.service.impl;

import com.cloudbrainmed.ai.dto.AiFeedbackRequest;
import com.cloudbrainmed.ai.mapper.AiFeedbackSampleMapper;
import com.cloudbrainmed.ai.mapper.AiInferenceLogMapper;
import com.cloudbrainmed.ai.service.AiReportService;
import com.cloudbrainmed.api.dto.ReportContextDto;
import com.cloudbrainmed.api.feign.DoctorFeignClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class AiReportServiceImpl implements AiReportService {

    private static final Set<String> ADOPTION_TYPES =
            Set.of("FULL", "PARTIAL", "REJECTED");

    private final ObjectMapper objectMapper;
    private final AiInferenceLogMapper inferenceLogMapper;
    private final AiFeedbackSampleMapper feedbackSampleMapper;
    private final DoctorFeignClient doctorFeignClient;
    private final String internalServiceKey;

    public AiReportServiceImpl(
            ObjectMapper objectMapper,
            AiInferenceLogMapper inferenceLogMapper,
            AiFeedbackSampleMapper feedbackSampleMapper,
            DoctorFeignClient doctorFeignClient,
            @Value("${internal.service-key:}") String internalServiceKey) {
        this.objectMapper = objectMapper;
        this.inferenceLogMapper = inferenceLogMapper;
        this.feedbackSampleMapper = feedbackSampleMapper;
        this.doctorFeignClient = doctorFeignClient;
        this.internalServiceKey = internalServiceKey;
    }

    @Override
    @Transactional
    public boolean saveFeedback(
            AiFeedbackRequest request, String doctorId) {
        if (request.getTraceId() == null || request.getTraceId().isBlank()) {
            return false;
        }
        if (!ADOPTION_TYPES.contains(request.getAdoptionType())) {
            throw new IllegalArgumentException(
                    "adoptionType must be FULL, PARTIAL or REJECTED");
        }

        String inputSummary = inferenceLogMapper.findInputByTraceId(
                request.getTraceId());
        String registerId = extractRegisterId(inputSummary);
        if (!hasText(registerId)) {
            return false;
        }
        ReportContextDto context = doctorFeignClient.getConsultContext(
                registerId, doctorId, requireInternalServiceKey());
        if (context == null || !context.isAvailable()) {
            return false;
        }
        if (feedbackSampleMapper.countByTraceIdAndDoctorId(
                request.getTraceId(), doctorId) > 0) {
            return true;
        }

        String aiOutputJson = inferenceLogMapper.findOutputByTraceId(
                request.getTraceId());
        if (!hasText(aiOutputJson)) {
            return false;
        }

        String finalOutputJson = toJson(Map.of(
                "finalRecordDesc", request.getFinalRecordDesc(),
                "adoptionType", request.getAdoptionType()));
        String aiRecordDesc = extractSuggestedRecordDesc(aiOutputJson);
        BigDecimal diffScore = calculateDiffScore(
                aiRecordDesc, request.getFinalRecordDesc());

        feedbackSampleMapper.insert(
                "SMP" + compactUuid().substring(0, 29),
                request.getTraceId(),
                aiOutputJson,
                finalOutputJson,
                (short) ("REJECTED".equals(request.getAdoptionType()) ? 0 : 1),
                diffScore,
                LocalDateTime.now(),
                doctorId);
        return true;
    }

    private String extractRegisterId(String inputSummary) {
        if (!hasText(inputSummary)) {
            return "";
        }
        try {
            JsonNode root = objectMapper.readTree(inputSummary);
            JsonNode node = root.get("registerId");
            return node == null ? "" : node.asText("");
        } catch (Exception ignored) {
            return "";
        }
    }

    private String extractSuggestedRecordDesc(String aiOutputJson) {
        try {
            JsonNode root = objectMapper.readTree(aiOutputJson);
            JsonNode node = root.get("suggestedRecordDesc");
            if (node == null || node.asText("").isBlank()) {
                node = root.get("draftRecordDesc");
            }
            return node == null ? "" : node.asText("");
        } catch (Exception ignored) {
            return "";
        }
    }

    private BigDecimal calculateDiffScore(String source, String target) {
        String left = source == null ? "" : source;
        String right = target == null ? "" : target;
        int maxLength = Math.max(left.length(), right.length());
        if (maxLength == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf((double) levenshtein(left, right) / maxLength)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private int levenshtein(String left, String right) {
        int[] previous = new int[right.length() + 1];
        int[] current = new int[right.length() + 1];
        for (int j = 0; j <= right.length(); j++) {
            previous[j] = j;
        }
        for (int i = 1; i <= left.length(); i++) {
            current[0] = i;
            for (int j = 1; j <= right.length(); j++) {
                int cost = left.charAt(i - 1) == right.charAt(j - 1) ? 0 : 1;
                current[j] = Math.min(
                        Math.min(current[j - 1] + 1, previous[j] + 1),
                        previous[j - 1] + cost);
            }
            int[] swap = previous;
            previous = current;
            current = swap;
        }
        return previous[right.length()];
    }

    private String requireInternalServiceKey() {
        if (!hasText(internalServiceKey)) {
            throw new IllegalStateException(
                    "INTERNAL_SERVICE_KEY is not configured");
        }
        return internalServiceKey;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            return "{}";
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String compactUuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}

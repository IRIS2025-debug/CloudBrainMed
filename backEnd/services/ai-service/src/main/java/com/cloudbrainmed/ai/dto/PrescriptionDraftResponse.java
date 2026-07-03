package com.cloudbrainmed.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PrescriptionDraftResponse {
    private String traceId;
    private String status;
    private String modelVersion;
    private String summary;
    private String overallRiskLevel;
    private List<PrescriptionDraftMedicine> medicines = new ArrayList<>();
    private List<String> missingInformation = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();
    private PrescriptionReviewResponse reviewResult;
    private boolean fallback;
}

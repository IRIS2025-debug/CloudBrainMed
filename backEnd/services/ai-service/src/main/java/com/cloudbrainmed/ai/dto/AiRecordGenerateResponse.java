package com.cloudbrainmed.ai.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AiRecordGenerateResponse {
    private String traceId;
    private String status;
    private String modelVersion;
    private String informationCompleteness;
    private AiStructuredMedicalRecord structuredRecord =
            new AiStructuredMedicalRecord();
    private String draftRecordDesc;
    private List<String> missingInformation = new ArrayList<>();
    private String riskLevel;
    private List<String> riskWarnings = new ArrayList<>();
    private boolean fallback;
}

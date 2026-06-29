package com.cloudbrainmed.ai.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class PrescriptionReviewResponse {
    private String traceId;
    private String status;
    private String modelVersion;
    private boolean passed;
    private String overallRiskLevel;
    private String summary;
    private List<DrugInteractionResult> interactions = new ArrayList<>();
    private List<PrescriptionMedicineRisk> medicineRisks = new ArrayList<>();
    private List<String> contraindications = new ArrayList<>();
    private List<String> recommendations = new ArrayList<>();
    private List<String> missingInformation = new ArrayList<>();
    private boolean fallback;
}

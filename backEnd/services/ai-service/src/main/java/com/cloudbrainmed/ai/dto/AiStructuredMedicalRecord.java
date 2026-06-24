package com.cloudbrainmed.ai.dto;

import lombok.Data;

@Data
public class AiStructuredMedicalRecord {
    private String chiefComplaint;
    private String historyOfPresentIllness;
    private String pastMedicalHistory;
    private String allergyHistory;
    private String personalHistory;
    private String familyHistory;
    private String physicalExamination;
    private String auxiliaryExamination;
    private String assessment;
    private String treatmentPlan;
}

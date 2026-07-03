package com.cloudbrainmed.ai.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class PrescriptionDraftRequest {

    @NotBlank
    @Size(max = 32)
    private String registerId;

    @Size(max = 2000)
    private String message;

    @Size(max = 10000)
    private String currentRecordDesc;

    @Size(max = 5000)
    private String symptomDescription;

    @Size(max = 20000)
    private String conversationText;

    @Size(max = 30)
    private Map<String, String> structuredParameters =
            new LinkedHashMap<>();

    @Size(max = 10)
    private Map<String, String> followUpAnswers = new LinkedHashMap<>();

    @Size(max = 30)
    private Map<String, String> patientInformation =
            new LinkedHashMap<>();

    @AssertTrue(message = "message, currentRecordDesc, conversationText or structuredParameters is required")
    public boolean isInputProvided() {
        return hasText(message)
                || hasText(currentRecordDesc)
                || hasText(conversationText)
                || hasStructuredInput();
    }

    private boolean hasStructuredInput() {
        return structuredParameters != null
                && structuredParameters.entrySet().stream().anyMatch(entry ->
                hasText(entry.getKey()) && hasText(entry.getValue()));
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}

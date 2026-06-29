package com.cloudbrainmed.ai.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class AiRecordGenerateRequest {

    @NotBlank
    @Size(max = 32)
    private String registerId;

    @Size(max = 20000)
    private String conversationText;

    @Size(max = 30)
    private Map<String, String> structuredParameters =
            new LinkedHashMap<>();

    @Size(max = 10000)
    private String currentRecordDesc;

    @AssertTrue(message = "conversationText和structuredParameters至少提供一项")
    public boolean isInputProvided() {
        if (conversationText != null && !conversationText.isBlank()) {
            return true;
        }
        return structuredParameters != null
                && structuredParameters.entrySet().stream().anyMatch(entry ->
                entry.getKey() != null && !entry.getKey().isBlank()
                        && entry.getValue() != null
                        && !entry.getValue().isBlank());
    }
}

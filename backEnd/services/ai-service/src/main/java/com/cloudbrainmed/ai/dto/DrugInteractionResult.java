package com.cloudbrainmed.ai.dto;

import lombok.Data;

@Data
public class DrugInteractionResult {
    private String medicineA;
    private String medicineB;
    private String severity;
    private String description;
    private String recommendation;
}

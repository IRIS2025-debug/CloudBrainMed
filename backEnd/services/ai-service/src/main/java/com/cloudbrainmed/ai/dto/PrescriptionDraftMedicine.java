package com.cloudbrainmed.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PrescriptionDraftMedicine {
    private String medicineId;
    private String medicineName;
    private String spec;
    private String usage;
    private Integer quantity;
    private String reason;
    private List<String> warnings = new ArrayList<>();
}

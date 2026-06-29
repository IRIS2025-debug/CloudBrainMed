package com.cloudbrainmed.ai.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class PrescriptionMedicineRisk {
    private String medicineId;
    private String medicineName;
    private String riskLevel;
    private List<String> issues = new ArrayList<>();
    private List<String> suggestions = new ArrayList<>();
}

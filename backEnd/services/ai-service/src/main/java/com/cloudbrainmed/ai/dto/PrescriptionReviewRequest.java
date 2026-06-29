package com.cloudbrainmed.ai.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
public class PrescriptionReviewRequest {

    @NotBlank
    @Size(max = 32)
    private String registerId;

    @Size(max = 10000)
    private String currentRecordDesc;

    @Size(max = 30)
    private Map<String, String> patientInformation =
            new LinkedHashMap<>();

    @Valid
    @NotEmpty
    @Size(max = 10)
    private List<PrescriptionReviewMedicineRequest> medicines =
            new ArrayList<>();
}

package com.cloudbrainmed.ai.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PrescriptionReviewMedicineRequest {

    @NotBlank
    @Size(max = 32)
    private String medicineId;

    @NotBlank
    @Size(max = 200)
    private String usage;

    @Min(1)
    @Max(10000)
    @NotNull
    private Integer quantity;
}

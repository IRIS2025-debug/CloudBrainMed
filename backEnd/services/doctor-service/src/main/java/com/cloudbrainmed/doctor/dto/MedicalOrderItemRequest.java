package com.cloudbrainmed.doctor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MedicalOrderItemRequest {

    @NotBlank
    @Size(max = 50)
    private String itemCode;

    @Size(max = 20)
    private String urgencyLevel;
}

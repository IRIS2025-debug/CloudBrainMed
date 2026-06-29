package com.cloudbrainmed.doctor.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class MedicalOrderConfirmRequest {

    @NotBlank
    @Size(max = 32)
    private String registerId;

    @Size(max = 64)
    private String aiTraceId;

    @NotBlank
    @Size(max = 10000)
    private String clinicalSummary;

    @Size(max = 20)
    private String urgencyLevel;

    @Valid
    @NotEmpty
    @Size(max = 5)
    private List<MedicalOrderItemRequest> items = new ArrayList<>();
}

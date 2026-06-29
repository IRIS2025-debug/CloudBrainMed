package com.cloudbrainmed.doctor.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RecordConfirmRequest {

    @NotBlank
    private String registerId;

    @NotBlank
    private String recordDesc;
}

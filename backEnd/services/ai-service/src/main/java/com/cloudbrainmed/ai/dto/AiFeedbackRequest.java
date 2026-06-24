package com.cloudbrainmed.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AiFeedbackRequest {

    @NotBlank
    @Size(max = 64)
    private String traceId;

    @NotBlank
    @Size(max = 10000)
    private String finalRecordDesc;

    @NotBlank
    @Pattern(regexp = "FULL|PARTIAL|REJECTED")
    private String adoptionType;
}

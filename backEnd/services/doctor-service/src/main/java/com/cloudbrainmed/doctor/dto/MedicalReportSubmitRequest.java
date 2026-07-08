package com.cloudbrainmed.doctor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MedicalReportSubmitRequest {

    @NotBlank
    @Size(max = 32)
    private String orderItemId;

    @Size(max = 10000)
    private String resultSummary;

    @Size(max = 10000)
    private String conclusion;

    @Size(max = 20)
    private String abnormalFlag;

    @Size(max = 255)
    private String attachmentUrl;

    @Size(max = 200000)
    private String aiResultJson;
}

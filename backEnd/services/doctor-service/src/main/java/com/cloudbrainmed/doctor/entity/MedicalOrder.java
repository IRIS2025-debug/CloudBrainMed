package com.cloudbrainmed.doctor.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MedicalOrder {
    private String orderId;
    private String patientId;
    private String registerId;
    private String doctorId;
    private String clinicalSummary;
    private String urgencyLevel;
    private String sourceType;
    private String aiTraceId;
    private String status;
    private String payStatus;
    private LocalDateTime confirmedTime;
    private LocalDateTime createTime;
}

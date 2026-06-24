package com.cloudbrainmed.doctor.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class MedicalOrderConfirmResponse {
    private String orderId;
    private String sourceType;
    private int itemCount;
    private BigDecimal totalAmount;
}

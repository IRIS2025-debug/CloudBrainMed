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
    private String status;
    private String payStatus;
    private boolean queueReady;
    private String paymentMessage;

    public MedicalOrderConfirmResponse(
            String orderId,
            String sourceType,
            int itemCount,
            BigDecimal totalAmount) {
        this(orderId, sourceType, itemCount, totalAmount,
                "WAITING_ASSIGN", "WAITING", false, null);
    }
}

package com.cloudbrainmed.payment.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付表实体
 */
@Data
public class Pay {
    private String payId;
    private String orderType;      // REGISTER / MEDICAL / PRESCRIPTION
    private String businessId;     // 关联业务ID
    private String patientId;
    private String patientName;
    private BigDecimal totalAmount;
    private String payStatus;      // WAITING / PAID / CANCELLED / REFUNDED
    private LocalDateTime payTime; // 未支付时=创建时间，已支付时=支付完成时间
}
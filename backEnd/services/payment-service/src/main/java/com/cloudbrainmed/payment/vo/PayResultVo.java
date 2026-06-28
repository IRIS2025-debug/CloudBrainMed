package com.cloudbrainmed.payment.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PayResultVo {
    private String payId;
    private String orderType;
    private String businessId;
    private String patientId;
    private String patientName;
    private BigDecimal totalAmount;
    private String payStatus;
    private String payStatusText;
    private LocalDateTime payTime;  // 统一使用 payTime
    private String description;
}
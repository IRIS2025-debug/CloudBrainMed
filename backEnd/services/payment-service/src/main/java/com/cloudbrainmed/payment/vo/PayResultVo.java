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
    private String businessStatus;      // 业务状态
    private Boolean canRefund;          // 是否可退款
    private Boolean isExpired;          // 是否过期
    private LocalDateTime visitDate;    // 就诊日期（用于前端判断）

}
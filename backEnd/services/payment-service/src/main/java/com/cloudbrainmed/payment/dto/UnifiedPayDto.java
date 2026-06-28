package com.cloudbrainmed.payment.dto;

import java.math.BigDecimal;

/**
 * 统一支付请求DTO
 */
public class UnifiedPayDto {
    private String patientId;
    private String patientName;
    private String orderType;      // REGISTER / MEDICAL / PRESCRIPTION
    private String businessId;     // 挂号ID/检查ID/处方ID
    private String description;    // 支付描述
    private BigDecimal amount;
    private String payMethod;      // WECHAT / ALIPAY / BALANCE

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }
    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }
    public String getOrderType() { return orderType; }
    public void setOrderType(String orderType) { this.orderType = orderType; }
    public String getBusinessId() { return businessId; }
    public void setBusinessId(String businessId) { this.businessId = businessId; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getPayMethod() { return payMethod; }
    public void setPayMethod(String payMethod) { this.payMethod = payMethod; }
}
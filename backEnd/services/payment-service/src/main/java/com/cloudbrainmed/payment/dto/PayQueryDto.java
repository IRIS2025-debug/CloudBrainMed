package com.cloudbrainmed.payment.dto;

/**
 * 支付查询DTO
 */
public class PayQueryDto {
    private String patientId;
    private String payStatus;      // 可选：WAITING / PAID / CANCELLED / REFUNDED
    private String orderType;      // 可选：REGISTER / MEDICAL / PRESCRIPTION
    private Integer pageNum = 1;
    private Integer pageSize = 10;

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }
    public String getPayStatus() { return payStatus; }
    public void setPayStatus(String payStatus) { this.payStatus = payStatus; }
    public String getOrderType() { return orderType; }
    public void setOrderType(String orderType) { this.orderType = orderType; }
    public Integer getPageNum() { return pageNum; }
    public void setPageNum(Integer pageNum) { this.pageNum = pageNum; }
    public Integer getPageSize() { return pageSize; }
    public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }
}

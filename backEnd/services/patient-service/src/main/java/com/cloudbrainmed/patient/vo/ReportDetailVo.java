package com.cloudbrainmed.patient.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 报告详情VO
 */
public class ReportDetailVo {
    /**
     * 报告ID
     */
    private String reportId;

    /**
     * 申请明细ID
     */
    private String orderItemId;

    /**
     * 申请ID
     */
    private String orderId;

    /**
     * 患者ID
     */
    private String patientId;

    /**
     * 患者姓名
     */
    private String patientName;

    /**
     * 医生姓名
     */
    private String doctorName;

    /**
     * 挂号ID
     */
    private String registerId;

    /**
     * 项目编码
     */
    private String itemCode;

    /**
     * 项目名称
     */
    private String itemName;

    /**
     * 项目类别
     */
    private String itemCategory;

    /**
     * 价格
     */
    private BigDecimal price;

    /**
     * 紧急等级
     */
    private String urgencyLevel;

    /**
     * 结果摘要
     */
    private String resultSummary;

    /**
     * 报告结论
     */
    private String conclusion;

    /**
     * 异常标记
     */
    private String abnormalFlag;

    /**
     * 附件URL
     */
    private String attachmentUrl;

    /**
     * 报告状态
     */
    private String status;

    /**
     * 执行时间
     */
    private LocalDateTime performedTime;

    /**
     * 报告时间
     */
    private LocalDateTime reportTime;

    /**
     * 检验指标列表（仅检验项目有）
     */
    private List<IndicatorVo> indicators;

    // Getters and Setters
    public String getReportId() {
        return reportId;
    }

    public void setReportId(String reportId) {
        this.reportId = reportId;
    }

    public String getOrderItemId() {
        return orderItemId;
    }

    public void setOrderItemId(String orderItemId) {
        this.orderItemId = orderItemId;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }

    public String getRegisterId() {
        return registerId;
    }

    public void setRegisterId(String registerId) {
        this.registerId = registerId;
    }

    public String getItemCode() {
        return itemCode;
    }

    public void setItemCode(String itemCode) {
        this.itemCode = itemCode;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getItemCategory() {
        return itemCategory;
    }

    public void setItemCategory(String itemCategory) {
        this.itemCategory = itemCategory;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getUrgencyLevel() {
        return urgencyLevel;
    }

    public void setUrgencyLevel(String urgencyLevel) {
        this.urgencyLevel = urgencyLevel;
    }

    public String getResultSummary() {
        return resultSummary;
    }

    public void setResultSummary(String resultSummary) {
        this.resultSummary = resultSummary;
    }

    public String getConclusion() {
        return conclusion;
    }

    public void setConclusion(String conclusion) {
        this.conclusion = conclusion;
    }

    public String getAbnormalFlag() {
        return abnormalFlag;
    }

    public void setAbnormalFlag(String abnormalFlag) {
        this.abnormalFlag = abnormalFlag;
    }

    public String getAttachmentUrl() {
        return attachmentUrl;
    }

    public void setAttachmentUrl(String attachmentUrl) {
        this.attachmentUrl = attachmentUrl;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getPerformedTime() {
        return performedTime;
    }

    public void setPerformedTime(LocalDateTime performedTime) {
        this.performedTime = performedTime;
    }

    public LocalDateTime getReportTime() {
        return reportTime;
    }

    public void setReportTime(LocalDateTime reportTime) {
        this.reportTime = reportTime;
    }

    public List<IndicatorVo> getIndicators() {
        return indicators;
    }

    public void setIndicators(List<IndicatorVo> indicators) {
        this.indicators = indicators;
    }
}

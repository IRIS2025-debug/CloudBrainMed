package com.cloudbrainmed.patient.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 检查/检验申请明细VO
 */
public class MedicalOrderItemVo {
    /**
     * 申请明细ID
     */
    private String orderItemId;

    /**
     * 申请ID
     */
    private String orderId;

    /**
     * 项目ID
     */
    private String itemId;

    /**
     * 项目编码
     */
    private String itemCode;

    /**
     * 项目名称
     */
    private String itemName;

    /**
     * 项目类别：EXAM=检查，LAB=检验
     */
    private String itemCategory;

    /**
     * 紧急等级
     */
    private String urgencyLevel;

    /**
     * 价格
     */
    private BigDecimal price;

    /**
     * 明细状态（展示值）
     */
    private String status;

    /**
     * 明细原始状态码
     */
    private String rawStatus;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 报告ID
     */
    private String reportId;

    /**
     * 报告状态
     */
    private String reportStatus;

    /**
     * 报告结论
     */
    private String conclusion;

    /**
     * 异常标记
     */
    private String abnormalFlag;

    /**
     * 报告时间
     */
    private LocalDateTime reportTime;

    /**
     * 随访建议
     */
    private String followUpAdvice;

    /**
     * 排队人数
     */
    private Integer queueCount;

    /**
     * 预计等待分钟数
     */
    private Integer estimatedWaitMinutes;

    /**
     * 检查室ID
     */
    private String roomId;

    /**
     * 检查室名称
     */
    private String roomName;

    // Getters and Setters
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

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
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

    public String getUrgencyLevel() {
        return urgencyLevel;
    }

    public void setUrgencyLevel(String urgencyLevel) {
        this.urgencyLevel = urgencyLevel;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRawStatus() {
        return rawStatus;
    }

    public void setRawStatus(String rawStatus) {
        this.rawStatus = rawStatus;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public String getReportId() {
        return reportId;
    }

    public void setReportId(String reportId) {
        this.reportId = reportId;
    }

    public String getReportStatus() {
        return reportStatus;
    }

    public void setReportStatus(String reportStatus) {
        this.reportStatus = reportStatus;
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

    public LocalDateTime getReportTime() {
        return reportTime;
    }

    public void setReportTime(LocalDateTime reportTime) {
        this.reportTime = reportTime;
    }

    public String getFollowUpAdvice() {
        return followUpAdvice;
    }

    public void setFollowUpAdvice(String followUpAdvice) {
        this.followUpAdvice = followUpAdvice;
    }

    public Integer getQueueCount() {
        return queueCount;
    }

    public void setQueueCount(Integer queueCount) {
        this.queueCount = queueCount;
    }

    public Integer getEstimatedWaitMinutes() {
        return estimatedWaitMinutes;
    }

    public void setEstimatedWaitMinutes(Integer estimatedWaitMinutes) {
        this.estimatedWaitMinutes = estimatedWaitMinutes;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }
}

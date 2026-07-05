// src/main/java/com/cloudbrainmed/patient/entity/MedicalReportIndicator.java
package com.cloudbrainmed.patient.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("medical_report_indicator")
public class MedicalReportIndicator {
    @TableId(type = IdType.INPUT)
    private String indicatorId;
    private String reportId;
    private String indicatorCode;
    private String indicatorName;
    private String resultValue;
    private String unit;
    private BigDecimal referenceMin;
    private BigDecimal referenceMax;
    private String referenceText;
    private String abnormalFlag;
    private Integer sortNo;
    private LocalDateTime createTime;

    // Getters and Setters
    public String getIndicatorId() { return indicatorId; }
    public void setIndicatorId(String indicatorId) { this.indicatorId = indicatorId; }
    public String getReportId() { return reportId; }
    public void setReportId(String reportId) { this.reportId = reportId; }
    public String getIndicatorCode() { return indicatorCode; }
    public void setIndicatorCode(String indicatorCode) { this.indicatorCode = indicatorCode; }
    public String getIndicatorName() { return indicatorName; }
    public void setIndicatorName(String indicatorName) { this.indicatorName = indicatorName; }
    public String getResultValue() { return resultValue; }
    public void setResultValue(String resultValue) { this.resultValue = resultValue; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public BigDecimal getReferenceMin() { return referenceMin; }
    public void setReferenceMin(BigDecimal referenceMin) { this.referenceMin = referenceMin; }
    public BigDecimal getReferenceMax() { return referenceMax; }
    public void setReferenceMax(BigDecimal referenceMax) { this.referenceMax = referenceMax; }
    public String getReferenceText() { return referenceText; }
    public void setReferenceText(String referenceText) { this.referenceText = referenceText; }
    public String getAbnormalFlag() { return abnormalFlag; }
    public void setAbnormalFlag(String abnormalFlag) { this.abnormalFlag = abnormalFlag; }
    public Integer getSortNo() { return sortNo; }
    public void setSortNo(Integer sortNo) { this.sortNo = sortNo; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
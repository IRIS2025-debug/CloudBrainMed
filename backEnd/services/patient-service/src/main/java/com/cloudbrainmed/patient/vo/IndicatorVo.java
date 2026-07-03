package com.cloudbrainmed.patient.vo;

import java.math.BigDecimal;

/**
 * 检验指标VO
 */
public class IndicatorVo {
    /**
     * 指标ID
     */
    private String indicatorId;

    /**
     * 指标编码
     */
    private String indicatorCode;

    /**
     * 指标名称
     */
    private String indicatorName;

    /**
     * 检验结果值
     */
    private String resultValue;

    /**
     * 单位
     */
    private String unit;

    /**
     * 参考下限
     */
    private BigDecimal referenceMin;

    /**
     * 参考上限
     */
    private BigDecimal referenceMax;

    /**
     * 文本参考范围
     */
    private String referenceText;

    /**
     * 异常标记
     */
    private String abnormalFlag;

    /**
     * 排序号
     */
    private Integer sortNo;

    // Getters and Setters
    public String getIndicatorId() {
        return indicatorId;
    }

    public void setIndicatorId(String indicatorId) {
        this.indicatorId = indicatorId;
    }

    public String getIndicatorCode() {
        return indicatorCode;
    }

    public void setIndicatorCode(String indicatorCode) {
        this.indicatorCode = indicatorCode;
    }

    public String getIndicatorName() {
        return indicatorName;
    }

    public void setIndicatorName(String indicatorName) {
        this.indicatorName = indicatorName;
    }

    public String getResultValue() {
        return resultValue;
    }

    public void setResultValue(String resultValue) {
        this.resultValue = resultValue;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public BigDecimal getReferenceMin() {
        return referenceMin;
    }

    public void setReferenceMin(BigDecimal referenceMin) {
        this.referenceMin = referenceMin;
    }

    public BigDecimal getReferenceMax() {
        return referenceMax;
    }

    public void setReferenceMax(BigDecimal referenceMax) {
        this.referenceMax = referenceMax;
    }

    public String getReferenceText() {
        return referenceText;
    }

    public void setReferenceText(String referenceText) {
        this.referenceText = referenceText;
    }

    public String getAbnormalFlag() {
        return abnormalFlag;
    }

    public void setAbnormalFlag(String abnormalFlag) {
        this.abnormalFlag = abnormalFlag;
    }

    public Integer getSortNo() {
        return sortNo;
    }

    public void setSortNo(Integer sortNo) {
        this.sortNo = sortNo;
    }
}

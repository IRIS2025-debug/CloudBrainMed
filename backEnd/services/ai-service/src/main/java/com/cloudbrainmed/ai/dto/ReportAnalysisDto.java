package com.cloudbrainmed.ai.dto;

import java.util.List;
import java.util.Map;

public class ReportAnalysisDto {
    private String registerId;
    private String reportType;
    private String reportText;
    private List<IndicatorDto> indicators;
    private Map<String, Object> reportInput;

    public String getRegisterId() { return registerId; }
    public void setRegisterId(String registerId) { this.registerId = registerId; }
    public String getReportType() { return reportType; }
    public void setReportType(String reportType) { this.reportType = reportType; }
    public String getReportText() { return reportText; }
    public void setReportText(String reportText) { this.reportText = reportText; }
    public List<IndicatorDto> getIndicators() { return indicators; }
    public void setIndicators(List<IndicatorDto> indicators) { this.indicators = indicators; }
    public Map<String, Object> getReportInput() { return reportInput; }
    public void setReportInput(Map<String, Object> reportInput) { this.reportInput = reportInput; }

    public static class IndicatorDto {
        private String name;
        private String value;
        private String unit;
        private String referenceRange;
        private String abnormalFlag;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
        public String getUnit() { return unit; }
        public void setUnit(String unit) { this.unit = unit; }
        public String getReferenceRange() { return referenceRange; }
        public void setReferenceRange(String referenceRange) { this.referenceRange = referenceRange; }
        public String getAbnormalFlag() { return abnormalFlag; }
        public void setAbnormalFlag(String abnormalFlag) { this.abnormalFlag = abnormalFlag; }
    }
}

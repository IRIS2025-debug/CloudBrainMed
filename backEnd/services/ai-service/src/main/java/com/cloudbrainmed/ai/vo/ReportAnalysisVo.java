package com.cloudbrainmed.ai.vo;

import java.util.List;

public class ReportAnalysisVo {
    private String summary;
    private String riskLevel;
    private List<AbnormalIndicatorVo> abnormalIndicators;
    private List<String> suggestions;
    private String followUpAdvice;
    private Boolean fallback;

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public List<AbnormalIndicatorVo> getAbnormalIndicators() { return abnormalIndicators; }
    public void setAbnormalIndicators(List<AbnormalIndicatorVo> abnormalIndicators) { this.abnormalIndicators = abnormalIndicators; }
    public List<String> getSuggestions() { return suggestions; }
    public void setSuggestions(List<String> suggestions) { this.suggestions = suggestions; }
    public String getFollowUpAdvice() { return followUpAdvice; }
    public void setFollowUpAdvice(String followUpAdvice) { this.followUpAdvice = followUpAdvice; }
    public Boolean getFallback() { return fallback; }
    public void setFallback(Boolean fallback) { this.fallback = fallback; }

    public static class AbnormalIndicatorVo {
        private String name;
        private String value;
        private String referenceRange;
        private String interpretation;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
        public String getReferenceRange() { return referenceRange; }
        public void setReferenceRange(String referenceRange) { this.referenceRange = referenceRange; }
        public String getInterpretation() { return interpretation; }
        public void setInterpretation(String interpretation) { this.interpretation = interpretation; }
    }
}

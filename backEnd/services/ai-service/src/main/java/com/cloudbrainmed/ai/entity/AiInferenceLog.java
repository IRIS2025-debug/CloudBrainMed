package com.cloudbrainmed.ai.entity;

import java.time.LocalDateTime;

/**
 * AI 推理审计日志实体
 */
public class AiInferenceLog {
    private String logId;
    private String traceId;
    private String callSource;
    private String modelKey;
    private String modelVersion;
    private String inputSummary;
    private String outputSummary;
    private String status;
    private Integer durationMs;
    private LocalDateTime createdAt;
    private String patientId;

    public String getLogId() { return logId; }
    public void setLogId(String logId) { this.logId = logId; }
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
    public String getCallSource() { return callSource; }
    public void setCallSource(String callSource) { this.callSource = callSource; }
    public String getModelKey() { return modelKey; }
    public void setModelKey(String modelKey) { this.modelKey = modelKey; }
    public String getModelVersion() { return modelVersion; }
    public void setModelVersion(String modelVersion) { this.modelVersion = modelVersion; }
    public String getInputSummary() { return inputSummary; }
    public void setInputSummary(String inputSummary) { this.inputSummary = inputSummary; }
    public String getOutputSummary() { return outputSummary; }
    public void setOutputSummary(String outputSummary) { this.outputSummary = outputSummary; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getDurationMs() { return durationMs; }
    public void setDurationMs(Integer durationMs) { this.durationMs = durationMs; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }
}

package com.cloudbrainmed.ai.entity;

import java.time.LocalDateTime;

/**
 * AI 推理审计日志实体
 */
public class AiInferenceLog {
    private String logId;
    private String inferenceType;
    private String status;
    private String modelId;
    private Long latencyMs;
    private String message;
    private LocalDateTime createTime;

    public String getLogId() { return logId; }
    public void setLogId(String logId) { this.logId = logId; }
    public String getInferenceType() { return inferenceType; }
    public void setInferenceType(String inferenceType) { this.inferenceType = inferenceType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getModelId() { return modelId; }
    public void setModelId(String modelId) { this.modelId = modelId; }
    public Long getLatencyMs() { return latencyMs; }
    public void setLatencyMs(Long latencyMs) { this.latencyMs = latencyMs; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}

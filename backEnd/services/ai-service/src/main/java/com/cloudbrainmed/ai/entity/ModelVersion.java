package com.cloudbrainmed.ai.entity;

import java.time.LocalDateTime;

/**
 * AI 模型版本注册表实体
 */
public class ModelVersion {
    private String modelId;
    private String modelKey;
    private String modelType;
    private String version;
    private String artifactPath;
    private String hyperParams;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime activatedAt;

    public String getModelId() { return modelId; }
    public void setModelId(String modelId) { this.modelId = modelId; }
    public String getModelKey() { return modelKey; }
    public void setModelKey(String modelKey) { this.modelKey = modelKey; }
    public String getModelType() { return modelType; }
    public void setModelType(String modelType) { this.modelType = modelType; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public String getArtifactPath() { return artifactPath; }
    public void setArtifactPath(String artifactPath) { this.artifactPath = artifactPath; }
    public String getHyperParams() { return hyperParams; }
    public void setHyperParams(String hyperParams) { this.hyperParams = hyperParams; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getActivatedAt() { return activatedAt; }
    public void setActivatedAt(LocalDateTime activatedAt) { this.activatedAt = activatedAt; }
}

package com.cloudbrainmed.ai.entity;

import java.time.LocalDateTime;

public class TrainingTask {
    private String taskId;
    private String modelKey;
    private String modelType;
    private String hyperParams;
    private String datasetPath;
    private String status;
    private String modelId;
    private String errorMessage;
    private LocalDateTime createTime;
    private LocalDateTime startTime;
    private LocalDateTime completedTime;

    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }
    public String getModelKey() { return modelKey; }
    public void setModelKey(String modelKey) { this.modelKey = modelKey; }
    public String getModelType() { return modelType; }
    public void setModelType(String modelType) { this.modelType = modelType; }
    public String getHyperParams() { return hyperParams; }
    public void setHyperParams(String hyperParams) { this.hyperParams = hyperParams; }
    public String getDatasetPath() { return datasetPath; }
    public void setDatasetPath(String datasetPath) { this.datasetPath = datasetPath; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getModelId() { return modelId; }
    public void setModelId(String modelId) { this.modelId = modelId; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getCompletedTime() { return completedTime; }
    public void setCompletedTime(LocalDateTime completedTime) { this.completedTime = completedTime; }
}

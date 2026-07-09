package com.cloudbrainmed.ai.entity;

import java.time.LocalDateTime;

/**
 * 训练样本实体
 */
public class TrainingSample {
    private String sampleId;
    private String datasetName;
    private String filePath;
    private String label;
    private String labelType;
    private String status;
    private LocalDateTime createTime;

    public String getSampleId() { return sampleId; }
    public void setSampleId(String sampleId) { this.sampleId = sampleId; }
    public String getDatasetName() { return datasetName; }
    public void setDatasetName(String datasetName) { this.datasetName = datasetName; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public String getLabelType() { return labelType; }
    public void setLabelType(String labelType) { this.labelType = labelType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}

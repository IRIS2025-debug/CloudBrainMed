package com.cloudbrainmed.ai.service;

import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

public interface MlOpsService {
    /** 推理统计 */
    Map<String, Object> getInferenceStats();
    /** 模型统计 */
    Map<String, Object> getModelStats();
    /** 推理日志分页 */
    Map<String, Object> getInferenceLogs(int page, int limit);
    /** 样本列表分页 */
    Map<String, Object> getSampleList(int page, int limit);
    /** 更新样本标注 */
    void updateSample(String sampleId, String label, String labelType);
    /** 模型列表 */
    List<Map<String, Object>> getModelList();
    /** 触发训练 */
    Map<String, Object> triggerTrain(Map<String, String> params);
    Map<String, Object> setModelTraffic(String modelId, int trafficPct);
    /** 训练任务列表 */
    Map<String, Object> getTrainingTasks();
    /** Python 服务健康检查 */
    Map<String, Object> checkPythonService();
    Map<String, Object> predictCtArtifact(MultipartFile file) throws Exception;
    ResponseEntity<byte[]> downloadCtArtifactMask(String maskFilename) throws Exception;
}

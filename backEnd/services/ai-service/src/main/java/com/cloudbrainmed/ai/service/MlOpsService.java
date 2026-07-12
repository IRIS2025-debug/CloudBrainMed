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
    /** 模型列表（固定两个业务模型，版本/状态来自 Python 健康响应） */
    List<Map<String, Object>> getModelList();
    /** Python 服务健康检查 */
    Map<String, Object> checkPythonService();
    Map<String, Object> predictCtArtifact(MultipartFile file) throws Exception;
    ResponseEntity<byte[]> downloadCtArtifactMask(String maskFilename) throws Exception;
    ResponseEntity<byte[]> downloadCtArtifactPreview(String previewFilename) throws Exception;
    Map<String, Object> predictCtLesion(MultipartFile file) throws Exception;
    ResponseEntity<byte[]> downloadCtLesionMask(String maskFilename) throws Exception;
    ResponseEntity<byte[]> downloadCtLesionPreview(String previewFilename) throws Exception;
}

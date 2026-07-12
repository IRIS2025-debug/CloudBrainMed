package com.cloudbrainmed.ai.service.impl;

import com.cloudbrainmed.ai.entity.ModelVersion;
import com.cloudbrainmed.ai.mapper.AiInferenceLogMapper;
import com.cloudbrainmed.ai.mapper.ModelVersionMapper;
import com.cloudbrainmed.ai.model.InferenceEngine;
import com.cloudbrainmed.ai.model.ModelLoader;
import com.cloudbrainmed.ai.service.MlOpsService;
import com.cloudbrainmed.common.exception.BusinessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@Service
public class MlOpsServiceImpl implements MlOpsService {

    /** 看板固定展示的两个业务模型，与两条 CT 推理链路一一对应。 */
    private static final String ARTIFACT_MODEL_KEY = "ct-artifact-model";
    private static final String LESION_MODEL_KEY = "ct-lesion-model";
    private static final String STATUS_READY = "READY";
    private static final String STATUS_FALLBACK = "FALLBACK";
    private static final String STATUS_OFFLINE = "OFFLINE";

    private final AiInferenceLogMapper inferenceLogMapper;
    private final ModelVersionMapper modelVersionMapper;
    private final ModelLoader modelLoader;
    private final InferenceEngine inferenceEngine;

    public MlOpsServiceImpl(AiInferenceLogMapper inferenceLogMapper,
                             ModelVersionMapper modelVersionMapper,
                             ModelLoader modelLoader,
                             InferenceEngine inferenceEngine) {
        this.inferenceLogMapper = inferenceLogMapper;
        this.modelVersionMapper = modelVersionMapper;
        this.modelLoader = modelLoader;
        this.inferenceEngine = inferenceEngine;
    }

    @Override
    public Map<String, Object> getInferenceStats() {
        return Map.of(
            "todayTotal", inferenceLogMapper.countToday(),
            "successRate", computeSuccessRate(),
            "avgLatency", Math.round(inferenceLogMapper.avgLatency())
        );
    }

    @Override
    public Map<String, Object> getModelStats() {
        return Map.of(
            "activeModels", modelVersionMapper.countByStatus("ACTIVE"),
            "totalInference", inferenceLogMapper.countAll()
        );
    }

    @Override
    public Map<String, Object> getInferenceLogs(int page, int limit) {
        validatePagination(page, limit);
        int offset = (page - 1) * limit;
        return Map.of(
            "list", inferenceLogMapper.selectPage(offset, limit),
            "total", inferenceLogMapper.countAll()
        );
    }

    @Override
    public List<Map<String, Object>> getModelList() {
        // 模型列表恒为两个业务模型，版本/状态取自 Python 服务健康响应，不再暴露 ai_model_registry 历史记录。
        Map<String, Object> health = inferenceEngine.getPythonHealth();
        List<Map<String, Object>> result = new ArrayList<>();

        if (health == null) {
            // Python 不可达时仍展示两个模型，版本 "--"、状态离线。
            result.add(buildModel(ARTIFACT_MODEL_KEY, "CT 金属伪影识别", "CT_ARTIFACT", "--", STATUS_OFFLINE));
            result.add(buildModel(LESION_MODEL_KEY, "CT 病灶识别/分割", "CT_LESION", "--", STATUS_OFFLINE));
            return result;
        }

        String artifactVersion = text(health.get("model_version"), "--");
        String artifactType = text(health.get("model_type"), "CT_ARTIFACT");
        result.add(buildModel(ARTIFACT_MODEL_KEY, "CT 金属伪影识别", artifactType, artifactVersion, STATUS_READY));

        String lesionVersion = text(health.get("lesion_model_version"), "--");
        String lesionType = text(health.get("lesion_model_type"), "CT_LESION");
        boolean lesionFallback = Boolean.TRUE.equals(health.get("lesion_fallback"));
        result.add(buildModel(LESION_MODEL_KEY, "CT 病灶识别/分割", lesionType, lesionVersion,
                lesionFallback ? STATUS_FALLBACK : STATUS_READY));
        return result;
    }

    private Map<String, Object> buildModel(String modelKey, String displayName,
                                           String modelType, String version, String status) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("modelKey", modelKey);
        item.put("displayName", displayName);
        item.put("modelType", modelType);
        item.put("version", version);
        item.put("status", status);
        return item;
    }

    @Override
    public Map<String, Object> checkPythonService() {
        boolean alive = inferenceEngine.isPythonServiceAlive();
        ModelVersion active = modelLoader.getActiveModel();
        return Map.of(
            "pythonServiceAlive", alive,
            "activeModel", active != null ? active.getModelKey() + " v" + active.getVersion() : "无"
        );
    }

    @Override
    public Map<String, Object> predictCtArtifact(MultipartFile file) throws Exception {
        return inferenceEngine.predictArtifact(file);
    }

    @Override
    public Map<String, Object> predictCtLesion(MultipartFile file) throws Exception {
        return inferenceEngine.predictLesion(file);
    }

    @Override
    public ResponseEntity<byte[]> downloadCtArtifactMask(String maskFilename) throws Exception {
        byte[] body = inferenceEngine.downloadMask(maskFilename);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + maskFilename + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(body);
    }

    @Override
    public ResponseEntity<byte[]> downloadCtArtifactPreview(String previewFilename) throws Exception {
        byte[] body = inferenceEngine.downloadPreview(previewFilename);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + previewFilename + "\"")
                .contentType(MediaType.IMAGE_PNG)
                .body(body);
    }

    @Override
    public ResponseEntity<byte[]> downloadCtLesionMask(String maskFilename) throws Exception {
        byte[] body = inferenceEngine.downloadMask(maskFilename);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + maskFilename + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(body);
    }

    @Override
    public ResponseEntity<byte[]> downloadCtLesionPreview(String previewFilename) throws Exception {
        byte[] body = inferenceEngine.downloadPreview(previewFilename);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + previewFilename + "\"")
                .contentType(MediaType.IMAGE_PNG)
                .body(body);
    }

    private double computeSuccessRate() {
        int total = inferenceLogMapper.countAll();
        if (total == 0) return 100.0;
        int success = inferenceLogMapper.countByStatus("SUCCESS");
        return Math.round(success * 1000.0 / total) / 10.0;
    }

    private void validatePagination(int page, int limit) {
        if (page < 1 || limit < 1) {
            throw new BusinessException("分页参数错误");
        }
    }

    private String text(Object value, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        String s = String.valueOf(value).trim();
        return s.isEmpty() ? defaultValue : s;
    }
}

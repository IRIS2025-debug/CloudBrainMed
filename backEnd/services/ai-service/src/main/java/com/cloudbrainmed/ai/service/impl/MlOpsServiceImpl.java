package com.cloudbrainmed.ai.service.impl;

import com.cloudbrainmed.ai.mapper.AiInferenceLogMapper;
import com.cloudbrainmed.ai.model.InferenceEngine;
import com.cloudbrainmed.ai.service.MlOpsService;
import com.cloudbrainmed.common.exception.BusinessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Service
public class MlOpsServiceImpl implements MlOpsService {

    private final AiInferenceLogMapper inferenceLogMapper;
    private final InferenceEngine inferenceEngine;

    public MlOpsServiceImpl(AiInferenceLogMapper inferenceLogMapper,
                            InferenceEngine inferenceEngine) {
        this.inferenceLogMapper = inferenceLogMapper;
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
                "activeModels", 0,
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
    public Map<String, Object> checkPythonService() {
        return Map.of(
                "pythonServiceAlive", inferenceEngine.isPythonServiceAlive(),
                "activeModel", "disabled"
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
        return fileDownload(inferenceEngine.downloadMask(maskFilename), maskFilename, MediaType.APPLICATION_OCTET_STREAM);
    }

    @Override
    public ResponseEntity<byte[]> downloadCtArtifactPreview(String previewFilename) throws Exception {
        return fileDownload(inferenceEngine.downloadPreview(previewFilename), previewFilename, MediaType.IMAGE_PNG);
    }

    @Override
    public ResponseEntity<byte[]> downloadCtLesionMask(String maskFilename) throws Exception {
        return fileDownload(inferenceEngine.downloadMask(maskFilename), maskFilename, MediaType.APPLICATION_OCTET_STREAM);
    }

    @Override
    public ResponseEntity<byte[]> downloadCtLesionPreview(String previewFilename) throws Exception {
        return fileDownload(inferenceEngine.downloadPreview(previewFilename), previewFilename, MediaType.IMAGE_PNG);
    }

    private ResponseEntity<byte[]> fileDownload(byte[] body, String filename, MediaType contentType) {
        String disposition = MediaType.IMAGE_PNG.equals(contentType) ? "inline" : "attachment";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition + "; filename=\"" + filename + "\"")
                .contentType(contentType)
                .body(body);
    }

    private double computeSuccessRate() {
        int total = inferenceLogMapper.countAll();
        if (total == 0) {
            return 100.0;
        }
        int success = inferenceLogMapper.countByStatus("SUCCESS");
        return Math.round(success * 1000.0 / total) / 10.0;
    }

    private void validatePagination(int page, int limit) {
        if (page < 1 || limit < 1) {
            throw new BusinessException("分页参数错误");
        }
    }
}

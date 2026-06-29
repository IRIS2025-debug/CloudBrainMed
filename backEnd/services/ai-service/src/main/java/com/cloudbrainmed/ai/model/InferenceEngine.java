package com.cloudbrainmed.ai.model;

import com.cloudbrainmed.ai.entity.AiInferenceLog;
import com.cloudbrainmed.ai.entity.ModelVersion;
import com.cloudbrainmed.ai.mapper.AiInferenceLogMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * CT 伪影检测推理引擎
 * 通过 HTTP 调用 Python FastAPI 推理微服务（CTDetectionServer.py）
 *
 * 架构: Java → HTTP → Python/FastAPI → PyTorch/UNet → 返回掩码+特征向量
 */
@Component
public class InferenceEngine {

    private static final Logger log = LoggerFactory.getLogger(InferenceEngine.class);

    /** Python 推理服务默认地址 */
    private String pythonServiceUrl = "http://localhost:8000";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final AiInferenceLogMapper inferenceLogMapper;
    private final ModelLoader modelLoader;

    public InferenceEngine(AiInferenceLogMapper inferenceLogMapper, ModelLoader modelLoader) {
        this.httpClient = HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(30)).build();
        this.objectMapper = new ObjectMapper();
        this.inferenceLogMapper = inferenceLogMapper;
        this.modelLoader = modelLoader;
    }

    public void setPythonServiceUrl(String url) {
        this.pythonServiceUrl = url;
    }

    /**
     * CT 伪影检测：上传 NIfTI 文件 → 返回掩码结果
     */
    public Map<String, Object> predictArtifact(MultipartFile niftiFile) throws IOException, InterruptedException {
        long startTime = System.currentTimeMillis();
        String logId = "INF" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        // 1. 保存临时文件
        Path tempFile = Files.createTempFile("ct_", niftiFile.getOriginalFilename());
        niftiFile.transferTo(tempFile.toFile());

        try {
            // 2. 构建 multipart 请求
            String boundary = "----CTBoundary" + System.currentTimeMillis();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(pythonServiceUrl + "/predict-ct-artifact"))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofFile(tempFile))
                    .timeout(java.time.Duration.ofMinutes(5))
                    .build();

            // 3. 调用 Python 推理服务
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            long latency = System.currentTimeMillis() - startTime;

            if (response.statusCode() != 200) {
                log.error("推理失败: HTTP {} → {}", response.statusCode(), response.body());
                saveInferenceLog(logId, "FAILED", null, latency, "HTTP " + response.statusCode());
                return Map.of("status", "failed", "error", response.body());
            }

            // 4. 解析结果
            @SuppressWarnings("unchecked")
            Map<String, Object> result = objectMapper.readValue(response.body(), Map.class);
            result.put("logId", logId);
            result.put("latencyMs", latency);

            // 5. 记录推理日志
            ModelVersion activeModel = modelLoader.getActiveModel();
            saveInferenceLog(logId, "SUCCESS",
                    activeModel != null ? activeModel.getModelId() : null,
                    latency, "检测完成");

            log.info("推理成功: {} → 耗时 {}ms", niftiFile.getOriginalFilename(), latency);
            return result;

        } catch (Exception e) {
            long latency = System.currentTimeMillis() - startTime;
            log.error("推理异常: {}", e.getMessage());
            saveInferenceLog(logId, "ERROR", null, latency, e.getMessage());
            return Map.of("status", "error", "message", e.getMessage());
        } finally {
            // 清理临时文件
            try { Files.deleteIfExists(tempFile); } catch (IOException ignored) {}
        }
    }

    /**
     * 提取 CT 图像的融合特征向量
     */
    public Map<String, Object> extractFeatures(MultipartFile niftiFile) throws IOException, InterruptedException {
        Path tempFile = Files.createTempFile("ct_feat_", niftiFile.getOriginalFilename());
        niftiFile.transferTo(tempFile.toFile());

        try {
            String boundary = "----CTBoundary" + System.currentTimeMillis();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(pythonServiceUrl + "/extract-features"))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofFile(tempFile))
                    .timeout(java.time.Duration.ofMinutes(5))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                return Map.of("status", "failed", "error", response.body());
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> result = objectMapper.readValue(response.body(), Map.class);
            return result;

        } finally {
            try { Files.deleteIfExists(tempFile); } catch (IOException ignored) {}
        }
    }

    /**
     * 健康检查：确认 Python 推理服务是否在线
     */
    public boolean isPythonServiceAlive() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(pythonServiceUrl + "/"))
                    .GET()
                    .timeout(java.time.Duration.ofSeconds(5))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 保存推理审计日志
     */
    private void saveInferenceLog(String logId, String status, String modelId, long latencyMs, String message) {
        try {
            AiInferenceLog logEntry = new AiInferenceLog();
            logEntry.setLogId(logId);
            logEntry.setInferenceType("CT_ARTIFACT");
            logEntry.setStatus(status);
            logEntry.setModelId(modelId);
            logEntry.setLatencyMs(latencyMs);
            logEntry.setMessage(message);
            logEntry.setCreateTime(LocalDateTime.now());
            inferenceLogMapper.insert(logEntry);
        } catch (Exception e) {
            log.warn("推理日志保存失败: {}", e.getMessage());
        }
    }
}

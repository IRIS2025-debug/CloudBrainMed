package com.cloudbrainmed.ai.model;

import com.cloudbrainmed.ai.entity.AiInferenceLog;
import com.cloudbrainmed.ai.entity.ModelVersion;
import com.cloudbrainmed.ai.mapper.AiInferenceLogMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.AbstractResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Component
public class InferenceEngine {

    private static final Logger log = LoggerFactory.getLogger(InferenceEngine.class);
    private static final String DEFAULT_PYTHON_SERVICE_URL = "http://localhost:8010";
    private static final int PYTHON_CONNECT_TIMEOUT_MS = 30_000;
    private static final int PYTHON_READ_TIMEOUT_MS = 300_000;

    private String pythonServiceUrl;

    private final HttpClient httpClient;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final AiInferenceLogMapper inferenceLogMapper;
    private final ModelLoader modelLoader;

    public InferenceEngine(AiInferenceLogMapper inferenceLogMapper,
                           ModelLoader modelLoader,
                           @Value("${ai.python-service.url:${AI_PYTHON_SERVICE_URL:http://localhost:8010}}")
                           String pythonServiceUrl) {
        this.httpClient = HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(30)).build();
        this.restTemplate = createRestTemplate();
        this.objectMapper = new ObjectMapper();
        this.inferenceLogMapper = inferenceLogMapper;
        this.modelLoader = modelLoader;
        this.pythonServiceUrl = normalizePythonServiceUrl(pythonServiceUrl);
    }

    private RestTemplate createRestTemplate() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(PYTHON_CONNECT_TIMEOUT_MS);
        requestFactory.setReadTimeout(PYTHON_READ_TIMEOUT_MS);
        return new RestTemplate(requestFactory);
    }

    public void setPythonServiceUrl(String url) {
        this.pythonServiceUrl = normalizePythonServiceUrl(url);
    }

    String getPythonServiceUrl() {
        return pythonServiceUrl;
    }

    private String normalizePythonServiceUrl(String url) {
        if (url == null || url.isBlank()) {
            return DEFAULT_PYTHON_SERVICE_URL;
        }
        String trimmedUrl = url.trim();
        return trimmedUrl.endsWith("/") ? trimmedUrl.substring(0, trimmedUrl.length() - 1) : trimmedUrl;
    }

    public Map<String, Object> predictArtifact(MultipartFile niftiFile) throws IOException {
        return predictSegmentation(niftiFile, "/predict-ct-artifact",
                "CT_ARTIFACT", "ct-artifact-model", "CT artifact");
    }

    public Map<String, Object> predictLesion(MultipartFile niftiFile) throws IOException {
        return predictSegmentation(niftiFile, "/predict-ct-lesion",
                "CT_LESION", "ct-lesion-model", "CT lesion");
    }

    private Map<String, Object> predictSegmentation(MultipartFile niftiFile,
                                                    String endpoint,
                                                    String callSource,
                                                    String fallbackModelKey,
                                                    String logLabel) throws IOException {
        long startTime = System.currentTimeMillis();
        String logId = "INF" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        try {
            org.springframework.http.ResponseEntity<String> response = restTemplate.postForEntity(
                    pythonServiceUrl + endpoint,
                    buildMultipartRequest(niftiFile),
                    String.class
            );
            long latency = System.currentTimeMillis() - startTime;

            if (!response.getStatusCode().is2xxSuccessful()) {
                String errorBody = response.getBody();
                log.error("{} inference failed: HTTP {} -> {}", logLabel, response.getStatusCode().value(), errorBody);
                saveInferenceLog(logId, callSource, "FAILED", null, latency,
                        "HTTP " + response.getStatusCode().value(), fallbackModelKey);
                return Map.of("status", "failed", "error", safeMessage(errorBody, "Python service returned an error"));
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> result = objectMapper.readValue(response.getBody(), Map.class);
            result.put("logId", logId);
            result.put("latencyMs", latency);

            ModelVersion activeModel = modelLoader.getActiveModel();
            saveInferenceLog(logId, callSource, "SUCCESS",
                    activeModel != null ? activeModel.getModelId() : null,
                    latency, "检测完成", fallbackModelKey);

            log.info("{} inference succeeded: {} -> {}ms", logLabel, niftiFile.getOriginalFilename(), latency);
            return result;
        } catch (HttpStatusCodeException e) {
            long latency = System.currentTimeMillis() - startTime;
            String errorBody = safeMessage(e.getResponseBodyAsString(), e.getMessage());
            log.error("{} inference failed: HTTP {} -> {}", logLabel, e.getStatusCode().value(), errorBody);
            saveInferenceLog(logId, callSource, "FAILED", null, latency,
                    "HTTP " + e.getStatusCode().value(), fallbackModelKey);
            return Map.of("status", "failed", "error", errorBody);
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - startTime;
            String message = safeMessage(e.getMessage(), e.getClass().getSimpleName());
            log.error("{} inference error: {}", logLabel, message);
            saveInferenceLog(logId, callSource, "ERROR", null, latency, message, fallbackModelKey);
            return Map.of("status", "error", "message", message);
        }
    }

    public Map<String, Object> extractFeatures(MultipartFile niftiFile) throws IOException {
        org.springframework.http.ResponseEntity<String> response = restTemplate.postForEntity(
                pythonServiceUrl + "/extract-features",
                buildMultipartRequest(niftiFile),
                String.class
        );

        if (!response.getStatusCode().is2xxSuccessful()) {
            return Map.of("status", "failed", "error",
                    safeMessage(response.getBody(), "Python service returned an error"));
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> result = objectMapper.readValue(response.getBody(), Map.class);
        return result;
    }

    public byte[] downloadMask(String maskFilename) throws IOException, InterruptedException {
        validateDownloadFilename(maskFilename, "掩码文件名无效");
        String encodedFilename = URLEncoder.encode(maskFilename, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(pythonServiceUrl + "/results/" + encodedFilename))
                .GET()
                .timeout(java.time.Duration.ofMinutes(5))
                .build();
        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() != 200) {
            throw new IOException("掩码文件下载失败: HTTP " + response.statusCode());
        }
        return response.body();
    }

    public byte[] downloadPreview(String previewFilename) throws IOException, InterruptedException {
        validateDownloadFilename(previewFilename, "预览图文件名无效");
        String encodedFilename = URLEncoder.encode(previewFilename, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(pythonServiceUrl + "/previews/" + encodedFilename))
                .GET()
                .timeout(java.time.Duration.ofMinutes(5))
                .build();
        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() != 200) {
            throw new IOException("预览图下载失败: HTTP " + response.statusCode());
        }
        return response.body();
    }

    private void validateDownloadFilename(String filename, String message) {
        if (filename == null || filename.isBlank()
                || filename.contains("/") || filename.contains("\\")) {
            throw new IllegalArgumentException(message);
        }
    }

    private HttpEntity<MultiValueMap<String, Object>> buildMultipartRequest(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "ct.nii.gz";
        String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
        AbstractResource resource = new AbstractResource() {
            @Override
            public String getFilename() {
                return filename;
            }

            @Override
            public long contentLength() {
                return file.getSize();
            }

            @Override
            public String getDescription() {
                return "multipart file resource [" + filename + "]";
            }

            @Override
            public InputStream getInputStream() throws IOException {
                return file.getInputStream();
            }
        };

        HttpHeaders fileHeaders = new HttpHeaders();
        fileHeaders.setContentType(MediaType.parseMediaType(contentType));
        HttpEntity<AbstractResource> filePart = new HttpEntity<>(resource, fileHeaders);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", filePart);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        return new HttpEntity<>(body, headers);
    }

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
     * 读取 Python 推理服务 "/" 健康响应，用于模型看板展示实际版本/状态。
     * 服务不可达或返回非 200 时返回 null，由上层降级为「离线」。
     */
    public Map<String, Object> getPythonHealth() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(pythonServiceUrl + "/"))
                    .GET()
                    .timeout(java.time.Duration.ofSeconds(5))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200 || response.body() == null || response.body().isBlank()) {
                return null;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> body = objectMapper.readValue(response.body(), Map.class);
            return body;
        } catch (Exception e) {
            return null;
        }
    }

    private String safeMessage(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary;
        }
        if (fallback != null && !fallback.isBlank()) {
            return fallback;
        }
        return "unknown error";
    }

    private void saveInferenceLog(String logId, String status, String modelId, long latencyMs, String message) {
        saveInferenceLog(logId, "CT_ARTIFACT", status, modelId, latencyMs, message, "ct-artifact-model");
    }

    private void saveInferenceLog(String logId,
                                  String callSource,
                                  String status,
                                  String modelId,
                                  long latencyMs,
                                  String message,
                                  String fallbackModelKey) {
        try {
            AiInferenceLog logEntry = new AiInferenceLog();
            logEntry.setLogId(logId);
            logEntry.setTraceId(logId);
            logEntry.setCallSource(callSource);
            logEntry.setStatus(status);
            logEntry.setModelKey(modelId != null ? modelId : fallbackModelKey);
            logEntry.setModelVersion("unknown");
            logEntry.setDurationMs((int) Math.min(latencyMs, Integer.MAX_VALUE));
            logEntry.setOutputSummary(message);
            logEntry.setCreatedAt(LocalDateTime.now());
            inferenceLogMapper.insert(logEntry);
        } catch (Exception e) {
            log.warn("Failed to save inference log: {}", e.getMessage());
        }
    }
}

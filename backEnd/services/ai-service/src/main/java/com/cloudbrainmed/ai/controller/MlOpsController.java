package com.cloudbrainmed.ai.controller;

import com.cloudbrainmed.ai.service.MlOpsService;
import com.cloudbrainmed.ai.support.AdminAuthHelper;
import com.cloudbrainmed.common.result.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

/**
 * 机器学习运维控制器
 * 对应前端: pages/admin/ml/ (Dashboard / CTInference)
 */
@RestController
@RequestMapping("/admin-service/ml")
public class MlOpsController {

    private final MlOpsService mlOpsService;
    private final String internalServiceKey;

    public MlOpsController(
            MlOpsService mlOpsService,
            @Value("${internal.service-key:}") String internalServiceKey) {
        this.mlOpsService = mlOpsService;
        this.internalServiceKey = internalServiceKey;
    }

    /** 推理统计仪表盘 */
    @GetMapping("/dashboard/inference-stats")
    public Result<?> inferenceStats(@RequestHeader Map<String, String> headers) {
        assertAdmin(headers, "MLOps推理统计");
        return Result.ok(mlOpsService.getInferenceStats());
    }

    /** 模型统计 */
    @GetMapping("/dashboard/model-stats")
    public Result<?> modelStats(@RequestHeader Map<String, String> headers) {
        assertAdmin(headers, "MLOps模型统计");
        return Result.ok(mlOpsService.getModelStats());
    }

    /** 推理日志分页 */
    @GetMapping("/inference/logs")
    public Result<?> inferenceLogs(@RequestHeader Map<String, String> headers,
                                   @RequestParam(defaultValue = "1") int page,
                                   @RequestParam(defaultValue = "10") int limit) {
        assertAdmin(headers, "MLOps推理日志");
        return Result.ok(mlOpsService.getInferenceLogs(page, limit));
    }

    /** Python 推理服务健康检查 */
    @GetMapping("/python/health")
    public Result<?> pythonHealth(@RequestHeader Map<String, String> headers) {
        assertAdmin(headers, "MLOps健康检查");
        return Result.ok(mlOpsService.checkPythonService());
    }

    @PostMapping("/inference/ct-artifact")
    public Result<?> predictCtArtifact(@RequestHeader Map<String, String> headers,
                                       @RequestParam("file") MultipartFile file) throws Exception {
        assertAdminOrInternal(headers, "CT金属伪影推理");
        return Result.ok(mlOpsService.predictCtArtifact(file));
    }

    @GetMapping("/inference/ct-artifact/result/{maskFilename}")
    public ResponseEntity<byte[]> downloadCtArtifactMask(@RequestHeader Map<String, String> headers,
                                                         @PathVariable String maskFilename) throws Exception {
        assertAdminOrInternal(headers, "CT金属伪影结果下载");
        return mlOpsService.downloadCtArtifactMask(maskFilename);
    }

    @GetMapping("/inference/ct-artifact/preview/{previewFilename}")
    public ResponseEntity<byte[]> downloadCtArtifactPreview(@RequestHeader Map<String, String> headers,
                                                            @PathVariable String previewFilename) throws Exception {
        assertAdminOrInternal(headers, "CT金属伪影预览下载");
        return mlOpsService.downloadCtArtifactPreview(previewFilename);
    }

    @PostMapping("/inference/ct-lesion")
    public Result<?> predictCtLesion(@RequestHeader Map<String, String> headers,
                                     @RequestParam("file") MultipartFile file) throws Exception {
        assertAdminOrInternal(headers, "CT病灶推理");
        return Result.ok(mlOpsService.predictCtLesion(file));
    }

    @GetMapping("/inference/ct-lesion/result/{maskFilename}")
    public ResponseEntity<byte[]> downloadCtLesionMask(@RequestHeader Map<String, String> headers,
                                                       @PathVariable String maskFilename) throws Exception {
        assertAdminOrInternal(headers, "CT病灶结果下载");
        return mlOpsService.downloadCtLesionMask(maskFilename);
    }

    @GetMapping("/inference/ct-lesion/preview/{previewFilename}")
    public ResponseEntity<byte[]> downloadCtLesionPreview(@RequestHeader Map<String, String> headers,
                                                          @PathVariable String previewFilename) throws Exception {
        assertAdminOrInternal(headers, "CT病灶预览下载");
        return mlOpsService.downloadCtLesionPreview(previewFilename);
    }

    private void assertAdmin(Map<String, String> headers, String capabilityName) {
        AdminAuthHelper.requireAdminId(resolveToken(headers), capabilityName);
    }

    private void assertAdminOrInternal(
            Map<String, String> headers, String capabilityName) {
        if (isTrustedInternalRequest(headers)) {
            return;
        }
        assertAdmin(headers, capabilityName);
    }

    private String resolveToken(Map<String, String> headers) {
        if (headers == null || headers.isEmpty()) {
            return "";
        }
        String authorization = firstHeader(
                headers, HttpHeaders.AUTHORIZATION, "authorization");
        if (authorization != null && !authorization.isBlank()) {
            String value = authorization.trim();
            return value.regionMatches(true, 0, "Bearer ", 0, 7)
                    ? value.substring(7).trim()
                    : value;
        }
        String legacyToken = firstHeader(headers, "token", "Token");
        return legacyToken == null ? "" : legacyToken.trim();
    }

    private boolean isTrustedInternalRequest(Map<String, String> headers) {
        if (internalServiceKey == null || internalServiceKey.isBlank()) {
            return false;
        }
        String providedKey = firstHeader(
                headers, "X-Internal-Service-Key", "x-internal-service-key");
        return providedKey != null && MessageDigest.isEqual(
                internalServiceKey.getBytes(StandardCharsets.UTF_8),
                providedKey.trim().getBytes(StandardCharsets.UTF_8));
    }

    private String firstHeader(Map<String, String> headers, String... names) {
        for (String name : names) {
            String value = headers.get(name);
            if (value != null) {
                return value;
            }
        }
        return null;
    }
}

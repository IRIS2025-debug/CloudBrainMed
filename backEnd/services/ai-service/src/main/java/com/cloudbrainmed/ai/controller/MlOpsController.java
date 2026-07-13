package com.cloudbrainmed.ai.controller;

import com.cloudbrainmed.ai.service.MlOpsService;
import com.cloudbrainmed.ai.support.AdminAuthException;
import com.cloudbrainmed.ai.support.AdminAuthHelper;
import com.cloudbrainmed.common.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 机器学习运维控制器
 * 对应前端: pages/admin/ml/ (Dashboard / CTInference) 及两条 CT 推理链路
 */
@RestController
@RequestMapping("/admin-service/ml")
public class MlOpsController {

    private final MlOpsService mlOpsService;

    public MlOpsController(MlOpsService mlOpsService) {
        this.mlOpsService = mlOpsService;
    }

    @ModelAttribute
    public void requireAdmin(@RequestHeader Map<String, String> headers,
                             HttpServletRequest request) {
        if (request.getRequestURI().startsWith("/admin-service/ml/inference/ct-")) {
            return;
        }
        AdminAuthHelper.requireAdminId(resolveToken(headers), "MLOps");
    }

    @ExceptionHandler(AdminAuthException.class)
    public Result<?> handleAdminAuth(AdminAuthException exception) {
        return Result.error(exception.getCode(), exception.getMessage());
    }

    /** 推理统计仪表盘 */
    @GetMapping("/dashboard/inference-stats")
    public Result<?> inferenceStats() {
        return Result.ok(mlOpsService.getInferenceStats());
    }

    /** 模型统计 */
    @GetMapping("/dashboard/model-stats")
    public Result<?> modelStats() {
        return Result.ok(mlOpsService.getModelStats());
    }

    /** 推理日志分页 */
    @GetMapping("/inference/logs")
    public Result<?> inferenceLogs(@RequestParam(defaultValue = "1") int page,
                                    @RequestParam(defaultValue = "10") int limit) {
        return Result.ok(mlOpsService.getInferenceLogs(page, limit));
    }

    /** 模型列表（固定两个业务模型） */
    @GetMapping("/models/list")
    public Result<?> modelList() {
        return Result.ok(mlOpsService.getModelList());
    }

    @GetMapping("/model/list")
    public Result<?> modelListAlias() {
        return modelList();
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

    private String firstHeader(Map<String, String> headers, String... names) {
        for (String name : names) {
            String value = headers.get(name);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    /** Python 推理服务健康检查 */
    @GetMapping("/python/health")
    public Result<?> pythonHealth() {
        return Result.ok(mlOpsService.checkPythonService());
    }

    @PostMapping("/inference/ct-artifact")
    public Result<?> predictCtArtifact(@RequestParam("file") MultipartFile file) throws Exception {
        return Result.ok(mlOpsService.predictCtArtifact(file));
    }

    @GetMapping("/inference/ct-artifact/result/{maskFilename}")
    public ResponseEntity<byte[]> downloadCtArtifactMask(@PathVariable String maskFilename) throws Exception {
        return mlOpsService.downloadCtArtifactMask(maskFilename);
    }

    @GetMapping("/inference/ct-artifact/preview/{previewFilename}")
    public ResponseEntity<byte[]> downloadCtArtifactPreview(@PathVariable String previewFilename) throws Exception {
        return mlOpsService.downloadCtArtifactPreview(previewFilename);
    }

    @PostMapping("/inference/ct-lesion")
    public Result<?> predictCtLesion(@RequestParam("file") MultipartFile file) throws Exception {
        return Result.ok(mlOpsService.predictCtLesion(file));
    }

    @GetMapping("/inference/ct-lesion/result/{maskFilename}")
    public ResponseEntity<byte[]> downloadCtLesionMask(@PathVariable String maskFilename) throws Exception {
        return mlOpsService.downloadCtLesionMask(maskFilename);
    }

    @GetMapping("/inference/ct-lesion/preview/{previewFilename}")
    public ResponseEntity<byte[]> downloadCtLesionPreview(@PathVariable String previewFilename) throws Exception {
        return mlOpsService.downloadCtLesionPreview(previewFilename);
    }
}

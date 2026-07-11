package com.cloudbrainmed.ai.controller;

import com.cloudbrainmed.ai.service.MlOpsService;
import com.cloudbrainmed.common.exception.BusinessException;
import com.cloudbrainmed.common.result.Result;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 机器学习运维控制器
 * 对应前端: pages/admin/ml/ (Dashboard / Models / Samples / CTInference)
 */
@RestController
@RequestMapping("/admin-service/ml")
public class MlOpsController {

    private final MlOpsService mlOpsService;

    public MlOpsController(MlOpsService mlOpsService) {
        this.mlOpsService = mlOpsService;
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

    /** 训练样本列表 */
    @GetMapping("/samples/list")
    public Result<?> sampleList(@RequestParam(defaultValue = "1") int page,
                                 @RequestParam(defaultValue = "10") int limit) {
        return Result.ok(mlOpsService.getSampleList(page, limit));
    }

    @GetMapping("/sample/list")
    public Result<?> sampleListAlias(@RequestParam(defaultValue = "1") int page,
                                     @RequestParam(defaultValue = "10") int limit) {
        return sampleList(page, limit);
    }

    /** 更新样本标注 */
    @PostMapping("/samples/update")
    public Result<?> updateSample(@RequestBody Map<String, String> body) {
        mlOpsService.updateSample(
                body.get("sampleId"), body.get("label"), body.get("labelType"));
        return Result.ok();
    }

    @PutMapping("/sample/label")
    public Result<?> labelSample(@RequestBody Map<String, String> body) {
        mlOpsService.updateSample(
                body.get("sampleId"),
                body.get("labelTag"),
                body.getOrDefault("labelType", "MANUAL"));
        return Result.ok();
    }

    /** 模型列表 */
    @GetMapping("/models/list")
    public Result<?> modelList() {
        return Result.ok(mlOpsService.getModelList());
    }

    @GetMapping("/model/list")
    public Result<?> modelListAlias() {
        return modelList();
    }

    /** 触发模型训练 */
    @PostMapping("/models/train")
    public Result<?> triggerTrain(@RequestBody(required = false) Map<String, String> body) {
        return Result.ok(mlOpsService.triggerTrain(body != null ? body : Map.of()));
    }

    @PostMapping("/model/train")
    public Result<?> triggerTrainAlias(@RequestBody(required = false) Map<String, String> body) {
        return triggerTrain(body);
    }

    @PutMapping("/model/traffic")
    public Result<?> setModelTraffic(@RequestBody Map<String, Object> body) {
        String modelId = String.valueOf(body.get("modelId"));
        int trafficPct = parseTrafficPct(body.get("trafficPct"));
        return Result.ok(mlOpsService.setModelTraffic(modelId, trafficPct));
    }

    private int parseTrafficPct(Object value) {
        if (value == null) {
            throw new BusinessException("流量配置参数错误");
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            throw new BusinessException("流量配置参数错误");
        }
    }

    /** 训练任务列表 */
    @GetMapping("/models/tasks")
    public Result<?> trainingTasks() {
        return Result.ok(mlOpsService.getTrainingTasks());
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

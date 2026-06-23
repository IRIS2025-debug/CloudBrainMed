package com.cloudbrainmed.ai.controller;

import com.cloudbrainmed.ai.service.MlOpsService;
import com.cloudbrainmed.common.result.Result;
import org.springframework.web.bind.annotation.*;

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

    /** 更新样本标注 */
    @PostMapping("/samples/update")
    public Result<?> updateSample(@RequestBody Map<String, String> body) {
        mlOpsService.updateSample(
                body.get("sampleId"), body.get("label"), body.get("labelType"));
        return Result.ok();
    }

    /** 模型列表 */
    @GetMapping("/models/list")
    public Result<?> modelList() {
        return Result.ok(mlOpsService.getModelList());
    }

    /** 触发模型训练 */
    @PostMapping("/models/train")
    public Result<?> triggerTrain(@RequestBody Map<String, String> body) {
        return Result.ok(mlOpsService.triggerTrain(body));
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
}
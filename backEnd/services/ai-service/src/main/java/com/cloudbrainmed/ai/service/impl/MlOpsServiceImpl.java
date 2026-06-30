package com.cloudbrainmed.ai.service.impl;

import com.cloudbrainmed.ai.entity.ModelVersion;
import com.cloudbrainmed.ai.mapper.AiInferenceLogMapper;
import com.cloudbrainmed.ai.mapper.ModelVersionMapper;
import com.cloudbrainmed.ai.mapper.TrainingSampleMapper;
import com.cloudbrainmed.ai.model.CnnModel;
import com.cloudbrainmed.ai.model.InferenceEngine;
import com.cloudbrainmed.ai.model.ModelLoader;
import com.cloudbrainmed.ai.model.ModelTrainer;
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

    private final AiInferenceLogMapper inferenceLogMapper;
    private final ModelVersionMapper modelVersionMapper;
    private final TrainingSampleMapper sampleMapper;
    private final ModelLoader modelLoader;
    private final ModelTrainer modelTrainer;
    private final InferenceEngine inferenceEngine;

    public MlOpsServiceImpl(AiInferenceLogMapper inferenceLogMapper,
                             ModelVersionMapper modelVersionMapper,
                             TrainingSampleMapper sampleMapper,
                             ModelLoader modelLoader,
                             ModelTrainer modelTrainer,
                             InferenceEngine inferenceEngine) {
        this.inferenceLogMapper = inferenceLogMapper;
        this.modelVersionMapper = modelVersionMapper;
        this.sampleMapper = sampleMapper;
        this.modelLoader = modelLoader;
        this.modelTrainer = modelTrainer;
        this.inferenceEngine = inferenceEngine;
    }

    @Override
    public Map<String, Object> getInferenceStats() {
        return Map.of(
            "todayTotal", inferenceLogMapper.countToday(),
            "successRate", computeSuccessRate(),
            "avgLatency", Math.round(inferenceLogMapper.avgLatency()),
            "adoptionRate", computeAdoptionRate()
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
    public Map<String, Object> getSampleList(int page, int limit) {
        validatePagination(page, limit);
        int offset = (page - 1) * limit;
        return Map.of(
            "list", sampleMapper.selectPage(offset, limit),
            "total", sampleMapper.countAll()
        );
    }

    @Override
    public void updateSample(String sampleId, String label, String labelType) {
        if (sampleId == null || sampleId.isBlank()) {
            throw new BusinessException("样本ID不能为空");
        }
        if (label == null || label.isBlank()) {
            throw new BusinessException("标签不能为空");
        }
        int updated = sampleMapper.updateLabel(
                sampleId.trim(), label.trim(),
                textOrDefault(labelType, "MANUAL"), "LABELED");
        if (updated == 0) {
            throw new BusinessException("样本不存在");
        }
    }

    @Override
    public List<Map<String, Object>> getModelList() {
        List<ModelVersion> models = modelLoader.listModels();
        List<Map<String, Object>> result = new ArrayList<>();
        for (ModelVersion mv : models) {
            Map<String, Object> item = new HashMap<>();
            item.put("modelId", mv.getModelId());
            item.put("modelKey", mv.getModelKey());
            item.put("modelType", mv.getModelType());
            item.put("version", mv.getVersion());
            item.put("status", mv.getStatus());
            item.put("createTime", mv.getCreateTime());
            item.put("createdAt", mv.getCreateTime());
            item.put("artifactPath", mv.getArtifactPath());
            item.put("trafficPct", "ACTIVE".equals(mv.getStatus()) ? 100 : 0);
            result.add(item);
        }
        return result;
    }

    @Override
    public Map<String, Object> triggerTrain(Map<String, String> params) {
        String modelKey = textOrDefault(params.get("modelKey"), "medical-ct-unet");
        String modelType = textOrDefault(params.get("modelType"), "unet");
        String datasetPath = textOrDefault(params.get("datasetPath"), "/data/ct-artifact/");

        CnnModel.ModelType type = parseModelType(modelType);
        CnnModel.HyperParams hyperParams = parseHyperParams(params);

        ModelTrainer.TrainingTask task = modelTrainer.createTrainingTask(
                modelKey, type, hyperParams, datasetPath);
        modelTrainer.startTraining(task.getTaskId());

        return Map.of("taskId", task.getTaskId(), "status", task.getStatus());
    }

    @Override
    public Map<String, Object> setModelTraffic(String modelId, int trafficPct) {
        if (modelId == null || modelId.isBlank() || "null".equals(modelId)) {
            throw new BusinessException("模型ID不能为空");
        }
        if (trafficPct != 0 && trafficPct != 100) {
            throw new BusinessException("当前仅支持0或100流量配置");
        }

        ModelVersion model = modelVersionMapper.selectById(modelId);
        if (model == null) {
            throw new BusinessException("模型不存在");
        }

        String status;
        if (trafficPct == 100) {
            modelLoader.activateModel(modelId);
            status = "ACTIVE";
        } else {
            modelVersionMapper.updateStatus(modelId, "INACTIVE");
            status = "INACTIVE";
        }
        return Map.of("modelId", modelId, "trafficPct", trafficPct, "status", status);
    }

    @Override
    public Map<String, Object> getTrainingTasks() {
        Map<String, ModelTrainer.TrainingTask> tasks = modelTrainer.listTasks();
        List<Map<String, Object>> list = new ArrayList<>();
        for (ModelTrainer.TrainingTask t : tasks.values()) {
            Map<String, Object> item = new HashMap<>();
            item.put("taskId", t.getTaskId());
            item.put("modelKey", t.getModelKey());
            item.put("modelType", t.getModelType());
            item.put("datasetPath", t.getDatasetPath());
            item.put("hyperParams", toHyperParamMap(t.getHyperParams()));
            item.put("status", t.getStatus());
            item.put("modelId", t.getModelId());
            item.put("errorMessage", t.getErrorMessage());
            item.put("createTime", t.getCreateTime());
            item.put("startTime", t.getStartTime());
            item.put("completedTime", t.getCompletedTime());
            list.add(item);
        }
        return Map.of("tasks", list, "stats", modelTrainer.getStats());
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
    public ResponseEntity<byte[]> downloadCtArtifactMask(String maskFilename) throws Exception {
        byte[] body = inferenceEngine.downloadMask(maskFilename);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + maskFilename + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(body);
    }

    private double computeSuccessRate() {
        int total = inferenceLogMapper.countAll();
        if (total == 0) return 100.0;
        int success = inferenceLogMapper.countByStatus("SUCCESS");
        return Math.round(success * 1000.0 / total) / 10.0;
    }

    private double computeAdoptionRate() {
        int total = sampleMapper.countAll();
        if (total == 0) return 0.0;
        int adopted = sampleMapper.countAdopted();
        return Math.round(adopted * 1000.0 / total) / 10.0;
    }

    private CnnModel.ModelType parseModelType(String modelType) {
        if ("unet".equalsIgnoreCase(modelType)) {
            return CnnModel.ModelType.UNET;
        }
        if ("attention".equalsIgnoreCase(modelType)
                || "attention_unet".equalsIgnoreCase(modelType)) {
            return CnnModel.ModelType.ATTENTION_UNET;
        }
        throw new BusinessException("模型类型仅支持 unet 或 attention");
    }

    private String textOrDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    private void validatePagination(int page, int limit) {
        if (page < 1 || limit < 1) {
            throw new BusinessException("分页参数错误");
        }
    }

    private Map<String, Object> toHyperParamMap(CnnModel.HyperParams params) {
        CnnModel.HyperParams safeParams = params != null ? params : CnnModel.HyperParams.defaultParams();
        Map<String, Object> result = new HashMap<>();
        result.put("learningRate", safeParams.getLearningRate());
        result.put("epochs", safeParams.getEpochs());
        result.put("batchSize", safeParams.getBatchSize());
        result.put("optimizer", safeParams.getOptimizer());
        return result;
    }

    private CnnModel.HyperParams parseHyperParams(Map<String, String> params) {
        CnnModel.HyperParams hyperParams = CnnModel.HyperParams.defaultParams();
        try {
            String learningRate = params.get("learningRate");
            if (learningRate != null && !learningRate.isBlank()) {
                hyperParams.setLearningRate(Double.parseDouble(learningRate.trim()));
            }
            String epochs = params.get("epochs");
            if (epochs != null && !epochs.isBlank()) {
                hyperParams.setEpochs(Integer.parseInt(epochs.trim()));
            }
            String batchSize = params.get("batchSize");
            if (batchSize != null && !batchSize.isBlank()) {
                hyperParams.setBatchSize(Integer.parseInt(batchSize.trim()));
            }
        } catch (NumberFormatException e) {
            throw new BusinessException("训练超参数格式错误");
        }
        String optimizer = params.get("optimizer");
        if (optimizer != null && !optimizer.isBlank()) {
            hyperParams.setOptimizer(optimizer.trim());
        }
        if (!Double.isFinite(hyperParams.getLearningRate())
                || hyperParams.getLearningRate() <= 0
                || hyperParams.getEpochs() <= 0
                || hyperParams.getBatchSize() <= 0) {
            throw new BusinessException("训练超参数范围错误");
        }
        return hyperParams;
    }
}

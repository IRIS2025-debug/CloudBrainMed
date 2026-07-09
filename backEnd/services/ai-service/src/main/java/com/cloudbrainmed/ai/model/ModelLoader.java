package com.cloudbrainmed.ai.model;

import com.cloudbrainmed.ai.entity.ModelVersion;
import com.cloudbrainmed.ai.mapper.ModelVersionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 模型加载器：管理模型版本注册、活跃模型切换、权重文件路径
 *
 * 架构职责：
 * - 维护当前活跃模型引用（AtomicReference 线程安全）
 * - 注册新模型版本到数据库
 * - 切换活跃模型（A/B 测试支持）
 */
@Component
public class ModelLoader {

    private static final Logger log = LoggerFactory.getLogger(ModelLoader.class);

    private final ModelVersionMapper modelVersionMapper;
    private final AtomicReference<ModelVersion> activeModel = new AtomicReference<>();

    public ModelLoader(ModelVersionMapper modelVersionMapper) {
        this.modelVersionMapper = modelVersionMapper;
    }

    /**
     * 注册新模型版本
     */
    public ModelVersion registerModel(String modelKey, String version, String artifactPath,
                                       CnnModel.ModelType modelType, CnnModel.HyperParams params) {
        String modelId = "MOD" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        ModelVersion mv = new ModelVersion();
        mv.setModelId(modelId);
        mv.setModelKey(modelKey);
        mv.setModelType(modelType.getKey());
        mv.setVersion(version);
        mv.setArtifactPath(artifactPath);
        mv.setStatus("REGISTERED");
        mv.setCreateTime(LocalDateTime.now());

        // 超参数序列化（简化版：存关键字段）
        mv.setHyperParams(buildHyperParamsJson(params));

        modelVersionMapper.insert(mv);
        log.info("模型已注册: {} v{} [{}] → {}", modelKey, version, modelType.getKey(), artifactPath);
        return mv;
    }

    /**
     * 激活指定模型版本
     */
    public void activateModel(String modelId) {
        ModelVersion mv = modelVersionMapper.selectById(modelId);
        if (mv == null) {
            throw new RuntimeException("模型不存在: " + modelId);
        }
        mv.setStatus("ACTIVE");
        mv.setActivatedAt(LocalDateTime.now());
        modelVersionMapper.updateStatus(modelId, "ACTIVE");

        // 将之前激活的模型标记为 INACTIVE
        ModelVersion previous = activeModel.get();
        if (previous == null) {
            previous = modelVersionMapper.selectActive();
        }
        activeModel.set(mv);
        if (previous != null && !previous.getModelId().equals(modelId)) {
            modelVersionMapper.updateStatus(previous.getModelId(), "INACTIVE");
        }

        log.info("模型已激活: {} v{} [{}]", mv.getModelKey(), mv.getVersion(), mv.getModelType());
    }

    /**
     * 获取当前活跃模型
     */
    public ModelVersion getActiveModel() {
        ModelVersion mv = activeModel.get();
        if (mv != null) return mv;
        // 从数据库加载
        mv = modelVersionMapper.selectActive();
        if (mv != null) {
            activeModel.set(mv);
        }
        return mv;
    }

    /**
     * 查询所有已注册模型
     */
    public List<ModelVersion> listModels() {
        return modelVersionMapper.selectAll();
    }

    /**
     * 获取模型权重文件路径
     */
    public String getWeightPath(ModelVersion mv) {
        return mv.getArtifactPath();
    }

    /**
     * 获取 Python 模型类型标识
     */
    public String getPythonModelType(ModelVersion mv) {
        return mv.getModelType() != null ? mv.getModelType() : "unet";
    }

    /**
     * 构建超参数 JSON 摘要
     */
    private String buildHyperParamsJson(CnnModel.HyperParams p) {
        return String.format(
            "{\"lr\":%f,\"batchSize\":%d,\"epochs\":%d,\"accumSteps\":%d," +
            "\"optimizer\":\"%s\",\"weightDecay\":%f,\"scheduler\":\"%s\"," +
            "\"lossType\":\"%s\",\"dropout\":%f,\"fp16\":%b}",
            p.getLearningRate(), p.getBatchSize(), p.getEpochs(), p.getGradientAccumSteps(),
            p.getOptimizer(), p.getWeightDecay(), p.getScheduler(),
            p.getLossType(), p.getDropout(), p.isUseFp16()
        );
    }
}

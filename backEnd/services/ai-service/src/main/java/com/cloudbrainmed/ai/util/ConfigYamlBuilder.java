package com.cloudbrainmed.ai.util;

import com.cloudbrainmed.ai.model.CnnModel;

/**
 * 将 Java HyperParams 转换成 Python train.py 可读的 config.yaml 字符串
 */
public class ConfigYamlBuilder {

    private ConfigYamlBuilder() {}

    public static String build(CnnModel.HyperParams params, CnnModel.ModelType modelType) {
        return build(params, modelType, "../../../../BrainCT/BrainCT/Datasets");
    }

    public static String build(CnnModel.HyperParams params, CnnModel.ModelType modelType, String datasetPath) {
        String normalizedDatasetPath = normalizeDatasetPath(datasetPath);
        StringBuilder sb = new StringBuilder();
        sb.append("# ===== 由 Java MlOpsService 自动生成 =====\n");

        sb.append("data:\n");
        sb.append("  ct_dir: \"").append(normalizedDatasetPath).append("/CT\"\n");
        sb.append("  mask_dir: \"").append(normalizedDatasetPath).append("/MASK\"\n");
        sb.append("  val_split: 0.2\n");
        sb.append("  seed: 42\n");

        sb.append("training:\n");
        sb.append("  batch_size: ").append(params.getBatchSize()).append("\n");
        sb.append("  epochs: ").append(params.getEpochs()).append("\n");
        sb.append("  learning_rate: ").append(params.getLearningRate()).append("\n");
        sb.append("  optimizer: \"").append(mapOptimizer(params.getOptimizer())).append("\"\n");
        sb.append("  weight_decay: ").append(params.getWeightDecay()).append("\n");
        sb.append("  scheduler: \"").append(mapScheduler(params.getScheduler())).append("\"\n");
        sb.append("  gradient_accumulation: ").append(params.getGradientAccumSteps()).append("\n");
        sb.append("  early_stop_patience: ").append(params.getEarlyStoppingPatience()).append("\n");
        sb.append("  fp16: ").append(params.isUseFp16()).append("\n");

        sb.append("model:\n");
        sb.append("  type: \"").append(modelType.getKey()).append("\"\n");
        sb.append("  in_channels: ").append(params.getInChannels()).append("\n");
        sb.append("  out_channels: ").append(params.getOutChannels()).append("\n");

        sb.append("loss:\n");
        sb.append("  dice_weight: 0.5\n");
        sb.append("  bce_weight: 0.5\n");
        sb.append("  pos_weight: 2.0\n");

        sb.append("augmentation:\n");
        sb.append("  enabled: true\n");
        sb.append("  hflip_prob: 0.5\n");
        sb.append("  vflip_prob: 0.3\n");
        sb.append("  rotate_limit: 30\n");
        sb.append("  elastic: true\n");
        sb.append("  gamma_range: [0.8, 1.2]\n");
        sb.append("  noise_std: 0.01\n");

        sb.append("logging:\n");
        sb.append("  experiment_root: \"experiments\"\n");
        sb.append("  viz_interval: 10\n");
        sb.append("  save_interval: 10\n");

        return sb.toString();
    }

    private static String normalizeDatasetPath(String datasetPath) {
        String path = datasetPath == null || datasetPath.isBlank()
                ? "../../../../BrainCT/BrainCT/Datasets"
                : datasetPath.trim();
        return path.replace("\\", "/").replaceAll("/+$", "");
    }

    private static String mapOptimizer(String javaOpt) {
        if (javaOpt == null) return "adamw";
        return switch (javaOpt.toLowerCase()) {
            case "adam" -> "adam";
            case "sgd" -> "sgd";
            default -> "adamw";
        };
    }

    private static String mapScheduler(String javaSched) {
        if (javaSched == null) return "cosine";
        return switch (javaSched.toLowerCase()) {
            case "steplr" -> "step";
            case "reducelronplateau" -> "plateau";
            default -> "cosine";
        };
    }
}

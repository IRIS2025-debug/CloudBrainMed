package com.cloudbrainmed.ai.model;

/**
 * CNN/U-Net 模型超参数配置
 * 对应课件《深度学习中超参数》的核心参数模板
 */
public class CnnModel {

    public static class HyperParams {
        // ========== 核心刚需超参数 ==========
        /** 学习率：1e-4 ~ 1e-3 */
        private double learningRate = 1e-4;

        /** 批次大小：2 / 4 / 8 / 16 */
        private int batchSize = 2;

        /** 训练轮次：50 ~ 200 */
        private int epochs = 100;

        /** 梯度累计步数：2 / 4 / 8（等效 Batch = batchSize × accumSteps） */
        private int gradientAccumSteps = 4;

        // ========== 优化器相关 ==========
        /** 优化器：AdamW / Adam / SGD */
        private String optimizer = "AdamW";

        /** 动量（仅 SGD 使用） */
        private double momentum = 0.9;

        /** 权重衰减：1e-4 ~ 1e-5 */
        private double weightDecay = 1e-5;

        // ========== 学习率调度器 ==========
        /** 调度策略：CosineAnnealing / StepLR / ReduceLROnPlateau */
        private String scheduler = "CosineAnnealing";

        /** 调度周期参数 */
        private int tMax = 100;

        // ========== 模型结构 ==========
        /** 输入通道数 */
        private int inChannels = 1;

        /** 输出通道数 */
        private int outChannels = 1;

        /** 基础通道数（UNet: 64→128→256→512） */
        private int baseChannels = 64;

        /** Dropout 率：0.1 ~ 0.3 */
        private double dropout = 0.1;

        /** 激活函数：ReLU / GELU / LeakyReLU */
        private String activation = "ReLU";

        // ========== 损失函数 ==========
        /** 损失类型：DiceCE / Dice / BCEWithLogits */
        private String lossType = "DiceCE";

        /** 组合损失权重 */
        private double lossLambda = 1.0;

        // ========== 硬件/训练策略 ==========
        /** 混合精度训练 */
        private boolean useFp16 = true;

        /** 早停 patience */
        private int earlyStoppingPatience = 15;

        /** 设备：cuda / cpu */
        private String device = "cuda";

        // ========== Getters & Setters ==========
        public double getLearningRate() { return learningRate; }
        public void setLearningRate(double learningRate) { this.learningRate = learningRate; }
        public int getBatchSize() { return batchSize; }
        public void setBatchSize(int batchSize) { this.batchSize = batchSize; }
        public int getEpochs() { return epochs; }
        public void setEpochs(int epochs) { this.epochs = epochs; }
        public int getGradientAccumSteps() { return gradientAccumSteps; }
        public void setGradientAccumSteps(int gradientAccumSteps) { this.gradientAccumSteps = gradientAccumSteps; }
        public String getOptimizer() { return optimizer; }
        public void setOptimizer(String optimizer) { this.optimizer = optimizer; }
        public double getMomentum() { return momentum; }
        public void setMomentum(double momentum) { this.momentum = momentum; }
        public double getWeightDecay() { return weightDecay; }
        public void setWeightDecay(double weightDecay) { this.weightDecay = weightDecay; }
        public String getScheduler() { return scheduler; }
        public void setScheduler(String scheduler) { this.scheduler = scheduler; }
        public int getTMax() { return tMax; }
        public void setTMax(int tMax) { this.tMax = tMax; }
        public int getInChannels() { return inChannels; }
        public void setInChannels(int inChannels) { this.inChannels = inChannels; }
        public int getOutChannels() { return outChannels; }
        public void setOutChannels(int outChannels) { this.outChannels = outChannels; }
        public int getBaseChannels() { return baseChannels; }
        public void setBaseChannels(int baseChannels) { this.baseChannels = baseChannels; }
        public double getDropout() { return dropout; }
        public void setDropout(double dropout) { this.dropout = dropout; }
        public String getActivation() { return activation; }
        public void setActivation(String activation) { this.activation = activation; }
        public String getLossType() { return lossType; }
        public void setLossType(String lossType) { this.lossType = lossType; }
        public double getLossLambda() { return lossLambda; }
        public void setLossLambda(double lossLambda) { this.lossLambda = lossLambda; }
        public boolean isUseFp16() { return useFp16; }
        public void setUseFp16(boolean useFp16) { this.useFp16 = useFp16; }
        public int getEarlyStoppingPatience() { return earlyStoppingPatience; }
        public void setEarlyStoppingPatience(int earlyStoppingPatience) { this.earlyStoppingPatience = earlyStoppingPatience; }
        public String getDevice() { return device; }
        public void setDevice(String device) { this.device = device; }

        /** 等效批次大小 */
        public int effectiveBatchSize() {
            return batchSize * gradientAccumSteps;
        }

        /** 预设：基础训练模板 */
        public static HyperParams defaultParams() {
            return new HyperParams();
        }

        /** 预设：快速训练（小数据集调试用） */
        public static HyperParams fastDebug() {
            HyperParams p = new HyperParams();
            p.epochs = 10;
            p.batchSize = 4;
            p.gradientAccumSteps = 1;
            p.earlyStoppingPatience = 3;
            return p;
        }

        /** 预设：高精度训练（生产环境） */
        public static HyperParams production() {
            return new HyperParams();
        }
    }

    /** 模型类型枚举 */
    public enum ModelType {
        UNET("unet", "基础 UNet2D + 多层融合特征提取"),
        ATTENTION_UNET("attention", "注意力增强 UNet2D + 时空注意力 + 特征提取");

        private final String key;
        private final String description;

        ModelType(String key, String description) {
            this.key = key;
            this.description = description;
        }

        public String getKey() { return key; }
        public String getDescription() { return description; }
    }
}

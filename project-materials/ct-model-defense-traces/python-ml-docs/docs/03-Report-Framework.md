# CT 金属伪影检测成果物报告

> 本报告对应当前已完成的 CT 金属伪影识别/分割模型。病灶识别、病灶分割需要额外病灶标签或病灶 mask 数据集，不能用金属伪影 mask 直接替代。

---

## 第 1 章：数据集准备

### 1.1 数据来源
- CT 金属伪影数据来源：`project-materials/ct-model-defense-traces/xunlian/ct_artifact_dataset.zip`，已同步到 `project-materials/ct-model-defense-traces/python-ml-data/data/ct_artifact_dataset/`
- 数据格式：DICOM 切片（.dcm），每张为 512x512 的 2D 图像
- 标注形式：二值掩码（伪影区域 = 1，背景 = 0）

### 1.2 数据统计
| 指标 | 数值 |
|------|------|
| 总样本数 | 460 对 CT+Mask 切片 |
| 正像素占比 | 约 0.141% |
| 不平衡比 | 约 1:708 |
| 训练集/验证集 | 368 / 92 张（8:2 划分） |

### 1.3 数据预处理
- Z-score 归一化（逐切片计算 mean/std）
- 数据增强（训练阶段）：水平/垂直翻转、随机旋转、弹性变形、Gamma 校正、高斯噪声
- 验证集不做增强

---

## 第 2 章：模型选择

### 2.1 模型对比

| 特性 | UNet2D | AttentionUNet2D |
|------|--------|----------------|
| 参数量 | 7.7M | 11.2M |
| 核心结构 | 编码-解码 + 跳跃连接 | 编码-解码 + 注意力瓶颈层 |
| 特征提取 | 多层全局平均池化、拼接、L2 归一化 | 多层全局平均池化、拼接、L2 归一化 |
| 推理速度 | 快，作为基线 | 略慢，但 Best Dice 最高 |

### 2.2 为什么选 2D 而不是 3D
- 数据量较小（460 张 2D 切片），3D 需要更多数据
- 2D 训练速度快，适合多组超参对比
- 注意力机制在瓶颈层添加，不增加编码/解码主体复杂度

---

## 第 3 章：超参数调优

### 3.1 调优方法
网格搜索 + 手动调整，对比以下维度：

| 维度 | 候选值 |
|------|--------|
| 模型结构 | UNet, AttentionUNet |
| 优化器 | AdamW, Adam, SGD |
| 学习率 | 1e-4, 5e-4 |
| 批次大小 | 8, 16 |
| 损失权重 | Dice=0.5, BCE=0.5 |

### 3.2 对比结果

| 实验 | 模型 | 优化器 | LR | Batch | 梯度累积 | 等效Batch | Best Dice | Best F1 | Best Acc |
|------|------|--------|----|-------|----------|-----------|-----------|---------|----------|
| E1 | UNet | AdamW | 1e-4 | 8 | 1 | 8 | 0.3114 | 0.7360 | 0.9988 |
| E2 | UNet | Adam | 1e-4 | 8 | 1 | 8 | 0.3265 | 0.7433 | 0.9988 |
| E3 | UNet | SGD | 1e-3 | 8 | 1 | 8 | 0.2719 | 0.7036 | 0.9988 |
| E4 | AttentionUNet | AdamW | 1e-4 | 2 | 4 | 8 | 0.3508 | 0.7242 | 0.9988 |
| E5 | UNet | AdamW | 5e-4 | 8 | 1 | 8 | 0.2499 | 0.6985 | 0.9988 |
| E6 | UNet | AdamW | 1e-4 | 4 | 4 | 16 | 0.2963 | 0.6644 | 0.9984 |

### 3.3 调参结论

按 Best Dice 排序，E4（AttentionUNet + AdamW）效果最好，Best Dice=0.3508，Best F1=0.7242。E2 的 F1 略高，但 Dice 更能反映分割区域与标注区域的重合程度，因此最终选择 E4 作为部署权重。E3（SGD）和 E5（更大学习率）效果下降，说明该数据集上 Adam/AdamW 更稳定，过大的学习率不利于伪影边界收敛。

---

## 第 4 章：训练结果

### 4.1 损失曲线
![Loss Curve](../../xunlian/20260628_172419_attention_adamw/20260628_172419_attention_adamw/loss_curve.png)

### 4.2 Dice/F1 曲线
![Dice/F1 Curve](../../xunlian/20260628_172419_attention_adamw/20260628_172419_attention_adamw/dice_f1_curve.png)

### 4.3 PR 曲线
![PR Curve](../../xunlian/20260628_172419_attention_adamw/20260628_172419_attention_adamw/pr_curve.png)

### 4.4 预测样例
![Prediction Sample](../../xunlian/20260628_172419_attention_adamw/20260628_172419_attention_adamw/pred_sample.png)

### 4.5 指标来源
最优实验归档中保留了 `config.yaml`、`train.log`、`metrics.json`、`loss_curve.png`、`dice_f1_curve.png`、`acc_curve.png`、`pr_curve.png` 和 `pred_sample.png`。六组对比表由 `docs/generated/xunlian-experiment-summary.md` 自动生成。

---

## 第 5 章：实验记录

### 5.1 目录结构
每次训练自动生成独立实验目录，互不覆盖：

```text
experiments/
├── 20260626_142848_unet_adamw/      # E1 UNet + AdamW
├── 20260627_120749_unet_adam/       # E2 UNet + Adam
├── 20260628_084614_unet_sgd/        # E3 UNet + SGD
├── 20260628_172419_attention_adamw/ # E4 AttentionUNet + AdamW
├── 20260629_150658_unet_adamw/      # E5 UNet + AdamW, LR=5e-4
└── 20260630_143434_unet_adamw/      # E6 UNet + AdamW, effective batch=16
```

### 5.2 留痕机制
- 自动记录：每个实验的 `config.yaml` 保证实验可复现
- 指标持久化：`metrics.json` 记录 Best Dice、Best F1、Best Accuracy 等指标
- 图表自动生成：每个实验归档包含损失曲线、Dice/F1 曲线、Accuracy 曲线、PR 曲线和预测样例
- 自动汇总：`docs/generated/xunlian-experiment-summary.md` 和 `.csv` 已生成六组对比表

---

## 第 6 章：推理部署

### 6.1 服务架构

```text
检查医生前端
  -> POST /doctor-service/exam/ct-artifact
  -> 网关/Vite 代理
  -> Java MlOpsController
  -> Python FastAPI /predict-ct-artifact
  -> PyTorch AttentionUNet
  -> mask + 结构化 JSON
```

### 6.2 推理流程
1. 用户上传 NIfTI 文件（.nii 或 .nii.gz）
2. 服务端逐切片推理（z-score 归一化 -> 模型预测 -> sigmoid -> 二值化）
3. 生成 3D 掩码 NIfTI，统计阳性像素、总像素和伪影占比
4. 返回 mask 下载链接和 `reportInput`，供后续大语言模型生成文字描述或报告初稿

### 6.3 Java 后端集成
- `MlOpsController.java` 提供 RESTful 接口
- `InferenceEngine.java` 通过 HTTP 调用 Python FastAPI
- `ModelTrainer.java` 通过 ProcessBuilder 调用 Python 训练脚本
- Python 默认加载 `Model/weights/best_attention_adamw.pth`

### 6.4 结构化 JSON 输出

核心字段如下：

```json
{
  "status": "success",
  "artifactDetected": true,
  "positivePixels": 3,
  "totalPixels": 8,
  "artifactRatio": 37.5,
  "maskFile": "xxx_mask.nii.gz",
  "downloadUrl": "/results/xxx_mask.nii.gz",
  "modelType": "attention",
  "modelVersion": "attention_adamw_e4",
  "reportInput": {
    "task": "CT_ARTIFACT_REPORT",
    "modality": "CT",
    "finding": {
      "artifactDetected": true,
      "positivePixels": 3,
      "totalPixels": 8,
      "artifactRatio": 37.5
    },
    "summary": "检测到CT金属伪影，伪影像素占比约37.5000%。"
  }
}
```

后续大语言模型只负责根据 `reportInput` 生成文字描述和报告初稿，不负责重新判断伪影区域。

---

## 第 7 章：算力说明

### 7.1 本地算力限制
| 硬件 | 指标 | 限制 |
|------|------|------|
| CPU | 本地 CPU 环境 | 每 epoch 约 2.5 分钟，100 epochs 需 4+ 小时 |
| 内存 | 本地开发机内存 | 数据量小，无瓶颈 |
| GPU | 无独立 GPU | 无法使用混合精度和 CUDA 加速 |

### 7.2 云 GPU 使用记录

| 平台 | 机型 | 单价 | 使用时长 | 总费用 |
|------|------|------|---------|--------|
| AutoDL | RTX 4090 | 约 3.5 元/小时 | 六组实验约 2-3 小时 | 约 10-20 元 |

### 7.3 成本对比
- 云 GPU 跑完全部实验：约 10-20 元
- 自购 RTX 4090：约 12,000-15,000 元
- 性价比：云 GPU 胜出

---

## 附录：关键代码路径

| 模块 | 文件路径 | 说明 |
|------|---------|------|
| 训练入口 | `python-ml/training/train.py` | CLI 训练主脚本 |
| 模型定义 | `python-ml/Model/UNet2D.py` | UNet 模型 |
| 模型定义 | `python-ml/Model/AttentionUNet2D.py` | Attention UNet |
| 推理服务 | `python-ml/CTDetectionServer.py` | FastAPI 服务 |
| 推理引擎 | `python-ml/Detection/CTArtifactInfer.py` | 切片推理引擎 |
| 结果结构化 | `python-ml/Detection/ct_artifact_result.py` | 生成统计字段和 LLM 报告输入 |
| Java 推理代理 | `src/main/java/com/cloudbrainmed/ai/model/InferenceEngine.java` | Java 调 Python 通道 |

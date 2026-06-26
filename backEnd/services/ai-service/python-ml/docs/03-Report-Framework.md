# 成果物报告框架

> 按照老师要求的 7 个章节组织结构。方括号 `[待补充]` 是跑完实验后填入的内容。

---

## 第 1 章：数据集准备

### 1.1 数据来源
- CT 金属伪影数据来源：[待补充]
- 数据格式：DICOM 切片（.dcm），每张为 512×512 的 2D 图像
- 标注形式：二值掩码（伪影区域 = 1，背景 = 0）

### 1.2 数据统计
| 指标 | 数值 |
|------|------|
| 总样本数 | 460 对 CT+Mask 切片 |
| 正像素占比 | [待补充，运行 `compute_pos_weight` 后填入] |
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
| 核心结构 | 编码-解码 + 跳跃连接 | 编码-解码 + 时空注意力瓶颈层 |
| 特征提取 | 多层全局平均池化 → 拼接 → L2 归一化 | 多层全局平均池化 → 拼接 → L2 归一化 |
| 推理速度 | [待补充] | [待补充] |

### 2.2 为什么选 2D 而不是 3D
- 数据量较小（460 张 2D 切片），3D 需要更多数据
- 2D 训练速度快，适合多组超参对比
- 注意力机制在瓶颈层添加，不增加编码/解码计算量

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

| 实验 | 模型 | 优化器 | LR | Batch | Best Dice | Best F1 | Best Acc | 总耗时 |
|------|------|--------|----|-------|-----------|---------|---------|--------|
| E1 | UNet | AdamW | 1e-4 | 8 | [待补充] | [待补充] | [待补充] | [待补充] |
| E2 | UNet | Adam | 1e-4 | 8 | [待补充] | [待补充] | [待补充] | [待补充] |
| E3 | UNet | SGD | 1e-3 | 8 | [待补充] | [待补充] | [待补充] | [待补充] |
| E4 | Attn UNet | AdamW | 1e-4 | 8 | [待补充] | [待补充] | [待补充] | [待补充] |
| E5 | UNet | AdamW | 5e-4 | 8 | [待补充] | [待补充] | [待补充] | [待补充] |
| E6 | UNet | AdamW | 1e-4 | 16 | [待补充] | [待补充] | [待补充] | [待补充] |

### 3.3 调参结论

[待补充：跑完实验后，分析哪个组合效果最好，为什么]

---

## 第 4 章：训练结果

### 4.1 损失曲线
![Loss Curve](placeholder_loss_curve.png)
*[替换为最优实验的 loss_curve.png]*

### 4.2 Dice/F1 曲线
![Dice/F1 Curve](placeholder_dice_curve.png)
*[替换为最优实验的 dice_f1_curve.png]*

### 4.3 混淆矩阵
![Confusion Matrix](placeholder_confusion.png)
*[替换为最优实验的 confusion.png]*

### 4.4 PR 曲线
![PR Curve](placeholder_pr_curve.png)
*[替换为最优实验的 pr_curve.png]*

### 4.5 预测样例
![Prediction Sample](placeholder_pred_sample.png)
*[替换为最优实验的 pred_sample.png]*

---

## 第 5 章：实验记录

### 5.1 目录结构
每次训练自动生成独立实验目录，互不覆盖：

```
experiments/
├── 20260625_152247_unet_adamw/      # UNet + AdamW
│   ├── config.yaml                  # 超参快照
│   ├── train.log                    # 完整日志
│   ├── best.pth                     # 最优权重
│   ├── metrics.json                 # 指标 JSON
│   ├── loss_curve.png               # 损失曲线
│   ├── dice_f1_curve.png            # Dice/F1 曲线
│   └── ...
├── 20260625_163801_attn_adamw/     # AttentionUNet + AdamW
└── ...
```

### 5.2 留痕机制
- 自动记录：每个实验的 config.yaml 保证实验可复现
- 指标持久化：metrics.json 记录每轮 loss、dice、f1、precision、recall
- 图表自动生成：每 10 轮重绘一次，最终全覆盖

---

## 第 6 章：推理部署

### 6.1 服务架构
```
[用户] → FastAPI (CTDetectionServer.py)
           ├── /predict-ct-artifact  POST   # CT 伪影检测
           ├── /extract-features     POST   # 特征向量提取
           ├── /results/{filename}   GET    # 掩码下载
           └── /                    GET    # 健康检查
```

### 6.2 推理流程
1. 用户上传 NIfTI 文件 (.nii.gz)
2. 服务端逐切片推理（z-score 归一化 → 模型预测 → sigmoid → 二值化）
3. 生成 3D 掩码 NIfTI，提供下载链接

### 6.3 Java 后端集成
- `MlOpsController.java` 提供 RESTful 接口
- `ModelTrainer` 通过 ProcessBuilder 调用 Python 训练脚本
- 训练完成后自动注册模型到 `ModelLoader`

---

## 第 7 章：算力说明

### 7.1 本地算力限制
| 硬件 | 指标 | 限制 |
|------|------|------|
| CPU | Intel [待补充] | 每 epoch 约 2.5 分钟，100 epochs 需 4+ 小时 |
| 内存 | [待补充] | 数据量小，无瓶颈 |
| GPU | 无独立 GPU | 无法使用混合精度和 CUDA 加速 |

### 7.2 云 GPU 使用记录

| 平台 | 机型 | 单价 | 使用时长 | 总费用 |
|------|------|------|---------|--------|
| AutoDL | RTX 4090 | ¥3.5/小时 | [待补充] | [待补充] |

### 7.3 成本对比
- 云 GPU 跑完全部实验：约 ¥10-20
- 自购 RTX 4090：约 ¥12,000-15,000
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
| 训练接口 | `Java/.../ModelTrainer.java` | Java 调 Python 通道 |

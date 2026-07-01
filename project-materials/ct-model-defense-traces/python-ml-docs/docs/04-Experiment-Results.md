# 六组调参实验结果

> 用途：记录老师要求的调参过程留痕。数据来自 `project-materials/ct-model-defense-traces/xunlian/*.zip` 中的 `config.yaml` 和 `metrics.json`，汇总脚本为 `tools/summarize_xunlian_archives.py`。

## 1. 结果生成方式

在 `python-ml/` 目录执行：

```powershell
python tools/summarize_xunlian_archives.py
```

脚本读取：

- `project-materials/ct-model-defense-traces/xunlian/*.zip`：六组实验压缩包
- `config.yaml`：模型、优化器、学习率、batch、梯度累积等超参数
- `metrics.json`：Best Dice、Best F1、Best Accuracy、最终 epoch

生成文件：

- `project-materials/ct-model-defense-traces/python-ml-docs/docs/generated/xunlian-experiment-summary.md`
- `project-materials/ct-model-defense-traces/python-ml-docs/docs/generated/xunlian-experiment-summary.csv`

## 2. 六组实验设计

| 实验 | 模型 | 优化器 | LR | Batch | 目的 |
|------|------|--------|----|-------|------|
| E1 | UNet | AdamW | 1e-4 | 8 | 基线实验 |
| E2 | UNet | Adam | 1e-4 | 8 | 对比优化器 Adam |
| E3 | UNet | SGD | 1e-3 | 8 | 对比优化器 SGD |
| E4 | AttentionUNet | AdamW | 1e-4 | 2，累积4步 | 对比注意力结构 |
| E5 | UNet | AdamW | 5e-4 | 8 | 对比更大学习率 |
| E6 | UNet | AdamW | 1e-4 | 4，累积4步 | 对比更大等效 batch |

## 3. 对比表

| 实验 | 压缩包 | 模型 | 优化器 | LR | Batch | 梯度累积 | 等效Batch | Epochs | Best Dice | Best F1 | Best Acc |
|---|---|---|---|---|---|---|---|---|---|---|---|
| E1 | `20260626_142848_unet_adamw.zip` | unet | adamw | 0.0001 | 8 | 1 | 8 | 100 | 0.3114 | 0.7360 | 0.9988 |
| E2 | `20260627_120749_unet_adam.zip` | unet | adam | 0.0001 | 8 | 1 | 8 | 100 | 0.3265 | 0.7433 | 0.9988 |
| E3 | `20260628_084614_unet_sgd.zip` | unet | sgd | 0.0010 | 8 | 1 | 8 | 100 | 0.2719 | 0.7036 | 0.9988 |
| E4 | `20260628_172419_attention_adamw.zip` | attention | adamw | 0.0001 | 2 | 4 | 8 | 100 | 0.3508 | 0.7242 | 0.9988 |
| E5 | `20260629_150658_unet_adamw.zip` | unet | adamw | 0.0005 | 8 | 1 | 8 | 100 | 0.2499 | 0.6985 | 0.9988 |
| E6 | `20260630_143434_unet_adamw.zip` | unet | adamw | 0.0001 | 4 | 4 | 16 | 100 | 0.2963 | 0.6644 | 0.9984 |

## 4. 最优模型选择

- 最优实验：E4 `20260628_172419_attention_adamw.zip`
- 最优模型：AttentionUNet + AdamW
- Best Dice：0.3508
- Best F1：0.7242
- 部署权重：`Model/weights/best_attention_adamw.pth`

选择理由：

1. CT 伪影检测是分割任务，优先看 Dice，因为它直接衡量预测区域和标注区域的重合度。
2. E4 的 Best Dice 在六组实验中最高，说明注意力结构对伪影区域定位更有帮助。
3. E2 的 F1 略高，但 E4 在分割区域一致性上更优，因此作为当前默认部署模型。
4. SGD 和更大学习率实验指标下降，说明当前数据集上 Adam/AdamW 更适合。

## 5. 需要归档的证据

每个实验目录或压缩包至少保留：

| 文件 | 作用 |
|------|------|
| `config.yaml` | 证明本次实验超参，保证可复现 |
| `train.log` | 证明训练过程和每轮指标变化 |
| `metrics.json` | 证明最终指标和最佳指标 |
| `best.pth` | 最优权重文件 |
| `loss_curve.png` | 损失变化曲线 |
| `dice_f1_curve.png` | Dice/F1 变化曲线 |
| `acc_curve.png` | Accuracy 变化曲线 |
| `pred_sample.png` | 预测样例 |
| `pr_curve.png` | PR 曲线 |

## 6. 和“训练任务持久化”的区别

本文档解决的是课程/老师要求的实验留痕：记录六组模型为什么这么跑、跑出来什么结果、最终为什么选某一组。

Java 后端的“训练任务持久化”解决的是系统运行问题：管理员在 Web 端触发训练后，任务状态是否能保存到数据库，服务重启后是否还能查到。两者不冲突，但当前课程交付优先保证实验留痕完整。

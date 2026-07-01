# 六组调参实验结果

> 用途：记录老师要求的调参过程留痕。其他成员跑完六组实验后，把 `experiments/` 目录同步回来，再运行汇总脚本生成表格。

## 1. 结果生成方式

在 `python-ml/` 目录执行：

```bash
python tools/summarize_experiments.py
```

脚本会读取：

- `experiments/*/config.yaml`：本次训练的模型、优化器、学习率、batch、epoch 等超参数快照。
- `experiments/*/metrics.json`：Best Dice、Best F1、Best Accuracy、最终 epoch。
- `experiments/*/train.log` 和曲线图：用于检查留痕完整性。

生成文件：

- `docs/generated/experiment-summary.md`
- `docs/generated/experiment-summary.csv`

## 2. 六组实验设计

| 实验 | 模型 | 优化器 | LR | Batch | 目的 |
|------|------|--------|----|-------|------|
| E1 | UNet | AdamW | 1e-4 | 8 | 基线实验 |
| E2 | UNet | Adam | 1e-4 | 8 | 对比优化器 Adam |
| E3 | UNet | SGD | 1e-3 | 8 | 对比优化器 SGD |
| E4 | AttentionUNet | AdamW | 1e-4 | 8 | 对比注意力结构 |
| E5 | UNet | AdamW | 5e-4 | 8 | 对比更大学习率 |
| E6 | UNet | AdamW | 1e-4 | 16 | 对比更大 batch |

## 3. 需要归档的证据

每个实验目录至少保留：

| 文件 | 作用 |
|------|------|
| `config.yaml` | 证明本次实验超参，保证可复现 |
| `train.log` | 证明训练过程和每轮指标变化 |
| `metrics.json` | 证明最终指标和最佳指标 |
| `best.pth` | 最优权重文件 |
| `loss_curve.png` | 损失变化曲线 |
| `dice_f1_curve.png` | Dice/F1 变化曲线 |
| `acc_curve.png` | Accuracy 变化曲线 |
| `pred_sample.png` | 预测样例，若训练脚本生成 |
| `confusion.png` / `pr_curve.png` | 混淆矩阵和 PR 曲线，若训练脚本生成 |

## 4. 结果填写区

跑完脚本后，把 `docs/generated/experiment-summary.md` 中的“对比表”和“最优模型建议”复制到这里或报告正文。

### 4.1 对比表

待生成。

### 4.2 最优模型选择

待生成。建议按以下逻辑写：

1. 先按 Best Dice 排序，因为 CT 伪影检测更关注分割区域重合度。
2. 若 Dice 接近，再比较 F1、Accuracy 和验证曲线稳定性。
3. 若 AttentionUNet 指标只小幅提升但耗时明显增加，需要说明是否值得部署。
4. 最终选择一个 `best.pth` 作为推理服务权重，并保留对应实验目录作为证据。

## 5. 和“训练任务持久化”的区别

本文档解决的是课程/老师要求的实验留痕：记录六组模型为什么这么跑、跑出来什么结果、最终为什么选某一组。

Java 后端的“训练任务持久化”解决的是系统运行问题：管理员在 Web 端触发训练后，任务状态是否能保存到数据库，服务重启后是否还能查到。两者不冲突，但当前课程交付优先保证实验留痕完整。

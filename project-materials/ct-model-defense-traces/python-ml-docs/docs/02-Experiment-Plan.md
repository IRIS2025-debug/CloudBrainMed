# 调参实验方案

> 核心目标：跑出 UNet vs AttentionUNet 在不同超参组合下的指标对比

## 实验设计

控制 5 个关键超参数，设计 6 组对比实验：

| 实验编号 | 模型类型 | 优化器 | 学习率 | Batch Size | 预期耗时 (RTX 4090) |
|---------|---------|--------|--------|-----------|-------------------|
| E1 | unet | adamw | 1e-4 | 8 | ~20 min |
| E2 | unet | adam | 1e-4 | 8 | ~20 min |
| E3 | unet | sgd | 1e-3 | 8 | ~20 min |
| E4 | attention | adamw | 1e-4 | 8 | ~25 min |
| E5 | unet | adamw | 5e-4 | 8 | ~20 min |
| E6 | unet | adamw | 1e-4 | 16 | ~15 min |

## 一键跑完脚本

在 AutoDL 实例上执行以下命令即可依次跑完所有实验：

```bash
cd /root/autodl-tmp/python-ml

# 先检查将要执行的六条训练命令
python tools/run_six_experiments.py --dry-run

# 确认参数无误后正式顺序运行 E1-E6，并在结束后生成汇总表
python tools/run_six_experiments.py
```

## 对比表生成

跑完后，在 `python-ml/` 目录执行统一汇总脚本：

```bash
python tools/summarize_experiments.py
```

脚本会读取每个实验目录中的 `config.yaml` 和 `metrics.json`，自动识别 E1-E6，并输出：

- `docs/generated/experiment-summary.md`
- `docs/generated/experiment-summary.csv`

把 Markdown 中的“对比表”“最优模型建议”“留痕完整性检查”复制到 `docs/04-Experiment-Results.md` 或最终报告中即可。

## 实验结果解读

训练完成后，每个实验目录包含：

```
experiments/{timestamp}_{model}_{optimizer}/
├── config.yaml              # 超参配置（留痕）
├── train.log                # 完整训练日志（留痕）
├── best.pth                 # 最优权重
├── last.pth                 # 最后一轮权重
├── loss_curve.png           # 损失曲线
├── dice_f1_curve.png        # Dice/F1 曲线
├── acc_curve.png            # 精度曲线
├── confusion.png            # 混淆矩阵
├── pr_curve.png             # PR 曲线
├── pred_sample.png          # 预测样例
└── metrics.json             # 最终指标
```

## 预期结论

根据 CT 金属伪影检测的文献经验，预期：

1. **UNet vs AttentionUNet** — AttentionUNet 的 Dice 应该高 2-5%，因为注意力机制能更好捕捉伪影边界
2. **AdamW vs Adam** — AdamW 的泛化性略好，Dice 相近但验证集更稳定
3. **SGD** — 收敛更慢，可能需要更长训练轮数或更大学习率
4. **学习率 5e-4** — 可能前期收敛快但最终指标略低于 1e-4
5. **Batch 16** — 训练更快，但 Dice 可能比 Batch 8 低 1-2%

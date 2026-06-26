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

在 AutoDL 实例上执行以下脚本即可依次跑完所有实验：

```bash
#!/bin/bash
# run_all_experiments.sh — 在 AutoDL GPU 实例上执行
cd /root/autodl-tmp/python-ml

# E1: UNet + AdamW（基线）
echo "=========================================="
echo "E1: UNet + AdamW, lr=1e-4, batch=8"
echo "=========================================="
python -m training.train \
  --model unet --optimizer adamw --epochs 100 \
  --batch-size 8 --accum 1 --fp16 --lr 1e-4

# E2: UNet + Adam
echo "=========================================="
echo "E2: UNet + Adam, lr=1e-4, batch=8"
echo "=========================================="
python -m training.train \
  --model unet --optimizer adam --epochs 100 \
  --batch-size 8 --accum 1 --fp16 --lr 1e-4

# E3: UNet + SGD
echo "=========================================="
echo "E3: UNet + SGD, lr=1e-3, batch=8"
echo "=========================================="
python -m training.train \
  --model unet --optimizer sgd --epochs 100 \
  --batch-size 8 --accum 1 --fp16 --lr 1e-3

# E4: AttentionUNet + AdamW
echo "=========================================="
echo "E4: AttentionUNet + AdamW, lr=1e-4, batch=8"
echo "=========================================="
python -m training.train \
  --model attention --optimizer adamw --epochs 100 \
  --batch-size 8 --accum 1 --fp16 --lr 1e-4

# E5: UNet + AdamW, 大学习率
echo "=========================================="
echo "E5: UNet + AdamW, lr=5e-4, batch=8"
echo "=========================================="
python -m training.train \
  --model unet --optimizer adamw --epochs 100 \
  --batch-size 8 --accum 1 --fp16 --lr 5e-4

# E6: UNet + AdamW, 大 batch
echo "=========================================="
echo "E6: UNet + AdamW, lr=1e-4, batch=16"
echo "=========================================="
python -m training.train \
  --model unet --optimizer adamw --epochs 100 \
  --batch-size 16 --accum 1 --fp16 --lr 1e-4

echo ""
echo "所有实验完成！结果保存在 experiments/ 目录"
ls experiments/
```

## 对比表模板

跑完后，用以下脚本自动生成对比表：

```bash
# generate_comparison_table.sh
echo "实验 | 模型 | 优化器 | LR | Batch | Best Dice | Best F1 | Best Acc | 耗时"
echo "-----|------|--------|----|-------|----------|---------|---------|------"
for d in experiments/*/; do
  name=$(basename "$d")
  if [ -f "$d/metrics.json" ]; then
    dice=$(python -c "import json; d=json.load(open('${d}metrics.json')); print(f\"{d['best_dice']:.4f}\")")
    f1=$(python -c "import json; d=json.load(open('${d}metrics.json')); print(f\"{d['best_metrics']['f1']:.4f}\")")
    acc=$(python -c "import json; d=json.load(open('${d}metrics.json')); print(f\"{d['best_metrics']['accuracy']:.4f}\")")
    echo "$name | ... | ... | ... | ... | $dice | $f1 | $acc |"
  fi
done
```

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

## 预期结论

根据 CT 金属伪影检测的文献经验，预期：

1. **UNet vs AttentionUNet** — AttentionUNet 的 Dice 应该高 2-5%，因为注意力机制能更好捕捉伪影边界
2. **AdamW vs Adam** — AdamW 的泛化性略好，Dice 相近但验证集更稳定
3. **SGD** — 收敛更慢，可能需要更长训练轮数或更大学习率
4. **学习率 5e-4** — 可能前期收敛快但最终指标略低于 1e-4
5. **Batch 16** — 训练更快，但 Dice 可能比 Batch 8 低 1-2%

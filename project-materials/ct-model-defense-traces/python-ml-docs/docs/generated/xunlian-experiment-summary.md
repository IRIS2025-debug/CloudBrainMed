# 队友六组训练结果汇总

> 根据 `project-materials/ct-model-defense-traces/xunlian/*.zip` 中的 `config.yaml` 和 `metrics.json` 自动生成，未解压大体积权重包。

## 对比表

| 实验 | 压缩包 | 模型 | 优化器 | LR | Batch | 梯度累积 | 等效Batch | Epochs | Best Dice | Best F1 | Best Acc |
|---|---|---|---|---|---|---|---|---|---|---|---|
| E1 | `20260626_142848_unet_adamw.zip` | unet | adamw | 0.0001 | 8 | 1 | 8 | 100 | 0.3114 | 0.7360 | 0.9988 |
| E2 | `20260627_120749_unet_adam.zip` | unet | adam | 0.0001 | 8 | 1 | 8 | 100 | 0.3265 | 0.7433 | 0.9988 |
| E3 | `20260628_084614_unet_sgd.zip` | unet | sgd | 0.0010 | 8 | 1 | 8 | 100 | 0.2719 | 0.7036 | 0.9988 |
| E4 | `20260628_172419_attention_adamw.zip` | attention | adamw | 0.0001 | 2 | 4 | 8 | 100 | 0.3508 | 0.7242 | 0.9988 |
| E5 | `20260629_150658_unet_adamw.zip` | unet | adamw | 0.0005 | 8 | 1 | 8 | 100 | 0.2499 | 0.6985 | 0.9988 |
| E6 | `20260630_143434_unet_adamw.zip` | unet | adamw | 0.0001 | 4 | 4 | 16 | 100 | 0.2963 | 0.6644 | 0.9984 |

## 最优模型

- 最优实验：E4 `20260628_172419_attention_adamw.zip`。
- 最优模型：attention + adamw，Best Dice=0.3508，Best F1=0.7242。
- 已复制其 `best.pth` 为 `Model/weights/best_attention_adamw.pth`，供 Python 推理服务默认加载。

## 留痕完整性

- E1 `20260626_142848_unet_adamw.zip` 已包含核心留痕文件。
- E2 `20260627_120749_unet_adam.zip` 已包含核心留痕文件。
- E3 `20260628_084614_unet_sgd.zip` 已包含核心留痕文件。
- E4 `20260628_172419_attention_adamw.zip` 已包含核心留痕文件。
- E5 `20260629_150658_unet_adamw.zip` 已包含核心留痕文件。
- E6 `20260630_143434_unet_adamw.zip` 已包含核心留痕文件。

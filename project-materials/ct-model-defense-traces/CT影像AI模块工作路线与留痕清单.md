# CT 影像 AI 模块工作路线与留痕清单

## 1. 当前结论

老师现在说的“AI智慧云脑诊疗平台 / AI辅助诊断平台 / 伪影识别 / 病灶识别 / 病灶分割 / 文字描述”，可以拆成两类工作：

| 层级 | 含义 | 当前项目状态 | 建议交付定位 |
|---|---|---|---|
| 伪影识别/分割 | 识别 CT 金属伪影，并输出伪影区域 mask | 已有 Python 推理、UNet/AttentionUNet、Java MLOps 接口、前端 CT 页面基础 | 作为本模块核心交付 |
| 病灶识别 | 判断 CT 中是否存在病灶及类别 | 当前未发现对应病灶数据集、病灶标签或训练代码 | 作为平台扩展能力，不承诺完整训练 |
| 病灶分割 | 输出病灶区域 mask | 当前没有病灶 mask 标注 | 作为扩展方向，除非补充数据 |
| 文字描述 | 根据识别/分割结果生成报告描述 | 系统已有大模型接入思路，可由 DeepSeek/通义等生成文本 | 用大模型基于结构化结果生成辅助描述 |

因此当前最稳妥的答辩口径是：本阶段以“CT 金属伪影识别/分割”为主线，补齐训练调参和留痕；病灶识别、病灶分割是同一平台架构下的可扩展任务；文字描述由大模型根据模型输出的结构化结果生成。

## 2. 已恢复与已找到的材料

### 2.1 已恢复到项目根目录

| 路径 | 内容 | 用途 |
|---|---|---|
| `teach/` | 老师给的 2D UNet / AttentionUNet / 推理入口示例 | 对照老师原始代码与当前 Python ML 模块 |
| `course/` | 标注工具、3D U-Net、数据预处理、滤波、可视化、PIMA/PyTorch 课程示例等 | 课程资料与早期代码参考 |

恢复来源：git 历史提交 `dc7c84222f4b5e5fb4f1ce3c227188788b15001c`。恢复时排除了 `__pycache__` 和 `.pyc`。

### 2.2 知识库可参考资料

| 路径 | 关键信息 |
|---|---|
| `D:\LLM -WIKI\llm-wiki\wiki\log.md` | 记录老师提供 BrainCT.zip、100 轮训练要求、3D 转 2D 原因、数据被清空后恢复过 |
| `D:\LLM -WIKI\llm-wiki\wiki\comparisons\模型对比实验.md` | 记录旧 3 组实验指标：Attention UNet2D + AdamW 最优 Dice 0.1592 |
| `D:\LLM -WIKI\llm-wiki\wiki\concepts\U-Net网络详解.md` | U-Net 原理、训练要点、模型代码与训练代码分离原则 |
| `D:\LLM -WIKI\llm-wiki\raw\articles\5.26讲课内容.docx` | SimpleITK 相关课程逐字稿 |
| `D:\LLM -WIKI\llm-wiki\raw\articles\5.27讲课内容.docx` | 滤波、VTK、标注工具相关课程逐字稿 |
| `D:\LLM -WIKI\llm-wiki\raw\articles\5.28讲课内容（数据集）.docx` | CQ500 / 数据集相关课程逐字稿 |
| `D:\LLM -WIKI\llm-wiki\raw\articles\5.29讲课内容.docx` | 标注点评、U-Net 选型相关课程逐字稿 |
| `D:\LLM -WIKI\llm-wiki\raw\assets\CT Plain.zip` | DICOM 测试数据，不等同于完整训练集 |
| `D:\LLM -WIKI\llm-wiki\raw\repos\CTAnnotationTool` | CT 标注工具源码 |
| `D:\LLM -WIKI\llm-wiki\outputs\vue-ct-viewer` | Vue CT 查看/标注组件模板 |

## 3. 当前缺口

| 缺口 | 证据 | 处理方式 |
|---|---|---|
| 完整训练数据 | 已从 `xunlian/ct_artifact_dataset.zip` 解压到 `python-ml/data/ct_artifact_dataset`，包含 460 对 CT/MASK DICOM | 已补齐；`training/config.yaml` 已指向该数据集 |
| 六组新实验结果 | `xunlian/` 包含 6 个训练 zip，每个 zip 有 config、train.log、metrics、best/last 权重、曲线图和预测样例 | 已生成 `python-ml/docs/generated/xunlian-experiment-summary.md` |
| 病灶识别/分割数据不存在 | 未发现病灶类别标签或病灶 mask | 不作为当前核心承诺，除非老师提供病灶数据 |

## 3.1 当前闭环状态

| 环节 | 状态 | 证据 |
|---|---|---|
| 数据集 | 已闭环 | `backEnd/services/ai-service/python-ml/data/ct_artifact_dataset/CT` 460 张，`MASK` 460 张 |
| 六组训练 | 已闭环 | `D:\CloudBrainMed\xunlian\20260626_142848_unet_adamw.zip` 等 6 个实验包 |
| 指标汇总 | 已闭环 | `backEnd/services/ai-service/python-ml/docs/generated/xunlian-experiment-summary.md` |
| 最优模型 | 已闭环 | E4 AttentionUNet + AdamW，Best Dice 0.3508 |
| 推理部署 | 已接入 | `Model/weights/best_attention_adamw.pth`，`CTDetectionServer.py` 默认 `MODEL_TYPE=attention` |
| 大模型文字描述 | 设计闭环 | 可根据伪影 mask、模型版本、置信度等结构化结果生成辅助报告草稿 |
| 病灶识别/病灶分割 | 未闭环 | 缺病灶标签和病灶 mask 数据，不建议承诺已完成 |

## 4. 要做的步骤

### 第一步：整理数据

目标目录建议使用：

```text
backEnd/services/ai-service/python-ml/data/
├── raw/                 # 原始 CT / DICOM / NIfTI，只本地或云盘保存，不建议进 git
├── slices/images/       # 2D CT 切片
├── slices/masks/        # 对应伪影 mask
└── README.md            # 记录数据来源、数量、标注人、转换脚本、时间
```

如果沿用老师原始工程路径，则需要准备：

```text
BrainCT/BrainCT/Datasets/CT/
BrainCT/BrainCT/Datasets/MASK/
```

验收证据：

- 数据目录截图。
- 样本数量统计截图。
- 至少 3 张 CT 与 mask 对应样例截图。
- 数据来源说明：老师数据 / 自己标注 / 组员提供 / 公开数据。

### 第二步：确认训练脚本能读到数据

进入：

```powershell
cd D:\CloudBrainMed\backEnd\services\ai-service\python-ml
```

检查配置：

```powershell
Get-Content training\config.yaml
```

需要确认：

- CT 图像路径正确。
- mask 路径正确。
- 模型类型支持 `unet` 和 `attention`。
- 输出目录是 `experiments/`。

验收证据：

- `python -m training.train --help` 可执行。
- 跑 1 个很小的 smoke test，例如 1 epoch，能创建实验目录。

### 第三步：购买/租用算力并留痕

推荐 AutoDL：

| 项 | 建议 |
|---|---|
| GPU | RTX 4090 或 RTX 3090 |
| 镜像 | PyTorch 2.x + CUDA 12.x |
| 计费 | 按量计费 |
| 预计费用 | 六组实验约 10-20 元，视 epoch 和数据量而定 |

必须截图：

- AutoDL 实例配置截图。
- GPU 型号截图。
- 余额/订单/计费截图。
- `nvidia-smi` 截图。
- 训练开始和训练结束截图。
- 关机截图。

### 第四步：跑六组调参实验

在 AutoDL 中执行：

```bash
cd /root/autodl-tmp/python-ml
python tools/run_six_experiments.py --dry-run
python tools/run_six_experiments.py
python tools/summarize_experiments.py
```

六组实验：

| 编号 | 模型 | 优化器 | 学习率 | Batch |
|---|---|---|---|---|
| E1 | UNet | AdamW | 1e-4 | 8 |
| E2 | UNet | Adam | 1e-4 | 8 |
| E3 | UNet | SGD | 1e-3 | 8 |
| E4 | AttentionUNet | AdamW | 1e-4 | 8 |
| E5 | UNet | AdamW | 5e-4 | 8 |
| E6 | UNet | AdamW | 1e-4 | 16 |

每组必须保留：

- `config.yaml`
- `train.log`
- `metrics.json`
- `best.pth`
- `last.pth`
- `loss_curve.png`
- `dice_f1_curve.png`
- `acc_curve.png`
- `confusion.png`
- `pr_curve.png`
- `pred_sample.png`

### 第五步：整理结果并选择模型

跑完后把 AutoDL 的结果下载回：

```text
backEnd/services/ai-service/python-ml/experiments/
```

再生成：

```powershell
cd D:\CloudBrainMed\backEnd\services\ai-service\python-ml
python tools\summarize_experiments.py
```

把生成的：

```text
docs/generated/experiment-summary.md
docs/generated/experiment-summary.csv
```

同步进项目报告。

选模型规则：

1. 先看 Best Dice，因为这是分割任务最核心指标。
2. Dice 接近时看 F1、Recall、预测样例是否漏检少。
3. 如果 AttentionUNet 指标明显好于 UNet，就选 AttentionUNet。
4. 如果提升很小但耗时很大，报告里说明部署时为什么选择更轻模型。

### 第六步：系统集成演示

演示链路：

```text
前端上传 CT
  -> Java MlOpsController
  -> Python CTDetectionServer
  -> UNet/AttentionUNet 推理
  -> 返回 mask + 结构化指标
  -> 前端显示伪影区域
  -> 大模型生成文字描述
```

结构化结果示例：

```json
{
  "task": "ct_metal_artifact_segmentation",
  "artifactDetected": true,
  "artifactType": "metal_streak",
  "severity": "moderate",
  "affectedRegion": "right temporal area",
  "maskFilename": "xxx_mask.png",
  "model": "AttentionUNet2D",
  "modelVersion": "E4_best",
  "confidence": 0.82
}
```

大模型文字描述只负责把结构化结果写成报告草稿，例如：

```text
本次 CT 图像可见条纹状金属伪影，主要位于右侧颞部区域，局部影响图像观察质量。建议结合原始影像及临床资料，由影像科医生进一步判断。
```

注意：不要让大模型凭空诊断病灶。没有病灶识别/分割模型结果时，只能写“未接入病灶识别模块”或“本次演示聚焦金属伪影识别”。

## 5. 对老师的解释口径

可以这样说：

```text
老师，我们现在把这个模块按医学影像 AI 流程拆成四层：伪影识别、病灶识别、病灶分割和文字描述。

当前我们已经有 CT 金属伪影识别/分割的 UNet 和 AttentionUNet 训练推理代码，也接入了 Java MLOps 和前端上传检测页面。接下来会补齐六组调参实验，用 AutoDL GPU 跑训练，保留每组 config、train.log、metrics、曲线图、预测样例、模型权重和算力订单截图，形成完整留痕。

病灶识别和病灶分割需要额外的病灶标签或 mask 数据，目前没有对应数据集，所以本阶段作为平台扩展能力写进设计。文字描述部分可以由大模型根据伪影识别/分割的结构化结果生成辅助报告草稿。
```

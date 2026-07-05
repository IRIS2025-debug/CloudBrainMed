# CT 金属伪影检测 — AI 训练与推理

> Note: training data, six experiment outputs, and defense evidence have been
> moved to `../../../../project-materials/CT金属伪影模型留痕/`. This backend
> folder keeps only runtime inference code and deployed weights.

## 项目结构

```
python-ml/
├── README.md                          ← 本文件：完整执行手册
│
├── CTDetectionServer.py               # FastAPI 推理服务
├── requirements.txt                   # 依赖
│
├── Model/                             # 模型定义
│   ├── UNet2D.py                      #   UNet 基础版
│   ├── AttentionUNet2D.py             #   注意力增强版
│   └── weights/best.pth               #   当前最优权重
│
├── Detection/                         # 推理引擎
│   └── CTArtifactInfer.py             #   加载模型 → 切片推理 → 生成掩码
│
├── training/                          # ← 训练核心
│   ├── train.py                       #   训练主入口
│   ├── trainer.py                     #   训练引擎
│   ├── config.yaml                    #   默认超参
│   ├── dataset.py                     #   数据加载
│   ├── losses.py                      #   损失函数
│   ├── metrics.py                     #   评估指标
│   ├── visualize.py                   #   图表生成
│   └── augment.py                     #   数据增强
│
├── experiments/                       # ← 训练结果自动保存
│   └── {timestamp}_{model}_{opt}/
│       ├── config.yaml                #   配置快照
│       ├── train.log                  #   训练日志
│       ├── best.pth                   #   最优权重
│       ├── last.pth                   #   最后一轮权重
│       ├── loss_curve.png             #   损失曲线
│       ├── dice_f1_curve.png          #   Dice/F1 曲线
│       ├── acc_curve.png              #   精度曲线
│       ├── confusion.png              #   混淆矩阵
│       ├── pr_curve.png               #   PR 曲线
│       ├── pred_sample.png            #   预测样例图
│       └── metrics.json               #   最终指标
│
└── docs/                              # ← 使用文档
    ├── 01-AutoDL-GPU-Guide.md         #   GPU 配置指南
    ├── 02-Experiment-Plan.md          #   调参实验方案
    └── 03-Report-Framework.md         #   报告框架
```

---

## 训练流程

### 1. 最简运行

```bash
cd backEnd/services/ai-service/python-ml
python -m training.train
```

### 病灶识别/分割训练

默认推荐公开数据集：Medical Segmentation Decathlon `Task10_Colon.tar`，下载地址：

```text
https://msd-for-monai.s3.us-west-2.amazonaws.com/Task10_Colon.tar
```

该数据集为 CT 肿瘤分割任务，`labelsTr` 中前景标签可直接作为病灶 mask。先抽取少量病例并转换成现有 2D 训练格式：

```bash
python tools/prepare_msd_lesion_dataset.py \
  --archive D:/datasets/Task10_Colon.tar \
  --output-dir data/CT病灶数据集 \
  --max-cases 8
```

然后使用病灶配置训练 Attention U-Net：

```bash
python -m training.train --config training/config_lesion.yaml
```

`training/config_lesion.yaml` 会把最佳权重同步到 `Model/weights/best_lesion_attention.pth`。推理服务默认通过 `LESION_MODEL_PATH` 加载该权重；如果权重还不存在，`/predict-ct-lesion` 会返回 `fallback=true`，表示当前只是启发式候选结果，不能当作训练模型诊断结论。

### 2. 指定参数运行

```bash
# 修改模型、优化器、轮数、学习率
python -m training.train \
  --model attention \
  --optimizer adamw \
  --epochs 100 \
  --batch-size 4 \
  --lr 1e-4 \
  --fp16
```

### 3. 完整流程分解

```
python -m training.train
  │
  ├── ① train.py 读取 config.yaml
  │      命令行参数覆盖（如 --model attention）
  │
  ├── ② 创建实验目录
  │      experiments/{时间戳}_{模型}_{优化器}/
  │
  ├── ③ 加载模型（UNet / AttentionUNet）
  │
  ├── ④ 加载数据
  │      BrainCT/BrainCT/Datasets/CT/     ← 460 张 DCM 切片
  │      BrainCT/BrainCT/Datasets/MASK/   ← 460 张对应掩码
  │      训练集 368 张 / 验证集 92 张
  │
  ├── ⑤ 训练循环（trainer.py）
  │      for epoch in 1..100:
  │        训练一轮 → 验证一轮 → 记录指标
  │        每轮打印: Epoch 50 | Dice: 0.78 | F1: 0.77 | ...
  │        Dice 创新高 → 保存 best.pth（同时同步到 Model/weights/best.pth）
  │        每 10 轮 → 重绘图表
  │        连续 20 轮不提升 → 早停
  │
  ├── ⑥ 最终保存
  │      图表 + metrics.json
  │
  └── ⑦ 验证推理链路
        最佳权重 → CTArtifactInfer 加载 → 预测样例
```

---

## 调优过程怎么体现（给老师看）

### 每次训练都是一条完整记录

```
experiments/20260625_152247_unet_adamw/    ← 时间戳 + 模型 + 优化器
├── config.yaml          ← 本次的所有超参
├── train.log            ← 每行日志
├── best.pth             ← 最佳模型
├── loss_curve.png       ← 损失下降曲线
├── dice_f1_curve.png    ← Dice/F1 上升曲线
└── metrics.json         ← 最终指标
```

### 多组实验自动形成对比

```
experiments/
├── 20260625_152247_unet_adamw          # UNet + AdamW
├── 20260625_163801_unet_adam           # UNet + Adam
├── 20260625_171234_unet_sgd            # UNet + SGD
├── 20260625_174567_attention_adamw     # AttentionUNet + AdamW
├── 20260625_181234_unet_adamw_highlr   # 高学习率
└── 20260625_184567_unet_adamw_large    # 大 batch
```

生成对比表：

```bash
python tools/summarize_experiments.py
```

脚本会生成：

- `docs/generated/experiment-summary.md`：可直接复制进报告的对比表、最优模型建议和留痕完整性检查。
- `docs/generated/experiment-summary.csv`：便于在 Excel/WPS 中继续整理。

### 直接用在报告中

- 对比表 → 报告第 3 章"超参数调优"
- loss_curve.png → 报告第 4 章"训练结果"
- dice_f1_curve.png → 报告第 4 章
- pred_sample.png → 报告第 4 章
- experiments/ 目录截图 → 报告第 5 章"实验记录"

---

## 剩余步骤 — 只差跑 GPU

> 文档 `docs/01-AutoDL-GPU-Guide.md` 有完整操作步骤

**① 注册 AutoDL（约 2 分钟）**
https://www.autodl.com/ → 注册 → 实名认证

**② 创建 GPU 实例（约 2 分钟）**
- GPU: RTX 4090（¥3.5/小时）
- 镜像: PyTorch 2.x + CUDA 12.x

**③ 上传代码（约 5 分钟）**
```bash
# 在 JupyterLab Terminal 中
cd /root/autodl-tmp
# 拖拽上传 python-ml/ 整个目录
# 或用 git clone 你的仓库
```

**④ 跑 6 组实验（约 2 小时，¥10-20）**
```bash
cd /root/autodl-tmp/python-ml

# 先检查将要执行的六条训练命令
python tools/run_six_experiments.py --dry-run

# 确认参数无误后正式顺序运行 E1-E6，并在结束后生成汇总表
python tools/run_six_experiments.py
```

**⑤ 下载结果**
```bash
tar czf results.tar.gz experiments/
# 在 JupyterLab 下载 results.tar.gz
```

**⑥ 关机**
- 在 AutoDL 控制台点击"关机"
- 不关机 = 持续扣费

---

## Java 集成

Java 端的训练接口在 `ModelTrainer.java`，流程：

```
HTTP POST /admin-service/ml/models/train
  body: { "modelType": "unet", "epochs": 100, ... }
    │
    ├── MlOpsController 接收请求
    ├── MlOpsService 创建训练任务
    ├── ModelTrainer:
    │     ① ConfigYamlBuilder 将参数转成 config.yaml
    │     ② ProcessBuilder 启动 python -m training.train
    │     ③ 实时读取 Python 输出到 Java 日志
    │     ④ 训练完成 → 读取 metrics.json
    │     ⑤ 注册模型到 ModelLoader
    │
    └── 返回 { "taskId": "TRN...", "status": "COMPLETED" }
```

---

## 文件索引

| 用途 | 文件 |
|------|------|
| 训练入口 | `training/train.py` |
| 训练引擎 | `training/trainer.py` |
| 默认配置 | `training/config.yaml` |
| 数据加载 | `training/dataset.py` |
| 损失函数 | `training/losses.py` |
| 评估指标 | `training/metrics.py` |
| 图表生成 | `training/visualize.py` |
| 数据增强 | `training/augment.py` |
| 模型-UNet | `Model/UNet2D.py` |
| 模型-Attention | `Model/AttentionUNet2D.py` |
| 推理服务 | `CTDetectionServer.py` |
| 推理引擎 | `Detection/CTArtifactInfer.py` |
| GPU 指南 | `docs/01-AutoDL-GPU-Guide.md` |
| 实验方案 | `docs/02-Experiment-Plan.md` |
| 报告框架 | `docs/03-Report-Framework.md` |
| Java 训练接口 | `../src/.../model/ModelTrainer.java` |

---

## 本地服务端口

Python AI 服务默认监听 `http://localhost:8010`，Java 后端默认通过 `AI_PYTHON_SERVICE_URL` 调用该地址。

不要把 Python AI 服务放在 `8000` 端口；本项目 `admin-service` 默认使用 `8000`。如需自定义端口：

```bash
PORT=8010 python CTDetectionServer.py
```

Windows PowerShell：

```powershell
$env:PORT="8010"
python CTDetectionServer.py
```

同时让 Java 后端使用同一个地址：

```bash
AI_PYTHON_SERVICE_URL=http://localhost:8010
```

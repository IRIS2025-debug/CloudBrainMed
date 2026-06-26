# GPU 算力配置指南

> 对应老师说的"买算力" —— 在云 GPU 上跑 CT 金属伪影检测训练

## 方案选择

| 平台 | 价格参考 | 推荐机型 | 理由 |
|------|---------|---------|------|
| **AutoDL** | ¥2-5/小时 | RTX 4090 (24GB) | 国内最方便，按量计费，学生认证有优惠 |
| 腾讯云 | ¥8-15/小时 | GN10Xp (V100) | 稳定，有学生优惠 |
| 阿里云 PAI | ¥10-20/小时 | ecs.gn6i | 功能多，略贵 |

**推荐 AutoDL**，性价比最高，也是最常见的方案。

---

## 操作步骤（AutoDL）

### 1. 注册与认证

1. 打开 https://www.autodl.com/，手机号注册
2. 完成实名认证（支付宝/微信）
3. 学生认证可享受折扣（选做）

### 2. 创建 GPU 实例

1. 进入控制台 → **GPU 云服务器** → **创建实例**
2. 配置参数：

   | 配置项 | 推荐值 |
   |--------|--------|
   | 计费方式 | **按量计费**（不用时关机，只收磁盘费 ≈ ¥0.1/天） |
   | 地区 | 靠近你所在区域（华北/华东） |
   | GPU 型号 | **RTX 4090**（24GB）或 **RTX 3090** |
   | 镜像 | **PyTorch 2.x + Python 3.11 + CUDA 12.x** |
   | 数据盘 | 30GB 够用 |

3. 确认创建，等待实例启动（约 1-2 分钟）

### 3. 上传代码

实例启动后，有四种方式上传：

#### 方式 A：JupyterLab（最推荐）
1. 在实例列表点击 **JupyterLab**
2. 打开 Terminal
3. 上传代码：
   ```bash
   # 在 JupyterLab Terminal 中执行
   cd /root/autodl-tmp
   # 如果你是 git 管理的项目：
   git clone <你的仓库地址>
   # 如果不是，用 JupyterLab 左侧文件上传面板直接拖拽 python-ml/ 目录
   ```

#### 方式 B：scp 上传（需要本地终端）
```bash
# 先找到你的实例 SSH 登录指令（AutoDL 控制台有显示）
scp -r /本地路径/python-ml root@<实例IP>:/root/autodl-tmp/
```

#### 方式 C：AutoDL-File 文件传输
1. 控制台 → 文件管理 → 上传
2. 把 `python-ml/` 打包成 zip 上传
3. 在实例里 `unzip`

### 4. 准备数据

```bash
cd /root/autodl-tmp/python-ml

# 检查 GPU 是否可用
python -c "import torch; print(torch.cuda.is_available(), torch.cuda.get_device_name(0))"
# 应输出: True NVIDIA GeForce RTX 4090

# 检查 DCM 数据集路径
ls -la ../../../../BrainCT/BrainCT/Datasets/CT | head -5
# 如果路径不对，修正 config.yaml 中 data.ct_dir 和 data.mask_dir
```

> 注意：如果你没有把 `BrainCT/BrainCT/Datasets/` 一起上传，需要单独上传或用 AutoDL 的网盘功能映射。

### 5. 运行训练

```bash
cd /root/autodl-tmp/python-ml

# 实验 1：UNet + AdamW（默认配置，100 轮）
python -m training.train \
  --model unet --optimizer adamw --epochs 100 \
  --batch-size 4 --accum 2 --fp16

# 实验 2：AttentionUNet + AdamW
python -m training.train \
  --model attention --optimizer adamw --epochs 100 \
  --batch-size 4 --accum 2 --fp16

# 实验 3：UNet + Adam（不同优化器对比）
python -m training.train \
  --model unet --optimizer adam --epochs 100 \
  --batch-size 4 --accum 2 --fp16
```

### 6. 下载结果

```bash
# 训练完成后，experiments/ 目录包含所有结果
ls experiments/
# 输出类似：
# 20260625_152247_unet_adamw
# 20260625_163801_attn_adamw

# 打包下载
tar czf results.tar.gz experiments/
# 在 JupyterLab 中直接下载 results.tar.gz
```

### 7. 关机（重要！）

```bash
# 在 AutoDL 控制台点击「关机」
# 按量计费扣的是 GPU 使用时间，不关机会持续扣费
```

**关机后数据还在**，下次开机可以继续。

---

## 节省费用技巧

| 场景 | 策略 | 预估费用 |
|------|------|---------|
| 单次跑 100 轮（RTX 4090）| 约 30 分钟 | ¥1.5-2.5 |
| 跑 6 组实验对比 | 约 2-3 小时 | ¥6-15 |
| 只调试代码 | 用 CPU 实例（¥0.5/小时） | ¥0.5 |
| 长期开发 | 关机不释放磁盘 | ≈ ¥0.1/天 |

**总结：全部实验跑完大概 ¥10-20，远低于本地买显卡的成本。**

---

## 附录：常见问题

**Q: 实例连接超时？**
A: 检查是否已开机，首次连接可能需要等 1-2 分钟。

**Q: 显存不够？**
A: 尝试减小 `--batch-size`（1 或 2）或增大 `--accum`（8 或 16）。

**Q: 上传速度慢？**
A: 用 AutoDL 网盘（10MB/s）比 scp 快，或者压缩后上传。

**Q: 训练中途断开？**
A: 使用 `nohup` 或 `tmux` 保持会话：
```bash
tmux new -s train
python -m training.train ...  # 训练命令
# 按 Ctrl+B 再按 D 断开，下次 tmux attach -t train 恢复
```

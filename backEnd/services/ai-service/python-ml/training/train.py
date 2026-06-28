#!/usr/bin/env python
"""
CT 金属伪影检测 — 训练主入口

用法:
    # 使用默认配置
    python -m training.train

    # 指定配置文件
    python -m training.train --config training/config.yaml

    # 快捷指定模型类型和优化器
    python -m training.train --model attention --optimizer adamw --epochs 100

    # 用 GPU 显存小的配置
    python -m training.train --batch-size 1 --accum 8 --fp16
"""
import os
import sys
import argparse
import yaml
import shutil
from datetime import datetime

# 确保 python-ml/ 在 sys.path 中
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

import logging
logger = logging.getLogger(__name__)

import torch
import numpy as np
import random


def parse_args():
    parser = argparse.ArgumentParser(description="CT 金属伪影检测训练")
    parser.add_argument("--config", default=None, help="YAML 配置文件路径")
    parser.add_argument("--model", choices=["unet", "attention"], default=None)
    parser.add_argument("--optimizer", choices=["adam", "adamw", "sgd"], default=None)
    parser.add_argument("--epochs", type=int, default=None)
    parser.add_argument("--batch-size", type=int, default=None)
    parser.add_argument("--lr", type=float, default=None)
    parser.add_argument("--accum", type=int, default=None)
    parser.add_argument("--fp16", action="store_true", default=None)
    parser.add_argument("--no-aug", action="store_true", help="禁用数据增强")
    return parser.parse_args()


def load_config(args):
    """加载并合并配置"""
    # 默认配置
    config_path = args.config or os.path.join(
        os.path.dirname(__file__), "config.yaml"
    )
    with open(config_path, "r", encoding="utf-8") as f:
        config = yaml.safe_load(f)

    # 命令行参数覆盖
    if args.model:
        config["model"]["type"] = args.model
    if args.optimizer:
        config["training"]["optimizer"] = args.optimizer
    if args.epochs:
        config["training"]["epochs"] = args.epochs
    if args.batch_size:
        config["training"]["batch_size"] = args.batch_size
    if args.lr:
        config["training"]["learning_rate"] = args.lr
    if args.accum:
        config["training"]["gradient_accumulation"] = args.accum
    if args.fp16:
        config["training"]["fp16"] = True
    if args.no_aug:
        config["augmentation"]["enabled"] = False

    return config


def create_experiment_dir(config):
    """创建实验输出目录"""
    model_type = config["model"]["type"]
    optimizer = config["training"]["optimizer"]
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    exp_name = f"{timestamp}_{model_type}_{optimizer}"

    root = config.get("logging", {}).get("experiment_root", "experiments")
    exp_dir = os.path.join(os.path.dirname(__file__), "..", root, exp_name)
    os.makedirs(exp_dir, exist_ok=True)

    # 保存本次使用的配置文件
    config_save_path = os.path.join(exp_dir, "config.yaml")
    with open(config_save_path, "w", encoding="utf-8") as f:
        yaml.dump(config, f, default_flow_style=False, allow_unicode=True)

    logger.info(f"\n实验目录: {exp_dir}")
    return exp_dir


def setup_logging(exp_dir):
    """配置日志：同时输出到控制台和实验目录下的 train.log"""
    log_path = os.path.join(exp_dir, "train.log")

    # 清除已有的 handlers
    root_logger = logging.getLogger()
    for handler in root_logger.handlers[:]:
        root_logger.removeHandler(handler)

    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s | %(name)s | %(message)s",
        datefmt="%H:%M:%S",
        handlers=[
            logging.StreamHandler(sys.stdout),
            logging.FileHandler(log_path, encoding="utf-8"),
        ],
    )
    return logging.getLogger(__name__)


def set_seed(seed):
    """设置随机种子"""
    random.seed(seed)
    np.random.seed(seed)
    torch.manual_seed(seed)
    if torch.cuda.is_available():
        torch.cuda.manual_seed_all(seed)


def main():
    # 检查 GPU
    if torch.cuda.is_available():
        logger.info(f"✅ GPU 可用: {torch.cuda.get_device_name(0)}")
        logger.info(f"   显存: {torch.cuda.get_device_properties(0).total_memory / 1e9:.2f} GB")
    else:
        logger.warning("⚠️  GPU 不可用，使用 CPU 训练")

    args = parse_args()
    config = load_config(args)

    # 随机种子
    set_seed(config.get("data", {}).get("seed", 42))

    # 创建实验目录
    exp_dir = create_experiment_dir(config)
    setup_logging(exp_dir)

    # 导入（延迟导入，确保 sys.path 已设置）
    from training.dataset import build_dataloaders
    from training.augment import build_train_augmentation
    from training.trainer import Trainer

    # 加载模型
    model_type = config["model"]["type"]
    in_ch = config["model"].get("in_channels", 1)
    out_ch = config["model"].get("out_channels", 1)

    if model_type == "attention":
        from Model.AttentionUNet2D import AttentionUNet2D
        model = AttentionUNet2D(in_ch=in_ch, out_ch=out_ch)
    else:
        from Model.UNet2D import UNet2D
        model = UNet2D(in_ch=in_ch, out_ch=out_ch)

    logger.info(f"模型: {model_type} ({sum(p.numel() for p in model.parameters()):,} 参数)")

    # 数据增强
    augment = build_train_augmentation(config)
    if augment:
        logger.info("数据增强: 已启用")
    else:
        logger.info("数据增强: 未启用")

    # 数据加载器
    data_cfg = config["data"]
    train_cfg = config["training"]
    train_loader, val_loader = build_dataloaders(
        ct_dir=data_cfg["ct_dir"],
        mask_dir=data_cfg["mask_dir"],
        val_split=data_cfg["val_split"],
        batch_size=train_cfg["batch_size"],
        seed=data_cfg["seed"],
        augment_train=augment,
    )

    # 正负样本比例
    from training.dataset import compute_pos_weight
    all_files = sorted([
        f for f in os.listdir(data_cfg["ct_dir"])
        if f.lower().endswith(".dcm")
    ])
    compute_pos_weight(all_files, data_cfg["mask_dir"])

    # 创建训练器
    trainer = Trainer(model, config, exp_dir)

    # 开始训练
    history = trainer.run(train_loader, val_loader, config)

    logger.info("\n✅ 训练完成！")
    logger.info(f"   实验目录: {exp_dir}")
    logger.info(f"   最优 Dice: {trainer.best_dice:.4f}")

    # 验证推理链路
    logger.info("\n验证推理链路...")
    try:
        from Detection.CTArtifactInfer import CTArtifactInfer
        infer = CTArtifactInfer(
            model_weight_path=os.path.join(exp_dir, "best.pth"),
            model_type=model_type,
        )
        logger.info("✅ 推理链路验证通过")
    except Exception as e:
        logger.info(f"⚠️  推理链路验证失败（不影响训练）: {e}")


if __name__ == "__main__":
    main()

"""
CT 金属伪影检测 — 训练引擎
封装训练循环、验证循环、checkpoint 保存、可视化调度
"""
import os
import json
import time
import shutil
import torch
import torch.optim as optim
from torch.cuda.amp import autocast, GradScaler

from training.losses import DiceBCELoss
from training.metrics import SegmentationMetrics
from training.visualize import (
    plot_loss_curve, plot_dice_f1_curve, plot_accuracy_curve,
    plot_confusion_matrix, plot_pr_curve, plot_prediction_sample
)


class Trainer:
    """模型训练器"""

    def __init__(self, model, config, exp_dir):
        self.model = model
        self.cfg = config
        self.exp_dir = exp_dir
        self.device = torch.device("cuda" if torch.cuda.is_available() else "cpu")

        # 损失函数
        loss_cfg = config.get("loss", {})
        self.criterion = DiceBCELoss(
            dice_weight=loss_cfg.get("dice_weight", 0.5),
            bce_weight=loss_cfg.get("bce_weight", 0.5),
            pos_weight=loss_cfg.get("pos_weight", 2.0),
        )

        # 优化器
        train_cfg = config.get("training", {})
        opt_name = train_cfg.get("optimizer", "adamw")
        lr = train_cfg.get("learning_rate", 1e-4)
        wd = train_cfg.get("weight_decay", 1e-5)

        if opt_name == "adam":
            self.optimizer = optim.Adam(model.parameters(), lr=lr, weight_decay=wd)
        elif opt_name == "adamw":
            self.optimizer = optim.AdamW(model.parameters(), lr=lr, weight_decay=wd)
        elif opt_name == "sgd":
            self.optimizer = optim.SGD(model.parameters(), lr=lr, momentum=0.9, weight_decay=wd)
        else:
            raise ValueError(f"Unknown optimizer: {opt_name}")

        # 学习率调度
        scheduler_name = train_cfg.get("scheduler", "cosine")
        epochs = train_cfg.get("epochs", 100)
        if scheduler_name == "cosine":
            self.scheduler = optim.lr_scheduler.CosineAnnealingLR(self.optimizer, T_max=epochs)
        elif scheduler_name == "step":
            self.scheduler = optim.lr_scheduler.StepLR(self.optimizer, step_size=30, gamma=0.5)
        elif scheduler_name == "plateau":
            self.scheduler = optim.lr_scheduler.ReduceLROnPlateau(
                self.optimizer, mode="min", factor=0.5, patience=10
            )
        else:
            self.scheduler = None

        # 混合精度
        self.fp16 = train_cfg.get("fp16", False) and torch.cuda.is_available()
        self.scaler = GradScaler() if self.fp16 else None

        # 梯度累计
        self.accum_steps = train_cfg.get("gradient_accumulation", 1)

        # 早停
        self.early_stop_patience = train_cfg.get("early_stop_patience", 20)

        # 历史记录
        self.history = {
            "train_loss": [], "val_loss": [],
            "val_dice": [], "val_f1": [],
            "val_accuracy": [], "val_precision": [], "val_recall": [],
            "learning_rates": [],
        }
        self.best_dice = 0.0
        self.early_stop_counter = 0

        self.model.to(self.device)
        logger.info(f"训练设备: {self.device}")
        logger.info(f"优化器: {opt_name}, 学习率: {lr}, 权重衰减: {wd}")
        logger.info(f"混合精度: {self.fp16}, 梯度累计: {self.accum_steps}步")

    def train_epoch(self, train_loader):
        """训练一轮"""
        self.model.train()
        total_loss = 0.0
        self.optimizer.zero_grad()

        for batch_idx, (images, masks) in enumerate(train_loader):
            images = images.to(self.device)
            masks = masks.to(self.device)

            if self.fp16:
                with autocast():
                    outputs = self.model(images)
                    loss, _ = self.criterion(outputs, masks)
                self.scaler.scale(loss).backward()

                if (batch_idx + 1) % self.accum_steps == 0:
                    self.scaler.step(self.optimizer)
                    self.scaler.update()
                    self.optimizer.zero_grad()
            else:
                outputs = self.model(images)
                loss, _ = self.criterion(outputs, masks)
                loss.backward()

                if (batch_idx + 1) % self.accum_steps == 0:
                    self.optimizer.step()
                    self.optimizer.zero_grad()

            total_loss += loss.item()

        avg_loss = total_loss / len(train_loader)
        return avg_loss

    @torch.no_grad()
    def validate(self, val_loader):
        """验证一轮"""
        self.model.eval()
        metrics = SegmentationMetrics(threshold=0.5)
        total_loss = 0.0

        all_targets = []
        all_preds = []
        sample_saved = False

        for images, masks in val_loader:
            images = images.to(self.device)
            masks = masks.to(self.device)

            outputs = self.model(images)
            loss, _ = self.criterion(outputs, masks)
            total_loss += loss.item()

            metrics.update(outputs, masks)

            # 收集 PR 曲线数据
            all_targets.append(masks.cpu().numpy())
            all_preds.append(torch.sigmoid(outputs).cpu().numpy())

            # 保存第一张预测样例图
            if not sample_saved:
                ct_np = images[0, 0].cpu().numpy()
                pred_np = (torch.sigmoid(outputs[0, 0]) > 0.5).cpu().numpy()
                true_np = masks[0, 0].cpu().numpy()

                sample_path = os.path.join(self.exp_dir, "pred_sample.png")
                plot_prediction_sample(ct_np, pred_np, true_np, sample_path)
                sample_saved = True

        results = metrics.get_all()
        avg_loss = total_loss / len(val_loader)
        results["val_loss"] = avg_loss

        # PR 曲线
        pr_path = os.path.join(self.exp_dir, "pr_curve.png")
        plot_pr_curve(all_targets, all_preds, pr_path)

        return results

    def run(self, train_loader, val_loader, config):
        """完整训练流程"""
        epochs = config.get("training", {}).get("epochs", 100)
        viz_interval = config.get("logging", {}).get("viz_interval", 10)
        save_interval = config.get("logging", {}).get("save_interval", 10)

        logger.info(f"\\n开始训练，共 {epochs} 轮\n")
        start_time = time.time()

        for epoch in range(1, epochs + 1):
            # 训练
            train_loss = self.train_epoch(train_loader)

            # 记录当前学习率
            current_lr = self.optimizer.param_groups[0]["lr"]
            self.history["learning_rates"].append(current_lr)

            # 验证
            val_results = self.validate(val_loader)

            # 记录历史
            self.history["train_loss"].append(train_loss)
            self.history["val_loss"].append(val_results["val_loss"])
            self.history["val_dice"].append(val_results["dice"])
            self.history["val_f1"].append(val_results["f1"])
            self.history["val_accuracy"].append(val_results["accuracy"])
            self.history["val_precision"].append(val_results["precision"])
            self.history["val_recall"].append(val_results["recall"])

            # 调度器步进
            if self.scheduler is not None:
                if isinstance(self.scheduler, optim.lr_scheduler.ReduceLROnPlateau):
                    self.scheduler.step(val_results["val_loss"])
                else:
                    self.scheduler.step()

            # 打印
            elapsed = time.time() - start_time
            logger.info(
                f"Epoch {epoch:3d}/{epochs} | "
                f"Train Loss: {train_loss:.4f} | "
                f"Val Loss: {val_results['val_loss']:.4f} | "
                f"Dice: {val_results['dice']:.4f} | "
                f"F1: {val_results['f1']:.4f} | "
                f"Acc: {val_results['accuracy']:.4f} | "
                f"LR: {current_lr:.2e} | "
                f"Time: {elapsed:.0f}s"
            )

            # 保存最优模型
            if val_results["dice"] > self.best_dice:
                self.best_dice = val_results["dice"]
                self.early_stop_counter = 0
                best_path = os.path.join(self.exp_dir, "best.pth")
                torch.save(self.model.state_dict(), best_path)
                # 同时更新推理服务用的模型
                deploy_path = os.path.join(
                    os.path.dirname(__file__), "..", "Model", "weights", "best.pth"
                )
                os.makedirs(os.path.dirname(deploy_path), exist_ok=True)
                shutil.copy2(best_path, deploy_path)
                logger.info(f"  → 新最优 Dice: {val_results['dice']:.4f}, 模型已保存")
            else:
                self.early_stop_counter += 1

            # 定期画图
            if epoch % viz_interval == 0 or epoch == epochs:
                self._save_charts()

            # 定期保存 checkpoint
            if epoch % save_interval == 0 or epoch == epochs:
                ckpt_path = os.path.join(self.exp_dir, f"checkpoint_epoch{epoch}.pth")
                torch.save({
                    "epoch": epoch,
                    "model_state_dict": self.model.state_dict(),
                    "optimizer_state_dict": self.optimizer.state_dict(),
                    "best_dice": self.best_dice,
                    "history": self.history,
                }, ckpt_path)
                # 保存最后的权重
                torch.save(self.model.state_dict(), os.path.join(self.exp_dir, "last.pth"))

            # 早停
            if self.early_stop_counter >= self.early_stop_patience:
                logger.info(f"\\n早停触发：连续 {self.early_stop_patience} 轮 Dice 未提升")
                break

        # 最终保存图表 + 指标
        self._save_charts()
        self._save_metrics(config)

        total_time = time.time() - start_time
        logger.info(f"\\n训练完成！总耗时: {total_time:.0f}s ({total_time / 60:.1f}min)")
        logger.info(f"最优 Dice: {self.best_dice:.4f}")
        logger.info(f"结果保存至: {self.exp_dir}")

        return self.history

    def _save_charts(self):
        """保存所有图表"""
        h = self.history
        plot_loss_curve(h["train_loss"], h["val_loss"],
                        os.path.join(self.exp_dir, "loss_curve.png"))
        plot_dice_f1_curve(h["val_dice"], h["val_f1"],
                           os.path.join(self.exp_dir, "dice_f1_curve.png"))
        plot_accuracy_curve(h["val_accuracy"],
                            os.path.join(self.exp_dir, "acc_curve.png"))

    def _save_metrics(self, config):
        """保存最终指标到 JSON"""
        summary = {
            "best_dice": self.best_dice,
            "final_epoch": len(self.history["train_loss"]),
            "final_metrics": {
                "train_loss": self.history["train_loss"][-1],
                "val_loss": self.history["val_loss"][-1],
                "dice": self.history["val_dice"][-1],
                "f1": self.history["val_f1"][-1],
                "accuracy": self.history["val_accuracy"][-1],
                "precision": self.history["val_precision"][-1],
                "recall": self.history["val_recall"][-1],
            },
            "best_metrics": {
                "dice": max(self.history["val_dice"]),
                "f1": max(self.history["val_f1"]),
                "accuracy": max(self.history["val_accuracy"]),
            },
            "history": self.history,
        }
        with open(os.path.join(self.exp_dir, "metrics.json"), "w", encoding="utf-8") as f:
            json.dump(summary, f, indent=2, ensure_ascii=False)


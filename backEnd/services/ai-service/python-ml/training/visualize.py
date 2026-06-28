"""
CT 金属伪影检测 — 图表可视化
生成 loss 曲线、Dice/F1 曲线、混淆矩阵、PR 曲线、预测样例
"""
import os
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
import numpy as np
from sklearn.metrics import precision_recall_curve


# 中文字体配置（服务器上可能没有中文字体，用英文回退）
plt.rcParams["font.size"] = 11


def plot_loss_curve(train_losses, val_losses, save_path):
    """损失曲线"""
    fig, ax = plt.subplots(figsize=(8, 5))
    ax.plot(train_losses, label="Train Loss", linewidth=1.5)
    ax.plot(val_losses, label="Val Loss", linewidth=1.5)
    ax.set_xlabel("Epoch")
    ax.set_ylabel("Loss")
    ax.set_title("Training & Validation Loss")
    ax.legend()
    ax.grid(True, alpha=0.3)
    fig.tight_layout()
    fig.savefig(save_path, dpi=150)
    plt.close(fig)


def plot_dice_f1_curve(val_dice, val_f1, save_path):
    """Dice / F1 曲线"""
    fig, ax = plt.subplots(figsize=(8, 5))
    ax.plot(val_dice, label="Val Dice", linewidth=1.5, marker="o", markersize=3)
    ax.plot(val_f1, label="Val F1", linewidth=1.5, marker="s", markersize=3)
    ax.set_xlabel("Epoch")
    ax.set_ylabel("Score")
    ax.set_title("Validation Dice & F1 Score")
    ax.legend()
    ax.grid(True, alpha=0.3)
    fig.tight_layout()
    fig.savefig(save_path, dpi=150)
    plt.close(fig)


def plot_accuracy_curve(val_acc, save_path):
    """精度曲线"""
    fig, ax = plt.subplots(figsize=(8, 5))
    ax.plot(val_acc, label="Val Accuracy", linewidth=1.5, color="green")
    ax.set_xlabel("Epoch")
    ax.set_ylabel("Accuracy")
    ax.set_title("Validation Accuracy")
    ax.legend()
    ax.grid(True, alpha=0.3)
    fig.tight_layout()
    fig.savefig(save_path, dpi=150)
    plt.close(fig)


def plot_confusion_matrix(cm, save_path):
    """混淆矩阵"""
    fig, ax = plt.subplots(figsize=(5, 5))
    im = ax.imshow(cm, cmap="Blues", interpolation="nearest")
    ax.figure.colorbar(im, ax=ax)
    tick_marks = [0, 1]
    ax.set_xticks(tick_marks)
    ax.set_yticks(tick_marks)
    ax.set_xticklabels(["Neg", "Pos"])
    ax.set_yticklabels(["Neg", "Pos"])
    ax.set_xlabel("Predicted")
    ax.set_ylabel("True")
    ax.set_title("Confusion Matrix")

    # 在每个格子里写数字
    for i in range(2):
        for j in range(2):
            ax.text(j, i, str(cm[i, j]), ha="center", va="center",
                    color="white" if cm[i, j] > cm.max() / 2 else "black")
    fig.tight_layout()
    fig.savefig(save_path, dpi=150)
    plt.close(fig)


def plot_pr_curve(all_targets, all_preds, save_path):
    """PR 曲线（需要累积所有验证样本的预测和标签）"""
    if len(all_targets) == 0:
        return
    precision, recall, _ = precision_recall_curve(
        np.concatenate(all_targets).ravel(),
        np.concatenate(all_preds).ravel()
    )
    fig, ax = plt.subplots(figsize=(7, 5))
    ax.plot(recall, precision, linewidth=1.5)
    ax.set_xlabel("Recall")
    ax.set_ylabel("Precision")
    ax.set_title("Precision-Recall Curve")
    ax.grid(True, alpha=0.3)
    ax.set_xlim(0, 1)
    ax.set_ylim(0, 1)
    fig.tight_layout()
    fig.savefig(save_path, dpi=150)
    plt.close(fig)


def plot_prediction_sample(ct_slice, pred_mask, true_mask, save_path):
    """预测样例：CT 原图 + 预测掩码 + 真实掩码 + 叠加图"""
    fig, axes = plt.subplots(2, 2, figsize=(10, 10))

    axes[0, 0].imshow(ct_slice, cmap="gray")
    axes[0, 0].set_title("CT Slice")
    axes[0, 0].axis("off")

    axes[0, 1].imshow(true_mask, cmap="gray")
    axes[0, 1].set_title("Ground Truth Mask")
    axes[0, 1].axis("off")

    axes[1, 0].imshow(pred_mask, cmap="gray")
    axes[1, 0].set_title("Predicted Mask")
    axes[1, 0].axis("off")

    axes[1, 1].imshow(ct_slice, cmap="gray")
    axes[1, 1].imshow(pred_mask, alpha=0.3, cmap="Reds")
    axes[1, 1].set_title("Overlay (CT + Pred)")
    axes[1, 1].axis("off")

    fig.tight_layout()
    fig.savefig(save_path, dpi=150, bbox_inches="tight")
    plt.close(fig)

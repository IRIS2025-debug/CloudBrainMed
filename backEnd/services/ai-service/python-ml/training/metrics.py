"""
CT 金属伪影检测 — 评估指标
精度、召回率、Dice、F1、混淆矩阵
"""
import numpy as np
import torch


class SegmentationMetrics:
    """二分类分割指标计算器"""

    def __init__(self, threshold=0.5):
        self.threshold = threshold
        self.reset()

    def reset(self):
        self.tp = 0  # 真正例
        self.fp = 0  # 假正例
        self.tn = 0  # 真负例
        self.fn = 0  # 假负例
        self.dice_scores = []

    def update(self, pred_logits, target):
        """
        pred_logits: [B, 1, H, W] 模型输出的 logits
        target:      [B, 1, H, W] 真实掩码 (0/1)
        """
        pred = (torch.sigmoid(pred_logits) > self.threshold).float()

        pred_np = pred.detach().cpu().numpy().astype(np.int32)
        target_np = target.detach().cpu().numpy().astype(np.int32)

        batch_tp = ((pred_np == 1) & (target_np == 1)).sum()
        batch_fp = ((pred_np == 1) & (target_np == 0)).sum()
        batch_tn = ((pred_np == 0) & (target_np == 0)).sum()
        batch_fn = ((pred_np == 0) & (target_np == 1)).sum()

        self.tp += batch_tp
        self.fp += batch_fp
        self.tn += batch_tn
        self.fn += batch_fn

        # 逐样本 Dice
        for b in range(pred_np.shape[0]):
            p = pred_np[b, 0]
            t = target_np[b, 0]
            inter = (p * t).sum()
            s = p.sum() + t.sum()
            dice = (2.0 * inter) / (s + 1e-7) if s > 0 else 1.0
            self.dice_scores.append(dice)

    def accuracy(self):
        total = self.tp + self.fp + self.tn + self.fn
        return (self.tp + self.tn) / (total + 1e-7)

    def precision(self):
        return self.tp / (self.tp + self.fp + 1e-7)

    def recall(self):
        return self.tp / (self.tp + self.fn + 1e-7)

    def f1(self):
        p = self.precision()
        r = self.recall()
        return 2 * p * r / (p + r + 1e-7)

    def dice_coeff(self):
        return np.mean(self.dice_scores) if self.dice_scores else 0.0

    def get_all(self):
        return {
            "accuracy": float(self.accuracy()),
            "precision": float(self.precision()),
            "recall": float(self.recall()),
            "f1": float(self.f1()),
            "dice": float(self.dice_coeff()),
            "tp": int(self.tp),
            "fp": int(self.fp),
            "tn": int(self.tn),
            "fn": int(self.fn),
        }

    def get_confusion_matrix(self):
        return np.array([[self.tn, self.fp], [self.fn, self.tp]])

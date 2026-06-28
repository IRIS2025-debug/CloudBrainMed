"""
CT 金属伪影检测 — 损失函数
DiceLoss + BCEWithLogits 组合损失
"""
import torch
import torch.nn as nn


class DiceLoss(nn.Module):
    """Dice Loss: 1 - (2*|X∩Y|) / (|X|+|Y|)"""

    def __init__(self, smooth=1e-5):
        super().__init__()
        self.smooth = smooth

    def forward(self, pred, target):
        # pred: 未经过 sigmoid 的 logits
        pred = torch.sigmoid(pred)
        pred = pred.contiguous().view(-1)
        target = target.contiguous().view(-1)

        intersection = (pred * target).sum()
        union = pred.sum() + target.sum()

        dice = (2.0 * intersection + self.smooth) / (union + self.smooth)
        return 1.0 - dice


class DiceBCELoss(nn.Module):
    """
    Dice + BCE 组合损失
    Args:
        dice_weight: Dice 损失权重
        bce_weight:  BCE 损失权重
        pos_weight:  正样本加重系数（缓解类别不平衡，>1 加重正样本）
    """

    def __init__(self, dice_weight=0.5, bce_weight=0.5, pos_weight=2.0):
        super().__init__()
        self.dice_weight = dice_weight
        self.bce_weight = bce_weight
        self.dice_loss = DiceLoss()

        # pos_weight: Tensor，用于 BCEWithLogitsLoss 加重正样本
        self.pos_weight_val = pos_weight
        self.bce_loss = None  # 延迟初始化（需要知道 device）

    def forward(self, pred, target):
        if self.bce_loss is None:
            if self.pos_weight_val > 1.0:
                pw = torch.tensor([self.pos_weight_val], device=pred.device)
                self.bce_loss = nn.BCEWithLogitsLoss(pos_weight=pw)
            else:
                self.bce_loss = nn.BCEWithLogitsLoss()

        dice = self.dice_loss(pred, target)
        bce = self.bce_loss(pred, target)

        total = self.dice_weight * dice + self.bce_weight * bce

        return total, {"total": total.item(), "dice": dice.item(), "bce": bce.item()}

"""
CT 金属伪影检测 — 数据集加载器
- 从 DCM 切片读取 CT 图像和对应的掩码
- Z-score 归一化
- 支持训练/验证划分
"""
import logging
import os
import random
import numpy as np
import SimpleITK as sitk
import torch
from torch.utils.data import Dataset, DataLoader


class CTArtifactDataset(Dataset):
    """CT 伪影数据集：读取 2D DCM 切片 + 对应的掩码 DCM"""

    def __init__(self, ct_dir, mask_dir, file_list, augment=None):
        """
        ct_dir:    CT DCM 切片目录
        mask_dir:  掩码 DCM 切片目录
        file_list: 使用的文件名列表（由外部划分传入）
        augment:   数据增强 transform
        """
        self.ct_dir = ct_dir
        self.mask_dir = mask_dir
        self.file_list = file_list
        self.augment = augment

    def __len__(self):
        return len(self.file_list)

    def __getitem__(self, idx):
        fname = self.file_list[idx]

        ct_path = os.path.join(self.ct_dir, fname)
        mask_path = os.path.join(self.mask_dir, fname)

        # 读 DCM
        ct_img = sitk.GetArrayFromImage(sitk.ReadImage(ct_path)).astype(np.float32)
        mask_img = sitk.GetArrayFromImage(sitk.ReadImage(mask_path)).astype(np.float32)

        # 确保是 2D (H, W)
        if ct_img.ndim == 3:
            ct_img = ct_img.squeeze()
        if mask_img.ndim == 3:
            mask_img = mask_img.squeeze()

        # Z-score 归一化
        mean, std = ct_img.mean(), ct_img.std()
        ct_img = (ct_img - mean) / (std + 1e-7)

        # 二值化掩码（转成 0/1）
        mask_img = (mask_img > 0.5).astype(np.float32)

        # 数据增强
        if self.augment is not None:
            augmented = self.augment(image=ct_img, mask=mask_img)
            ct_img = augmented["image"]
            mask_img = augmented["mask"]

        # 转成 Tensor: [1, H, W]
        ct_tensor = torch.from_numpy(ct_img).unsqueeze(0).float()
        mask_tensor = torch.from_numpy(mask_img).unsqueeze(0).float()

        return ct_tensor, mask_tensor


def build_dataloaders(ct_dir, mask_dir, val_split=0.2, batch_size=2,
                      seed=42, augment_train=None):
    """
    构建训练/验证 DataLoader
    返回: (train_loader, val_loader)
    """
    all_files = sorted([
        f for f in os.listdir(ct_dir)
        if f.lower().endswith(".dcm")
        and os.path.exists(os.path.join(mask_dir, f))
    ])

    logging.getLogger(__name__).info(f"共发现 {len(all_files)} 对 CT+Mask DCM 文件")

    # 随机划分（固定种子保证可复现）
    random.seed(seed)
    random.shuffle(all_files)

    split_idx = int(len(all_files) * (1 - val_split))
    train_files = all_files[:split_idx]
    val_files = all_files[split_idx:]

    logging.getLogger(__name__).info(f"训练集: {len(train_files)} 张")
    logging.getLogger(__name__).info(f"验证集: {len(val_files)} 张")

    train_dataset = CTArtifactDataset(ct_dir, mask_dir, train_files, augment=augment_train)
    val_dataset = CTArtifactDataset(ct_dir, mask_dir, val_files, augment=None)  # 验证集不做增强

    train_loader = DataLoader(train_dataset, batch_size=batch_size, shuffle=True,
                              num_workers=0, pin_memory=True)
    val_loader = DataLoader(val_dataset, batch_size=batch_size, shuffle=False,
                            num_workers=0, pin_memory=True)

    return train_loader, val_loader


def compute_pos_weight(all_files, mask_dir):
    """计算正负样本比例，用于 loss 的 pos_weight"""
    pos_pixels = 0
    neg_pixels = 0
    for fname in all_files:
        mask = sitk.GetArrayFromImage(sitk.ReadImage(os.path.join(mask_dir, fname)))
        pos_pixels += (mask > 0.5).sum()
        neg_pixels += (mask <= 0.5).sum()
    ratio = neg_pixels / max(pos_pixels, 1)
    logging.getLogger(__name__).info(f"正像素: {pos_pixels}, 负像素: {neg_pixels}, 不平衡比: 1:{ratio:.0f}")
    return ratio

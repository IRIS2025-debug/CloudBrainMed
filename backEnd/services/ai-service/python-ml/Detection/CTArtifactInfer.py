"""
CT 金属伪影检测推理类
封装 U-Net 模型加载、切片推理、掩码生成全流程
"""
import os
import numpy as np
import torch
import SimpleITK as sitk
from tqdm import tqdm

# 模型配置
DEVICE = torch.device("cuda" if torch.cuda.is_available() else "cpu")


class CTArtifactInfer:
    """CT 伪影检测推理引擎"""

    def __init__(self, model_weight_path, model_type="unet", device=None):
        """
        :param model_weight_path: 模型权重路径 best.pth
        :param model_type: "unet" 基础版 / "attention" 注意力版
        :param device: 推理设备，默认自动检测
        """
        self.device = device if device is not None else DEVICE
        self.model_weight_path = model_weight_path
        self.model_type = model_type
        self.model = self._load_model()
        print(f"Model loaded [{model_type}] on {self.device}")

    def _load_model(self):
        """加载训练好的 UNet 模型（全局只加载一次）"""
        if self.model_type == "attention":
            from Model.AttentionUNet2D import AttentionUNet2D
            model = AttentionUNet2D().to(self.device)
        else:
            from Model.UNet2D import UNet2D
            model = UNet2D().to(self.device)

        try:
            state_dict = torch.load(self.model_weight_path, map_location=self.device, weights_only=True)
        except TypeError:
            state_dict = torch.load(self.model_weight_path, map_location=self.device)
        model.load_state_dict(state_dict)
        model.eval()
        return model

    def predict_slice(self, img_slice):
        """单张切片推理"""
        img_slice = img_slice.astype(np.float32)

        # Z-score 归一化
        mean = img_slice.mean()
        std = img_slice.std()
        img_slice = (img_slice - mean) / (std + 1e-7)

        tensor = torch.from_numpy(img_slice).unsqueeze(0).unsqueeze(0).to(self.device)

        with torch.no_grad():
            output = self.model(tensor)
            pred = torch.sigmoid(output).squeeze().cpu().numpy()
            pred_mask = (pred > 0.5).astype(np.int16)

        return pred_mask

    def predict_from_nii(self, nii_path, save_mask_path=None):
        """从 .nii/.nii.gz 文件推理"""
        sitk_ct = sitk.ReadImage(nii_path)
        return self.predict_from_sitk(sitk_ct, save_mask_path)

    def predict_from_sitk(self, sitk_ct, save_mask_path=None):
        """从 SimpleITK 图像直接推理"""
        ct_vol = sitk.GetArrayFromImage(sitk_ct)
        D, H, W = ct_vol.shape
        mask_vol = np.zeros((D, H, W), dtype=np.int16)

        for z in tqdm(range(D), desc="推理切片"):
            mask_vol[z] = self.predict_slice(ct_vol[z])

        sitk_mask = sitk.GetImageFromArray(mask_vol)
        sitk_mask.CopyInformation(sitk_ct)

        if save_mask_path:
            output_dir = os.path.dirname(save_mask_path)
            if output_dir and not os.path.exists(output_dir):
                os.makedirs(output_dir, exist_ok=True)
            sitk.WriteImage(sitk_mask, save_mask_path)

        return sitk_mask

    def extract_features(self):
        """提取当前推理的融合特征向量"""
        return self.model.extract_features()

"""CT lesion recognition and segmentation inference."""

from __future__ import annotations

import os

import numpy as np
import SimpleITK as sitk
import torch
from tqdm import tqdm

DEVICE = torch.device("cuda" if torch.cuda.is_available() else "cpu")


class CTLesionInfer:
    """Load a lesion segmentation model and return a 3D lesion candidate mask."""

    def __init__(self, model_weight_path: str, model_type: str = "attention", device=None, allow_fallback: bool = True):
        self.device = device if device is not None else DEVICE
        self.model_weight_path = model_weight_path
        self.model_type = model_type
        self.allow_fallback = allow_fallback
        self.fallback = False
        self.model = self._try_load_model()
        print(f"Lesion model loaded [{model_type}] on {self.device}; fallback={self.fallback}")

    def _try_load_model(self):
        if not self.model_weight_path or not os.path.exists(self.model_weight_path):
            if self.allow_fallback:
                self.fallback = True
                return None
            raise FileNotFoundError(f"Lesion model weights not found: {self.model_weight_path}")

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

    def predict_slice(self, img_slice: np.ndarray) -> np.ndarray:
        if self.fallback:
            return self._predict_slice_fallback(img_slice)

        img_slice = img_slice.astype(np.float32)
        mean = img_slice.mean()
        std = img_slice.std()
        img_slice = (img_slice - mean) / (std + 1e-7)
        tensor = torch.from_numpy(img_slice).unsqueeze(0).unsqueeze(0).to(self.device)

        with torch.no_grad():
            output = self.model(tensor)
            pred = torch.sigmoid(output).squeeze().cpu().numpy()
        return (pred > 0.5).astype(np.int16)

    def predict_from_sitk(self, sitk_ct, save_mask_path: str | None = None):
        ct_vol = sitk.GetArrayFromImage(sitk_ct)
        if ct_vol.ndim != 3:
            raise ValueError("CT lesion inference expects a 3D CT volume")

        mask_vol = np.zeros(ct_vol.shape, dtype=np.int16)
        for z in tqdm(range(ct_vol.shape[0]), desc="CT lesion inference"):
            mask_vol[z] = self.predict_slice(ct_vol[z])

        sitk_mask = sitk.GetImageFromArray(mask_vol)
        sitk_mask.CopyInformation(sitk_ct)

        if save_mask_path:
            output_dir = os.path.dirname(save_mask_path)
            if output_dir:
                os.makedirs(output_dir, exist_ok=True)
            sitk.WriteImage(sitk_mask, save_mask_path)

        return sitk_mask

    def _predict_slice_fallback(self, img_slice: np.ndarray) -> np.ndarray:
        """Deterministic candidate extractor used only before trained weights exist."""
        image = img_slice.astype(np.float32)
        if image.size == 0 or float(image.std()) < 1e-6:
            return np.zeros_like(image, dtype=np.int16)

        body = image > -900
        if not np.any(body):
            return np.zeros_like(image, dtype=np.int16)

        body_values = image[body]
        mean = float(body_values.mean())
        std = float(body_values.std()) + 1e-7
        z = (image - mean) / std
        candidate = ((z > 3.0) | (z < -3.0)) & body
        if int(candidate.sum()) < 8:
            return np.zeros_like(image, dtype=np.int16)
        return candidate.astype(np.int16)

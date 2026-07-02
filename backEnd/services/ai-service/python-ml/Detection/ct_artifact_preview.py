"""Build 2D CT+mask preview images from 3D inference results."""

from __future__ import annotations

from pathlib import Path

import numpy as np


def select_preview_slice_index(mask_array: np.ndarray) -> int:
    if mask_array.ndim < 3:
        return 0

    per_slice = np.count_nonzero(mask_array, axis=tuple(range(1, mask_array.ndim)))
    if int(per_slice.max(initial=0)) == 0:
        return int(mask_array.shape[0] // 2)
    return int(np.argmax(per_slice))


def save_ct_mask_preview_png(
    *,
    ct_array: np.ndarray,
    mask_array: np.ndarray,
    slice_index: int,
    save_path: str | Path,
) -> None:
    ct_slice = ct_array[slice_index] if ct_array.ndim >= 3 else ct_array
    mask_slice = mask_array[slice_index] if mask_array.ndim >= 3 else mask_array

    gray = _normalize_to_uint8(ct_slice)
    rgb = np.stack([gray, gray, gray], axis=-1).astype(np.float32)

    mask = mask_slice > 0
    rgb[mask, 0] = 255
    rgb[mask, 1] = rgb[mask, 1] * 0.35
    rgb[mask, 2] = rgb[mask, 2] * 0.35

    import SimpleITK as sitk

    image = sitk.GetImageFromArray(rgb.astype(np.uint8), isVector=True)
    sitk.WriteImage(image, str(save_path))


def _normalize_to_uint8(image: np.ndarray) -> np.ndarray:
    image = image.astype(np.float32)
    if image.size == 0:
        return np.zeros_like(image, dtype=np.uint8)

    low, high = np.percentile(image, [1, 99])
    if high <= low:
        low = float(image.min())
        high = float(image.max())
    if high <= low:
        return np.zeros_like(image, dtype=np.uint8)

    clipped = np.clip(image, low, high)
    normalized = (clipped - low) / (high - low)
    return np.round(normalized * 255).astype(np.uint8)

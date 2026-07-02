"""Build structured CT lesion inference results for Java and report drafting."""

from __future__ import annotations

from typing import Any, Iterable

import numpy as np


def build_lesion_result(
    *,
    original_filename: str,
    mask_filename: str,
    mask_array: np.ndarray,
    image_size: Iterable[int],
    spacing: Iterable[float],
    origin: Iterable[float],
    download_url: str,
    model_type: str,
    model_version: str,
    preview_filename: str | None = None,
    preview_url: str | None = None,
    preview_slice_index: int | None = None,
    fallback: bool = False,
) -> dict[str, Any]:
    lesion_pixels = int(np.count_nonzero(mask_array))
    total_pixels = int(mask_array.size)
    lesion_ratio = round((lesion_pixels / total_pixels) * 100, 4) if total_pixels else 0.0
    lesion_detected = lesion_pixels > 0
    lesion_slice_indices = _positive_slice_indices(mask_array)
    component_sizes = _connected_component_sizes(mask_array > 0)
    lesion_count = len(component_sizes)
    largest_lesion_pixels = max(component_sizes, default=0)
    summary = _build_summary(lesion_detected, lesion_count, lesion_ratio, fallback)

    finding = {
        "lesionDetected": lesion_detected,
        "lesionPixels": lesion_pixels,
        "totalPixels": total_pixels,
        "lesionRatio": lesion_ratio,
        "maskFile": mask_filename,
        "lesionSliceIndices": lesion_slice_indices,
        "lesionCount": lesion_count,
        "largestLesionPixels": largest_lesion_pixels,
        "fallback": fallback,
    }
    if preview_slice_index is not None:
        finding["previewSliceIndex"] = preview_slice_index
    if preview_filename:
        finding["previewImageFile"] = preview_filename
    if preview_url:
        finding["previewImageUrl"] = preview_url

    report_input = {
        "task": "CT_LESION_REPORT",
        "modality": "CT",
        "finding": finding,
        "imageMeta": {
            "shape": list(image_size),
            "spacing": list(spacing),
            "origin": list(origin),
        },
        "model": {
            "modelType": model_type,
            "modelVersion": model_version,
        },
        "summary": summary,
    }

    return {
        "status": "success",
        "message": "CT病灶识别与分割完成",
        "original_file": original_filename,
        "originalFile": original_filename,
        "mask_file": mask_filename,
        "maskFile": mask_filename,
        "shape": list(image_size),
        "spacing": list(spacing),
        "origin": list(origin),
        "download_url": download_url,
        "downloadUrl": download_url,
        "lesion_slice_indices": lesion_slice_indices,
        "lesionSliceIndices": lesion_slice_indices,
        "preview_image_file": preview_filename,
        "previewImageFile": preview_filename,
        "preview_image_url": preview_url,
        "previewImageUrl": preview_url,
        "preview_slice_index": preview_slice_index,
        "previewSliceIndex": preview_slice_index,
        "lesion_detected": lesion_detected,
        "lesionDetected": lesion_detected,
        "lesion_pixels": lesion_pixels,
        "lesionPixels": lesion_pixels,
        "total_pixels": total_pixels,
        "totalPixels": total_pixels,
        "lesion_ratio": lesion_ratio,
        "lesionRatio": lesion_ratio,
        "ratio": lesion_ratio,
        "lesion_count": lesion_count,
        "lesionCount": lesion_count,
        "largest_lesion_pixels": largest_lesion_pixels,
        "largestLesionPixels": largest_lesion_pixels,
        "fallback": fallback,
        "model_type": model_type,
        "modelType": model_type,
        "model_version": model_version,
        "modelVersion": model_version,
        "summary": summary,
        "report_input": report_input,
        "reportInput": report_input,
    }


def _build_summary(lesion_detected: bool, lesion_count: int, lesion_ratio: float, fallback: bool) -> str:
    prefix = "未加载训练权重，当前为启发式候选结果；" if fallback else ""
    if lesion_detected:
        return f"{prefix}检测到CT病灶候选区{lesion_count}处，候选像素占比约{lesion_ratio:.4f}%。"
    return f"{prefix}未检测到明显CT病灶候选区。"


def _positive_slice_indices(mask_array: np.ndarray) -> list[int]:
    if mask_array.ndim == 0:
        return [0] if int(mask_array) != 0 else []
    if mask_array.ndim < 3:
        return [0] if np.count_nonzero(mask_array) else []

    per_slice = np.count_nonzero(mask_array, axis=tuple(range(1, mask_array.ndim)))
    return [int(index) for index, count in enumerate(per_slice) if int(count) > 0]


def _connected_component_sizes(mask: np.ndarray) -> list[int]:
    if not np.any(mask):
        return []
    mask = np.asarray(mask, dtype=bool)
    visited = np.zeros(mask.shape, dtype=bool)
    sizes: list[int] = []

    for start in zip(*np.where(mask & ~visited)):
        start_index = tuple(int(v) for v in start)
        if visited[start_index]:
            continue
        stack = [start_index]
        visited[start_index] = True
        size = 0
        while stack:
            current = stack.pop()
            size += 1
            for neighbor in _neighbors(current, mask.shape):
                if mask[neighbor] and not visited[neighbor]:
                    visited[neighbor] = True
                    stack.append(neighbor)
        sizes.append(size)
    return sizes


def _neighbors(index: tuple[int, ...], shape: tuple[int, ...]):
    for axis, value in enumerate(index):
        if value > 0:
            yield index[:axis] + (value - 1,) + index[axis + 1 :]
        if value + 1 < shape[axis]:
            yield index[:axis] + (value + 1,) + index[axis + 1 :]

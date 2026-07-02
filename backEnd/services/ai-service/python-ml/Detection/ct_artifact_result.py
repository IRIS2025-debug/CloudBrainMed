"""Build structured CT artifact inference results for Java and LLM reporting."""

from __future__ import annotations

from typing import Any, Iterable

import numpy as np


def build_artifact_result(
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
) -> dict[str, Any]:
    positive_pixels = int(np.count_nonzero(mask_array))
    total_pixels = int(mask_array.size)
    artifact_ratio = round((positive_pixels / total_pixels) * 100, 4) if total_pixels else 0.0
    artifact_detected = positive_pixels > 0
    artifact_slice_indices = _artifact_slice_indices(mask_array)
    summary = _build_summary(artifact_detected, artifact_ratio)

    finding = {
        "artifactDetected": artifact_detected,
        "positivePixels": positive_pixels,
        "totalPixels": total_pixels,
        "artifactRatio": artifact_ratio,
        "maskFile": mask_filename,
        "artifactSliceIndices": artifact_slice_indices,
    }
    if preview_slice_index is not None:
        finding["previewSliceIndex"] = preview_slice_index
    if preview_filename:
        finding["previewImageFile"] = preview_filename
    if preview_url:
        finding["previewImageUrl"] = preview_url

    report_input = {
        "task": "CT_ARTIFACT_REPORT",
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
        "message": "CT金属伪影检测完成",
        "original_file": original_filename,
        "originalFile": original_filename,
        "mask_file": mask_filename,
        "maskFile": mask_filename,
        "shape": list(image_size),
        "spacing": list(spacing),
        "origin": list(origin),
        "download_url": download_url,
        "downloadUrl": download_url,
        "artifact_slice_indices": artifact_slice_indices,
        "artifactSliceIndices": artifact_slice_indices,
        "preview_image_file": preview_filename,
        "previewImageFile": preview_filename,
        "preview_image_url": preview_url,
        "previewImageUrl": preview_url,
        "preview_slice_index": preview_slice_index,
        "previewSliceIndex": preview_slice_index,
        "artifact_detected": artifact_detected,
        "artifactDetected": artifact_detected,
        "positive_pixels": positive_pixels,
        "positivePixels": positive_pixels,
        "total_pixels": total_pixels,
        "totalPixels": total_pixels,
        "artifact_ratio": artifact_ratio,
        "artifactRatio": artifact_ratio,
        "ratio": artifact_ratio,
        "model_type": model_type,
        "modelType": model_type,
        "model_version": model_version,
        "modelVersion": model_version,
        "summary": summary,
        "report_input": report_input,
        "reportInput": report_input,
    }


def _build_summary(artifact_detected: bool, artifact_ratio: float) -> str:
    if artifact_detected:
        return f"检测到CT金属伪影，伪影像素占比约{artifact_ratio:.4f}%。"
    return "未检测到明显CT金属伪影。"


def _artifact_slice_indices(mask_array: np.ndarray) -> list[int]:
    if mask_array.ndim == 0:
        return [0] if int(mask_array) != 0 else []
    if mask_array.ndim < 3:
        return [0] if np.count_nonzero(mask_array) else []

    per_slice = np.count_nonzero(mask_array, axis=tuple(range(1, mask_array.ndim)))
    return [int(index) for index, count in enumerate(per_slice) if int(count) > 0]

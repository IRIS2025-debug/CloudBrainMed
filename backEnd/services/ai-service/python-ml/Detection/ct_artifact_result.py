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
) -> dict[str, Any]:
    positive_pixels = int(np.count_nonzero(mask_array))
    total_pixels = int(mask_array.size)
    artifact_ratio = round((positive_pixels / total_pixels) * 100, 4) if total_pixels else 0.0
    artifact_detected = positive_pixels > 0
    summary = _build_summary(artifact_detected, artifact_ratio)

    finding = {
        "artifactDetected": artifact_detected,
        "positivePixels": positive_pixels,
        "totalPixels": total_pixels,
        "artifactRatio": artifact_ratio,
        "maskFile": mask_filename,
    }

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

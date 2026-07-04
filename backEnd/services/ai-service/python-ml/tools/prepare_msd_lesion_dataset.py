#!/usr/bin/env python
"""Prepare a small CT lesion segmentation subset from an MSD task archive.

Default dataset:
    Medical Segmentation Decathlon Task10_Colon.tar
    https://msd-for-monai.s3.us-west-2.amazonaws.com/Task10_Colon.tar

The script extracts paired imagesTr/labelsTr NIfTI volumes and writes 2D .npy
slices into the existing training layout:

    output_dir/CT/case_xxx_z000.npy
    output_dir/MASK/case_xxx_z000.npy

MSD labels use foreground > 0 for tumor/lesion. The generated MASK files are
binary lesion masks, so lesion recognition labels can be derived from whether a
slice or case contains any positive mask pixels.
"""

from __future__ import annotations

import argparse
import os
import tarfile
import tempfile
from pathlib import Path

import numpy as np

DEFAULT_MSD_URL = "https://msd-for-monai.s3.us-west-2.amazonaws.com/Task10_Colon.tar"


def parse_args():
    parser = argparse.ArgumentParser(description="Prepare MSD CT lesion slices for U-Net training")
    parser.add_argument("--archive", required=True, help="Local MSD .tar archive path")
    parser.add_argument("--output-dir", default="data/CT病灶数据集", help="Output dataset root")
    parser.add_argument("--max-cases", type=int, default=8, help="Maximum paired cases to extract")
    parser.add_argument("--max-positive-slices-per-case", type=int, default=80)
    parser.add_argument("--negative-slices-per-case", type=int, default=20)
    parser.add_argument("--min-mask-pixels", type=int, default=8)
    parser.add_argument("--print-source", action="store_true", help="Print the default public dataset URL")
    return parser.parse_args()


def main():
    args = parse_args()
    if args.print_source:
        print(DEFAULT_MSD_URL)

    archive = Path(args.archive)
    if not archive.exists():
        raise FileNotFoundError(f"Archive not found: {archive}")

    output_root = Path(args.output_dir)
    ct_out = output_root / "CT"
    mask_out = output_root / "MASK"
    ct_out.mkdir(parents=True, exist_ok=True)
    mask_out.mkdir(parents=True, exist_ok=True)

    with tempfile.TemporaryDirectory() as tmp:
        tmp_dir = Path(tmp)
        extracted = extract_pairs(archive, tmp_dir, args.max_cases)
        written = 0
        for image_path, label_path in extracted:
            written += write_case_slices(
                image_path=image_path,
                label_path=label_path,
                ct_out=ct_out,
                mask_out=mask_out,
                max_positive_slices=args.max_positive_slices_per_case,
                negative_slices=args.negative_slices_per_case,
                min_mask_pixels=args.min_mask_pixels,
            )

    print(f"Prepared {written} paired CT/MASK slices under {output_root}")


def extract_pairs(archive: Path, tmp_dir: Path, max_cases: int):
    images: dict[str, Path] = {}
    labels: dict[str, Path] = {}
    with tarfile.open(archive, "r") as tar:
        for member in tar:
            normalized_name = member.name.replace("\\", "/")
            if not member.isfile() or not normalized_name.endswith(".nii.gz"):
                continue
            parent = Path(normalized_name).parent.name
            if parent not in {"imagesTr", "labelsTr"}:
                continue

            case_id = Path(normalized_name).name.replace(".nii.gz", "")
            target_dir = tmp_dir / parent
            target_dir.mkdir(parents=True, exist_ok=True)
            target_path = target_dir / Path(normalized_name).name
            with tar.extractfile(member) as src, open(target_path, "wb") as dst:
                if src is None:
                    continue
                dst.write(src.read())
            if parent == "imagesTr":
                images[case_id] = target_path
            else:
                labels[case_id] = target_path

            paired = sorted(set(images) & set(labels))
            if len(paired) >= max_cases:
                break

    pairs = []
    for case_id in sorted(set(images) & set(labels))[:max_cases]:
        pairs.append((images[case_id], labels[case_id]))
    if not pairs:
        raise RuntimeError("No paired imagesTr/labelsTr NIfTI files found in archive")
    return pairs


def write_case_slices(
    *,
    image_path: Path,
    label_path: Path,
    ct_out: Path,
    mask_out: Path,
    max_positive_slices: int,
    negative_slices: int,
    min_mask_pixels: int,
) -> int:
    import SimpleITK as sitk

    image = sitk.GetArrayFromImage(sitk.ReadImage(str(image_path))).astype(np.float32)
    label = sitk.GetArrayFromImage(sitk.ReadImage(str(label_path)))
    if image.shape != label.shape:
        raise ValueError(f"Shape mismatch for {image_path.name}: {image.shape} vs {label.shape}")

    mask = (label > 0).astype(np.float32)
    positive_indices = [
        z for z in range(mask.shape[0])
        if int(mask[z].sum()) >= min_mask_pixels
    ][:max_positive_slices]
    negative_indices = [
        z for z in range(mask.shape[0])
        if int(mask[z].sum()) == 0
    ][:negative_slices]

    case_id = image_path.name.replace(".nii.gz", "")
    written = 0
    for z in positive_indices + negative_indices:
        filename = f"{case_id}_z{z:03d}.npy"
        np.save(ct_out / filename, image[z])
        np.save(mask_out / filename, mask[z])
        written += 1
    return written


if __name__ == "__main__":
    main()

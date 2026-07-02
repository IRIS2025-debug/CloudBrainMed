#!/usr/bin/env python
"""Prepare COVID-19 CT infection lesion slices for U-Net training.

Dataset:
    COVID-19 CT Lung and Infection Segmentation Dataset
    https://zenodo.org/records/3757476

Download the CT image zip and Infection_Mask.zip, then convert matched NIfTI
volumes into the existing 2D training layout:

    output_dir/CT/case_z000.npy
    output_dir/MASK/case_z000.npy

The infection masks are binary lesion labels. Slice-level lesion recognition
labels can be derived from whether the generated MASK slice contains positives.
"""

from __future__ import annotations

import argparse
import os
import tempfile
import zipfile
from pathlib import Path

import numpy as np

DEFAULT_DATASET_PAGE = "https://zenodo.org/records/3757476"
DEFAULT_CT_URL = "https://zenodo.org/records/3757476/files/COVID-19-CT-Seg_20cases.zip?download=1"
DEFAULT_MASK_URL = "https://zenodo.org/records/3757476/files/Infection_Mask.zip?download=1"


def parse_args():
    parser = argparse.ArgumentParser(description="Prepare COVID CT infection lesion slices")
    parser.add_argument("--ct-zip", help="Local COVID-19-CT-Seg_20cases.zip path")
    parser.add_argument("--mask-zip", help="Local Infection_Mask.zip path")
    parser.add_argument("--output-dir", default="data/ct_lesion_dataset", help="Output dataset root")
    parser.add_argument("--max-cases", type=int, default=20)
    parser.add_argument("--max-positive-slices-per-case", type=int, default=80)
    parser.add_argument("--negative-slices-per-case", type=int, default=20)
    parser.add_argument("--min-mask-pixels", type=int, default=8)
    parser.add_argument("--print-source", action="store_true", help="Print dataset URLs")
    return parser.parse_args()


def main():
    args = parse_args()
    if args.print_source:
        print(DEFAULT_DATASET_PAGE)
        print(DEFAULT_CT_URL)
        print(DEFAULT_MASK_URL)
        if not args.ct_zip or not args.mask_zip:
            return

    if not args.ct_zip or not args.mask_zip:
        raise ValueError("--ct-zip and --mask-zip are required unless only using --print-source")

    ct_zip = Path(args.ct_zip)
    mask_zip = Path(args.mask_zip)
    if not ct_zip.exists():
        raise FileNotFoundError(f"CT zip not found: {ct_zip}")
    if not mask_zip.exists():
        raise FileNotFoundError(f"Mask zip not found: {mask_zip}")

    output_root = Path(args.output_dir)
    ct_out = output_root / "CT"
    mask_out = output_root / "MASK"
    ct_out.mkdir(parents=True, exist_ok=True)
    mask_out.mkdir(parents=True, exist_ok=True)

    with tempfile.TemporaryDirectory() as tmp:
        tmp_dir = Path(tmp)
        pairs = extract_matched_pairs(ct_zip, mask_zip, tmp_dir, args.max_cases)
        written = 0
        for image_path, mask_path in pairs:
            written += write_case_slices(
                image_path=image_path,
                mask_path=mask_path,
                ct_out=ct_out,
                mask_out=mask_out,
                max_positive_slices=args.max_positive_slices_per_case,
                negative_slices=args.negative_slices_per_case,
                min_mask_pixels=args.min_mask_pixels,
            )

    print(f"Prepared {written} paired CT/MASK slices under {output_root}")


def extract_matched_pairs(ct_zip: Path, mask_zip: Path, tmp_dir: Path, max_cases: int):
    ct_dir = tmp_dir / "ct"
    mask_dir = tmp_dir / "mask"
    ct_dir.mkdir(parents=True, exist_ok=True)
    mask_dir.mkdir(parents=True, exist_ok=True)

    ct_files = extract_nii_members(ct_zip, ct_dir)
    mask_files = extract_nii_members(mask_zip, mask_dir)
    paired_names = sorted(set(ct_files) & set(mask_files))[:max_cases]
    if not paired_names:
        raise RuntimeError("No matched .nii.gz files found between CT and infection mask zips")
    return [(ct_files[name], mask_files[name]) for name in paired_names]


def extract_nii_members(zip_path: Path, target_dir: Path) -> dict[str, Path]:
    files: dict[str, Path] = {}
    with zipfile.ZipFile(zip_path) as zf:
        for member in zf.infolist():
            normalized_name = member.filename.replace("\\", "/")
            if member.is_dir() or not normalized_name.endswith(".nii.gz"):
                continue
            filename = Path(normalized_name).name
            target_path = target_dir / filename
            with zf.open(member) as src, open(target_path, "wb") as dst:
                dst.write(src.read())
            files[filename] = target_path
    return files


def write_case_slices(
    *,
    image_path: Path,
    mask_path: Path,
    ct_out: Path,
    mask_out: Path,
    max_positive_slices: int,
    negative_slices: int,
    min_mask_pixels: int,
) -> int:
    import SimpleITK as sitk

    image = sitk.GetArrayFromImage(sitk.ReadImage(str(image_path))).astype(np.float32)
    mask = sitk.GetArrayFromImage(sitk.ReadImage(str(mask_path)))
    if image.shape != mask.shape:
        raise ValueError(f"Shape mismatch for {image_path.name}: {image.shape} vs {mask.shape}")

    lesion_mask = (mask > 0).astype(np.float32)
    positive_indices = [
        z for z in range(lesion_mask.shape[0])
        if int(lesion_mask[z].sum()) >= min_mask_pixels
    ][:max_positive_slices]
    negative_indices = [
        z for z in range(lesion_mask.shape[0])
        if int(lesion_mask[z].sum()) == 0
    ][:negative_slices]

    case_id = image_path.name.replace(".nii.gz", "")
    written = 0
    for z in positive_indices + negative_indices:
        filename = f"{case_id}_z{z:03d}.npy"
        np.save(ct_out / filename, image[z])
        np.save(mask_out / filename, lesion_mask[z])
        written += 1
    return written


if __name__ == "__main__":
    main()

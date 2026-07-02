#!/usr/bin/env python
"""Prepare a small brain CT ICH segmentation subset for U-Net training.

Dataset:
    Computed Tomography Images for Intracranial Hemorrhage Detection and Segmentation
    https://physionet.org/content/ct-ich/1.3.1/

The PhysioNet resource is restricted-access: a registered user must sign the
data use agreement before downloading files. After download/extraction, this
script reads matched NIfTI files from `ct_scans/` and `masks/`, then writes 2D
`.npy` slices into the existing training layout:

    output_dir/CT/case_z000.npy
    output_dir/MASK/case_z000.npy

Using `--max-cases 20` is enough to prove the training and deployment chain for
a course project; full-dataset training is optional.
"""

from __future__ import annotations

import argparse
import tarfile
import tempfile
import zipfile
from pathlib import Path

import numpy as np

DATASET_PAGE = "https://physionet.org/content/ct-ich/1.3.1/"


def parse_args():
    parser = argparse.ArgumentParser(description="Prepare PhysioNet brain CT ICH lesion slices")
    parser.add_argument("--input", help="Extracted dataset root, .zip, .tar, .tar.gz, or .tgz")
    parser.add_argument("--output-dir", default="data/ct_lesion_dataset")
    parser.add_argument("--max-cases", type=int, default=20)
    parser.add_argument("--max-positive-slices-per-case", type=int, default=80)
    parser.add_argument("--negative-slices-per-case", type=int, default=20)
    parser.add_argument("--min-mask-pixels", type=int, default=8)
    parser.add_argument("--print-source", action="store_true")
    return parser.parse_args()


def main():
    args = parse_args()
    if args.print_source:
        print(DATASET_PAGE)
        if not args.input:
            return
    if not args.input:
        raise ValueError("--input is required unless only using --print-source")

    source = Path(args.input)
    if not source.exists():
        raise FileNotFoundError(f"Dataset input not found: {source}")

    output_root = Path(args.output_dir)
    ct_out = output_root / "CT"
    mask_out = output_root / "MASK"
    ct_out.mkdir(parents=True, exist_ok=True)
    mask_out.mkdir(parents=True, exist_ok=True)

    with tempfile.TemporaryDirectory() as tmp:
        dataset_root = extract_if_archive(source, Path(tmp))
        pairs = find_pairs(dataset_root, args.max_cases)
        written = 0
        for ct_path, mask_path in pairs:
            written += write_case_slices(
                ct_path=ct_path,
                mask_path=mask_path,
                ct_out=ct_out,
                mask_out=mask_out,
                max_positive_slices=args.max_positive_slices_per_case,
                negative_slices=args.negative_slices_per_case,
                min_mask_pixels=args.min_mask_pixels,
            )

    print(f"Prepared {written} paired brain CT/MASK slices under {output_root}")


def extract_if_archive(source: Path, tmp_dir: Path) -> Path:
    if source.is_dir():
        return source
    lower_name = source.name.lower()
    if lower_name.endswith(".zip"):
        with zipfile.ZipFile(source) as zf:
            zf.extractall(tmp_dir)
        return tmp_dir
    if lower_name.endswith((".tar", ".tar.gz", ".tgz")):
        with tarfile.open(source) as tf:
            tf.extractall(tmp_dir)
        return tmp_dir
    raise ValueError(f"Unsupported dataset input: {source}")


def find_pairs(dataset_root: Path, max_cases: int):
    ct_files = collect_nii_by_name(dataset_root, "ct_scans")
    mask_files = collect_nii_by_name(dataset_root, "masks")
    paired_names = sorted(set(ct_files) & set(mask_files))[:max_cases]
    if not paired_names:
        raise RuntimeError("No matched NIfTI files found under ct_scans/ and masks/")
    return [(ct_files[name], mask_files[name]) for name in paired_names]


def collect_nii_by_name(dataset_root: Path, folder_name: str) -> dict[str, Path]:
    candidates = [
        path for path in dataset_root.rglob("*")
        if path.is_file()
        and path.name.lower().endswith((".nii", ".nii.gz"))
        and folder_name in [part.lower() for part in path.parts]
    ]
    return {normalize_case_name(path.name): path for path in candidates}


def normalize_case_name(filename: str) -> str:
    lower = filename.lower()
    if lower.endswith(".nii.gz"):
        return filename[:-7]
    if lower.endswith(".nii"):
        return filename[:-4]
    return filename


def write_case_slices(
    *,
    ct_path: Path,
    mask_path: Path,
    ct_out: Path,
    mask_out: Path,
    max_positive_slices: int,
    negative_slices: int,
    min_mask_pixels: int,
) -> int:
    import SimpleITK as sitk

    image = sitk.GetArrayFromImage(sitk.ReadImage(str(ct_path))).astype(np.float32)
    mask = sitk.GetArrayFromImage(sitk.ReadImage(str(mask_path)))
    if image.shape != mask.shape:
        raise ValueError(f"Shape mismatch for {ct_path.name}: {image.shape} vs {mask.shape}")

    lesion_mask = (mask > 0).astype(np.float32)
    positive_indices = [
        z for z in range(lesion_mask.shape[0])
        if int(lesion_mask[z].sum()) >= min_mask_pixels
    ][:max_positive_slices]
    negative_indices = [
        z for z in range(lesion_mask.shape[0])
        if int(lesion_mask[z].sum()) == 0
    ][:negative_slices]

    case_id = normalize_case_name(ct_path.name)
    written = 0
    for z in positive_indices + negative_indices:
        filename = f"{case_id}_z{z:03d}.npy"
        np.save(ct_out / filename, image[z])
        np.save(mask_out / filename, lesion_mask[z])
        written += 1
    return written


if __name__ == "__main__":
    main()

# CT Metal Artifact Dataset

Source archive: `D:\CloudBrainMed\xunlian\ct_artifact_dataset.zip`

Contents:

| Directory | Count | Description |
|---|---:|---|
| `CT/` | 460 | 2D CT DICOM slices |
| `MASK/` | 460 | DICOM masks with matching filenames |

Usage:

- Training config: `training/config.yaml`
- Loader: `training/dataset.py`
- Expected mapping: each file in `CT/` must have the same filename in `MASK/`.

This dataset is used for the six CT metal artifact segmentation tuning experiments summarized in `docs/generated/xunlian-experiment-summary.md`.

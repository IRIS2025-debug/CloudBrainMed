#!/usr/bin/env python
"""
Summarize teammate training archives without extracting large weight files.

Usage:
    python tools/summarize_xunlian_archives.py
"""
from __future__ import annotations

import argparse
import csv
import json
import zipfile
from dataclasses import dataclass
from pathlib import Path
from typing import Any

import yaml


REQUIRED = [
    "config.yaml",
    "train.log",
    "metrics.json",
    "best.pth",
    "last.pth",
    "loss_curve.png",
    "dice_f1_curve.png",
    "acc_curve.png",
    "pred_sample.png",
    "pr_curve.png",
]


@dataclass
class ArchiveSummary:
    exp_id: str
    archive: str
    directory: str
    model: str
    optimizer: str
    learning_rate: float | None
    batch_size: int | None
    grad_accum: int | None
    effective_batch: int | None
    epochs: int | None
    best_dice: float | None
    best_f1: float | None
    best_accuracy: float | None
    final_epoch: int | None
    missing: list[str]


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--archives", default="../../../../project-materials/ct-model-defense-traces/xunlian")
    parser.add_argument("--out", default="../../../../project-materials/ct-model-defense-traces/python-ml-docs/docs/generated")
    args = parser.parse_args()

    archive_dir = Path(args.archives)
    out_dir = Path(args.out)
    out_dir.mkdir(parents=True, exist_ok=True)

    summaries = [read_archive(path) for path in sorted(archive_dir.glob("2026*.zip"))]
    summaries = [item for item in summaries if item is not None]
    summaries.sort(key=lambda item: item.exp_id)

    write_markdown(summaries, out_dir / "xunlian-experiment-summary.md")
    write_csv(summaries, out_dir / "xunlian-experiment-summary.csv")
    print(f"Wrote {len(summaries)} archive summaries to {out_dir}")


def read_archive(path: Path) -> ArchiveSummary | None:
    with zipfile.ZipFile(path) as archive:
        names = [name.replace("\\", "/") for name in archive.namelist() if not name.endswith("/")]
        if not names:
            return None

        directory = names[0].split("/")[0]
        config_name = find_entry(names, "config.yaml")
        metrics_name = find_entry(names, "metrics.json")
        if not config_name or not metrics_name:
            return None

        config = yaml.safe_load(archive.read(config_name).decode("utf-8-sig")) or {}
        metrics = json.loads(archive.read(metrics_name).decode("utf-8-sig"))

        model = str(config.get("model", {}).get("type", ""))
        training = config.get("training", {})
        optimizer = str(training.get("optimizer", ""))
        learning_rate = as_float(training.get("learning_rate"))
        batch_size = as_int(training.get("batch_size"))
        grad_accum = as_int(training.get("gradient_accumulation")) or 1
        effective_batch = batch_size * grad_accum if batch_size is not None else None
        epochs = as_int(training.get("epochs"))
        best_metrics = metrics.get("best_metrics", {})
        missing = [name for name in REQUIRED if not find_entry(names, name)]

        return ArchiveSummary(
            exp_id=identify_experiment(model, optimizer, learning_rate, effective_batch),
            archive=path.name,
            directory=directory,
            model=model,
            optimizer=optimizer,
            learning_rate=learning_rate,
            batch_size=batch_size,
            grad_accum=grad_accum,
            effective_batch=effective_batch,
            epochs=epochs,
            best_dice=as_float(metrics.get("best_dice") or best_metrics.get("dice")),
            best_f1=as_float(best_metrics.get("f1")),
            best_accuracy=as_float(best_metrics.get("accuracy")),
            final_epoch=as_int(metrics.get("final_epoch")),
            missing=missing,
        )


def write_markdown(items: list[ArchiveSummary], path: Path) -> None:
    lines = [
        "# 队友六组训练结果汇总",
        "",
        "> 根据 `project-materials/ct-model-defense-traces/xunlian/*.zip` 中的 `config.yaml` 和 `metrics.json` 自动生成，未解压大体积权重包。",
        "",
        "## 对比表",
        "",
        "| 实验 | 压缩包 | 模型 | 优化器 | LR | Batch | 梯度累积 | 等效Batch | Epochs | Best Dice | Best F1 | Best Acc |",
        "|---|---|---|---|---|---|---|---|---|---|---|---|",
    ]
    for item in items:
        lines.append(
            f"| {item.exp_id} | `{item.archive}` | {item.model} | {item.optimizer} | "
            f"{format_float(item.learning_rate)} | {item.batch_size or ''} | {item.grad_accum or ''} | "
            f"{item.effective_batch or ''} | {item.epochs or ''} | {format_float(item.best_dice)} | "
            f"{format_float(item.best_f1)} | {format_float(item.best_accuracy)} |"
        )

    best = max((item for item in items if item.best_dice is not None), key=lambda item: item.best_dice, default=None)
    lines.extend(["", "## 最优模型", ""])
    if best:
        lines.extend([
            f"- 最优实验：{best.exp_id} `{best.archive}`。",
            f"- 最优模型：{best.model} + {best.optimizer}，Best Dice={format_float(best.best_dice)}，Best F1={format_float(best.best_f1)}。",
            "- 已复制其 `best.pth` 为 `Model/weights/best_attention_adamw.pth`，供 Python 推理服务默认加载。",
        ])

    lines.extend(["", "## 留痕完整性", ""])
    for item in items:
        if item.missing:
            lines.append(f"- {item.exp_id} `{item.archive}` 缺少：{', '.join(item.missing)}")
        else:
            lines.append(f"- {item.exp_id} `{item.archive}` 已包含核心留痕文件。")

    path.write_text("\n".join(lines) + "\n", encoding="utf-8")


def write_csv(items: list[ArchiveSummary], path: Path) -> None:
    with path.open("w", newline="", encoding="utf-8-sig") as file:
        writer = csv.writer(file)
        writer.writerow([
            "experiment", "archive", "directory", "model", "optimizer", "learning_rate",
            "batch_size", "gradient_accumulation", "effective_batch", "epochs",
            "best_dice", "best_f1", "best_accuracy", "final_epoch", "missing",
        ])
        for item in items:
            writer.writerow([
                item.exp_id, item.archive, item.directory, item.model, item.optimizer,
                item.learning_rate, item.batch_size, item.grad_accum, item.effective_batch,
                item.epochs, item.best_dice, item.best_f1, item.best_accuracy,
                item.final_epoch, ";".join(item.missing),
            ])


def find_entry(names: list[str], suffix: str) -> str | None:
    return next((name for name in names if name.endswith("/" + suffix) or name == suffix), None)


def identify_experiment(model: str, optimizer: str, lr: float | None, effective_batch: int | None) -> str:
    if model == "unet" and optimizer == "adamw" and lr == 1e-4 and effective_batch == 8:
        return "E1"
    if model == "unet" and optimizer == "adam" and lr == 1e-4 and effective_batch == 8:
        return "E2"
    if model == "unet" and optimizer == "sgd" and lr == 1e-3 and effective_batch == 8:
        return "E3"
    if model == "attention" and optimizer == "adamw" and lr == 1e-4 and effective_batch == 8:
        return "E4"
    if model == "unet" and optimizer == "adamw" and lr == 5e-4 and effective_batch == 8:
        return "E5"
    if model == "unet" and optimizer == "adamw" and lr == 1e-4 and effective_batch == 16:
        return "E6"
    return "EXTRA"


def as_float(value: Any) -> float | None:
    if value is None:
        return None
    try:
        return float(value)
    except (TypeError, ValueError):
        return None


def as_int(value: Any) -> int | None:
    if value is None:
        return None
    try:
        return int(value)
    except (TypeError, ValueError):
        return None


def format_float(value: float | None) -> str:
    return "" if value is None else f"{value:.4f}"


if __name__ == "__main__":
    main()

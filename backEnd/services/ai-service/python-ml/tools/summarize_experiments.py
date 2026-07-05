#!/usr/bin/env python
"""
Summarize CT artifact training experiments into report-ready Markdown and CSV.

Usage:
    python tools/summarize_experiments.py
    python tools/summarize_experiments.py --experiments ../../../../project-materials/CT金属伪影模型留痕/experiments
"""
from __future__ import annotations

import argparse
import csv
import json
from dataclasses import dataclass
from pathlib import Path
from typing import Any

import yaml


PLAN = [
    ("E1", "unet", "adamw", 1e-4, 8, "UNet + AdamW baseline"),
    ("E2", "unet", "adam", 1e-4, 8, "optimizer comparison: Adam"),
    ("E3", "unet", "sgd", 1e-3, 8, "optimizer comparison: SGD"),
    ("E4", "attention", "adamw", 1e-4, 8, "architecture comparison: AttentionUNet"),
    ("E5", "unet", "adamw", 5e-4, 8, "learning-rate comparison"),
    ("E6", "unet", "adamw", 1e-4, 16, "batch-size comparison"),
]


@dataclass
class Experiment:
    exp_id: str
    directory: str
    model: str
    optimizer: str
    learning_rate: float | None
    batch_size: int | None
    epochs: int | None
    best_dice: float | None
    best_f1: float | None
    best_accuracy: float | None
    final_epoch: int | None
    note: str
    missing: list[str]


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--experiments", default="../../../../project-materials/CT金属伪影模型留痕/experiments")
    parser.add_argument("--out", default="../../../../project-materials/CT金属伪影模型留痕/python-ml-docs/docs/generated")
    args = parser.parse_args()

    root = Path(args.experiments)
    out_dir = Path(args.out)
    out_dir.mkdir(parents=True, exist_ok=True)

    experiments = collect_experiments(root)
    experiments.sort(key=lambda item: (experiment_sort_key(item.exp_id), item.directory))

    write_markdown(experiments, out_dir / "experiment-summary.md")
    write_csv(experiments, out_dir / "experiment-summary.csv")
    print(f"Wrote {len(experiments)} experiments to {out_dir}")


def collect_experiments(root: Path) -> list[Experiment]:
    if not root.exists():
        return []

    result: list[Experiment] = []
    for exp_dir in sorted(path for path in root.iterdir() if path.is_dir()):
        config_path = exp_dir / "config.yaml"
        metrics_path = exp_dir / "metrics.json"
        if not config_path.exists() and not metrics_path.exists():
            continue

        config = read_yaml(config_path)
        metrics = read_json(metrics_path)
        model = str(config.get("model", {}).get("type") or infer_model(exp_dir.name))
        training = config.get("training", {})
        optimizer = str(training.get("optimizer") or infer_optimizer(exp_dir.name))
        learning_rate = as_float(training.get("learning_rate"))
        batch_size = as_int(training.get("batch_size"))
        epochs = as_int(training.get("epochs"))

        exp_id, note = identify_experiment(model, optimizer, learning_rate, batch_size)
        best_metrics = metrics.get("best_metrics", {})
        missing = missing_artifacts(exp_dir)

        result.append(Experiment(
            exp_id=exp_id,
            directory=exp_dir.name,
            model=model,
            optimizer=optimizer,
            learning_rate=learning_rate,
            batch_size=batch_size,
            epochs=epochs,
            best_dice=as_float(metrics.get("best_dice") or best_metrics.get("dice")),
            best_f1=as_float(best_metrics.get("f1")),
            best_accuracy=as_float(best_metrics.get("accuracy")),
            final_epoch=as_int(metrics.get("final_epoch")),
            note=note,
            missing=missing,
        ))
    return result


def write_markdown(experiments: list[Experiment], path: Path) -> None:
    lines = [
        "# 六组调参实验结果汇总",
        "",
        "> 由 `tools/summarize_experiments.py` 根据 `experiments/*/config.yaml` 和 `metrics.json` 自动生成。",
        "",
        "## 对比表",
        "",
        "| 实验 | 目录 | 模型 | 优化器 | LR | Batch | Epochs | Best Dice | Best F1 | Best Acc | 说明 |",
        "|------|------|------|--------|----|-------|--------|-----------|---------|----------|------|",
    ]
    for item in experiments:
        lines.append(
            "| {exp_id} | `{directory}` | {model} | {optimizer} | {lr} | {batch} | {epochs} | "
            "{dice} | {f1} | {acc} | {note} |".format(
                exp_id=item.exp_id,
                directory=item.directory,
                model=item.model,
                optimizer=item.optimizer,
                lr=format_float(item.learning_rate),
                batch=item.batch_size or "",
                epochs=item.epochs or "",
                dice=format_float(item.best_dice),
                f1=format_float(item.best_f1),
                acc=format_float(item.best_accuracy),
                note=item.note,
            )
        )

    best = best_experiment(experiments)
    lines.extend(["", "## 最优模型建议", ""])
    if best:
        lines.extend([
            f"- 当前按 Best Dice 排名最优：{best.exp_id} `{best.directory}`。",
            f"- 关键指标：Dice={format_float(best.best_dice)}，F1={format_float(best.best_f1)}，Accuracy={format_float(best.best_accuracy)}。",
            "- 最终结论仍需结合预测样例图、混淆矩阵和训练曲线判断，避免只看单一指标。",
        ])
    else:
        lines.append("- 暂未发现可汇总的实验结果。")

    lines.extend(["", "## 留痕完整性检查", ""])
    if experiments:
        for item in experiments:
            if item.missing:
                lines.append(f"- {item.exp_id} `{item.directory}` 缺少：{', '.join(item.missing)}")
            else:
                lines.append(f"- {item.exp_id} `{item.directory}` 留痕文件完整。")
    else:
        lines.append("- 暂未发现 `experiments/*/config.yaml` 或 `metrics.json`。")

    path.write_text("\n".join(lines) + "\n", encoding="utf-8")


def write_csv(experiments: list[Experiment], path: Path) -> None:
    with path.open("w", newline="", encoding="utf-8-sig") as file:
        writer = csv.writer(file)
        writer.writerow([
            "experiment", "directory", "model", "optimizer", "learning_rate",
            "batch_size", "epochs", "best_dice", "best_f1", "best_accuracy",
            "final_epoch", "note", "missing_artifacts",
        ])
        for item in experiments:
            writer.writerow([
                item.exp_id, item.directory, item.model, item.optimizer,
                item.learning_rate, item.batch_size, item.epochs,
                item.best_dice, item.best_f1, item.best_accuracy,
                item.final_epoch, item.note, ";".join(item.missing),
            ])


def identify_experiment(
    model: str,
    optimizer: str,
    learning_rate: float | None,
    batch_size: int | None,
) -> tuple[str, str]:
    for exp_id, plan_model, plan_optimizer, plan_lr, plan_batch, note in PLAN:
        if (
            model == plan_model
            and optimizer == plan_optimizer
            and learning_rate is not None
            and abs(learning_rate - plan_lr) < 1e-12
            and batch_size == plan_batch
        ):
            return exp_id, note
    return "EXTRA", "not in six-group plan"


def missing_artifacts(exp_dir: Path) -> list[str]:
    required = [
        "config.yaml",
        "train.log",
        "metrics.json",
        "best.pth",
        "loss_curve.png",
        "dice_f1_curve.png",
        "acc_curve.png",
    ]
    return [name for name in required if not (exp_dir / name).exists()]


def best_experiment(experiments: list[Experiment]) -> Experiment | None:
    scored = [item for item in experiments if item.best_dice is not None]
    if not scored:
        return None
    return max(scored, key=lambda item: item.best_dice or 0.0)


def read_yaml(path: Path) -> dict[str, Any]:
    if not path.exists():
        return {}
    with path.open("r", encoding="utf-8-sig") as file:
        return yaml.safe_load(file) or {}


def read_json(path: Path) -> dict[str, Any]:
    if not path.exists():
        return {}
    with path.open("r", encoding="utf-8-sig") as file:
        return json.load(file)


def infer_model(name: str) -> str:
    return "attention" if "attention" in name else "unet"


def infer_optimizer(name: str) -> str:
    for optimizer in ("adamw", "adam", "sgd"):
        if optimizer in name:
            return optimizer
    return ""


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
    if value is None:
        return ""
    if value != 0 and abs(value) < 0.001:
        return f"{value:.0e}"
    return f"{value:.4f}"


def experiment_sort_key(exp_id: str) -> int:
    if exp_id.startswith("E") and exp_id[1:].isdigit():
        return int(exp_id[1:])
    return 99


if __name__ == "__main__":
    main()

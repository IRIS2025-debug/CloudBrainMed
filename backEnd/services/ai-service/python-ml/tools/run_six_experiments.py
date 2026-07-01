#!/usr/bin/env python
"""
Run the six planned CT artifact tuning experiments in a fixed, auditable order.

Use --dry-run before starting a GPU job:
    python tools/run_six_experiments.py --dry-run
"""
from __future__ import annotations

import argparse
import subprocess
import sys
from dataclasses import dataclass
from pathlib import Path


@dataclass(frozen=True)
class ExperimentCommand:
    exp_id: str
    label: str
    args: list[str]


EXPERIMENTS = [
    ExperimentCommand(
        "E1",
        "UNet + AdamW baseline",
        ["--model", "unet", "--optimizer", "adamw", "--epochs", "100", "--batch-size", "8", "--accum", "1", "--fp16", "--lr", "1e-4"],
    ),
    ExperimentCommand(
        "E2",
        "UNet + Adam optimizer comparison",
        ["--model", "unet", "--optimizer", "adam", "--epochs", "100", "--batch-size", "8", "--accum", "1", "--fp16", "--lr", "1e-4"],
    ),
    ExperimentCommand(
        "E3",
        "UNet + SGD optimizer comparison",
        ["--model", "unet", "--optimizer", "sgd", "--epochs", "100", "--batch-size", "8", "--accum", "1", "--fp16", "--lr", "1e-3"],
    ),
    ExperimentCommand(
        "E4",
        "AttentionUNet architecture comparison",
        ["--model", "attention", "--optimizer", "adamw", "--epochs", "100", "--batch-size", "8", "--accum", "1", "--fp16", "--lr", "1e-4"],
    ),
    ExperimentCommand(
        "E5",
        "UNet + AdamW higher learning rate",
        ["--model", "unet", "--optimizer", "adamw", "--epochs", "100", "--batch-size", "8", "--accum", "1", "--fp16", "--lr", "5e-4"],
    ),
    ExperimentCommand(
        "E6",
        "UNet + AdamW larger batch",
        ["--model", "unet", "--optimizer", "adamw", "--epochs", "100", "--batch-size", "16", "--accum", "1", "--fp16", "--lr", "1e-4"],
    ),
]


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--dry-run", action="store_true", help="print commands without running training")
    parser.add_argument("--skip-summary", action="store_true", help="do not run summarize_experiments.py after training")
    args = parser.parse_args()

    project_root = Path(__file__).resolve().parents[1]
    for experiment in EXPERIMENTS:
        command = [sys.executable, "-m", "training.train", *experiment.args]
        print_header(experiment)
        print(shell_join(command))
        if not args.dry_run:
            subprocess.run(command, cwd=project_root, check=True)

    if not args.skip_summary:
        summary_command = [sys.executable, "tools/summarize_experiments.py"]
        print("\n=== Generate experiment summary ===")
        print(shell_join(summary_command))
        if not args.dry_run:
            subprocess.run(summary_command, cwd=project_root, check=True)

    return 0


def print_header(experiment: ExperimentCommand) -> None:
    print(f"\n=== {experiment.exp_id}: {experiment.label} ===")


def shell_join(parts: list[str]) -> str:
    return " ".join(quote(part) for part in parts)


def quote(value: str) -> str:
    if not value or any(ch.isspace() for ch in value):
        return '"' + value.replace('"', '\\"') + '"'
    return value


if __name__ == "__main__":
    raise SystemExit(main())

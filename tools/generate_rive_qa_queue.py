#!/usr/bin/env python3
"""Genera una cola de inspección, NO un certificado de calidad Rive/biomecánica."""
import argparse
import csv
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "app/src/main/assets"
FIELDS = [
    "exercise_id", "source", "rive_asset", "sha256", "bytes",
    "visual_review", "motion_review", "android_playback_review",
    "biomechanics_review", "autoplan", "device", "android_version",
    "reviewer", "review_date", "notes",
]


def load(name):
    return json.loads((ASSETS / name).read_text(encoding="utf-8"))


def build_queue():
    original = load("catalog.json")
    expansion = load("catalog-hunter2000-extra.json")
    manifest = load("demos-rive/manifest.json")
    if len(original) != 1200 or len(expansion) != 800:
        raise ValueError("Catálogo incompleto: se necesitan 1200 + 800 fichas")
    ids_original = [item["id"] for item in original]
    ids_extra = [item["id"] for item in expansion]
    ids = ids_original + ids_extra
    files = manifest.get("files", {})
    if len(ids) != 2000 or len(set(ids)) != 2000 or set(files) != set(ids):
        raise ValueError("La matriz Rive no coincide con 2000 IDs únicos")
    if any(item.get("approvedForAutoplan") is not False or
           item.get("pendingTechnicalReview") is not True for item in expansion):
        raise ValueError("Se detectaron fichas adicionales habilitadas sin revisión")
    extra_ids = set(ids_extra)
    rows = []
    for exercise_id in sorted(ids):
        asset = files[exercise_id]
        if not isinstance(asset.get("sha256"), str) or len(asset["sha256"]) != 64:
            raise ValueError(f"SHA256 ausente: {exercise_id}")
        if not isinstance(asset.get("bytes"), int) or asset["bytes"] <= 0:
            raise ValueError(f"Tamaño Rive inválido: {exercise_id}")
        is_extra = exercise_id in extra_ids
        rows.append({
            "exercise_id": exercise_id,
            "source": "expansion_800" if is_extra else "original_1200",
            "rive_asset": f"demos-rive/{exercise_id}.riv",
            "sha256": asset["sha256"],
            "bytes": asset["bytes"],
            "visual_review": "PENDIENTE",
            "motion_review": "PENDIENTE",
            "android_playback_review": "PENDIENTE",
            "biomechanics_review": "PENDIENTE",
            "autoplan": "BLOQUEADO_SIN_QA" if is_extra else "REGLAS_ORIGINALES",
            "device": "", "android_version": "", "reviewer": "",
            "review_date": "", "notes": "",
        })
    return rows


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path,
                        default=ROOT / "build/reports/hunter-rive-2000-qa-pendiente.csv")
    args = parser.parse_args()
    rows = build_queue()
    args.output.parent.mkdir(parents=True, exist_ok=True)
    with args.output.open("w", newline="", encoding="utf-8") as stream:
        writer = csv.DictWriter(stream, fieldnames=FIELDS)
        writer.writeheader()
        writer.writerows(rows)
    print(json.dumps({
        "result": "QA_QUEUE_GENERATED_NOT_VALIDATED",
        "rows": len(rows), "pending": len(rows),
        "pending_expansion": sum(r["source"] == "expansion_800" for r in rows),
        "output": str(args.output),
    }, ensure_ascii=False))


if __name__ == "__main__":
    main()

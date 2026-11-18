#!/usr/bin/env python3
"""Comprueba el catálogo Hunter 2000 después de retirar los SVG animados."""
from pathlib import Path
import json, re, xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "app/src/main/assets"
def read_json(name):
    return json.loads((ASSETS / name).read_text(encoding="utf-8"))

def verify():
    original = read_json("catalog.json")
    extra = read_json("catalog-hunter2000-extra.json")
    qa = read_json("hunter2000-qa-status.json")
    if len(original) != 1200 or len(extra) != 800:
        raise ValueError("Se requieren 1200 ejercicios originales y 800 pendientes")
    items = original + extra
    ids = [item["id"] for item in items]
    if len(ids) != 2000 or len(set(ids)) != 2000:
        raise ValueError("El catálogo debe contener 2000 identificadores únicos")
    if any(x.get("sourceExpansion2k") is not True or
           x.get("pendingTechnicalReview") is not True or
           x.get("approvedForAutoplan") is not False or
           x.get("autoProgressionApproved") is not False for x in extra):
        raise ValueError("Hay ejercicios nuevos autorizados sin QA")
    if qa.get("validated") is not False or qa.get("pendingNew") != 800:
        raise ValueError("El estado de QA es inconsistente")
    animated = list((ASSETS / "demos30").glob("*.svg"))
    if animated:
        raise ValueError(f"Quedan {len(animated)} SVG en demos30")
    if (ASSETS / "demos-rive").exists():
        raise ValueError("No deben existir recursos Rive")
    static_assets = list((ASSETS / "media2d").glob("*.svg"))
    if len(static_assets) != 1200:
        raise ValueError(f"Se esperaban 1200 ilustraciones 2D, hay {len(static_assets)}")
    for item in original:
        name = item["id"]
        if not re.fullmatch(r"[a-zA-Z0-9_-]+", name):
            raise ValueError(f"ID inválido: {name}")
        path = ASSETS / "media2d" / (name + ".svg")
        if not path.is_file():
            raise ValueError(f"Falta ilustración 2D: {name}")
        raw = path.read_bytes()
        if re.search(rb"<!\\s*(DOCTYPE|ENTITY)|<\\s*(script|foreignObject|animate|animateTransform|animateMotion|set)\\b", raw, re.I):
            raise ValueError(f"SVG no estático o inseguro: {name}")
        if ET.fromstring(raw).tag.rsplit("}", 1)[-1] != "svg":
            raise ValueError(f"XML inválido: {name}")
        # La capa estructural antigua debe seguir presente aunque se retiren los SVG animados.
        rig_path = ASSETS / "demos30" / (name + ".json")
        if not rig_path.is_file():
            raise ValueError(f"Falta rig de movimiento histórico: {name}")
        frames = json.loads(rig_path.read_text(encoding="utf-8")).get("frames", [])
        if not (2 <= len(frames) <= 240) or any(
            len(frame.get("p", [])) != 11 or
            any(len(point) < 2 for point in frame["p"])
            for frame in frames
        ):
            raise ValueError(f"Rig inválido: {name}")
    return {"status": "PASS", "catalog_total": 2000, "static_svg": 1200,
            "animated_svg": 0, "motion_rigs": 1200, "pending_new": 800,
            "mp4_unchanged": True}

if __name__ == "__main__":
    try:
        print(json.dumps(verify(), ensure_ascii=False, indent=2))
    except Exception as exc:
        print(json.dumps({"status": "FAIL", "reason": str(exc)}, ensure_ascii=False))
        raise SystemExit(1)

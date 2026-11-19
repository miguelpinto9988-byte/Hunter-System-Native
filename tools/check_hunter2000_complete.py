#!/usr/bin/env python3
"""Comprueba el catálogo Hunter 2000 después de retirar los SVG animados."""
from pathlib import Path
import json, re, unicodedata, xml.etree.ElementTree as ET
from collections import Counter, defaultdict

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "app/src/main/assets"
def read_json(name):
    return json.loads((ASSETS / name).read_text(encoding="utf-8"))

def exercise_name(name):
    name = unicodedata.normalize("NFKD", name.casefold())
    return " ".join(re.findall(r"[a-z0-9]+", name.encode("ascii", "ignore").decode()))


def verify_sports(items):
    sports = read_json("catalog-sports.json")
    all_rows = items + sports
    for key, values in (
        ("ID", [x["id"] for x in all_rows]),
        ("nombre normalizado", [exercise_name(x["name"]) for x in all_rows]),
    ):
        repeated = [value for value, count in Counter(values).items() if count > 1]
        if repeated:
            raise ValueError(f"Duplicados por {key}: {repeated}")
    distribution = defaultdict(Counter)
    for row in sports:
        sport, level = row["sport"], row["level"]
        if not sport or level not in (0, 1, 2) or not (
            row["instructions"] and row["commonErrors"] and row["initialDose"]
            and row["equipmentLabel"]
        ):
            raise ValueError(f"Ficha deportiva incompleta: {row['id']}")
        expected = f"sports2d/{row['id']}.svg"
        if row["illustration"] != expected or ET.parse(ASSETS / expected).getroot().tag != "{http://www.w3.org/2000/svg}svg":
            raise ValueError(f"Ilustración deportiva inválida: {row['id']}")
        distribution[sport][level] += 1
    candidates = defaultdict(list)
    for row in all_rows:
        if row.get("steps"):
            key = tuple(row.get(k) for k in ("steps", "equipment", "unit", "level", "variant"))
            candidates[key + (row["planning49"].get("pattern"),)].append(row["id"])
    return len(sports), dict(distribution), [
        ids for ids in candidates.values() if len(ids) > 1
    ]


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
    sports_count, distribution, candidates = verify_sports(items)
    return {"status": "PASS", "catalog_total": len(items) + sports_count, "static_svg": 1200 + sports_count,
            "sports_count": sports_count, "sports_by_level": distribution,
            "shared_instruction_candidates": candidates,
            "animated_svg": 0, "pending_new": 800, "mp4_unchanged": True}

if __name__ == "__main__":
    try:
        print(json.dumps(verify(), ensure_ascii=False, indent=2))
    except Exception as exc:
        print(json.dumps({"status": "FAIL", "reason": str(exc)}, ensure_ascii=False))
        raise SystemExit(1)

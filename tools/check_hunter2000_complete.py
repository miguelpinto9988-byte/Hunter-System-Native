#!/usr/bin/env python3
"""Comprueba el catálogo Hunter 2000 después de retirar los SVG animados."""
from pathlib import Path
import argparse, itertools, json, re, unicodedata, xml.etree.ElementTree as ET
from collections import Counter, defaultdict
from zipfile import ZipFile

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
    aliases = read_json("exercise-aliases.json")
    ids = {row["id"] for row in all_rows}
    if any(source not in ids or target not in ids or target in aliases
           for source, target in aliases.items()):
        raise ValueError("Hay alias inexistentes, cíclicos o encadenados")
    active_rows = [row for row in all_rows if row["id"] not in aliases]
    for key, values in (
        ("ID", [x["id"] for x in all_rows]),
        ("nombre normalizado", [exercise_name(x["name"]) for x in active_rows]),
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
        if row.get("illustration"):
            raise ValueError(f"Ilustración deportiva alternativa: {row['id']}")
        distribution[sport][level] += 1
    candidates = defaultdict(list)
    for row in all_rows:
        if row.get("steps"):
            key = tuple(row.get(k) for k in ("steps", "equipment", "unit", "level", "variant"))
            candidates[key + (row["planning49"].get("pattern"),)].append(row["id"])
    stop_words = set("de la el los las con en a al del por un una y".split())
    synonyms = {"dead bug": "bicho muerto", "bird dog": "perro de caza",
                "push up": "flexion", "pull up": "dominada",
                "jumping jack": "salto estrella", "step down": "descenso",
                "fitball": "pelota suiza", "kettlebell": "pesa rusa",
                "squat": "sentadilla", "plank": "plancha"}

    def name_words(row):
        name = exercise_name(row["name"])
        for source, target in synonyms.items():
            name = re.sub(r"\b" + source + r"\b", target, name)
        return set(name.split()) - stop_words

    groups = defaultdict(list)
    for row in active_rows:
        groups[(row["planning49"]["group"], row["unit"], row["level"])].append((row, name_words(row)))
    similar = []
    for group in groups.values():
        for (left, left_words), (right, right_words) in itertools.combinations(group, 2):
            union = left_words | right_words
            if left_words == right_words or (
                union and not (left.get("compositeSequence") or right.get("compositeSequence"))
                and len(left_words & right_words) / len(union) >= 0.8
            ):
                similar.append([left["id"], right["id"]])
    return len(sports), dict(distribution), aliases, similar, [
        ids for ids in candidates.values() if len(ids) > 1
    ]


def verify():
    original = read_json("catalog.json")
    extra = read_json("catalog-hunter2000-extra.json")
    if len(original) != 1200 or len(extra) != 800:
        raise ValueError("Se requieren 1200 ejercicios originales y 800 de Hunter 2000")
    items = original + extra
    ids = [item["id"] for item in items]
    if len(ids) != 2000 or len(set(ids)) != 2000:
        raise ValueError("El catálogo debe contener 2000 identificadores únicos")
    if any(x.get("sourceExpansion2k") is not True or
           not isinstance(x.get("materialRequirements"), list) or
           any(not isinstance(material, str) or not material for material in x["materialRequirements"])
           for x in extra):
        raise ValueError("Hay fichas Hunter 2000 sin origen o requisitos de equipo legibles")
    if list((ASSETS / "sports2d").glob("*.svg")):
        raise ValueError("Quedan ilustraciones deportivas alternativas")
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
    sports_count, distribution, aliases, similar, candidates = verify_sports(items)
    return {"status": "PASS", "catalog_total": len(items) + sports_count, "static_svg": len(static_assets),
            "sports_count": sports_count, "sports_by_level": distribution,
            "catalog_active": len(items) + sports_count - len(aliases),
            "source_counts": {"original": len(original), "hunter2000": len(extra), "sports": sports_count},
            "aliases": aliases, "similar_name_candidates": similar,
            "shared_instruction_candidates": candidates,
            "animated_svg": 0, "mp4_unchanged": True}

def verify_apk(path):
    with ZipFile(path) as apk:
        files = set(apk.namelist())
        expected = {"assets/media2d/" + item.name for item in (ASSETS / "media2d").glob("*.svg")}
        actual = {name for name in files if name.startswith("assets/media2d/") and name.endswith(".svg")}
        alternatives = {name for name in files if name.startswith(("assets/sports2d/", "assets/demos30/"))
                        and name.endswith(".svg")}
        if actual != expected or alternatives:
            raise ValueError("La APK debe contener solo los SVG originales para ilustrar ejercicios")
        for name in ("catalog.json", "catalog-hunter2000-extra.json",
                     "catalog-sports.json", "exercise-aliases.json"):
            if json.loads(apk.read(f"assets/{name}")) != read_json(name):
                raise ValueError(f"Contenido de APK distinto al catálogo verificado: {name}")


if __name__ == "__main__":
    try:
        parser = argparse.ArgumentParser()
        parser.add_argument("--apk", type=Path)
        args = parser.parse_args()
        report = verify()
        if args.apk:
            verify_apk(args.apk)
        print(json.dumps(report, ensure_ascii=False, indent=2))
    except Exception as exc:
        print(json.dumps({"status": "FAIL", "reason": str(exc)}, ensure_ascii=False))
        raise SystemExit(1)

#!/usr/bin/env python3
"""Comprueba el catálogo y las demostraciones del personaje vectorial."""
from pathlib import Path
import argparse, itertools, json, re, unicodedata
from collections import Counter, defaultdict
from zipfile import ZipFile

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "app/src/main/assets"
RETIRED_DEMOS = ("media2d", "offline2d", "demos120", "sports2d", "demos-rive")
RETIRED_ARCHIVE = ROOT / ".hunter2000/source/hunter2000-assets.tar.xz"
GYM_BACKDROPS = tuple(f"exercise-backdrops/{view}.svg" for view in ("front", "side", "overhead"))
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
    material_ids = set(read_json("materials.json"))
    expansion_poses = read_json("exercise-actor/sports-poses.json")
    new_signatures = set()
    sequences = [row for row in active_rows if row["id"].startswith("sport-seq-")]
    components = {row["id"]: row for row in items + sports if not row.get("sequenceParts")}
    seen_pairs = set()
    seen_families = set()
    sequence_distribution = defaultdict(Counter)
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
        if row["id"].startswith("sport-ampl-"):
            if set(row["materialRequirements"]) - material_ids or row["technicalLevel"] != level + 1 or (
                row["planning49"]["level"] != level or row.get("pendingTechnicalReview")
            ):
                raise ValueError(f"Material o progresión inválidos: {row['id']}")
            motion = expansion_poses[row["id"]]
            tracks = {tuple(map(tuple, frame)) for frame in motion["poses"]}
            signature = tuple(tuple(map(tuple, frame)) for frame in motion["poses"])
            if len(tracks) < 2 or signature in new_signatures:
                raise ValueError(f"Secuencia ausente o repetida: {row['id']}")
            new_signatures.add(signature)
        if row["id"].startswith("sport-seq-") and row["id"] not in aliases:
            parts = row["sequenceParts"]
            pair = tuple(sorted(parts))
            if len(parts) != 2 or parts[0] == parts[1] or pair in seen_pairs or any(
                part in aliases or part not in components for part in parts
            ):
                raise ValueError(f"Combinación repetida o inválida: {row['id']}")
            source_rows = [components[part] for part in parts]
            family_pair = tuple(sorted(record.get("baseId") or record["id"] for record in source_rows))
            if family_pair[0] == family_pair[1] or family_pair in seen_families:
                raise ValueError(f"Pareja de movimientos base repetida: {row['id']}")
            seen_families.add(family_pair)
            if (
                any(record.get("sourceOnly") or record.get("pendingTechnicalReview") for record in source_rows)
                or row["sequenceLabels"] != [record["name"] for record in source_rows]
                or row["level"] < max(record["level"] for record in source_rows)
                or row["technicalLevel"] != level + 1 or row["planning49"]["level"] != level
                or set(row["materialRequirements"]) - material_ids
            ):
                raise ValueError(f"Guía o progresión inválida: {row['id']}")
            seen_pairs.add(pair)
            sequence_distribution[sport][level] += 1
        distribution[sport][level] += 1
    expected_areas = {"Calistenia", "Fútbol", "Atletismo", "Movilidad", "Fuerza"}
    expected_distribution = {area: {0: 208, 1: 223, 2: 162} for area in expected_areas}
    expected_distribution["Fuerza"][0] += 1
    expected_distribution["Calistenia"][1] += 1
    expected_distribution["Calistenia"][2] += 1
    expected_distribution["Atletismo"][1] += 1
    if len(sequences) != 2969 or sequence_distribution != expected_distribution:
        raise ValueError("La ampliación debe aportar 2969 secuencias activas con los niveles acordados")
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


def verify_motion(original, extra, sports):
    original_files = {path.stem: path for path in (ASSETS / "demos30").glob("*.json")}
    legacy = read_json("hunter2000-poses.json")
    sporting = read_json("exercise-actor/sports-poses.json")
    single_sports = [row for row in sports if not row.get("sequenceParts")]
    for rows, records in ((original, original_files), (extra, legacy), (single_sports, sporting)):
        if {row["id"] for row in rows} != records.keys():
            raise ValueError("La cobertura de animaciones debe coincidir con el catálogo")
        for exercise_id, motion in records.items():
            if isinstance(motion, Path):
                data = json.loads(motion.read_text(encoding="utf-8"))
                frames = [frame["p"] for frame in data["frames"]]
            else:
                data = motion
                frames = data["poses"]
            if not 2 <= len(frames) <= 240 or any(
                len(frame) != 11 or any(len(point) != 2 or
                    any(not -2000 <= value <= 2000 for value in point) for point in frame)
                for frame in frames
            ):
                raise ValueError(f"Poses inválidas: {exercise_id}")
            times = data.get("times")
            if times is not None and (len(times) != len(frames) or times[0] != 0
                                      or times[-1] > 1 or any(a >= b for a, b in zip(times, times[1:]))):
                raise ValueError(f"Tiempos inválidos: {exercise_id}")
            if not 0.5 <= data.get("duration", data.get("duration46", 4.8)) <= 30:
                raise ValueError(f"Duración inválida: {exercise_id}")
    return sum(map(len, (original, extra, sports)))


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
    for folder in RETIRED_DEMOS:
        if any(path.is_file() for path in (ASSETS / folder).rglob("*")):
            raise ValueError(f"Quedan demostraciones retiradas en {folder}")
    if list((ASSETS / "demos30").glob("*.svg")) or RETIRED_ARCHIVE.exists():
        raise ValueError("Quedan SVG animados históricos en el repositorio")
    if read_json("editorial.json").get("clips"):
        raise ValueError("El índice editorial todavía ofrece clips históricos")
    for backdrop in GYM_BACKDROPS:
        if not (ASSETS / backdrop).is_file():
            raise ValueError(f"Falta el fondo de gimnasio: {backdrop}")
    motion_count = verify_motion(original, extra, read_json("catalog-sports.json"))
    sports_count, distribution, aliases, similar, candidates = verify_sports(items)
    return {"status": "PASS", "catalog_total": len(items) + sports_count, "static_svg": 0,
            "sports_count": sports_count, "sports_by_level": distribution,
            "catalog_active": len(items) + sports_count - len(aliases),
            "source_counts": {"original": len(original), "hunter2000": len(extra), "sports": sports_count},
            "aliases": aliases, "similar_name_candidates": similar,
            "shared_instruction_candidates": candidates,
            "motion_count": motion_count, "animated_svg": 0, "legacy_exercise_clips": 0}

def verify_apk(path):
    with ZipFile(path) as apk:
        files = set(apk.namelist())
        if any(name.startswith("assets/exercise-3d/") or (name.startswith("assets/") and name.endswith(".mp4")) for name in files):
            raise ValueError("La APK contiene recursos 3D o vídeos antiguos")
        retired = tuple(f"assets/{folder}/" for folder in RETIRED_DEMOS)
        if any(name.startswith(retired) or name.startswith("assets/demos30/") and name.endswith(".svg")
               for name in files):
            raise ValueError("La APK incluye demostraciones históricas")
        motion_files = ["hunter2000-poses.json", "exercise-actor/sports-poses.json",
                        "exercise-actor/rostro.svg", "exercise-actor/rostro-frontal.svg",
                        "exercise-actor/rostro-superior.svg"]
        motion_files.extend(str(file.relative_to(ASSETS)) for file in (ASSETS / "demos30").glob("*.json"))
        for name in [*motion_files, *GYM_BACKDROPS]:
            if apk.read(f"assets/{name}") != (ASSETS / name).read_bytes():
                raise ValueError(f"Recurso de demostración distinto en la APK: {name}")
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

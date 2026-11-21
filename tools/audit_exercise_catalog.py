#!/usr/bin/env python3
"""Cruza cada ficha canónica con las poses y el equipo que carga el reproductor nativo."""
import argparse
import csv
import json
import re
import unicodedata
from collections import Counter, defaultdict
from pathlib import Path


ASSETS = Path(__file__).resolve().parents[1] / "app/src/main/assets"
MATERIAL_FIELDS = ("materialRequirements", "materials46", "gymRequirements23")
# Una máquina puede integrar el equipo listado como sujeción o como accesorio.
GEAR = {
    "abwheel": "AB_WHEEL", "anchor": "ANCHOR BAND STRAP BATTLE_ROPE",
    "ball": "BALL MED_BALL SOCCER BASKETBALL VOLLEYBALL SWISS_BALL",
    "band": "BAND", "barbell": "BARBELL LANDMINE SMITH TRAP_BAR",
    "basketball": "BASKETBALL", "battlerope": "BATTLE_ROPE",
    "bench": "BENCH TABLE INCLINED_SUPPORT CHAIR HIP_LEVER BIKE AIR_BIKE RECUMBENT ROWER",
    "cable": "CABLE", "cardio": "BIKE AIR_BIKE RECUMBENT ROWER TREADMILL ELLIPTICAL STEP_MILL SKI_ERG",
    "chair": "CHAIR TABLE BENCH INCLINED_SUPPORT BIKE AIR_BIKE RECUMBENT",
    "cones": "CONES", "db": "DUMBBELL",
    "doorframe": "WALL", "foamroller": "FOAM_ROLLER", "footanchor": "ANCHOR",
    "harness": "HARNESS", "hurdle": "HURDLE", "jumprope": "JUMP_ROPE",
    "kb": "KETTLEBELL", "ladder": "LADDER", "landmine": "LANDMINE",
    "light_rope": "JUMP_ROPE", "lightlever": "STICK", "lowbar": "LOW_BAR PULL_BAR",
    "machine": "LEG_PRESS GUIDED_SQUAT LEG_LEVER TIBIALIS ARM_LEVER HIP_LEVER ASSISTED SMITH",
    "medball": "MED_BALL", "parallels": "PARALLETTES", "parallettes": "PARALLETTES",
    "plate": "PLATE", "plyobox": "BOX STEP", "pullbar": "PULL_BAR", "pullup_bar": "PULL_BAR",
    "rack": "RACK SMITH", "rings": "RINGS", "romanchair": "INCLINED_SUPPORT",
    "rope": "ROPE BATTLE_ROPE", "safetybar": "BARBELL", "slamball": "MED_BALL",
    "sled": "SLED", "slider": "SLIDER", "smith": "SMITH", "soccer_ball": "SOCCER",
    "step": "STEP BOX", "stick": "STICK", "strap": "STRAP", "support": "TABLE CHAIR BENCH WALL",
    "suspension": "STRAP", "swissball": "SWISS_BALL", "table": "TABLE BENCH",
    "tennis_racket": "RACKET", "throwwall": "WALL", "towel": "TOWEL",
    "trapbar": "TRAP_BAR", "volleyball": "VOLLEYBALL", "wall": "WALL"
}
# El suelo acolchado de la vista superior forma parte de la propia escena.
GEAR["mat"] = "MAT"

NAME_CUES = {
    "towel": r"\btoallas?\b",
    "slider": r"\bdiscos? deslizantes?\b",
    "db": r"\bmancuernas?\b",
    "cable": r"\bpoleas?\b",
    "kb": r"\b(?:kettlebells?|pesa rusa)\b",
    "foamroller": r"\brodillo de espuma\b",
    "smith": r"\bmaquina smith\b",
    "rings": r"\banillas\b",
    "bench": r"\bbanco (?:fijo|firme|estable)\b",
    "jumprope": r"\bcuerda de saltar\b"
}

SLIDER_SOURCE = "https://www.researchgate.net/publication/301602326_The_Sliding_Leg_Curl"
TOE_SOURCE = "https://orthoinfo.aaos.org/en/recovery/foot-and-ankle-conditioning-program/"
EXTENSION_SOURCE = "https://www.technogym.com/en-US/product/selection-900-leg-extension_MNJP.html"
SEATED_CURL_SOURCE = "https://www.technogym.com/en-INT/product/selection-700-leg-curl_MNIC.html"
PRONE_CURL_SOURCE = "https://www.yanrefitness.com/how-to-use-a-lying-leg-curl-machine/"
RENEGADE_SOURCE = "https://barbend.com/renegade-row/"
PULLOVER_SOURCE = "https://fitnessinstitute.com.au/pullover-hammer-strength/"
PALLOF_SOURCE = "https://www.issaonline.com/blogs/training-tips/pallof-press"
HIGH_ROW_SOURCE = "https://www.fitness24.nl/upload/docs/hammer-strength-iso-lateral-manual.pdf"
REFERENCES = {
    "home-ham-slide23": ("Recorrido y apoyos del deslizamiento bilateral", SLIDER_SOURCE),
    "home-ham-slide-one23": ("Recorrido y apoyo unilateral sobre la toalla", SLIDER_SOURCE),
    "towelrow": ("Toalla declarada como requisito para el remo isométrico", ""),
    "new46-toetowel": ("Flexión de dedos y talón fijo", TOE_SOURCE),
    "new46-slidereverse": ("Disco bajo el pie móvil", ""),
    "prone": ("Toalla bajo la frente", ""),
    "reverse": ("Toalla bajo la frente y recorrido de los brazos", ""),
    "home-renegade23": ("Mancuernas en ambos agarres", RENEGADE_SOURCE),
    "home-renegade-knee23": ("Mancuernas en ambos agarres", RENEGADE_SOURCE),
    "gym-leg-extension23": ("Eje de rodilla y contacto anterior del rodillo", EXTENSION_SOURCE),
    "gym-ham-seated23": ("Eje de rodilla y contacto posterior del rodillo", SEATED_CURL_SOURCE),
    "gym-ham-lying23": ("Eje de rodilla y contacto posterior del rodillo", PRONE_CURL_SOURCE),
    "gym-highrow30": ("Instrucciones y trayectoria descendente de las asas", HIGH_ROW_SOURCE),
    "gym-supportrow30": ("Instrucciones y apoyo de pecho", ""),
    "gym-lateralm30": ("Instrucciones y almohadillas laterales", ""),
    "gym-pulloverm30": ("Arco del brazo y apoyo de codos", PULLOVER_SOURCE),
    "hx_lower_0003": ("Pesa rusa declarada como carga", ""),
    "hx_lower_0012": ("Pesas rusas declaradas como carga", "")
}


def normalized(text):
    return "".join(char for char in unicodedata.normalize("NFD", text.casefold())
                   if not unicodedata.combining(char))


def materials(row):
    keys = {material for field in MATERIAL_FIELDS for material in (row.get(field) or [])}
    if row["equipment"] == "db":
        keys.add("db")
    return keys


def hints(row):
    title = normalized(row["name"])
    setup = normalized(row.get("setup", ""))
    instructions = normalized(" ".join((row.get("instructions") or [])[:2]))
    return {key for key, pattern in NAME_CUES.items()
            if any(re.search(pattern,text) for text in (title,setup,instructions))}


def source_review(rows, labels, signature_families, view):
    exercise = rows[view["canonica"]]
    gear = set(view["equipo"])
    phase = view.get("gesto_independiente", False)
    declared = {key for key in materials(exercise) if gear.intersection(GEAR.get(key, "").split())} if phase else materials(exercise)
    mentioned = set() if phase else hints(exercise)
    if view["vista"] == "OVERHEAD":
        gear.add("MAT")
    candidates = {label for label in declared | mentioned if label in GEAR
                  and not gear.intersection(GEAR[label].split())}
    warnings = set()
    if view["segmento_corto"]:
        warnings.add("segmento corto o escorzado")
    if not view["isometrico"] and not view["puntos_moviles"] and not view["gesto_de_detalle"]:
        warnings.add("sin recorrido corporal detectado")
    if not phase and len(signature_families[view["huella"]]) > 1:
        warnings.add("poses compartidas por fichas de distinta base")
    title = normalized(exercise["name"])
    if not view["isometrico"] and not re.search(r"\b(isometr\w+|sosten|sujecion)\b",title):
        fixed_squat = exercise["id"] in {"new46-deepshift", "sport-ampl-mobility-deep-squat-rotation"}
        if not fixed_squat and re.search(r"\b(curl femoral|extension de rodillas?|sentadilla)\b",title) and view.get("rango_rodilla",float("inf")) < 10:
            warnings.add("recorrido de rodilla pequeño para el movimiento descrito")
        if re.search(r"\b(curl de biceps|flexi(?:on|ones) de brazos?)\b",title) and view.get("rango_codo",float("inf")) < 10:
            warnings.add("recorrido de codo pequeño para el movimiento descrito")
    focus, source = REFERENCES.get(exercise.get("baseId", exercise["id"]), ("", ""))
    if exercise["id"].startswith("hx_core_") and title.startswith("pallof press "):
        focus, source = "Material de la variante", PALLOF_SOURCE
        if "pase de balon" in title or "sin carga" in title:
            warnings.add("verificar la variante frente al gesto de antirrotación")
    if view.get("origen") == "authored-phase" and not focus:
        focus = "Gesto separado con apoyos propios" if phase else "Posición y gesto corregidos en una sola animación"
    elif view.get("origen") == "authored-variant" and not focus:
        focus = "Recorrido y apoyo de la variante corregidos"
    return {
        "declared": {labels.get(k,k) for k in declared},
        "rendered": set(view["etiquetas"]), "review": {labels.get(k,k) for k in candidates},
        "vectors": sorted(view["svg"]), "moving_gear": view["equipo_movimiento"],
        "warnings": warnings, "focus": focus, "source": source
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--runtime", type=Path, required=True, help="JSON de ExerciseCatalogAuditTest")
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    runtime = json.loads(args.runtime.read_text())
    rows = {row["id"]: row for file in sorted(ASSETS.glob("catalog*.json"))
            for row in json.loads(file.read_text())}
    labels = json.loads((ASSETS / "materials.json").read_text())
    signature_families = defaultdict(set)
    for view in runtime["fuentes"].values():
        exercise = rows[view["canonica"]]
        if not view.get("gesto_independiente", False):
            signature_families[view["huella"]].add(exercise.get("baseId", exercise["id"]))
    source_rows = {id: source_review(rows,labels,signature_families,view)
                   for id,view in runtime["fuentes"].items()}
    output = []
    totals = Counter()
    for item in runtime["fichas"]:
        exercise = rows[item["id"]]
        parts = [source_rows[id] for id in item["fuentes"]]
        separator = " | "
        if exercise.get("compositeSequence") and len(parts)==1 and not item.get("movimiento_unico", False):
            parts = [{**parts[0], "warnings": parts[0]["warnings"] | {"secuencia compuesta sin clips separados por gesto"}}]
        if item.get("ficha_inconsistente", False):
            parts = [{"declared": {labels.get(k,k) for k in materials(exercise)},
                      "rendered": set(), "review": set(), "vectors": [], "moving_gear": False,
                      "warnings": {"instrucciones incompatibles, demostración en revisión"},
                      "focus": "", "source": PALLOF_SOURCE}]
        for key in ("review","warnings","focus"):
            totals[key] += any(part[key] for part in parts)
            totals[key+"_visibles"] += item["visible"] and any(part[key] for part in parts)
        output.append({
            "id": exercise["id"], "ejercicio": exercise["name"],
            "categoria": exercise.get("category",""), "familia": exercise.get("baseId",exercise["id"]),
            "visible_en_catalogo": "sí" if item["visible"] else "no",
            "fuentes_de_animacion": separator.join(item["fuentes"]),
            "equipo_declarado_por_gesto": separator.join(", ".join(sorted(part["declared"])) or "Sin equipo declarado" for part in parts),
            "equipo_dibujado_por_gesto": separator.join(", ".join(sorted(part["rendered"])) or "Sin equipo dibujado" for part in parts),
            "svg_de_equipo_por_gesto": separator.join(", ".join(part["vectors"]) or "—" for part in parts),
            "equipo_en_movimiento_por_gesto": separator.join("sí" if part["moving_gear"] else "no" for part in parts),
            "posible_equipo_faltante": separator.join(", ".join(sorted(part["review"])) or "—" for part in parts),
            "indicadores_movimiento": separator.join(", ".join(sorted(part["warnings"])) or "—" for part in parts),
            "detalle_corregido": separator.join(part["focus"] or "—" for part in parts),
            "referencia_del_detalle": separator.join(part["source"] or "—" for part in parts),
            "revision_biomecanica_integral": "Pendiente",
        })
    args.output.parent.mkdir(parents=True,exist_ok=True)
    with args.output.open("w",encoding="utf-8-sig",newline="") as stream:
        writer = csv.DictWriter(stream,fieldnames=list(output[0]))
        writer.writeheader()
        writer.writerows(output)
    print(json.dumps({"fichas":len(output), "fuentes":len(source_rows),
                      "visibles":sum(item["visible"] for item in runtime["fichas"]),
                      "fuentes_con_ajustes":sum(bool(source["focus"]) for source in source_rows.values()),
                      "fuentes_a_revisar_equipo":sum(bool(source["review"]) for source in source_rows.values()),
                      **totals},ensure_ascii=False))


if __name__ == "__main__":
    main()

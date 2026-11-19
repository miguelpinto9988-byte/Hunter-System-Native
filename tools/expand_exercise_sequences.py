#!/usr/bin/env python3
"""Build named two-gesture exercises using existing, independently playable motion clips."""

import itertools
import json
import math
from functools import cache
from collections import Counter
from pathlib import Path

ASSETS = Path(__file__).resolve().parents[1] / "app/src/main/assets"
TARGETS = (68, 73, 52)
PREFIX = "sport-seq-"

# Clips are grouped by training purpose. Each tier adds gestures with new demands.
POOLS = (
    ("calistenia", "Calistenia", "Fuerza", "core",
     """wallplank wall kneepush prone bridge standingextend standingcurl deadbug bird heel
     towelrow kickback kneelingplank home-pushcloseknee30 home-frog23 home-reversebird30
     sport-ampl-wall-plus""",
     """home-scappush30 split pushup incline home-deadbugpress30 home-bridge-walk23
     new46-invertedrow sport-ampl-pushup-staggered-hands sport-ampl-hang-knee-pause""",
     """home-staggered23 closepush plank home-bear-tap23 home-bearreach30
     sport-ampl-lean-planche sport-ampl-l-sit-single-switch"""),
    ("futbol", "Fútbol", "Cardio", "cardio",
     """sport-futbol-sole sport-ampl-football-toe-taps sport-ampl-football-inside-out
     fastmarch sidestep vstep stepreach doubletouch kneeliftcardio heelback walk linewalk
     stepjack tandem march""",
     """sport-ampl-football-hook-behind sport-ampl-football-v-pull skaterreach
     sport-ampl-track-shuffle-brake jog stepcross shuffle home-lateralstepover30""",
     """sport-ampl-football-shield-exit new46-sprintbrake shuffle-touch19
     new46-backrun sport-ampl-track-bound-single"""),
    ("atletismo", "Atletismo", "Cardio", "cardio",
     """fastmarch walk heelback kneeliftcardio march sidestep stepjack vstep doubletouch
     stepreach linewalk wallcalf tandem sport-track-a-wall sport-ampl-track-march-toe
     standingabduct toelift""",
     """jog shuffle stepcross skater skaterreach new46-askip new46-backrun
     new46-doublepogo sport-ampl-track-a-skip sport-ampl-track-grapevine""",
     """new46-sprintbrake new46-hexagon jumpjack skaterhop new46-turnjump
     shuffle-touch19 sport-ampl-track-bound-single new46-singlepogo"""),
    ("movilidad", "Movilidad", "Movilidad", "mobility",
     """chestopen hipcircle shoulder sidebendstand wrist neck standingabduct cat calf ankle
     hipflex hamstretch calfstretch new46-pelvictilt home-hip909030 new46-frogrock
     sport-ampl-mobility-cross-reach sport-ampl-mobility-side-lying-arm-arc
     sport-ampl-mobility-wall-shoulder-cactus""",
     """new46-shouldercar new46-hip9090lift new46-lungerotation new46-deepshift
     balance new46-couchstretch sport-ampl-mobility-ankle-halfkneel""",
     """new46-airplanefree home-cossack23 home-airplane23
     sport-yoga-warrior3 sport-ampl-mobility-deep-squat-rotation"""),
    ("fuerza", "Fuerza", "Fuerza", "lower",
     """chair hinge bridge kickback standingcurl standingextend legextend wall wallplank
     kneepush home-frog23 new46-tke new46-bandrow new46-ballsqueeze
     sport-ampl-strength-hip-hinge-wall seatedcalf wallcalf""",
     """squat split reverselunge stepup singlecalf home-curtsy23 home-bridgemarch30
     new46-dbpushpress sport-ampl-strength-offset-squat sport-ampl-strength-split-squat-reach
     sport-ampl-strength-calf-opposite-knee""",
     """home-step-cross23 singlebridge home-pistol-box23 home-thrustsingle30 new46-kbswing
     sport-ampl-strength-single-leg-row"""),
)

# Some early fichas describe material in prose rather than in their material list.
MATERIAL = {
    "chair": ["chair"], "wall": ["wall"], "wallplank": ["wall"], "wallcalf": ["wall"],
    "standingabduct": ["support"], "standingextend": ["support"], "standingcurl": ["support"],
    "tandem": ["support"], "linewalk": ["wall"], "calfstretch": ["wall"],
    "seatedcalf": ["chair"], "legextend": ["chair"], "toelift": ["chair"],
    "towelrow": ["towel"], "incline": ["bench"], "new46-invertedrow": ["lowbar"],
    "new46-sprintbrake": ["cones"], "new46-hexagon": ["space"],
    "new46-tke": ["band", "anchor"], "new46-bandrow": ["band", "anchor"],
    "new46-ballsqueeze": ["ball", "chair"], "new46-kbswing": ["kb"],
    "new46-ballhip": ["swissball"], "new46-couchstretch": ["wall"],
    "home-airplane23": ["support"], "new46-lungerotation": ["mat"],
    "sport-futbol-sole": ["ball"], "sport-track-a-wall": ["wall"],
}
GROUP_NAMES = {
    "upper": "Tren superior", "core": "Core", "lower": "Piernas",
    "cardio": "Cuerpo completo", "mobility": "Cuerpo completo",
}


def load(name):
    return json.loads((ASSETS / name).read_text())


def component_material(row):
    if row["id"] in MATERIAL:
        return MATERIAL[row["id"]]
    return row.get("materialRequirements", []) + row.get("materials46", [])


def patterns(row):
    return row["planning49"].get("pattern", row["id"])


def first_sentence(row):
    instructions = row.get("instructions") or [row["steps"]]
    return " ".join(text.strip().rstrip(".") + "." for text in instructions)


@cache
def duration(id):
    if id.startswith("sport-"):
        return sport_tracks()[id].get("duration", 4.8)
    track = load(f"demos30/{id}.json")
    return track.get("duration", track.get("duration46", 4.8))


@cache
def sport_tracks():
    return load("exercise-actor/sports-poses.json")


def compose(discipline, category, group, level, first, second, labels):
    if discipline in ("Fuerza", "Calistenia") and first["planning49"]["group"] in ("upper", "core", "lower"):
        group = first["planning49"]["group"]
    material = sorted(set(component_material(first) + component_material(second)))
    gear = {first.get("equipment"), second.get("equipment")}
    equipment = "gym" if "gym" in gear else "db" if "db" in gear or "db" in material else "body"
    impact = any(r["planning49"].get("impact") or r.get("impact") == "Con saltos" for r in (first, second))
    power = any(r["planning49"].get("power") for r in (first, second))
    first_name, second_name = first["name"], second["name"]
    category_name = "Movilidad" if group == "mobility" else category
    seconds = math.ceil(sum(duration(item["id"]) for item in (first, second))) + 2
    dose = f"{seconds} segundos para una pasada por los dos gestos a ritmo cómodo."
    family = f'{first["planning49"].get("family", first["id"])}-con-transicion'
    return {
        "name": f"{first_name} → {second_name}",
        "sport": discipline, "level": level, "category": category_name,
        "group": GROUP_NAMES[group], "unit": "sec", "variant": "base",
        "equipment": equipment, "materialRequirements": material,
        "equipmentLabel": ", ".join(labels.get(item, item) for item in material) or "Sin equipo",
        "setup": "Prepara el espacio y los apoyos de ambos gestos. "
            + " ".join(filter(None, (first.get("setup"), second.get("setup")))),
        "instructions": [
            f"{first_name}. {first_sentence(first)}",
            "Termina el primer gesto en una posición estable. Recolócate para el segundo.",
            f"{second_name}. {first_sentence(second)}",
        ],
        "commonErrors": "Iniciar el segundo gesto antes de recuperar el equilibrio o mantener un apoyo que no corresponde.",
        "initialDose": dose, "compositeSequence": True,
        "sequenceParts": [first["id"], second["id"]],
        "sequenceLabels": [first_name, second_name],
        "zones": sorted(set(first.get("zones", []) + second.get("zones", []))),
        "impact": "Con saltos" if impact else "Sin saltos",
        "intensity": "high" if impact and level == 2 else "moderate" if level else "low",
        "technicalLevel": level + 1,
        "doseCap46": {"initial": seconds, "target": seconds, "sets": 1, "rest": 60},
        **{key: bool(first.get(key) or second.get(key)) for key in
           ("floor", "standing", "overhead", "kneeBend", "spineMove", "wristLoad", "requiresSupervision")},
        "seated": bool(first.get("seated") and second.get("seated")),
        "planning49": {
            "family": family, "pattern": f"{patterns(first)} y {patterns(second)}",
            "group": group, "level": level, "role": "accessory", "primary": False,
            "impact": impact, "power": power, "conditioning": level + 1,
        },
    }


def main():
    sports = [row for row in load("catalog-sports.json") if not row["id"].startswith(PREFIX)]
    originals = load("catalog.json")
    components = {row["id"]: row for row in originals + sports}
    labels = load("materials.json")
    aliases = load("exercise-aliases.json")
    used = set()
    sequences = []
    for slug, discipline, category, group, *tiers in POOLS:
        counts = Counter()
        candidates = [(components[id], level) for level, tier in enumerate(tiers) for id in tier.split()]
        for level, target in enumerate(TARGETS):
            options = [
                (first, second) for (first, a), (second, b) in itertools.combinations(candidates, 2)
                if max(a, b) == level and first["id"] not in aliases and second["id"] not in aliases
                and not first.get("pendingTechnicalReview") and not second.get("pendingTechnicalReview")
                and patterns(first) != patterns(second)
                and first.get("baseId", first["id"]) != second.get("baseId", second["id"])
                and tuple(sorted((first["id"], second["id"]))) not in used
            ]
            if len(options) < target:
                raise ValueError(f"{discipline} / {level}: {len(options)} distinct pairs available")
            for _ in range(target):
                # Spread combinations across primary gestures and avoid repeating the same few clips.
                index = min(range(len(options)), key=lambda i: (
                    counts[options[i][0]["id"]] + counts[options[i][1]["id"]],
                    -sum(item["sport"] == discipline for item in options[i] if "sport" in item),
                    i,
                ))
                first, second = options.pop(index)
                counts.update((first["id"], second["id"]))
                used.add(tuple(sorted((first["id"], second["id"]))))
                row = compose(discipline, category, group, level, first, second, labels)
                row["id"] = f"{PREFIX}{slug}-{first['id']}-{second['id']}"
                sequences.append(row)
    (ASSETS / "catalog-sports.json").write_text(
        json.dumps(sports, ensure_ascii=False, indent=2)[:-2] + ",\n" + ",\n".join(
            "  " + json.dumps(row, ensure_ascii=False, separators=(",", ":")) for row in sequences
        ) + "\n]\n", encoding="utf-8"
    )


if __name__ == "__main__":
    main()

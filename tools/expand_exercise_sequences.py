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

# The second release adds new gestures to each of the established training pools.
ADDITIONS = (
    ("shoulder home-wallangel30 new46-scapcar new46-wristrock new46-firehydrant",
     "home-handrelease23 home-plank-toe23 home-bear-return23 home-hollow-tuck23",
     "home-push-tap23 home-spiderman23 home-body-saw23 home-hindu23"),
    ("sport-ampl-track-march-toe sport-track-a-wall standingabduct new46-shortfoot heeldig frontbackstep ankledrive",
     "sport-ampl-track-grapevine new46-ladderinout new46-ickyshuffle new46-laddercarioca",
     "new46-shuttle new46-tdrill new46-forwardhop skater-stick19"),
    ("new46-bentcalfstretch heeltoe new46-walltibia new46-shortfoot heeldig frontbackstep standingcurl hipflex calf",
     "new46-heelwalk new46-toewalk new46-droplanding sport-ampl-track-shuffle-brake",
     "new46-shuttle new46-tdrill new46-lateralhurdle new46-forwardhop"),
    ("new46-quadthoracic new46-adductorrock new46-seatedhipir new46-butterfly new46-pelvicclock",
     "new46-hipcarquad new46-seatedhinge skaterreach",
     "home-skater-squat23 home-shrimp23 sport-ampl-strength-single-leg-row"),
    ("new46-firehydrant new46-walltibia new46-reverseclam new46-sideadduct new46-ankleinvert",
     "new46-soleus new46-slidelateral new46-monsterwalk home-bridge-walk23 new46-bandkick",
     "new46-trxsplit new46-copenhagendyn home-ham-slide-one23 new46-spanishsquat"),
)
SECOND_TARGETS = (70, 75, 55)


THIRD_ADDITIONS = (
    ("hs379-scapular hs379-palms hs379-isobiceps pau-pushchild new46-serratusband "
     "new46-bandpullapart clam",
     "home-reverse-plank-lift23 home-legraise23 home-knee-tuck23 home-sidehip30 "
     "new46-scapularhang new46-scapdip new46-trxpress",
     "new46-paralleldip home-push-one23 home-side-knee23 home-side-thread23 "
     "home-crab23 home-hollow-rock23"),
    ("calf new46-walltibia new46-bentcalfstretch standingcurl hipcircle "
     "new46-ankleinvert new46-ankleevert new46-toeyoga",
     "new46-heelwalk new46-toewalk home-hopforward30 new46-droplanding "
     "sport-ampl-track-a-skip new46-pivotlunge",
     "new46-crosshop new46-turnjump new46-lateralhurdle new46-splitjump "
     "sport-ampl-track-bound-single"),
    ("ankle heeldig frontbackstep new46-bandplantar new46-toeyoga new46-ankleinvert "
     "new46-ankleevert new46-pelvictilt",
     "home-hopforward30 new46-ladderinout new46-laddercarioca "
     "new46-jumpropealt new46-jumpropeside new46-ickyshuffle",
     "new46-crosshop new46-boxjump skater-stick19 new46-resistedsprint new46-depthjump"),
    ("home-wallangel30 childpose latstretch new46-crossshoulder new46-stickextension "
     "new46-doorpec new46-hipcapsule new46-scapcar",
     "home-step-lateral23 new46-hiphike new46-stepdown new46-soleus "
     "new46-trxsquat new46-pivotlunge",
     "home-side-thread23 home-sideabduct30 home-splithigh30 new46-ringturnout "
     "new46-sissysquat"),
    ("new46-bandplantar new46-ankleevert new46-bandpullapart new46-bander new46-bandir "
     "seatedhip pau-lateralcurl",
     "home-rdlstagger30 home-lungefront30 home-floor-fly23 home-hammer-cross23 "
     "home-thruster-db23 new46-suitcasecarry new46-rackcarry",
     "home-rdlsuit30 home-lungeoffset30 home-renegade23 home-cyclistdb30 "
     "home-clean-db23 new46-cubanpress"),
)
THIRD_TARGETS = (70, 75, 55)
# Keep the historical IDs as aliases. The forward catalog uses one canonical movement.
COMPONENT_MERGES = {
    "home-staggered23": "sport-ampl-pushup-staggered-hands",
    "new46-askip": "sport-ampl-track-a-skip",
    "sport-calisthenics-scapula": "new46-scapularhang",
}
MERGED_COMPONENT_REPLACEMENTS = {
    ("Calistenia", 1): 1, ("Calistenia", 2): 1, ("Atletismo", 1): 1,
}


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
    "home-wallangel30": ["wall"], "heeltoe": ["chair"],
    "ankledrive": ["wall"],
    "seatedhip": ["chair"], "hs379-scapular": ["chair"],
    "hs379-palms": ["chair"], "hs379-isobiceps": ["chair"],
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


def pair_key(first, second):
    return tuple(sorted(row.get("baseId") or row["id"] for row in (first, second)))


def build_sequences(components, labels, aliases, used, targets, additions, wave, replacements=None):
    sequences = []
    for (slug, discipline, category, group, *tiers), extra in zip(POOLS, additions):
        extra_ids = set(" ".join(extra).split())
        tiers = [f"{base} {extension}" for base, extension in zip(tiers, extra)]
        counts = Counter()
        candidate_levels = {}
        for level, tier in enumerate(tiers):
            for identity in tier.split():
                candidate_levels.setdefault(identity, level)
        candidates = [(components[identity], level) for identity, level in candidate_levels.items()]
        for level, target in enumerate(targets):
            if replacements is not None:
                target += replacements.get((discipline, level), 0)
            elif wave == 3 and slug == "fuerza" and level == 0:
                target += 1
            options = [
                (first, second) for (first, a), (second, b) in itertools.combinations(candidates, 2)
                if max(a, b) == level and first["id"] not in aliases and second["id"] not in aliases
                and (not extra_ids or first["id"] in extra_ids or second["id"] in extra_ids)
                and not first.get("sourceOnly") and not second.get("sourceOnly")
                and not first.get("pendingTechnicalReview") and not second.get("pendingTechnicalReview")
                and patterns(first) != patterns(second)
                and first.get("baseId", first["id"]) != second.get("baseId", second["id"])
                and pair_key(first, second) not in used
            ]
            if len(options) < target:
                raise ValueError(f"{discipline} / {level}: {len(options)} distinct pairs available")
            for _ in range(target):
                # Spread combinations across primary gestures and avoid repeating the same few clips.
                index = min(range(len(options)), key=lambda i: (
                    counts[options[i][0]["id"]] + counts[options[i][1]["id"]],
                    -sum(item["sport"] == discipline for item in options[i] if "sport" in item),
                    -(bool(extra_ids) and bool(options[i][0].get("floor")) == bool(options[i][1].get("floor"))),
                    i,
                ))
                first, second = options.pop(index)
                counts.update((first["id"], second["id"]))
                used.add(pair_key(first, second))
                row = compose(discipline, category, group, level, first, second, labels)
                row["id"] = f"{PREFIX}{slug}-{first['id']}-{second['id']}"
                if wave > 1:
                    row["catalogWave"] = wave
                sequences.append(row)
    return sequences


def consolidate_components(sequences, components, labels, aliases):
    replacements = Counter(MERGED_COMPONENT_REPLACEMENTS)
    canonical_pairs = {}
    groups = {discipline: (category, group) for _, discipline, category, group, *_ in POOLS}
    for index, row in enumerate(sequences):
        parts = [COMPONENT_MERGES.get(part, part) for part in row["sequenceParts"]]
        first, second = (components[part] for part in parts)
        pair = pair_key(first, second)
        if pair[0] == pair[1] or pair in canonical_pairs:
            aliases[row["id"]] = parts[0] if pair[0] == pair[1] else canonical_pairs[pair]
            replacements[(row["sport"], row["level"])] += 1
            continue
        canonical_pairs[pair] = row["id"]
        if parts != row["sequenceParts"]:
            category, group = groups[row["sport"]]
            updated = compose(row["sport"], category, group, row["level"], first, second, labels)
            updated["id"] = row["id"]
            if "catalogWave" in row:
                updated["catalogWave"] = row["catalogWave"]
            sequences[index] = updated
    return replacements, set(canonical_pairs)


def main():
    sports = [row for row in load("catalog-sports.json") if not row["id"].startswith(PREFIX)]
    originals = load("catalog.json")
    components = {row["id"]: row for row in originals + sports}
    labels = load("materials.json")
    aliases = {
        old: new for old, new in load("exercise-aliases.json").items()
        if old not in COMPONENT_MERGES and not old.startswith(PREFIX)
    }
    used = set()
    sequences = []
    sequences.extend(build_sequences(components, labels, aliases, used, TARGETS, [("", "", "")] * len(POOLS), 1))
    sequences.extend(build_sequences(components, labels, aliases, used, SECOND_TARGETS, ADDITIONS, 2))
    sequences.extend(build_sequences(components, labels, aliases, used, THIRD_TARGETS, THIRD_ADDITIONS, 3))
    aliases.update(COMPONENT_MERGES)
    replacements, canonical_pairs = consolidate_components(sequences, components, labels, aliases)
    replacements_used = used | canonical_pairs
    all_additions = [
        tuple(" ".join(pool) for pool in zip(first, second))
        for first, second in zip(ADDITIONS, THIRD_ADDITIONS)
    ]
    sequences.extend(build_sequences(
        components, labels, aliases, replacements_used, (0, 0, 0), all_additions, 3, replacements
    ))
    (ASSETS / "exercise-aliases.json").write_text(
        json.dumps(aliases, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )
    (ASSETS / "catalog-sports.json").write_text(
        json.dumps(sports, ensure_ascii=False, indent=2)[:-2] + ",\n" + ",\n".join(
            "  " + json.dumps(row, ensure_ascii=False, separators=(",", ":")) for row in sequences
        ) + "\n]\n", encoding="utf-8"
    )


if __name__ == "__main__":
    main()

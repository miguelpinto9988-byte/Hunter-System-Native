#!/usr/bin/env python3
"""Report pose issues that can be measured without judging exercise technique."""
import argparse
from collections import Counter, defaultdict
import json
from math import dist
from pathlib import Path

ASSETS = Path(__file__).resolve().parents[1] / "app/src/main/assets"
SEGMENTS = [(0, 1), (1, 2), (1, 3), (3, 4), (1, 5), (5, 6), (2, 7), (7, 8), (2, 9), (9, 10)]


def load(path):
    return json.loads(path.read_text())


def audit():
    poses = {path.stem: load(path) for path in (ASSETS / "demos30").glob("*.json")}
    poses.update(load(ASSETS / "hunter2000-poses.json"))
    poses.update(load(ASSETS / "exercise-actor" / "sports-poses.json"))
    records = [row for path in sorted(ASSETS.glob("catalog*.json")) for row in load(path)]
    issues = defaultdict(set)
    fingerprints = defaultdict(list)
    for exercise_id, record in sorted(poses.items()):
        frames = [frame["p"] for frame in record["frames"]] if "frames" in record else record["poses"]
        camera = (record.get("view21") or {}).get("primary")
        if len(frames) < 2 or any(len(frame) != 11 for frame in frames):
            issues[exercise_id].add("estructura_incompleta")
            continue
        if not record.get("hold") and all(frame == frames[0] for frame in frames):
            issues[exercise_id].add("fotogramas_identicos_sin_marca_isometrica")
        if camera == "front" and all(abs(f[2][1]-f[1][1]) < abs(f[2][0]-f[1][0])*.4 for f in frames):
            issues[exercise_id].add("torso_horizontal_con_proyeccion_frontal")
        first_torso = dist(frames[0][1], frames[0][2])
        if any(dist(frame[a], frame[b]) < first_torso*.035 for frame in frames for a, b in SEGMENTS):
            issues[exercise_id].add("segmento_colapsado_o_escorzado")
        fingerprints[json.dumps(frames, separators=(",", ":"))].append(exercise_id)

    by_id = {row["id"]: row for row in records}
    for ids in fingerprints.values():
        families = {by_id[id].get("baseId", id) for id in ids if id in by_id}
        if len(families) > 1:
            for id in ids:
                issues[id].add("poses_compartidas_entre_variantes_de_distintas_bases")

    rows = []
    for record in records:
        exercise_id = record["id"]
        ids = record.get("sequenceParts") or [exercise_id]
        found = {id: sorted(issues[id]) for id in ids if issues[id]}
        if missing := [id for id in ids if id not in poses]:
            found.update({id: ["sin_fotogramas"] for id in missing})
        if found:
            rows.append({"id": exercise_id, "name": record["name"], "sources": found})
    return {"fichas_fuente": len(records), "fuentes_de_poses": len(poses),
            "fichas_con_indicadores": len(rows),
            "indicadores_por_fuente": dict(Counter(issue for values in issues.values() for issue in values)),
            "fichas": rows}


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()
    result = audit()
    if args.output:
        args.output.write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n")
    print(json.dumps({key: val for key,val in result.items() if key != "fichas"}, ensure_ascii=False, indent=2))

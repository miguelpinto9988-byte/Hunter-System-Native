#!/usr/bin/env python3
"""Separar un SVG animado antiguo en movimiento (JSON) y apariencia (SVG).

El archivo SVG animado original se obtiene de un commit histórico con 'git show'
para no reintroducir los recursos demos30 en los assets de Android.

Ejemplos:
  python3 tools/split_legacy_svg_layers.py squat curl
  python3 tools/split_legacy_svg_layers.py --all --out build/svg-dos-capas
"""
import argparse
import json
import re
import subprocess
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SOURCE_COMMIT = "c0802ce30837605752faed9a97a161beb9a11838"
SVG_NS = "http://www.w3.org/2000/svg"
ET.register_namespace("", SVG_NS)
ET.register_namespace("xlink", "http://www.w3.org/1999/xlink")
CHANNEL_TAGS = {"animate", "animateTransform", "animateMotion", "set"}
BONES = [[0, 1], [1, 2], [1, 3], [3, 4], [1, 5], [5, 6],
         [2, 7], [7, 8], [2, 9], [9, 10]]
JOINTS = ["cabeza", "hombros", "cadera", "codo_a", "mano_a",
          "codo_b", "mano_b", "rodilla_a", "pie_a", "rodilla_b", "pie_b"]

def source(ref, path):
    return subprocess.check_output(["git", "-C", str(ROOT), "show", f"{ref}:{path}"])

def local_name(tag):
    return tag.rsplit("}", 1)[-1]

def split_one(ref, exercise, out):
    if not re.fullmatch(r"[a-zA-Z0-9_-]+", exercise):
        raise ValueError("Identificador de ejercicio no válido")
    prefix = f"app/src/main/assets/demos30/{exercise}"
    original = source(ref, prefix + ".svg")
    frames = json.loads(source(ref, prefix + ".json"))["frames"]
    root = ET.fromstring(original)
    tracks = []
    counter = 0
    for parent in root.iter():
        for child in list(parent):
            kind = local_name(child.tag)
            if kind not in CHANNEL_TAGS:
                continue
            if "data-rig-key" not in parent.attrib:
                counter += 1
                parent.set("data-rig-key", f"rig-{counter}")
            tracks.append({
                "target": parent.attrib["data-rig-key"],
                "element": kind,
                "attributes": {local_name(k): v for k, v in child.attrib.items()},
            })
            parent.remove(child)

    skin = ET.tostring(root, encoding="unicode", xml_declaration=False)
    if re.search(r"<(?:animate|animateTransform|animateMotion|set)\b", skin):
        raise ValueError("La apariencia todavía contiene animación")
    duration = max(
        (float(str(track["attributes"].get("dur", "0")).rstrip("s"))
         for track in tracks if "dur" in track["attributes"]), default=0.0)
    points = [frame["p"] for frame in frames]
    if any(len(pose) != len(JOINTS) for pose in points):
        raise ValueError(f"Poses con longitud inesperada: {exercise}")
    motion = {
        "version": 1,
        "exercise": exercise,
        "source": f"demos30/{exercise}.svg",
        "description": "Canales originales de movimiento, sin atributos estéticos.",
        "durationSeconds": duration,
        "skeleton": {
            "coordinateSystem": "Coordenadas originales de las poses JSON, independientes del viewBox del arte.",
            "boneEdges": BONES,
            "jointLabels": JOINTS,
            "frames": points,
        },
        "tracks": tracks,
    }
    out.mkdir(parents=True, exist_ok=True)
    (out / f"{exercise}.appearance.svg").write_text(skin, encoding="utf-8")
    (out / f"{exercise}.motion.json").write_text(
        json.dumps(motion, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")
    return {"exercise": exercise, "frames": len(points), "tracks": len(tracks)}

def main():
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("exercises", nargs="*", help="IDs de ejercicios")
    ap.add_argument("--all", action="store_true", help="Extraer todos los ejercicios del commit")
    ap.add_argument("--ref", default=SOURCE_COMMIT, help="Commit con los SVG originales")
    ap.add_argument("--out", type=Path, default=ROOT / "docs/svg-dos-capas")
    args = ap.parse_args()
    exercises = list(args.exercises)
    if args.all:
        all_files = subprocess.check_output(
            ["git", "-C", str(ROOT), "ls-tree", "-r", "--name-only", args.ref,
             "app/src/main/assets/demos30"], text=True)
        exercises += [Path(p).stem for p in all_files.splitlines() if p.endswith(".svg")]
    ids = list(dict.fromkeys(exercises))
    if not ids:
        ap.error("Indica al menos un ejercicio o usa --all")
    results = [split_one(args.ref, exercise, args.out) for exercise in ids]
    print(json.dumps({"converted": len(results), "examples": results[:5]}, ensure_ascii=False))

if __name__ == "__main__":
    main()

#!/usr/bin/env python3
"""Recover skeletal tracks from the historical Hunter 2000 illustrations."""
import json
import re
import subprocess
import tarfile
from pathlib import Path
from xml.etree import ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "app/src/main/assets"
COMMIT = "42789455b740bdc66a91fa35966d2af4c5f2fa91"
SVG = "{http://www.w3.org/2000/svg}"
NUMBER = re.compile(r"[-+]?(?:\d*\.\d+|\d+)")


def extract(svg):
    root = ET.fromstring(svg)
    elements = {item.get("id"): item for item in root.iter() if item.get("id")}
    duration, times = None, None

    def joints(name):
        nonlocal duration, times
        animation = elements[name].find(SVG + "animate")
        duration_text = animation.attrib["dur"]
        track_times = animation.attrib["keyTimes"]
        if duration is None:
            duration, times = duration_text, track_times
        if (duration, times) != (duration_text, track_times):
            raise ValueError("Pistas con tiempos distintos")
        values = [list(map(float, NUMBER.findall(value))) for value in animation.attrib["values"].split(";")]
        if any(len(value) != 22 for value in values):
            raise ValueError(f"Trayectoria no reconocida: {name}")
        return [(value[18:20], value[8:10]) for value in values]

    neck = joints("neck")
    upper = joints("front-upper-arm")
    far_upper = joints("back-upper-arm")
    wrist = joints("front-forearm")
    far_wrist = joints("back-forearm")
    thigh = joints("thigh")
    far_thigh = joints("back-thigh")
    ankle = joints("calf")
    far_ankle = joints("back-calf")
    frames = []
    for i, ((nx, ny), shoulder) in enumerate(neck):
        dx, dy = nx - shoulder[0], ny - shoulder[1]
        hip = [(x + y) / 2 for x, y in zip(thigh[i][0], far_thigh[i][0])]
        frames.append([[nx + dx * 1.2, ny + dy * 1.2], shoulder, hip, upper[i][1], wrist[i][1],
                       far_upper[i][1], far_wrist[i][1], thigh[i][1], ankle[i][1],
                       far_thigh[i][1], far_ankle[i][1]])
    return {"duration": float(duration.removesuffix("s")), "times": list(map(float, times.split(";"))),
            "poses": frames}


def main():
    ids = {row["id"] for row in json.loads((ASSETS / "catalog-hunter2000-extra.json").read_text())}
    animations = {}
    with subprocess.Popen(["git", "archive", COMMIT, "app/src/main/assets/demos30"],
                          cwd=ROOT, stdout=subprocess.PIPE) as archive:
        with tarfile.open(fileobj=archive.stdout, mode="r|") as files:
            for file in files:
                path = Path(file.name)
                if path.stem in ids and path.suffix == ".svg":
                    animations[path.stem] = extract(files.extractfile(file).read())
        if archive.wait():
            raise RuntimeError("No se pudo leer el archivo histórico")
    if animations.keys() != ids:
        raise ValueError(f"Faltan pistas: {sorted(ids - animations.keys())}")
    (ASSETS / "hunter2000-poses.json").write_text(
        json.dumps(animations, separators=(",", ":"), sort_keys=True) + "\n")


if __name__ == "__main__":
    main()

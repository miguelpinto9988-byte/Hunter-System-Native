#!/usr/bin/env python3
"""Verifica rig 3D proyectado, SMIL, continuidad y anclajes de los tres SVG delgados.

Es una prueba geométrica; NO certifica técnica deportiva ni valida dispositivos Android.
"""
from __future__ import annotations

import math
import pathlib
import sys
import xml.etree.ElementTree as ET

ROOT = (pathlib.Path(sys.argv[1]) if len(sys.argv) > 1 else
        pathlib.Path(__file__).resolve().parents[1] / "app/src/main/assets/media2d/rig-v2")
NS = "{http://www.w3.org/2000/svg}"
FILES = {
    "squat_front.svg": ("front", ("ankleL", "ankleR", "toeL", "toeR")),
    "squat_side.svg": ("side", ("ankleL", "ankleR", "toeL", "toeR")),
    "pushup_top.svg": ("top", ("wristL", "wristR", "ankleL", "ankleR", "toeL", "toeR")),
}

def norm3(a, b):
    return math.sqrt(sum((x - y) ** 2 for x, y in zip(a, b)))

def run():
    for name, (view, anchors) in FILES.items():
        svg = ET.parse(ROOT / name).getroot()
        assert svg.tag == NS + "svg", name
        assert svg.get("data-view") == view, name
        assert svg.get("data-renderer") == "thin-rig-v2", name
        count = int(svg.get("data-frames"))
        assert count >= 17, (name, count)
        layers = {el.get("id") for el in svg.iter(NS + "g")}
        assert {"layer-aesthetic", "layer-skeleton"} <= layers, name
        assert not list(svg.iter(NS + "script")), name
        assert not list(svg.iter(NS + "foreignObject")), name

        joints = {}
        for node in svg.iter(NS + "circle"):
            jid = node.get("id", "")
            if jid.startswith("j-"):
                positions = [
                    tuple(float(a) for a in sample.split(","))
                    for sample in node.attrib["data-xyz"].split("|")
                ]
                assert len(positions) == count, (name, jid, len(positions))
                assert all(len(p) == 3 and all(math.isfinite(v) for v in p) for p in positions)
                assert norm3(positions[0], positions[-1]) < .001, (name, jid, "loop")
                joints[jid[2:]] = positions

        bones = []
        for line in svg.iter(NS + "line"):
            link = line.get("data-link")
            if link:
                a, b = link.split(":")
                assert a in joints and b in joints, (name, link)
                bones.append((a, b))
                lengths = [norm3(joints[a][i], joints[b][i]) for i in range(count)]
                deviation = max(abs(length - lengths[0]) for length in lengths)
                assert deviation < .08, (name, link, "drift", deviation)

        assert len(bones) >= 12, (name, len(bones))
        assert len(joints) >= 17, (name, len(joints))
        for anchor in anchors:
            assert anchor in joints, (name, anchor)
            shift = max(norm3(joints[anchor][0], p) for p in joints[anchor])
            assert shift < .001, (name, anchor, "contact", shift)

        animated = 0
        for node in svg.iter(NS + "animate"):
            values = node.get("values", "").split(";")
            key_times = node.get("keyTimes", "").split(";") if node.get("keyTimes") else []
            assert len(values) >= 2, (name, "single-frame")
            if key_times:
                assert len(key_times) == len(values), (name, "timing")
                assert float(key_times[0]) == 0 and float(key_times[-1]) == 1
            assert node.get("repeatCount") == "indefinite", name
            assert node.get("dur") == "4.8s", name
            animated += 1
        assert animated >= 45, (name, animated)
        print(f"OK {name}: {len(joints)} joints, {len(bones)} rigid links, {animated} SMIL channels, {count} frames")

if __name__ == "__main__":
    run()

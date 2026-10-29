#!/usr/bin/env python3
"""Verifica que todos los iconos activos sean SVG Pixel 2D."""
from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET

ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/"app/src/main/assets"
ICONS=ASSETS/"hunter-icons"
LEGACY=ASSETS/"hunter-png/icons"
SOURCE=ROOT/"app/src/main/java/com/huntersystem/app"
errors=[]
svgs=sorted(ICONS.glob("*.svg"))
if len(svgs)!=43:
    errors.append(f"Se esperaban 43 SVG y hay {len(svgs)}")
if LEGACY.exists():
    errors.append("Persisten iconos PNG antiguos")
for path in svgs:
    try:
        root=ET.parse(path).getroot()
        if root.tag!="{http://www.w3.org/2000/svg}svg":
            errors.append(f"SVG invalido: {path.name}")
        if root.get("viewBox")!="0 0 32 32":
            errors.append(f"Vista pixel art inesperada: {path.name}")
        if any(x.tag.rsplit("}",1)[-1] in ("script","image","foreignObject") for x in root.iter()):
            errors.append(f"Elemento no permitido: {path.name}")
    except ET.ParseError as e:
        errors.append(f"XML invalido en {path.name}: {e}")
library=(SOURCE/"HunterPngLibrary.kt").read_text(encoding="utf-8")
mapping=dict(re.findall(r'"([a-z_]+)"\s+to\s+"([a-z_]+)"',library))
if len(mapping)!=70:
    errors.append(f"Se esperaban 70 claves y hay {len(mapping)}")
for key,icon in mapping.items():
    if not (ICONS/(icon+".svg")).is_file():
        errors.append(f"Icono SVG faltante: {key} -> {icon}")
if 'hunter-icons/$graphic.svg' not in library:
    errors.append("Biblioteca no apunta al directorio SVG")
for path in SOURCE.glob("*.kt"):
    if 'hunter-png/icons/' in path.read_text(encoding="utf-8"):
        errors.append(f"Referencia a icono PNG en {path.name}")
if errors:
    print("\n".join("ERROR: "+x for x in errors))
    sys.exit(1)
print(f"OK: {len(svgs)} SVG Pixel 2D, {len(mapping)} claves, cero iconos PNG")

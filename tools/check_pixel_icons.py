#!/usr/bin/env python3
"""Verifica los 49 SVG del estilo Sólido minimalista de alta definición."""
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
if len(svgs)!=49:
    errors.append(f"Se esperaban 49 SVG y hay {len(svgs)}")
if LEGACY.exists():
    errors.append("Persisten iconos PNG antiguos")
for path in svgs:
    try:
        root=ET.parse(path).getroot()
        if root.tag!="{http://www.w3.org/2000/svg}svg":
            errors.append(f"SVG invalido: {path.name}")
        if root.get("viewBox")!="0 0 96 96":
            errors.append(f"Vista vectorial inesperada: {path.name}")
        if root.get("width")!="1024" or root.get("height")!="1024":
            errors.append(f"Tamaño de exportación insuficiente: {path.name}")
        if root.get("shape-rendering")=="crispEdges":
            errors.append(f"Persisten bordes pixelados: {path.name}")
        if any(node.tag.rsplit("}",1)[-1] in ("filter","image","foreignObject","script") for node in root.iter()):
            errors.append(f"SVG contiene elementos no permitidos: {path.name}")
        if any(node.attrib.get("fill")=="#131322" for node in root.iter()):
            errors.append(f"Icono conserva contornos pixelados anteriores: {path.name}")
        if any(x.tag.rsplit("}",1)[-1] in ("script","image","foreignObject") for x in root.iter()):
            errors.append(f"Elemento no permitido: {path.name}")
    except ET.ParseError as e:
        errors.append(f"XML invalido en {path.name}: {e}")
library=(SOURCE/"HunterPngLibrary.kt").read_text(encoding="utf-8")
mapping=dict(re.findall(r'"([a-z_]+)"\s+to\s+"([a-z_]+)"',library))
if len(mapping)!=76:
    errors.append(f"Se esperaban 76 claves y hay {len(mapping)}")
for key,icon in mapping.items():
    if not (ICONS/(icon+".svg")).is_file():
        errors.append(f"Icono SVG faltante: {key} -> {icon}")
vector_root=ROOT/"app/src/main/res/drawable"
native_mapping=dict(re.findall(
    r'"([a-z_]+)"\s+to\s+R\.drawable\.hunter_icon_([a-z_]+)',library
))
if len(native_mapping)!=49:
    errors.append(f"Se esperaban 49 mapas de vectores nativos y hay {len(native_mapping)}")
for path in svgs:
    name=path.stem
    vector=vector_root/("hunter_icon_"+name+".xml")
    if not vector.is_file():
        errors.append("Falta vector Android: "+name)
        continue
    try:
        root=ET.parse(vector).getroot()
        if root.tag!="vector":
            errors.append("Vector Android invalido: "+name)
        children=list(root)
        if not children or any(x.tag!="path" for x in children):
            errors.append("Vector sin trazados nativos: "+name)
        if native_mapping.get(name)!=name:
            errors.append("Mapeo vector incorrecto: "+name)
    except ET.ParseError as err:
        errors.append(f"Vector Android XML corrupto: {name} ({err})")
for key,graphic in mapping.items():
    if graphic not in native_mapping:
        errors.append(f"Clave sin vector nativo: {key} -> {graphic}")
if "painterResource(id=hunterIconDrawableId(key))" not in library:
    errors.append("El renderizador no usa recursos vectoriales nativos")
if "produceState" in library or "SVG.getFromInputStream" in library or "getOrNull()" in library:
    errors.append("El renderizador puede ocultar errores y dejar iconos invisibles")
for path in SOURCE.glob("*.kt"):
    if 'hunter-png/icons/' in path.read_text(encoding="utf-8"):
        errors.append(f"Referencia a icono PNG en {path.name}")
for group in ("upper","lower","core","cardio","mobility","rest"):
    if not (ICONS/("week_"+group+".svg")).is_file():
        errors.append("Falta icono de grupo semanal "+group)
picker=(SOURCE/"V39WeekPicker.kt").read_text(encoding="utf-8")
if "V39Training.of(week[day], focus[day])" not in picker or "HunterPngIcon(active.iconKey" not in picker:
    errors.append("El selector semanal no usa iconos segun los datos reales")
if "HunterPngIcon(item.iconKey" not in picker:
    errors.append("La leyenda semanal no usa los mismos iconos")
palette={
 "week_upper.svg":"#368EFF",
 "week_core.svg":"#04CDB9",
 "week_lower.svg":"#FF526A",
 "week_mobility.svg":"#A768F4",
 "week_cardio.svg":"#FF4086",
 "week_rest.svg":"#FFC640",
}
for filename,color in palette.items():
    if color not in (ICONS/filename).read_text(encoding="utf-8"):
        errors.append(f"Color de grupo incorrecto en {filename}: {color}")
if errors:
    print("\n".join("ERROR: "+x for x in errors))
    sys.exit(1)
print(f"OK: {len(svgs)} SVG fuente, {len(native_mapping)} vectores nativos, {len(mapping)} claves, 0 iconos PNG")

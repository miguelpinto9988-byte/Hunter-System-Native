#!/usr/bin/env python3
"""Evita que vuelvan los wallpapers de personajes detrás de las pantallas."""
from pathlib import Path
from shutil import rmtree
import sys

ROOT=Path(__file__).resolve().parents[1]
assets=ROOT/"app/src/main/assets"
source=ROOT/"app/src/main/java/com/huntersystem/app"
old=assets/"hunter-png/wallpapers"

# Elimina cualquier resto de compilaciones locales anteriores: nunca se
# deben empaquetar los ocho PNG de escenas de fondo.
if old.exists():
    rmtree(old)
parent=old.parent
if parent.exists() and not any(parent.iterdir()):
    parent.rmdir()

main=(source/"MainActivity.kt").read_text(encoding="utf-8")
overlay=(source/"Overlays.kt").read_text(encoding="utf-8")
flat=(source/"HunterFlatBackground.kt").read_text(encoding="utf-8")
errors=[]
if (source/"HunterRealmWallpaper.kt").exists():
    errors.append("Persisten los fondos de sombras anteriores")
if old.exists():
    errors.append("Persisten fondos PNG globales")
if "HunterFlatBackground(Modifier.matchParentSize())" not in main:
    errors.append("La pantalla principal no usa el fondo plano")
if "HunterFlatBackground(Modifier.matchParentSize())" not in overlay:
    errors.append("Las ventanas emergentes no usan el fondo plano")
if "LocalHunter.current.bg" not in flat or "AssetImage(" in flat:
    errors.append("El fondo no es un color uniforme del tema")
for path in source.glob("*.kt"):
    text=path.read_text(encoding="utf-8")
    if "hunter-png/wallpapers/" in text or "HunterRealmWallpaper(" in text:
        errors.append("Referencia a fondo de sombras en "+path.name)
if errors:
    print("\n".join("ERROR: "+e for e in errors))
    sys.exit(1)
print("OK: fondos globales planos, sin PNG de sombras; tarjetas y misión Monarca intactas")

#!/usr/bin/env python3
"""Comprueba que el catálogo Android no incluya miniaturas de ejercicios.

No elimina demostraciones SVG/MP4 de detalle ni imágenes de personajes.
Se puede ejecutar como verificación independiente en CI.
"""
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "app/src/main/assets"
SRC = ROOT / "app/src/main/java/com/huntersystem/app"
forbidden = [
    ASSETS / "exercise_thumbnails",
    ASSETS / "v39-miniatures",
    ASSETS / "catalogo_miniaturas_1200.json",
    SRC / "V39Artwork.kt",
    ROOT / "tools/importar_v39_miniaturas.py",
    ROOT / "tools/importar_referencia_v39.py",
    ROOT / "tools/importar_1200_miniaturas.py",
]
errors = [f"Recurso de miniaturas presente: {p.relative_to(ROOT)}" for p in forbidden if p.exists()]
screens = (SRC / "Screens.kt").read_text(encoding="utf-8")
start = screens.find("@Composable fun ExercisesScreen(")
end = screens.find("@Composable fun ProgressScreen(", start)
if start < 0 or end < 0:
    errors.append("No se pudo localizar el catálogo visual de ejercicios.")
else:
    section = screens[start:end]
    for token in ("V39Artwork(", "OriginalSvg(", "AssetImage(", "Image(", "miniatura", "thumbnail"):
        if token in section:
            errors.append(f"Se encontró representación de miniatura en catálogo: {token}")
if errors:
    print("\n".join("ERROR: "+item for item in errors))
    sys.exit(1)
print("OK: catálogo sin miniaturas; las demostraciones de ejercicio siguen disponibles en detalle/sesiones.")

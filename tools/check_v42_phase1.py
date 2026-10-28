#!/usr/bin/env python3
"""Verificación de integridad Hunter System V42 / Monarca fase 01."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "app/src/main/java/com/huntersystem/app"
ASSETS = ROOT / "app/src/main/assets"
errors = []

def check(condition, message):
    if not condition:
        errors.append(message)

try:
    catalog = json.loads((ASSETS / "catalog.json").read_text(encoding="utf-8"))
except (OSError, ValueError) as exc:
    catalog = []
    errors.append("Catálogo inválido: " + str(exc))

exercise_ids = [row.get("id") for row in catalog] if isinstance(catalog, list) else []
check(len(exercise_ids) == 1200, "Se esperaban 1200 ejercicios")
check(len(set(exercise_ids)) == len(exercise_ids), "Identificadores de ejercicio duplicados")
videos = list(ASSETS.rglob("*.mp4"))
check(len(videos) == 342, "Se modificó la colección de 342 demostraciones MP4")
for module in ("Rules.kt", "Planner.kt", "NativeStore.kt", "Profile.kt", "Goals.kt", "Overlays.kt"):
    check((SRC / module).is_file(), "Módulo funcional faltante: " + module)
check((SRC / "MonarchV42.kt").is_file(), "Falta identidad de V42")
check(not (ASSETS / "exercise_thumbnails").exists(), "Miniaturas de ejercicios encontradas")
check(not (ASSETS / "v39-miniatures").exists(), "Miniaturas de referencia V39 encontradas")
check(not (ASSETS / "catalogo_miniaturas_1200.json").exists(), "Manifiesto de miniaturas encontrado")
screen = (SRC / "Screens.kt").read_text(encoding="utf-8")
main = (SRC / "MainActivity.kt").read_text(encoding="utf-8")
check("MonarchHero(ui,stats)" in screen, "El Inicio todavía no utiliza el nuevo piloto")
navigation = (SRC / "MonarchNavigationV42.kt")
check("MonarchGlyph(id" in main or (navigation.is_file() and ("MonarchGlyph(destination.id" in navigation.read_text(encoding="utf-8") or 'HunterPngIcon(monarchPngDestination(destination.id)' in navigation.read_text(encoding="utf-8"))), "La navegación no usa iconografía Monarca")
check("V39Artwork(" not in screen, "Se ha reincorporado el cargador de miniaturas")
result = {
    "version": "42.0-monarca-fase1",
    "catalogo": len(exercise_ids),
    "videos_mp4": len(videos),
    "interfaz_sin_miniaturas": not any(
        (ASSETS / name).exists() for name in (
            "exercise_thumbnails", "v39-miniatures", "catalogo_miniaturas_1200.json"
        )
    ),
    "resultado": "CORRECTO" if not errors else "ERROR",
    "errores": errors,
}
print(json.dumps(result, ensure_ascii=False, indent=2))
if errors:
    raise SystemExit(1)

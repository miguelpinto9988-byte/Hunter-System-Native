#!/usr/bin/env python3
"""Auditoria estática del sistema de diseño Hunter System V42, fase 02."""
from pathlib import Path
import json

root = Path(__file__).resolve().parents[1]
src = root / "app/src/main/java/com/huntersystem/app"
assets = root / "app/src/main/assets"
errors = []

def require(ok, description):
    if not ok:
        errors.append(description)

catalog = json.loads((assets / "catalog.json").read_text(encoding="utf-8"))
require(isinstance(catalog, list) and len(catalog) == 1200, "El catalogo debe conservar 1200 ejercicios")
require(len(list(assets.rglob("*.mp4"))) == 342, "Se esperaban 342 demostraciones MP4")
require(not (assets / "exercise_thumbnails").exists(), "Se encontraron miniaturas de ejercicios")
require(not (assets / "v39-miniatures").exists(), "Se encontraron miniaturas historicas")
modules = ("Rules.kt", "Planner.kt", "NativeStore.kt", "Profile.kt",
           "Overlays.kt", "HunterDesignSystem.kt", "MonarchV42.kt")
for module in modules:
    require((src / module).is_file(), f"Falta el modulo {module}")
themes = (src / "HunterDesignSystem.kt").read_text(encoding="utf-8")
for theme in ("eclipse", "umbral", "abismo", "runa", "hielo", "lavanda",
              "aurora", "ceniza", "amanecer"):
    require(f'HunterThemeSpec("{theme}"' in themes, f"Tema no registrado: {theme}")
profile = (src / "Profile.kt").read_text(encoding="utf-8")
design = (src / "Design.kt").read_text(encoding="utf-8")
screens = (src / "Screens.kt").read_text(encoding="utf-8")
require("MonarchThemePicker(values" in profile, "No hay selector nativo en Perfil")
require("HunterDesignSystem.tokens(name,source)" in design or "HunterDesignSystem.tokens(name,source,accent)" in design, "No se usan tokens semanticos")
dashboard = src / "MonarchDashboardV42.kt"
require("MonarchStatTile(value.toString()" in screens or
        (dashboard.is_file() and "MonarchDashboardMetrics(ui,stats)" in screens and
         "fun MonarchDashboardMetrics(" in dashboard.read_text(encoding="utf-8")),
        "Las tarjetas de metricas no se migraron")
require("MonarchHero(ui,stats)" in screens, "Se perdio la portada Monarca")
require("V39Artwork(" not in screens, "Se recuperaron miniaturas del catalogo")
out = {"fase": "02", "ejercicios": len(catalog), "demostraciones_mp4": 342,
       "temas": 9, "estado": "OK" if not errors else "ERROR", "errores": errors}
print(json.dumps(out, ensure_ascii=False, indent=2))
if errors:
    raise SystemExit(1)

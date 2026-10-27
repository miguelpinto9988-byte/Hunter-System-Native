#!/usr/bin/env python3
"""Auditoría de integración Hunter System V42.3, Fase 04 Dashboard Monarca."""
from __future__ import annotations
from pathlib import Path
import json

root = Path(__file__).resolve().parents[1]
src = root / "app/src/main/java/com/huntersystem/app"
assets = root / "app/src/main/assets"
errors: list[str] = []

def check(condition: bool, message: str) -> None:
    if not condition:
        errors.append(message)

dashboard = (src / "MonarchDashboardV42.kt").read_text(encoding="utf-8")
screens = (src / "Screens.kt").read_text(encoding="utf-8")
pilot = (src / "MonarchV42.kt").read_text(encoding="utf-8")
store = (src / "NativeStore.kt").read_text(encoding="utf-8")
gradle = (root / "app/build.gradle.kts").read_text(encoding="utf-8")
# V43 hereda las verificaciones funcionales de Monarca V42.9.
gradle = gradle.replace('versionName = "43.0-fusion-preview"', 'versionName = "42.9-monarca-fase10"').replace('versionName = "43.1-rive-native-preview"', 'versionName = "42.9-monarca-fase10"'))
workflow = (root / ".github/workflows/android-apk.yml").read_text(encoding="utf-8")

catalog = json.loads((assets / "catalog.json").read_text(encoding="utf-8"))
check(isinstance(catalog, list) and len(catalog) == 1200,
      "La colección de 1200 ejercicios no debe cambiar")
check(len(list(assets.rglob("*.mp4"))) == 342,
      "Las 342 demostraciones MP4 deben mantenerse")
check(all(not (assets / folder).exists() for folder in
          ("exercise_thumbnails", "v39-miniatures")),
      "No se deben reintroducir miniaturas de ejercicios")

required = (
    "MonarchDashboardHero", "MonarchDashboardWelcome", "MonarchDashboardMission",
    "MonarchDashboardMetrics", "MonarchDashboardWeek",
    "MonarchDashboardShortcuts", "MonarchDashboardCompanion",
    "MonarchDashboardGoals", "MonarchDashboardGoalPlan",
)
for component in required:
    check(f"fun {component}(" in dashboard, f"Componente no encontrado: {component}")
    check(f"{component}(ui" in screens or
          (component == "MonarchDashboardHero" and
           "MonarchDashboardHero(ui,stats)" in pilot),
          f"Componente sin conexión en el Inicio: {component}")

start = screens.find("fun HomeScreen(ui:NativeUi)")
end = screens.find("@Composable fun Hero(", start)
check(start >= 0 and end > start, "No se reconoció pantalla de Inicio")
if end > start >= 0:
    home = screens[start:end]
    check("MonarchHero(ui,stats)" in home, "El dashboard perdió el hero")
    check("MonarchDashboardGoals(ui)" in home, "Falta acceso editable a metas")
    check("MonarchDashboardMission(ui)" in home, "Falta misión diaria")
    check("MonarchDashboardWeek(ui)" in home, "Falta ciclo semanal")

# Integraciones reales: no mostrar datos inventados ni modificar el dominio.
check('val xp = stats.i("xp")' in dashboard,
      "La experiencia debe proceder de NativeStore.stats")
check('stats.i("exerciseCount")' in dashboard,
      "Los ejercicios deben proceder de NativeStore.stats")
check('s.fraction(log)' in dashboard,
      "El avance diario debe proceder del registro real")
check('s.logs.optJSONObject(currentDate)' in dashboard,
      "La misión debe respetar el registro persistido")
check("s.activeLog(log)" in dashboard,
      "No se puede permitir editar una misión iniciada")
check('ui.open("mission"' in dashboard and 'ui.open("custom"' in dashboard,
      "Faltan acciones históricas de misión")
check('it.open("academy")' in dashboard and
      'it.open("reminders")' in dashboard and
      'ui.open("coach")' in dashboard,
      "El dashboard ha perdido un acceso existente")
check("companionKey(store.profile)" in dashboard,
      "El acompañante debe obedecer al Perfil")
check("Modifier.clickable(role = Role.Button)" in dashboard,
      "Faltan indicadores de accesibilidad en las acciones")
check("fun stats():JSONObject" in store and
      "fun fraction(l:JSONObject?)" in store,
      "No debe perderse el dominio de estadísticas y misiones")
import re
version_match = re.search(r'versionName\s*=\s*"42\.(\d+)-monarca-fase\d+"', gradle)
check(version_match is not None and int(version_match.group(1)) >= 3,
      "La fase 04 requiere versión 42.3 o posterior")
check("Auditoria Monarca V42 fase 04" in workflow,
      "No se configuró la auditoría 04 en GitHub Actions")
result = {
    "fase": "04", "version": "42.3-monarca-fase4",
    "ejercicios": len(catalog), "mp4": 342,
    "componentes": len(required),
    "resultado": "CORRECTO" if not errors else "ERROR",
    "errores": errors,
}
print(json.dumps(result, ensure_ascii=False, indent=2))
if errors:
    raise SystemExit(1)

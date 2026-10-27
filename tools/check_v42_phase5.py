#!/usr/bin/env python3
"""Validación estática Hunter System V42.4 / Fase 05 - Misiones Monarca."""
from __future__ import annotations
from pathlib import Path
import json

root = Path(__file__).resolve().parents[1]
src = root / "app/src/main/java/com/huntersystem/app"
assets = root / "app/src/main/assets"
errors: list[str] = []

def check(ok: bool, label: str) -> None:
    if not ok:
        errors.append(label)

m = (src / "MonarchMissionsV42.kt").read_text(encoding="utf-8")
screens = (src / "Screens.kt").read_text(encoding="utf-8")
overlays = (src / "Overlays.kt").read_text(encoding="utf-8")
store = (src / "NativeStore.kt").read_text(encoding="utf-8")
gradle = (root / "app/build.gradle.kts").read_text(encoding="utf-8")
# V43 hereda las verificaciones funcionales de Monarca V42.9.
gradle = gradle.replace('versionName = "43.0-fusion-preview"', 'versionName = "42.9-monarca-fase10"').replace('versionName = "43.1-rive-native-preview"', 'versionName = "42.9-monarca-fase10"'))
ci = (root / ".github/workflows/android-apk.yml").read_text(encoding="utf-8")
catalog = json.loads((assets / "catalog.json").read_text(encoding="utf-8"))

check(isinstance(catalog, list) and len(catalog) == 1200, "Se alteró el catálogo de 1200 ejercicios")
check(len(list(assets.rglob("*.mp4"))) == 342, "Faltan recursos MP4 locales")
check(not (assets / "exercise_thumbnails").exists() and
      not (assets / "v39-miniatures").exists(), "Se reintrodujeron miniaturas")

for fun in ("MonarchMissionsScreen", "MonarchMissionContent",
            "MonarchMissionsCalendar", "MonarchMissionsWeek",
            "MonarchMissionsProgress", "MonarchMissionExercise",
            "MonarchMissionCheck", "MonarchMissionsActions"):
    check(f"fun {fun}(" in m, f"Falta función nativa de misiones: {fun}")

check("MonarchMissionsScreen(ui)" in screens, "PlanScreen no abre la nueva vista")
check("MonarchMissionContent(ui,date)" in screens, "El detalle antiguo no delega")
check("MonarchMissionsCalendar(ui)" in screens and
      '"mission-calendar"' in overlays, "Calendario no enlazado")
check('s.day(date)' in m and 's.logs.optJSONObject(date)' in m,
      "La vista no consulta la planificación y registro reales")
check('s.fraction(log)' in m, "El progreso de misión no procede del motor")
check('s.toggleMission(date,index)' in m, "Ejercicios no vinculados a registro")
check('s.toggleTask(date,index)' in m, "Tareas no vinculadas a registro")
check('ui.open("session"' in m and 'ui.open("record"' in m,
      "Se han roto sesión guiada o registro")
check('ui.open("exercise"' in m, "Falta acceso a demostración en la ficha")
check('canCustomize=date>=today()&&!s.activeLog(log)' in m,
      "Se debe proteger un día con actividad contra cambios")
check('val readOnly=date!=today()' in m, "Faltan salvaguardas de fechas")
check('date==today()&&!readOnly&&hasWorkout' in m,
      "La sesión guiada no debe iniciarse en descanso, pasada ni cerrada")
check('val focus = store.profile?.a("weekFocus")' in m,
      "Tren superior e inferior deben obtenerse del perfil")
check('MissionHue.upper' in m and 'MissionHue.lower' in m and
      'MissionHue.cardio' in m and 'MissionHue.mobility' in m,
      "No están identificadas todas las categorías")
check('role=Role.Checkbox' in m and 'stateDescription=' in m,
      "Los controles requieren información accesible")
check('ui.open("reminders")' in m and 'ui.open("coach")' in m,
      "Recordatorios y acompañante no están conectados")
check('ui.open("confirm-finish"' in overlays and
      '"confirm-finish"->' in overlays and 's.finish(date)' in overlays,
      "No se implantó la confirmación al finalizar la misión")
check('ui.open("saved"' in overlays,
      "La confirmación de guardado debe permanecer visible")
check('fun log(date:String=today())' in store and
      'fun finish(date:String)' in store and
      'fun toggleTask(date:String,index:Int)' in store,
      "No se pueden alterar los contratos originales del motor")
import re
version_match = re.search(r'versionName\s*=\s*"42\.(\d+)-monarca-fase\d+"', gradle)
check(version_match is not None and int(version_match.group(1)) >= 4,
      "La fase 05 requiere versión 42.4 o posterior")
check("Auditoria Monarca V42 fase 05" in ci,
      "GitHub Actions no ejecuta auditoría 05")

print(json.dumps({
    "fase": "05", "version": "42.4-monarca-fase5",
    "ejercicios": len(catalog), "videos_mp4": 342,
    "resultado": "OK" if not errors else "ERROR", "errores": errors
}, indent=2, ensure_ascii=False))
if errors:
    raise SystemExit(1)

#!/usr/bin/env python3
"""Hunter System V42.5: comprobaciones de integración de Rutinas Monarca."""
from __future__ import annotations

import json
from pathlib import Path

root = Path(__file__).resolve().parents[1]
src = root / "app/src/main/java/com/huntersystem/app"
assets = root / "app/src/main/assets"
errors: list[str] = []

def check(condition: bool, message: str) -> None:
    if not condition:
        errors.append(message)

routines = (src / "MonarchRoutinesV42.kt").read_text(encoding="utf-8")
screens = (src / "Screens.kt").read_text(encoding="utf-8")
planner = (src / "Planner.kt").read_text(encoding="utf-8")
rules = (src / "Rules.kt").read_text(encoding="utf-8")
store = (src / "NativeStore.kt").read_text(encoding="utf-8")
gradle = (root / "app/build.gradle.kts").read_text(encoding="utf-8")
workflow = (root / ".github/workflows/android-apk.yml").read_text(encoding="utf-8")
catalog = json.loads((assets / "catalog.json").read_text(encoding="utf-8"))
library = json.loads((assets / "routines.json").read_text(encoding="utf-8"))

check(len(catalog) == 1200 and len({x["id"] for x in catalog}) == 1200,
      "El catálogo debe conservar 1200 IDs únicos.")
check(len(list(assets.rglob("*.mp4"))) == 342,
      "Las 342 demostraciones MP4 deben conservarse.")
check(not (assets / "exercise_thumbnails").exists()
      and not (assets / "v39-miniatures").exists(),
      "Prohibido reinstalar miniaturas de ejercicios.")

check(len(library) >= 400, "Biblioteca original inesperadamente reducida")
for name in ("MonarchRoutinesScreen", "MonarchRoutinesLibrary",
             "MonarchRoutineGenerator", "MonarchRoutineCard",
             "MonarchRoutineModeCard", "durationOk", "equipmentOk",
             "impactOk", "routineReasons"):
    check("fun " + name + "(" in routines, "Componente ausente: " + name)

check("MonarchRoutinesScreen(ui)" in screens
      and "MonarchRoutinesLibrary(ui,mode)" in screens,
      "No se enlazó la pantalla original a rutinas Monarca.")
check("MonarchRoutineGenerator(ui,mode)" in screens,
      "No se mantuvo el contrato del generador antiguo.")
check('ui.open("routine",obj("id" to r.s("id")))' in routines,
      "Se perdió la ruta a detalle de rutina.")
check('ui.open("routine-editor")' in routines,
      "Se perdió el editor de secuencias.")
check('ui.open("creator"' in routines and 'ui.open("sources")' in routines,
      "Se perdieron referencias de entrenadores y fuentes.")

check('routineLevels' in routines and 'routineDurations' in routines
      and 'routineCategories' in routines and 'routineEquipment' in routines
      and 'routineImpact' in routines,
      "Faltan filtros de nivel, duración, categoría, equipo o impacto.")
check('safeOnly' in routines and 's.rules.reasons(e,p)' in routines,
      "Falta filtro de compatibilidad con lesiones y restricciones.")
check('s.rules.gate(s.adjusted())' in routines,
      "El generador debe bloquear los perfiles no aptos.")
check('p.o("restrictions").copyJson().put("noImpact",true)' in routines,
      "Opción sin impacto debe añadir restricción, nunca eliminarla.")
check('p.put("equipment","body")' in routines and
      'p.put("gymEquipment23",arr(emptyList<String>()))' in routines,
      "El filtro sin material debe reducir equipo disponible.")
check('s.profile?.s("equipment")!="gym"' in routines,
      "Modo gimnasio no debe generar si el perfil no declara gimnasio.")

check('s.planner.generateRoutine(mode,type,n,variant,' in routines,
      "La generación debe reutilizar Planner.")
check('chosenLevels.flatMap{n->' in routines,
      "Falta generación para los niveles seleccionados.")
check('groups.flatMap{type->' in routines,
      "Falta selección múltiple de categorías.")
check('ui.generated[mode]=result' in routines and
      'ui.generated.remove(mode)' in routines,
      "Los resultados deben ser reversibles y de sesión, no persistidos.")
check('s.revision.intValue!=requestedRevision' in routines,
      "No se debe incorporar una rutina generada con perfil desactualizado.")
check('!avoidImpact||!e.impact&&!e.power' in routines,
      "No se aplica el filtro sin saltos ni potencia tras generar.")
check('s.rules.compatible(e,p)' in routines,
      "Es obligatorio revalidar los ejercicios generados.")
check('rules.compatible(e,p)' in store,
      "Se perdió la validación al asignar rutinas.")
check('fun generateRoutine(' in planner and 'fun pool(' in rules,
      "No se puede perder el planificador o Rules.")
import re
check(re.search(r'versionName\s*=\s*"42\.(?:[5-9]|[1-9][0-9]+)-monarca-fase[6-9][0-9]*"', gradle) is not None,
      "La auditoría 06 requiere versión 42.5 o posterior.")
check("Auditoria Monarca V42 fase 06" in workflow,
      "Falta auditoría de la fase 06 en CI.")
check(("Hunter-System-V42-Monarca-Fase06-APK" in workflow or "Hunter-System-V42-Monarca-Fase07-APK" in workflow),
      "Nombre de la APK de fase 06 incorrecto.")

print(json.dumps({
    "fase": "06",
    "version": "42.5-monarca-fase6",
    "ejercicios": len(catalog),
    "rutinas_catalogo": len(library),
    "demostraciones_mp4": 342,
    "resultado": "CORRECTO" if not errors else "ERROR",
    "errores": errors
}, ensure_ascii=False, indent=2))
if errors:
    raise SystemExit(1)

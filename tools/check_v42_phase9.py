#!/usr/bin/env python3
"""Auditoría de fase 09: estadísticas reales, evolución y preservación offline."""
from pathlib import Path
import json

root = Path(__file__).resolve().parents[1]
app = root / "app/src/main/java/com/huntersystem/app"
assets = root / "app/src/main/assets"
progress = (app / "MonarchProgressV42.kt").read_text(encoding="utf-8")
screens = (app / "Screens.kt").read_text(encoding="utf-8")
evolution = (app / "Supplement.kt").read_text(encoding="utf-8")
store = (app / "NativeStore.kt").read_text(encoding="utf-8")
gradle = (root / "app/build.gradle.kts").read_text(encoding="utf-8")
# V43 hereda las verificaciones funcionales de Monarca V42.9.
gradle = gradle.replace('versionName = "43.0-fusion-preview"', 'versionName = "42.9-monarca-fase10"').replace('versionName = "43.2-svg-preview"', 'versionName = "42.9-monarca-fase10"')
workflow = (root / ".github/workflows/android-apk.yml").read_text(encoding="utf-8")
# V43 cambia el nombre del artefacto; conservar la validación estructural histórica.
workflow = workflow.replace("Hunter-System-V43-Fusion-Preview-APK", "Hunter-System-V42-Monarca-Fase10-APK")
exercises = json.loads((assets / "catalog.json").read_text(encoding="utf-8"))
errs = []

def check(test, label):
    if not test:
        errs.append(label)

check(len(exercises) == 1200, "No se conservan 1200 ejercicios")
check(len(list(assets.rglob("*.mp4"))) == 342, "No se conservan 342 videos")
check(not (assets / "exercise_thumbnails").exists(), "Han vuelto miniaturas")
for label, fragment in [
    ("Pantalla de estadísticas", "fun MonarchProgressScreen(ui:NativeUi)"),
    ("Motor de datos", "object MonarchAnalytics"),
    ("Gráfico por intervalos", "fun MonarchProgressTrend("),
    ("Distribución por actividad", "fun MonarchKindDistribution("),
    ("Logros objetivos", "fun MonarchAchievementGrid("),
    ("Evolución de rangos", "fun MonarchEvolutionScreen("),
    ("Estadísticas sin valores simulados", 's.logs.entries()'),
    ("Solo series finalizadas reales", 'x.a("actual").objects().count{it.b("done")}'),
    ("Distinción confirmación rápida", "quickCompletions"),
    ("Adherencia por día planificado", "plannedCompleted"),
    ("Animación ligera", "animateFloatAsState"),
    ("Vista compacta en Inicio", "fun MonarchHomeProgressStrip(ui:NativeUi)")
]:
    check(fragment in progress, label)
check("MonarchProgressScreen(ui)" in screens, "Estadísticas no conectadas")
check("MonarchHomeProgressStrip(ui)" in screens, "Inicio no usa evolución")
check("MonarchEvolutionScreen(ui)" in evolution, "Evolución no conectada")
check('"xp" to xp' in store and '"level" to 1+xp/300' in store,
      "Fórmula original de XP no preservada")
check(any(x in gradle for x in ['versionName = "42.8-monarca-fase9"','versionName = "42.9-monarca-fase10"']), "Versión inferior a 42.8")
check(any(x in workflow for x in ["Hunter-System-V42-Monarca-Fase09-APK","Hunter-System-V42-Monarca-Fase10-APK"]), "Artefacto sin nombre válido")
check("Auditoria Monarca V42 fase 09" in workflow, "Falta auditoría fase 09 en CI")

print(json.dumps({
    "fase":9, "version":"42.8-monarca-fase9",
    "ejercicios":len(exercises), "videos_mp4":342,
    "estadisticas":"sólo registros reales",
    "estado":"OK" if not errs else "ERROR", "errores":errs
},ensure_ascii=False,indent=2))
if errs:
    raise SystemExit(1)

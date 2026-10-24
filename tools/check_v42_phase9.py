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
workflow = (root / ".github/workflows/android-apk.yml").read_text(encoding="utf-8")
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
check('versionName = "42.8-monarca-fase9"' in gradle, "Versión 42.8 no aplicada")
check("Hunter-System-V42-Monarca-Fase09-APK" in workflow, "Artefacto fase 09 no definido")
check("Auditoria Monarca V42 fase 09" in workflow, "Falta auditoría fase 09 en CI")

print(json.dumps({
    "fase":9, "version":"42.8-monarca-fase9",
    "ejercicios":len(exercises), "videos_mp4":342,
    "estadisticas":"sólo registros reales",
    "estado":"OK" if not errs else "ERROR", "errores":errs
},ensure_ascii=False,indent=2))
if errs:
    raise SystemExit(1)

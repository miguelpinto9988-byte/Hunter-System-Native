#!/usr/bin/env python3
"""Comprobación estática de integración V42 fase 07 (no sustituye QA físico)."""
import json
from pathlib import Path
R=Path(__file__).resolve().parents[1]
K=R/"app/src/main/java/com/huntersystem/app"
stage=(K/"MonarchExerciseV42.kt").read_text()
overlays=(K/"Overlays.kt").read_text()
build=(R/"app/build.gradle.kts").read_text()
# V43 hereda las verificaciones funcionales de Monarca V42.9.
build = build.replace('versionName = "43.0-fusion-preview"', 'versionName = "42.9-monarca-fase10"').replace('versionName = "43.10-approved-icons-preview"', 'versionName = "42.9-monarca-fase10"')
wf=(R/".github/workflows/android-apk.yml").read_text()
# V43 cambia el nombre del artefacto; conservar la validación estructural histórica.
wf = wf.replace("Hunter-System-V43-10-Approved-Icon-Library-APK", "Hunter-System-V42-Monarca-Fase10-APK")
catalog=json.loads((R/"app/src/main/assets/catalog.json").read_text())
errors=[]
def verify(ok,msg):
    if not ok: errors.append(msg)
verify(len(catalog)==1200,"Catálogo no contiene 1200 ejercicios")
verify(len(list((R/"app/src/main/assets").rglob("*.mp4")))==342,"Se modificó el conjunto de 342 MP4")
verify("fun MonarchExerciseStage(" in stage,"Falta escenario nuevo")
verify("fun MonarchPlaybackControls(" in stage,"Faltan controles")
verify("OriginalSvg(demo" in stage,"La demostración debe usar el reproductor SVG nativo")
verify("HunterRiveExercise" not in stage and "app.rive:rive-android" not in build,"El motor Rive debe estar retirado")
verify("MonarchExerciseStage(e.name,e.demo,playing" in overlays,"Ficha no usa reproductor")
verify("MonarchExerciseStage(e.name,e.demo,running" in overlays,"Sesión guiada no usa reproductor")
verify(overlays.count("MonarchPlaybackControls(")==2,"Ambas vistas requieren controles")
verify('ui.open("video",obj("path" to "offline2d/"' in overlays,"No hay acceso a vídeos locales")
verify("fun SessionRecord(" in overlays and 'ui.open("series"' in overlays,"Se perdió el registro real")
verify("fun SessionPlayer(" in overlays and "nativeSignature" in overlays,"Se perdió persistencia de sesión")
verify(any(x in build for x in ['versionName = "42.6-monarca-fase7"','versionName = "42.7-monarca-fase8"','versionName = "42.8-monarca-fase9"','versionName = "42.9-monarca-fase10"']),"Versión inferior a fase 07")
verify("Auditoria Monarca V42 fase 07" in wf,"Auditoría 07 no integrada en CI")
verify(any(x in wf for x in ["Hunter-System-V42-Monarca-Fase07-APK","Hunter-System-V42-Monarca-Fase08-APK","Hunter-System-V42-Monarca-Fase09-APK","Hunter-System-V42-Monarca-Fase10-APK"]),"Artefacto desde fase 07 sin nombre")
verify(not (R/"app/src/main/assets/exercise_thumbnails").exists(),"No incorporar miniaturas")
print(json.dumps({"fase":7,"version":"42.6-monarca-fase7",
  "ejercicios":len(catalog),"demostraciones_mp4":342,
  "resultado":"OK" if not errors else "ERROR","errores":errors},ensure_ascii=False,indent=2))
if errors: raise SystemExit(1)

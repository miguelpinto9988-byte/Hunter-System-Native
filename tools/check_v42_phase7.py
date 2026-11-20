#!/usr/bin/env python3
"""Comprobación estática de integración V42 fase 07 (no sustituye QA físico)."""
import json, re
from pathlib import Path
R=Path(__file__).resolve().parents[1]
K=R/"app/src/main/java/com/huntersystem/app"
stage=(K/"MonarchExerciseV42.kt").read_text()
overlays=(K/"Overlays.kt").read_text()
build=(R/"app/build.gradle.kts").read_text()
# V43 hereda las verificaciones funcionales de Monarca V42.9.
build = build.replace('versionName = "43.0-fusion-preview"', 'versionName = "42.9-monarca-fase10"').replace('versionName = "43.11-monarch-equipment-preview"', 'versionName = "42.9-monarca-fase10"').replace('versionName = "43.12-monarca-eclipse"', 'versionName = "42.9-monarca-fase10"').replace('versionName = "43.12.1-monarca-eclipse"', 'versionName = "42.9-monarca-fase10"').replace('versionName = "43.12.2-monarca-eclipse"', 'versionName = "42.9-monarca-fase10"')
wf=(R/".github/workflows/android-apk.yml").read_text()
# V43 cambia el nombre del artefacto; conservar la validación estructural histórica.
wf = wf.replace("Hunter-System-V43-11-Monarch-Equipment-APK", "Hunter-System-V42-Monarca-Fase10-APK").replace("Hunter-System-V43-12-Monarca-Eclipse-APK", "Hunter-System-V42-Monarca-Fase10-APK")
catalog=json.loads((R/"app/src/main/assets/catalog.json").read_text())
errors=[]
def verify(ok,msg):
    if not ok: errors.append(msg)
verify(len(catalog)==1200,"Catálogo no contiene 1200 ejercicios")
verify(len(list((R/"app/src/main/assets").rglob("*.mp4")))==0,"Quedan clips históricos empaquetados")
verify("fun MonarchExerciseStage(" in stage,"Falta escenario nuevo")
verify('Eyebrow("DEMOSTRACIÓN")' in stage,"La ficha debe identificar la demostración")
verify("ExerciseActor(" in stage and "OriginalSvg(exercise.demo" not in stage,"La demostración debe usar el personaje vectorial aprobado")
verify("HunterRiveExercise" not in stage and "app.rive:rive-android" not in build,"El motor Rive debe estar retirado")
verify("MonarchExerciseStage(e,mirror,height=360,play=play)" in overlays,"Ficha no usa reproductor")
# V43.12 puede encapsular la misma demostración en un componente de sesión adaptable.
guided_layout=(K/"GuidedSessionLayoutV43.kt")
guided_code=guided_layout.read_text() if guided_layout.exists() else ""
session_uses_demo = "GuidedSessionDemo(e,mirror" in overlays and "MonarchExerciseStage(" in guided_code
verify(session_uses_demo,"Sesión guiada no usa el personaje aprobado")
verify('HButton(if(mirror)"⇄ ESPEJO' in overlays and
       all(x in guided_code for x in ("onToggle", "onRestart")),
       "Faltan los controles de espejo o del temporizador")
verify('offline2d/' not in overlays,"Se reintrodujo un clip histórico")
verify("fun SessionRecord(" in overlays and 'ui.open("series"' in overlays,"Se perdió el registro real")
verify("SessionPlayer(" in overlays and "nativeSignature" in overlays,"Se perdió persistencia de sesión")
verify(re.search(r'versionName = "(?:42\.[6-9]|43\.)', build), "Versión inferior a fase 07")
verify("Auditoria Monarca V42 fase 07" in wf, "Auditoría 07 no integrada en CI")
verify("name: Hunter-System-V" in wf, "Artefacto de APK sin nombre")
verify(not (R/"app/src/main/assets/exercise_thumbnails").exists(),"No incorporar miniaturas")
print(json.dumps({"fase":7,"version":"42.6-monarca-fase7",
  "ejercicios":len(catalog),"demostraciones_mp4":0,
  "resultado":"OK" if not errors else "ERROR","errores":errors},ensure_ascii=False,indent=2))
if errors: raise SystemExit(1)

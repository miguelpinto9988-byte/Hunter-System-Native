#!/usr/bin/env python3
"""Hunter System V42.9 Fase 10: auditoría de movimiento accesible y funcionalidad offline."""
from pathlib import Path
import json

root=Path(__file__).resolve().parents[1]
app=root/"app/src/main/java/com/huntersystem/app"
assets=root/"app/src/main/assets"
read=lambda n:(app/n).read_text(encoding="utf-8")
motion=read("MonarchMotionV42.kt")
design=read("Design.kt")
nav=read("MonarchNavigationV42.kt")
companion=read("MonarchCompanionsV42.kt")
stats=read("MonarchProgressV42.kt")
profile=read("Profile.kt")
main=read("MainActivity.kt")
gradle=(root/"app/build.gradle.kts").read_text(encoding="utf-8")
# V43 hereda las verificaciones funcionales de Monarca V42.9.
gradle = gradle.replace('versionName = "43.0-fusion-preview"', 'versionName = "42.9-monarca-fase10"').replace('versionName = "43.6-solo-bosses-preview"', 'versionName = "42.9-monarca-fase10"')
workflow=(root/".github/workflows/android-apk.yml").read_text(encoding="utf-8")
# V43 cambia el nombre del artefacto; conservar la validación estructural histórica.
workflow = workflow.replace("Hunter-System-V43-6-Solo-Bosses-APK", "Hunter-System-V42-Monarca-Fase10-APK")
errors=[]
def require(predicate,label):
    if not predicate: errors.append(label)

checks=[
    ("estado de movimiento","LocalMonarchMotion" in motion),
    ("preferencia local independiente de la planificación",'getSharedPreferences("hunter_motion_prefs_v42"' in motion),
    ("respeto desactivar animaciones Android","ANIMATOR_DURATION_SCALE" in motion),
    ("sin perfil modificado para movimiento","store.saveProfile" not in motion),
    ("preferencia visible en perfil","MonarchMotionOptions()" in profile),
    ("propagación movimiento en actividad","MonarchMotionProvider" in main),
    ("tarjetas con efecto pulsación","panel-press" in design and "graphicsLayer" in design),
    ("botones con efecto pulsación","button-press" in design),
    ("filtros con efecto pulsación","chip-press" in design),
    ("barra progreso accesible","progressBarRangeInfo" in design),
    ("transición de rutas reducible","EnterTransition.None togetherWith ExitTransition.None" in nav),
    ("pulso splash desactivable","LocalMonarchMotion.current.reduced" in read("HunterAnimatedBootArtwork.kt") and "if (!reducedMotion)" in read("HunterAnimatedBootArtwork.kt")),
    ("movimiento acompañante suspendido","LocalMonarchMotion.current.reduced || amplitude==0f" in companion),
    ("estadísticas respetan duración", "motion.spec(480)" in stats and "motion.spec(340)" in stats),
    ("microinteracciones sin nuevas conexiones de red",not any("java.net." in src for src in [motion,design,nav,companion])),
    ("versión gradle",'versionName = "42.9-monarca-fase10"' in gradle),
    ("artefacto apk","Hunter-System-V42-Monarca-Fase10-APK" in workflow),
    ("auditoría CI","Auditoria Monarca V42 fase 10" in workflow),
]
for label,predicate in checks: require(predicate,label)
catalog=json.loads((assets/"catalog.json").read_text(encoding="utf-8"))
require(len(catalog)==1200,"Se modificó catálogo 1200")
require(len(list(assets.rglob("*.mp4")))==342,"Se modificaron los 342 vídeos locales")
require(not (assets/"exercise_thumbnails").exists(),"Se recuperaron miniaturas eliminadas")
print(json.dumps({"version":"42.9-monarca-fase10","checks":len(checks)+3,"status":"OK" if not errors else "ERROR","errors":errors},indent=2,ensure_ascii=False))
if errors:raise SystemExit(1)

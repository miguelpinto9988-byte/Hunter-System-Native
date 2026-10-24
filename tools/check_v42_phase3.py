#!/usr/bin/env python3
"""Auditoría estática de navegación y acceso Monarca V42 fase 03."""
from pathlib import Path
import json

root = Path(__file__).resolve().parents[1]
src = root / "app/src/main/java/com/huntersystem/app"
assets = root / "app/src/main/assets"
issues = []

def check(value, message):
    if not value:
        issues.append(message)

def read(relative):
    return (root / relative).read_text(encoding="utf-8")

navigation = (src / "MonarchNavigationV42.kt").read_text(encoding="utf-8")
main = (src / "MainActivity.kt").read_text(encoding="utf-8")
screens = (src / "Screens.kt").read_text(encoding="utf-8")
styles = read("app/src/main/res/values/styles.xml")
splash = read("app/src/main/res/values-v31/styles.xml")
gradle = read("app/build.gradle.kts")
manifest = read("app/src/main/AndroidManifest.xml")
workflow = read(".github/workflows/android-apk.yml")
catalog = json.loads((assets / "catalog.json").read_text(encoding="utf-8"))
check(isinstance(catalog,list) and len(catalog) == 1200,
      "El catálogo de 1200 ejercicios debe conservarse")
check(len(list(assets.rglob("*.mp4"))) == 342,
      "Deben conservarse 342 demostraciones MP4")
check(not (assets / "exercise_thumbnails").exists(), "No se admiten miniaturas de ejercicios")
check(not (assets / "v39-miniatures").exists(), "No se admiten miniaturas V39")
check(all(f'MonarchDestination("{name}"' in navigation
          for name in ("home","plan","routines","book","profile")),
      "Falta alguna pestaña del menú principal")
for name in ("MonarchBootScreen", "MonarchBottomNavigation",
             "MonarchNavigationRail", "MonarchScreenTransition",
             "MonarchRecoveryScreen"):
    check(f"fun {name}(" in navigation, f"Falta componente {name}")
check("MonarchScreenTransition(ui.tab" in main, "No está conectado el host de transición")
check("rememberSaveableStateHolder()" in main, "Las rutas no conservan su estado guardable")
check("MonarchNavigationRail(ui)" in main, "No hay rail de navegación en tablet")
check("MonarchBottomNavigation(ui)" in main, "No hay navegación inferior móvil")
check("MonarchBootScreen(error)" in main, "No se sustituyó carga inicial")
check("MonarchRecoveryScreen(ui,store.readError!!)" in main,
      "No hay recuperación de guardados corruptos")
check("rememberSaveable(ui.tab,ui.library,saver=ScrollState.Saver)" in screens,
      "Las pantallas no conservan el scroll")
check("widthIn(max=960.dp)" in screens, "No está limitado el contenido en tablets")
check('versionName = "42.2-monarca-fase3"' in gradle,
      "Versión de compilación incorrecta")
check("#080D19" in styles and "windowSplashScreenAnimatedIcon" in splash,
      "Falta integración de splash por versión de Android")
check("android:allowBackup=\"false\"" in manifest,
      "No debe cambiar la protección de datos de la app")
check("Auditoria Monarca V42 fase 03" in workflow,
      "La auditoría de fase 03 no está integrada en CI")
out={"fase":"03","ejercicios":len(catalog),"mp4":342,
     "secciones":5,"responsive":True,"estado":"OK" if not issues else "ERROR",
     "errores":issues}
print(json.dumps(out,ensure_ascii=False,indent=2))
if issues:
    raise SystemExit(1)

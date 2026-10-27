#!/usr/bin/env python3
import json
from pathlib import Path
R=Path(__file__).resolve().parents[1]
def read(path):return (R/path).read_text(encoding="utf8")
src="app/src/main/java/com/huntersystem/app/"
gradle=read("app/build.gradle.kts")
checks={
 "version V43 preview":'versionName = "43.4-shadow-realms-preview"' in gradle and "versionCode = 122" in gradle,
 "Kotlin y Compose":'id("org.jetbrains.kotlin.plugin.compose")' in gradle,
 "identidad Eclipse":(R/(src+"EclipseVisuals.kt")).is_file(),
 "componentes Eclipse":(R/(src+"EclipseComponents.kt")).is_file(),
 "composición Monarca":(R/(src+"MonarchDashboardV42.kt")).is_file(),
 "portal Eclipse retirado":'FusionEclipsePortal()' not in read(src+"Screens.kt") and not (R/"app/src/main/assets/eclipse/portal.webp").exists(),
 "fondos por ventana":(R/(src+"HunterSceneBackdropsV43.kt")).is_file() and 'hunterSceneBackdrop(' in read(src+"MainActivity.kt") and 'hunterSceneBackdrop(' in read(src+"Overlays.kt"),
 "temas conjuntos":'"constellation"' in read(src+"HunterDesignSystem.kt") and '"ice"' in read(src+"HunterDesignSystem.kt"),
 "splash animado":(R/"app/src/main/java/com/huntersystem/app/HunterAnimatedBootArtwork.kt").is_file() and (R/"app/src/main/assets/monarch_splash_background.webp").is_file() and "HunterAnimatedBootArtwork(" in read(src+"MonarchNavigationV42.kt"),
 "catálogo ampliable":'catalog-hunter2000-extra.json' in read(src+"Catalog.kt"),
 "restricción QA":'approvedForAutoplan' in read(src+"Rules.kt"),
 "artefacto V43":'Hunter-System-V43-4-Shadow-Realms-APK' in read(".github/workflows/android-apk.yml"),
 "pruebas de catálogo":(R/"app/src/test/java/com/huntersystem/app/Hunter2000CatalogTest.kt").is_file(),
}
errors=[name for name,ok in checks.items() if not ok]
print(json.dumps({"version":"43.4-shadow-realms-preview","checks":checks,"status":"ERROR" if errors else "OK","errors":errors},ensure_ascii=False,indent=2))
if errors:raise SystemExit(1)

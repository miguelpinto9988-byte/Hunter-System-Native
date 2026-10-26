#!/usr/bin/env python3
import json
from pathlib import Path
R=Path(__file__).resolve().parents[1]
def read(path):return (R/path).read_text(encoding="utf8")
src="app/src/main/java/com/huntersystem/app/"
gradle=read("app/build.gradle.kts")
checks={
 "version V43 preview":'versionName = "43.0-fusion-preview"' in gradle and "versionCode = 118" in gradle,
 "Kotlin y Compose":'id("org.jetbrains.kotlin.plugin.compose")' in gradle,
 "identidad Eclipse":(R/(src+"EclipseVisuals.kt")).is_file(),
 "componentes Eclipse":(R/(src+"EclipseComponents.kt")).is_file(),
 "composición Monarca":(R/(src+"MonarchDashboardV42.kt")).is_file(),
 "portal Eclipse":'FusionEclipsePortal()' in read(src+"Screens.kt") and (R/"app/src/main/assets/eclipse/portal.webp").is_file(),
 "temas conjuntos":'"constellation"' in read(src+"HunterDesignSystem.kt") and '"ice"' in read(src+"HunterDesignSystem.kt"),
 "catálogo ampliable":'catalog-hunter2000-extra.json' in read(src+"Catalog.kt"),
 "restricción QA":'approvedForAutoplan' in read(src+"Rules.kt"),
 "artefacto V43":'Hunter-System-V43-Fusion-Preview-APK' in read(".github/workflows/android-apk.yml"),
 "pruebas de catálogo":(R/"app/src/test/java/com/huntersystem/app/Hunter2000CatalogTest.kt").is_file(),
}
errors=[name for name,ok in checks.items() if not ok]
print(json.dumps({"version":"43.0-fusion-preview","checks":checks,"status":"ERROR" if errors else "OK","errors":errors},ensure_ascii=False,indent=2))
if errors:raise SystemExit(1)

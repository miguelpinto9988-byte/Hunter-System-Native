#!/usr/bin/env python3
import json
from pathlib import Path
R=Path(__file__).resolve().parents[1]
def read(path):return (R/path).read_text(encoding="utf8")
src="app/src/main/java/com/huntersystem/app/"
gradle=read("app/build.gradle.kts")
checks={
 "version V43 preview":'versionName = "43.9-png-ui-preview"' in gradle and "versionCode = 127" in gradle,
 "Kotlin y Compose":'id("org.jetbrains.kotlin.plugin.compose")' in gradle,
 "identidad Eclipse":(R/(src+"EclipseVisuals.kt")).is_file(),
 "componentes Eclipse":(R/(src+"EclipseComponents.kt")).is_file(),
 "composición Monarca":(R/(src+"MonarchDashboardV42.kt")).is_file(),
 "portal Eclipse retirado":'FusionEclipsePortal()' not in read(src+"Screens.kt") and not (R/"app/src/main/assets/eclipse/portal.webp").exists(),
 "fondos por ventana":all((R/("app/src/main/assets/hunter-png/wallpapers/"+route+".png")).is_file() for route in
    ("home","plan","routines","book","stats","objectives","profile","recovery"))
    and 'HunterRealmWallpaper(' in read(src+"MainActivity.kt") and 'HunterRealmWallpaper(' in read(src+"Overlays.kt")
    and 'hunter-png/wallpapers/' in read(src+"HunterRealmWallpaper.kt")
    and 'realm-art/' not in read(src+"HunterRealmWallpaper.kt"),
 "biblioteca iconos PNG":(R/(src+"HunterPngLibrary.kt")).is_file()
    and len(list((R/"app/src/main/assets/hunter-png/icons").glob("*.png")))==32
    and 'HunterPngIcon(' in read(src+"MonarchRoutinesV42.kt")
    and 'HunterPngIcon(' in read(src+"MonarchProgressV42.kt")
    and 'HunterPngIcon(' in read(src+"Screens.kt"),
 "menu lateral PNG":(R/(src+"HunterSideMenu.kt")).is_file()
    and 'ModalNavigationDrawer(' in read(src+"MainActivity.kt")
    and 'HunterSideMenu(ui)' in read(src+"MainActivity.kt"),
 "recompensa real":'if(!log.b("complete")) return' in read(src+"HunterMissionCompletedCard.kt")
    and 'HunterMissionCompletedCard(ui,selected)' in read(src+"MonarchMissionsV42.kt"),
 "portadas originales descargables":(R/"tools/download_monarch_boss_originals.py").is_file()
    and 'monarch-boss-original/' in read(src+"MonarchSpecialMissionUI.kt")
    and not any((R/("app/src/main/assets/monarch-boss/"+boss+".svg")).exists()
      for boss in ("guardia","fortaleza","trono")),
 "reubicación Monarca":read(src+"Screens.kt").index("MonarchSpecialMissionsPanel(ui)") < read(src+"Screens.kt").index("MonarchHero(ui,stats)") and read(src+"Screens.kt").count("MonarchSpecialMissionsPanel(ui)")==1,
 "objetivos desplegables":'detailsExpanded' in read(src+"MonarchSpecialMissionUI.kt") and 'recordMonarchProgress' in read(src+"MonarchSpecialMissionUI.kt") and 'MonarchBossMissionArtwork(' in read(src+"MonarchSpecialMissionUI.kt"),
 "temas conjuntos":'"constellation"' in read(src+"HunterDesignSystem.kt") and '"ice"' in read(src+"HunterDesignSystem.kt"),
 "splash animado":(R/"app/src/main/java/com/huntersystem/app/HunterAnimatedBootArtwork.kt").is_file() and (R/"app/src/main/assets/monarch_splash_background.webp").is_file() and "HunterAnimatedBootArtwork(" in read(src+"MonarchNavigationV42.kt"),
 "catálogo ampliable":'catalog-hunter2000-extra.json' in read(src+"Catalog.kt"),
 "restricción QA":'approvedForAutoplan' in read(src+"Rules.kt"),
 "artefacto V43":'Hunter-System-V43-9-PNG-UI-APK' in read(".github/workflows/android-apk.yml"),
 "pruebas de catálogo":(R/"app/src/test/java/com/huntersystem/app/Hunter2000CatalogTest.kt").is_file(),
}
errors=[name for name,ok in checks.items() if not ok]
print(json.dumps({"version":"43.9-png-ui-preview","checks":checks,"status":"ERROR" if errors else "OK","errors":errors},ensure_ascii=False,indent=2))
if errors:raise SystemExit(1)

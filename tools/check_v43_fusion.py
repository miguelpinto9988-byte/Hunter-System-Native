#!/usr/bin/env python3
import json
from pathlib import Path
R=Path(__file__).resolve().parents[1]
def read(path):return (R/path).read_text(encoding="utf8")
src="app/src/main/java/com/huntersystem/app/"
gradle=read("app/build.gradle.kts")
checks={
 "version V43.12":'versionName = "43.12-monarca-eclipse"' in gradle and "versionCode = 130" in gradle,
 "Kotlin y Compose":'id("org.jetbrains.kotlin.plugin.compose")' in gradle,
 "identidad Eclipse":(R/(src+"EclipseVisuals.kt")).is_file(),
 "componentes Eclipse":(R/(src+"EclipseComponents.kt")).is_file(),
 "composición Monarca":(R/(src+"MonarchDashboardV42.kt")).is_file(),
 "cazador sin circulos en tarjeta de evolucion":'val hunterArtwork = profileMonarchHunterArtwork(' in read(src+"MonarchDashboardV42.kt")
    and 'monarchHunterHeroArtwork(profile.s("gender"), rank)' not in read(src+"MonarchDashboardV42.kt")
    and ('AssetImage(\n                            hunterArtwork' in read(src+"MonarchDashboardV42.kt") or 'AssetImage(hunterArtwork,' in read(src+"MonarchDashboardV42.kt") or 'HunterEvolutionImage(appearance.s("collection","hunter"),profile.s("gender"),rank,hunterArtwork,' in read(src+"MonarchDashboardV42.kt"))
    and 'drawCircle(' not in read(src+"MonarchDashboardV42.kt").split("fun MonarchDashboardHero(",1)[1].split("fun MonarchDashboardWelcome(",1)[0]
    and 'ui33/companion-$companion.webp' not in read(src+"MonarchDashboardV42.kt").split("fun MonarchDashboardHero(",1)[1].split("fun MonarchDashboardWelcome(",1)[0],
 "portal Eclipse retirado":'FusionEclipsePortal()' not in read(src+"Screens.kt") and not (R/"app/src/main/assets/eclipse/portal.webp").exists(),
 "fondos planos sin imagenes de sombras":(R/(src+"HunterFlatBackground.kt")).is_file()
    and not (R/(src+"HunterRealmWallpaper.kt")).exists()
    and not (R/"app/src/main/assets/hunter-png/wallpapers").exists()
    and "HunterFlatBackground(Modifier.matchParentSize())" in read(src+"MainActivity.kt")
    and "HunterFlatBackground(Modifier.matchParentSize())" in read(src+"Overlays.kt")
    and "LocalHunter.current.bg" in read(src+"HunterFlatBackground.kt")
    and "AssetImage(" not in read(src+"HunterFlatBackground.kt"),
 "biblioteca iconos SVG 2D":(R/(src+"HunterPngLibrary.kt")).is_file()
    and len(list((R/"app/src/main/assets/hunter-icons").glob("*.svg")))==49
    and not (R/"app/src/main/assets/hunter-png/icons").exists()
    and 'painterResource(id=hunterIconDrawableId(key))' in read(src+"HunterPngLibrary.kt")
    and len(list((R/"app/src/main/res/drawable").glob("hunter_icon_*.xml")))==49
    and 'HunterPngIcon(' in read(src+"MonarchRoutinesV42.kt")
    and 'HunterPngIcon(' in read(src+"MonarchProgressV42.kt")
    and 'HunterPngIcon(' in read(src+"Screens.kt"),
 "navegacion inferior unica":not (R/(src+"HunterSideMenu.kt")).exists()
    and "ModalNavigationDrawer(" not in read(src+"MainActivity.kt")
    and "MonarchNavigationRail(" not in read(src+"MainActivity.kt")
    and "MonarchNavigationRail(" not in read(src+"MonarchNavigationV42.kt")
    and "openMenu" not in read(src+"Screens.kt")
    and "MonarchBottomNavigation(ui)" in read(src+"MainActivity.kt"),
 "perfil inicial independiente":'profileStandalone -> ProfileScreen(ui)' in read(src+"MainActivity.kt")
    and 'if(hasProfile && !profileStandalone' in read(src+"MainActivity.kt")
    and 'fun openProfile()' in read(src+"Screens.kt")
    and 'fun profileSaved()' in read(src+"Screens.kt")
    and 'SceneColumn(ui,showHeader=false)' in read(src+"Profile.kt")
    and 'ui.profileSaved()' in read(src+"Profile.kt")
    and 'setOf("objectives","stats","profile")' not in read(src+"MonarchNavigationV42.kt"),
 "recompensa real":'if(!log.b("complete")) return' in read(src+"HunterMissionCompletedCard.kt")
    and 'HunterMissionCompletedCard(ui,selected)' in read(src+"MonarchMissionsV42.kt"),
 "portadas originales descargables":(R/"tools/download_monarch_boss_originals.py").is_file()
    and 'monarch-boss-original/' in read(src+"MonarchSpecialMissionUI.kt")
    and not any((R/("app/src/main/assets/monarch-boss/"+boss+".svg")).exists()
      for boss in ("guardia","fortaleza","trono")),
 "reubicación Monarca":read(src+"Screens.kt").index("MonarchSpecialMissionsPanel(ui)") < read(src+"Screens.kt").index("MonarchHero(ui,stats)") and read(src+"Screens.kt").count("MonarchSpecialMissionsPanel(ui)")==1,
 "misiones Monarca voluntarias y solo equipo":'fun offerFor(store:NativeStore' in read(src+"MonarchSpecialMissionState.kt")
    and 'fun equipmentReady(store:NativeStore' in read(src+"MonarchSpecialMissionState.kt")
    and 'MonarchMissionCatalog.offerFor(store,sunday,zone)' in read(src+"MonarchSpecialMissionUI.kt")
    and 'enabled=reason==null&&consent' in read(src+"MonarchSpecialMissionUI.kt"),
 "objetivos desplegables y arte integrado en tarjeta":'detailsExpanded' in read(src+"MonarchSpecialMissionUI.kt")
    and 'recordMonarchProgress' in read(src+"MonarchSpecialMissionUI.kt")
    and 'MonarchBossMissionCard(mission.id)' in read(src+"MonarchSpecialMissionUI.kt")
    and 'MonarchBossMissionHeading(' in read(src+"MonarchSpecialMissionUI.kt")
    and 'MonarchBossMissionArtwork(' not in read(src+"MonarchSpecialMissionUI.kt"),
 "fondo de los tres jefes hasta borde inferior":'MonarchBossMissionCard(mission.id)' in read(src+"MonarchSpecialMissionUI.kt")
    and 'AssetImage(' in read(src+"MonarchSpecialMissionUI.kt")
    and '"monarch-boss-original/"+id+".jpg"' in read(src+"MonarchSpecialMissionUI.kt")
    and 'Modifier.matchParentSize()' in read(src+"MonarchSpecialMissionUI.kt").split('private fun MonarchBossMissionCard(',1)[1].split('private fun MonarchBossMissionHeading(',1)[0]
    and 'alignment=Alignment.BottomCenter' in read(src+"MonarchSpecialMissionUI.kt")
    and '.height(355.dp)' not in read(src+"MonarchSpecialMissionUI.kt")
    and all('"'+name+'"' in read(src+"MonarchSpecialMissionState.kt") for name in ("guardia","fortaleza","trono")),
 "temas conjuntos":'"constellation"' in read(src+"HunterDesignSystem.kt") and '"ice"' in read(src+"HunterDesignSystem.kt"),
 "splash animado":(R/"app/src/main/java/com/huntersystem/app/HunterAnimatedBootArtwork.kt").is_file() and (R/"app/src/main/assets/monarch_splash_background.webp").is_file() and "HunterAnimatedBootArtwork(" in read(src+"MonarchNavigationV42.kt"),
 "catálogo ampliable":'catalog-hunter2000-extra.json' in read(src+"Catalog.kt"),
 "restricción QA":'approvedForAutoplan' in read(src+"Rules.kt"),
 "artefacto V43":'Hunter-System-V43-12-Monarca-Eclipse-APK' in read(".github/workflows/android-apk.yml"),
 "sin ventana Evolucionar Monarca":all(
     'EVOLUCIONAR' not in read(src+name)
     for name in ("MonarchDashboardV42.kt","HunterProfileLayoutV43.kt"))
     and '"evolution"->' not in read(src+"Overlays.kt")
     and 'fun MonarchEvolutionScreen(' not in read(src+"MonarchProgressV42.kt")
     and 'fun Evolution(ui:NativeUi)' not in read(src+"Supplement.kt")
     and all('ui.open("evolution")' not in f.read_text(encoding="utf8")
             for f in (R/src).glob("*.kt"))
     and 'if(type=="evolution")return' in read(src+"Screens.kt")
     and 'HProgress(xpInLevel/300f)' in read(src+"MonarchDashboardV42.kt")
     and 'HunterEvolutionImage(' in read(src+"MonarchDashboardV42.kt"),
 "contornos suaves V43.12":(R/"tools/check_monarch_outlines_v43.py").is_file()
    and 'fun Modifier.monarchOutline(' in read(src+"MonarchOutlineV43.kt")
    and 'fun Modifier.monarchPressable(' in read(src+"MonarchOutlineV43.kt")
    and 'if(emphasis)1.4.dp else .8.dp' in read(src+"MonarchOutlineV43.kt")
    and '.monarchOutline(shape,pressed=pressed)' in read(src+"Design.kt")
    and '.monarchOutline(shape,focused=focused)' in read(src+"Design.kt")
    and '.monarchOutline(shape,active=selected,pressed=pressed)' in read(src+"Design.kt")
    and '.monarchPressable(' in read(src+"EclipseComponents.kt")
    and '.monarchOutline(' in read(src+"Overlays.kt")
    and '.monarchPressable(' in read(src+"MonarchMissionsV42.kt"),
 "selector dual de temas y cinco contornos":all(
     fragment in read(src+"HunterAppearanceV43.kt")
     for fragment in ('fun mode(', 'fun outline(', 'fun tokens(',
                     'fun HunterModePicker(', 'fun HunterOutlinePicker('))
    and read(src+"HunterAppearanceV43.kt").count('Outline("')==5
    and 'HunterModePicker(values["theme"]' in read(src+"Profile.kt")
    and 'HunterOutlinePicker(values["outlineColor"]' in read(src+"Profile.kt")
    and 'outlineColor=values["outlineColor"]' in read(src+"Profile.kt")
    and '.put("outlineColor",HunterAppearanceV43.outline(outlineColor))' in read(src+"NativeStore.kt")
    and 'HunterAppearanceV43.tokens(mode,border)' in read(src+"Design.kt")
    and 'c.outlineAccent.copy(alpha=.96f)' in read(src+"MonarchOutlineV43.kt"),
 "mismo acento para letras y marcos":'val selectedAccent=borderHex(currentMode,outline(outlineValue))' in read(src+"HunterAppearanceV43.kt")
    and '"--accent" to selectedAccent' in read(src+"HunterAppearanceV43.kt")
    and '"--outlineAccent" to selectedAccent' in read(src+"HunterAppearanceV43.kt")
    and 'color=LocalHunter.current.accent' in read(src+"Design.kt")
    and 'c.outlineAccent.copy(alpha=.96f)' in read(src+"MonarchOutlineV43.kt"),
 "pruebas de catálogo":(R/"app/src/test/java/com/huntersystem/app/Hunter2000CatalogTest.kt").is_file(),
}
errors=[name for name,ok in checks.items() if not ok]
print(json.dumps({"version":"43.12-monarca-eclipse-pixel-2d","checks":checks,"status":"ERROR" if errors else "OK","errors":errors},ensure_ascii=False,indent=2))
if errors:raise SystemExit(1)

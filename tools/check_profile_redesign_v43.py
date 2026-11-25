#!/usr/bin/env python3
"""Auditoría del rediseño de perfil V43.12 sin alterar persistencia ni navegación."""
from pathlib import Path
import json

root=Path(__file__).resolve().parents[1]
source=root/"app/src/main/java/com/huntersystem/app"
def read(name):
    return (source/name).read_text(encoding="utf-8")
profile=read("Profile.kt")
host=read("ComposeProfile.kt")
assets=root/"app/src/main/assets/profile-monarca"
fields=read("HunterProfileFieldsV43.kt")
pickers=read("EclipseComponents.kt")
companions=read("MonarchCompanionsV42.kt")
screens=read("Screens.kt")
main=read("MainActivity.kt")
dashboard=read("MonarchDashboardV42.kt")
checks={
 "perfil independiente":'profileStandalone -> ProfileScreen(ui)' in main and 'ComposeHunterProfile(ui,session,' in profile,
 "cuatro etapas navegables":all(x in host for x in
     ('Identidad del cazador','Entrenamiento','Personalización','Activación del sistema','ACTIVAR PERFIL')),
 "calendario por toques":'(days.getInt(day) + 1) % categories.size' in host and '"weeklyCategories",arr(week)' in profile,
 "controles por modalidad":all(x in host for x in ('"dailyMissions"','"minutes"','"blockMode22"','"restSeconds"')),
 "carrusel horizontal con Tusk":'MonarchCompanions.profileChoices' in profile and
     'horizontalScroll' in host and 'companion-tusk.webp' in host and
     (root/"app/src/main/assets/ui33/companion-tusk.webp").is_file(),
 "arte de los cuatro pasos":all((assets/name).is_file()
     for name in ("identity.webp","training.webp","appearance.webp","activation.webp")),
 "vista previa seleccionada":all(x in host for x in ('sceneArt','figureArt','companionCharacter','ProfilePalette')),
 "perfil offline":'context.assets.open(path)' in host and 'WebView' not in host,
 "ilustraciones originales HD sin recorte":all(
     (root/"app/src/main/assets/ui34"/name).is_file()
     for name in ("reader-d.webp","regressor-e.webp","hunter-male-e.webp","hunter-female-e.webp"))
     and 'height(278.dp)' in pickers
     and 'ContentScale.Fit' in pickers.split('@Composable fun HunterPicker(',1)[1].split('@Composable fun BackgroundPicker(',1)[0]
     and 'ContentScale.Crop' not in pickers.split('@Composable fun HunterPicker(',1)[1].split('@Composable fun BackgroundPicker(',1)[0],
 "selección guardada y leída por el Panel Monarca":'put("collection",value("collection")).put("scene",value("scene"))' in profile
     and 'profileMonarchHunterArtwork(' in dashboard
     and 'chosenHunterArtwork(collection,gender)' in dashboard,
 "24 evoluciones restauradas E-S en GitHub":all(
     (lambda data:data[:4]==b"RIFF" and data[8:12]==b"WEBP")(
         (root/"app/src/main/assets/hunter-evolutions-v43"/f"{name}-{rank}.webp").read_bytes())
     for name in ("reader","regressor","hunter-male","hunter-female")
     for rank in "edcbas")
     and 'HunterEvolutionImage(' in pickers
     and 'HunterEvolutionImage(' in dashboard,
 "8 escenarios HD":len(list((root/"app/src/main/assets/hunter-scenes-hd").glob("*.webp")))==8
     and all(key in pickers for key in (
      'biblioteca-lunar','palacio-submarino','reino-de-las-nubes',
      'ciudadela-boreal','trono-volcanico','santuario-de-bambu',
      'jardin-sagrado','templo-del-ocaso')),
 "panel usa selección guardada":'val hunterArtwork = profileMonarchHunterArtwork(' in dashboard
     and 'appearance.s("collection","hunter")' in dashboard
     and 'chosenSceneArtwork(selectedScene)' in dashboard,
 "guardar perfil nuevo y editado":'ui.profileSaved()' in host and 'require(input.b("confirmed"))' in profile
     and 'fun profileSaved()' in screens and 'tab="home"' in screens,
 "todos los campos permanecen":all(x in profile+fields for x in
      ('name','age','height','weight','goalGroup','finalGoal','difficulty',
       'trainingMode','restSeconds','dailyMissions','equipment',
       'injuries','restrictions','companionCharacter')),
 "sin menú lateral":not (source/"HunterSideMenu.kt").exists()
     and 'ModalNavigationDrawer' not in main and 'MonarchNavigationRail' not in main,
 "sin ajustes redundantes":'Ajustes del entorno' not in profile
     and 'HSelect("Iluminación"' not in profile,
}
failed=[name for name,ok in checks.items() if not ok]
print(json.dumps({"version":"perfil-cuatro-pasos","checks":checks,
  "status":"ERROR" if failed else "OK","errors":failed},ensure_ascii=False,indent=2))
if failed:raise SystemExit(1)

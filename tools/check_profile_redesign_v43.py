#!/usr/bin/env python3
"""Auditoría del rediseño de perfil V43.12 sin alterar persistencia ni navegación."""
from pathlib import Path
import json

root=Path(__file__).resolve().parents[1]
source=root/"app/src/main/java/com/huntersystem/app"
def read(name):
    return (source/name).read_text(encoding="utf-8")
profile=read("Profile.kt")
layout=read("HunterProfileLayoutV43.kt")
fields=read("HunterProfileFieldsV43.kt")
pickers=read("EclipseComponents.kt")
companions=read("MonarchCompanionsV42.kt")
screens=read("Screens.kt")
main=read("MainActivity.kt")
dashboard=read("MonarchDashboardV42.kt")
checks={
 "perfil independiente":'profileStandalone -> ProfileScreen(ui)' in main and
     'SceneColumn(ui,showHeader=false)' in profile,
 "tres etapas navegables":all(x in profile for x in
     ('profileStage==0','profileStage==1','HunterProfileStageTabs(profileStage)',
      'profileStage=1','profileStage=2')),
 "13 secciones con numeración":profile.count("HunterProfileSection(")==13 and
     all('HunterProfileSection('+str(i)+',' in profile for i in range(1,14)),
 "línea numerada":'HunterProfileNumberedHeading' in layout and
     'Brush.horizontalGradient' in layout and
     'number.toString().padStart(2' in layout,
 "descanso junto a nivel y modalidad":profile.index('HunterProfileSection(6,')<
     profile.index('HSelect("Descanso entre series"')<
     profile.index('HunterProfileSection(7,'),
 "compañero original horizontal":'MonarchCompanionPicker(values["companionCharacter"]' in profile and
     'fun MonarchCompanionPicker(' in companions and
     '.horizontalScroll(rememberScrollState())' in companions,
 "vista previa seleccionada":'HunterProfileCardPreview(' in profile and
     'chosenHunterArtwork(collection,gender)' in layout and
     'chosenSceneArtwork(scene)' in layout,
 "8 escenarios HD":len(list((root/"app/src/main/assets/hunter-scenes-hd").glob("*.webp")))==8
     and all(key in pickers for key in (
      'biblioteca-lunar','palacio-submarino','reino-de-las-nubes',
      'ciudadela-boreal','trono-volcanico','santuario-de-bambu',
      'jardin-sagrado','templo-del-ocaso')),
 "panel usa selección guardada":'profileMonarchHunterArtwork(appearance.s("collection"' in dashboard
     and 'chosenSceneArtwork(selectedScene)' in dashboard,
 "guardar perfil nuevo y editado":profile.count('ui.profileSaved()')==2
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
print(json.dumps({"version":"43.12-perfil-tres-etapas","checks":checks,
  "status":"ERROR" if failed else "OK","errors":failed},ensure_ascii=False,indent=2))
if failed:raise SystemExit(1)

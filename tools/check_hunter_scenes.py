#!/usr/bin/env python3
"""Valida escenas, selección, persistencia y recursos del APK V43.12."""
import hashlib
from io import BytesIO
import json
from pathlib import Path
import sys
import zipfile
from PIL import Image

ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/"app/src/main/assets/hunter-scenes-hd"
NAMES={"biblioteca-lunar","jardin-sagrado","palacio-submarino","templo-del-ocaso",
       "ciudadela-boreal","reino-de-las-nubes","trono-volcanico","santuario-de-bambu"}
manifest=json.loads((ASSETS/"manifest.json").read_text(encoding="utf-8"))
assert len(manifest)==8 and {x["id"] for x in manifest}==NAMES
for item in manifest:
 raw=(ASSETS/(item["id"]+".webp")).read_bytes()
 assert hashlib.sha256(raw).hexdigest()==item["sha256"]
 with Image.open(BytesIO(raw)) as im:assert im.size==(960,720),item
scene=(ROOT/"app/src/main/java/com/huntersystem/app/HunterSceneCustomizationV43.kt").read_text()
profile=(ROOT/"app/src/main/java/com/huntersystem/app/Profile.kt").read_text()
dashboard=(ROOT/"app/src/main/java/com/huntersystem/app/MonarchDashboardV42.kt").read_text()
global_wallpaper=(ROOT/"app/src/main/java/com/huntersystem/app/HunterRealmWallpaper.kt").read_text()
assert all('HunterScene("'+n+'"' in scene for n in NAMES)
assert all(x in profile for x in ('HunterSceneCustomizer(',
 'onScene={values["scene33"]=it}', '"lighting33"','"atmosphere33"','"frame33"'))
assert "MonarchCardArtwork(" in dashboard and 'appearance.s("scene","auto")' in dashboard
assert "AssetImage(" not in global_wallpaper
if "--apk" in sys.argv:
 apk=next((ROOT/"app/build/outputs/apk/release").glob("*.apk"))
 with zipfile.ZipFile(apk) as z:
  for item in manifest:
   name="assets/hunter-scenes-hd/"+item["id"]+".webp"
   raw=z.read(name)
   assert hashlib.sha256(raw).hexdigest()==item["sha256"],name
   with Image.open(BytesIO(raw)) as im:assert im.size==(960,720),name
 print("APK verificada: ocho escenas HD empaquetadas.")
else:
 print("Galería HD, vista previa y ajustes persistentes verificados.")

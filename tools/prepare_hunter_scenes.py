#!/usr/bin/env python3
"""Extrae recursos HD incluidos en Git sin descargar imágenes desde internet."""
import hashlib
import json
from pathlib import Path
import zipfile

ROOT=Path(__file__).resolve().parents[1]
ARCHIVE=ROOT/"art-masters"/"hunter-scenes-hd.zip"
OUTPUT=ROOT/"app"/"src"/"main"/"assets"/"hunter-scenes-hd"
PREFIX="app/src/main/assets/hunter-scenes-hd/"
NAMES={"biblioteca-lunar","jardin-sagrado","palacio-submarino",
       "templo-del-ocaso","ciudadela-boreal","reino-de-las-nubes",
       "trono-volcanico","santuario-de-bambu"}

def prepare():
 with zipfile.ZipFile(ARCHIVE) as z:
  expected={PREFIX+"manifest.json"}|{PREFIX+x+".webp" for x in NAMES}
  assert set(z.namelist())==expected,"Entradas inesperadas en el paquete"
  manifest=json.loads(z.read(PREFIX+"manifest.json"))
  assert {x["id"] for x in manifest}==NAMES
  OUTPUT.mkdir(parents=True,exist_ok=True)
  for item in manifest:
   name=item["id"]
   assert item["hd"]==[960,720],name
   raw=z.read(PREFIX+name+".webp")
   assert raw[:4]==b"RIFF" and raw[8:12]==b"WEBP",name
   assert hashlib.sha256(raw).hexdigest()==item["sha256"],name
   dest=OUTPUT/(name+".webp")
   if not dest.exists() or hashlib.sha256(dest.read_bytes()).hexdigest()!=item["sha256"]:
    dest.write_bytes(raw)
   print(f"Ambiente HD {name}: {len(raw)} bytes")
  (OUTPUT/"manifest.json").write_text(
    json.dumps(manifest,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
 print("Ocho ambientes HD preparados, sin tocar los fondos globales.")

if __name__=="__main__":
 prepare()

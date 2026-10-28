#!/usr/bin/env python3
"""Install approved PNG backgrounds/icons, unchanged, from a versioned ZIP.
PNG icon alpha is mandatory. This runs in CI before building the APK.
"""
from pathlib import Path
from urllib.request import urlopen, Request
from io import BytesIO
from zipfile import ZipFile
import struct
ROOT=Path(__file__).resolve().parents[1]
SOURCE="https://gcdn.picsart.com/editing-temp/108634cb-1ddd-43e1-9998-b56076925396.zip"
OUT=ROOT/"app/src/main/assets/hunter-png"
SCENES=("home","plan","routines","book","stats","objectives","profile","recovery")
ICONS=("academy","achievement","book","cardio","dumbbell","energy","exercises","focus","goal_habit","goal_read","goal_strength","goals","level","lightning","milestone","missions","mobility","panel","profile","progress","reminders","routines","settings","shield","streak","strength","success","timer","training","trophy","warning","xp")
def png_info(buf):
 if buf[:8]!=b"\x89PNG\r\n\x1a\n":raise ValueError("PNG signature missing")
 width,height,depth,mode=struct.unpack(">IIBB",buf[16:26])
 return width,height,depth,mode
def main():
 req=Request(SOURCE,headers={"User-Agent":"HunterSystem-Asset-Installer/1.0"})
 with urlopen(req,timeout=75) as res:
  raw=res.read(15_000_001)
 if len(raw)>15_000_000:raise ValueError("Asset package too large")
 with ZipFile(BytesIO(raw)) as z:
  names=set(z.namelist())
  for area,keys in (("wallpapers",SCENES),("icons",ICONS)):
   (OUT/area).mkdir(parents=True,exist_ok=True)
   for name in keys:
    src=f"{area}/{name}.png"
    if src not in names:raise ValueError("Missing "+src)
    buf=z.read(src)
    w,h,depth,mode=png_info(buf)
    if w<64 or h<64:raise ValueError("Image too small "+src)
    if area=="icons" and mode!=6:raise ValueError("Icons must be true RGBA PNG: "+src)
    (OUT/src).write_bytes(buf)
    print(src,w,h,"RGBA" if mode==6 else "PNG")
 print("Hunter PNG assets validated:",len(SCENES),"wallpapers,",len(ICONS),"transparent icons")
if __name__=="__main__":main()

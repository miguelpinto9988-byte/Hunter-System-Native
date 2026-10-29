#!/usr/bin/env python3
"""Instala los fondos PNG; los iconos de la app son exclusivamente SVG Pixel 2D."""
from pathlib import Path
from urllib.request import Request, urlopen
from io import BytesIO
from zipfile import ZipFile
from shutil import rmtree
import struct

ROOT=Path(__file__).resolve().parents[1]
SOURCE="https://gcdn.picsart.com/editing-temp/108634cb-1ddd-43e1-9998-b56076925396.zip"
OUT=ROOT/"app/src/main/assets/hunter-png"
SCENES=("home","plan","routines","book","stats","objectives","profile","recovery")

def png_info(data):
    if data[:8]!=b"\x89PNG\r\n\x1a\n":
        raise ValueError("PNG signature missing")
    return struct.unpack(">IIBB",data[16:26])

def main():
    legacy=OUT/"icons"
    if legacy.exists():
        rmtree(legacy)
    req=Request(SOURCE,headers={"User-Agent":"HunterSystem-Asset-Installer/1.0"})
    with urlopen(req,timeout=75) as response:
        raw=response.read(15_000_001)
    if len(raw)>15_000_000:
        raise ValueError("Asset package too large")
    with ZipFile(BytesIO(raw)) as z:
        names=set(z.namelist())
        (OUT/"wallpapers").mkdir(parents=True,exist_ok=True)
        for name in SCENES:
            path=f"wallpapers/{name}.png"
            if path not in names:
                raise ValueError("Missing "+path)
            data=z.read(path)
            w,h,depth,mode=png_info(data)
            if w<64 or h<64:
                raise ValueError("Image too small "+path)
            (OUT/path).write_bytes(data)
            print(path,w,h,"RGBA" if mode==6 else "PNG")
    assert not legacy.exists(),"Persisten iconos PNG antiguos"
    print("Hunter: 8 fondos PNG, 0 iconos PNG antiguos")

if __name__=="__main__":
    main()

#!/usr/bin/env python3
"""Extract transparent raster icons from the Hunter System design approved by the user.

Input is an existing PNG icon-sheet illustration, not vector geometry, SVG,
or a recreation of its icon designs. Each output is a standalone RGBA PNG.
No square/tile artwork is baked into the output. The sheet is cached in CI
and individual crops are deterministic.
"""
from __future__ import annotations
from io import BytesIO
from pathlib import Path
from urllib.request import urlopen, Request
from PIL import Image
import json

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/"app/src/main/assets/hunter-png/icons"
SHEET_URL="https://gcdn.picsart.com/editing-temp/7c6b73e8-0a5f-4736-90e6-c33eb7be514b.png"
# Coordinates in the source 1536x1024 approved icon sheet; only the
# visible icon is cropped, never the panel, text, label, or frame.
REGIONS={
"panel":(735,242,819,318),"missions":(52,426,139,509),
"routines":(884,249,983,319),"exercises":(1052,248,1137,320),
"progress":(1220,248,1307,319),"profile":(1406,247,1495,320),
"menu":(68,246,147,311),"reminders":(215,242,300,317),
"back":(370,241,444,313),"settings":(525,241,613,315),
"strength":(160,428,270,516),"academy":(276,428,372,513),
"trophy":(509,425,605,512),"cardio":(632,426,720,510),
"start":(788,434,875,510),"pause":(879,433,965,508),
"success":(966,430,1056,512),"available":(1063,430,1145,510),
"locked":(1147,432,1227,512),"warning":(1231,427,1324,516),
"timer":(1329,428,1410,510),"retry":(1417,429,1500,511),
"progress_bar":(45,636,180,710),"milestone":(203,637,285,715),
"streak":(298,637,400,716),"level":(398,635,493,721),
"xp":(497,642,638,716),"goals":(630,641,728,718),
"crystal":(781,631,867,715),"rune":(869,631,957,720),
"monarch_seal":(959,626,1056,719),"portal":(1062,622,1148,714),
"chest":(1155,629,1244,713),"insignia":(1259,632,1350,712),
"special":(1361,630,1490,715),
"next":(45,840,119,909),"close":(133,842,213,912),
"add":(227,844,307,911),"remove":(315,847,395,912),
"edit":(405,842,494,915),
}
# Aliases use identical original raster artwork, not rebuilt shapes.
ALIASES={
"book":"exercises","training":"strength","dumbbell":"strength",
"focus":"goals","energy":"crystal","lightning":"rune",
"shield":"insignia","achievement":"trophy","goal_strength":"strength",
"goal_habit":"goals","goal_read":"academy",
"calendar":"timer","history":"retry","rest":"pause",
"coach":"profile","search":"goals","export":"next","import":"back",
"notifications":"reminders","play":"start","complete":"success",
"delete":"remove","back_arrow":"back","goal_finish":"monarch_seal",
"rank":"insignia","stats":"progress","library":"exercises",
"monarch":"missions","programs":"academy"
}
def input_sheet()->Image.Image:
    req=Request(SHEET_URL,headers={"User-Agent":"HunterSystem-V43.10-PNG-assets/1.0"})
    with urlopen(req,timeout=45) as response:
        buf=response.read(4_000_001)
    if not buf.startswith(b"\x89PNG\r\n\x1a\n") or len(buf)>4_000_000:
        raise ValueError("Reference icon sheet is not a valid PNG")
    im=Image.open(BytesIO(buf)).convert("RGB")
    if im.size!=(1536,1024):
        raise ValueError("Icon sheet resolution changed; refuse incorrect crops")
    return im

def transparent_icon(sheet:Image.Image,region:tuple[int,int,int,int])->Image.Image:
    crop=sheet.crop(region)
    rgba=Image.new("RGBA",crop.size)
    pixels=[]
    for r,g,b in crop.getdata():
        peak=max(r,g,b)
        # Every original glyph glows above the dark card. Smooth-cut the
        # dark card's pixels; don't reconstruct, paint, or vectorize glyphs.
        opacity=max(0,min(255,round((peak-46)*255/100)))
        pixels.append((r,g,b,opacity))
    rgba.putdata(pixels)
    bbox=rgba.getbbox()
    if bbox is None:raise ValueError("Icon vanished during alpha isolation")
    rgba=rgba.crop(bbox)
    rgba.thumbnail((116,116),Image.Resampling.LANCZOS)
    result=Image.new("RGBA",(128,128),(0,0,0,0))
    result.alpha_composite(rgba,((128-rgba.width)//2,(128-rgba.height)//2))
    if result.getpixel((0,0))[3]!=0:
        raise ValueError("Transparent icon corner is not transparent")
    return result

def main()->None:
    sheet=input_sheet()
    OUT.mkdir(parents=True,exist_ok=True)
    # Old 32 icons had embedded square tiles; never leave those behind.
    for old in OUT.glob("*.png"):
        old.unlink()
    count=0
    for name,region in REGIONS.items():
        transparent_icon(sheet,region).save(OUT/f"{name}.png",optimize=True)
        count+=1
    for name,origin in ALIASES.items():
        (OUT/f"{name}.png").write_bytes((OUT/f"{origin}.png").read_bytes())
        count+=1
    assert count==len(REGIONS)+len(ALIASES)
    (OUT/"README.json").write_text(json.dumps({
        "source":SHEET_URL,"license":"Generated graphic approved by app owner",
        "source_asset":"biblioteca_de_íconos_hunter_system.png",
        "format":"PNG RGBA transparent, 128px","icons":count,
        "regions":list(REGIONS),"aliases":ALIASES
    },ensure_ascii=False,indent=2),encoding="utf-8")
    print(f"{count} original-design transparent PNG icons installed",flush=True)

if __name__=="__main__":main()

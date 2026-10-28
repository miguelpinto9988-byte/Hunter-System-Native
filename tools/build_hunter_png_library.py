#!/usr/bin/env python3
"""Raster-only UI artwork for Hunter System. Generates real PNG images from
existing original WebP scene artwork and CC BY 4.0 OpenEmoji PNGs.

This script is part of the Android build so that backgrounds and icons are
actually packaged as bitmaps, not SVG, Compose polygons, or remote URLs.
"""
from __future__ import annotations
import hashlib
import io
import json
from pathlib import Path
from urllib.request import Request, urlopen
from PIL import Image, ImageColor, ImageDraw, ImageEnhance, ImageFilter, ImageOps

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "app/src/main/assets"
OUTPUT = ASSETS / "hunter-png"
ICONS = OUTPUT / "icons"
WALLPAPERS = OUTPUT / "wallpapers"
# The WebP originals belong to this application already; no internet required.
WALLPAPER_SOURCES = {
    "home":("scene-s-0.webp","#122A53"),
    "plan":("scene-s-1.webp","#53163B"),
    "routines":("scene-e-1.webp","#3B2364"),
    "book":("scene-a-1.webp","#0B4E68"),
    "stats":("scene-d-0.webp","#12486D"),
    "objectives":("scene-c-0.webp","#5C3C6D"),
    "profile":("scene-c-1.webp","#3D2B72"),
    "recovery":("scene-b-1.webp","#64253C"),
}
# Semantic image icons from OpenEmoji (AI-illustrated PNGs, CC BY 4.0).\n# Source: https://github.com/profullstack/openemoji
EMOJI={
 "panel":"1f3e0","missions":"2694","routines":"1f4aa",
 "exercises":"1f3cb","progress":"1f4c8","profile":"1f9d1",
 "goals":"1f3af","academy":"1f4da","strength":"1f4aa",
 "cardio":"1f3c3","mobility":"1f9d8","book":"1f4d6",
 "level":"1f451","streak":"1f525","achievement":"1f396",
 "timer":"23f1","training":"1f6b4","dumbbell":"1f3cb",
 "focus":"1f3af","lightning":"26a1","energy":"2728",
 "shield":"1f6e1","milestone":"1f3c5","settings":"2699",
 "trophy":"1f3c6","success":"2705","reminders":"1f514",
 "xp":"1f48e","goal_strength":"1f4aa","goal_habit":"1f331",
 "goal_read":"1f4d6","warning":"26a0",
 "calendar":"1f4c5","history":"1f4dc","rest":"1f6cc",
 "coach":"1f393","search":"1f50e","export":"1f4be",
 "import":"1f4e5","menu":"1f5c3",
}
TINTS={"panel":"#38c7ff","missions":"#ed5f9b","routines":"#a27cff",
 "exercises":"#42d7ff","progress":"#5ee1d0","profile":"#b18aff",
 "goals":"#ffc96b","academy":"#8da8ff","strength":"#a47aff",
 "cardio":"#ffc86c","mobility":"#55ebcf","book":"#65c7fc",
 "level":"#c7a2ff","streak":"#ffae63","achievement":"#ffd67f",
 "timer":"#8cc9f5","warning":"#ff6c74","success":"#49ebc9"}
for folder in (ICONS,WALLPAPERS):folder.mkdir(parents=True,exist_ok=True)

def wallpaper(route:str,name:str,accent:str)->None:
    src=Image.open(ASSETS/"ui33"/name).convert("RGB")
    W,H=512,1024
    # Keep the original illustration recognizable in top 60% of the screen,
    # extend it softly behind cards, darken bottom for text accessibility.
    blurred=ImageOps.fit(src,(W,H),centering=(.5,.34)).filter(ImageFilter.GaussianBlur(22))
    foreground=ImageOps.fit(src,(W,645),centering=(.5,.29))
    mask=Image.new("L",(W,645))
    m=ImageDraw.Draw(mask)
    for y in range(645):
        m.line((0,y,W,y),fill=255 if y<335 else int(255*((645-y)/310)**1.5))
    blurred.paste(foreground,(0,0),mask)
    shade=Image.new("RGBA",(W,H))
    d=ImageDraw.Draw(shade);rgb=ImageColor.getrgb(accent)
    for y in range(H):
        dark=int(48+174*max(0,(y-260)/(H-260)))
        dark=max(40,min(240,dark))
        d.line((0,y,W,y),fill=(2,5,16,dark))
    final=Image.alpha_composite(blurred.convert("RGBA"),shade).convert("RGB")
    final=ImageEnhance.Contrast(final).enhance(1.11)
    final.quantize(colors=192,method=Image.Quantize.MEDIANCUT).save(
        WALLPAPERS/(route+".png"),optimize=True)

def emoji_png(code:str)->bytes:
    url=("https://raw.githubusercontent.com/profullstack/openemoji/"
         "main/png/128/"+code+".png")
    req=Request(url,headers={"User-Agent":"HunterSystem-APK-Build/43.9",
                             "Accept":"image/png"})
    with urlopen(req,timeout=25) as response:
        data=response.read(400_001)
    if len(data)>400_000 or data[:8]!=b"\x89PNG\r\n\x1a\n":
        raise ValueError("Invalid raster image "+url)
    return data

def icon(name:str,code:str,cache:dict)->None:
    if code not in cache:
        try:cache[code]=Image.open(io.BytesIO(emoji_png(code))).convert("RGBA")
        except Exception as error:
            print("Fallback OpenEmoji for",name,code,type(error).__name__,flush=True)
            cache[code]=Image.open(io.BytesIO(emoji_png("2728"))).convert("RGBA")
    original=cache[code]
    color=ImageColor.getrgb(TINTS.get(name,"#89abed"))
    base=Image.new("RGBA",(128,128),(0,0,0,0))
    draw=ImageDraw.Draw(base)
    draw.rounded_rectangle((3,3,124,124),radius=22,
        fill=(11,19,39,241),outline=(*color,150),width=2)
    symbol=ImageOps.contain(original,(95,95),Image.Resampling.LANCZOS)
    x=(128-symbol.width)//2;y=(128-symbol.height)//2-1
    glow=Image.new("RGBA",(128,128))
    glow.paste(symbol,(x,y),symbol)
    halo=glow.filter(ImageFilter.GaussianBlur(11))
    tint=Image.new("RGBA",(128,128),(*color,145))
    halo=Image.composite(tint,Image.new("RGBA",(128,128)),halo.getchannel("A"))
    base=Image.alpha_composite(base,halo)
    base.alpha_composite(symbol,(x,y))
    base.save(ICONS/(name+".png"),optimize=True)

def main()->None:
    records={}
    for route,(source,accent) in WALLPAPER_SOURCES.items():
        wallpaper(route,source,accent)
        path=WALLPAPERS/(route+".png")
        records[route]=hashlib.sha256(path.read_bytes()).hexdigest()
        print("wallpaper",route,path.stat().st_size,flush=True)
    cache={}
    for name,code in EMOJI.items():icon(name,code,cache)
    manifest={"wallpapers":list(WALLPAPER_SOURCES),"icons":list(EMOJI),
      "format":"PNG", "source":"Original app scene bitmap WebP + Noto Emoji PNG",
      "notoLicense":"https://github.com/googlefonts/noto-emoji/blob/main/LICENSE",
      "wallpaperSha256":records}
    (OUTPUT/"manifest.json").write_text(json.dumps(manifest,ensure_ascii=False,indent=2))
    assert len(list(WALLPAPERS.glob("*.png")))==8
    assert len(list(ICONS.glob("*.png")))==len(EMOJI)
    print("PNG wallpapers / pictorial icons ready:",len(records),len(EMOJI))

if __name__=="__main__":main()

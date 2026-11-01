#!/usr/bin/env python3
"""One-time read-only analysis of public Agenda Clara baseline asset integration."""
from io import BytesIO
from collections import Counter
from zipfile import ZipFile
from urllib.request import urlopen, Request
import re

url=("https://raw.githubusercontent.com/miguelpinto9988-byte/Agenda-Clara-V2/"
     "main/Agenda-Clara-1.3.0-Fuentes-Sin-Clave.zip")
request=Request(url,headers={"User-Agent":"HunterSystem-IconAudit/1.0"})
with urlopen(request,timeout=75) as response:
    data=response.read(35*1024*1024+1)
assert len(data)<35*1024*1024, "Refused excessive archive size"

with ZipFile(BytesIO(data)) as z:
    names=[p for p in z.namelist() if not p.endswith("/")]
    print("AGENDA_ARCHIVE_FILES",len(names))
    android_res=[p for p in names if "/src/main/res/" in p.replace("\\","/")]
    print("ANDROID_RES_COUNTS",dict(Counter(p.rsplit("/",2)[-2] for p in android_res)))
    print("ANDROID_PNG",*[p for p in android_res if p.lower().endswith(".png")][:95],sep="\n  ")
    print("ANDROID_DRAWABLE_XML",*[p for p in android_res if p.lower().endswith(".xml") and ("/drawable" in p or "/mipmap" in p)][:90],sep="\n  ")
    java=[p for p in names if p.endswith((".java",".kt")) and ("/src/" in p)]
    pattern=re.compile(r"R\\.(drawable|mipmap)\\.|setImageResource|setImageDrawable|ImageView|BitmapFactory|\.setImageBitmap|ImageButton|setCompoundDrawables|ic_[a-zA-Z_]|icons?/",re.I)
    print("JAVA_SOURCE_COUNT",len(java))
    seen=0
    for p in java:
        if seen>=120:break
        try: lines=z.read(p).decode("utf-8",errors="replace").splitlines()
        except Exception:continue
        matches=[(i+1,s.strip()) for i,s in enumerate(lines) if pattern.search(s)]
        if not matches:continue
        print("SOURCE",p)
        for i,s in matches[:12]:
            print(" L",i,":",s[:220])
            seen+=1
    print("END_AUDIT")

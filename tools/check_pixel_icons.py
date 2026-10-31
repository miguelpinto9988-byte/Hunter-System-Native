#!/usr/bin/env python3
"""Auditoría completa de la biblioteca Pixel Art PNG transparente V43.12."""
import hashlib, re, struct, subprocess, sys, tempfile, zlib
from pathlib import Path
R=Path(__file__).resolve().parents[1]
SOURCE=R/"app/src/main/java/com/huntersystem/app"
DRAW=R/"app/src/main/res/drawable-nodpi"
LIB=(SOURCE/"HunterPngLibrary.kt").read_text(encoding="utf8")
errors=[]
files=sorted(DRAW.glob("hunter_pixel_*.png"))
if len(files)!=49:errors.append(f"Se esperaban 49 PNG y hay {len(files)}")
for f in files+[DRAW/"app_icon_pixel.png",DRAW/"notification_icon_pixel.png"]:
    if not f.is_file():errors.append("Falta: "+f.name);continue
    raw=f.read_bytes()
    if raw[:8]!=b"\x89PNG\r\n\x1a\n":errors.append("Firma PNG inválida: "+f.name);continue
    width,height,depth,ctype=struct.unpack(">IIBB",raw[16:26])
    if (width,height,depth,ctype)!=(256,256,8,6):errors.append(f"PNG no es RGBA 256: {f.name}")
    pos=8;parts=[]
    while pos<len(raw):
        n=int.from_bytes(raw[pos:pos+4],"big");kind=raw[pos+4:pos+8]
        if kind==b"IDAT":parts.append(raw[pos+8:pos+8+n])
        pos+=12+n
    try:
        scan=zlib.decompress(b"".join(parts))
        if not any(scan[row*(1+4*width)+1+4*x+3]==0 for row in range(height) for x in range(width)):
            errors.append("El fondo no es transparente: "+f.name)
        if not any(scan[row*(1+4*width)+1+4*x+3]>0 for row in range(height) for x in range(width)):
            errors.append("Icono totalmente invisible: "+f.name)
    except Exception as e:errors.append(f"PNG inválido {f.name}: {e}")
mapping=dict(re.findall(r'"([a-z_]+)"\s+to\s+"([a-z_]+)"',LIB))
native=dict(re.findall(r'"([a-z_]+)"\s+to\s+R\.drawable\.hunter_pixel_([a-z_]+)',LIB))
if len(mapping)!=76:errors.append(f"Claves funcionales: {len(mapping)}, esperadas 76")
if len(native)!=49:errors.append(f"Recursos PNG: {len(native)}, esperados 49")
for key,name in mapping.items():
    if name not in native or not (DRAW/("hunter_pixel_"+name+".png")).is_file():
        errors.append(f"Sin PNG: {key} -> {name}")
if 'bitmap=ImageBitmap.imageResource(id=hunterIconDrawableId(key))' not in LIB or 'filterQuality=FilterQuality.None' not in LIB:
    errors.append("Compose no carga PNG pixel-perfect")
if 'R.drawable.notification_icon_pixel' not in (SOURCE/"HunterAlarms.kt").read_text():
    errors.append("Las alarmas no usan PNG nuevo")
if '@drawable/app_icon_pixel' not in (R/"app/src/main/AndroidManifest.xml").read_text():
    errors.append("Launcher no usa PNG nuevo")
# El generador es determinista. Valida correspondencia byte a byte.
with tempfile.TemporaryDirectory() as tmp:
    ret=subprocess.run([sys.executable,str(R/"tools/build_hunter_pixel_icons.py"),"--output",tmp,"--scale","4","--verify"],capture_output=True,text=True)
    if ret.returncode:errors.append("Generador falló: "+ret.stderr[-400:])
    else:
        for f in files:
            expected=Path(tmp)/f.name
            if hashlib.sha256(expected.read_bytes()).digest()!=hashlib.sha256(f.read_bytes()).digest():
                errors.append("Fuente y recurso divergen: "+f.name)
if errors:
    print("\n".join("ERROR: "+e for e in errors));sys.exit(1)
print("OK: 49 PNG RGBA transparentes, 76 alias, generador reproducible, Compose, launcher y notificaciones")

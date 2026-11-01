#!/usr/bin/env python3
"""Audita los 57 PNG originales y transparentes de Hunter System V43.12.

No genera ni pinta iconos: comprueba los recursos PNG ya integrados en Android.
"""
from pathlib import Path
import re
import struct
import sys
import zlib

ROOT = Path(__file__).resolve().parents[1]
DRAW = ROOT / "app/src/main/res/drawable-nodpi"
SOURCE = ROOT / "app/src/main/java/com/huntersystem/app"
errors = []


def paeth(a, b, c):
    p = a + b - c
    da, db, dc = abs(p-a), abs(p-b), abs(p-c)
    return a if da <= db and da <= dc else b if db <= dc else c


def alpha_summary(path):
    data = path.read_bytes()
    if not data.startswith(b"\x89PNG\r\n\x1a\n"):
        raise ValueError("firma PNG inválida")
    offset = 8
    chunks = []
    width = height = depth = color = None
    while offset + 12 <= len(data):
        size = int.from_bytes(data[offset:offset+4], "big")
        kind = data[offset+4:offset+8]
        payload = data[offset+8:offset+8+size]
        if kind == b"IHDR":
            width, height, depth, color = struct.unpack(">IIBB", payload[:10])
        if kind == b"IDAT":
            chunks.append(payload)
        offset += 12 + size
        if kind == b"IEND":
            break
    if (width, height, depth, color) != (256, 256, 8, 6):
        raise ValueError(f"se requiere RGBA de 256 px; recibido {(width,height,depth,color)}")
    raw = zlib.decompress(b"".join(chunks))
    stride = width * 4
    if len(raw) != height * (stride+1):
        raise ValueError("datos PNG truncados o tamaño de scanline erróneo")
    prev = bytearray(stride)
    zero = visible = False
    corner = None
    offset = 0
    for y in range(height):
        filt = raw[offset]
        offset += 1
        row = bytearray(raw[offset:offset+stride])
        offset += stride
        for i in range(stride):
            a = row[i-4] if i >= 4 else 0
            b = prev[i]
            c = prev[i-4] if i >= 4 else 0
            delta = (0 if filt == 0 else a if filt == 1 else b if filt == 2 else
                     (a+b)//2 if filt == 3 else paeth(a,b,c) if filt == 4 else None)
            if delta is None:
                raise ValueError(f"filtro PNG {filt} no permitido")
            row[i] = (row[i] + delta) & 255
        if y == 0:
            corner = row[3]
        for v in row[3::4]:
            zero |= v == 0
            visible |= v > 0
        prev = row
    if corner != 0:
        raise ValueError("la esquina superior izquierda no es transparente")
    if not zero or not visible:
        raise ValueError("icono completamente opaco o invisible")


icons = sorted(DRAW.glob("hunter_pixel_*.png"))
selected = sorted(DRAW.glob("hunter_nav_*.png"))
if len(icons) != 49:
    errors.append(f"49 iconos funcionales requeridos, hallados {len(icons)}")
if len(selected) != 6:
    errors.append(f"6 variantes de navegación requeridas, halladas {len(selected)}")
for path in icons + selected + [DRAW/"app_icon_pixel.png",DRAW/"notification_icon_pixel.png"]:
    if not path.is_file():
        errors.append("Falta " + path.name)
        continue
    try:
        alpha_summary(path)
    except Exception as e:
        errors.append(f"{path.name}: {e}")

library = (SOURCE/"HunterPngLibrary.kt").read_text(encoding="utf-8")
mapping = dict(re.findall(r'"([a-z_]+)"\s+to\s+"([a-z_]+)"', library))
native = dict(re.findall(r'"([a-z_]+)"\s+to\s+R\.drawable\.hunter_pixel_([a-z_]+)', library))
if len(mapping) != 76:
    errors.append(f"Se esperan 76 claves semánticas, hay {len(mapping)}")
if len(native) != 49:
    errors.append(f"Se esperan 49 recursos PNG, hay {len(native)}")
for key, name in mapping.items():
    if name not in native or not (DRAW/("hunter_pixel_"+name+".png")).is_file():
        errors.append(f"Falta PNG semántico {key}: {name}")
for name in ("home","quest","calendar","dumbbell","chart","profile"):
    if f"R.drawable.hunter_nav_{name}" not in library:
        errors.append("Variante seleccionada no mapeada: " + name)
if "AndroidView(" not in library or "view.setImageResource(drawableId)" not in library:
    errors.append("La iconografía debe usar ImageView nativo con R.drawable, igual que Agenda Clara")
if "setFilterBitmap(false)" not in library:
    errors.append("El escalado de los PNG no preserva los bordes Pixel Art")
if "view.imageTintList = null" not in library:
    errors.append("Los tintes podrían modificar los colores originales de los PNG")
if "HunterNativePngImage(hunterIconDrawableId(key), modifier, contentDescription)" not in library:
    errors.append("Las claves funcionales no están conectadas al visor PNG nativo")
if "hunterNavigationDrawableId(key, selected)" not in library:
    errors.append("La navegación seleccionada no carga sus PNG correspondientes")
navigation = (SOURCE/"MonarchNavigationV42.kt").read_text(encoding="utf-8")
if "HunterNavigationPngIcon(monarchPngDestination(destination.id), selected" not in navigation:
    errors.append("La navegación no usa PNG normal y seleccionado")
glyph = (SOURCE/"MonarchV42.kt").read_text(encoding="utf-8")
if "Canvas(" in glyph or "HunterPngIcon(monarchGlyphPixelKey(kind), modifier, kind)" not in glyph:
    errors.append("Los accesos rápidos del Panel deben usar PNG, no Canvas")
if "R.drawable.notification_icon_pixel" not in (SOURCE/"HunterAlarms.kt").read_text():
    errors.append("Las notificaciones no usan el PNG")
if '@drawable/app_icon_pixel' not in (ROOT/"app/src/main/AndroidManifest.xml").read_text():
    errors.append("El icono launcher no usa PNG")
if errors:
    print("\n".join("ERROR: " + e for e in errors))
    sys.exit(1)
print("OK: 49 PNG RGBA + 6 variantes seleccionadas + iconos launcher/notificación; 76 claves y alfa auténtico.")

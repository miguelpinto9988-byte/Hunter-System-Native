#!/usr/bin/env python3
"""Single source for transparent SVG and Android vectors; no bitmap conversion."""
from pathlib import Path
from xml.sax.saxutils import escape
ROOT=Path(__file__).resolve().parents[1]
# Each icon: structural outline, contrasting detail. 24-unit optical grid.
ICONS={
'home':('M3 10L12 3L21 10V20H15V14H9V20H3Z','M9 9H15'),
'quest':('M6 4H18V21H6ZM9 4V2H15V4','M9 10L11 12L15 8M9 16H15'),
'calendar':('M4 5H20V21H4ZM4 10H20M8 3V7M16 3V7','M8 14H10M14 14H16M8 18H10'),
'chart':('M4 3V21H21','M8 17V12M13 17V8M18 17V4'),
'profile':('M8 7A4 4 0 1 0 16 7A4 4 0 1 0 8 7M4 21V19C4 15 20 15 20 19V21','M10 20H14'),
'dumbbell':('M3 8H7V16H3ZM17 8H21V16H17ZM7 12H17','M1 10V14M23 10V14'),
'crown':('M3 7L7 11L12 3L17 11L21 7L19 20H5Z','M6 16H18M11 12L12 11L13 12L12 13Z'),
'crystal':('M12 2L20 8L17 19L12 22L7 19L4 8Z','M4 8H20M12 2L8 8L12 22L16 8Z'),
'portal':('M5 20V9C5 0 19 0 19 9V20M3 21H21','M9 18V10C9 6 15 6 15 10V18'),
'chest':('M3 11V8C3 3 21 3 21 8V11ZM3 11H21V21H3ZM10 10H14V15H10Z','M7 6V10M17 6V10M7 15V19M17 15V19'),
'shield':('M12 2L21 6V12C21 17 16 21 12 22C8 21 3 17 3 12V6Z','M8 12L11 15L17 9'),
'swords':('M4 2L8 3L19 16L16 19L3 8ZM3 17L7 21M5 19L2 22','M20 2L16 3L13 7M11 13L5 19M17 17L21 21M19 19L22 22'),
'heart':('M12 21L4 13C-2 6 7 0 12 7C17 0 26 6 20 13Z','M6 12H9L11 9L13 15L15 12H18'),
'fire':('M13 2C14 8 21 9 20 15C20 24 4 24 4 15C4 10 8 8 8 5L11 9Z','M12 12C12 15 9 15 9 18C9 22 16 22 16 18C16 16 14 15 12 12Z'),
'lightning':('M14 2L4 14H11L10 22L21 9H13Z','M13 6L9 11'),
'book':('M12 6C9 3 5 3 2 4V20C6 19 9 20 12 22C15 20 18 19 22 20V4C18 3 15 3 12 6ZM12 6V22','M5 8L9 9M5 12L9 13M15 9L19 8M15 13L19 12'),
'checklist':('M6 3H20V21H4V7','M2 4L4 6L8 2M8 11H16M8 16H16'),
'leaf':('M20 3C4 1 0 12 7 18C14 24 23 14 20 3Z','M4 22L16 8M9 17V11M13 13H18'),
'rest':('M20 15C10 20 3 9 10 3C-1 5 0 21 12 21C16 21 19 19 20 15Z','M16 3V7M14 5H18'),
'clock':('M3 12A9 9 0 1 0 21 12A9 9 0 1 0 3 12','M12 6V12L16 14'),
'bell':('M5 17V10C5 1 19 1 19 10V17L21 19H3ZM9 22H15','M12 2V4M8 10V13'),
'gear':('M9 3H15L16 6L19 7L22 11L20 14V18L15 21L12 20L9 21L4 18V14L2 11L5 7L8 6ZM8 12A4 4 0 1 0 16 12A4 4 0 1 0 8 12','M11 12H13'),
'sliders':('M3 6H8M14 6H21M3 18H12M18 18H21','M8 3H14V9H8ZM12 15H18V21H12Z'),
'chat':('M3 3H21V17H10L4 22V17H3Z','M7 8H17M7 12H14'),
'trophy':('M7 3H17V10C17 17 7 17 7 10ZM7 6H3V9C3 12 5 13 8 13M17 6H21V9C21 12 19 13 16 13M12 15V21M7 21H17','M10 7H14'),
'star':('M12 2L15 8L22 9L17 14L18 21L12 18L6 21L7 14L2 9L9 8Z','M12 7L13 10L16 11'),
'target':('M20 12A8 8 0 1 1 12 4M16 12A4 4 0 1 1 12 8','M12 12L21 3M16 3H21V8'),
'lock':('M5 10H19V22H5ZM8 10V6C8 0 16 0 16 6V10','M12 15V18'),
'warning':('M12 3L22 21H2Z','M12 9V14M12 17V18'),
'search':('M3 10A7 7 0 1 0 17 10A7 7 0 1 0 3 10M15 15L22 22','M6 10C6 8 8 6 10 6'),
'edit':('M4 16L17 3L21 7L8 20L3 21ZM14 6L18 10','M12 21H21'),
'trash':('M5 6H19L18 22H6ZM3 6H21M9 6V2H15V6','M10 10V18M14 10V18'),
'check':('M4 12L10 18L21 6',''),
'close':('M5 5L19 19M19 5L5 19',''),
'add':('M4 12H20M12 4V20',''),
'remove':('M4 12H20',''),
'back':('M13 4L5 12L13 20M5 12H21',''),
'next':('M11 4L19 12L11 20M3 12H19',''),
'play':('M7 3L21 12L7 21Z','M10 9L15 12'),
'pause':('M6 4H9V20H6ZM15 4H18V20H15Z',''),
'refresh':('M20 8C16 0 3 3 3 12C3 21 16 24 21 16','M20 3V8H15'),
'upload':('M3 15V21H21V15','M12 16V3M7 8L12 3L17 8'),
'download':('M3 15V21H21V15','M12 3V16M7 11L12 16L17 11'),
'week_upper':('M3 15L5 7L9 5L10 9L8 12C13 9 17 10 21 13V19H5Z','M13 13C15 12 17 13 18 15'),
'week_core':('M7 2L4 8L7 12L6 22H18L17 12L20 8L17 2','M12 7V20M9 10H15M9 14H15M9 18H15'),
'week_lower':('M6 3H18L17 12L20 21H14L12 14L10 21H4L7 12Z','M7 7H17M12 7V11'),
'week_mobility':('M10 4A2 2 0 1 0 14 4A2 2 0 1 0 10 4M4 10L12 8L20 10M12 8V15L5 21M12 15L19 21','M3 4L2 8M21 4L22 8'),
'week_rest':('M19 16C10 20 4 10 10 3C0 6 2 21 13 21C16 21 18 19 19 16Z','M16 4H21L16 9H21'),
'week_cardio':('M8 4A2 2 0 1 0 12 4A2 2 0 1 0 8 4M3 12L7 8L12 10L16 7M10 10L9 15L4 21M9 15L15 16L18 21','M17 11H22M18 14H21')}

def generate():
    svgdir=ROOT/'app/src/main/assets/hunter-icons'
    drawdir=ROOT/'app/src/main/res/drawable'
    for name,parts in ICONS.items():
        paths=[]; native=[]
        for d,color in zip(parts,('#A8DCFF','#A99AFF')):
            if not d: continue
            paths.append(f'<path d="{d}" fill="none" stroke="{color}" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>')
            native.append(f'  <path android:pathData="{d}" android:fillColor="#00000000" android:strokeColor="{color}" android:strokeWidth="1.8" android:strokeLineCap="round" android:strokeLineJoin="round"/>')
        (svgdir/f'{name}.svg').write_text(f'<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24"><title>{escape(name)}</title>'+''.join(paths)+'</svg>\n')
        (drawdir/f'hunter_icon_{name}.xml').write_text('<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">\n'+'\n'.join(native)+'\n</vector>\n')
    print(f'Generated {len(ICONS)} matching SVG / VectorDrawable pairs')
if __name__=='__main__': generate()

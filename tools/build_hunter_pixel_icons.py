#!/usr/bin/env python3
"""Hunter System V43.12 — pixel art PNG RGBA built with Python standard library.

Original symbolic pixel art (not traced from third-party anime). All images are
transparent, offline, deterministic, and generated without network access.
"""
from __future__ import annotations
import argparse
import binascii
import re
import struct
import zlib
from pathlib import Path

S = 64
PALETTES = {
    'blue': ('#092443','#1364bc','#20c7ff','#ddfaff'),
    'purple': ('#261047','#6a2dc8','#c35aff','#f6ceff'),
    'gold': ('#55300d','#bd6c18','#ffc443','#fff3a1'),
    'red': ('#4b1326','#c72c62','#ff6789','#ffe1d7'),
    'green': ('#0b3c35','#1c9a85','#41f2b9','#e5fff1'),
    'silver': ('#212d48','#687f9e','#c3d9f4','#ffffff'),
    'orange': ('#562315','#d56828','#ffac37','#fff0a9'),
}
GROUPS = {
    'blue': 'home dumbbell chart download upload search clock pause play refresh next back week_upper',
    'purple': 'crown crystal portal profile quest book star swords sliders chat week_mobility',
    'gold': 'chest trophy calendar bell gear lock lightening lightning target add week_rest',
    'red': 'heart fire warning trash close remove week_lower week_cardio',
    'green': 'check checklist leaf rest week_core',
    'silver': 'edit shield',
}
NAMES = '''add back bell book calendar chart chat check checklist chest clock close crown crystal download dumbbell edit fire gear heart home leaf lightning lock next pause play portal profile quest refresh remove rest search shield sliders star swords target trash trophy upload warning week_cardio week_core week_lower week_mobility week_rest week_upper'''.split()
assert len(NAMES) == 49
GROUP_LOOKUP = {name: family for family, names in GROUPS.items() for name in names.split()}

def rgba(h: str, a: int = 255):
    h = h.lstrip('#')
    return (int(h[:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)

def packet(kind: bytes, raw: bytes):
    return struct.pack('!I', len(raw)) + kind + raw + struct.pack('!I', binascii.crc32(kind + raw) & 0xffffffff)

def png_bytes(pixels, scale: int):
    width = S * scale
    scan = bytearray()
    for y in range(width):
        scan.append(0)
        row = pixels[(y // scale) * S:(y // scale + 1) * S]
        for color in row:
            scan.extend(bytes(color) * scale)
    return b'\x89PNG\r\n\x1a\n' + packet(b'IHDR', struct.pack('!2I5B', width, width, 8, 6, 0, 0, 0)) + packet(b'IDAT', zlib.compress(bytes(scan), 9)) + packet(b'IEND', b'')

class Art:
    def __init__(self, family):
        self.pixels = [(0,0,0,0)] * (S*S)
        self.dark, self.mid, self.light, self.shine = map(rgba, PALETTES[family])
    def put(self,x,y,c):
        x=int(x); y=int(y)
        if 0<=x<S and 0<=y<S:
            if c[3]==255:
                self.pixels[y*S+x]=c
            elif c[3]:
                under=self.pixels[y*S+x]
                a=c[3]/255
                self.pixels[y*S+x]=tuple(round(c[k]*a+under[k]*(1-a)) for k in range(3))+(min(255,c[3]+round(under[3]*(1-a))),)
    def rect(self,x0,y0,x1,y1,c):
        for y in range(max(0,int(y0)),min(S,int(y1)+1)):
            for x in range(max(0,int(x0)),min(S,int(x1)+1)):self.put(x,y,c)
    def line(self,x0,y0,x1,y1,c,w=1):
        dx=abs(int(x1-x0)); dy=abs(int(y1-y0)); x=int(x0);y=int(y0)
        sx=1 if x0<x1 else -1;sy=1 if y0<y1 else -1;err=dx-dy
        while True:
            self.rect(x-w//2,y-w//2,x+(w-1)//2,y+(w-1)//2,c)
            if x==int(x1) and y==int(y1):break
            e=2*err
            if e>-dy:err-=dy;x+=sx
            if e<dx:err+=dx;y+=sy
    def ellipse(self,x0,y0,x1,y1,c):
        mx=(x0+x1)/2;my=(y0+y1)/2;rx=max(.1,(x1-x0)/2);ry=max(.1,(y1-y0)/2)
        for y in range(max(0,int(y0)),min(64,int(y1)+1)):
            for x in range(max(0,int(x0)),min(64,int(x1)+1)):
                if ((x-mx)/rx)**2+((y-my)/ry)**2<=1:self.put(x,y,c)
    def polygon(self,points,c):
        left=max(0,int(min(x for x,y in points)));right=min(63,int(max(x for x,y in points)))
        top=max(0,int(min(y for x,y in points)));bottom=min(63,int(max(y for x,y in points)))
        for y in range(top,bottom+1):
            for x in range(left,right+1):
                inside=False
                for i in range(len(points)):
                    x1,y1=points[i]; x2,y2=points[(i+1)%len(points)]
                    if (y1>y)!=(y2>y) and x<(x2-x1)*(y-y1)/(y2-y1)+x1:inside=not inside
                if inside:self.put(x,y,c)
    def polyline(self,points,c,w=2):
        for a,b in zip(points,points[1:]):self.line(*a,*b,c,w)
    def bevel(self,points):
        self.polygon([(x+1,y+2) for x,y in points],self.dark)
        self.polygon(points,self.mid)
        self.polyline(points+[points[0]],self.dark,3)
        self.polyline(points[:max(2,len(points)//2)],self.light,2)
    def spark(self,x,y):
        self.line(x-3,y,x+3,y,self.light)
        self.line(x,y-3,x,y+3,self.shine)
    def aura(self):
        for radius,alpha in ((30,12),(25,17),(21,22)):
            light=self.light[:3]+(alpha,)
            self.ellipse(32-radius,32-radius,32+radius,32+radius,light)


def draw(name: str):
    family=GROUP_LOOKUP.get(name,'purple'); a=Art(family)
    a.aura(); D,M,L,H=a.dark,a.mid,a.light,a.shine
    # Reusable symbols are drawn on a true transparent canvas; no backing tiles.
    if name in ('add','remove','close','check','next','back','play','pause','refresh','upload','download'):
        if name in ('add','remove','close'):
            a.bevel([(17,18),(46,18),(49,21),(49,46),(46,49),(18,49),(15,46),(15,21)])
            a.line(23,33,41,33,H,5)
            if name=='add':a.line(32,24,32,42,H,5)
            if name=='close':a.line(24,24,40,41,D,5);a.line(40,24,24,41,H,5)
        elif name=='check':a.polyline([(13,33),(26,45),(51,19)],D,12);a.polyline([(13,32),(26,43),(51,17)],M,8);a.polyline([(13,30),(26,40),(50,16)],H,3)
        elif name in ('next','back'):
            pts=[(12,26),(36,26),(36,17),(54,32),(36,47),(36,38),(12,38)]
            if name=='back':pts=[(64-x,y) for x,y in pts]
            a.bevel(pts)
        elif name=='play':a.bevel([(21,12),(52,31),(21,52)])
        elif name=='pause':
            for x in (16,35):a.bevel([(x,15),(x+13,15),(x+13,49),(x,49)])
        elif name=='refresh':
            a.ellipse(12,12,52,52,D);a.ellipse(17,17,47,47,L);a.ellipse(23,23,41,41,(0,0,0,0))
            a.bevel([(43,9),(56,22),(39,24)])
        else:
            a.rect(13,39,51,47,D);a.rect(16,41,48,45,L)
            upward=name=='upload'
            a.line(32,16 if upward else 12,32,40 if upward else 36,M,8)
            p=[(18,29),(32,13),(46,29)] if upward else [(18,29),(32,46),(46,29)]
            a.bevel(p)
    elif name in ('home',):
        a.bevel([(9,31),(32,10),(55,31),(49,31),(49,52),(16,52),(16,31)])
        a.rect(27,36,37,51,D);a.rect(29,38,35,50,L);a.line(18,30,32,18,H,3)
    elif name in ('quest','book','checklist','calendar'):
        if name=='quest':
            a.bevel([(12,13),(52,13),(47,21),(50,43),(43,49),(15,49),(20,41),(15,23)])
            a.ellipse(26,25,38,37,D);a.ellipse(29,28,35,34,H);a.spark(32,31)
        elif name=='book':
            a.bevel([(8,16),(28,11),(32,17),(36,11),(56,16),(56,49),(36,47),(32,52),(28,47),(8,49)])
            a.line(32,17,32,48,H,3);a.line(14,23,26,20,H,2);a.line(39,20,50,23,L,2)
        elif name=='calendar':
            a.bevel([(13,16),(50,16),(53,51),(11,51)])
            a.rect(15,21,49,29,D)
            for x in (22,32,42):a.rect(x-2,36,x+2,40,H)
            a.rect(18,13,22,21,H);a.rect(42,13,46,21,H)
        else:
            a.bevel([(13,13),(52,13),(52,53),(13,53)])
            for y in (22,33,44):
                a.rect(18,y,23,y+5,D);a.line(18,y+2,20,y+4,H,2);a.line(20,y+4,25,y-1,L,2)
                a.line(30,y+2,45,y+2,H,2)
    elif name in ('dumbbell','week_upper','week_lower','week_core','week_cardio','week_mobility','week_rest'):
        if name=='dumbbell' or name=='week_upper':
            a.line(11,34,53,30,D,9);a.line(11,32,53,29,H,4)
            for x in (14,21,41,48):a.bevel([(x-4,20),(x+4,20),(x+5,44),(x-3,44)])
        elif name=='week_lower':
            a.bevel([(20,13),(35,13),(38,29),(47,43),(43,53),(32,52),(28,42),(19,35)])
            a.line(34,26,27,41,H,4)
        elif name=='week_core':
            a.bevel([(22,12),(43,12),(48,22),(45,43),(38,53),(26,53),(17,43),(16,22)])
            for x in (24,35):
                for y in (26,34,42):a.rect(x,y,x+6,y+4,H)
        elif name=='week_cardio':
            a.polyline([(10,41),(23,41),(27,24),(33,48),(40,32),(54,32)],D,8)
            a.polyline([(10,40),(23,40),(27,22),(33,45),(40,30),(54,30)],L,4)
        elif name=='week_mobility':
            a.ellipse(15,13,49,47,D);a.ellipse(20,18,44,42,M)
            a.line(13,43,50,18,H,6)
            a.spark(50,16)
        elif name=='week_rest':
            a.ellipse(15,10,48,47,H);a.ellipse(27,5,55,36,(0,0,0,0))
            a.spark(42,43)
    elif name in ('chart','target','trophy','star','crown','shield','swords','crystal','portal','chest','lock','lightning'):
        if name=='lightning':
            a.bevel([(36,7),(17,35),(30,35),(25,57),(52,25),(37,25)])
            a.line(33,16,26,30,H,3)
        elif name=='chart':
            for j,(height,color) in enumerate(((19,M),(28,L),(38,H))):
                x=13+j*14;a.rect(x,51-height,x+9,51,D);a.rect(x+2,53-height,x+7,49,color)
            a.polyline([(13,40),(26,33),(38,30),(50,13)],H,3)
        elif name=='target':
            for x,y,z in ((13,13,51),(20,20,44),(28,28,36)):
                a.ellipse(x,y,z,z,D);a.ellipse(x+2,y+2,z-2,z-2, M if x!=28 else H)
            a.line(37,26,52,11,H,3)
        elif name=='trophy':
            a.bevel([(17,12),(48,12),(44,39),(38,44),(38,49),(45,49),(45,54),(19,54),(19,49),(26,49),(26,44),(21,39)])
            a.ellipse(9,17,22,35,D);a.ellipse(43,17,56,35,D);a.spark(34,23)
        elif name=='star':
            a.bevel([(32,7),(39,25),(57,26),(43,38),(48,56),(32,46),(16,56),(21,38),(7,26),(25,25)])
        elif name=='crown':
            a.bevel([(9,19),(20,30),(28,11),(35,28),(51,17),(47,48),(15,48)])
            a.rect(17,40,45,46,D)
            for x,y in ((20,31),(32,22),(43,30)):a.rect(x-2,y-2,x+2,y+2,H)
        elif name=='shield':
            a.bevel([(32,8),(54,19),(51,40),(32,55),(13,40),(10,19)])
            a.polyline([(20,32),(28,40),(44,24)],H,4)
        elif name=='swords':
            for start,end in (((12,12),(52,52)),((52,12),(12,52))):
                a.line(*start,*end,D,9)
                a.line(*start,*end,H,4)
            a.line(11,42,24,55,M,5);a.line(53,42,40,55,M,5)
        elif name=='crystal':
            for pts in [[(32,7),(44,26),(35,54),(20,30)],[(15,25),(29,15),(26,51),(12,47)],[(39,21),(52,30),(50,48),(35,54)]]:a.bevel(pts)
            a.line(32,13,34,42,H,3)
        elif name=='portal':
            a.ellipse(8,8,56,56,D);a.ellipse(12,12,52,52,M);a.ellipse(18,16,46,50,L);a.ellipse(23,21,41,45,D)
            a.polyline([(25,24),(33,18),(39,28)],H,3)
        elif name=='chest':
            a.bevel([(9,23),(14,15),(50,15),(55,23),(54,50),(10,50)])
            a.rect(11,29,53,35,D);a.rect(29,26,35,43,H);a.rect(30,29,34,34,D)
        else:
            a.bevel([(17,28),(17,20),(22,12),(32,10),(42,12),(47,20),(47,28),(51,28),(51,53),(13,53),(13,28)])
            a.rect(23,29,41,49,D);a.ellipse(28,33,36,42,H)
    elif name in ('gear','sliders','clock','bell','fire','heart','leaf','rest'):
        if name=='gear':
            a.ellipse(13,13,51,51,D);a.ellipse(17,17,47,47,M)
            for x,y in ((28,8),(28,49),(8,29),(48,29),(14,14),(45,45),(45,14),(14,45)):a.rect(x,y,x+8,y+8,L)
            a.ellipse(25,25,39,39,D);a.ellipse(29,29,35,35,H)
        elif name=='sliders':
            for x,y in ((19,22),(32,38),(45,27)):
                a.line(x,10,x,54,H,3);a.bevel([(x-7,y-5),(x+7,y-5),(x+7,y+5),(x-7,y+5)])
        elif name=='clock':
            a.ellipse(9,9,55,55,D);a.ellipse(13,13,51,51,M);a.ellipse(19,19,45,45,D)
            a.line(32,31,32,18,H,4);a.line(32,31,43,38,H,4)
        elif name=='bell':
            a.bevel([(18,40),(21,31),(21,23),(25,16),(32,13),(39,16),(43,23),(43,31),(46,40),(51,45),(13,45)])
            a.ellipse(27,44,38,53,H)
        elif name=='fire':
            a.bevel([(29,56),(16,46),(18,34),(12,27),(29,8),(31,29),(43,17),(47,35),(52,44),(41,54)])
            a.polygon([(29,48),(26,39),(35,29),(40,44),(34,51)],H)
        elif name=='heart':
            a.bevel([(32,55),(10,35),(10,22),(17,15),(26,15),(32,22),(38,15),(47,15),(54,22),(54,35)])
            a.polyline([(16,35),(25,35),(30,28),(35,42),(39,34),(49,34)],H,3)
        elif name=='leaf':
            a.bevel([(9,47),(13,24),(30,10),(55,8),(52,35),(33,51)])
            a.polyline([(14,51),(31,33),(48,18)],H,4)
        else:
            a.bevel([(12,32),(19,16),(31,8),(43,16),(52,32),(43,48),(31,56),(19,48)])
            a.ellipse(24,20,42,40,M);a.spark(26,20)
    elif name in ('profile','chat','search','warning','trash','edit'):
        if name=='profile':
            a.ellipse(23,10,42,32,D);a.ellipse(25,11,40,29,L)
            a.bevel([(12,53),(14,42),(22,34),(42,34),(50,42),(52,53)])
            a.rect(22,42,43,47,H)
        elif name=='chat':
            a.bevel([(11,12),(53,12),(53,43),(35,43),(23,53),(23,43),(11,43)])
            for x in (21,32,43):a.rect(x-2,27,x+2,31,H)
        elif name=='search':
            a.ellipse(10,9,43,43,D);a.ellipse(14,13,39,39,L);a.ellipse(20,19,33,33,D)
            a.line(38,39,54,55,D,9);a.line(39,39,52,52,H,5)
        elif name=='warning':
            a.bevel([(32,8),(57,53),(7,53)])
            a.rect(29,24,35,40,D);a.rect(29,44,35,49,D)
        elif name=='trash':
            a.bevel([(17,19),(47,19),(44,53),(20,53)])
            a.rect(13,15,50,21,H);a.rect(24,10,40,16,D)
            for x in (25,33,40):a.line(x,28,x,46,H,2)
        else:
            a.line(14,49,50,13,D,12);a.line(14,47,47,14,H,6)
            a.bevel([(12,49),(21,42),(25,48),(15,54)])
    else:
        raise ValueError(name)
    # Sparse sparks give the family its Eclipse energy, without backing frames.
    if name not in ('check','close','warning','trash'):
        a.spark(52,10)
    return a.pixels


def main():
    p=argparse.ArgumentParser()
    p.add_argument('--output',type=Path,default=Path('app/src/main/res/drawable-nodpi'))
    p.add_argument('--scale',type=int,default=4,choices=(1,2,4,8,16))
    p.add_argument('--verify',action='store_true')
    args=p.parse_args()
    args.output.mkdir(parents=True,exist_ok=True)
    for name in NAMES:
        raw=png_bytes(draw(name),args.scale)
        (args.output/f'hunter_pixel_{name}.png').write_bytes(raw)
    if args.verify:
        icons=list(args.output.glob('hunter_pixel_*.png'))
        assert len(icons)==49,len(icons)
        for f in icons:
            data=f.read_bytes()
            assert data[:8]==b'\x89PNG\r\n\x1a\n' and data[25]==6
        print(f'OK: 49 PNG RGBA {S*args.scale}px, transparentes; {sum(f.stat().st_size for f in icons)} bytes')

if __name__=='__main__':main()
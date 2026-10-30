#!/usr/bin/env python3
"""Restaura los 24 WebP de evolución desde cuatro láminas RGBA 724x2172."""
import argparse
from pathlib import Path
from PIL import Image

BOXES={
"reader":[(0,0,211,732),(211,0,445,732),(445,0,724,732),
          (0,732,346,1433),(346,732,724,1433),(0,1433,724,2172)],
"regressor":[(0,0,724,285),(0,282,724,613),(0,615,724,970),
             (0,966,724,1310),(0,1301,724,1710),(0,1698,724,2172)],
"hunter-male":[(0,0,352,617),(335,0,724,617),
               (0,615,356,1327),(339,615,724,1327),
               (0,1325,360,2172),(300,1325,724,2172)],
"hunter-female":[(0,0,724,374),(0,362,724,712),(0,649,724,1048),
                 (0,1037,724,1416),(0,1393,724,1800),(0,1796,724,2172)]
}

def process(source:Path,character:str,target:Path):
 with Image.open(source) as src:
  image=src.convert("RGBA")
 w,h=image.size
 if abs(w/h-1/3)>.03: raise ValueError(f"Unexpected size: {source}")
 for rank,rect in zip("edcbas",BOXES[character]):
  coords=tuple(round(value*(w/724 if index%2==0 else h/2172)) for index,value in enumerate(rect))
  crop=image.crop(coords)
  bounds=crop.getchannel("A").point(lambda v:255 if v>=9 else 0).getbbox()
  if bounds is None: raise ValueError(f"Blank stage: {character}-{rank}")
  margin=max(4,w//60)
  left,top,right,bottom=bounds
  crop=crop.crop((max(0,left-margin),max(0,top-margin),
                  min(crop.width,right+margin),min(crop.height,bottom+margin)))
  crop.save(target/f"{character}-{rank}.webp","WEBP",lossless=True,method=4,exact=True)
  with Image.open(target/f"{character}-{rank}.webp") as check:
   if check.mode!="RGBA":raise ValueError("Transparency lost")

def main():
 p=argparse.ArgumentParser(description=__doc__)
 for key in BOXES:p.add_argument("--"+key,type=Path,required=True)
 p.add_argument("--out",type=Path,required=True)
 args=p.parse_args()
 args.out.mkdir(parents=True,exist_ok=True)
 for key in BOXES:process(getattr(args,key.replace("-","_")),key,args.out)
 print("24 transparent WebP images restored")

if __name__=="__main__":main()

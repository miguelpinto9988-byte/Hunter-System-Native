#!/usr/bin/env python3
"""Valida las capas 3 existentes, sin modificar los SVG ni las poses originales."""
from pathlib import Path
import json
import math

ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/"app/src/main/assets"
LAYERS=ROOT/"docs/layer3/profiles"
TRACKS={"body","shin-near","shin-far","foot-near","foot-far"}

def validate_profile(file):
    data=json.loads(file.read_text(encoding="utf-8"))
    assert data.get("format")=="MonarcaExteriorVolumeLayer",file
    assert data.get("layer")==3 and data.get("fixedProportions") is True,file
    assert data.get("stageScale")=="Unchanged viewBox; no global scale deformation",file
    exercise=data.get("exercise")
    assert exercise and file.stem==exercise,file
    assert (ASSETS/"media2d"/(exercise+".svg")).is_file(),file
    original=ASSETS/"demos30"/(exercise+".json")
    assert original.is_file(),f"Faltan poses para {exercise}"
    poses=json.loads(original.read_text(encoding="utf-8")).get("frames",[])
    assert len(poses)==48 and all(len(f.get("p",[]))==11 for f in poses),exercise
    profiles=data.get("profiles",[])
    assert profiles and len({p.get("id") for p in profiles})==len(profiles),file
    stations=0
    for profile in profiles:
        assert profile.get("track") in TRACKS,profile
        points=profile.get("stations",[])
        assert len(points)>=3,profile
        for p in points:
            assert len(p)==4 and all(isinstance(v,(float,int)) and math.isfinite(v) for v in p),profile
            x,y,l,r=p
            assert 0<=x<=720 and 0<=y<=800 and 0<l<=100 and 0<r<=100,profile
            stations+=1
    return {"exercise":exercise,"profiles":len(profiles),"stations":stations,"frames":len(poses)}

def main():
    originals=list((ASSETS/"media2d").glob("*.svg"))
    historical=list((ASSETS/"demos30").glob("*.json"))
    animated=list((ASSETS/"demos30").glob("*.svg"))
    assert len(originals)==1200 and len(historical)==1200 and not animated
    models=[validate_profile(x) for x in sorted(LAYERS.glob("*.json"))]
    total_catalog=2000
    print(json.dumps({"status":"PASS","layer1_motion_json":len(historical),
        "layer3_ready":len(models),"layer3_pending_original":len(originals)-len(models),
        "expansion_without_verified_motion":total_catalog-len(originals),
        "models":models},ensure_ascii=False,indent=2))

if __name__=="__main__":
    main()

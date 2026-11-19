#!/usr/bin/env python3
"""Independent sampled QA of layer-3 exterior contour stations; never edits source assets."""
import json
import math
import sys
from pathlib import Path

def validate(stations, samples=12):
    if len(stations) < 3:
        raise ValueError("at least three stations required")
    for s in stations:
        if len(s) != 4 or any(isinstance(v, bool) or not isinstance(v, (int, float)) or not math.isfinite(v) for v in s):
            raise ValueError("nonfinite or invalid station")
        x,y,l,r=s
        if not (0<=x<=720 and 0<=y<=800 and 0<l<=100 and 0<r<=100 and 0<=x-l and x+r<=720):
            raise ValueError("station out of stage")
    if any(math.dist(a[:2],b[:2])<1e-6 for a,b in zip(stations,stations[1:])):
        raise ValueError("duplicate adjacent station centers")
    def curve(points):
        tangents=[]
        for i,p in enumerate(points):
            a=points[max(0,i-1)]; b=points[min(len(points)-1,i+1)]
            dx,dy=b[0]-a[0],b[1]-a[1]
            n=math.hypot(dx,dy)
            tangents.append((dx/n,dy/n) if n>1e-9 else (0,0))
        result=[points[0]]
        for i,(a,b) in enumerate(zip(points,points[1:])):
            h=min(math.dist(a,b)*.75/3,math.dist(a,b)*.45)
            c1=(a[0]+h*tangents[i][0],a[1]+h*tangents[i][1])
            c2=(b[0]-h*tangents[i+1][0],b[1]-h*tangents[i+1][1])
            for k in range(1,samples+1):
                t=k/samples; u=1-t
                result.append((u**3*a[0]+3*u*u*t*c1[0]+3*u*t*t*c2[0]+t**3*b[0],
                               u**3*a[1]+3*u*u*t*c1[1]+3*u*t*t*c2[1]+t**3*b[1]))
        return result
    left=curve([(x-l,y) for x,y,l,r in stations])
    right=curve([(x+r,y) for x,y,l,r in stations])
    if any(not(0<=x<=720 and 0<=y<=800) for x,y in left+right):
        raise ValueError("Bezier overshoot beyond stage")
    if any(a[0]>=b[0] for a,b in zip(left,right)):
        raise ValueError("inverted exterior silhouette")
    boundary=left+right[::-1]
    edges=list(zip(boundary,boundary[1:]+boundary[:1]))
    def cross(p,q,r):
        return (q[0]-p[0])*(r[1]-p[1])-(q[1]-p[1])*(r[0]-p[0])
    for i,(a,b) in enumerate(edges):
        for j in range(i+2,len(edges)):
            if i==0 and j==len(edges)-1:
                continue
            c,d=edges[j]
            if cross(a,b,c)*cross(a,b,d)<-1e-8 and cross(c,d,a)*cross(c,d,b)<-1e-8:
                raise ValueError("sampled self-intersection")
    return len(left)+len(right)

def main():
    paths=[Path(p) for p in sys.argv[1:]] or sorted(Path("docs/layer3/profiles").glob("*.json"))
    if not paths:
        raise SystemExit("No layer 3 profiles found")
    report=[]
    for path in paths:
        data=json.loads(path.read_text(encoding="utf-8"))
        for profile in data["profiles"]:
            try:
                validate(profile["stations"])
            except ValueError as exc:
                raise ValueError(f"{path}:{profile['id']}: {exc}") from exc
        report.append({"exercise":data["exercise"],"profiles_validated":len(data["profiles"]),"status":"PASS"})
    print(json.dumps(report,ensure_ascii=False,indent=2))

if __name__=="__main__":
    main()

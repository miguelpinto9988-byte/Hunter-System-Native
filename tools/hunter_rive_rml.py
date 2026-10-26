#!/usr/bin/env python3
"""SVG/SMIL -> native Rive Markup Language with REAL keyed paths and circles.
Not an anatomical X-Ray converter: original SVG styling/motion are retained.
Requires: svgpathtools. Compile the result with official Rive CLI.
Never creates faux .riv by renaming a file.
"""
import argparse
import math
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path
from xml.sax.saxutils import quoteattr
from svgpathtools import parse_path, CubicBezier, QuadraticBezier, Line

def local(element):return element.tag.rsplit("}",1)[-1]
def n(v,default=0.):return float(re.match(r"[-+]?(?:\d+\.?\d*|\.\d+)",str(v)).group()) if re.match(r"[-+]?(?:\d+\.?\d*|\.\d+)",str(v)) else default
def f(v):return f"{float(v):.5f}".rstrip("0").rstrip(".") or "0"
def attrs(k):
    return "".join(" "+str(key)+"="+quoteattr(str(val)) for key,val in k.items() if val is not None)
def el(tag,attributes=None,children=None):return {"tag":tag,"a":attributes or {},"ch":children or []}
def write(node,depth=0):
    pad="  "*depth
    head="<"+node["tag"]+attrs(node["a"])
    if not node["ch"]:return pad+head+"/>\n"
    return pad+head+">\n"+"".join(write(c,depth+1) for c in node["ch"])+pad+"</"+node["tag"]+">\n"
def clamp(v):return max(0,min(255,int(round(v))))
def hexcolor(color,opacity=1.):
    color=str(color or "#afcedd").strip().lower()
    c={'white':'#ffffff','black':'#000000','red':'#ff0000','none':'#000000'}.get(color,color)
    if c.startswith("#") and len(c)==4:c="#"+''.join(x*2 for x in c[1:])
    if not re.fullmatch(r"#[0-9a-f]{6}",c):c="#b2c4d6"
    return f"{clamp(opacity*255):02X}"+c[1:].upper()
def curve(seg):
    if isinstance(seg,CubicBezier):return seg.start,seg.control1,seg.control2,seg.end
    if isinstance(seg,QuadraticBezier):
        return seg.start,seg.start+2/3*(seg.control-seg.start),seg.end+2/3*(seg.control-seg.end),seg.end
    if isinstance(seg,Line):return seg.start,seg.start,seg.end,seg.end
    # Arcs are converted into cubic segments by svgpathtools
    all_parts=list(seg.as_cubic_curves())
    if len(all_parts)!=1:raise ValueError("SVG arc multi-cubic requires split")
    return curve(all_parts[0])
def vertices(data):
    output=[]
    for p in parse_path(data).continuous_subpaths():
        segs=[]
        for segment in p:
            if hasattr(segment,"as_cubic_curves") and not isinstance(segment,(Line,CubicBezier,QuadraticBezier)):
                segs.extend(curve(x) for x in segment.as_cubic_curves())
            else:segs.append(curve(segment))
        if not segs:continue
        closed=abs(segs[0][0]-segs[-1][3])<.01
        anchors=[s[0] for s in segs]
        if not closed:anchors.append(segs[-1][3])
        v=[]
        for i,pt in enumerate(anchors):
            old=segs[i-1] if i>0 else (segs[-1] if closed else None)
            nxt=segs[i] if i<len(segs) else None
            ih=old[2]-pt if old is not None else 0j
            oh=nxt[1]-pt if nxt is not None else 0j
            v.append([pt.real,pt.imag,math.atan2(ih.imag,ih.real),abs(ih),
                      math.atan2(oh.imag,oh.real),abs(oh)])
        output.append((closed,v))
    if not output:raise ValueError("La ruta no contiene vértices")
    return output
def timeline(animation):
    v=(animation.get("values") or "").split(";")
    v=[x.strip() for x in v if x.strip()]
    if len(v)<2:raise ValueError("Animación sin claves suficientes")
    keys=[n(t) for t in (animation.get("keyTimes") or "").split(";") if t.strip()]
    if len(keys)!=len(v):keys=[i/(len(v)-1) for i in range(len(v))]
    if keys[0]!=0 or keys[-1]!=1:raise ValueError("La animación no cierra en los extremos de ciclo")
    return [(min(120,max(0,int(round(t*120)))),val) for t,val in zip(keys,v)]
class Renderer:
    def __init__(self,source):
        self.source=source
        self.root=ET.fromstring(source.read_bytes())
        if local(self.root)!="svg":raise ValueError("No es SVG")
        self.counter=2
        self.animations=[]
        self.errors=[]
        self.timeline=el("LinearAnimation",{"loopValue":"loop","fps":"25","duration":"120",
                                           "name":"Loop","id":"0:900000"})
        self.board=el("Artboard",{"id":"0:1","styleId":"0:900001","defaultStateMachineId":"0:900002",
                                  "width":f(n(self.root.get("width"),480)),"height":f(n(self.root.get("height"),480)),
                                  "name":source.stem})
        self.board["ch"].append(el("LayoutComponentStyle",{"id":"0:900001","name":"Artboard Style"}))
        self.colors={}
        for node in self.root.iter():
            if local(node.tag)=="linearGradient" and node.get("id"):
                stops=[x for x in node if local(x.tag)=="stop"]
                if stops:self.colors[node.get("id")]=stops[0].get("stop-color","#aaaaaa")
    def next(self):
        self.counter+=1
        return "0:"+str(self.counter)
    def style(self,node,st):
        result=dict(st)
        for k in ("fill","stroke","stroke-width","opacity","fill-opacity","stroke-opacity"):
            if node.get(k) is not None:result[k]=node.get(k)
        return result
    def fill(self,s):
        raw=s.get("fill","#acbbc8")
        if raw=="none":return None
        if raw.startswith("url("):
            m=re.search(r"#([^)]+)",raw)
            raw=self.colors.get(m.group(1) if m else ""," #b2c4d6").strip()
        alpha=n(s.get("opacity"),1.)*n(s.get("fill-opacity"),1.)
        return el("Fill",{"name":"Fill"},[el("SolidColor",{"colorValue":hexcolor(raw,alpha),"name":"Color"})])
    def stroke(self,s):
        raw=s.get("stroke","none")
        if raw=="none":return None
        alpha=n(s.get("opacity"),1.)*n(s.get("stroke-opacity"),1.)
        return el("Stroke",{"name":"Stroke","thickness":f(n(s.get("stroke-width"),1.5))},
                  [el("SolidColor",{"colorValue":hexcolor(raw,alpha),"name":"Color"})])
    def key(self,object_id,prop,values):
        frames={}
        for frame,value in values:frames[frame]=float(value)
        if len(frames)<2 or max(frames.values())-min(frames.values())<.0001:return
        node=el("KeyedObject",{"objectId":object_id},[el("KeyedProperty",{"propertyKey":str(prop)},
            [el("KeyFrameDouble",{"frame":str(frame),"value":f(value),"interpolationType":"linear"})
             for frame,value in sorted(frames.items())])])
        self.timeline["ch"].append(node)
    def shape_path(self,node,style):
        d=node.get("d","")
        morphs=[m for m in node if local(m.tag)=="animate" and m.get("attributeName")=="d"]
        phases=timeline(morphs[0]) if morphs else [(0,d),(120,d)]
        shapes=[vertices(val) for _,val in phases]
        counts=[[len(v) for _,v in pose] for pose in shapes]
        if any(x!=counts[0] for x in counts):
            raise ValueError(f"Topología cambiante {node.get('id','?')}: {counts}")
        shape=el("Shape",{"id":self.next(),"name":node.get("id","Path")})
        for j,(closed,base_vertices) in enumerate(shapes[0]):
            path=el("PointsPath",{"isClosed":str(closed).lower(),"name":"Path"})
            for i,base in enumerate(base_vertices):
                vid=self.next()
                names=["x","y","inRotation","inDistance","outRotation","outDistance"]
                path["ch"].append(el("CubicDetachedVertex",dict({"id":vid},
                    **{key:f(val) for key,val in zip(names,base)})))
                if morphs:
                    for key_index,key in enumerate((24,25,84,85,86,87)):
                        self.key(vid,key,[(frame,pose[j][1][i][key_index]) for (frame,_),pose in zip(phases,shapes)])
            shape["ch"].append(path)
        for paint in (self.fill(style),self.stroke(style)):
            if paint:shape["ch"].append(paint)
        return shape
    def shape_circle(self,node,style):
        rx=n(node.get("rx") or node.get("r"),3)
        ry=n(node.get("ry") or node.get("r"),3)
        centerx=n(node.get("cx"),0);centery=n(node.get("cy"),0)
        shape=el("Shape",{"id":self.next(),"name":node.get("id","Circle")})
        vid=self.next()
        shape["ch"].append(el("Ellipse",{"id":vid,"name":"Ellipse","width":f(rx*2),"height":f(ry*2),
                                          "originX":"0.5","originY":"0.5","x":f(centerx),"y":f(centery)}))
        for attr,prop in (("cx",13),("cy",14)):
            an=[a for a in node if local(a.tag)=="animate" and a.get("attributeName")==attr]
            if an:self.key(vid,prop,[(frame,n(val)) for frame,val in timeline(an[0])])
        for paint in (self.fill(style),self.stroke(style)):
            if paint:shape["ch"].append(paint)
        return shape
    def walk(self,node,st):
        style=self.style(node,st)
        tag=local(node.tag)
        if tag=="path" and node.get("d"):self.board["ch"].append(self.shape_path(node,style))
        elif tag in ("circle","ellipse"):self.board["ch"].append(self.shape_circle(node,style))
        elif tag=="rect":
            # Converted to a vector path so it can be rendered without extensions.
            x,y,w,h=[n(node.get(k)) for k in ("x","y","width","height")]
            if w>0 and h>0:
                copy=ET.Element("path",{"id":node.get("id","Rect"),
                  "d":f"M{x} {y}H{x+w}V{y+h}H{x}Z"})
                self.board["ch"].append(self.shape_path(copy,style))
        for child in node:
            if local(child.tag) in ("g","path","circle","ellipse","rect"):
                self.walk(child,style)
    def emit(self):
        if re.search(rb"<!\s*(?:DOCTYPE|ENTITY)",self.source.read_bytes(),re.I):
            raise ValueError("DOCTYPE no permitido")
        self.walk(self.root,{})
        # Don't pass broken/empty assets. Require at least one actual animated numeric property.
        if len(self.timeline["ch"])<2:raise ValueError("Sin movimiento Rive convertible")
        self.board["ch"].append(self.timeline)
        self.board["ch"].append(el("StateMachine",{"name":"Main","id":"0:900002"},[
            el("StateMachineLayer",{"name":"Loop","id":"0:900003"},[
                el("AnyState",{"x":"400","y":"0"}),el("ExitState",{"x":"500","y":"0"}),
                el("EntryState",{},[el("StateTransition",{"stateToId":"0:900004"})]),
                el("AnimationState",{"animationId":"0:900000","id":"0:900004","x":"100","y":"0"})
            ])]))
        return write(el("Rive",{"version":"1","kind":"fragment"},[self.board]))
def main():
    parser=argparse.ArgumentParser()
    parser.add_argument("--source",type=Path,required=True)
    parser.add_argument("--output",type=Path,required=True)
    args=parser.parse_args()
    data=Renderer(args.source).emit()
    args.output.parent.mkdir(parents=True,exist_ok=True)
    args.output.write_text(data,encoding="utf8")
    print(f"{args.source.stem}: {len(data):,} RML bytes, real SMIL motion keyframes")
if __name__=="__main__":main()
